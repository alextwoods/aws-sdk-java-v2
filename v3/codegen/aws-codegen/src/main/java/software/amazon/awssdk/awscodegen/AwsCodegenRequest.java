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

package software.amazon.awssdk.awscodegen;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;

/**
 * Input to one {@link AwsCodegen} invocation: the service to generate, the Java package to generate it into, the
 * directory holding its canonical Smithy model files, the directories that receive the generated source and resource
 * trees, and the immutable revision of the canonical model source.
 *
 * <p>Build tools (for example {@code aws-codegen-maven-plugin}) construct this request; they never interpret the model.
 *
 * @param serviceShapeId absolute Smithy shape ID of the service, for example {@code com.amazonaws.s3#AmazonS3}
 * @param packageName Java package that receives the generated service sources
 * @param modelDirectory directory containing the canonical Smithy model files ({@code *.smithy}, {@code *.json})
 * @param outputDirectory directory that receives the generated Java source tree; replaced on every invocation
 * @param resourceOutputDirectory directory that receives the generated resource tree (model provenance and the
 *        generation manifest under {@code META-INF/aws-sdk-java-v3/}); replaced on every invocation. It must not
 *        overlap {@code outputDirectory}.
 * @param modelRevision the full, untruncated immutable revision of the canonical model source: a Git commit object ID
 *        of 40 (SHA-1) or 64 (SHA-256) lower-case hexadecimal digits, recorded verbatim in the model provenance. May be
 *        {@code null} only for unpinned (development or test) generation, in which case the provenance records the
 *        content digest of the model files as a {@code model-digest} revision. Release builds
 *        always pass a revision ({@code aws-codegen-maven-plugin} requires one).
 */
public record AwsCodegenRequest(String serviceShapeId, String packageName, Path modelDirectory, Path outputDirectory,
                                Path resourceOutputDirectory, String modelRevision) {

    /** Suffix of the default resource directory, a sibling of the output directory, used by the 4-argument form. */
    static final String DEFAULT_RESOURCE_DIRECTORY_SUFFIX = "-resources";

    private static final String IDENTIFIER = "[A-Za-z_][A-Za-z0-9_]*";
    private static final Pattern SHAPE_ID = Pattern.compile(IDENTIFIER + "(\\." + IDENTIFIER + ")*#" + IDENTIFIER);
    private static final Pattern JAVA_PACKAGE = Pattern.compile("[a-z_][a-z0-9_]*(\\.[a-z_][a-z0-9_]*)*");

    public AwsCodegenRequest {
        Objects.requireNonNull(serviceShapeId, "serviceShapeId");
        Objects.requireNonNull(packageName, "packageName");
        Objects.requireNonNull(modelDirectory, "modelDirectory");
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(resourceOutputDirectory, "resourceOutputDirectory");
        if (!SHAPE_ID.matcher(serviceShapeId).matches()) {
            throw new AwsCodegenException("Service '" + serviceShapeId + "' is not an absolute Smithy shape ID "
                                          + "(expected namespace#Name).");
        }
        requireJavaPackage(packageName);
        Path sources = outputDirectory.toAbsolutePath().normalize();
        Path resources = resourceOutputDirectory.toAbsolutePath().normalize();
        if (sources.startsWith(resources) || resources.startsWith(sources)) {
            throw new AwsCodegenException("The generated resource directory " + resourceOutputDirectory
                                          + " must not overlap the generated source directory " + outputDirectory
                                          + ".");
        }
        if (modelRevision != null) {
            ModelRevision.requireGitCommit(modelRevision);
        }
    }

    /**
     * An unpinned request whose resources are generated into the sibling {@code <outputDirectory>-resources}
     * directory and whose provenance records the model content digest as its revision.
     */
    public AwsCodegenRequest(String serviceShapeId, String packageName, Path modelDirectory, Path outputDirectory) {
        this(serviceShapeId, packageName, modelDirectory, outputDirectory, defaultResourceDirectory(outputDirectory),
             null);
    }

    /** A copy of this request pinned to the canonical model source revision {@code revision}. */
    public AwsCodegenRequest withModelRevision(String revision) {
        return new AwsCodegenRequest(serviceShapeId, packageName, modelDirectory, outputDirectory,
                                     resourceOutputDirectory, Objects.requireNonNull(revision, "revision"));
    }

    /** A copy of this request that generates its resource tree into {@code directory}. */
    public AwsCodegenRequest withResourceOutputDirectory(Path directory) {
        return new AwsCodegenRequest(serviceShapeId, packageName, modelDirectory, outputDirectory, directory,
                                     modelRevision);
    }

    /** Rejects anything but a lower-case Java package name whose segments are not Java keywords or literals. */
    static void requireJavaPackage(String packageName) {
        if (!JAVA_PACKAGE.matcher(packageName).matches() || !SourceVersion.isName(packageName, SourceVersion.RELEASE_21)) {
            throw new AwsCodegenException("Package '" + packageName + "' is not a valid lower-case Java package name.");
        }
    }

    private static Path defaultResourceDirectory(Path outputDirectory) {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Path name = outputDirectory.toAbsolutePath().normalize().getFileName();
        if (name == null) {
            throw new AwsCodegenException("The generated source directory must not be a file system root.");
        }
        return outputDirectory.resolveSibling(name + DEFAULT_RESOURCE_DIRECTORY_SUFFIX);
    }
}
