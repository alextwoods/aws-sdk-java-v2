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

package software.amazon.awssdk.protocols.client.awsquery.internal;

import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.Ec2QueryNameTrait;
import software.amazon.awssdk.schema.traits.SparseTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.XmlFlattenedTrait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;

/**
 * Schemas for the query serializer tests: the input shapes of the Smithy {@code aws.protocoltests.query#AwsQuery} and
 * {@code aws.protocoltests.ec2#AwsEc2} request test cases (same member names and traits), plus SDK additions for
 * nested collections, sparse collections and every scalar type.
 */
final class QueryTestModel {
    static final Schema STRING_LIST = Schema.listBuilder(id("StringList"))
                                            .putMember("member", PreludeSchemas.STRING)
                                            .build();

    static final Schema SPARSE_STRING_LIST = Schema.listBuilder(id("SparseStringList"), SparseTrait.INSTANCE)
                                                   .putMember("member", PreludeSchemas.STRING)
                                                   .build();

    static final Schema LIST_WITH_XML_NAME = Schema.listBuilder(id("ListWithXmlName"))
                                                   .putMember("member", PreludeSchemas.STRING, new XmlNameTrait("item"))
                                                   .build();

    static final Schema LIST_OF_LISTS = Schema.listBuilder(id("ListOfLists"))
                                              .putMember("member", STRING_LIST)
                                              .build();

    static final Schema GREETING_STRUCT = Schema.structureBuilder(id("GreetingStruct"))
                                                .putMember("hi", PreludeSchemas.STRING)
                                                .build();

    static final Schema GREETING_LIST = Schema.listBuilder(id("GreetingList"))
                                              .putMember("member", GREETING_STRUCT)
                                              .build();

    static final Schema NESTED_STRUCT_WITH_LIST = Schema.structureBuilder(id("NestedStructWithList"))
                                                        .putMember("ListArg", STRING_LIST)
                                                        .build();

    static final Schema EPOCH_SECONDS = Schema.createTimestamp(id("EpochSeconds"),
                                                               new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS));

    static final Schema EPOCH_LIST = Schema.listBuilder(id("EpochList"))
                                           .putMember("member", EPOCH_SECONDS)
                                           .build();

    static final Schema LISTS = Schema.structureBuilder(id("QueryListsInput"))
                                      .putMember("ListArg", STRING_LIST)
                                      .putMember("ComplexListArg", GREETING_LIST)
                                      .putMember("FlattenedListArg", STRING_LIST, XmlFlattenedTrait.INSTANCE)
                                      .putMember("ListArgWithXmlNameMember", LIST_WITH_XML_NAME)
                                      .putMember("FlattenedListArgWithXmlName", LIST_WITH_XML_NAME,
                                                 XmlFlattenedTrait.INSTANCE, new XmlNameTrait("Hi"))
                                      .putMember("NestedWithList", NESTED_STRUCT_WITH_LIST)
                                      .putMember("ListOfLists", LIST_OF_LISTS)
                                      .putMember("SparseList", SPARSE_STRING_LIST)
                                      .putMember("EpochList", EPOCH_LIST)
                                      .build();

    static final Schema STRING_MAP = Schema.mapBuilder(id("StringMap"))
                                           .putMember("key", PreludeSchemas.STRING)
                                           .putMember("value", PreludeSchemas.STRING)
                                           .build();

    static final Schema SPARSE_STRING_MAP = Schema.mapBuilder(id("SparseStringMap"), SparseTrait.INSTANCE)
                                                  .putMember("key", PreludeSchemas.STRING)
                                                  .putMember("value", PreludeSchemas.STRING)
                                                  .build();

    static final Schema COMPLEX_MAP = Schema.mapBuilder(id("ComplexMap"))
                                            .putMember("key", PreludeSchemas.STRING)
                                            .putMember("value", GREETING_STRUCT)
                                            .build();

    static final Schema MAP_WITH_XML_NAME = Schema.mapBuilder(id("MapWithXmlName"))
                                                  .putMember("key", PreludeSchemas.STRING, new XmlNameTrait("K"))
                                                  .putMember("value", PreludeSchemas.STRING, new XmlNameTrait("V"))
                                                  .build();

    static final Schema MAP_OF_LISTS = Schema.mapBuilder(id("MapOfLists"))
                                             .putMember("key", PreludeSchemas.STRING)
                                             .putMember("value", STRING_LIST)
                                             .build();

    static final Schema MAP_OF_MAPS = Schema.mapBuilder(id("MapOfMaps"))
                                            .putMember("key", PreludeSchemas.STRING)
                                            .putMember("value", STRING_MAP)
                                            .build();

    static final Schema NESTED_STRUCT_WITH_MAP = Schema.structureBuilder(id("NestedStructWithMap"))
                                                       .putMember("MapArg", STRING_MAP)
                                                       .build();

    static final Schema MAPS = Schema.structureBuilder(id("QueryMapsInput"))
                                     .putMember("MapArg", STRING_MAP)
                                     .putMember("RenamedMapArg", STRING_MAP, new XmlNameTrait("Foo"))
                                     .putMember("ComplexMapArg", COMPLEX_MAP)
                                     .putMember("MapWithXmlMemberName", MAP_WITH_XML_NAME)
                                     .putMember("FlattenedMap", STRING_MAP, XmlFlattenedTrait.INSTANCE)
                                     .putMember("FlattenedMapWithXmlName", MAP_WITH_XML_NAME,
                                                XmlFlattenedTrait.INSTANCE, new XmlNameTrait("Hi"))
                                     .putMember("MapOfLists", MAP_OF_LISTS)
                                     .putMember("NestedStructWithMap", NESTED_STRUCT_WITH_MAP)
                                     .putMember("MapOfMaps", MAP_OF_MAPS)
                                     .putMember("SparseMap", SPARSE_STRING_MAP)
                                     .build();

    static final Schema SIMPLE = Schema.structureBuilder(id("SimpleInputParamsInput"))
                                       .putMember("Foo", PreludeSchemas.STRING)
                                       .putMember("Bar", PreludeSchemas.STRING)
                                       .putMember("Baz", PreludeSchemas.BOOLEAN)
                                       .putMember("Bam", PreludeSchemas.INTEGER)
                                       .putMember("FloatValue", PreludeSchemas.FLOAT)
                                       .putMember("Boo", PreludeSchemas.DOUBLE)
                                       .putMember("Qux", PreludeSchemas.BLOB)
                                       .putMember("ByteValue", PreludeSchemas.BYTE)
                                       .putMember("ShortValue", PreludeSchemas.SHORT)
                                       .putMember("LongValue", PreludeSchemas.LONG)
                                       .putMember("BigIntegerValue", PreludeSchemas.BIG_INTEGER)
                                       .putMember("BigDecimalValue", PreludeSchemas.BIG_DECIMAL)
                                       .putMember("DocumentValue", PreludeSchemas.DOCUMENT)
                                       .putMember("HasQueryName", PreludeSchemas.STRING,
                                                  new Ec2QueryNameTrait("HasQueryName"))
                                       .putMember("HasQueryAndXmlName", PreludeSchemas.STRING,
                                                  new Ec2QueryNameTrait("HasQueryAndXmlName"),
                                                  new XmlNameTrait("hasQueryAndXmlName"))
                                       .putMember("UsesXmlName", PreludeSchemas.STRING, new XmlNameTrait("usesXmlName"))
                                       .putMember("token", PreludeSchemas.STRING)
                                       .putMember("NonAsciiXmlName", PreludeSchemas.STRING, new XmlNameTrait("Na me\u00e9"))
                                       .build();

    static final Schema EC2_NAMES = Schema.structureBuilder(id("DifferentNamesInput"))
                                          .putMember("queryName", PreludeSchemas.STRING,
                                                     new Ec2QueryNameTrait("QueryName"))
                                          .putMember("queryAndXmlName", PreludeSchemas.STRING,
                                                     new Ec2QueryNameTrait("queryAndXmlName"),
                                                     new XmlNameTrait("xmlName"))
                                          .putMember("xmlNameOnly", PreludeSchemas.STRING,
                                                     new XmlNameTrait("xmlNameOnly"))
                                          .build();

    static final Schema TIMESTAMPS = Schema.structureBuilder(id("QueryTimestampsInput"))
                                           .putMember("normalFormat", PreludeSchemas.TIMESTAMP)
                                           .putMember("epochMember", PreludeSchemas.TIMESTAMP,
                                                      new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS))
                                           .putMember("epochTarget", EPOCH_SECONDS)
                                           .putMember("httpDate", PreludeSchemas.TIMESTAMP,
                                                      new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE))
                                           .putMember("unknownFormat", PreludeSchemas.TIMESTAMP,
                                                      new TimestampFormatTrait("unknown-format"))
                                           .build();

    static final Schema STRUCT_ARG;
    static final Schema NESTED_STRUCTURES;

    static {
        SchemaBuilder structArg = Schema.structureBuilder(id("StructArg"));
        structArg.putMember("StringArg", PreludeSchemas.STRING);
        structArg.putMember("OtherArg", PreludeSchemas.BOOLEAN);
        structArg.putMember("RecursiveArg", structArg);
        STRUCT_ARG = structArg.build();
        NESTED_STRUCTURES = Schema.structureBuilder(id("NestedStructuresInput"))
                                  .putMember("Nested", STRUCT_ARG)
                                  .build();
    }

    private QueryTestModel() {
    }

    private static ShapeId id(String name) {
        return ShapeId.fromParts("aws.protocoltests.query", name);
    }
}
