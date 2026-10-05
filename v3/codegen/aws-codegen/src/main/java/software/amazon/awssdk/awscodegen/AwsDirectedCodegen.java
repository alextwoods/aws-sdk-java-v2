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
import software.amazon.awssdk.awscodegen.model.ModelTypeGenerator;
import software.amazon.awssdk.awscodegen.schema.SchemaGenerator;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.directed.CreateContextDirective;
import software.amazon.smithy.codegen.core.directed.CreateSymbolProviderDirective;
import software.amazon.smithy.codegen.core.directed.CustomizeDirective;
import software.amazon.smithy.codegen.core.directed.DirectedCodegen;
import software.amazon.smithy.codegen.core.directed.GenerateEnumDirective;
import software.amazon.smithy.codegen.core.directed.GenerateErrorDirective;
import software.amazon.smithy.codegen.core.directed.GenerateIntEnumDirective;
import software.amazon.smithy.codegen.core.directed.GenerateOperationDirective;
import software.amazon.smithy.codegen.core.directed.GenerateServiceDirective;
import software.amazon.smithy.codegen.core.directed.GenerateStructureDirective;
import software.amazon.smithy.codegen.core.directed.GenerateUnionDirective;
import software.amazon.smithy.model.shapes.Shape;

/**
 * The AWS SDK for Java V3 {@link DirectedCodegen}: the only AWS generator. It is driven by Smithy's
 * {@code CodegenDirector} from {@link AwsCodegen#generate(AwsCodegenRequest)} and receives shapes directly from the
 * Smithy {@code Model}; it keeps no whole-service state between directives.
 *
 * <p>Every emitter traverses the directive's {@code Model}, Smithy indexes, traits and symbols directly and renders
 * one file from a file-local {@link RenderPlan}; no emitter builds, caches or serializes a whole-service
 * representation (Requirements 3.2-3.4).
 *
 * <ul>
 *   <li>The service directive emits the {@value AwsCodegen#SERVICE_INFO_TYPE} identity type, the service's schemas
 *       ({@link SchemaGenerator}: the {@code <Service>Schemas} holder with the service, operation and model-type
 *       schemas), the static {@code <Service>Operations} table ({@link OperationTableGenerator}), the primary
 *       {@code <Service>TypeRegistry} ({@link TypeRegistryGenerator}), the public
 *       {@code <Service>ServiceMetadata} ({@link ServiceMetadataGenerator}) and, for a service with an endpoint rule
 *       set, its endpoint parameters, provider, rules and operation bindings ({@link EndpointRulesGenerator}).</li>
 *   <li>The operation directive emits one static descriptor per operation ({@link OperationDescriptorGenerator}),
 *       linked to the operation's schemas.</li>
 *   <li>The structure, error, union, enum and intEnum directives emit the immutable model types and builders
 *       ({@link ModelTypeGenerator}); the service directive also emits the service's base exception.</li>
 * </ul>
 * The service directive also emits the sync and async clients, their builders and default implementations
 * ({@link ClientGenerator}). Before publication,
 * {@link GeneratedSourceGuard} rejects any generated class-name lookup, reflection or embedded model.
 *
 * <p>When the file manifest carries a {@link ContributionLedger}, every shape directive runs attributed to the core
 * generator ({@value AwsCodegen#GENERATOR_IDENTIFIER}) and the directive's shape, and creating the context checks the
 * integrations' model and symbol-provider changes for conflicts before any file is generated (Requirement 3.7).
 * Creating the context then rejects generated Java types that would share a name or a source file
 * ({@link GeneratedNameCollisions}), on the final symbol provider.
 */
final class AwsDirectedCodegen
    implements DirectedCodegen<AwsCodegenContext, AwsCodegenSettings, AwsCodegenIntegration> {

    private final String modelDigest;
    private final ModelRevision modelRevision;
    private final List<String> integrationOrder;
    private AwsCodegenContext context;

    /**
     * @param modelDigest location-independent digest of the model input files
     * @param integrationOrder identifiers of the integrations in the order {@link AwsCodegenIntegrations} computed
     */
    AwsDirectedCodegen(String modelDigest, List<String> integrationOrder) {
        this(modelDigest, ModelRevision.of(null, modelDigest), integrationOrder);
    }

    /**
     * @param modelDigest location-independent digest of the model input files
     * @param modelRevision exact immutable revision of the canonical model source
     * @param integrationOrder identifiers of the integrations in the order {@link AwsCodegenIntegrations} computed
     */
    AwsDirectedCodegen(String modelDigest, ModelRevision modelRevision, List<String> integrationOrder) {
        this.modelDigest = modelDigest;
        this.modelRevision = modelRevision;
        this.integrationOrder = List.copyOf(integrationOrder);
    }

    /**
     * The context this generator created for its (single) generation invocation, used after the director has run to
     * emit the provenance and generation manifest (phase 9) from the same model, service and symbols.
     *
     * @throws IllegalStateException if the director has not created the context
     */
    AwsCodegenContext context() {
        if (context == null) {
            throw new IllegalStateException("The directed generation context has not been created.");
        }
        return context;
    }

    @Override
    public SymbolProvider createSymbolProvider(CreateSymbolProviderDirective<AwsCodegenSettings> directive) {
        return new AwsSymbolProvider(directive.model(), directive.expectService(), directive.settings());
    }

    @Override
    public AwsCodegenContext createContext(
        CreateContextDirective<AwsCodegenSettings, AwsCodegenIntegration> directive) {
        List<String> actual = directive.integrations().stream().map(AwsCodegenIntegration::identifier).toList();
        if (!actual.equals(integrationOrder)) {
            // CodegenDirector re-sorts integrations; it must preserve the (order, identifier) order it was given.
            throw new AwsCodegenException("Integration execution order " + actual + " differs from the required "
                                          + "(order, identifier) order " + integrationOrder + ".");
        }
        ContributionLedger ledger = ledger(directive.fileManifest());
        if (ledger != null) {
            ledger.bind(directive.model(), directive.expectService(), directive.symbolProvider());
            ledger.requireNoConflicts();
        }
        // On the final (decorated) symbol provider, so integration renames are checked too.
        List<Diagnostic> collisions = GeneratedNameCollisions.find(
            directive.model(), directive.expectService(), directive.symbolProvider(), directive.settings(),
            ledger == null ? shape -> List.of() : ledger::symbolChangers);
        if (!collisions.isEmpty()) {
            throw new AwsCodegenException(
                "Code generation of service " + directive.expectService().getId() + " would generate colliding Java "
                + "types (" + collisions.size() + " diagnostic(s)); no Generated Service Package artifact was "
                + "produced:", collisions);
        }
        context = new AwsCodegenContext(directive.model(), directive.expectService(), directive.symbolProvider(),
                                        directive.settings(), directive.fileManifest(),
                                        AttributedIntegration.unwrap(directive.integrations()),
                                        modelDigest, modelRevision);
        return context;
    }

    @Override
    public void generateService(GenerateServiceDirective<AwsCodegenContext, AwsCodegenSettings> directive) {
        attributed(directive.context(), directive.shape(), () -> {
            ServiceInfoGenerator.generate(directive.context());
            SchemaGenerator.generate(directive.context(), AwsSymbolProvider.schemaHolder(directive.context().service(),
                                                                                         directive.settings()));
            OperationTableGenerator.generate(directive.context());
            TypeRegistryGenerator.generate(directive.context());
            ServiceMetadataGenerator.generate(directive.context());
            EndpointRulesGenerator.generate(directive.context());
            ClientGenerator.generate(directive.context());
            ModelTypeGenerator.generateServiceException(
                directive.context(), AwsSymbolProvider.serviceException(directive.context().service(),
                                                                        directive.settings()));
        });
    }

    /** Emits the static descriptor of every operation in the service closure, including resource operations. */
    @Override
    public void generateOperation(GenerateOperationDirective<AwsCodegenContext, AwsCodegenSettings> directive) {
        attributed(directive.context(), directive.shape(),
                   () -> OperationDescriptorGenerator.generate(directive.context(), directive.shape()));
    }

    /** Runs {@code emitter} attributed to the core generator rendering {@code shape}. */
    private static void attributed(AwsCodegenContext context, Shape shape, Runnable emitter) {
        ContributionLedger ledger = ledger(context.fileManifest());
        if (ledger == null) {
            emitter.run();
        } else {
            ledger.within(AwsCodegen.GENERATOR_IDENTIFIER, shape.getId(), emitter);
        }
    }

    private static ContributionLedger ledger(FileManifest fileManifest) {
        return fileManifest instanceof StagedFileManifest staged ? staged.ledger() : null;
    }

    /**
     * Flushes every writer into the staged manifest once integrations have run, so that all output (including a
     * duplicate write) is staged before {@code CodegenDirector} returns and publication can begin.
     */
    @Override
    public void customizeAfterIntegrations(CustomizeDirective<AwsCodegenContext, AwsCodegenSettings> directive) {
        directive.context().writerDelegator().flushWriters();
    }

    /** Emits the immutable type and builder of a structure (including operation inputs and outputs). */
    @Override
    public void generateStructure(GenerateStructureDirective<AwsCodegenContext, AwsCodegenSettings> directive) {
        generateModelType(directive.context(), directive.shape());
    }

    /** Emits a modeled error, which extends the service's base exception. */
    @Override
    public void generateError(GenerateErrorDirective<AwsCodegenContext, AwsCodegenSettings> directive) {
        generateModelType(directive.context(), directive.shape());
    }

    @Override
    public void generateUnion(GenerateUnionDirective<AwsCodegenContext, AwsCodegenSettings> directive) {
        generateModelType(directive.context(), directive.shape());
    }

    @Override
    public void generateEnumShape(GenerateEnumDirective<AwsCodegenContext, AwsCodegenSettings> directive) {
        generateEnumType(directive.context(), directive.shape());
    }

    @Override
    public void generateIntEnumShape(GenerateIntEnumDirective<AwsCodegenContext, AwsCodegenSettings> directive) {
        generateEnumType(directive.context(), directive.shape());
    }

    private static void generateModelType(AwsCodegenContext context, Shape shape) {
        attributed(context, shape, () -> ModelTypeGenerator.generate(
            context, AwsSymbolProvider.schemaHolder(context.service(), context.settings()),
            AwsSymbolProvider.serviceException(context.service(), context.settings()), shape));
    }

    private static void generateEnumType(AwsCodegenContext context, Shape shape) {
        attributed(context, shape, () -> ModelTypeGenerator.generateEnum(
            context, AwsSymbolProvider.schemaHolder(context.service(), context.settings()), shape));
    }
}
