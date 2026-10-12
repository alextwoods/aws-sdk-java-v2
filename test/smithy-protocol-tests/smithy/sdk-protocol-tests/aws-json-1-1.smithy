// SDK-owned awsJson1_1 protocol tests for SDK serialization behavior that the upstream Smithy protocol tests do not
// cover, ported from the legacy json-core and json-rpc suites (task 1.34, design D.1, Requirement 32.10).
// legacy-disposition.csv maps each legacy case to its test case id.
$version: "2.0"

namespace software.amazon.awssdk.protocoltests.json

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@service(sdkId: "Sdk Json Protocol")
@sigv4(name: "sdkjsonprotocol")
@awsJson1_1
@title("SDK Json 1.1 Protocol Test Service")
service SdkJsonProtocol {
    version: "2016-03-11"
    operations: [
        SdkTimestamps
        SdkCollections
        SdkDocuments
        SdkIdempotentOperation
        SdkNoOutput
    ]
}

/// Timestamps in the body.
operation SdkTimestamps {
    input: SdkTimestampsInputOutput
    output: SdkTimestampsInputOutput
}

apply SdkTimestamps @httpRequestTests([
    {
        id: "SdkAwsJson11TimestampMillisecondPrecision"
        documentation: """
            Serializes timestamps with millisecond precision: epoch seconds at the top level and in list and map
            values, and date-time."""
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "SdkJsonProtocol.SdkTimestamps" }
        body: """
            {
                "epochMember": 1422172801.123,
                "dateTimeMember": "2014-04-29T18:30:38.123Z",
                "epochList": [1422172801.123],
                "epochMap": { "key1": 1422172801.123 }
            }"""
        bodyMediaType: "application/json"
        params: {
            epochMember: 1422172801.123
            dateTimeMember: 1398796238.123
            epochList: [1422172801.123]
            epochMap: { key1: 1422172801.123 }
        }
    }
])

apply SdkTimestamps @httpResponseTests([
    {
        id: "SdkAwsJson11FractionalEpochSecondsResponse"
        documentation: "Parses epoch-seconds timestamps with fractional seconds at the top level, nested, and in lists and maps"
        protocol: awsJson1_1
        code: 200
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        body: """
            {
                "epochMember": 1398796238.123,
                "nested": { "epochMember": 1398796238.123 },
                "epochList": [1398796238.123],
                "epochMap": { "key1": 1398796238.123 }
            }"""
        bodyMediaType: "application/json"
        params: {
            epochMember: 1398796238.123
            nested: { epochMember: 1398796238.123 }
            epochList: [1398796238.123]
            epochMap: { key1: 1398796238.123 }
        }
    }
])

structure SdkTimestampsInputOutput {
    epochMember: Timestamp

    @timestampFormat("date-time")
    dateTimeMember: Timestamp

    nested: TimestampHolder

    epochList: TimestampList

    epochMap: TimestampMap
}

structure TimestampHolder {
    epochMember: Timestamp
}

list TimestampList {
    member: Timestamp
}

map TimestampMap {
    key: String
    value: Timestamp
}

/// Nested lists, and collections with null and empty elements.
operation SdkCollections {
    input: SdkCollectionsInputOutput
    output: SdkCollectionsInputOutput
}

apply SdkCollections @httpRequestTests([
    {
        id: "SdkAwsJson11NestedLists"
        documentation: "Serializes lists of lists and lists of lists of lists"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "SdkJsonProtocol.SdkCollections" }
        body: """
            {
                "nestedStringList": [["foo", "bar"], ["baz"]],
                "deeplyNestedStringList": [[["foo", "bar"], ["baz"]], [["abcd"]]]
            }"""
        bodyMediaType: "application/json"
        params: {
            nestedStringList: [["foo", "bar"], ["baz"]]
            deeplyNestedStringList: [[["foo", "bar"], ["baz"]], [["abcd"]]]
        }
    }
    {
        id: "SdkAwsJson11ListOfEnumsWithNull"
        documentation: "Serializes a null element of an enum list as JSON null"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "SdkJsonProtocol.SdkCollections" }
        body: "{\"sparseEnumList\": [\"EnumValue1\", null, \"EnumValue2\"]}"
        bodyMediaType: "application/json"
        params: {
            sparseEnumList: ["EnumValue1", null, "EnumValue2"]
        }
    }
])

apply SdkCollections @httpResponseTests([
    {
        id: "SdkAwsJson11ListsWithNullAndEmptyElements"
        documentation: "Keeps null and empty structure and map elements of lists"
        protocol: awsJson1_1
        code: 200
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        body: """
            {
                "sparseStructList": [{ "stringMember": "foo" }, null, {}],
                "sparseMapList": [{ "keyOne": "valOne", "keyTwo": "valTwo" }, null, {}]
            }"""
        bodyMediaType: "application/json"
        params: {
            sparseStructList: [{ stringMember: "foo" }, null, {}]
            sparseMapList: [{ keyOne: "valOne", keyTwo: "valTwo" }, null, {}]
        }
    }
])

structure SdkCollectionsInputOutput {
    nestedStringList: ListOfListOfStrings
    deeplyNestedStringList: ListOfListOfListOfStrings
    sparseEnumList: SparseEnumList
    sparseStructList: SparseStructList
    sparseMapList: SparseMapList
}

list StringList {
    member: String
}

list ListOfListOfStrings {
    member: StringList
}

list ListOfListOfListOfStrings {
    member: ListOfListOfStrings
}

enum SdkEnum {
    ENUM_VALUE_1 = "EnumValue1"
    ENUM_VALUE_2 = "EnumValue2"
}

@sparse
list SparseEnumList {
    member: SdkEnum
}

structure SimpleStruct {
    stringMember: String
}

@sparse
list SparseStructList {
    member: SimpleStruct
}

map StringMap {
    key: String
    value: String
}

@sparse
list SparseMapList {
    member: StringMap
}

/// Document values.
operation SdkDocuments {
    input: SdkDocumentsInputOutput
    output: SdkDocumentsInputOutput
}

apply SdkDocuments @httpRequestTests([
    {
        id: "SdkAwsJson11DocumentScalarsAndArrays"
        documentation: "Serializes a string document and an array document that holds a null element"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "SdkJsonProtocol.SdkDocuments" }
        body: """
            {
                "stringDocument": "JustAString",
                "arrayDocument": [false, "One", 2, {"key": "Three"}, null]
            }"""
        bodyMediaType: "application/json"
        params: {
            stringDocument: "JustAString"
            arrayDocument: [false, "One", 2, { key: "Three" }, null]
        }
    }
])

apply SdkDocuments @httpResponseTests([
    {
        id: "SdkAwsJson11DocumentScalarsAndArraysResponse"
        documentation: "Parses a string document and an array document"
        protocol: awsJson1_1
        code: 200
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        body: """
            {
                "stringDocument": "JustAString",
                "arrayDocument": [false, "One", 2, {"key": "Three"}]
            }"""
        bodyMediaType: "application/json"
        params: {
            stringDocument: "JustAString"
            arrayDocument: [false, "One", 2, { key: "Three" }]
        }
    }
    {
        id: "SdkAwsJson11NullDocumentResponse"
        documentation: "Parses a document member whose JSON value is null"
        protocol: awsJson1_1
        code: 200
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        body: "{\"documentValue\": null}"
        bodyMediaType: "application/json"
        params: {
            documentValue: null
        }
    }
])

structure SdkDocumentsInputOutput {
    stringDocument: Document
    arrayDocument: Document
    documentValue: Document
}

/// An idempotency token member.
operation SdkIdempotentOperation {
    input: SdkIdempotentOperationInput
}

apply SdkIdempotentOperation @httpRequestTests([
    {
        id: "SdkAwsJson11IdempotencyTokenAutoFill"
        documentation: "Fills an unset idempotency token member"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        headers: {
            "Content-Type": "application/x-amz-json-1.1"
            "X-Amz-Target": "SdkJsonProtocol.SdkIdempotentOperation"
        }
        body: "{\"token\": \"00000000-0000-4000-8000-000000000000\"}"
        bodyMediaType: "application/json"
        params: {}
    }
    {
        id: "SdkAwsJson11IdempotencyTokenAutoFillIsSet"
        documentation: "Sends a set idempotency token as-is"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        headers: {
            "Content-Type": "application/x-amz-json-1.1"
            "X-Amz-Target": "SdkJsonProtocol.SdkIdempotentOperation"
        }
        body: "{\"token\": \"abcd\"}"
        bodyMediaType: "application/json"
        params: {
            token: "abcd"
        }
    }
])

@input
structure SdkIdempotentOperationInput {
    @idempotencyToken
    token: String
}

/// An operation with no input or output.
operation SdkNoOutput {}

apply SdkNoOutput @httpResponseTests([
    {
        id: "SdkAwsJson11NoOutputIgnoresNonJsonBody"
        documentation: "Ignores a body that is not JSON for an operation with no output"
        protocol: awsJson1_1
        code: 200
        body: "THIS ISN'T JSON!!!"
        bodyMediaType: "text/plain"
    }
])
