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

package software.amazon.awssdk.schema.serde.document;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.SensitiveTrait;

/**
 * Hand-written shapes shaped like generated model classes, for the typed document tests: a structure with members of
 * most types ({@link Outer}), a nested structure ({@link Inner}) and a union ({@link Choice}).
 */
final class DocumentTestShapes {
    static final String NAMESPACE = "com.example";

    private DocumentTestShapes() {
    }

    static final class Inner implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NAMESPACE, "Inner");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("value", PreludeSchemas.STRING)
                                           .builderSupplier(Builder::new)
                                           .build();
        static final Schema VALUE = SCHEMA.member("value");

        private final String value;

        Inner(String value) {
            this.value = value;
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (value != null) {
                serializer.writeString(VALUE, value);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) SchemaUtils.validateSameMember(VALUE, member, value);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Inner && Objects.equals(value, ((Inner) o).value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        static final class Builder implements ShapeBuilder<Inner> {
            private String value;

            @Override
            public Inner build() {
                return new Inner(value);
            }

            @Override
            public Builder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (b, member, d) -> b.value = d.readString(member));
                return this;
            }

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }
        }
    }

    static final class Outer implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NAMESPACE, "Outer");
        static final Schema TAG_LIST = Schema.listBuilder(ShapeId.fromParts(NAMESPACE, "TagList"))
                                             .putMember("member", PreludeSchemas.STRING)
                                             .build();
        static final Schema SCORE_MAP = Schema.mapBuilder(ShapeId.fromParts(NAMESPACE, "ScoreMap"))
                                              .putMember("key", PreludeSchemas.STRING)
                                              .putMember("value", PreludeSchemas.INTEGER)
                                              .build();
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("name", PreludeSchemas.STRING)
                                           .putMember("count", PreludeSchemas.INTEGER)
                                           .putMember("total", PreludeSchemas.LONG)
                                           .putMember("tags", TAG_LIST)
                                           .putMember("scores", SCORE_MAP)
                                           .putMember("child", Inner.SCHEMA)
                                           .putMember("created", PreludeSchemas.TIMESTAMP)
                                           .putMember("data", PreludeSchemas.BLOB)
                                           .putMember("extra", PreludeSchemas.DOCUMENT)
                                           .putMember("secret", PreludeSchemas.STRING, SensitiveTrait.INSTANCE)
                                           .builderSupplier(Builder::new)
                                           .build();
        static final Schema NAME = SCHEMA.member("name");
        static final Schema COUNT = SCHEMA.member("count");
        static final Schema TOTAL = SCHEMA.member("total");
        static final Schema TAGS = SCHEMA.member("tags");
        static final Schema SCORES = SCHEMA.member("scores");
        static final Schema CHILD = SCHEMA.member("child");
        static final Schema CREATED = SCHEMA.member("created");
        static final Schema DATA = SCHEMA.member("data");
        static final Schema EXTRA = SCHEMA.member("extra");
        static final Schema SECRET = SCHEMA.member("secret");

        final String name;
        final Integer count;
        final Long total;
        final List<String> tags;
        final Map<String, Integer> scores;
        final Inner child;
        final Instant created;
        final ByteBuffer data;
        final Document extra;
        final String secret;
        final AtomicInteger serializeMembersCalls = new AtomicInteger();

        private Outer(Builder builder) {
            this.name = builder.name;
            this.count = builder.count;
            this.total = builder.total;
            this.tags = builder.tags;
            this.scores = builder.scores;
            this.child = builder.child;
            this.created = builder.created;
            this.data = builder.data;
            this.extra = builder.extra;
            this.secret = builder.secret;
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
            serializeMembersCalls.incrementAndGet();
            // Written in an order other than the schema's, to show documents keep the written order.
            if (count != null) {
                serializer.writeInteger(COUNT, count);
            }
            if (name != null) {
                serializer.writeString(NAME, name);
            }
            if (total != null) {
                serializer.writeLong(TOTAL, total);
            }
            if (tags != null) {
                serializer.writeList(TAGS, tags, tags.size(), (list, s) -> {
                    for (String tag : list) {
                        if (tag == null) {
                            s.writeNull(TAGS.listMember());
                        } else {
                            s.writeString(TAGS.listMember(), tag);
                        }
                    }
                });
            }
            if (scores != null) {
                serializer.writeMap(SCORES, scores, scores.size(), (map, s) -> {
                    for (Map.Entry<String, Integer> entry : map.entrySet()) {
                        s.writeEntry(SCORES.mapKeyMember(), entry.getKey(), entry.getValue(),
                                     (v, vs) -> vs.writeInteger(SCORES.mapValueMember(), v));
                    }
                });
            }
            if (child != null) {
                serializer.writeStruct(CHILD, child);
            }
            if (created != null) {
                serializer.writeTimestamp(CREATED, created);
            }
            if (data != null) {
                serializer.writeBlob(DATA, data);
            }
            if (extra != null) {
                serializer.writeDocument(EXTRA, extra);
            }
            if (secret != null) {
                serializer.writeString(SECRET, secret);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            switch (member.memberName()) {
                case "name":
                    return (T) SchemaUtils.validateSameMember(NAME, member, name);
                case "count":
                    return (T) SchemaUtils.validateSameMember(COUNT, member, count);
                case "child":
                    return (T) SchemaUtils.validateSameMember(CHILD, member, child);
                default:
                    throw SchemaUtils.notAMember(SCHEMA, member);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Outer)) {
                return false;
            }
            Outer other = (Outer) o;
            return Objects.equals(name, other.name)
                   && Objects.equals(count, other.count)
                   && Objects.equals(total, other.total)
                   && Objects.equals(tags, other.tags)
                   && Objects.equals(scores, other.scores)
                   && Objects.equals(child, other.child)
                   && Objects.equals(created, other.created)
                   && Objects.equals(data, other.data)
                   && Document.equals(extra, other.extra)
                   && Objects.equals(secret, other.secret);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, count, total, tags, scores, child, created, data, secret);
        }

        @Override
        public String toString() {
            return "Outer(name=" + name + ", count=" + count + ")";
        }

        static final class Builder implements ShapeBuilder<Outer> {
            private String name;
            private Integer count;
            private Long total;
            private List<String> tags;
            private Map<String, Integer> scores;
            private Inner child;
            private Instant created;
            private ByteBuffer data;
            private Document extra;
            private String secret;
            private int errorCorrections;

            Builder name(String name) {
                this.name = name;
                return this;
            }

            Builder count(Integer count) {
                this.count = count;
                return this;
            }

            Builder total(Long total) {
                this.total = total;
                return this;
            }

            Builder tags(List<String> tags) {
                this.tags = tags;
                return this;
            }

            Builder scores(Map<String, Integer> scores) {
                this.scores = scores;
                return this;
            }

            Builder child(Inner child) {
                this.child = child;
                return this;
            }

            Builder created(Instant created) {
                this.created = created;
                return this;
            }

            Builder data(ByteBuffer data) {
                this.data = data;
                return this;
            }

            Builder extra(Document extra) {
                this.extra = extra;
                return this;
            }

            Builder secret(String secret) {
                this.secret = secret;
                return this;
            }

            int errorCorrections() {
                return errorCorrections;
            }

            @Override
            public Outer build() {
                return new Outer(this);
            }

            @Override
            public Builder errorCorrection() {
                errorCorrections++;
                return this;
            }

            @Override
            public Builder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, Builder::readMember);
                return this;
            }

            private static void readMember(Builder b, Schema member, ShapeDeserializer d) {
                switch (member.memberName()) {
                    case "name":
                        b.name = d.readString(member);
                        break;
                    case "count":
                        b.count = d.readInteger(member);
                        break;
                    case "total":
                        b.total = d.readLong(member);
                        break;
                    case "tags":
                        List<String> tags = new ArrayList<>();
                        d.readList(member, tags, (list, e) -> list.add(e.isNull() ? e.readNull()
                                                                                    : e.readString(TAGS.listMember())));
                        b.tags = tags;
                        break;
                    case "scores":
                        Map<String, Integer> scores = new LinkedHashMap<>();
                        d.readStringMap(member, scores,
                                        (map, key, v) -> map.put(key, v.readInteger(SCORES.mapValueMember())));
                        b.scores = scores;
                        break;
                    case "child":
                        b.child = new Inner.Builder().deserialize(d).build();
                        break;
                    case "created":
                        b.created = d.readTimestamp(member);
                        break;
                    case "data":
                        b.data = d.readBlob(member);
                        break;
                    case "extra":
                        b.extra = d.readDocument();
                        break;
                    case "secret":
                        b.secret = d.readString(member);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected member " + member);
                }
            }

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }
        }
    }

    static final class Choice implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NAMESPACE, "Choice");
        static final Schema SCHEMA = Schema.unionBuilder(ID)
                                           .putMember("text", PreludeSchemas.STRING)
                                           .putMember("number", PreludeSchemas.INTEGER)
                                           .putMember("any", PreludeSchemas.DOCUMENT)
                                           .builderSupplier(Builder::new)
                                           .build();
        static final Schema TEXT = SCHEMA.member("text");
        static final Schema NUMBER = SCHEMA.member("number");
        static final Schema ANY = SCHEMA.member("any");

        private final String text;
        private final Integer number;
        private final Document any;

        private Choice(String text, Integer number, Document any) {
            this.text = text;
            this.number = number;
            this.any = any;
        }

        static Choice text(String text) {
            return new Choice(text, null, null);
        }

        static Choice number(int number) {
            return new Choice(null, number, null);
        }

        static Choice any(Document any) {
            return new Choice(null, null, any);
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (text != null) {
                serializer.writeString(TEXT, text);
            } else if (number != null) {
                serializer.writeInteger(NUMBER, number);
            } else if (any != null) {
                serializer.writeDocument(ANY, any);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Choice)) {
                return false;
            }
            Choice other = (Choice) o;
            return Objects.equals(text, other.text) && Objects.equals(number, other.number)
                   && Document.equals(any, other.any);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, number);
        }

        static final class Builder implements ShapeBuilder<Choice> {
            private String text;
            private Integer number;
            private Document any;

            @Override
            public Choice build() {
                return new Choice(text, number, any);
            }

            @Override
            public Builder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (b, member, d) -> {
                    switch (member.memberName()) {
                        case "text":
                            b.text = d.readString(member);
                            break;
                        case "number":
                            b.number = d.readInteger(member);
                            break;
                        default:
                            b.any = d.readDocument();
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
