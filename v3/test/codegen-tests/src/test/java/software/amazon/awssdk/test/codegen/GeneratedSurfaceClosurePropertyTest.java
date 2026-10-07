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

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.pipeline.AwsOperation;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 7 (Requirements 3.8, 7.1, 7.4, 7.5, 7.6, 8.1, 8.3, 16.5, 21.1): for any client-supported service, the
 * generated model, response, operation, sync-client, async-client, overload, paginator, waiter, presigner and utility
 * surfaces equal the canonical model closure plus the entries the Parity Manifest requires as {@code PRESERVED} or
 * {@code ADAPTED}, every operation output extends the service's base response, and every generated operation method
 * holds a static descriptor reference.
 *
 * <p><b>Services.</b> Each try draws a {@link Service}: a parity {@link Profile} (a plain service, or a service with
 * the shape ID of S3, RDS, Aurora DSQL or CloudFront, the services whose V2 presigner or utilities the baseline
 * preserves), a protocol (AWS JSON 1.0/1.1, REST JSON 1 or, for S3, REST XML), optional endpoint rules (always for
 * S3, whose utilities need the {@code Bucket} and {@code Region} parameters), one to five operations and pools of data
 * shapes (structures, unions, enums, int enums, lists, maps; possibly recursive through structures and unions) and
 * errors.
 * Operations have unit, empty, optional-member or required-member inputs, and on HTTP-binding protocols streaming-blob
 * inputs and outputs and, on REST JSON, input and output event streams; some are paginated and some waitable; each has
 * a random subset of the errors, and the service has another. Data shapes and errors that no operation reaches remain
 * in the model, outside the closure. A streaming output always has a modeled input (as every V2 operation does), so
 * its response-transformer overloads take a request. S3 services draw some operation names from V2's presigned S3
 * operations.
 *
 * <p><b>Parity entries.</b> Each try derives its {@link ParityEntry parity entries} from the V2 Launch Parity Baseline:
 * the async client of the service, the request-builder consumer overload of every operation with modeled input, the
 * no-argument overload of every operation with modeled input and no required input member, and the presigner and
 * utilities of the profile, each {@code PRESERVED}. The generator takes no Parity Manifest input through its public
 * entry point yet (its client-surface classifier is the fixed V2 baseline), so these are exactly the entries it is
 * required to honor; the oracle nevertheless reads every overload, client and entry point from the entries rather than
 * assuming them.
 *
 * <p><b>Oracle.</b> Set-based and independent of {@code aws-codegen}: computed from the {@link Service} description
 * that the model files are rendered from, with V2's naming rules ({@code <Operation>Request}/{@code Response},
 * {@code <Error>Exception}, {@code <Service>Client}, {@code <Service>Response}/{@code ResponseMetadata}, lower-camel
 * operation methods). The drawn shape names never take a generated type's name, so no name collision is rejected.
 * The model closure is a breadth-first walk from every operation's input and output members, operation errors and
 * service errors through structure, union, list, map, event-stream and error members. The expected surfaces are:
 * <ul>
 *   <li><em>Public types</em>: exactly the model types of the closure (structures, unions, enums and int enums;
 *       {@code <Operation>Request}/{@code Response} of non-unit inputs and outputs; the exceptions of reached errors;
 *       the service base exception; the {@code <Operation>ResponseHandler} of output event streams), one service base
 *       response {@code <Service>Response} and one response metadata view {@code <Service>ResponseMetadata} in the
 *       model package, the sync and async client interfaces and builders, the service metadata, operation table,
 *       schema holder and type registry, the endpoint parameters, provider and rules of a service with rules, one
 *       {@code Iterable} and {@code Publisher} per paginated operation, the sync and async waiter of a service with
 *       waiters, and the presigner, its request types and the utilities (with their support types) of the parity
 *       entries. Nothing else is public.</li>
 *   <li><em>Response hierarchy</em>: the direct superclass of every public {@code AwsResponse} and
 *       {@code AwsResponseMetadata} type, as in V2: {@code <Service>Response} extends {@code AwsResponse},
 *       {@code <Service>ResponseMetadata} extends {@code AwsResponseMetadata}, and the {@code <Operation>Response} of
 *       every non-unit output (plain, streaming or event stream) extends {@code <Service>Response}. A unit output has
 *       no response type, so nothing else extends either base.</li>
 *   <li><em>Response metadata</em>: the declared methods of {@code <Service>ResponseMetadata}: its
 *       {@code create(AwsResponseMetadata)} factory and V2's custom accessors of the service, which for the S3 profile
 *       are {@code cloudFrontId()}, {@code extendedRequestId()} and {@code requestId()}; the other profiles have
 *       none.</li>
 *   <li><em>Client methods</em>: the exact signatures (name, parameter erasures, return erasure) of the sync and async
 *       client interfaces: the typed request method and explicit-override method of every operation available to
 *       their callers (event stream operations are async only, as in V2), streaming request-body and
 *       response-transformer overloads, the consumer and no-argument overloads the parity entries preserve, the
 *       paginator methods, {@code waiter()}, {@code utilities()}, {@code create()}, {@code builder()},
 *       {@code serviceName()}, {@code serviceClientConfiguration()} and {@code close()}.</li>
 *   <li><em>Waiters, presigner, utilities</em>: the {@code waitUntil*} signatures of both waiters, the
 *       {@code presign*} signatures of the presigner (only for modeled presigned operations with input) and the
 *       entry-point method names of the utilities.</li>
 *   <li><em>Static descriptor references</em>: the operation table's public static descriptors are exactly the
 *       modeled operations, with the modeled service, input, output and (operation and service) errors, and are the
 *       instances {@code operations()} and {@code operation(ShapeId)} return; each operation's static binding holds
 *       the identical table descriptor; and the default sync (async) client plans exactly the sync (all) operations,
 *       each once, from those static bindings.</li>
 * </ul>
 * Every difference between the oracle and the generated surface is reported at once.
 *
 * <p>{@link #plantedDefectsAreReported} checks that the checks are not vacuous.
 *
 * <p><b>Tries.</b> 100 (the design's minimum). A small service takes a fraction of a second to generate and compile,
 * so the property runs in well under half a minute. Coverage checks require every profile, input and output kind,
 * paginated and waitable operations, and services with unreached shapes.
 */
class GeneratedSurfaceClosurePropertyTest {

    private static final Path WORK = Path.of("target", "generated-surface-closure");
    private static final String ROOT = "software.amazon.awssdk.test.codegen.surface";
    private static final String UNIT = "smithy.api#Unit";
    private static final List<String> PRELUDE = List.of("String", "Integer", "Long", "Boolean", "Timestamp", "Blob",
                                                        "Double");
    private static final List<String> DATA_NAMES = List.of("Anchor", "Beacon", "Cobalt", "Dynamo", "Ember", "Falcon",
                                                           "Garnet", "Hollow");
    private static final List<String> ERROR_NOUNS = List.of("Quota", "Access", "Conflict", "Throttle", "Capacity");
    private static final List<String> ERROR_SUFFIXES = List.of("", "Exception", "Fault", "NotFound", "Error");
    private static final List<String> VERBS = List.of("Get", "List", "Put", "Delete", "Describe", "Update", "Create",
                                                      "Start");
    private static final List<String> OPERATION_NOUNS = List.of("Order", "Report", "Queue", "Table", "Metric",
                                                                "Alarm", "Ledger");
    private static final List<String> MEMBER_NAMES = List.of("label", "size", "note", "flag", "amount", "marker",
                                                             "origin", "weight");
    private static final List<String> PLAIN_SERVICES = List.of("Orchard", "Lantern", "Quarry", "Meadow");

    // Feature: aws-sdk-java-v3, Property 7: Generated surface matches modeled and parity closures
    @Property(tries = 100, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 7: Generated surface matches modeled and parity closures")
    void generatedSurfaceMatchesModeledAndParityClosures(@ForAll("services") Service service) throws IOException {
        collectStatistics(service);
        Path directory = Files.createTempDirectory(Files.createDirectories(WORK), "try-");
        List<String> differences;
        try {
            differences = check(service, Generated.of(service, directory));
        } catch (RuntimeException | AssertionError e) {
            throw new AssertionError("Property 7 failed for " + service + "; model:\n" + service.render(), e);
        } finally {
            deleteRecursively(directory);
        }
        assertTrue(differences.isEmpty(), () -> "Property 7 failed for " + service + ":\n  "
                                                + String.join("\n  ", differences) + "\nmodel:\n" + service.render());
    }

    /**
     * The checks are not vacuous: each defect planted in otherwise correct generated sources is reported, and the
     * unmodified sources pass. The defects are a stray public model type, a stray response metadata subtype, an extra
     * response metadata accessor, a missing waiter accessor, an extra client overload, and an operation binding that
     * holds another operation's descriptor.
     */
    @Example
    void plantedDefectsAreReported() throws IOException {
        Service service = fixedService();
        Path directory = Files.createTempDirectory(Files.createDirectories(WORK), "mutants-");
        try {
            Generated generated = Generated.of(service, directory);
            List<String> clean = check(service, generated);
            assertTrue(clean.isEmpty(), () -> "unmodified sources: " + clean);

            String type = service.typeName();
            Path root = generated.sourceRoot().resolve(ROOT.replace('.', '/'));
            Path client = root.resolve(type + "Client.java");
            Path bindings = root.resolve(type + "OperationBindings.java");
            Path stray = root.resolve("model/StrayShape.java");
            Path strayMetadata = root.resolve("model/StrayMetadata.java");
            Path metadata = root.resolve("model/" + type + "ResponseMetadata.java");
            String bindingsText = Files.readString(bindings);
            Matcher constants = Pattern.compile("(\\w+Operations)\\.(\\w+), ").matcher(bindingsText);
            assertTrue(constants.find(), bindingsText);
            String firstReference = constants.group(0);
            assertTrue(constants.find(), bindingsText);
            String secondReference = constants.group(0);

            record Mutant(String name, Path file, Pattern from, String to, String expected) {
            }
            List<Mutant> mutants = List.of(
                new Mutant("stray public type", stray, Pattern.compile("\\A\\z"),
                           "package " + ROOT + ".model;\n\npublic final class StrayShape {\n}\n",
                           "unexpected public type " + ROOT + ".model.StrayShape"),
                new Mutant("stray response metadata subtype", strayMetadata, Pattern.compile("\\A\\z"),
                           "package " + ROOT + ".model;\n\npublic final class StrayMetadata extends "
                           + AwsResponseMetadata.class.getName() + " {\n    StrayMetadata() {\n"
                           + "        super(java.util.Map.of());\n    }\n}\n",
                           "unexpected response type hierarchy " + extendsClause(
                               ROOT + ".model.StrayMetadata", AwsResponseMetadata.class.getName())),
                new Mutant("extra response metadata accessor", metadata,
                           Pattern.compile("(?m)^    /\\*\\* The view of the raw metadata "),
                           "    public String strayId() {\n        return getValue(\"x-stray\");\n    }\n\n$0",
                           "unexpected response metadata method strayId():String"),
                new Mutant("missing waiter accessor", client,
                           Pattern.compile("(?m)^    default \\w+ waiter\\(\\) \\{\\n[^\\n]*\\n    \\}\\n"), "",
                           "missing sync client method waiter():" + type + "Waiter"),
                new Mutant("extra overload", client,
                           Pattern.compile("(?m)^    /\\*\\* The service's V2-compatible name, "),
                           "    default void strayOverload() {\n    }\n\n$0",
                           "unexpected sync client method strayOverload():void"),
                new Mutant("binding of another descriptor", bindings, Pattern.compile(Pattern.quote(firstReference)),
                           Matcher.quoteReplacement(secondReference),
                           "not the input and output of its descriptor"));
            for (Mutant mutant : mutants) {
                boolean existed = Files.exists(mutant.file());
                String original = existed ? Files.readString(mutant.file()) : "";
                Matcher matcher = mutant.from().matcher(original);
                assertTrue(matcher.find(), mutant.name() + ": " + mutant.from() + " not in\n" + original);
                Files.writeString(mutant.file(), matcher.replaceFirst(mutant.to()), StandardCharsets.UTF_8);
                try {
                    Path classes = directory.resolve("classes-" + mutant.name().replace(' ', '-'));
                    List<String> differences = check(service, generated.compile(classes));
                    assertTrue(differences.stream().anyMatch(d -> d.contains(mutant.expected())),
                               mutant.name() + ": expected a difference mentioning '" + mutant.expected() + "', got "
                               + differences);
                } finally {
                    if (existed) {
                        Files.writeString(mutant.file(), original, StandardCharsets.UTF_8);
                    } else {
                        Files.delete(mutant.file());
                    }
                }
            }
        } finally {
            deleteRecursively(directory);
        }
    }

    private static void collectStatistics(Service service) {
        Statistics.label("profile").collect(service.profile());
        Statistics.label("profile").coverage(c -> {
            for (Profile profile : Profile.values()) {
                c.check(profile).count(n -> n >= 3);
            }
        });
        Statistics.label("protocol").collect(service.protocol());
        Statistics.label("endpoint rules").collect(service.endpointRules());
        for (Operation operation : service.operations()) {
            Statistics.label("operation input").collect(operation.input());
            Statistics.label("operation output").collect(operation.output());
            Statistics.label("operation surface").collect(operation.pagination() != null ? "paginated"
                                                          : operation.waiter() != null ? "waitable" : "plain");
        }
        Statistics.label("operation input").coverage(c -> {
            for (InputKind kind : InputKind.values()) {
                c.check(kind).count(n -> n >= 10);
            }
        });
        Statistics.label("operation output").coverage(c -> {
            for (OutputKind kind : OutputKind.values()) {
                c.check(kind).count(n -> n >= 10);
            }
        });
        Statistics.label("operation surface").coverage(c -> {
            c.check("paginated").count(n -> n >= 10);
            c.check("waitable").count(n -> n >= 10);
        });
        Oracle oracle = Oracle.of(service);
        Statistics.label("unreached shapes").collect(oracle.unreached().isEmpty() ? "none" : "some");
        Statistics.label("unreached shapes").coverage(c -> c.check("some").count(n -> n >= 20));
        Statistics.label("presigned operations").collect(oracle.presigned().size());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The check
    // ---------------------------------------------------------------------------------------------------------------

    /** Every difference between the oracle of {@code service} and its generated surface. */
    private static List<String> check(Service service, Generated generated) throws IOException {
        Oracle oracle = Oracle.of(service);
        List<String> differences = new ArrayList<>();
        try (URLClassLoader loader = generated.compiled().loader()) {
            Surface surface = Surface.read(service, generated, loader);
            compare(differences, "public type", oracle.publicTypes(), surface.publicTypes());
            compare(differences, "response type hierarchy", oracle.responseHierarchy(), surface.responseHierarchy());
            compare(differences, "response metadata method", oracle.responseMetadataMethods(),
                    surface.responseMetadataMethods());
            compare(differences, "sync client method", oracle.syncMethods(), surface.syncMethods());
            compare(differences, "async client method", oracle.asyncMethods(), surface.asyncMethods());
            compare(differences, "sync waiter method", oracle.syncWaiterMethods(), surface.syncWaiterMethods());
            compare(differences, "async waiter method", oracle.asyncWaiterMethods(), surface.asyncWaiterMethods());
            compare(differences, "presigner method", oracle.presignerMethods(), surface.presignerMethods());
            compare(differences, "utilities method", oracle.utilitiesMethods(), surface.utilitiesMethods());
            checkDescriptors(differences, service, oracle, generated, loader);
        }
        return differences;
    }

    private static void compare(List<String> differences, String what, Set<String> expected, Set<String> actual) {
        for (String missing : difference(expected, actual)) {
            differences.add("missing " + what + " " + missing);
        }
        for (String unexpected : difference(actual, expected)) {
            differences.add("unexpected " + what + " " + unexpected);
        }
    }

    private static SortedSet<String> difference(Set<String> left, Set<String> right) {
        SortedSet<String> result = new TreeSet<>(left);
        result.removeAll(right);
        return result;
    }

    /**
     * The static descriptor references: the operation table, the static operation bindings, and the plans of the
     * default clients.
     */
    private static void checkDescriptors(List<String> differences, Service service, Oracle oracle,
                                         Generated generated, ClassLoader loader) throws IOException {
        String type = service.typeName();
        Class<?> table = load(loader, ROOT + ".internal.operation." + type + "Operations");
        if (table == null) {
            differences.add("no operation table");
            return;
        }
        Map<String, AwsOperationDescriptor> byId = new TreeMap<>();
        for (Field field : table.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || field.getType() != AwsOperationDescriptor.class) {
                continue;
            }
            if (!Modifier.isPublic(modifiers) || !Modifier.isFinal(modifiers)) {
                differences.add("operation table descriptor " + field.getName() + " is not public static final");
            }
            AwsOperationDescriptor descriptor = (AwsOperationDescriptor) staticValue(field);
            if (byId.put(descriptor.id().toString(), descriptor) != null) {
                differences.add("second operation table descriptor of " + descriptor.id());
            }
        }
        compare(differences, "operation table descriptor", oracle.descriptors().keySet(),
                new TreeSet<>(byId.keySet()));
        oracle.descriptors().forEach((id, expected) -> {
            AwsOperationDescriptor actual = byId.get(id);
            if (actual != null) {
                String observed = new DescriptorShape(actual.serviceId().toString(), actual.inputId().toString(),
                                                      actual.outputId().toString(),
                                                      new TreeSet<>(actual.errorIds().stream().map(ShapeId::toString)
                                                                          .toList())).toString();
                if (!observed.equals(expected.toString())) {
                    differences.add("descriptor of " + id + " is " + observed + ", modeled " + expected);
                }
            }
        });

        @SuppressWarnings("unchecked")
        List<AwsOperationDescriptor> listed = (List<AwsOperationDescriptor>) CompiledSources.invokeStatic(
            loader, table.getName(), "operations");
        Map<AwsOperationDescriptor, Boolean> tableInstances = new IdentityHashMap<>();
        byId.values().forEach(d -> tableInstances.put(d, true));
        Map<AwsOperationDescriptor, Boolean> listedInstances = new IdentityHashMap<>();
        listed.forEach(d -> listedInstances.put(d, true));
        if (listed.size() != byId.size() || !tableInstances.keySet().equals(listedInstances.keySet())) {
            differences.add("operations() " + listed + " are not the table's static descriptors " + byId.values());
        }
        for (Map.Entry<String, AwsOperationDescriptor> entry : byId.entrySet()) {
            Object found = invoke(table, "operation", ShapeId.class, ShapeId.from(entry.getKey()));
            if (!(found instanceof Optional<?> optional) || optional.orElse(null) != entry.getValue()) {
                differences.add("operation(" + entry.getKey() + ") is " + found + ", not the static descriptor");
            }
        }

        Class<?> bindings = load(loader, ROOT + "." + type + "OperationBindings");
        if (bindings == null) {
            differences.add("no operation bindings");
            return;
        }
        Map<String, String> bindingIds = new TreeMap<>();
        for (Field field : bindings.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || field.getType() != AwsOperation.class) {
                continue;
            }
            if (!Modifier.isFinal(modifiers)) {
                differences.add("operation binding " + field.getName() + " is not static final");
            }
            AwsOperation<?, ?> binding = (AwsOperation<?, ?>) staticValue(field);
            String id = binding.descriptor().id().toString();
            bindingIds.put(field.getName(), id);
            if (binding.descriptor() != byId.get(id)) {
                differences.add("operation binding " + field.getName() + " of " + id
                                + " does not hold the operation table's descriptor");
            }
            String built = builtShape(binding.inputBuilder()) + " -> " + builtShape(binding.outputBuilder());
            String described = binding.descriptor().inputId() + " -> " + binding.descriptor().outputId();
            if (!built.equals(described)) {
                differences.add("operation binding " + field.getName() + " builds " + built
                                + ", not the input and output of its descriptor " + described);
            }
            if (!binding.id().toString().equals(id)) {
                differences.add("operation binding " + field.getName() + " has ID " + binding.id()
                                + " but descriptor " + id);
            }
        }
        List<String> boundIds = new ArrayList<>(bindingIds.values());
        compare(differences, "operation binding", oracle.descriptors().keySet(), new TreeSet<>(boundIds));
        if (new TreeSet<>(boundIds).size() != boundIds.size()) {
            differences.add("operations bound more than once: " + bindingIds);
        }

        Path root = generated.sourceRoot().resolve(ROOT.replace('.', '/'));
        checkPlans(differences, "sync", root.resolve("Default" + type + "Client.java"), type, bindingIds,
                   oracle.syncOperationIds());
        checkPlans(differences, "async", root.resolve("Default" + type + "AsyncClient.java"), type, bindingIds,
                   oracle.descriptors().keySet());
    }

    /** The default client plans each of {@code expected} once, from its static operation binding. */
    private static void checkPlans(List<String> differences, String client, Path source, String type,
                                   Map<String, String> bindingIds, Set<String> expected) throws IOException {
        if (!Files.exists(source)) {
            differences.add("no default " + client + " client");
            return;
        }
        Matcher plan = Pattern.compile("runtime\\.plan\\(" + type + "OperationBindings\\.(\\w+)\\)")
                              .matcher(Files.readString(source));
        List<String> planned = new ArrayList<>();
        while (plan.find()) {
            planned.add(bindingIds.getOrDefault(plan.group(1), "unknown binding " + plan.group(1)));
        }
        compare(differences, "default " + client + " client plan of", new TreeSet<>(expected), new TreeSet<>(planned));
        if (new TreeSet<>(planned).size() != planned.size()) {
            differences.add("default " + client + " client plans an operation more than once: " + planned);
        }
    }

    /** The shape ID of what {@code builders} build. */
    private static String builtShape(Supplier<? extends ShapeBuilder<?>> builders) {
        return builders.get().schema().id().toString();
    }

    private static Object staticValue(Field field) {
        try {
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field + " is not readable", e);
        }
    }

    private static Object invoke(Class<?> type, String method, Class<?> parameter,
                                 Object argument) {
        try {
            return type.getMethod(method, parameter).invoke(null, argument);
        } catch (ReflectiveOperationException e) {
            return e;
        }
    }

    private static Class<?> load(ClassLoader loader, String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The generated surface
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * One generation of a service and its compiled classes.
     *
     * @param sourceRoot the generated source root
     * @param compiled the compiled generated sources
     * @param classes the directory of the compiled classes
     */
    record Generated(Path sourceRoot, CompiledSources compiled, Path classes) {

        static Generated of(Service service, Path directory) throws IOException {
            Path models = Files.createDirectories(directory.resolve("model"));
            Files.writeString(models.resolve("service.smithy"), service.render(), StandardCharsets.UTF_8);
            AwsCodegenResult result = AwsCodegen.generate(
                new AwsCodegenRequest(service.shapeId(), ROOT, models, directory.resolve("out/src"))
                    .withResourceOutputDirectory(directory.resolve("out/resources")));
            Path classes = directory.resolve("classes");
            return new Generated(result.sourceRoot(), CompiledSources.compile(result.sourceRoot(), classes), classes);
        }

        /** The same sources compiled again, after they were changed, into {@code classes}. */
        Generated compile(Path classes) {
            return new Generated(sourceRoot, CompiledSources.compile(sourceRoot, classes), classes);
        }

        /** Binary names of every compiled top-level class. */
        SortedSet<String> topLevelClasses() throws IOException {
            SortedSet<String> names = new TreeSet<>();
            try (Stream<Path> walk = Files.walk(classes)) {
                walk.filter(p -> p.toString().endsWith(".class"))
                    .map(p -> classes.relativize(p).toString().replace(p.getFileSystem().getSeparator(), "."))
                    .map(p -> p.substring(0, p.length() - ".class".length()))
                    .filter(name -> !name.contains("$"))
                    .forEach(names::add);
            }
            return names;
        }
    }

    /**
     * The observed generated surface, read reflectively from the compiled classes.
     *
     * @param publicTypes the public top-level types
     * @param responseHierarchy {@code <type> extends <direct superclass>} of every public top-level type that is an
     *     {@code AwsResponse} or an {@code AwsResponseMetadata}
     * @param responseMetadataMethods the signatures of the {@code <Service>ResponseMetadata} view's declared methods
     * @param syncMethods the signatures of the sync client interface's declared methods
     * @param asyncMethods the signatures of the async client interface's declared methods
     * @param syncWaiterMethods the {@code waitUntil*} signatures of the sync waiter
     * @param asyncWaiterMethods the {@code waitUntil*} signatures of the async waiter
     * @param presignerMethods the {@code presign*} signatures of the presigner
     * @param utilitiesMethods the names of the utilities' instance methods
     */
    record Surface(SortedSet<String> publicTypes, SortedSet<String> responseHierarchy,
                   SortedSet<String> responseMetadataMethods, SortedSet<String> syncMethods,
                   SortedSet<String> asyncMethods, SortedSet<String> syncWaiterMethods,
                   SortedSet<String> asyncWaiterMethods, SortedSet<String> presignerMethods,
                   SortedSet<String> utilitiesMethods) {

        static Surface read(Service service, Generated generated, ClassLoader loader) throws IOException {
            SortedSet<String> publicTypes = new TreeSet<>();
            SortedSet<String> responseHierarchy = new TreeSet<>();
            for (String name : generated.topLevelClasses()) {
                Class<?> type = load(loader, name);
                if (type != null && Modifier.isPublic(type.getModifiers())) {
                    publicTypes.add(name);
                    if (AwsResponse.class.isAssignableFrom(type) || AwsResponseMetadata.class.isAssignableFrom(type)) {
                        responseHierarchy.add(extendsClause(name, type.getSuperclass().getName()));
                    }
                }
            }
            String type = service.typeName();
            Profile profile = service.profile();
            return new Surface(
                publicTypes,
                responseHierarchy,
                signatures(loader, ROOT + ".model." + type + "ResponseMetadata", name -> true),
                signatures(loader, ROOT + "." + type + "Client", name -> true),
                signatures(loader, ROOT + "." + type + "AsyncClient", name -> true),
                signatures(loader, ROOT + ".waiters." + type + "Waiter", name -> name.startsWith("waitUntil")),
                signatures(loader, ROOT + ".waiters." + type + "AsyncWaiter", name -> name.startsWith("waitUntil")),
                profile.presigner() == null ? new TreeSet<>()
                                            : signatures(loader, ROOT + ".presigner." + profile.presigner().type(),
                                                         name -> name.startsWith("presign")),
                profile.utilities() == null ? new TreeSet<>()
                                            : instanceMethodNames(loader, ROOT + "." + profile.utilities().type()));
        }

        /** {@code name(parameter erasures):return erasure} of every public declared method, by simple names. */
        private static SortedSet<String> signatures(ClassLoader loader, String className,
                                                    Predicate<String> names) {
            SortedSet<String> signatures = new TreeSet<>();
            Class<?> type = load(loader, className);
            if (type == null) {
                return signatures;
            }
            for (Method method : type.getDeclaredMethods()) {
                if (method.isSynthetic() || method.isBridge() || !Modifier.isPublic(method.getModifiers())
                    || !names.test(method.getName())) {
                    continue;
                }
                signatures.add(signature(method.getName(), method.getReturnType().getSimpleName(),
                                         Stream.of(method.getParameterTypes()).map(Class::getSimpleName).toList()));
            }
            return signatures;
        }

        private static SortedSet<String> instanceMethodNames(ClassLoader loader, String className) {
            SortedSet<String> names = new TreeSet<>();
            Class<?> type = load(loader, className);
            if (type != null) {
                for (Method method : type.getDeclaredMethods()) {
                    if (!method.isSynthetic() && Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers())) {
                        names.add(method.getName());
                    }
                }
            }
            return names;
        }
    }

    static String signature(String name, String returnType, List<String> parameters) {
        return name + "(" + String.join(",", parameters) + "):" + returnType;
    }

    /** {@code type extends superclass}, by binary names. */
    static String extendsClause(String type, String superclass) {
        return type + " extends " + superclass;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The oracle
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * The modeled service, input, output and errors of one operation descriptor.
     *
     * @param service absolute service shape ID
     * @param input absolute input shape ID ({@code smithy.api#Unit} when there is none)
     * @param output absolute output shape ID ({@code smithy.api#Unit} when there is none)
     * @param errors absolute IDs of the operation's and the service's errors
     */
    record DescriptorShape(String service, String input, String output, SortedSet<String> errors) {
    }

    /**
     * The expected surface of a service.
     *
     * @param publicTypes every public top-level type
     * @param responseHierarchy {@code <type> extends <direct superclass>} of every public response and response
     *     metadata type
     * @param responseMetadataMethods the {@code <Service>ResponseMetadata} view's method signatures
     * @param syncMethods the sync client interface's method signatures
     * @param asyncMethods the async client interface's method signatures
     * @param syncWaiterMethods the sync waiter's {@code waitUntil*} signatures
     * @param asyncWaiterMethods the async waiter's {@code waitUntil*} signatures
     * @param presignerMethods the presigner's {@code presign*} signatures
     * @param utilitiesMethods the utilities' entry-point method names
     * @param descriptors the expected descriptor of every operation, by absolute operation ID
     * @param syncOperationIds the operations available to synchronous callers
     * @param presigned the presigned operations
     * @param unreached the names of data shapes and errors outside the closure
     */
    record Oracle(SortedSet<String> publicTypes, SortedSet<String> responseHierarchy,
                  SortedSet<String> responseMetadataMethods, SortedSet<String> syncMethods,
                  SortedSet<String> asyncMethods, SortedSet<String> syncWaiterMethods,
                  SortedSet<String> asyncWaiterMethods, SortedSet<String> presignerMethods,
                  SortedSet<String> utilitiesMethods, SortedMap<String, DescriptorShape> descriptors,
                  SortedSet<String> syncOperationIds, List<String> presigned, SortedSet<String> unreached) {

        static Oracle of(Service service) {
            String type = service.typeName();
            String ns = service.namespace();
            List<ParityEntry> parity = service.parityEntries();
            SortedSet<String> types = new TreeSet<>();
            SortedSet<String> hierarchy = new TreeSet<>();
            SortedSet<String> metadataMethods = new TreeSet<>();
            SortedSet<String> sync = new TreeSet<>();
            SortedSet<String> async = new TreeSet<>();
            SortedSet<String> syncWaiter = new TreeSet<>();
            SortedSet<String> asyncWaiter = new TreeSet<>();
            SortedSet<String> presigner = new TreeSet<>();
            SortedSet<String> utilities = new TreeSet<>();
            SortedMap<String, DescriptorShape> descriptors = new TreeMap<>();
            SortedSet<String> syncIds = new TreeSet<>();

            // The model closure.
            Set<String> closure = closure(service);
            SortedSet<String> unreached = new TreeSet<>();
            for (DataShape shape : service.data()) {
                if (!closure.contains(shape.name())) {
                    unreached.add(shape.name());
                } else if (shape.kind().generatesType()) {
                    types.add(ROOT + ".model." + shape.name());
                }
            }
            for (ErrorShape error : service.errors()) {
                if (closure.contains(error.name())) {
                    types.add(ROOT + ".model." + RandomSmithyModels.v2ExceptionName(error.name(), type));
                } else {
                    unreached.add(error.name());
                }
            }
            types.add(ROOT + ".model." + type + "Exception");

            // The service's base response and its V2-compatible metadata view, with V2's custom accessors.
            String baseResponse = ROOT + ".model." + type + "Response";
            String metadata = ROOT + ".model." + type + "ResponseMetadata";
            types.add(baseResponse);
            types.add(metadata);
            hierarchy.add(extendsClause(baseResponse, AwsResponse.class.getName()));
            hierarchy.add(extendsClause(metadata, AwsResponseMetadata.class.getName()));
            metadataMethods.add(signature("create", type + "ResponseMetadata", List.of("AwsResponseMetadata")));
            service.profile().responseMetadata().forEach(m -> metadataMethods.add(signature(m, "String", List.of())));

            // The service's fixed public types.
            boolean asyncClient = preserved(parity, "async client");
            for (String name : List.of(type + "Client", type + "ClientBuilder", type + "ServiceMetadata",
                                       "internal.operation." + type + "Operations", "model.internal." + type
                                                                                    + "Schemas",
                                       "model.internal." + type + "TypeRegistry")) {
                types.add(ROOT + "." + name);
            }
            if (asyncClient) {
                types.add(ROOT + "." + type + "AsyncClient");
                types.add(ROOT + "." + type + "AsyncClientBuilder");
            }
            if (service.endpointRules()) {
                types.add(ROOT + ".endpoints." + type + "EndpointParams");
                types.add(ROOT + ".endpoints." + type + "EndpointProvider");
                types.add(ROOT + ".endpoints.internal." + type + "EndpointRules");
            }

            // Operations.
            SortedSet<String> serviceErrors = new TreeSet<>();
            service.serviceErrors().forEach(e -> serviceErrors.add(ns + "#" + e));
            boolean waiters = false;
            for (Operation operation : service.operations()) {
                String name = operation.name();
                String method = method(name);
                String request = operation.input() == InputKind.UNIT ? "Unit" : name + "Request";
                String response = operation.output() == OutputKind.UNIT ? "Unit" : name + "Response";
                if (operation.input() != InputKind.UNIT) {
                    types.add(ROOT + ".model." + request);
                }
                if (operation.output() != OutputKind.UNIT) {
                    // Every modeled output (plain, streaming or event stream) extends the service's base response;
                    // a unit output has no response type.
                    types.add(ROOT + ".model." + response);
                    hierarchy.add(extendsClause(ROOT + ".model." + response, baseResponse));
                }
                SortedSet<String> errors = new TreeSet<>(serviceErrors);
                operation.errors().forEach(e -> errors.add(ns + "#" + e));
                descriptors.put(ns + "#" + name, new DescriptorShape(
                    service.shapeId(), operation.input() == InputKind.UNIT ? UNIT : ns + "#" + name + "Input",
                    operation.output() == OutputKind.UNIT ? UNIT : ns + "#" + name + "Output", errors));
                boolean consumer = preserved(parity, "consumer overload " + name);
                boolean noArgument = preserved(parity, "no-argument overload " + name);
                if (operation.eventStream()) {
                    // As in V2, event stream operations are async only.
                    List<String> rest = new ArrayList<>();
                    if (operation.input() == InputKind.EVENTS) {
                        rest.add("Publisher");
                    }
                    if (operation.output() == OutputKind.EVENTS) {
                        rest.add(name + "ResponseHandler");
                        types.add(ROOT + ".model." + name + "ResponseHandler");
                    }
                    if (operation.input() == InputKind.UNIT) {
                        async.add(signature(method, "CompletableFuture", rest));
                    } else {
                        async.add(signature(method, "CompletableFuture", concat(request, rest)));
                        if (consumer) {
                            async.add(signature(method, "CompletableFuture", concat("Consumer", rest)));
                        }
                    }
                    continue;
                }
                syncIds.add(ns + "#" + name);
                operationMethods(sync, operation, method, request, response, false, consumer, noArgument);
                operationMethods(async, operation, method, request, response, true, consumer, noArgument);
                if (operation.pagination() != null) {
                    types.add(ROOT + ".paginators." + name + "Iterable");
                    types.add(ROOT + ".paginators." + name + "Publisher");
                    sync.add(signature(method + "Paginator", name + "Iterable", List.of(request)));
                    sync.add(signature(method + "Paginator", name + "Iterable", List.of("Consumer")));
                    async.add(signature(method + "Paginator", name + "Publisher", List.of(request)));
                    async.add(signature(method + "Paginator", name + "Publisher", List.of("Consumer")));
                }
                if (operation.waiter() != null) {
                    waiters = true;
                    String wait = "waitUntil" + operation.waiter();
                    syncWaiter.add(signature(wait, "WaiterResponse", List.of(request, "WaiterOverrideConfiguration")));
                    syncWaiter.add(signature(wait, "WaiterResponse", List.of(request)));
                    syncWaiter.add(signature(wait, "WaiterResponse", List.of("Consumer")));
                    asyncWaiter.add(signature(wait, "CompletableFuture",
                                              List.of(request, "WaiterOverrideConfiguration")));
                    asyncWaiter.add(signature(wait, "CompletableFuture", List.of(request)));
                    asyncWaiter.add(signature(wait, "CompletableFuture", List.of("Consumer")));
                }
            }

            // Client-level members.
            for (boolean isAsync : List.of(false, true)) {
                SortedSet<String> methods = isAsync ? async : sync;
                String client = type + (isAsync ? "AsyncClient" : "Client");
                methods.add(signature("create", client, List.of()));
                methods.add(signature("builder", client + "Builder", List.of()));
                methods.add(signature("serviceName", "String", List.of()));
                methods.add(signature("serviceClientConfiguration", "SdkClientConfiguration", List.of()));
                methods.add(signature("close", "void", List.of()));
                if (waiters) {
                    methods.add(signature("waiter", type + (isAsync ? "AsyncWaiter" : "Waiter"), List.of()));
                }
            }
            if (waiters) {
                types.add(ROOT + ".waiters." + type + "Waiter");
                types.add(ROOT + ".waiters." + type + "AsyncWaiter");
            }
            if (!asyncClient) {
                async.clear();
            }

            // Presigner and utilities of the parity entries.
            List<String> presigned = new ArrayList<>();
            PresignerEntry presignerEntry = service.profile().presigner();
            if (presignerEntry != null && preserved(parity, "presigner " + presignerEntry.type())) {
                for (String name : presignerEntry.operations()) {
                    service.operations().stream()
                           .filter(o -> o.name().equals(name) && o.input() != InputKind.UNIT)
                           .findFirst().ifPresent(o -> presigned.add(name));
                }
                if (!presigned.isEmpty()) {
                    types.add(ROOT + ".presigner." + presignerEntry.type());
                    for (String name : presigned) {
                        types.add(ROOT + ".presigner.model." + name + "PresignRequest");
                        types.add(ROOT + ".presigner.model.Presigned" + name + "Request");
                        presigner.add(signature("presign" + name, "Presigned" + name + "Request",
                                                List.of(name + "PresignRequest")));
                        presigner.add(signature("presign" + name, "Presigned" + name + "Request",
                                                List.of("Consumer")));
                    }
                }
            }
            UtilitiesEntry utilitiesEntry = service.profile().utilities();
            if (utilitiesEntry != null && preserved(parity, "utilities " + utilitiesEntry.type())) {
                types.add(ROOT + "." + utilitiesEntry.type());
                utilitiesEntry.supportTypes().forEach(t -> types.add(ROOT + "." + t));
                utilities.addAll(utilitiesEntry.methods());
                sync.add(signature("utilities", utilitiesEntry.type(), List.of()));
            }
            return new Oracle(types, hierarchy, metadataMethods, sync, async, syncWaiter, asyncWaiter, presigner,
                              utilities, descriptors, syncIds, presigned, unreached);
        }

        /** The request methods of a non-event-stream operation (V2's shape, with the explicit-override form). */
        private static void operationMethods(SortedSet<String> methods, Operation operation, String method,
                                             String request, String response, boolean async, boolean consumer,
                                             boolean noArgument) {
            String result = async ? "CompletableFuture" : response;
            String override = "AwsRequestOverrideConfiguration";
            if (operation.input() == InputKind.STREAMING) {
                String body = async ? "AsyncRequestBody" : "RequestBody";
                methods.add(signature(method, result, List.of(request, body, override)));
                methods.add(signature(method, result, List.of(request, body)));
                if (consumer) {
                    methods.add(signature(method, result, List.of("Consumer", body)));
                }
            } else if (operation.input() == InputKind.UNIT) {
                methods.add(signature(method, result, List.of(override)));
                methods.add(signature(method, result, List.of()));
            } else {
                methods.add(signature(method, result, List.of(request, override)));
                methods.add(signature(method, result, List.of(request)));
                if (consumer) {
                    methods.add(signature(method, result, List.of("Consumer")));
                }
                if (noArgument) {
                    methods.add(signature(method, result, List.of()));
                }
            }
            if (operation.output() == OutputKind.STREAMING) {
                String transformer = async ? "AsyncResponseTransformer" : "ResponseTransformer";
                String transformed = async ? "CompletableFuture" : "Object";
                methods.add(signature(method, transformed, List.of(request, transformer)));
                if (consumer) {
                    methods.add(signature(method, transformed, List.of("Consumer", transformer)));
                }
            }
        }

        private static boolean preserved(List<ParityEntry> parity, String surface) {
            return parity.contains(new ParityEntry(surface, Classification.PRESERVED))
                   || parity.contains(new ParityEntry(surface, Classification.ADAPTED));
        }

        /** Names of every data shape and error reachable from the operations' members and errors. */
        private static Set<String> closure(Service service) {
            Map<String, List<String>> edges = new LinkedHashMap<>();
            for (DataShape shape : service.data()) {
                edges.put(shape.name(), shape.members().stream().map(Ref::target).toList());
            }
            for (ErrorShape error : service.errors()) {
                edges.put(error.name(), error.detail() == null ? List.of() : List.of(error.detail()));
            }
            Deque<String> pending = new ArrayDeque<>(service.serviceErrors());
            for (Operation operation : service.operations()) {
                operation.inputMembers().forEach(m -> pending.add(m.target()));
                operation.outputMembers().forEach(m -> pending.add(m.target()));
                pending.addAll(operation.errors());
            }
            Set<String> closure = new TreeSet<>();
            while (!pending.isEmpty()) {
                String name = pending.pop();
                if (edges.containsKey(name) && closure.add(name)) {
                    pending.addAll(edges.get(name));
                }
            }
            return closure;
        }

        private static List<String> concat(String first, List<String> rest) {
            List<String> all = new ArrayList<>();
            all.add(first);
            all.addAll(rest);
            return all;
        }
    }

    /** V2's operation method name of a PascalCase operation name made of capitalized words. */
    static String method(String operationName) {
        return Character.toLowerCase(operationName.charAt(0)) + operationName.substring(1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Services
    // ---------------------------------------------------------------------------------------------------------------

    /** A Parity Manifest classification. */
    enum Classification {
        PRESERVED, ADAPTED, EXCLUDED
    }

    /**
     * One Parity Manifest entry of a generated surface.
     *
     * @param surface the surface ({@code async client}, {@code consumer overload <Operation>},
     *     {@code no-argument overload <Operation>}, {@code presigner <Type>}, {@code utilities <Type>})
     * @param classification its classification
     */
    record ParityEntry(String surface, Classification classification) {
    }

    /**
     * A V2 presigner of the baseline.
     *
     * @param type the presigner interface, in {@code <root>.presigner}
     * @param operations the presigned operations
     */
    record PresignerEntry(String type, List<String> operations) {
    }

    /**
     * V2 service utilities of the baseline.
     *
     * @param type the utilities interface, in the root package
     * @param methods the entry-point methods
     * @param supportTypes the public request and result types of the entry points, relative to the root package
     */
    record UtilitiesEntry(String type, List<String> methods, List<String> supportTypes) {
    }

    /**
     * The service identities whose parity entries or response metadata differ: the V2 presigners and utilities, and
     * V2's custom response metadata accessors ({@code customResponseMetadata}; of the profiles, only S3 has them).
     */
    enum Profile {
        PLAIN(null, null, null, null, null, null, List.of()),
        S3("com.amazonaws.s3", "AmazonS3", "S3", "S3",
           new PresignerEntry("S3Presigner", List.of("GetObject", "HeadObject", "HeadBucket", "PutObject",
                                                     "DeleteObject", "CreateMultipartUpload", "UploadPart",
                                                     "CompleteMultipartUpload", "AbortMultipartUpload")),
           new UtilitiesEntry("S3Utilities", List.of("getUrl", "parseUri"), List.of("S3Uri", "model.GetUrlRequest")),
           List.of("cloudFrontId", "extendedRequestId", "requestId")),
        RDS("com.amazonaws.rds", "AmazonRDSv19", "RDS", "Rds", null,
            new UtilitiesEntry("RdsUtilities", List.of("generateAuthenticationToken"),
                               List.of("model.GenerateAuthenticationTokenRequest")),
            List.of()),
        DSQL("com.amazonaws.dsql", "DSQL", "DSQL", "Dsql", null,
             new UtilitiesEntry("DsqlUtilities", List.of("generateDbConnectAuthToken",
                                                         "generateDbConnectAdminAuthToken"),
                                List.of("model.GenerateAuthTokenRequest")),
             List.of()),
        CLOUDFRONT("com.amazonaws.cloudfront", "Cloudfront2020_05_31", "CloudFront", "CloudFront", null,
                   new UtilitiesEntry("CloudFrontUtilities",
                                      List.of("getSignedUrlWithCannedPolicy", "getSignedUrlWithCustomPolicy",
                                              "getCookiesForCannedPolicy", "getCookiesForCustomPolicy"),
                                      List.of("model.CannedSignerRequest", "model.CustomSignerRequest",
                                              "url.SignedUrl", "cookie.CookiesForCannedPolicy",
                                              "cookie.CookiesForCustomPolicy", "utils.SigningHashAlgorithm")),
                   List.of());

        private final String namespace;
        private final String shapeName;
        private final String sdkId;
        private final String typeName;
        private final PresignerEntry presigner;
        private final UtilitiesEntry utilities;
        private final List<String> responseMetadata;

        Profile(String namespace, String shapeName, String sdkId, String typeName, PresignerEntry presigner,
                UtilitiesEntry utilities, List<String> responseMetadata) {
            this.namespace = namespace;
            this.shapeName = shapeName;
            this.sdkId = sdkId;
            this.typeName = typeName;
            this.presigner = presigner;
            this.utilities = utilities;
            this.responseMetadata = responseMetadata;
        }

        PresignerEntry presigner() {
            return presigner;
        }

        UtilitiesEntry utilities() {
            return utilities;
        }

        /** V2's custom {@code String} accessors of {@code <Service>ResponseMetadata}, beyond the inherited ones. */
        List<String> responseMetadata() {
            return responseMetadata;
        }
    }

    enum Protocol {
        AWS_JSON_1_0("@aws.protocols#awsJson1_0", false, false),
        AWS_JSON_1_1("@aws.protocols#awsJson1_1", false, false),
        REST_JSON_1("@aws.protocols#restJson1", true, true),
        REST_XML("@aws.protocols#restXml", true, false);

        private final String trait;
        private final boolean httpBindings;
        private final boolean eventStreams;

        Protocol(String trait, boolean httpBindings, boolean eventStreams) {
            this.trait = trait;
            this.httpBindings = httpBindings;
            this.eventStreams = eventStreams;
        }
    }

    /** An operation input. */
    enum InputKind {
        /** No input ({@code smithy.api#Unit}). */
        UNIT,
        /** An input structure without members. */
        EMPTY,
        /** Members, none required. */
        OPTIONAL,
        /** Members, at least one required. */
        REQUIRED,
        /** A required streaming-blob payload (HTTP-binding protocols). */
        STREAMING,
        /** An event stream payload (REST JSON). */
        EVENTS
    }

    /** An operation output. */
    enum OutputKind {
        UNIT, EMPTY, MEMBERS, STREAMING, EVENTS
    }

    enum DataKind {
        STRUCTURE, UNION, ENUM, INT_ENUM, LIST, MAP, EVENT_UNION;

        /** Whether the shape is a generated model type (collections are not). */
        boolean generatesType() {
            return this != LIST && this != MAP;
        }
    }

    /**
     * A member.
     *
     * @param name member name
     * @param target target shape name (a prelude shape, data shape or error)
     * @param required whether it is {@code @required}
     * @param binding its HTTP binding ({@code header}, {@code payload}) or {@code null}
     */
    record Ref(String name, String target, boolean required, String binding) {
        String render() {
            StringBuilder out = new StringBuilder();
            if (required) {
                out.append("@required ");
            }
            if ("header".equals(binding)) {
                out.append("@httpHeader(\"x-").append(name.toLowerCase(Locale.ROOT)).append("\") ");
            } else if ("payload".equals(binding)) {
                out.append("@httpPayload ");
            }
            return out.append(name).append(": ").append(target).append('\n').toString();
        }
    }

    /**
     * A data shape.
     *
     * @param name shape name
     * @param kind shape type
     * @param members members (of a structure or union; the {@code member} of a list; the {@code value} of a map)
     */
    record DataShape(String name, DataKind kind, List<Ref> members) {
        String render() {
            return switch (kind) {
                case STRUCTURE -> "structure " + name + " {\n" + renderMembers(members, "    ") + "}\n";
                case UNION -> "union " + name + " {\n" + renderMembers(members, "    ") + "}\n";
                case EVENT_UNION -> "@streaming\nunion " + name + " {\n" + renderMembers(members, "    ") + "}\n";
                case ENUM -> "enum " + name + " {\n    RED\n    GREEN\n}\n";
                case INT_ENUM -> "intEnum " + name + " {\n    LOW = 1\n    HIGH = 2\n}\n";
                case LIST -> "list " + name + " {\n    member: " + members.get(0).target() + "\n}\n";
                case MAP -> "map " + name + " {\n    key: String\n    value: " + members.get(0).target() + "\n}\n";
            };
        }
    }

    /**
     * A modeled error.
     *
     * @param name shape name
     * @param detail target of its {@code detail} member, or {@code null}
     */
    record ErrorShape(String name, String detail) {
        String render() {
            return "@error(\"client\")\nstructure " + name + " {\n    message: String\n"
                   + (detail == null ? "" : "    detail: " + detail + "\n") + "}\n";
        }
    }

    /**
     * Pagination of an operation by {@code nextToken}.
     *
     * @param pageSize whether a {@code maxResults} page size is modeled
     * @param items whether an {@code entries} items list is modeled
     */
    record Pagination(boolean pageSize, boolean items) {
    }

    /**
     * An operation.
     *
     * @param name operation name
     * @param input input kind
     * @param output output kind
     * @param inputMembers input members (with pagination members)
     * @param outputMembers output members (with pagination members)
     * @param pagination its pagination, or {@code null}
     * @param waiter the name of its waiter, or {@code null}
     * @param errors its errors
     */
    record Operation(String name, InputKind input, OutputKind output, List<Ref> inputMembers, List<Ref> outputMembers,
                     Pagination pagination, String waiter, List<String> errors) {

        boolean eventStream() {
            return input == InputKind.EVENTS || output == OutputKind.EVENTS;
        }

        String render(Protocol protocol, boolean bucketParameter) {
            StringBuilder out = new StringBuilder();
            if (bucketParameter) {
                // Binds the rule set's Bucket parameter, which S3's utilities need, whatever the input.
                out.append("@smithy.rules#staticContextParams(Bucket: { value: \"bucket\" })\n");
            }
            if (protocol.httpBindings) {
                out.append("@http(method: \"POST\", uri: \"/").append(name.toLowerCase(Locale.ROOT)).append("\")\n");
            }
            if (pagination != null) {
                out.append("@paginated(inputToken: \"nextToken\", outputToken: \"nextToken\"")
                   .append(pagination.pageSize() ? ", pageSize: \"maxResults\"" : "")
                   .append(pagination.items() ? ", items: \"entries\"" : "").append(")\n");
            }
            if (waiter != null) {
                out.append("@smithy.waiters#waitable(").append(waiter)
                   .append(": { acceptors: [{ state: \"success\", matcher: { success: true } }] })\n");
            }
            out.append("operation ").append(name).append(" {\n");
            if (input != InputKind.UNIT) {
                out.append("    input := {\n").append(renderMembers(inputMembers, "        ")).append("    }\n");
            }
            if (output != OutputKind.UNIT) {
                out.append("    output := {\n").append(renderMembers(outputMembers, "        ")).append("    }\n");
            }
            if (!errors.isEmpty()) {
                out.append("    errors: ").append(errors).append('\n');
            }
            return out.append("}\n").toString();
        }
    }

    private static String renderMembers(List<Ref> members, String indent) {
        return members.stream().map(m -> indent + m.render()).collect(Collectors.joining());
    }

    /**
     * One generated service.
     *
     * @param profile its parity profile
     * @param namespace its namespace
     * @param shapeName its service shape name
     * @param sdkId its {@code aws.api#service} SDK ID
     * @param typeName the V2 type-name stem of its clients ({@code <typeName>Client})
     * @param protocol its protocol
     * @param endpointRules whether it has an endpoint rule set
     * @param operations its operations
     * @param serviceErrors its service errors
     * @param errors every error shape of the model (reached or not)
     * @param data every data shape of the model (reached or not), with the event stream unions and events
     */
    record Service(Profile profile, String namespace, String shapeName, String sdkId, String typeName,
                   Protocol protocol, boolean endpointRules, List<Operation> operations, List<String> serviceErrors,
                   List<ErrorShape> errors, List<DataShape> data) {

        String shapeId() {
            return namespace + "#" + shapeName;
        }

        /**
         * The parity entries of the V2 Launch Parity Baseline for this service: its async client, the consumer
         * overload of every operation with modeled input, the no-argument overload of every operation with modeled
         * input and no required input member, and the profile's presigner and utilities, each preserved.
         */
        List<ParityEntry> parityEntries() {
            List<ParityEntry> entries = new ArrayList<>();
            entries.add(new ParityEntry("async client", Classification.PRESERVED));
            for (Operation operation : operations) {
                if (operation.input() == InputKind.UNIT) {
                    continue;
                }
                entries.add(new ParityEntry("consumer overload " + operation.name(), Classification.PRESERVED));
                boolean required = operation.inputMembers().stream().anyMatch(Ref::required);
                if (!required && !operation.eventStream() && operation.input() != InputKind.STREAMING) {
                    entries.add(new ParityEntry("no-argument overload " + operation.name(),
                                                Classification.PRESERVED));
                }
            }
            if (profile.presigner() != null) {
                entries.add(new ParityEntry("presigner " + profile.presigner().type(), Classification.PRESERVED));
            }
            if (profile.utilities() != null) {
                entries.add(new ParityEntry("utilities " + profile.utilities().type(), Classification.PRESERVED));
            }
            return entries;
        }

        /** The model file. */
        String render() {
            String prefix = typeName.toLowerCase(Locale.ROOT);
            StringBuilder out = new StringBuilder("$version: \"2\"\nnamespace ").append(namespace).append("\n\n");
            out.append(protocol.trait).append('\n');
            out.append("@aws.auth#sigv4(name: \"").append(prefix).append("\")\n");
            out.append("@aws.api#service(sdkId: \"").append(sdkId).append("\", endpointPrefix: \"").append(prefix)
               .append("\")\n");
            if (endpointRules) {
                out.append(rules(prefix));
            }
            out.append("service ").append(shapeName).append(" {\n    version: \"2024-01-01\"\n    operations: ")
               .append(operations.stream().map(Operation::name).toList()).append('\n');
            if (!serviceErrors.isEmpty()) {
                out.append("    errors: ").append(serviceErrors).append('\n');
            }
            out.append("}\n");
            for (Operation operation : operations) {
                out.append('\n').append(operation.render(protocol, endpointRules && profile == Profile.S3));
            }
            for (DataShape shape : data) {
                out.append('\n').append(shape.render());
            }
            for (ErrorShape error : errors) {
                out.append('\n').append(error.render());
            }
            if (operations.stream().anyMatch(o -> o.input() == InputKind.STREAMING
                                                  || o.output() == OutputKind.STREAMING)) {
                out.append("\n@streaming\nblob StreamingBlob\n");
            }
            if (operations.stream().anyMatch(o -> o.pagination() != null && o.pagination().items())) {
                out.append("\nlist EntryList {\n    member: String\n}\n");
            }
            return out.toString();
        }

        /** A minimal rule set; S3's has the {@code Bucket} parameter its utilities need. */
        private String rules(String prefix) {
            String bucket = profile == Profile.S3
                            ? "        Bucket: { type: \"String\", required: false, documentation: \"The bucket.\" }\n"
                            : "";
            return "@smithy.rules#endpointRuleSet({\n"
                   + "    version: \"1.0\"\n"
                   + "    parameters: {\n"
                   + bucket
                   + "        Region: { type: \"String\", builtIn: \"AWS::Region\", required: true, "
                   + "documentation: \"The region.\" }\n"
                   + "    }\n"
                   + "    rules: [\n"
                   + "        { conditions: [], endpoint: { url: \"https://" + prefix
                   + ".{Region}.amazonaws.com\" }, type: \"endpoint\" }\n"
                   + "    ]\n"
                   + "})\n";
        }

        @Override
        public String toString() {
            return profile + " " + shapeId() + " (" + protocol + (endpointRules ? ", rules" : "") + ") operations "
                   + operations.stream().map(o -> o.name() + "[" + o.input() + "->" + o.output()
                                                  + (o.pagination() != null ? ", paginated" : "")
                                                  + (o.waiter() != null ? ", waiter " + o.waiter() : "")
                                                  + (o.errors().isEmpty() ? "" : ", errors " + o.errors()) + "]")
                                 .toList()
                   + " service errors " + serviceErrors;
        }
    }

    @Provide
    Arbitrary<Service> services() {
        Arbitrary<Profile> profiles = Arbitraries.frequency(
            Tuple.of(4, Profile.PLAIN), Tuple.of(2, Profile.S3), Tuple.of(1, Profile.RDS), Tuple.of(1, Profile.DSQL),
            Tuple.of(1, Profile.CLOUDFRONT));
        return Combinators.combine(profiles, Arbitraries.integers().between(1, 5), Arbitraries.integers().between(0, 6),
                                   Arbitraries.integers().between(0, 4), Arbitraries.longs().withoutEdgeCases())
                          .as((profile, operations, data, errors, seed) -> randomService(
                              new Random(seed), profile, operations, data, errors));
    }

    /** A random service of {@code profile} with the given pool sizes. */
    static Service randomService(Random random, Profile profile, int operationCount, int dataCount, int errorCount) {
        Protocol protocol = profile == Profile.S3
                            ? Protocol.REST_XML
                            : pick(random, List.of(Protocol.AWS_JSON_1_0, Protocol.AWS_JSON_1_1,
                                                   Protocol.REST_JSON_1, Protocol.REST_JSON_1));
        String namespace;
        String shapeName;
        String sdkId;
        String typeName;
        if (profile == Profile.PLAIN) {
            String name = pick(random, PLAIN_SERVICES);
            boolean twoWords = random.nextBoolean();
            namespace = "example." + name.toLowerCase(Locale.ROOT);
            shapeName = name + "Service";
            sdkId = twoWords ? name + " Data" : name;
            typeName = twoWords ? name + "Data" : name;
        } else {
            namespace = profile.namespace;
            shapeName = profile.shapeName;
            sdkId = profile.sdkId;
            typeName = profile.typeName;
        }

        // Data shapes; their members are drawn once every name is known.
        List<String> dataNames = DATA_NAMES.subList(0, dataCount);
        List<DataKind> dataKinds = new ArrayList<>();
        for (int i = 0; i < dataCount; i++) {
            dataKinds.add(pick(random, List.of(DataKind.STRUCTURE, DataKind.STRUCTURE, DataKind.UNION, DataKind.ENUM,
                                               DataKind.INT_ENUM, DataKind.LIST, DataKind.MAP)));
        }
        List<DataShape> data = new ArrayList<>();
        for (int i = 0; i < dataCount; i++) {
            DataKind kind = dataKinds.get(i);
            List<Ref> members = switch (kind) {
                case STRUCTURE -> randomMembers(random, dataNames, random.nextInt(4), false);
                // The first member of a union is a prelude shape, so that every union has a finite value.
                case UNION -> {
                    List<Ref> union = new ArrayList<>(randomMembers(random, dataNames, 1 + random.nextInt(3), false));
                    union.set(0, new Ref(union.get(0).name(), pick(random, PRELUDE), false, null));
                    yield union;
                }
                // A collection reaches only earlier shapes, so every cycle passes through a structure or union.
                case LIST, MAP -> List.of(new Ref("member", target(random, dataNames.subList(0, i)), false, null));
                default -> List.of();
            };
            data.add(new DataShape(dataNames.get(i), kind, members));
        }

        List<String> errorNouns = new ArrayList<>(ERROR_NOUNS);
        Collections.shuffle(errorNouns, random);
        List<ErrorShape> errors = new ArrayList<>();
        for (int i = 0; i < errorCount; i++) {
            errors.add(new ErrorShape(errorNouns.get(i) + pick(random, ERROR_SUFFIXES),
                                      random.nextInt(3) == 0 && !dataNames.isEmpty() ? pick(random, dataNames)
                                                                                    : null));
        }
        List<String> errorNames = errors.stream().map(ErrorShape::name).toList();

        // Operation names: for S3, some of V2's presigned operations.
        List<String> names = new ArrayList<>();
        if (profile.presigner() != null) {
            List<String> presigned = new ArrayList<>(profile.presigner().operations());
            Collections.shuffle(presigned, random);
            names.addAll(presigned.subList(0, random.nextInt(Math.min(operationCount, 4) + 1)));
        }
        while (names.size() < operationCount) {
            String name = pick(random, VERBS) + pick(random, OPERATION_NOUNS);
            if (!names.contains(name)) {
                names.add(name);
            }
        }

        List<Operation> operations = new ArrayList<>();
        for (String name : names) {
            operations.add(randomOperation(random, name, protocol, dataNames, errorNames, data));
        }
        List<String> serviceErrors = errorNames.stream().filter(e -> random.nextInt(4) == 0).toList();
        boolean rules = profile == Profile.S3 || random.nextInt(3) == 0;
        return new Service(profile, namespace, shapeName, sdkId, typeName, protocol, rules, operations, serviceErrors,
                           errors, data);
    }

    private static Operation randomOperation(Random random, String name, Protocol protocol, List<String> dataNames,
                                             List<String> errorNames, List<DataShape> data) {
        List<InputKind> inputs = new ArrayList<>(List.of(InputKind.UNIT, InputKind.EMPTY, InputKind.OPTIONAL,
                                                         InputKind.OPTIONAL, InputKind.REQUIRED,
                                                         InputKind.REQUIRED));
        List<OutputKind> outputs = new ArrayList<>(List.of(OutputKind.UNIT, OutputKind.EMPTY, OutputKind.MEMBERS,
                                                           OutputKind.MEMBERS));
        if (protocol.httpBindings) {
            inputs.add(InputKind.STREAMING);
            inputs.add(InputKind.STREAMING);
            outputs.add(OutputKind.STREAMING);
            outputs.add(OutputKind.STREAMING);
        }
        if (protocol.eventStreams) {
            inputs.add(InputKind.EVENTS);
            inputs.add(InputKind.EVENTS);
            outputs.add(OutputKind.EVENTS);
            outputs.add(OutputKind.EVENTS);
        }
        InputKind input = pick(random, inputs);
        OutputKind output = pick(random, outputs);
        if (output == OutputKind.STREAMING && input == InputKind.UNIT) {
            // A streaming response is transformed from a request; V2 models every such operation with an input.
            input = InputKind.EMPTY;
        }
        boolean plainShapes = (input == InputKind.EMPTY || input == InputKind.OPTIONAL || input == InputKind.REQUIRED)
                              && (output == OutputKind.EMPTY || output == OutputKind.MEMBERS);
        Pagination pagination = plainShapes && random.nextInt(3) == 0
                                ? new Pagination(random.nextBoolean(), random.nextBoolean()) : null;
        String waiter = plainShapes && random.nextInt(3) == 0 ? name + "Done" : null;

        List<Ref> inputMembers = new ArrayList<>();
        switch (input) {
            case OPTIONAL -> inputMembers.addAll(randomMembers(random, dataNames, 1 + random.nextInt(3), false));
            case REQUIRED -> inputMembers.addAll(randomMembers(random, dataNames, 1 + random.nextInt(3), true));
            case STREAMING -> {
                inputMembers.addAll(headers(random, random.nextBoolean()));
                inputMembers.add(new Ref("body", "StreamingBlob", true, "payload"));
            }
            case EVENTS -> {
                inputMembers.addAll(headers(random, false));
                inputMembers.add(new Ref("events", name + "InEvents", false, "payload"));
                events(random, name + "In", dataNames, data);
            }
            default -> {
            }
        }
        List<Ref> outputMembers = new ArrayList<>();
        switch (output) {
            case MEMBERS -> outputMembers.addAll(randomMembers(random, dataNames, 1 + random.nextInt(3), false));
            case STREAMING -> {
                outputMembers.addAll(headers(random, false));
                outputMembers.add(new Ref("body", "StreamingBlob", true, "payload"));
            }
            case EVENTS -> {
                outputMembers.addAll(headers(random, false));
                outputMembers.add(new Ref("events", name + "OutEvents", false, "payload"));
                events(random, name + "Out", dataNames, data);
            }
            default -> {
            }
        }
        if (pagination != null) {
            inputMembers.add(new Ref("nextToken", "String", false, null));
            if (pagination.pageSize()) {
                inputMembers.add(new Ref("maxResults", "Integer", false, null));
            }
            outputMembers.add(new Ref("nextToken", "String", false, null));
            if (pagination.items()) {
                outputMembers.add(new Ref("entries", "EntryList", false, null));
            }
        }
        List<String> errors = errorNames.stream().filter(e -> random.nextInt(3) == 0).toList();
        return new Operation(name, input, output, inputMembers, outputMembers, pagination, waiter, errors);
    }

    /** An event stream union {@code <prefix>Events} of one event structure {@code <prefix>Event}. */
    private static void events(Random random, String prefix, List<String> dataNames, List<DataShape> data) {
        List<Ref> eventMembers = new ArrayList<>(List.of(new Ref("text", "String", false, null)));
        if (!dataNames.isEmpty() && random.nextBoolean()) {
            eventMembers.add(new Ref("detail", pick(random, dataNames), false, null));
        }
        data.add(new DataShape(prefix + "Event", DataKind.STRUCTURE, eventMembers));
        data.add(new DataShape(prefix + "Events", DataKind.EVENT_UNION,
                               List.of(new Ref("event", prefix + "Event", false, null))));
    }

    private static List<Ref> headers(Random random, boolean required) {
        List<Ref> headers = new ArrayList<>();
        if (required || random.nextBoolean()) {
            headers.add(new Ref("label", "String", required, "header"));
        }
        return headers;
    }

    private static List<Ref> randomMembers(Random random, List<String> dataNames, int count, boolean firstRequired) {
        List<String> names = new ArrayList<>(MEMBER_NAMES);
        Collections.shuffle(names, random);
        List<Ref> members = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            members.add(new Ref(names.get(i), target(random, dataNames), firstRequired && i == 0, null));
        }
        return members;
    }

    private static String target(Random random, List<String> dataNames) {
        return !dataNames.isEmpty() && random.nextBoolean() ? pick(random, dataNames) : pick(random, PRELUDE);
    }

    private static <T> T pick(Random random, List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    /**
     * The fixed service of {@link #plantedDefectsAreReported}: a REST JSON service with rules and a waiter, a
     * paginated, a streaming and an event stream operation, an unreached shape and an unreached error.
     */
    private static Service fixedService() {
        List<DataShape> data = new ArrayList<>(List.of(
            new DataShape("Anchor", DataKind.STRUCTURE, List.of(new Ref("label", "Beacon", false, null))),
            new DataShape("Beacon", DataKind.ENUM, List.of()),
            new DataShape("Cobalt", DataKind.STRUCTURE, List.of(new Ref("size", "Integer", false, null)))));
        Random random = new Random(7);
        events(random, "StartOrderOut", List.of(), data);
        List<Operation> operations = List.of(
            new Operation("GetOrder", InputKind.REQUIRED, OutputKind.MEMBERS,
                          List.of(new Ref("label", "String", true, null)), List.of(new Ref("note", "Anchor", false,
                                                                                           null)),
                          null, "GetOrderDone", List.of("QuotaFault")),
            new Operation("ListOrders", InputKind.OPTIONAL, OutputKind.MEMBERS,
                          List.of(new Ref("nextToken", "String", false, null)),
                          List.of(new Ref("nextToken", "String", false, null)), new Pagination(false, false), null,
                          List.of()),
            new Operation("PutReport", InputKind.STREAMING, OutputKind.EMPTY,
                          List.of(new Ref("body", "StreamingBlob", true, "payload")), List.of(), null, null,
                          List.of()),
            new Operation("StartOrder", InputKind.UNIT, OutputKind.EVENTS, List.of(),
                          List.of(new Ref("events", "StartOrderOutEvents", false, "payload")), null, null,
                          List.of()));
        return new Service(Profile.PLAIN, "example.orchard", "OrchardService", "Orchard", "Orchard",
                           Protocol.REST_JSON_1, true, operations, List.of(),
                           List.of(new ErrorShape("QuotaFault", null), new ErrorShape("AccessDenied", null)), data);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
