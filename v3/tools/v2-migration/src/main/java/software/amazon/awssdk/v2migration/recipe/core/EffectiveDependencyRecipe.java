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
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies.Declared;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.PomNode;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/**
 * Decides the V2 artifacts of the effective dependency graph that the POM does not declare. An artifact that arrives
 * through an SDK dependency of this POM that is itself migrated is preserved: the V3 artifact brings its own V3
 * dependencies. An artifact that arrives through anything else (a third-party library built against V2, or a parent
 * POM) is reported, because V2 and V3 share class names and cannot both be on one class path, and a library built
 * against V2 may rely on V2 behavior; the owner must upgrade or exclude it.
 */
public final class EffectiveDependencyRecipe implements MigrationRecipe {

    @Override
    public String id() {
        return "dependencies.effective-graph";
    }

    @Override
    public String description() {
        return "Preserves transitive V2 artifacts that arrive through migrated SDK dependencies and reports those "
               + "that arrive through third-party or inherited dependencies.";
    }

    @Override
    public RecipeOutcome visit(RecipeContext context) {
        Optional<PomNode> node = context.pom();
        if (context.item().kind() != ItemKind.EFFECTIVE_DEPENDENCY || node.isEmpty()
            || node.get().resolved() == null) {
            return RecipeOutcome.notApplicable();
        }
        ProjectDependencies.Resolved artifact = node.get().resolved();
        DependencyPlans plans = DependencyPlans.of(node.get(), context.manifest());
        if (!plans.target(artifact.groupId(), artifact.artifactId()).migratable()) {
            return RecipeOutcome.notApplicable();
        }
        String coordinates = artifact.groupId() + ":" + artifact.artifactId();
        String direct = artifact.trail().get(0);
        Optional<Declared> through = plans.directDependency(direct);
        if (through.isEmpty()) {
            return RecipeOutcome.unsafe(coordinates + " is inherited through " + direct + ", which this POM does not "
                                        + "declare (for example a parent POM declares it); migrate the POM that "
                                        + "declares it");
        }
        if (!DependencyPlans.SDK_GROUP.equals(through.get().groupId())) {
            return RecipeOutcome.unsafe(coordinates + " arrives through the third-party dependency " + direct
                                        + ", which was built against V2; V2 and V3 cannot share a class path, so use a "
                                        + "release of " + direct + " that supports V3, or exclude the V2 artifact and "
                                        + "verify " + direct + " against V3");
        }
        DependencyPlans.Plan plan = plans.declaration(through.get());
        if (plan instanceof DependencyPlans.Blocked blocked) {
            return RecipeOutcome.unsafe(coordinates + " arrives through " + direct + ", which cannot be migrated: "
                                        + blocked.reason());
        }
        return RecipeOutcome.preserve(coordinates + " arrives through " + direct + ", which is migrated to V3; the "
                                      + "V3 artifact brings its own V3 dependencies.");
    }
}
