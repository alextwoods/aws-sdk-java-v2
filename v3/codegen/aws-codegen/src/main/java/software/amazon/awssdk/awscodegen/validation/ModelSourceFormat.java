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

import java.util.List;
import java.util.Optional;

/**
 * The format of a file found in a service's model directory. Only {@link #SMITHY_IDL} and {@link #SMITHY_JSON_AST} are
 * canonical model inputs; every other format rejects generation of the service.
 *
 * <p>Legacy formats list the file names V2 code generation used for them. Detection is by content first (see
 * {@link ModelSourceValidator#classify}); a file name is only used when the content alone is not conclusive, for
 * example an empty {@code examples-1.json}.
 */
public enum ModelSourceFormat {
    SMITHY_IDL("Smithy IDL model", null, List.of(), null),
    SMITHY_JSON_AST("Smithy JSON AST model", null, List.of(), null),

    C2J_SERVICE("C2J service model format (service-2.json)", DiagnosticCode.LEGACY_C2J_MODEL,
                List.of("service-2.json", "api-2.json"),
                "the service's canonical Smithy model (for example aws-models <service>/smithy/model.json)"),
    C2J_DOCUMENTATION("C2J documentation model format (docs-2.json)", DiagnosticCode.LEGACY_C2J_MODEL,
                      List.of("docs-2.json"), "@smithy.api#documentation traits in the canonical Smithy model"),
    V2_PAGINATORS("V2 paginator sidecar format (paginators-1.json)", DiagnosticCode.LEGACY_PAGINATOR_SIDECAR,
                  List.of("paginators-1.json", "paginators.json"), "@smithy.api#paginated traits"),
    V2_WAITERS("V2 waiter sidecar format (waiters-2.json)", DiagnosticCode.LEGACY_WAITER_SIDECAR,
               List.of("waiters-2.json"), "@smithy.waiters#waitable traits"),
    V2_ENDPOINT_RULE_SET("V2 endpoint rule-set sidecar format (endpoint-rule-set-1.json)",
                         DiagnosticCode.LEGACY_ENDPOINT_SIDECAR,
                         List.of("endpoint-rule-set-1.json", "endpoint-rule-set.json"),
                         "the @smithy.rules#endpointRuleSet trait"),
    V2_ENDPOINT_BDD("V2 endpoint BDD sidecar format (endpoint-bdd-1.json)", DiagnosticCode.LEGACY_ENDPOINT_SIDECAR,
                    List.of("endpoint-bdd-1.json"), "the @smithy.rules#endpointBdd trait"),
    V2_ENDPOINT_TESTS("V2 endpoint test sidecar format (endpoint-tests-1.json)", DiagnosticCode.LEGACY_ENDPOINT_SIDECAR,
                      List.of("endpoint-tests-1.json", "endpoint-tests.json"), "the @smithy.rules#endpointTests trait"),
    V2_EXAMPLES("V2 example sidecar format (examples-1.json)", DiagnosticCode.LEGACY_EXAMPLE_SIDECAR,
                List.of("examples-1.json"), "@smithy.api#examples traits"),
    V2_CUSTOMIZATION("V2 customization sidecar format (customization.config)",
                     DiagnosticCode.LEGACY_CUSTOMIZATION_SIDECAR, List.of("customization.config"),
                     "canonical Smithy traits or an AwsCodegenIntegration"),
    V2_SMOKE_TESTS("V2 smoke-test sidecar format (smoke.json)", DiagnosticCode.LEGACY_SMOKE_TEST_SIDECAR,
                   List.of("smoke.json"), "@smithy.test#smokeTests traits"),

    UNRECOGNIZED_JSON("unrecognized JSON document", DiagnosticCode.UNRECOGNIZED_MODEL_SOURCE, List.of(), null),
    UNREADABLE("unreadable model file", DiagnosticCode.UNREADABLE_MODEL_SOURCE, List.of(), null);

    private final String description;
    private final DiagnosticCode rejection;
    private final List<String> legacyFileNames;
    private final String canonicalReplacement;

    ModelSourceFormat(String description, DiagnosticCode rejection, List<String> legacyFileNames,
                      String canonicalReplacement) {
        this.description = description;
        this.rejection = rejection;
        this.legacyFileNames = legacyFileNames;
        this.canonicalReplacement = canonicalReplacement;
    }

    /** Exact human-readable name of the format, used as the diagnostic element of a rejected file. */
    public String description() {
        return description;
    }

    /** {@code true} for the Smithy formats that are assembled into the canonical model. */
    public boolean isCanonical() {
        return rejection == null;
    }

    /** {@code true} for C2J and V2 sidecar formats. */
    public boolean isLegacy() {
        return canonicalReplacement != null;
    }

    /** The diagnostic code reported for a file of this format; empty for canonical formats. */
    public Optional<DiagnosticCode> rejection() {
        return Optional.ofNullable(rejection);
    }

    /** File names V2 code generation used for this legacy format; empty for other formats. */
    public List<String> legacyFileNames() {
        return legacyFileNames;
    }

    /** The canonical Smithy representation that replaces this legacy format; empty for other formats. */
    public Optional<String> canonicalReplacement() {
        return Optional.ofNullable(canonicalReplacement);
    }
}
