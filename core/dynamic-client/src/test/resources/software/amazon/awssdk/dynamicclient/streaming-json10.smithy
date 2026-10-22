$version: "2.0"

namespace test.streaming.json

use aws.auth#sigv4
use aws.protocols#awsJson1_0

/// An awsJson1_0 service with a streaming blob input member, which only HTTP binding protocols can bind.
@sigv4(name: "jsonstream")
@awsJson1_0
service JsonStreaming {
    version: "2024-01-01"
    operations: [PutBlob]
}

operation PutBlob {
    input := {
        @required
        body: Payload
    }
}

@streaming
blob Payload
