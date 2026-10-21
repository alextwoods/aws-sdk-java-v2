$version: "2.0"

namespace test.json10

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_0

/// An awsJson1_0 service with an operation for every coercion rule, an operation bound through a resource, an
/// operation without input or output, and errors on the operation and the service.
@service(sdkId: "Json Ten")
@sigv4(name: "jsonten")
@awsJson1_0
service JsonTen {
    version: "2024-01-01"
    operations: [PutItem, Ping]
    resources: [Thing]
    errors: [ServiceFault]
}

resource Thing {
    identifiers: { thingId: String }
    read: GetThing
}

@readonly
operation GetThing {
    input := {
        @required
        thingId: String
    }
    output := {
        thingId: String
        name: String
    }
}

operation Ping {}

operation PutItem {
    input := {
        @required
        name: String
        count: Integer
        tiny: Byte
        small: Short
        big: Long
        huge: BigInteger
        ratio: Double
        exact: BigDecimal
        flag: Boolean
        created: Timestamp
        data: Blob
        tags: TagList
        attributes: AttributeMap
        nested: Nested
        choice: Choice
    }
    output := {
        id: String
        created: Timestamp
        tags: TagList
        nested: Nested
        missing: String
    }
    errors: [ItemFault]
}

list TagList {
    member: String
}

map AttributeMap {
    key: String
    value: Integer
}

structure Nested {
    label: String
    when: Timestamp
}

union Choice {
    text: String
    number: Integer
}

@error("client")
structure ItemFault {
    message: String
}

@error("server")
structure ServiceFault {
    message: String
}
