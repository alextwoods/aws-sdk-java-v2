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

package software.amazon.awssdk.test.modulepath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The forbidden-dependency classes of {@code v3/bom-internal/src/main/policy/dependency-policy.xml} that no artifact
 * of a generated-client application may resolve (Requirements 15.5, 15.7, 15.8), with the policy's waivers.
 *
 * <p>The concrete HTTP class is not forbidden here because the application opts into every HTTP Implementation.
 */
final class ForbiddenDependencies {
    /** The policy's forbidden classes for a generated-client application's runtime. */
    static final Set<String> FORBIDDEN = Set.of("v2-runtime", "codegen", "smithy-model-parser",
                                                "generated-protocol-marshaller", "telemetry-exporter",
                                                "logging-implementation", "non-production");

    private record Coordinates(Pattern group, Pattern artifact, Pattern version) {
        boolean matches(String groupId, String artifactId, String version) {
            return group.matcher(groupId).matches() && artifact.matcher(artifactId).matches()
                   && this.version.matcher(version).matches();
        }
    }

    private final Map<String, List<Coordinates>> coordinates = new HashMap<>();
    private final Map<String, List<String>> v3Modules = new HashMap<>();
    private final List<String[]> waivers = new ArrayList<>();

    private ForbiddenDependencies(Document policy) {
        Map<String, String> kinds = new HashMap<>();
        NodeList modules = policy.getElementsByTagName("module");
        for (int i = 0; i < modules.getLength(); i++) {
            Element module = (Element) modules.item(i);
            if (module.hasAttribute("artifactId") && module.hasAttribute("kind")) {
                kinds.put(module.getAttribute("artifactId"), module.getAttribute("kind"));
            }
        }
        NodeList classes = policy.getElementsByTagName("artifactClass");
        for (int i = 0; i < classes.getLength(); i++) {
            Element artifactClass = (Element) classes.item(i);
            String id = artifactClass.getAttribute("id");
            if (!FORBIDDEN.contains(id)) {
                continue;
            }
            List<Coordinates> patterns = coordinates.computeIfAbsent(id, k -> new ArrayList<>());
            List<String> members = v3Modules.computeIfAbsent(id, k -> new ArrayList<>());
            NodeList coords = artifactClass.getElementsByTagName("coordinates");
            for (int j = 0; j < coords.getLength(); j++) {
                Element c = (Element) coords.item(j);
                patterns.add(new Coordinates(glob(c.getAttribute("groupId")), glob(c.getAttribute("artifactId")),
                                             glob(c.getAttribute("version"))));
            }
            NodeList kindRefs = artifactClass.getElementsByTagName("v3Modules");
            for (int j = 0; j < kindRefs.getLength(); j++) {
                String kind = ((Element) kindRefs.item(j)).getAttribute("kind");
                kinds.forEach((artifactId, k) -> {
                    if (k.equals(kind)) {
                        members.add(artifactId);
                    }
                });
            }
            NodeList moduleRefs = artifactClass.getElementsByTagName("v3Module");
            for (int j = 0; j < moduleRefs.getLength(); j++) {
                members.add(((Element) moduleRefs.item(j)).getAttribute("module"));
            }
        }
        NodeList waiverNodes = policy.getElementsByTagName("waiver");
        for (int i = 0; i < waiverNodes.getLength(); i++) {
            Element w = (Element) waiverNodes.item(i);
            waivers.add(new String[] {w.getAttribute("class"), w.getAttribute("groupId"), w.getAttribute("artifactId")});
        }
        if (!coordinates.keySet().containsAll(FORBIDDEN)) {
            throw new IllegalStateException("dependency-policy.xml lacks forbidden classes: " + FORBIDDEN);
        }
    }

    static ForbiddenDependencies load(Path policy) throws IOException {
        try (InputStream in = Files.newInputStream(policy)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return new ForbiddenDependencies(builder.parse(in));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Cannot read " + policy, e);
        }
    }

    /** The forbidden classes {@code groupId:artifactId:version} belongs to, minus waived ones. */
    List<String> violations(String gav) {
        String[] parts = gav.split(":");
        String groupId = parts[0];
        String artifactId = parts[1];
        String version = parts[2];
        boolean v3Module = "software.amazon.awssdk".equals(groupId) && !version.startsWith("2.");
        List<String> found = new ArrayList<>();
        for (String id : FORBIDDEN) {
            boolean member = coordinates.get(id).stream().anyMatch(c -> c.matches(groupId, artifactId, version))
                             || (v3Module && v3Modules.get(id).contains(artifactId));
            boolean waived = waivers.stream().anyMatch(w -> w[0].equals(id) && w[1].equals(groupId)
                                                             && w[2].equals(artifactId));
            if (member && !waived) {
                found.add(id);
            }
        }
        return found;
    }

    private static Pattern glob(String glob) {
        if (glob.isEmpty()) {
            return Pattern.compile(".*");
        }
        List<String> parts = new ArrayList<>();
        for (String part : glob.split("\\*", -1)) {
            parts.add(Pattern.quote(part));
        }
        return Pattern.compile(String.join(".*", parts));
    }
}
