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

package software.amazon.awssdk.awscodegen.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;

/**
 * Generates a service's Java sources in the {@code generate-sources} phase and registers them as a compile source root,
 * so that they exist before the {@code compile} phase of the same module. The generated model provenance and
 * generation manifest ({@code META-INF/aws-sdk-java-v3/services/<package>/}) are generated into a separate directory
 * that is registered as a resource root, so {@code process-resources} packages them into the service artifact.
 *
 * <p>This goal is a lifecycle-only wrapper: it translates Maven configuration into an {@link AwsCodegenRequest} and
 * delegates to {@link AwsCodegen#generate(AwsCodegenRequest)}, the single AWS generator. It contains no emitters.
 *
 * <p>Modules with {@code pom} packaging (for example the {@code v3-services} parent that binds this goal for every
 * generated service) are skipped. Every other module must configure {@code service}, {@code packageName} and
 * {@code modelRevision} and provide at least one model file; otherwise the build fails rather than compiling a
 * service without its generated sources or packaging it without an exact model revision.
 *
 * <p>Generated service modules additionally pin their model digest ({@code modelDigest}) and the baseline types they
 * must generate ({@code requiredTypes}); a provenance mismatch or a missing type fails the build before the generated
 * roots are registered, so the output is never compiled or packaged.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public final class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    /** Absolute Smithy shape ID of the service to generate, for example {@code com.amazonaws.s3#AmazonS3}. */
    @Parameter(property = "aws.codegen.service")
    String service;

    /** Java package that receives the generated service sources. */
    @Parameter(property = "aws.codegen.packageName")
    String packageName;

    /** Directory containing the service's canonical Smithy model files. */
    @Parameter(property = "aws.codegen.modelDirectory", defaultValue = "${project.basedir}/src/main/smithy")
    File modelDirectory;

    /** Generated source root; replaced on every execution and added to the module's compile source roots. */
    @Parameter(property = "aws.codegen.outputDirectory",
               defaultValue = "${project.build.directory}/generated-sources/aws-codegen")
    File outputDirectory;

    /** Generated resource root; replaced on every execution and added to the module's resources. */
    @Parameter(property = "aws.codegen.resourceOutputDirectory",
               defaultValue = "${project.build.directory}/generated-resources/aws-codegen")
    File resourceOutputDirectory;

    /**
     * The full, untruncated immutable revision of the canonical model source: its Git commit object ID (40 or 64
     * lower-case hexadecimal digits). It is recorded verbatim in the service's model provenance and is required, so
     * that no service artifact is built without an exact model revision (Requirements 2.12, 22.2).
     */
    @Parameter(property = "aws.codegen.modelRevision")
    String modelRevision;

    /**
     * The expected location-independent digest of the model files ({@code sha256:<hex>}), as pinned for the service
     * by the service fleet ({@code v3/services/service-fleet.json}). When set, generation fails with a provenance
     * mismatch if the model files the build read are not exactly the pinned canonical model, so a service artifact
     * can never record a model revision its model files do not come from.
     */
    @Parameter(property = "aws.codegen.modelDigest")
    String modelDigest;

    /**
     * Fully qualified names of types the generated tree must contain, separated by commas or white space: the
     * V2 baseline client surfaces of the service (for example its sync and async client interfaces and builders).
     * Generation fails if any of them was not generated.
     */
    @Parameter(property = "aws.codegen.requiredTypes")
    String requiredTypes;

    /** Skips generation. A service module skipped this way will not compile. */
    @Parameter(property = "aws.codegen.skip", defaultValue = "false")
    boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if (skip) {
            getLog().info("aws-codegen generation skipped (aws.codegen.skip=true).");
            return;
        }
        if ("pom".equals(project.getPackaging())) {
            getLog().debug("aws-codegen generation skipped for pom-packaged " + project.getArtifactId());
            return;
        }
        if (isBlank(service) || isBlank(packageName)) {
            throw new MojoFailureException("aws-codegen:generate requires 'service' (aws.codegen.service) and "
                                           + "'packageName' (aws.codegen.packageName) for " + project.getArtifactId()
                                           + ".");
        }

        if (isBlank(modelRevision)) {
            throw new MojoFailureException("aws-codegen:generate requires 'modelRevision' (aws.codegen.modelRevision), "
                                           + "the full Git commit object ID of the canonical model source, for "
                                           + project.getArtifactId() + ".");
        }

        AwsCodegenResult result;
        try {
            // The revision is passed verbatim (not trimmed) so that it is validated and recorded exactly as given.
            result = AwsCodegen.generate(new AwsCodegenRequest(service.trim(), packageName.trim(),
                                                               modelDirectory.toPath(), outputDirectory.toPath(),
                                                               resourceOutputDirectory.toPath(), modelRevision));
        } catch (AwsCodegenException e) {
            throw new MojoFailureException("aws-codegen failed for " + project.getArtifactId() + ": " + e.getMessage(), e);
        }

        List<String> problems = new ArrayList<>();
        if (!isBlank(modelDigest) && !modelDigest.trim().equals(result.modelDigest())) {
            problems.add("provenance mismatch: the model files under " + modelDirectory + " have digest "
                         + result.modelDigest() + ", but the service is pinned to model digest " + modelDigest.trim()
                         + " of revision " + modelRevision);
        }
        Set<Path> generated = new HashSet<>(result.generatedFiles());
        for (String type : types(requiredTypes)) {
            if (!generated.contains(Path.of(type.replace('.', '/') + ".java"))) {
                problems.add("missing generated type " + type);
            }
        }
        if (!problems.isEmpty()) {
            // The roots are not registered, so the rejected tree is neither compiled nor packaged; the next run of
            // the generator replaces it. (This wrapper writes and deletes no files itself.)
            throw new MojoFailureException("aws-codegen output of " + project.getArtifactId() + " for " + service
                                           + " failed its service fleet checks: " + String.join("; ", problems));
        }

        project.addCompileSourceRoot(result.sourceRoot().toString());
        Resource resources = new Resource();
        resources.setDirectory(result.resourceRoot().toString());
        project.addResource(resources);
        getLog().info("Generated " + result.generatedFiles().size() + " source file(s) and "
                      + result.generatedResources().size() + " resource(s) for " + service + " (model revision "
                      + result.modelRevision() + ", " + result.modelDigest() + ")");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static List<String> types(String value) {
        if (isBlank(value)) {
            return List.of();
        }
        return Stream.of(value.split("[,\\s]+")).filter(s -> !s.isEmpty()).sorted().distinct().toList();
    }
}
