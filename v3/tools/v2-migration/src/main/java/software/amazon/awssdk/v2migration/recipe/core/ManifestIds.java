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

package software.amazon.awssdk.v2migration.recipe.core;

import java.util.Optional;
import javax.lang.model.SourceVersion;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;

/**
 * Parsing of the Parity Manifest entry IDs that the core recipes act on. Every transformation these recipes make is
 * read from an entry's classification and {@code replacement} ID, never from a recipe-side table, so recipes cannot
 * drift from the manifest.
 */
final class ManifestIds {

    private static final String API = "api:";

    private ManifestIds() {
    }

    /** A public member ID: {@code api:<owner>#<name><descriptor>}, where a field descriptor starts with {@code :}. */
    record Member(String owner, String name, String descriptor) {

        /** True for a field ({@code name:descriptor}). */
        boolean isField() {
            return descriptor.startsWith(":");
        }

        /** True for a constructor. */
        boolean isConstructor() {
            return name.equals("<init>");
        }
    }

    /** A configuration ID: {@code config:<source>:<key>}. */
    record Configuration(String source, String key) {
    }

    /**
     * True if {@code entry} is {@code ADAPTED} with an automated recipe and names its replacement: the only entries
     * the core recipes transform.
     */
    static boolean hasRecipeReplacement(ManifestEntry entry) {
        return entry != null && entry.classification() == Classification.ADAPTED
               && entry.migrationKind() == MigrationKind.RECIPE && entry.replacement() != null;
    }

    /** The binary name of a {@link EntryKind#PUBLIC_TYPE} ID. */
    static Optional<String> type(String id) {
        return EntryKind.PUBLIC_TYPE.accepts(id) ? Optional.of(id.substring(API.length())) : Optional.empty();
    }

    /** The parts of a {@link EntryKind#PUBLIC_MEMBER} ID. */
    static Optional<Member> member(String id) {
        if (!EntryKind.PUBLIC_MEMBER.accepts(id)) {
            return Optional.empty();
        }
        String body = id.substring(API.length());
        int hash = body.indexOf('#');
        String owner = body.substring(0, hash);
        String rest = body.substring(hash + 1);
        int paren = rest.indexOf('(');
        int colon = rest.indexOf(':');
        int split = paren >= 0 && (colon < 0 || paren < colon) ? paren : colon;
        return Optional.of(new Member(owner, rest.substring(0, split), rest.substring(split)));
    }

    /** The parts of a {@link EntryKind#CONFIGURATION} ID. */
    static Optional<Configuration> configuration(String id) {
        if (!EntryKind.CONFIGURATION.accepts(id)) {
            return Optional.empty();
        }
        String body = id.substring("config:".length());
        int colon = body.indexOf(':');
        return Optional.of(new Configuration(body.substring(0, colon), body.substring(colon + 1)));
    }

    /** The {@code groupId:artifactId} of an {@link EntryKind#ARTIFACT} ID. */
    static Optional<String> artifact(String id) {
        return EntryKind.ARTIFACT.accepts(id) ? Optional.of(id.substring("artifact:".length())) : Optional.empty();
    }

    /** The binary name the manifest renames type {@code binaryName} to, if its entry is a recipe-backed rename. */
    static Optional<String> renamedType(MigrationManifest manifest, String binaryName) {
        return manifest.entry(EntryKind.typeId(binaryName))
                       .filter(ManifestIds::hasRecipeReplacement)
                       .flatMap(e -> type(e.replacement()))
                       .filter(r -> !canonical(r).equals(canonical(binaryName)));
    }

    /** The source (canonical) name of a binary name: nested types are separated by dots. */
    static String canonical(String binaryName) {
        return binaryName.replace('$', '.');
    }

    /** The simple name of a binary name. */
    static String simpleName(String binaryName) {
        return binaryName.substring(Math.max(binaryName.lastIndexOf('.'), binaryName.lastIndexOf('$')) + 1);
    }

    /** True if {@code name} can be written as a Java identifier in source. */
    static boolean isIdentifier(String name) {
        return SourceVersion.isIdentifier(name) && !SourceVersion.isKeyword(name);
    }

    /**
     * {@code descriptor} with every class it references that the manifest renames ({@link #renamedType}) replaced by
     * its replacement: the descriptor a member keeps in V3 when only the types around it are renamed.
     */
    static String substituteRenamed(MigrationManifest manifest, String descriptor) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < descriptor.length()) {
            char c = descriptor.charAt(i);
            int end = c == 'L' ? descriptor.indexOf(';', i) : -1;
            if (end < 0) {
                out.append(c);
                i++;
                continue;
            }
            String binary = descriptor.substring(i + 1, end).replace('/', '.');
            String replaced = renamedType(manifest, binary).orElse(binary);
            out.append('L').append(replaced.replace('.', '/')).append(';');
            i = end + 1;
        }
        return out.toString();
    }

    /** The diagnostic phrase for a manifest entry: its ID and classification. */
    static String describe(ManifestEntry entry) {
        return entry.id() + " (" + entry.classification() + ")";
    }
}
