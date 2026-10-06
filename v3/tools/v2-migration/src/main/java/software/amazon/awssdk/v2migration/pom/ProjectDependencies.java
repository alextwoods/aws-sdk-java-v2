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

package software.amazon.awssdk.v2migration.pom;

import java.util.List;
import java.util.Objects;

/**
 * The effective dependency and BOM inventory of one Maven project: what its POM declares (with the effective version
 * of each declaration after property interpolation, inheritance and dependency management, including imported BOMs)
 * and what its effective dependency graph resolves to. The Maven plugin builds it from Maven's own effective model;
 * {@link PomDocument#standaloneDependencies()} approximates it from a single POM file.
 *
 * @param declared every {@code <dependency>} the POM file declares, in document order per section
 * @param resolved every artifact of the effective dependency graph (direct, inherited and transitive)
 */
public record ProjectDependencies(List<Declared> declared, List<Resolved> resolved) {

    /** The project-level {@code <dependencies>} section. */
    public static final String DEPENDENCIES = "dependencies";
    /** The project-level {@code <dependencyManagement><dependencies>} section. */
    public static final String DEPENDENCY_MANAGEMENT = "dependencyManagement";

    public ProjectDependencies {
        declared = List.copyOf(declared);
        resolved = List.copyOf(resolved);
    }

    /** The section name of {@code <profiles><profile><id>profileId</id>}'s dependencies or dependency management. */
    public static String profileSection(String profileId, boolean management) {
        return "profile:" + profileId + "/" + (management ? DEPENDENCY_MANAGEMENT : DEPENDENCIES);
    }

    /** True if {@code section} is a dependency-management section. */
    public static boolean isManagement(String section) {
        return section.endsWith(DEPENDENCY_MANAGEMENT);
    }

    /**
     * One {@code <dependency>} declaration of the POM file.
     *
     * @param section          {@link #DEPENDENCIES}, {@link #DEPENDENCY_MANAGEMENT} or a {@link #profileSection} name
     * @param index            the declaration's position among the section's {@code <dependency>} elements
     * @param groupId          the interpolated group ID
     * @param artifactId       the interpolated artifact ID
     * @param declaredVersion  the version text as written (possibly a property expression), or {@code null}
     * @param effectiveVersion the version Maven uses for the declaration, or {@code null} when unknown
     * @param type             the dependency type ({@code jar} when not written)
     * @param classifier       the classifier, or {@code null}
     * @param scope            the scope as written, or {@code null}
     */
    public record Declared(String section, int index, String groupId, String artifactId, String declaredVersion,
                           String effectiveVersion, String type, String classifier, String scope) {

        public Declared {
            Objects.requireNonNull(section, "section");
            Objects.requireNonNull(groupId, "groupId");
            Objects.requireNonNull(artifactId, "artifactId");
            type = type == null ? "jar" : type;
        }

        /** True for a {@code <type>pom</type><scope>import</scope>} BOM import. */
        public boolean isBomImport() {
            return "import".equals(scope) && "pom".equals(type);
        }

        /** {@code groupId:artifactId:effectiveVersion}. */
        public String coordinates() {
            return groupId + ":" + artifactId + ":" + (effectiveVersion == null ? "?" : effectiveVersion);
        }
    }

    /**
     * One artifact of the effective dependency graph.
     *
     * @param groupId    the group ID
     * @param artifactId the artifact ID
     * @param version    the resolved version
     * @param scope      the effective scope
     * @param trail      the {@code groupId:artifactId} path from the project's direct (or inherited) dependency to this
     *                   artifact; the first element is the direct dependency and the last is this artifact
     */
    public record Resolved(String groupId, String artifactId, String version, String scope, List<String> trail) {

        public Resolved {
            Objects.requireNonNull(groupId, "groupId");
            Objects.requireNonNull(artifactId, "artifactId");
            Objects.requireNonNull(version, "version");
            trail = trail == null || trail.isEmpty() ? List.of(groupId + ":" + artifactId) : List.copyOf(trail);
        }
    }
}
