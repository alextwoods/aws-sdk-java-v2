$version: "2.0"

namespace demo.streaming

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@service(sdkId: "StreamDemo")
@sigv4(name: "streamdemo")
@restJson1
service StreamDemo {
    version: "2024-01-01"
    operations: [UploadThing, DownloadThing]
}

@http(method: "PUT", uri: "/upload")
@httpRequestTests([
    {
        id: "UploadThingStream"
        protocol: restJson1
        method: "PUT"
        uri: "/upload"
        headers: { "X-Label": "l" }
        body: "payload"
        bodyMediaType: "application/octet-stream"
        params: { Body: "payload", Label: "l" }
    }
])
operation UploadThing {
    input: UploadThingInput
}

@input
structure UploadThingInput {
    @httpHeader("X-Label")
    Label: String

    @httpPayload
    @default("")
    Body: StreamingBlob
}

@http(method: "GET", uri: "/download")
@httpResponseTests([
    {
        id: "DownloadThingStream"
        protocol: restJson1
        code: 200
        headers: { "X-Label": "l" }
        body: "payload"
        bodyMediaType: "application/octet-stream"
        params: { Body: "payload", Label: "l" }
    }
])
operation DownloadThing {
    output: DownloadThingOutput
}

@output
structure DownloadThingOutput {
    @httpHeader("X-Label")
    Label: String

    @httpPayload
    @default("")
    Body: StreamingBlob
}

@streaming
blob StreamingBlob
