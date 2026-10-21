$version: "2.0"

namespace test.multi

use aws.protocols#awsJson1_0

/// Two services, neither with the aws.api#service trait, and a structure that is not a service.
@awsJson1_0
service AlphaService {
    operations: [Ping]
}

@awsJson1_0
service BetaService {
    operations: [Ping]
}

operation Ping {
    input := {
        message: String
    }
}

structure NotAService {
    value: String
}
