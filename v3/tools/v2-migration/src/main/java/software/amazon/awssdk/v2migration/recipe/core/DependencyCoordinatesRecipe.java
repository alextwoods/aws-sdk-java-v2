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
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies.Declared;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.PomNode;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/**
 * Moves the V2 dependencies, managed dependencies and BOM imports of a POM to V3 (Requirements 17.3, 17.6): the
 * version becomes the Parity Manifest's release candidate version, written where the version is defined (the
 * declaration's {@code <version>}, or the {@code <properties>} element it references), and an artifact that the
 * manifest replaces with another artifact gets the replacement's coordinates. A declaration without a version is
 * preserved when this POM manages it in a way that is migrated. Anything else (a version from a parent POM, a shared
 * property, an expression or a range) is left unchanged and reported. Artifacts that the manifest does not classify
 * {@code PRESERVED}, or {@code ADAPTED} with an artifact replacement, are left to the manifest's own diagnostic.
 */
public final class DependencyCoordinatesRecipe implements MigrationRecipe {

    @Override
    public String id() {
        return "dependencies.v3-coordinates";
    }

    @Override
    public String description() {
        return "Moves V2 dependencies, managed dependencies, BOM imports and the version properties they use to the "
               + "V3 coordinates and release candidate version recorded in the Parity Manifest.";
    }

    @Override
    public RecipeOutcome visit(RecipeContext context) {
        ItemKind kind = context.item().kind();
        Optional<PomNode> node = context.pom();
        if (node.isEmpty()) {
            return RecipeOutcome.notApplicable();
        }
        DependencyPlans plans = DependencyPlans.of(node.get(), context.manifest());
        DependencyPlans.Plan plan;
        switch (kind) {
            case DEPENDENCY:
            case MANAGED_DEPENDENCY:
            case BOM_IMPORT:
                Declared declared = node.get().declared();
                if (declared == null || !plans.target(declared.groupId(), declared.artifactId()).migratable()) {
                    // Not classified for automated migration: the manifest's disposition and diagnostic apply.
                    return RecipeOutcome.notApplicable();
                }
                plan = plans.declaration(declared);
                break;
            case VERSION_PROPERTY:
                if (node.get().element() == null) {
                    return RecipeOutcome.notApplicable();
                }
                plan = plans.property(node.get().element());
                break;
            default:
                return RecipeOutcome.notApplicable();
        }
        return outcome(plan);
    }

    static RecipeOutcome outcome(DependencyPlans.Plan plan) {
        if (plan instanceof DependencyPlans.Edits edits) {
            return RecipeOutcome.transform(edits.summary(), edits.edits());
        }
        if (plan instanceof DependencyPlans.Unchanged unchanged) {
            return RecipeOutcome.preserve(unchanged.rationale());
        }
        return RecipeOutcome.unsafe(((DependencyPlans.Blocked) plan).reason());
    }
}
