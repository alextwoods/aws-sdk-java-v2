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

package software.amazon.awssdk.test.fault;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.http.HttpPhase;

/**
 * What {@link FaultyHttpClient} does with one transmission: pure data, so a property test can generate sequences of
 * them. Every fault behaves the same on the sync and async transport, as far as each driver's contract allows:
 *
 * <ul>
 *     <li>{@link Respond}: reads the whole request body, then answers.</li>
 *     <li>{@link ConnectFailure}: fails before reading the request body, in {@link HttpPhase#CONNECT}.</li>
 *     <li>{@link Timeout}: fails with a timeout of its phase, after reading the request body if the phase is
 *     {@link HttpPhase#RESPONSE_CONSUMPTION}.</li>
 *     <li>{@link PartialRequest}: reads at most {@code bytes} of the request body, then fails in
 *     {@link HttpPhase#REQUEST_TRANSMISSION}.</li>
 *     <li>{@link PartialResponse}: delivers the response's status and headers and the first {@code bytes} of its body,
 *     then fails in {@link HttpPhase#RESPONSE_CONSUMPTION}.</li>
 *     <li>{@link Hang}: reads the request body and never answers until the exchange is cancelled or aborted.</li>
 *     <li>{@link HangBody}: delivers the response's status and headers and the first {@code bytes} of its body, then
 *     sends nothing more until the exchange (or its body subscription) is cancelled.</li>
 * </ul>
 */
public sealed interface TransportFault {

    /** A short stable label for reports. */
    String label();

    /** A response with {@code status}, {@code headers} and a UTF-8 {@code body}. */
    static Respond respond(int status, String body, String... headerNamesAndValues) {
        return Respond.of(status, body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8),
                          headerNamesAndValues);
    }

    static ConnectFailure connectFailure() {
        return new ConnectFailure();
    }

    static Timeout timeout(HttpPhase phase) {
        return new Timeout(phase);
    }

    static PartialRequest partialRequest(int bytes) {
        return new PartialRequest(bytes);
    }

    static PartialResponse partialResponse(Respond response, int bytes) {
        return new PartialResponse(response, bytes);
    }

    static Hang hang() {
        return new Hang();
    }

    static HangBody hangBody(Respond response, int bytes) {
        return new HangBody(response, bytes);
    }

    /** A complete HTTP response. The body is copied on the way in and out. */
    record Respond(int status, Map<String, List<String>> headers, byte[] body) implements TransportFault {
        public Respond {
            if (status < 100 || status > 999) {
                throw new IllegalArgumentException("Not an HTTP status code: " + status);
            }
            Map<String, List<String>> copy = new LinkedHashMap<>();
            Objects.requireNonNull(headers, "headers").forEach((name, values) -> copy.put(name, List.copyOf(values)));
            headers = Map.copyOf(copy);
            body = Objects.requireNonNull(body, "body").clone();
        }

        /** A response of {@code status} and {@code body}, with alternating header names and values. */
        public static Respond of(int status, byte[] body, String... headerNamesAndValues) {
            if (headerNamesAndValues.length % 2 != 0) {
                throw new IllegalArgumentException("Header names and values must alternate");
            }
            Map<String, List<String>> headers = new LinkedHashMap<>();
            for (int i = 0; i < headerNamesAndValues.length; i += 2) {
                headers.computeIfAbsent(headerNamesAndValues[i], name -> new ArrayList<>())
                       .add(headerNamesAndValues[i + 1]);
            }
            return new Respond(status, headers, body);
        }

        /** This response with {@code name: value} added. */
        public Respond withHeader(String name, String value) {
            Map<String, List<String>> copy = new LinkedHashMap<>(headers);
            List<String> values = new ArrayList<>(copy.getOrDefault(name, List.of()));
            values.add(value);
            copy.put(name, values);
            return new Respond(status, copy, body);
        }

        @Override
        public byte[] body() {
            return body.clone();
        }

        @Override
        public String label() {
            return "respond(" + status + ")";
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Respond that && status == that.status && headers.equals(that.headers)
                   && Arrays.equals(body, that.body);
        }

        @Override
        public int hashCode() {
            return Objects.hash(status, headers, Arrays.hashCode(body));
        }

        @Override
        public String toString() {
            return "Respond[" + status + ", " + headers + ", " + new String(body, StandardCharsets.UTF_8) + "]";
        }
    }

    /** A connection failure ({@link software.amazon.awssdk.http.HttpFailureCategory#CONNECT}). */
    record ConnectFailure() implements TransportFault {
        @Override
        public String label() {
            return "connect-failure";
        }
    }

    /** A transport timeout during {@code phase} ({@link software.amazon.awssdk.http.HttpFailureCategory#TIMEOUT}). */
    record Timeout(HttpPhase phase) implements TransportFault {
        public Timeout {
            Objects.requireNonNull(phase, "phase");
        }

        @Override
        public String label() {
            return "timeout(" + phase + ")";
        }
    }

    /** A request transmission failure after reading at most {@code bytes} of the request body. */
    record PartialRequest(int bytes) implements TransportFault {
        public PartialRequest {
            if (bytes < 0) {
                throw new IllegalArgumentException("bytes must not be negative");
            }
        }

        @Override
        public String label() {
            return "partial-request(" + bytes + ")";
        }
    }

    /** {@code response}'s head and first {@code bytes} body bytes, then a response consumption failure. */
    record PartialResponse(Respond response, int bytes) implements TransportFault {
        public PartialResponse {
            Objects.requireNonNull(response, "response");
            if (bytes < 0 || bytes >= response.body.length) {
                throw new IllegalArgumentException("bytes must be within the body, before its end: " + bytes);
            }
        }

        @Override
        public String label() {
            return "partial-response(" + response.status + ", " + bytes + ")";
        }
    }

    /** No response until the exchange is cancelled or aborted. */
    record Hang() implements TransportFault {
        @Override
        public String label() {
            return "hang";
        }
    }

    /** {@code response}'s head and first {@code bytes} body bytes, then nothing until cancelled. */
    record HangBody(Respond response, int bytes) implements TransportFault {
        public HangBody {
            Objects.requireNonNull(response, "response");
            if (bytes < 0 || bytes >= response.body.length) {
                throw new IllegalArgumentException("bytes must be within the body, before its end: " + bytes);
            }
        }

        @Override
        public String label() {
            return "hang-body(" + response.status + ", " + bytes + ")";
        }
    }
}
