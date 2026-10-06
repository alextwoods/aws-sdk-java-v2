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

package software.amazon.awssdk.v2migration.recipe;

import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.v2migration.MigrationItem;
import software.amazon.awssdk.v2migration.SourceLocation;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;

/** What a {@link MigrationRecipe} sees for one item. All text is the project as originally read. */
public interface RecipeContext {

    /** The item. */
    MigrationItem item();

    /** The item's Parity Manifest entry, if it has one. */
    Optional<ManifestEntry> entry();

    /** The whole manifest, for example to look up the entry of a replacement. */
    MigrationManifest manifest();

    /** The project-relative paths of every file a recipe may edit. */
    Set<String> editablePaths();

    /** The original text of the editable file at {@code path}. */
    String text(String path);

    /** The original text of {@code location}. */
    default String text(SourceLocation location) {
        return text(location.path()).substring(location.startOffset(), location.endOffset());
    }

    /** The attributed Java syntax of a source item. */
    Optional<JavaNode> java();

    /** The Maven context of a dependency item. */
    Optional<PomNode> pom();
}
