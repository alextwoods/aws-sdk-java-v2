$version: "2.0"

namespace test.auth

use aws.api#service
use aws.auth#sigv4
use aws.auth#sigv4a
use aws.auth#unsignedPayload
use aws.protocols#awsJson1_0

/// A service with every auth scheme the SDK implements, and HTTP basic auth, which it does not.
@service(sdkId: "Auth Test")
@awsJson1_0
@sigv4(name: "auth-signing")
@sigv4a(name: "auth-signing")
@httpBearerAuth
@httpBasicAuth
@auth([sigv4])
service AuthService {
    version: "2024-01-01"
    operations: [
        DefaultAuth
        Sigv4aFirst
        BearerOnly
        Anonymous
        OptionalAuth
        BasicOnly
        BasicThenSigv4
        UnsignedPayload
    ]
}

/// The service's auth: sigv4.
operation DefaultAuth {}

@auth([sigv4a, sigv4])
operation Sigv4aFirst {}

@auth([httpBearerAuth])
operation BearerOnly {}

/// No auth schemes: noAuth only.
@auth([])
operation Anonymous {}

/// The service's auth, then noAuth.
@optionalAuth
operation OptionalAuth {}

/// Only a scheme without an implementation.
@auth([httpBasicAuth])
operation BasicOnly {}

/// A scheme without an implementation, then sigv4.
@auth([httpBasicAuth, sigv4])
operation BasicThenSigv4 {}

@unsignedPayload
operation UnsignedPayload {}
