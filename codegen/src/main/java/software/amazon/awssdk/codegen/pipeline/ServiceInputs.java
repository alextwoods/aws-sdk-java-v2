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

package software.amazon.awssdk.codegen.pipeline;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * The inputs and output locations of one service for {@link CodegenPipeline}.
 *
 * <p>Exactly one model source is set:
 * <ul>
 *     <li>{@link Builder#c2jModelDirectory(Path)}: a C2J {@code codegen-resources} directory. The pipeline runs all six
 *     stages.</li>
 *     <li>{@link Builder#smithyModelSources(List)}: native Smithy model files or directories. Stages 1 to 3 are skipped
 *     (Requirement 17.4) and stage 4 assembles the files.</li>
 *     <li>{@link Builder#smithyModel(Model, List)}: a model that a caller already assembled, such as a smithy-build
 *     projection, with its validation events. Stages 1 to 3 are skipped and stage 4 checks the events.</li>
 * </ul>
 */
@SdkInternalApi
public final class ServiceInputs {

    /**
     * Where the service model comes from.
     */
    public enum Source {
        /** A C2J {@code codegen-resources} directory. */
        C2J,
        /** Native Smithy model files. */
        SMITHY_FILES,
        /** An already assembled Smithy model. */
        SMITHY_MODEL
    }

    private final String serviceName;
    private final Source source;
    private final Path c2jModelDirectory;
    private final List<Path> smithyModelSources;
    private final Model smithyModel;
    private final List<ValidationEvent> smithyModelEvents;
    private final CustomizationConfig customizationConfig;
    private final Path sourcesDirectory;
    private final Path resourcesDirectory;
    private final Path testsDirectory;
    private final Path buildDirectory;
    private final boolean writeIntermediateModel;
    private final boolean emitValidationReport;
    private final boolean allowSmithyValidationSuppressions;
    private final ClassLoader classLoader;

    private ServiceInputs(Builder builder) {
        this.serviceName = Objects.requireNonNull(builder.serviceName, "serviceName");
        this.c2jModelDirectory = builder.c2jModelDirectory;
        this.smithyModelSources = builder.smithyModelSources == null
                                  ? Collections.emptyList()
                                  : Collections.unmodifiableList(new ArrayList<>(builder.smithyModelSources));
        this.smithyModel = builder.smithyModel;
        this.smithyModelEvents = builder.smithyModelEvents == null
                                 ? Collections.emptyList()
                                 : Collections.unmodifiableList(new ArrayList<>(builder.smithyModelEvents));
        this.source = source(builder);
        this.customizationConfig = builder.customizationConfig == null
                                   ? CustomizationConfig.create()
                                   : builder.customizationConfig;
        this.sourcesDirectory = Objects.requireNonNull(builder.sourcesDirectory, "sourcesDirectory");
        this.resourcesDirectory = Objects.requireNonNull(builder.resourcesDirectory, "resourcesDirectory");
        this.testsDirectory = Objects.requireNonNull(builder.testsDirectory, "testsDirectory");
        this.buildDirectory = Objects.requireNonNull(builder.buildDirectory, "buildDirectory");
        this.writeIntermediateModel = builder.writeIntermediateModel;
        this.emitValidationReport = builder.emitValidationReport;
        this.allowSmithyValidationSuppressions = builder.allowSmithyValidationSuppressions;
        this.classLoader = builder.classLoader == null
                           ? CodegenPipeline.class.getClassLoader()
                           : builder.classLoader;
    }

    private Source source(Builder builder) {
        int count = 0;
        Source result = null;
        if (builder.c2jModelDirectory != null) {
            count++;
            result = Source.C2J;
        }
        if (!smithyModelSources.isEmpty()) {
            count++;
            result = Source.SMITHY_FILES;
        }
        if (builder.smithyModel != null) {
            count++;
            result = Source.SMITHY_MODEL;
        }
        if (count != 1) {
            throw new IllegalArgumentException("Exactly one of c2jModelDirectory, smithyModelSources and smithyModel must "
                                               + "be set for service " + serviceName + ", but " + count + " are set");
        }
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The service name used in messages, in the gap report file name and in the staging directory name. */
    public String serviceName() {
        return serviceName;
    }

    /** Where the service model comes from. */
    public Source source() {
        return source;
    }

    /** The C2J {@code codegen-resources} directory, or null unless {@link #source()} is {@link Source#C2J}. */
    public Path c2jModelDirectory() {
        return c2jModelDirectory;
    }

    /** The native Smithy model files or directories; empty unless {@link #source()} is {@link Source#SMITHY_FILES}. */
    public List<Path> smithyModelSources() {
        return smithyModelSources;
    }

    /** The assembled Smithy model, or null unless {@link #source()} is {@link Source#SMITHY_MODEL}. */
    public Model smithyModel() {
        return smithyModel;
    }

    /** The validation events of {@link #smithyModel()}. */
    public List<ValidationEvent> smithyModelEvents() {
        return smithyModelEvents;
    }

    /** The customization config. Never null. */
    public CustomizationConfig customizationConfig() {
        return customizationConfig;
    }

    /** The final directory of generated sources. */
    public Path sourcesDirectory() {
        return sourcesDirectory;
    }

    /** The final directory of generated resources. */
    public Path resourcesDirectory() {
        return resourcesDirectory;
    }

    /** The final directory of generated test sources. */
    public Path testsDirectory() {
        return testsDirectory;
    }

    /**
     * The build output directory, for example the module's {@code target} directory. The C2J conversion gap report is
     * written to {@code c2j-conversion-gaps/<service>.json} under it, and stage 6 generates into a staging directory
     * under it before moving the output into place.
     */
    public Path buildDirectory() {
        return buildDirectory;
    }

    /** Whether to write {@code <endpointPrefix>-<apiVersion>-intermediate.json} with the generated sources. */
    public boolean writeIntermediateModel() {
        return writeIntermediateModel;
    }

    /** Whether to write the codegen validation report. */
    public boolean emitValidationReport() {
        return emitValidationReport;
    }

    /**
     * Whether the {@code smithyValidationSuppressions} of the customization config may be applied. False by default:
     * a service whose customization config has suppressions then fails in stage 4. Only test modules allow them
     * (see {@code SmithyValidationSuppressions}).
     */
    public boolean allowSmithyValidationSuppressions() {
        return allowSmithyValidationSuppressions;
    }

    /** The class loader whose Smithy trait definitions, validators and model files stage 4 discovers. */
    public ClassLoader classLoader() {
        return classLoader;
    }

    public static final class Builder {
        private String serviceName;
        private Path c2jModelDirectory;
        private List<Path> smithyModelSources;
        private Model smithyModel;
        private List<ValidationEvent> smithyModelEvents;
        private CustomizationConfig customizationConfig;
        private Path sourcesDirectory;
        private Path resourcesDirectory;
        private Path testsDirectory;
        private Path buildDirectory;
        private boolean writeIntermediateModel;
        private boolean emitValidationReport;
        private boolean allowSmithyValidationSuppressions;
        private ClassLoader classLoader;

        private Builder() {
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder c2jModelDirectory(Path c2jModelDirectory) {
            this.c2jModelDirectory = c2jModelDirectory;
            return this;
        }

        public Builder smithyModelSources(List<Path> smithyModelSources) {
            this.smithyModelSources = smithyModelSources;
            return this;
        }

        /**
         * An already assembled model and the events of its validation. Stage 4 fails when any event is an
         * {@code ERROR} or {@code DANGER}.
         */
        public Builder smithyModel(Model smithyModel, List<ValidationEvent> validationEvents) {
            this.smithyModel = smithyModel;
            this.smithyModelEvents = validationEvents;
            return this;
        }

        public Builder customizationConfig(CustomizationConfig customizationConfig) {
            this.customizationConfig = customizationConfig;
            return this;
        }

        public Builder sourcesDirectory(Path sourcesDirectory) {
            this.sourcesDirectory = sourcesDirectory;
            return this;
        }

        public Builder resourcesDirectory(Path resourcesDirectory) {
            this.resourcesDirectory = resourcesDirectory;
            return this;
        }

        public Builder testsDirectory(Path testsDirectory) {
            this.testsDirectory = testsDirectory;
            return this;
        }

        public Builder buildDirectory(Path buildDirectory) {
            this.buildDirectory = buildDirectory;
            return this;
        }

        public Builder writeIntermediateModel(boolean writeIntermediateModel) {
            this.writeIntermediateModel = writeIntermediateModel;
            return this;
        }

        public Builder emitValidationReport(boolean emitValidationReport) {
            this.emitValidationReport = emitValidationReport;
            return this;
        }

        /**
         * Allow the customization config's {@code smithyValidationSuppressions}. Only for test modules.
         */
        public Builder allowSmithyValidationSuppressions(boolean allowSmithyValidationSuppressions) {
            this.allowSmithyValidationSuppressions = allowSmithyValidationSuppressions;
            return this;
        }

        public Builder classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public ServiceInputs build() {
            return new ServiceInputs(this);
        }
    }
}
