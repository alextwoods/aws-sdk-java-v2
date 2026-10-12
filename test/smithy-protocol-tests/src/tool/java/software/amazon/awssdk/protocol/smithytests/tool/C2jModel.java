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

package software.amazon.awssdk.protocol.smithytests.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.protocol.smithytests.tool.ShapeView.Binding;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * A minimal, read-only view of a legacy C2J test model ({@code service-2.json}), enough to fingerprint legacy cases. It
 * reads the JSON with comments allowed, as the legacy {@code ProtocolTestSuiteLoader} does.
 */
final class C2jModel {

    private final ObjectNode shapes;
    private final ObjectNode operations;

    C2jModel(ObjectNode serviceModel) {
        this.shapes = serviceModel.expectObjectMember("shapes");
        this.operations = serviceModel.expectObjectMember("operations");
    }

    static C2jModel load(Path serviceJson) throws IOException {
        String text = new String(Files.readAllBytes(serviceJson), StandardCharsets.UTF_8);
        return new C2jModel(Node.parseJsonWithComments(text, serviceJson.toString()).expectObjectNode());
    }

    /** The operation with this name, or null. */
    ShapeView.OperationView operation(String name) {
        ObjectNode op = operations.getObjectMember(name).orElse(null);
        return op == null ? null : new C2jOperation(name, op);
    }

    /** The shape with this name, or null. */
    ShapeView shape(String name) {
        return shapes.getObjectMember(name).isPresent() ? new C2jShape(name, null) : null;
    }

    private final class C2jOperation implements ShapeView.OperationView {
        private final String name;
        private final ObjectNode op;
        private final String requestUri;

        C2jOperation(String name, ObjectNode op) {
            this.name = name;
            this.op = op;
            this.requestUri = op.getObjectMember("http")
                                .flatMap(h -> h.getStringMember("requestUri"))
                                .map(s -> s.getValue())
                                .orElse("");
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public ShapeView input() {
            return shapeRef(op.getObjectMember("input").orElse(null));
        }

        @Override
        public ShapeView output() {
            return shapeRef(op.getObjectMember("output").orElse(null));
        }

        private ShapeView shapeRef(ObjectNode ref) {
            return ref == null ? null : new C2jShape(ref.expectStringMember("shape").getValue(), requestUri);
        }


        @Override
        public Set<String> traitTokens() {
            Set<String> out = new TreeSet<>();
            if (op.containsMember("endpoint")) {
                out.add("endpoint");
            }
            if (op.containsMember("requestcompression")) {
                out.add("requestCompression");
            }
            if (op.containsMember("httpChecksum")) {
                out.add("httpChecksum");
            }
            return out;
        }
    }

    private final class C2jShape implements ShapeView {
        private final String name;
        private final ObjectNode shape;
        /** The operation request URI, used to detect greedy labels on top-level input members. */
        private final String requestUri;

        C2jShape(String name, String requestUri) {
            this.name = name;
            this.shape = shapes.expectObjectMember(name);
            this.requestUri = requestUri;
        }

        @Override
        public String id() {
            return name;
        }

        @Override
        public Kind kind() {
            String type = shape.expectStringMember("type").getValue();
            switch (type) {
                case "structure":
                    if (shape.getBooleanMemberOrDefault("union")) {
                        return Kind.UNION;
                    }
                    return shape.getBooleanMemberOrDefault("document") ? Kind.DOCUMENT : Kind.STRUCTURE;
                case "list":
                    return Kind.LIST;
                case "map":
                    return Kind.MAP;
                case "string":
                    return shape.containsMember("enum") ? Kind.ENUM : Kind.STRING;
                case "integer":
                case "long":
                case "short":
                case "byte":
                    return Kind.INTEGER;
                case "float":
                case "double":
                    return Kind.FLOAT;
                case "boolean":
                    return Kind.BOOLEAN;
                case "timestamp":
                    return Kind.TIMESTAMP;
                case "blob":
                    return Kind.BLOB;
                default:
                    throw new IllegalStateException("Unknown C2J shape type " + type + " on " + name);
            }
        }

        @Override
        public List<MemberView> members() {
            ObjectNode members = shape.getObjectMember("members").orElse(null);
            if (members == null) {
                return Collections.emptyList();
            }
            List<MemberView> out = new ArrayList<>();
            for (String member : members.getStringMap().keySet()) {
                out.add(member(member));
            }
            return out;
        }

        @Override
        public MemberView member(String memberName) {
            ObjectNode member = shape.getObjectMember("members").flatMap(m -> m.getObjectMember(memberName))
                                     .orElse(null);
            if (member == null) {
                return null;
            }
            String payload = shape.getStringMember("payload").map(s -> s.getValue()).orElse(null);
            return new C2jMember(memberName, member, memberName.equals(payload), requestUri);
        }

        @Override
        public MemberView listMember() {
            ObjectNode member = shape.expectObjectMember("member");
            return new C2jMember("member", member, false, null, shape.getBooleanMemberOrDefault("flattened"));
        }

        @Override
        public MemberView mapValue() {
            return new C2jMember("value", shape.expectObjectMember("value"), false, null,
                                 shape.getBooleanMemberOrDefault("flattened"));
        }

        @Override
        public boolean streaming() {
            return shape.getBooleanMemberOrDefault("streaming") || shape.getBooleanMemberOrDefault("eventstream");
        }

        Set<String> shapeTraitTokens() {
            Set<String> out = new TreeSet<>();
            shape.getStringMember("timestampFormat").ifPresent(f -> out.add("timestampFormat:" + f.getValue()));
            return out;
        }
    }

    private final class C2jMember implements ShapeView.MemberView {
        private final String name;
        private final ObjectNode member;
        private final boolean payload;
        private final String requestUri;
        private final boolean containerFlattened;

        C2jMember(String name, ObjectNode member, boolean payload, String requestUri) {
            this(name, member, payload, requestUri, false);
        }

        C2jMember(String name, ObjectNode member, boolean payload, String requestUri, boolean containerFlattened) {
            this.name = name;
            this.member = member;
            this.payload = payload;
            this.requestUri = requestUri;
            this.containerFlattened = containerFlattened;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public ShapeView target() {
            return new C2jShape(member.expectStringMember("shape").getValue(), null);
        }

        private String locationName() {
            return member.getStringMember("locationName").map(s -> s.getValue()).orElse(null);
        }

        @Override
        public Binding binding() {
            if (payload) {
                return Binding.PAYLOAD;
            }
            String location = member.getStringMember("location").map(s -> s.getValue()).orElse(null);
            if (location == null) {
                return Binding.BODY;
            }
            switch (location) {
                case "header":
                    return Binding.HEADER;
                case "headers":
                    return Binding.PREFIX_HEADERS;
                case "querystring":
                    return target().kind() == ShapeView.Kind.MAP ? Binding.QUERY_PARAMS : Binding.QUERY;
                case "uri":
                    String label = locationName() == null ? name : locationName();
                    return requestUri != null && requestUri.contains("{" + label + "+}")
                           ? Binding.GREEDY_LABEL : Binding.LABEL;
                case "statusCode":
                    return Binding.STATUS_CODE;
                default:
                    return Binding.BODY;
            }
        }

        @Override
        public String serializedName() {
            return binding() == Binding.BODY && locationName() != null ? locationName() : name;
        }

        @Override
        public Set<String> traitTokens() {
            Set<String> out = new TreeSet<>();
            C2jShape target = (C2jShape) target();
            boolean flattened = member.getBooleanMemberOrDefault("flattened")
                                || target.shape.getBooleanMemberOrDefault("flattened") || containerFlattened;
            if (flattened && target.kind() == ShapeView.Kind.LIST || flattened && target.kind() == ShapeView.Kind.MAP) {
                out.add("xmlFlattened");
            }
            if (locationName() != null && binding() == Binding.BODY && !locationName().equals(name)) {
                out.add("rename");
            }
            if (member.containsMember("queryName")) {
                out.add("ec2QueryName");
            }
            if (member.containsMember("xmlNamespace") || target.shape.containsMember("xmlNamespace")) {
                out.add("xmlNamespace");
            }
            if (member.getBooleanMemberOrDefault("xmlAttribute")) {
                out.add("xmlAttribute");
            }
            if (member.getBooleanMemberOrDefault("jsonvalue")) {
                out.add("mediaTypeJson");
            }
            if (member.getBooleanMemberOrDefault("idempotencyToken")) {
                out.add("idempotencyToken");
            }
            if (member.getBooleanMemberOrDefault("hostLabel")) {
                out.add("hostLabel");
            }
            out.addAll(target.shapeTraitTokens());
            return out;
        }
    }
}
