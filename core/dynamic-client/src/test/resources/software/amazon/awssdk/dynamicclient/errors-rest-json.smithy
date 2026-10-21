$version: "2.0"

namespace test.errors.rest

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1

/// A restJson1 service whose operation error binds a member to a header and another to a jsonName.
@service(sdkId: "Error Rest")
@sigv4(name: "errorrest")
@restJson1
service ErrorRest {
    version: "2024-01-01"
    operations: [GetWidget]
    errors: [AccessDenied]
}

@readonly
@http(method: "GET", uri: "/widgets/{id}")
operation GetWidget {
    input := {
        @required
        @httpLabel
        id: String
    }
    output := {
        name: String
    }
    errors: [WidgetNotFound]
}

@error("client")
@httpError(404)
structure WidgetNotFound {
    message: String

    @httpHeader("X-Widget-Id")
    widgetId: String

    @jsonName("retry_after")
    retryAfter: Integer
}

@error("client")
@httpError(403)
structure AccessDenied {
    Message: String
}
