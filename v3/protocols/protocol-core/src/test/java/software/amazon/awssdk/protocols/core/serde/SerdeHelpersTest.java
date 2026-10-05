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

package software.amazon.awssdk.protocols.core.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.UriEncoding;
import software.amazon.awssdk.protocols.core.binding.HttpBindingLocation;
import software.amazon.awssdk.protocols.core.error.ErrorCodes;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/** Shared protocol helpers: timestamps, error codes, request targets and the HTTP message values they use. */
class SerdeHelpersTest {
    @Test
    void epochSecondsAreReadExactly() {
        assertEquals(Instant.ofEpochSecond(512, 35_000_000), TimestampFormats.fromEpochSeconds(512.035));
        assertEquals(Instant.ofEpochSecond(512, 35_000_000),
                     TimestampFormats.EXACT_EPOCH_SECONDS.readFromNumber(512.035));
        assertEquals(Instant.ofEpochSecond(1515531081, 123_400_000),
                     TimestampFormats.EXACT_EPOCH_SECONDS.readFromString("1515531081.1234", true));
        assertEquals(Instant.ofEpochSecond(-2, 500_000_000), TimestampFormats.fromEpochSeconds(new BigDecimal("-1.5")));
        assertEquals(Instant.ofEpochSecond(7), TimestampFormats.fromEpochSeconds(7L));
        assertThrows(SerializationException.class, () -> TimestampFormats.fromEpochSeconds(Double.NaN));
    }

    @Test
    void bindingLocationsHaveSmithyDefaultFormats() {
        assertEquals(TimestampFormatTrait.Format.HTTP_DATE, TimestampFormats.defaultFormat(HttpBindingLocation.HEADER));
        assertEquals(TimestampFormatTrait.Format.DATE_TIME, TimestampFormats.defaultFormat(HttpBindingLocation.QUERY));
        assertEquals(TimestampFormatTrait.Format.DATE_TIME, TimestampFormats.defaultFormat(HttpBindingLocation.LABEL));
        assertThrows(IllegalArgumentException.class, () -> TimestampFormats.defaultFormat(HttpBindingLocation.BODY));
        Instant instant = Instant.parse("2019-12-16T23:48:18Z");
        assertEquals("Mon, 16 Dec 2019 23:48:18 GMT",
                     TimestampFormats.write(instant, TimestampFormatTrait.Format.HTTP_DATE));
        assertEquals("2019-12-16T23:48:18Z", TimestampFormats.write(instant, TimestampFormatTrait.Format.DATE_TIME));
        assertEquals(instant, TimestampFormats.read("1576540098", TimestampFormatTrait.Format.EPOCH_SECONDS));
        // Epoch-seconds text is exact at millisecond precision, before the epoch too.
        assertEquals("1576540098.500", TimestampFormats.write(Instant.ofEpochMilli(1576540098500L),
                                                              TimestampFormatTrait.Format.EPOCH_SECONDS));
        Instant beforeEpoch = Instant.parse("1935-12-31T23:59:59.5Z");
        assertEquals("-1073001600.500", TimestampFormats.write(beforeEpoch, TimestampFormatTrait.Format.EPOCH_SECONDS));
        assertEquals(beforeEpoch, TimestampFormats.read("-1073001600.500", TimestampFormatTrait.Format.EPOCH_SECONDS));
        assertEquals("-1", TimestampFormats.write(Instant.ofEpochSecond(-1), TimestampFormatTrait.Format.EPOCH_SECONDS));
    }

    @Test
    void errorTypesAreSanitized() {
        assertEquals("FooError", ErrorCodes.sanitizeErrorType("FooError"));
        assertEquals("FooError", ErrorCodes.sanitizeErrorType("aws.protocoltests.restjson#FooError"));
        assertEquals("FooError", ErrorCodes.sanitizeErrorType(
            "aws.protocoltests.restjson#FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/"));
        assertEquals("FooError", ErrorCodes.sanitizeErrorType(" FooError:http://x "));
        assertNull(ErrorCodes.sanitizeErrorType(" "));
        assertNull(ErrorCodes.sanitizeErrorType(null));
    }

    @Test
    void queryErrorHeaderIsParsed() {
        assertEquals(Optional.of(new ErrorCodes.QueryError("AWS.SimpleQueueService.NonExistentQueue", "Sender")),
                     ErrorCodes.parseQueryError("AWS.SimpleQueueService.NonExistentQueue;Sender"));
        assertEquals(Optional.of(new ErrorCodes.QueryError("Code", null)), ErrorCodes.parseQueryError("Code"));
        assertEquals(Optional.empty(), ErrorCodes.parseQueryError(";Sender"));
        HttpHeaders headers = HttpHeaders.builder().add("X-Amz-Request-Id", "r").build();
        assertEquals(Optional.of("r"), ErrorCodes.requestId(headers));
    }

    @Test
    void requestTargetsJoinBasePaths() {
        assertEquals("/", RequestTargets.joinPath("", "/"));
        assertEquals("/", RequestTargets.joinPath("/", "/"));
        assertEquals("/base/", RequestTargets.joinPath("/base", "/"));
        assertEquals("/base/op/x", RequestTargets.joinPath("/base/", "/op/x"));
        assertEquals("/op", RequestTargets.joinPath(null, "/op"));
        SdkHttpRequest request = RequestTargets.start(java.net.URI.create("http://localhost:8080"), "post", "/")
                                               .addQueryParameter("a b", "c/d")
                                               .addQueryParameter("flag", null)
                                               .build();
        assertEquals("POST", request.method());
        assertEquals("localhost:8080", request.authority());
        assertEquals("/?a%20b=c%2Fd&flag", request.rawTarget());
        assertThrows(IllegalArgumentException.class,
                     () -> RequestTargets.start(java.net.URI.create("https://h/?q=1"), "GET", "/"));
    }

    @Test
    void headersPreserveOrderCaseAndRepeats() {
        HttpHeaders headers = HttpHeaders.builder().add("X-A", "1").add("x-b", "2").add("x-a", "3").build();
        assertEquals(3, headers.size());
        assertEquals("X-A", headers.name(0));
        assertEquals("x-a", headers.name(2));
        assertEquals(java.util.List.of("1", "3"), headers.allValues("x-A"));
        HttpHeaders replaced = headers.toBuilder().set("X-A", "4").build();
        assertEquals(java.util.List.of("x-b", "X-A"), java.util.List.of(replaced.name(0), replaced.name(1)));
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.builder().add("X", "a\r\nb"));
        assertEquals("a%20b%2F%E2%82%AC~", UriEncoding.encode("a b/\u20ac~"));
        assertEquals("a/b%20c", UriEncoding.encode("a/b c", true));
    }
}
