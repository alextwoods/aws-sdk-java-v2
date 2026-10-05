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
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.LIST;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.OPTIONAL;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.SHAPE_ID;

import java.util.List;
import software.amazon.smithy.codegen.core.WriterDelegator;

/**
 * Emits the public {@code <Service>Operations} static operation table (Requirements 3.8, 3.9, 10.12).
 *
 * <p>The table has one {@code public static final} constant per operation in the service closure, initialized with
 * {@code <Name>Operation.INSTANCE}. Generated clients and the pipeline reach descriptors only through these
 * constants. The table also offers {@code operations()} and {@code operation(ShapeId)} for tooling; the lookup is a
 * generated {@code switch} over exact shape IDs that returns the same constants, so it involves no reflection, no
 * class-name lookup and no runtime map built from names.
 */
final class OperationTableGenerator {

    private OperationTableGenerator() {
    }

    static void generate(AwsCodegenContext context) {
        render(OperationTablePlan.of(context), context.writerDelegator());
    }

    static void render(OperationTablePlan plan, WriterDelegator<AwsJavaWriter> writers) {
        String typeName = plan.table().getName();
        writers.useFileWriter(plan.table().getDefinitionFile(), plan.table().getNamespace(), writer -> {
            writer.write("/**");
            writer.write(" * Static operation table of the {@code $L} service.", plan.service());
            writer.write(" *");
            writer.write(" * <p>Each constant is a direct static reference to the descriptor of one operation in the "
                         + "service");
            writer.write(" * closure. Operations are resolved only through these references.");
            writer.write(" */");
            writer.openBlock("public final class $L {", "}", typeName, () -> {
                for (OperationTablePlan.Entry entry : plan.entries()) {
                    writer.write("/** Descriptor of {@code $L}. */", entry.operation());
                    writer.write("public static final $T $L = $T.INSTANCE;", AWS_OPERATION_DESCRIPTOR,
                                 entry.constant(), entry.descriptor());
                    writer.write("");
                }
                writeOperationList(writer, plan.entries());
                writer.write("");
                writer.openBlock("private $L() {", "}", typeName, () -> {
                });
                writer.write("");
                writer.write("/** Every operation of the service, sorted by absolute shape ID. */");
                writer.openBlock("public static $T<$T> operations() {", "}", LIST, AWS_OPERATION_DESCRIPTOR,
                                 () -> writer.write("return operations;"));
                writer.write("");
                writer.write("/** The operation of this service with exactly the absolute shape ID {@code id}. */");
                writer.openBlock("public static $T<$T> operation($T id) {", "}", OPTIONAL, AWS_OPERATION_DESCRIPTOR,
                                 SHAPE_ID, () -> {
                        writer.openBlock("return switch (id.toString()) {", "};", () -> {
                            for (OperationTablePlan.Entry entry : plan.entries()) {
                                writer.write("case $S -> $T.of($L);", entry.operation().toString(), OPTIONAL,
                                             entry.constant());
                            }
                            writer.write("default -> $T.empty();", OPTIONAL);
                        });
                    });
            });
        });
    }

    /** The list field is lower case so that it can never collide with an upper-case operation constant. */
    private static void writeOperationList(AwsJavaWriter writer, List<OperationTablePlan.Entry> entries) {
        if (entries.isEmpty()) {
            writer.write("private static final $T<$T> operations = $T.of();", LIST, AWS_OPERATION_DESCRIPTOR, LIST);
            return;
        }
        writer.write("private static final $T<$T> operations = $T.of(", LIST, AWS_OPERATION_DESCRIPTOR, LIST);
        writer.indent(2);
        for (int i = 0; i < entries.size(); i++) {
            writer.write("$L$L", entries.get(i).constant(), i + 1 < entries.size() ? "," : ");");
        }
        writer.dedent(2);
    }
}
