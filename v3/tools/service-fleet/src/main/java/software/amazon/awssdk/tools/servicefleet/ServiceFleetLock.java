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

package software.amazon.awssdk.tools.servicefleet;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.tools.servicefleet.CanonicalModel.ModelFile;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * Reads and writes {@code v3/services/service-fleet.json}, the pinned {@link FleetPlan}. The JSON is written with a
 * fixed member order, sorted arrays and {@code \n} line endings, so identical plans produce identical bytes.
 */
public final class ServiceFleetLock {

    /** The lock file, relative to the V3 root. */
    public static final String PATH = "services/service-fleet.json";

    static final int FORMAT_VERSION = 1;

    private ServiceFleetLock() {
    }

    /** The JSON of {@code plan}. */
    public static String write(FleetPlan plan) {
        ObjectNode.Builder root = Node.objectNodeBuilder();
        root.withMember("formatVersion", FORMAT_VERSION);
        root.withMember("canonicalModels", Node.objectNodeBuilder()
                                               .withMember("repository", FleetPlan.CANONICAL_REPOSITORY)
                                               .withMember("revision", plan.revision())
                                               .build());
        List<Node> services = new ArrayList<>();
        for (FleetService s : plan.services()) {
            services.add(Node.objectNodeBuilder()
                             .withMember("name", s.name())
                             .withMember("module", s.module())
                             .withMember("packageName", s.packageName())
                             .withMember("service", s.service())
                             .withMember("sdkId", s.sdkId())
                             .withMember("canonicalDirectory", s.canonicalDirectory())
                             .withMember("modelDirectory", s.modelDirectory())
                             .withMember("modelDigest", s.modelDigest())
                             .withMember("modelFiles", ArrayNode.fromNodes(s.modelFiles().stream().map(
                                 f -> (Node) Node.objectNodeBuilder().withMember("path", f.path())
                                                 .withMember("digest", f.digest()).build()).toList()))
                             .withMember("protocolModules", strings(s.protocolModules()))
                             .withMember("requiredTypes", strings(s.requiredTypes()))
                             .build());
        }
        root.withMember("services", ArrayNode.fromNodes(services));
        root.withMember("missing", strings(plan.missing()));
        root.withMember("unmappedCanonicalModels", strings(plan.unmappedCanonicalModels()));
        return Node.prettyPrintJson(root.build(), "  ") + "\n";
    }

    /** The plan in {@code json}. */
    public static FleetPlan read(String json) {
        ObjectNode root = Node.parse(json).expectObjectNode();
        int version = root.expectNumberMember("formatVersion").getValue().intValue();
        if (version != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported service fleet formatVersion " + version);
        }
        String revision = root.expectObjectMember("canonicalModels").expectStringMember("revision").getValue();
        List<FleetService> services = new ArrayList<>();
        for (Node n : root.expectArrayMember("services").getElements()) {
            ObjectNode s = n.expectObjectNode();
            List<ModelFile> files = s.expectArrayMember("modelFiles").getElements().stream()
                                     .map(Node::expectObjectNode)
                                     .map(f -> new ModelFile(f.expectStringMember("path").getValue(),
                                                             f.expectStringMember("digest").getValue()))
                                     .toList();
            services.add(new FleetService(text(s, "module"), text(s, "name"), text(s, "packageName"),
                                          text(s, "service"), text(s, "sdkId"), text(s, "canonicalDirectory"),
                                          text(s, "modelDirectory"), strings(s, "protocolModules"),
                                          strings(s, "requiredTypes"), files, text(s, "modelDigest")));
        }
        return new FleetPlan(revision, services, strings(root, "missing"), strings(root, "unmappedCanonicalModels"));
    }

    private static ArrayNode strings(List<String> values) {
        return ArrayNode.fromStrings(values);
    }

    private static List<String> strings(ObjectNode node, String member) {
        return node.expectArrayMember(member).getElements().stream().map(Node::expectStringNode)
                   .map(StringNode::getValue).toList();
    }

    private static String text(ObjectNode node, String member) {
        return node.expectStringMember(member).getValue();
    }
}
