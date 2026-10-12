// SDK-owned restJson1 protocol tests ported from the legacy rest-json-contenttype.json suite (task 1.34, design D.1,
// Requirement 32.10). Only cases that aws.protocoltests.restjson#RestJson does not already cover are here; the others
// are recorded as equivalent in legacy-disposition.csv.
$version: "2.0"

namespace software.amazon.awssdk.protocoltests

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1
use smithy.test#httpRequestTests

@service(sdkId: "Rest Json Content Type")
@sigv4(name: "restjsoncontenttype")
@restJson1
@title("SDK Rest Json Content Type Protocol Service")
service RestJsonContentType {
    version: "2021-05-13"
    operations: [
        TestBody
        TestPayload
        TestBlobPayload
        TestStringPayload
    ]
}

/// Body members next to a header member.
@http(uri: "/body", method: "POST")
operation TestBody {
    input: TestBodyInput
}

apply TestBody @httpRequestTests([
    {
        id: "RestJsonContentTypeBodyWithHeader"
        documentation: "Serializes body members as a JSON document and binds a header member"
        protocol: restJson1
        method: "POST"
        uri: "/body"
        headers: { "Content-Type": "application/json", "x-amz-test-id": "t-12345" }
        body: "{\"testConfig\": {\"timeout\": 10}}"
        bodyMediaType: "application/json"
        params: {
            testConfig: { timeout: 10 }
            testId: "t-12345"
        }
    }
])

@input
structure TestBodyInput {
    testConfig: TestConfig

    @httpHeader("x-amz-test-id")
    testId: String
}

structure TestConfig {
    timeout: Integer
}

/// A structure payload next to a header member.
@http(uri: "/payload", method: "POST")
operation TestPayload {
    input: TestPayloadInput
}

apply TestPayload @httpRequestTests([
    {
        id: "RestJsonContentTypePayloadWithHeader"
        documentation: "Serializes a structure payload unwrapped and binds a header member"
        protocol: restJson1
        method: "POST"
        uri: "/payload"
        headers: { "Content-Type": "application/json", "x-amz-test-id": "t-12345" }
        body: "{\"data\": 25}"
        bodyMediaType: "application/json"
        params: {
            payloadConfig: { data: 25 }
            testId: "t-12345"
        }
    }
])

@input
structure TestPayloadInput {
    @httpPayload
    payloadConfig: PayloadConfig

    @httpHeader("x-amz-test-id")
    testId: String
}

structure PayloadConfig {
    data: Integer
}

/// A blob payload with a Content-Type header member.
@http(uri: "/blob-payload", method: "POST")
operation TestBlobPayload {
    input: TestBlobPayloadInput
}

apply TestBlobPayload @httpRequestTests([
    {
        id: "RestJsonContentTypeBlobPayloadNoParams"
        documentation: "Sends no body and no Content-Type when the blob payload and the Content-Type member are unset"
        protocol: restJson1
        method: "POST"
        uri: "/blob-payload"
        body: ""
        forbidHeaders: ["Content-Type"]
        params: {}
    }
])

@input
structure TestBlobPayloadInput {
    @httpPayload
    data: Blob

    @httpHeader("Content-Type")
    contentType: String
}

/// A string payload with a Content-Type header member.
@http(uri: "/string-payload", method: "POST")
operation TestStringPayload {
    input: TestStringPayloadInput
}

apply TestStringPayload @httpRequestTests([
    {
        id: "RestJsonContentTypeStringPayload"
        documentation: "Sends a string payload as-is with the Content-Type from the header member"
        protocol: restJson1
        method: "POST"
        uri: "/string-payload"
        headers: { "Content-Type": "text/plain" }
        body: "1234"
        bodyMediaType: "text/plain"
        params: {
            data: "1234"
            contentType: "text/plain"
        }
    }
    {
        id: "RestJsonContentTypeStringPayloadNoParams"
        documentation: "Sends no body and no Content-Type when the string payload and the Content-Type member are unset"
        protocol: restJson1
        method: "POST"
        uri: "/string-payload"
        body: ""
        forbidHeaders: ["Content-Type"]
        params: {}
    }
])

@input
structure TestStringPayloadInput {
    @httpPayload
    data: String

    @httpHeader("Content-Type")
    contentType: String
}
