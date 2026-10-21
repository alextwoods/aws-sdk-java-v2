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

package software.amazon.awssdk.eventnotifications.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.eventnotifications.s3.model.GlacierEventData;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3Bucket;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.eventnotifications.s3.model.S3ObjectAnnotation;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;

/**
 * Compares the schema-based {@code fromJson}, {@code toJson} and {@code toJsonPretty} with the JsonNodeParser reader
 * and JsonWriter writer they replaced (kept in test scope), over every JSON sample in {@code samples/}: the JSON
 * strings of {@code S3EventNotificationReaderTest} and {@code S3EventNotificationWriterTest}, and samples for the
 * cases those tests do not cover (Requirement 38.4).
 */
class S3EventNotificationParityTest {
    private static final S3EventNotificationReader OLD_READER = new DefaultS3EventNotificationReader();
    private static final S3EventNotificationWriter OLD_WRITER = DefaultS3EventNotificationWriter.builder().build();
    private static final S3EventNotificationWriter OLD_PRETTY_WRITER =
        DefaultS3EventNotificationWriter.builder().prettyPrint(true).build();

    @Test
    void samples_coverBothSuitesAndTheExtraCases() {
        assertThat(validSamples()).hasSize(50);
        assertThat(invalidSamples()).hasSize(28);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validSamples")
    void fromJson_validSample_equalsOldReader(String name, byte[] json) {
        S3EventNotification expected = OLD_READER.read(json);

        assertThat(S3EventNotification.fromJson(json)).isEqualTo(expected);
        assertThat(S3EventNotification.fromJson(new String(json, StandardCharsets.UTF_8))).isEqualTo(expected);
        assertThat(S3EventNotification.fromJson(new ByteArrayInputStream(json))).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validSamples")
    void toJson_validSample_equalsOldWriter(String name, byte[] json) {
        S3EventNotification event = OLD_READER.read(json);

        String compact = event.toJson();
        assertThat(compact).isEqualTo(OLD_WRITER.writeToString(event));
        assertThat(event.toJsonPretty()).isEqualTo(OLD_PRETTY_WRITER.writeToString(event));
        assertThat(S3EventNotification.fromJson(compact)).isEqualTo(OLD_READER.read(compact));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidSamples")
    void fromJson_invalidSample_throwsLikeOldReader(String name, byte[] json) {
        Throwable expected = catchThrowable(() -> OLD_READER.read(json));
        assertThat(expected).as("old reader").isNotNull();

        Throwable actual = catchThrowable(() -> S3EventNotification.fromJson(json));
        assertThat(actual).isExactlyInstanceOf(expected.getClass());
        if (expected instanceof UncheckedIOException) {
            // Jackson's message and subclass (JsonEOFException for a truncated input) are not reproduced.
            assertThat(expected).hasCauseInstanceOf(JsonParseException.class);
            assertThat(actual).hasCauseInstanceOf(JsonParseException.class);
        } else {
            assertThat(actual).hasMessage(expected.getMessage());
        }
    }

    @Test
    void knownDifference_numberForStringMember_isReadAsItsText() {
        byte[] json = "{\"Records\":[{\"eventVersion\":2.5}]}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> OLD_READER.read(json)).isInstanceOf(IllegalArgumentException.class)
                                                       .hasMessage("'eventVersion' was not a string");
        assertThat(S3EventNotification.fromJson(json).getRecords().get(0).getEventVersion()).isEqualTo("2.5");
    }

    @Test
    void knownDifference_fractionalSize_throwsIllegalArgumentExceptionNotNumberFormatException() {
        byte[] json = "{\"Records\":[{\"s3\":{\"object\":{\"size\":1.5}}}]}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> OLD_READER.read(json)).isExactlyInstanceOf(NumberFormatException.class);
        // The codec reads the integer part as the long and then fails on the fraction as object syntax.
        assertThatThrownBy(() -> S3EventNotification.fromJson(json))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("expected 'object' to be an object, but was not.");
    }

    @Test
    void knownDifference_controlCharacterEscape_isLowerCaseHex() {
        S3EventNotification event = OLD_READER.read("{\"Records\":[{\"eventName\":\"a\\u001fb\"}]}");

        assertThat(OLD_WRITER.writeToString(event)).contains("\"a\\u001Fb\"");
        assertThat(event.toJson()).contains("\"a\\u001fb\"");
    }

    @Test
    void knownDifference_supplementaryCharacter_isWrittenAsUtf8NotEscaped() {
        S3EventNotification event = OLD_READER.read("{\"Records\":[{\"eventName\":\"a\\uD83D\\uDE00b\"}]}");

        assertThat(OLD_WRITER.writeToString(event)).contains("\"a\\uD83D\\uDE00b\"");
        assertThat(event.toJson()).contains("\"a\uD83D\uDE00b\"");
        assertThat(S3EventNotification.fromJson(event.toJson())).isEqualTo(event);
    }

    @Test
    void knownDifference_glacierEventDataWithoutRestoreEventData_isWrittenAsEmptyObject() {
        S3EventNotification event = withRecord(record -> record.setGlacierEventData(new GlacierEventData(null)));

        assertThat(catchThrowable(() -> OLD_WRITER.writeToString(event))).isNotNull();
        assertThat(event.toJson()).endsWith(",\"s3\":null,\"glacierEventData\":{}}]}");
        assertThat(S3EventNotification.fromJson(event.toJson())).isEqualTo(event);
    }

    @Test
    void knownDifference_nullBucketOwnerAndAnnotation_areWrittenAsNull() {
        S3 s3 = new S3("c", new S3Bucket("b", null, "a"), null, "1.0", Arrays.asList((S3ObjectAnnotation) null));
        S3EventNotification event = withRecord(record -> record.setS3(s3));

        assertThatThrownBy(() -> OLD_WRITER.writeToString(event)).isInstanceOf(NullPointerException.class);
        assertThat(event.toJson()).contains("\"ownerIdentity\":null", "\"objectAnnotation\":[null]");
        assertThat(S3EventNotification.fromJson(event.toJson())).isEqualTo(event);
    }

    private static S3EventNotification withRecord(Consumer<S3EventNotificationRecord> setup) {
        S3EventNotificationRecord record = new S3EventNotificationRecord();
        setup.accept(record);
        return new S3EventNotification(Collections.singletonList(record));
    }

    static List<Arguments> validSamples() {
        return samples("valid");
    }

    static List<Arguments> invalidSamples() {
        return samples("invalid");
    }

    private static List<Arguments> samples(String directory) {
        try {
            Path root = Paths.get(S3EventNotificationParityTest.class.getResource("/samples/" + directory).toURI());
            List<Arguments> samples = new ArrayList<>();
            try (Stream<Path> files = Files.list(root)) {
                for (Path file : files.sorted().collect(Collectors.toList())) {
                    samples.add(Arguments.of(file.getFileName().toString(), Files.readAllBytes(file)));
                }
            }
            return samples;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
