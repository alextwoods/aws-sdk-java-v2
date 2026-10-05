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

package software.amazon.awssdk.test.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.test.architecture.GenerationPathInspector.Allowance;
import software.amazon.awssdk.test.architecture.GenerationPathInspector.Scope;
import software.amazon.awssdk.test.architecture.GenerationPathInspector.Violation;

/**
 * Fails the V3 reactor when a forbidden generation path is introduced (task 2.6; Requirements 2.1, 2.3-2.8, 3.1,
 * 3.3, 3.4, 3.8, 3.9, 5.3, 5.4, 10.5, 10.12, 12.15, 20.22). {@link GenerationPathInspector} documents every rule and
 * {@link GenerationPathInspectionTest} proves each one against negative fixtures. This test applies them to:
 * <ul>
 *   <li>the compiled classes of every production module (release artifacts, generated services and generated-code
 *       modules; required, and compiled before {@code test/} by {@code v3-all} and {@code v3-tests}) and of every
 *       other non-test module that has been compiled (release tooling, which only {@code v3-all} builds);</li>
 *   <li>the packaged resources ({@code target/classes}) of the same modules;</li>
 *   <li>the generated source and resource trees ({@code target/generated-sources/aws-codegen},
 *       {@code target/generated-resources}) of every generated-code module.</li>
 * </ul>
 * Scopes follow the module kinds of the dependency policy; generated-code modules are always {@code GENERATED}.
 * Any exemption beyond the inspector's fixed ones must be listed in {@link #ALLOWANCES} with its reason, and every
 * listed allowance must still be needed.
 */
class ForbiddenGenerationPathsTest {

    /** Module kind to scope. Every kind of the policy must be listed (checked below). */
    static final Map<String, Scope> SCOPES_BY_KIND = Map.ofEntries(
        Map.entry("api", Scope.RUNTIME),
        Map.entry("runtime", Scope.RUNTIME),
        Map.entry("protocol", Scope.RUNTIME),
        Map.entry("http-spi", Scope.RUNTIME),
        Map.entry("http-implementation", Scope.RUNTIME),
        Map.entry("telemetry-spi", Scope.RUNTIME),
        Map.entry("telemetry-exporter", Scope.RUNTIME),
        Map.entry("high-level-library", Scope.RUNTIME),
        Map.entry(DependencyPolicy.SERVICE_KIND, Scope.GENERATED),
        Map.entry("dynamic", Scope.DYNAMIC),
        Map.entry("codegen", Scope.CODEGEN),
        Map.entry("migration-tool", Scope.TOOLING),
        Map.entry("release-tooling", Scope.TOOLING),
        Map.entry("test", Scope.TOOLING));

    /**
     * Explicitly allowed uses. Add an entry only for a construction-time or build-time use that a requirement
     * permits, name that requirement in the reason, and scope it to one method. For example, HTTP implementation
     * discovery at client construction (Requirement 12.11) would be allowed as
     * {@code new Allowance("http-spi", "...HttpImplementationDiscovery", "discover", SERVICE_DISCOVERY, "...")}.
     */
    static final List<Allowance> ALLOWANCES = Stream.of(List.of(
        new Allowance("protocol-core", "software.amazon.awssdk.protocols.core.InstalledProtocols", "discover",
                      GenerationPathRule.SERVICE_DISCOVERY,
                      "Requirements 5.6 and 5.12: client construction discovers the installed protocol modules "
                      + "(ProtocolProvider service entries) once, to select exactly one declared, installed protocol. "
                      + "It never runs while invoking an operation (Requirements 10.12, 20.22): the selected provider "
                      + "is held by the client, and a caller-supplied protocol bypasses discovery."),
        new Allowance("http-spi", "software.amazon.awssdk.http.provider.HttpImplementationDiscovery", "discover",
                      GenerationPathRule.SERVICE_DISCOVERY,
                      "Requirements 12.11 and 12.12: client construction discovers the installed HTTP implementations "
                      + "(SdkHttpService / SdkAsyncHttpService service entries) at most once, and only when neither a "
                      + "client nor an implementation is configured, to select the highest-priority one of the mode "
                      + "or fail on a tie. It never runs while invoking an operation (Requirements 10.12, 20.22): the "
                      + "resolved client is held by the SDK client, and HttpClientResolver.installed replaces "
                      + "discovery in tests."),
        new Allowance("aws-pipeline", "software.amazon.awssdk.pipeline.InterceptorDispatch", "overrides",
                      GenerationPathRule.REFLECTIVE_MEMBER_ACCESS,
                      "Requirements 10.11 and 11.13: when ClientRuntime builds an operation's plan, the interceptor "
                      + "dispatch detects once which hooks each interceptor class overrides (Class.getMethod), so a "
                      + "hook no interceptor implements costs no dispatch (Requirement 10.10). It never runs while "
                      + "invoking an operation (Requirement 20.22): PipelineArchitectureTest proves the call path "
                      + "cannot reach it.")),
        // DynamoDB Enhanced bean and immutable mapping (task 14.1).
        enhancedSchemaIntrospection("internal.immutable.ImmutableIntrospector", "<init>", "extractBuildMethod",
                                    "extractSetterMethod", "findStaticBuilderMethod", "introspect",
                                    "isMappableMethod", "isMethodBoolean", "normalizeGetterName",
                                    "normalizeSetterName", "setterHasValidParameterSignature",
                                    "setterHasValidReturnType", "validateGetter"),
        enhancedSchemaIntrospection("internal.mapper.BeanAttributeGetter", "create"),
        enhancedSchemaIntrospection("internal.mapper.BeanAttributeSetter", "create"),
        enhancedSchemaIntrospection("internal.mapper.LambdaToMethodBridgeBuilder", "lambda$build$1",
                                    "lambda$build$3"),
        enhancedSchemaIntrospection("internal.mapper.ObjectConstructor", "create"),
        enhancedSchemaIntrospection("internal.mapper.ObjectGetterMethod", "create"),
        enhancedSchemaIntrospection("internal.mapper.StaticGetterMethod", "create"),
        enhancedSchemaIntrospection("mapper.AnnotationUtils", "extractFromContainer"),
        enhancedSchemaIntrospection("mapper.BeanTableSchema", "findFluentSetter", "getPropertyAnnotation",
                                    "lambda$addTagsToAttribute$0", "lambda$createStaticTableSchema$1",
                                    "lambda$findFluentSetter$0", "newObjectSupplierForClass", "propertyAnnotations",
                                    "staticAttributeBuilder", "validateDynamoDbFlattenAnnotations"),
        enhancedSchemaIntrospection("mapper.ImmutableTableSchema", "getPropertyAnnotation", "immutableAttributeBuilder",
                                    "lambda$addTagsToAttribute$0", "lambda$createStaticImmutableTableSchema$0",
                                    "newObjectSupplierForClass", "propertyAnnotations"))
        .flatMap(List::stream).toList();

    /**
     * Allowances for the DynamoDB Enhanced Client's schema introspection, one per method. Requirement 16.6 launches
     * the V2 bean and immutable mapping behavior ({@code TableSchema.fromBean}, {@code fromImmutableClass},
     * {@code BeanTableSchema.create}, {@code ImmutableTableSchema.create}), which discovers properties, annotations,
     * builders and constructors by reflection when the table schema is built. Introspection runs once per schema
     * (cached by the enhanced client's MetaTableSchemaCache), and the accessors it produces are
     * {@code LambdaMetafactory}-generated functions, so mapping items and executing enhanced or generated operations
     * performs no reflective member access (Requirement 20.22).
     */
    private static List<Allowance> enhancedSchemaIntrospection(String className, String... methods) {
        return Stream.of(methods)
                     .map(m -> new Allowance("dynamodb-enhanced", "software.amazon.awssdk.enhanced.dynamodb." + className,
                                             m, GenerationPathRule.REFLECTIVE_MEMBER_ACCESS,
                                             "Requirement 16.6: DynamoDB Enhanced table-schema construction "
                                             + "introspects mapped classes once per schema; item mapping and "
                                             + "operations use the generated accessors (Requirement 20.22)."))
                     .toList();
    }

    /** Relative to a generated-code module: the aws-codegen output root (GenerateMojo's default outputDirectory). */
    private static final String GENERATED_SOURCES = "target/generated-sources/aws-codegen";
    private static final String GENERATED_RESOURCES = "target/generated-resources";

    private static V3Reactor reactor;
    private static DependencyPolicy policy;
    private static Map<String, Scope> scopes;

    @BeforeAll
    static void load() {
        reactor = V3Reactor.load();
        policy = reactor.policy();
        scopes = new TreeMap<>();
        for (String module : reactor.productionModules()) {
            scopes.put(module, scopeOf(module));
        }
        // Modules without a policy entry are reported by ReactorPolicyConformanceTest.
        for (String module : reactor.artifactModules().keySet()) {
            String kind = policy.kindOf(module).orElse("test");
            if (!scopes.containsKey(module) && !kind.equals("test")
                && Files.isDirectory(reactor.moduleDirectory(module).resolve("target/classes"))) {
                scopes.put(module, scopeOf(module));
            }
        }
    }

    @Test
    void everyModuleKindHasAScope() {
        Set<String> kinds = new TreeSet<>();
        policy.modules().values().forEach(m -> kinds.add(m.kind()));
        kinds.add(DependencyPolicy.SERVICE_KIND);
        kinds.removeAll(SCOPES_BY_KIND.keySet());
        assertEquals(Set.of(), kinds, "Add new module kinds to ForbiddenGenerationPathsTest.SCOPES_BY_KIND");
    }

    @Test
    void theScanCoversTheGeneratorAndGeneratedCode() {
        assertEquals(Scope.CODEGEN, scopes.get(GenerationPathInspector.AWS_CODEGEN.generatorModule()));
        assertEquals(Scope.CODEGEN, scopes.get(GenerationPathInspector.AWS_CODEGEN.pluginModule()));
        Set<String> generated = new TreeSet<>(reactor.generatedCodeModules());
        generated.addAll(reactor.serviceModules());
        assertFalse(generated.isEmpty(), "No generated-code module found; the generated-tree checks would be vacuous");
        generated.forEach(m -> assertEquals(Scope.GENERATED, scopes.get(m), m));
    }

    @Test
    void thereIsExactlyOneGeneratorWithOneEntryPoint() {
        Map<String, Map<String, ClassFile>> classes = new TreeMap<>();
        scopes.keySet().forEach(m -> classes.put(m, reactor.classesOf(m)));
        assertNoViolations(GenerationPathInspector.inspectGenerators(classes, GenerationPathInspector.AWS_CODEGEN));
    }

    @Test
    void compiledClassesContainNoForbiddenGenerationPath() {
        List<Violation> violations = new ArrayList<>();
        scopes.forEach((module, scope) -> violations.addAll(GenerationPathInspector.inspectModule(
            module, scope, reactor.classesOf(module), GenerationPathInspector.AWS_CODEGEN)));
        assertNoViolations(violations);
    }

    @Test
    void packagedResourcesContainNoSerializedSemanticRepresentation() {
        List<Violation> violations = new ArrayList<>();
        scopes.forEach((module, scope) -> {
            if (scope != Scope.TOOLING) {
                violations.addAll(GenerationPathInspector.inspectResources(module, reactor.compiledClasses(module)));
            }
        });
        assertNoViolations(violations);
    }

    @Test
    void generatedTreesContainNoTransformerOrSerializedModel() {
        List<Violation> violations = new ArrayList<>();
        scopes.forEach((module, scope) -> {
            if (scope != Scope.GENERATED) {
                return;
            }
            Path directory = reactor.moduleDirectory(module);
            Path sources = directory.resolve(GENERATED_SOURCES);
            assertTrue(Files.isDirectory(sources) && containsJava(sources),
                       module + " has no generated sources under " + sources + "; generated-code modules must be "
                       + "generated before the architecture tests run");
            Set<String> operations = GenerationPathInspector.operationNames(
                reactor.classesOf(module).values().stream().filter(GenerationPathInspector::isDescriptor).toList());
            violations.addAll(GenerationPathInspector.inspectGeneratedSources(module, sources, operations));
            Path resources = directory.resolve(GENERATED_RESOURCES);
            if (Files.isDirectory(resources)) {
                violations.addAll(GenerationPathInspector.inspectResources(module, resources));
            }
        });
        assertNoViolations(violations);
    }

    @Test
    void everyAllowanceIsNeededAndNamesAScannedMethod() {
        Set<Allowance> used = new HashSet<>();
        List<Violation> all = new ArrayList<>();
        scopes.forEach((module, scope) -> all.addAll(GenerationPathInspector.inspectModule(
            module, scope, reactor.classesOf(module), GenerationPathInspector.AWS_CODEGEN)));
        GenerationPathInspector.withoutAllowed(all, ALLOWANCES, used);
        List<String> problems = new ArrayList<>();
        for (Allowance allowance : ALLOWANCES) {
            if (allowance.reason().isBlank()) {
                problems.add(allowance + " has no reason");
            }
            ClassFile type = scopes.containsKey(allowance.module())
                             ? reactor.classesOf(allowance.module()).get(allowance.className()) : null;
            if (type == null || type.methods().stream().noneMatch(m -> m.name().equals(allowance.method()))) {
                problems.add(allowance + " names no scanned module, class and method");
            } else if (!used.contains(allowance)) {
                problems.add(allowance + " is no longer needed; remove it");
            }
        }
        assertEquals(List.of(), problems);
    }

    private static Scope scopeOf(String module) {
        if (reactor.generatedCodeModules().contains(module) || reactor.serviceModules().contains(module)) {
            return Scope.GENERATED;
        }
        String kind = policy.kindOf(module).orElseThrow(() -> new IllegalStateException("No policy for " + module));
        Scope scope = SCOPES_BY_KIND.get(kind);
        if (scope == null) {
            throw new IllegalStateException("Module kind " + kind + " of " + module + " has no scope");
        }
        return scope;
    }

    private static void assertNoViolations(List<Violation> violations) {
        List<String> remaining = GenerationPathInspector.withoutAllowed(violations, ALLOWANCES, new HashSet<>())
                                                        .stream().map(Violation::toString).toList();
        assertEquals(List.of(), remaining, "Forbidden generation paths:\n  " + String.join("\n  ", remaining));
    }

    private static boolean containsJava(Path root) {
        try (Stream<Path> files = Files.walk(root)) {
            return files.anyMatch(f -> f.toString().endsWith(".java"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
