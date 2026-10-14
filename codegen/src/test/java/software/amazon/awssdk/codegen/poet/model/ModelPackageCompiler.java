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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.JavaFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.common.EnumClass;
import software.amazon.awssdk.codegen.poet.schema.SchemaCodegen;
import software.amazon.awssdk.codegen.poet.schema.SchemaSerdeModel;
import software.amazon.awssdk.codegen.schema.RuntimeCompiler;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Generates the model package of a schema-enabled IntermediateModel (model classes, enums, base request, response and
 * exception classes, copiers and the schema classes) and compiles it with {@code --release 8}.
 */
public final class ModelPackageCompiler {
    private ModelPackageCompiler() {
    }

    /**
     * @param model the IntermediateModel
     * @param codegen its Schema_Generator
     * @param shapeName an IntermediateModel shape name
     * @return the class spec of the shape's model class or enum, with schema serde members where the model has them
     */
    public static ClassSpec modelClass(IntermediateModel model, SchemaCodegen codegen, String shapeName) {
        ShapeModel shape = model.getShapes().get(shapeName);
        SchemaSerdeModel serde = codegen.serde();
        if (shape.getShapeType() == ShapeType.Enum) {
            return new EnumClass(model.getMetadata().getFullModelPackageName(), shape, serde.enumSchemaReference(shape));
        }
        return new AwsServiceModel(model, shape, serde);
    }

    /**
     * @return the directory of the compiled classes
     */
    public static Path compile(IntermediateModel model, SchemaCodegen codegen, Path dir) {
        List<JavaFile> files = new ArrayList<>();
        for (ShapeModel shape : model.getShapes().values()) {
            if (!shape.getCustomization().isSkipGeneratingModelClass()) {
                files.add(PoetUtils.buildJavaFile(modelClass(model, codegen, shape.getShapeName())));
            }
        }
        files.add(PoetUtils.buildJavaFile(new AwsServiceBaseRequestSpec(model)));
        files.add(PoetUtils.buildJavaFile(new AwsServiceBaseResponseSpec(model)));
        files.add(PoetUtils.buildJavaFile(new ResponseMetadataSpec(model)));
        if (model.getCustomizationConfig().getSdkModeledExceptionBaseClassName() == null) {
            files.add(PoetUtils.buildJavaFile(new BaseExceptionClass(model)));
        }
        for (ClassSpec copier : new ServiceModelCopiers(model).copierSpecs()) {
            files.add(PoetUtils.buildJavaFile(copier));
        }
        for (ClassSpec spec : codegen.classSpecs()) {
            files.add(PoetUtils.buildJavaFile(spec));
        }
        return RuntimeCompiler.compileAll(files, dir, Arrays.asList(
            SdkField.class, AwsRequest.class, SdkHttpResponse.class, MetricPublisher.class, IdentityProvider.class,
            AwsCredentialsProvider.class, Region.class, EndpointProvider.class, AuthScheme.class,
            SdkBuilder.class, Publisher.class, ShapeSerializer.class));
    }
}
