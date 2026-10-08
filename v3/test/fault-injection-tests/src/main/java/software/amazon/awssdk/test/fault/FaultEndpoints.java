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

import software.amazon.awssdk.endpoints.rules.EndpointRuleSet;

/**
 * Endpoint rule sets that replace a service's rules so every attempt's endpoint resolution is controlled: the real
 * rules engine and endpoint stage run, but the outcome depends only on the {@link EndpointMode}. Neither rule set
 * declares operation context parameters, so they bind to any operation.
 */
public final class FaultEndpoints {
    /** The rule error of {@link EndpointMode#FAIL}. */
    public static final String INJECTED_ERROR = "Injected endpoint resolution failure";

    private static final String RESOLVE = """
        {
          "version": "1.0",
          "parameters": {
            "Endpoint": {"builtIn": "SDK::Endpoint", "required": false, "type": "String",
                         "documentation": "The client endpoint."}
          },
          "rules": [
            {"type": "endpoint", "conditions": [{"fn": "isSet", "argv": [{"ref": "Endpoint"}]}],
             "endpoint": {"url": {"ref": "Endpoint"}, "properties": {}, "headers": {}}},
            {"type": "error", "conditions": [], "error": "The fault harness needs a client endpoint"}
          ]
        }
        """;

    private static final String FAIL = """
        {
          "version": "1.0",
          "parameters": {},
          "rules": [
            {"type": "error", "conditions": [], "error": "%s"}
          ]
        }
        """.formatted(INJECTED_ERROR);

    private FaultEndpoints() {
    }

    /** The rule set of {@code mode}. */
    public static EndpointRuleSet ruleSet(EndpointMode mode) {
        return EndpointRuleSet.fromJson(mode == EndpointMode.RESOLVE ? RESOLVE : FAIL);
    }
}
