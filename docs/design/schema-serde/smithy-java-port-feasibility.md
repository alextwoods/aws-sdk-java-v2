# Feasibility of porting smithy-java performance work with an agent (round 2: smithy-java 1.7.0 → 1.8.0)

Our schema serde runtime (`core/schema`, the codecs in `core/codecs`, `http-binding`, the client protocols) is a
Java 8 port of smithy-java 1.7.0 (commit `142eecf4d`). smithy-java keeps optimizing the same code. This report asks
whether we can keep taking those optimizations by having an agent port them. It covers one full round: the 40
upstream commits from `142eecf4d` to the 1.8.0 release `95df4c14f`, triaged, ported, verified, measured on a laptop
and reviewed by an agent. The metal measurement of the same round is running and is a placeholder here (section 7).

Sources. Unless a path is given, every number comes from the task directory
`.agents/tasks/smithy-java-port-round2/` (not committed): `triage.md`, `plan.md`, `port-queue.json`,
`ports/P1.md`–`ports/P5.md` and their `-review.md` files, `jmh/P<n>/compare.md`, `verification.md`,
`final-review.md`, `merge.md` and `metal-status.md`. The code is on this branch as 14 commits, `dee335ca13b` to
`a64bd7cb09b` (`merge.md`). The per-port notes were written before a rebase and cite the earlier shas; the table in
section 3 lists both.

## 1. Summary

**Verdict: feasible, with two limits.** The agent ported every upstream serde optimization that fits our
constraints, quickly and without behavior changes. But in this release most of upstream's measured serde gain sits in
code that a Java 8 port cannot reach, or that would need a change to V2's request lifecycle. So the mechanism works,
and the payoff per release depends on what upstream optimized.

- **Scope.** 6 of the 40 commits are serde performance work. The other 34 are endpoint rules (14, which V2
  generates as code instead), pipeline performance (7), benchmark harness and data (6), MCP, build and release (5)
  and correctness fixes (2). Section 2 has every commit.
- **Ported.** 4 of the 6 serde commits were ported in some form (P1, P2, P3, P5). The CBOR read part of `8c5cf2907`
  was ported as P4, measured, and reverted. Writing the code and tests took the agent 2.5 to 22 minutes per port, up
  to the code commit (section 3). The new code is 9 main files, +504/−104 lines, plus 416 test lines and a 355-line
  benchmark (`git diff --numstat fae161e1f79..a64bd7cb09b`).
- **Correctness.** Every gate passed on the first run: 923 protocol tests with the same 16 skipped IDs,
  `SerdeBenchmarkEquivalenceTest` 142/142 with no allowlist change, the zero-copy tests, `DynamoDbNewPathTest`,
  the architecture tests including the Java 8 API check, and the touched modules' unit tests with checkstyle and
  SpotBugs (`verification.md`). The final review approved the branch (`final-review.md`).
- **Laptop results** (laptop-grade, PRELIMINARY; section 3). One clear win: P5, event stream encode and decode,
  G = 0.876 (encode 0.829, decode 0.925). Three neutral: P1 0.998, P2 0.992, P3 0.999. One regression, reverted: P4
  1.017. G is the port / previous geomean over each port's targeted cases; lower is faster.
- **What did not port.** Upstream's largest stated serde numbers are on the write side of `8c5cf2907` (CBOR
  serialize `ShallowMap_M` −25.0%, `MixedItem_M` −11.2%, PR #1393). They need `String` internals through
  `sun.misc.Unsafe`, which Java 8 public API does not offer (FU-23). Those two numbers are x86; the same PR reported
  `ShallowMap_S` serialize +6–8% on Apple silicon, and our laptop and metal host are both ARM, so the gain behind
  FU-23 is not certain for us (`triage.md`, `8c5cf2907` row). P4 showed that a Java 8 substitute for the
  matching read-side intrinsic loses time. Output buffer pooling (`0e88c8e76`) would hand pooled arrays to V2 HTTP
  clients and interceptors, which is a request lifecycle change, not a codec port (FU-22).
- **Expected effect on the serde benchmark headline.** Small. P1 to P3 were neutral on the laptop, and P5 is outside
  the 71 serde cases, because none of them covers event streams. The metal round decides (section 7).
- **Where the time went.** Measurement, not porting. Each port took 49 to 269 minutes of wall clock with the
  recorded idle, sleep and reboot gaps removed (P1 179, P2 about 201, P3 about 269, P4 about 99, P5 about 49), and
  the code commit came within the first 2.5 to 22 of them. The rest went to shared checks, JMH rounds and reruns
  on a noisy laptop (section 3).

## 2. Triage of all 40 upstream commits

`git -C <smithy-java> log --oneline --no-merges 142eecf4d..95df4c14f` lists 40 commits (`setup.md`). The agent
checked each against its diff, message and, for the PRs in the 1.8.0 changelog, the PR description
(`triage.md`). The triage took about 16 minutes (`setup.md` written at 15:39:43Z, `triage.md` last written at
15:55:25Z on 2026-10-24).

Outcomes: **ported** (kept on this branch), **reverted** (ported, measured, removed), **follow-up** (applicable, not
ported, tracked as an FU in `.kiro/specs/schema-decoupling-serde/followups.md`), **not applicable** (no
counterpart in our code, or already there).

| sha | subject | category | applies to our code? | outcome | reason |
|---|---|---|---|---|---|
| `95df4c14f` | Bump version to 1.8.0 (#1422) | release | no | not applicable | Release commit. Its changelog is the PR index the triage used |
| `d62ebebbe` | Ignore the body of an HTTP notification response | MCP | no | not applicable | MCP server, not ported |
| `8e892b96c` | Add schema extensions for event-stream serde (#1380) | serde perf | yes | **ported (P5)** | Member bindings precomputed per schema. Our pooled body path kept instead of upstream's `serialize()` plus copy |
| `4a8bea126` | Coerce untyped numbers to modeled types in StructDocument | correctness | already in our port | not applicable | Our `StructDocument.convertDocument` already coerces per numeric type |
| `70c0f918e` | Bump the gradle group with 12 updates | build | no | not applicable | Dependabot |
| `4bb3eaad3` | Skip defaulted builtins during register filling | endpoint rules | no | not applicable | V2 generates endpoint rules code; the rules engine is not ported |
| `9341050c4` | Reduce rules engine node compilation overhead | endpoint rules | no | not applicable | Same |
| `ac39c17af` | Compile endpoint result plans lazily | endpoint rules | no | not applicable | Same |
| `8820b722c` | Project endpoint properties without materialization | endpoint rules | no | not applicable | Same; also touches `CompactStringAccess`, which is never ported |
| `b386e9aca` | Preserve substring equality semantics | endpoint rules (fix) | no | not applicable | Fix to `7aa78c168` |
| `7aa78c168` | Optimize compiled endpoint rule evaluation | endpoint rules | no | not applicable | Rules engine |
| `2ac9166c1` | Optimize endpoint URI templates | endpoint rules | no | not applicable | Rules engine |
| `be145fcab` | Validate bucket names eight bytes at a time too | endpoint rules | no | not applicable (listed in FU-23) | V2's equivalent is each service's generated `RulesFunctions`. The SWAR scan also reads `String` bytes through `CompactStringAccess` |
| `b11fd4417` | Validate host labels eight bytes at a time | endpoint rules | no | not applicable (listed in FU-23) | Same |
| `ec12fbba2` | Add a same-URL control to the endpoint cost decomposition | endpoint rules (benchmark) | no | not applicable | Benchmark only |
| `f12e11a6b` | Locate endpoint resolution cost, and stop reallocating the partition wrapper | endpoint rules | no | not applicable | Rules engine |
| `51bc74f04` | Benchmark endpoint params as a generated client supplies them | endpoint rules (benchmark) | no | not applicable | Benchmark only |
| `67e1f063c` | Avoid a list allocation for the single auth scheme of an endpoint | endpoint rules | no | not applicable | V2 endpoint codegen builds its own `Endpoint` |
| `473877a37` | Compile endpoint rules to node trees instead of interpreting bytecode | endpoint rules | no | not applicable | Rules engine |
| `ba1901282` | Add latest benchmark data | benchmark data | no | not applicable | Data; read as context only |
| `d3dd17843` | Add option to restrict the MCP protocol versions a server serves | MCP | no | not applicable | MCP server |
| `304e02511` | Fixes after rebase | serde perf (fix-up of `0e88c8e76`) | partial | **follow-up (FU-22)** | Goes with `0e88c8e76`. Its `discardBody` part does not apply: the V2 pipeline closes the response stream |
| `0e88c8e76` | Add output buffer pooling | serde perf | partial | **follow-up (FU-22; its `LazyXmlSerializer` fix is FU-24)** | We already removed the body copy upstream also removed. The rest leases pooled arrays to the request, which V2's `ContentStreamProvider` raw view would expose across requests |
| `c62499ffd` | Improve SigV4 performance | pipeline perf | concept only | **follow-up (FU-25)** | V2's signer is unrelated code, so this is a re-implementation (racecar F), not a port |
| `e839b1b49` | Forward remote MCP calls in the protocol the remote negotiated | MCP | no | not applicable | MCP server |
| `156e5939d` | Reduce token bucket lookup and debug logging overhead | pipeline perf | no | not applicable | V2's `tokenBucketForScope` is already a plain `get` on a pre-sized map |
| `2fd0f976f` | Keep schema member target checks small for JVM inlining | serde perf | yes | **ported (P1)** | Line for line |
| `2e6f15e60` | Optimize HTTP header updates and removals by isolating array compaction | pipeline perf | no | not applicable | V2 headers are a different structure (`LowCopyListMap`); racecar B1 covers V2's header copies |
| `0f0bedf59` | Improve perf of UserAgentPlugin | pipeline perf | partial | **follow-up (FU-25)** | The schema pipeline already builds the per-client part once (`UserAgentHeaderCache`) |
| `ebe122c96` | Only invoke interceptors which have an override | pipeline perf | concept only | **follow-up (FU-25)** | V2 calls every interceptor hook, default methods included |
| `1f68318ce` | Skip auth scheme property merge when the option carries no overrides | pipeline perf | partial | **follow-up (FU-25)** | Racecar E13 overlaps; `mergePreExistingAuthSchemeProperties` remains |
| `534de7824` | Cache resolved member targets to reduce recursive serialization overhead | serde perf | yes | **ported (P2)** | On our diverged `DeferredMemberSchema`, so it saves less than upstream's |
| `500cc0d5c` | Add baseline benchmark results from Feb 11 2026 | benchmark data | no | not applicable | Data |
| `298b5d799` | Skip retry logging work when debug logging is disabled | pipeline perf | partial | **follow-up (FU-25)** | V2 already defers the `String.format`, but still does the token bucket lookup and lambda capture |
| `d5bbff7be` | Normalize Json in e2e-benchmarks too | benchmark harness | no | not applicable | Our harness is `test/sdk-standard-benchmarks` |
| `83331aaf1` | Simplify e2e-benchmark harness | benchmark harness | no | not applicable | Same |
| `057770a2c` | Ignore the e2e fixture certs from the repo root | benchmark harness | no | not applicable | Same |
| `8f038fd5a` | Add cross-SDK e2e benchmark harness, fixture server and metal runner | benchmark harness | no | not applicable | Methodology reference only |
| `8c5cf2907` | Use the JDK countPositives intrinsic and the Latin-1 String constructor on ASCII string paths | serde perf | partial | **JSON and XML decode ported (P3); CBOR decode reverted (P4); CBOR and XML write follow-up (FU-23)** | The decode side has a Java 8 equivalent (the Latin-1 "hibyte" `String` constructor). The intrinsic and the write side need `String` internals |
| `eb23fa19f` | Fix identity resolver lookup ignoring user-supplied static identities | correctness | no | not applicable | V2 identity providers are a different design |

Counts: 4 commits ported (one of them partly reverted and partly a follow-up), 7 follow-ups (`0e88c8e76` and
`304e02511` in FU-22, five pipeline commits in FU-25), 29 not applicable (14 endpoint rules, 6 benchmark harness and
data, 5 MCP, build and release, `156e5939d`, `2e6f15e60`, `eb23fa19f` and `4a8bea126`). The summary table of
`triage.md` says "15 endpoint-rules commits"; its per-commit table has 14, which is the count used here.

Upstream gave a number for few of these commits. Of the 6 serde commits, only `8c5cf2907` has one (PR #1393). The
changelog describes the rest without numbers (`triage.md`, "claimed gain" column).

## 3. Per-port detail

### 3.1 Overview

| port | upstream | adaptation | agent time to code commit | whole port, wall clock with recorded gaps removed | upstream diff main / tests | our diff main / tests | laptop G (port / prev) | matched upstream's claim? | outcome |
|---|---|---|---:|---:|---|---|---:|---|---|
| P1 | `2fd0f976f` | mechanical (line for line) | about 6 min | 179 min (no gap recorded; 16:13Z to 16:49Z is not accounted for) | +12/−5 / +33/−0 | +11/−3 / +26/−4 | 0.998 | mechanism yes (JIT log); no upstream number | kept (neutral) |
| P2 | `534de7824` | mechanical, on diverged code | about 2.5 min | about 201 min (298 wall clock, less 97 min from the first confirm attempt at 21:06Z to the rebuild at 22:43Z, lost to a reboot) | +10/−1 / +19/−0 | +23/−3 / +24/−0 | 0.992 | smaller saving by design; no upstream number | kept (neutral) |
| P3 | `8c5cf2907` (JSON and XML decode) | mechanical, Java 8 fallback dropped | 22 min (the edit a few minutes; the rest module builds) | about 269 min (1,304 wall clock, less about 1,035 with the laptop asleep) | ported part: about 14 lines of `CompactStringAccess`, JSON +3/−2, XML +3/−6, one test | +56/−5 / +73/−0, then +18/−1 test follow-up | 0.999 | partly: mechanism yes, number not reproduced | kept (neutral) |
| P4 | `8c5cf2907` (CBOR decode) | redesign (no intrinsic on Java 8) | 4 min | about 99 min (289 wall clock, less an idle gap of about 190) | `CborReadUtil` +7/−2 plus most of `CompactStringAccess` +89/−8; no read-path unit test | +31/−0 / +143/−0, then reverted | 1.017 | no | reverted |
| P5 | `8e892b96c` | mechanical, five behavior-preserving deviations, new benchmark | 12 min (benchmark 5, port 7) | about 49 min (758 wall clock, less an idle gap of about 709) | +292/−117 / +165/−1, JMH +239 | +414/−93 / +276/−0, benchmark +355 | 0.876 | yes in direction; no upstream number | kept (win) |

Times are from the "Time" section of each `ports/P<n>.md`: start, code commit and end timestamps, with idle and
sleep gaps called out there. The gaps were periods when the session was not running or the laptop was asleep, not
agent work. Upstream
sizes are `git show --stat`/`--numstat` of the upstream commit; ours are `git show --numstat` of our code commit.

Commits on this branch (post-rebase sha, with the sha the per-port notes cite in parentheses):

| port | code and tests | docs | other |
|---|---|---|---|
| P1 | `dee335ca13b` (`1bbfae7b00b`) | `48aa8933712` (`5fb24b54735`) | `ccce84666ca` (`96eacb08575`): FU-22 to FU-25 |
| P2 | `508cab24208` (`af2be4784d1`) | `f6d2118a377` (`d57bd0f440f`) | |
| P3 | `5a6f0a6c451` (`4b48ce92a05`) | `36fc8782582` (`de8adf5e0be`) | `43c3bca24f3` (`7c2f55622da`): 0–255 test sweep from the P3 review |
| P4 | `7d090077a2c` (`fa5705a6b0a`) | `c4904d6861d` (`93872ba9887`) | revert `5af80c3c3d7` (`433e8311bb6`) |
| P5 | `cd581132efd` (`bfb02d4320a`) | `a64bd7cb09b` (`b8182a8a62d`) | benchmark `4c096e61f2a` (`fb35e4db779`), committed before the port |

### 3.2 How the laptop numbers were taken

These are **laptop-grade** numbers: an Apple M4 Pro, 14 cores, JDK 25.0.2, sometimes shared with other work. They
decide whether a port is kept, not what gain we report.

- JMH options for every port: `-f 2 -wi 3 -w 1s -i 4 -r 1s -prof gc`, from the plan. (The P1 step prompt suggested
  `-f 1 -i 3`; the agent kept the plan's settings, `P1.md`.)
- Jars: `prev` is the jar of the last applied port, `port` the jar with this port. The agent checked that only the
  intended classes differ between each pair for P2–P5; for P1 it compared the bytecode of `assertMemberTargetIs`
  only (`P1.md`).
- Rounds: at least five. P1 ran base then port in every round. From P2's confirm rounds on, prev and port ran back
  to back per benchmark set, the order was reversed in even rounds, the one-minute load average was logged each
  minute, and no build ran alongside. These rules were added to the plan after the P1 and P2 reviews.
- Statistic: per case, the p50 for `SampleTime` classes (`serde.*`) and the mean for `AverageTime` classes
  (`schema.*`), as a port / prev ratio per round, pooled with the median of the round ratios. G is the geomean of
  those per-case ratios over the port's targeted cases. The plan's original statistic (means, geomean of round
  ratios) is reported next to it.
- Gate: kept if G ≤ 0.99 or B/op falls by more than 16 B on a targeted case in every round, with no case above
  1.05 in every round. P1, P2, P3 and P5 could also be kept when neutral (0.99 < G ≤ 1.01, no B/op rise above 16 B in
  every round), because they keep upstream's code shape and change no behavior. P4 was experimental and needed
  G ≤ 0.99.
- Resolution: with the laptop under load (P1, one-minute load 6–125), single cases swung 0.4x to 6.3x between
  rounds, and the P1 review judged that the laptop could not show effects below about 5%. On quiet rounds the
  untouched control classes moved by 0.997 and 1.042 (P2) and 0.995 to 1.027 (P3), which bounds what a laptop G can
  resolve to a few percent.

### 3.3 P1: `assertMemberTargetIs` small enough to inline (`2fd0f976f`)

- **Upstream claim.** Keeps the success path under C2's 35-byte inlining limit, so generated deserializers can
  inline a check they call for every nested structure member. No number (PR #1400).
- **Adaptation.** Line for line: `final`, exception built in a static helper. The only difference is
  `Validate.paramNotNull` for `Objects.requireNonNull` (SDK convention). Bytecode 49 → 16 bytes (javap).
- **Tests.** The three upstream cases plus a null-target case (`SchemaTest`, 31 tests); `schema` 686 tests;
  `dynamic-client` 494 (1 skipped, as before); the shared checks (equivalence 142/142, protocol tests 923 with 16
  skipped, architecture tests 79 with 11 skipped).
- **Laptop.** 42 deserialization cases (28 `serde.*Unmarshall`, 14 `SchemaUnmarshall`), five rounds: G = 0.998;
  per class 0.978–1.006; plan statistic 1.085, dominated by load stalls. No case above 1.05 in every round, no B/op
  movement beyond noise.
- **JIT evidence.** `-XX:+PrintInlining` on `SchemaUnmarshallBenchmark` LARGE (CBOR, awsJson 1.0): 19 call sites
  `failed to inline: callee is too large` before, none after. The hot sites were already inlined before (C2 uses the
  larger `FreqInlineSize` for them), so only warm sites gain, which fits the neutral timing.
- **Matched upstream?** The mechanism, yes, measured. Upstream gave no timing number, and ours is neutral.

### 3.4 P2: cache the resolved target of a deferred member (`534de7824`)

- **Upstream claim.** Recursive serialization calls `memberTarget()` repeatedly; caching the resolved target removes
  a `SchemaBuilder.build()` call per access. No number (PR #1403).
- **Adaptation.** Mechanical, on code that had already diverged. Our deferred member never called `build()`: it reads
  the builder's volatile built schema and throws if the builder is unbuilt (task 1.11). The port adds the same cache
  with upstream's cache condition, so on our side it saves one volatile read per access. The agent checked that the
  plain-field race is safe (every field the cached schema exposes is final or a documented racy cache). Bytecode
  64 → 16 bytes.
- **Tests.** Upstream's resolution-tracking test, adapted because our member throws on an unbuilt builder, plus a
  repeated-throw assertion; `schema` 687 tests; `dynamic-client` 494; equivalence and protocol tests unchanged.
  The architecture tests were not rerun for P2, which adds no type, package or API (`P2.md`).
- **Laptop.** 28 cases (DynamoDB `PutItem`/`GetItem` marshall and unmarshall for awsJson 1.0 and CBOR, and
  `SchemaMarshall`/`SchemaUnmarshall` as a control that never reaches the class). Five quiet, paired confirm rounds:
  G = 0.992. Per class 0.971–0.989 on the DynamoDB classes, against 0.997 and 1.042 on the controls, so the gains are
  inside the noise. Five earlier rounds under other users' builds (load median 35, peak 252) gave 0.936 and did not
  reproduce. `rpcv2Cbor_GetItemOutput_L` allocated 100–773 B less in all ten rounds (of 83 KB); the port changes no
  allocation, so this is held for a 4-fork metal check.
- **JIT evidence.** `memberTarget` was inlined as hot in both codecs' `writeStruct` on both jars, so the change is a
  plain load instead of a volatile one, not new inlining.
- **Matched upstream?** The saving upstream removed does not exist in our code, so less was available. Neutral.

### 3.5 P3: Latin-1 String constructor on the JSON and XML ASCII decode paths (`8c5cf2907`, decode part)

- **Upstream claim** (PR #1393, x86). AWS JSON deserialize `GetItemOutput_M` −2.0% (generated codec) and −1.4%
  (generic); REST JSON deserialize −0.8% to +0.6%; REST XML ±1.6%.
- **Adaptation.** Mechanical, with one Java 8 change. New `AsciiStrings.fromAscii` always uses the deprecated hibyte
  constructor `new String(bytes, 0, off, len)` (Java 1.1 API). Upstream switches to the ISO-8859-1 constructor when
  compact strings are off, which it detects by reading `String.coder` through `sun.misc.Unsafe`; we cannot, so under
  the non-default `-XX:-CompactStrings` our path inflates with a scalar loop. The JSON hunks match upstream apart from
  the class name. The port also found that our XML reader had silently diverged from upstream (ISO-8859-1 since task
  7.4, no deviation row) and moved it back to upstream's form.
- **Tests.** `AsciiStringsTest` (4, later 5 with the 0–255 sweep from the P3 review); `codec-commons` 1071,
  `json-codec` 1154 (5 pre-existing skips), `xml-codec` 353; architecture tests; shared checks unchanged.
- **Laptop.** 33 targeted cases (`JsonRpc10`, `RestJson`, `RestXml`, `Query` unmarshall and `SchemaUnmarshall` for
  the six JSON and XML protocols), ten paired rounds: G = 0.999 (0.994 and 1.002 for the two halves). Per class: JSON
  RPC 1.0 0.996, REST JSON 0.996, REST XML 1.005, Query 0.974, `SchemaUnmarshall` 1.006. Query
  `GetMetricDataResponse_L` was faster in all ten rounds (median 0.964). The untouched CBOR control moved 1.011. Our
  `awsJson1_0_GetItemOutput_M` was 1.008, where upstream reported −2.0%.
- **JIT evidence.** Before, the ISO-8859-1 constructor called an 852-byte private charset constructor out of line;
  after, `fromAscii` and the 128-byte hibyte constructor inline at the hot JSON sites. In the XML reader the bigger
  inlined body stops `readStringAndConsumeEndTag` from inlining into 16 callers, so for XML the port moves a call
  rather than removing it.
- **Matched upstream?** Partly. The mechanism is visible; upstream's −2.0% did not reproduce within laptop noise.

### 3.6 P4: ASCII fast path for short CBOR text strings (`8c5cf2907`, CBOR read part), reverted

- **Upstream claim** (PR #1393, x86). `readTextString` 8.0 → 5.9 ns at 4 bytes, 9.2 → 7.5 at 16, 14.7 → 13.3 at
  128; end-to-end CBOR deserialize −0.9% to +1.6%. Upstream only takes the fast path when the JDK's `countPositives`
  intrinsic is reachable, and otherwise skips the check to "avoid an extra scalar scan".
- **Adaptation: redesign.** The intrinsic is not reachable with Java 8 API, so the port used a scalar early-exit
  scan for definite lengths up to 32 bytes, then the P3 helper.
- **Tests.** A new 7-test sweep against the UTF-8 constructor (all ASCII lengths 0–40 at offsets 0–2, every high
  byte at every position, well-formed and malformed multibyte sequences); `codec-commons` 1072, `cbor-codec` 346.
  After the revert, `cbor-codec` 339 (the 7 removed).
- **Laptop.** 9 targeted cases, eight paired rounds: G = 1.017 (1.025 for the first five, 1.017 for the three-round
  confirmation). `rpcv2Cbor_GetItemOutput_M` was slower in all eight rounds (1.045–1.109, median 1.087). The
  untouched JSON RPC 1.0 control was 0.996.
- **Why it lost** (a micro benchmark copied from upstream's `CborStringReadBenchmark`, JDK 25.0.2, ns/op):

  | strategy | 4 B | 16 B | 32 B | 128 B |
  |---|---:|---:|---:|---:|
  | UTF-8 constructor (current code) | 4.80 | 5.09 | 5.86 | 8.77 |
  | early-exit scan + hibyte (P4) | 4.83 | 6.07 | 7.78 | 20.0 |
  | hibyte with no check (the ceiling) | 3.15 | 3.11 | 3.20 | 5.16 |

  The saving upstream measured is real here too (the ceiling row). But JDK 17+'s UTF-8 constructor already detects
  ASCII with the intrinsic, so a replacement check has to be almost free. A scalar loop costs as much as the saving at
  4–8 bytes and more beyond. On JDK 21.0.2 the scan was 0.2 ns faster than the UTF-8 constructor at 4 bytes
  (4.62 against 4.86) and slower from 16 bytes on (5.70 against 5.10; 7.41 against 6.39 at 32).
- **Matched upstream?** No. Reverted; recorded in `porting-deviations.md` `### P4` and FU-23.

### 3.7 P5: precomputed event stream member bindings (`8e892b96c`)

- **Upstream claim.** "Improved event stream serialization and deserialization performance by precomputing header
  and payload member bindings per schema" (1.8.0 changelog). No number.
- **Adaptation.** Mechanical, the largest port of the round: a new `SchemaExtensionProvider`
  (`EventStreamSchemaExtensions`, registered in a new service file), the encoder and decoder rewired, Java 8 classes
  instead of records. No serde benchmark covers event streams, so the agent first wrote and committed
  `EventStreamCodecBenchmark` (upstream's six archetypes, encode and decode) against the unchanged encoder. Five
  deviations keep behavior identical (`porting-deviations.md` `### P5`). Three of them are places where a literal
  port would have changed behavior: the schema the filtering views report to the codec, an event of another shape
  passed to `encodeEvent`, and the decoder's member order. The fifth keeps our pooled body path over upstream's
  `codec.serialize()` plus copy.
- **Tests.** `EventStreamSchemaExtensionsTest` (12, new) and 2 new `AwsEventCodecTest` cases. All 31 tests of the two
  classes also pass against the previous encoder and decoder. `http-client-protocol` 128 tests; `dynamic-client` 494;
  architecture tests; shared checks unchanged.
- **Laptop.** 12 cases, five paired rounds: G = 0.876 (encode 0.829, per archetype 0.78–0.88; decode 0.925,
  0.86–0.97). No case above 1.05 in any round. B/op fell in every round on encode `structure` (−52 to −104 B),
  `blobPayload` (−48 to −76) and `initialRequest` (−16 to −32), because the header pass is skipped for events without
  header members. B/op rose on encode `bodyAndHeader` (+16 to +64) and `stringPayload` (+8 to +24). One extra field
  per filtering view explains +16 B; the larger rises are not explained and are on the metal checklist.
- **Limit of the measurement.** The benchmark and the new tests use only the JSON codec. G = 0.876 was measured with
  JSON event bodies only (P5 review, final review).
- **Matched upstream?** Yes in direction; upstream gave no number to compare with.

## 4. What made ports easy or hard

**Easy.**

- **The original port kept upstream's structure.** Our codecs and schema classes are ports of the same 1.7.0 files
  with the package renamed. For every serde commit the triage could name our file and often the line
  (`triage.md`, "our equivalent files" column). P1 matches upstream line for line, and P3's JSON hunks differ only in
  the class name.
- **Upstream commits are small and come with tests.** P1 and P2 are 2 files each upstream. The agent ported
  upstream's tests with them (P1's three cases, P2's resolution test, P5's extension test class).
- **The checks already existed.** The equivalence test, protocol tests, zero-copy tests and architecture tests from
  the original port caught nothing in this round, but they let each port be checked for unchanged behavior within
  minutes of its code commit (the shared checks took about 12 minutes for P4, 22:05Z to 22:17Z, and finished
  81 seconds after P5's code commit, 03:05:29Z to 03:06:50Z).

**Hard.**

- **Java 8.** Records (P5) and `var` are cheap to rewrite. `String` internals are not: upstream reaches
  `String.value`, `String.coder` and `StringCoding.countPositives` through `sun.misc.Unsafe` and a trusted
  `MethodHandles.Lookup`, which our design never ports. That cost P3 its `-XX:-CompactStrings` fallback, made P4 fail,
  and leaves the CBOR and XML write paths and the endpoint SWAR validators unreachable (FU-23). This is the biggest
  limit found.
- **Our own divergences.** Several of our changes after the original port overlap upstream's new work, and each needed
  a decision rather than a copy:
  - The deferred member had already been changed not to call `build()` (task 1.11), so P2 saves less than upstream's.
  - The XML reader had silently diverged (task 7.4); P3 found it.
  - Our JSON and CBOR serializers write in place into a `ByteBufferOutputStream` (FU-17), and other sinks borrow a
    pooled 1 KiB buffer from `SinkBufferPool`. Upstream's event stream encoder switched to `codec.serialize()` plus a
    copy, so P5 kept our path. Upstream's fix for direct and read-only buffers in the same PR applies only to that
    copy, so it did not apply either.
  - Output buffer pooling (`0e88c8e76`, FU-22) overlaps the marshall allocation fix, which already removed the copy
    upstream removed. What is left is the body array itself. Upstream removes it by leasing a pooled array to the
    request and releasing it after the call, retries included. V2 hands request bodies to HTTP clients and
    interceptors as a `ContentStreamProvider` whose `contentAsByteBufferOrNull()` is a raw view, the same zero-copy
    view the blob payload fixes rely on. A reused array behind that view could expose one request's bytes to another,
    and async clients can still be writing after a timeout. That is a lifecycle redesign, not a port.
- **SDK conventions.** `ServiceLoader` needs a public class, and a public `@SdkInternalApi` type must be in an
  `internal` package (`PackageContainmentTest`), so P5's extension moved packages.
- **Missing benchmarks.** P5 needed a new benchmark before it could be measured.
- **Pipeline work is not a port.** V2's signer, interceptor chain, retry strategy and user-agent code are not
  derived from smithy-java, so the pipeline commits (FU-25) are re-implementations of an idea.
- **Measuring.** Upstream rarely states a number, and its numbers are x86 and its own harness. The laptop resolves
  a few percent at best. Measurement took most of each port's time.

## 5. Recommended ongoing process

**Cadence.** One round per smithy-java minor release. 1.7.0 to 1.8.0 was 40 commits, 6 of them serde performance.
A patch release with a serde fix can be triaged on its own.

**Triage** (about 16 minutes this round):

1. `git log --oneline --no-merges <last port base>..<release>`, and the release changelog for PR numbers.
2. One row per commit: category (serde perf, pipeline perf, endpoint rules, correctness, benchmark harness, other),
   the upstream files, our equivalent files, whether it applies, the disposition, and the gain upstream states with
   its source (never an inferred one).
3. Check each serde commit against our divergences (the sections of `porting-deviations.md`, the FU list) and against
   Java 8 before queuing it. Mark a port "experimental" when it replaces something Java 8 cannot reach, and give it a
   strict gate (G ≤ 0.99).
4. Queue the smallest ports first. Record what is not ported as FUs, including upstream correctness fixes in or next
   to ported modules (appendix).

**Per port.** One port per iteration: code and tests in one commit, docs (`porting-deviations.md` row, FUs) in a
second, with an `Upstream: smithy-java <sha> (<subject>)` line in each. Revert with a new commit. A separate agent
reviews each port before the next starts. In this round, the reviews changed the measurement rules (P1, P2), fixed
a misquoted upstream number (P3) and kept a misleading micro benchmark geomean out of this report (P4).

**Gates** (run on every port's commit, then once more on the finished branch, `verification.md`):

| gate | check | this round |
|---|---|---|
| 0 | clean build of the benchmark, protocol test, dynamic client and architecture modules with `--am` | 90 modules, success |
| 1 | unit tests of each touched module, with checkstyle and SpotBugs | `schema` 687, `codec-commons` 1072, `json-codec` 1154 (5 pre-existing skips), `cbor-codec` 339, `xml-codec` 353, `http-client-protocol` 128; 0 failures, 0 violations |
| 2 | `smithy-protocol-tests`, all 7 protocols: per-class counts and skipped IDs identical to before | 923 run, 16 skipped, same IDs |
| 3 | `SerdeBenchmarkEquivalenceTest`, no allowlist change | 142/142 |
| 4 | `BlobPayloadZeroCopyTest`, `BlobMemberZeroCopyTest`, `DynamoDbNewPathTest`, `:architecture-tests` (`PortComplianceTest` and others) | 6/6, 6/6, 4/4, 79 with 11 skipped |
| 5 | `dynamic-client`; prototype services when generated code changes | 494, 1 skipped; services not needed |
| 6 | Java 8: `javac --release 8` and `PortComplianceTest` (animal-sniffer `java18`) | pass; `-P jdk8` runtime tests not run (no JDK 8 on the laptop) |

**Measuring.**

- Laptop, per port: the rules in section 3.2, fixed in the plan before the first measurement. Run
  `-XX:+PrintInlining` when a port claims an inlining or call-site effect; when the timing is neutral, that log is the
  evidence that the mechanism happens on our code (P1, P2, P3).
- Write a benchmark first when none covers the code (P5), and commit it before the port so both jars contain it.
- Metal, once per round: the same m6g.metal host and settings as the serde report, with three arms (baseline, branch
  before the ports, branch after), all 71 serde cases, the null floors of `20261023-011610`, a profiling pass for the
  work boundary, and the ports' own benchmarks. Report the port effect as the change from the before-ports ratios to
  the after-ports ratios.

**Decide FU-23 first.** A JDK 9+ code path for the codecs (a multi-release JAR or a runtime-selected
implementation) is what would make upstream's `String` fast paths available. Without it, every future upstream
change built on `CompactStringAccess` will land in FU-23. Measure upstream's CBOR write change on ARM before building
that path: upstream's only ARM number for it is a regression (section 1).

## 6. Risks and limits

- **Laptop numbers only, so far.** Every gain and every "neutral" in section 3 is laptop-grade. The metal round may
  move them. The keep rule accepts neutral ports, so the branch carries P1–P3 without a measured gain; the argument
  for keeping them is that they keep upstream's code shape for later ports and change no behavior.
- **Gate statistic chosen after the first data.** P1's statistic was changed after two rounds. Under the original one,
  G = 1.085 would have meant a revert. The P1 review accepted the change and had it fixed in the plan before P2.
- **P5 is measured with JSON bodies only.** No XML or CBOR event body is benchmarked or tested.
- **Unexplained allocation changes** on large cases (P2 `rpcv2Cbor_GetItemOutput_L`, P3
  `awsJson1_0_GetItemOutputBinary_L`, P4 `GetItemOutputBinary_L`) and the P5 `bodyAndHeader` rise. None comes from an
  allocation the ports add or remove; they are on the metal checklist.
- **No Java 8 runtime run.** Gate 6 checked compilation and API signatures only.
- **The branch was rebased after the gates.** The base moved 13 commits (`git rev-list --count
  a8557684f83..fae161e1f79`), and the 14 port commits were rebased onto it; the only main-code change among them is
  `CrtRequestAdapter` in `aws-crt-client`, which the serde benchmarks do not call. The port modules' unit tests were
  rerun on the rebased branch (`merge.md`); the full gate set was not.
- **The agent reviewed the agent.** Triage, ports, reviews and verification were all done by agents. The reviews found
  real issues (section 5), and the triage summary still miscounted the endpoint commits (section 2). A human should
  check the triage dispositions for "not applicable" and "follow-up", where a wrong call loses a gain silently.
- **Divergence grows.** Each of our own optimizations (marshall allocation, response body reads, blob zero-copy) makes
  the next upstream change less mechanical to apply. Recording each divergence in `porting-deviations.md` is what made
  this round's overlaps quick to find.
- **Upstream's direction may not suit us.** In 1.8.0, 14 of 40 commits optimized the endpoint rules engine, which V2
  does not use, and the serde commits with the largest stated numbers depend on `String` internals. A release whose
  work is mostly of that kind yields little through porting.

## 7. Metal results (pending run 20261026-180212)

The metal round is running on the serde report's host (`i-018869b3464a7fae5`, m6g.metal, Corretto 25.0.4.1), session
`serde-ab-port-r2-session-20261026-162006`. Run 1 started at 18:02:12Z on 2026-10-26; the session ETA is about
05:25Z on 2026-10-27 (`metal-status.md`).

Arms: `baseline` (`fc8b74e9fe21`, legacy), `feature-marshall` (`a8557684f83`, before the ports), `feature-port`
(`a64bd7cb09b`, SDK pin `cd581132efd`), and for run 3b only `feature-port-pre5` (`4c096e61f2a`, the P5 benchmark
without the P5 port).

What will be filled in here:

1. **Run 1** (`20261026-180212`): all 71 serde cases on three arms, `--reps 3`, `-f 2 -wi 5 -w 2s -i 5 -r 2s`, cores
   32–47. The per-protocol and aggregate geomeans of feature-marshall / baseline and feature-port / baseline, with
   the null floors. The port effect on serde is the change between the two. The null floors were measured with
   `--reps 4` and run 1 uses `--reps 3`, so run 1's own spread is slightly wider and a case near its floor needs
   care (`metal-status.md`, "Reps 3, not 4").
2. **Run 2** (profiling pass, id assigned when run 1 ends): CPU and allocation profiles for the three arms, the work
   boundary for the comparable view, and `profile-comparison.md`, to check that the ports change no work outside the
   code they touch.
3. **Run 3a**: `SchemaMarshallBenchmark` and `SchemaUnmarshallBenchmark`, 7 protocols × SMALL/LARGE, feature-marshall
   vs feature-port (P1–P3).
4. **Run 3b**: `EventStreamCodecBenchmark` encode and decode, 6 archetypes, feature-port-pre5 vs feature-port (P5),
   4 forks per arm.
5. **The metal checklist** (`plan.md`, P2–P5 reviews): B/op of `rpcv2Cbor_PutItemRequest_ShallowMap_M` and
   `rpcv2Cbor_GetItemOutput_L`; `SchemaUnmarshall` AWS_JSON_1_0, AWS_JSON_1_1 and REST_JSON_1 against
   `JsonRpc10Unmarshall` `GetItemOutput_L` and `GetItemOutputBinary_L`; Query `GetMetricDataResponse_L`; B/op of
   `awsJson1_0_GetItemOutputBinary_L`; B/op of event stream encode `bodyAndHeader` and `stringPayload`. Also
   `awsJson1_0_PutItemRequest_BinaryData_L`, where feature-port allocated 428 B/op more than feature-marshall in the
   first repetition of run 1.
6. **Section 1 and 3 updates**: per port, whether the metal result confirms the laptop verdict.

The only data so far is the first repetition of `JsonRpc10MarshallBenchmark` in run 1, where feature-port is within
a few percent of feature-marshall (for example `awsJson1_0_GetItemInput_Baseline` 573.4 against 579.2 ns/op, both
1,160 B/op). That is expected, because P1–P3 target deserialization and P5 event streams. It is one repetition and is
not a result.

How to fetch and summarize, after `SESSION DONE` (from `metal-status.md`; `<profile>`, `<schema>` and
`<eventstream>` are the run ids printed as `Run directory:` in the session log):

```bash
export SERDE_AB_REMOTE_TARGET=ec2-user@<host>
export SERDE_AB_REMOTE_KEY=<key.pem>
R=scripts/schema-serde/serde-ab-remote.sh
RD=(--remote-dir serde-ab-port-r2)
DEST=.agents/tasks/smithy-java-port-round2/runs
/bin/bash $R status "${RD[@]}"
/bin/bash $R log 400 "${RD[@]}" | grep 'Run directory'
/bin/bash $R fetch 20261026-180212 "${RD[@]}" --dest "$DEST"
/bin/bash $R fetch <profile> "${RD[@]}" --dest "$DEST"
/bin/bash $R fetch <schema> "${RD[@]}" --dest "$DEST"
/bin/bash $R fetch <eventstream> "${RD[@]}" --dest "$DEST"
# Run 1 with the null floors and the run 2 work boundary
python3 scripts/schema-serde/serde_ab_summary.py summarize "$DEST/20261026-180212/serde-ab.json" \
  --null .agents/tasks/serde-benchmark-metal/runs/20261023-011610/serde-ab.json \
  --boundary "$DEST/<profile>/boundary.tsv" > .agents/tasks/smithy-java-port-round2/metal-ab-summary.md
# Runs 3a and 3b (no null floors; the first arm is the reference)
python3 scripts/schema-serde/serde_ab_summary.py summarize "$DEST/<schema>/serde-ab.json" \
  > .agents/tasks/smithy-java-port-round2/metal-schema-summary.md
python3 scripts/schema-serde/serde_ab_summary.py summarize "$DEST/<eventstream>/serde-ab.json" \
  > .agents/tasks/smithy-java-port-round2/metal-eventstream-summary.md
```

`metal-status.md` also has the `jmh_compare.py` commands that compute the laptop gate's statistic (median of rep
ratios) on the run 3 data, so the metal and laptop G can be compared directly.

## Appendix: Upstream fixes to track

Correctness and behavior commits in, or next to, the modules we ported (`triage.md` appendix):

| sha | upstream fix | our status | FU |
|---|---|---|---|
| `4a8bea126` | `StructDocument` coerces untyped numbers to the modeled numeric type (was a `ClassCastException`, PR #1419) | Already in our port (`StructDocument.convertDocument`) | none |
| `0e88c8e76` | `LazyXmlSerializer.close()` releases its delegate once, and a write after `close()` throws | We already forget the delegate before closing it. A write after `close()` still silently takes a new pooled delegate that is never returned | **FU-24** |
| `304e02511` | Empty or unit responses call `discardBody`, so the pooled connection is returned | Not applicable: the V2 pipeline closes the response stream | none |
| `8e892b96c` | Event payload body written correctly from direct and read-only buffers (`ByteBufferUtils.getBytes`) | Fixes the `serialize()` copy introduced in the same PR, which P5 did not take | none |
| `eb23fa19f` | Static identities honored by the identity resolver lookup (PR #1392) | Not in ported modules; V2's identity design differs | none |
| `b386e9aca` | Substring equality in compiled endpoint rules | Endpoint rules engine, not ported | none |
| `d62ebebbe`, `e839b1b49`, `d3dd17843` | MCP server fixes and a feature | Not in ported modules | none |

Performance follow-ups from the same triage, for reference: FU-22 (output buffer pooling with leased request bodies,
`0e88c8e76`, `304e02511`; needs a decision), FU-23 (`String`-internals fast paths not reachable from Java 8,
`8c5cf2907`, `b11fd4417`, `be145fcab`; needs a decision), FU-25 (pipeline optimizations applicable to V2 core:
`ebe122c96`, `298b5d799`, `1f68318ce`, `c62499ffd`, `0f0bedf59`). FU-17's remaining event stream item (the
`ByteArrayOutputStream` copy in `AwsEventShapeEncoder`) stays open after P5.
