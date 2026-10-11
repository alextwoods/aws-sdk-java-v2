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

package software.amazon.awssdk.protocol.smithytests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.utils.Validate;

/**
 * Assertions emitted by the protocol test generator. Each request assertion maps to one property of a Smithy
 * {@code HttpRequestTestCase}; {@link #outputEquals} implements the {@code params} comparison of an
 * {@code HttpResponseTestCase}. Every method throws {@link AssertionError} with the expected and actual values on failure.
 */
public final class ProtocolAsserts {

    private ProtocolAsserts() {
    }

    /**
     * {@code method}: the HTTP method equals {@code expected}, ignoring case.
     */
    public static void method(CapturedRequest request, String expected) {
        Validate.paramNotNull(expected, "expected");
        if (!expected.equalsIgnoreCase(request.method())) {
            throw new AssertionError("Expected HTTP method " + expected + " but was " + request.method());
        }
    }

    /**
     * {@code uri}: the percent-encoded request path equals {@code expected}, without the query string.
     */
    public static void uri(CapturedRequest request, String expected) {
        Validate.paramNotNull(expected, "expected");
        if (!expected.equals(request.path())) {
            throw new AssertionError("Expected request path " + expected + " but was " + request.path());
        }
    }

    /**
     * {@code queryParams}: each percent-encoded {@code name[=value]} entry is present in the request query string, once
     * per occurrence in {@code expected}. Names and values are compared after percent-decoding, so equivalent encodings
     * (for example {@code %2a} and {@code %2A}) match. Other query parameters may also be present.
     */
    public static void queryParams(CapturedRequest request, List<String> expected) {
        List<QueryParameter> remaining = new ArrayList<>(request.queryParameters());
        List<String> missing = new ArrayList<>();
        for (String entry : expected) {
            if (!remaining.remove(QueryParameter.parse(entry))) {
                missing.add(entry);
            }
        }
        if (!missing.isEmpty()) {
            throw new AssertionError("Expected query parameters " + missing + " to be present, but the query parameters were "
                                     + request.queryParameters());
        }
    }

    /**
     * {@code forbidQueryParams}: no query parameter has one of these names.
     */
    public static void forbidQueryParams(CapturedRequest request, List<String> names) {
        List<String> present = new ArrayList<>();
        for (String name : names) {
            if (hasQueryParam(request, name)) {
                present.add(name);
            }
        }
        if (!present.isEmpty()) {
            throw new AssertionError("Expected query parameters " + present + " to be absent, but the query parameters were "
                                     + request.queryParameters());
        }
    }

    /**
     * {@code requireQueryParams}: a query parameter with each of these names is present, with any value.
     */
    public static void requireQueryParams(CapturedRequest request, List<String> names) {
        List<String> absent = new ArrayList<>();
        for (String name : names) {
            if (!hasQueryParam(request, name)) {
                absent.add(name);
            }
        }
        if (!absent.isEmpty()) {
            throw new AssertionError("Expected query parameters " + absent + " to be present, but the query parameters were "
                                     + request.queryParameters());
        }
    }

    private static boolean hasQueryParam(CapturedRequest request, String name) {
        String decoded = PercentDecoding.decode(name);
        return request.queryParameters().stream().anyMatch(p -> p.name().equals(decoded));
    }

    /**
     * {@code resolvedHost}: the request host equals {@code expected}, ignoring case.
     */
    public static void resolvedHost(CapturedRequest request, String expected) {
        Validate.paramNotNull(expected, "expected");
        if (!expected.equalsIgnoreCase(request.host())) {
            throw new AssertionError("Expected resolved host " + expected + " but was " + request.host());
        }
    }

    /**
     * {@code headers}: each header is present with an equal value. Names compare case-insensitively; a header sent more
     * than once is compared as its values joined with {@code ", "}. Other headers may also be present.
     */
    public static void headers(CapturedRequest request, Map<String, String> expected) {
        List<String> mismatches = new ArrayList<>();
        expected.forEach((name, value) -> {
            Optional<String> actual = request.header(name);
            if (!actual.isPresent()) {
                mismatches.add(name + ": expected \"" + value + "\" but the header is absent");
            } else if (!actual.get().equals(value)) {
                mismatches.add(name + ": expected \"" + value + "\" but was \"" + actual.get() + "\"");
            }
        });
        if (!mismatches.isEmpty()) {
            throw new AssertionError("Header mismatch:\n  " + String.join("\n  ", mismatches) + "\nActual headers: "
                                     + request.headers());
        }
    }

    /**
     * {@code forbidHeaders}: no header has one of these names, ignoring case.
     */
    public static void forbidHeaders(CapturedRequest request, List<String> names) {
        List<String> present = new ArrayList<>();
        for (String name : names) {
            if (!request.headerValues(name).isEmpty()) {
                present.add(name);
            }
        }
        if (!present.isEmpty()) {
            throw new AssertionError("Expected headers " + present + " to be absent, but the headers were "
                                     + request.headers());
        }
    }

    /**
     * {@code requireHeaders}: a header with each of these names is present, ignoring case, with any value.
     */
    public static void requireHeaders(CapturedRequest request, List<String> names) {
        List<String> absent = new ArrayList<>();
        for (String name : names) {
            if (request.headerValues(name).isEmpty()) {
                absent.add(name);
            }
        }
        if (!absent.isEmpty()) {
            throw new AssertionError("Expected headers " + absent + " to be present, but the headers were "
                                     + request.headers());
        }
    }

    /**
     * {@code body} and {@code bodyMediaType}: the request body matches {@code expected} using the comparator for
     * {@code mediaType} (see {@link BodyComparators}). An empty {@code expected} matches only an empty body.
     */
    public static void body(CapturedRequest request, String expected, String mediaType) {
        BodyComparators.assertBodyEquals(expected, mediaType, request.body());
    }

    /**
     * {@code params} of a response test: {@code actual} (a deserialized output or a thrown modeled exception) equals
     * {@code expected} member by member:
     *
     * <ul>
     *     <li>both values have the same class, and {@link software.amazon.awssdk.core.SdkPojo}s are compared through their
     *     {@code sdkFields()}, recursively</li>
     *     <li>lists compare in order and maps by key</li>
     *     <li>blobs ({@link software.amazon.awssdk.core.SdkBytes}, {@code byte[]}, {@link java.nio.ByteBuffer}) compare by
     *     bytes, and timestamps by instant</li>
     *     <li>{@code Float} and {@code Double} compare with {@code equals}, so NaN equals NaN</li>
     *     <li>{@code BigDecimal}s compare by numeric value</li>
     *     <li>documents compare structurally with number promotion: numbers compare by value whatever their Java type</li>
     * </ul>
     *
     * The failure message lists every differing member path, for example {@code $.Nested.Values[2]}.
     */
    public static void outputEquals(Object expected, Object actual) {
        ShapeComparison.assertShapesEqual(expected, actual);
    }
}
