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

package software.amazon.awssdk.protocol.smithytests.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.protocoltests.traits.HttpMessageTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestCase;

/**
 * Compares the content of a legacy case that carries a Smithy case id (a copy of an upstream case) with that Smithy
 * case. {@link Matcher}'s id strategy matches on the id alone, so this check catches a copy the SDK edited.
 *
 * <p>It compares what the legacy case sets or asserts against the Smithy case: the params; for requests the method,
 * URI, host, asserted, required and forbidden headers, and body (by the legacy body assertion kind); for responses and
 * errors the status code, headers, body and asserted error code. A legacy header assertion is a subset check (legacy
 * {@code contains} semantics), so a copy that asserts fewer headers than the Smithy case is not a difference. A legacy
 * assertion this check does not know is reported as a difference, so it is never silently skipped.
 */
final class ContentCheck {

    private static final Set<String> REQUEST_ASSERTIONS = new HashSet<>(Arrays.asList(
        "method", "uri", "headers", "body", "resolvedHost"));
    private static final Set<String> HEADER_ASSERTIONS = new HashSet<>(Arrays.asList(
        "contains", "mustContain", "doesNotContain"));
    private static final Set<String> RESPONSE_FIELDS = new HashSet<>(Arrays.asList(
        "status_code", "headers", "body", "binaryBody"));
    private static final Set<String> THEN_KEYS = new HashSet<>(Arrays.asList(
        "serializedAs", "deserializedAs", "errorCode"));

    private ContentCheck() {
    }

    /**
     * @param legacy the raw legacy case
     * @return one entry per difference, empty when the legacy case matches the Smithy case
     */
    static List<String> differences(ObjectNode legacy, HttpMessageTestCase smithy) {
        List<String> out = new ArrayList<>();
        ObjectNode given = legacy.getObjectMember("given").orElse(Node.objectNode());
        ObjectNode then = legacy.expectObjectMember("then");
        for (String key : then.getStringMap().keySet()) {
            if (!THEN_KEYS.contains(key)) {
                out.add("unchecked assertion then." + key);
            }
        }
        String action = legacy.expectObjectMember("when").expectStringMember("action").getValue();
        if ("marshall".equals(action)) {
            Node params = given.getMember("input").orElse(Node.objectNode());
            compareParams(params, smithy.getParams(), out);
            if (smithy instanceof HttpRequestTestCase) {
                compareRequest(given, then.getObjectMember("serializedAs").orElse(Node.objectNode()),
                               (HttpRequestTestCase) smithy, out);
            } else {
                out.add("legacy case is a request, Smithy case is a response");
            }
        } else {
            Node params = then.getMember("deserializedAs").orElse(Node.objectNode());
            compareParams(params, smithy.getParams(), out);
            if (smithy instanceof HttpResponseTestCase) {
                compareResponse(given.getObjectMember("response").orElse(Node.objectNode()),
                                then.getStringMember("errorCode").map(StringNode::getValue).orElse(null),
                                (HttpResponseTestCase) smithy, out);
            } else {
                out.add("legacy case is a response, Smithy case is a request");
            }
        }
        return out;
    }

    private static void compareParams(Node legacy, ObjectNode smithy, List<String> out) {
        if (!legacy.equals(smithy)) {
            out.add("params differ");
        }
    }

    private static void compareRequest(ObjectNode given, ObjectNode serialized, HttpRequestTestCase smithy,
                                       List<String> out) {
        for (String key : serialized.getStringMap().keySet()) {
            if (!REQUEST_ASSERTIONS.contains(key)) {
                out.add("unchecked assertion serializedAs." + key);
            }
        }
        serialized.getStringMember("method").map(StringNode::getValue).ifPresent(m -> {
            if (!m.equalsIgnoreCase(smithy.getMethod())) {
                out.add("method " + m + " vs " + smithy.getMethod());
            }
        });
        serialized.getStringMember("uri").map(StringNode::getValue).ifPresent(u -> {
            if (!u.equals(smithy.getUri())) {
                out.add("uri " + u + " vs " + smithy.getUri());
            }
        });
        given.getStringMember("host").map(StringNode::getValue).ifPresent(h -> {
            if (!Optional.of(h).equals(smithy.getHost())) {
                out.add("host " + h + " vs " + smithy.getHost().orElse("(none)"));
            }
        });
        serialized.getStringMember("resolvedHost").map(StringNode::getValue).ifPresent(h -> {
            if (!Optional.of(h).equals(smithy.getResolvedHost())) {
                out.add("resolvedHost " + h + " vs " + smithy.getResolvedHost().orElse("(none)"));
            }
        });
        serialized.getObjectMember("headers").ifPresent(h -> compareRequestHeaders(h, smithy, out));
        serialized.getObjectMember("body").ifPresent(b -> {
            for (Map.Entry<String, Node> e : b.getStringMap().entrySet()) {
                String expected = e.getValue().expectStringNode().getValue();
                if (!bodyEquals(e.getKey(), expected, smithy.getBody().orElse(""))) {
                    out.add("body (" + e.getKey() + ") differs");
                }
            }
        });
    }

    private static void compareRequestHeaders(ObjectNode headers, HttpRequestTestCase smithy, List<String> out) {
        for (String key : headers.getStringMap().keySet()) {
            if (!HEADER_ASSERTIONS.contains(key)) {
                out.add("unchecked assertion serializedAs.headers." + key);
            }
        }
        Map<String, String> smithyHeaders = caseInsensitive(smithy.getHeaders());
        headers.getObjectMember("contains").ifPresent(c -> {
            for (Map.Entry<String, Node> e : c.getStringMap().entrySet()) {
                String expected = e.getValue().expectStringNode().getValue();
                String actual = smithyHeaders.get(e.getKey());
                if (!expected.equals(actual)) {
                    out.add("header " + e.getKey() + ": " + expected + " vs " + (actual == null ? "(none)" : actual));
                }
            }
        });
        Set<String> required = lowerCase(smithy.getRequireHeaders());
        required.addAll(lowerCase(smithy.getHeaders().keySet()));
        for (String h : strings(headers.getArrayMember("mustContain").orElse(null))) {
            if (!required.contains(h.toLowerCase(Locale.ROOT))) {
                out.add("required header " + h + " not required by the Smithy case");
            }
        }
        Set<String> forbidden = lowerCase(smithy.getForbidHeaders());
        for (String h : strings(headers.getArrayMember("doesNotContain").orElse(null))) {
            if (!forbidden.contains(h.toLowerCase(Locale.ROOT))) {
                out.add("forbidden header " + h + " not forbidden by the Smithy case");
            }
        }
    }

    private static void compareResponse(ObjectNode response, String errorCode, HttpResponseTestCase smithy,
                                        List<String> out) {
        for (String key : response.getStringMap().keySet()) {
            if (!RESPONSE_FIELDS.contains(key)) {
                out.add("unchecked given.response." + key);
            }
        }
        int code = response.getNumberMember("status_code").map(n -> n.getValue().intValue()).orElse(200);
        if (code != smithy.getCode()) {
            out.add("status " + code + " vs " + smithy.getCode());
        }
        Map<String, String> legacyHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        response.getObjectMember("headers").ifPresent(h -> {
            for (Map.Entry<String, Node> e : h.getStringMap().entrySet()) {
                Node v = e.getValue();
                legacyHeaders.put(e.getKey(), v.isArrayNode() ? String.join(",", strings(v.expectArrayNode()))
                                                              : v.expectStringNode().getValue());
            }
        });
        if (!legacyHeaders.equals(caseInsensitive(smithy.getHeaders()))) {
            out.add("response headers " + legacyHeaders + " vs " + smithy.getHeaders());
        }
        String smithyBody = smithy.getBody().orElse("");
        String mediaType = smithy.getBodyMediaType().orElse("");
        if (response.containsMember("binaryBody")) {
            if (!bodyEquals("cborEquals", response.expectStringMember("binaryBody").getValue(), smithyBody)) {
                out.add("binary body differs");
            }
        } else {
            String body = response.getStringMember("body").map(StringNode::getValue).orElse("");
            String kind = mediaType.contains("json") ? "jsonEquals"
                          : mediaType.contains("xml") ? "xmlEquals" : "equals";
            if (!bodyEquals(kind, body, smithyBody)) {
                out.add("body differs");
            }
        }
        if (errorCode != null) {
            Optional<String> vendorCode = smithy.getVendorParams().getStringMember("code").map(StringNode::getValue);
            if (vendorCode.isPresent()) {
                if (!errorCode.equals(vendorCode.get())) {
                    out.add("error code " + errorCode + " vs " + vendorCode.get());
                }
            } else if (!smithyBody.contains(errorCode) && !smithy.getHeaders().values().stream()
                                                                 .anyMatch(v -> v.contains(errorCode))) {
                out.add("error code " + errorCode + " is not in the Smithy case's response");
            }
        }
    }

    /** Compares an expected legacy body with a Smithy body according to the legacy body assertion kind. */
    static boolean bodyEquals(String kind, String legacy, String smithy) {
        switch (kind) {
            case "jsonEquals":
                if (legacy.trim().isEmpty() || smithy.trim().isEmpty()) {
                    return legacy.trim().equals(smithy.trim());
                }
                try {
                    return Node.parse(legacy).equals(Node.parse(smithy));
                } catch (RuntimeException e) {
                    return false;
                }
            case "xmlEquals":
                return normalizeXml(legacy).equals(normalizeXml(smithy));
            case "queryEquals":
                return queryPairs(legacy).equals(queryPairs(smithy));
            case "cborEquals":
                try {
                    return Arrays.equals(Base64.getDecoder().decode(legacy.trim()),
                                         Base64.getDecoder().decode(smithy.trim()));
                } catch (IllegalArgumentException e) {
                    return false;
                }
            default:
                return legacy.equals(smithy);
        }
    }

    private static String normalizeXml(String xml) {
        return xml.replaceAll(">\\s+<", "><").trim();
    }

    private static List<String> queryPairs(String body) {
        List<String> out = new ArrayList<>();
        for (String pair : body.trim().split("&")) {
            if (!pair.isEmpty()) {
                out.add(pair);
            }
        }
        Collections.sort(out);
        return out;
    }

    private static Map<String, String> caseInsensitive(Map<String, String> headers) {
        Map<String, String> out = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        out.putAll(headers);
        return out;
    }

    private static Set<String> lowerCase(java.util.Collection<String> values) {
        Set<String> out = new HashSet<>();
        for (String v : values) {
            out.add(v.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private static List<String> strings(ArrayNode array) {
        List<String> out = new ArrayList<>();
        if (array != null) {
            for (Node n : array.getElements()) {
                out.add(n.expectStringNode().getValue());
            }
        }
        return out;
    }
}
