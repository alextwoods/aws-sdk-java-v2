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

package software.amazon.awssdk.codegen.emitters.tasks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.schema.SchemaCodegen;
import software.amazon.awssdk.codegen.poet.schema.SchemaIndexGenerator;
import software.amazon.awssdk.codegen.schema.TraitInitializers;

/**
 * Generates the schema classes of a service built from a Smithy model (design A.17): the {@code <Svc>Schemas}
 * partitions, {@code <Svc>SchemaIndex} with its {@code META-INF/services} entry, {@code <Svc>ServiceSchemas}, and
 * {@code <Svc>Operations} with the service's {@code ApiOperation}s and Error_Registry (design A.18). Has
 * no tasks for an intermediate model built from C2J, so the legacy route's output is unchanged.
 *
 * <p>The trait inclusion policy, the trait initializer registry and every class are prepared in the constructor,
 * which runs when the composite task tree is created and so before any generator task writes a file.
 */
public final class SchemaGeneratorTasks extends BaseGeneratorTasks {
    private static final Object SERVICE_FILE_LOCK = new Object();

    private final SchemaCodegen schemaCodegen;
    private final String resourcesDirectory;

    SchemaGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.resourcesDirectory = dependencies.getPathProvider().getResourcesDirectory();
        this.schemaCodegen = SchemaCodegen.isApplicable(model)
                             ? SchemaCodegen.create(model, TraitInitializers.load(classLoader()))
                             : null;
    }

    /**
     * The codegen class loader: in the Maven plugin it also holds the plugin's dependencies, where a
     * {@code SchemaCodegenExtension} would be.
     */
    private static ClassLoader classLoader() {
        return SchemaGeneratorTasks.class.getClassLoader();
    }

    @Override
    protected boolean hasTasks() {
        return schemaCodegen != null;
    }

    @Override
    protected List<GeneratorTask> createTasks() {
        List<GeneratorTask> tasks = new ArrayList<>();
        for (ClassSpec spec : schemaCodegen.classSpecs()) {
            tasks.add(createPoetGeneratorTask(spec));
        }
        tasks.add(new ServiceFileTask(Paths.get(resourcesDirectory).resolve(SchemaIndexGenerator.serviceFileName()),
                                      schemaCodegen.schemaIndexClass().reflectionName()));
        return tasks;
    }

    /**
     * Adds the schema index to the {@code ServiceLoader} registration file, keeping entries other services generated
     * into the same resources directory.
     */
    private static final class ServiceFileTask extends GeneratorTask {
        private static final long serialVersionUID = 1L;

        private final String fileName;
        private final String entry;

        private ServiceFileTask(Path file, String entry) {
            this.fileName = file.toString();
            this.entry = entry;
        }

        @Override
        protected void compute() {
            Path file = Paths.get(fileName).toAbsolutePath();
            synchronized (SERVICE_FILE_LOCK) {
                try {
                    TreeSet<String> entries = new TreeSet<>();
                    if (Files.isRegularFile(file)) {
                        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                                entries.add(trimmed);
                            }
                        }
                    }
                    entries.add(entry);
                    Path parent = file.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    StringBuilder content = new StringBuilder();
                    for (String line : entries) {
                        content.append(line).append('\n');
                    }
                    Files.write(file, content.toString().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new UncheckedIOException("Unable to write " + file, e);
                }
            }
        }
    }
}
