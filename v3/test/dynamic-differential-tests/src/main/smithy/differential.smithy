$version: "2"

namespace example.differential

use smithy.waiters#waitable

/// The synthetic services of the generated-versus-dynamic differential matrix (task 18.4): one SigV4 service per
/// launch protocol, sharing every shape. Each service binds the call shapes its protocol supports: a finite operation
/// (Echo/RestEcho) with scalar, timestamp, blob, list, map, nested structure and idempotency-token members and a
/// modeled error; a token-paginated operation (ListItems/RestListItems); a waiter (StatusReady on
/// GetStatus/RestGetStatus) with success, failure, output-retry and error-retry acceptors; for the HTTP-binding
/// protocols, streaming blob input (Upload) and output (Download); and, for every protocol with event streams, an input
/// (Publish/RestPublish) and an output (Watch/RestWatch) event stream with header, payload and structure events and a
/// modeled exception event. The query protocols bind no streaming operation: they have neither an HTTP payload binding
/// nor event streams.
@aws.protocols#awsJson1_0
@aws.auth#sigv4(name: "jsonzero")
@aws.api#service(sdkId: "JsonZero", endpointPrefix: "jsonzero")
@smithy.rules#endpointRuleSet({
    version: "1.0"
    parameters: {
        Region: { type: "string", builtIn: "AWS::Region", required: false, documentation: "The region." }
        Endpoint: { type: "string", builtIn: "SDK::Endpoint", required: false, documentation: "An override." }
    }
    rules: [
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Endpoint" }] }]
            endpoint: { url: { ref: "Endpoint" } }
        }
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
            endpoint: { url: "https://jsonzero.{Region}.example.test" }
        }
        { type: "error", conditions: [], error: "A region is required" }
    ]
})
service JsonZero {
    version: "2024-01-01"
    operations: [Echo, Defaults, ListItems, GetStatus, Watch, Publish]
}

@aws.protocols#awsJson1_1
@aws.auth#sigv4(name: "jsonone")
@aws.api#service(sdkId: "JsonOne", endpointPrefix: "jsonone")
@smithy.rules#endpointRuleSet({
    version: "1.0"
    parameters: {
        Region: { type: "string", builtIn: "AWS::Region", required: false, documentation: "The region." }
        Endpoint: { type: "string", builtIn: "SDK::Endpoint", required: false, documentation: "An override." }
    }
    rules: [
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Endpoint" }] }]
            endpoint: { url: { ref: "Endpoint" } }
        }
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
            endpoint: { url: "https://jsonone.{Region}.example.test" }
        }
        { type: "error", conditions: [], error: "A region is required" }
    ]
})
service JsonOne {
    version: "2024-01-01"
    operations: [Echo, Defaults, ListItems, GetStatus, Watch, Publish]
}

@smithy.protocols#rpcv2Cbor
@aws.auth#sigv4(name: "cbor")
@aws.api#service(sdkId: "Cbor", endpointPrefix: "cbor")
@smithy.rules#endpointRuleSet({
    version: "1.0"
    parameters: {
        Region: { type: "string", builtIn: "AWS::Region", required: false, documentation: "The region." }
        Endpoint: { type: "string", builtIn: "SDK::Endpoint", required: false, documentation: "An override." }
    }
    rules: [
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Endpoint" }] }]
            endpoint: { url: { ref: "Endpoint" } }
        }
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
            endpoint: { url: "https://cbor.{Region}.example.test" }
        }
        { type: "error", conditions: [], error: "A region is required" }
    ]
})
service Cbor {
    version: "2024-01-01"
    operations: [Echo, Defaults, ListItems, GetStatus, Watch, Publish]
}

@aws.protocols#awsQuery
@xmlNamespace(uri: "https://example.test/differential/")
@aws.auth#sigv4(name: "query")
@aws.api#service(sdkId: "Query", endpointPrefix: "query")
@smithy.rules#endpointRuleSet({
    version: "1.0"
    parameters: {
        Region: { type: "string", builtIn: "AWS::Region", required: false, documentation: "The region." }
        Endpoint: { type: "string", builtIn: "SDK::Endpoint", required: false, documentation: "An override." }
    }
    rules: [
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Endpoint" }] }]
            endpoint: { url: { ref: "Endpoint" } }
        }
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
            endpoint: { url: "https://query.{Region}.example.test" }
        }
        { type: "error", conditions: [], error: "A region is required" }
    ]
})
service Query {
    version: "2024-01-01"
    operations: [Echo, Defaults, ListItems, GetStatus]
}

@aws.protocols#ec2Query
@xmlNamespace(uri: "https://example.test/differential/")
@aws.auth#sigv4(name: "ec2")
@aws.api#service(sdkId: "Ec2", endpointPrefix: "ec2")
@smithy.rules#endpointRuleSet({
    version: "1.0"
    parameters: {
        Region: { type: "string", builtIn: "AWS::Region", required: false, documentation: "The region." }
        Endpoint: { type: "string", builtIn: "SDK::Endpoint", required: false, documentation: "An override." }
    }
    rules: [
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Endpoint" }] }]
            endpoint: { url: { ref: "Endpoint" } }
        }
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
            endpoint: { url: "https://ec2.{Region}.example.test" }
        }
        { type: "error", conditions: [], error: "A region is required" }
    ]
})
service Ec2 {
    version: "2024-01-01"
    operations: [Echo, Defaults, ListItems, GetStatus]
}

@aws.protocols#restJson1
@aws.auth#sigv4(name: "restjson")
@aws.api#service(sdkId: "RestJson", endpointPrefix: "restjson")
@smithy.rules#endpointRuleSet({
    version: "1.0"
    parameters: {
        Region: { type: "string", builtIn: "AWS::Region", required: false, documentation: "The region." }
        Endpoint: { type: "string", builtIn: "SDK::Endpoint", required: false, documentation: "An override." }
    }
    rules: [
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Endpoint" }] }]
            endpoint: { url: { ref: "Endpoint" } }
        }
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
            endpoint: { url: "https://restjson.{Region}.example.test" }
        }
        { type: "error", conditions: [], error: "A region is required" }
    ]
})
service RestJson {
    version: "2024-01-01"
    operations: [RestEcho, RestDefaults, RestListItems, RestGetStatus, Upload, Download, RestWatch, RestPublish]
}

@aws.protocols#restXml
@xmlNamespace(uri: "https://example.test/differential/")
@aws.auth#sigv4(name: "restxml")
@aws.api#service(sdkId: "RestXml", endpointPrefix: "restxml")
@smithy.rules#endpointRuleSet({
    version: "1.0"
    parameters: {
        Region: { type: "string", builtIn: "AWS::Region", required: false, documentation: "The region." }
        Endpoint: { type: "string", builtIn: "SDK::Endpoint", required: false, documentation: "An override." }
    }
    rules: [
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Endpoint" }] }]
            endpoint: { url: { ref: "Endpoint" } }
        }
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
            endpoint: { url: "https://restxml.{Region}.example.test" }
        }
        { type: "error", conditions: [], error: "A region is required" }
    ]
})
service RestXml {
    version: "2024-01-01"
    operations: [RestEcho, RestDefaults, RestListItems, RestGetStatus, Upload, Download, RestWatch, RestPublish]
}

// ---------------------------------------------------------------------------------------------------------------------
// RPC operations (awsJson1_0, awsJson1_1, rpcv2Cbor, awsQuery, ec2Query)

operation Echo {
    input := {
        @required
        name: String

        count: Integer
        flag: Boolean
        when: Timestamp
        data: Blob
        tags: TagList
        attrs: AttrMap
        inner: Inner
        color: Color
        rank: Rank

        @idempotencyToken
        token: String
    }
    output := {
        name: String
        count: Integer
        flag: Boolean
        when: Timestamp
        tags: TagList
        inner: Inner
        color: Color
        rank: Rank
    }
    errors: [Bad]
}

/// Members with @default values that the call leaves unset: the input's are serialized, the output's are read as
/// their defaults when absent.
operation Defaults {
    input := {
        @required
        name: String

        limit: Integer = 10
        mode: Color = "RED"
        nested: WithDefaults
    }
    output := {
        name: String
        total: Long = 0
        enabled: Boolean = false
        nested: WithDefaults
    }
    errors: [Bad]
}

@readonly
@paginated(inputToken: "nextToken", outputToken: "nextToken", items: "items", pageSize: "maxResults")
operation ListItems {
    input := {
        nextToken: String
        maxResults: Integer
    }
    output := {
        items: TagList
        nextToken: String
    }
    errors: [Bad]
}

@readonly
@waitable(
    StatusReady: {
        documentation: "Waits until the status is READY."
        acceptors: [
            { state: "success", matcher: { output: { path: "status", expected: "READY", comparator: "stringEquals" } } }
            { state: "failure", matcher: { output: { path: "status", expected: "FAILED", comparator: "stringEquals" } } }
            { state: "retry", matcher: { errorType: "Bad" } }
        ]
        minDelay: 1
        maxDelay: 2
    }
)
operation GetStatus {
    input := {
        @required
        id: String
    }
    output := {
        status: String
        detail: Inner
    }
    errors: [Bad]
}

operation Watch {
    input := {
        name: String
        inner: Inner
    }
    output := {
        status: String
        events: OutEvents
    }
    errors: [Bad]
}

operation Publish {
    input := {
        name: String
        events: InEvents
    }
    output := {
        count: Integer
    }
    errors: [Bad]
}

// ---------------------------------------------------------------------------------------------------------------------
// HTTP-binding operations (restJson1, restXml)

@http(method: "POST", uri: "/echo/{name}")
operation RestEcho {
    input := {
        @required
        @httpLabel
        name: String

        @httpQuery("count")
        count: Integer

        @httpHeader("X-Flag")
        flag: Boolean

        @httpHeader("X-Token")
        @idempotencyToken
        token: String

        when: Timestamp
        data: Blob
        tags: TagList
        attrs: AttrMap
        inner: Inner
        color: Color
        rank: Rank
    }
    output := {
        @httpHeader("X-Name")
        name: String

        count: Integer
        flag: Boolean
        when: Timestamp
        tags: TagList
        inner: Inner
        color: Color
        rank: Rank
    }
    errors: [Bad]
}

/// The HTTP-binding counterpart of Defaults, with the defaulted members in the body.
@http(method: "POST", uri: "/defaults/{name}")
operation RestDefaults {
    input := {
        @required
        @httpLabel
        name: String

        limit: Integer = 10
        mode: Color = "RED"
        nested: WithDefaults
    }
    output := {
        @httpHeader("X-Name")
        name: String

        total: Long = 0
        enabled: Boolean = false
        nested: WithDefaults
    }
    errors: [Bad]
}

@readonly
@http(method: "GET", uri: "/items")
@paginated(inputToken: "nextToken", outputToken: "nextToken", items: "items", pageSize: "maxResults")
operation RestListItems {
    input := {
        @httpQuery("next")
        nextToken: String

        @httpQuery("max")
        maxResults: Integer
    }
    output := {
        items: TagList
        nextToken: String
    }
    errors: [Bad]
}

@readonly
@http(method: "GET", uri: "/status/{id}")
@waitable(
    StatusReady: {
        documentation: "Waits until the status is READY."
        acceptors: [
            { state: "success", matcher: { output: { path: "status", expected: "READY", comparator: "stringEquals" } } }
            { state: "failure", matcher: { output: { path: "status", expected: "FAILED", comparator: "stringEquals" } } }
            { state: "retry", matcher: { errorType: "Bad" } }
        ]
        minDelay: 1
        maxDelay: 2
    }
)
operation RestGetStatus {
    input := {
        @required
        @httpLabel
        id: String
    }
    output := {
        status: String
        detail: Inner
    }
    errors: [Bad]
}

@idempotent
@http(method: "PUT", uri: "/blobs/{id}")
operation Upload {
    input := {
        @required
        @httpLabel
        id: String

        @httpHeader("X-Meta")
        meta: String

        @required
        @httpPayload
        body: Payload
    }
    output := {
        @httpHeader("ETag")
        etag: String
    }
    errors: [Bad]
}

@readonly
@http(method: "GET", uri: "/blobs/{id}")
operation Download {
    input := {
        @required
        @httpLabel
        id: String

        @httpQuery("part")
        part: Integer
    }
    output := {
        @httpHeader("X-Size")
        size: Long

        @required
        @httpPayload
        body: Payload
    }
    errors: [Bad]
}

@http(method: "POST", uri: "/watch/{id}")
operation RestWatch {
    input := {
        @required
        @httpLabel
        id: String

        @httpHeader("X-Topic")
        topic: String
    }
    output := {
        @httpHeader("X-Session")
        session: String

        @httpPayload
        events: OutEvents
    }
    errors: [Bad]
}

@http(method: "POST", uri: "/publish/{id}")
operation RestPublish {
    input := {
        @required
        @httpLabel
        id: String

        @httpHeader("X-Topic")
        topic: String

        @httpPayload
        events: InEvents
    }
    output := {
        @httpHeader("X-Version")
        version: Integer

        count: Integer
    }
    errors: [Bad]
}

// ---------------------------------------------------------------------------------------------------------------------
// Shared shapes

@streaming
blob Payload

@streaming
union OutEvents {
    note: Note
    chunk: Chunk
    failure: Failure
}

@streaming
union InEvents {
    note: Note
    chunk: Chunk
}

/// A structure event: its members are the event payload.
structure Note {
    text: String

    @required
    level: Integer

    tags: TagList
}

/// An event of header members and a blob payload.
structure Chunk {
    @eventHeader
    kind: String

    @eventHeader
    seq: Integer

    @eventPayload
    data: Blob
}

@error("client")
structure Failure {
    message: String
    reason: String
}

/// A nested structure with a defaulted member.
structure WithDefaults {
    label: String
    size: Integer = 1
}

structure Inner {
    label: String

    @required
    level: Integer
}

list TagList {
    member: String
}

enum Color {
    RED
    GREEN = "green"
}

intEnum Rank {
    LOW = 1
    HIGH = 2
}

map AttrMap {
    key: String
    value: String
}

@error("client")
@httpError(400)
structure Bad {
    message: String
    reason: String
}
