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

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.pom.PomDocument;
import software.amazon.awssdk.v2migration.pom.XmlElement;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.PomNode;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/**
 * Raises the Java language level a V2 project compiles for to the V3 minimum (Requirement 17.7: migrated projects
 * compile as JDK 21 source; design "Coordinates, packages, and coexistence": consumers upgrade the JDK to 21). Each
 * {@code maven.compiler.*} property and {@code maven-compiler-plugin} language-level parameter below the minimum is
 * set to it; one at or above it is preserved; one written as an expression or in an unknown form is reported.
 */
public final class JavaReleaseRecipe implements MigrationRecipe {

    /** The Java release V3 requires: the V3 build's {@code v3.minimum.jdk.version}. */
    static final int V3_MINIMUM_JAVA_RELEASE = 21;

    /** {@code 1.5}–{@code 1.8} or a plain feature release number. */
    private static final Pattern LEVEL = Pattern.compile("1\\.([5-8])|([1-9][0-9]*)");

    @Override
    public String id() {
        return "build.java-release";
    }

    @Override
    public String description() {
        return "Raises the Maven compiler's Java source, target and release levels of a V2 project to Java "
               + V3_MINIMUM_JAVA_RELEASE + ", the V3 minimum.";
    }

    @Override
    public RecipeOutcome visit(RecipeContext context) {
        Optional<PomNode> node = context.pom();
        if (context.item().kind() != ItemKind.BUILD_SETTING || node.isEmpty() || node.get().element() == null
            || node.get().document() == null) {
            return RecipeOutcome.notApplicable();
        }
        XmlElement setting = node.get().element();
        PomDocument document = node.get().document();
        String target = String.valueOf(V3_MINIMUM_JAVA_RELEASE);
        String written = setting.text().orElse("");
        if (written.contains("${")) {
            return RecipeOutcome.unsafe("The Java level of " + setting.name() + " is the expression " + written
                                        + "; set the property it references to " + target + " or later");
        }
        Matcher m = LEVEL.matcher(written);
        if (!m.matches()) {
            return RecipeOutcome.unsafe("The Java level " + (written.isEmpty() ? "(empty)" : written) + " of "
                                        + setting.name() + " is not a recognized Java version; set it to " + target
                                        + " or later");
        }
        int level = Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(2));
        if (level >= V3_MINIMUM_JAVA_RELEASE) {
            return RecipeOutcome.preserve("The project already compiles " + setting.name() + " for Java " + written
                                          + ", at least the V3 minimum Java " + target + ".");
        }
        String raw = document.text().substring(setting.contentStart(), setting.contentEnd());
        if (raw.indexOf('<') >= 0 || raw.indexOf('&') >= 0) {
            return RecipeOutcome.unsafe("The " + setting.name() + " element contains markup (a comment, CDATA or "
                                        + "entity), so it is not rewritten; set it to " + target);
        }
        int start = setting.contentStart() + raw.indexOf(written);
        return RecipeOutcome.transform("Raise " + setting.name() + " from Java " + written + " to Java " + target
                                       + ", the V3 minimum.",
                                       List.of(new TextEdit(document.path(), start, start + written.length(), target)));
    }
}
