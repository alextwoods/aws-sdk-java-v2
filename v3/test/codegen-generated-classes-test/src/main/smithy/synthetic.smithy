$version: "2"
namespace software.amazon.awssdk.test.generated.synthetic

use smithy.waiters#waitable

/// Synthetic awsJson1_0 service of the generated-class compile fixtures: a plain operation, a no-input operation, a
/// token-paginated operation and a waiter.
@aws.protocols#awsJson1_0
@aws.auth#sigv4(name: "syntheticservice")
@paginated(inputToken: "nextToken", outputToken: "nextToken", pageSize: "maxResults")
service SyntheticService {
    version: "2026-01-01"
    operations: [GetThing, ListThings, DescribeService]
    errors: [ThingNotFound]
}

@readonly
@waitable(
    ThingExists: {
        acceptors: [
            { state: "success", matcher: { success: true } }
            { state: "retry", matcher: { errorType: "ThingNotFound" } }
        ]
        minDelay: 1
        maxDelay: 2
    }
)
operation GetThing {
    input := {
        @required
        thingId: String
    }
    output := {
        thingId: String
        name: String
        sizeInBytes: Long
    }
    errors: [ThingNotFound]
}

@readonly
@paginated(items: "things")
operation ListThings {
    input := {
        nextToken: String
        maxResults: Integer
    }
    output := {
        nextToken: String
        things: ThingList
    }
}

/// Has no input, so the client keeps a no-argument overload.
@readonly
operation DescribeService {
    output := {
        version: String
    }
}

list ThingList {
    member: String
}

@error("client")
structure ThingNotFound {
    message: String
}
