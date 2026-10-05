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
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.MODELED_PROTOCOL;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.MODEL_PROVENANCE;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.OPTIONAL;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.SCHEMA;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.SCHEMA_TYPE_REGISTRY;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.SERVICE_PROTOCOLS;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.SERVICE_SCHEMA_INDEX;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.SHAPE_ID;
import static software.amazon.awssdk.awscodegen.RuntimeSymbols.STRING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Emits the public {@code <root>.<Service>ServiceMetadata} class (phase 6 of generation): immutable advanced metadata
 * of the service for frameworks and tooling, without a model parser (Requirements 4.6, 6.5).
 *
 * <p>It exposes the exact service shape ID, SDK ID, version, protocols and auth schemes as generated constants; the
 * declared protocols with the options of their AWS protocol traits (which are build-time only and so are not in
 * schemas) as the {@code protocol-core} {@code ServiceProtocols} that protocol selection reads at client construction,
 * built on first use by a nested holder so that reading the service identity does not initialize the schemas; the
 * service schema and schema index of the {@code <Service>Schemas} holder; the operations of the
 * {@code <Service>Operations} table; the primary type registry; and the model provenance, which the package-private
 * {@value AwsCodegen#SERVICE_INFO_TYPE} identity type reads. Every accessor returns a static reference or delegates to
 * the class that owns the value, so the metadata never becomes a second copy of the operation table or registry.
 */
final class ServiceMetadataGenerator {
    /**
     * Most declarations the {@code ServiceProtocols} builder chain holds before it is split into methods: well below
     * the depth at which javac overflows its stack and the JVM 64 KB method limit.
     */
    static final int MAX_CHAINED_DECLARATIONS = 256;

    /** Most builder statements per {@code declareN} method of a split declaration. */
    static final int DECLARATIONS_PER_METHOD = 500;


    private ServiceMetadataGenerator() {
    }

    static void generate(AwsCodegenContext context) {
        render(ServiceMetadataPlan.of(context), context.writerDelegator());
    }

    static void render(ServiceMetadataPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        String typeName = plan.metadata().getName();
        writers.useFileWriter(plan.metadata().getDefinitionFile(), plan.metadata().getNamespace(), writer -> {
            writer.reserveTypeNames(plan.metadata(), "Protocols");
            writer.write("/**");
            writer.write(" * Metadata of the {@code $L} service: its identity, protocols and auth schemes, "
                         + "schemas, operations,", plan.service());
            writer.write(" * primary type registry and model provenance.");
            writer.write(" */");
            writer.openBlock("public final class $L {", "}", typeName, () -> {
                writer.write("private static final $T SERVICE_ID = $T.from($S);", SHAPE_ID, SHAPE_ID,
                             plan.service().toString());
                writeIds(writer, "PROTOCOLS", plan.protocols());
                writeIds(writer, "AUTH_SCHEMES", plan.authSchemes());
                writer.write("");
                writer.openBlock("private $L() {", "}", typeName, () -> {
                });
                writer.write("");
                writer.write("/** The exact absolute shape ID of the service. */");
                writer.openBlock("public static $T serviceId() {", "}", SHAPE_ID,
                                 () -> writer.write("return SERVICE_ID;"));
                writer.write("");
                writer.write("/** The {@code aws.api#service} SDK ID of the service, if it has one. */");
                writer.openBlock("public static $T<$T> sdkId() {", "}", OPTIONAL, STRING, () -> {
                    if (plan.sdkId().isEmpty()) {
                        writer.write("return $T.empty();", OPTIONAL);
                    } else {
                        writer.write("return $T.of($S);", OPTIONAL, plan.sdkId());
                    }
                });
                writer.write("");
                writer.write("/** The modeled service version; empty if the service has none. */");
                writer.openBlock("public static $T version() {", "}", STRING,
                                 () -> writer.write("return $S;", plan.version()));
                writer.write("");
                writer.write("/** The shape IDs of the service's protocol traits, sorted. */");
                writer.openBlock("public static $T<$T> protocols() {", "}", LIST, SHAPE_ID,
                                 () -> writer.write("return PROTOCOLS;"));
                writer.write("");
                writer.write("/**");
                writer.write(" * The protocols the service's model declares, with their modeled options and the "
                             + "service's");
                writer.write(" * {@code awsQueryCompatible} and {@code awsQueryError} data, for protocol selection "
                             + "at client construction.");
                writer.write(" */");
                writer.openBlock("public static $T serviceProtocols() {", "}", SERVICE_PROTOCOLS,
                                 () -> writer.write("return Protocols.DECLARED;"));
                writer.write("");
                writer.write("/** The shape IDs of the service's effective auth schemes, in modeled priority "
                             + "order. */");
                writer.openBlock("public static $T<$T> authSchemes() {", "}", LIST, SHAPE_ID,
                                 () -> writer.write("return AUTH_SCHEMES;"));
                writer.write("");
                writer.write("/** The service schema. */");
                writer.openBlock("public static $T serviceSchema() {", "}", SCHEMA,
                                 () -> writer.write("return $T.$L;", plan.schemaHolder(), plan.serviceSchemaField()));
                writer.write("");
                writer.write("/** The exact-ID view of every schema of the service. */");
                writer.openBlock("public static $T schemaIndex() {", "}", SERVICE_SCHEMA_INDEX,
                                 () -> writer.write("return $T.index();", plan.schemaHolder()));
                writer.write("");
                writer.write("/** Every operation of the service, sorted by absolute shape ID. */");
                writer.openBlock("public static $T<$T> operations() {", "}", LIST, AWS_OPERATION_DESCRIPTOR,
                                 () -> writer.write("return $T.operations();", plan.operationTable()));
                writer.write("");
                writer.write("/**");
                writer.write(" * The operation of this service with exactly the absolute shape ID {@code id}, for "
                             + "tooling. Generated");
                writer.write(" * clients reference operations statically and never look them up.");
                writer.write(" */");
                writer.openBlock("public static $T<$T> operation($T id) {", "}", OPTIONAL, AWS_OPERATION_DESCRIPTOR,
                                 SHAPE_ID, () -> writer.write("return $T.operation(id);", plan.operationTable()));
                writer.write("");
                writer.write("/** The service's primary type registry. */");
                writer.openBlock("public static $T typeRegistry() {", "}", SCHEMA_TYPE_REGISTRY,
                                 () -> writer.write("return $T.registry();", plan.typeRegistry()));
                writer.write("");
                writer.write("/** The model provenance published in this service's artifact. */");
                writer.openBlock("public static $T provenance() {", "}", MODEL_PROVENANCE,
                                 () -> writer.write("return $T.provenance();", plan.serviceInfo()));
                writer.write("");
                writeProtocols(writer, plan);
            });
        });
    }

    /** The nested holder of the {@code ServiceProtocols} constant, initialized on first use. */
    private static void writeProtocols(AwsJavaWriter writer, ServiceMetadataPlan plan) {
        writer.write("/** Initialized on first use, so that reading the service identity does not load its schemas. "
                     + "*/");
        int chained = plan.protocolDeclarations().size() + plan.awsQueryErrors().size() + plan.ec2QueryNames().size();
        if (chained > MAX_CHAINED_DECLARATIONS) {
            writePartitionedProtocols(writer, plan);
            return;
        }
        writer.openBlock("private static final class Protocols {", "}", () -> {
            writer.write("static final $T DECLARED = $T.builder($T.$L)", SERVICE_PROTOCOLS, SERVICE_PROTOCOLS,
                         plan.schemaHolder(), plan.serviceSchemaField());
            writer.indent(2);
            for (ServiceMetadataPlan.ProtocolDeclaration protocol : plan.protocolDeclarations()) {
                List<Object> args = new ArrayList<>(List.of(MODELED_PROTOCOL, SHAPE_ID, protocol.id().toString()));
                String http = stringList(protocol.http(), args);
                String eventStreamHttp = stringList(protocol.eventStreamHttp(), args);
                args.add(protocol.noErrorWrapping());
                writer.write(".protocol(new $T($T.from($S), " + http + ", " + eventStreamHttp + ", $L))",
                             args.toArray());
            }
            if (plan.awsQueryCompatible()) {
                writer.write(".awsQueryCompatible(true)");
            }
            for (ServiceMetadataPlan.AwsQueryError error : plan.awsQueryErrors()) {
                writer.write(".awsQueryError($T.from($S), $S, $L)", SHAPE_ID, error.error().toString(), error.code(),
                             error.httpResponseCode());
            }
            if (!plan.queryVersion().isEmpty()) {
                writer.write(".serviceVersion($S)", plan.queryVersion());
            }
            for (ServiceMetadataPlan.Ec2QueryName name : plan.ec2QueryNames()) {
                writer.write(".ec2QueryName($T.from($S), $S)", SHAPE_ID, name.member().toString(), name.name());
            }
            writer.write(".build();");
            writer.dedent(2);
            writer.write("");
            writer.openBlock("private Protocols() {", "}", () -> {
            });
        });
    }

    /**
     * {@link #writeProtocols} for services with more than {@link #MAX_CHAINED_DECLARATIONS} declarations (EC2 declares
     * one {@code ec2QueryName} per member, about 5,900): one fluent chain that long overflows javac's stack and its
     * initializer exceeds the JVM's 64 KB method limit. The same declarations, in the same order, become statements
     * on the builder, split into {@code declareN} methods of at most {@link #DECLARATIONS_PER_METHOD} each.
     */
    private static void writePartitionedProtocols(AwsJavaWriter writer, ServiceMetadataPlan plan) {
        List<Consumer<AwsJavaWriter>> statements = new ArrayList<>();
        for (ServiceMetadataPlan.ProtocolDeclaration protocol : plan.protocolDeclarations()) {
            List<Object> args = new ArrayList<>(List.of(MODELED_PROTOCOL, SHAPE_ID, protocol.id().toString()));
            String http = stringList(protocol.http(), args);
            String eventStreamHttp = stringList(protocol.eventStreamHttp(), args);
            args.add(protocol.noErrorWrapping());
            statements.add(w -> w.write("builder.protocol(new $T($T.from($S), " + http + ", " + eventStreamHttp
                                        + ", $L));", args.toArray()));
        }
        if (plan.awsQueryCompatible()) {
            statements.add(w -> w.write("builder.awsQueryCompatible(true);"));
        }
        for (ServiceMetadataPlan.AwsQueryError error : plan.awsQueryErrors()) {
            statements.add(w -> w.write("builder.awsQueryError($T.from($S), $S, $L);", SHAPE_ID,
                                        error.error().toString(), error.code(), error.httpResponseCode()));
        }
        if (!plan.queryVersion().isEmpty()) {
            statements.add(w -> w.write("builder.serviceVersion($S);", plan.queryVersion()));
        }
        for (ServiceMetadataPlan.Ec2QueryName name : plan.ec2QueryNames()) {
            statements.add(w -> w.write("builder.ec2QueryName($T.from($S), $S);", SHAPE_ID, name.member().toString(),
                                        name.name()));
        }
        int methods = (statements.size() + DECLARATIONS_PER_METHOD - 1) / DECLARATIONS_PER_METHOD;
        writer.openBlock("private static final class Protocols {", "}", () -> {
            writer.write("static final $T DECLARED = declared();", SERVICE_PROTOCOLS);
            writer.write("");
            writer.openBlock("private Protocols() {", "}", () -> {
            });
            writer.write("");
            writer.openBlock("private static $T declared() {", "}", SERVICE_PROTOCOLS, () -> {
                writer.write("$T.Builder builder = $T.builder($T.$L);", SERVICE_PROTOCOLS, SERVICE_PROTOCOLS,
                             plan.schemaHolder(), plan.serviceSchemaField());
                for (int i = 1; i <= methods; i++) {
                    writer.write("declare$L(builder);", i);
                }
                writer.write("return builder.build();");
            });
            for (int i = 0; i < methods; i++) {
                List<Consumer<AwsJavaWriter>> part =
                    statements.subList(i * DECLARATIONS_PER_METHOD,
                                       Math.min(statements.size(), (i + 1) * DECLARATIONS_PER_METHOD));
                writer.write("");
                writer.openBlock("private static void declare$L($T.Builder builder) {", "}", i + 1,
                                 SERVICE_PROTOCOLS, () -> part.forEach(statement -> statement.accept(writer)));
            }
        });
    }

    /** The writer format of a {@code List.of(...)} of {@code values}, whose arguments are appended to {@code args}. */
    private static String stringList(List<String> values, List<Object> args) {
        args.add(LIST);
        args.addAll(values);
        return "$T.of(" + String.join(", ", Collections.nCopies(values.size(), "$S")) + ")";
    }

    /** A {@code private static final List<ShapeId>} constant holding {@code ids} in order. */
    private static void writeIds(AwsJavaWriter writer, String name, List<ShapeId> ids) {
        if (ids.isEmpty()) {
            writer.write("private static final $T<$T> $L = $T.of();", LIST, SHAPE_ID, name, LIST);
            return;
        }
        writer.write("private static final $T<$T> $L = $T.of(", LIST, SHAPE_ID, name, LIST);
        writer.indent(2);
        for (int i = 0; i < ids.size(); i++) {
            writer.write("$T.from($S)$L", SHAPE_ID, ids.get(i).toString(), i + 1 < ids.size() ? "," : ");");
        }
        writer.dedent(2);
    }
}
