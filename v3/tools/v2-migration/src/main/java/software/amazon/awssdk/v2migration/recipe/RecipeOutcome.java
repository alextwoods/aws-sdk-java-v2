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

import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.v2migration.TextEdit;

/** A recipe's answer for one item; see {@link MigrationRecipe} for how answers become dispositions. */
public sealed interface RecipeOutcome {

    /** The recipe does not handle the item. */
    static RecipeOutcome notApplicable() {
        return NotApplicable.INSTANCE;
    }

    /** The construct needs no change in V3, for example an adapted hook whose semantics match; {@code rationale} says why. */
    static RecipeOutcome preserve(String rationale) {
        return new Preserve(rationale);
    }

    /** The construct is rewritten by {@code edits}; {@code summary} describes the change. */
    static RecipeOutcome transform(String summary, List<TextEdit> edits) {
        return new Transform(summary, edits);
    }

    /** The construct cannot be transformed safely; it is left unchanged and {@code reason} is reported. */
    static RecipeOutcome unsafe(String reason) {
        return new Unsafe(reason);
    }

    /** See {@link #notApplicable()}. */
    enum NotApplicable implements RecipeOutcome {
        INSTANCE
    }

    /** See {@link #preserve(String)}. */
    record Preserve(String rationale) implements RecipeOutcome {
        public Preserve {
            requireText(rationale, "rationale");
        }
    }

    /** See {@link #transform(String, List)}. */
    record Transform(String summary, List<TextEdit> edits) implements RecipeOutcome {
        public Transform {
            requireText(summary, "summary");
            edits = List.copyOf(edits);
            if (edits.isEmpty()) {
                throw new IllegalArgumentException("A transformation needs at least one edit");
            }
        }
    }

    /** See {@link #unsafe(String)}. */
    record Unsafe(String reason) implements RecipeOutcome {
        public Unsafe {
            requireText(reason, "reason");
        }
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
