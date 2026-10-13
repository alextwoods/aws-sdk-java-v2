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

package software.amazon.awssdk.codecs.cbor.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Port of smithy-java {@code CborTestData}: a hand-written {@code Bird} structure with string, blob, timestamp,
 * bigInteger and bigDecimal members.
 */
final class CborTestData {
    static final ShapeId BIRD_ID = ShapeId.from("smithy.example#Bird");
    static final Schema BIRD = Schema.structureBuilder(BIRD_ID)
                                     .putMember("name", PreludeSchemas.STRING)
                                     .putMember("bytes", PreludeSchemas.BLOB)
                                     .putMember("lastSquawkAt", PreludeSchemas.TIMESTAMP)
                                     .putMember("flightRange", PreludeSchemas.BIG_INTEGER)
                                     .putMember("wingspan", PreludeSchemas.BIG_DECIMAL)
                                     .build();
    static final Schema BIRD_NAME = BIRD.member("name");
    static final Schema BIRD_BYTES = BIRD.member("bytes");
    static final Schema BIRD_LAST_SQUAWK_AT = BIRD.member("lastSquawkAt");
    static final Schema BIRD_FLIGHT_RANGE = BIRD.member("flightRange");
    static final Schema BIRD_WING_SPAN = BIRD.member("wingspan");

    private CborTestData() {
    }

    /**
     * Parses a string of hex digit pairs; replaces the Java 17 {@code HexFormat} used by the smithy-java tests.
     */
    static byte[] hex(String hex) {
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return result;
    }

    static final class Bird implements SerializableStruct {
        String name;
        ByteBuffer bytes;
        Instant lastSquawkAt;
        BigInteger flightRange;
        BigDecimal wingspan;

        private Bird(BirdBuilder builder) {
            name = builder.name;
            bytes = builder.bytes;
            lastSquawkAt = builder.lastSquawkAt;
            flightRange = builder.flightRange;
            wingspan = builder.wingspan;
        }

        @Override
        public Schema sdkSchema() {
            return BIRD;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (name != null) {
                serializer.writeString(BIRD_NAME, name);
            }
            if (bytes != null) {
                serializer.writeBlob(BIRD_BYTES, bytes);
            }
            if (lastSquawkAt != null) {
                serializer.writeTimestamp(BIRD_LAST_SQUAWK_AT, lastSquawkAt);
            }
            if (flightRange != null) {
                serializer.writeBigInteger(BIRD_FLIGHT_RANGE, flightRange);
            }
            if (wingspan != null) {
                serializer.writeBigDecimal(BIRD_WING_SPAN, wingspan);
            }
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return null;
        }
    }

    static final class BirdBuilder implements ShapeBuilder<Bird> {
        private String name;
        private ByteBuffer bytes;
        private Instant lastSquawkAt;
        private BigInteger flightRange;
        private BigDecimal wingspan;

        @Override
        public Bird build() {
            return new Bird(this);
        }

        @Override
        public BirdBuilder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(sdkSchema(), this, (builder, member, de) -> {
                switch (member.memberIndex()) {
                    case 0:
                        builder.name(de.readString(member));
                        break;
                    case 1:
                        builder.bytes(de.readBlob(member));
                        break;
                    case 2:
                        builder.lastSquawkAt(de.readTimestamp(member));
                        break;
                    case 3:
                        builder.flightRange(de.readBigInteger(member));
                        break;
                    case 4:
                        builder.wingspan(de.readBigDecimal(member));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected member " + member);
                }
            });
            return this;
        }

        BirdBuilder name(String name) {
            this.name = name;
            return this;
        }

        BirdBuilder bytes(ByteBuffer bytes) {
            this.bytes = bytes;
            return this;
        }

        BirdBuilder lastSquawkAt(Instant lastSquawkAt) {
            this.lastSquawkAt = lastSquawkAt;
            return this;
        }

        BirdBuilder flightRange(BigInteger flightRange) {
            this.flightRange = flightRange;
            return this;
        }

        BirdBuilder wingspan(BigDecimal wingspan) {
            this.wingspan = wingspan;
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return BIRD;
        }
    }
}
