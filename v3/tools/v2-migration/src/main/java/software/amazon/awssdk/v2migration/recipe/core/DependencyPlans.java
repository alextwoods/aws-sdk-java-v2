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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.pom.PomDocument;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies.Declared;
import software.amazon.awssdk.v2migration.pom.XmlElement;
import software.amazon.awssdk.v2migration.recipe.PomNode;

/**
 * How one POM's V2 declarations move to V3, decided from the Parity Manifest alone: the V3 coordinates of an artifact
 * come from its {@code artifact:} entry (unchanged when {@code PRESERVED}, the {@code replacement} artifact when
 * {@code ADAPTED} with a recipe) and the V3 version is the manifest's release candidate version. A plan is a pure
 * function of the POM text, the dependency inventory and the manifest, so the recipes that share it (a declaration,
 * the property that supplies its version, the declaration that manages it, the artifacts that arrive through it)
 * always agree.
 */
final class DependencyPlans {

    /** The V2 and V3 Maven group. */
    static final String SDK_GROUP = "software.amazon.awssdk";
    /** The V2 and V3 BOM artifact. */
    static final String BOM = "bom";

    /** A plan for one POM element. */
    sealed interface Plan {
    }

    /** The element is rewritten by {@code edits}, all within the element. */
    record Edits(String summary, List<TextEdit> edits) implements Plan {
    }

    /** The element needs no change: its version comes from another declaration or property that is migrated. */
    record Unchanged(String rationale) implements Plan {
    }

    /** The element cannot be migrated safely and must be left unchanged. */
    record Blocked(String reason) implements Plan {
    }

    /** The V3 coordinates of a V2 artifact, or why the manifest does not allow migrating it automatically. */
    record Target(String groupId, String artifactId, String notMigratable) {

        boolean migratable() {
            return notMigratable == null;
        }

        String coordinates() {
            return groupId + ":" + artifactId;
        }
    }

    private final PomDocument document;
    private final ProjectDependencies dependencies;
    private final MigrationManifest manifest;
    private final String version;

    private DependencyPlans(PomDocument document, ProjectDependencies dependencies, MigrationManifest manifest) {
        this.document = document;
        this.dependencies = dependencies;
        this.manifest = manifest;
        this.version = manifest.baseline().candidateVersion();
    }

    static DependencyPlans of(PomNode node, MigrationManifest manifest) {
        return new DependencyPlans(node.document(), node.dependencies(), manifest);
    }

    /** The V3 coordinates of {@code groupId:artifactId}, read from its Parity Manifest artifact entry. */
    Target target(String groupId, String artifactId) {
        String coordinates = groupId + ":" + artifactId;
        Optional<ManifestEntry> found = manifest.entry(EntryKind.artifactId(groupId, artifactId));
        if (found.isEmpty()) {
            return blocked(coordinates + " has no Parity Manifest entry, so it is not part of the Launch Parity "
                           + "Baseline");
        }
        ManifestEntry entry = found.get();
        if (entry.classification() == Classification.PRESERVED) {
            if (entry.v3Artifact() == null || entry.v3Artifact().equals(coordinates)) {
                return new Target(groupId, artifactId, null);
            }
            return blocked("The Parity Manifest classifies " + coordinates + " as PRESERVED but names the V3 artifact "
                           + entry.v3Artifact());
        }
        if (ManifestIds.hasRecipeReplacement(entry)) {
            Optional<String> replacement = ManifestIds.artifact(entry.replacement());
            if (replacement.isPresent()) {
                String[] parts = replacement.get().split(":", 2);
                return new Target(parts[0], parts[1], null);
            }
        }
        return blocked(coordinates + " is " + entry.classification() + " in the Parity Manifest"
                       + (entry.migration() == null ? "" : ": " + entry.migration()));
    }

    private static Target blocked(String reason) {
        return new Target(null, null, reason);
    }

    /** The plan for a V2 declaration of this POM: its coordinates and its version. */
    Plan declaration(Declared d) {
        Target target = target(d.groupId(), d.artifactId());
        if (!target.migratable()) {
            return new Blocked(target.notMigratable());
        }
        if (version == null) {
            return new Blocked("The Parity Manifest names no V3 release candidate version, so the V3 version of "
                               + target.coordinates() + " is unknown; migrate with a candidate Parity Manifest");
        }
        Optional<XmlElement> located = element(d);
        if (located.isEmpty()) {
            return new Blocked("The declaration of " + d.groupId() + ":" + d.artifactId()
                               + " was not located in an editable POM");
        }
        XmlElement element = located.get();
        List<TextEdit> edits = new ArrayList<>();
        List<String> changes = new ArrayList<>();
        for (String[] coordinate : List.of(new String[] {"groupId", d.groupId(), target.groupId()},
                                           new String[] {"artifactId", d.artifactId(), target.artifactId()})) {
            if (coordinate[1].equals(coordinate[2])) {
                continue;
            }
            Optional<TextEdit> edit = element.child(coordinate[0]).flatMap(c -> replaceLiteral(c, coordinate[1],
                                                                                                coordinate[2]));
            if (edit.isEmpty()) {
                return new Blocked("The " + coordinate[0] + " of " + d.groupId() + ":" + d.artifactId()
                                   + " is not written as a literal, so it cannot be changed to " + coordinate[2]);
            }
            edits.add(edit.get());
            changes.add(coordinate[0] + " " + coordinate[1] + " -> " + coordinate[2]);
        }
        String versionSource;
        Optional<XmlElement> versionElement = element.child("version");
        if (versionElement.isPresent()) {
            Optional<String> property = d.versionProperty();
            if (property.isPresent()) {
                Optional<XmlElement> definition = propertyElement(property.get());
                if (definition.isEmpty()) {
                    return new Blocked("The version comes from the property ${" + property.get() + "}, which this POM "
                                       + "does not define (for example a parent POM or the command line defines it); "
                                       + "set it to " + version + " where it is defined");
                }
                Plan plan = property(definition.get());
                if (plan instanceof Blocked blocked) {
                    return new Blocked("The version comes from the property ${" + property.get() + "}, which cannot be "
                                       + "migrated: " + blocked.reason());
                }
                versionSource = "its version comes from the property ${" + property.get() + "}, which is set to "
                                + version;
            } else {
                String written = versionElement.get().text().orElse("");
                if (!written.startsWith("2.") || written.contains("${")) {
                    return new Blocked("The version " + written + " is not a literal 2.x version (version ranges and "
                                       + "expressions are not rewritten); set it to " + version);
                }
                Optional<TextEdit> edit = replaceLiteral(versionElement.get(), written, version);
                if (edit.isEmpty()) {
                    return new Blocked("The version element contains markup (a comment, CDATA or entity), so it is not "
                                       + "rewritten; set it to " + version);
                }
                edits.add(edit.get());
                changes.add("version " + written + " -> " + version);
                versionSource = null;
            }
        } else if (ProjectDependencies.isManagement(d.section())) {
            return new Blocked("The managed declaration has no version; set it to " + version);
        } else {
            Plan managed = managedVersion(d);
            if (managed instanceof Blocked) {
                return managed;
            }
            versionSource = ((Unchanged) managed).rationale();
        }
        String subject = d.groupId() + ":" + d.artifactId();
        if (edits.isEmpty()) {
            return new Unchanged("The declaration of " + subject + " needs no change: " + versionSource + ".");
        }
        return new Edits("Migrate " + subject + " to V3 (" + String.join(", ", changes) + ")"
                         + (versionSource == null ? "" : "; " + versionSource) + ".", edits);
    }

    /** Where an unversioned dependency's version comes from, if this POM manages it in a way that is migrated. */
    private Plan managedVersion(Declared d) {
        for (Declared m : dependencies.declared()) {
            if (ProjectDependencies.isManagement(m.section()) && !m.isBomImport() && m.groupId().equals(d.groupId())
                && m.artifactId().equals(d.artifactId()) && Objects.equals(m.type(), d.type())
                && Objects.equals(m.classifier(), d.classifier()) && element(m).isPresent()) {
                Plan plan = declaration(m);
                if (plan instanceof Blocked blocked) {
                    return new Blocked("The version is managed by the " + m.section() + " declaration of "
                                       + m.groupId() + ":" + m.artifactId() + ", which cannot be migrated: "
                                       + blocked.reason());
                }
                return new Unchanged("its version is managed by the " + m.section() + " declaration, which is migrated");
            }
        }
        Optional<Declared> bom = bomImport();
        if (bom.isPresent() && !(declaration(bom.get()) instanceof Blocked)) {
            return new Unchanged("its version is managed by the " + SDK_GROUP + ":" + BOM + " import, which is migrated");
        }
        return new Blocked("The version (" + (d.effectiveVersion() == null ? "unknown" : d.effectiveVersion())
                           + ") is managed outside this POM, for example by a parent POM or a third-party BOM; update "
                           + "the managing declaration to " + (version == null ? "the V3 version" : version));
    }

    /** The plan for a {@code <properties>} element that supplies the version of V2 declarations. */
    Plan property(XmlElement property) {
        String name = property.name();
        long definitions = document.project().child("properties").map(p -> p.children(name).size()).orElse(0);
        if (definitions != 1) {
            return new Blocked("The property " + name + " is defined " + definitions + " times in this POM");
        }
        String value = property.text().orElse("");
        if (!value.startsWith("2.") || value.contains("${")) {
            return new Blocked("The property value " + value + " is not a literal 2.x version");
        }
        if (version == null) {
            return new Blocked("The Parity Manifest names no V3 release candidate version, so the V3 value of "
                               + name + " is unknown; migrate with a candidate Parity Manifest");
        }
        List<Declared> users = dependencies.declared().stream()
                                           .filter(d -> d.versionProperty().filter(name::equals).isPresent())
                                           .toList();
        for (Declared user : users) {
            String coordinates = user.groupId() + ":" + user.artifactId();
            if (!SDK_GROUP.equals(user.groupId())) {
                return new Blocked("The property also supplies the version of " + coordinates + ", which is not an "
                                   + "SDK artifact");
            }
            Target target = target(user.groupId(), user.artifactId());
            if (!target.migratable()) {
                return new Blocked("The property also supplies the version of " + coordinates + ", which cannot be "
                                   + "migrated automatically: " + target.notMigratable());
            }
        }
        int references = occurrences(document.text(), "${" + name + "}");
        if (references != users.size()) {
            return new Blocked("This POM references ${" + name + "} " + references + " times, but only " + users.size()
                               + " of them are SDK dependency versions; changing it would change the other uses too");
        }
        Optional<TextEdit> edit = replaceLiteral(property, value, version);
        if (edit.isEmpty()) {
            return new Blocked("The property element contains markup (a comment, CDATA or entity), so it is not "
                               + "rewritten");
        }
        String artifacts = users.stream().map(u -> u.groupId() + ":" + u.artifactId()).distinct().sorted()
                                .collect(Collectors.joining(", "));
        return new Edits("Set the property " + name + " from " + value + " to the V3 version " + version + " ("
                         + artifacts + ").", List.of(edit.get()));
    }

    /** The {@code <dependency>} element of {@code d} in this POM, confirmed by its written artifact ID. */
    Optional<XmlElement> element(Declared d) {
        if (document == null) {
            return Optional.empty();
        }
        return document.dependency(d.section(), d.index())
                       .filter(e -> e.childText("artifactId").map(a -> a.equals(d.artifactId()) || a.contains("${"))
                                     .orElse(false));
    }

    /** The located declaration of a {@code groupId:artifactId} in this POM's (non-management) dependencies. */
    Optional<Declared> directDependency(String coordinates) {
        return dependencies.declared().stream()
                           .filter(d -> !ProjectDependencies.isManagement(d.section())
                                        && coordinates.equals(d.groupId() + ":" + d.artifactId())
                                        && element(d).isPresent())
                           .findFirst();
    }

    /** This POM's located {@code software.amazon.awssdk:bom} import, if any. */
    Optional<Declared> bomImport() {
        return dependencies.declared().stream()
                           .filter(d -> d.isBomImport() && SDK_GROUP.equals(d.groupId()) && BOM.equals(d.artifactId())
                                        && element(d).isPresent())
                           .findFirst();
    }

    private Optional<XmlElement> propertyElement(String name) {
        if (document == null) {
            return Optional.empty();
        }
        return document.project().child("properties").flatMap(p -> p.child(name));
    }

    /**
     * Replaces the text {@code expected} of a leaf element with {@code replacement}, keeping the surrounding whitespace,
     * or empty if the element's content is anything but whitespace around exactly {@code expected}.
     */
    private Optional<TextEdit> replaceLiteral(XmlElement element, String expected, String replacement) {
        if (!element.children().isEmpty()) {
            return Optional.empty();
        }
        String raw = document.text().substring(element.contentStart(), element.contentEnd());
        String stripped = raw.strip();
        if (!stripped.equals(expected) || raw.indexOf('<') >= 0 || raw.indexOf('&') >= 0) {
            return Optional.empty();
        }
        int start = element.contentStart() + raw.indexOf(stripped);
        return Optional.of(new TextEdit(document.path(), start, start + stripped.length(), replacement));
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        for (int i = text.indexOf(token); i >= 0; i = text.indexOf(token, i + token.length())) {
            count++;
        }
        return count;
    }
}
