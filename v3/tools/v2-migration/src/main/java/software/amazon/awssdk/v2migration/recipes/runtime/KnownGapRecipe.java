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
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/**
 * {@code hll.known-gaps}: every construct that joins a known V3 launch gap of the high-level-library parity matrix
 * ({@link KnownGaps}) is unsafe, whatever its manifest classification claims, so it is never preserved silently or
 * rewritten by another recipe; the diagnostic names the library and the gap. Examples are the V2 {@code SdkPublisher}
 * operators applied to enhanced-client and paginator publishers, the {@code AttributeValue} {@code nul} members used by
 * custom attribute converters, and the Transfer Manager presigned-URL download API.
 */
public final class KnownGapRecipe implements MigrationRecipe {

    private final KnownGaps gaps;

    public KnownGapRecipe() {
        this(KnownGaps.bundled());
    }

    KnownGapRecipe(KnownGaps gaps) {
        this.gaps = gaps;
    }

    @Override
    public String id() {
        return "hll.known-gaps";
    }

    @Override
    public String description() {
        return "Reports constructs that use known V3 launch gaps of the high-level-library parity matrix and leaves "
               + "them unchanged.";
    }

    @Override
    public RecipeOutcome visit(RecipeContext context) {
        String id = context.item().id();
        Optional<KnownGaps.Gap> gap = id == null ? Optional.empty() : gaps.gapOf(id);
        if (gap.isEmpty()) {
            return RecipeOutcome.notApplicable();
        }
        return RecipeOutcome.unsafe("V3 does not ship or has not verified " + id + ": it is a known launch gap of the "
                                    + gap.get().library() + " parity matrix. " + gap.get().reason()
                                    + " Keep V2 for this use or rewrite it manually; the construct is left unchanged.");
    }
}
