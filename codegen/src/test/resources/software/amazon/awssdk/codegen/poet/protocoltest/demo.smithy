$version: "2.0"

namespace demo.protocoltests

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_0
use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A protocol the SDK does not support; its test cases are not selected.
@protocolDefinition
@trait(selector: "service")
structure fakeProtocol {}

@service(sdkId: "ProtoDemo")
@sigv4(name: "protodemo")
@awsJson1_0
@restJson1
@fakeProtocol
service ProtoDemo {
    version: "2024-01-01"
    operations: [PutThing, GetThing, WatchThings]
    errors: [ServiceFault]
}

@http(method: "POST", uri: "/things/{Name}")
@httpRequestTests([
    {
        id: "PutThingAllTypes"
        documentation: "Serializes every simple and aggregate member type."
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "ProtoDemo.PutThing" }
        forbidHeaders: ["X-Forbidden"]
        requireHeaders: ["Content-Length"]
        body: "{\"Name\":\"thing\"}"
        bodyMediaType: "application/json"
        params: {
            Name: "thing"
            Count: -2147483648
            Big: 9007199254740993
            Small: 7
            Tiny: -3
            Ratio: 1.5
            Fraction: "NaN"
            Infinite: "-Infinity"
            Flag: true
            When: 1576540098.5
            Data: "blob \"quoted\""
            Color: "red"
            Colors: ["red", "green"]
            Tags: ["a", null, "b"]
            Empty: []
            Attributes: { k1: "v1", k2: "v2" }
            Nested: { Label: "x", Children: [{ Label: "y", Children: [] }] }
            Choice: { Text: "t" }
            Doc: { a: [1, 2.5, "s", null, true], b: {} }
            Precise: 0.1000000000000000055511151231257827
            Level: 2
            Grid: [[1, 2], []]
            ByColor: { red: [1] }
        }
    }
    {
        id: "PutThingRestJson"
        protocol: restJson1
        method: "POST"
        uri: "/things/thing"
        host: "example.com/base"
        resolvedHost: "example.com"
        queryParams: ["a=b"]
        forbidQueryParams: ["c"]
        requireQueryParams: ["a"]
        body: ""
        params: { Name: "thing" }
    }
    {
        id: "PutThingServerOnly"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        appliesTo: "server"
        params: { Name: "thing" }
    }
    {
        id: "PutThingFakeProtocol"
        protocol: fakeProtocol
        method: "POST"
        uri: "/"
        params: { Name: "thing" }
    }
])
operation PutThing {
    input: PutThingInput
    output: PutThingOutput
    errors: [ThingConflict]
}

@input
structure PutThingInput {
    @required
    @httpLabel
    Name: String
    Count: Integer
    Big: Long
    Small: Short
    Tiny: Byte
    Ratio: Float
    Fraction: Double
    Infinite: Double
    Flag: Boolean
    When: Timestamp
    Data: Blob
    Color: Color
    Colors: ColorList
    Tags: SparseStrings
    Empty: Strings
    Attributes: StringMap
    Nested: Node
    Choice: Choice
    Doc: Document
    Precise: BigDecimal
    Level: Level
    Grid: IntegerGrid
    ByColor: ColorToIntegers

    @idempotencyToken
    Token: String
}

@output
structure PutThingOutput {}

@http(method: "GET", uri: "/things")
@httpResponseTests([
    {
        id: "GetThingOutput"
        protocol: awsJson1_0
        code: 200
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        body: "{\"Label\":\"x\",\"Values\":[1,2],\"Created\":0}"
        bodyMediaType: "application/json"
        params: { Label: "x", Values: [1, 2], Created: 0 }
    }
    {
        id: "GetThingEmptyOutput"
        protocol: awsJson1_0
        code: 200
        params: {}
    }
])
operation GetThing {
    output: GetThingOutput
    errors: [ThingNotFound]
}

@output
structure GetThingOutput {
    Label: String
    Values: Integers
    Created: Timestamp
}

@http(method: "POST", uri: "/watch")
@httpRequestTests([
    {
        id: "WatchThingsEventStream"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        params: {}
    }
])
operation WatchThings {
    output: WatchThingsOutput
}

@output
structure WatchThingsOutput {
    @httpPayload
    Events: ThingEvents
}

@streaming
union ThingEvents {
    Changed: ThingChanged
}

structure ThingChanged {
    Label: String
}

@error("client")
@httpError(404)
@httpResponseTests([
    {
        id: "ThingNotFoundError"
        protocol: awsJson1_0
        code: 404
        headers: { "X-Amzn-Errortype": "ThingNotFound" }
        body: "{\"Message\":\"missing\",\"Resource\":\"thing\"}"
        bodyMediaType: "application/json"
        params: { Message: "missing", Resource: "thing" }
    }
])
structure ThingNotFound {
    Message: String
    Resource: String
}

@error("client")
structure ThingConflict {
    Message: String
}

@error("server")
@httpResponseTests([
    {
        id: "ServiceFaultError"
        protocol: awsJson1_0
        code: 500
        headers: { "X-Amzn-Errortype": "ServiceFault" }
        body: "{\"Message\":\"boom\"}"
        bodyMediaType: "application/json"
        params: { Message: "boom" }
    }
])
structure ServiceFault {
    Message: String
}

enum Color {
    RED = "red"
    GREEN = "green"
}

intEnum Level {
    LOW = 1
    HIGH = 2
}

list ColorList {
    member: Color
}

@sparse
list SparseStrings {
    member: String
}

list Strings {
    member: String
}

list Integers {
    member: Integer
}

list IntegerGrid {
    member: Integers
}

map StringMap {
    key: String
    value: String
}

map ColorToIntegers {
    key: Color
    value: Integers
}

structure Node {
    Label: String
    Children: NodeList
}

list NodeList {
    member: Node
}

union Choice {
    Text: String
    Number: Integer
}
