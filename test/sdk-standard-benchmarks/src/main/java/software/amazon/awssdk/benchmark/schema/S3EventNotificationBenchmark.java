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

package software.amazon.awssdk.benchmark.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.eventnotifications.s3.internal.DefaultS3EventNotificationReader;
import software.amazon.awssdk.eventnotifications.s3.internal.DefaultS3EventNotificationWriter;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationWriter;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;

/**
 * S3 Event Notifications before and after task 11.19 (design D.7 row 1, Requirement 38.2(e)): {@code fromJson(String)}
 * and {@code toJson()} on the valid JSON samples of the library's test suite.
 *
 * <ul>
 *     <li>{@link Implementation#LEGACY}: the {@code JsonNodeParser} tree reader and {@code JsonWriter} writer the
 *     library used before, called the way its {@code fromJson} and {@code toJson} called them (a new reader per
 *     call, one shared writer). The module build copies both classes from the library's test sources.</li>
 *     <li>{@link Implementation#SCHEMA}: the library's public {@code S3EventNotification.fromJson(String)} and
 *     {@code toJson()}, which run the JSON codec over the generated Schemas.</li>
 * </ul>
 *
 * <p>One operation reads (or writes) every notification of the sample set once, so with {@code ALL_SAMPLES} an
 * operation covers all the samples. Setup checks that both implementations read every sample to equal values, so both
 * write the same notifications. Run with {@code -prof gc} for bytes per op.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(2)
public class S3EventNotificationBenchmark {

    /**
     * The code path under test.
     */
    public enum Implementation {
        /** The previous {@code DefaultS3EventNotificationReader} and {@code DefaultS3EventNotificationWriter}. */
        LEGACY {
            @Override
            S3EventNotification read(String json) {
                return new DefaultS3EventNotificationReader().read(json);
            }

            @Override
            String write(S3EventNotification notification) {
                return LEGACY_WRITER.writeToString(notification);
            }
        },
        /** The library's public API on schemas. */
        SCHEMA {
            @Override
            S3EventNotification read(String json) {
                return S3EventNotification.fromJson(json);
            }

            @Override
            String write(S3EventNotification notification) {
                return notification.toJson();
            }
        };

        abstract S3EventNotification read(String json);

        abstract String write(S3EventNotification notification);
    }

    private static final S3EventNotificationWriter LEGACY_WRITER = DefaultS3EventNotificationWriter.builder().build();

    @Param({"LEGACY", "SCHEMA"})
    private Implementation implementation;

    @Param({"ALL_SAMPLES", "OBJECT_CREATED_PUT"})
    private S3EventNotificationSampleSet sampleSet;

    private String[] json;
    private S3EventNotification[] notifications;

    @Setup
    public void setup() {
        setup(implementation, sampleSet);
    }

    void setup(Implementation implementation, S3EventNotificationSampleSet sampleSet) {
        this.implementation = implementation;
        Map<String, String> loaded = sampleSet.load();
        List<String> texts = new ArrayList<>();
        List<S3EventNotification> parsed = new ArrayList<>();
        for (Map.Entry<String, String> sample : loaded.entrySet()) {
            S3EventNotification legacy = Implementation.LEGACY.read(sample.getValue());
            S3EventNotification schema = Implementation.SCHEMA.read(sample.getValue());
            if (legacy == null ? schema != null : !legacy.equals(schema)) {
                throw new IllegalStateException("The implementations read " + sample.getKey() + " differently");
            }
            texts.add(sample.getValue());
            parsed.add(schema);
        }
        json = texts.toArray(new String[0]);
        notifications = parsed.toArray(new S3EventNotification[0]);
    }

    @Benchmark
    public void read(Blackhole blackhole) {
        for (String text : json) {
            blackhole.consume(implementation.read(text));
        }
    }

    @Benchmark
    public void write(Blackhole blackhole) {
        for (S3EventNotification notification : notifications) {
            blackhole.consume(implementation.write(notification));
        }
    }

    int sampleCount() {
        return json.length;
    }
}
