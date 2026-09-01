# Project Racecar — incremental SDK pipeline optimization

Running log of the optimization phases applied to the AWS SDK for Java v2 request pipeline on
`feature/poc/racecar`, with measured results per phase.

The end goal is options A–F from the pipeline analysis, finishing with G: collapsing the
composable stage chain into a straight-line `ClientPipeline`-style call sequence. Phases are
applied as a **stack** — each measurement includes all preceding phases — so the deltas below are
cumulative unless stated otherwise.

Source of truth for measurement: `test/standalone-e2e-benchmarks` via
`./scripts/collect.sh --clients v2-sync,v2-async`, 200,000 measured ops + 20,000 warmup, 3
interleaved timing reps, out-of-process canned-response mock DynamoDB server.

---

## Measurement methodology (read this before trusting any number below)

**Allocation is the primary signal. CPU time from the e2e harness is currently not usable for
changes of this size.**

Allocation per operation (async-profiler `alloc --total`, bytes) reproduces to within 0.7% of the
[earlier deep-dive report](analysis/20260824-1618/report.md) on the same hardware:

| case | report §5.2 | phase-0 baseline here | agreement |
|------|------------:|----------------------:|----------:|
| v2-sync small-get | 60,963 | 61,387 | 0.7% |
| v2-sync batch-put | 205,292 | 204,272 | 0.5% |
| v2-async small-get | 69,509 | 69,702 | 0.3% |
| v2-async batch-put | 372,456 | 371,884 | 0.2% |

CPU time does not. Rep-to-rep spread in this environment is **6–31%**, against the 2–6% the
original report achieved on the same box. Two causes, one of them mine:

1. **Self-inflicted (fixed):** the first baseline attempt ran while I executed a `git merge` on the
   repo. Two of eight cases showed 2–6× inflated wall time and doubled CPU. That collection was
   discarded (`raw/DISCARDED-phase0-baseline-contended/`) and re-run with the machine idle.
2. **Environmental (not fixed):** even idle, spread stayed at 6–31%. The machine is a developer
   workstation with an IDE and other applications resident; the original report's run was
   evidently quieter. A 5–15% CPU improvement is below this noise floor.
3. **A harness bug (found after phase D, now fixed):** the harness itself was printing a progress
   line per measured operation, from inside the timed loop. See below — it added ~28–39 µs to
   every operation, which is ~22% of a small-get.

Consequences for how phases are judged:

- **Allocation deltas** from the e2e harness are treated as authoritative.
- **CPU-time claims about a specific component** come from a JMH microbenchmark of that component,
  not from the e2e harness.
- **e2e CPU/wall numbers** are recorded for completeness with their spread shown, and are called
  inconclusive whenever the delta is smaller than the spread. They are *not* used to accept or
  reject a phase.

To make e2e CPU time usable later, the environment needs to be quiesced (dedicated host, no IDE,
ideally core-pinned client and server) or the rep count raised substantially. Worth doing before
phase G, whose payoff is CPU-shaped rather than allocation-shaped.

Secondary caveat: `SigningDuration` and friends from the `--metrics` runs are single-run, not
repped, and inherit the same noise. Phase F's `SigningDuration` moved by −24% to +53% depending on
scenario — inconsistent in *sign*, i.e. measuring nothing. Ignore those rows.

### The harness was timing itself (found after phase D, fixed in `ee972091035`)

`--progress-seconds 0` is documented to disable progress reporting, and `collect.sh` has always
passed it. It disabled nothing. The "never" deadline was computed as `start + Long.MAX_VALUE`,
which overflows to a negative value, so every iteration compared as due: each measured operation
printed a formatted progress line and — because `System.out` auto-flushes on a newline — issued a
write syscall, inside the timed region, with stdout redirected to the collection log. The tell was
sitting in every log file all along: **200,005 lines for 200,000 operations.**

Cost, from a paired A/B of the pre-fix and post-fix jars alternating arms within one session
(v2-sync, 50k iterations, 3 reps, stdout to a file exactly as `collect.sh` does):

| scenario | pre-fix µs/op | post-fix µs/op | delta |
|----------|--------------:|---------------:|------:|
| small-get | 125.1 | 97.0 | **−22.5%** |
| batch-put | 380.0 | 340.8 | **−10.3%** |

So the harness charged roughly 28–39 µs to every operation it timed. Consequences:

- **Allocation results — all of them — stand.** The print path allocated 2,071 bytes/op (4.8% of
  profiled bytes), but every one of those stacks is rooted in `BenchmarkRunner` and was therefore
  categorized `benchmark-harness`, which `phase_alloc_compare.py` excludes. Verified directly
  against the phase D profile: 100% of the progress-print bytes land in that category and none leak
  into a client category. Since allocation is the acceptance metric, no phase verdict changes.
- **Every recorded e2e timing number is inflated by a near-constant ~28–39 µs/op.** Because it is
  additive and roughly equal across arms, the *direction* of each phase's timing delta survives,
  but the *magnitude* is understated: a real X→Y improvement was measured as (X+30)→(Y+30). The
  e2e CPU sections below were already labelled inconclusive and not used to accept a phase, so
  nothing needs retracting — but none of those numbers should be quoted, and a baseline-vs-current
  paired re-collection is a prerequisite for any CPU claim, including for phase G.
- **The noise floor is partly measurement, not just environment.** Removing per-op file I/O from
  the loop removes a variance source that had nothing to do with the SDK. How much of the 6–31%
  spread it accounted for is still open; the phase A collection managed 0.4–12.6% *with* the bug
  present, while phase D's first rep came in 5.6× slow, so episodic external interference is
  clearly a separate and larger effect.

### The measured transport was not the documented one (`6cc46827f72`)

`dynamodb` pulls in `apache5-client` and `netty-nio-client` transitively. With `apache-client` and
`aws-crt-client` also declared, three `SdkHttpService` implementations sit on the benchmark
classpath, and V2 does not fail on that — `ClasspathSdkHttpServiceProvider` picks by an internal
priority table, and **Apache5 wins at priority 1**. So `v2-sync` has been `Apache5HttpClient`
throughout, while this module's README said "Apache HttpClient 4.x (default)". Confirmed by running
the same `DefaultSdkHttpClientBuilder` path a client builder uses:

```
RESOLVED sync  = software.amazon.awssdk.http.apache5.Apache5HttpClient  clientName=Apache5
RESOLVED async = software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
```

Consequences:

- **Phase D part 1 was measured against the client it changed.** That commit touched
  `apache5-client` header handling, and Apache5 is what ran. Had the resolution gone the other way,
  half that change would have been dead code and the recorded delta would have come only from the
  user-agent part.
- **No phase verdict changes**, because every phase ran on the same transport. But "V2 sync" in this
  document means Apache5, not the Apache 4.x the earlier report and README implied.
- Every transport is now pinned in code, printed in the run header, and recorded in a `transport`
  column, so no results file can leave the question open again.

**Settled scope: Apache5 for sync, CRT for async.** These are the transports V2 is standardizing on,
so they are the only ones the benchmark carries. `v2-sync-apache4` and `v2-async-netty` existed
briefly while this was being untangled and have been removed; the numbers they produced are recorded
below, but they are not part of the measurement matrix. Worth noting that V2's current *async*
resolution picks Netty at priority 1, so `v2-async` deliberately measures the intended long-term
default rather than today's fallback.

**How much Apache5 and Apache 4.x differ was never established**, and the attempt to answer it is
what exposed the CPU-metric problem below. A single run at 4,000 iterations put Apache5 17% cheaper
per operation; the same comparison at 40,000 iterations put the two within 0.1%, with Apache 4.x
ahead on wall-clock throughput. Neither is a result — two one-off runs at different window lengths
that disagree. It is moot now that Apache5 is the only sync transport under test, but it stands as the
cleanest example of what this metric will do to you.

### Resolved: per-operation CPU is now a converged quantity (`64bee8c616a`)

The section below records the problem as found. It is fixed; the fix needed two things, because
either alone was insufficient.

**Application CPU instead of process CPU.** `cpu_ms` includes the C1/C2 compiler threads, the VM
thread and GC, whose cost is fixed per JVM rather than per operation. `app_cpu_ms` sums per-thread
CPU over Java threads only — `ThreadMXBean` cannot see compiler, VM or GC threads, so that is exactly
the right filter without matching thread names. Threads the harness creates fold their CPU into a
retired counter as they exit, since a thread's CPU disappears with the thread and the driver's workers
are joined before the closing snapshot. `unattributed_cpu_ms` (process minus application) is reported
rather than assumed to be zero.

**Warmup until compilation stops.** Fixing the accounting removes compiler CPU from the total but does
not stop the code changing. Warmup now runs until total compilation time is flat for 3 s, with a 5 s
floor on warmup wall time. That needs **55k–315k operations and 6–16 s** depending on client, against
the 20k that was being requested — so the old warmup was short by an order of magnitude.

Same experiment as below, after both:

| iterations | `cpu_us_per_op` | `app_cpu_us_per_op` | `jit_ms` in window |
|-----------:|----------------:|--------------------:|-------------------:|
| 40,000  | 41.1 | 39.5 | 14 |
| 100,000 | 41.0 | 39.5 | 9  |
| 300,000 | 39.7 | 39.0 | 12 |

**Spread across window lengths: 135% → 1.3%.** Steady-state cost for `v2-sync` `small-get` at
concurrency 1 is ~39 µs/op, against the 48.5 the old metric reported at its most favourable window and
114 at its least.

The warmup gate is evidence, not proof — compilation tails off in bursts — so the runner independently
measures compilation *inside* the window and reports `steady_state`. **The async client still fails
that check at concurrency 4+**, compiling 300–550 ms per window; its per-operation CPU should not be
quoted, and the harness now says so instead of reporting a number that looks fine.

### `cpu_us_per_op` does not converge, and that blocks the concurrency default

Concurrency was added (`--concurrency N`, `6cc46827f72`) to get more samples per second of wall clock
under a more realistic workload. It works: throughput rises 3.2–3.9× by concurrency 4–8. **The mock
server is not the bottleneck** — across a 1→32 sweep on four clients its queue never grew, it never
ran low on handler threads, and its CPU per operation stayed well under the client's. What flattens
the curves is total core demand, peaking at ~11 of this box's 14 cores.

But the default stayed at 1, because the metric that should decide it is an artifact. Whole-process
CPU includes the C1/C2 compiler threads, the VM thread and GC, and that fixed cost is amortized over
the measured operations rather than converging. `v2-sync`/`small-get`, concurrency 1, 20k warmup:

| iterations | wall/op | `mean_lat_us` | `cpu_us_per_op` | client cores |
|-----------:|--------:|--------------:|----------------:|-------------:|
| 40,000  | 94.6 µs | 94.5 µs | 114.1 | 1.21 |
| 100,000 | 89.9 µs | 89.9 µs | 77.4  | 0.86 |
| 300,000 | 84.4 µs | 84.3 µs | 48.5  | 0.57 |

Latency moves 11% while per-operation CPU falls 2.3×, and a *single-threaded blocking* client reports
1.21 cores — CPU that cannot be the caller thread.

The sharpest demonstration: two comparisons measured at 4,000 iterations, then repeated at 40,000.

| comparison | at 4,000 iterations | at 40,000 iterations |
|------------|--------------------:|---------------------:|
| Apache5 vs Apache 4.x, concurrency 1 | Apache5 −16.7% CPU/op | −0.1% (indistinguishable) |
| in-flight vs `join`, CRT, concurrency 8 | `join` +59% CPU/op | `join` −7.5% |

(The Apache 4.x arm was measured while both sync transports were still wired up; Apache5 is now the
only one.)

**Both differences shrank to nothing, and one reversed sign.** They were window artifacts. Anything
this metric appears to show at present has to be treated as unproven.

So:

- CPU numbers are only comparable between runs with **identical** iteration counts. The phase
  collections all used 200k, so phase-to-phase comparisons were not broken by this, but every
  absolute CPU/op figure recorded here is inflated.
- The sweep's per-operation CPU column mixes real contention with the same artifact: at fixed
  iterations, higher concurrency finishes sooner, leaving less compilation inside the window, which
  makes higher concurrency look artificially cheaper. `v2-sync` at concurrency 2 showing −30%
  per-op CPU against concurrency 1 is that, not an efficiency gain.

Prerequisite for a defensible default (and for any phase G claim, which is CPU-shaped): measure
**application CPU** per thread, excluding compiler/VM/GC threads, and warm up until compilation
quiesces. Until then latency percentiles and allocation are the trustworthy per-operation metrics.
*(Both done — see the resolution above.)*

### Concurrency default: 2, and the server is the ceiling after all

With per-operation CPU trustworthy, the sweep became interpretable and two earlier conclusions had to
be revised.

**Concurrency: 2 is the highest level every client reaches steady state at** (the async client stops
settling at 4 and above), and drift in per-operation CPU above that is real contention cost which
applies equally to both arms of a fixed-concurrency comparison. That made 2 look like the right
default — until the null experiment below showed it costs ~4× precision on this machine, at which point
the default went back to 1. Both facts hold; the second one decides it.

**"Total core demand is the limit" was wrong, and so was "the server is not the bottleneck".** The
old sweep put demand at ~11 of 14 cores; with compiler CPU excluded it is ~5. Yet all three clients
still flatten near 48k ops/s. A direct test settles it: two independent client processes against one
server produced 52,569 ops/s against 46,407 for one — **1.13×**, so an entire extra client bought
almost nothing. The ceiling is the apparatus.

Where, specifically: `server_saturated` stayed `false` throughout, and correctly — Jetty's handler pool
really is idle (9 busy of 200, empty queue). The limit is beneath it, in the socket layer. Over that
test the server burned **66.6 s of system time against 34.2 s of user time**: kernel loopback work.
So `server_saturated` is necessary but not sufficient, and a `false` there means the thread pool has
headroom, not the server.

Practical consequence: below concurrency ~4 this does not bite (server 43–47 µs/op, ~2 cores total).
At 8+ the client waits on the server and per-operation CPU absorbs contention unrelated to the SDK.
Treat ~48k ops/s as the apparatus limit; raising it means moving the server off the box, which is what
the separate-host plan is for.

### How small a change can we resolve? A null experiment answers it

The convergence fix removed *bias* from per-operation CPU. It said nothing about *precision*, so the
resolution floor was measured directly: two jars built from one `~/.m2` state, verified byte-identical
across all 25,820 entries apart from the provenance stamp, run as the two arms of a paired comparison.
Every difference reported is noise.

200k operations, 4 repetitions, paired, application CPU per operation:

| case | concurrency | reported delta | spread of pairs |
|------|-----------:|---------------:|----------------:|
| `v2-sync` / small-get | 1 | −0.6% | **±2.0%** |
| `v2-sync` / small-get | 2 | +4.0% | ±8.4% |
| `v2-sync` / batch-put | 2 | −2.0% | ±4.6% |
| `v2-async` / small-get | 1 | −3.9% | ±11.3% |
| `v2-async` / small-get | 2 | −1.2% | ±16.0% |
| `v2-async` / batch-put | 2 | +6.3% | ±6.3% |

What this settles:

- **`v2-sync` CPU work can be done on this laptop**, at concurrency 1: ±2.0% gives a ~1% standard error
  over 4 reps, so a 3–4% change is a real signal. That is good enough for phase-sized optimizations
  and for phase G.
- **`v2-async` cannot.** ±11% at best means only changes above ~20% are visible. Reaching a 1.5%
  standard error would take ~57 repetitions. Async CPU claims need a quieter host.
- **Concurrency is a net loss for comparisons here, and the default went back to 1.** Doubling
  throughput is worth √2 in precision and cost about 4× — the co-resident server, which is also the
  throughput ceiling, contends for the same cores. This reverses the earlier decision to default to 2.
  It is a property of this machine, not of concurrency: on a host where the server has its own cores
  the trade may reverse, and the null experiment should be re-run there rather than assumed.
- **Pairing is essential.** Per-arm spread ran 3.5–43% against paired spreads of 2–16%. An unpaired
  comparison on this machine cannot resolve anything under ~20%, which retroactively explains the
  "6–31% rep spread" that made e2e CPU look useless early on.
- **Allocation remains the acceptance metric**, unaffected by any of this and reproducing to 0.7%.

### The dedicated host, and what it actually bought (`f5f871fd7e6`)

Measurements moved to a **c6g.metal** — Graviton2 Neoverse-N1, 64 physical cores, no SMT, one socket,
one NUMA node, 126 GiB, Amazon Linux 2023, JDK 25 to match the local toolchain. Client and mock server
pinned to disjoint 16-core sets, and the JVM's 64-core ergonomics tamed (it defaults to 18 compiler
threads and 43 GC threads for a benchmark running one application thread).

Null experiment repeated there, 200k operations, 5 reps, concurrency 1, application CPU per operation:

| case | laptop (unpinned) | c6g.metal (pinned, tuned) |
|------|------------------:|--------------------------:|
| `v2-sync` / small-get | ±2.0% | ±2.7% |
| `v2-async` / small-get | ±11.3% | **±2.5%** |

**Async is the unlock.** Sync was already usable on the laptop and is about the same on the host. Async
went from ±11.3% — able to see only changes above roughly 20% — to ±2.5%, which makes the async side of
every phase claim measurable for the first time. The latency distribution is also far tighter than
anything the laptop produced: p50 163.8 µs, p90 170.2, p99 182.8 on a representative run.

Costs are ~4× higher per operation than the M4 Pro (`v2-sync` small-get: ~152 µs/op application CPU
against ~38), since Neoverse-N1 runs at a fixed, lower clock. Runs take proportionally longer; nothing
else changes.

Two host-specific findings worth keeping:

- **Application-CPU accounting matters more here, not less.** One run with a short fixed warmup showed
  995 µs/op of process CPU against 227 µs/op of application CPU — a 4.4× inflation, against ~1.05× on
  the Mac. With 18 compiler threads available, whole-process CPU on this box is close to meaningless
  until the JIT settles.
- **`v2-async` still fails the in-window steady-state check on ~30% of runs at 200k iterations.** The
  slower cores stretch how much of the window residual compilation occupies. Async needs higher
  iteration counts here, and runs flagged `steady_state=false` remain latency-only.

### The concurrency sweep on the host, and an async ceiling that is ours

Re-run on the host with per-operation CPU now trustworthy, client pinned to 32 cores (fixed across
levels so concurrency is the only variable), `small-get`, 300k operations per point. Throughput and
total core demand:

| client | c=1 | c=2 | c=4 | c=8 | c=16 | c=32 |
|--------|----:|----:|----:|----:|-----:|-----:|
| `v2-sync` (apache5) | 5,805 / 1.2c | 11,319 / 2.3c | 21,628 / 4.5c | 41,511 / 9.1c | 80,059 / 18.0c | **143,340** / 34.3c |
| `v2-async` (crt) | 4,575 / 1.2c | 9,520 / 2.4c | 19,582 / 4.4c | 22,341 / 5.0c | 20,707 / 4.8c | 21,639 / 5.0c |
| `smithy` | 11,855 / 1.3c | 23,568 / 2.6c | 43,531 / 5.2c | 88,613 / 10.3c | 164,150 / 20.3c | **272,857** / 33.1c |

**The laptop's ~48k ops/s ceiling was the laptop.** `v2-sync` reaches 143k and smithy 273k here, both
scaling near-linearly in cores (24.7× and 23.0× throughput at concurrency 32), with no saturation flag
at any level. That number should never have been treated as a property of the apparatus in general, and
this is the reason the host mattered beyond noise.

**`v2-async` plateaus at ~22k ops/s from concurrency 8 upward, using only ~5 cores — and the cap is
ours, not CRT's.** Sync and smithy scale right past it, so it is neither the server nor the machine. A
plateau pinned at roughly one core of useful work points at a single-threaded bottleneck, and the
harness has exactly one candidate: in-flight mode submits every request from **one** thread by
deliberate design, because chaining from the completion callback would move marshalling and signing
onto the transport's event-loop threads. Driving the same client and transport in `join` mode, which
uses N threads instead, settles it:

| mode | concurrency 8 | concurrency 16 | app CPU/op |
|------|--------------:|---------------:|-----------:|
| `inflight` (one submitter) | 20,080 | 20,547 | 152.7 → 153.5 |
| `join` (N threads) | 34,571 | **63,191** | 199.1 → 202.2 |

In-flight is flat between 8 and 16 while join scales 1.8×, so **~20.5k ops/s is the submitter thread's
ceiling**, at roughly 45 µs of submit-side CPU per operation. Note also that join costs ~30% more CPU
per operation, which is the thread-per-request overhead — so in-flight is the more efficient model per
operation and the throughput-capped one at the same time.

Consequence: **async throughput above concurrency ~4 measures the harness, not the SDK**, and should
not be quoted. Per-operation CPU is less affected but every async level at 2 and above is flagged
`NOT-STEADY` anyway. The fix is to allow several submitter threads while keeping the in-flight model
(`--submitters N`), which is not yet implemented. None of this touches the phase comparison, which runs
at concurrency 1 where in-flight and join are the same thing.

**The server has headroom even at these rates.** Two independent clients against one server produced
83,407 ops/s against 43,400 for one — 1.92×, so the run is client-bound. On the laptop the same test
gave 1.13× and identified the server as the ceiling; that conclusion was specific to that machine.

Two further notes from the sweep: every `v2-async` level at 2 and above is flagged `NOT-STEADY`, so its
per-operation CPU there is unreliable regardless; and async per-operation CPU *falls* with concurrency
(192.6 → 146.4 µs/op), which is the shape of fixed per-operation costs amortizing across more in-flight
work, but is not worth interpreting while the runs are flagged.

### An experiment ran against the wrong server (`d66b5af5c39`)

Worth recording as a methodology failure, because the harness was complicit. The first
client-vs-server ceiling test was invalid: a `MockDdbServer` from a **two-hour-old jar** was still
listening on the port, the server the test launched died with "Address already in use", and the
clients measured against the stale build for the whole experiment.

`benchmark.sh` allowed it. Readiness was a `/ping` probe, which cannot distinguish our server from
anyone else's — the stale server answered instantly, the loop broke on success, and the dead child was
never noticed. What caught it was the `server_requests` cross-check reporting 0 against an expected
150,000, a check added for entirely different reasons.

Fixed: readiness now comes from the launched child's own `READY` line, and startup failures are
diagnosed from the server log rather than from `kill -0`, which keeps succeeding for an exited
background child until the shell reaps it. A port conflict fails in about a second with the `lsof`
command to find the squatter.

Two lessons kept: cross-checks that seem redundant are how you find out the apparatus lied, and any
result that arrives without a verified provenance chain from server to jar to commit should be assumed
suspect until it has one.

### The async comparison conflated call style with transport

Two confounds sat in the old `v2-async` numbers. Both are now separable, though neither has been
quantified yet — the CPU metric above cannot support it, and these need paired A/B runs:

- **Call style.** The old loop did `join()` per call, holding exactly one operation in flight — an
  async client doing a blocking client's job. `--async-mode inflight|join` now drives the same client
  and transport both ways, so the programming model can be isolated from the transport.
- **Transport.** `v2-sync` is Apache5 while `v2-async` is CRT, so every sync-vs-async statement in
  this document also compares two unrelated HTTP stacks. That is now a deliberate and documented
  property rather than an accident — those are the two transports V2 is standardizing on — but any
  sync-vs-async claim must be read as "Apache5 sync versus CRT async", not as a statement about the
  programming models in isolation.

One durable observation from the sweep before Netty was dropped from the matrix: Netty was
substantially more expensive than CRT at every concurrency level — 4,489 ops/s against 7,330 at
concurrency 1, and roughly double the per-operation CPU. That gap was large and consistent enough to
survive the noise, unlike the two comparisons above, and it is a point in favour of CRT as the
long-term async default.

### Build provenance

The benchmark module resolves the SDK from `~/.m2`, so each phase requires installing the changed
modules. Two wrinkles worth recording:

- Building any *service* module hits
  `maven-plugin-plugin:3.6.0:descriptor ... Unsupported class file major version 61` on
  `codegen-maven-plugin` under JDK 25. Avoided by installing only the non-generated modules that
  actually change (`:sdk-core`, `:http-auth-aws`, `:http-client-spi`, …) and leaving the already
  installed generated `dynamodb` artifact in place. All phase changes are internal, so the
  pre-built `dynamodb` jar stays compatible; the benchmark exercises real DynamoDB calls, so an
  incompatibility would fail loudly rather than silently.
- Switching branches leaves stale `target/classes` that poison compilation
  (`cannot access SdkBuilder`, `cannot find symbol` in unrelated modules). `mvn clean install` on
  the reduced module set is required after a branch switch.
- Installing a *single* core module on its own desynchronizes `~/.m2` and produces
  `VerifyError: AwsAdvancedClientOption is not assignable to AttributeMap$Key` at benchmark
  runtime. A consistent set is required:
  `mvn clean install -pl ':dynamodb,:apache-client,:apache5-client,:aws-crt-client,!:codegen-maven-plugin' --am -P quick -Dmaven.test.skip=true`.

**Superseded by an artifact-based flow (`c3c8651129f`).** The above is the reason a collection used
to be irreproducible: it depended on `~/.m2` state that nothing recorded. The harness now shades
itself, both SDKs and smithy-java into a single jar and stamps the build into it — phase label, git
commit/branch/dirty flag, build time, and the V2/V1/smithy-java versions — surfaced in the run
header, the `phase`/`commit` columns of `results.csv`, and the collection manifest. `scripts/build-jar.sh
PHASE` runs the consistent-module install, shades, and files the result in `pipeline_benchmark2/jars/`
(gitignored); `benchmark.sh`, `server.sh` and `collect.sh` all take `--jar`. This is what makes the
paired A/B above possible at all: two arms alternating in one session with no Maven reinstall
between them. It is also the prerequisite for moving collection to a dedicated host — the jar is
the only thing that needs to be copied.

---

## Phase 0 — baseline

- Commit: `10f88f7bffd` (pre-merge tip of `feature/poc/racecar`)
- Raw data: `raw/phase0-baseline/20260827-1134/`

Allocation, bytes/op (client code only):

| client | small-get | small-put | batch-get | batch-put |
|--------|----------:|----------:|----------:|----------:|
| v2-sync | 61,387 | 54,509 | 533,097 | 204,272 |
| v2-async | 69,702 | 61,797 | 742,955 | 371,884 |

v2-sync small-get by category — the four subsystems the analysis called out:

| category | bytes/op |
|----------|---------:|
| pipeline-framework | 25,356 |
| signing | 20,905 |
| unmarshall | 7,142 |
| json | 3,334 |
| retry | 1,544 |
| endpoint-rules | 1,497 |
| marshall | 1,246 |
| crypto | 338 |

e2e timing (mean of 3 reps, user-CPU ops/s, with spread):

| client | small-get | small-put | batch-get | batch-put |
|--------|----------:|----------:|----------:|----------:|
| v2-sync | 23,655 (7.9%) | 24,386 (26.3%) | 5,813 (28.2%) | 4,790 (11.2%) |
| v2-async | 19,558 (7.9%) | 18,792 (31.5%) | 5,727 (6.2%) | 5,123 (13.4%) |

---

## Phase F — SigV4 fast-path header signer

- Commit: `d9da6c9ff0d` — merge of `alexwoo/sigv4_smithy-java-opts`
- Raw data: `raw/phaseF-signer/20260827-1223/`
- Analysis: `analysis/racecar/alloc-phaseF.md`, `analysis/racecar/timing-phaseF.md`

### What changed

A fast path for header-based SigV4 signing, modelled on smithy-java's `SigV4Signer`:

- `FastV4HeaderSigner` runs the whole SigV4 algorithm in one method instead of composing
  `Checksummer` → `V4RequestSigner` → `V4PayloadSigner`. Streams the body through a pooled
  `MessageDigest` (no `ChecksumInputStream` + per-call `byte[4096]`), builds the canonical request
  and string-to-sign directly into pooled ASCII byte buffers (no `String.getBytes(UTF_8)`), keeps
  headers in a strided `String[]` sorted in place, and applies the signer-managed headers in a
  single builder pass at the end.
- `V4SigningResources` + `Pool`: bounded (32) lock-free pool of `MessageDigest`, `Mac`,
  `StringBuilder` and byte-buffer scratch, reset on acquire and cleared on release.
- `V4SigningKeyCache`: shared bounded (300-entry, LRU) `(secret, region, service)` → signing-key
  cache with a precomputed-hashCode key, replacing a per-lookup `String` key. Same caching posture
  as the `FifoCache<SignerKey>` it replaces — no new secret-material exposure.
- `DefaultAwsV4HttpSigner` dispatches to the fast path only for header-auth SigV4; presigning,
  query auth, flexible checksums, chunk encoding, event streams, aws-chunked trailers and
  anonymous credentials all keep the legacy pipeline.

### Correctness

`FastV4HeaderSignerTest` asserts byte-identical `Authorization`, `X-Amz-Date` and
`X-Amz-Content-Sha256` against `signLegacyPath` across 11 shapes (no body, query parameters,
whitespace in header values, ignored headers, session credentials, payload signing disabled,
multi-valued headers, mixed-case names, non-standard port, nested path). Full `mvn install` on
`:http-auth-aws`: 268 JUnit + 166 TestNG pass, checkstyle and spotbugs clean.

### Isolated signer measurement (JMH, low noise)

`AwsV4HttpSignerBenchmark`, 2 forks × (3×2 s warmup + 5×2 s measurement):

| | legacy path | fast path | delta |
|---|---:|---:|---:|
| time | 2,253.1 ± 83.4 ns/op | 1,323.8 ± 21.8 ns/op | **−41.2%** |
| allocation | 16,552 ± 13 B/op | 2,608 ± 25 B/op | **−84.2%** |

Non-overlapping error bars. This is the authoritative CPU-time claim for phase F.

### End-to-end allocation (authoritative)

Total bytes/op, client code:

| client | scenario | baseline | phase F | delta |
|--------|----------|---------:|--------:|------:|
| v2-sync | small-get | 61,387 | 44,922 | **−26.8%** |
| v2-sync | small-put | 54,509 | 37,803 | **−30.6%** |
| v2-sync | batch-get | 533,097 | 517,121 | −3.0% |
| v2-sync | batch-put | 204,272 | 191,024 | −6.5% |
| v2-async | small-get | 69,702 | 55,184 | **−20.8%** |
| v2-async | small-put | 61,797 | 46,759 | **−24.3%** |
| v2-async | batch-get | 742,955 | 725,353 | −2.4% |
| v2-async | batch-put | 371,884 | 356,448 | −4.2% |

Batch scenarios move less because marshalling and unmarshalling dominate their allocation; signing
is a fixed per-call cost, so its removal shows up proportionally largest on small operations.

By category, v2-sync small-get:

| category | baseline | phase F | delta |
|----------|---------:|--------:|------:|
| signing | 20,905 | 5,607 | **−73.2%** |
| crypto | 338 | 105 | −69.0% |
| endpoint-rules | 1,497 | 1,261 | −15.8% |
| pipeline-framework | 25,356 | 24,680 | −2.7% |
| unmarshall | 7,142 | 7,154 | +0.2% |

Allocation sites that went to zero (v2-sync small-get, bytes/op):

| site | baseline | phase F |
|------|---------:|--------:|
| `ChecksumUtil.lambda$readAll$0` (per-request 4 KiB body-drain buffer) | 4,020 | **0** |
| `V4CanonicalRequest.getCanonicalHeadersString` (2 KiB StringBuilder + String) | 2,579 | **0** |
| `V4RequestSigner.lambda$header$0` | 1,203 | **0** |

The e2e `signing` category (5,607 B/op remaining) is larger than the isolated signer's 2,608
because it also covers `SigningStage` plumbing and auth-scheme resolution, which phase F does not
touch.

### End-to-end CPU (inconclusive — recorded for completeness)

user-CPU ops/s delta vs baseline, with baseline rep spread:

| client | scenario | delta | baseline spread |
|--------|----------|------:|----------------:|
| v2-sync | small-get | −11.8% | 7.9% |
| v2-sync | small-put | +8.0% | 26.3% |
| v2-sync | batch-get | +3.8% | 28.2% |
| v2-sync | batch-put | −9.6% | 11.2% |
| v2-async | small-get | −16.7% | 7.9% |
| v2-async | small-put | +4.1% | 31.5% |
| v2-async | batch-get | −6.3% | 6.2% |
| v2-async | batch-put | −5.0% | 13.4% |

Deltas of both signs at magnitudes at or below the spread: this is noise. Given the JMH result
(−41% on a component worth ~4 µs of a ~78 µs sync call, i.e. ~2% of end-to-end CPU) and a −27%
allocation reduction, no e2e CPU change of this size would be resolvable here anyway.

### Verdict

**Accepted.** −73% signing allocation, −27 to −31% total allocation on small operations, −41% and
−84% on the isolated signer, byte-identical output, full test suite green.

### Follow-up identified

`CollectionUtils.lambda$deepCopyMap$1` is still 1,756 B/op after phase F (was 2,402). The fast
signer still does `source.toBuilder()` → 5 × `putHeader` → `build()`, and the first `putHeader`
after the builder/buildable share triggers a full `deepCopyMap` (TreeMap + one `ArrayList` per
header). That is exactly what phase B targets.

---

## Phase B (part 1) — shallow header copy-on-write

- Commit: `b283db70db0`
- Raw data: `raw/phaseB-mutability/20260827-1754/`
- Analysis: `analysis/racecar/timing-phaseB.md`

### What was intended vs what was done

The plan was "move the immutability barrier to after signing" so the signer mutates the request in
place instead of round-tripping through a builder. **That turned out to be blocked**, so this phase
delivers the other half of option B instead. The blocker is worth recording:

`MakeRequestImmutableStage` sits *outside* the retry loop, and `RetryableStage` re-executes
`SigningStage` with the same input on every attempt. If that input became a mutable builder, attempt
2 would be handed the builder already carrying attempt 1's `Authorization`, `X-Amz-Date` and
`X-Amz-Content-Sha256`. `authorization` is **not** in either signer's canonicalization ignore list
(`V4CanonicalRequest.HEADERS_TO_IGNORE_IN_LOWER_CASE` at `V4CanonicalRequest.java:46`, mirrored in
`FastV4HeaderSigner.IGNORED_HEADERS_LOWERCASE`), so the stale `Authorization` would be folded into
the canonical request and every retry would be signed wrongly.

smithy-java avoids this deliberately — its `isIgnoredHeader` excludes `authorization` with the
comment that "ignoring it keeps re-signing a reused request idempotent". V2 gets away without that
exclusion today only because the request handed to the signer is freshly derived from an immutable
pre-signing request on every attempt.

So moving the barrier requires a prerequisite: **make re-signing idempotent** by excluding the
signer-managed headers from canonicalization. That is a signing-behavior change needing its own
justification and test matrix (including a real multi-attempt retry test), so it is deliberately
not bundled here. Tracked as phase B part 2 below.

### What changed

`LowCopyListMap` shares its map between a builder and the object it builds, and deep-copied on the
first write after sharing — a new `TreeMap` **plus a new `ArrayList` per header**. That copy runs
once per signing, per attempt.

Most mutations don't need the value lists copied: `putHeader`/`putRawQueryParameter` replace an
entry's list wholesale, and `remove`/`clear` only touch the map. Only `appendHeader`/
`appendRawQueryParameter` mutate an existing list in place. The copy-on-write is now split
accordingly:

- `forInternalWrite()` — copies the map only, leaving value lists shared (put, remove, clear).
- `forInternalWriteWithListMutation()` — also privatizes the value lists (the append mutators).

Two share flags are tracked rather than one, because a shallow map copy leaves the lists shared: a
put followed by an append still has to privatize them.

### Correctness

No behavior change is intended, and the risk is entirely aliasing, so the tests target that:

- `LowCopyListMapTest` (14 tests) — the storage layer: put/replace/remove/clear/append after
  sharing, append-after-put, two builders from one buildable, external-map ownership.
- `SdkHttpRequestResponseAliasingTest` (11 tests) — the same contract through the public
  `SdkHttpFullRequest`/`SdkHttpFullResponse` builder API, which is what catches a mutator wired to
  the wrong path.

Mutation-tested: reverting `appendHeader` to the shallow path fails 3 of the new tests, while the
pre-existing `SdkHttpRequestResponseTest` stays green — i.e. without the new tests this bug would
have shipped.

`http-client-spi`: 91 tests pass, checkstyle and spotbugs clean. `http-auth-aws`: 268 JUnit + 166
TestNG pass. **Not verified:** `sdk-core`'s suite could not be run — 136 test classes fail
identically with and without this change (`ObjenesisException` from Mockito under JDK 25 in a
partial reactor), so it is environmental, but it does mean sdk-core coverage is currently missing
for these phases. Worth fixing before phase G.

### Allocation (authoritative), phase F → phase B

| client | scenario | phase F | phase B | delta |
|--------|----------|--------:|--------:|------:|
| v2-sync | small-get | 44,922 | 44,421 | −1.1% |
| v2-sync | small-put | 37,803 | 37,520 | −0.8% |
| v2-sync | batch-get | 517,121 | 515,968 | −0.2% |
| v2-sync | batch-put | 191,024 | 187,566 | −1.8% |
| v2-async | small-get | 55,184 | 52,753 | **−4.4%** |
| v2-async | small-put | 46,759 | 45,401 | −2.9% |
| v2-async | batch-get | 725,353 | 725,008 | −0.0% |
| v2-async | batch-put | 356,448 | 355,498 | −0.3% |

The targeted site is gone completely, but is partly replaced:

| site (v2-sync small-get) | phase F | phase B |
|--------------------------|--------:|--------:|
| `CollectionUtils.lambda$deepCopyMap$1` | 1,756 | **0** |
| `LowCopyListMap.shallowCopyMap` | – | 925 |

So the per-header `ArrayList` allocations are eliminated (−1,756 B/op) and replaced by a map-only
copy (+925 B/op), for a net ~830 B/op — consistent with the ~500 B/op measured at the total level
once category re-attribution is accounted for. **The remaining 925 B/op is the `TreeMap` and its
`Entry` nodes**, which this change cannot remove: eliminating it needs either the barrier move
(part 2) or a strided-array header representation like smithy-java's `ArrayHttpHeaders`.

Async benefits ~4× more than sync (−4.4% vs −1.1% on small-get), with `pipeline-framework` down
7.7% and `retry` down 10.4% — the async path does more builder round-trips per call, so it was
paying the copy more often.

### Internal control for the rebuild

Phase B required rebuilding `dynamodb` (see below), so untouched allocation categories were checked
against phase F to confirm the two builds are comparable: `unmarshall` 7,154 → 7,157 (+0.0%),
`marshall` 1,268 → 1,258 (−0.8%), `pipeline-framework` 24,680 → 24,768 (+0.4%). The build sets are
equivalent, so the deltas above are attributable to the change.

### e2e CPU (inconclusive, as before)

user-CPU ops/s vs the phase 0 baseline: v2-async small-get −9.7%, small-put +3.8%, batch-get +1.2%,
batch-put −2.7%, against baseline spreads of 6–31%. Still noise-dominated; not used for acceptance.

### Verdict

**Accepted, but a small win.** Removes the header-list deep copy entirely and is a prerequisite for
cleaner header handling later, but nets only ~1% of total allocation on sync and ~4% on async. The
larger prize in this area is still on the table.

### Follow-ups identified

1. **Phase B part 2 — move the barrier.** Requires excluding signer-managed headers from
   canonicalization first, to make re-signing idempotent. Needs a multi-attempt retry test.
2. **Header storage.** The residual 925 B/op per copy plus
   `Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` (1,268 B/op restating headers for
   Apache) and `DefaultSdkHttpFullRequest$Builder.putHeader` (841 B/op) all point at the
   `Map<String, List<String>>` representation itself.

### Environment issue hit during this phase (and how it was resolved)

The first phase B collection produced **48/48 failed runs** with
`VerifyError: AwsAdvancedClientOption is not assignable to AttributeMap$Key`. Cause: while debugging
an unrelated build failure I rebuilt and installed `utils` on its own, desynchronizing `~/.m2` — the
previously installed `dynamodb` and `aws-core` jars had been compiled against a different
`AttributeMap`. The baseline and phase F runs were unaffected because all their jars were mutually
consistent.

Resolution: rebuild the full set consistently, excluding only the module that cannot build under
JDK 25:

```bash
mvn clean install -pl ':dynamodb,:apache-client,:aws-crt-client,!:codegen-maven-plugin' \
    --am -P quick -Dmaven.test.skip=true
```

`codegen-maven-plugin` is excluded from the reactor and resolved from `~/.m2` instead (its source is
unchanged); this sidesteps the `maven-plugin-plugin:3.6.0:descriptor` /
`Unsupported class file major version 61` failure. **This is now the standard build command for
every subsequent phase** — always install a mutually consistent set, and smoke-test with
`./scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 300` before starting a
collection.

---

## Cumulative result so far (phase 0 → phase B)

Allocation, bytes/op, client code:

| client | scenario | phase 0 | phase B | total delta |
|--------|----------|--------:|--------:|-----------:|
| v2-sync | small-get | 61,387 | 44,421 | **−27.6%** |
| v2-sync | small-put | 54,509 | 37,520 | **−31.2%** |
| v2-sync | batch-get | 533,097 | 515,968 | −3.2% |
| v2-sync | batch-put | 204,272 | 187,566 | −8.2% |
| v2-async | small-get | 69,702 | 52,753 | **−24.3%** |
| v2-async | small-put | 61,797 | 45,401 | **−26.5%** |
| v2-async | batch-get | 742,955 | 725,008 | −2.4% |
| v2-async | batch-put | 371,884 | 355,498 | −4.4% |

For reference, smithy-java on the same workloads allocates 10,062 B/op (small-get) and
121,708 B/op (batch-put). v2-sync small-get has gone from 6.1× to 4.4× smithy; batch-put from
1.68× to 1.54×.

### Next targets, ranked by current allocation (v2-sync small-get, bytes/op)

Straight from the phase B profile, so this is where the remaining headroom actually is:

| bytes/op | site | option |
|---------:|------|--------|
| 4,161 | `org/apache/hc/core5 InputStreamEntity.writeTo` | A — body materialized once, written without a stream copy |
| 1,597 | `AttributeMapCopier.lambda$copy$0` | D — generated response copier re-copying parser output |
| 1,268 | `Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | B/header storage |
| 1,015 | `ExecutionAttributes.<init>` (`IdentityHashMap(64)`) | D — typed dense-key attribute store |
| 941 | `SdkByteArrayOutputStream.<init>` | E — size the marshalling buffer from a per-operation hint |
| 939 | `DefaultAuthSchemeOption$BuilderImpl.<init>` | D — cache the constant auth-scheme option per client |
| 925 | `LowCopyListMap.shallowCopyMap` | B part 2 / header storage |
| 841 | `DefaultSdkHttpFullRequest$Builder.putHeader` | header storage |

Option A is the single largest remaining item and is the one that generalizes the
`SimpleHttpContentPublisher` fix into a pipeline-wide "the body is already bytes" contract, so it is
the natural next phase.

---

## Phase A — materialized-body contract (zero-copy non-streaming bodies)

- Commits: `0c55ba2a691` (contract + producers + async publisher), `8a018f4b6dc` (signer hashes the
  buffer), `0a662b24412` (Apache ByteArrayEntity), `3add9f48b8b` (sync metrics wrapper propagates),
  `b199c36972a` (BaseClientHandler + StreamManagingStage wrappers propagate)
- Raw data: `raw/phaseA-final/20260828-0112/` (an intermediate run after parts 1–3 is at
  `raw/phaseA-body/20260827-2254/` and `raw/phaseA-body-p4/20260828-0000/`)
- Analysis: `analysis/racecar/alloc-phaseA-cumulative.md`, `analysis/racecar/timing-phaseA.md`

### What changed

The generalization of the earlier `SimpleHttpContentPublisher` fix into a pipeline-wide contract.
Non-streaming bodies are marshalled into memory but carried behind `ContentStreamProvider`, whose
only accessor is `newStream()` — so every consumer re-buffered the stream.

New contract: `ContentStreamProvider.contentAsByteBufferOrNull()`, a default method returning the
content as a `ByteBuffer` when it is already in memory, else `null` (callers fall back to
`newStream()`). Producers: the JSON marshaller's buffer, `fromByteArrayUnsafe`, and
`QueryParametersToBodyStage`. Consumers: the async request publisher (zero-copy `duplicate()`
views), the fast SigV4 signer (hashes the buffer directly, one less full-body traversal per
attempt), and the Apache5 sync client (single-write `ByteArrayEntity` instead of
`InputStreamEntity`'s 4 KiB copy loop, gated on exact Content-Length match so wire framing is
unchanged).

### The lesson of parts 4 and 5: wrappers eat contracts

Parts 1–3 measured **+0–1.5% on sync — no change**. Runtime tracing (temporary debug output in the
gate) showed the provider reaching Apache was a three-deep wrapper chain, each layer hiding the
buffer:

```
TrackingContentStreamProvider          (MakeHttpRequestStage, write metrics)
  -> ClosingStreamProvider             (StreamManagingStage, stream close management)
    -> BaseClientHandler lambda        (length enforcement, round-trips through interceptor context)
      -> SingleBufferContentStreamProvider   <- the buffer, unreachable
```

Every wrapper had to learn to propagate the contract (parts 4–5). The async path was also affected:
the `BaseClientHandler` lambda had silently downgraded async from true zero-copy to the sized-copy
fallback. This is a structural observation worth carrying into phase G: **an optional capability on
an interface is only as good as the least-aware wrapper in the chain.** Any future contract of this
kind either needs a wrapper-audit like this one, or the pipeline needs fewer wrappers — which is
exactly what the straight-line pipeline (option G) buys.

Verification per part: an allocation-profile probe (22k-op sync small-put) showed
`InputStreamEntity.writeTo` at ~86 MB after parts 1–3 and **0 bytes** after part 5.

### Correctness

- `SimpleHttpContentPublisherTest` (24 tests): zero-copy fast path (stream never opened), provider
  reuse across attempts, short/long stream vs Content-Length, partial reads, cap overflow,
  demand/cancel semantics, stream never closed.
- `FastV4HeaderSignerTest` +2: byte-equivalence of the buffer-hashing path (and empty-buffer path)
  against the legacy signing pipeline.
- `ApacheHttpRequestFactoryTest` +5: ByteArrayEntity engagement, repeatability across two writes,
  and fallback on length mismatch / missing length / chunked encoding / plain stream provider.
- Suites: sdk-core 1,534, http-auth-aws 434, apache5-client 14/14 factory tests, http-client-spi +
  aws-json-protocol 125. All pass.
- Flake note: `HttpClientApiCallTimeoutTest.errorResponse_SlowErrorResponseHandler_*` fails ~1-in-5
  in isolation on the **unmodified** tree as well (timing-sensitive 1s timeout vs slow handler
  race); unrelated to these changes.

### Allocation (authoritative), phase B → phase A

| client | scenario | phase B | phase A | delta |
|--------|----------|--------:|--------:|------:|
| v2-sync | small-get | 44,421 | 39,109 | **−12.0%** |
| v2-sync | small-put | 37,520 | 33,514 | **−10.7%** |
| v2-sync | batch-get | 515,968 | 514,464 | −0.3% |
| v2-sync | batch-put | 187,566 | 184,380 | −1.7% |
| v2-async | small-get | 52,753 | 49,240 | −6.7% |
| v2-async | small-put | 45,401 | 39,383 | **−13.3%** |
| v2-async | batch-get | 725,008 | 719,408 | −0.8% |
| v2-async | batch-put | 355,498 | 192,275 | **−45.9%** |

`InputStreamEntity.writeTo`: 4,161 → **0** B/op on every sync scenario. Async batch-put's
`pipeline-framework` category (which contained the old `IoUtils.toByteArray` re-copy) went
176,218 → 50,043 B/op after parts 1–3 and further down with true zero-copy in part 5.

Sync batch-put barely moves because its allocation is dominated by the marshalling buffer growth
chain (`json` + `marshall` ≈ 155 KB/op) — that is option E's target, not A's.

### e2e CPU (still noise-dominated, but now positive across the board)

v2-sync user-CPU ops/s vs baseline: small-get +9.0%, small-put +9.3%, batch-get +3.8%, batch-put
+1.8% — first phase where every sync scenario shows positive, though spreads of 8–28% keep this
inconclusive as evidence.

### Verdict

**Accepted. Biggest phase so far.** Option A delivered exactly what the analysis predicted: the
body is written once at marshalling and never copied again by the framework on the common path —
sync writes it straight from the buffer, async publishes views of it, and the signer hashes it in
place.

---

## Cumulative result (phase 0 → F → B → A)

Allocation, bytes/op, client code:

| client | scenario | phase 0 | phase A | total delta | vs smithy-java |
|--------|----------|--------:|--------:|-----------:|---------------:|
| v2-sync | small-get | 61,387 | 39,109 | **−36.3%** | 3.9× (was 6.1×) |
| v2-sync | small-put | 54,509 | 33,514 | **−38.5%** | 4.8× (was 7.8×) |
| v2-sync | batch-get | 533,097 | 514,464 | −3.5% | 2.3× |
| v2-sync | batch-put | 204,272 | 184,380 | −9.7% | 1.51× (was 1.68×) |
| v2-async | small-get | 69,702 | 49,240 | **−29.4%** | 4.9× |
| v2-async | small-put | 61,797 | 39,383 | **−36.3%** | 5.6× |
| v2-async | batch-get | 742,955 | 719,408 | −3.2% | 3.3× |
| v2-async | batch-put | 371,884 | 192,275 | **−48.3%** | 1.58× (was 3.06×) |

(smithy-java reference values from the 20260824-1618 report: small-get 10,062, small-put 6,977,
batch-get 219,894, batch-put 121,708 B/op.)

### Next targets (from the phase A profile, v2-sync small-get)

| bytes/op | site | option |
|---------:|------|--------|
| 1,594 | `AttributeMapCopier.lambda$copy$0` | D — response copier re-copying parser output |
| 1,404 | `Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | header restatement for Apache |
| 1,146 | `ExecutionAttributes.<init>` (`IdentityHashMap(64)`) | D — typed dense-key attribute store |
| 1,107 | `AttributeValue.builder` + `build` (2,296 combined) | batch-get driver; D |
| 1,078 | `SdkByteArrayOutputStream.<init>` | E — size marshalling buffer from a per-op hint |
| 995 | `LowCopyListMap.shallowCopyMap` | B part 2 / header storage |
| 990 | `DefaultAuthSchemeOption.<init>` + builder | D — cache constant auth option per client |
| 990 | `DefaultSdkHttpFullRequest$Builder.putHeader` | header storage |

Remaining big structural items: **E** (marshalling: buffer sizing + straight-line field loop —
the whole batch-put story), **D** (framework: attributes, metric-stage eliding, auth-option
caching, response copiers), **C** (de-future the async request path), then **G**.

---

## Phase E (part 1) — marshalling buffer sized from recent body sizes

- Commit: `b70aa6b5b45`
- Raw data: `raw/phaseE-marshal/20260828-0225/`
- Analysis: `analysis/racecar/alloc-phaseE-cumulative.md`

### What changed

`SdkJsonGenerator` allocated its output buffer at a fixed 1 KB and grew to the body size by
doubling — a 50 KB batch-put body allocates ~127 KB of cumulative garbage per request
(1+2+4+…+64 KB), the dominant allocation on write-heavy workloads.

New `MarshallBufferSizeHints` (one per protocol factory, i.e. per client) tracks recently observed
marshalled-body sizes per operation and the generator allocates the buffer at that size up front.
The hint grows immediately on a larger body and decays by 1/8th of the gap per smaller observation
(with a floor step of 1, so integer division can't stall it above a smaller steady state — caught
by a unit test on the first attempt at the formula). Clamped to [1 KB, 128 KB]; above 128 KB the
buffer switches to chunked storage anyway.

Plumbing: `StructuredJsonFactory.createWriter(contentType, initialBufferCapacity)` default-method
overload (CBOR/RPCv2 factories ignore it, unchanged); the marshaller reports the final size back
via `JsonProtocolMarshallerBuilder.marshalledSizeReporter`.

### Correctness

`MarshallBufferSizeHintsTest` (8 tests): growth, decay, convergence to steady state, clamping both
ends, per-operation independence, null operation id. `aws-json-protocol` suite green with
checkstyle + spotbugs (spotbugs caught a now-unused private method, removed);
`aws-cbor-protocol` and `smithy-rpcv2-protocol` compile and pass against the new default methods.

### Allocation (authoritative), phase A → phase E

| client | scenario | phase A | phase E | delta |
|--------|----------|--------:|--------:|------:|
| v2-sync | batch-put | 184,380 | 105,355 | **−42.9%** |
| v2-async | batch-put | 192,275 | 112,345 | **−41.6%** |
| v2-sync | small-get | 39,109 | 40,153 | +2.7% (noise) |
| v2-sync | small-put | 33,514 | 33,094 | −1.3% |
| others | | | | ±0.6% |

The `json` category on sync batch-put: 117,188 → 38,175 B/op (−67%) — the doubling chain is gone;
what remains is Jackson's own writer scratch plus the single right-sized buffer. Small operations
are unaffected because their bodies already fit in the 1 KB default.

**Milestone: v2 batch-put now allocates less than smithy-java** (sync 105,355 and async 112,345 vs
smithy's 121,708 B/op) — smithy pays Jackson's growth chain on every call since it sizes its
`ByteBufferOutputStream` statically, while V2 now predicts per operation.

### Verdict

**Accepted.** Biggest single-scenario win of the project so far, exactly where the deep-dive
predicted (§6.1). The remaining `marshall` category cost (36 KB/op on batch-put: `sdkFields()`
iterators, trait probes) is option E part 2 — a CPU-shaped change requiring the field-loop rework.

---

## Cumulative scoreboard (phase 0 → F → B → A → E1)

Allocation, bytes/op, client code:

| client | scenario | phase 0 | now | total delta | vs smithy-java |
|--------|----------|--------:|----:|-----------:|---------------:|
| v2-sync | small-get | 61,387 | 40,153 | **−34.6%** | 4.0× (was 6.1×) |
| v2-sync | small-put | 54,509 | 33,094 | **−39.3%** | 4.7× (was 7.8×) |
| v2-sync | batch-get | 533,097 | 511,523 | −4.0% | 2.3× |
| v2-sync | batch-put | 204,272 | 105,355 | **−48.4%** | **0.87×** (was 1.68×) |
| v2-async | small-get | 69,702 | 49,085 | **−29.6%** | 4.9× |
| v2-async | small-put | 61,797 | 39,345 | **−36.3%** | 5.6× |
| v2-async | batch-get | 742,955 | 717,944 | −3.4% | 3.3× |
| v2-async | batch-put | 371,884 | 112,345 | **−69.8%** | **0.92×** (was 3.06×) |

### What remains, and where it lives

- **batch-get (−4% so far)**: dominated by response-side work — `AttributeValue` builders,
  `AttributeMapCopier`/`BatchGetResponseMapCopier` re-copying parser output (~30% of its
  allocation), i.e. option D's generated-copier item, which requires codegen changes.
- **small ops (~40 KB/op, 4-5× smithy)**: a long tail led by pipeline-framework (~20 KB/op):
  Apache header restatement (~2.2 KB), per-call `RequestPipelineBuilder` stage-chain construction
  (~1 KB — a phase G item), user-agent rebuild (~1 KB), `ExecutionAttributes` (~1.1 KB),
  auth-scheme option rebuild (~1 KB), plus response-side unmarshalling (~7 KB).
- **Options not yet started**: C (de-future the async request path — the async wall/CPU story
  rather than allocation), D (framework: attributes store, metric-stage eliding, auth-option
  caching, generated copiers), B part 2 (idempotent re-signing then the barrier move), E part 2
  (field-loop), and G (straight-line pipeline).

---

## Phase D (part 1) — per-request user-agent and header work

- Commit: `c01f8e84f29`
- Raw data: `raw/phaseD-framework/20260828-0729/`
- Analysis: `analysis/racecar/alloc-phaseD-cumulative.md`

### What changed

Four contained items in the `pipeline-framework` bucket, the largest remaining category on small
operations:

- `ApplyUserAgentStage` rebuilt the constant leading portion of the user agent
  (`userAgentPrefix + clientUserAgent`, including a `trim()` and emptiness checks) on every request.
  It is per-client constant — computed once in the constructor now.
- The user-agent `StringBuilder` started at the default 16 chars and grew by doubling to the typical
  100–200 char result. Now sized from the known prefix length plus headroom.
- `groupApiNames` allocated two `ArrayList`s even when the request had no api names (the common
  case). Short-circuits to a shared empty pair.
- `Apache5HttpRequestFactory.addHeadersToRequest` evaluated `IGNORE_HEADERS.stream().noneMatch(...)`
  **per header**, allocating a stream pipeline and capturing lambda for every header of every
  request. Replaced with an indexed loop.

### Allocation, phase E → phase D

| client | scenario | phase E | phase D | delta |
|--------|----------|--------:|--------:|------:|
| v2-sync | small-get | 40,153 | 38,995 | −2.9% |
| v2-sync | small-put | 33,094 | 31,472 | **−4.9%** |
| v2-sync | batch-get | 511,523 | 509,926 | −0.3% |
| v2-sync | batch-put | 105,355 | 104,150 | −1.1% |
| v2-async | small-get | 49,085 | 47,877 | −2.5% |
| v2-async | small-put | 39,345 | 39,128 | −0.6% |
| v2-async | batch-get | 717,944 | 699,732 | −2.5% |
| v2-async | batch-put | 112,345 | 111,664 | −0.6% |

The Apache header site went from ~1,250–1,640 to ~230–245 B/op (**−80 to −85%**) across scenarios —
the stream-per-header was most of its cost.

`sdk-core` 1,534 tests pass (apart from the known pre-existing `HttpClientApiCallTimeoutTest`
flake), `ApplyUserAgentStageTest` 11/11, apache5-client green with checkstyle + spotbugs.

### A measurement that stopped a change

`ExecutionAttributes.<init>` (`new IdentityHashMap<>(64)`, ~1,040 B of table) looked like an easy
win by shrinking the initial size. Instrumenting the real attribute count first showed **53
attributes per request** on every scenario — so `expectedMaxSize=64` is *correctly* sized (53
entries need capacity ≥ 80 → a 256-slot table), and shrinking it would have forced a rehash and
made things worse.

The only way to improve this site is the dense-int-key store (smithy-java's
`ChunkedArrayStorageContext` model): 53 attributes in an `Object[64]` is ~272 B versus ~1,040 B.
That is a ~770 B/op win but it touches `ExecutionAttributes`' public surface and the subtle
derived/mapped attribute `ValueStorage` semantics, so it is deliberately left as a scoped
follow-up rather than bundled here. Worth noting independently: **53 execution attributes per
request** is itself a lot of per-call state, and is the kind of thing phase G should question.

---

## Final scoreboard (phase 0 → F → B → A → E1 → D1)

Allocation, bytes/op, client code, versus the phase 0 baseline and smithy-java:

| client | scenario | phase 0 | now | total delta | vs smithy-java |
|--------|----------|--------:|----:|-----------:|---------------:|
| v2-sync | small-get | 61,387 | 38,995 | **−36.5%** | 3.9× (was 6.1×) |
| v2-sync | small-put | 54,509 | 31,472 | **−42.3%** | 4.5× (was 7.8×) |
| v2-sync | batch-get | 533,097 | 509,926 | −4.3% | 2.3× |
| v2-sync | batch-put | 204,272 | 104,150 | **−49.0%** | **0.86×** (was 1.68×) |
| v2-async | small-get | 69,702 | 47,877 | **−31.3%** | 4.8× (was 6.9×) |
| v2-async | small-put | 61,797 | 39,128 | **−36.7%** | 5.6× (was 8.9×) |
| v2-async | batch-get | 742,955 | 699,732 | −5.8% | 3.2× |
| v2-async | batch-put | 371,884 | 111,664 | **−70.0%** | **0.92×** (was 3.06×) |

Both batch-put cases now allocate **less than smithy-java**. Small operations are down ~1/3 but
remain 4–6× smithy, and batch-get has barely moved.

### CPU: the first credible measurement of the stack (c6g.metal, paired, 5 reps)

Every CPU figure recorded earlier in this document was inconclusive. With application-CPU accounting,
quiescence warmup, paired arms and a quiet pinned host, the stack can finally be measured. Both arms
are the same harness (`e1df7d63dc0`) and differ only in the SDK inside them; concurrency 1, the
configuration whose noise floor was measured at ±2.7% sync / ±2.5% async.

**Application CPU per operation, phase 0 → phase D1:**

| client | scenario | phase 0 | phase D1 | delta | spread | wins | steady |
|--------|----------|--------:|---------:|------:|-------:|-----:|-------:|
| v2-sync | small-get | 155.7 | 140.1 | **−10.0%** | ±1.8% | 5/5 | 10/10 |
| v2-sync | small-put | 145.5 | 130.3 | **−10.4%** | ±1.3% | 5/5 | 10/10 |
| v2-sync | batch-get | 669.6 | 652.0 | −2.6% | ±1.5% | 5/5 | 10/10 |
| v2-sync | batch-put | 804.5 | 755.5 | **−6.1%** | ±2.7% | 5/5 | 10/10 |
| v2-async | small-get | 208.3 | 195.9 | **−5.9%** | ±1.2% | 5/5 | 6/10 |
| v2-async | small-put | 200.3 | 189.2 | **−5.5%** | ±2.4% | 5/5 | 9/10 |
| v2-async | batch-get | 736.8 | 725.1 | −1.6% | ±2.6% | 4/5 | 10/10 |
| v2-async | batch-put | 784.2 | 749.0 | **−4.5%** | ±1.8% | 5/5 | 10/10 |

(The batch rows are from a re-collection at 80k iterations. The first pass at 25k iterations left the
async batch windows 0/10 steady-state; at 80k every run in the session, sync and async, was steady.
The re-measured sync batch numbers moved from −1.4%/−3.4% to −2.6%/−6.1% — the flagged run had been
*understating* the improvement, which is the direction JIT-in-window bias usually takes.)

**Mean latency moves with it**, slightly less than CPU: −8.1% and −7.8% on sync small-get/small-put,
−5.3% and −4.9% on async.

These are real signals, not noise: every delta exceeds the measured floor, the paired spreads are
±0.7–2.7%, and the sign is consistent across all 5 repetitions in 15 of 16 case-metric pairs.

#### Allocation savings do not convert to CPU one-for-one

The most useful thing in the table is the ratio between the two metrics:

| scenario (v2-sync) | allocation | app CPU | ratio |
|--------------------|-----------:|--------:|------:|
| small-get | −36.5% | −10.0% | ~1 : 3.7 |
| small-put | −42.3% | −10.4% | ~1 : 4.1 |
| batch-get | −4.3% | −2.6% | ~1 : 1.7 |
| batch-put | −49.0% | −6.1% | **~1 : 8** |

Small operations convert at a fairly consistent 1:3.7–4.1. **batch-put is still the outlier**: a 49%
allocation reduction bought 6.1% CPU. That is consistent with where the work actually is — batch-put
serializes ~50 KB of items and writes them to a socket, so removing body copies eliminates a great deal
of *allocation* while the serialization and syscall cost that dominates its CPU is untouched. The
allocation-shaped optimizations paid off where per-request fixed overhead dominates, which is small
operations.

This has a direct bearing on phase G. Collapsing the stage chain is a fixed-per-request cost
optimization, so on this evidence it should land in the same place as phases A–F: visible on small
operations, largely invisible on batch-put.

## Phase G (part 1) — straight-line sync pipeline

- Commit: `ac0c028febb` (`perf(sdk-core): Straight-line sync request pipeline`)
- Raw: `pipeline_benchmark2/paired/host-20260831-0334` (small), `host-20260831-0406` (batch)

### What changed

The sync pipeline was assembled per request from `RequestPipelineBuilder`: ~30 single-use builder
objects, ~19 two-field `ComposingRequestPipelineStage` pair nodes, and a polymorphic `execute()` hop
at every stage boundary. The DSL's flexibility was unused — the chain has exactly one shape.

`SyncApiCallPipeline` is the same chain written in a straight line: the eleven mutation stages and the
six attempt stages become plain sequences of method calls, and only the wrappers with real behavior
(retry, timeouts, metrics, stream management, failure reporting) remain as objects, hand-nested once.
Stage logic is **reused, not copied** — there is no second implementation of any stage to drift — and
construction stays per-request, because `BaseSyncClientHandler` supplies per-request dependencies whose
configuration plugins may have modified. The async pipeline is untouched in this part.

### Correctness

- sdk-core: 1,534 tests, 1 failure — the documented pre-existing flake, passes on re-run.
- All four scenarios run through the new pipeline; the `--metrics` output set is identical to phase
  D1's (11 metrics), confirming the metric-collection wrappers are wired correctly.

### Measurement (paired, host, 5 reps — with a built-in control)

`v2-async` was included in the comparison deliberately: phase G does not touch the async path, so its
delta must read zero. It does — all four async cases land between −1.8% and +0.4%, within the ±2.5%
floor. A non-zero control would have invalidated the session; instead it certifies it.

Application CPU per operation, phase D1 → phase G:

| client | scenario | phase D1 | phase G | delta | spread | wins | steady |
|--------|----------|---------:|--------:|------:|-------:|-----:|-------:|
| v2-sync | small-get | 137.2 | 124.6 | **−9.0%** | ±4.8% | 5/5 | 10/10 |
| v2-sync | small-put | 128.9 | 117.8 | **−8.5%** | ±3.8% | 5/5 | 10/10 |
| v2-sync | batch-get | 650.1 | 648.0 | −0.3% | ±1.1% | 3/5 | 10/10 |
| v2-sync | batch-put | 754.9 | 752.3 | −0.3% | ±1.0% | 2/5 | 10/10 |
| v2-async | *(control)* | — | — | −1.8%…+0.4% | ≤±2.8% | — | — |

Latency: −7.4% and −6.5% on the sync small operations, flat elsewhere.

**The prediction held exactly.** The conversion analysis above predicted a fixed-per-request
optimization would be visible on small operations and largely invisible on batch-put; it delivered
−9% and −0.3% respectively. The size of the small-op win — ~12 µs/op for removing composition
machinery — is more than allocation alone explains; the likely bulk of it is that the
`ComposingRequestPipelineStage.execute` call sites were megamorphic (every pair node dispatches to
different stage types), which defeats inlining along the entire chain, whereas the straight-line
calls are monomorphic.

A session-to-session note: the phase D1 arm measured 137.2 µs/op here against 140.1 in the phase-0
session — a 2% shift between sessions run hours apart, which is exactly why arms are paired within a
session and cross-session numbers are never compared directly.

### Verdict

Kept. The cumulative sync small-get improvement from phase 0 is now **≈ −18%** application CPU
(−10.0% to D1, then −9.0% to G, compounded), with allocation down 36.5% and every phase individually
validated against a measured noise floor.

## Phase G (part 2) — straight-line async pipeline

- Commit: `ee3fb0765c2` (`perf(sdk-core): Straight-line async request pipeline`)
- Raw: `pipeline_benchmark2/paired/host-20260831-0522` (small), `host-20260831-0553` (batch-put)

### What changed

The same collapse applied to `AmazonAsyncHttpClient`. The eleven mutation stages moved to
`RequestMutationStages`, shared with the sync pipeline (the sequence is identical apart from the
`ClientType` handed to the checksum stage), so both paths run one implementation instead of two
copies. The delicate piece is the tail: the builder form adapted the trailing synchronous stages
through `RequestPipelineBuilder.async()`, whose wrapper links exceptions *backwards* with
`forwardExceptionTo` so that cancelling the returned future reaches the in-flight HTTP future.
`FinishStages` reproduces that structure exactly, because dropping the backward link would leave a
cancelled call's HTTP exchange running. De-futuring the effectively-synchronous stages (option C) is
deliberately not in this change.

Mechanism verified directly: in phase D1's allocation profiles, 78% (sync) and 45% (async) of
allocation stacks passed through `RequestPipelineBuilder`/`ComposingRequestPipelineStage`/
`AsyncRequestPipelineWrapper` frames; in G2 both are **zero** — the machinery is off every path.

### Measurement (paired, host, 5 reps — roles reversed)

This time `v2-sync` is the control (parts 1 and 2 share the sync path) and `v2-async` is the
measurement. The control reads zero: −1.1%, +1.6%, −0.5%, all inside the floor.

| client | scenario | phase G | phase G2 | delta | spread | wins | steady |
|--------|----------|--------:|---------:|------:|-------:|-----:|-------:|
| v2-async | small-get | 195.6 | 181.3 | **−7.3%** | ±1.5% | 5/5 | 7/10 |
| v2-async | small-put | 187.7 | 172.4 | **−8.1%** | ±1.8% | 5/5 | 4/10 |
| v2-async | batch-put | 749.3 | 739.1 | −1.3% | ±2.1% | 4/5 | 10/10 |
| v2-sync | *(control)* | — | — | −1.1%…+1.6% | ≤±3.3% | — | 30/30 |

Caveat, stated rather than hidden: the async small windows are only 7/10 and 4/10 steady-state (the
async client chronically compiles longer on these cores), so the CPU figures there carry residual-JIT
contamination. Two things say the result is real anyway: the paired spreads are tight (±1.5–1.8%)
with 5/5 sign consistency, and **latency — which the steady-state issue does not distort — agrees**:
−6.2% and −6.8%.

### Verdict

Kept. Phase G lands almost identically on both clients (−9.0%/−8.5% sync, −7.3%/−8.1% async), which
is what you'd expect from removing the same machinery from both paths.

### Cumulative scoreboard, phase 0 → G2 (application CPU per op, host, paired)

| client | scenario | phase 0 | now | total |
|--------|----------|--------:|----:|------:|
| v2-sync | small-get | 155.7 | ~127 | **≈ −18%** |
| v2-sync | small-put | 145.5 | ~119 | **≈ −18%** |
| v2-async | small-get | 208.3 | ~181 | **≈ −13%** |
| v2-async | small-put | 200.3 | ~172 | **≈ −14%** |
| v2-sync | batch-put | 804.5 | ~750 | ≈ −7% |
| v2-async | batch-put | 784.2 | ~739 | ≈ −6% |

("now" chains the paired deltas; absolute values shift ~2% between sessions, the deltas are what is
measured.)

### Follow-ups identified

- **Option C (de-futuring):** `AsyncSigningStage` and the interceptor stages usually return completed
  futures; short-circuiting the completed-future case would remove `thenApply` hops from the async
  path. The G2 numbers are the baseline.
- **G part 3 (per-client stages):** construction is still per-request because of the per-request
  dependencies copy; routing the response handler through `RequestExecutionContext` would allow one
  pipeline per client. Expected value is small next to the composition collapse.

### Where the remaining gap is

**batch-get (−4 to −6%)** is response-side and needs codegen work: `AttributeValue` builders plus
`AttributeMapCopier`/`BatchGetResponseMapCopier` re-copying the parser's output (~30% of its
allocation). No pipeline change reaches it.

**Small ops (~39 KB/op)** are a long tail with no single dominant site left. From the phase D
profile (v2-sync small-get): response unmarshalling ~7 KB, `ExecutionAttributes` ~1.1 KB,
per-request `RequestPipelineBuilder` stage-chain construction ~1.0 KB, auth-scheme option rebuild
~1.0 KB, `LowCopyListMap` header machinery ~1.0 KB, `putHeader` ~1.0 KB, Jackson parser scratch
~3 KB.

Notably, **the pipeline object graph is rebuilt on every request** (`RequestPipelineBuilder.then` +
`wrappedWith` ≈ 1 KB/op). It cannot simply be cached because `HandleResponseStage` captures the
per-request response handler — which is precisely the argument for option G: a straight-line
pipeline has no per-request stage graph to allocate at all.

### Recommended next order

1. **G (straight-line pipeline)** — now the best-motivated item. It removes the per-request stage
   graph, makes the wrapper-chain problem from phase A structurally impossible, and is where the
   remaining `pipeline-framework` cost lives. **Prerequisite: fix the e2e CPU noise floor**
   (dedicated/quiesced host or many more reps), because G's payoff is CPU-shaped, not
   allocation-shaped, and the current 6–31% spread cannot resolve it.
2. **D part 2** — dense-int-key `ExecutionAttributes` (~770 B/op, needs a design decision on the
   public surface), and eliding the metric stages when no publisher is configured.
3. **C (de-future the async request path)** — the async CPU/latency story; also blocked on the
   noise floor.
4. **B part 2** — idempotent re-signing, then move the immutability barrier.
5. **E part 2 / codegen items** — the `sdkFields()` field loop and the generated response copiers;
   biggest remaining allocation items but they live in codegen, so longest validation tail.

## Phase E2 — extreme JSON request marshalling (smithy-java serde techniques)

- Commits (one optimization each, in order):
  - `8c311fc13b9` E2.1 `perf(aws-json-protocol): Hand-rolled JSON generator replacing Jackson`
  - `2fbae67d9fd` harness: sized-writer benchmark variant (production steady state)
  - `3ba6907097e` E2.2 `perf(aws-json-protocol): Cache per-field marshalling plan`
  - `1bf927a3759` E2.3 `perf(aws-json-protocol): Dispatch container elements without registry lookups`
  - `b0fa0d66f50` E2.4 `perf(aws-json-protocol): Pre-encoded field-name tokens`
  - `fbbd5f4c6bb` E2.6 `perf(codegen): Generate straight-line JSON marshalling on model classes`
  - (`7a6dccd8c04` ci: maven-plugin-plugin 3.6.0→3.13.1, unblocks codegen builds on JDK 25)
- Raw: `raw/e2-jmh/t0-baseline-*.json` (local), `raw/e2-jmh/host-e2.1/` (paired host session 1),
  `raw/e2-jmh/host-e2x/` (paired host session 2, five arms)
- Measurement harness: **component-level JMH** (`test/sdk-standard-benchmarks`,
  `JsonRpc10MarshallBenchmark` — POJO → SdkHttpFullRequest, no network), per the phase brief. No e2e
  runs. All host numbers from the c6g.metal box, `taskset -c 32-47`, arms alternating within a
  session, order reversed on even reps, 3 reps, `-prof gc` for allocation.

### What changed, per commit

**E2.1 — FastJsonGenerator.** `SdkJsonGenerator` (shaded Jackson `UTF8JsonGenerator` →
`SdkByteArrayOutputStream`) replaced for JSON text protocols by a hand-rolled writer modelled on
smithy-java's `SmithyJsonSerializer`: single `byte[]` cursor, worst-case capacity reservation per
write with a cold `grow()`, comma tracking via a per-depth `boolean[]`, two-digits-at-a-time
integers, single-pass ASCII string fast path with escape/UTF-8 fallback, base64 straight into the
output buffer, zero-copy `contentAsByteBufferOrNull()` handoff. Byte-identity with Jackson is
enforced by a golden test (uppercase `\uXXXX` escapes, surrogates escaped not raw, quoted
NaN/Infinity, epoch-seconds timestamps) plus 5,000-case string fuzz. CBOR/RPCv2 keep Jackson-CBOR.

**E2.2 — FieldPlan.** The per-field trait probes in `doMarshall` (up to 3× PayloadTrait EnumMap
lookups, RequiredTrait, location comparison, knownType resolution, registry lookup) move into an
identity-cached per-`SdkField` plan; `DefaultValueTrait` is eagerly dereferenced in the SdkField
constructor (same pattern as LocationTrait).

**E2.3 — container element dispatch.** LIST/MAP marshallers resolved every element via
`context.marshall(PAYLOAD, val)` — instanceof + two HashMap lookups per element. Element type now
resolved once per container from `ListTrait.memberFieldInfo`/`MapTrait.valueFieldInfo`, elements
written through a knownType switch.

**E2.4 — pre-encoded field names.** Additive `StructuredJsonGenerator.writeFieldName(String, byte[])`
default method (CBOR unaffected); FastJsonGenerator overrides with a fused comma+token arraycopy.
FieldPlan pre-encodes each payload field's `"name":` token once.

**E2.6 — generated self-marshalling (the structural one).** Model shapes of JSON-family protocols
whose members all bind to the payload now implement `StructuredJsonWritable`; codegen emits a
`marshallJsonFields` method of straight-line writes: null check per member, static pre-encoded name
token, direct typed write, inline list/map loops, direct nested-shape calls.
`JsonProtocolMarshaller.doMarshall` dispatches on one instanceof. This is smithy-java's generated
`serializeMembers` translated to the SdkPojo world: for DynamoDB's `AttributeValue` (a 10-field
pseudo-union marshalled through the generic loop) one value's cost collapses from ~10 getter
lambdas + ~40 trait/plan probes to ≤10 null checks and monomorphic writes. Qualification is
transitive over nested shapes, computed as a fixpoint per model (handles recursion); shapes with
non-payload members, explicit payloads, documents, streaming/events, idempotency tokens or custom
defaults keep the generic loop, as does all older generated code. Works for CBOR too (the generated
method targets the `StructuredJsonGenerator` interface).

Not done: E2.5 (name-token storage on SdkField) — mooted by E2.6, which bypasses FieldPlan entirely
on qualified shapes; E2.7 (transform-marshaller singletons) — deferred, ~24 B/op against a
~1,336 B/op steady-state total, no expected CPU signal; generator/buffer pooling — deferred until
the remaining per-request allocation (~1.3 KB) justifies it.

### Correctness gates (every commit)

aws-json-protocol 151→152 tests, sdk-core 2,152, cbor + rpcv2 module suites, **protocol-tests 726**
(exact expected-body assertions; after E2.6, 92 protocol-test model classes are on the generated
path, so the suite genuinely exercises it), codegen 675 (23 fixtures regenerated),
codegen-generated-classes-test 1,973. FastJsonGenerator wire-identity golden test incl. randomized
fuzz vs Jackson.

### Results (paired, host, marshallSized = steady-state buffer sizing, ns/op)

Cumulative E2.1→E2.6 (session 2) on top of the T0→E2.1 win (session 1), DynamoDB-shaped corpus:

| case | T0→E2.1 time | E2.1→E2.6 time | E2.1→E2.6 alloc |
|------|-------------:|---------------:|----------------:|
| PutItem ShallowMap S/M/L | −5.0/−6.8/−7.8% | **−69.8/−67.4/−65.1%** | −0/−0/−0% |
| PutItem MixedItem S/M/L | −6.7/−7.2/−6.1% | **−70.2/−71.8/−71.4%** | −20/−34/−40% |
| PutItem Nested M/L | −11.9/−13.9% | **−67.7/−75.5%** | −0/−40% |
| PutItem BinaryData S/M/L | −7.7/−26.6/−31.2% | −55.8/−22.1/−7.5% | −0/−0/−0% |
| GetItem / PutItem Baseline | −14.9/−10.4% | −51.9/−55.7% | −0/−0% |
| RPCv2-CBOR (3 cases) | (control: flat) | **−68.5…−71.9%** | −1…+11% |

Per-commit attribution (session 2, sized): E2.2 **regressed time** on map-heavy cases (+8…+20% on
ShallowMap/Nested) while cutting allocation up to 40% on MixedItem — the FieldPlan CHM read per
field plus the extra indirection cost more than the EnumMap probes it replaced on these shapes.
E2.3 (−1…−9.5% on item cases) and E2.4 (−2…−7% broadly) clawed most of it back; the three runtime
commits net out roughly flat on time for map-heavy cases and −7…−14% allocation. **E2.6 then makes
the dispatch question moot** — on qualified shapes none of that machinery runs at all, and it
delivers −48…−77% against E2.4 with the identical wire bytes. The E2.2 lesson is recorded: plan
caching is the right shape for the *fallback* path, but on this corpus the fallback was already
cheap enough that only removing the loop entirely (codegen) moves marshalling CPU decisively.

CBOR arm doubled as control for E2.1–E2.4 (flat within noise in session 1; ±small drifts session 2)
and as a *measurement* for E2.6, since the generated method also serves CBOR: −70% there too.

Absolute steady-state numbers worth keeping (host, Graviton2): PutItemRequest MixedItem_M marshals
in **7.95 µs → was 30.8 µs at E2.1, ~31 µs at T0**; ShallowMap_L 28.9 µs (was 92.6); a small
GetItem marshals in 0.58 µs. For scale against the e2e picture: sync batch-put's whole-call app CPU
was ~752 µs/op at G2, of which serialization was the dominant component — this phase removes
roughly 60 µs/op of it (batch-put ≈ 25 MixedItem-ish items).

### The wrong-looking result that was real

Session 1 (local Mac) showed the E2.1 arm regressing allocation up to +77% on L cases while time
improved — cause: the un-hinted benchmark constructor path doubles the new generator's buffer from
1 KB, where Jackson accumulated into a recycled thread-local. That is a cold-start artifact the E1
size hints already fix in production; the `marshallSized` variant (buffer pre-sized like
`MarshallBufferSizeHints` steady state) was added to the harness and shows allocation flat-to-down
everywhere. Both variants are kept in the harness deliberately: `marshall` measures cold start,
`marshallSized` measures steady state.

### Interface changes (all additive, reviewed and approved up front)

- `StructuredJsonGenerator.writeFieldName(String, byte[])` — default method, delegates.
- `StructuredJsonWritable` — new `@SdkProtectedApi` interface in aws-json-protocol.
- `JsonFieldNameToken` — new `@SdkProtectedApi` token pre-encoder.
- `codegen-maven-plugin` maven-plugin-plugin 3.6.0→3.13.1 (build-only; JDK 25 unblock).

### Follow-ups

- **Response side (unmarshalling) is now the bigger half of the serde gap** — batch-get barely moved
  all project. The same treatment (generated `readJsonFields` + hand-rolled parser) is the natural
  E3.
- E2.7 (marshaller singletons) and generator pooling: revisit if per-request fixed overhead shows up
  in e2e small-op profiles; both are small against the current ~1.3 KB/op steady state.
- The E2.2 FieldPlan is now fallback-path only; if REST-JSON top-level shapes show up hot in
  profiles, E2.5 (plan storage on SdkField) is the next step there.

## Phase E3 — JSON response unmarshalling (builder churn + byte-level parsing)

- Commits:
  - `4d62743d988` E3.1 `perf(aws-json-protocol): Remove JsonNode detours from fast unmarshalling parser`
  - `2ae5cc4d446` E3.2 `perf(codegen): Generate straight-line JSON deserialization on model builders`
  - `8b5b027045c` E3.3 `perf(aws-json-protocol): Byte-level JSON reader for generated deserialization`
  - `04528e3c84b` E3.3 fix: gate byte reader on known Content-Length (+ benchmark harness realism)
- Raw: `raw/e2-jmh/host-e3/` (T0), `host-e3x/` (T0/E3.1/E3.2 paired), `host-e3y/` (first E3.3 run,
  kept as the negative result), `host-e3z/` (final E3.3 paired)
- Harness: `JsonRpc10UnmarshallBenchmark` (8 GetItemOutput cases) + `RpcV2CborUnmarshallBenchmark`
  (control for E3.1/E3.3, measurement for E3.2), c6g.metal, pinned, arms alternating, 3 reps.

### Context

Production JSON clients all run the "fast" Jackson-streaming unmarshaller (codegen emits
`ENABLE_FAST_UNMARSHALLER=true` for every JSON protocol), so that was the baseline — post-E2 the
read side stood at 3–10× the cost of the equivalent marshal (GetItemOutput_M 10.9 µs vs ~1–3 µs).
Per nested shape the fast path paid: a builder, a `sdkFieldNameToField` HashMap lookup per key, a
megamorphic setter per member, per-setter union bookkeeping, and — the churn this phase targeted —
the generated copier double-copy: `parseMap`/`parseList` build a collection, then the setter's
copier rebuilds it entry-by-entry (re-hashing every key) and wraps it, discarding the original.
Plus two `JsonNode` detours (timestamps, quoted scalars) and a `String` per value via Jackson.

### What changed

**E3.1** removed the JsonNode+registry detours inside `JsonUnmarshallingParser` (timestamps in all
formats, quoted numbers) with identical `StringToValueConverter`/`DateUtils` conversions.
Measured: flat on this corpus (the detours don't appear in GetItemOutput's shapes) — kept as an
enabler and for services with timestamp-heavy responses.

**E3.2** — the builder-churn kill, mirror of E2.6. New format-agnostic `StructuredJsonReader`
cursor + `JsonMemberTable` (per-shape static member table with packed-long short-name identities);
codegen emits `readJsonFields` on qualifying builders (`StructuredJsonReadable`): switch on member
ordinal, direct field writes, collections built exactly once and wrapped unmodifiable directly,
union type maintained without per-setter bookkeeping. First reader implementation wraps the
existing Jackson token stream, so JSON text and CBOR both dispatch through the same generated code.
Null members are skipped (provably identical end state); unknown keys skipped via `skipChildren`.
Qualification shared with E2.6's fixpoint, extended to Response shapes.

**E3.3** — the Jackson replacement. `FastJsonStructuredReader` parses JSON text directly from the
body bytes: packed-long member matching (one long compare for names ≤7 bytes — the AttributeValue
S/N/B/M/L path), pooled ≤8-byte string dedup cache (smithy-java's design: map keys and short values
repeated across items decode once), integers from digits, base64 decoded from the buffer region
into `SdkBytes.fromByteArrayUnsafe`, single-pass escape-free string decode. Engages only when
Content-Length is known so the body buffer is allocated exactly once at the right size; CBOR keeps
the Jackson-backed reader.

### The negative result worth keeping

The first E3.3 run regressed small responses +82…+91% time and +48…+361% allocation. Cause: the
benchmark responses carried no Content-Length, so every parse drained the stream through
`IoUtils.toByteArray`'s growing-buffer path — costing far more than Jackson's recycled input buffer
saves. Two fixes, both kept: the byte path now requires a known Content-Length (real HTTP responses
have it; anything else falls back to the Jackson reader), and the unmarshall benchmarks now set
Content-Length like a real response. The diagnosis run is preserved in `host-e3y/`.

### Results (paired, host)

| step | time delta | alloc delta | control (CBOR) |
|------|-----------:|------------:|---------------:|
| T0 → E3.1 | −2.6…+1.0% (flat) | flat | — |
| E3.1 → E3.2 | **−11.8…−13.4%** (trivial cases flat) | −11…−18% | −9.2/−18.4% (measurement: same generated code) |
| E3.2 → E3.3 (final, paired same harness) | **−11.6…−43.0%** | −8…−53% small, +7…+26% large (the one-time body buffer) | +0.2% (flat ✓) |

Cumulative T0 → E3.3, compounded: **GetItemOutput_M −33% (10.9 → 7.3 µs), _L −33% (78.8 → 52.7 µs),
Binary_M −35%, GetItemOutput_Baseline −25% (318 → 240 ns), Healthcheck −43%**; CBOR −18% via E3.2
alone. Response-side alloc: _M 11.2 → 10.3 KB (−9%) with copier churn replaced by the single body
buffer; small cases −15…−50%.

### Correctness

- Differential suite: `FastJsonStructuredReaderDifferentialTest` asserts object equality between
  the byte reader and the Jackson reader across the full value matrix (escapes, unicode, quoted
  numbers, NaN/Infinity, base64, all timestamp formats, nested/null container permutations,
  unknown-key skipping) plus 500 randomized documents, and malformed-document rejection parity.
- 726 protocol-tests green per commit (103 protocol-test model classes on the generated read path;
  JSON-text suites run through the byte reader after E3.3), codegen 675 with 22 fixtures
  regenerated, codegen-generated-classes 1,973, aws-json-protocol 155.

### Interface changes (additive, approved)

`StructuredJsonReader` (+ 3 consumer interfaces), `StructuredJsonReadable`, `JsonMemberTable` —
all `@SdkProtectedApi` in aws-json-protocol; generated BuilderImpls implement the readable
interface.

### Follow-ups

- Speculative in-order member matching (smithy-java's fused expected-next check) — the packed-long
  scan is O(members) per field; in-order responses could match in one comparison.
- Direct double parsing from bytes (currently String-boxed; DynamoDB numbers are strings, so this
  didn't matter on this corpus).
- CBOR byte-level reader (definite-length pre-sizing of collections is possible there).
- Error-path unmarshalling still uses the JsonNode DOM; cold path, untouched.

## Post-E3 opportunity queue

Serde is considered tapped out for now — the remaining serde items (speculative in-order member
matching in the byte reader, byte-level double parsing, a CBOR byte-level reader, error-path DOM
removal) are recorded in the E2/E3 follow-up sections as **future opportunities**, not next work.

Reading `analysis/crosssdk-254/report.md` (unmodified 2.54.0 vs smithy) against what the branch has
already landed, the remaining identified items, ranked:

1. **E2E validation of E2/E3** — the serde wins are component-level JMH; the crosssdk gaps #1
   (marshalling 595 µs/op batch-put) and #4 (response copiers, 332 KB/op batch-get) should be
   re-measured end-to-end (racecar HEAD vs phase-G2 jar, existing e2e harness) to confirm they
   close and to re-baseline the category tables. Cheap, and re-ranks everything below.
2. **D part 2 — per-call framework allocations** (crosssdk gap #2, 23.7% CPU / 11.8 KB/op small-get
   on unmodified V2; G1/G2 already took ~9%):
   - dense-int-key `ExecutionAttributes` (~1.1 KB/op; needs a public-surface design decision)
   - auth-scheme option resolution rebuild per call (~1 KB/op, `DefaultAuthSchemeOption
     .consumeProperty` 1.4% CPU)
   - header-map churn: `CollectionUtils.deepCopyMap` on builder mutation (2.2 KB/op), `putHeader`
     paths, `Apache5HttpRequestFactory.addHeadersToRequest` (1.7 KB/op)
   - metric-stage elision when no publisher is configured
3. **C — de-future the async request path** (~18% of async small-op CPU is coordination;
   overlaps G2's `FinishStages`; async-capable measurement now exists on the host).
4. **B part 2 — signing** (idempotent re-signing, then move the immutability barrier; the object
   graph remainder after phase F).
5. **G part 3 — per-client pipeline stages** (small; response handler through
   `RequestExecutionContext`).
6. **E2.7 + generator/reader pooling** (small; revisit if e2e small-op profiles still show
   per-request serde fixed costs).

## E2E validation of phases E2+E3 (paired, host)

- Arms: `e3base` (phase G2 SDK `ee3fb0765c2`, current harness) vs `phaseE3` (`6f07335b3a2`), 5 reps,
  concurrency 1, pinned, identical harness commit across arms.
- Raw: `paired/host-20260901-0410` (small), `host-20260901-0440` (batch).

Whole-call **application CPU per operation**, all cases 5/5 wins:

| client | scenario | G2 | E2+E3 | delta | spread |
|--------|----------|---:|------:|------:|-------:|
| v2-sync | small-get | 128.3 | 113.7 | **−11.3%** | ±1.8% |
| v2-sync | small-put | 120.5 | 107.3 | **−10.9%** | ±2.6% |
| v2-async | small-get | 184.4 | 166.4 | −9.7% † | ±2.5% |
| v2-async | small-put | 177.8 | 157.8 | −11.2% † | ±2.6% |
| v2-sync | batch-get | 649.1 | 485.4 | **−25.2%** | ±0.7% |
| v2-sync | batch-put | 756.7 | 328.0 | **−56.6%** | ±0.5% |
| v2-async | batch-get | 719.8 | 565.7 | −21.4% | ±0.7% |
| v2-async | batch-put | 747.9 | 393.6 | −47.4% | ±1.2% |

† async small runs flagged not-steady on some reps; latency (−8.2%, −8.5%) confirms the direction.

The component-level JMH wins converted to end-to-end almost 1:1 where serde dominates: the
crosssdk-254 report put marshalling at 51% of batch-put CPU (595 µs/op) — E2 removed most of it and
the whole call dropped 56.6%. Batch-get's −25% matches E3's unmarshalling share. Small operations
got −11% from serde alone, consistent with serde being ~10–15% of a small call post-G2.

Cumulative against phase 0 (compounding recorded phase deltas): **v2-sync small-get ≈ −27%
application CPU, batch-put ≈ −59%, batch-get ≈ −27%**. Against the crosssdk-254 absolute table
(unmodified 2.54.0: sync 151.6/798.5/664.7 µs for small-get/batch-put/batch-get), the branch now
measures 113.7/328.0/485.4 — the smithy-java gap on batch-put has closed from 2.62× to ~1.08×
(304.3 µs), and small-get from 3.30× to ~2.5×.

## Phase D2 — per-call framework allocations

- Commits: `9c9344b1819` (`perf(sdk-core): Dense-array storage for ExecutionAttributes`),
  `e833b09e60f` (`perf(sdk-core): Skip per-request auth scheme option rebuild when nothing merges`)
- Raw: `paired/host-20260901-0533` (small, 5 reps), `host-20260901-0602` (batch-put sync, 3 reps)

### What changed

**Dense-array `ExecutionAttributes`** (`9c9344b1819`): every `ExecutionAttribute` now takes a
small-int id from a copy-on-write global registry at construction; the per-request store is a plain
`Object[]` indexed by id instead of an `IdentityHashMap` sized 64. `ValueStorage` was refactored
onto `rawGet`/`rawSet` so derived/mapped attributes keep working. Allocation is roughly neutral
(the array is about the same size as the old pre-sized map); the win is CPU — no hashing, no
collision probing, no `Map.Entry` traffic on the hottest read path in the SDK (every interceptor,
stage, signer and auth resolver reads these). Two behavioral notes flagged for interface review:
`getAttributes()` now returns a snapshot rather than a live view, and a null-valued attribute is
indistinguishable from an absent one.

**Auth scheme option rebuild skip** (`e833b09e60f`): `AuthSchemeResolver
.mergePreExistingAuthSchemeProperties` re-built every `AuthSchemeOption` (builder + copy-on-write
property maps, ~1 KB/op) even when there was nothing to merge — the common case. A
`PropertyAbsenceProbe` fast path detects "no pre-existing properties to merge" without allocating
and returns the original option untouched; `ResolveIdentityRequest` now shares a static empty
instance for the no-property case.

### Measurement (paired, host, vs phase E3 `6f07335b3a2`)

Application CPU per op:

| client | scenario | phaseE3 | phaseD2 | delta | spread | wins |
|--------|----------|--------:|--------:|------:|-------:|-----:|
| v2-sync | small-get | 113.7 | 116.2 | +2.3% | ±4.6% | 2/5 |
| v2-async | small-get | 170.2 | 164.3 | **−3.4%** | ±2.7% | 4/5 |
| v2-sync | small-put | 107.2 | 103.4 | **−3.6%** | ±2.0% | 5/5 |
| v2-async | small-put | 161.4 | 154.7 | **−4.1%** | ±3.5% | 5/5 |
| v2-sync | batch-put | 330.0 | 323.4 | −2.0% | ±2.3% | 2/3 |

Latency agrees where CPU is noisy: batch-put latency −1.7% at 3/3 wins, small-put −2.4%/−3.9% at
5/5. The one red cell (sync small-get +2.3%) sits inside its own ±4.6% pair spread at 2/5 wins —
statistically indistinguishable from zero, while the same code path on small-put reads −3.6% at
5/5. Async small windows were flagged not-steady on some reps (chronic for the async client on
these cores); latency confirms the direction there too (−2.8%, −3.9%).

### Verdict

Kept. A consistent −2…−4% whole-call win from pure framework overhead removal, which is what the
profile predicted: ExecutionAttributes + auth option churn were ~2 KB/op and a few percent of CPU
on a small call. The remaining D2 item — header-map churn (`deepCopyMap`, `putHeader`,
`Apache5HttpRequestFactory.addHeadersToRequest`, ~3.7 KB/op) — is deferred to phase B2.b, which
moves the same immutability barrier.

## Phase B2.a — signing: re-signing idempotency

- Commit: `5826db2974b` (`fix(http-auth-aws): Make legacy-path re-signing idempotent`)

### What changed

Prerequisite for B2.b (moving the request-immutability barrier out of the retry loop): signing the
same request twice must yield the same result, which requires the signer to ignore its own prior
output. Added `authorization` to `V4CanonicalRequest.HEADERS_TO_IGNORE_IN_LOWER_CASE` (it already
ignored `x-amzn-trace-id` and `user-agent`); `FastV4HeaderSigner` was verified already-idempotent
(it overwrites rather than appends its headers). `ReSigningIdempotencyTest` (4 tests) locks the
property: sign(sign(r)) == sign(r) for header and query signers. Mutation-verified — reverting the
one-line change fails 2 of the new tests.

No measurement: this is a correctness enabler, not an optimization; no hot-path behavior changes
until B2.b consumes it.

### B2.b deliberately not attempted unattended

Moving the immutability barrier means the retry loop holds a mutable request builder across
attempts, retyping `RequestPipeline` stages on both sync and async paths. Too much regression
surface for unattended work; scoped out pending review. The concrete prize measured in phase D1
profiles: ~3.7 KB/op of header-map copies plus the per-attempt `toBuilder().build()` round trip.

## Phase C — de-future the async request path

- Commit: `65433be31bd` (`perf(sdk-core): De-future the async request path when futures are already
  complete`)
- Raw: `paired/host-20260901-0608` (5 reps, vs phase D2 `e833b09e60f`)

### What changed

Three async stages that almost always hold already-completed futures now detect that and run
inline instead of scheduling continuations:

- `AsyncSigningStage`: when identity resolution is already done (cached credentials — the steady
  state), sign synchronously instead of `thenCompose` off the identity future.
- `AsyncBeforeTransmissionExecutionInterceptorsStage`: runs its interceptors inline (they are
  synchronous callbacks; the stage only returned a future for pipeline shape).
- `MakeAsyncHttpRequestStage`: returns the execution future directly instead of wrapping it in
  another dependent completion.

All fast paths read the completed value with `isDone()` + `getNow(null)` (the ASYNC_BLOCKING_CALL
spotbugs rule forbids `join()` in async paths, correctly). The slow path — identity actually
pending — is untouched.

### Measurement (paired, host; v2-sync is the control — C touches only the async client)

Application CPU per op:

| client | scenario | phaseD2 | phaseC | delta | spread | wins |
|--------|----------|--------:|-------:|------:|-------:|-----:|
| v2-async | small-get | 167.1 | 156.1 | **−6.6%** | ±1.5% | 5/5 |
| v2-async | small-put | 149.3 | 149.1 | −0.1% | ±2.2% | 2/5 |
| v2-sync | *(control)* | — | — | −0.4%…−1.0% | ≤±6.5% | — |

The control reads zero ✓. Latency agrees on the win (small-get −4.9% at 5/5) and on the flat
(small-put −0.2%). The get/put asymmetry is real and repeatable in this run: small-put's async
completion timing is dominated by the request-body write, so the continuation hops C removes were
already off its critical path, while small-get's response-side chain shortens directly. Async
small windows again flagged partially non-steady; the 5/5 latency agreement on small-get is the
reliable signal.

### Verdict

Kept. −6.6% async small-get CPU / −5% latency from removing coordination hops alone, with a clean
control. This banks part of the ~18% "async coordination" share identified in the crosssdk-254
report; the remainder is the executor handoffs and the netty/CRT boundary, out of scope here.

## Phase B2.b — the immutability barrier, and what was actually behind it

- Commit: `fa076acc284` (`perf(sdk-core): Stamp the first attempt's retry-info header before the barrier`)
- Raw: `paired/host-20260901-1610` (discarded variant), `host-20260901-1704` (committed form),
  `host-20260901-1733` (null experiment) — 7 reps each, small ops

### The premise was wrong

B2.b was queued as "move the immutability barrier out of the retry loop", carried over from the phase D1
profiles. Reading the post-G2 pipeline first showed the barrier is *already* outside it:
`RequestMutationStages` — `MakeRequestMutableStage` through `MakeRequestImmutableStage` — runs once per
call, and `RetryableStage` is nested inside that expression, so retries re-enter only the attempt block.
Nothing needed moving, and the feared retyping of the retry loop across both clients was not the work.

What the per-attempt cost actually was, on both paths:

1. `RetryableStageHelper.requestToSend()` — `toBuilder()` + one `putHeader` + `build()`, every attempt
   including the first. The `putHeader` is the expensive part: the retry stage holds an immutable
   request, so the first mutation of a builder derived from it makes `LowCopyListMap` clone the entire
   header map (`shallowCopyMap`, a fresh case-insensitive `TreeMap` plus an entry per header), and the
   rebuild then materializes a second request with its buildable/lazy wrappers.
2. `FastV4HeaderSigner` — the same pattern again, `source.toBuilder()` plus five `putHeader` calls, so a
   second map clone per attempt. Removing this one requires the signer to accept a builder, which is an
   `HttpSigner`/`SignRequest` interface change. Left alone.
3. `MakeHttpRequestStage.wrapRequestContentStream` — a round trip per attempt when a body is present,
   but it touches no header, so under the low-copy scheme it costs only wrapper objects.

### What changed

Item 1, and only item 1. `ApplyRetryInfoStage` stamps `attempt=1; max=N` inside the mutation sequence,
where the request is already in builder form and the map is already privately owned — the transaction-id
stage immediately before it has paid that sequence's one copy — so the write is free. `requestToSend()`
then compares the header against the value it would write itself and returns the request untouched when
they match. Retries still rebuild, which is right: the attempt number changes, and the previous attempt's
request shares the map anyway.

The comparison is what makes this safe rather than clever. The stage resolves max-attempts by a different
route than the helper (which goes through the retry-policy adapter), and if the two ever disagree — or the
stage declines to stamp because no retry configuration exposes a count — the match fails and the original
rebuild happens. The fast path can cost performance; it cannot cost correctness.

### Measurement

Two things were measured, and they disagree in the way that matters to record.

**The mechanism, directly.** A thread-allocation probe over the exact sequence a call performs, on a
request carrying the headers a marshalled DynamoDB call has: the two round trips cost **1,424 B/op**, the
one round trip plus the untouched return costs **774 B/op** — **−649 B/op on every call**, both clients.
This is not a sampled profile; it is `getCurrentThreadAllocatedBytes` around 200,000 warmed iterations.

**Whole-call CPU: unmeasurable, and the rig said so out loud.** Two paired runs, 7 reps each — the first
against the discarded builder-threading variant, the second against the committed form:

| client | scenario | run 1 (variant) | run 2 (committed) |
|--------|----------|----------------:|------------------:|
| v2-sync | small-get | +1.4% (3/7) | −0.8% (4/7) |
| v2-sync | small-put | +0.8% (3/7) | **+4.2% (1/7)** |
| v2-async | small-get | −0.9% (5/7) | +1.3% (2/7) |
| v2-async | small-put | −0.8% (5/7) | +0.0% (3/7) |

The two runs disagree case by case, and run 2's sync small-put reads like a real regression: +4.2% CPU,
+2.9% latency, 1 of 7 pairs favouring the candidate. A change that only removes work cannot cost 4%, so
rather than argue about it, the floor was measured — a **null experiment**, the same jar copied under two
names, run as both arms, 7 reps, `paired/host-20260901-1733`:

| client | scenario | null delta | spread | "wins" | per-pair |
|--------|----------|-----------:|-------:|-------:|----------|
| v2-sync | small-get | +2.7% | ±6.9% | 1/7 | −3.4, +2.8, +0.5, +5.3, +2.0, **+10.3**, +1.5 |
| v2-sync | small-put | +0.1% | ±4.3% | 4/7 | −3.8, −1.6, +3.4, +3.7, +4.8, −3.3, −2.9 |

Byte-identical code produced **+2.7% at 1/7** on small-get, with one pair at +10.3%. That is the same
shape as the "regression", so the +4.2% is not interpretable, and neither is any other reading in the
table. (The summarizer cannot label a same-jar comparison — arms are keyed by the jar's stamped phase —
so the null was paired from `results.csv` against the run log's execution order. The log also confirms
`paired-ab.sh` alternates which arm goes first per repetition, ruling out a fixed position bias.)

Verdict on the timing: **no effect measurable at this resolution, and no evidence of regression.** 649
B/op is a few percent of a small call's allocation, and a red-black tree copy of a dozen entries is a few
hundred nanoseconds against 107 µs, so the expected effect was always below what this rig resolves.

**Caveat that outlives this phase:** the floor moved. Earlier phases on this host reported ±1.5–2.7%
paired spreads with 5/5 sign consistency; this session's null resolves only ±4–7% on sync small ops. Any
sub-2% measurement taken in this window is unusable, and a null run — not a rerun — is what tells you
which regime you are in.

### Kept, on these grounds

Not on the timing, which the null experiment shows cannot speak to an effect this size. The allocation
reduction is real and measured, there is no evidence of regression, the diff is 35 lines plus one stage,
and the change is binary compatible. The tests are worth
having independently: `RetryableStageRequestIsolationTest` pins invariants that had no coverage at all
before — per-attempt request identity, single-valued attempt headers, each attempt starting from the
*unsigned* request, that a stale or absent pre-stamped header is not trusted, and that the stage and
helper agree under both retry-policy and retry-strategy configuration. Mutation-verified: trusting the
header's presence instead of its value fails 3 of them; making the stage disagree with the helper fails 1.

### The version that was written first, and discarded

The first implementation threaded the mutation sequence's builder through the retry stage, so the retry
helper owned the barrier and materialized one request per attempt. It worked, passed every gate, and
saved **646 B/op** — three bytes less than the version that shipped. It also changed `execute()`
signatures on five `@SdkInternalApi` pipeline stages plus `RetryableStageHelper`'s constructor, and broke
the japicmp binary-compatibility gate, which a temp worktree confirmed passes at `HEAD`. The root pom
excludes `*.internal.*` from that gate, but the pattern does not reach nested internal subpackages, so
landing it would have meant either relaxing a repo-wide gate or scattering deprecated bridge methods
through six internal classes — for the same 649 bytes obtainable without touching a single signature. The
patch is kept at `/tmp/b2b_builder_threading.patch` for the record but is not the direction.

### Follow-ups

- **The signer's map clone (item 2 above)** is the other half of the per-attempt header churn and needs
  an `HttpSigner`/`SignRequest` interface change to accept a builder. Worth a design discussion; the
  B2.a idempotency work is already in place as its prerequisite.
- **Allocation profiles from separate `collect.sh` runs are not comparable arm-to-arm.** An attempt to
  verify this change that way showed a uniform −25% across every category, including unmarshalling and
  Apache internals that the change cannot touch: `asprof alloc --total` scales with sample count, so two
  independently scheduled recordings do not share a denominator. The thread-allocation probe was used
  instead. Worth remembering before quoting a cross-run alloc delta.
