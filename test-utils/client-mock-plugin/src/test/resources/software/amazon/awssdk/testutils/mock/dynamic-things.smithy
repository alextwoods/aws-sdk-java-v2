$version: "2.0"

namespace test.mock.things

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_0
use aws.protocols#restJson1
use aws.protocols#restXml
use smithy.protocols#rpcv2Cbor

/// Four services, one per protocol, that share their operations, for DynamicClientMockPluginTest.
@service(sdkId: "Json Things")
@sigv4(name: "jsonthings")
@awsJson1_0
service JsonThings {
    version: "2024-01-01"
    operations: [GetThing, PutThing]
    errors: [AccessDenied]
}

@service(sdkId: "Rest Json Things")
@sigv4(name: "restjsonthings")
@restJson1
service RestJsonThings {
    version: "2024-01-01"
    operations: [GetThing, PutThing]
    errors: [AccessDenied]
}

@service(sdkId: "Cbor Things")
@sigv4(name: "cborthings")
@rpcv2Cbor
service CborThings {
    version: "2024-01-01"
    operations: [GetThing, PutThing]
    errors: [AccessDenied]
}

@service(sdkId: "Xml Things")
@sigv4(name: "xmlthings")
@restXml
service XmlThings {
    version: "2024-01-01"
    operations: [GetThing, PutThing]
    errors: [AccessDenied]
}

@readonly
@http(method: "GET", uri: "/things/{id}")
operation GetThing {
    input := {
        @required
        @httpLabel
        id: String
    }

    output := {
        name: String

        count: Integer

        @httpHeader("X-Thing-Version")
        version: String

        created: Timestamp

        tags: Tags

        owner: Owner
    }

    errors: [ThingNotFound]
}

@idempotent
@http(method: "PUT", uri: "/things/{id}")
operation PutThing {
    input := {
        @required
        @httpLabel
        id: String

        name: String
    }

    output := {
        id: String
    }
}

list Tags {
    member: String
}

structure Owner {
    name: String

    level: Integer
}

@error("client")
@httpError(404)
structure ThingNotFound {
    message: String

    thingId: String

    retries: Integer
}

@error("client")
@httpError(403)
structure AccessDenied {
    message: String
}
