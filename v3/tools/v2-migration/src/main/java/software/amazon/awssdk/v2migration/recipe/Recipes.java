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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

/** Recipe discovery and ordering. */
public final class Recipes {

    private Recipes() {
    }

    /** The recipes registered with {@link ServiceLoader} on {@code loader}, in {@link #ordered} order. */
    public static List<MigrationRecipe> discover(ClassLoader loader) {
        List<MigrationRecipe> found = new ArrayList<>();
        ServiceLoader.load(MigrationRecipe.class, loader).forEach(found::add);
        return ordered(found);
    }

    /**
     * {@code recipes} sorted by ID, the order the engine always consults them in.
     *
     * @throws IllegalArgumentException if an ID is blank or used twice
     */
    public static List<MigrationRecipe> ordered(Collection<? extends MigrationRecipe> recipes) {
        Set<String> ids = new HashSet<>();
        for (MigrationRecipe recipe : recipes) {
            if (recipe.id() == null || recipe.id().isBlank()) {
                throw new IllegalArgumentException("Recipe " + recipe.getClass().getName() + " has a blank ID");
            }
            if (!ids.add(recipe.id())) {
                throw new IllegalArgumentException("Two recipes have the ID " + recipe.id());
            }
        }
        List<MigrationRecipe> sorted = new ArrayList<>(recipes);
        sorted.sort(Comparator.comparing(MigrationRecipe::id));
        return List.copyOf(sorted);
    }
}
