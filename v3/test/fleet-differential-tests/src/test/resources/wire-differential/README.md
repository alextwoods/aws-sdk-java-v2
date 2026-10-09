# Fleet wire differential (task 18.2)

`WireDifferentialTest` sends the same request members through a V3 generated client and the frozen V2 baseline
client (the released `v3.v2Baseline.version` jars in `target/v2-baseline`, loaded by an isolated class loader whose
parent is the platform loader) over in-memory capturing transports that return the same canned response. Each case is
checked against the canonical expectations of the Smithy model and protocol, then compared with V2 aspect by aspect.

## Case selection (deterministic)

- Services: the fixed list `WireCases.V2_SERVICES` (cloudwatch, cognitoidentityprovider, dynamodb, ec2, kinesis,
  lambda, pinpoint, s3, sns, sts), which together model every launch protocol.
- Request cases: for each service and each binding category of its protocol (LABEL, QUERY, HEADER, PAYLOAD,
  STREAMING for restJson1/restXml; BODY for RPC protocols), pick the first operation in shape-ID order that meets
  three conditions: its input has a simple member of that category, every required input member is simple, and it has
  no event stream. Simple means string, enum, boolean, integer, long or timestamp, or a streaming blob payload for
  STREAMING. The request sets the required members and the first simple member of the category, using fixed values:
  `fd-<membername>`, the first enum value, `true`, `7`, `2024-01-02T03:04:05Z`, and the body
  `fleet-differential-body`.
- Response cases: for each service, pick the first selected operation (shape-ID order) that has a simple output member
  in the binding its protocol reads (a header for HTTP-binding protocols, the body otherwise) and at least one error.
  It is answered three ways: a success carrying that member (SUCCESS), its first error in shape-ID order
  (MODELED_ERROR), and the error code `FleetDifferentialUnmodeledError` (UNMODELED_ERROR).

## Normalization

A request is compared as these aspects:

- method, host, and raw path (an empty path is `/`)
- decoded query parameters
- the protocol headers `content-type` (media type only), `x-amz-target`, `smithy-protocol` and `accept`
- every header the input binds
- the body, normalized by media type: JSON and CBOR as sorted data, form bodies as sorted parameters, XML without
  whitespace between elements, `aws-chunked` frames decoded

Signing, user-agent, invocation-ID, retry, length and checksum headers belong to the runtime and have their own
conformance suites, so they are not compared here. A response is compared as the outcome, the deserialized output
member, and for errors the exception class simple name, error code and status code.

## Intentional deltas: `intentional-deltas.json` (format version 1)

```json
{
  "formatVersion": 1,
  "deltas": [
    {
      "id": "unique-record-id",
      "case": "<service>/<Operation>/<CATEGORY> glob, * matches any run",
      "aspect": "aspect glob, e.g. request.header:content-type or response.exception",
      "manifestEntry": "Parity Manifest entry ID, e.g. behavior:telemetry.user-agent",
      "rule": "name of the tools/parity-manifest classification rule that classifies manifestEntry",
      "rationale": "why V3 intentionally differs"
    }
  ]
}
```

A record is valid only if all of these hold:

- the named rule exists in `classification-rules.json` and classifies `ADAPTED` or `EXCLUDED`
- the rule's `match.id` glob matches `manifestEntry`
- no more specific matching rule classifies the entry otherwise
- the rationale is not blank

Any V3/V2 difference that no valid record covers is unclassified and fails its case. A Parity Manifest known gap
(for example `behavior:s3.bucket-addressing`) is not a classification, so a defect must never be recorded here.
Task 18.3 (Property 62) builds on this format.
