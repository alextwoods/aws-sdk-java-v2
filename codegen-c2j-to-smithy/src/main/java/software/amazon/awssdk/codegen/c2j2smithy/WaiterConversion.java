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

package software.amazon.awssdk.codegen.c2j2smithy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.waiters.WaitableTrait;

/**
 * Converts C2J {@code waiters-2.json} waiters into {@code smithy.waiters#waitable} traits on their operations.
 *
 * <p>{@code delay} becomes {@code minDelay} (with {@code maxDelay} raised to it when it exceeds Smithy's default of
 * 120 seconds), and {@code description} becomes {@code documentation}. Acceptors map as follows:
 * <ul>
 *     <li>{@code path}, {@code pathAll}, {@code pathAny} with a string {@code expected}: an {@code output} matcher with
 *     {@code stringEquals}, {@code allStringEquals} or {@code anyStringEquals}.</li>
 *     <li>{@code path} with a boolean {@code expected}: an {@code output} matcher with {@code booleanEquals}.</li>
 *     <li>{@code error} with an error code: {@code errorType}. {@code error} with {@code true} or {@code false}:
 *     {@code success: false} or {@code success: true}.</li>
 *     <li>{@code status} 200: {@code success: true}.</li>
 * </ul>
 * A waiter with any other acceptor is not converted, and is recorded in the gap report. {@code maxAttempts} has no
 * Smithy equivalent and is always recorded.
 */
final class WaiterConversion {
    private static final Pattern WAITER_NAME = Pattern.compile("^[A-Z]+[A-Za-z0-9]*$");
    private static final int SMITHY_DEFAULT_MAX_DELAY = 120;

    private WaiterConversion() {
    }

    /**
     * Convert every waiter of {@code waiters} into a waitable trait per operation.
     *
     * @return the waitable trait per operation ShapeId, in waiter file order
     */
    static Map<ShapeId, Trait> convert(JsonNode waiters, Function<String, ShapeId> operationIds,
                                       ConversionGapReport.Builder gaps) {
        Map<ShapeId, ObjectNode.Builder> byOperation = new LinkedHashMap<>();
        if (waiters != null) {
            for (Map.Entry<String, JsonNode> e : waiters.path("waiters").properties()) {
                ObjectNode waiter = convertOne(e.getKey(), e.getValue(), gaps);
                if (waiter != null) {
                    ShapeId operation = operationIds.apply(e.getValue().path("operation").asText());
                    byOperation.computeIfAbsent(operation, k -> Node.objectNodeBuilder()).withMember(e.getKey(), waiter);
                }
            }
        }
        Map<ShapeId, Trait> result = new LinkedHashMap<>();
        for (Map.Entry<ShapeId, ObjectNode.Builder> e : byOperation.entrySet()) {
            result.put(e.getKey(), new WaitableTrait.Provider().createTrait(e.getKey(), e.getValue().build()));
        }
        return result;
    }

    private static ObjectNode convertOne(String name, JsonNode c2j, ConversionGapReport.Builder gaps) {
        if (!WAITER_NAME.matcher(name).matches()) {
            notConverted("waiter.name(invalid)", "The waiter name is not a valid Smithy waiter name.", gaps);
            return null;
        }
        ArrayNode.Builder acceptors = ArrayNode.builder();
        for (JsonNode acceptor : c2j.path("acceptors")) {
            ObjectNode matcher = matcher(acceptor);
            if (matcher == null) {
                notConverted("waiter.acceptor.matcher=" + acceptor.path("matcher").asText(),
                             "Smithy waiters cannot express this acceptor (for example a status other than 200, or a "
                             + "numeric expected value).", gaps);
                return null;
            }
            acceptors.withValue(Node.objectNodeBuilder()
                                    .withMember("state", acceptor.path("state").asText())
                                    .withMember("matcher", matcher)
                                    .build());
        }
        if (c2j.has("maxAttempts")) {
            gaps.record("waiter.maxAttempts", "Smithy waiters bound waiting by maxWaitTime, not by attempt count. A "
                                              + "waiter generated from standard traits does not get the C2J "
                                              + "maxAttempts.");
        }
        ObjectNode.Builder waiter = Node.objectNodeBuilder();
        if (c2j.hasNonNull("description")) {
            waiter.withMember("documentation", c2j.get("description").asText());
        }
        if (c2j.has("delay")) {
            int delay = c2j.get("delay").asInt();
            waiter.withMember("minDelay", delay);
            if (delay > SMITHY_DEFAULT_MAX_DELAY) {
                waiter.withMember("maxDelay", delay);
            }
        }
        waiter.withMember("acceptors", acceptors.build());
        return waiter.build();
    }

    private static ObjectNode matcher(JsonNode acceptor) {
        JsonNode expected = acceptor.path("expected");
        String matcher = acceptor.path("matcher").asText();
        switch (matcher) {
            case "path":
                if (expected.isBoolean()) {
                    return output(acceptor, String.valueOf(expected.asBoolean()), "booleanEquals");
                }
                return expected.isTextual() ? output(acceptor, expected.asText(), "stringEquals") : null;
            case "pathAll":
                return expected.isTextual() ? output(acceptor, expected.asText(), "allStringEquals") : null;
            case "pathAny":
                return expected.isTextual() ? output(acceptor, expected.asText(), "anyStringEquals") : null;
            case "error":
                if (expected.isBoolean()) {
                    return Node.objectNodeBuilder().withMember("success", !expected.asBoolean()).build();
                }
                return expected.isTextual()
                       ? Node.objectNodeBuilder().withMember("errorType", expected.asText()).build()
                       : null;
            case "status":
                return expected.isIntegralNumber() && expected.asInt() == 200
                       ? Node.objectNodeBuilder().withMember("success", true).build()
                       : null;
            default:
                return null;
        }
    }

    private static ObjectNode output(JsonNode acceptor, String expected, String comparator) {
        if (!acceptor.path("argument").isTextual()) {
            return null;
        }
        return Node.objectNodeBuilder()
                   .withMember("output", Node.objectNodeBuilder()
                                             .withMember("path", acceptor.get("argument").asText())
                                             .withMember("expected", expected)
                                             .withMember("comparator", comparator)
                                             .build())
                   .build();
    }

    private static void notConverted(String property, String reason, ConversionGapReport.Builder gaps) {
        gaps.record(property, reason + " The waiter is not converted, so it is not generated from standard traits.");
    }
}
