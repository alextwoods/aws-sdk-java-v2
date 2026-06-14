# Bridge serde benchmark — design

Goal: measure what the **v2-API-over-smithy-java bridge** costs for request
serialization, head-to-head against **plain v2 marshalling**, in the same module / JVM /
JMH config / test cases as the existing v2 serde benchmarks.

## Why it belongs in this repo

v2's marshall benchmark builds its input as a **C2J-derived `SdkPojo`** via
`ShapeModelReflector` over a C2J `IntermediateModel` (see `RestJsonMarshallBenchmark`).
Those C2J types only exist here. A fair bridge must start from the *same* `SdkPojo` a real
shim would receive — so the benchmark lives next to v2's.

## The two contenders, same input

Setup (untimed, identical for both): build the v2 `SdkPojo` from the test case via
`IntermediateModel` + `ShapeModelReflector` (exactly as `RestJsonMarshallBenchmark` does).

- **v2 (baseline):** `marshaller.marshall(sdkPojo)` -> `SdkHttpFullRequest`.
- **bridge:** timed window =
  1. `SdkPojo` -> smithy `Document`  (generic walk of `sdkFields()`: `memberName()`,
     `marshallingType()`, `getValueOrDefault(pojo)`; recurse for `SDK_POJO`/`LIST`/`MAP`) —
     **the shim's real cost**, no per-shape code.
  2. `StructDocument.of(op.inputSchema(), doc, serviceId)` then
     `protocol.createRequest(op, inputStruct, context, endpoint)` — **smithy-java's
     schema-based serde**, the same timed call the smithy-java serde benchmark uses.

The delta (bridge - v2) is the true per-call cost of running v2's surface on the
smithy-java engine for serialization.

## smithy-java pieces (dynamic client path, from Maven Central 1.4.0)

- `DynamicClient.builder().model(model).service(id).build()` — loads the benchmark
  service's Smithy `model.json` (built under
  `AwsSdkPerformanceBenchmarkModels/build/smithyprojections/.../<proj>/model/model.json`).
- `client.getOperation(name)` -> `ApiOperation<StructDocument,StructDocument>`.
- `StructDocument.of(schema, document, serviceId)` builds the input shape.
- The protocol (`RestJsonClientProtocol`, etc.) `createRequest(...)` does the serialization.

## Scope for first cut

restJson1 serialize, the same 5 test case ids as `RestJsonMarshallBenchmark`
(`restJson1_CopyObjectRequest_Baseline/_M`, `restJson1_PutObject_S/_M/_L`). Same
`@BenchmarkMode(SampleTime)`, warmup 5x2s, measure 10x5s, fork 3. Emit via the existing
`JmhResultConverter` so it drops into `results/` and the aggregate as a `bridge` column.
Extend to the other protocols once the shape is proven.
