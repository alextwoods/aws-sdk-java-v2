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

package software.amazon.awssdk.tools.paritymanifest;

import java.util.Objects;

/**
 * Where an inventory item was found in the V2 baseline.
 *
 * @param artifact the owning V2 artifact as {@code groupId:artifactId}, or a non-artifact origin such as
 *                 {@code docs} for documented behaviors
 * @param path     the repository-relative file or jar entry, or {@code null} when the artifact is the location
 */
public record SourceLocation(String artifact, String path) {
    public SourceLocation {
        Objects.requireNonNull(artifact, "artifact");
    }
}
