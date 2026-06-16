# Removing the codegen IR (`IntermediateModel`) — teardown plan

**Status:** scoping document. No IR-removal code written yet. Numbers below are measured against the
current tree (5 benchmark services; full SDK is larger but structurally identical).

## TL;DR

Removing `IntermediateModel` is **not** deleting a class — it's replacing the spine of v2's code
generator. The IR is a *computed, v2-resolved view* that 109 of 157 generators read through ~360
distinct accessors.

Under the project's fixed constraints (generated code frozen, build inputs frozen, codegen layer
disposable — see next section) this is a **pure internal refactor with an exact oracle**: regenerate,
diff against the committed output, require an empty diff. Multi-week and mechanical, low-risk per stage.
The payoff is enabling (Smithy as the genuine internal model), not functional — behavior is unchanged by
constraint.

The Smithy front-end already landed (C2J→Smithy→IR, byte-identical, committed). That's the safe
precondition: the model *source* is already Smithy. This plan removes the IR *consumer* layer.

## What the IR actually is (why this is hard)

`IntermediateModel` is not raw model data — `IntermediateModelBuilder` (324 LOC) + 17 customization
processors compute a v2-specific resolved view:

- **v2 naming** (`DefaultNamingStrategy`): member → fluent getter/`hasX`, shape → request/response
  class, enum constant names, builder/setter types (`variableSetterType`, `simpleType`, …).
- **Protocol/marshalling resolution**: `ShapeMarshaller`/`ShapeUnmarshaller`, `ParameterHttpMapping`,
  payload/streaming flags, `marshallLocationName`.
- **Per-operation derived data**: `methodName`, `syncReturnType`, `authenticated`, `paginated`,
  `fullRequestTransformPackageName`, exception lists.
- **17 customizations** that MUTATE the model (shapeModifiers, renames, substitutions, S3 host-prefix,
  event-stream rewrites, …). These MUST still run — they are product behavior, not legacy cruft.

A Smithy model has none of these; it has shapes + traits. So removal requires a component that computes
this view from Smithy. That component is the real work.

## Blast radius (measured)

| Scope | Files |
|---|---|
| Total repo refs to `IntermediateModel`/`model.intermediate` | **274** |
| codegen/ main | 190 |
| codegen/ test | 56 |
| test/sdk-standard-benchmarks | 18 |
| test/protocol-tests-core | 6 |
| codegen-c2j-to-smithy | 3 (the new front-end) |
| codegen-maven-plugin | 1 |

Inbound references to the hot IR types: `IntermediateModel` 134, `OperationModel` 64, `ShapeModel` 58,
`MemberModel` 48, `Metadata` 17.

### Generator families (`poet/`, 157 files, 109 IR-coupled) — migration units

| family | files | IR-coupled | notes |
|---|---|---|---|
| model | 26 | 25 | POJOs/builders/enums — **start here** (smithy-java types-mode already does this, 1.11× proven) |
| client | 20 | 20 | sync/async clients + builders — **net-new from Smithy**; types-mode emits NO client |
| auth | 18 | 13 | auth scheme generation |
| rules2 | 36 | 1 | endpoint rules — **already nearly IR-free** |
| rules | 13 | 13 | older endpoint rules |
| transform | 8 | 5 | marshallers/unmarshallers |
| eventstream | 8 | 8 | |
| waiters | 7 | 6 | |
| builder | 6 | 6 | |
| paginators | 5 | 5 | |
| common | 3 | 3 | |
| endpointdiscovery | 2 | 2 | |

Key gap: the existing no-IR path (`JavaCodegenPlugin` types-mode) generates **only model types (29
files/service)**. v2 emits **~718 files/service** — clients, builders, `Default*Client`, transforms,
auth, endpoints, service config. None of those have a Smithy-direct generator today.

## Constraints (fixed) → one plan, not a fork

Three hard constraints, stated by the project owner:

1. **Generated code: backward-compatible (frozen).** The emitted v2 clients/models/builders must not
   change. Verification target: **byte-identical generated output**.
2. **Build inputs: backward-compatible (frozen).** Maven plugin goals/params, `customization.config`
   schema, C2J `service-2.json`, and Smithy model files all keep working unchanged.
3. **Codegen layer: fully disposable.** The IR, all 157 `poet/` generators, the 17 customization
   processors — anything *between* input and output may be replaced/deleted freely.

These collapse the earlier "two strategies" into one. Shipping smithy-java's own client generation is
**ruled out** — it changes the generated client/builder/auth API, violating constraint 1. So the only
admissible plan: replace the codegen middle (remove the IR) while holding the generated tree
byte-identical.

**Consequence — this is a pure internal refactor.** With inputs and outputs both frozen, the
byte-identical parity harness *is the complete spec*: every step has an exact oracle (regenerate, diff;
empty diff ⇒ correct by construction). No API reasoning, no customer impact, no need to preserve any
generator's internal structure. The payoff is architectural/enabling (Smithy becomes the genuine
internal model, unblocking future Smithy-native features), **not** functional — by constraint, behavior
is unchanged.

## Migration order — each stage independently shippable + verifiable

0. **Precondition (DONE):** Smithy is the IR front-end, byte-identical. ✓
1. **Introduce the Smithy-backed view, dual-run.** Build `CodegenView` computing the v2-resolved data
   (reuse `DefaultNamingStrategy` + the 17 processors, fed from the Smithy model). Don't migrate any
   generator yet — just stand it up and assert it reproduces the IR field-for-field (extend
   `SmithyIrParityRunner`).
2. **model/ (26 files).** Migrate POJO/enum/builder generators to the view. Verify generated sources
   byte-identical for all 5 protocols + serde verifiers still pass.
3. **transform/ (8), then rules/rules2 (49).** Rules are nearly IR-free already — cheap win.
4. **client/ (20) + builder/ (6) + auth (18).** The hard core — sync/async clients, config, auth scheme.
5. **eventstream/waiters/paginators/paginators/common/endpointdiscovery (25).** Tail.
6. **Delete `model/intermediate/` (22 classes) + `IntermediateModelBuilder` + the `C2jModels`→IR seam.**
   Remove `SmithyToServiceModel` IR-adapter once nothing builds an IR. Update the 56 codegen tests +
   ~25 downstream test refs.

## Verification harness (already exists, extend it)

- `SmithyIrParityRunner` — byte-identical IR check (now: view-vs-IR field check).
- Regenerate all 5 services through the maven plugin; **diff generated Java is the gate** (we already
  proved 718/718 byte-identical across the front-end swap — same technique per stage).
- `ConvertedModelSerdeVerifier` + serde verifiers — wire-byte correctness.

## Rough estimate

- Stage 1 (the view): the highest-uncertainty piece; ~1 week to reproduce `IntermediateModelBuilder`'s
  computed fields from Smithy with parity.
- Stages 2–5 (generator migration): ~109 generator files, mechanical once the view exists, but
  client/auth are intricate. ~2–4 weeks.
- Stage 6 (deletion + tests): ~1 week.
- **Total: ~4–6 engineer-weeks**, staged so the build stays green and output stays byte-identical
  throughout. No customer-visible change (Strategy A).

## Stage 1 findings (measured via codegen-diff.sh on 6 real services, all protocols)

The before/after byte-diff harness is built + validated (same code ⇒ IDENTICAL; perturbed ⇒ caught).
Running it on REAL services (not just the 5 benchmark ones) corrected an earlier over-claim: the Smithy
front-end is **NOT yet byte-identical to legacy C2J** on real services. Diffing legacy-C2J vs
Smithy-front-end generated code (s3, dynamodb, sqs, cloudwatch, polly, route53):

- **~1131 model files differ, almost entirely DROPPED DOCUMENTATION.** The converter does not carry
  C2J `documentation` → generated javadoc degrades to "Returns the value of the X property". (Confirmed:
  with comments + flattened excluded, AddPermissionRequest has only 2 diff lines.)
- **`flattened` not carried.** C2J list `flattened:true` → missing `.isFlattened(true)` in generated
  marshalling metadata. A wire-correctness bug, not cosmetic.
- **Event streams dropped (11 files).** C2J `eventstream:true`/`event:true` shape flags aren't mapped to
  Smithy (`@streaming` union + event traits), so event-stream operations lose their response handlers /
  visitor builders / event marshallers (polly StartSpeechSynthesisStream, s3 SelectObjectContent).

None handled by the converter today (`documentation`/`flattened`/`eventstream` = 0 refs). All three are
the SAME class of "preserve what C2J carries" gap already closed for metadata/min/max/httpChecksum — they
just weren't exercised by the 5 benchmark services. **These must be fixed (forward converter + inverse
adapter) before the IR-removal refactor — they are front-end completeness, a precondition, not part of
the refactor.** Each is verifiable by re-running codegen-diff.sh until legacy-vs-smithy is empty.

### Front-end gap closure progress (legacy-vs-smithy diff on the 6-service sample)

| state | differing paths (6-service sample) |
|---|---|
| start | 1230 |
| + documentation (@documentation at service/op/shape/member) | 272 |
| + flattened (@xmlFlattened at shape + member) | 219 |
| + event streams (eventstream/event marker traits) | 196 |
| + uri-label locationName (marker for divergent @httpLabel names) | 177 |
| + list/map element locationName (@xmlName/@jsonName on collection members) | 127 |
| + idempotencyToken (@idempotencyToken) + shape-level timestampFormat | 116 |
| + sensitive (@sensitive at shape + member) | 92 |
| + exception error block (verbatim C2J error: code + httpStatusCode marker) | 92* |

\* error-block fix made httpStatusCode byte-match on every exception (the dominant content fix) but the
path count held at 92 because those files have *other* remaining diffs too. Cumulative: **1230 → 92,
~92.5% closed.** Each gap = same "preserve what C2J carries" pattern (forward trait + inverse read-back),
verified by re-running codegen-diff.sh.

REMAINING ~92 (the hard tail) — ALL FOUR CLOSED. Final state: **0 differing paths** across all 6 sample
services (s3/dynamodb/sqs/cloudwatch/polly/route53), all 5 protocols, including S3's heavy customizations.
codegen-diff.sh legacy-vs-smithy = IDENTICAL; SmithyIrParityRunner = PARITY OK (all 5 benchmark services).

## FULL FLEET PARITY: 422/422 services byte-identical IR (100%)

The `SmithyIrParityRunner` sweep over **every** service-2.json in the repo (422 services) now reports
**422 OK / 0 DIFF / 0 ERROR** — the C2J→Smithy→IR path produces a byte-identical `IntermediateModel`
to the legacy direct C2J→IR path for the entire AWS surface. Progression this session: 325 OK / 95 DIFF
/ 2 ERROR → 355/65/2 → 420/0/2 → **422/0/0**.

Final cluster of gaps closed (each = forward trait + inverse read-back, re-measured by the full sweep):
- **operation-level `deprecated`(+message)** — was only handled at shape/member level (appconfig et al.).
- **verbatim operation `errors[]`** (`com.amazonaws.c2j#errors`) — Smithy's error set dedupes/reorders;
  v2's IR keeps the exact declared sequence INCLUDING duplicates (appsync CreateApiKey lists
  LimitExceededException twice). Marker preserves order + dupes + ref-level doc.
- **document type** — C2J models the open document as a memberless `structure` with `document:true`;
  mapped to Smithy's native `DocumentShape` ↔ IR's `software.amazon.awssdk.core.document.Document`
  (variableType/marshallingType DOCUMENT). Cleared the single biggest cluster (18 services:
  bedrockruntime, cognitoidentityprovider, glue, securityhub, …).
- **shape-level `wrapper:true`** (`com.amazonaws.c2j#wrapper`) — query/rest-xml result wrapping
  (docdb/elasticache/neptune/rds/redshift, ~100 shapes).
- **ref-level `documentation`** on operation `output`/`input` refs (`com.amazonaws.c2j#outputDoc` +
  fold into input-meta) and on `errors[]` entries — distinct from the target shape's own doc; drives
  the IR's returnType / @throws javadoc (mq/kafka/amplifybackend/apigatewayv2, ~300 ops).
- **boolean `httpChecksumRequired`** → `@httpChecksumRequired` (s3control, 37 ops; distinct from the
  httpChecksum block).
- **list/map element `jsonvalue`** — was carried for struct members but not collection elements
  (pricing PriceListJsonItems).
- **empty `required:[]`** — now emitted as `[]` not null (pinpointsmsvoice).
- **shape-level `synthetic:true`** (`com.amazonaws.c2j#synthetic`) — SDK-synthesized event-stream
  exception members (polly/sagemakerruntime).
- **service/shape id collision** — a serviceId matching a modeled shape name (Budgets service has a
  `Budgets` list shape) clobbered the shape; service shape id now suffixed `C2jService` (lossless —
  real serviceId round-trips via the metadata trait; adapter finds the service via getServiceShapes()).

Two prior "ERROR" services were a **harness gap, not converter bugs**: the parity runner built
`C2jModels` without the sibling models, so services whose customization.config references endpoint
parameters (s3) NPEd on a null endpointRuleSet on BOTH paths. The runner now loads waiters / paginators /
endpoint-rule-set / endpoint-tests exactly like the maven plugin (`buildIr` helper). Added a `--dump
<svc> <dir>` mode for full-IR structural diffing.

Gaps closed to reach byte-identical (each = forward trait + inverse read-back, re-measured):
documentation; flattened; eventstream/event; uri-label locationName; list/map element locationName;
idempotencyToken; shape-level timestampFormat; sensitive; exception error block (code+httpStatusCode);
shape ORDER (sort by name = C2J alphabetical file order — fixed the LimitExceededFault/Exception dedup,
which was an order-dependent "last-class-name-wins" in AddExceptionShapes, NOT a separate bug);
awsQueryCompatible/protocolSettings/resultWrapped metadata; xmlAttribute; deprecated(+message);
member xmlNamespace; requestcompression; endpointdiscovery+endpointoperation; retryable(throttling);
eventpayload/eventheader; streaming(+requiresLength); shape-level xmlNamespace (drives XmlAttributesTrait);
clientContextParams; staticContextParams/operationContextParams (as jackson-jr JrsValue TreeNodes, NOT
databind — the endpoint codegen casts to JrsBoolean/JrsString/JrsArray); member contextParam;
endpoint.hostPrefix; authtype; unsignedPayload; ContentLength kept (IR path doesn't validate; reserved
headers bound).

The 4 specifically-named concerns resolved: (1) requestcompression — verbatim op marker; (2) event-stream
client wiring — fell out once eventstream/streaming/eventpayload + member xmlNamespace were carried;
(3) cloudwatch dup-shape — was the missing error.code/httpStatusCode + shape ordering, now identical;
(4) line-wrapping — was secondary to trait-count, gone once traits matched.

## Expanded coverage: 8 feature-diverse services (ec2/glacier/transcribestreaming/cloudfront/sts/iam/
apigateway/kinesis, ~8000 files, incl. the unique ec2 protocol)

Ran the harness on a second, feature-diverse batch. Result after fixes: only the cloudfront
paginator false-positive remains (see below). One real gap found + fixed:
- **ec2 member `queryName`** (the ec2 protocol's wire name, distinct from locationName — e.g.
  Ipv6Addresses has locationName "ipv6AddressesSet" but queryName "Ipv6Addresses"). Carried via a
  com.amazonaws.c2j#queryName marker -> Member.queryName. ec2 now byte-identical.

KNOWN FALSE POSITIVE (not a smithy-path gap): cloudfront generates 0 paginators under the
`-Dawssdk.codegen.legacyC2jIr=true` ESCAPE HATCH but 186 under the smithy path. Verified this reproduces
on the COMMITTED code (before any uncommitted changes), so the escape hatch itself is buggy for
cloudfront — the shipping smithy path (186, matching real cloudfront) is CORRECT. The diff oracle's
"legacy" baseline is therefore unreliable for this service; the 2 cloudfront diffs are oracle noise, not
a front-end defect. (Root cause in the hatch not chased — debug-only flag.)

- **documentation: DONE.** Forward maps C2J `documentation` → `@documentation` (service via ServiceShape,
  op via OperationShape, shape via `Shape.shapeToBuilder().addTrait()`, member via memberTraits); inverse
  reads it back at all 4 levels. Closed ~78% of the gap.
- **flattened: DONE.** C2J `flattened` (shape list/map + member) ↔ `@xmlFlattened`. AddPermissionRequest
  etc. now byte-identical.
- **event streams: REMAINING (219 paths).** All remaining diffs trace to event streams: 13 dropped files
  (response handlers / visitor builders / event marshallers) + ~206 client/transform files that *import*
  them (e.g. DefaultS3AsyncClient wires SelectObjectContentEventStream). Need C2J `eventstream:true` /
  `event:true` → Smithy `@streaming` union + event member traits, both directions. s3(119)+route53(49)+
  polly(26) dominate.

## Recommendation

Do Stage 1 as the next concrete step — it's the make-or-break: if a Smithy-backed view can reproduce the
IR field-for-field with parity, the rest is mechanical migration. If it can't (some computed field has
no Smithy source), that surfaces immediately and reshapes the estimate before weeks are spent.
