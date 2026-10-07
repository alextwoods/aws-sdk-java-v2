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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.aws.AwsAuthSchemeOptions;
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
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.awssdk.telemetry.spi.TelemetryConfiguration;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Generated Java sources compiled in-test with {@code javac} against the generated-client runtime ({@code aws-core},
 * {@code sdk-core}, {@code protocol-core}, {@code smithy-runtime}, Smithy Java core and {@code smithy-model}), and
 * loaded from the compiled
 * classes.
 *
 * <p>Test code may use reflection to reach generated types; generated code may not, which the architecture tests and
 * {@code GeneratedSourceGuard} enforce.
 */
public final class CompiledSources {

    private static final JavaCompiler JAVAC = Objects.requireNonNull(ToolProvider.getSystemJavaCompiler(),
                                                                     "the tests must run on a JDK");
    /**
     * The generated-client runtime: {@code aws-core} (operation descriptors, {@code AwsServiceException}),
     * {@code sdk-core} (the model types' {@code SdkBytes}, exceptions, collection snapshots, builders and
     * {@code ToString}), {@code protocol-core} (the metadata's {@code ServiceProtocols}), {@code endpoints} and
     * {@code regions} (the generated endpoint parameters, provider, rules and bindings), {@code smithy-runtime},
     * Smithy Java core and {@code smithy-model}.
     */
    private static final String RUNTIME_CLASS_PATH = classPath(List.of(
        AwsOperationDescriptor.class, AwsServiceException.class, SdkBytes.class, ServiceProtocols.class,
        ServiceSchemaIndex.class, EndpointRuleSet.class, Region.class,
        Schema.class, ShapeId.class, SmithyBuilder.class,
        // The generated clients: the AWS pipeline, auth (with the AWS schemes of SigV4 services), identity, retries
        // and the HTTP SPI; the default credentials chain of SigV4 services; the httpChecksum metadata of operation
        // bindings.
        AwsCallEngine.class, ModeledAuth.class, AwsAuthSchemeOptions.class, IdentityProvider.class,
        DefaultCredentialsProvider.class, RetryStrategy.class, SdkSyncHttpClient.class, Publisher.class,
        HttpChecksumMetadata.class,
        // The client builders' telemetry settings (TelemetryConfiguration, MetricPublisher): telemetry-spi.
        TelemetryConfiguration.class));

    private final Path classes;

    private CompiledSources(Path classes) {
        this.classes = classes;
    }

    /**
     * Compiles every generated source under {@code sourceRoot} whose relative path starts with one of
     * {@code prefixes} (all sources if none is given) into {@code classes}.
     *
     * @throws AssertionError with the compiler output if compilation fails
     */
    public static CompiledSources compile(Path sourceRoot, Path classes, String... prefixes) {
        List<String> sources = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .filter(p -> prefixes.length == 0 || Stream.of(prefixes).anyMatch(
                    prefix -> sourceRoot.relativize(p).toString().replace(File.separatorChar, '/').startsWith(prefix)))
                .sorted()
                .forEach(p -> sources.add(p.toString()));
            Files.createDirectories(classes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (sources.isEmpty()) {
            throw new AssertionError("No generated sources under " + sourceRoot + " match " + List.of(prefixes));
        }
        List<String> arguments = new ArrayList<>(List.of("-proc:none", "-implicit:none", "--release", "21",
                                                         "-classpath", RUNTIME_CLASS_PATH, "-d", classes.toString()));
        arguments.addAll(sources);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int status = JAVAC.run(null, diagnostics, diagnostics, arguments.toArray(String[]::new));
        if (status != 0) {
            throw new AssertionError("javac failed:\n" + diagnostics.toString(StandardCharsets.UTF_8));
        }
        return new CompiledSources(classes);
    }

    /**
     * Loads the compiled classes with the test class loader as parent, so runtime types (for example
     * {@link ServiceSchemaIndex} and {@link Schema}) are shared with the test.
     */
    public URLClassLoader loader() {
        return new URLClassLoader(new URL[] {url(classes)}, CompiledSources.class.getClassLoader());
    }

    /**
     * Loads the compiled classes in isolation from the test: the parent is the platform class loader, and every other
     * class (including every Smithy class, resolved from this test's class path) is loaded by the returned loader,
     * which records every class name it is asked for.
     */
    public RecordingClassLoader isolatedLoader() {
        List<URL> urls = new ArrayList<>();
        urls.add(url(classes));
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!entry.isBlank()) {
                urls.add(url(Path.of(entry)));
            }
        }
        return new RecordingClassLoader(urls.toArray(URL[]::new));
    }

    /** The {@link ServiceSchemaIndex} of the generated holder {@code holderClass}, loaded by {@code loader}. */
    public static ServiceSchemaIndex index(ClassLoader loader, String holderClass) {
        return (ServiceSchemaIndex) invokeStatic(loader, holderClass, "index");
    }

    /** Invokes a public static no-argument method of a generated class, initializing it. */
    public static Object invokeStatic(ClassLoader loader, String className, String method) {
        try {
            Method target = Class.forName(className, true, loader).getMethod(method);
            return target.invoke(null);
        } catch (InvocationTargetException e) {
            throw new AssertionError(className + "." + method + "() failed", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** Reads a static field of a generated class, initializing it. */
    public static Object staticField(ClassLoader loader, String className, String field) {
        try {
            var target = Class.forName(className, true, loader).getDeclaredField(field);
            target.setAccessible(true);
            return target.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** A class loader that records the binary name of every class it is asked to load. */
    public static final class RecordingClassLoader extends URLClassLoader {
        static {
            registerAsParallelCapable();
        }

        private final Set<String> requested = new ConcurrentSkipListSet<>();

        private RecordingClassLoader(URL[] urls) {
            super("generated-code-isolated", urls, ClassLoader.getPlatformClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            requested.add(name);
            return super.loadClass(name, resolve);
        }

        /** Every class this loader was asked to load, in lexical order. */
        public SortedSet<String> requestedClasses() {
            return new TreeSet<>(requested);
        }
    }

    private static String classPath(List<Class<?>> types) {
        Set<String> entries = new LinkedHashSet<>();
        for (Class<?> type : types) {
            try {
                entries.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return String.join(File.pathSeparator, entries);
    }

    private static URL url(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(path.toString(), e);
        }
    }
}
