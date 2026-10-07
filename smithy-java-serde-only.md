# Serde-only: v2's pipeline, smithy-java's codecs

Branch: `smithy-java-serde-only`. smithy-java 1.6.1 / smithy 1.73.0 / AWS SDK for Java v2 2.46.11-SNAPSHOT.
Scope: **DynamoDB (awsJson 1.0, sync and async) and S3 (rest-xml, sync and async)**.

The full bridge (`smithy-java-bridge-alexwoo-full`, inventoried in `compatability_issues.md`) replaced the v2
request pipeline with smithy-java's `Client`. That reaches further than it needs to: 17 sections of that
ledger are about endpoints, identity, signing, retries, interceptors, timeouts and metrics — none of which is
what makes serde fast. This branch keeps **all** of that v2 code and replaces exactly two things per call:

```
                           stock v2                          this branch
  marshall request    *RequestMarshaller(protocolFactory)   smithySerde.marshaller(XOperation.instance())
  send                        ... unchanged: endpoint rules, SigV4, retries, interceptors, metrics ...
  unmarshall success  protocolFactory.createResponseHandler smithySerde.responseHandler(XOperation.instance())
  unmarshall error            ... unchanged: v2's error response handler ...
```

Everything else a call does is byte-for-byte the code that ran before, because it *is* that code.

## How it is wired

`core/protocols/smithy-java-serde` (`software.amazon.awssdk.protocols.smithyjava`) holds the two protocol
implementations and the schema adapters. It depends on smithy-java's serialization modules only — `core`,
`io`, `json-codec`, `xml-codec`, `http-binding`, `http-api` — and on no part of smithy-java's client,
transport, auth, endpoint or retry stack.

A generated client for a service with `generateSmithyJavaSerde` gets one extra field, set once in its
constructor:

```java
// DefaultDynamoDbClient (generated)
this.smithySerde = SmithyJavaSerde.isEnabled()
        ? SmithyAwsJsonProtocol.builder().clientConfiguration(clientConfiguration)
                .contentType("application/x-amz-json-1.0").targetPrefix("DynamoDB_20120810")
                .serviceNamespace("com.amazonaws.dynamodb").hasAwsQueryCompatible(false).build()
        : null;
```

and each operation picks its marshaller and handler from it, falling back to v2's:

```java
HttpResponseHandler<GetItemResponse> responseHandler = (smithySerde != null
        ? smithySerde.responseHandler(GetItemOperation.instance())
        : protocolFactory.createResponseHandler(operationMetadata, GetItemResponse::builder));
...
    .withMarshaller(smithySerde != null ? smithySerde.marshaller(GetItemOperation.instance())
                                        : new GetItemRequestMarshaller(protocolFactory))
```

`-Daws.smithyJavaSerde=false` takes the v2 branch. The v2 marshallers and handlers are still generated and
still reachable, which is what lets one jar measure both and lets the differential tests run both clients in
one JVM.

The shapes are unchanged from the full-bridge work: a generated v2 POJO *is* a smithy `SerializableStruct`
and its builder a `ShapeBuilder`, with a static `$SCHEMA` built from `SDK_FIELDS` by `SdkSchemaFactory`, so
the codecs read from and write into v2's own objects with no intermediate document. The generated
`ApiOperation` per operation carries the operation, input and output schemas and, for rest-xml, the `@http`
trait. Its error registry is empty: errors are v2's.

Operations that stay on v2's serde: event streams for both services, and for awsJson any streaming
operation. S3's streaming operations (`GetObject`, `PutObject`, `UploadPart`) do use smithy-java — the body
travels beside the shape in v2's `RequestBody`/`AsyncRequestBody` as it always did, and only the
headers/URI/XML go through the codec.

## What this costs and buys

Measured in `test/smithy-java-serde-benchmarks` (JMH, in-process canned HTTP, the project's four standard
DynamoDB cases; see that module's README for the method and the caveats):

| | serialize | deserialize | whole call |
|---|---|---|---|
| smithy-java, default JSON provider | 33–54% cheaper | 55–62% cheaper | 1–37% cheaper |
| smithy-java, native JSON provider | 60–69% cheaper | 62–66% cheaper | 12–53% cheaper |

Two results worth knowing before quoting the win:

- **Allocation per call rises** (+9% on the small cases, +24% on `batchGet`) even though serde itself
  allocates less. smithy-java's `Codec` deserializes from a `byte[]`/`ByteBuffer` and offers no
  `InputStream` entry point, so a response body is read whole into memory where v2 streams it. On a 50 KB
  `BatchGetItem` response that extra copy is also why its wall-clock win is only 1%.
- **An empty response body is slower** (`{}`: +87% time, 3× allocation). Standing up a smithy-java
  deserializer does not amortize over two bytes.

## Correctness bar: differential tests

`SmithyJavaSerdeEquivalenceTest` (one per service) runs the same calls through a client on each serde,
against the same canned responses, and requires byte-identical requests and equal responses — equal by
`equals`, by `toString`, and in their response metadata, so a difference in a field that nothing prints
still fails. S3 compares the request *after the whole pipeline has run on it*, so header case, query
encoding, checksums and the signed body are all in scope. `SmithyJavaSerdeAsyncEquivalenceTest` does the
same for the async client over a real Netty connection to WireMock, including a modeled error.

Every item below was found by those tests. All but the last two are fixed on this branch.

### Fixed: silent wrong bytes

1. **`@jsonName` was ignored, so DynamoDB's `NULL` went out as `NUL`.** smithy-java's own awsJson protocol
   sets `useJsonName(false)` — correct for a generated smithy client, where the member name *is* the wire
   name. It is wrong here: `SdkSchemaFactory` names members after v2's Java-facing member name, which
   customizations rename (`AttributeValue`'s `NULL` member is `NUL` in v2), and records the wire name as
   `@jsonName`. Ignoring it sent `{"NUL":true}`, which DynamoDB rejects, and dropped `NULL` from every
   response. `SmithyAwsJsonProtocol` sets `useJsonName(true)`.

2. **Epoch-second timestamps were spelled differently.** smithy-java writes a `double`:
   `1700000000` for a whole second, `1.700000000123E9` otherwise. v2 writes millisecond-precision plain
   decimal: `1700000000.000`, `1700000000.123`. Same instant, different bytes — and different bytes mean a
   different `Content-Length` and a different payload hash. `V2EpochSecondsFormatter` is the codec's default
   timestamp format.

3. **A map entry with a null value produced invalid JSON.** v2 omits the entry; smithy-java wrote the key
   with no value (`{"a"}`). A null *list element*, meanwhile, v2 writes as `null` — and writing nothing
   silently shortened the list. `SdkPojoSerializer` now follows v2 on both.

4. **DynamoDB attribute names over 50,000 characters failed the call.** smithy-java's shaded Jackson 3 is
   built once in a static initializer with Jackson's default `StreamReadConstraints`, which cap an object key
   at 50,000; DynamoDB allows 65,535 and v2 raises the cap to exactly that. There is no per-codec setting, so
   `SmithyJsonDefaults` raises Jackson's process-wide default before the provider's factory exists.
   (`LongAttributeNameTest` is the test that already held this behavior.)

5. **Every S3 bucket-level request gained a trailing slash, and every valueless query parameter gained
   `=`.** S3's customizations strip `{Bucket}` from the modeled URI, leaving patterns with no path at all
   (`?list-type=2`); smithy requires a pattern to start with `/`, so the generated `@http` trait has one
   added, turning `GET /bucket` into `GET /bucket/`. Separately, v2 spells a literal parameter with no value
   as a null value (`?tagging`) where smithy writes `?tagging=`. The marshaller takes both from v2's own
   request URI, which codegen now passes it.

6. **An XML payload's root element was named after the target shape, not the member.** v2 names it from the
   payload member's `locationName`. They usually agree — `PutObjectTagging`'s `Tagging` member targets the
   `Tagging` shape — but `PutBucketLifecycleConfiguration`'s `LifecycleConfiguration` member targets
   `BucketLifecycleConfiguration`, and S3 answers `MalformedXML` to `<BucketLifecycleConfiguration>`.

7. **`xsi:type` was written as a child element.** v2 carries `XmlAttributeTrait` on the member;
   `SdkSchemaFactory` did not translate it to `@xmlAttribute`, so S3's `Grantee` sent
   `<xsi:type>CanonicalUser</xsi:type>` where an attribute is required, and read nothing back — every ACL
   grant lost its grantee type, in both directions.

8. **CR and LF in XML text were written raw.** An XML parser normalizes a raw CR to LF, so an object key
   containing one would reach S3 as a different key; S3's documentation tells clients to escape it, and v2
   does (`&#x0D;`, `&#x0A;`). `V2XmlBodyRules` applies v2's escaping, and its XML declaration, to the
   document smithy-java produced.

9. **A duplicate `Content-Type` went out on `CopyObject`.** The operation models a `Content-Type` header and
   also has an XML body, so smithy-java's `application/xml` and the caller's value were merged into one
   multi-valued header. v2 lets the modeled value win.

10. **Response header members could not be read faithfully.** Three separate failures, all silent:
    `GetObjectResponse` binds both `Expires` (timestamp) and `ExpiresString` to the `Expires` header and
    smithy-java indexes one member per header, so one was always null; `x-amz-meta-Color` came back as
    `color` because smithy-java's `HttpHeaders` lower-cases names, and user metadata keys are data;
    and a field marked `ignoreDataTypeConversionFailures` (S3's `Expires`, which the service has been known
    to fill with unparseable values) failed the call instead of being left null. `V2HeaderBindings` reads
    header- and status-code-bound members from `SdkField`s with v2's own rules; smithy-java keeps the body.

11. **A missing required member was sent instead of refused.** smithy-java's binding layer leaves an unset
    member out and carries on. An `UploadPart` with no `uploadId` went out as a plain `PUT`, which S3 answers
    by *overwriting the object*. v2 throws `Parameter 'uploadId' must not be null`, and so does the rest-xml
    marshaller now.

12. **S3 responses carried the wrong request id in their metadata.** v2's two XML response handlers disagree
    with each other — the combined handler reads only `x-amzn-RequestId`, the streaming-output handler accepts
    any of v2's spellings — and both are in play. The map is part of `AwsResponse.equals`, so getting it wrong
    makes every S3 response unequal to the one stock v2 returns while printing identically.

13. **An XML response whose root element is not the output shape's name failed the call.** S3 answers
    `ListObjectsV2` with `<ListBucketResult>` and `CreateMultipartUpload` with
    `<InitiateMultipartUploadResult>`; v2 never checks the name. The response codec sets
    `strictRootElement(false)`.

### Open: needs an upstream smithy-java change

14. **`BLOCKED` — a flattened list inside a flattened list's item loses data.** Reading an item structure
    that itself contains an `@xmlFlattened` list drops every member of that item after the inner list, *and*
    every item after the first. `ListObjectsV2` hits it through `Contents` (flattened) containing
    `ChecksumAlgorithm` (flattened): the response comes back with one object instead of 25, missing `Size`,
    `StorageClass` and `Owner`, while everything outside `Contents` — `Name`, `KeyCount`,
    `NextContinuationToken`, `CommonPrefixes` — reads correctly, which is what makes it silent. Isolated to
    `xml-codec` in a standalone reproduction with hand-built schemas, so it is not a schema-translation
    problem. Pinned by `listObjectsV2LosesMembersAfterNestedFlattenedList`. **This disqualifies smithy-java
    1.6.1 for S3 response unmarshalling.**

15. **`BLOCKED` — `@xmlNamespace` with a prefix writes no `xmlns:` declaration.** With `@xmlAttribute` fixed
    (item 7), `xsi:type` is written as an attribute but its namespace is never declared, so the document uses
    an undeclared prefix. v2 models the declaration as a plain attribute and writes both. The serializer owns
    an element's attributes, so this layer cannot add it. Affects `PutBucketAcl`/`PutObjectAcl`; pinned by
    `putObjectAclOmitsNamespaceDeclaration`, and it is why `AclTest.putBucketAcl_marshalling` fails with
    smithy-java serde on.

## What is *not* different, and why that matters

Nothing in sections 1-11 and 13-17 of `compatability_issues.md` applies to this branch. Errors and
exceptions, interceptors and plugins, retries and their classification, auth and credentials, endpoint
resolution and discovery, checksums, compression, metrics, timeouts, request-level overrides, async
cancellation and event streams are all v2's own code on v2's own path, reached in v2's own order. The
pipeline-shape incompatibilities that the full bridge had to work around — endpoint resolution after the
modify hook, per-attempt identity, the retry loop inside `deserialize`, an attempt ending at the headers —
do not arise, because the pipeline is not replaced.

What remains is the Java baseline: smithy-java requires **JDK 21**, and `core/protocols/smithy-java-serde`
and the two generated clients inherit that. That is unchanged from the full bridge and is still the single
largest obstacle to shipping any of this.
