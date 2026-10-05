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

package software.amazon.awssdk.http.crt.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.http.ContentLengthMismatchException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpFailureCategory.Kind;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;

class CrtFailureMappingTest {
    private static void check(String crtName, HttpPhase current, HttpPhase phase, Kind kind) {
        int code = codeOf(crtName);
        CrtFailureMapping.Classification c = CrtFailureMapping.classify(code, current);
        assertEquals(new CrtFailureMapping.Classification(phase, kind), c, crtName);
        HttpTransportException e = CrtFailureMapping.fromErrorCode(code, current, "AwsCommonRuntime");
        assertEquals(HttpFailureCategory.of(phase, kind), e.category(), crtName);
        assertEquals(phase, e.phase(), crtName);
    }

    /** Looks the code up by the native error name, so the table is checked against the loaded CRT. */
    private static int codeOf(String name) {
        for (int code = 0; code < 4096; code++) {
            if (name.equals(CRT.awsErrorName(code))) {
                return code;
            }
        }
        throw new AssertionError("Unknown CRT error " + name);
    }

    @Test
    void crtErrorCodesMapToThePhaseTable() {
        HttpPhase any = HttpPhase.RESPONSE_CONSUMPTION;
        check("AWS_IO_TLS_ERROR_NEGOTIATION_FAILURE", any, HttpPhase.TLS_HANDSHAKE, Kind.IO);
        check("AWS_IO_TLS_HOST_NAME_MISMATCH", any, HttpPhase.TLS_HANDSHAKE, Kind.IO);
        check("AWS_IO_TLS_NEGOTIATION_TIMEOUT", any, HttpPhase.TLS_HANDSHAKE, Kind.TIMEOUT);
        check("AWS_IO_SOCKET_TIMEOUT", any, HttpPhase.CONNECT, Kind.TIMEOUT);
        check("AWS_IO_SOCKET_CONNECTION_REFUSED", any, HttpPhase.CONNECT, Kind.IO);
        check("AWS_IO_DNS_INVALID_NAME", any, HttpPhase.CONNECT, Kind.IO);
        check("AWS_ERROR_HTTP_PROXY_CONNECT_FAILED", any, HttpPhase.PROXY, Kind.IO);
        check("AWS_ERROR_HTTP_CONNECTION_MANAGER_ACQUISITION_TIMEOUT", any, HttpPhase.CONNECTION_ACQUISITION,
              Kind.TIMEOUT);
        check("AWS_ERROR_HTTP_CONNECTION_MANAGER_MAX_PENDING_ACQUISITIONS_EXCEEDED", any,
              HttpPhase.CONNECTION_ACQUISITION, Kind.IO);
        check("AWS_ERROR_HTTP_RESPONSE_FIRST_BYTE_TIMEOUT", HttpPhase.REQUEST_TRANSMISSION,
              HttpPhase.RESPONSE_CONSUMPTION, Kind.TIMEOUT);
        check("AWS_ERROR_HTTP_CHANNEL_THROUGHPUT_FAILURE", HttpPhase.REQUEST_TRANSMISSION,
              HttpPhase.REQUEST_TRANSMISSION, Kind.TIMEOUT);
        check("AWS_ERROR_HTTP_STREAM_CANCELLED", any, any, Kind.CANCELLED);
        check("AWS_ERROR_HTTP_PROTOCOL_ERROR", any, any, Kind.PROTOCOL_VIOLATION);
        check("AWS_ERROR_HTTP_INVALID_HEADER_VALUE", any, any, Kind.PROTOCOL_VIOLATION);
        check("AWS_ERROR_HTTP_OUTGOING_STREAM_LENGTH_INCORRECT", any, HttpPhase.REQUEST_TRANSMISSION, Kind.IO);
        check("AWS_ERROR_HTTP_CONNECTION_CLOSED", HttpPhase.REQUEST_TRANSMISSION, HttpPhase.REQUEST_TRANSMISSION,
              Kind.IO);
        check("AWS_ERROR_HTTP_SERVER_CLOSED", any, any, Kind.IO);
    }

    @Test
    void everyPhaseOfAnUnknownCodeKeepsTheCurrentPhase() {
        for (HttpPhase phase : HttpPhase.values()) {
            assertEquals(HttpFailureCategory.of(phase, Kind.IO),
                         CrtFailureMapping.fromErrorCode(999_999, phase, "x").category());
        }
    }

    @Test
    void nonCrtFailures() {
        HttpTransportException existing = HttpTransportException.create(HttpPhase.CONNECT, Kind.IO, "m", null);
        assertSame(existing, CrtFailureMapping.toTransportException(new CompletionException(existing),
                                                                    HttpPhase.RESPONSE_CONSUMPTION, "x"));
        assertEquals(HttpFailureCategory.REQUEST_TRANSMISSION,
                     CrtFailureMapping.toTransportException(new ContentLengthMismatchException(3, 2),
                                                            HttpPhase.RESPONSE_CONSUMPTION, "x").category());
        assertEquals(HttpFailureCategory.TIMEOUT,
                     CrtFailureMapping.toTransportException(new TimeoutException(), HttpPhase.CONNECTION_ACQUISITION,
                                                            "x").category());
        HttpTransportException wrapped = CrtFailureMapping.toTransportException(
            new CompletionException(new HttpException(codeOf("AWS_IO_SOCKET_CONNECTION_REFUSED"))),
            HttpPhase.CONNECTION_ACQUISITION, "x");
        assertEquals(HttpFailureCategory.CONNECT, wrapped.category());
        assertEquals(HttpPhase.CONNECT, wrapped.phase());
    }
}
