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
import java.util.List;

/**
 * Outcome of a successful {@link AwsCodegen} invocation.
 *
 * @param sourceRoot the published generated source root (equal to {@link AwsCodegenRequest#outputDirectory()})
 * @param generatedFiles generated files relative to {@code sourceRoot}, sorted
 * @param modelDigest location-independent digest of the model inputs, {@code sha256:<hex>}
 * @param resourceRoot the published generated resource root (equal to
 *        {@link AwsCodegenRequest#resourceOutputDirectory()})
 * @param generatedResources generated resources relative to {@code resourceRoot}, sorted; always includes the model
 *        provenance and the generation manifest
 * @param modelRevision the exact model revision recorded in the provenance: the requested Git commit, or
 *        {@code modelDigest} for unpinned generation
 */
public record AwsCodegenResult(Path sourceRoot, List<Path> generatedFiles, String modelDigest, Path resourceRoot,
                               List<Path> generatedResources, String modelRevision) {
    public AwsCodegenResult {
        generatedFiles = List.copyOf(generatedFiles);
        generatedResources = List.copyOf(generatedResources);
    }
}
