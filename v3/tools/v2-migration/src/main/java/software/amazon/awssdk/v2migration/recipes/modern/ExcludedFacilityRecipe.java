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

import java.util.Optional;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/**
 * Halts the migration of every use of an excluded legacy signing or retry facility (Requirements 18.7-18.9): pre-SRA
 * signers and signer overrides, the legacy {@code Signer} and {@code Presigner} contracts, and legacy retry policies,
 * strategies, backoff strategies and conditions, that is every {@code EXCLUDED} Parity Manifest entry of a
 * {@link ModernFacility}. V3 has no such contract, so no rewrite is safe: the answer is always
 * {@linkplain RecipeOutcome#unsafe unsafe}, which leaves the construct byte-for-byte unchanged and blocks any other
 * recipe from transforming it. The reason names the entry's Modern Auth or Modern Retry replacement when the manifest
 * records one; without a replacement it states that no safe replacement exists and cites the exclusion rationale and
 * the documentation identifier ({@link software.amazon.awssdk.v2migration.MigrationDiagnostic#documentationId()}).
 * The engine's diagnostic carries the entry's rationale, replacement and migration text as well.
 *
 * <p>Other excluded APIs (for example deprecated generated or high-level-library members) are left to their own
 * recipe families and the engine's manifest disposition.
 */
public final class ExcludedFacilityRecipe implements MigrationRecipe {
    @Override
    public String id() {
        return "excluded.legacy-signer-retry";
    }

    @Override
    public String description() {
        return "Leaves uses of excluded legacy signer, presigner and retry APIs unchanged and names their Modern Auth "
               + "or Modern Retry replacement.";
    }

    @Override
    public RecipeOutcome visit(RecipeContext context) {
        ManifestEntry entry = context.entry().orElse(null);
        if (entry == null || entry.classification() != Classification.EXCLUDED) {
            return RecipeOutcome.notApplicable();
        }
        Optional<ModernFacility> facility = ModernFacility.of(entry);
        if (facility.isEmpty()) {
            return RecipeOutcome.notApplicable();
        }
        return RecipeOutcome.unsafe(reason(entry, facility.get()));
    }

    private static String reason(ManifestEntry entry, ModernFacility facility) {
        String legacy = "This excluded legacy " + facility.noun() + " API has no V3 equivalent contract, so the use "
                        + "is left unchanged and its migration halts.";
        if (entry.replacement() != null) {
            String replacement = facility.isModern(entry.replacement())
                                 ? facility.label() + " replacement: "
                                 : "Replacement: ";
            return legacy + " " + replacement + Names.display(entry.replacement()) + " (" + entry.replacement() + ")"
                   + (entry.migration() == null ? "." : ". " + entry.migration());
        }
        return "No safe " + facility.label() + " replacement exists for this excluded legacy " + facility.noun()
               + " API, so the use is left unchanged and its migration halts. Exclusion rationale: "
               + sentence(entry.rationale() == null ? "none recorded" : entry.rationale())
               + " Documentation: " + entry.id() + ".";
    }

    private static String sentence(String text) {
        return text.endsWith(".") ? text : text + ".";
    }
}
