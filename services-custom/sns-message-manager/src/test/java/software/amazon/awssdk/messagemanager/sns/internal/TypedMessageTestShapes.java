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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.util.Objects;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.TypeRegistry;

/**
 * Two hand-written structures shaped like generated model classes, each with a root schema, a shape class and a
 * builder. A minimal copy of the {@code message-payloads} test shapes.
 */
final class TypedMessageTestShapes {
    static final String NS = "smithy.example";

    static final TypeRegistry REGISTRY = TypeRegistry.builder()
                                                     .putType(OrderPlaced.ID, OrderPlaced.class, OrderPlaced::builder)
                                                     .putType(OrderCancelled.ID, OrderCancelled.class,
                                                              OrderCancelled::builder)
                                                     .build();

    private TypedMessageTestShapes() {
    }

    /**
     * A structure with a string member, {@code orderId}, and an integer member, {@code quantity}.
     */
    static final class OrderPlaced implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "OrderPlaced");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("orderId", PreludeSchemas.STRING)
                                           .putMember("quantity", PreludeSchemas.INTEGER)
                                           .builderSupplier(OrderPlaced::builder)
                                           .shapeClass(OrderPlaced.class)
                                           .build();
        static final Schema ORDER_ID = SCHEMA.member("orderId");
        static final Schema QUANTITY = SCHEMA.member("quantity");

        private final String orderId;
        private final Integer quantity;

        OrderPlaced(String orderId, Integer quantity) {
            this.orderId = orderId;
            this.quantity = quantity;
        }

        static Builder builder() {
            return new Builder();
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (orderId != null) {
                serializer.writeString(ORDER_ID, orderId);
            }
            if (quantity != null) {
                serializer.writeInteger(QUANTITY, quantity);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            switch (member.memberName()) {
                case "orderId":
                    return (T) orderId;
                case "quantity":
                    return (T) quantity;
                default:
                    throw new IllegalArgumentException("Not a member of " + ID + ": " + member.id());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof OrderPlaced)) {
                return false;
            }
            OrderPlaced other = (OrderPlaced) o;
            return Objects.equals(orderId, other.orderId) && Objects.equals(quantity, other.quantity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderId, quantity);
        }

        @Override
        public String toString() {
            return "OrderPlaced(orderId=" + orderId + ", quantity=" + quantity + ")";
        }

        static final class Builder implements ShapeBuilder<OrderPlaced> {
            private String orderId;
            private Integer quantity;

            @Override
            public OrderPlaced build() {
                return new OrderPlaced(orderId, quantity);
            }

            @Override
            public Builder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (b, member, d) -> {
                    switch (member.memberName()) {
                        case "orderId":
                            b.orderId = d.readString(member);
                            break;
                        default:
                            b.quantity = d.readInteger(member);
                            break;
                    }
                });
                return this;
            }

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }
        }
    }

    /**
     * A structure with two string members, {@code orderId} and {@code reason}.
     */
    static final class OrderCancelled implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "OrderCancelled");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("orderId", PreludeSchemas.STRING)
                                           .putMember("reason", PreludeSchemas.STRING)
                                           .builderSupplier(OrderCancelled::builder)
                                           .shapeClass(OrderCancelled.class)
                                           .build();
        static final Schema ORDER_ID = SCHEMA.member("orderId");
        static final Schema REASON = SCHEMA.member("reason");

        private final String orderId;
        private final String reason;

        OrderCancelled(String orderId, String reason) {
            this.orderId = orderId;
            this.reason = reason;
        }

        static Builder builder() {
            return new Builder();
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (orderId != null) {
                serializer.writeString(ORDER_ID, orderId);
            }
            if (reason != null) {
                serializer.writeString(REASON, reason);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            switch (member.memberName()) {
                case "orderId":
                    return (T) orderId;
                case "reason":
                    return (T) reason;
                default:
                    throw new IllegalArgumentException("Not a member of " + ID + ": " + member.id());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof OrderCancelled)) {
                return false;
            }
            OrderCancelled other = (OrderCancelled) o;
            return Objects.equals(orderId, other.orderId) && Objects.equals(reason, other.reason);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderId, reason);
        }

        @Override
        public String toString() {
            return "OrderCancelled(orderId=" + orderId + ", reason=" + reason + ")";
        }

        static final class Builder implements ShapeBuilder<OrderCancelled> {
            private String orderId;
            private String reason;

            @Override
            public OrderCancelled build() {
                return new OrderCancelled(orderId, reason);
            }

            @Override
            public Builder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (b, member, d) -> {
                    switch (member.memberName()) {
                        case "orderId":
                            b.orderId = d.readString(member);
                            break;
                        default:
                            b.reason = d.readString(member);
                            break;
                    }
                });
                return this;
            }

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }
        }
    }
}
