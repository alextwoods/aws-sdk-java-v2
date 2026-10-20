# schema-serde-source-compat

Source-compatibility fixtures for the schema-serde Prototype_Service_Set (Requirement 16.8). The fixtures are code
written against the Baseline_Build public API of CloudWatch, SQS, DynamoDB (with DynamoDB Streams), Secrets Manager,
Account, Route 53 and SNS, and of the Legacy_Document (`software.amazon.awssdk.core.document.Document`).

## Layout

- `src/test/java/.../sourcecompat/fixtures/`: the fixtures. They may use only APIs that exist in the Baseline_Build.
  Each one builds sync and async clients (region, credentials, override configuration, endpoint override, HTTP
  client, plugin), builds requests with builders and consumer-builder overloads, reads responses through their
  getters, calls `toBuilder`, catches modeled exceptions by type and reads `awsErrorDetails()`, and uses paginators
  and waiters where the service has them.
- `src/test/java/.../sourcecompat/*SourceCompatTest.java`: run each fixture once per client call against a client
  whose transport is replaced by `client-mock-plugin`, so no request reaches the network. The HTTP clients passed to
  the fixtures fail if called. DynamoDB Streams is still on the legacy route, so its test uses a stub HTTP client.
- `BaselineCompileTest`: compiles the fixtures with `javax.tools` against the Baseline_Build JARs alone, and checks
  that code using a feature-only API (`MetricAlarm.SCHEMA`) does not compile there.

The Maven build compiles the same fixture sources against the feature build.

## Baseline_Build

`BaselineCompileTest` reads the Baseline_Build that `test/size-report` uses: the merge base `fc8b74e9fe21`, built by

```bash
scripts/schema-serde/build-size-baseline.sh
```

into `~/.cache/aws-sdk-java-v2/size-baseline/fc8b74e9fe21/`. The test fails, naming the script, when that directory
is missing. Point it at another directory with `-Dsource-compat.baselineDir=<dir>`.

## Run

```bash
mvn clean install -pl :schema-serde-source-compat
```
