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
import java.util.Objects;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Execution data of one AWS directed generation:
 * {@code Model + ServiceShape + SymbolProvider + Settings + FileManifest + ordered AwsCodegenIntegration list}.
 *
 * <p>The {@link #model()} is the only semantic authority. The context adds no derived whole-service structure; indexes
 * and render plans are obtained from the model (for example {@code TopDownIndex.of(context.model())}) where they are
 * needed and are scoped to one emitted file or this one invocation.
 *
 * <p>The file manifest stages files in memory. Nothing reaches the published output directory until every generation
 * phase has succeeded.
 */
public final class AwsCodegenContext implements CodegenContext<AwsCodegenSettings, AwsJavaWriter, AwsCodegenIntegration> {

    private final Model model;
    private final ServiceShape service;
    private final SymbolProvider symbolProvider;
    private final AwsCodegenSettings settings;
    private final FileManifest fileManifest;
    private final List<AwsCodegenIntegration> integrations;
    private final String modelDigest;
    private final ModelRevision modelRevision;
    private final WriterDelegator<AwsJavaWriter> writerDelegator;

    /** An unpinned context, whose model revision is {@code modelDigest}. */
    AwsCodegenContext(Model model, ServiceShape service, SymbolProvider symbolProvider, AwsCodegenSettings settings,
                      FileManifest fileManifest, List<AwsCodegenIntegration> integrations, String modelDigest) {
        this(model, service, symbolProvider, settings, fileManifest, integrations, modelDigest,
             ModelRevision.of(null, modelDigest));
    }

    AwsCodegenContext(Model model, ServiceShape service, SymbolProvider symbolProvider, AwsCodegenSettings settings,
                      FileManifest fileManifest, List<AwsCodegenIntegration> integrations, String modelDigest,
                      ModelRevision modelRevision) {
        this.model = Objects.requireNonNull(model, "model");
        this.service = Objects.requireNonNull(service, "service");
        this.symbolProvider = Objects.requireNonNull(symbolProvider, "symbolProvider");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.fileManifest = Objects.requireNonNull(fileManifest, "fileManifest");
        this.integrations = List.copyOf(integrations);
        this.modelDigest = Objects.requireNonNull(modelDigest, "modelDigest");
        this.modelRevision = Objects.requireNonNull(modelRevision, "modelRevision");
        // Attributes every writer checkout to the contributor in scope when the manifest has a ContributionLedger.
        this.writerDelegator = AttributedWriterDelegator.create(
            fileManifest, symbolProvider,
            GeneratedNameCollisions.typeNamesByPackage(model, service, symbolProvider, settings));
    }

    @Override
    public Model model() {
        return model;
    }

    /** The service shape being generated, taken from {@link #model()}. */
    public ServiceShape service() {
        return service;
    }

    @Override
    public SymbolProvider symbolProvider() {
        return symbolProvider;
    }

    @Override
    public AwsCodegenSettings settings() {
        return settings;
    }

    @Override
    public FileManifest fileManifest() {
        return fileManifest;
    }

    @Override
    public WriterDelegator<AwsJavaWriter> writerDelegator() {
        return writerDelegator;
    }

    /** Integrations in execution order: ascending {@code (order(), identifier())}. */
    @Override
    public List<AwsCodegenIntegration> integrations() {
        return integrations;
    }

    /**
     * Location-independent digest of the canonical model input files, {@code sha256:<hex>}. It identifies the inputs;
     * it carries no model semantics.
     */
    public String modelDigest() {
        return modelDigest;
    }

    /**
     * The exact, untruncated immutable revision of the canonical model source recorded in the model provenance: a full
     * Git commit object ID, or {@link #modelDigest()} for unpinned generation. Like the digest, it identifies the
     * inputs and carries no model semantics.
     */
    public String modelRevision() {
        return modelRevision.value();
    }

    /** The revision together with its kind. */
    ModelRevision revision() {
        return modelRevision;
    }
}
