/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.utils.Redaction;

/**
 * The pipeline's request log (Requirements 21.8-21.11): DEBUG only, under the V2 request logger name, lazily built,
 * with sensitive header and query values absent, and no WARN or ERROR event for a failure the caller receives.
 */
class RequestLoggingTest {
    private static final String CANARY = "canary-7f3a9c";

    private final ScriptedHttp http = new ScriptedHttp();

    @Test
    void debugLogNamesTheRequestAndResponseWithoutSensitiveValues() {
        try (Capture capture = Capture.of(RequestLogging.LOGGER_NAME, Level.ALL)) {
            AwsCallEngine engine = engine();
            http.respond(SdkHttpResponse.builder()
                                        .statusCode(200)
                                        .addHeader("x-amzn-RequestId", "req-1")
                                        .addHeader("x-amz-id-2", "ext-1")
                                        .addHeader("Set-Cookie", "session=" + CANARY)
                                        .body(HttpBody.fromUtf8("v"))
                                        .build());

            engine.executeSync(engine.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"),
                               sensitiveOverride(), null);

            assertThat(capture.records()).extracting(LogRecord::getLevel).containsOnly(Level.FINE);
            List<String> messages = capture.messages();
            assertThat(messages).hasSize(2);
            assertThat(messages.get(0))
                .startsWith("Sending Request: POST https://things.example.com")
                .contains("?X-Amz-Security-Token=" + Redaction.REDACTED + "&visible=1")
                .contains("operation=com.example#GetThing", "attempt=1", "Authorization", "X-Name");
            assertThat(messages.get(1)).startsWith("Received successful response: 200, Request ID: req-1, "
                                                   + "Extended Request ID: ext-1");
            assertThat(messages).noneMatch(m -> m.contains(CANARY));
        }
    }

    @Test
    void nothingIsBuiltOrLoggedWhileDebugIsDisabled() {
        try (Capture capture = Capture.of(RequestLogging.LOGGER_NAME, Level.WARNING)) {
            AwsCallEngine engine = engine();
            http.respond(200, "v");
            engine.executeSync(engine.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"),
                               sensitiveOverride(), null);
            assertThat(capture.records()).isEmpty();
        }
    }

    @Test
    void aFailureReturnedToTheCallerIsNotLoggedAtWarnOrError() {
        try (Capture capture = Capture.of("software.amazon.awssdk", Level.ALL)) {
            AwsCallEngine engine = engine();
            http.respond(400, null);
            assertThatThrownBy(() -> engine.executeSync(engine.runtime().plan(GET_THING),
                                                        PipelineFixtures.request(GET_INPUT, "a"), null, null))
                .isInstanceOf(SdkException.class);
            assertThat(capture.records()).isNotEmpty()
                                         .allSatisfy(r -> assertThat(r.getLevel().intValue())
                                             .isLessThan(Level.INFO.intValue()));
            assertThat(capture.messages()).anyMatch(m -> m.startsWith("Received failed response: 400"));
        }
    }

    private AwsCallEngine engine() {
        return AwsCallEngine.builder()
                            .runtime(PipelineFixtures.engineRuntime(PipelineFixtures.retries(1),
                                                                    HttpTransportMode.SYNC).build())
                            .syncHttpClient(http)
                            .build();
    }

    private static AwsRequestOverrideConfiguration sensitiveOverride() {
        return AwsRequestOverrideConfiguration.builder()
                                              .putHeader("Authorization", "Bearer " + CANARY)
                                              .putRawQueryParameter("X-Amz-Security-Token", CANARY)
                                              .putRawQueryParameter("visible", "1")
                                              .build();
    }

    /** Captures the records of a {@code java.util.logging} logger, the default platform-logging backend. */
    private static final class Capture implements AutoCloseable {
        private final java.util.logging.Logger logger;
        private final Level previous;
        private final List<LogRecord> records = new CopyOnWriteArrayList<>();
        private final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        private Capture(String name, Level level) {
            logger = java.util.logging.Logger.getLogger(name);
            previous = logger.getLevel();
            handler.setLevel(Level.ALL);
            logger.setLevel(level);
            logger.addHandler(handler);
        }

        static Capture of(String name, Level level) {
            return new Capture(name, level);
        }

        List<LogRecord> records() {
            return records;
        }

        List<String> messages() {
            return records.stream().map(LogRecord::getMessage).toList();
        }

        @Override
        public void close() {
            logger.removeHandler(handler);
            logger.setLevel(previous);
        }
    }
}
