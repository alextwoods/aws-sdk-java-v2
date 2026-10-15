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

package software.amazon.awssdk.codegen.pipeline;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.CodeGenerator;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.emitters.GeneratorPathProvider;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestSuiteModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.client.AsyncClientClass;
import software.amazon.awssdk.codegen.poet.client.SyncClientClass;
import software.amazon.awssdk.codegen.poet.common.EnumClass;
import software.amazon.awssdk.codegen.poet.model.AwsServiceModel;
import software.amazon.awssdk.codegen.poet.schema.ClientProtocolSelection;
import software.amazon.awssdk.codegen.poet.schema.SchemaOperationRouting;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

/**
 * Generates a Prototype_Service_Set module twice, through the legacy C2J route ({@link IntermediateModelBuilder}) and
 * through the {@link CodegenPipeline} (C2J_Converter and {@code SmithyIntermediateModelBuilder}), and compares the
 * generated files.
 *
 * <p>Files only the pipeline generates because they need the Smithy model, the Schema_Generator's schema classes and
 * their {@code ServiceLoader} entry, are not compared ({@link #isSchemaGeneratorOutput}). The pipeline's model classes
 * with schema serde members are compared as the generator renders them without those members
 * ({@link #withoutSchemaSerdeMembers}).
 *
 * <p>The comparison ignores layout and the one known documentation-only difference, so that what it reports is a
 * change in generated code: whitespace and Javadoc line breaks are collapsed, and the service's
 * {@code serviceAbbreviation} and {@code serviceFullName} (the Smithy route only has the full name, from
 * {@code @title}) are both replaced by {@code <service>}. Files are then compared statement by statement (text split
 * after {@code ;}, <code>{</code> and <code>}</code> followed by a space, and after comment ends).
 *
 * <p>A module with a native Smithy model ({@link #nativeModelDirectory}: Secrets Manager and Account, OD-15) is generated
 * by the pipeline from that model, as {@code GenerationMojo} does, and still compared with the legacy route's output for
 * its C2J model. For those modules the comparison also ignores comments ({@link #withoutComments}), because the
 * documentation comes from the Smithy model's own {@code @documentation} traits, and the order of {@code throws}
 * clauses, because a native model lists operation errors in ShapeId order. Differences between the two model files
 * themselves (a native model newer than the C2J model) are reported like any other difference.
 *
 * <p>Used by {@code LegacyRouteParityTest}. Run {@link #main} with module names (default: all seven) to print the
 * differences, for example to update the test's expected differences.
 */
final class LegacyRouteParity {
    static final List<String> PROTOTYPE_MODULES = Arrays.asList("sqs", "secretsmanager", "account", "route53", "sns",
                                                               "cloudwatch", "dynamodb");

    private static final String SERVICE_MODEL = "service-2.json";
    private static final Pattern JAVADOC_LINE_BREAK = Pattern.compile("\\s*\\R\\s*\\*(?!/) ?");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern SCHEMA_GENERATOR_OUTPUT = Pattern.compile(
        "(.*/model/[A-Za-z0-9]+(Schemas\\d*|SchemaIndex|Operations|SharedSerde)\\.java)"
        + "|(.*/internal/[A-Za-z0-9]+ServiceSchemas\\.java)"
        + "|(.*META-INF/services/software\\.amazon\\.awssdk\\.schema\\.SchemaIndex)");
    /**
     * The {@code sdkSchema()} method that the model classes and enums with schema serde members (task 3.7) declare.
     */
    private static final String SCHEMA_SERDE_MARKER = "public Schema sdkSchema()";
    private static final Pattern SPACE_BEFORE_DOT = Pattern.compile(" \\.");
    private static final Pattern STATEMENT_END = Pattern.compile("(?<=[;{}])(?= )|(?<=\\*/)");
    private static final Pattern THROWS_CLAUSE = Pattern.compile("^(.*\\) throws )([A-Za-z0-9_., ]+?)( ?[{;])$");

    private LegacyRouteParity() {
    }

    public static void main(String[] args) throws IOException {
        List<String> modules = args.length == 0 ? PROTOTYPE_MODULES : Arrays.asList(args);
        Path work = Files.createTempDirectory("legacy-route-parity");
        for (String module : modules) {
            Map<String, List<String>> differences = compare(module, work.resolve(module));
            System.out.println("=== " + module + ": " + differences.size() + " file(s) differ");
            System.out.print(format(differences));
        }
    }

    /**
     * The module's {@code codegen-resources} directory, relative to a module directory of this repository.
     */
    static Path codegenResources(String module) {
        return Paths.get("..", "services", module, "src", "main", "resources", "codegen-resources");
    }

    /**
     * Generates the module both ways under {@code work} and returns, per relative file path, the differing statements
     * ({@code "- legacy"} / {@code "+ pipeline"}). A file generated by only one route is reported as such.
     */
    static Map<String, List<String>> compare(String module, Path work) {
        Path legacy = work.resolve("legacy");
        Path pipeline = work.resolve("pipeline");
        List<Path> roots = modelRoots(codegenResources(module));
        generateLegacy(roots, legacy);
        generatePipeline(module, roots, pipeline, work.resolve("build"));
        return diffTrees(legacy, pipeline, serviceNames(roots), Files.isDirectory(nativeModelDirectory(module)));
    }

    /**
     * The differences in the form {@link #main} prints and the test's expected-difference files hold.
     */
    static String format(Map<String, List<String>> differences) {
        StringBuilder out = new StringBuilder();
        differences.forEach((file, lines) -> {
            out.append("--- ").append(file).append('\n');
            lines.forEach(l -> out.append(l).append('\n'));
        });
        return out.toString();
    }

    private static List<Path> modelRoots(Path resources) {
        try (Stream<Path> files = Files.find(resources, 10, (p, a) -> p.toString().endsWith(SERVICE_MODEL))) {
            // Model sharers (dynamodbstreams) last, as GenerationMojo orders them.
            return files.map(Path::getParent)
                        .sorted(Comparator.comparing((Path p) -> customization(p).getShareModelConfig() != null)
                                          .thenComparing(Path::toString))
                        .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static CustomizationConfig customization(Path root) {
        return ModelLoaderUtils.loadOptionalModel(CustomizationConfig.class, root.resolve("customization.config").toFile(),
                                                  true)
                               .orElse(CustomizationConfig.create());
    }

    private static ServiceModel serviceModel(Path root) {
        return ModelLoaderUtils.loadModel(ServiceModel.class, root.resolve(SERVICE_MODEL).toFile());
    }

    /**
     * The names the documentation-only difference is about, longest first so that a full name containing the
     * abbreviation is replaced as a whole.
     */
    private static List<String> serviceNames(List<Path> roots) {
        TreeSet<String> names = new TreeSet<>(Comparator.comparing(String::length).reversed()
                                                        .thenComparing(Comparator.naturalOrder()));
        for (Path root : roots) {
            ServiceMetadata metadata = serviceModel(root).getMetadata();
            if (metadata.getServiceFullName() != null) {
                names.add(metadata.getServiceFullName());
            }
            if (metadata.getServiceAbbreviation() != null) {
                names.add(metadata.getServiceAbbreviation());
            }
        }
        return new ArrayList<>(names);
    }

    private static void generateLegacy(List<Path> roots, Path out) {
        Map<String, IntermediateModel> byName = new HashMap<>();
        List<IntermediateModel> models = new ArrayList<>();
        for (Path root : roots) {
            C2jModels c2j = C2jModels.builder()
                                     .customizationConfig(customization(root))
                                     .serviceModel(serviceModel(root))
                                     .waitersModel(optional(Waiters.class, root.resolve("waiters-2.json"),
                                                            Waiters.none()))
                                     .paginatorsModel(optional(Paginators.class, root.resolve("paginators-1.json"),
                                                               Paginators.none()))
                                     .endpointRuleSetModel(optional(EndpointRuleSetModel.class,
                                                                    root.resolve("endpoint-rule-set.json"), null))
                                     .endpointTestSuiteModel(optional(EndpointTestSuiteModel.class,
                                                                      root.resolve("endpoint-tests.json"), null))
                                     .build();
            IntermediateModel model = new IntermediateModelBuilder(c2j).build();
            models.add(model);
            byName.put(model.getMetadata().getServiceName().toLowerCase(), model);
        }
        for (IntermediateModel model : models) {
            CodeGenerator.builder()
                         .intermediateModel(model)
                         .shareModelsTarget(shareTarget(model, byName))
                         .sourcesDirectory(out.resolve("src").toString())
                         .resourcesDirectory(out.resolve("resources").toString())
                         .testsDirectory(out.resolve("tests").toString())
                         .build()
                         .execute();
        }
    }

    /**
     * The module's native Smithy model directory ({@code GenerationMojo}'s default {@code smithyModelDirectory}),
     * relative to a module directory of this repository. When it exists, the module's pipeline output is generated
     * from it, as the mojo does (Requirement 17.4), and compared with the legacy route's output for the C2J model.
     */
    static Path nativeModelDirectory(String module) {
        return Paths.get("..", "services", module, "model");
    }

    private static void generatePipeline(String module, List<Path> roots, Path out, Path build) {
        CodegenPipeline pipeline = CodegenPipeline.builder().listener(new CodegenPipelineListener() {
        }).build();
        List<PreparedService> prepared = new ArrayList<>();
        Map<String, IntermediateModel> byName = new HashMap<>();
        Path nativeModel = nativeModelDirectory(module);
        boolean isNative = Files.isDirectory(nativeModel);
        if (isNative && roots.size() != 1) {
            throw new IllegalStateException(module + " has a native Smithy model and " + roots.size() + " C2J models");
        }
        for (Path root : roots) {
            String name = roots.size() == 1 ? module : root.getFileName().toString();
            ServiceInputs.Builder inputs = ServiceInputs.builder()
                                                        .serviceName(name)
                                                        .customizationConfig(customization(root))
                                                        .sourcesDirectory(out.resolve("src"))
                                                        .resourcesDirectory(out.resolve("resources"))
                                                        .testsDirectory(out.resolve("tests"))
                                                        .buildDirectory(build)
                                                        .classLoader(CodegenPipeline.class.getClassLoader());
            if (isNative) {
                inputs.smithyModelSources(Collections.singletonList(nativeModel));
            } else {
                inputs.c2jModelDirectory(root);
            }
            PreparedService service = pipeline.prepare(inputs.build());
            prepared.add(service);
            IntermediateModel model = service.intermediateModel();
            byName.put(model.getMetadata().getServiceName().toLowerCase(), model);
        }
        pipeline.generate(prepared, s -> shareTarget(s.intermediateModel(), byName));
        withoutSchemaSerdeMembers(prepared, out);
        withoutSchemaOperationRouting(prepared, out);
    }

    /**
     * Re-renders the pipeline's sync and async client classes whose methods run on the schema-based pipeline path
     * (task 3.32, {@link SchemaOperationRouting}) the way the client class generators render them without it: from a
     * view of the same IntermediateModel without its Smithy model, which has no runtime protocol selection and so keeps
     * every operation's marshaller and response handlers. Only the pipeline route routes calls (it needs the Smithy
     * model's protocol traits), so comparing the routed classes with the legacy route would report every client
     * method; what the comparison checks is that the pipeline's IntermediateModel produces the legacy route's client
     * classes. The routed calls are covered by the codegen golden files ({@code SchemaClientClassTest}) and the
     * modules' tests ({@code CloudWatchNewPathTest}).
     */
    private static void withoutSchemaOperationRouting(List<PreparedService> services, Path out) {
        for (PreparedService service : services) {
            IntermediateModel model = service.intermediateModel();
            if (!ClientProtocolSelection.forModel(model).isPresent()) {
                continue;
            }
            IntermediateModel withoutSmithy = new IntermediateModel(model.getMetadata(), model.getOperations(),
                                                                    model.getShapes(), model.getCustomizationConfig(),
                                                                    model.getEndpointOperation().orElse(null),
                                                                    model.getPaginators(), model.getNamingStrategy(),
                                                                    model.getWaiters(), model.getEndpointRuleSetModel(),
                                                                    model.getEndpointTestSuiteModel(),
                                                                    model.getClientContextParams());
            GeneratorTaskParams params = GeneratorTaskParams.create(withoutSmithy, out.resolve("src").toString(),
                                                                    out.resolve("tests").toString(),
                                                                    out.resolve("resources").toString());
            String clientDirectory = params.getPathProvider().getClientDirectory();
            Map<String, ClassSpec> clients = new LinkedHashMap<>();
            clients.put(model.getMetadata().getSyncClient(), new SyncClientClass(params));
            clients.put(model.getMetadata().getAsyncClient(), new AsyncClientClass(params));
            for (Map.Entry<String, ClassSpec> client : clients.entrySet()) {
                Path file = Paths.get(clientDirectory, client.getKey() + ".java");
                ClassSpec spec = client.getValue();
                if (!Files.exists(file)) {
                    continue;
                }
                try {
                    // The generator's writer refuses to overwrite a file with other content.
                    Files.delete(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                new PoetGeneratorTask(clientDirectory, model.getFileHeader(), spec).compute();
            }
        }
    }

    /**
     * Re-renders the pipeline's model classes that have schema serde members (task 3.7) the way the model class
     * generators render a class without them, from the same IntermediateModel. Only the pipeline route generates those
     * members, from the Smithy model, so comparing them with the legacy route would report every model class; what
     * the comparison checks is that the pipeline's IntermediateModel produces the legacy route's model classes. The
     * schema serde members themselves are covered by the codegen golden files and the modules' tests.
     *
     * <p>A class is re-rendered for every service that has the shape, in generation order, as the legacy route writes
     * a class that a model sharer also generates.
     */
    private static void withoutSchemaSerdeMembers(List<PreparedService> services, Path out) {
        Map<Path, Boolean> hasSchemaSerde = new HashMap<>();
        for (PreparedService service : services) {
            IntermediateModel model = service.intermediateModel();
            String modelDirectory = new GeneratorPathProvider(model, out.resolve("src").toString(),
                                                              out.resolve("tests").toString(),
                                                              out.resolve("resources").toString()).getModelDirectory();
            for (ShapeModel shape : new TreeMap<>(model.getShapes()).values()) {
                Path file = Paths.get(modelDirectory, shape.getShapeName() + ".java");
                boolean schemaSerde = hasSchemaSerde.computeIfAbsent(
                    file, f -> Files.exists(f) && read(f).contains(SCHEMA_SERDE_MARKER));
                if (!schemaSerde) {
                    continue;
                }
                ClassSpec spec = shape.getShapeType() == ShapeType.Enum
                                 ? new EnumClass(model.getMetadata().getFullModelPackageName(), shape)
                                 : new AwsServiceModel(model, shape);
                try {
                    // The generator's writer refuses to overwrite a file with other content.
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                new PoetGeneratorTask(modelDirectory, model.getFileHeader(), spec).compute();
            }
        }
    }

    private static IntermediateModel shareTarget(IntermediateModel model, Map<String, IntermediateModel> byName) {
        CustomizationConfig config = model.getCustomizationConfig();
        return config.getShareModelConfig() == null ? null : byName.get(config.getShareModelConfig().getShareModelWith());
    }

    private static <T> T optional(Class<T> type, Path file, T orElse) {
        return ModelLoaderUtils.loadOptionalModel(type, file.toFile()).orElse(orElse);
    }

    private static Map<String, List<String>> diffTrees(Path legacy, Path pipeline, List<String> serviceNames,
                                                       boolean nativeModel) {
        TreeSet<String> files = new TreeSet<>();
        files.addAll(relativeFiles(legacy));
        files.addAll(relativeFiles(pipeline));
        Map<String, List<String>> differences = new LinkedHashMap<>();
        for (String file : files) {
            Path l = legacy.resolve(file);
            Path p = pipeline.resolve(file);
            if (!Files.exists(l)) {
                if (!isSchemaGeneratorOutput(file)) {
                    differences.put(file, single("only generated by the pipeline"));
                }
            } else if (!Files.exists(p)) {
                differences.put(file, single("only generated by the legacy route"));
            } else {
                String legacyContent = read(l);
                String pipelineContent = read(p);
                if (nativeModel && file.endsWith(".java")) {
                    legacyContent = withoutComments(legacyContent);
                    pipelineContent = withoutComments(pipelineContent);
                }
                List<String> lines = diff(statements(legacyContent, serviceNames, nativeModel),
                                          statements(pipelineContent, serviceNames, nativeModel));
                if (!lines.isEmpty()) {
                    differences.put(file, lines);
                }
            }
        }
        return differences;
    }

    /**
     * Whether a file is one of the schema classes (or their {@code ServiceLoader} entry) that only the pipeline
     * generates (tasks 1.27 and 3.15, {@code <Svc>Operations}). The legacy route has no Smithy model to generate them from, so they are left out of the
     * comparison, which covers the files both routes generate.
     */
    static boolean isSchemaGeneratorOutput(String file) {
        return SCHEMA_GENERATOR_OUTPUT.matcher(file).matches();
    }

    private static List<String> single(String s) {
        List<String> list = new ArrayList<>();
        list.add(s);
        return list;
    }

    private static List<String> relativeFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return new ArrayList<>();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                       .map(f -> root.relativize(f).toString().replace('\\', '/'))
                       .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<String> statements(String content, List<String> serviceNames) {
        return statements(content, serviceNames, false);
    }

    /**
     * @param sortThrows whether to sort the exception list of each {@code throws} clause. Used for native-model
     * modules: a native Smithy model lists operation errors in ShapeId order, not in the C2J declaration order, and the
     * order of a {@code throws} clause changes neither source nor binary compatibility.
     */
    static List<String> statements(String content, List<String> serviceNames, boolean sortThrows) {
        String text = JAVADOC_LINE_BREAK.matcher(content).replaceAll(" ");
        text = WHITESPACE.matcher(text).replaceAll(" ");
        // A wrapped method chain has a line break before some of its dots.
        text = SPACE_BEFORE_DOT.matcher(text).replaceAll(".");
        for (String name : serviceNames) {
            text = text.replace(name, "<service>");
        }
        List<String> statements = new ArrayList<>();
        for (String statement : STATEMENT_END.split(text)) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                statements.add(sortThrows ? withSortedThrows(trimmed) : trimmed);
            }
        }
        return statements;
    }

    private static String withSortedThrows(String statement) {
        Matcher m = THROWS_CLAUSE.matcher(statement);
        if (!m.find()) {
            return statement;
        }
        String sorted = Arrays.stream(m.group(2).split(","))
                              .map(String::trim)
                              .sorted()
                              .collect(Collectors.joining(", "));
        return m.group(1) + sorted + m.group(3);
    }

    /**
     * Java source without its comments; string and character literals are kept as they are. Used for native-model
     * modules, whose documentation comes from the Smithy model's {@code @documentation} traits instead of the C2J
     * documentation, and so differs in markup and whitespace without changing the generated code.
     */
    static String withoutComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            if (c == '"' || c == '\'') {
                int end = i + 1;
                while (end < n && source.charAt(end) != c) {
                    end += source.charAt(end) == '\\' ? 2 : 1;
                }
                end = Math.min(end + 1, n);
                out.append(source, i, end);
                i = end;
            } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '*') {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
                out.append(' ');
            } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '/') {
                int end = source.indexOf('\n', i);
                i = end < 0 ? n : end;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /**
     * A minimal diff: the items of each side that are not matched by a longest common subsequence.
     */
    static List<String> diff(List<String> a, List<String> b) {
        List<String> result = new ArrayList<>();
        if (a.equals(b)) {
            return result;
        }
        // Trim the common prefix and suffix, which keeps the LCS table small for the usual few differences.
        int start = 0;
        while (start < a.size() && start < b.size() && a.get(start).equals(b.get(start))) {
            start++;
        }
        int endA = a.size();
        int endB = b.size();
        while (endA > start && endB > start && a.get(endA - 1).equals(b.get(endB - 1))) {
            endA--;
            endB--;
        }
        List<String> x = a.subList(start, endA);
        List<String> y = b.subList(start, endB);
        if ((long) x.size() * y.size() > 25_000_000L) {
            x.forEach(l -> result.add("- " + l));
            y.forEach(l -> result.add("+ " + l));
            return result;
        }
        int[][] lcs = new int[x.size() + 1][y.size() + 1];
        for (int i = x.size() - 1; i >= 0; i--) {
            for (int j = y.size() - 1; j >= 0; j--) {
                lcs[i][j] = x.get(i).equals(y.get(j)) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        int i = 0;
        int j = 0;
        while (i < x.size() && j < y.size()) {
            if (x.get(i).equals(y.get(j))) {
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                result.add("- " + x.get(i++));
            } else {
                result.add("+ " + y.get(j++));
            }
        }
        while (i < x.size()) {
            result.add("- " + x.get(i++));
        }
        while (j < y.size()) {
            result.add("+ " + y.get(j++));
        }
        return result;
    }
}
