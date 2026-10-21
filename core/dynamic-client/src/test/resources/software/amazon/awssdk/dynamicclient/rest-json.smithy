$version: "2.0"

namespace test.restjson

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1

/// A restJson1 service with label, query, header and body members.
@service(sdkId: "Rest Widgets")
@sigv4(name: "restwidgets")
@restJson1
service RestWidgets {
    version: "2024-01-01"
    operations: [UpdateWidget]
}

@idempotent
@http(method: "PUT", uri: "/widgets/{widgetId}")
operation UpdateWidget {
    input := {
        @required
        @httpLabel
        widgetId: String

        @httpQuery("dryRun")
        dryRun: Boolean

        @httpHeader("X-Trace")
        trace: String

        @jsonName("display_name")
        displayName: String

        updatedAt: Timestamp
    }
    output := {
        @httpHeader("X-Version")
        version: Integer

        @jsonName("display_name")
        displayName: String

        updatedAt: Timestamp

        missing: String
    }
}
