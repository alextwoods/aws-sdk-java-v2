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

package software.amazon.awssdk.schema.serde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.SensitiveTrait;
import software.amazon.awssdk.schema.traits.StreamingTrait;
import software.amazon.awssdk.utils.ToString;

/**
 * The first group of tests is ported from smithy-java {@code ToStringSerializerTest}, with the expected strings
 * changed to the SDK format ({@code Name(m=v)} and {@code *** Sensitive Data Redacted ***}). The rest cover the
 * SDK-specific behavior: unset and streaming members, nesting depth, container redaction and value formats.
 */
class ToStringSerializerTest {

    private static final String REDACTED = "*** Sensitive Data Redacted ***";

    private static ShapeId id(String name) {
        return ShapeId.fromParts("smithy.example", name);
    }

    // smithy-java writesStructToString (Bird); the generated test model is replaced by an inline struct.
    @Test
    void writesStructToString() {
        Schema bird = Schema.structureBuilder(id("Bird")).putMember("name", PreludeSchemas.STRING).build();

        String str = ToStringSerializer.serialize(TestHelper.create(bird, (s, ser) -> {
            ser.writeString(s.member("name"), "foo");
        }));

        assertThat(str).isEqualTo("Bird(name=foo)");
    }

    // smithy-java writesStructToStringMultipleValues (Person); birthday is sensitive in that model.
    @Test
    void writesStructToStringMultipleValues() {
        Schema stringList = Schema.listBuilder(id("StringList")).putMember("member", PreludeSchemas.STRING).build();
        Schema queryParams = Schema.mapBuilder(id("QueryParams"))
                                   .putMember("key", PreludeSchemas.STRING)
                                   .putMember("value", stringList)
                                   .build();
        Schema person = Schema.structureBuilder(id("Person"))
                              .putMember("name", PreludeSchemas.STRING)
                              .putMember("age", PreludeSchemas.INTEGER)
                              .putMember("binary", PreludeSchemas.BLOB)
                              .putMember("birthday", PreludeSchemas.TIMESTAMP, SensitiveTrait.INSTANCE)
                              .putMember("queryParams", queryParams)
                              .build();

        String str = ToStringSerializer.serialize(TestHelper.create(person, (s, ser) -> {
            ser.writeString(s.member("name"), "Mike");
            ser.writeInteger(s.member("age"), 102);
            ser.writeBlob(s.member("binary"), ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
            ser.writeTimestamp(s.member("birthday"), Instant.EPOCH);
            ser.writeMap(s.member("queryParams"), queryParams, 1, (m, map) -> {
                map.writeEntry(m.mapKeyMember(), "a", m, (m2, vs) -> {
                    vs.writeList(m2.mapValueMember(), "unused", 2, (state, ls) -> {
                        ls.writeString(stringList.listMember(), "1");
                        ls.writeString(stringList.listMember(), "2");
                    });
                });
            });
        }));

        assertThat(str).isEqualTo("Person(name=Mike, age=102, binary=SdkBytes(bytes=0x68656c6c6f), birthday=" + REDACTED
                                  + ", queryParams={a=[1, 2]})");
    }

    // smithy-java redactsSensitiveKeys. The map holds sensitive keys and values, so the SDK redacts it whole.
    @Test
    void redactsSensitiveKeys() {
        Schema mapMemberSchema = Schema.createString(id("Str"), SensitiveTrait.INSTANCE);
        Schema mapSchema = Schema.mapBuilder(id("Map"))
                                 .putMember("key", mapMemberSchema)
                                 .putMember("value", mapMemberSchema)
                                 .build();
        Schema schema = Schema.structureBuilder(id("Struct")).putMember("foo", mapSchema).build();

        String str = ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> writeTwoEntries(s, ser, mapSchema)));

        assertThat(str).isEqualTo("Struct(foo=" + REDACTED + ")");
    }

    // The same map written directly, outside a member, redacts each key and value.
    @Test
    void redactsSensitiveKeysAndValuesOfAMapWrittenDirectly() {
        Schema mapMemberSchema = Schema.createString(id("Str"), SensitiveTrait.INSTANCE);
        Schema mapSchema = Schema.mapBuilder(id("Map"))
                                 .putMember("key", mapMemberSchema)
                                 .putMember("value", PreludeSchemas.STRING)
                                 .build();

        ToStringSerializer serializer = new ToStringSerializer();
        serializer.writeMap(PreludeSchemas.DOCUMENT, mapSchema, 1, (m, map) -> {
            map.writeEntry(m.mapKeyMember(), "secret-key", m, (m2, vs) -> vs.writeString(m2.mapValueMember(), "v"));
        });

        assertThat(serializer.toString()).isEqualTo("{" + REDACTED + "=v}");
    }

    @Test
    void redactsSensitiveBlobs() {
        Schema blobSchema = Schema.createBlob(id("Blob"), SensitiveTrait.INSTANCE);
        Schema schema = Schema.structureBuilder(id("Struct")).putMember("foo", blobSchema).build();

        String str = ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> {
            ser.writeBlob(s.member("foo"), ByteBuffer.wrap("abc".getBytes(StandardCharsets.UTF_8)));
        }));

        assertThat(str).isEqualTo("Struct(foo=" + REDACTED + ")");
    }

    // smithy-java redactsFullListIfSensitive (which tests a map and asserts nothing).
    @Test
    void redactsFullMapIfSensitive() {
        Schema mapSchema = Schema.mapBuilder(id("Map"))
                                 .putMember("key", PreludeSchemas.STRING)
                                 .putMember("value", PreludeSchemas.STRING)
                                 .build();
        Schema schema = Schema.structureBuilder(id("Struct"))
                              .putMember("foo", mapSchema, SensitiveTrait.INSTANCE)
                              .build();

        String str = ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> writeTwoEntries(s, ser, mapSchema)));

        assertThat(str).isEqualTo("Struct(foo=" + REDACTED + ")");
    }

    // smithy-java redactsFullMapIfSensitive (which tests a list).
    @Test
    void redactsFullListIfSensitive() {
        Schema listSchema = Schema.listBuilder(id("List")).putMember("member", PreludeSchemas.STRING).build();
        Schema schema = Schema.structureBuilder(id("Struct"))
                              .putMember("foo", listSchema, SensitiveTrait.INSTANCE)
                              .build();

        String str = ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> {
            ser.writeList(s.member("foo"), listSchema, 2, (innerListSchema, ls) -> {
                ls.writeString(listSchema.member("member"), "a");
                ls.writeString(listSchema.member("member"), "b");
            });
        }));

        assertThat(str).isEqualTo("Struct(foo=" + REDACTED + ")");
    }

    @Test
    void redactsFullStructIfSensitive() {
        Schema nestedStruct = Schema.structureBuilder(id("Nested")).putMember("bar", PreludeSchemas.STRING).build();
        Schema schema = Schema.structureBuilder(id("Struct"))
                              .putMember("foo", nestedStruct, SensitiveTrait.INSTANCE)
                              .build();

        String str = ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> {
            ser.writeStruct(s.member("foo"), TestHelper.create(nestedStruct, (ns, nser) -> {
                fail("A redacted structure's members must not be read");
            }));
        }));

        assertThat(str).isEqualTo("Struct(foo=" + REDACTED + ")");
    }

    @Test
    void omitsUnsetMembers() {
        Schema schema = Schema.structureBuilder(id("Struct"))
                              .putMember("a", PreludeSchemas.STRING)
                              .putMember("b", PreludeSchemas.STRING)
                              .putMember("c", PreludeSchemas.STRING)
                              .build();

        String str = ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> {
            ser.writeNull(s.member("a"));
            ser.writeString(s.member("b"), "x");
            ser.writeNull(s.member("c"));
        }));

        assertThat(str).isEqualTo("Struct(b=x)");
        assertThat(ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> { })))
            .isEqualTo("Struct()");
    }

    @Test
    void omitsStreamingMembersWithoutReadingThem() {
        Schema streamingBlob = Schema.createBlob(id("Body"), StreamingTrait.INSTANCE);
        Schema event = Schema.structureBuilder(id("Event")).putMember("x", PreludeSchemas.STRING).build();
        Schema eventStream = Schema.unionBuilder(id("Events"), StreamingTrait.INSTANCE).putMember("event", event).build();
        Schema schema = Schema.structureBuilder(id("Output"))
                              .putMember("before", PreludeSchemas.STRING)
                              .putMember("body", streamingBlob)
                              .putMember("events", eventStream)
                              .putMember("after", PreludeSchemas.STRING)
                              .build();
        ByteBuffer body = ByteBuffer.wrap(new byte[] {1, 2, 3});

        String str = ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> {
            ser.writeString(s.member("before"), "1");
            ser.writeBlob(s.member("body"), body);
            ser.writeStruct(s.member("events"), TestHelper.create(eventStream, (es, eser) -> {
                fail("An event stream member must not be read");
            }));
            ser.writeString(s.member("after"), "2");
        }));

        assertThat(str).isEqualTo("Output(before=1, after=2)");
        assertThat(body.position()).isZero();
    }

    @Test
    void redactsAtEveryNestingDepth() {
        Schema secret = Schema.createString(id("Secret"), SensitiveTrait.INSTANCE);
        Schema inner = Schema.structureBuilder(id("Inner"))
                             .putMember("visible", PreludeSchemas.STRING)
                             .putMember("hidden", secret)
                             .putMember("memberLevel", PreludeSchemas.INTEGER, SensitiveTrait.INSTANCE)
                             .build();
        Schema innerList = Schema.listBuilder(id("InnerList")).putMember("member", inner).build();
        Schema outer = Schema.structureBuilder(id("Outer")).putMember("items", innerList).build();

        String str = ToStringSerializer.serialize(TestHelper.create(outer, (s, ser) -> {
            ser.writeList(s.member("items"), null, 2, (state, ls) -> {
                for (int i = 0; i < 2; i++) {
                    String suffix = String.valueOf(i);
                    ls.writeStruct(innerList.listMember(), TestHelper.create(inner, (is, iser) -> {
                        iser.writeString(is.member("visible"), "v" + suffix);
                        iser.writeString(is.member("hidden"), "marker" + suffix);
                        iser.writeInteger(is.member("memberLevel"), 4242);
                    }));
                }
            });
        }));

        assertThat(str).isEqualTo("Outer(items=[Inner(visible=v0, hidden=" + REDACTED + ", memberLevel=" + REDACTED
                                  + "), Inner(visible=v1, hidden=" + REDACTED + ", memberLevel=" + REDACTED + ")])");
        assertThat(str).doesNotContain("marker").doesNotContain("4242");
    }

    @Test
    void redactsListsAndMapsHoldingSensitiveValuesAsAWhole() {
        Schema secret = Schema.createString(id("Secret"), SensitiveTrait.INSTANCE);
        Schema secretList = Schema.listBuilder(id("SecretList")).putMember("member", secret).build();
        Schema listOfSecretLists = Schema.listBuilder(id("ListOfSecretLists")).putMember("member", secretList).build();
        Schema mapOfSecretLists = Schema.mapBuilder(id("MapOfSecretLists"))
                                        .putMember("key", PreludeSchemas.STRING)
                                        .putMember("value", secretList)
                                        .build();
        Schema schema = Schema.structureBuilder(id("Struct"))
                              .putMember("list", secretList)
                              .putMember("nested", listOfSecretLists)
                              .putMember("map", mapOfSecretLists)
                              .build();

        String str = ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> {
            ser.writeList(s.member("list"), null, 1, (state, ls) -> ls.writeString(secretList.listMember(), "marker"));
            ser.writeList(s.member("nested"), null, 1, (state, ls) -> {
                ls.writeList(listOfSecretLists.listMember(), null, 1,
                             (st, inner) -> inner.writeString(secretList.listMember(), "marker"));
            });
            ser.writeMap(s.member("map"), null, 1, (state, ms) -> {
                ms.writeEntry(mapOfSecretLists.mapKeyMember(), "k", null, (st, vs) -> {
                    vs.writeList(mapOfSecretLists.mapValueMember(), null, 1,
                                 (st2, inner) -> inner.writeString(secretList.listMember(), "marker"));
                });
            });
        }));

        assertThat(str).isEqualTo("Struct(list=" + REDACTED + ", nested=" + REDACTED + ", map=" + REDACTED + ")");
    }

    @Test
    void redactsElementsOfAListWrittenDirectly() {
        Schema secret = Schema.createString(id("Secret"), SensitiveTrait.INSTANCE);
        Schema secretList = Schema.listBuilder(id("SecretList")).putMember("member", secret).build();

        ToStringSerializer serializer = new ToStringSerializer();
        serializer.writeList(PreludeSchemas.DOCUMENT, null, 2, (state, ls) -> {
            ls.writeString(secretList.listMember(), "marker");
            ls.writeNull(secretList.listMember());
        });

        assertThat(serializer.toString()).isEqualTo("[" + REDACTED + ", null]");
    }

    @Test
    void doesNotRedactTheMembersOfARootSensitiveStructure() {
        Schema schema = Schema.structureBuilder(id("Credentials"), SensitiveTrait.INSTANCE)
                              .putMember("user", PreludeSchemas.STRING)
                              .putMember("password", PreludeSchemas.STRING, SensitiveTrait.INSTANCE)
                              .build();

        String str = ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> {
            ser.writeString(s.member("user"), "u");
            ser.writeString(s.member("password"), "p");
        }));

        assertThat(str).isEqualTo("Credentials(user=u, password=" + REDACTED + ")");
    }

    @Test
    void writesNestedStructuresWithTheirTargetName() {
        Schema child = Schema.structureBuilder(id("Child")).putMember("v", PreludeSchemas.STRING).build();
        Schema parent = Schema.structureBuilder(id("Parent")).putMember("child", child).build();

        String str = ToStringSerializer.serialize(TestHelper.create(parent, (s, ser) -> {
            ser.writeStruct(s.member("child"), TestHelper.create(child, (cs, cser) -> cser.writeString(cs.member("v"), "x")));
        }));

        assertThat(str).isEqualTo("Parent(child=Child(v=x))");
    }

    @Test
    void matchesTheSdkToStringFormatForScalarValues() {
        Schema schema = Schema.structureBuilder(id("AllTypes"))
                              .putMember("Boolean", PreludeSchemas.BOOLEAN)
                              .putMember("Byte", PreludeSchemas.BYTE)
                              .putMember("Short", PreludeSchemas.SHORT)
                              .putMember("Integer", PreludeSchemas.INTEGER)
                              .putMember("Long", PreludeSchemas.LONG)
                              .putMember("Float", PreludeSchemas.FLOAT)
                              .putMember("Double", PreludeSchemas.DOUBLE)
                              .putMember("BigInteger", PreludeSchemas.BIG_INTEGER)
                              .putMember("BigDecimal", PreludeSchemas.BIG_DECIMAL)
                              .putMember("String", PreludeSchemas.STRING)
                              .putMember("Blob", PreludeSchemas.BLOB)
                              .putMember("EmptyBlob", PreludeSchemas.BLOB)
                              .putMember("Timestamp", PreludeSchemas.TIMESTAMP)
                              .build();
        Instant timestamp = Instant.parse("2025-03-10T12:30:00Z");
        byte[] bytes = {0, 15, (byte) 0x80, (byte) 0xff};

        String str = ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> {
            ser.writeBoolean(s.member("Boolean"), true);
            ser.writeByte(s.member("Byte"), (byte) -1);
            ser.writeShort(s.member("Short"), (short) 2);
            ser.writeInteger(s.member("Integer"), 3);
            ser.writeLong(s.member("Long"), 4L);
            ser.writeFloat(s.member("Float"), 1.5f);
            ser.writeDouble(s.member("Double"), 2.25d);
            ser.writeBigInteger(s.member("BigInteger"), new BigInteger("12345678901234567890"));
            ser.writeBigDecimal(s.member("BigDecimal"), new BigDecimal("1.10"));
            ser.writeString(s.member("String"), "text");
            ser.writeBlob(s.member("Blob"), ByteBuffer.wrap(bytes));
            ser.writeBlob(s.member("EmptyBlob"), ByteBuffer.allocate(0));
            ser.writeTimestamp(s.member("Timestamp"), timestamp);
        }));

        // The SDK's generated toString methods build the same string with ToString; blobs are SdkBytes, whose
        // toString is ToString.builder("SdkBytes").add("bytes", byte[]).
        String expected = ToString.builder("AllTypes")
                                  .add("Boolean", true)
                                  .add("Byte", (byte) -1)
                                  .add("Short", (short) 2)
                                  .add("Integer", 3)
                                  .add("Long", 4L)
                                  .add("Float", 1.5f)
                                  .add("Double", 2.25d)
                                  .add("BigInteger", new BigInteger("12345678901234567890"))
                                  .add("BigDecimal", new BigDecimal("1.10"))
                                  .add("String", "text")
                                  .add("Blob", ToString.builder("SdkBytes").add("bytes", bytes).build())
                                  .add("EmptyBlob", ToString.builder("SdkBytes").add("bytes", new byte[0]).build())
                                  .add("Timestamp", timestamp)
                                  .build();
        assertThat(str).isEqualTo(expected);
    }

    @Test
    void writesBlobsWithoutMovingTheBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {9, 1, 2, 9});
        buffer.position(1);
        buffer.limit(3);

        ToStringSerializer serializer = new ToStringSerializer();
        serializer.writeBlob(PreludeSchemas.BLOB, buffer);

        assertThat(serializer.toString()).isEqualTo("SdkBytes(bytes=0x0102)");
        assertThat(buffer.position()).isEqualTo(1);
        assertThat(buffer.limit()).isEqualTo(3);
    }

    @Test
    void writesDocumentsInTheSdkDocumentFormat() {
        Map<String, Document> members = new LinkedHashMap<>();
        members.put("s", Document.of("a\"b\\c"));
        members.put("n", Document.of(1));
        members.put("b", Document.of(false));
        members.put("l", Document.of(Arrays.asList(Document.of(1.5d), null)));
        members.put("m", Document.EMPTY_MAP);

        ToStringSerializer serializer = new ToStringSerializer();
        serializer.writeDocument(PreludeSchemas.DOCUMENT, Document.of(members));

        assertThat(serializer.toString()).isEqualTo("{\"s\": \"a\\\"b\\\\c\",\"n\": 1,\"b\": false,\"l\": [1.5, null],\"m\": {}}");
    }

    @Test
    void redactsSensitiveDocuments() {
        Schema schema = Schema.structureBuilder(id("Struct"))
                              .putMember("doc", PreludeSchemas.DOCUMENT, SensitiveTrait.INSTANCE)
                              .build();

        String str = ToStringSerializer.serialize(TestHelper.create(schema, (s, ser) -> {
            ser.writeDocument(s.member("doc"), Document.of("marker"));
        }));

        assertThat(str).isEqualTo("Struct(doc=" + REDACTED + ")");
    }

    @Test
    void closeDiscardsTheOutput() {
        ToStringSerializer serializer = new ToStringSerializer();
        serializer.writeString(PreludeSchemas.STRING, "value");
        serializer.close();

        assertThat(serializer.toString()).isEmpty();
    }

    @Test
    void redactionPlaceholderMatchesTheSdkCodeGenerator() {
        assertThat(ToStringSerializer.REDACTED).isEqualTo(REDACTED);
    }

    private static void writeTwoEntries(Schema struct, ShapeSerializer ser, Schema mapSchema) {
        ser.writeMap(struct.member("foo"), mapSchema, 2, (innerMapSchema, map) -> {
            map.writeEntry(innerMapSchema.member("key"), "a", innerMapSchema, (mapSchema2, ms) -> {
                ms.writeString(mapSchema2.member("value"), "hi");
            });
            map.writeEntry(innerMapSchema.member("key"), "b", innerMapSchema, (mapSchema2, ms) -> {
                ms.writeNull(mapSchema2.member("value"));
            });
        });
    }
}
