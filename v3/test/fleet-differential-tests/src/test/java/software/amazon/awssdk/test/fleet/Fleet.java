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

package software.amazon.awssdk.test.fleet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * The generated service fleet as {@code services/service-fleet.json} declares it: one {@link Service} per generated
 * service (bundled services of one V2 module share its {@link Module} and jar).
 *
 * <p>Each module's jar is found on the test class path through the generation manifest it carries
 * ({@code META-INF/aws-sdk-java-v3/services/<package>/generation-manifest.json}), and {@link Module#open() opened} in
 * its own child-first class loader over the test class path's V3 runtime: the jar's classes are defined by that
 * loader only, so they are released with it and the fleet is never resident at once. The canonical model of a service
 * is the module's vendored model files, assembled here independently of {@code aws-codegen} but with the trait
 * definitions it generates with ({@link #canonicalModel}).
 */
final class Fleet {
    /** The V3 reactor root ({@code v3/}). */
    static final Path V3_ROOT = Path.of(System.getProperty("v3.root", "../..")).toAbsolutePath().normalize();

    private static final String MANIFEST = "META-INF/aws-sdk-java-v3/services/%s/generation-manifest.json";
    private static Fleet instance;

    private final List<Service> services;
    private final List<Module> modules;

    private Fleet(List<Service> services, List<Module> modules) {
        this.services = services;
        this.modules = modules;
    }

    /** The fleet of this checkout (read once). */
    static synchronized Fleet get() {
        if (instance == null) {
            instance = read(V3_ROOT.resolve("services/service-fleet.json"));
        }
        return instance;
    }

    /** Every generated service, in lock order (by V2 module name). */
    List<Service> services() {
        return services;
    }

    /** Every generated service module, by module name. */
    List<Module> modules() {
        return modules;
    }

    /** The service named {@code name} in the lock (for example {@code dynamodb.streams}). */
    Service service(String name) {
        return services.stream().filter(s -> s.name().equals(name)).findFirst()
                       .orElseThrow(() -> new IllegalArgumentException("No fleet service " + name));
    }

    private static Fleet read(Path lock) {
        ObjectNode root;
        try {
            root = Node.parse(Files.readString(lock, StandardCharsets.UTF_8)).expectObjectNode();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, List<Service>> byModule = new TreeMap<>();
        List<Service> services = new ArrayList<>();
        for (Node node : root.expectArrayMember("services").getElements()) {
            ObjectNode s = node.expectObjectNode();
            String module = s.expectStringMember("module").getValue();
            Service service = new Service(s.expectStringMember("name").getValue(), module,
                                          s.expectStringMember("packageName").getValue(),
                                          ShapeId.from(s.expectStringMember("service").getValue()),
                                          s.expectStringMember("sdkId").getValue(),
                                          V3_ROOT.resolve("services").resolve(module)
                                                 .resolve(s.expectStringMember("modelDirectory").getValue()),
                                          s.expectArrayMember("protocolModules").getElementsAs(StringNode::getValue));
            services.add(service);
            byModule.computeIfAbsent(module, k -> new ArrayList<>()).add(service);
        }
        List<Module> modules = new ArrayList<>();
        byModule.forEach((name, members) -> modules.add(new Module(name, List.copyOf(members))));
        return new Fleet(List.copyOf(services), List.copyOf(modules));
    }

    /** The canonical model of {@code service}: its vendored model files, with unknown traits allowed. */
    static Model canonicalModel(Service service) {
        ClassLoader loader = AwsCodegen.class.getClassLoader();
        ModelAssembler assembler = Model.assembler(loader).discoverModels(loader)
                                        .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
        try (Stream<Path> files = Files.walk(service.modelDirectory())) {
            files.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".json") || p.toString().endsWith(".smithy"))
                 .sorted().forEach(assembler::addImport);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Model model = assembler.assemble().unwrap();
        // The two Smithy-defined normalizations aws-codegen applies before generating (AwsCodegen:
        // performDefaultCodegenTransforms and changeStringEnumsToEnumShapes(true)): service errors are copied to every
        // operation, and Smithy 1 @enum strings become enum shapes with synthesized constant names. Both are
        // Smithy's own ModelTransformer operations, applied here directly, so the oracle stays independent of the
        // generator's code.
        ModelTransformer transformer = ModelTransformer.create();
        model = transformer.copyServiceErrorsToOperations(model, model.expectShape(service.id(), ServiceShape.class));
        return transformer.changeStringEnumsToEnumShapes(model, true);
    }

    /**
     * One generated service of the lock.
     *
     * @param name the lock name (V2 module, with a suffix for a bundled service)
     * @param module the V2 module (and V3 artifact) that holds the service
     * @param packageName the Java package of the generated sources
     * @param id the absolute service shape ID
     * @param sdkId the service's SDK ID
     * @param modelDirectory the vendored canonical model files of the service
     * @param protocolModules the V3 protocol artifacts of its modeled protocols
     */
    record Service(String name, String module, String packageName, ShapeId id, String sdkId, Path modelDirectory,
                   List<String> protocolModules) {

        /** The generated model package. */
        String modelPackage() {
            return packageName + ".model";
        }


        @Override
        public String toString() {
            return name + " (" + id + ")";
        }
    }

    /** One generated service module and the services it bundles. */
    record Module(String name, List<Service> services) {

        /** The module's jar on the test class path, found through the generation manifest of its first service. */
        Path jar() {
            String resource = String.format(MANIFEST, services.get(0).packageName());
            try {
                List<URL> found = Collections.list(Fleet.class.getClassLoader().getResources(resource));
                if (found.size() != 1) {
                    throw new AssertionError("Expected exactly one " + resource + " on the test class path (the "
                                             + name + " service artifact), found " + found);
                }
                URL url = found.get(0);
                if (!"jar".equals(url.getProtocol())) {
                    throw new AssertionError(name + " is not on the test class path as a jar: " + url);
                }
                return Path.of(((JarURLConnection) url.openConnection()).getJarFileURL().toURI());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (java.net.URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }

        /** A fresh child-first class loader over the module's jar; close it to release the service's classes. */
        ServiceClassLoader open() {
            return new ServiceClassLoader(name, jar());
        }
    }

    /**
     * A child-first class loader over one service jar: every class the jar contains is defined here (even though the
     * jar is also on the test class path), everything else is the parent's V3 runtime.
     */
    static final class ServiceClassLoader extends URLClassLoader {
        private final Path jar;
        private final List<String> classNames;

        ServiceClassLoader(String name, Path jar) {
            super("fleet-" + name, new URL[] {url(jar)}, Fleet.class.getClassLoader());
            this.jar = jar;
            this.classNames = classNames(jar);
        }

        /** Every top-level and nested class name in the jar, sorted. */
        List<String> classNames() {
            return classNames;
        }

        Path jar() {
            return jar;
        }

        /** Loads and initializes {@code className} from this loader. */
        Class<?> load(String className) {
            try {
                return Class.forName(className, true, this);
            } catch (ClassNotFoundException e) {
                throw new AssertionError(className + " is not in " + jar, e);
            }
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null && findResource(name.replace('.', '/') + ".class") != null) {
                    loaded = findClass(name);
                }
                if (loaded == null) {
                    return super.loadClass(name, resolve);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        private static URL url(Path jar) {
            try {
                return jar.toUri().toURL();
            } catch (java.net.MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }

        private static List<String> classNames(Path jar) {
            List<String> names = new ArrayList<>();
            try (JarFile file = new JarFile(jar.toFile())) {
                Enumeration<JarEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    if (entry.endsWith(".class") && !entry.endsWith("module-info.class")) {
                        names.add(entry.substring(0, entry.length() - ".class".length()).replace('/', '.'));
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            names.sort(Comparator.naturalOrder());
            return List.copyOf(names);
        }
    }
}
