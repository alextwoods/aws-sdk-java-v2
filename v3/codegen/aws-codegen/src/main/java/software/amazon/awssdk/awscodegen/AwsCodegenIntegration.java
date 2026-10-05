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

import java.util.List;
import software.amazon.smithy.codegen.core.SmithyIntegration;

/**
 * An AWS customization of the single AWS directed code generator.
 *
 * <p>Integrations are discovered with {@link java.util.ServiceLoader} from the class loader that loaded
 * {@code aws-codegen} (register implementations in
 * {@code META-INF/services/software.amazon.awssdk.awscodegen.AwsCodegenIntegration}) and run in ascending
 * {@code (order(), identifier())} order. That pair is the only ordering authority: an integration must not override
 * Smithy's {@link #priority()}, {@link #runBefore()} or {@link #runAfter()}, and {@link #name()} is always the
 * {@link #identifier()}. Violations, blank or malformed identifiers, and duplicate identifiers fail generation
 * before any integration runs.
 *
 * <p>Integrations observe and customize generation only through the Smithy hooks inherited from
 * {@link SmithyIntegration}: {@link #preprocessModel}, {@link #decorateSymbolProvider}, {@link #interceptors} and
 * {@link #customize}. Every hook receives the Smithy {@code Model} (directly or through {@link AwsCodegenContext}),
 * which remains the only semantic source; an integration must not build or serialize an alternative whole-service
 * representation. Files are written only through {@link AwsCodegenContext#writerDelegator()} or
 * {@link AwsCodegenContext#fileManifest()}, so they are staged and published atomically with the rest of the tree.
 *
 * <p>Every hook runs attributed to the integration's identifier, and generation fails before publication, naming both
 * integrations and the shape, if two contributors produce the same generated file, two integrations change the Java
 * symbol or member name of the same shape, or two integrations change the same trait (or the definition) of the same
 * shape in {@link #preprocessModel}. Each generated file therefore has exactly one owner; an integration changes
 * files it does not own only through {@code CodeInterceptor} sections returned from {@link #interceptors}. The
 * identifier {@value AwsCodegen#GENERATOR_IDENTIFIER} is reserved for the core generator. Integrations render
 * collections in {@link CodegenOrdering} order so the generated tree is byte-identical across runs.
 */
public interface AwsCodegenIntegration
    extends SmithyIntegration<AwsCodegenSettings, AwsJavaWriter, AwsCodegenContext> {

    /**
     * Stable identifier used for ordering and diagnostics, for example {@code aws.endpoints}. Lower-case ASCII
     * letters and digits, in segments separated by {@code .} or {@code -}; the first segment starts with a letter.
     */
    String identifier();

    /** Primary sort key; integrations with lower values run first. Ties are broken by {@link #identifier()}. */
    default int order() {
        return 0;
    }

    /** Always {@link #identifier()}; do not override. */
    @Override
    default String name() {
        return identifier();
    }

    /** Not used by AWS integrations, which are ordered by {@code (order(), identifier())}; do not override. */
    @Override
    default byte priority() {
        return 0;
    }

    /** Not used by AWS integrations, which are ordered by {@code (order(), identifier())}; do not override. */
    @Override
    default List<String> runBefore() {
        return List.of();
    }

    /** Not used by AWS integrations, which are ordered by {@code (order(), identifier())}; do not override. */
    @Override
    default List<String> runAfter() {
        return List.of();
    }
}
