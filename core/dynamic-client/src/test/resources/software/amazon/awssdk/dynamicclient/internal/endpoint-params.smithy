$version: "2.0"

namespace test.endpoints

use aws.protocols#awsJson1_0
use smithy.rules#contextParam
use smithy.rules#endpointRuleSet
use smithy.rules#operationContextParams
use smithy.rules#staticContextParams

/// A service whose endpoint rules show which parameter binding won: each of Bucket, Name and Stage, when set, puts
/// its value in the host.
@awsJson1_0
@endpointRuleSet({
    version: "1.0"
    parameters: {
        Region: { type: "string", builtIn: "AWS::Region", documentation: "The region." }
        UseFIPS: {
            type: "boolean"
            builtIn: "AWS::UseFIPS"
            required: true
            default: false
            documentation: "Whether to use the FIPS endpoint."
        }
        Bucket: { type: "string", documentation: "A bucket." }
        Name: { type: "string", documentation: "A name." }
        Stage: { type: "string", documentation: "A stage." }
        Keys: { type: "stringArray", documentation: "Keys." }
    }
    rules: [
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Bucket" }] }]
            endpoint: { url: "https://{Bucket}.bucket.example.com" }
        }
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Name" }] }]
            endpoint: { url: "https://{Name}.name.example.com" }
        }
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Stage" }] }]
            endpoint: { url: "https://{Stage}.stage.example.com" }
        }
        {
            type: "endpoint"
            conditions: [
                { fn: "isSet", argv: [{ ref: "Region" }] }
                { fn: "booleanEquals", argv: [{ ref: "UseFIPS" }, true] }
            ]
            endpoint: {
                url: "https://service-fips.{Region}.example.com"
                headers: { "x-test": ["a", "b"] }
                properties: {
                    authSchemes: [{ name: "sigv4", signingName: "service", signingRegion: "{Region}" }]
                    metricValues: ["1", "2"]
                }
            }
        }
        {
            type: "endpoint"
            conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
            endpoint: { url: "https://service.{Region}.example.com" }
        }
        { type: "error", conditions: [], error: "A region is required" }
    ]
})
service EndpointService {
    version: "2024-01-01"
    operations: [Plain, GetObject, PutObject, ListItems, Describe, Staged]
}

operation Plain {}

operation GetObject {
    input := {
        @contextParam(name: "Bucket")
        bucket: String
    }
}

/// The static Bucket wins over the contextParam member.
@staticContextParams(Bucket: { value: "static-bucket" })
operation PutObject {
    input := {
        @contextParam(name: "Bucket")
        bucket: String
    }
}

@operationContextParams(
    Name: { path: "Nested.Name" }
    Keys: { path: "Items[*].Key" }
)
operation ListItems {
    input := {
        Nested: Nested
        Items: ItemList
    }
}

/// The contextParam member wins over operationContextParams.
@operationContextParams(Name: { path: "Other" })
operation Describe {
    input := {
        @contextParam(name: "Name")
        name: String
        Other: String
    }
}

@staticContextParams(Stage: { value: "beta" })
operation Staged {}

structure Nested {
    Name: String
}

list ItemList {
    member: Item
}

structure Item {
    Key: String
}
