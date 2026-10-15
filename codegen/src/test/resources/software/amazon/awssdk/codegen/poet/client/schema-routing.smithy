$version: "2.0"

namespace demo

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_0
use aws.protocols#awsQuery
use smithy.protocols#rpcv2Cbor

@service(sdkId: "Demo")
@sigv4(name: "demo")
@xmlNamespace(uri: "https://demo.example.com")
@rpcv2Cbor
@awsJson1_0
@awsQuery
service DemoService {
    version: "2024-01-01"
    operations: [GetThing, PutThing, ListThings, WatchThings, CountThings]
    errors: [InternalFault]
}

/// A plain operation with a modeled error and a host prefix.
@endpoint(hostPrefix: "data.")
operation GetThing {
    input: GetThingRequest
    output: GetThingResponse
    errors: [ThingNotFound]
}

/// An operation with request compression and a required checksum.
@requestCompression(encodings: ["gzip"])
@httpChecksumRequired
operation PutThing {
    input: PutThingRequest
    output: PutThingResponse
}

@paginated(inputToken: "NextToken", outputToken: "NextToken", items: "Names")
@readonly
operation ListThings {
    input: ListThingsRequest
    output: ListThingsResponse
}

/// An event-stream operation, which stays on the legacy path.
operation WatchThings {
    input: WatchThingsRequest
    output: WatchThingsResponse
}

/// An operation whose input class has no schema serde members (a BigInteger member), which stays on the legacy path.
operation CountThings {
    input: CountThingsRequest
    output: CountThingsResponse
}

structure CountThingsRequest {
    Total: BigInteger
}

structure CountThingsResponse {}

structure GetThingRequest {
    Name: String
}

structure GetThingResponse {
    Name: String
}

structure PutThingRequest {
    Name: String
}

structure PutThingResponse {}

structure ListThingsRequest {
    NextToken: String
}

structure ListThingsResponse {
    Names: NameList
    NextToken: String
}

list NameList {
    member: String
}

structure WatchThingsRequest {
    Name: String
}

structure WatchThingsResponse {
    Events: ThingEvents
}

@streaming
union ThingEvents {
    Changed: ThingChanged
}

structure ThingChanged {
    Name: String
}

@error("client")
@httpError(404)
structure ThingNotFound {
    message: String
}

@error("server")
structure InternalFault {
    message: String
}
