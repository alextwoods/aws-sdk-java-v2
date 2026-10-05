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

package software.amazon.awssdk.awscodegen.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Runs the required-AWS-behavior {@link SemanticCheck}s over the assembled canonical model and aggregates every
 * problem they find (Requirement 2.10).
 *
 * <p>The default checks cover the behaviors every generated client needs at this stage of V3:
 * <ul>
 *   <li>{@code aws.protocol}: the service applies at least one supported launch protocol;</li>
 *   <li>{@code aws.auth}: every operation resolves a supported auth scheme or explicitly modeled anonymous access.</li>
 * </ul>
 * Components that start depending on further canonical semantics add their check here when their emitter lands, for
 * example: the endpoint emitter requires {@code @smithy.rules#endpointRuleSet} (or {@code endpointBdd}) with
 * {@code @smithy.rules#endpointTests}; client naming requires {@code @aws.api#service} with an {@code sdkId}; SigV4
 * signing requires a signing name consistent with {@code @aws.api#service}; query protocols require
 * {@code @xmlNamespace}; paginator, waiter, checksum, presigning and event-stream emitters require their traits to be
 * representable by the runtime.
 */
public final class SemanticValidator {

    /** The checks {@code aws-codegen} runs for every service, in execution order. */
    public static final List<SemanticCheck> DEFAULT_CHECKS = List.of(new ProtocolCheck(), new AuthCheck());

    private SemanticValidator() {
    }

    /** Runs {@link #DEFAULT_CHECKS}. */
    public static List<Diagnostic> validate(Model model, ServiceShape service) {
        return validate(model, service, DEFAULT_CHECKS);
    }

    /**
     * Runs every check and returns all of their diagnostics, sorted.
     *
     * @throws IllegalArgumentException if two checks share an identifier
     */
    public static List<Diagnostic> validate(Model model, ServiceShape service, List<? extends SemanticCheck> checks) {
        Set<String> ids = new HashSet<>();
        for (SemanticCheck check : checks) {
            if (!ids.add(check.id())) {
                throw new IllegalArgumentException("Two semantic checks share identifier '" + check.id() + "'.");
            }
        }
        return checks.stream().flatMap(check -> check.check(model, service).stream()).sorted().toList();
    }
}
