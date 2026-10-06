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

/**
 * The recipe extension point (design "Migration tooling flow", step 4): {@link
 * software.amazon.awssdk.v2migration.recipe.MigrationRecipe} implementations answer per encountered item with a
 * {@link software.amazon.awssdk.v2migration.recipe.RecipeOutcome}, given the item's manifest entry and its attributed
 * Java syntax ({@link software.amazon.awssdk.v2migration.recipe.JavaNode}) or Maven context
 * ({@link software.amazon.awssdk.v2migration.recipe.PomNode}). Recipes propose text edits; the engine decides
 * dispositions, rejects unsafe or conflicting edits and applies the rest deterministically.
 */
package software.amazon.awssdk.v2migration.recipe;
