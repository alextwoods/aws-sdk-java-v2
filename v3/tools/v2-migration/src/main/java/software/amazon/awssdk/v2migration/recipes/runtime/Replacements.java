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

package software.amazon.awssdk.v2migration.recipes.runtime;

import java.util.Optional;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;

/**
 * The only manifest facts these recipes act on. An entry is <em>recipe-replaced</em> when it is {@code ADAPTED}, its
 * migration is delivered by an automated {@link MigrationKind#RECIPE} and it names a replacement: the replacement ID
 * is then the exact V3 construct to convert to. Every other entry is left to the engine's manifest disposition:
 * {@code PRESERVED} entries are preserved, {@code ADAPTED} entries with a {@link MigrationKind#DIAGNOSTIC} migration
 * and {@code EXCLUDED} entries become source-located unresolved diagnostics carrying the manifest's text. No recipe
 * here keeps its own table of V2-to-V3 names, so a manifest change is the only way to change a conversion.
 */
final class Replacements {

    private Replacements() {
    }

    /** {@code entry}, if it is recipe-replaced. */
    static Optional<ManifestEntry> recipe(ManifestEntry entry) {
        if (entry != null && entry.classification() == Classification.ADAPTED
            && entry.migrationKind() == MigrationKind.RECIPE && entry.replacement() != null
            && !entry.replacement().isBlank()) {
            return Optional.of(entry);
        }
        return Optional.empty();
    }

    /** The recipe-replaced entry with {@code id}, if any. */
    static Optional<ManifestEntry> recipe(MigrationManifest manifest, String id) {
        return manifest.entry(id).flatMap(Replacements::recipe);
    }

    /** The binary name of the V3 type that replaces the V2 type {@code binaryName}, if its entry is recipe-replaced. */
    static Optional<String> type(MigrationManifest manifest, String binaryName) {
        return recipe(manifest, EntryKind.typeId(binaryName)).flatMap(e -> ApiIds.type(e.replacement()));
    }

    /** {@code binaryName}, or its recipe replacement. */
    static String mapType(MigrationManifest manifest, String binaryName) {
        return type(manifest, binaryName).orElse(binaryName);
    }

    /** {@code descriptor} with the class it denotes (also as an array component) mapped by {@link #mapType}. */
    static String mapDescriptor(MigrationManifest manifest, String descriptor) {
        int dims = 0;
        while (dims < descriptor.length() && descriptor.charAt(dims) == '[') {
            dims++;
        }
        String arrays = descriptor.substring(0, dims);
        return ApiIds.className(descriptor.substring(dims))
                     .map(c -> arrays + "L" + mapType(manifest, c).replace('.', '/') + ";")
                     .orElse(descriptor);
    }

    /**
     * True if code written against the V2 descriptor {@code v2} is written against {@code v3} once every V2 type is
     * replaced by its recipe replacement: the descriptors are equal after mapping.
     */
    static boolean compatible(MigrationManifest manifest, String v2, String v3) {
        return mapDescriptor(manifest, v2).equals(v3);
    }

    /** True if the V3 owner {@code v3Owner} is the V2 owner {@code v2Owner} or its recipe replacement. */
    static boolean sameOwner(MigrationManifest manifest, String v2Owner, String v3Owner) {
        return mapType(manifest, v2Owner).equals(v3Owner);
    }
}
