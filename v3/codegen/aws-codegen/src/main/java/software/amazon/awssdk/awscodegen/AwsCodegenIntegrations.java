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

package software.amazon.awssdk.awscodegen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

/**
 * Discovers {@link AwsCodegenIntegration}s and puts them in execution order.
 */
final class AwsCodegenIntegrations {

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z][a-z0-9]*([.-][a-z0-9]+)*");

    /** Execution order: ascending {@code order()}, then ascending {@code identifier()}. */
    static final Comparator<AwsCodegenIntegration> EXECUTION_ORDER =
        Comparator.comparingInt(AwsCodegenIntegration::order).thenComparing(AwsCodegenIntegration::identifier);

    private AwsCodegenIntegrations() {
    }

    /** Every integration registered with {@link ServiceLoader} in {@code classLoader}, in execution order. */
    static List<AwsCodegenIntegration> discover(ClassLoader classLoader) {
        List<AwsCodegenIntegration> found = new ArrayList<>();
        try {
            ServiceLoader.load(AwsCodegenIntegration.class, classLoader).forEach(found::add);
        } catch (ServiceConfigurationError e) {
            throw new AwsCodegenException("Unable to load AwsCodegenIntegration implementations: " + e.getMessage(), e);
        }
        return order(found);
    }

    /**
     * Validates {@code integrations} and returns them in execution order. The result does not depend on the order
     * in which they were enumerated.
     *
     * @throws AwsCodegenException if an identifier is malformed or shared by two integrations, or an integration
     *         uses a Smithy ordering mechanism other than {@code (order(), identifier())}
     */
    static List<AwsCodegenIntegration> order(Iterable<? extends AwsCodegenIntegration> integrations) {
        List<AwsCodegenIntegration> result = new ArrayList<>();
        Map<String, AwsCodegenIntegration> byIdentifier = new HashMap<>();
        List<String> problems = new ArrayList<>();
        for (AwsCodegenIntegration integration : integrations) {
            String type = integration.getClass().getName();
            String identifier = integration.identifier();
            if (identifier == null || !IDENTIFIER.matcher(identifier).matches()) {
                problems.add("Integration " + type + " has invalid identifier '" + identifier + "' (expected "
                             + "lower-case segments separated by '.' or '-', for example 'aws.endpoints').");
                continue;
            }
            if (identifier.equals(AwsCodegen.GENERATOR_IDENTIFIER)) {
                problems.add("Integration " + type + " uses the identifier '" + identifier + "', which is reserved for "
                             + "the core generator in generation diagnostics.");
                continue;
            }
            AwsCodegenIntegration previous = byIdentifier.putIfAbsent(identifier, integration);
            if (previous != null) {
                List<String> types = new ArrayList<>(List.of(previous.getClass().getName(), type));
                types.sort(null);
                problems.add("Integrations " + types.get(0) + " and " + types.get(1) + " share identifier '"
                             + identifier + "'.");
                continue;
            }
            if (!identifier.equals(integration.name())) {
                problems.add("Integration '" + identifier + "' (" + type + ") overrides name() as '"
                             + integration.name() + "'; name() must equal identifier().");
            }
            if (integration.priority() != 0 || !integration.runBefore().isEmpty()
                || !integration.runAfter().isEmpty()) {
                problems.add("Integration '" + identifier + "' (" + type + ") uses Smithy priority/runBefore/"
                             + "runAfter ordering; AWS integrations are ordered only by (order(), identifier()).");
            }
            result.add(integration);
        }
        if (!problems.isEmpty()) {
            problems.sort(null);
            throw new AwsCodegenException("Invalid aws-codegen integrations:\n  " + String.join("\n  ", problems));
        }
        result.sort(EXECUTION_ORDER);
        return List.copyOf(result);
    }
}
