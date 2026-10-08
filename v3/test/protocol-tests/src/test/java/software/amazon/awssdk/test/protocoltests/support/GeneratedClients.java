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

package software.amazon.awssdk.test.protocoltests.support;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.aws.AwsAuthSchemeOptions;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.checksums.HttpChecksumMetadata;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.endpoints.rules.EndpointRuleSet;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.providers.DefaultCredentialsProvider;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.pipeline.AwsCallEngine;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.awssdk.telemetry.spi.TelemetryConfiguration;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * The generated clients of the protocol test services: each service under test is generated with the single generator
 * ({@link AwsCodegen#generate}) from the prepared codegen input into its own package, every generated source is
 * compiled together with {@code javac} against the generated-client runtime, and the classes are loaded with the test
 * class loader as parent so runtime types are shared.
 *
 * <p>A service the generator rejects, or whose sources do not compile, is recorded with the failure; each of its
 * applicable cases then fails with it (no case is silently dropped).
 */
public final class GeneratedClients implements AutoCloseable {
    private static final String ROOT_PACKAGE = "software.amazon.awssdk.protocoltests.";
    private static final JavaCompiler JAVAC = Objects.requireNonNull(ToolProvider.getSystemJavaCompiler(),
                                                                     "the tests must run on a JDK");
    /** The generated-client runtime, by a class of each artifact (as codegen-tests' CompiledSources). */
    private static final List<Class<?>> RUNTIME = List.of(
        AwsOperationDescriptor.class, AwsServiceException.class, SdkBytes.class, ServiceProtocols.class,
        ServiceSchemaIndex.class, EndpointRuleSet.class, Region.class, Schema.class, ShapeId.class,
        SmithyBuilder.class, AwsCallEngine.class, ModeledAuth.class, AwsAuthSchemeOptions.class,
        IdentityProvider.class, DefaultCredentialsProvider.class, RetryStrategy.class, SdkSyncHttpClient.class,
        Publisher.class, HttpChecksumMetadata.class, TelemetryConfiguration.class);

    /**
     * One generated service.
     *
     * @param service the test service
     * @param packageName the generated root package
     * @param typeName the generated service type name ({@code <typeName>Client})
     */
    public record Service(ShapeId service, String packageName, String typeName) {
        public String clientClass() {
            return packageName + "." + typeName + "Client";
        }

        public String builderClass() {
            return packageName + "." + typeName + "ClientBuilder";
        }

        public String metadataClass() {
            return packageName + "." + typeName + "ServiceMetadata";
        }
    }

    private final Map<ShapeId, Service> generated;
    private final Map<ShapeId, Throwable> failures;
    private final URLClassLoader loader;

    private GeneratedClients(Map<ShapeId, Service> generated, Map<ShapeId, Throwable> failures,
                             URLClassLoader loader) {
        this.generated = generated;
        this.failures = failures;
        this.loader = loader;
    }

    /**
     * Generates and compiles every service under test of {@code models} in {@code work}.
     *
     * @param canonicalS3 the canonical S3 model (see {@link ProtocolTestModels})
     */
    public static GeneratedClients generate(ProtocolTestModels models, Path work,
                                            software.amazon.smithy.model.Model canonicalS3) {
        deleteRecursively(work);
        Path modelDirectory = work.resolve("model");
        models.writeCodegenModel(modelDirectory, canonicalS3);
        Path sources = work.resolve("sources");
        Map<ShapeId, Service> generated = new LinkedHashMap<>();
        Map<ShapeId, Throwable> failures = new LinkedHashMap<>();
        for (var service : models.servicesUnderTest()) {
            String packageName = ROOT_PACKAGE + service.getId().getName().toLowerCase(Locale.ROOT)
                                                       .replaceAll("[^a-z0-9]", "");
            Path out = work.resolve("generated").resolve(service.getId().getName());
            try {
                AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest(
                    service.getId().toString(), packageName, modelDirectory, out.resolve("src"),
                    out.resolve("resources"), null), List.of());
                Service info = new Service(service.getId(), packageName, typeName(result, packageName));
                copyTree(result.sourceRoot(), sources);
                generated.put(service.getId(), info);
            } catch (RuntimeException e) {
                failures.put(service.getId(), e);
            }
        }
        URLClassLoader loader = null;
        if (!generated.isEmpty()) {
            try {
                loader = compile(sources, work.resolve("classes"));
            } catch (AssertionError e) {
                generated.keySet().forEach(id -> failures.put(id, e));
                generated.clear();
            }
        }
        return new GeneratedClients(generated, failures, loader);
    }

    /** The generated service, or the generation/compilation failure of {@code service}. */
    public Service service(ShapeId service) {
        Service info = generated.get(service);
        if (info == null) {
            Throwable failure = failures.get(service);
            throw new AssertionError("The client of " + service + " was not generated: "
                                     + (failure == null ? "not a service under test" : Conformance.describe(failure)),
                                     failure);
        }
        return info;
    }

    /** Generation/compilation failures by service, for the report. */
    public Map<ShapeId, Throwable> failures() {
        return failures;
    }

    public ClassLoader loader() {
        return loader;
    }

    public Class<?> type(String name) {
        try {
            return Class.forName(name, true, loader);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Generated class " + name + " is missing", e);
        }
    }

    /** Invokes a public static no-argument method of a generated class. */
    public Object invokeStatic(String className, String method) {
        try {
            return type(className).getMethod(method).invoke(null);
        } catch (InvocationTargetException e) {
            throw new AssertionError(className + "." + method + "() failed", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** The operations of a generated service. */
    @SuppressWarnings("unchecked")
    public List<AwsOperationDescriptor> operations(Service service) {
        return (List<AwsOperationDescriptor>) invokeStatic(service.metadataClass(), "operations");
    }

    /** The type registry of a generated service. */
    public SchemaTypeRegistry typeRegistry(Service service) {
        return (SchemaTypeRegistry) invokeStatic(service.metadataClass(), "typeRegistry");
    }

    @Override
    public void close() throws IOException {
        if (loader != null) {
            loader.close();
        }
    }

    /** The service type name: the {@code <Name>ServiceMetadata.java} file in the generated root package. */
    private static String typeName(AwsCodegenResult result, String packageName) {
        String directory = packageName.replace('.', '/') + "/";
        for (Path file : result.generatedFiles()) {
            String relative = file.toString().replace(File.separatorChar, '/');
            if (relative.startsWith(directory) && relative.endsWith("ServiceMetadata.java")
                && relative.indexOf('/', directory.length()) < 0) {
                String name = relative.substring(directory.length());
                return name.substring(0, name.length() - "ServiceMetadata.java".length());
            }
        }
        throw new AssertionError("No generated ServiceMetadata in " + packageName);
    }

    private static URLClassLoader compile(Path sources, Path classes) {
        List<String> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(sources)) {
            walk.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> files.add(p.toString()));
            Files.createDirectories(classes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Path arguments = classes.resolveSibling("javac-sources.txt");
        try {
            Files.write(arguments, files, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int status = JAVAC.run(null, diagnostics, diagnostics, "-proc:none", "-implicit:none", "--release", "21",
                               "-nowarn", "-encoding", "UTF-8", "-classpath", classPath(), "-d", classes.toString(),
                               "@" + arguments);
        if (status != 0) {
            String output = diagnostics.toString(StandardCharsets.UTF_8);
            throw new AssertionError("javac failed on the generated protocol test clients:\n"
                                     + output.substring(0, Math.min(output.length(), 20_000)));
        }
        try {
            return new URLClassLoader(new URL[] {classes.toUri().toURL()}, GeneratedClients.class.getClassLoader());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String classPath() {
        Set<String> entries = new LinkedHashSet<>();
        for (Class<?> type : RUNTIME) {
            try {
                entries.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return String.join(File.pathSeparator, entries);
    }

    private static void copyTree(Path from, Path to) {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path source : (Iterable<Path>) walk::iterator) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void deleteRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
