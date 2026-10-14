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

package software.amazon.awssdk.codegen.poet.schema;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.schema.SchemaCodegenException;
import software.amazon.awssdk.codegen.schema.TraitInclusionPolicy;
import software.amazon.awssdk.codegen.schema.TraitInitializers;
import software.amazon.awssdk.codegen.schema.TraitRenderContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The Schema_Generator (design A.17): generates a service's schema classes from its Smithy model.
 *
 * <ul>
 *     <li>{@code <Svc>Schemas}, {@code <Svc>Schemas1}, … in the model package ({@link SchemasGenerator})</li>
 *     <li>{@code <Svc>SchemaIndex} in the model package, and its {@code META-INF/services} entry
 *     ({@link SchemaIndexGenerator})</li>
 *     <li>{@code <Svc>ServiceSchemas} in the client's {@code internal} package ({@link ServiceSchemasGenerator})</li>
 *     <li>{@code <Svc>Operations}, with every operation's {@code ApiOperation} and the Error_Registry, in the model
 *     package ({@link OperationsClassSpec}, design A.18)</li>
 * </ul>
 *
 * <p>Everything is computed and rendered when the instance is created, so an invalid trait inclusion configuration, a
 * conflicting trait initializer, a trait that cannot be rendered or a name conflict fails with a
 * {@link SchemaCodegenException} before any code is written. When created from an IntermediateModel, it also decides
 * which model classes get schema serde members ({@link #serde()}, task 3.7) and generates their
 * {@code <Svc>SharedSerde} ({@link SharedSerdeSpec}); the model class generators read {@link #serde()}.
 */
@SdkInternalApi
public final class SchemaCodegen {
    /**
     * The default static-initializer bytecode budget of one partition class: 48 KB, below the JVM's 64 KB method limit.
     */
    public static final int DEFAULT_PARTITION_BUDGET = 48 * 1024;

    private final SchemasGenerator schemas;
    private final SchemaIndexGenerator index;
    private final ServiceSchemasGenerator serviceSchemas;
    private final OperationsClassSpec operations;
    private final SchemaSerdeModel serde;
    private final List<ClassSpec> classSpecs;

    private SchemaCodegen(Builder builder) {
        Model model = Objects.requireNonNull(builder.model, "model");
        ServiceShape service = Objects.requireNonNull(builder.service, "service");
        TraitInclusionPolicy policy = Objects.requireNonNull(builder.policy, "policy");
        TraitInitializers initializers = Objects.requireNonNull(builder.initializers, "initializers");
        String modelPackage = Objects.requireNonNull(builder.modelPackage, "modelPackage");
        String internalPackage = Objects.requireNonNull(builder.internalPackage, "internalPackage");
        String serviceName = Objects.requireNonNull(builder.serviceName, "serviceName");
        SchemaTraits traits = new SchemaTraits(policy, initializers, TraitRenderContext.defaultContext());
        this.serde = builder.intermediateModel == null
                     ? SchemaSerdeModel.none()
                     : SchemaSerdeModel.create(builder.intermediateModel, modelPackage, serviceName);

        this.schemas = new SchemasGenerator(model, service, traits, modelPackage, serviceName, builder.reservedNames,
                                            builder.partitionBudget, serde);
        if (serde.isEnabled()) {
            serde.attach(schemas);
        }
        this.index = new SchemaIndexGenerator(schemas, service.getId(), modelPackage, serviceName);
        this.serviceSchemas = new ServiceSchemasGenerator(model, service, traits, internalPackage, serviceName);
        this.operations = new OperationsClassSpec(model, service, schemas, serviceSchemas, modelPackage, serviceName,
                                                  builder.errorShapeOwner, serde);

        List<ClassSpec> specs = new ArrayList<>();
        for (TypeSpec partition : schemas.partitionSpecs()) {
            specs.add(new GeneratedClassSpec(ClassName.get(modelPackage, partition.name), partition));
        }
        specs.add(new GeneratedClassSpec(index.className(), index.spec()));
        specs.add(new GeneratedClassSpec(serviceSchemas.className(), serviceSchemas.spec()));
        specs.add(new GeneratedClassSpec(operations.className(), operations.spec()));
        if (serde.isEnabled()) {
            SharedSerdeSpec sharedSerde = new SharedSerdeSpec(serde, builder.intermediateModel);
            specs.add(new GeneratedClassSpec(sharedSerde.className(), sharedSerde.spec()));
        }
        checkNoConflicts(specs, builder.reservedNames);
        this.classSpecs = Collections.unmodifiableList(specs);
    }

    /**
     * @param model an intermediate model
     * @return whether schemas are generated for it: it was built from a Smithy model and does not share another
     *     service's model package
     */
    public static boolean isApplicable(IntermediateModel model) {
        return model.smithyModel() != null && model.serviceId() != null
               && model.getCustomizationConfig().getShareModelConfig() == null;
    }

    /**
     * Prepares schema generation for an intermediate model built from a Smithy model.
     *
     * @param model the intermediate model
     * @param initializers the trait initializer registry
     * @return the prepared generator
     * @throws SchemaCodegenException if the trait inclusion configuration is invalid or generation cannot proceed
     */
    public static SchemaCodegen create(IntermediateModel model, TraitInitializers initializers) {
        if (!isApplicable(model)) {
            throw new IllegalArgumentException("Schemas are generated only for intermediate models built from Smithy");
        }
        Metadata metadata = model.getMetadata();
        Model smithyModel = model.smithyModel();
        return builder().model(smithyModel)
                        .service(smithyModel.expectShape(model.serviceId(), ServiceShape.class))
                        .policy(TraitInclusionPolicy.forService(model))
                        .initializers(initializers)
                        .modelPackage(metadata.getFullModelPackageName())
                        .internalPackage(metadata.getFullInternalPackageName())
                        .serviceName(metadata.getServiceName())
                        .reservedNames(model.getShapes().keySet())
                        .errorShapeOwner(errorShapeOwners(model))
                        .intermediateModel(model)
                        .build();
    }

    /**
     * Maps each error shape to the error shape that owns its generated exception class. The intermediate model
     * generates one exception class per class name; when several error shapes get the same name (CloudWatch
     * {@code LimitExceededException} and {@code LimitExceededFault}), the others are mapped to the one it kept.
     *
     * @param model the intermediate model
     * @return the owner of an error shape's class: itself if the model has an exception class for it, else the error
     *     shape with that class name, else null
     */
    static Function<ShapeId, ShapeId> errorShapeOwners(IntermediateModel model) {
        Set<ShapeId> kept = new HashSet<>();
        Map<String, ShapeId> byClassName = new HashMap<>();
        for (ShapeModel shape : model.getShapes().values()) {
            if (shape.getShapeType() == ShapeType.Exception && shape.shapeId() != null) {
                kept.add(shape.shapeId());
                byClassName.put(shape.getShapeName(), shape.shapeId());
            }
        }
        NamingStrategy naming = model.getNamingStrategy();
        return id -> {
            if (kept.contains(id)) {
                return id;
            }
            return naming == null ? null : byClassName.get(naming.getExceptionName(id.getName()));
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the partition classes, the schema index, the service schemas and the operations, in that order
     */
    public List<ClassSpec> classSpecs() {
        return classSpecs;
    }

    /**
     * @return the field order and partitions
     */
    public SchemaFieldOrder order() {
        return schemas.order();
    }

    /**
     * @return the partition class generator
     */
    public SchemasGenerator schemas() {
        return schemas;
    }

    /**
     * @return the schema index class name, which the {@code META-INF/services} entry lists
     */
    public ClassName schemaIndexClass() {
        return index.className();
    }

    /**
     * @return the service schemas class name
     */
    public ClassName serviceSchemasClass() {
        return serviceSchemas.className();
    }

    /**
     * @return the operations class generator
     */
    public OperationsClassSpec operations() {
        return operations;
    }

    /**
     * @return the model classes with schema serde members; none when the generator was not created from an
     *     IntermediateModel
     */
    public SchemaSerdeModel serde() {
        return serde;
    }

    private static void checkNoConflicts(List<ClassSpec> specs, Set<String> reservedNames) {
        List<String> conflicts = new ArrayList<>();
        for (ClassSpec spec : specs) {
            if (reservedNames.contains(spec.className().simpleName())) {
                conflicts.add(spec.className().simpleName());
            }
        }
        if (!conflicts.isEmpty()) {
            throw new SchemaCodegenException("Generated schema classes " + conflicts + " have the same names as "
                                             + "generated model classes");
        }
    }

    /**
     * Inputs of a {@link SchemaCodegen}.
     */
    public static final class Builder {
        private Model model;
        private ServiceShape service;
        private TraitInclusionPolicy policy;
        private TraitInitializers initializers;
        private String modelPackage;
        private String internalPackage;
        private String serviceName;
        private Set<String> reservedNames = Collections.emptySet();
        private int partitionBudget = DEFAULT_PARTITION_BUDGET;
        private Function<ShapeId, ShapeId> errorShapeOwner = Function.identity();
        private IntermediateModel intermediateModel;

        private Builder() {
        }

        /**
         * @param intermediateModel the IntermediateModel whose model classes get schema serde members (task 3.7); when
         *     absent, schemas reference no generated class
         */
        public Builder intermediateModel(IntermediateModel intermediateModel) {
            this.intermediateModel = intermediateModel;
            return this;
        }

        public Builder model(Model model) {
            this.model = model;
            return this;
        }

        public Builder service(ServiceShape service) {
            this.service = service;
            return this;
        }

        public Builder policy(TraitInclusionPolicy policy) {
            this.policy = policy;
            return this;
        }

        public Builder initializers(TraitInitializers initializers) {
            this.initializers = initializers;
            return this;
        }

        /**
         * @param modelPackage the package of the partition classes and the schema index
         */
        public Builder modelPackage(String modelPackage) {
            this.modelPackage = modelPackage;
            return this;
        }

        /**
         * @param internalPackage the package of the service schemas class
         */
        public Builder internalPackage(String internalPackage) {
            this.internalPackage = internalPackage;
            return this;
        }

        /**
         * @param serviceName the class name prefix, such as {@code Sqs}
         */
        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        /**
         * @param reservedNames simple names of the model package's generated classes, which schema fields and classes
         *     must not take
         */
        public Builder reservedNames(Set<String> reservedNames) {
            this.reservedNames = Collections.unmodifiableSet(new TreeSet<>(reservedNames));
            return this;
        }

        /**
         * @param partitionBudget the static-initializer bytecode budget of one partition class
         */
        public Builder partitionBudget(int partitionBudget) {
            if (partitionBudget <= 0) {
                throw new IllegalArgumentException("partitionBudget must be positive");
            }
            this.partitionBudget = partitionBudget;
            return this;
        }

        /**
         * @param errorShapeOwner maps an error shape to the error shape that owns its generated exception class:
         *     itself (the default for every shape), another error shape whose class it shares, or null if no class is
         *     generated for it. Only owners get an Error_Registry entry.
         */
        public Builder errorShapeOwner(Function<ShapeId, ShapeId> errorShapeOwner) {
            this.errorShapeOwner = Objects.requireNonNull(errorShapeOwner, "errorShapeOwner");
            return this;
        }

        /**
         * @return the prepared generator
         * @throws SchemaCodegenException if generation cannot proceed
         */
        public SchemaCodegen build() {
            return new SchemaCodegen(this);
        }
    }

    private static final class GeneratedClassSpec implements ClassSpec {
        private final ClassName className;
        private final TypeSpec spec;

        private GeneratedClassSpec(ClassName className, TypeSpec spec) {
            this.className = className;
            this.spec = spec;
        }

        @Override
        public TypeSpec poetSpec() {
            return spec;
        }

        @Override
        public ClassName className() {
            return className;
        }
    }
}
