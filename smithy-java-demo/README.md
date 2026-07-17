# smithy-java DynamoDB demo

Proof-of-concept for the thesis in [`../RFC-smithy-java-runtime.md`](../RFC-smithy-java-runtime.md):
**the AWS SDK for Java becomes a thin shell over [smithy-java](https://github.com/smithy-lang/smithy-java)**,
which supplies the entire runtime — protocol, SigV4 auth, HTTP transport, schema-based
serde, endpoint rules, retries. No C2J anywhere.

Everything resolves from **Maven Central**:
- smithy-java runtime + codegen: `software.amazon.smithy.java:*:1.4.0`
- DynamoDB Smithy model: `software.amazon.api.models:dynamodb:1.0.12`
- smithy Gradle plugin: `software.amazon.smithy.gradle.smithy-base:1.4.0`

Requires **JDK 21** (smithy-java's baseline; the parent SDK was bumped to 21 to match).

## What's here

| File | Role |
|---|---|
| `smithy-build.json` | Drives codegen: service `com.amazonaws.dynamodb#DynamoDB_20120810`, protocol `aws.protocols#awsJson1_0`, transport `http-java`, `client` mode. |
| `build.gradle.kts` | Applies the smithy Gradle plugin, declares the codegen + runtime deps, wires generated sources/resources into the build. |
| `src/.../Main.java` | Builds the **generated** `DynamoDBClient` directly and runs createTable / putItem / getItem / listTables, with two interceptors registered. |
| `src/.../v2shim/V2Style.java` | A hand-written **AWS SDK v2-shaped** `DynamoDbClient` facade that delegates to the smithy-java client — the "shell over smithy-java" in miniature. |
| `src/.../interceptors/LoggingInterceptor.java` | A **native** smithy-java `ClientInterceptor` — shows the generated client takes interceptors via the inherited `Builder.addInterceptor(...)`. |
| `src/.../interceptors/V2ExecutionInterceptor.java` | A miniature of the v2 `ExecutionInterceptor` SPI (v2 hook names: `beforeExecution`, `modifyHttpRequest`, `afterExecution`). |
| `src/.../interceptors/V2InterceptorBridge.java` | **RFC §10.4 artifact:** adapts a v2 `ExecutionInterceptor` onto smithy-java's `ClientInterceptor`, so customer v2 interceptors keep working on the new engine. |
| `src/.../transport/V2TransportBridge.java` | **Transport bridge:** wraps an AWS SDK v2 `SdkHttpClient` as a smithy-java `ClientTransport`, so the smithy-java runtime sends over v2's HTTP stack. |

## Transport bridge (v2 `SdkHttpClient` → smithy-java `ClientTransport`)

`V2TransportBridge` implements smithy-java's `ClientTransport<HttpRequest, HttpResponse>`
and delegates `send(...)` to an AWS SDK v2 `SdkHttpClient`:

- smithy `HttpRequest` → v2 `SdkHttpFullRequest` + `ContentStreamProvider` (body buffered
  into a replayable provider, since v2 may re-read it across retries),
- `v2HttpClient.prepareRequest(...).call()`,
- v2 `HttpExecuteResponse` → smithy `HttpResponse.of(...)` (reusing
  `HttpMessageExchange.INSTANCE`),
- JDK/socket exceptions mapped via `ClientTransport.remapExceptions(...)`.

The demo wires it with v2's `UrlConnectionHttpClient` via `.transport(v2Transport)`. Every
request now prints proof it travels over the v2 stack:

```
  [v2-transport] POST http://localhost:8000/ via UrlConnection (UrlConnectionHttpClient)
```

This is the third of the RFC's three adapters (interceptors ✅, transport ✅, credentials
provider — TODO). Scope: synchronous `SdkHttpClient` only, with buffered (non-streaming)
bodies — an async bridge over `SdkAsyncHttpClient` and true streaming are the follow-ups.

## Interceptors (RFC §10.4: the v2 ⇄ smithy-java interceptor bridge)

Two interceptors are wired into the demo client, both via the generated builder's
inherited `addInterceptor(ClientInterceptor)`:

1. **Native** — `LoggingInterceptor` implements smithy-java's `ClientInterceptor` directly.
2. **Bridged v2** — `V2InterceptorBridge` wraps a v2-style `ExecutionInterceptor` and forwards
   each smithy-java hook to its v2 equivalent. The sample v2 interceptor adds an
   `X-Demo-Interceptor` HTTP header.

**Fidelity detail that matters:** v2's `modifyHttpRequest` runs *before* signing, so its
mutations are signed. smithy-java signs between `modifyBeforeSigning` and
`modifyBeforeTransmit`, so the bridge maps `modifyHttpRequest` → **`modifyBeforeSigning`**
(not `modifyBeforeTransmit`). The native interceptor's `readBeforeTransmit` (which runs
*after* signing) reads the header back and prints
`X-Demo-Interceptor=v2-bridge`, proving the v2-added header survived onto the signed,
transmitted request. (SigV4 signs all headers except a small denylist; this header is not
on it, so it is part of the signature.)

The DynamoDB client is generated at build time into
`build/smithyprojections/smithy-java-dynamodb-demo/source/java-codegen/` (368 model
classes + client + waiter + the compiled `.bdd` endpoint ruleset).

## Run it

Needs a local DynamoDB. Docker Desktop here is gated behind an org sign-in, so the demo
uses the standalone **DynamoDB Local** jar instead:

```bash
# 1. one-time: fetch DynamoDB Local (already done in .ddb-local/ if present)
mkdir -p .ddb-local && cd .ddb-local
curl -sL -o ddb.tar.gz https://s3.us-west-2.amazonaws.com/dynamodb-local/dynamodb_local_latest.tar.gz
tar xzf ddb.tar.gz && cd ..

# 2. start it (in-memory, port 8000)
java -Djava.library.path=.ddb-local/DynamoDBLocal_lib \
     -jar .ddb-local/DynamoDBLocal.jar -inMemory -port 8000 &

# 3. run the demos (JDK 21)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home
./gradlew run      # raw generated smithy-java client
./gradlew runV2    # AWS SDK v2-style facade over it
```

Override the endpoint with `-Dddb.endpoint=...` (defaults to `http://localhost:8000`).

## Expected output

```
== smithy-java DynamoDB demo (endpoint: http://localhost:8000) ==
  [native] readBeforeExecution   op=ListTables
  [v2-bridge] beforeExecution  op=ListTables
  [native] readAfterSerialization  request=ModifiableHttpRequestImpl
  [v2-bridge] modifyHttpRequest POST -> adding header X-Demo-Interceptor
  [native] readBeforeTransmit    (post-sign) X-Demo-Interceptor=v2-bridge
  [native] readAfterTransmit     response=HttpResponseImpl
  [native] readAfterExecution    error=none
  [v2-bridge] afterExecution   op=ListTables error=none
  ... (same hook trace repeats per operation) ...
[createTable] created table DemoSmithyJava
[putItem]    wrote {id=user-1, name=Ada Lovelace}
[getItem]    read  {id=user-1, name=Ada Lovelace}
[listTables] [DemoSmithyJava]
== done ==

== v2-style facade over smithy-java (endpoint: http://localhost:8000) ==
[v2.putItem] wrote {id=user-2, name=Grace Hopper}
[v2.getItem] read  {id=user-2, name=Grace Hopper}
== done — v2 API on top, smithy-java on the wire ==
```

## Error path (modeled exception deserialization)

`Main` also calls `getItem` on a non-existent table to exercise the **unhappy path** —
the riskiest serde route, since the error mapping is entirely schema-driven at runtime.
It deserializes into the generated typed exception:

```
[error]      caught typed ResourceNotFoundException: fault=CLIENT throttle=false msg="null"
```

Two findings fell out of this, both genuinely useful:

1. **aws-json error-type namespace bug in 1.4.0 (fixed in main, unreleased).** DynamoDB
   Local returns `__type = "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException"`,
   but the model's shape id is `com.amazonaws.dynamodb#ResourceNotFoundException` (no
   `.v20120810`). Published **1.4.0** does not strip the wire namespace, so registry lookup
   misses and the error falls back to a generic `CallException`. smithy-java commit
   `a279c2eed` ("Fix aws-json type deser") adds the `removeNamespaceAndUri` step that fixes
   exactly this — but it's 111 commits past the 1.4.0 tag and not on Maven Central. This
   demo therefore uses a locally-published **`1.4.1-local`** build (see `gradle.properties`).
   Switch back to `1.4.0` from Central once the fix ships.
2. **Error `message` casing (`msg="null"`).** The model member is lowercase `message`; the
   server sent `"Message"`. smithy-java matches the schema member name strictly, so the
   message didn't populate. The `message`/`Message` discrepancy is a known AWS quirk that
   the v2 SDK handles case-insensitively — a real, if minor, gap a production cutover must
   cover. (The exception *type*, fault, and throttle flag all deserialized correctly.)

### Rebuilding the local smithy-java (only needed for the error-message fix)

```bash
cd ~/projects/smithy-java
echo "1.4.1-local" > VERSION     # already done
./gradlew publishToMavenLocal -x test -x javadoc
```

## Notes / rough edges found during the spike

- **Static identity:** `IdentityResolver.of(creds)` does **not** work — `StaticIdentityResolver`
  reports the concrete class as its `identityType()`, but the SigV4 scheme looks up by the
  `AwsCredentialsIdentity` interface, so resolution misses. Use `AwsCredentialsResolver`
  (which pins `identityType()` to the interface) instead. This is a real papercut a v2
  credentials-provider bridge would need to handle.
- **Endpoint ruleset:** the generated builder unconditionally loads a compiled `.bdd`
  endpoint ruleset from resources, so codegen needs `aws-client-rulesengine` on the
  `smithyBuild` classpath and the generated `resources/` dir wired into the source set —
  even when you override the endpoint.
- **Waiters:** DynamoDB's model has waiters, so `client-waiters` is required on the
  `smithyBuild` classpath or codegen fails.
