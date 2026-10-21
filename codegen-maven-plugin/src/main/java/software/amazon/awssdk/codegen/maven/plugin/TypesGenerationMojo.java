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

package software.amazon.awssdk.codegen.maven.plugin;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import software.amazon.awssdk.codegen.pipeline.TypesOnlySchemaCodegen;
import software.amazon.awssdk.codegen.poet.schema.SchemaCodegen;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;

/**
 * Types-only generation: generates only the Schemas ({@code <classPrefix>Schemas} and {@code <classPrefix>SchemaIndex})
 * of a Smithy model, through {@link TypesOnlySchemaCodegen}, for a library whose model classes are hand-written (design
 * D.7). The output directories are the same as the {@code generate} goal's, and the generated sources directory is
 * added as a compile source root.
 *
 * <pre>{@code
 * <goal>generate-types</goal>
 * <configuration>
 *     <service>example.ns#Example</service>
 *     <rootShapes><rootShape>example.ns#Event</rootShape></rootShapes>
 *     <packageName>example.internal</packageName>
 *     <classPrefix>Example</classPrefix>
 * </configuration>
 * }</pre>
 */
@Mojo(name = "generate-types", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class TypesGenerationMojo extends AbstractMojo {
    /**
     * The Smithy model files.
     */
    @Parameter(property = "smithyModelDirectory", defaultValue = "${basedir}/src/main/smithy")
    private File smithyModelDirectory;

    /**
     * The service shape: its namespace and its operations' closure are used. It may have no operations.
     */
    @Parameter(required = true)
    private String service;

    /**
     * Shapes that get schemas, with every shape they reach through members, in addition to the service's closure.
     */
    @Parameter
    private List<String> rootShapes;

    /**
     * The package of the generated classes.
     */
    @Parameter(required = true)
    private String packageName;

    /**
     * The prefix of the generated class names.
     */
    @Parameter(required = true)
    private String classPrefix;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    private String outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        Path buildDirectory = Paths.get(outputDirectory);
        Path sourcesDirectory = buildDirectory.resolve("generated-sources").resolve("sdk");
        Path resourcesDirectory = buildDirectory.resolve("generated-resources").resolve("sdk-resources");
        getLog().info("Generating schemas only from the Smithy model in " + smithyModelDirectory);
        try {
            SchemaCodegen codegen = TypesOnlySchemaCodegen.builder()
                                                          .smithyModelSources(Collections.singletonList(
                                                              smithyModelDirectory.toPath()))
                                                          .service(shapeId(service))
                                                          .rootShapes(rootShapeIds())
                                                          .packageName(packageName)
                                                          .classPrefix(classPrefix)
                                                          .sourcesDirectory(sourcesDirectory)
                                                          .resourcesDirectory(resourcesDirectory)
                                                          .classLoader(TypesOnlySchemaCodegen.class.getClassLoader())
                                                          .build()
                                                          .generate();
            getLog().info("Generated " + codegen.classSpecs().size() + " classes with "
                          + codegen.order().fields().size() + " schemas");
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Types-only generation failed: " + e.getMessage(), e);
        }
        project.addCompileSourceRoot(sourcesDirectory.toFile().getAbsolutePath());
    }

    private List<ShapeId> rootShapeIds() {
        if (rootShapes == null) {
            return Collections.emptyList();
        }
        return rootShapes.stream().map(TypesGenerationMojo::shapeId).collect(Collectors.toList());
    }

    private static ShapeId shapeId(String id) {
        try {
            return ShapeId.from(id.trim());
        } catch (ShapeIdSyntaxException e) {
            throw new IllegalArgumentException("Not an absolute shape ID: " + id, e);
        }
    }
}
