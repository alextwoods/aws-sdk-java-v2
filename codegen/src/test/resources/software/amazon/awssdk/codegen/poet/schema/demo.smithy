$version: "2.0"

namespace demo.schemas

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1
use other.ns#External

@service(sdkId: "Demo")
@sigv4(name: "demo")
@restJson1
@documentation("Not included in schemas")
service DemoService {
    version: "2024-01-01"
    operations: [GetThing, PutThing, Ping]
    errors: [ServiceFault]
}

@http(method: "POST", uri: "/things/{Id}")
@readonly
operation GetThing {
    input: GetThingRequest
    output: GetThingResponse
    errors: [NotFound]
}

@http(method: "PUT", uri: "/things")
@idempotent
operation PutThing {
    input: PutThingRequest
    output: Unit
}

@http(method: "GET", uri: "/ping")
@readonly
operation Ping {
    input: Unit
    output: Unit
}

structure GetThingRequest {
    @required
    @httpLabel
    Id: String

    @httpQuery("verbose")
    Verbose: Boolean

    @jsonName("count")
    Count: PrimitiveInteger = 0
}

structure GetThingResponse {
    Root: TreeNode
    Self: SelfRef
    A: MutualA
    Color: Color
    Level: Level
    Choice: Choice
    Tags: TagMap
    Created: EpochTime
    Data: Blob
    Doc: Document
    Secret: SecretString
    External: External
}

structure PutThingRequest {
    @required
    @httpPayload
    Body: ThingBody
}

structure ThingBody {
    Name: String
    Size: Long
    Ratio: Double
    Big: BigDecimal
    Huge: BigInteger
    Tiny: Byte
    Small: Short
    Weight: Float
}

/// A tree: TreeNode and TreeNodeList form one recursive component.
structure TreeNode {
    Value: String
    Children: TreeNodeList
}

list TreeNodeList {
    member: TreeNode
}

structure SelfRef {
    Next: SelfRef
}

structure MutualA {
    B: MutualB
}

structure MutualB {
    A: MutualA
    Name: String
}

enum Color {
    RED = "red"
    GREEN = "green"
}

intEnum Level {
    LOW = 1
    HIGH = 10
}

union Choice {
    Text: String
    Empty: Unit
}

map TagMap {
    key: String
    value: String
}

@timestampFormat("epoch-seconds")
timestamp EpochTime

@sensitive
string SecretString

@error("client")
@httpError(404)
structure NotFound {
    message: String
}

@error("server")
@httpError(500)
@retryable(throttling: true)
structure ServiceFault {
    message: String
}

structure NotInTheClosure {
    Value: String
}
