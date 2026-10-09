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

package software.amazon.awssdk.benchmark.harness;

import java.util.Locale;
import java.util.Objects;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * One failed validity rule: the rule id (see {@link ValidityRules}), what it invalidates, and a human-readable detail.
 * Recorded as {@code {"rule": ..., "scope": ..., "detail": ...}}.
 *
 * @param scope what the failure invalidates: the repetition (Req 20.27), the whole cell (Req 20.26, 20.28), or the
 *     whole collection (Req 20.25)
 */
public record Violation(String rule, Scope scope, String detail) {

    public enum Scope {
        REPETITION, CELL, COLLECTION;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public Violation {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(detail, "detail");
    }

    public static Violation repetition(String rule, String detail) {
        return new Violation(rule, Scope.REPETITION, detail);
    }

    public static Violation cell(String rule, String detail) {
        return new Violation(rule, Scope.CELL, detail);
    }

    public static Violation collection(String rule, String detail) {
        return new Violation(rule, Scope.COLLECTION, detail);
    }

    public ObjectNode toNode() {
        return ObjectNode.builder().withMember("rule", rule).withMember("scope", scope.id())
                         .withMember("detail", detail).build();
    }
}
