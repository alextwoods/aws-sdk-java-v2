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

package software.amazon.awssdk.test.codegen.support;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.endpoints.binding.EndpointBinding;
import software.amazon.awssdk.endpoints.binding.EndpointClientValues;
import software.amazon.awssdk.endpoints.rules.EndpointRuleSet;
import software.amazon.awssdk.endpoints.rules.EndpointTestSuite;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.json.JsonCodec;

/**
 * The endpoint test-vector runner for generated services: it runs every {@code operationInputs} entry of a service's
 * canonical {@code smithy.rules#endpointTests} through the service's <em>generated</em> endpoint surface. The
 * operation input is built from the case's {@code operationParams} with the generated input type (through the
 * generated type registry and schema deserialization), the generated {@code EndpointBinding} of the operation binds
 * the rule-set parameters from it and from the case's built-in and client parameters, and the generated rule set
 * resolves them; the outcome must be exactly the case's expected endpoint or error.
 */
public final class GeneratedEndpointVectors {
    private static final JsonCodec JSON = JsonCodec.builder().build();

    private final EndpointRuleSet ruleSet;
    private final Map<String, EndpointBinding> bindings;
    private final List<AwsOperationDescriptor> operations;
    private final SchemaTypeRegistry registry;

    /**
     * @param loader the class loader of the compiled generated service
     * @param packageName the service's root package
     * @param serviceTypeName the service's base Java type name (for example {@code AmazonS3})
     */
    public GeneratedEndpointVectors(ClassLoader loader, String packageName, String serviceTypeName) {
        this(loader, packageName, serviceTypeName, true);
    }

    /**
     * @param typedInputs whether operation inputs are built with the generated input types; if {@code false} (only
     *        the generated {@code endpoints} packages are compiled), an input is its {@code operationParams} JSON
     *        object, which the binding reads by member name exactly as it reads a structure through its schema
     */
    @SuppressWarnings("unchecked")
    public GeneratedEndpointVectors(ClassLoader loader, String packageName, String serviceTypeName,
                                    boolean typedInputs) {
        String rules = packageName + ".endpoints.internal." + serviceTypeName + "EndpointRules";
        String metadata = packageName + "." + serviceTypeName + "ServiceMetadata";
        this.ruleSet = (EndpointRuleSet) CompiledSources.invokeStatic(loader, rules, "ruleSet");
        this.bindings = (Map<String, EndpointBinding>) CompiledSources.invokeStatic(loader, rules, "bindings");
        this.operations = typedInputs
                          ? (List<AwsOperationDescriptor>) CompiledSources.invokeStatic(loader, metadata, "operations")
                          : null;
        this.registry = typedInputs
                        ? (SchemaTypeRegistry) CompiledSources.invokeStatic(loader, metadata, "typeRegistry")
                        : null;
    }

    /** The generated rule set. */
    public EndpointRuleSet ruleSet() {
        return ruleSet;
    }

    /** The generated bindings by operation name. */
    public Map<String, EndpointBinding> bindings() {
        return bindings;
    }

    /**
     * Runs every operation input of every case of {@code suite}.
     *
     * @return one message per failing operation input; empty if all pass
     */
    public List<String> run(EndpointTestSuite suite) {
        List<String> failures = new ArrayList<>();
        for (EndpointTestSuite.TestCase testCase : suite.testCases()) {
            for (EndpointTestSuite.OperationInput input : testCase.operationInputs()) {
                String prefix = input.operationName() + ": ";
                try {
                    Object request = registry == null ? input.operationParams() : input(input);
                    EndpointClientValues client = clientValues(input);
                    EndpointBinding binding = bindings.get(input.operationName());
                    if (binding == null) {
                        failures.add(prefix + "no generated endpoint binding");
                        continue;
                    }
                    // The endpoint test vectors describe the rules' result; the host prefix is a later stage.
                    testCase.check(() -> ruleSet.resolve(binding.bind(ruleSet, request, client)))
                            .ifPresent(failure -> failures.add(prefix + failure));
                } catch (RuntimeException e) {
                    failures.add(prefix + "test case " + testCase.index() + " (" + testCase.documentation()
                                 + ") could not run: " + e);
                }
            }
        }
        return failures;
    }

    /** The number of operation inputs in {@code suite}. */
    public static int operationInputCount(EndpointTestSuite suite) {
        return suite.testCases().stream().mapToInt(c -> c.operationInputs().size()).sum();
    }

    /** The operation input of {@code input}, built with the generated input type from its JSON members. */
    public SerializableStruct input(EndpointTestSuite.OperationInput input) {
        AwsOperationDescriptor operation = operations.stream()
                                                     .filter(o -> o.id().getName().equals(input.operationName()))
                                                     .findFirst()
                                                     .orElseThrow(() -> new IllegalArgumentException(
                                                         "No generated operation " + input.operationName()));
        ShapeBuilder<?> builder = registry.createBuilder(operation.inputId());
        byte[] json = JsonText.write(input.operationParams()).getBytes(StandardCharsets.UTF_8);
        try (var deserializer = JSON.createDeserializer(json)) {
            return (SerializableStruct) builder.deserialize(deserializer).build();
        }
    }

    /** The case's built-in and client context parameter values. */
    public static EndpointClientValues clientValues(EndpointTestSuite.OperationInput input) {
        EndpointClientValues.Builder client = EndpointClientValues.builder();
        input.builtInParams().forEach(client::builtIn);
        input.clientParams().forEach(client::clientContextParam);
        return client.build();
    }

    /** Minimal JSON rendering of the endpoints runtime's parsed JSON values. */
    static final class JsonText {
        private JsonText() {
        }

        static String write(Object value) {
            StringBuilder out = new StringBuilder();
            write(value, out);
            return out.toString();
        }

        private static void write(Object value, StringBuilder out) {
            if (value == null) {
                out.append("null");
            } else if (value instanceof String s) {
                out.append('"');
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    switch (c) {
                        case '"' -> out.append("\\\"");
                        case '\\' -> out.append("\\\\");
                        case '\n' -> out.append("\\n");
                        case '\r' -> out.append("\\r");
                        case '\t' -> out.append("\\t");
                        default -> {
                            if (c < 0x20) {
                                out.append(String.format("\\u%04x", (int) c));
                            } else {
                                out.append(c);
                            }
                        }
                    }
                }
                out.append('"');
            } else if (value instanceof Map<?, ?> map) {
                out.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    write(String.valueOf(entry.getKey()), out);
                    out.append(':');
                    write(entry.getValue(), out);
                }
                out.append('}');
            } else if (value instanceof List<?> list) {
                out.append('[');
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        out.append(',');
                    }
                    write(list.get(i), out);
                }
                out.append(']');
            } else {
                out.append(value);
            }
        }
    }
}
