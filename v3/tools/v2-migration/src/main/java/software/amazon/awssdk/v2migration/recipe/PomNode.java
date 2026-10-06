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

import java.util.Optional;
import software.amazon.awssdk.v2migration.pom.PomDocument;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;
import software.amazon.awssdk.v2migration.pom.XmlElement;

/**
 * The Maven context of a POM item: the POM, the project's effective dependency inventory, and either the
 * declaration (with its {@code <dependency>} element when the POM file is editable), the effective-graph artifact, or
 * the property or build-setting element of a {@link software.amazon.awssdk.v2migration.ItemKind#VERSION_PROPERTY} or
 * {@link software.amazon.awssdk.v2migration.ItemKind#BUILD_SETTING} item.
 *
 * @param document     the POM file, or {@code null} when the project has none the tool may edit
 * @param dependencies the project's effective dependency inventory
 * @param declared     the declaration of a declared item, or {@code null}
 * @param element      the {@code <dependency>} element of a declared item in {@code document}, the element of a
 *                     version-property or build-setting item, or {@code null}
 * @param resolved     the artifact of an {@link software.amazon.awssdk.v2migration.ItemKind#EFFECTIVE_DEPENDENCY} item,
 *                     or {@code null}
 */
public record PomNode(PomDocument document, ProjectDependencies dependencies, ProjectDependencies.Declared declared,
                      XmlElement element, ProjectDependencies.Resolved resolved) {

    /** The POM file, if editable. */
    public Optional<PomDocument> pom() {
        return Optional.ofNullable(document);
    }

    /** The {@code <dependency>} element, if located in an editable POM. */
    public Optional<XmlElement> dependencyElement() {
        return Optional.ofNullable(element);
    }
}
