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

package software.amazon.awssdk.v2migration.recipes.modern;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.MigrationCategory;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;

/**
 * The two V3 security and resilience facilities these recipes migrate to (Requirement 18), and which V2 constructs
 * belong to each. A Parity Manifest entry is a signing ({@link #AUTH}) or retry ({@link #RETRY}) facility when
 * <ol>
 *   <li>its V2 owner type is in the engine's {@link MigrationCategory#SIGNER} or {@link MigrationCategory#RETRY}
 *       category, or in a V2 {@code signer}, {@code retry} or {@code retries} package;</li>
 *   <li>it is a member named for signing or retrying (such as {@code RequestOverrideConfiguration.Builder#signer},
 *       {@code SdkAdvancedClientOption#SIGNER} or {@code ClientOverrideConfiguration.Builder#retryPolicy}); or</li>
 *   <li>its manifest replacement is a Modern Auth or Modern Retry API.</li>
 * </ol>
 * Which facility, and which replacement, always comes from the entry: nothing here decides a disposition.
 */
enum ModernFacility {
    /** Modern Auth: auth schemes, {@code HttpSigner} and the service presigners. */
    AUTH("Modern Auth", "signer", MigrationCategory.SIGNER, Set.of("signer"),
         List.of("software.amazon.awssdk.auth.api.", "software.amazon.awssdk.auth.aws.")),
    /** Modern Retry: {@code RetryStrategy} and its backoff and classification contracts. */
    RETRY("Modern Retry", "retry", MigrationCategory.RETRY, Set.of("retry", "retries"),
          List.of("software.amazon.awssdk.retries."));

    private static final String V2 = "software.amazon.awssdk.";
    private static final String API = "api:";

    private final String label;
    private final String noun;
    private final MigrationCategory category;
    private final Set<String> segments;
    private final List<String> v3Packages;

    ModernFacility(String label, String noun, MigrationCategory category, Set<String> segments,
                   List<String> v3Packages) {
        this.label = label;
        this.noun = noun;
        this.category = category;
        this.segments = segments;
        this.v3Packages = v3Packages;
    }

    /** The facility's name in diagnostics, for example {@code Modern Retry}. */
    String label() {
        return label;
    }

    /** What a legacy construct of the facility is, for example {@code signer}. */
    String noun() {
        return noun;
    }

    /** True if {@code entry} belongs to this facility. */
    boolean owns(ManifestEntry entry) {
        String body = entry.id().startsWith(API) ? entry.id().substring(API.length()) : null;
        if (body != null) {
            int hash = body.indexOf('#');
            String owner = hash < 0 ? body : body.substring(0, hash);
            if (MigrationCategory.of(ItemKind.TYPE_REFERENCE, EntryKind.typeId(owner)) == category
                || inFacilityPackage(owner)) {
                return true;
            }
            if (hash >= 0) {
                String member = body.substring(hash + 1).split("[(:]", 2)[0].toLowerCase(Locale.ROOT);
                if (member.contains(noun)) {
                    return true;
                }
            }
        }
        return isModern(entry.replacement());
    }

    /** True if {@code replacementId} is a non-internal API of this facility's V3 packages. */
    boolean isModern(String replacementId) {
        if (replacementId == null || !replacementId.startsWith(API) || replacementId.contains(".internal.")) {
            return false;
        }
        String name = replacementId.substring(API.length());
        return v3Packages.stream().anyMatch(name::startsWith);
    }

    /** The facility of {@code entry}, if it has one. */
    static Optional<ModernFacility> of(ManifestEntry entry) {
        for (ModernFacility f : values()) {
            if (f.owns(entry)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    private boolean inFacilityPackage(String binaryName) {
        if (!binaryName.startsWith(V2)) {
            return false;
        }
        String[] parts = binaryName.substring(V2.length()).split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            if (segments.contains(parts[i])) {
                return true;
            }
        }
        return false;
    }
}
