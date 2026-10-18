// SDK-owned restJson1 protocol tests for SDK serialization behavior that the upstream Smithy protocol tests do not
// cover, ported from the legacy json-core, rest-core and rest-json suites (task 1.34, design D.1, Requirement 32.10).
// legacy-disposition.csv maps each legacy case to its test case id.
$version: "2.0"

namespace software.amazon.awssdk.protocoltests.restjson

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@service(sdkId: "Sdk Rest Json Protocol")
@sigv4(name: "sdkrestjson")
@restJson1
@title("SDK Rest Json Protocol Test Service")
service SdkRestJson {
    version: "2016-03-11"
    operations: [
        SdkTimestamps
        SdkCollections
        SdkDocuments
        SdkHeaders
        SdkQuery
        SdkGreedyLabel
        SdkNoOutput
        SdkBlobPayload
        SdkMultiLocation
        SdkMixedLocation
        SdkEnums
    ]
}

/// Timestamps in the body and the query string.
@http(uri: "/SdkTimestamps", method: "POST")
operation SdkTimestamps {
    input: SdkTimestampsInput
    output: SdkTimestampsOutput
}

apply SdkTimestamps @httpRequestTests([
    {
        id: "SdkRestJsonTimestampMillisecondPrecision"
        documentation: """
            Serializes timestamps with millisecond precision: epoch seconds in the body (top level, list and map
            values), date-time in the body and in the query string."""
        protocol: restJson1
        method: "POST"
        uri: "/SdkTimestamps"
        queryParams: ["Timestamp=2015-01-25T08%3A00%3A00.123Z"]
        headers: { "Content-Type": "application/json" }
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
            queryMember: 1422172800.123
        }
    }
])

apply SdkTimestamps @httpResponseTests([
    {
        id: "SdkRestJsonFractionalEpochSecondsResponse"
        documentation: "Parses epoch-seconds timestamps with fractional seconds at the top level, nested, and in lists and maps"
        protocol: restJson1
        code: 200
        headers: { "Content-Type": "application/json" }
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

@input
structure SdkTimestampsInput {
    epochMember: Timestamp

    @timestampFormat("date-time")
    dateTimeMember: Timestamp

    epochList: TimestampList

    epochMap: TimestampMap

    @httpQuery("Timestamp")
    queryMember: Timestamp
}

@output
structure SdkTimestampsOutput {
    epochMember: Timestamp

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

/// Collections with null and empty elements, and a string member.
@http(uri: "/SdkCollections", method: "POST")
operation SdkCollections {
    input: SdkCollectionsInputOutput
    output: SdkCollectionsInputOutput
}

apply SdkCollections @httpRequestTests([
    {
        id: "SdkRestJsonEmptyMap"
        documentation: "Serializes an empty map as an empty JSON object"
        protocol: restJson1
        method: "POST"
        uri: "/SdkCollections"
        headers: { "Content-Type": "application/json" }
        body: "{\"stringMap\": {}}"
        bodyMediaType: "application/json"
        params: {
            stringMap: {}
        }
    }
    {
        id: "SdkRestJsonListOfEnumsWithNull"
        documentation: "Serializes a null element of an enum list as JSON null"
        protocol: restJson1
        method: "POST"
        uri: "/SdkCollections"
        headers: { "Content-Type": "application/json" }
        body: "{\"sparseEnumList\": [\"EnumValue1\", null, \"EnumValue2\"]}"
        bodyMediaType: "application/json"
        params: {
            sparseEnumList: ["EnumValue1", null, "EnumValue2"]
        }
    }
])

apply SdkCollections @httpResponseTests([
    {
        id: "SdkRestJsonListsWithNullAndEmptyElements"
        documentation: "Keeps null and empty structure and map elements of lists"
        protocol: restJson1
        code: 200
        headers: { "Content-Type": "application/json" }
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
    {
        id: "SdkRestJsonNumberIntoStringMember"
        documentation: """
            Reads a JSON number sent for a string member as the literal text of the number. The value has more
            precision than a double, so the test fails if the number is parsed and formatted again."""
        protocol: restJson1
        code: 200
        headers: { "Content-Type": "application/json" }
        body: "{\"stringValue\": 123456789012345678901.000000000000000001}"
        bodyMediaType: "application/json"
        params: {
            stringValue: "123456789012345678901.000000000000000001"
        }
    }
    {
        id: "SdkRestJsonIgnoresUnknownMembers"
        documentation: "Ignores JSON members the output structure does not define"
        protocol: restJson1
        code: 200
        headers: { "Content-Type": "application/json" }
        body: "{\"foo\": \"bar\"}"
        bodyMediaType: "application/json"
        params: {}
    }
    {
        id: "SdkRestJsonNoContentWithOutput"
        documentation: "Unmarshalls a 204 No Content response with no body as an output with no members set"
        protocol: restJson1
        code: 204
        body: ""
        params: {}
    }
])

structure SdkCollectionsInputOutput {
    stringValue: String
    stringMap: StringMap
    sparseEnumList: SparseEnumList
    sparseStructList: SparseStructList
    sparseMapList: SparseMapList
}

enum SdkEnum {
    ENUM_VALUE_1 = "EnumValue1"
    ENUM_VALUE_2 = "EnumValue2"
}

@sparse
list SparseEnumList {
    member: SdkEnum
}

map StringMap {
    key: String
    value: String
}

structure SimpleStruct {
    stringMember: String
}

@sparse
list SparseStructList {
    member: SimpleStruct
}

@sparse
list SparseMapList {
    member: StringMap
}

/// Document values.
@http(uri: "/SdkDocuments", method: "POST")
operation SdkDocuments {
    input: SdkDocumentsInput
    output: SdkDocumentsOutput
}

apply SdkDocuments @httpRequestTests([
    {
        id: "SdkRestJsonDocumentArrayWithNull"
        documentation: "Serializes a document array that holds a null element"
        protocol: restJson1
        method: "POST"
        uri: "/SdkDocuments"
        headers: { "Content-Type": "application/json" }
        body: "{\"documentValue\": [false, \"One\", 2, {\"key\": \"Three\"}, null]}"
        bodyMediaType: "application/json"
        params: {
            documentValue: [false, "One", 2, { key: "Three" }, null]
        }
    }
])

@input
structure SdkDocumentsInput {
    documentValue: Document
}

/// Read by the hand-written NullDocumentProtocolTest (legacy-disposition.csv row json-core-output.json#35).
@output
structure SdkDocumentsOutput {
    documentValue: Document
}

/// List header members with null and empty elements.
@http(uri: "/SdkHeaders", method: "POST")
operation SdkHeaders {
    input: SdkHeadersInput
}

apply SdkHeaders @httpRequestTests([
    {
        id: "SdkRestJsonHeaderListSkipsNullAndEmptyElements"
        documentation: "Leaves null and empty string elements of a list header member out of the header"
        protocol: restJson1
        method: "POST"
        uri: "/SdkHeaders"
        headers: { "x-amz-string-list": "listValue1, listValue6" }
        body: ""
        params: {
            stringList: ["listValue1", null, "", "", "listValue6"]
        }
    }
    {
        id: "SdkRestJsonHeaderListWithOnlyNullIsOmitted"
        documentation: "Sends no header for a list header member that holds only null"
        protocol: restJson1
        method: "POST"
        uri: "/SdkHeaders"
        forbidHeaders: ["x-amz-string-list"]
        body: ""
        params: {
            stringList: [null]
        }
    }
    {
        id: "SdkRestJsonNoTargetHeader"
        documentation: "Does not send X-Amz-Target, which only the JSON RPC protocols use"
        protocol: restJson1
        method: "POST"
        uri: "/SdkHeaders"
        forbidHeaders: ["X-Amz-Target"]
        body: ""
        params: {}
    }
])

@input
structure SdkHeadersInput {
    @httpHeader("x-amz-string-list")
    stringList: SparseStringList
}

@sparse
list SparseStringList {
    member: String
}

/// A list query member.
@http(uri: "/SdkQuery", method: "GET")
@readonly
operation SdkQuery {
    input: SdkQueryInput
}

apply SdkQuery @httpRequestTests([
    {
        id: "SdkRestJsonQueryListWithEmptyString"
        documentation: "Sends a query list holding one empty string as a parameter with an empty value"
        protocol: restJson1
        method: "GET"
        uri: "/SdkQuery"
        queryParams: ["item="]
        body: ""
        params: {
            items: [""]
        }
    }
])

@input
structure SdkQueryInput {
    @httpQuery("item")
    items: StringList
}

list StringList {
    member: String
}

/// A greedy label.
@http(uri: "/SdkGreedyLabel/{prefix}/{suffix+}", method: "GET")
@readonly
operation SdkGreedyLabel {
    input: SdkGreedyLabelInput
}

apply SdkGreedyLabel @httpRequestTests([
    {
        id: "SdkRestJsonGreedyLabelLeadingSlashRemoved"
        documentation: "Removes the leading slash of a greedy label value"
        protocol: restJson1
        method: "GET"
        uri: "/SdkGreedyLabel/pathParamValue/foo/bar/baz"
        body: ""
        params: {
            prefix: "pathParamValue"
            suffix: "/foo/bar/baz"
        }
    }
])

@input
structure SdkGreedyLabelInput {
    @required
    @httpLabel
    prefix: String

    @required
    @httpLabel
    suffix: String
}

/// An operation with no input or output.
@http(uri: "/SdkNoOutput", method: "DELETE")
@idempotent
operation SdkNoOutput {}

apply SdkNoOutput @httpResponseTests([
    {
        id: "SdkRestJsonNoContentWithNoOutput"
        documentation: "Accepts a 204 No Content response for an operation with no output"
        protocol: restJson1
        code: 204
        body: ""
    }
    {
        id: "SdkRestJsonNoOutputIgnoresNonJsonBody"
        documentation: "Ignores a body that is not JSON for an operation with no output"
        protocol: restJson1
        code: 200
        body: "THIS ISN'T JSON!!!"
        bodyMediaType: "text/plain"
    }
])

/// A blob payload in the output.
@http(uri: "/SdkBlobPayload", method: "POST")
operation SdkBlobPayload {
    output: SdkBlobPayloadOutput
}

apply SdkBlobPayload @httpResponseTests([
    {
        id: "SdkRestJsonNoContentWithBlobPayload"
        documentation: "Unmarshalls an empty 204 No Content body as an empty blob payload"
        protocol: restJson1
        code: 204
        body: ""
        params: {
            data: ""
        }
    }
])

@output
structure SdkBlobPayloadOutput {
    @httpPayload
    data: Blob
}

/// Members bound to a URI label, query parameters, a header and the body of one request.
@http(uri: "/SdkMultiLocation/{pathParam}", method: "POST")
operation SdkMultiLocation {
    input: SdkMultiLocationInput
}

apply SdkMultiLocation @httpRequestTests([
    {
        id: "SdkRestJsonMultiLocationRequest"
        documentation: "Serializes a URI label, two query parameters, a header and a body structure in one request"
        protocol: restJson1
        method: "POST"
        uri: "/SdkMultiLocation/pathParamValue"
        queryParams: ["QueryParamOne=valueOne", "QueryParamTwo=valueTwo"]
        headers: { "x-amz-header-string": "headerVal" }
        body: """
            {
                "payloadStructParam": {
                    "payloadMemberOne": "payloadValueOne",
                    "payloadMemberTwo": "payloadValueTwo"
                }
            }"""
        bodyMediaType: "application/json"
        params: {
            pathParam: "pathParamValue"
            queryParamOne: "valueOne"
            queryParamTwo: "valueTwo"
            stringHeaderMember: "headerVal"
            payloadStructParam: { payloadMemberOne: "payloadValueOne", payloadMemberTwo: "payloadValueTwo" }
        }
    }
])

@input
structure SdkMultiLocationInput {
    @required
    @httpLabel
    pathParam: String

    @httpQuery("QueryParamOne")
    queryParamOne: String

    @httpQuery("QueryParamTwo")
    queryParamTwo: String

    @httpHeader("x-amz-header-string")
    stringHeaderMember: String

    payloadStructParam: PayloadStruct
}

structure PayloadStruct {
    payloadMemberOne: String
    payloadMemberTwo: String
}

/// The status code, a header and body members of one response.
@http(uri: "/SdkMixedLocation", method: "POST")
operation SdkMixedLocation {
    output: SdkMixedLocationOutput
}

apply SdkMixedLocation @httpResponseTests([
    {
        id: "SdkRestJsonMixedLocationResponse"
        documentation: "Parses the status code, a header and body members of one response"
        protocol: restJson1
        code: 201
        headers: { "x-amz-string": "header member value" }
        body: "{\"stringMember\": \"string member value\", \"integerMember\": 123}"
        bodyMediaType: "application/json"
        params: {
            statusCodeMember: 201
            stringHeaderMember: "header member value"
            stringMember: "string member value"
            integerMember: 123
        }
    }
])

@output
structure SdkMixedLocationOutput {
    @httpResponseCode
    statusCodeMember: Integer

    @httpHeader("x-amz-string")
    stringHeaderMember: String

    stringMember: String

    integerMember: Integer
}

/// Enum values in a member, a list element, a map key and a map value. Read by the hand-written
/// UnknownEnumValueProtocolTest (legacy-disposition.csv rows json-core-output.json#5, #7, #9 and #10).
@http(uri: "/SdkEnums", method: "POST")
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
