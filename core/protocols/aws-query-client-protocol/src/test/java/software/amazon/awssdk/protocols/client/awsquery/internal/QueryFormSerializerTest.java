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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocols.client.awsquery.internal.QueryTestModel.LISTS;
import static software.amazon.awssdk.protocols.client.awsquery.internal.QueryTestModel.MAPS;
import static software.amazon.awssdk.protocols.client.awsquery.internal.QueryTestModel.NESTED_STRUCTURES;
import static software.amazon.awssdk.protocols.client.awsquery.internal.QueryTestModel.SIMPLE;
import static software.amazon.awssdk.protocols.client.awsquery.internal.QueryTestModel.TIMESTAMPS;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Tests the form bodies {@link QueryFormSerializer} writes. Expected bodies are those of the legacy
 * {@code QueryProtocolMarshaller} for the same input, except where a comment names a difference; cases named after a
 * Smithy protocol test use that test's input and expected body.
 */
class QueryFormSerializerTest {
    private static final String PREFIX = "Action=Op&Version=2020-01-08";
    private static final Instant EPOCH_2015 = Instant.parse("2015-01-25T08:00:00Z");

    @Test
    void forAwsQuery_noMembers_writesActionAndVersionOnly() {
        assertThat(awsQuery(SchemaStruct.builder(SIMPLE).build())).isEqualTo(PREFIX);
    }

    @Test
    void forAwsQuery_actionAndVersion_arePercentEncoded() {
        QueryFormSerializer serializer = QueryFormSerializer.forAwsQuery("Op Name", "2020-01-08~*");
        assertThat(finish(serializer)).isEqualTo("Action=Op%20Name&Version=2020-01-08~%2A");
    }

    @Test
    void forAwsQuery_nullActionOrVersion_throwsNullPointerException() {
        assertThatThrownBy(() -> QueryFormSerializer.forAwsQuery(null, "1")).isInstanceOf(NullPointerException.class)
                                                                            .hasMessageContaining("action");
        assertThatThrownBy(() -> QueryFormSerializer.forEc2Query("Op", null)).isInstanceOf(NullPointerException.class)
                                                                             .hasMessageContaining("version");
    }

    // QuerySimpleInputParamsStrings, ...StringAndBooleanTrue, ...Integer, ...Float, ...Blob (and the Ec2 ones)
    @Test
    void scalars_smithyCases_matchExpectedBodies() {
        SchemaStruct input = SchemaStruct.builder(SIMPLE)
                                         .set("Foo", "val1")
                                         .set("Bar", "val2")
                                         .set("Baz", true)
                                         .set("Bam", 10)
                                         .set("Boo", 10.8)
                                         .set("Qux", bytes("value"))
                                         .build();
        String expected = PREFIX + "&Foo=val1&Bar=val2&Baz=true&Bam=10&Boo=10.8&Qux=dmFsdWU%3D";
        assertThat(awsQuery(input)).isEqualTo(expected);
        assertThat(ec2Query(input)).isEqualTo(expected);
    }

    @Test
    void integralTypes_writeDecimalText() {
        SchemaStruct input = SchemaStruct.builder(SIMPLE)
                                         .set("Baz", false)
                                         .set("Bam", Integer.MIN_VALUE)
                                         .set("ByteValue", (byte) -8)
                                         .set("ShortValue", (short) 300)
                                         .set("LongValue", Long.MAX_VALUE)
                                         .set("BigIntegerValue", new BigInteger("-123456789012345678901234567890"))
                                         .build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&Baz=false&Bam=-2147483648&ByteValue=-8&ShortValue=300"
                                              + "&LongValue=9223372036854775807"
                                              + "&BigIntegerValue=-123456789012345678901234567890");
    }

    static Stream<Arguments> floatingPoint() {
        return Stream.of(
            Arguments.of(10.8f, 10.8, "10.8", "10.8"),
            Arguments.of(1.0f, 1.0, "1.0", "1.0"),
            Arguments.of(1.0e10f, 1.0e10, "1.0E10", "1.0E10"),
            Arguments.of(-0.0f, 1.5e-7, "-0.0", "1.5E-7"),
            // AwsQuerySupportsNaNFloatInputs, ...InfinityFloatInputs, ...NegativeInfinityFloatInputs
            Arguments.of(Float.NaN, Double.NaN, "NaN", "NaN"),
            Arguments.of(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, "Infinity", "Infinity"),
            Arguments.of(Float.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, "-Infinity", "-Infinity"));
    }

    @ParameterizedTest
    @MethodSource("floatingPoint")
    void floatAndDouble_writeJavaToString(float f, double d, String expectedFloat, String expectedDouble) {
        SchemaStruct input = SchemaStruct.builder(SIMPLE).set("FloatValue", f).set("Boo", d).build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&FloatValue=" + expectedFloat + "&Boo=" + expectedDouble);
    }

    @Test
    void bigDecimal_writesPlainNotation() {
        // The legacy query marshaller has no bigDecimal marshaller.
        SchemaStruct input = SchemaStruct.builder(SIMPLE).set("BigDecimalValue", new BigDecimal("1E+3")).build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&BigDecimalValue=1000");
        input = SchemaStruct.builder(SIMPLE).set("BigDecimalValue", new BigDecimal("-0.000123")).build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&BigDecimalValue=-0.000123");
    }

    @Test
    void blob_writesPaddedBase64PercentEncodedWithoutConsumingTheBuffer() {
        ByteBuffer blob = ByteBuffer.wrap(new byte[] {0, (byte) 0xfb, (byte) 0xff, 1}, 1, 3);
        SchemaStruct input = SchemaStruct.builder(SIMPLE).set("Qux", blob).build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&Qux=%2B%2F8B");
        assertThat(blob.remaining()).isEqualTo(3);
    }

    static Stream<Arguments> encodings() {
        return Stream.of(
            Arguments.of("AZaz09-._~", "AZaz09-._~"),
            Arguments.of("a b", "a%20b"),
            Arguments.of("*+/=&?#%", "%2A%2B%2F%3D%26%3F%23%25"),
            Arguments.of("\t\r\n\"'<>", "%09%0D%0A%22%27%3C%3E"),
            Arguments.of("", ""),
            Arguments.of("\u00e9", "%C3%A9"),
            Arguments.of("\u00ff\u0100", "%C3%BF%C4%80"),
            Arguments.of("\u4e2d", "%E4%B8%AD"),
            Arguments.of("\ud83d\ude00", "%F0%9F%98%80"),
            Arguments.of("x\ud83dy", "x%3Fy"),
            Arguments.of("x\ude00", "x%3F"),
            Arguments.of("x\ud83d", "x%3F"),
            Arguments.of("ab\u00e9cd*", "ab%C3%A9cd%2A"));
    }

    @ParameterizedTest
    @MethodSource("encodings")
    void writeString_percentEncodesUtf8WithRfc3986Unreserved(String value, String expected) {
        SchemaStruct input = SchemaStruct.builder(SIMPLE).set("Foo", value).build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&Foo=" + expected);
    }

    @Test
    void writeString_longNonAsciiValue_isWrittenInFull() {
        char[] chars = new char[3000];
        Arrays.fill(chars, '\u4e2d');
        chars[0] = 'a';
        String value = new String(chars);
        StringBuilder expected = new StringBuilder(PREFIX).append("&Foo=a");
        for (int i = 1; i < chars.length; i++) {
            expected.append("%E4%B8%AD");
        }
        assertThat(awsQuery(SchemaStruct.builder(SIMPLE).set("Foo", value).build())).isEqualTo(expected.toString());
    }

    @Test
    void memberName_isPercentEncoded() {
        SchemaStruct input = SchemaStruct.builder(SIMPLE).set("NonAsciiXmlName", "v").build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&Na%20me%C3%A9=v");
    }

    // QueryTimestampsInput and Ec2TimestampsInput
    @Test
    void timestamps_smithyCases_useDateTimeByDefaultAndHonorTimestampFormat() {
        SchemaStruct input = SchemaStruct.builder(TIMESTAMPS)
                                         .set("normalFormat", EPOCH_2015)
                                         .set("epochMember", EPOCH_2015)
                                         .set("epochTarget", EPOCH_2015)
                                         .build();
        // The legacy SDK writes epoch-seconds with three fraction digits (1422172800.000).
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&normalFormat=2015-01-25T08%3A00%3A00Z"
                                              + "&epochMember=1422172800&epochTarget=1422172800");
        assertThat(ec2Query(input)).isEqualTo(PREFIX + "&NormalFormat=2015-01-25T08%3A00%3A00Z"
                                              + "&EpochMember=1422172800&EpochTarget=1422172800");
    }

    @Test
    void timestamps_fractionsAndHttpDate_matchLegacySpellings() {
        Instant value = Instant.parse("2015-01-05T08:00:00.100Z");
        SchemaStruct input = SchemaStruct.builder(TIMESTAMPS)
                                         .set("normalFormat", value)
                                         .set("epochMember", Instant.ofEpochMilli(1422172800001L))
                                         .set("httpDate", value)
                                         .build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&normalFormat=2015-01-05T08%3A00%3A00.100Z"
                                              + "&epochMember=1422172800.001"
                                              + "&httpDate=Mon%2C%2005%20Jan%202015%2008%3A00%3A00%20GMT");
    }

    @Test
    void timestamps_unknownFormat_throwsSerializationException() {
        SchemaStruct input = SchemaStruct.builder(TIMESTAMPS).set("unknownFormat", EPOCH_2015).build();
        assertThatThrownBy(() -> awsQuery(input)).isInstanceOf(SerializationException.class)
                                                 .hasMessageContaining("Unknown timestamp format");
    }

    // NestedStructures and Ec2NestedStructures
    @Test
    void nestedStructures_joinMemberNamesWithDots() {
        SchemaStruct recursive = SchemaStruct.builder(QueryTestModel.STRUCT_ARG).set("StringArg", "baz").build();
        SchemaStruct nested = SchemaStruct.builder(QueryTestModel.STRUCT_ARG)
                                          .set("StringArg", "foo")
                                          .set("OtherArg", true)
                                          .set("RecursiveArg", recursive)
                                          .build();
        SchemaStruct input = SchemaStruct.builder(NESTED_STRUCTURES).set("Nested", nested).build();
        String expected = PREFIX + "&Nested.StringArg=foo&Nested.OtherArg=true&Nested.RecursiveArg.StringArg=baz";
        assertThat(awsQuery(input)).isEqualTo(expected);
        assertThat(ec2Query(input)).isEqualTo(expected);
    }

    @Test
    void nestedStructures_emptyStructure_writesNothing() {
        SchemaStruct input = SchemaStruct.builder(NESTED_STRUCTURES)
                                         .set("Nested", SchemaStruct.builder(QueryTestModel.STRUCT_ARG).build())
                                         .build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX);
    }

    // QueryLists
    @Test
    void awsQueryLists_nonFlattened_useMemberSegment() {
        SchemaStruct input = SchemaStruct.builder(LISTS)
                                         .set("ListArg", Arrays.asList("foo", "bar", "baz"))
                                         .set("ComplexListArg", Arrays.asList(greeting("hello"), greeting("hola")))
                                         .build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&ListArg.member.1=foo&ListArg.member.2=bar"
                                              + "&ListArg.member.3=baz&ComplexListArg.member.1.hi=hello"
                                              + "&ComplexListArg.member.2.hi=hola");
    }

    // EmptyQueryLists
    @Test
    void awsQueryLists_empty_writeNameWithEmptyValue() {
        SchemaStruct input = SchemaStruct.builder(LISTS).set("ListArg", Collections.emptyList()).build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&ListArg=");
    }

    // FlattenedQueryLists, QueryListArgWithXmlNameMember, QueryFlattenedListArgWithXmlName, QueryNestedStructWithList
    @Test
    void awsQueryLists_flattenedAndRenamed_smithyCases() {
        SchemaStruct input = SchemaStruct.builder(LISTS)
                                         .set("FlattenedListArg", Arrays.asList("A", "B"))
                                         .set("ListArgWithXmlNameMember", Arrays.asList("A", "B"))
                                         .set("FlattenedListArgWithXmlName", Arrays.asList("A", "B"))
                                         .set("NestedWithList", SchemaStruct.builder(QueryTestModel.NESTED_STRUCT_WITH_LIST)
                                                                            .set("ListArg", Arrays.asList("A", "B"))
                                                                            .build())
                                         .build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&FlattenedListArg.1=A&FlattenedListArg.2=B"
                                              + "&ListArgWithXmlNameMember.item.1=A&ListArgWithXmlNameMember.item.2=B"
                                              + "&Hi.1=A&Hi.2=B"
                                              + "&NestedWithList.ListArg.member.1=A&NestedWithList.ListArg.member.2=B");
    }

    @Test
    void awsQueryLists_listOfLists_nestMemberSegmentsOnce() {
        SchemaStruct input = SchemaStruct.builder(LISTS)
                                         .set("ListOfLists", Arrays.asList(Arrays.asList("a", "b"),
                                                                           Collections.emptyList(),
                                                                           Collections.singletonList("c")))
                                         .build();
        // smithy-java repeats the member segment (ListOfLists.member.1.member.member.1); the legacy SDK does not.
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&ListOfLists.member.1.member.1=a"
                                              + "&ListOfLists.member.1.member.2=b&ListOfLists.member.2="
                                              + "&ListOfLists.member.3.member.1=c");
    }

    @Test
    void lists_sparseNullElements_keepTheirIndex() {
        SchemaStruct input = SchemaStruct.builder(LISTS).set("SparseList", Arrays.asList("a", null, "c")).build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&SparseList.member.1=a&SparseList.member.3=c");
        assertThat(ec2Query(input)).isEqualTo(PREFIX + "&SparseList.1=a&SparseList.3=c");
    }

    @Test
    void lists_timestampElements_useTheTargetTimestampFormat() {
        SchemaStruct input = SchemaStruct.builder(LISTS).set("EpochList", Collections.singletonList(EPOCH_2015)).build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&EpochList.member.1=1422172800");
    }

    // Ec2Lists, Ec2EmptyQueryLists, Ec2ListArgWithXmlNameMember, Ec2ListMemberWithXmlName, Ec2ListNestedStructWithList
    @Test
    void ec2QueryLists_areAlwaysFlattenedAndEmptyListsAreOmitted() {
        SchemaStruct input = SchemaStruct.builder(LISTS)
                                         .set("ListArg", Arrays.asList("foo", "bar", "baz"))
                                         .set("ComplexListArg", Arrays.asList(greeting("hello"), greeting("hola")))
                                         .set("FlattenedListArg", Collections.emptyList())
                                         .set("ListArgWithXmlNameMember", Arrays.asList("A", "B"))
                                         .set("FlattenedListArgWithXmlName", Arrays.asList("A", "B"))
                                         .set("NestedWithList", SchemaStruct.builder(QueryTestModel.NESTED_STRUCT_WITH_LIST)
                                                                            .set("ListArg", Arrays.asList("A", "B"))
                                                                            .build())
                                         .set("ListOfLists", Arrays.asList(Arrays.asList("a", "b"),
                                                                           Collections.singletonList("c")))
                                         .build();
        assertThat(ec2Query(input)).isEqualTo(PREFIX + "&ListArg.1=foo&ListArg.2=bar&ListArg.3=baz"
                                              + "&ComplexListArg.1.Hi=hello&ComplexListArg.2.Hi=hola"
                                              + "&ListArgWithXmlNameMember.1=A&ListArgWithXmlNameMember.2=B"
                                              + "&Hi.1=A&Hi.2=B"
                                              + "&NestedWithList.ListArg.1=A&NestedWithList.ListArg.2=B"
                                              + "&ListOfLists.1.1=a&ListOfLists.1.2=b&ListOfLists.2.1=c");
    }

    // Ec2Query, Ec2QueryIsPreferred, Ec2XmlNameIsUppercased, Ec2ProtocolIdempotencyTokenAutoFillIsSet
    @Test
    void ec2QueryNames_preferEc2QueryNameThenCapitalizedXmlNameOrMemberName() {
        SchemaStruct input = SchemaStruct.builder(SIMPLE)
                                         .set("HasQueryName", "Hi")
                                         .set("HasQueryAndXmlName", "Hi")
                                         .set("UsesXmlName", "Hi")
                                         .set("token", "00000000-0000-4000-8000-000000000123")
                                         .build();
        assertThat(ec2Query(input)).isEqualTo(PREFIX + "&HasQueryName=Hi&HasQueryAndXmlName=Hi&UsesXmlName=Hi"
                                              + "&Token=00000000-0000-4000-8000-000000000123");
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&HasQueryName=Hi&hasQueryAndXmlName=Hi&usesXmlName=Hi"
                                              + "&token=00000000-0000-4000-8000-000000000123");
    }

    // Ec2QueryNameDistinctFromXmlNameAndMemberName
    @Test
    void ec2QueryNames_distinctFromXmlNameAndMemberName() {
        SchemaStruct input = SchemaStruct.builder(QueryTestModel.EC2_NAMES)
                                         .set("queryName", "value1")
                                         .set("queryAndXmlName", "value2")
                                         .set("xmlNameOnly", "value3")
                                         .build();
        assertThat(ec2Query(input)).isEqualTo(PREFIX + "&QueryName=value1&queryAndXmlName=value2&XmlNameOnly=value3");
    }

    // QuerySimpleQueryMaps, QuerySimpleQueryMapsWithXmlName, QueryComplexQueryMaps, QueryQueryMapWithMemberXmlName
    @Test
    void awsQueryMaps_entryKeyAndValueSegments() {
        SchemaStruct input = SchemaStruct.builder(MAPS)
                                         .set("MapArg", map("bar", "Bar", "foo", "Foo"))
                                         .set("RenamedMapArg", map("foo", "Foo"))
                                         .set("ComplexMapArg", map("bar", greeting("Bar"), "foo", greeting("Foo")))
                                         .set("MapWithXmlMemberName", map("bar", "Bar", "foo", "Foo"))
                                         .build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&MapArg.entry.1.key=bar&MapArg.entry.1.value=Bar"
                                              + "&MapArg.entry.2.key=foo&MapArg.entry.2.value=Foo"
                                              + "&Foo.entry.1.key=foo&Foo.entry.1.value=Foo"
                                              + "&ComplexMapArg.entry.1.key=bar&ComplexMapArg.entry.1.value.hi=Bar"
                                              + "&ComplexMapArg.entry.2.key=foo&ComplexMapArg.entry.2.value.hi=Foo"
                                              + "&MapWithXmlMemberName.entry.1.K=bar&MapWithXmlMemberName.entry.1.V=Bar"
                                              + "&MapWithXmlMemberName.entry.2.K=foo&MapWithXmlMemberName.entry.2.V=Foo");
    }

    // QueryEmptyQueryMaps, QueryFlattenedQueryMaps, QueryFlattenedQueryMapsWithXmlName, QueryQueryMapOfLists,
    // QueryNestedStructWithMap
    @Test
    void awsQueryMaps_flattenedNestedAndEmpty_smithyCases() {
        SchemaStruct input = SchemaStruct.builder(MAPS)
                                         .set("MapArg", Collections.emptyMap())
                                         .set("FlattenedMap", map("bar", "Bar", "foo", "Foo"))
                                         .set("FlattenedMapWithXmlName", map("bar", "Bar", "foo", "Foo"))
                                         .set("MapOfLists", map("bar", Arrays.asList("C", "D"),
                                                                "foo", Arrays.asList("A", "B")))
                                         .set("NestedStructWithMap", SchemaStruct.builder(QueryTestModel.NESTED_STRUCT_WITH_MAP)
                                                                                 .set("MapArg", map("bar", "Bar"))
                                                                                 .build())
                                         .build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&FlattenedMap.1.key=bar&FlattenedMap.1.value=Bar"
                                              + "&FlattenedMap.2.key=foo&FlattenedMap.2.value=Foo"
                                              + "&Hi.1.K=bar&Hi.1.V=Bar&Hi.2.K=foo&Hi.2.V=Foo"
                                              + "&MapOfLists.entry.1.key=bar&MapOfLists.entry.1.value.member.1=C"
                                              + "&MapOfLists.entry.1.value.member.2=D&MapOfLists.entry.2.key=foo"
                                              + "&MapOfLists.entry.2.value.member.1=A&MapOfLists.entry.2.value.member.2=B"
                                              + "&NestedStructWithMap.MapArg.entry.1.key=bar"
                                              + "&NestedStructWithMap.MapArg.entry.1.value=Bar");
    }

    @Test
    void maps_nestedMapsEmptyListValuesAndNullValues() {
        SchemaStruct input = SchemaStruct.builder(MAPS)
                                         .set("MapOfMaps", map("o1", map("i1", "v1"), "o2", map("i2", "v2")))
                                         .set("MapOfLists", map("empty", Collections.emptyList()))
                                         .set("SparseMap", map("k", null, "k2", "v"))
                                         .build();
        assertThat(awsQuery(input)).isEqualTo(PREFIX + "&MapOfLists.entry.1.key=empty&MapOfLists.entry.1.value="
                                              + "&MapOfMaps.entry.1.key=o1&MapOfMaps.entry.1.value.entry.1.key=i1"
                                              + "&MapOfMaps.entry.1.value.entry.1.value=v1"
                                              + "&MapOfMaps.entry.2.key=o2&MapOfMaps.entry.2.value.entry.1.key=i2"
                                              + "&MapOfMaps.entry.2.value.entry.1.value=v2"
                                              + "&SparseMap.entry.1.key=k&SparseMap.entry.2.key=k2"
                                              + "&SparseMap.entry.2.value=v");
    }

    @Test
    void ec2QueryMaps_useTheAwsQueryMapRules() {
        // smithy-java rejects maps in ec2Query; the legacy SDK writes them as awsQuery does.
        SchemaStruct input = SchemaStruct.builder(MAPS)
                                         .set("MapArg", map("bar", "Bar"))
                                         .set("MapOfLists", map("k", Arrays.asList("A", "B")))
                                         .build();
        assertThat(ec2Query(input)).isEqualTo(PREFIX + "&MapArg.entry.1.key=bar&MapArg.entry.1.value=Bar"
                                              + "&MapOfLists.entry.1.key=k&MapOfLists.entry.1.value.1=A"
                                              + "&MapOfLists.entry.1.value.2=B");
    }

    @Test
    void writeDocument_throwsSerializationExceptionNamingTheSchema() {
        SchemaStruct input = SchemaStruct.builder(SIMPLE).set("DocumentValue", Document.of("x")).build();
        assertThatThrownBy(() -> awsQuery(input)).isInstanceOf(SerializationException.class)
                                                 .hasMessageContaining("do not support document types")
                                                 .hasMessageContaining("DocumentValue");
    }

    @Test
    void writeString_nonMemberSchema_throwsSerializationException() {
        QueryFormSerializer serializer = QueryFormSerializer.forAwsQuery("Op", "1");
        Schema root = SIMPLE;
        assertThatThrownBy(() -> serializer.writeString(root, "x")).isInstanceOf(SerializationException.class)
                                                                   .hasMessageContaining("must be a structure member");
    }

    @Test
    void finish_returnsExactArrayBackedBodyAndCanBeCalledOnce() {
        QueryFormSerializer serializer = QueryFormSerializer.forAwsQuery("Op", "2020-01-08");
        SchemaStruct.builder(SIMPLE).set("Foo", "val1").build().serializeMembers(serializer);
        ByteBuffer body = serializer.finish();
        assertThat(body.position()).isZero();
        assertThat(body.hasArray()).isTrue();
        assertThat(body.arrayOffset()).isZero();
        assertThat(new String(body.array(), 0, body.remaining(), StandardCharsets.UTF_8)).isEqualTo(PREFIX + "&Foo=val1");
        assertThatThrownBy(serializer::finish).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> serializer.writeString(SIMPLE.member("Foo"), "x"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void initialCapacity_doesNotChangeTheBody() {
        SchemaStruct flat = SchemaStruct.builder(SIMPLE).set("Foo", "val1").set("Bar", "a b ü").set("Bam", 10).build();
        SchemaStruct lists = SchemaStruct.builder(LISTS)
                                         .set("ListArg", Arrays.asList("foo", "bar", "baz"))
                                         .set("ComplexListArg", Arrays.asList(greeting("hello"), greeting("hola")))
                                         .build();
        SchemaStruct maps = SchemaStruct.builder(MAPS)
                                        .set("MapArg", map("bar", "Bar", "foo", "Foo"))
                                        .build();
        for (SchemaStruct input : Arrays.asList(flat, lists, maps)) {
            String expectedAwsQuery = awsQuery(input);
            String expectedEc2Query = ec2Query(input);
            for (int capacity : new int[] {0, 1, 16, 4096}) {
                QueryFormSerializer aws = QueryFormSerializer.forAwsQuery("Op", "2020-01-08", capacity);
                input.serializeMembers(aws);
                assertThat(finish(aws)).as("awsQuery, capacity %s", capacity).isEqualTo(expectedAwsQuery);
                QueryFormSerializer ec2 = QueryFormSerializer.forEc2Query("Op", "2020-01-08", capacity);
                ec2.writeStruct(input.sdkSchema(), input);
                assertThat(finish(ec2)).as("ec2Query, capacity %s", capacity).isEqualTo(expectedEc2Query);
            }
        }
    }

    @Test
    void finishArray_returnsTheBodyArrayAndCanBeCalledOnce() {
        QueryFormSerializer serializer = QueryFormSerializer.forAwsQuery("Op", "2020-01-08", 4096);
        SchemaStruct.builder(SIMPLE).set("Foo", "val1").build().serializeMembers(serializer);
        int size = serializer.size();
        byte[] body = serializer.finishArray();
        assertThat(body).hasSize(4096);
        assertThat(new String(body, 0, size, StandardCharsets.UTF_8)).isEqualTo(PREFIX + "&Foo=val1");
        assertThat(serializer.size()).isEqualTo(size);
        assertThatThrownBy(serializer::finishArray).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(serializer::finish).isInstanceOf(IllegalStateException.class);

        QueryFormSerializer finished = QueryFormSerializer.forAwsQuery("Op", "2020-01-08");
        finished.finish();
        assertThatThrownBy(finished::finishArray).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requiredCapacity_isACapacityThatDoesNotGrow() {
        char[] spaces = new char[300];
        Arrays.fill(spaces, ' ');
        SchemaStruct small = SchemaStruct.builder(SIMPLE).set("Foo", "hello world").build();
        SchemaStruct large = SchemaStruct.builder(SIMPLE).set("Foo", new String(spaces)).set("Bar", "ü").build();
        for (SchemaStruct input : Arrays.asList(small, large)) {
            QueryFormSerializer first = QueryFormSerializer.forAwsQuery("Op", "2020-01-08", 0);
            input.serializeMembers(first);
            int required = first.requiredCapacity();
            assertThat(required).isGreaterThanOrEqualTo(first.size());
            String expected = finish(first);

            QueryFormSerializer second = QueryFormSerializer.forAwsQuery("Op", "2020-01-08", required);
            input.serializeMembers(second);
            int size = second.size();
            byte[] body = second.finishArray();
            assertThat(body).hasSize(required);
            assertThat(new String(body, 0, size, StandardCharsets.UTF_8)).isEqualTo(expected);
        }
    }

    @Test
    void finish_largeBody_growsTheBuffer() {
        char[] chars = new char[5000];
        Arrays.fill(chars, ' ');
        StringBuilder expected = new StringBuilder(PREFIX).append("&Foo=");
        for (int i = 0; i < chars.length; i++) {
            expected.append("%20");
        }
        assertThat(awsQuery(SchemaStruct.builder(SIMPLE).set("Foo", new String(chars)).build()))
            .isEqualTo(expected.toString());
    }

    @Test
    void schemaExtension_isRegisteredThroughServiceLoader() {
        assertThat(SIMPLE.member("UsesXmlName").getExtension(AwsQuerySchemaExtensions.KEY)).isNotNull();
        assertThat(SIMPLE.getExtension(AwsQuerySchemaExtensions.KEY)).isNull();
    }

    private static String awsQuery(SchemaStruct input) {
        QueryFormSerializer serializer = QueryFormSerializer.forAwsQuery("Op", "2020-01-08");
        input.serializeMembers(serializer);
        return finish(serializer);
    }

    private static String ec2Query(SchemaStruct input) {
        QueryFormSerializer serializer = QueryFormSerializer.forEc2Query("Op", "2020-01-08");
        serializer.writeStruct(input.sdkSchema(), input);
        return finish(serializer);
    }

    private static String finish(QueryFormSerializer serializer) {
        ByteBuffer body = serializer.finish();
        byte[] bytes = new byte[body.remaining()];
        body.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static ByteBuffer bytes(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }

    private static SchemaStruct greeting(String hi) {
        return SchemaStruct.builder(QueryTestModel.GREETING_STRUCT).set("hi", hi).build();
    }

    private static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            result.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return result;
    }
}
