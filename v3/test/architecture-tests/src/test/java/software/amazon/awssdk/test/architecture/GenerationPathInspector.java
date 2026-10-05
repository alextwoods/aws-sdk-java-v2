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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary;
import software.amazon.awssdk.test.architecture.ClassFile.Member;
import software.amazon.awssdk.test.architecture.ClassFile.MemberRef;

/**
 * Detects forbidden generation paths (task 2.6) in compiled V3 classes, packaged resources and generated source
 * trees. Every finding is a {@link Violation} naming the module, the class (and method, for code-level findings), the
 * {@link GenerationPathRule} and its requirements.
 *
 * <h2>Scopes</h2>
 * Which rules apply depends on the module's {@link Scope}:
 * <table>
 *   <caption>Rules by scope</caption>
 *   <tr><th>Rule</th><th>GENERATED</th><th>RUNTIME</th><th>DYNAMIC</th><th>CODEGEN</th><th>TOOLING</th></tr>
 *   <tr><td>smithy-build plugin, second generator toolchain</td><td>x</td><td>x</td><td>x</td><td>x</td><td>x</td></tr>
 *   <tr><td>V2 codegen model, V2 pipeline/HTTP adapter/marshaller types, legacy input file names, serialized
 *       representations (resources and string constants)</td><td>x</td><td>x</td><td>x</td><td>x</td><td></td></tr>
 *   <tr><td>class-name lookup, reflective member access, service discovery</td><td>x</td><td>x</td><td>x</td>
 *       <td></td><td></td></tr>
 *   <tr><td>runtime model loading (Smithy parser/assembler classes, {@code Node.parse*})</td><td>x</td><td>x</td>
 *       <td></td><td></td><td></td></tr>
 *   <tr><td>resource lookup, runtime operation lookup, descriptor references, per-operation transformers and
 *       protocol coupling</td><td>x</td><td></td><td></td><td></td><td></td></tr>
 *   <tr><td>main entry points, intermediate-model types, static model state, model serialization, legacy input
 *       parsing and classifier purity</td><td></td><td></td><td></td><td>x</td><td></td></tr>
 * </table>
 * {@code DYNAMIC} modules parse Smithy JSON AST models at run time by design ({@code dynamic-model}) and resolve
 * operations by ID ({@code dynamic-client}); they are still free of class-name lookup, reflection, undeclared service
 * discovery and V2 paths. {@code CODEGEN} runs at build time, where model assembly, integration discovery and file
 * writing are its job. {@code TOOLING} (migration and release tooling) reads V2 artifacts by design and is only
 * checked for a second generator.
 *
 * <h2>Fixed exemptions</h2>
 * <ul>
 *   <li>The legacy-input classifier package of the generator module ({@link CodegenLayout#classifierPackage()}) may
 *       name and parse C2J and V2 sidecar files: it reads them only to reject them. In exchange it must be a leaf
 *       (no generator or toolchain references, no parsed content in its non-private API, no file writing) and must
 *       recognize every name in {@link #LEGACY_INPUT_FILE_NAMES}.</li>
 *   <li>Outside generated code, {@code Lookup.findVarHandle}/{@code findStaticVarHandle} in a static initializer
 *       resolve a field once per class, the JDK's supported way to create a {@code VarHandle} for atomic field
 *       updates; it is not per-call reflection.</li>
 *   <li>Outside generated code, resource lookup is permitted: runtime modules read their version resources once at
 *       client construction (Requirement 15.11).</li>
 * </ul>
 * Every other exemption is an explicit {@link Allowance} for one module, class, method and rule.
 */
final class GenerationPathInspector {

    /** How a module's classes are checked. */
    enum Scope {
        /** Generated service code: every class may run on the per-call path. */
        GENERATED,
        /** Runtime modules of the generated-client path. */
        RUNTIME,
        /** Dynamic-client modules, which own runtime model loading and ID-based operation lookup. */
        DYNAMIC,
        /** The generator and its build-tool wrapper. */
        CODEGEN,
        /** Migration and release tooling. */
        TOOLING
    }

    /** One finding. {@code location} is a class, {@code class#method}, {@code class#field} or a file. */
    record Violation(String module, String location, GenerationPathRule rule, String detail)
        implements Comparable<Violation> {

        @Override
        public int compareTo(Violation other) {
            return toString().compareTo(other.toString());
        }

        @Override
        public String toString() {
            return module + ": " + location + ": " + rule.id() + ": " + detail + " (Requirements "
                   + String.join(", ", rule.requirements()) + ")";
        }
    }

    /**
     * An explicitly allowed use: {@code rule} may be violated in {@code method} (all overloads) of {@code className}
     * in {@code module}, for the documented {@code reason}.
     */
    record Allowance(String module, String className, String method, GenerationPathRule rule, String reason) {

        String location() {
            return className + "#" + method;
        }

        boolean covers(Violation violation) {
            return violation.module().equals(module) && violation.rule() == rule
                   && violation.location().equals(location());
        }
    }

    /**
     * Where the single generator lives: its module, the Maven wrapper module, the one {@code DirectedCodegen}, the
     * one entry method, the legacy-input classifier package, and the entry-point types the wrapper may use.
     */
    record CodegenLayout(String generatorModule, String pluginModule, String directedCodegen, String entryClass,
                         String entryMethod, String entryDescriptor, String classifierPackage, Set<String> facade) {

        /** The generator's package prefix (the entry class's package and its subpackages). */
        String generatorPackage() {
            return entryClass.substring(0, entryClass.lastIndexOf('.') + 1);
        }
    }

    /** The V3 layout: {@code aws-codegen} and {@code aws-codegen-maven-plugin}. */
    static final CodegenLayout AWS_CODEGEN = new CodegenLayout(
        "aws-codegen", "aws-codegen-maven-plugin",
        "software.amazon.awssdk.awscodegen.AwsDirectedCodegen",
        "software.amazon.awssdk.awscodegen.AwsCodegen", "generate",
        "(Lsoftware/amazon/awssdk/awscodegen/AwsCodegenRequest;)Lsoftware/amazon/awssdk/awscodegen/AwsCodegenResult;",
        "software.amazon.awssdk.awscodegen.validation.",
        Set.of("software.amazon.awssdk.awscodegen.AwsCodegen", "software.amazon.awssdk.awscodegen.AwsCodegenRequest",
               "software.amazon.awssdk.awscodegen.AwsCodegenResult",
               "software.amazon.awssdk.awscodegen.AwsCodegenException"));

    /** The type of every generated operation descriptor constant. */
    static final String OPERATION_DESCRIPTOR = "software.amazon.awssdk.awscore.operation.AwsOperationDescriptor";

    /**
     * File names that V2 code generation used for C2J models and sidecars (independent of the validator's own list,
     * so that the classifier's coverage is checked rather than assumed).
     */
    static final List<String> LEGACY_INPUT_FILE_NAMES = List.of(
        "service-2.json", "api-2.json", "docs-2.json", "paginators-1.json", "paginators.json", "waiters-2.json",
        "endpoint-rule-set-1.json", "endpoint-rule-set.json", "endpoint-bdd-1.json", "endpoint-tests-1.json",
        "endpoint-tests.json", "examples-1.json", "customization.config", "smoke.json");

    static final String DIRECTED_CODEGEN = "software.amazon.smithy.codegen.core.directed.DirectedCodegen";
    static final String CODEGEN_DIRECTOR = "software.amazon.smithy.codegen.core.directed.CodegenDirector";

    private static final String NODE = "software.amazon.smithy.model.node.Node";
    private static final String SHAPE_ID = "software.amazon.smithy.model.shapes.ShapeId";
    private static final String LOOKUP = "java.lang.invoke.MethodHandles$Lookup";
    private static final String CLASS = "java.lang.Class";
    private static final String CLASS_LOADER = "java.lang.ClassLoader";

    // --- V2 marker types. V2 and V3 share the software.amazon.awssdk packages, so these are exact V2-only names or
    // packages that V3 does not use; v2-marker-defined-by-v3 fails if a V3 module ever defines one. V2 types whose
    // names V3 reuses (for example software.amazon.awssdk.http.SdkHttpRequest) are covered by dependency-tree
    // resolution in BytecodePurityTest and RuntimeDependencyPurityTest instead.

    /** V2 code generation: IntermediateModel, C2jModels, service/customization models, emitters, codegen-lite. */
    private static final List<String> V2_CODEGEN_PACKAGES = List.of("software.amazon.awssdk.codegen.");

    private static final Set<String> V2_PIPELINE_CLASSES = Set.of(
        "software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient",
        "software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient",
        "software.amazon.awssdk.core.internal.http.HttpClientDependencies",
        "software.amazon.awssdk.core.http.ExecutionContext",
        "software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain");
    private static final List<String> V2_PIPELINE_PACKAGES = List.of(
        "software.amazon.awssdk.core.internal.http.pipeline.",
        "software.amazon.awssdk.core.client.handler.",
        "software.amazon.awssdk.awscore.client.handler.",
        "software.amazon.awssdk.core.internal.handler.",
        // Smithy Java's SDK V2 bridge (aws-sdkv2-shapes, -auth, -retries): V2 runtime adapters.
        "software.amazon.smithy.java.aws.sdkv2.");

    private static final Set<String> V2_HTTP_ADAPTER_CLASSES = Set.of(
        "software.amazon.awssdk.http.SdkHttpClient",
        "software.amazon.awssdk.http.SdkHttpFullRequest",
        "software.amazon.awssdk.http.SdkHttpFullResponse",
        "software.amazon.awssdk.http.HttpExecuteRequest",
        "software.amazon.awssdk.http.HttpExecuteResponse",
        "software.amazon.awssdk.http.ExecutableHttpRequest",
        "software.amazon.awssdk.http.async.AsyncExecuteRequest",
        "software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler",
        "software.amazon.awssdk.http.async.SdkHttpContentPublisher");

    private static final Set<String> V2_MARSHALLER_CLASSES = Set.of(
        "software.amazon.awssdk.protocols.core.ProtocolMarshaller",
        "software.amazon.awssdk.protocols.core.Marshaller",
        "software.amazon.awssdk.protocols.core.OperationInfo",
        "software.amazon.awssdk.core.http.HttpResponseHandler",
        "software.amazon.awssdk.core.SdkField",
        "software.amazon.awssdk.protocols.query.AwsQueryProtocolFactory");
    private static final List<String> V2_MARSHALLER_PACKAGES = List.of(
        "software.amazon.awssdk.core.runtime.transform.",   // Marshaller: V2's per-operation marshaller contract
        "software.amazon.awssdk.protocols.json.",           // V3 uses protocols.awsjson / protocols.restjson
        "software.amazon.awssdk.protocols.xml.",            // V3 uses protocols.restxml
        "software.amazon.awssdk.protocols.cbor.",
        "software.amazon.awssdk.protocols.rpcv2.");         // V3 uses protocols.rpcv2cbor

    // --- Generator toolchains.

    /** smithy-build execution: projections and plugins. {@code FileManifest} (used by CodegenDirector) is allowed. */
    private static final Set<String> SMITHY_BUILD_RUN_CLASSES = Set.of(
        "software.amazon.smithy.build.SmithyBuild", "software.amazon.smithy.build.SmithyBuildImpl",
        "software.amazon.smithy.build.SmithyBuildPlugin", "software.amazon.smithy.build.PluginContext",
        "software.amazon.smithy.build.ProjectionTransformer", "software.amazon.smithy.build.SmithyBuildResult",
        "software.amazon.smithy.build.ProjectionResult", "software.amazon.smithy.build.TransformContext");
    private static final String SMITHY_BUILD_PLUGINS_PACKAGE = "software.amazon.smithy.build.plugins.";
    static final String SMITHY_BUILD_PLUGIN_SERVICE = "META-INF/services/software.amazon.smithy.build.SmithyBuildPlugin";

    /** Smithy Java's code generator (codegen-core, codegen-plugin: JavaCodegenPlugin, DirectedJavaCodegen). */
    private static final List<String> SECOND_GENERATOR_PACKAGES = List.of("software.amazon.smithy.java.codegen.");

    // --- Hot path.

    private static final Set<String> CLASS_LOADING_METHODS = Set.of(
        "loadClass", "findClass", "findLoadedClass", "findSystemClass", "defineClass", "findBootstrapClassOrNull");
    private static final Set<String> LOOKUP_CLASS_METHODS = Set.of(
        "findClass", "defineClass", "defineHiddenClass", "defineHiddenClassWithClassData");
    private static final Set<String> REFLECTIVE_CLASS_METHODS = Set.of(
        "getMethod", "getMethods", "getDeclaredMethod", "getDeclaredMethods", "getField", "getFields",
        "getDeclaredField", "getDeclaredFields", "getConstructor", "getConstructors", "getDeclaredConstructor",
        "getDeclaredConstructors", "getRecordComponents", "getEnclosingMethod", "getEnclosingConstructor",
        "newInstance");
    private static final Set<String> REFLECTION_TYPES = Set.of(
        "java.lang.reflect.Method", "java.lang.reflect.Field", "java.lang.reflect.Constructor",
        "java.lang.reflect.Executable", "java.lang.reflect.AccessibleObject", "java.lang.reflect.Proxy",
        "java.lang.reflect.RecordComponent", "java.lang.reflect.Parameter");
    private static final Set<String> VAR_HANDLE_LOOKUPS = Set.of("findVarHandle", "findStaticVarHandle");
    private static final Set<String> CLASS_RESOURCE_METHODS = Set.of("getResource", "getResourceAsStream");
    private static final Set<String> CLASS_LOADER_RESOURCE_METHODS = Set.of(
        "getResource", "getResources", "getResourceAsStream", "resources", "getSystemResource", "getSystemResources",
        "getSystemResourceAsStream");
    private static final Set<String> SERVICE_LOADER_TYPES = Set.of("java.util.ServiceLoader",
                                                                   "java.util.ServiceLoader$Provider");

    // --- Code generation.

    /** Simple names of V2's whole-service intermediate-model types. */
    private static final Set<String> INTERMEDIATE_MODEL_TYPE_NAMES = Set.of(
        "IntermediateModel", "C2jModels", "ServiceModel", "ShapeModel", "OperationModel", "MemberModel");
    /**
     * Writers of serialized models and object graphs. Implementing {@code Serializable} is not itself a finding (for
     * example diagnostics carried by the generator's exception are serializable); producing a serialized form is, and
     * any serialized output that reaches a packaged or generated tree is also caught by the resource checks.
     */
    private static final Set<String> SERIALIZERS = Set.of(
        "software.amazon.smithy.model.shapes.ModelSerializer",
        "software.amazon.smithy.model.shapes.SmithyIdlModelSerializer", "java.io.ObjectOutputStream");
    private static final List<String> JSON_LIBRARY_PACKAGES = List.of(
        "com.fasterxml.jackson.", "com.google.gson.", "javax.json.", "jakarta.json.", "org.json.",
        "software.amazon.smithy.java.json.");
    private static final Set<String> SHAPE_IDENTITY_TYPES = Set.of(SHAPE_ID,
                                                                   "software.amazon.smithy.model.shapes.ShapeType");
    private static final List<String> SEMANTIC_MODEL_PACKAGES = List.of(
        "software.amazon.smithy.model.shapes.", "software.amazon.smithy.model.knowledge.",
        "software.amazon.smithy.model.traits.", "software.amazon.smithy.model.node.");
    private static final Set<String> FILE_WRITER_TYPES = Set.of(
        "java.io.FileWriter", "java.io.FileOutputStream", "java.io.PrintWriter", "java.io.RandomAccessFile",
        "java.nio.channels.FileChannel");
    private static final Set<String> FILES_WRITE_METHODS = Set.of(
        "write", "writeString", "newBufferedWriter", "newOutputStream", "newByteChannel", "copy", "move",
        "createFile", "createTempFile", "createDirectory", "createDirectories", "delete", "deleteIfExists");

    // --- Generated code.

    /** Protocol-specific runtimes and codecs, which per-operation generated classes must not use. */
    private static final List<String> PROTOCOL_SPECIFIC_PACKAGES = List.of(
        "software.amazon.awssdk.protocols.awsjson.", "software.amazon.awssdk.protocols.restjson.",
        "software.amazon.awssdk.protocols.restxml.", "software.amazon.awssdk.protocols.query.",
        "software.amazon.awssdk.protocols.ec2query.", "software.amazon.awssdk.protocols.rpcv2cbor.",
        "software.amazon.smithy.java.json.", "software.amazon.smithy.java.cbor.", "software.amazon.smithy.java.xml.",
        "software.amazon.smithy.java.aws.client.awsjson.", "software.amazon.smithy.java.aws.client.restjson.",
        "software.amazon.smithy.java.aws.client.restxml.", "software.amazon.smithy.java.aws.client.awsquery.",
        "software.amazon.smithy.java.client.rpcv2", "software.amazon.smithy.java.client.http.binding.",
        "software.amazon.awssdk.protocols.json.", "software.amazon.awssdk.protocols.xml.",
        "software.amazon.awssdk.protocols.cbor.", "software.amazon.awssdk.protocols.rpcv2.");
    private static final Pattern MARSHALLER_NAME = Pattern.compile(".*(Marshaller|Unmarshaller)");
    /** The V2-compatible Event Stream callback that generated {@code <Operation>ResponseHandler} interfaces extend. */
    static final String EVENT_STREAM_RESPONSE_HANDLER = "software.amazon.awssdk.awscore.eventstream."
                                                        + "EventStreamResponseHandler";
    private static final int ACC_INTERFACE = 0x0200;
    private static final Pattern PER_OPERATION_TRANSFORMER_SUFFIX = Pattern.compile(
        "(Request|Response|Input|Output)?(Marshaller|Unmarshaller|Serializer|Deserializer|Transformer"
        + "|ResponseHandler|ErrorHandler|Codec)");

    private GenerationPathInspector() {
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Per-module bytecode.

    /** Every per-module violation of {@code classes} (a module's compiled main classes) under {@code scope}. */
    static List<Violation> inspectModule(String module, Scope scope, Map<String, ClassFile> classes,
                                         CodegenLayout layout) {
        List<Violation> out = new ArrayList<>();
        boolean hotPath = scope == Scope.GENERATED || scope == Scope.RUNTIME || scope == Scope.DYNAMIC;
        boolean sdk = hotPath || scope == Scope.CODEGEN;
        for (ClassFile type : classes.values()) {
            boolean classifier = isClassifier(module, type, layout);
            for (String reference : type.referencedClasses()) {
                if (isSmithyBuildRun(reference)) {
                    out.add(new Violation(module, type.name(), GenerationPathRule.SMITHY_BUILD_PLUGIN,
                                          "references smithy-build plugin/projection type " + reference));
                } else if (startsWithAny(reference, SECOND_GENERATOR_PACKAGES)) {
                    out.add(new Violation(module, type.name(), GenerationPathRule.SECOND_GENERATOR_TOOLCHAIN,
                                          "references Smithy Java code-generator type " + reference));
                } else if ((scope == Scope.GENERATED || scope == Scope.RUNTIME)
                           && SmithyRuntimeBoundary.isForbiddenRuntimeClass(reference)) {
                    out.add(new Violation(module, type.name(), GenerationPathRule.RUNTIME_MODEL_LOADING,
                                          "references Smithy model parsing/assembly/toolchain type " + reference));
                }
                if (sdk) {
                    v2Marker(reference).ifPresent(rule -> out.add(new Violation(
                        module, type.name(), rule, "references V2 type " + reference)));
                }
            }
            if (sdk) {
                for (String constant : type.stringConstants()) {
                    if (!classifier) {
                        for (String legacy : LEGACY_INPUT_FILE_NAMES) {
                            if (constant.contains(legacy)) {
                                out.add(new Violation(module, type.name(), GenerationPathRule.LEGACY_INPUT_FILE_NAME,
                                                      "string constant names legacy input " + legacy));
                            }
                        }
                    }
                    SerializedModelDetector.detectText(constant).ifPresent(kind -> out.add(new Violation(
                        module, type.name(), GenerationPathRule.SERIALIZED_SEMANTIC_REPRESENTATION,
                        "string constant holds a " + kind)));
                    if (scope == Scope.RUNTIME && namesModelResource(constant)) {
                        out.add(new Violation(module, type.name(), GenerationPathRule.RUNTIME_MODEL_LOADING,
                                              "string constant names a Smithy model resource: " + constant));
                    }
                }
            }
            if (hotPath) {
                out.addAll(hotPathMembers(module, scope, type, classes));
            }
        }
        if (scope == Scope.CODEGEN) {
            out.addAll(codegenRules(module, classes, layout));
        }
        if (module.equals(layout.pluginModule())) {
            out.addAll(pluginRules(module, classes, layout));
        }
        if (scope == Scope.GENERATED) {
            out.addAll(generatedRules(module, classes));
        }
        return sorted(out);
    }

    private static List<Violation> hotPathMembers(String module, Scope scope, ClassFile type,
                                                  Map<String, ClassFile> classes) {
        List<Violation> out = new ArrayList<>();
        Set<MemberRef> attributed = new HashSet<>();
        for (Member method : type.methods()) {
            attributed.addAll(method.memberReferences());
            for (MemberRef ref : method.memberReferences()) {
                classifyMember(ref, method, scope, type, classes).ifPresent(rule -> out.add(new Violation(
                    module, type.name() + "#" + method.name(), rule, "uses " + ref)));
            }
        }
        // Constant-pool member references no method's code uses (not emitted by javac, but possible in other output).
        for (MemberRef ref : type.memberReferences()) {
            if (!attributed.contains(ref)) {
                classifyMember(ref, null, scope, type, classes).ifPresent(rule -> out.add(new Violation(
                    module, type.name(), rule, "constant pool references " + ref)));
            }
        }
        return out;
    }

    /**
     * The rule {@code ref} violates when used by {@code caller} (a method of {@code type}, or {@code null} for a
     * constant-pool reference no method uses), if any.
     */
    private static Optional<GenerationPathRule> classifyMember(MemberRef ref, Member caller, Scope scope,
                                                               ClassFile type, Map<String, ClassFile> classes) {
        String method = caller == null ? "" : caller.name();
        String owner = ref.owner();
        String name = ref.name();
        if (owner.equals(CLASS) && name.equals("forName")
            || LOOKUP_CLASS_METHODS.contains(name) && owner.equals(LOOKUP)
            || CLASS_LOADING_METHODS.contains(name) && isClassLoader(owner, classes)) {
            return Optional.of(GenerationPathRule.CLASS_NAME_LOOKUP);
        }
        if (owner.equals(LOOKUP) && (name.startsWith("find") || name.startsWith("unreflect")
                                     || name.equals("revealDirect"))) {
            boolean varHandleInInitializer = VAR_HANDLE_LOOKUPS.contains(name) && method.equals("<clinit>");
            return varHandleInInitializer && scope != Scope.GENERATED ? Optional.empty()
                                                                     : Optional.of(GenerationPathRule.REFLECTIVE_MEMBER_ACCESS);
        }
        if (owner.equals(CLASS) && REFLECTIVE_CLASS_METHODS.contains(name) || REFLECTION_TYPES.contains(owner)
            || owner.equals("java.lang.invoke.MethodHandles") && (name.equals("privateLookupIn")
                                                                  || name.equals("reflectAs"))) {
            return Optional.of(GenerationPathRule.REFLECTIVE_MEMBER_ACCESS);
        }
        if (SERVICE_LOADER_TYPES.contains(owner)) {
            return Optional.of(GenerationPathRule.SERVICE_DISCOVERY);
        }
        if ((scope == Scope.GENERATED || scope == Scope.RUNTIME) && owner.equals(NODE) && name.startsWith("parse")) {
            return Optional.of(GenerationPathRule.RUNTIME_MODEL_LOADING);
        }
        if (scope == Scope.GENERATED) {
            if (owner.equals(CLASS) && CLASS_RESOURCE_METHODS.contains(name)
                || owner.equals("java.lang.Module") && name.equals("getResourceAsStream")
                || CLASS_LOADER_RESOURCE_METHODS.contains(name) && isClassLoader(owner, classes)) {
                return Optional.of(GenerationPathRule.RUNTIME_MODEL_LOADING);
            }
            ClassFile target = classes.get(owner);
            if (target != null && target != type && ref.descriptor().startsWith("(")
                && takesIdentifier(ref.descriptor())
                && (isOperationTable(target) && !isLookupForwarder(type, caller, ref)
                    || isForwardingLookup(target, ref, classes))) {
                return Optional.of(GenerationPathRule.RUNTIME_OPERATION_LOOKUP);
            }
        }
        return Optional.empty();
    }

    /** True for {@code java.lang.ClassLoader} and its JDK or in-module subclasses. */
    private static boolean isClassLoader(String owner, Map<String, ClassFile> classes) {
        if (owner.equals(CLASS_LOADER)) {
            return true;
        }
        ClassFile local = classes.get(owner);
        if (local != null) {
            return !local.superName().isEmpty() && isClassLoader(local.superName(), classes);
        }
        if (!ClassPath.isJdkClass(owner)) {
            return false;
        }
        try {
            return ClassLoader.class.isAssignableFrom(Class.forName(owner, false,
                                                                    ClassLoader.getPlatformClassLoader()));
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static boolean takesIdentifier(String methodDescriptor) {
        Set<String> parameters = ClassFile.classNamesIn(methodDescriptor.substring(0, methodDescriptor.indexOf(')')
                                                                                      + 1));
        return parameters.contains(SHAPE_ID) || parameters.contains("java.lang.String");
    }

    /**
     * True for a Smithy model resource name: a path or file name ending in {@code .smithy} or {@code .smithy.json},
     * or a {@code model.json} file. A dotted name without a path ({@code software.amazon.smithy}) is a package or
     * Maven group, not a file.
     */
    private static boolean namesModelResource(String constant) {
        String segment = constant.substring(constant.lastIndexOf('/') + 1);
        if (segment.equals("model.json")) {
            return true;
        }
        String base = segment.endsWith(".smithy.json") ? segment.substring(0, segment.length() - ".smithy.json".length())
                      : segment.endsWith(".smithy") ? segment.substring(0, segment.length() - ".smithy".length())
                      : null;
        return base != null && !base.isEmpty() && !base.contains(" ")
               && (constant.contains("/") || !base.contains("."));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Code generation.

    private static List<Violation> codegenRules(String module, Map<String, ClassFile> classes, CodegenLayout layout) {
        List<Violation> out = new ArrayList<>();
        for (ClassFile type : classes.values()) {
            for (Member method : type.methods()) {
                if (method.name().equals("main") && method.descriptor().equals("([Ljava/lang/String;)V")
                    && method.isStatic()) {
                    out.add(new Violation(module, type.name() + "#main", GenerationPathRule.CODEGEN_MAIN_ENTRY_POINT,
                                          "declares a command-line entry point; generation runs only through "
                                          + layout.entryClass() + "#" + layout.entryMethod()));
                }
            }
            String leaf = leafName(type);
            if (INTERMEDIATE_MODEL_TYPE_NAMES.contains(leaf) || leaf.endsWith("IntermediateModel")
                || leaf.endsWith("IntermediateRepresentation")) {
                out.add(new Violation(module, type.name(), GenerationPathRule.INTERMEDIATE_MODEL_TYPE,
                                      "is named like a whole-service intermediate-model type"));
            }
            for (Member field : type.fields()) {
                if (field.isStatic()) {
                    Set<String> types = new TreeSet<>(ClassFile.classNamesIn(field.descriptor()));
                    types.addAll(ClassFile.classNamesIn(field.signature()));
                    types.stream().filter(GenerationPathInspector::isSemanticModelType).findFirst()
                         .ifPresent(t -> out.add(new Violation(
                             module, type.name() + "#" + field.name(), GenerationPathRule.STATIC_SEMANTIC_MODEL_STATE,
                             "static field holds Smithy semantic model type " + t)));
                }
            }
            for (String reference : type.referencedClasses()) {
                if (SERIALIZERS.contains(reference)) {
                    out.add(new Violation(module, type.name(), GenerationPathRule.MODEL_SERIALIZATION,
                                          "references serializer " + reference));
                }
            }
            if (isClassifier(module, type, layout)) {
                out.addAll(classifierPurity(module, type, layout));
            } else {
                for (Member method : type.methods()) {
                    for (MemberRef ref : method.memberReferences()) {
                        if (ref.owner().equals(NODE) && ref.name().startsWith("parse")) {
                            out.add(new Violation(module, type.name() + "#" + method.name(),
                                                  GenerationPathRule.LEGACY_INPUT_PARSING,
                                                  "parses JSON outside the legacy-input classifier with " + ref));
                        }
                    }
                }
                for (String reference : type.referencedClasses()) {
                    if (startsWithAny(reference, JSON_LIBRARY_PACKAGES)) {
                        out.add(new Violation(module, type.name(), GenerationPathRule.LEGACY_INPUT_PARSING,
                                              "references JSON library type " + reference
                                              + " outside the legacy-input classifier"));
                    }
                }
            }
        }
        if (module.equals(layout.generatorModule())) {
            out.addAll(classifierCoverage(module, classes, layout));
        }
        return out;
    }

    private static boolean isClassifier(String module, ClassFile type, CodegenLayout layout) {
        return module.equals(layout.generatorModule()) && type.name().startsWith(layout.classifierPackage());
    }

    private static List<Violation> classifierPurity(String module, ClassFile type, CodegenLayout layout) {
        List<Violation> out = new ArrayList<>();
        for (String reference : type.referencedClasses()) {
            if (reference.startsWith(layout.generatorPackage()) && !reference.startsWith(layout.classifierPackage())) {
                out.add(new Violation(module, type.name(), GenerationPathRule.LEGACY_CLASSIFIER_PURITY,
                                      "legacy-input classifier references generator type " + reference));
            } else if (reference.startsWith("software.amazon.smithy.codegen.")
                       || reference.startsWith("software.amazon.smithy.build.")) {
                out.add(new Violation(module, type.name(), GenerationPathRule.LEGACY_CLASSIFIER_PURITY,
                                      "legacy-input classifier references code-generation type " + reference));
            }
        }
        List<Member> members = new ArrayList<>(type.fields());
        members.addAll(type.methods());
        for (Member member : members) {
            if (member.isPrivate()) {
                continue;
            }
            Set<String> types = new TreeSet<>(ClassFile.classNamesIn(member.descriptor()));
            types.addAll(ClassFile.classNamesIn(member.signature()));
            for (String t : types) {
                if (t.startsWith("software.amazon.smithy.model.node.")) {
                    out.add(new Violation(module, type.name() + "#" + member.name(),
                                          GenerationPathRule.LEGACY_CLASSIFIER_PURITY,
                                          "legacy-input classifier exposes parsed content type " + t));
                }
            }
        }
        out.addAll(fileWrites(module, type, GenerationPathRule.LEGACY_CLASSIFIER_PURITY,
                              "legacy-input classifier writes files"));
        return out;
    }

    /** The classifier exemption is only valid while the classifier exists and recognizes every legacy file name. */
    private static List<Violation> classifierCoverage(String module, Map<String, ClassFile> classes,
                                                      CodegenLayout layout) {
        Set<String> constants = new TreeSet<>();
        classes.values().stream().filter(t -> t.name().startsWith(layout.classifierPackage()))
               .forEach(t -> constants.addAll(t.stringConstants()));
        List<Violation> out = new ArrayList<>();
        for (String legacy : LEGACY_INPUT_FILE_NAMES) {
            if (constants.stream().noneMatch(c -> c.contains(legacy))) {
                out.add(new Violation(module, layout.classifierPackage() + "*",
                                      GenerationPathRule.LEGACY_CLASSIFIER_PURITY,
                                      "the legacy-input classifier does not recognize " + legacy
                                      + "; its exemption from legacy-input-file-name is stale"));
            }
        }
        return out;
    }

    private static List<Violation> pluginRules(String module, Map<String, ClassFile> classes, CodegenLayout layout) {
        List<Violation> out = new ArrayList<>();
        for (ClassFile type : classes.values()) {
            for (String reference : type.referencedClasses()) {
                if (reference.startsWith("software.amazon.smithy.")) {
                    out.add(new Violation(module, type.name(), GenerationPathRule.PLUGIN_EMITTER,
                                          "the build-tool wrapper references Smithy type " + reference));
                } else if (reference.startsWith(layout.generatorPackage()) && !classes.containsKey(reference)
                           && !layout.facade().contains(reference)) {
                    out.add(new Violation(module, type.name(), GenerationPathRule.PLUGIN_EMITTER,
                                          "the build-tool wrapper references generator internal " + reference
                                          + "; it may only use " + new TreeSet<>(layout.facade())));
                }
            }
            out.addAll(fileWrites(module, type, GenerationPathRule.PLUGIN_EMITTER,
                                  "the build-tool wrapper writes files itself"));
        }
        return out;
    }

    private static List<Violation> fileWrites(String module, ClassFile type, GenerationPathRule rule, String what) {
        List<Violation> out = new ArrayList<>();
        for (String reference : type.referencedClasses()) {
            if (FILE_WRITER_TYPES.contains(reference)) {
                out.add(new Violation(module, type.name(), rule, what + " (" + reference + ")"));
            }
        }
        for (Member method : type.methods()) {
            for (MemberRef ref : method.memberReferences()) {
                if (ref.owner().equals("java.nio.file.Files") && FILES_WRITE_METHODS.contains(ref.name())) {
                    out.add(new Violation(module, type.name() + "#" + method.name(), rule, what + " (" + ref + ")"));
                }
            }
        }
        return out;
    }

    private static boolean isSemanticModelType(String type) {
        return !SHAPE_IDENTITY_TYPES.contains(type)
               && (type.equals("software.amazon.smithy.model.Model") || startsWithAny(type, SEMANTIC_MODEL_PACKAGES));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generated code.

    private static List<Violation> generatedRules(String module, Map<String, ClassFile> classes) {
        List<Violation> out = new ArrayList<>();
        Map<String, ClassFile> descriptors = new TreeMap<>();
        classes.values().stream().filter(GenerationPathInspector::isDescriptor).forEach(d -> descriptors.put(d.name(), d));
        if (descriptors.isEmpty()) {
            out.add(new Violation(module, module, GenerationPathRule.NON_STATIC_DESCRIPTOR_REFERENCE,
                                  "no generated operation descriptor (a static INSTANCE field of type "
                                  + OPERATION_DESCRIPTOR + ") was found, so descriptor references cannot be checked"));
        }
        for (ClassFile descriptor : descriptors.values()) {
            MemberRef instance = new MemberRef(descriptor.name(), "INSTANCE", descriptorFieldType());
            boolean referenced = classes.values().stream().filter(t -> t != descriptor)
                                        .flatMap(t -> t.methods().stream())
                                        .anyMatch(m -> m.memberReferences().contains(instance));
            if (!referenced) {
                out.add(new Violation(module, descriptor.name(), GenerationPathRule.NON_STATIC_DESCRIPTOR_REFERENCE,
                                      "operation descriptor INSTANCE is not read through a static reference by any "
                                      + "other generated class"));
            }
        }
        Set<String> operations = operationNames(descriptors.values());
        for (ClassFile type : classes.values()) {
            out.addAll(transformerNames(module, type.name(), type.name(), operations,
                                        isEventStreamHandlerInterface(type)));
            String operation = operationOf(outerName(type.name()), operations);
            if (operation != null) {
                for (String reference : type.referencedClasses()) {
                    if (startsWithAny(reference, PROTOCOL_SPECIFIC_PACKAGES)) {
                        out.add(new Violation(module, type.name(), GenerationPathRule.PER_OPERATION_PROTOCOL_COUPLING,
                                              "per-operation class of " + operation
                                              + " references protocol-specific type " + reference));
                    }
                }
            }
        }
        return out;
    }

    /** Operation names from generated descriptor classes named {@code <Operation>Operation}. */
    static Set<String> operationNames(Iterable<ClassFile> descriptors) {
        Set<String> result = new TreeSet<>();
        for (ClassFile descriptor : descriptors) {
            String outer = outerName(descriptor.name());
            if (outer.endsWith("Operation") && outer.length() > "Operation".length()) {
                result.add(outer.substring(0, outer.length() - "Operation".length()));
            }
        }
        return result;
    }

    static boolean isDescriptor(ClassFile type) {
        return type.fields().stream().anyMatch(f -> f.isStatic() && f.name().equals("INSTANCE")
                                                    && f.descriptor().equals(descriptorFieldType()));
    }

    private static boolean isOperationTable(ClassFile type) {
        return type.fields().stream().anyMatch(f -> f.isStatic() && f.descriptor().equals(descriptorFieldType()));
    }

    /**
     * True if {@code caller}, a method of {@code type} (which is not itself an operation table), is a pure public
     * forwarder of the table lookup {@code ref}: a public static method with the lookup's own name and descriptor.
     * This is the generated {@code <Service>ServiceMetadata.operation(ShapeId)} tooling surface; its own callers are
     * checked as lookups by {@link #isForwardingLookup}, so forwarding never hides a lookup.
     */
    private static boolean isLookupForwarder(ClassFile type, Member caller, MemberRef ref) {
        return caller != null && !isOperationTable(type) && caller.isStatic()
               && (caller.access() & ClassFile.ACC_PUBLIC) != 0 && caller.name().equals(ref.name())
               && caller.descriptor().equals(ref.descriptor());
    }

    /** True if {@code ref} names a {@linkplain #isLookupForwarder lookup forwarder} declared by {@code target}. */
    private static boolean isForwardingLookup(ClassFile target, MemberRef ref, Map<String, ClassFile> classes) {
        for (Member method : target.methods()) {
            if (!method.name().equals(ref.name()) || !method.descriptor().equals(ref.descriptor())) {
                continue;
            }
            for (MemberRef forwarded : method.memberReferences()) {
                ClassFile table = classes.get(forwarded.owner());
                if (table != null && table != target && isOperationTable(table)
                    && isLookupForwarder(target, method, forwarded)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String descriptorFieldType() {
        return "L" + OPERATION_DESCRIPTOR.replace('.', '/') + ";";
    }

    /**
     * Whether {@code type} is a V2-shaped event stream response handler: a public interface that directly extends
     * {@value #EVENT_STREAM_RESPONSE_HANDLER} and declares no instance methods of its own. V2 publishes one per Event
     * Stream operation ({@code <Operation>ResponseHandler}) for customers to implement; it is a callback type, not a
     * protocol transformer, so the {@code ResponseHandler} naming rule does not apply to it.
     */
    static boolean isEventStreamHandlerInterface(ClassFile type) {
        return (type.access() & ACC_INTERFACE) != 0 && type.isPublic()
               && type.interfaces().equals(List.of(EVENT_STREAM_RESPONSE_HANDLER))
               && type.methods().stream().allMatch(ClassFile.Member::isStatic);
    }

    /** The source-file form of {@link #isEventStreamHandlerInterface(ClassFile)}. */
    static boolean isEventStreamHandlerInterface(String simpleName, String source) {
        return source.contains("import " + EVENT_STREAM_RESPONSE_HANDLER + ";")
               && Pattern.compile("(?m)^public interface " + Pattern.quote(simpleName)
                                  + " extends EventStreamResponseHandler<[^{]*\\{\\s*}\\s*$").matcher(source).find();
    }

    /** Naming rules for generated per-operation transformers, shared by compiled classes and source files. */
    private static List<Violation> transformerNames(String module, String location, String binaryName,
                                                    Set<String> operations, boolean eventStreamHandler) {
        List<Violation> out = new ArrayList<>();
        int dot = binaryName.lastIndexOf('.');
        String simple = binaryName.substring(dot + 1);
        List<String> packageSegments = dot < 0 ? List.of() : Arrays.asList(binaryName.substring(0, dot).split("\\."));
        if (Arrays.stream(simple.split("\\$")).anyMatch(n -> MARSHALLER_NAME.matcher(n).matches())) {
            out.add(new Violation(module, location, GenerationPathRule.PER_OPERATION_TRANSFORMER,
                                  "generated marshaller/unmarshaller class; protocols serialize through Schema "
                                  + "SERDE in runtime libraries"));
        }
        if (packageSegments.contains("transform")) {
            out.add(new Violation(module, location, GenerationPathRule.PER_OPERATION_TRANSFORMER,
                                  "generated class in a transform package"));
        }
        String outer = outerName(binaryName);
        String operation = operationOf(outer, operations);
        if (operation != null
            && PER_OPERATION_TRANSFORMER_SUFFIX.matcher(outer.substring(operation.length())).matches()
            && !(eventStreamHandler && outer.equals(operation + "ResponseHandler"))) {
            out.add(new Violation(module, location, GenerationPathRule.PER_OPERATION_TRANSFORMER,
                                  "per-operation protocol transformer for operation " + operation));
        }
        return out;
    }

    /** The longest operation name that {@code outerName} is named for ({@code <Operation>} + capitalized suffix). */
    private static String operationOf(String outerName, Set<String> operations) {
        String result = null;
        for (String operation : operations) {
            if (outerName.startsWith(operation)
                && (outerName.length() == operation.length()
                    || Character.isUpperCase(outerName.charAt(operation.length())))
                && (result == null || operation.length() > result.length())) {
                result = operation;
            }
        }
        return result;
    }

    private static String outerName(String binaryName) {
        String simple = binaryName.substring(binaryName.lastIndexOf('.') + 1);
        int nested = simple.indexOf('$');
        return nested < 0 ? simple : simple.substring(0, nested);
    }

    private static String leafName(ClassFile type) {
        String simple = type.simpleName();
        return simple.substring(simple.lastIndexOf('$') + 1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Cross-module generator uniqueness.

    /**
     * Violations across every scanned module ({@code module -> classes}): exactly one {@code DirectedCodegen}
     * implementation, {@code CodegenDirector} created and run only by the entry method, callers outside the generator
     * using only the entry method, and no V3 class impersonating a V2 marker type. {@code DirectedCodegen} and
     * {@code CodegenDirector} are followed through supertypes defined by scanned classes.
     */
    static List<Violation> inspectGenerators(Map<String, Map<String, ClassFile>> modules, CodegenLayout layout) {
        List<Violation> out = new ArrayList<>();
        Map<String, ClassFile> all = new TreeMap<>();
        modules.values().forEach(all::putAll);
        boolean expectedFound = false;
        boolean directorRun = false;
        for (Map.Entry<String, Map<String, ClassFile>> entry : modules.entrySet()) {
            String module = entry.getKey();
            boolean generatorModule = module.equals(layout.generatorModule());
            for (ClassFile type : entry.getValue().values()) {
                if (isSubtype(type, DIRECTED_CODEGEN, all, new HashSet<>())) {
                    if (generatorModule && type.name().equals(layout.directedCodegen())) {
                        expectedFound = true;
                    } else {
                        out.add(new Violation(module, type.name(), GenerationPathRule.SINGLE_DIRECTED_CODEGEN,
                                              "implements DirectedCodegen; " + layout.directedCodegen()
                                              + " is the only AWS directed code generator"));
                    }
                }
                if (isSubtype(type, CODEGEN_DIRECTOR, all, new HashSet<>())) {
                    out.add(new Violation(module, type.name(), GenerationPathRule.SINGLE_CODEGEN_DIRECTOR,
                                          "extends CodegenDirector"));
                }
                v2Marker(type.name()).ifPresent(rule -> out.add(new Violation(
                    module, type.name(), GenerationPathRule.V2_MARKER_DEFINED_BY_V3,
                    "V3 defines a class the " + rule.id() + " rule treats as V2; make that rule's class list exact")));
                for (Member method : type.methods()) {
                    boolean entryMethod = generatorModule && type.name().equals(layout.entryClass())
                                          && method.name().equals(layout.entryMethod());
                    for (MemberRef ref : method.memberReferences()) {
                        if (ref.owner().equals(CODEGEN_DIRECTOR)
                            && (ref.name().equals("<init>") || ref.name().equals("run"))) {
                            if (entryMethod) {
                                directorRun |= ref.name().equals("run");
                            } else {
                                out.add(new Violation(module, type.name() + "#" + method.name(),
                                                      GenerationPathRule.SINGLE_CODEGEN_DIRECTOR,
                                                      "uses " + ref + " outside " + layout.entryClass() + "#"
                                                      + layout.entryMethod()));
                            }
                        }
                        if (!generatorModule && ref.owner().equals(layout.entryClass())
                            && !(ref.name().equals(layout.entryMethod())
                                 && ref.descriptor().equals(layout.entryDescriptor()))) {
                            out.add(new Violation(module, type.name() + "#" + method.name(),
                                                  GenerationPathRule.GENERATOR_ENTRY_POINT,
                                                  "uses " + ref + "; callers outside the generator may only call "
                                                  + layout.entryClass() + "." + layout.entryMethod()
                                                  + layout.entryDescriptor()));
                        }
                    }
                }
            }
        }
        if (!expectedFound) {
            out.add(new Violation(layout.generatorModule(), layout.directedCodegen(),
                                  GenerationPathRule.SINGLE_DIRECTED_CODEGEN,
                                  "the AWS directed code generator is missing or does not implement DirectedCodegen"));
        }
        if (!directorRun) {
            out.add(new Violation(layout.generatorModule(), layout.entryClass() + "#" + layout.entryMethod(),
                                  GenerationPathRule.SINGLE_CODEGEN_DIRECTOR,
                                  "the generator entry point does not run CodegenDirector"));
        }
        return sorted(out);
    }

    private static boolean isSubtype(ClassFile type, String supertype, Map<String, ClassFile> all, Set<String> seen) {
        if (!seen.add(type.name())) {
            return false;
        }
        List<String> parents = new ArrayList<>(type.interfaces());
        if (!type.superName().isEmpty()) {
            parents.add(type.superName());
        }
        for (String parent : parents) {
            if (parent.equals(supertype)) {
                return true;
            }
            ClassFile scanned = all.get(parent);
            if (scanned != null && isSubtype(scanned, supertype, all, seen)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Resources and generated trees.

    /**
     * Serialized semantic representations and smithy-build plugin registrations among the files under {@code root}
     * (a class directory or a generated resource tree). Class files and Java sources are skipped.
     */
    static List<Violation> inspectResources(String module, Path root) {
        List<Violation> out = new ArrayList<>();
        for (Path file : files(root)) {
            String relative = portable(root.relativize(file));
            if (relative.endsWith(".class") || relative.endsWith(".java")) {
                continue;
            }
            if (relative.equals(SMITHY_BUILD_PLUGIN_SERVICE)) {
                out.add(new Violation(module, "resource " + relative, GenerationPathRule.SMITHY_BUILD_PLUGIN,
                                      "registers a smithy-build plugin"));
            }
            SerializedModelDetector.detectFile(file.getFileName().toString(), read(file)).ifPresent(kind -> out.add(
                new Violation(module, "resource " + relative, GenerationPathRule.SERIALIZED_SEMANTIC_REPRESENTATION,
                              "is a " + kind)));
        }
        return sorted(out);
    }

    /**
     * Violations in a generated source tree: transformer-named source files and {@code transform} packages (for the
     * given operation names), and serialized representations among its non-Java files.
     */
    static List<Violation> inspectGeneratedSources(String module, Path root, Set<String> operations) {
        List<Violation> out = new ArrayList<>();
        for (Path file : files(root)) {
            String relative = portable(root.relativize(file));
            if (relative.endsWith(".java")) {
                String binaryName = relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
                String simpleName = binaryName.substring(binaryName.lastIndexOf('.') + 1);
                boolean handler = simpleName.endsWith("ResponseHandler")
                                  && isEventStreamHandlerInterface(simpleName,
                                                                   new String(read(file), StandardCharsets.UTF_8));
                out.addAll(transformerNames(module, "source " + relative, binaryName, operations, handler));
            }
        }
        out.addAll(inspectResources(module, root));
        return sorted(out);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Allowances.

    /** {@code violations} without those an allowance covers; every allowance that covered one is added to {@code used}. */
    static List<Violation> withoutAllowed(List<Violation> violations, List<Allowance> allowances,
                                          Set<Allowance> used) {
        List<Violation> result = new ArrayList<>();
        for (Violation violation : violations) {
            Optional<Allowance> allowance = allowances.stream().filter(a -> a.covers(violation)).findFirst();
            allowance.ifPresent(used::add);
            if (allowance.isEmpty()) {
                result.add(violation);
            }
        }
        return result;
    }

    // ---------------------------------------------------------------------------------------------------------------

    /** The V2 marker rule that matches {@code binaryName}, if any. */
    static Optional<GenerationPathRule> v2Marker(String binaryName) {
        if (startsWithAny(binaryName, V2_CODEGEN_PACKAGES)) {
            return Optional.of(GenerationPathRule.V2_CODEGEN_MODEL);
        }
        if (V2_PIPELINE_CLASSES.contains(binaryName) || startsWithAny(binaryName, V2_PIPELINE_PACKAGES)) {
            return Optional.of(GenerationPathRule.V2_EXECUTION_PIPELINE);
        }
        if (matchesClass(binaryName, V2_HTTP_ADAPTER_CLASSES)) {
            return Optional.of(GenerationPathRule.V2_HTTP_ADAPTER);
        }
        if (matchesClass(binaryName, V2_MARSHALLER_CLASSES) || startsWithAny(binaryName, V2_MARSHALLER_PACKAGES)) {
            return Optional.of(GenerationPathRule.V2_PROTOCOL_MARSHALLER);
        }
        return Optional.empty();
    }

    /** {@code binaryName} is one of {@code classes} or nested in one. */
    private static boolean matchesClass(String binaryName, Set<String> classes) {
        int nested = binaryName.indexOf('$');
        return classes.contains(nested < 0 ? binaryName : binaryName.substring(0, nested));
    }

    private static boolean isSmithyBuildRun(String reference) {
        return SMITHY_BUILD_RUN_CLASSES.contains(reference) || reference.startsWith(SMITHY_BUILD_PLUGINS_PACKAGE);
    }

    private static boolean startsWithAny(String name, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static List<Path> files(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list " + root, e);
        }
    }

    private static byte[] read(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    private static String portable(Path relative) {
        return relative.toString().replace('\\', '/');
    }

    private static List<Violation> sorted(List<Violation> violations) {
        return violations.stream().distinct().sorted().toList();
    }
}
