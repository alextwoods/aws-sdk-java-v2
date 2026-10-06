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

/**
 * A deterministic V2-to-V3 migration recipe: the extension point that recipe families plug into. Implementations are
 * discovered with {@link java.util.ServiceLoader} ({@code META-INF/services/software.amazon.awssdk.v2migration.recipe.MigrationRecipe})
 * and always run in {@link #id()} order.
 *
 * <p>The engine offers every encountered item to every recipe. A recipe answers with a {@link RecipeOutcome}:
 * {@linkplain RecipeOutcome#notApplicable() not applicable}, {@linkplain RecipeOutcome#preserve(String) preserved},
 * {@linkplain RecipeOutcome#transform(String, java.util.List) transformed} by text edits, or
 * {@linkplain RecipeOutcome#unsafe(String) unsafe}. The engine then assigns the item exactly one disposition:
 * <ul>
 *   <li>any {@code unsafe} answer, or answers that disagree (two transformations, or a transformation and a
 *       preservation), leave the item unresolved;</li>
 *   <li>one {@code transform} answer transforms the item, provided its edits stay within the item's own range (or are
 *       insertions), do not overlap another item's edits, and do not touch the text of any unresolved item;
 *       otherwise the item is unresolved and its text is left unchanged;</li>
 *   <li>{@code preserve} answers preserve the item;</li>
 *   <li>without an answer, the Parity Manifest decides: {@code PRESERVED} entries are preserved, and {@code ADAPTED} or
 *       {@code EXCLUDED} entries, or constructs without an entry, are unresolved with the entry's diagnostic text.</li>
 * </ul>
 *
 * <p>Recipes must be pure functions of the {@link RecipeContext}: the same project and manifest must always produce
 * the same answers, so migrations are reproducible. A recipe that throws is treated as answering {@code unsafe}.
 */
public interface MigrationRecipe {

    /** The stable recipe ID, for example {@code dependencies.v3-version}; reported with every decision it makes. */
    String id();

    /** A one-sentence description for the migration report. */
    String description();

    /** Answers for one item. */
    RecipeOutcome visit(RecipeContext context);
}
