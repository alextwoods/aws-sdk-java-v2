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

package software.amazon.awssdk.awscodegen.schema;

import static software.amazon.awssdk.awscodegen.schema.SchemaRuntimeSymbols.SERVICE_SCHEMA_INDEX;

import java.util.List;
import software.amazon.awssdk.awscodegen.AwsCodegenContext;
import software.amazon.awssdk.awscodegen.AwsJavaWriter;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.WriterDelegator;

/**
 * Emits the schemas of one service (phase 4 of generation): the {@code <root>.model.internal.<Service>Schemas} holder
 * with the service schema, one operation schema per operation and the one exported schema of every public model type,
 * the recursive and shared schemas they are built from, and the lazily built {@code ServiceSchemaIndex} that exposes
 * enum member schemas, modeled member order and unknown traits by exact ID. See {@link SchemaPlanner} for the rules.
 *
 * <p>The generated code only constructs Smithy Java schemas, Smithy traits and nodes; it loads no model, parses
 * nothing and uses no reflection. Model types (task 3.2) re-export their exported schema as {@code SCHEMA}; operation
 * descriptors link operation schemas through {@link SchemaSymbols}.
 */
public final class SchemaGenerator {

    private SchemaGenerator() {
    }

    /** Emits the schema holder, and partitions if needed, of the context's service into {@code holder}. */
    public static void generate(AwsCodegenContext context, Symbol holder) {
        List<SchemaFilePlan> plans = new SchemaPlanner(context.model(), context.service(), holder,
                                                       SchemaPlanner.DEFAULT_PARTITION_WEIGHT).plan();
        plans.forEach(plan -> render(plan, holder, context.writerDelegator()));
    }

    static void render(SchemaFilePlan plan, Symbol holder, WriterDelegator<AwsJavaWriter> writers) {
        String typeName = plan.type().getName();
        writers.useFileWriter(plan.type().getDefinitionFile(), plan.type().getNamespace(), writer -> {
            writer.write("/**");
            if (plan.holder()) {
                writer.write(" * Schemas of the {@code $L} service.", plan.service());
                writer.write(" *");
                writer.write(" * <p>Each exported constant is the one schema of the service, an operation or a "
                             + "public model type. Recursive");
                writer.write(" * shapes are built in two phases, so recursive links are the exported objects. "
                             + "{@link #index()} adds");
                writer.write(" * exact-ID lookup, enum member schemas, modeled member order and unknown traits.");
            } else {
                writer.write(" * Schemas of the {@code $L} service, one partition of {@link $L}.", plan.service(),
                             holder.getName());
            }
            writer.write(" */");
            writer.openBlock((plan.holder() ? "public " : "") + "final class $L {", "}", typeName, () -> {
                for (SchemaFilePlan.Code field : plan.fields()) {
                    write(writer, field);
                    writer.write("");
                }
                writer.openBlock("private $L() {", "}", typeName, () -> {
                });
                writer.write("");
                if (plan.holder()) {
                    renderIndex(writer, plan);
                } else {
                    writer.openBlock("static void register($T.Builder builder) {", "}", SERVICE_SCHEMA_INDEX,
                                     () -> plan.registrations().forEach(r -> write(writer, r)));
                }
            });
        });
    }

    private static void renderIndex(AwsJavaWriter writer, SchemaFilePlan plan) {
        writer.write("/** The exact-ID view of this service's schemas, built on first use. */");
        writer.openBlock("public static $T index() {", "}", SERVICE_SCHEMA_INDEX,
                         () -> writer.write("return Index.INDEX;"));
        writer.write("");
        writer.write("/** Initialization-on-demand holder of the index. */");
        writer.openBlock("private static final class Index {", "}", () -> {
            writer.write("static final $T INDEX = create();", SERVICE_SCHEMA_INDEX);
            writer.write("");
            writer.openBlock("private Index() {", "}", () -> {
            });
            writer.write("");
            writer.openBlock("private static $T create() {", "}", SERVICE_SCHEMA_INDEX, () -> {
                writer.write("$T.Builder builder = $T.builder($L);", SERVICE_SCHEMA_INDEX, SERVICE_SCHEMA_INDEX,
                             plan.serviceField());
                plan.registrations().forEach(r -> write(writer, r));
                writer.write("return builder.build();");
            });
        });
    }

    private static void write(AwsJavaWriter writer, SchemaFilePlan.Code code) {
        writer.write(code.template(), code.types().toArray());
    }
}
