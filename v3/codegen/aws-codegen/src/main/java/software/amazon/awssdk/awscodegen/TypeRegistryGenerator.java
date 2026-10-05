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

import static software.amazon.awssdk.awscodegen.RuntimeSymbols.LIST;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.REGISTERED_TYPE;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.SCHEMA_TYPE_REGISTRY;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.SHAPE_ID;

import java.util.List;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.WriterDelegator;

/**
 * Emits the service's one primary type registry (Requirements 6.5-6.7, 6.9): the public
 * {@code <root>.model.internal.<Service>TypeRegistry} holder and, for services with more than
 * {@link TypeRegistryPlan#DEFAULT_PARTITION_SIZE} registered types, its package-private partitions.
 *
 * <p>The holder builds a {@code SchemaTypeRegistry} on first use. Each partition declares its exact shape IDs and
 * supplies, on the first lookup of one of them, one {@code RegisteredType} per type: the exported schema constant of
 * the service's schema holder (the same object as the type's {@code SCHEMA}), the class literal and
 * {@code Type::builder}. The registry is a separate class from the {@code <Service>Operations} table: registry
 * lookups by shape ID resolve modeled types, never operations. No generated registry code loads classes by name, uses
 * reflection or reads a model.
 */
final class TypeRegistryGenerator {

    /** The registry's initialization-on-demand holder class, nested in the public registry type. */
    private static final String HOLDER = "Holder";

    private TypeRegistryGenerator() {
    }

    static void generate(AwsCodegenContext context) {
        render(TypeRegistryPlan.of(context, TypeRegistryPlan.DEFAULT_PARTITION_SIZE), context.writerDelegator());
    }

    static void render(List<TypeRegistryPlan> plans, WriterDelegator<AwsJavaWriter> writers) {
        plans.forEach(plan -> render(plan, writers));
    }

    private static void render(TypeRegistryPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        String typeName = plan.type().getName();
        writers.useFileWriter(plan.type().getDefinitionFile(), plan.type().getNamespace(), writer -> {
            // The nested holder class is written by simple name: a modeled type named Holder must not shadow it.
            writer.reserveTypeNames(plan.type(), plan.holder() ? new String[] {HOLDER} : new String[0]);
            writer.write("/**");
            if (plan.holder()) {
                writer.write(" * Primary type registry of the {@code $L} service.", plan.service());
                writer.write(" *");
                writer.write(" * <p>Maps the exact shape ID of every structure (including requests, responses and "
                             + "errors) and union of the");
                writer.write(" * service to its schema, Java class and builder factory. The registry is built on "
                             + "first use, and each");
                writer.write(" * partition's entries are created on the first lookup of one of its types.");
            } else {
                writer.write(" * Types of the {@code $L} service, one partition of its primary type registry.",
                             plan.service());
            }
            writer.write(" */");
            writer.openBlock((plan.holder() ? "public " : "") + "final class $L {", "}", typeName, () -> {
                writer.openBlock("private $L() {", "}", typeName, () -> {
                });
                if (plan.holder()) {
                    writer.write("");
                    renderHolder(writer, plan);
                }
                if (!plan.holder() || plan.partitions().isEmpty()) {
                    writer.write("");
                    renderEntries(writer, plan, plan.holder() ? "private " : "");
                }
            });
        });
    }

    private static void renderHolder(AwsJavaWriter writer, TypeRegistryPlan plan) {
        writer.write("/** The service's primary type registry, built on first use. */");
        writer.openBlock("public static $T registry() {", "}", SCHEMA_TYPE_REGISTRY,
                         () -> writer.write("return Holder.REGISTRY;"));
        writer.write("");
        writer.write("/** Initialization-on-demand holder of the registry. */");
        writer.openBlock("private static final class Holder {", "}", () -> {
            writer.write("static final $T REGISTRY = $T.builder()", SCHEMA_TYPE_REGISTRY, SCHEMA_TYPE_REGISTRY);
            writer.indent(2);
            List<Symbol> partitions = plan.partitions().isEmpty() ? List.of(plan.type()) : plan.partitions();
            for (Symbol partition : partitions) {
                writer.write(".partition($T.shapeIds(), $T::entries)", partition, partition);
            }
            writer.write(".build();");
            writer.dedent(2);
            writer.write("");
            writer.openBlock("private Holder() {", "}", () -> {
            });
        });
    }

    private static void renderEntries(AwsJavaWriter writer, TypeRegistryPlan plan, String modifiers) {
        List<TypeRegistryPlan.Entry> entries = plan.entries();
        writer.write("/** The exact shape IDs of the types this class registers, sorted. */");
        writer.openBlock(modifiers + "static $T<$T> shapeIds() {", "}", LIST, SHAPE_ID, () -> {
            if (entries.isEmpty()) {
                writer.write("return $T.of();", LIST);
                return;
            }
            writer.write("return $T.of(", LIST);
            writer.indent(2);
            for (int i = 0; i < entries.size(); i++) {
                writer.write("$T.from($S)$L", SHAPE_ID, entries.get(i).shape().toString(), separator(i, entries));
            }
            writer.dedent(2);
        });
        writer.write("");
        writer.write("/** One registry entry per type, in {@link #shapeIds()} order. */");
        writer.openBlock(modifiers + "static $T<$T<?>> entries() {", "}", LIST, REGISTERED_TYPE, () -> {
            if (entries.isEmpty()) {
                writer.write("return $T.of();", LIST);
                return;
            }
            writer.write("return $T.<$T<?>>of(", LIST, REGISTERED_TYPE);
            writer.indent(2);
            for (int i = 0; i < entries.size(); i++) {
                TypeRegistryPlan.Entry entry = entries.get(i);
                writer.write("$T.of($T.$L, $T.class, $T::builder)$L", REGISTERED_TYPE, entry.schemaOwner(),
                             entry.schemaField(), entry.javaType(), entry.javaType(), separator(i, entries));
            }
            writer.dedent(2);
        });
    }

    private static String separator(int index, List<?> elements) {
        return index + 1 < elements.size() ? "," : ");";
    }
}
