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
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

/**
 * Emits the endpoint surface of a service whose canonical model has a {@code smithy.rules#endpointRuleSet} trait
 * (Requirement 13.5), in the service's {@code endpoints} packages, as V2 does:
 * <ul>
 *   <li>{@code <Service>EndpointParams} ({@link EndpointParamsPlan}): the typed rule-set parameters;</li>
 *   <li>{@code <Service>EndpointProvider} ({@link EndpointProviderPlan}): resolution from typed parameters, with the
 *       rule-set-backed {@code defaultProvider()};</li>
 *   <li>{@code internal.<Service>EndpointRules} ({@link EndpointRulesPlan}): the canonical rule set, parsed once at run
 *       time by the {@code endpoints} runtime, and the generated parameter binding of every operation: its static,
 *       context and operation-context parameters, host prefix and endpoint discovery facts. The service's client
 *       context parameters and discovery operation are recorded too.</li>
 * </ul>
 * The rule set is carried as its exact canonical JSON text (not a Smithy model), so resolution is the canonical
 * rules' result for every regional, partition, FIPS, dual-stack, account, host-prefix, discovery and endpoint-auth
 * dimension. Services without a rule set get none of these types.
 */
final class EndpointRulesGenerator {
    private static final String RUNTIME = "software.amazon.awssdk.endpoints";
    private static final Symbol ENDPOINT = type(RUNTIME, "Endpoint");
    private static final Symbol ENDPOINT_PARAMETERS = type(RUNTIME, "EndpointParameters");
    private static final Symbol ENDPOINT_RESOLUTION_EXCEPTION = type(RUNTIME, "EndpointResolutionException");
    private static final Symbol ENDPOINT_RULE_SET = type(RUNTIME + ".rules", "EndpointRuleSet");
    private static final Symbol ENDPOINT_BINDING = type(RUNTIME + ".binding", "EndpointBinding");
    private static final Symbol REGION = type("software.amazon.awssdk.regions", "Region");
    private static final Symbol STRING = type("java.lang", "String");
    private static final Symbol BOOLEAN = type("java.lang", "Boolean");
    private static final Symbol OBJECT = type("java.lang", "Object");
    private static final Symbol OVERRIDE = type("java.lang", "Override");
    private static final Symbol LIST = type("java.util", "List");
    private static final Symbol MAP = type("java.util", "Map");
    private static final Symbol LINKED_HASH_MAP = type("java.util", "LinkedHashMap");
    private static final Symbol COLLECTIONS = type("java.util", "Collections");
    private static final Symbol OBJECTS = type("java.util", "Objects");
    private static final Symbol OPTIONAL = type("java.util", "Optional");
    private static final Symbol COMPLETABLE_FUTURE = type("java.util.concurrent", "CompletableFuture");
    private static final Symbol CONSUMER = type("java.util.function", "Consumer");
    private static final Symbol SHAPE_ID = RuntimeSymbols.SHAPE_ID;

    private EndpointRulesGenerator() {
    }

    /** Emits the endpoint types of the context's service, if it has an endpoint rule set. */
    static void generate(AwsCodegenContext context) {
        context.service().getTrait(EndpointRuleSetTrait.class).ifPresent(trait -> {
            WriterDelegator<AwsJavaWriter> writers = context.writerDelegator();
            renderParams(EndpointParamsPlan.of(context, trait), writers);
            renderProvider(EndpointProviderPlan.of(context), writers);
            renderRules(EndpointRulesPlan.of(context, trait), writers);
        });
    }

    /** {@code <root>.endpoints.<Service>EndpointParams}. */
    static Symbol paramsSymbol(AwsCodegenContext context) {
        return generatedType(context.settings().packageName() + ".endpoints", serviceName(context) + "EndpointParams");
    }

    /** {@code <root>.endpoints.<Service>EndpointProvider}. */
    static Symbol providerSymbol(AwsCodegenContext context) {
        return generatedType(context.settings().packageName() + ".endpoints",
                             serviceName(context) + "EndpointProvider");
    }

    /** {@code <root>.endpoints.internal.<Service>EndpointRules}. */
    static Symbol rulesSymbol(AwsCodegenContext context) {
        return generatedType(context.settings().packageName() + ".endpoints.internal",
                             serviceName(context) + "EndpointRules");
    }

    private static String serviceName(AwsCodegenContext context) {
        return AwsSymbolProvider.serviceTypeName(context.service());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // <Service>EndpointParams

    static void renderParams(EndpointParamsPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        Symbol type = plan.params();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type, "Builder");
            writer.write("/**");
            writer.write(" * The parameters of the endpoint rule set of service {@code $L}. Every parameter is "
                         + "optional: an unset", plan.service());
            writer.write(" * parameter takes its rule-set default, if any. Immutable.");
            writer.write(" */");
            writer.openBlock("public final class $L {", "}", type.getName(), () -> {
                for (EndpointParamsPlan.Parameter parameter : plan.parameters()) {
                    writer.write("private final $L $L;", javaType(writer, parameter), parameter.javaName());
                }
                writer.write("");
                writer.openBlock("private $L(Builder builder) {", "}", type.getName(), () -> {
                    for (EndpointParamsPlan.Parameter parameter : plan.parameters()) {
                        writer.write("this.$L = builder.$L;", parameter.javaName(), parameter.javaName());
                    }
                });
                writer.write("");
                writer.write("/** A builder with no parameter set. */");
                writer.openBlock("public static Builder builder() {", "}", () -> writer.write("return new Builder();"));
                writer.write("");
                writer.write("/** A builder with this object's parameters set. */");
                writer.openBlock("public Builder toBuilder() {", "}", () -> {
                    writer.write("Builder builder = new Builder();");
                    for (EndpointParamsPlan.Parameter parameter : plan.parameters()) {
                        writer.write("builder.$L = $L;", parameter.javaName(), parameter.javaName());
                    }
                    writer.write("return builder;");
                });
                for (EndpointParamsPlan.Parameter parameter : plan.parameters()) {
                    writer.write("");
                    writeParameterDoc(writer, parameter, "The value of");
                    writer.openBlock("public $L $L() {", "}", javaType(writer, parameter), parameter.javaName(),
                                     () -> writer.write("return $L;", parameter.javaName()));
                }
                writer.write("");
                writer.write("/** The set parameters by rule-set parameter name, for rule evaluation. */");
                writer.openBlock("public $T toEndpointParameters() {", "}", ENDPOINT_PARAMETERS, () -> {
                    writer.write("$T.Builder parameters = $T.builder();", ENDPOINT_PARAMETERS, ENDPOINT_PARAMETERS);
                    for (EndpointParamsPlan.Parameter parameter : plan.parameters()) {
                        String value = parameter.region()
                                       ? parameter.javaName() + " == null ? null : " + parameter.javaName() + ".id()"
                                       : parameter.javaName();
                        writer.write("parameters.put($S, $L);", parameter.name(), value);
                    }
                    writer.write("return parameters.build();");
                });
                writer.write("");
                writer.write("@$T", OVERRIDE);
                writer.openBlock("public boolean equals($T o) {", "}", OBJECT, () -> {
                    writer.openBlock("if (!(o instanceof $L that)) {", "}", type.getName(),
                                     () -> writer.write("return false;"));
                    for (EndpointParamsPlan.Parameter parameter : plan.parameters()) {
                        writer.openBlock("if (!$T.equals($L, that.$L)) {", "}", OBJECTS, parameter.javaName(),
                                         parameter.javaName(), () -> writer.write("return false;"));
                    }
                    writer.write("return true;");
                });
                writer.write("");
                writer.write("@$T", OVERRIDE);
                writer.openBlock("public int hashCode() {", "}", () -> {
                    writer.write("int hashCode = 1;");
                    for (EndpointParamsPlan.Parameter parameter : plan.parameters()) {
                        writer.write("hashCode = 31 * hashCode + $T.hashCode($L);", OBJECTS, parameter.javaName());
                    }
                    writer.write("return hashCode;");
                });
                writer.write("");
                writer.write("@$T", OVERRIDE);
                writer.openBlock("public $T toString() {", "}", STRING,
                                 () -> writer.write("return $S + toEndpointParameters().asMap();", type.getName()));
                writer.write("");
                writer.write("/** Builder of {@link $L}. Not thread-safe. */", type.getName());
                writer.openBlock("public static final class Builder {", "}", () -> {
                    for (EndpointParamsPlan.Parameter parameter : plan.parameters()) {
                        writer.write("private $L $L;", javaType(writer, parameter), parameter.javaName());
                    }
                    writer.write("");
                    writer.openBlock("private Builder() {", "}", () -> {
                    });
                    for (EndpointParamsPlan.Parameter parameter : plan.parameters()) {
                        writer.write("");
                        writeParameterDoc(writer, parameter, "Sets");
                        writer.openBlock("public Builder $L($L $L) {", "}", parameter.javaName(),
                                         javaType(writer, parameter), parameter.javaName(), () -> {
                                if (parameter.type().equals("STRING_ARRAY")) {
                                    writer.write("this.$L = $L == null ? null : $T.copyOf($L);", parameter.javaName(),
                                                 parameter.javaName(), LIST, parameter.javaName());
                                } else {
                                    writer.write("this.$L = $L;", parameter.javaName(), parameter.javaName());
                                }
                                writer.write("return this;");
                            });
                    }
                    writer.write("");
                    writer.openBlock("public $L build() {", "}", type.getName(),
                                     () -> writer.write("return new $L(this);", type.getName()));
                });
            });
        });
    }

    private static void writeParameterDoc(AwsJavaWriter writer, EndpointParamsPlan.Parameter parameter, String verb) {
        writer.write("/**");
        writer.write(" * $L rule-set parameter {@code $L}$L.", verb, parameter.name(),
                     parameter.builtIn().isEmpty() ? "" : " (built-in {@code " + parameter.builtIn() + "})");
        if (parameter.deprecated()) {
            writer.write(" *");
            writer.write(" * <p>The rule set deprecates this parameter.");
        }
        writer.write(" */");
        if (parameter.deprecated()) {
            writer.write("@$T", type("java.lang", "Deprecated"));
        }
    }

    /** The Java type of a parameter, written through the writer so it is imported. */
    private static String javaType(AwsJavaWriter writer, EndpointParamsPlan.Parameter parameter) {
        return switch (parameter.type()) {
            case "BOOLEAN" -> writer.format("$T", BOOLEAN);
            case "STRING_ARRAY" -> writer.format("$T<$T>", LIST, STRING);
            default -> parameter.region() ? writer.format("$T", REGION) : writer.format("$T", STRING);
        };
    }

    // ---------------------------------------------------------------------------------------------------------------
    // <Service>EndpointProvider

    static void renderProvider(EndpointProviderPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        Symbol type = plan.provider();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/**");
            writer.write(" * Resolves the endpoint of service {@code $L} from its endpoint parameters.", plan.service());
            writer.write(" * {@link #defaultProvider()} evaluates the service's canonical endpoint rule set.");
            writer.write(" */");
            writer.openBlock("public interface $L {", "}", type.getName(), () -> {
                writer.write("/** Resolves the endpoint for {@code endpointParams}. */");
                writer.write("$T<$T> resolveEndpoint($T endpointParams);", COMPLETABLE_FUTURE, ENDPOINT,
                             plan.params());
                writer.write("");
                writer.write("/** Resolves the endpoint for the parameters {@code endpointParamsConsumer} sets. */");
                writer.openBlock("default $T<$T> resolveEndpoint($T<$T.Builder> endpointParamsConsumer) {", "}",
                                 COMPLETABLE_FUTURE, ENDPOINT, CONSUMER, plan.params(), () -> {
                        writer.write("$T.Builder builder = $T.builder();", plan.params(), plan.params());
                        writer.write("endpointParamsConsumer.accept(builder);");
                        writer.write("return resolveEndpoint(builder.build());");
                    });
                writer.write("");
                writer.write("/**");
                writer.write(" * The provider that evaluates the service's canonical endpoint rule set. Resolution "
                             + "failures, including");
                writer.write(" * error rules, complete the future with an {@code EndpointResolutionException}.");
                writer.write(" */");
                writer.openBlock("static $L defaultProvider() {", "}", type.getName(),
                                 () -> writer.write("return $T.defaultProvider();", plan.rules()));
            });
        });
    }

    // ---------------------------------------------------------------------------------------------------------------
    // internal.<Service>EndpointRules

    static void renderRules(EndpointRulesPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        Symbol type = plan.rules();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type, "RuleSetHolder", "DefaultProvider");
            writer.write("/**");
            writer.write(" * The endpoint rules of service {@code $L}: its canonical endpoint rule set and the "
                         + "endpoint binding", plan.service());
            writer.write(" * of each operation, generated from the operation's endpoint traits.");
            writer.write(" */");
            writer.openBlock("public final class $L {", "}", type.getName(), () -> {
                for (EndpointRulesPlan.Binding binding : plan.bindings()) {
                    writeBinding(writer, binding);
                }
                writer.write("");
                writer.write("private static final $T<$T, $T> BINDINGS = bindingsByOperation();", MAP, STRING,
                             ENDPOINT_BINDING);
                writer.write("");
                writer.openBlock("private $L() {", "}", type.getName(), () -> {
                });
                writer.write("");
                writer.write("/** The service's endpoint rule set, parsed on first use and shared. */");
                writer.openBlock("public static $T ruleSet() {", "}", ENDPOINT_RULE_SET,
                                 () -> writer.write("return RuleSetHolder.RULE_SET;"));
                writer.write("");
                writer.write("/** The endpoint binding of every operation, by operation name, sorted. Immutable. */");
                writer.openBlock("public static $T<$T, $T> bindings() {", "}", MAP, STRING, ENDPOINT_BINDING,
                                 () -> writer.write("return BINDINGS;"));
                writer.write("");
                writer.write("/** The modeled client context parameters by name, with their shape type. */");
                writer.openBlock("public static $T<$T, $T> clientContextParams() {", "}", MAP, STRING, STRING, () -> {
                    writer.write("$T<$T, $T> params = new $T<>();", MAP, STRING, STRING, LINKED_HASH_MAP);
                    for (EndpointRulesPlan.ClientContextParam param : plan.clientContextParams()) {
                        writer.write("params.put($S, $S);", param.name(), param.type());
                    }
                    writer.write("return $T.unmodifiableMap(params);", COLLECTIONS);
                });
                writer.write("");
                writer.write("/** The service's endpoint discovery operation, if it models endpoint discovery. */");
                writer.openBlock("public static $T<$T> discoveryOperation() {", "}", OPTIONAL, SHAPE_ID,
                                 () -> writeOptionalShapeId(writer, plan.discoveryOperation()));
                writer.write("");
                writer.write("/** The service's modeled invalid-endpoint error of endpoint discovery, if any. */");
                writer.openBlock("public static $T<$T> discoveryError() {", "}", OPTIONAL, SHAPE_ID,
                                 () -> writeOptionalShapeId(writer, plan.discoveryError()));
                writer.write("");
                writer.write("/** The endpoint provider that evaluates {@link #ruleSet()}. */");
                writer.openBlock("public static $T defaultProvider() {", "}", plan.provider(),
                                 () -> writer.write("return DefaultProvider.INSTANCE;"));
                writer.write("");
                writer.openBlock("private static $T<$T, $T> bindingsByOperation() {", "}", MAP, STRING,
                                 ENDPOINT_BINDING, () -> {
                        writer.write("$T<$T, $T> bindings = new $T<>();", MAP, STRING, ENDPOINT_BINDING,
                                     LINKED_HASH_MAP);
                        plan.bindings().stream()
                            .sorted((a, b) -> a.operationName().compareTo(b.operationName()))
                            .forEach(binding -> writer.write("bindings.put($S, $L);", binding.operationName(),
                                                             binding.constant()));
                        writer.write("return $T.unmodifiableMap(bindings);", COLLECTIONS);
                    });
                writer.write("");
                writeRuleSetHolder(writer, plan.ruleSetChunks());
                writer.write("");
                writeDefaultProvider(writer, plan);
            });
        });
    }

    private static void writeBinding(AwsJavaWriter writer, EndpointRulesPlan.Binding binding) {
        writer.write("/** The endpoint binding of {@code $L}. */", binding.operation());
        writer.write("public static final $T $L = $T.builder($S)", ENDPOINT_BINDING, binding.constant(),
                     ENDPOINT_BINDING, binding.operationName());
        writer.indent(2);
        for (EndpointRulesPlan.StaticParam param : binding.staticParams()) {
            switch (param.kind()) {
                case "BOOLEAN" -> writer.write(".staticContextParam($S, $L)", param.name(), param.bool());
                case "STRING" -> writer.write(".staticContextParam($S, $S)", param.name(), param.string());
                default -> writer.write(".staticContextParam($S, $T.of($L))", param.name(), LIST,
                                        String.join(", ", param.strings().stream()
                                                               .map(s -> writer.format("$S", s)).toList()));
            }
        }
        for (EndpointRulesPlan.ContextParam param : binding.contextParams()) {
            writer.write(".contextParam($S, $S)", param.name(), param.member());
        }
        for (EndpointRulesPlan.OperationContextParam param : binding.operationContextParams()) {
            writer.write(".operationContextParam($S, $S)", param.name(), param.path());
        }
        if (!binding.hostPrefix().isEmpty()) {
            writer.write(".hostPrefix($S)", binding.hostPrefix());
        }
        if (binding.discovered()) {
            writer.write(".discovery($L, $T.of($L))", binding.discoveryRequired(), LIST,
                         String.join(", ", binding.discoveryIds().stream().map(s -> writer.format("$S", s)).toList()));
        }
        writer.write(".build();");
        writer.dedent(2);
    }

    private static void writeOptionalShapeId(AwsJavaWriter writer, String shapeId) {
        if (shapeId.isEmpty()) {
            writer.write("return $T.empty();", OPTIONAL);
        } else {
            writer.write("return $T.of($T.from($S));", OPTIONAL, SHAPE_ID, shapeId);
        }
    }

    private static void writeRuleSetHolder(AwsJavaWriter writer, List<String> chunks) {
        writer.write("/** Parses the canonical rule set on first use. */");
        writer.openBlock("private static final class RuleSetHolder {", "}", () -> {
            writer.write("static final $T RULE_SET = $T.fromJsonChunks(chunks());", ENDPOINT_RULE_SET,
                         ENDPOINT_RULE_SET);
            writer.write("");
            writer.openBlock("private RuleSetHolder() {", "}", () -> {
            });
            writer.write("");
            writer.write("/** The compact canonical JSON of the endpointRuleSet trait, in consecutive chunks. */");
            writer.openBlock("private static $T[] chunks() {", "}", STRING, () -> {
                writer.openBlock("return new $T[] {", "};", STRING, () -> {
                    for (int i = 0; i < chunks.size(); i++) {
                        writer.write("$S$L", chunks.get(i), i + 1 < chunks.size() ? "," : "");
                    }
                });
            });
        });
    }

    private static void writeDefaultProvider(AwsJavaWriter writer, EndpointRulesPlan plan) {
        writer.write("/** The rule-set-backed endpoint provider. */");
        writer.openBlock("private static final class DefaultProvider implements $T {", "}", plan.provider(), () -> {
            writer.write("static final DefaultProvider INSTANCE = new DefaultProvider();");
            writer.write("");
            writer.write("@$T", OVERRIDE);
            writer.openBlock("public $T<$T> resolveEndpoint($T endpointParams) {", "}", COMPLETABLE_FUTURE, ENDPOINT,
                             plan.params(), () -> {
                    writer.openBlock("try {", "}", () -> writer.write(
                        "return $T.completedFuture(ruleSet().resolve(endpointParams.toEndpointParameters()));",
                        COMPLETABLE_FUTURE));
                    writer.openBlock("catch ($T e) {", "}", ENDPOINT_RESOLUTION_EXCEPTION,
                                     () -> writer.write("return $T.failedFuture(e);", COMPLETABLE_FUTURE));
                });
        });
    }

    private static Symbol generatedType(String packageName, String name) {
        return Symbol.builder()
                     .namespace(packageName, ".")
                     .name(name)
                     .definitionFile(packageName.replace('.', '/') + "/" + name + ".java")
                     .build();
    }

    private static Symbol type(String packageName, String name) {
        return Symbol.builder().namespace(packageName, ".").name(name).build();
    }
}
