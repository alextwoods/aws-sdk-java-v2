// SDK-owned ec2Query protocol tests for SDK serialization behavior that the upstream Smithy protocol tests do not
// cover, ported from the legacy ec2 and query-core suites (task 1.34, design D.1, Requirement 32.10).
// legacy-disposition.csv maps each legacy case to its test case id.
$version: "2.0"

namespace software.amazon.awssdk.protocoltests.ec2

use aws.api#service
use aws.auth#sigv4
use aws.protocols#ec2Query
use aws.protocols#ec2QueryName
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@service(sdkId: "Sdk EC2 Protocol")
@suppress(["SigV4Traits"])
@sigv4(name: "sdkec2query")
@ec2Query
@xmlNamespace(uri: "https://example.com/")
@title("SDK Ec2 Protocol Test Service")
service SdkEc2 {
    version: "2020-01-08"
    operations: [
        SdkQueryLists
        SdkTimestamps
        SdkXmlLists
        SdkEnums
    ]
}

/// ec2QueryName on a list member.
operation SdkQueryLists {
    input: SdkQueryListsInput
}

apply SdkQueryLists @httpRequestTests([
    {
        id: "SdkEc2QueryNameOnListMember"
        documentation: "Uses the ec2QueryName of a list member over its xmlName and member name"
        protocol: ec2Query
        method: "POST"
        uri: "/"
        headers: { "Content-Type": "application/x-www-form-urlencoded" }
        body: "Action=SdkQueryLists&Version=2020-01-08&listQueryName.1=foo&listQueryName.2=bar"
        bodyMediaType: "application/x-www-form-urlencoded"
        params: {
            listMember: ["foo", "bar"]
        }
    }
])

@input
structure SdkQueryListsInput {
    @ec2QueryName("listQueryName")
    @xmlName("ListMemberLocation")
    listMember: ListWithXmlName
}

list ListWithXmlName {
    @xmlName("item")
    member: String
}

/// A timestamp member.
operation SdkTimestamps {
    input: SdkTimestampsInput
}

apply SdkTimestamps @httpRequestTests([
    {
        id: "SdkEc2TimestampMillisecondPrecision"
        documentation: "Serializes a date-time timestamp with millisecond precision"
        protocol: ec2Query
        method: "POST"
        uri: "/"
        headers: { "Content-Type": "application/x-www-form-urlencoded" }
        body: "Action=SdkTimestamps&Version=2020-01-08&TimestampMember=2015-01-25T08%3A00%3A00.123Z"
        bodyMediaType: "application/x-www-form-urlencoded"
        params: {
            timestampMember: 1422172800.123
        }
    }
])

@input
structure SdkTimestampsInput {
    timestampMember: Timestamp
}

/// Flattened lists in the output.
operation SdkXmlLists {
    output: SdkXmlListsOutput
}

apply SdkXmlLists @httpResponseTests([
    {
        id: "SdkEc2FlattenedListOfStructures"
        documentation: "Parses a flattened list of structures"
        protocol: ec2Query
        code: 200
        headers: { "Content-Type": "text/xml;charset=UTF-8" }
        body: """
            <SdkXmlListsResponse xmlns="https://example.com/">
                <flattenedStructList>
                    <stringMember>foo</stringMember>
                </flattenedStructList>
                <flattenedStructList>
                    <stringMember>bar</stringMember>
                </flattenedStructList>
                <requestId>requestid</requestId>
            </SdkXmlListsResponse>
            """
        bodyMediaType: "application/xml"
        params: {
            flattenedStructList: [{ stringMember: "foo" }, { stringMember: "bar" }]
        }
    }
    {
        id: "SdkEc2SingleElementFlattenedList"
        documentation: "Parses a flattened list that holds exactly one element"
        protocol: ec2Query
        code: 200
        headers: { "Content-Type": "text/xml;charset=UTF-8" }
        body: """
            <SdkXmlListsResponse xmlns="https://example.com/">
                <flattenedStringList>val1</flattenedStringList>
                <requestId>requestid</requestId>
            </SdkXmlListsResponse>
            """
        bodyMediaType: "application/xml"
        params: {
            flattenedStringList: ["val1"]
        }
    }
])

@output
structure SdkXmlListsOutput {
    @xmlFlattened
    flattenedStructList: StructList

    @xmlFlattened
    flattenedStringList: StringList
}

list StringList {
    member: String
}

list StructList {
    member: SimpleStruct
}

structure SimpleStruct {
    stringMember: String
}

/// Enum values in a member, a list element, a map key and a map value. Read by the hand-written
/// UnknownEnumValueProtocolTest (legacy-disposition.csv rows json-core-output.json#5, #7, #9 and #10).
operation SdkEnums {
    output: SdkEnumsOutput
}

@output
structure SdkEnumsOutput {
    enumMember: SdkEnum
    enumList: SdkEnumList
    enumKeyMap: SdkEnumKeyMap
    enumValueMap: SdkEnumValueMap
}

list SdkEnumList {
    member: SdkEnum
}

map SdkEnumKeyMap {
    key: SdkEnum
    value: String
}

map SdkEnumValueMap {
    key: String
    value: SdkEnum
}

enum SdkEnum {
    ENUM_VALUE_1 = "EnumValue1"
    ENUM_VALUE_2 = "EnumValue2"
}
