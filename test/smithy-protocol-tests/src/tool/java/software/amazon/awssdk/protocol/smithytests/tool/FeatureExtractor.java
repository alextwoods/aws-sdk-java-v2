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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * Computes a protocol-behavior fingerprint (a set of feature tokens) for a test case, from the operation model and the
 * case's input or output values. Both legacy C2J cases and Smithy cases go through this one class, so equal behavior
 * produces equal tokens regardless of the model format or operation name.
 *
 * <p>Token families:
 * <ul>
 *     <li>{@code kind:request|response|error}</li>
 *     <li>{@code <binding>:<kind>} for each set top-level member (for example {@code header:timestamp})</li>
 *     <li>{@code nested:<kind>} for values inside containers</li>
 *     <li>{@code empty:list|map|string|struct}, {@code null-in-collection}, {@code recursive}, {@code float:nan},
 *     {@code float:inf}</li>
 *     <li>{@code trait:<token>} for serde-relevant member traits; {@code op:<token>} for operation traits</li>
 *     <li>{@code method:<M>} (REST only), {@code forbid:<header>}, {@code assertHeader:<header>}, {@code assert:host}</li>
 *     <li>{@code reqbody:empty|emptyObject}, {@code body:empty}, {@code status:<code>}, {@code unknown-member}</li>
 *     <li>{@code err:header|__type|code|query-error}</li>
 * </ul>
 */
final class FeatureExtractor {

    /** Headers whose presence assertions are protocol behavior rather than member bindings. */
    static final Set<String> PROTOCOL_HEADERS = new HashSet<>(Arrays.asList(
        "content-type", "content-length", "x-amz-target", "content-encoding", "x-amzn-query-mode", "smithy-protocol",
        "accept"));

    private FeatureExtractor() {
    }

    /** Request-side inputs to fingerprinting. */
    static final class RequestFacts {
        String method;
        Collection<String> assertedHeaders = new TreeSet<>();
        Collection<String> forbiddenHeaders = new TreeSet<>();
        boolean assertsHost;
        /** Expected body, or null when the case does not assert a body. */
        String body;
    }

    /** Response-side inputs to fingerprinting. */
    static final class ResponseFacts {
        int statusCode = 200;
        Map<String, String> headers = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        /** Response body text, or null when absent. Binary (CBOR) bodies are passed as null with {@link #binary}. */
        String body;
        boolean binary;
    }

    static Set<String> request(ShapeView.OperationView op, Node params, boolean httpBindings, RequestFacts facts) {
        Set<String> out = new TreeSet<>();
        out.add("kind:request");
        addOperationTokens(op, out);
        if (op.input() == null) {
            out.add("op:noInput");
        } else if (op.input().members().isEmpty()) {
            out.add("op:emptyInput");
        }
        if (httpBindings && facts.method != null) {
            out.add("method:" + facts.method.toUpperCase(Locale.ROOT));
        }
        for (String h : facts.assertedHeaders) {
            String name = h.toLowerCase(Locale.ROOT);
            if (PROTOCOL_HEADERS.contains(name)) {
                out.add("assertHeader:" + name);
            }
        }
        for (String h : facts.forbiddenHeaders) {
            out.add("forbid:" + h.toLowerCase(Locale.ROOT));
        }
        if (facts.assertsHost) {
            out.add("assert:host");
        }
        if (facts.body != null) {
            String trimmed = facts.body.trim();
            if (trimmed.isEmpty()) {
                out.add("reqbody:empty");
            } else if (trimmed.replaceAll("\\s", "").equals("{}")) {
                out.add("reqbody:emptyObject");
            }
        }
        walkStruct(op.input(), params, httpBindings, out);
        return out;
    }

    static Set<String> response(ShapeView.OperationView op, Node params, boolean httpBindings, ResponseFacts facts) {
        Set<String> out = new TreeSet<>();
        out.add("kind:response");
        addOperationTokens(op, out);
        if (op.output() == null) {
            out.add("op:noOutput");
        } else if (op.output().members().isEmpty()) {
            out.add("op:emptyOutput");
        }
        addResponseTokens(op.output(), httpBindings, facts, out);
        if (facts.statusCode != 200) {
            out.add("status:" + facts.statusCode);
        }
        walkStruct(op.output(), params, httpBindings, out);
        return out;
    }

    static Set<String> error(ShapeView errorShape, Node params, boolean httpBindings, ResponseFacts facts) {
        Set<String> out = new TreeSet<>();
        out.add("kind:error");
        if (facts.headers.containsKey("X-Amzn-Errortype")) {
            out.add("err:header");
        }
        if (facts.headers.containsKey("x-amzn-query-error")) {
            out.add("err:query-error");
        }
        ObjectNode json = parseJsonObject(facts.body);
        if (json != null) {
            if (json.containsMember("__type")) {
                out.add("err:__type");
            }
            if (json.containsMember("code")) {
                out.add("err:code");
            }
        }
        addResponseTokens(errorShape, httpBindings, facts, out);
        walkStruct(errorShape, params, httpBindings, out);
        return out;
    }

    private static void addOperationTokens(ShapeView.OperationView op, Set<String> out) {
        for (String t : op.traitTokens()) {
            out.add("op:" + t);
        }
    }

    private static void addResponseTokens(ShapeView shape, boolean httpBindings, ResponseFacts facts, Set<String> out) {
        if (!facts.binary && (facts.body == null || facts.body.trim().isEmpty())) {
            out.add("body:empty");
        }
        ObjectNode json = parseJsonObject(facts.body);
        if (json != null && shape != null) {
            Set<String> known = new HashSet<>();
            for (ShapeView.MemberView m : shape.members()) {
                if (!httpBindings || m.binding() == ShapeView.Binding.BODY) {
                    known.add(m.serializedName());
                }
            }
            for (String key : json.getStringMap().keySet()) {
                if (!known.contains(key) && !"__type".equals(key) && !"code".equals(key) && !"message".equals(key)
                    && !"Message".equals(key)) {
                    out.add("unknown-member");
                }
            }
        }
    }

    private static ObjectNode parseJsonObject(String body) {
        if (body == null || !body.trim().startsWith("{")) {
            return null;
        }
        try {
            return Node.parse(body).asObjectNode().orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void walkStruct(ShapeView shape, Node params, boolean httpBindings, Set<String> out) {
        if (shape == null || params == null || !params.isObjectNode()) {
            return;
        }
        ObjectNode obj = params.expectObjectNode();
        if (obj.isEmpty()) {
            out.add("noparams");
            return;
        }
        Deque<String> stack = new ArrayDeque<>();
        stack.push(shape.id());
        for (Map.Entry<String, Node> e : obj.getStringMap().entrySet()) {
            ShapeView.MemberView member = shape.member(e.getKey());
            if (member == null) {
                out.add("unknown-param");
                continue;
            }
            ShapeView.Binding binding = httpBindings ? member.binding() : ShapeView.Binding.BODY;
            walkValue(member, e.getValue(), binding.token(), stack, out);
        }
    }

    private static void walkValue(ShapeView.MemberView member, Node value, String prefix, Deque<String> stack,
                                  Set<String> out) {
        ShapeView target = member.target();
        out.add(prefix + ":" + target.kind().token());
        for (String t : member.traitTokens()) {
            out.add("trait:" + t);
        }
        if (target.streaming()) {
            out.add("trait:streaming");
        }
        if (value.isNullNode()) {
            out.add("nested".equals(prefix) ? "null-in-collection" : "null-value");
            return;
        }
        switch (target.kind()) {
            case STRUCTURE:
            case UNION:
                walkAggregate(target, value, stack, out);
                break;
            case LIST:
                List<Node> elements = value.asArrayNode().map(ArrayNode::getElements).orElse(null);
                if (elements == null) {
                    return;
                }
                if (elements.isEmpty()) {
                    out.add("empty:list");
                }
                for (Node element : elements) {
                    walkValue(target.listMember(), element, "nested", stack, out);
                }
                break;
            case MAP:
                ObjectNode map = value.asObjectNode().orElse(null);
                if (map == null) {
                    return;
                }
                if (map.isEmpty()) {
                    out.add("empty:map");
                }
                for (Node v : map.getMembers().values()) {
                    walkValue(target.mapValue(), v, "nested", stack, out);
                }
                break;
            case STRING:
            case ENUM:
                if (value.asStringNode().map(StringNode::getValue).map(String::isEmpty).orElse(false)) {
                    out.add("empty:string");
                }
                break;
            case FLOAT:
                String s = value.asStringNode().map(StringNode::getValue).orElse("");
                if ("NaN".equals(s)) {
                    out.add("float:nan");
                } else if (s.endsWith("Infinity")) {
                    out.add("float:inf");
                }
                break;
            default:
                break;
        }
    }

    private static void walkAggregate(ShapeView target, Node value, Deque<String> stack, Set<String> out) {
        ObjectNode obj = value.asObjectNode().orElse(null);
        if (obj == null) {
            return;
        }
        if (stack.contains(target.id())) {
            out.add("recursive");
        }
        if (obj.isEmpty()) {
            out.add("empty:struct");
        }
        stack.push(target.id());
        for (Map.Entry<String, Node> e : obj.getStringMap().entrySet()) {
            ShapeView.MemberView nested = target.member(e.getKey());
            if (nested == null) {
                out.add("unknown-param");
                continue;
            }
            walkValue(nested, e.getValue(), "nested", stack, out);
        }
        stack.pop();
    }
}
