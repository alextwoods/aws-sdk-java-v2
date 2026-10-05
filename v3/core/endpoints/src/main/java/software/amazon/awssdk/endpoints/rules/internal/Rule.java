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

package software.amazon.awssdk.endpoints.rules.internal;

import java.util.List;
import java.util.Map;

/**
 * One rule of a rule set: its conditions, all of which must hold (be non-empty and not {@code false}) for the rule to
 * apply, and its body: an endpoint, an error, or a tree of further rules.
 */
public sealed interface Rule {

    /** The rule's conditions, evaluated in order. */
    List<Condition> conditions();

    /**
     * A condition: a function expression and the optional name its non-empty result is assigned to.
     *
     * @param function the expression
     * @param assign the name visible to later conditions and the rule body, or {@code null}
     */
    record Condition(Expr function, String assign) {
    }

    /** A rule that resolves an endpoint. */
    record EndpointRule(List<Condition> conditions, EndpointTemplate endpoint) implements Rule {
        public EndpointRule {
            conditions = List.copyOf(conditions);
        }
    }

    /** A rule that fails resolution with the message its error expression evaluates to. */
    record ErrorRule(List<Condition> conditions, Expr error) implements Rule {
        public ErrorRule {
            conditions = List.copyOf(conditions);
        }
    }

    /** A rule whose body is further rules; one of them must match when its conditions hold. */
    record TreeRule(List<Condition> conditions, List<Rule> rules) implements Rule {
        public TreeRule {
            conditions = List.copyOf(conditions);
            rules = List.copyOf(rules);
        }
    }

    /**
     * The endpoint of an endpoint rule.
     *
     * @param url the URL expression
     * @param headers header value expressions by header name, in rule order
     * @param properties property expressions by name, in rule order
     */
    record EndpointTemplate(Expr url, Map<String, List<Expr>> headers, Map<String, Expr> properties) {
    }
}
