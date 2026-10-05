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

package software.amazon.awssdk.tools.paritymanifest;

import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.MALFORMED;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Strict reading of one JSON object: unknown members, wrong types and bad enum values are recorded as
 * {@link ManifestProblem.Code#MALFORMED} problems against a subject instead of failing on the first one.
 */
final class JsonFields {
    private final ObjectNode node;
    private final String subject;
    private final List<ManifestProblem> problems;

    JsonFields(ObjectNode node, String subject, Set<String> allowed, List<ManifestProblem> problems) {
        this.node = node;
        this.subject = subject;
        this.problems = problems;
        node.getStringMap().keySet().stream().filter(k -> !allowed.contains(k)).sorted()
            .forEach(k -> problems.add(new ManifestProblem(MALFORMED, subject, "unknown member '" + k + "'")));
    }

    /** {@code node} as an object, or a problem and {@code null}. */
    static ObjectNode object(Node node, String subject, String what, List<ManifestProblem> problems) {
        if (node.isObjectNode()) {
            return node.expectObjectNode();
        }
        problems.add(new ManifestProblem(MALFORMED, subject, what + " must be an object"));
        return null;
    }

    boolean has(String member) {
        return node.getMember(member).isPresent();
    }

    /** A string or {@code null}; absent and JSON {@code null} both read as {@code null}. */
    String string(String member) {
        Optional<Node> value = node.getMember(member).filter(n -> !n.isNullNode());
        if (value.isEmpty()) {
            return null;
        }
        if (!value.get().isStringNode()) {
            bad(member, "must be a string or null");
            return null;
        }
        return value.get().expectStringNode().getValue();
    }

    /** A required string; records a problem when absent. */
    String requiredString(String member) {
        String value = string(member);
        if (value == null && !node.getMember(member).filter(n -> !n.isNullNode() && !n.isStringNode()).isPresent()) {
            bad(member, "is required");
        }
        return value;
    }

    Boolean bool(String member) {
        Optional<Node> value = node.getMember(member).filter(n -> !n.isNullNode());
        if (value.isEmpty()) {
            return null;
        }
        if (!value.get().isBooleanNode()) {
            bad(member, "must be a boolean");
            return null;
        }
        return value.get().expectBooleanNode().getValue();
    }

    Integer integer(String member) {
        Optional<Node> value = node.getMember(member).filter(n -> !n.isNullNode());
        if (value.isEmpty()) {
            return null;
        }
        if (!value.get().isNumberNode() || !value.get().expectNumberNode().isNaturalNumber()) {
            bad(member, "must be an integer");
            return null;
        }
        return value.get().expectNumberNode().getValue().intValue();
    }

    /** A list of strings; absent reads as empty. */
    List<String> strings(String member) {
        Optional<Node> value = node.getMember(member).filter(n -> !n.isNullNode());
        List<String> out = new ArrayList<>();
        if (value.isEmpty()) {
            return out;
        }
        if (!value.get().isArrayNode()) {
            bad(member, "must be an array of strings");
            return out;
        }
        for (Node element : value.get().expectArrayNode().getElements()) {
            if (element.isStringNode()) {
                out.add(element.expectStringNode().getValue());
            } else {
                bad(member, "must contain only strings");
            }
        }
        return out;
    }

    /** An enum constant parsed from a string member, or {@code null} when absent or invalid. */
    <T> T enumValue(String member, Function<String, T> parse) {
        String value = string(member);
        if (value == null) {
            return null;
        }
        try {
            return parse.apply(value);
        } catch (IllegalArgumentException e) {
            bad(member, "has unknown value '" + value + "'");
            return null;
        }
    }

    Optional<Node> member(String member) {
        return node.getMember(member);
    }

    void bad(String member, String detail) {
        problems.add(new ManifestProblem(MALFORMED, subject, "'" + member + "' " + detail));
    }
}
