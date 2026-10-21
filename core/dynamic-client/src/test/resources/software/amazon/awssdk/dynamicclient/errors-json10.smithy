$version: "2.0"

namespace test.errors.json

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_0

/// An awsJson1_0 service with operation errors, service errors, an error listed by both an operation and the
/// service, and a retryable error.
@service(sdkId: "Error Json")
@sigv4(name: "errorjson")
@awsJson1_0
service ErrorJson {
    version: "2024-01-01"
    operations: [PutRecord, GetRecord]
    errors: [ServiceFault, SharedFault]
}

operation PutRecord {
    input := {
        name: String
    }
    output := {}
    errors: [RecordInvalid, SharedFault, TryAgain]
}

@readonly
operation GetRecord {
    input := {
        id: String
    }
    output := {}
    errors: [RecordMissing]
}

@error("client")
structure RecordInvalid {
    message: String
    reason: String
    fieldCount: Integer
    details: Details
    tags: Tags
}

structure Details {
    field: String
    at: Timestamp
}

list Tags {
    member: String
}

@error("client")
structure RecordMissing {
    id: String
}

@error("client")
@retryable
structure TryAgain {
    message: String
}

/// Not bound to the service: DocumentExceptionTest builds its exceptions directly.
@error("client")
@retryable(throttling: true)
structure Throttled {
    message: String
}

@error("client")
structure SharedFault {
    message: String
}

@error("server")
structure ServiceFault {
    Message: String
    code: Integer
}
