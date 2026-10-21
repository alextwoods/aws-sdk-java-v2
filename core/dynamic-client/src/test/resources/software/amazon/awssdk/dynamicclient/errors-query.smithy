$version: "2.0"

metadata suppressions = [
    {
        id: "QueryErrorCode"
        namespace: "test.errors.query"
        reason: "The dynamic client tests check that the operation's error wins when two errors share a code."
    }
    {
        id: "DeprecatedTrait"
        namespace: "test.errors.query"
        reason: "awsQuery is the protocol under test."
    }
]

namespace test.errors.query

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsQuery
use aws.protocols#awsQueryError

/// An awsQuery service whose operation error and service error have the same awsQueryError code.
@service(sdkId: "Error Query")
@sigv4(name: "errorquery")
@awsQuery
@xmlNamespace(uri: "https://example.com/doc/2024-01-01/")
service ErrorQuery {
    version: "2024-01-01"
    operations: [Publish, Describe]
    errors: [ServiceLimit]
}

operation Publish {
    input := {
        topic: String
    }
    output := {}
    errors: [PublishLimit]
}

@readonly
operation Describe {
    input := {}
    output := {}
}

@error("client")
@awsQueryError(code: "LimitExceeded", httpResponseCode: 400)
structure PublishLimit {
    message: String
    limit: Integer
}

@error("client")
@awsQueryError(code: "LimitExceeded", httpResponseCode: 400)
structure ServiceLimit {
    Message: String
}
