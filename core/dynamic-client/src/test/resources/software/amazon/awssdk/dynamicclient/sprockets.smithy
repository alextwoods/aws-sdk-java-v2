$version: "2"

// The reference model of the Dynamic Client SEP's Testing section, unchanged. DynamicClientBehaviorTest applies each
// DC case's "Given" traits to it, and swaps the protocol trait for the other Supported_Protocols (SprocketsModels).
namespace smithy.example

@aws.protocols#awsJson1_0
@smithy.rules#endpointRuleSet(
    version: "1.0",
    parameters: {},
    rules: [
        {
            "type": "endpoint",
            "conditions": [],
            "endpoint": { "url": "https://example.com" }
        }
    ]
)
service Sprockets {
    operations: [
        CreateSprocket,
        GetSprocket,
        TagSprocket,
        UploadSprocket,
        DownloadSprocket,
        WatchSprockets,
        SendSprocketCommands,
        SyncSprockets
    ]
    errors: [ServiceFooError]
}

operation CreateSprocket {
    input := {}
    output := { id: String }
    errors: [InvalidSprocketId]
}

operation GetSprocket {
    input := { id: String }
    output := { id: String }
    errors: [InvalidSprocketId]
}

// Member carries @jsonName; its target SprocketId carries @pattern.
operation TagSprocket {
    input := {
        @jsonName("Id")
        id: SprocketId
    }
    output := {}
}

// Streaming blob input.
operation UploadSprocket {
    input := {
        @required
        data: StreamingBlob

        checksum: Blob
    }
    output := {}
}

// Streaming blob output.
operation DownloadSprocket {
    input := { id: String }
    output := {
        @required
        data: StreamingBlob
    }
}

// Output event stream.
operation WatchSprockets {
    input := {}
    output := {
        events: SprocketEvents
    }
}

// Input event stream.
operation SendSprocketCommands {
    input := {
        commands: SprocketEvents
    }
    output := {}
}

// Bidirectional event stream.
operation SyncSprockets {
    input := {
        commands: SprocketEvents
    }
    output := {
        events: SprocketEvents
    }
}

@pattern("^[a-z]+$")
string SprocketId

@streaming
blob StreamingBlob

@streaming
union SprocketEvents {
    sprocket: SprocketEvent
}

structure SprocketEvent {
    id: String
}

@error("client")
structure InvalidSprocketId { id: String }

@error("server")
structure ServiceFooError { why: String }
