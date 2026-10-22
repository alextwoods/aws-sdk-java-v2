$version: "2.0"

namespace test.streaming

use aws.api#service
use aws.auth#sigv4
use aws.auth#unsignedPayload
use aws.protocols#httpChecksum
use aws.protocols#restJson1

/// A restJson1 and sigv4 service with streaming blob inputs and outputs, and event stream inputs and outputs.
@service(sdkId: "Archives")
@sigv4(name: "archives")
@restJson1
service ArchiveService {
    version: "2024-01-01"
    operations: [
        UploadArchive
        UploadUnsigned
        UploadOptional
        UploadChecked
        DownloadArchive
        PublishEvents
        WatchEvents
        Describe
    ]
}

/// A streaming upload with a flexible checksum.
@httpChecksum(requestAlgorithmMember: "algorithm", requestChecksumRequired: true)
@http(method: "PUT", uri: "/checked")
operation UploadChecked {
    input := {
        @httpHeader("x-amz-sdk-checksum-algorithm")
        algorithm: ChecksumAlgorithm

        @required
        @httpPayload
        body: Payload
    }
}

enum ChecksumAlgorithm {
    CRC32
    SHA256
}

@idempotent
@http(method: "PUT", uri: "/archives/{name}")
operation UploadArchive {
    input := {
        @required
        @httpLabel
        name: String

        @httpHeader("X-Tag")
        tag: String

        @required
        @httpPayload
        body: Payload
    }
    output := {
        @httpHeader("X-Version")
        version: Integer
    }
}

/// Sent with chunked transfer encoding when the body has no length.
@unsignedPayload
@http(method: "POST", uri: "/unsigned")
operation UploadUnsigned {
    input := {
        @required
        @httpPayload
        body: Payload
    }
}

@http(method: "POST", uri: "/optional")
operation UploadOptional {
    input := {
        @httpPayload
        @default("")
        body: Payload
    }
}

@readonly
@http(method: "GET", uri: "/archives/{name}")
operation DownloadArchive {
    input := {
        @required
        @httpLabel
        name: String
    }
    output := {
        @httpHeader("X-Version")
        version: Integer

        @required
        @httpPayload
        body: Payload
    }
}

@http(method: "POST", uri: "/events")
operation PublishEvents {
    input := {
        @httpHeader("X-Channel")
        channel: String

        @required
        @httpPayload
        events: Events
    }
}

@readonly
@http(method: "GET", uri: "/events")
operation WatchEvents {
    output := {
        @httpPayload
        events: Events
    }
}

/// An operation without streaming members.
@readonly
@http(method: "GET", uri: "/describe")
operation Describe {
    output := {
        name: String
    }
}

@streaming
blob Payload

@streaming
union Events {
    message: MessageEvent
}

structure MessageEvent {
    text: String
}
