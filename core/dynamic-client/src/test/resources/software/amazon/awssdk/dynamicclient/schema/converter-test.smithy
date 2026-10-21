$version: "2.0"

namespace test.dyn

use aws.auth#sigv4
use aws.protocols#restJson1
use com.amazonaws.c2j#c2jName

@restJson1
@sigv4(name: "dyn")
@documentation("The test service.")
service DynService {
    version: "2024-01-01"
    operations: [GetThing, Download]
    resources: [Thing]
}

resource Thing {
    identifiers: { thingId: String }
}

@http(method: "POST", uri: "/thing")
@documentation("Gets a thing.")
operation GetThing {
    input: GetThingInput
    output: GetThingOutput
    errors: [NotFound]
}

@http(method: "GET", uri: "/download")
@readonly
operation Download {
    input := {}
    output: DownloadOutput
}

@input
@c2jName("GetThingRequest")
structure GetThingInput {
    @required
    @httpHeader("x-id")
    @documentation("The ID.")
    @length(min: 1)
    id: String

    @jsonName("cnt")
    @range(min: 0)
    count: Integer

    @httpQuery("v")
    @tags(["internal"])
    verbose: Boolean
}

@output
structure GetThingOutput {
    blob: Blob
    bool: Boolean
    str: String
    time: Timestamp
    epoch: EpochTime
    b: Byte
    s: Short
    i: Integer
    l: Long
    f: Float
    d: Double
    bi: BigInteger
    bd: BigDecimal
    doc: Document
    color: Color
    level: Level
    legacy: LegacyEnum
    list: StringList
    sparse: SparseList
    map: IntMap
    choice: Choice
    nested: Nested
    secret: Secret

    @pattern("^a")
    patterned: String
}

@output
structure DownloadOutput {
    @httpPayload
    @required
    body: Stream
}

@streaming
blob Stream

structure Nested {
    name: String
    tags: StringList
}

enum Color {
    RED = "red"
    GREEN = "green"
}

intEnum Level {
    LOW = 1
    HIGH = 10
}

@enum([{ value: "b" }, { value: "a" }])
string LegacyEnum

list StringList {
    member: String
}

@sparse
list SparseList {
    member: String
}

map IntMap {
    key: String
    value: Integer
}

union Choice {
    text: String
    number: Integer
    nested: Nested
}

@sensitive
string Secret

@timestampFormat("epoch-seconds")
timestamp EpochTime

@error("client")
@httpError(404)
structure NotFound {
    message: String
}
