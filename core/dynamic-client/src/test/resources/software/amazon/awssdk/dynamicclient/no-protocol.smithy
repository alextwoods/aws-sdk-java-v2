$version: "2.0"

namespace test.noprotocol

use aws.auth#sigv4

/// A service without a protocol trait.
@sigv4(name: "noprotocol")
service NoProtocolService {
    version: "2024-01-01"
    operations: [Ping]
}

operation Ping {}
