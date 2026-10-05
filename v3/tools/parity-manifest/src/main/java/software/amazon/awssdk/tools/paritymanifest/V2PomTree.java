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

package software.amazon.awssdk.tools.paritymanifest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * The V2 Maven aggregator tree: every module reachable from the root {@code pom.xml} through {@code <modules>} (not
 * through profiles, so the V3 reactor, which V2 adds only in a profile, is never part of the baseline).
 *
 * <p>A module is <em>published</em> unless (a) its POM skips {@code maven-deploy-plugin} (or sets
 * {@code maven.deploy.skip} to {@code true}), or (b) it lives under the V2 {@code test/} directory (test
 * infrastructure, benchmarks and test fixtures, which are not customer-facing even where they are deployed), or (c) it
 * is in {@link #UNPUBLISHED}, the curated list of release tooling not published for customers.
 */
final class V2PomTree {

    /** Curated V2 modules that are release tooling rather than customer artifacts. */
    static final Set<String> UNPUBLISHED = Set.of("release-scripts");

    /**
     * One V2 module.
     *
     * @param artifactId  the module's artifact ID
     * @param groupId     its group ID (inherited from the parent when absent)
     * @param packaging   its packaging ({@code jar} when absent)
     * @param path        its directory relative to the repository root, with {@code /} separators
     * @param published   whether it is a published customer artifact (see the class documentation)
     */
    record Module(String artifactId, String groupId, String packaging, String path, boolean published) {
    }

    private V2PomTree() {
    }

    /** Every module of the tree below {@code repositoryRoot}, sorted by path; the root itself is included. */
    static List<Module> modules(Path repositoryRoot) throws IOException {
        List<Module> result = new ArrayList<>();
        collect(repositoryRoot, repositoryRoot, null, result);
        result.sort(Comparator.comparing(Module::path));
        return List.copyOf(result);
    }

    private static void collect(Path root, Path dir, String parentGroup, List<Module> out) throws IOException {
        Path pomFile = dir.resolve("pom.xml");
        Element project = parse(pomFile);
        String groupId = childText(project, "groupId");
        if (groupId == null) {
            Element parent = child(project, "parent");
            groupId = parent == null ? parentGroup : childText(parent, "groupId");
        }
        String artifactId = childText(project, "artifactId");
        String packaging = childText(project, "packaging");
        String path = root.relativize(dir).toString().replace('\\', '/');
        boolean published = !skipsDeploy(project) && !path.equals("test") && !path.startsWith("test/")
                            && !UNPUBLISHED.contains(artifactId);
        out.add(new Module(artifactId, groupId, packaging == null ? "jar" : packaging, path, published));
        Element modules = child(project, "modules");
        if (modules == null) {
            return;
        }
        for (Node n = modules.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getTagName().equals("module")) {
                Path moduleDir = dir.resolve(e.getTextContent().strip()).normalize();
                if (Files.isRegularFile(moduleDir.resolve("pom.xml"))) {
                    collect(root, moduleDir, groupId, out);
                }
            }
        }
    }

    /** True if the POM's own build skips {@code maven-deploy-plugin} or sets {@code maven.deploy.skip}. */
    private static boolean skipsDeploy(Element project) {
        Element properties = child(project, "properties");
        if (properties != null && "true".equals(childText(properties, "maven.deploy.skip"))) {
            return true;
        }
        Element build = child(project, "build");
        Element plugins = build == null ? null : child(build, "plugins");
        if (plugins == null) {
            return false;
        }
        for (Node n = plugins.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element plugin && "maven-deploy-plugin".equals(childText(plugin, "artifactId"))) {
                Element configuration = child(plugin, "configuration");
                if (configuration != null && "true".equals(childText(configuration, "skip"))) {
                    return true;
                }
            }
        }
        return false;
    }

    static Element parse(Path pom) throws IOException {
        try (InputStream in = Files.newInputStream(pom)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(in).getDocumentElement();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Cannot read " + pom, e);
        }
    }

    static Element child(Element parent, String tag) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getTagName().equals(tag)) {
                return e;
            }
        }
        return null;
    }

    static String childText(Element parent, String tag) {
        Element e = child(parent, tag);
        return e == null ? null : e.getTextContent().strip();
    }
}
