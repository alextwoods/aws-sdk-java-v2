// SDK-owned restXml protocol tests for SDK serialization behavior that the upstream Smithy protocol tests do not
// cover, ported from the legacy rest-core and rest-xml suites (task 1.34, design D.1, Requirement 32.10).
// legacy-disposition.csv maps each legacy case to its test case id.
$version: "2.0"

namespace software.amazon.awssdk.protocoltests.restxml

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restXml
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@service(sdkId: "Sdk Rest Xml Protocol")
@sigv4(name: "sdkrestxml")
@restXml
@title("SDK Rest Xml Protocol Test Service")
service SdkRestXml {
    version: "2016-03-11"
    operations: [
        SdkTimestamps
        SdkXmlBody
        SdkHeaders
        SdkModeledContentType
        SdkQuery
        SdkGreedyLabel
        SdkNoOutput
        SdkBlobPayload
        SdkPrefixHeaders
        SdkEnums
    ]
}

/// Timestamps in the payload and the query string.
@http(uri: "/SdkTimestamps", method: "PUT")
@idempotent
operation SdkTimestamps {
    input: SdkTimestampsRequest
}

apply SdkTimestamps @httpRequestTests([
    {
        id: "SdkRestXmlTimestampMillisecondPrecision"
        documentation: "Serializes date-time timestamps with millisecond precision in the payload, nested, and in the query string"
        protocol: restXml
        method: "PUT"
        uri: "/SdkTimestamps"
        queryParams: ["Timestamp=2015-01-25T08%3A00%3A00.123Z"]
        headers: { "Content-Type": "application/xml" }
        body: """
            <SdkTimestampsRequest>
                <timestampMember>2015-01-25T08:00:00.123Z</timestampMember>
                <nested>
                    <timestampMember>2015-01-25T08:00:00.123Z</timestampMember>
                </nested>
            </SdkTimestampsRequest>
            """
        bodyMediaType: "application/xml"
        params: {
            timestampMember: 1422172800.123
            nested: { timestampMember: 1422172800.123 }
            queryMember: 1422172800.123
        }
    }
])

@input
structure SdkTimestampsRequest {
    timestampMember: Timestamp

    nested: TimestampHolder

    @httpQuery("Timestamp")
    queryMember: Timestamp
}

structure TimestampHolder {
    timestampMember: Timestamp
}

/// Payload members.
@http(uri: "/SdkXmlBody", method: "PUT")
@idempotent
operation SdkXmlBody {
    input: SdkXmlBodyRequest
    output: SdkXmlBodyResponse
}

apply SdkXmlBody @httpRequestTests([
    {
        id: "SdkRestXmlEscapedCharacters"
        documentation: "Escapes quotes, apostrophes, angle brackets, CR and LF in element text"
        protocol: restXml
        method: "PUT"
        uri: "/SdkXmlBody"
        headers: { "Content-Type": "application/xml" }
        body: "<SdkXmlBodyRequest><stringMember>&quot;&apos;&lt;&#x0D;&#x0A;NormalCharacters&quot;&apos;&gt;&#x0D;&#x0A;</stringMember></SdkXmlBodyRequest>"
        bodyMediaType: "application/xml"
        params: {
            stringMember: "\"'<\r\nNormalCharacters\"'>\r\n"
        }
    }
    {
        id: "SdkRestXmlEmptyStructure"
        documentation: "Serializes a structure with no members set as an empty element"
        protocol: restXml
        method: "PUT"
        uri: "/SdkXmlBody"
        headers: { "Content-Type": "application/xml" }
        body: "<SdkXmlBodyRequest><simpleStruct/></SdkXmlBodyRequest>"
        bodyMediaType: "application/xml"
        params: {
            simpleStruct: {}
        }
    }
    {
        id: "SdkRestXmlNoParamsBody"
        documentation: "Sends an empty root element when no payload member is set"
        protocol: restXml
        method: "PUT"
        uri: "/SdkXmlBody"
        headers: { "Content-Type": "application/xml" }
        body: "<SdkXmlBodyRequest></SdkXmlBodyRequest>"
        bodyMediaType: "application/xml"
        params: {}
    }
    {
        id: "SdkRestXmlNoTargetHeader"
        documentation: "Does not send X-Amz-Target, which only the JSON RPC protocols use"
        protocol: restXml
        method: "PUT"
        uri: "/SdkXmlBody"
        forbidHeaders: ["X-Amz-Target"]
        params: {}
    }
])

apply SdkXmlBody @httpResponseTests([
    {
        id: "SdkRestXmlNoContentWithOutput"
        documentation: "Unmarshalls a 204 No Content response with no body as an output with no members set"
        protocol: restXml
        code: 204
        body: ""
        params: {}
    }
    {
        id: "SdkRestXmlSingleElementFlattenedList"
        documentation: "Parses a flattened list that holds exactly one element"
        protocol: restXml
        code: 200
        headers: { "Content-Type": "application/xml" }
        body: """
            <SdkXmlBodyResponse>
                <flattenedStringList>val1</flattenedStringList>
            </SdkXmlBodyResponse>
            """
        bodyMediaType: "application/xml"
        params: {
            flattenedStringList: ["val1"]
        }
    }
])

@input
structure SdkXmlBodyRequest {
    stringMember: String
    simpleStruct: SimpleStruct
}

@output
structure SdkXmlBodyResponse {
    stringMember: String

    @xmlFlattened
    flattenedStringList: StringList
}

structure SimpleStruct {
    stringMember: String
}

/// List header members with null and empty elements.
@http(uri: "/SdkHeaders", method: "POST")
operation SdkHeaders {
    input: SdkHeadersRequest
}

apply SdkHeaders @httpRequestTests([
    {
        id: "SdkRestXmlHeaderListSkipsNullAndEmptyElements"
        documentation: "Leaves null and empty string elements of a list header member out of the header"
        protocol: restXml
        method: "POST"
        uri: "/SdkHeaders"
        headers: { "x-amz-string-list": "listValue1, listValue6" }
        body: ""
        params: {
            stringList: ["listValue1", null, "", "", "listValue6"]
        }
    }
    {
        id: "SdkRestXmlHeaderListWithOnlyNullIsOmitted"
        documentation: "Sends no header for a list header member that holds only null"
        protocol: restXml
        method: "POST"
        uri: "/SdkHeaders"
        forbidHeaders: ["x-amz-string-list"]
        body: ""
        params: {
            stringList: [null]
        }
    }
])

@input
structure SdkHeadersRequest {
    @httpHeader("x-amz-string-list")
    stringList: SparseStringList
}

@sparse
list SparseStringList {
    member: String
}

/// A Content-Type header member with no payload.
@http(uri: "/SdkModeledContentType", method: "POST")
operation SdkModeledContentType {
    input: SdkModeledContentTypeRequest
}

apply SdkModeledContentType @httpRequestTests([
    {
        id: "SdkRestXmlModeledContentTypeHeader"
        documentation: "Sends the Content-Type header member instead of the protocol default"
        protocol: restXml
        method: "POST"
        uri: "/SdkModeledContentType"
        headers: { "Content-Type": "application/foo" }
        params: {
            contentType: "application/foo"
        }
    }
])

@input
structure SdkModeledContentTypeRequest {
    @httpHeader("Content-Type")
    contentType: String
}

/// A list query member on a POST operation with no payload members.
@http(uri: "/SdkQuery", method: "POST")
operation SdkQuery {
    input: SdkQueryRequest
}

apply SdkQuery @httpRequestTests([
    {
        id: "SdkRestXmlOmitsEmptyListQueryValues"
        documentation: "Does not send an empty query list"
        protocol: restXml
        method: "POST"
        uri: "/SdkQuery"
        forbidQueryParams: ["item"]
        body: ""
        params: {
            items: []
        }
    }
    {
        id: "SdkRestXmlQueryListWithEmptyString"
        documentation: "Sends a query list holding one empty string as a parameter with an empty value"
        protocol: restXml
        method: "POST"
        uri: "/SdkQuery"
        queryParams: ["item="]
        body: ""
        params: {
            items: [""]
        }
    }
    {
        id: "SdkRestXmlNoContentTypeWithoutBody"
        documentation: "Sends no Content-Type for a POST operation whose input has no payload members"
        protocol: restXml
        method: "POST"
        uri: "/SdkQuery"
        forbidHeaders: ["Content-Type"]
        body: ""
        params: {}
    }
])

@input
structure SdkQueryRequest {
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
    input: SdkGreedyLabelRequest
}

apply SdkGreedyLabel @httpRequestTests([
    {
        id: "SdkRestXmlGreedyLabelLeadingSlashPreserved"
        documentation: "Keeps the leading slash of a greedy label value"
        protocol: restXml
        method: "GET"
        uri: "/SdkGreedyLabel/pathParamValue//foo/bar/baz"
        body: ""
        params: {
            prefix: "pathParamValue"
            suffix: "/foo/bar/baz"
        }
    }
])

@input
structure SdkGreedyLabelRequest {
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
        id: "SdkRestXmlNoContentWithNoOutput"
        documentation: "Accepts a 204 No Content response for an operation with no output"
        protocol: restXml
        code: 204
        body: ""
    }
])

/// A blob payload in the output.
@http(uri: "/SdkBlobPayload", method: "POST")
operation SdkBlobPayload {
    output: SdkBlobPayloadResponse
}

apply SdkBlobPayload @httpResponseTests([
    {
        id: "SdkRestXmlNoContentWithBlobPayload"
        documentation: "Unmarshalls an empty 204 No Content body as an empty blob payload"
        protocol: restXml
        code: 204
        body: ""
        params: {
            data: ""
        }
    }
])

@output
structure SdkBlobPayloadResponse {
    @httpPayload
    data: Blob
}

/// Prefix headers in the output.
@http(uri: "/SdkPrefixHeaders", method: "GET")
@readonly
operation SdkPrefixHeaders {
    output: SdkPrefixHeadersResponse
}

apply SdkPrefixHeaders @httpResponseTests([
    {
        id: "SdkRestXmlPrefixHeadersCaseInsensitive"
        documentation: "Matches the header prefix case-insensitively and keeps the case of the rest of the header name"
        protocol: restXml
        code: 200
        headers: { "x-amz-meta-FoO": "foo", "X-aMZ-mEtA-bAr": "bar" }
        body: ""
        params: {
            metadata: { FoO: "foo", bAr: "bar" }
        }
    }
])

@output
structure SdkPrefixHeadersResponse {
    @httpPrefixHeaders("x-amz-meta-")
    metadata: StringMap
}

map StringMap {
    key: String
    value: String
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

enum SdkEnum {
    ENUM_VALUE_1 = "EnumValue1"
    ENUM_VALUE_2 = "EnumValue2"
}
