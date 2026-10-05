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

import static software.amazon.awssdk.awscodegen.RuntimeSymbols.AWS_OPERATION_DESCRIPTOR;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.OPERATION_SCHEMAS;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.SHAPE_ID;

import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Emits the package-private static descriptor class of one operation:
 *
 * <pre>{@code
 * final class GetWidgetOperation {
 *     static final AwsOperationDescriptor INSTANCE = AwsOperationDescriptor.builder()
 *             .id(ShapeId.from("example#GetWidget"))
 *             ...
 *             .schemas(OperationSchemas.builder(WidgetsSchemas.GET_WIDGET)
 *                     .input(WidgetsSchemas.GET_WIDGET_INPUT)
 *                     ...
 *                     .build())
 *             .build();
 * }
 * }</pre>
 *
 * <p>The only other generated class the initializer references is the service's schema holder, which references no
 * descriptor or operation table, so descriptor, table and schema initialization cannot form a class-initialization
 * cycle.
 */
final class OperationDescriptorGenerator {

    private OperationDescriptorGenerator() {
    }

    static void generate(AwsCodegenContext context, OperationShape operation) {
        render(OperationDescriptorPlan.of(context, operation), context.writerDelegator());
    }

    static void render(OperationDescriptorPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        String typeName = plan.descriptor().getName();
        writers.useFileWriter(plan.descriptor().getDefinitionFile(), plan.descriptor().getNamespace(), writer -> {
            writer.write("/**");
            writer.write(" * Static descriptor of the {@code $L} operation.", plan.operation());
            writer.write(" *");
            writer.write(" * <p>It is reached only through a direct static reference from the service's operation table.");
            writer.write(" */");
            writer.openBlock("final class $L {", "}", typeName, () -> {
                writer.write("static final $T INSTANCE = $T.builder()", AWS_OPERATION_DESCRIPTOR,
                             AWS_OPERATION_DESCRIPTOR);
                writer.indent(2);
                shapeId(writer, "id", plan.operation());
                shapeId(writer, "service", plan.service());
                shapeId(writer, "input", plan.input());
                shapeId(writer, "output", plan.output());
                plan.errors().forEach(error -> shapeId(writer, "errors", error));
                writer.write(".schemas($T.builder($T.$L)", OPERATION_SCHEMAS, plan.operationSchema().owner(),
                             plan.operationSchema().field());
                writer.indent(2);
                schema(writer, "input", plan.inputSchema());
                schema(writer, "output", plan.outputSchema());
                plan.errorSchemas().forEach(error -> schema(writer, "errors", error));
                if (!plan.inputStreamMember().isEmpty()) {
                    writer.write(".inputStreamMember($S)", plan.inputStreamMember());
                }
                if (!plan.outputStreamMember().isEmpty()) {
                    writer.write(".outputStreamMember($S)", plan.outputStreamMember());
                }
                writer.write(".build())");
                writer.dedent(2);
                writer.write(".build();");
                writer.dedent(2);
                writer.write("");
                writer.openBlock("private $L() {", "}", typeName, () -> {
                });
            });
        });
    }

    private static void shapeId(AwsJavaWriter writer, String method, ShapeId id) {
        writer.write(".$L($T.from($S))", method, SHAPE_ID, id.toString());
    }

    private static void schema(AwsJavaWriter writer, String method, OperationDescriptorPlan.SchemaLink schema) {
        writer.write(".$L($T.$L)", method, schema.owner(), schema.field());
    }
}
