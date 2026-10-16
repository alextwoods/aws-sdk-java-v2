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

package software.amazon.awssdk.codecs.json;

import static software.amazon.awssdk.codecs.json.TestCollections.setOf;

import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.RequiredTrait;
import software.amazon.awssdk.schema.traits.SparseTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Schemas of smithy-java's json-codec test model ({@code model/bench.smithy}, namespace
 * {@code smithy.java.json.bench}), for {@link GeneratedModelSerdeTest}. smithy-java generates model classes from the
 * model; the SDK tests use these schemas with {@link SchemaStruct} instead. Only the shapes the ported tests use are
 * defined.
 */
final class BenchModel {

    static final String NS = "smithy.java.json.bench";

    static final Schema STRING_LIST = list("StringList", PreludeSchemas.STRING);
    static final Schema INTEGER_LIST = list("IntegerList", PreludeSchemas.INTEGER);
    static final Schema STRING_MAP = map("StringMap", PreludeSchemas.STRING);
    static final Schema INTEGER_MAP = map("IntegerMap", PreludeSchemas.INTEGER);

    static final Schema COLOR = Schema.createEnum(id("Color"), setOf("RED", "GREEN", "BLUE", "YELLOW"));
    static final Schema COLOR_LIST = list("ColorList", COLOR);
    static final Schema PRIORITY = Schema.createIntEnum(id("Priority"),
                                                        setOf(Integer.MIN_VALUE, -7, 0, 1, 100, Integer.MAX_VALUE));

    static final Schema SPARSE_STRING_LIST = Schema.listBuilder(id("SparseStringList"), SparseTrait.INSTANCE)
                                                   .putMember("member", PreludeSchemas.STRING)
                                                   .build();
    static final Schema SPARSE_STRING_MAP = Schema.mapBuilder(id("SparseStringMap"), SparseTrait.INSTANCE)
                                                  .putMember("key", PreludeSchemas.STRING)
                                                  .putMember("value", PreludeSchemas.STRING)
                                                  .build();

    static final Schema INNER_STRUCT = Schema.structureBuilder(id("InnerStruct"))
                                             .putMember("value", PreludeSchemas.STRING)
                                             .putMember("numbers", INTEGER_LIST)
                                             .build();

    static final Schema NESTED_STRUCT = Schema.structureBuilder(id("NestedStruct"))
                                              .putMember("field1", PreludeSchemas.STRING, RequiredTrait.INSTANCE)
                                              .putMember("field2", PreludeSchemas.INTEGER, RequiredTrait.INSTANCE)
                                              .putMember("inner", INNER_STRUCT)
                                              .build();
    static final Schema NESTED_STRUCT_LIST = list("NestedStructList", NESTED_STRUCT);
    static final Schema NESTED_STRUCT_MAP = map("NestedStructMap", NESTED_STRUCT);

    static final Schema BENCH_UNION = Schema.unionBuilder(id("BenchUnion"))
                                            .putMember("stringValue", PreludeSchemas.STRING)
                                            .putMember("intValue", PreludeSchemas.INTEGER)
                                            .putMember("structValue", NESTED_STRUCT)
                                            .build();

    static final Schema COMPLEX_STRUCT =
        Schema.structureBuilder(id("ComplexStruct"))
              .putMember("id", PreludeSchemas.STRING, RequiredTrait.INSTANCE)
              .putMember("count", PreludeSchemas.INTEGER, RequiredTrait.INSTANCE)
              .putMember("enabled", PreludeSchemas.PRIMITIVE_BOOLEAN, RequiredTrait.INSTANCE)
              .putMember("ratio", PreludeSchemas.PRIMITIVE_DOUBLE, RequiredTrait.INSTANCE)
              .putMember("score", PreludeSchemas.PRIMITIVE_FLOAT, RequiredTrait.INSTANCE)
              .putMember("bigCount", PreludeSchemas.PRIMITIVE_LONG, RequiredTrait.INSTANCE)
              .putMember("optionalString", PreludeSchemas.STRING)
              .putMember("optionalInt", PreludeSchemas.INTEGER)
              .putMember("createdAt", PreludeSchemas.TIMESTAMP)
              .putMember("updatedAt", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
              .putMember("expiresAt", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE))
              .putMember("payload", PreludeSchemas.BLOB)
              .putMember("tags", STRING_LIST)
              .putMember("intList", INTEGER_LIST)
              .putMember("metadata", STRING_MAP)
              .putMember("intMap", INTEGER_MAP)
              .putMember("nested", NESTED_STRUCT, RequiredTrait.INSTANCE)
              .putMember("optionalNested", NESTED_STRUCT)
              .putMember("structList", NESTED_STRUCT_LIST)
              .putMember("structMap", NESTED_STRUCT_MAP)
              .putMember("choice", BENCH_UNION)
              .putMember("color", COLOR)
              .putMember("colorList", COLOR_LIST)
              .putMember("priority", PRIORITY)
              .putMember("sparseStrings", SPARSE_STRING_LIST)
              .putMember("sparseMap", SPARSE_STRING_MAP)
              .putMember("bigIntValue", PreludeSchemas.BIG_INTEGER)
              .putMember("bigDecValue", PreludeSchemas.BIG_DECIMAL)
              .putMember("freeformData", PreludeSchemas.DOCUMENT)
              .build();

    static final Schema SIMPLE_STRUCT = Schema.structureBuilder(id("SimpleStruct"))
                                              .putMember("name", PreludeSchemas.STRING, RequiredTrait.INSTANCE)
                                              .putMember("age", PreludeSchemas.INTEGER, RequiredTrait.INSTANCE)
                                              .putMember("active", PreludeSchemas.BOOLEAN)
                                              .putMember("score", PreludeSchemas.DOUBLE)
                                              .putMember("createdAt", PreludeSchemas.TIMESTAMP)
                                              .build();

    static final Schema NUMERIC_STRUCT = Schema.structureBuilder(id("NumericStruct"))
                                               .putMember("byteVal", PreludeSchemas.BYTE)
                                               .putMember("shortVal", PreludeSchemas.SHORT)
                                               .putMember("intVal", PreludeSchemas.INTEGER)
                                               .putMember("longVal", PreludeSchemas.LONG)
                                               .putMember("floatVal", PreludeSchemas.FLOAT)
                                               .putMember("doubleVal", PreludeSchemas.DOUBLE)
                                               .putMember("bigIntVal", PreludeSchemas.BIG_INTEGER)
                                               .putMember("bigDecVal", PreludeSchemas.BIG_DECIMAL)
                                               .build();

    static final Schema STRING_STRUCT = Schema.structureBuilder(id("StringStruct"))
                                              .putMember("value", PreludeSchemas.STRING, RequiredTrait.INSTANCE)
                                              .build();

    static final Schema TIMESTAMP_STRUCT =
        Schema.structureBuilder(id("TimestampStruct"))
              .putMember("epochSeconds", PreludeSchemas.TIMESTAMP)
              .putMember("dateTime", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
              .putMember("httpDate", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE))
              .build();

    static final Schema JSON_NAME_STRUCT = Schema.structureBuilder(id("JsonNameStruct"))
                                                 .putMember("id", PreludeSchemas.STRING, new JsonNameTrait("ID"))
                                                 .putMember("displayName", PreludeSchemas.STRING,
                                                            new JsonNameTrait("DisplayName"))
                                                 .putMember("unicodeName", PreludeSchemas.STRING,
                                                            new JsonNameTrait("\u00e9"))
                                                 .putMember("normalField", PreludeSchemas.STRING)
                                                 .build();

    static final Schema RECURSIVE_STRUCT;

    static final Schema BLOB_STRUCT = Schema.structureBuilder(id("BlobStruct"))
                                            .putMember("data", PreludeSchemas.BLOB)
                                            .build();

    static final Schema ALL_LISTS_STRUCT =
        Schema.structureBuilder(id("AllListsStruct"))
              .putMember("booleans", list("BooleanList", PreludeSchemas.BOOLEAN))
              .putMember("bytes", list("ByteList", PreludeSchemas.BYTE))
              .putMember("shorts", list("ShortList", PreludeSchemas.SHORT))
              .putMember("ints", INTEGER_LIST)
              .putMember("longs", list("LongList", PreludeSchemas.LONG))
              .putMember("floats", list("FloatList", PreludeSchemas.FLOAT))
              .putMember("doubles", list("DoubleList", PreludeSchemas.DOUBLE))
              .putMember("bigInts", list("BigIntegerList", PreludeSchemas.BIG_INTEGER))
              .putMember("bigDecs", list("BigDecimalList", PreludeSchemas.BIG_DECIMAL))
              .putMember("strings", STRING_LIST)
              .putMember("blobs", list("BlobList", PreludeSchemas.BLOB))
              .putMember("timestamps", list("TimestampList", PreludeSchemas.TIMESTAMP))
              .build();

    static {
        SchemaBuilder recursive = Schema.structureBuilder(id("RecursiveStruct"));
        recursive.putMember("value", PreludeSchemas.STRING);
        recursive.putMember("child", recursive);
        RECURSIVE_STRUCT = recursive.build();
    }

    private BenchModel() {
    }

    private static ShapeId id(String name) {
        return ShapeId.fromParts(NS, name);
    }

    private static Schema list(String name, Schema member) {
        return Schema.listBuilder(id(name)).putMember("member", member).build();
    }

    private static Schema map(String name, Schema value) {
        return Schema.mapBuilder(id(name))
                     .putMember("key", PreludeSchemas.STRING)
                     .putMember("value", value)
                     .build();
    }
}
