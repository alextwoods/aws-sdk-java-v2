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

package software.amazon.awssdk.endpoints;

import java.util.Objects;
import software.amazon.awssdk.endpoints.binding.EndpointBinding;
import software.amazon.awssdk.endpoints.binding.EndpointClientValues;
import software.amazon.awssdk.endpoints.rules.EndpointRuleSet;

/**
 * The endpoint subplan of one operation's pipeline plan: the service's parsed canonical rule set and the operation's
 * generated {@link EndpointBinding}. It holds no request data and is shared by every call of the operation; the
 * rule set itself is shared by identity across the service's operations.
 *
 * @param ruleSet the service's endpoint rule set
 * @param binding the operation's parameter binding, host prefix and discovery facts
 */
public record EndpointPlan(EndpointRuleSet ruleSet, EndpointBinding binding) {
    public EndpointPlan {
        Objects.requireNonNull(ruleSet, "ruleSet");
        Objects.requireNonNull(binding, "binding");
    }

    /**
     * The endpoint of one attempt: binds the parameters from the input and client values, evaluates the rules, and
     * applies the operation's host prefix.
     *
     * @throws EndpointResolutionException if no endpoint can be resolved; nothing may be transmitted
     */
    public Endpoint resolve(Object input, EndpointClientValues client) {
        return binding.resolve(ruleSet, input, client);
    }
}
