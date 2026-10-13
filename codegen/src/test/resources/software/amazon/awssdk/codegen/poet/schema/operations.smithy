$version: "2.0"

metadata suppressions = [
    {
        id: "QueryErrorCode"
        namespace: "ops.test"
        reason: "ItemMissing and OtherMissing share a code to test which one the code index keeps"
    }
]

namespace ops.test

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsQuery
use aws.protocols#awsQueryError

@service(sdkId: "Ops")
@sigv4(name: "ops")
@httpBearerAuth
@awsQuery
@xmlNamespace(uri: "https://example.com/ops/")
service OpsService {
    version: "2024-01-01"
    operations: [GetItem, PutItem, Ping, SignedOnly]
    errors: [ServerFault]
}

operation GetItem {
    input: GetItemInput
    output: GetItemOutput
    errors: [OtherMissing, ItemMissing, LegacyMissing, ServerFault]
}

@optionalAuth
operation PutItem {
    input: PutItemInput
    output: Unit
}

@auth([])
operation Ping {
    input: Unit
    output: Unit
}

@auth([sigv4])
operation SignedOnly {
    input: SignedOnlyInput
    output: SignedOnlyOutput
    errors: [ItemMissing]
}

structure GetItemInput {
    @required
    Id: String
}

structure GetItemOutput {
    Item: Item
}

structure Item {
    Name: String
    /// A structure with the error trait that is reachable only through a member.
    LastError: MemberError
}

structure PutItemInput {
    Item: Item
}

structure SignedOnlyInput {}

structure SignedOnlyOutput {}

@error("client")
@httpError(404)
@awsQueryError(code: "Missing", httpResponseCode: 404)
structure ItemMissing {
    message: String
}

/// Has the same awsQueryError code as ItemMissing, and sorts after it.
@error("client")
@awsQueryError(code: "Missing", httpResponseCode: 404)
structure OtherMissing {
    message: String
}

/// Shares ItemMissing's exception class in the tests, so it gets no registry entry.
@error("client")
@awsQueryError(code: "Legacy", httpResponseCode: 400)
structure LegacyMissing {
    message: String
}

@error("server")
structure ServerFault {
    message: String
}

@error("client")
structure MemberError {
    message: String
}

@error("client")
structure NotInTheClosure {
    message: String
}
