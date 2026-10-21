$version: "2.0"

namespace test.features

use aws.api#service
use aws.auth#sigv4
use aws.protocols#httpChecksum
use aws.protocols#restJson1

/// A restJson1 and sigv4 service whose operations use the runtime features that read operation and member metadata:
/// host prefixes, idempotency tokens, request compression, checksums and pagination. The service's paginated trait
/// gives the tokens and page size that the paginated operations inherit.
@service(sdkId: "Pipeline Features", arnNamespace: "features")
@sigv4(name: "features")
@restJson1
@paginated(inputToken: "nextToken", outputToken: "nextToken", pageSize: "maxResults")
service FeatureService {
    version: "2024-01-01"
    operations: [
        PutLabeled
        PutStatic
        CreateThing
        UploadLogs
        PutChecksummed
        PutChecksumRequired
        ListThings
        ListNested
        Ping
    ]
}

@endpoint(hostPrefix: "{tenant}-data.")
@http(method: "POST", uri: "/labeled")
operation PutLabeled {
    input := {
        @required
        @hostLabel
        tenant: String

        value: String
    }
}

@endpoint(hostPrefix: "static.")
@http(method: "POST", uri: "/static")
operation PutStatic {
    input := {
        value: String
    }
}

@http(method: "POST", uri: "/things")
operation CreateThing {
    input := {
        @idempotencyToken
        clientToken: String

        name: String
    }
}

@requestCompression(encodings: ["gzip"])
@http(method: "POST", uri: "/logs")
operation UploadLogs {
    input := {
        data: String
    }
}

@httpChecksum(
    requestChecksumRequired: true
    requestAlgorithmMember: "checksumAlgorithm"
    requestValidationModeMember: "checksumMode"
    responseAlgorithms: ["SHA256", "CRC32", "CRC32C"]
)
@idempotent
@http(method: "PUT", uri: "/checksummed")
operation PutChecksummed {
    input := {
        @httpHeader("x-amz-sdk-checksum-algorithm")
        checksumAlgorithm: ChecksumAlgorithm

        @httpHeader("x-amz-checksum-mode")
        checksumMode: ChecksumMode

        content: String
    }
    output := {
        content: String
    }
}

@httpChecksumRequired
@http(method: "POST", uri: "/required")
operation PutChecksumRequired {
    input := {
        content: String
    }
}

@readonly
@paginated(items: "things")
@http(method: "GET", uri: "/things")
operation ListThings {
    input := {
        @httpQuery("nextToken")
        nextToken: String

        @httpQuery("maxResults")
        maxResults: Integer

        @httpQuery("prefix")
        prefix: String
    }
    output := {
        nextToken: String
        things: StringList
    }
}

@readonly
@paginated(outputToken: "meta.next", items: "things")
@http(method: "GET", uri: "/nested")
operation ListNested {
    input := {
        @httpQuery("nextToken")
        nextToken: String

        @httpQuery("maxResults")
        maxResults: Integer
    }
    output := {
        meta: PageMeta
        things: StringList
    }
}

@readonly
@http(method: "GET", uri: "/ping")
operation Ping {}

structure PageMeta {
    next: String
}

list StringList {
    member: String
}

enum ChecksumAlgorithm {
    CRC32
    CRC32C
    SHA1
    SHA256
}

enum ChecksumMode {
    ENABLED
}
