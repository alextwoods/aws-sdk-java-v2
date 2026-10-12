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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.protocol.smithytests.tool.ShapeView.Binding;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryNameTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/** {@link ShapeView} adapters over a Smithy {@link Model}. */
final class SmithyShapes {

    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");
    private static final ShapeId HTTP_CHECKSUM = ShapeId.from("aws.protocols#httpChecksum");

    private final Model model;

    SmithyShapes(Model model) {
        this.model = model;
    }

    ShapeView.OperationView operation(OperationShape op) {
        return new SmithyOperation(op);
    }

    ShapeView shape(ShapeId id) {
        return new SmithyShape(model.expectShape(id), null);
    }

    private final class SmithyOperation implements ShapeView.OperationView {
        private final OperationShape op;
        private final String uri;

        SmithyOperation(OperationShape op) {
            this.op = op;
            this.uri = op.getTrait(HttpTrait.class).map(t -> t.getUri().toString()).orElse("");
        }

        @Override
        public String name() {
            return op.getId().getName();
        }

        @Override
        public ShapeView input() {
            return UNIT.equals(op.getInputShape()) ? null : new SmithyShape(model.expectShape(op.getInputShape()), uri);
        }

        @Override
        public ShapeView output() {
            return UNIT.equals(op.getOutputShape()) ? null : new SmithyShape(model.expectShape(op.getOutputShape()), null);
        }


        @Override
        public Set<String> traitTokens() {
            Set<String> out = new TreeSet<>();
            if (op.hasTrait(EndpointTrait.class)) {
                out.add("endpoint");
            }
            if (op.hasTrait(RequestCompressionTrait.class)) {
                out.add("requestCompression");
            }
            if (op.findTrait(HTTP_CHECKSUM).isPresent()) {
                out.add("httpChecksum");
            }
            return out;
        }
    }

    private final class SmithyShape implements ShapeView {
        private final Shape shape;
        private final String uri;

        SmithyShape(Shape shape, String uri) {
            this.shape = shape;
            this.uri = uri;
        }

        @Override
        public String id() {
            return shape.getId().toString();
        }

        @Override
        public Kind kind() {
            ShapeType type = shape.getType();
            switch (type) {
                case STRUCTURE:
                    return Kind.STRUCTURE;
                case UNION:
                    return Kind.UNION;
                case LIST:
                case SET:
                    return Kind.LIST;
                case MAP:
                    return Kind.MAP;
                case ENUM:
                    return Kind.ENUM;
                case STRING:
                    return shape.hasTrait(EnumTrait.class) ? Kind.ENUM : Kind.STRING;
                case INT_ENUM:
                    return Kind.INT_ENUM;
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                    return Kind.INTEGER;
                case FLOAT:
                case DOUBLE:
                    return Kind.FLOAT;
                case BIG_DECIMAL:
                    return Kind.BIG_DECIMAL;
                case BIG_INTEGER:
                    return Kind.BIG_INTEGER;
                case BOOLEAN:
                    return Kind.BOOLEAN;
                case TIMESTAMP:
                    return Kind.TIMESTAMP;
                case BLOB:
                    return Kind.BLOB;
                case DOCUMENT:
                    return Kind.DOCUMENT;
                default:
                    throw new IllegalStateException("Unexpected shape type " + type + " for " + shape.getId());
            }
        }

        @Override
        public List<MemberView> members() {
            List<MemberView> out = new ArrayList<>();
            if (kind() == Kind.STRUCTURE || kind() == Kind.UNION) {
                for (MemberShape m : shape.members()) {
                    out.add(new SmithyMember(m, uri));
                }
            }
            return out;
        }

        @Override
        public MemberView member(String name) {
            if (kind() != Kind.STRUCTURE && kind() != Kind.UNION) {
                return null;
            }
            return shape.getMember(name).map(m -> (MemberView) new SmithyMember(m, uri)).orElse(null);
        }

        @Override
        public MemberView listMember() {
            return new SmithyMember(shape.asListShape().get().getMember(), null);
        }

        @Override
        public MemberView mapValue() {
            return new SmithyMember(shape.asMapShape().get().getValue(), null);
        }

        @Override
        public boolean streaming() {
            return shape.hasTrait(StreamingTrait.class);
        }
    }

    private final class SmithyMember implements ShapeView.MemberView {
        private final MemberShape member;
        private final String uri;

        SmithyMember(MemberShape member, String uri) {
            this.member = member;
            this.uri = uri;
        }

        private Shape targetShape() {
            return model.expectShape(member.getTarget());
        }

        @Override
        public String name() {
            return member.getMemberName();
        }

        @Override
        public ShapeView target() {
            return new SmithyShape(targetShape(), null);
        }

        @Override
        public Binding binding() {
            if (member.hasTrait(HttpPayloadTrait.class)) {
                return Binding.PAYLOAD;
            }
            if (member.hasTrait(HttpHeaderTrait.class)) {
                return Binding.HEADER;
            }
            if (member.hasTrait(HttpPrefixHeadersTrait.class)) {
                return Binding.PREFIX_HEADERS;
            }
            if (member.hasTrait(HttpQueryTrait.class)) {
                return Binding.QUERY;
            }
            if (member.hasTrait(HttpQueryParamsTrait.class)) {
                return Binding.QUERY_PARAMS;
            }
            if (member.hasTrait(HttpLabelTrait.class)) {
                return uri != null && uri.contains("{" + member.getMemberName() + "+}")
                       ? Binding.GREEDY_LABEL : Binding.LABEL;
            }
            if (member.hasTrait(HttpResponseCodeTrait.class)) {
                return Binding.STATUS_CODE;
            }
            return Binding.BODY;
        }

        @Override
        public String serializedName() {
            return member.getTrait(JsonNameTrait.class).map(JsonNameTrait::getValue)
                         .orElseGet(() -> member.getTrait(XmlNameTrait.class).map(XmlNameTrait::getValue)
                                                .orElse(member.getMemberName()));
        }

        @Override
        public Set<String> traitTokens() {
            Set<String> out = new TreeSet<>();
            Shape target = targetShape();
            if (member.hasTrait(XmlFlattenedTrait.class)) {
                out.add("xmlFlattened");
            }
            if (member.hasTrait(XmlNameTrait.class) || member.hasTrait(JsonNameTrait.class)) {
                out.add("rename");
            }
            if (member.hasTrait(Ec2QueryNameTrait.class)) {
                out.add("ec2QueryName");
            }
            if (member.hasTrait(XmlNamespaceTrait.class) || target.hasTrait(XmlNamespaceTrait.class)) {
                out.add("xmlNamespace");
            }
            if (member.hasTrait(XmlAttributeTrait.class)) {
                out.add("xmlAttribute");
            }
            if (target.getTrait(MediaTypeTrait.class).map(t -> t.getValue().contains("json")).orElse(false)) {
                out.add("mediaTypeJson");
            }
            if (member.hasTrait(IdempotencyTokenTrait.class)) {
                out.add("idempotencyToken");
            }
            if (member.hasTrait(software.amazon.smithy.model.traits.HostLabelTrait.class)) {
                out.add("hostLabel");
            }
            TimestampFormatTrait format = member.getTrait(TimestampFormatTrait.class)
                                                .orElse(target.getTrait(TimestampFormatTrait.class).orElse(null));
            if (format != null) {
                out.add("timestampFormat:" + format.getValue());
            }
            return out;
        }
    }
}
