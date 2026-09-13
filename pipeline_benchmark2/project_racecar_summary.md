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
- Raw, 7 reps each, small ops: `paired/host-20260901-1840` (committed form) and `host-20260901-1928`
  (matched null) are the runs to read — both at 200k iterations / 30k warmup, client pinned 32–47.
  `host-20260901-1610` (discarded variant), `host-20260901-1704` (committed form) and `host-20260901-1733`
  (null) were taken at the harness default 50k/10k on cores 8–15 and are superseded; see the measurement
  section for why they are kept rather than deleted.

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
two things were checked instead of argued about.

**First, the configuration — which was wrong.** Both runs above used `paired-ab.sh`'s default 50,000
iterations and 10,000 warmup, and pinned the client to cores 8–15. Every earlier phase on this host used
150,000–200,000 iterations, 20,000–30,000 warmup, and client cores 32–47 with the server on 0–15. A
50,000-iteration window is about seven seconds of measured work, on half as many cores, adjacent to core
0 where kernel, IRQ and timer work concentrates — and the host does have periodic background activity
that is not excluded from the pinned set (a `refresh-policy-routes` timer every ~60 s, `sysstat-collect`
every 10 min, a resident `chronicled` agent). The host itself was otherwise clean: load 0.03 at rest, no
other users since the instance booted, no leftover JVMs or harness processes, and the three runs did not
overlap each other.

So both runs were repeated with the established configuration (`paired/host-20260901-1840`), and it did
matter — for accuracy, if not for the verdict. Async absolute CPU dropped from ~171 µs/op to ~158 µs/op
once warmup and iterations were restored, i.e. the short-window runs had been reporting async numbers
roughly 8% too high through JIT contamination. That is worth knowing independently of B2.b.

**Second, the floor — measured under the same corrected configuration.** A null experiment, the same jar
copied under two names and run as both arms, 7 reps, `paired/host-20260901-1928`, beside the corrected
B2.b comparison:

| client | scenario | B2.b delta (wins) | null delta (wins) | null worst pair |
|--------|----------|------------------:|------------------:|----------------:|
| v2-sync | small-get | +2.4% (2/7) | −0.3% (4/7) | −4.5% |
| v2-sync | small-put | +0.6% (3/7) | +0.4% (2/7) | **−8.8%** |
| v2-async | small-get | +1.4% (4/7) | +2.4% (2/7) | +7.2% |
| v2-async | small-put | −0.1% (4/7) | −1.6% (5/7) | −4.9% |

Identical code produces means from −1.6% to +2.4%, sign consistency from 2/7 to 5/7, and individual pairs
as extreme as −8.8%. The B2.b column is drawn from that same distribution on every case. (The summarizer
cannot label a same-jar comparison — arms are keyed by the jar's stamped phase label — so the null was
paired from `results.csv` against the run log's execution order. The log also confirms `paired-ab.sh`
alternates which arm goes first per repetition, ruling out a fixed position bias.)

Verdict on the timing: **no effect measurable, and no evidence of regression.** 649 B/op is a few percent
of a small call's allocation, and a red-black tree copy of a dozen entries is a few hundred nanoseconds
against 105 µs, so the expected effect was always below what this rig resolves.

**What this says about the method, correcting an earlier reading of it.** The first instinct was that the
host's noise floor had degraded. It had not. Checking the earlier manifests, D2's paired spreads were
±2.0–4.6% and C's ±1.5–6.5% — the same regime as today's ±3.3–6.3%. For small operations this rig has
always resolved roughly ±2.5% on a 7-rep mean, with individual pairs swinging several times that. What
made D2 and C credible was never tight spreads: it was effect sizes of 3–6% *combined with* 5/5 sign
consistency and latency agreeing with CPU. Below about 2–3% on small ops, this harness cannot distinguish
a change from a no-op, and the way to find that out is a null run at the same settings — not a rerun,
which just produces a second uninterpretable number, as it did here.

### Kept, on these grounds

Not on the timing, which the matched null shows cannot speak to an effect this size. The allocation
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
- **Always pass the measurement configuration explicitly.** `paired-ab.sh` defaults to 50,000 iterations
  and 10,000 warmup, which is not what any phase here was measured at. Use
  `--iterations 200000 --warmup 30000 --pin-client 32-47 --pin-server 0-15` for small operations on this
  host, and check the run's `manifest.md` before comparing its numbers against a recorded phase. The
  defaults cost ~8% accuracy on async absolute CPU through JIT contamination, and the harness's own
  "N of M runs were not steady-state" warning is the tell.
- **Allocation profiles from separate `collect.sh` runs are not comparable arm-to-arm.** An attempt to
  verify this change that way showed a uniform −25% across every category, including unmarshalling and
  Apache internals that the change cannot touch: `asprof alloc --total` scales with sample count, so two
  independently scheduled recordings do not share a denominator. The thread-allocation probe was used
  instead. Worth remembering before quoting a cross-run alloc delta.

## Phase E4 — the response toBuilder() tax

- Commit: `b935f474c49` (`perf(codegen): Adopt model collections in the builder copy constructor`)
- Raw: `paired/host-20260901-2357` (A/B, 7 reps), `host-20260902-0125` (matched null),
  profile `raw/host-20260901-2057` — all at 200k/30k on cores 32–47

### Re-ranking, and a premise corrected again

With D2/C/B2 done, a fresh profile of branch head re-ranked what was left: v2-sync batch-get at
500 KB/op with 63% of CPU in unmarshall, and `BatchGetResponseMapCopier.lambda$copy$2` at 79 KB/op.
The opportunity queue had this filed as "response copiers on the read path" (crosssdk gap #4),
implying the parser routes its output through the generated copiers. It does not: the E3 read path
assigns parsed collections directly into builder fields and wraps them unmodifiable — the copiers
never run during parsing.

The JFR caller chain pointed somewhere better. `BaseClientHandler.attachHttpResponseToResult`
(BaseClientHandler.java:238) runs `response.toBuilder().sdkHttpResponse(httpFullResponse).build()`
on **every response of every service**, to attach one metadata field. The generated builder copy
constructor routed every member through its fluent setter, and for a list or map member that setter
is a deep copy: a fresh collection at every nesting level plus an unmodifiable wrapper for each. So
the SDK parsed the response once and then rebuilt its entire collection spine to record which HTTP
response produced it.

### What changed

`ModelBuilderSpecs.modelCopyConstructor()` now emits `this.field = model.field` instead of
`setter(model.field)` for list and map members. The copy was redundant by provenance: a collection
reached through `model.<field>` is not caller-supplied — it is what the member copier or the
generated read path produced at build time, deeply unmodifiable in both cases (or an
`SdkAutoConstruct` sentinel). Neither side can mutate it in place, and every setter replaces its
field wholesale, so an adopted collection and a re-copied one are observationally identical.

Two deliberate limits. Only collections adopt — other setters assign rather than copy, so there is
nothing to save and no need to audit setters that do more than assign. And unions never adopt:
their setters maintain `type`/`setTypes` via `handleUnionValueChange`, and removing that exclusion
fails 12 existing `UnionTypeTest` assertions (which is how the guard is mutation-verified).
`BuilderCopyConstructorAdoptionTest` (new, 5 tests) pins the properties the adoption rests on:
model collections are unmodifiable, setters on a derived builder do not write through to the
original, caller-supplied collections are still defensively copied on the way in.

### Measurement

**Mechanism:** thread-allocation probe on a 25-item/10-attribute BatchGetItemResponse — the
`toBuilder().build()` round trip went from **14,849 B/op to 80 B/op** (−99.5%).

**End to end** (paired, 7 reps, vs phase B2b), with the matched null beside it:

| client | scenario | B2b | E4 | delta | wins | null (same jar twice) |
|--------|----------|----:|---:|------:|-----:|----------------------:|
| v2-sync | batch-get | 466.9 | 400.7 | **−14.2%** | 7/7 | +0.3% (4/7) |
| v2-async | batch-get | 548.9 | 479.4 | **−12.7%** | 7/7 | +0.3% (4/7) |
| v2-sync | small-get | 101.3 | 102.1 | +0.8% | 3/7 | −2.5% (5/7) |
| v2-async | small-get | 158.8 | 158.5 | −0.2% | 4/7 | +0.3% (3/7) |

Latency agrees at 7/7: −12.8% sync, −12.0% async. Small-get is the built-in control — its response
carries one small map, so there was almost nothing to adopt, and it reads inside the null's floor.
This is the cleanest e2e result of the project: the effect is ~10× the measured floor with perfect
sign consistency, on the framework path every AWS service response takes.

Cumulative sync batch-get from unmodified 2.54.0 (664.7 µs): now ~400 µs, **≈ −40%**.

### Follow-ups

- **AttributeValue builder churn is now the batch-get ceiling**: `$readJson` 85 KB/op,
  `BuilderImpl.build` 74 KB/op, `BuilderImpl.<init>` 42 KB/op, `decodeCached` 83 KB/op. Real work
  rather than waste — one builder + one immutable object per attribute value is the design. The
  smithy-java answer (one object, no builder round trip, via generated per-member deserializer
  singletons writing final fields) needs a different generated-object shape; sized at −30…−40% of
  the remaining unmarshall allocation if pursued.
- `getResponses()`-style bean getters still call `copyToBuilder` per invocation; untouched (cold,
  and semantically a mutable escape hatch).

## Phase E5 — union type tracking (negative result, reverted)

- Not committed. Raw: `raw/e2-jmh/host-e5`

E4's follow-up list pointed at builder churn on the read path: the profile attributed 42 KB/op on
batch-get to `AttributeValue$BuilderImpl.<init>`, and every union builder carried an
`EnumSet<Type>` plus an iterator allocation each time `handleUnionValueChange` inspected it. Since a
large DynamoDB read constructs tens of thousands of union builders, replacing that with a `long`
bitmask indexed by `Type.ordinal()` looked like free money.

It was worth exactly nothing. Paired JMH unmarshall runs, 3 reps:

| metric | result |
|--------|--------|
| allocation | **0.0% on every case** — identical to the byte |
| time | −3.5% … +2.7%, no consistent sign |

The reason is the useful part. The builder never escapes `$readJson` — it is constructed, written
through, and consumed by `build()` in one inlined region — so escape analysis scalar-replaces it
*and* its `EnumSet`, and the iterator too. There was no allocation to remove. A standalone
thread-allocation probe agreed exactly: 34 B/value before and after.

Reverted rather than kept: it changed generated output for every union in every service, added a
63-member cap at generation time, and bought nothing.

**Lesson that generalizes:** "reduce builder allocation on the read path" is a suspect target in
this codebase. Where the builder is confined to one generated method, the JIT has already deleted
it, and the profiler's attribution of bytes to `BuilderImpl.<init>` reflects the cases where escape
analysis fails, not a per-value cost. Check with a probe before committing to a codegen change.

## Phase E6 — four characters per iteration in the JSON string writer

- Commit: `7da96a1b2a3` (`perf(json): Test four characters per iteration when writing JSON strings`)
- Raw: `raw/e2-jmh/host-e6` (3 paired reps, arms alternating per rep)

`FastJsonGenerator.writeQuotedString` was the largest single frame anywhere in the profile — 24.1%
of batch-put client CPU. Its ASCII fast path ran two branches per character (non-ASCII check, then
escape-table entry), so a 64 KB batch-put body cost roughly 130,000 branches to emit.

The fast path now takes four characters at a time and folds both rejection tests across the group:
a non-ASCII character shows up in the OR of all four characters, and a character needing an escape
shows up in the OR of their four escape-table entries. Two branches per four characters. A group is
stored only after both tests pass, so a group containing a rejected character stores nothing and a
single-character tail loop re-walks it to locate the exact rejection point for the existing slow path.

| case | rep1 | rep2 | rep3 |
|------|-----:|-----:|-----:|
| PutItemRequest_MixedItem_L | −6.6% | −7.0% | −5.7% |
| PutItemRequest_MixedItem_M | −5.7% | −4.5% | −5.1% |
| PutItemRequest_Nested_L | −3.9% | −4.4% | −4.3% |
| PutItemRequest_BinaryData_* *(control)* | ~0 | ~0 | ~0 |

`BinaryData` is a built-in control — those payloads are base64 blobs rather than quoted strings, and
they do not move. Allocation unchanged. Correctness is byte-identity against Jackson; the existing
5,000-string fuzz covered this probabilistically, and two deterministic tests were added because the
change makes behaviour depend on where a rejecting character falls in a group and how the length
lines up with the stride (eight rejecting characters × every offset × lengths 1–13, plus pure-ASCII
at every length 0–40).

### The bigger win here, deliberately not taken

smithy-java goes considerably further (`JsonWriteUtils.copyJsonAscii` +
`CompactStringAccess.latin1Bytes`): it reads the String's **internal Latin-1 byte array** and copies
it eight bytes at a time, checking all eight for escapes with a single SWAR mask, with overlapping
head/tail words to avoid a loop for short strings. That is a much larger reduction in instructions
per byte than grouping by four.

Two things block simply copying it, and both are decisions rather than code:

1. **Java 8 source level.** This module compiles at `-source 1.8`, so `VarHandle` — which is how
   both the byte-array word access and the trusted-lookup field access are done — is unavailable.
2. **Reflective access into `java.lang.String`.** smithy-java reaches `String.value`/`String.coder`
   by pulling `MethodHandles.Lookup.IMPL_LOOKUP` out with `sun.misc.Unsafe`. It is guarded (system
   property, GraalVM check, null fallback) and it is fast, but for the production SDK it is a
   compatibility and security-review question — JDK internals, module access, native-image — not a
   perf question.

Recorded so it can be revisited with the right people rather than decided here. If taken, the
expected prize is the remainder of that 24%: roughly another 8–15% of batch-put CPU.

### E4+E6 end-to-end validation

- Raw: `paired/host-20260902-1807` (7 reps, 200k/30k, cores 32–47), vs phase B2b

| client | scenario | B2b | E4+E6 | delta | spread | wins |
|--------|----------|----:|------:|------:|-------:|-----:|
| v2-sync | batch-put | 318.8 | 296.9 | **−6.8%** | ±2.4% | 7/7 |
| v2-async | batch-put | 364.4 | 341.3 | **−6.3%** | ±1.3% | 7/7 |
| v2-sync | batch-get | 474.1 | 406.7 | **−14.2%** | ±0.6% | 7/7 |
| v2-async | batch-get | 550.2 | 479.5 | **−12.8%** | ±0.9% | 7/7 |
| v2-sync | small-get | 104.5 | 102.3 | −2.0% | ±4.0% | 4/7 |
| v2-async | small-get | 160.2 | 157.0 | −2.0% | ±1.8% | 7/7 |

The two changes separate cleanly by scenario, which is a useful check on both. batch-put is
marshalling-dominated and its PutItem response is empty, so its −6.8%/−6.3% is E6 alone — and it
lands exactly where the JMH marshall benchmarks predicted (−4…−7% on string-heavy cases). batch-get
carries E4 and reproduces that phase's isolated measurement (−14.2%/−12.7% then, −14.2%/−12.8% now).

Cumulative against the unmodified 2.54.0 absolutes from `analysis/crosssdk-254` (sync 151.6 /
798.5 / 664.7 µs for small-get / batch-put / batch-get): now ~102 / ~297 / ~407 µs, i.e.
**≈ −33% small-get, ≈ −63% batch-put, ≈ −39% batch-get**.

**Process note.** The first attempt at this run was invalid and discarded. Two invocations of the
launcher script each started a detached `paired-ab.sh`; they competed for the mock server port, 83 of
84 iterations failed, and both wrote to the same log — which then reported `DONE` in five minutes.
Two corrupted result directories (`20260902-1741`, `20260902-1756`) were deleted rather than kept.
The launcher now refuses to start when a run is already active, and the first four iterations are
checked for failures before leaving it unattended. Worth remembering: a suspiciously fast completion
is a symptom, and `FAILED` lines in the run log are the thing to grep for.

## Phase E7 — whitespace skipping in the byte reader

- Commit: `8285d0c9e27` (`perf(json): Give whitespace skipping a one-comparison fast path`)
- Raw: `raw/e2-jmh/host-e7` (component, 3 paired reps), `paired/host-20260902-2055` (e2e, 7 reps)

`skipWs` was 6.5% of batch-get client CPU. It runs several times per member — before the name, around
the colon, and again inside whichever value reader handles the value — so a response of any size calls
it thousands of times, and it spent four byte comparisons per call discovering what AWS JSON responses
always are: whitespace-free.

Every whitespace byte JSON allows is `<= ' '` and every byte that can legally start a name or value is
`> ' '`, so one comparison settles the common case. The loop moved out of line, leaving a stub small
enough to inline into all its callers. Whitespace that hand-written or proxied JSON does contain takes
the cold path unchanged.

| case | rep1 | rep2 | rep3 |
|------|-----:|-----:|-----:|
| GetItemOutput_L | −8.5% | −8.3% | −7.6% |
| GetItemOutputBinary_L | −9.8% | −9.5% | −9.6% |
| GetItemOutput_M | −4.9% | −4.3% | −5.9% |
| GetItemOutput_S | −2.1% | −1.7% | −2.6% |
| GetItemOutput_Baseline | −1.7% | −1.2% | −1.6% |

The gain scaling with document size is the signature of the mechanism: more members means more skips.
Allocation is unchanged on every case, as it must be for a pure CPU change.

End to end against phase E6 (7 reps, 200k/30k, cores 32–47), `paired/host-20260902-2055`:

| client | scenario | E6 | E7 | delta | spread | wins |
|--------|----------|---:|---:|------:|-------:|-----:|
| v2-sync | batch-get | 405.7 | 391.9 | **−3.4%** | ±0.6% | 7/7 |
| v2-async | batch-get | 479.0 | 465.8 | **−2.7%** | ±1.3% | 7/7 |
| v2-sync | small-get | 106.1 | 105.5 | −0.5% | ±4.6% | 3/7 |
| v2-async | small-get | 161.0 | 157.9 | −1.8% | ±3.8% | 5/7 |

The e2e and component numbers agree without fudging: −4.9% on the medium document, times
unmarshalling's 63% share of batch-get CPU, predicts ≈ −3%, and the measurement is −3.4%. Small-get
sits inside its floor, as a change proportional to member count should.

Cumulative sync batch-get against unmodified 2.54.0: 664.7 → ~392 µs, **≈ −41%**.

## Phase E8 — grouped string scan on the read side (negative result, reverted)

- Not committed (the differential test cases written for it were kept). Raw: `raw/e2-jmh/host-e8`

E7 left `readString` as the largest reader frame (5.61% of batch-get CPU), and its scan tested three
byte comparisons per byte — the closing quote, a backslash, an illegal control byte. E6 had just won
on the write side by clearing four characters per branch, so the same shape was tried here: a
256-entry `STRING_STOP` table, four entries OR'd together to clear a group in one branch.

It made things **worse**, consistently, across all three reps:

| case | rep1 | rep2 | rep3 |
|------|-----:|-----:|-----:|
| GetItemOutput_M | +2.5% | +2.2% | +2.4% |
| GetItemOutput_S | +1.7% | +0.9% | +3.2% |
| GetItemOutputBinary_S | +2.0% | +3.1% | +4.5% |
| GetItemOutput_L | +1.2% | −0.0% | −0.2% |

Why it lost, where E6 won on the same idea:

- The three tests it replaced were comparisons against **constants** — immediate operands, no memory
  traffic, and highly predictable. The replacement pays four array loads (with masking) instead.
- The group loop only advances over safe bytes, so the byte-at-a-time loop still has to re-read the
  group containing the terminator to classify it. E6 had no equivalent re-read: it stored as it went.
- DynamoDB strings are short. At 10–20 bytes a group loop runs two to five times, which is not enough
  to amortize the extra setup.

E6's write-side loop was doing materially more per character to begin with — a `charAt`, a table
lookup *and* a store — so folding its branches paid. The read-side loop was already three register
comparisons. **The lesson: grouping pays where the per-element work is heavy, not where the loop is
already tight; check what the existing per-element cost actually is before assuming a win.**

A second idea was dropped without measuring, on the strength of this one. Strings are built with
`new String(bytes, UTF_8)`, and constructing with `ISO_8859_1` instead would skip UTF-8's validation
pass for the ASCII case — but knowing the string is ASCII means tracking it during the scan, and the
scan just proved exquisitely sensitive to added work. The JDK's own check is the vectorized
`countPositives` intrinsic, which is unlikely to be beaten by hand.

### Where the read side stands

`raw/host-20260902-2222` profiles branch head. batch-get is 414,723 B/op (down from 500,102 before
E4) with unmarshall at 61% of CPU, and what remains is dominated by object materialization rather
than parsing overhead: `$readJsonMember` 8.0%, `readString` 5.6%, `decodeCached` 4.5%,
`AttributeValue$BuilderImpl.<init>` 4.4%, `skipWs` 4.4% (from 6.5% pre-E7), `AttributeValue.<init>`
3.8%, `String.<init>` 3.5%, `HashMap.putVal` 2.9%. The Strings, the builders, the AttributeValues and
the map nodes are all objects the API contract requires. Cheap parsing tricks are close to exhausted
here; the remaining levers are the two structural ones already recorded — smithy's Latin-1/SWAR
string handling (needs the `Unsafe`/JDK-internals decision) and a generated object shape that
materializes values without a builder round trip.

## Phase E9 — bulk character extraction on the write side (mixed, reverted)

- Not committed. Raw: `raw/e2-jmh/host-e9`

The post-E6 batch-put profile showed something unexpected: `String.charAt`, `StringLatin1.charAt`,
`String.isLatin1` and `String.length` together were **9.2% of client CPU**. The coder check was being
repeated per character access rather than hoisted out of the write loop. So `writeQuotedString` was
changed to pull the characters out once with `String.getChars` into a per-generator scratch array — a
single inflating arraycopy for a latin1 string — and scan a plain local array after that.

The result is a real trade, consistent across 3 reps rather than noise:

| case | rep1 | rep2 | rep3 | alloc |
|------|-----:|-----:|-----:|------:|
| PutItemRequest_Nested_L | **−9.1%** | **−6.8%** | **−7.0%** | +4.3% |
| PutItemRequest_MixedItem_L | −2.1% | −3.9% | −2.5% | +1.1% |
| PutItemRequest_MixedItem_M | −1.5% | −1.0% | −1.2% | +2.5% |
| PutItemRequest_Baseline | **+3.8%** | **+2.8%** | **+3.5%** | +6.0% |
| GetItemInput_Baseline | +1.5% | +1.7% | +3.7% | +6.0% |

Large and deeply nested payloads gain up to 9%; tiny ones lose about 3%, because `getChars` has a
fixed cost that a handful of characters cannot amortize. Every case also gains ~80 B/op for the
scratch buffer.

Reverted, and the reasoning is worth keeping because it is a judgement rather than a measurement
failure. The mixed result is fixable — gate on string length, taking the array path only above ~16
characters — but that means carrying two copies of the grouped scan loop, one reading a `String` and
one reading a `char[]`, plus the buffer's allocation. Against that: batch-put is the target workload,
its strings are `MixedItem`-shaped, so the honest e2e expectation is ~−1% of batch-put CPU once
marshalling's share is applied. Not worth the surface area on this corpus.

Worth revisiting if a string-heavy or deeply-nested non-DynamoDB workload becomes a target, where the
Nested_L figure (−7…−9%) is the relevant one rather than MixedItem. The 9.2% accessor overhead it was
attacking is real and still there.

## Phase E10 — single-copy async non-streaming response aggregation

- Commit: `40187bba428` (`perf(sdk-core): Buffer async responses once`)
- Paired timing: `paired/host-20260903-0404`
- Allocation profiles: `raw/host-e10-alloc-20260903-052722`

### What changed

`AsyncResponseHandler` adapted non-streaming async HTTP responses to the synchronous protocol handlers
through three copies: every transport `ByteBuffer` was first copied by `BinaryUtils.copyBytesFrom`,
that temporary array was copied into a growing `ByteArrayOutputStream`, and `prepare()` cloned the
complete body again with `toByteArray()` before creating the response stream. The JSON byte reader then
allocated another exact body array and read that stream into it.

E10 replaces the first three copies with one SDK-owned accumulator:

- a valid `Content-Length` up to 8 MiB is a sizing hint, allocated lazily only when a chunk arrives;
- absent, malformed, negative, and larger lengths use the small growing path;
- inaccurate lengths do not truncate or expose trailing capacity — received bytes remain authoritative;
- heap, sliced, read-only, and direct buffers copy straight into the owned array through a duplicate,
  leaving the transport buffer's position and limit unchanged;
- the protocol handler receives a count-bounded `ByteArrayInputStream` over the owned backing array,
  with no final clone;
- absent content remains distinct from an explicitly delivered zero-length chunk; and
- duplicate subscriptions and aggregation/request failures cancel upstream and release body state.

This is deliberately `sdk-core`-only. Generic JSON/XML/Query/custom handlers still receive the same
`AbortableInputStream`, and CRC/gzip adaptation still runs after buffering and before protocol parsing.
Direct handoff of the owned array to `FastJsonStructuredReader` is left as a separate follow-up so its
module/interface work and performance are independently attributable.

### Correctness

- 13 focused tests cover buffer types/windows, source-buffer state, inaccurate and unsafe length hints,
  absent versus present-empty bodies, sequential attempts, publisher/handler failures, upstream
  cancellation, and CRC consumption.
- A new whitebox Reactive Streams TCK runs 27 checks (13 optional checks skipped by the TCK), all green.
- Focused async/CRC/retry suite: 46 JUnit tests plus the TCK, green.
- Full `sdk-core`: 1,555 JUnit and 651 TestNG/TCK tests, zero failures; checkstyle and SpotBugs clean.
- The consistent 53-module DynamoDB/Apache5/CRT build passed.
- Local async and sync smoke runs covered all four scenarios, reconciled every server request, and
  produced the same 11 metric names as before.

### Allocation mechanism (authoritative)

Both allocation profiles ran 5,000 warmup + 30,000 measured operations. They were intentionally fixed-
warmup profiler runs and not used for CPU claims. Dividing sampled allocation totals by the same 35,000
operations:

| allocation | E10 base | E10 | delta |
|---|---:|---:|---:|
| client code (benchmark harness excluded) | 630,350 B/op | 495,953 B/op | **−21.3%** |
| all `byte[]` allocation | 294,447 B/op | 164,304 B/op | **−44.2%** |
| async response aggregation sites | 165,090 B/op | 36,790 B/op | **−128,301 B/op (−77.7%)** |

The old sites disappear exactly as intended:

| site | base | E10 |
|---|---:|---:|
| `AsyncResponseHandler$BaosSubscriber.onNext` | 91,915 B/op | **0** |
| `BinaryUtils.copyBytesFrom` | 36,790 B/op | **0** |
| `AsyncResponseHandler.lambda$prepare$0` full-body clone | 36,386 B/op | **0** |
| `BufferedResponseBody.ensureCapacity` owned array | 0 | 36,790 B/op |

Two body-sized allocations intentionally remain and size the next work: the JSON byte reader at
36,341 B/op and CRT's response callback at 36,221 B/op. The former is SDK-owned and avoidable by a
capability-preserving direct parser handoff; the latter is at the transport/native ownership boundary.

### End-to-end timing (paired dedicated host)

Both jars use harness commit `40187bba428`; the SDK arms are predecessor `2b39ced77cf` and E10
`40187bba428`. Seven paired reps, 200k measured / 30k minimum warmup, concurrency 1, client cores
32–47 and server cores 0–15:

| client | scenario | base app CPU | E10 app CPU | CPU delta | spread | wins | latency delta | latency wins |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| v2-async | batch-get | 467.6 | 456.2 | **−2.4%** | ±0.8% | **7/7** | **−2.3%** | **7/7** |
| v2-sync *(control)* | batch-get | 387.4 | 392.7 | +1.4% | ±2.5% | 3/7 | +1.3% | 2/7 |
| v2-sync *(control)* | small-get | 106.7 | 106.3 | −0.3% | ±4.3% | 4/7 | −0.1% | 4/7 |
| v2-async | small-get | — | — | not steady | — | — | +0.9% | 2/7 |

All 14 async small-get windows were flagged non-steady, so their CPU values are discarded; latency is
inside noise, as expected for a 470-byte response. The sync controls are also noise and certify that the
batch-get signal is specific to the async body path. Async batch-get is steady in all 14 arm runs, has a
paired spread well below the measured floor, wins every CPU and latency pair, and is backed by the direct
allocation mechanism result.

### Verdict and follow-up

**Kept.** E10 removes about 128 KB/op of redundant response aggregation allocation and improves the
large async read path by 2.4% CPU / 2.3% latency without affecting sync controls. The next coherent
phase is direct handoff of this owned array to the byte-level JSON reader, eliminating the remaining
~36 KB/op parser body copy while preserving CRC, handler wrapping, and non-JSON fallbacks.

## Phase E11 — direct async response-buffer JSON parsing

- Commit: `6774e92dd1e` (`perf(json): Parse owned async response buffer`)
- Paired timing: `paired/host-20260903-1502`
- Allocation profiles: `raw/host-e11-alloc-20260903-160054`

E11 exposes E10's SDK-owned immutable byte range through an internal bounded input stream. Eligible
generated JSON deserialization recognizes that immediate stream delegate, consumes it to preserve
handler drain semantics, and passes the array/range directly to `FastJsonStructuredReader`. CRC and
gzip replace the delegate with their wrappers and therefore retain the existing copy/stream fallback;
sync and generic response streams keep the Content-Length path unchanged.

Full sdk-core (1,559 JUnit + 651 TestNG/TCK), aws-json-protocol (158), checkstyle, SpotBugs, and the
consistent 53-module build passed.

Equal 35,000-operation allocation profiles confirm the target disappeared:

| site/metric | E10 | E11 | delta |
|---|---:|---:|---:|
| `JsonProtocolUnmarshaller.byteUnmarshallFromJson` body array | 37,838 B/op | **0** | **−37,838 B/op** |
| all `byte[]` | 165,682 B/op | 129,401 B/op | **−36,281 B/op** |
| total allocation | 501,106 B/op | 466,233 B/op | **−34,873 B/op (−7.0%)** |

The E10 owned array (~36.7 KB/op) and CRT callback copy (~37.1 KB/op) remain, as expected.

Dedicated-host timing, seven paired reps at 200k/30k:

| client | scenario | E10 app CPU | E11 app CPU | CPU delta | spread | wins | latency delta | latency wins |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| v2-async | batch-get | 453.4 | 449.5 | −0.9% | ±0.9% | 6/7 | **−1.2%** | **7/7** |
| v2-sync *(control)* | batch-get | 399.1 | 399.5 | +0.1% | ±0.8% | 1/7 | −0.0% | 3/7 |

**Kept as an allocation win.** The CPU result is at/below the measured resolution floor and is not
claimed as independently significant; latency agrees in direction with perfect sign consistency. The
mechanism is exact, removes one full-body allocation, and leaves the sync control at zero.

## Phase E12 — direct CRT request-header array construction

- Commit: `79687b4e359` (`perf(aws-crt-client): Build headers directly`)
- Paired timing: `paired/host-20260903-1743`
- Allocation profiles: `raw/host-e12-alloc-20260903-191748`

E12 replaces the CRT adapter's intermediate `ArrayList<HttpHeader>`, per-value stream/lambda
pipeline, and final `toArray` copy with a one-pass growable `HttpHeader[]`. It also obtains the
synthetic Host value from `SdkHttpRequest.host()` instead of materializing a full URI. Per-value
`HttpHeader` objects and CRT JNI marshalling remain unchanged.

The focused adapter suite expanded from four to fifteen tests and covers exact synthetic/header
ordering, duplicate and multi-values, empty lists, original casing, explicit and derived Host values
(DNS, IPv4, and bracketed IPv6), HTTP/1.1 and HTTP/2 Connection behavior, known/unknown content
length, Transfer-Encoding, and source immutability. The complete `aws-crt-client` build passed 319
tests (two skipped), checkstyle, SpotBugs, dependency analysis, Java 8 compilation, and the
consistent 53-module benchmark build.

Equal 35,000-operation async small-get allocation profiles confirm the old adapter path disappeared:

| site/metric | E11 | E12 | delta |
|---|---:|---:|---:|
| `CrtRequestAdapter.createAsyncHttpHeaderList` list/stream/lambda site | 2,247 B/op | **0** | **−2,247 B/op** |
| HTTP-client category | 8,478 B/op | 7,175 B/op | **−1,303 B/op (−15.4%)** |
| total sampled allocation | 44,242 B/op | 42,564 B/op | **−1,678 B/op (−3.8%)** |

The candidate's replacement builder/array sites are approximately 479 B/op combined in this
sample. `HttpHeader.<init>` and `HttpRequestBase.marshalForJni` remain, as intended.

Dedicated-host timing used seven paired, reversed-order repetitions at 200k/30k, concurrency one,
with async CRT small-get/small-put as targets and Apache5 sync as a control (56/56 runs passed):

| client | scenario | app CPU delta | paired spread | latency delta | latency spread |
|---|---|---:|---:|---:|---:|
| v2-async | small-get | +0.3% | ±3.5% | +0.4% | ±2.2% |
| v2-async | small-put | +0.2% | ±2.5% | −0.0% | ±1.8% |
| v2-sync *(control)* | small-get | +1.7% | ±2.3% | +1.3% | ±2.4% |
| v2-sync *(control)* | small-put | +1.2% | ±5.3% | +0.9% | ±3.9% |

All 28 async timing runs still had JIT compilation in the measured window, so their CPU values are
not independently claimable. Latency and both controls likewise place the change below the timing
resolution floor.

**Kept as an allocation-only improvement.** E12 removes the exact profiled adapter mechanism and
reduces total small-get allocation without a measurable timing effect.

The E12 profile also ranks the two contained auth follow-ups. The no-change
`AuthSchemeResolver.doApplyInterceptorModifiedProperties` path accounts for approximately 494 B/op,
mostly copied `HashMap` storage, while the always-created discarded-reasons `ArrayList` is only about
30 B/op. The next isolated phase should lazily create the interceptor-property builder and copy
properties only after the first actual modification; lazy discarded diagnostics should follow later.

## Phase E13 — lazy interceptor auth-property rebuilding

- Commit: `6d81ac812ae` (`perf(sdk-core): Rebuild auth properties lazily`)
- Paired timing: `paired/host-20260903-2014`
- Allocation profiles: `raw/host-e13-alloc-20260903-204915`

E13 removes an eager no-change rebuild in `AuthSchemeResolver.applyInterceptorModifiedProperties`.
When the pre- and post-interceptor snapshots share the same immutable auth option, the method now
returns before traversing properties or allocating a consumer, builder, copied property maps, or
boolean holder. For distinct snapshots it traverses normally but creates the endpoint-resolved
option builder only after the first signer property that differs under `Objects.equals`.

The change preserves the existing null contract and fail-fast behavior, non-null-to-null overrides,
unordered multiple-property application, current-only signer and identity properties, scheme ID,
identity future, and signer. `AuthSchemeResolverTest` now has thirteen focused cases. The S3 sync and
async execution-attribute compatibility suite passed five cases. The full sdk-core rerun passed
1,565 JUnit and 651 TestNG/TCK tests after an unrelated timeout test passed its isolated rerun;
checkstyle, SpotBugs, dependency analysis, Java 8 compilation, the 25-module dependency build, and
the consistent 53-module benchmark build passed. Semantic review approved the final change.

Equal 35,000-operation small-get profiles confirm the exact target disappeared:

| client | E12 `doApplyInterceptorModifiedProperties` | E13 | delta |
|---|---:|---:|---:|
| v2-sync | 360 B/op | **0** | **−360 B/op** |
| v2-async | 225 B/op | **0** | **−225 B/op** |

Whole-process sampled allocation moved in conflicting directions (sync approximately −2.3 KB/op,
async approximately +45 B/op), reflecting profile-JVM sampling/setup noise outside the changed
method. No total-allocation claim is made; retention is based on the exact method subtree reaching
zero and the source-level common-path early return.

Dedicated-host timing used seven reversed-order repetitions at 200k/30k and concurrency one, with
V2 sync/async as targets and smithy-java as an unaffected control (42/42 runs passed):

| client | scenario | app CPU delta | paired spread | latency delta | latency spread |
|---|---|---:|---:|---:|---:|
| v2-sync | small-get | +0.1% | ±4.8% | +0.2% | ±3.5% |
| v2-async | small-get | −1.1% | ±1.6% | −0.8% | ±1.3% |
| smithy *(control)* | small-get | +0.3% | ±8.8% | +0.9% | ±6.8% |

All fourteen async runs had JIT compilation in the measured window, so the async CPU direction is
not independently claimable. Sync and control results are neutral.

**Kept as an exact allocation-mechanism improvement.** Timing remains below resolution and no broad
total-allocation claim is made.

The E13 candidate profiles expose the next larger contained auth target. Checksum-free requests
write an explicit null `CHECKSUM_ALGORITHM` into the compatibility auth option. That write copies the
option once, and because the generated DynamoDB option has no checksum entry, the null entry then
forces `mergePreExistingAuthSchemeProperties` to rebuild again (profile estimates approximately
764 B/op sync and 449 B/op async). The next isolated phase should make a null checksum write a no-op
when the current property already reads null, while preserving the existing behavior that a null
write clears a non-null checksum. This ranks ahead of the eager discarded-reasons list (~30 B/op).

## Phase E14 — reading unions without a builder

- Commits: `a4ac3496b07` (fixture reformat, semantically inert), `09e00bc4578` (direct union factories for
  every union + non-copying variants), `c9e8722cd97` (the read path uses them)
- Raw: `raw/e2-jmh/host-g11` (component, flat — see below), `paired/host-20260904-0452` (e2e, 7 reps)

### The mechanism, and why the component benchmark could not see it

A union's `$readJson` allocated a builder, pushed members through `readStruct`, and called `build()`.
`readStruct` hands each member to a consumer, and generated code supplies **one consumer implementation
per shape**, so the call site inside the reader is megamorphic across a real client. Two costs follow:
it cannot be inlined, so the builder handed to it escapes and cannot be scalar-replaced; and every
member pays a virtual dispatch on top. The batch-get profile showed 4.4% of client CPU in
`AttributeValue$BuilderImpl.<init>` and 2.5% in itable stubs, with ~275 `AttributeValue`s materialized
per 25-item BatchGetItem response.

This resolves the contradiction phase E5 left behind. E5 measured the union builder as costing exactly
nothing — allocation identical to the byte — and concluded escape analysis had already deleted it. That
was true *of that harness*: a JMH benchmark exercising one shape makes the consumer call site
monomorphic, so the builder really is free there. It is not free in a client with hundreds of shapes.
**The component benchmark is structurally blind to this optimization**, and duly reported flat again
here (`host-g11`: time noise, allocation identical to the byte). Only the e2e measurement can see it.

### What changed

`StructuredJsonReader` gained `beginStruct()` and `nextMember(table)` — a caller-driven alternative to
`readStruct`, implemented in both readers. A union's `$readJson` now drives the member loop itself,
keeps the value in a **local**, and constructs once through the direct factories: `createX` for a scalar
member, `createXUnsafe` for a collection, adopting the collection the parser just built instead of
copying it (the same provenance argument as E4).

The enabling commit generalized those factories: they were opt-in per shape via the
`generateDirectUnionConstructors` customization, so in practice only DynamoDB's `AttributeValue` had
them. They now apply to every union that can support them — a `Model` shape whose collection members all
have copiers — and collection members additionally get the non-copying `Unsafe` variant, named after the
`SdkBytes.fromByteArrayUnsafe` precedent. The customization is deprecated and its entries removed.

Malformed documents are reproduced rather than rejected, which is what lets this be one path with no
fallback and no reader rewind (the Jackson-backed reader could not support rewind): no members set gives
`UNSET_INSTANCE`; more than one member gives the positional constructor with a **null type**, exactly
what `handleUnionValueChange` leaves behind on seeing a second member.

### Measurement

| client | scenario | base | direct | delta | spread | wins | latency |
|--------|----------|-----:|-------:|------:|-------:|-----:|--------:|
| v2-sync | batch-get | 391.8 | 328.9 | **−16.1%** | ±0.9% | 7/7 | −14.4% |
| v2-async | batch-get | 447.5 | 384.8 | **−14.0%** | ±1.5% | 7/7 | −13.3% |
| v2-sync | small-get | 105.5 | 103.5 | −1.9% | ±2.9% | 4/7 | −1.3% |
| v2-async | small-get | 155.9 | 151.1 | −3.1% | ±3.3% | 5/7 | −2.4% |

Well beyond the −3…−6% predicted, against a batch-get floor of +0.3% at 4/7. The harness was identical
across arms, so the SDK is the only difference. small-get moves less because a GetItem response carries
about ten attribute values rather than 275 — the gain tracks union count, which is the signature of the
mechanism.

Cumulative sync batch-get against unmodified 2.54.0 (664.7 µs): now ~329 µs, **≈ −50%**.

### Correctness

Differential, at the level where the risk is. Every document in
`FastJsonStructuredReaderDifferentialTest` — including its 500 randomized ones and all the whitespace,
escape and malformed cases — is now additionally read through `beginStruct`/`nextMember` on **both**
readers and asserted equal to the `readStruct` result. Mutation-verified three ways: not skipping a
null-valued member, mis-setting the first-member flag, and not skipping an unknown member's value each
fail the suite. Generated builders keep `readJsonFields`, so the builder path remains available and is
what the differential compares against. protocol-tests 726 exercises real generated unions end to end.

### Follow-ups

- **Non-union structures still use `readStruct`** and so still pay the megamorphic dispatch and an
  escaping builder. The same treatment applies, but a structure sets many members at once, so it needs
  locals plus a positional constructor per shape rather than single-member factories — the Option B
  shape. Given the −16% this bought on unions, that is now the most valuable thing in the queue.
- The `Unsafe` factories have no other caller yet. dynamodb-enhanced's converters build collections they
  own (`JsonNodeToAttributeValueMapConverter.visitObject`, `StaticImmutableTableSchema`) and could adopt.

---

## Phase E15 — smithy-java's latin1 + SWAR string write: rejected, and the reason generalises

Asked for directly: port smithy-java's write-side string optimisation, which reads a `String`'s internal
latin1 byte array and copies it to the wire eight bytes at a time behind a SWAR escape mask. Estimated
prize was the remainder of `writeQuotedString`'s share of batch-put, 8–15%.

**Not committed.** Raw: `raw/e2-jmh/host-e15` (component, rep 1 of 3; run stopped early, see below).

### Two corrections to the original framing

An earlier note in this document gave two blockers: Java 8 has no `VarHandle`, and reaching
`String.value` needs `sun.misc.Unsafe`. The first is wrong and the second understates the problem.

1. **Java 8 was never the blocker.** `sun.misc.Unsafe` supplies both halves at Java 8 source level:
   `objectFieldOffset`/`getObject` for `String.value` and `coder`, and `getLong` for the SWAR word reads.
   `VarHandle` is the tidy modern spelling, not a requirement.
2. **The real blockers are three, and each is independently fatal for this repository.**
   - `sun.misc.Unsafe::objectFieldOffset` is *terminally deprecated*. On JDK 24+ the runtime prints a
     warning naming the calling library on first use, and a future release removes the method. This
     session's own build logs contain that warning, emitted by Guava and ByteBuddy on the JDK 25 in use.
     Shipping it would put a scary warning attributed to the AWS SDK in every customer JVM on 24+.
   - The build forbids it outright. `maven.compiler.release=8` restricts javac to the documented API, so
     `package sun.misc does not exist` — a compile error, not a warning. That setting is what backs the
     Java 8 compatibility promise, so working around it is not a local decision.
   - There is no precedent: `sun.misc.Unsafe` appears nowhere in the SDK's main sources (`core`, `utils`,
     `http-clients`, `services-custom`).

   Reflection or `MethodHandles` could dodge the compile error, but not the deprecation, and per-word
   reflective reads would defeat the entire purpose.

### The sound subset was measured, and it is a regression

The technique decomposes into two wins: (a) bulk-copying the bytes instead of storing them one at a
time, and (b) scanning eight bytes per iteration instead of four chars. Only (b) strictly needs the
internal array. (a) has a supported route: `String.getBytes(int,int,byte[],int)` copies the low byte of
each char with no allocation, and on JDK 9+ for a latin1 string it is a plain `System.arraycopy`. Its
usual hazard — silently truncating chars above `0xFF` — cannot arise if only the prefix the scan has
already proven to be below `0x80` is copied.

So `writeQuotedString` was split: scan for the verbatim-copyable prefix (store-free), then copy that run
in one move. Four arms, two jars, `JsonRpc10MarshallBenchmark`, alternating per rep, `taskset -c 32-47`.
`bulkCopyMin` selects the shortest run copied with `getBytes` rather than per-char stores.

Rep 1, % change vs base (negative is better):

| case | bulk0 (always bulk) | bulk16 (gated) | nobulk (split only) |
|---|---:|---:|---:|
| MixedItem_L | +7.4% | +29.4% | +29.8% |
| MixedItem_M | +7.5% | +29.1% | +29.4% |
| MixedItem_S | −0.1% | +19.2% | +14.3% |
| ShallowMap_L | +6.4% | +21.7% | +22.4% |
| ShallowMap_M | +0.4% | +14.4% | +15.0% |
| ShallowMap_S | +3.8% | +17.4% | +15.1% |
| Nested_L | −3.6% | +10.3% | +9.8% |
| Nested_M | −0.6% | +17.8% | +11.8% |
| GetItemInput_Baseline | +0.2% | +4.1% | +7.1% |
| PutItemRequest_Baseline | −4.0% | +5.5% | +1.8% |

Stopped after rep 1 of 3. The effects are +10…+30%, an order of magnitude outside the ±1–2% this host
has shown across seven-rep collections, and all three candidate arms agree in direction and rank. The
remaining host time was worth more on the next phase than on tightening an interval around a clear
rejection.

### Why, and why it generalises

**Splitting the scan from the copy turns one pass over the string into two.** Base (E6) reads each char
once and stores it. `nobulk` reads every char twice — once to scan, once to copy — and pays +10…+30% for
it. `bulk16` is no better because DynamoDB strings are mostly shorter than 16 chars, so they take the
per-char copy path anyway; the gate protects nothing. `bulk0` is only *near* neutral because `getBytes`
makes the second pass a memcpy rather than a `charAt` loop — and it still loses 6–7% on the map-heavy
cases, which are the ones batch-put is made of.

The general lesson, and the reason the remaining prize should not be chased in another form: **the SWAR
win and the bulk-copy win are not separable.** Both are consequences of holding the byte array, where
the scan and the copy read the same cheap array and the second pass is nearly free. Extracting only the
sound half forces an extra pass over `charAt` and costs more than the stores it removes. There is no
partial-credit version of this optimisation, so the write side is done at E6 until the byte array is
reachable by supported means.

The word-at-a-time technique itself is *not* dead — it just belongs on the **read** side, where the
parser already owns a `byte[]`. SWAR there needs no internals at all and no `Unsafe`: a cached
`ByteBuffer` wrapping the reader's own buffer gives intrinsified `getLong` at Java 8 source level. That
is the form worth trying, and E8's failure (four table lookups per group) does not predict it.

### Gates
aws-json-protocol 158, checkstyle 0, dynamodb 61. protocol-tests 726 with one error, `QueryExceptionTests`
`SocketException: Connection reset` inside WireMock's own client — infrastructure, and 18/18 clean on
re-run. codegen-generated-classes-test 1981 with the 6 known-pre-existing `DelegatingAsyncClientTest`
errors.

---

## Phase E16 — smithy-java's HTTP client as an SDK transport

The cross-SDK breakdown (`analysis/crosssdk-254/report.md` §3.0) put smithy-java's transport at 18.7 µs/op
and 1.8 KB on small-get against v2-sync's 54.5 µs and 13.4 KB. Splitting that by profile category, the
HTTP client framework itself was roughly 7 µs/op for smithy against roughly 32 µs for Apache5 — about 16%
of v2-sync's small-get client CPU. This phase makes smithy's client an SDK HTTP client so that difference
can be measured *inside* the SDK pipeline instead of inferred across two different stacks.

- Commits: `b1cd7d1a67f` (the client), `422bfc449e7` (the benchmark arms)
- Raw: `raw/e16-smithyhttp/all-results.txt`
- New module: `http-clients/smithy-http-client`, artifact `smithy-http-client`

### What was built

`SmithyHttpClient` (sync) and `SmithyAsyncHttpClient` (async), neither registered in
`META-INF/services`, so nothing enters the default-client priority table and both are reachable only by
being passed to a builder explicitly.

The synchronous client is a faithful mapping — smithy's client is blocking, which is the shape of
`SdkHttpClient` — and passes the shared `SdkHttpClientTestSuite`. **The asynchronous client is a bridge,
not a port**: smithy's HTTP client contains no `CompletableFuture` anywhere, so each request runs on a
virtual thread. That is smithy's intended execution model, but it is a hand-off a natively asynchronous
client does not pay, and the response body is buffered before publishing. Its numbers are a bridge's
numbers.

### Measurement

One jar, four client arms. `v2-sync-smithy` and `v2-async-smithy` are identical to `v2-sync` and
`v2-async` except for the transport, so each pair isolates the HTTP client with the rest of the pipeline
fixed — a cleaner comparison than the existing `smithy` arm, which differs in serialization, signing and
client framework too. 200,000 iterations, 30,000 warmup, 7 repetitions, client pinned to 32–47 and server
to 0–15, client order reversed on even repetitions so drift across a repetition cannot masquerade as a
transport difference. 112 results, no failures.

**Verified before trusting any of it:** `Epoll.isAvailable() == true` on the host (Linux aarch64, JDK
25.0.4) and smithy's `EpollAccess` bridge loads. Without that check a silent fallback to smithy's
portable socket transport would have measured the wrong thing entirely — and it does silently fall back,
which is how the local macOS smoke run produced a +8% batch-get "regression" that did not reproduce here.

app CPU µs/op, mean of 7 paired repetitions:

| scenario | v2-sync (Apache5) | +smithy | delta | spread | wins | latency delta |
|---|---:|---:|---:|---:|---:|---:|
| small-get | 101.8 | 94.6 | −7.0% | ±7.3% | 7/7 | −5.4% |
| small-put | 97.9 | 89.9 | −8.0% | ±7.1% | 7/7 | −5.6% |
| batch-get | 334.4 | 327.8 | −2.0% | ±3.2% | 5/7 | −2.5% |
| batch-put | 303.6 | 300.0 | −1.1% | ±4.2% | 5/7 | −1.6% |

| scenario | v2-async (CRT) | +smithy | delta | spread | wins | latency delta |
|---|---:|---:|---:|---:|---:|---:|
| small-get | 152.5 | **126.4** | **−17.1%** | ±2.7% | 7/7 | −15.1% |
| small-put | 148.1 | 129.3 | **−12.7%** | ±3.9% | 7/7 | −11.8% |
| batch-get | 393.1 | 378.3 | −3.8% | ±1.8% | 7/7 | −4.2% |
| batch-put | 349.0 | 336.6 | −3.6% | ±1.9% | 7/7 | −5.6% |

Per-repetition small-get deltas, which matter for how much to believe each number:

```
sync :  -10.5  -3.7  -2.1  -7.3  -9.3 -15.3  -0.7     mean  -7.0%
async:  -15.9 -17.3 -20.1 -14.7 -17.0 -18.8 -15.7     mean -17.1%
```

**The async result is solid**: every repetition between −14.7% and −20.1%, spread far below the effect.
**The sync result is a reliable win of poorly determined size**: every repetition is negative, and 7/7 in
one direction is a 1-in-128 coincidence per scenario, but the magnitude ranges from −0.7% to −15.3%, so
"−7%" should be read as "somewhere around 5–10%".

Batch scenarios barely move in either, which is the expected shape: transport is a fixed per-request cost,
so it is a large share of a small operation and a rounding error next to marshalling 25 items.

### Why sync fell short of the predicted 16%

The prediction came from comparing whole stacks, and two things that comparison folded into "transport"
do not move when only the client is swapped.

1. **Much of V2's transport cost is V2's, not the client's.** Building `SdkHttpFullRequest`, the header
   multimap, `ContentStreamProvider`, `AbortableInputStream` wrapping and metric collection all stay
   exactly where they were. Only the part below that boundary was replaced.
2. **An adapter pays a translation the native stacks do not.** Every request converts
   `SdkHttpRequest` → smithy `HttpRequest` (a `SmithyUri.of` parse plus a header copy) and every response
   converts back. That cost is proportional to header count and is *added* to smithy's cheap transport, so
   the achievable saving is smithy's advantage minus this translation. This is a hypothesis consistent
   with the numbers, not something the profile has confirmed yet — see follow-ups.

The general point is worth keeping: **a transport's advantage is partly an advantage of the request and
response types it consumes natively.** Reaching the full 16% would mean pushing smithy's types further up
the SDK pipeline, not just plugging its socket layer in underneath.

The async gain is larger mostly because its baseline is worse: CRT costs 152.5 µs/op on small-get against
Apache5's 101.8 in this harness at concurrency 1, so there is more to recover. **That is a finding
independent of smithy: the SDK's async small-operation cost carries roughly 26 µs/op that a blocking
client on a virtual thread does not**, and it is worth understanding on its own.

### Constraints, all inherited from the dependency

- **Java 21.** smithy-java publishes class file major version 65, so the module sets `release 21` while
  every other module targets Java 8. This alone rules the client out as a default.
- **Linux for the fast path.** smithy reaches epoll through Netty's *package-private* internals, in a
  class that lives in `io.netty.channel.epoll`. The split package rules out the module path, a Netty
  upgrade can break it, and elsewhere it silently falls back to a portable socket transport.
- **`trustAll` covers issuer trust only.** smithy's `JdkTlsProvider` unconditionally forces
  `setEndpointIdentificationAlgorithm("HTTPS")` after copying caller-supplied `SSLParameters`, so a
  certificate whose name does not match the host still fails. Overriding it would mean supplying a custom
  `TlsProvider`, and one that did not report `supportsEpoll()` would silently drop the client onto the
  slow transport — a worse failure than the limitation.
- Consequently the shared **async** conformance suite cannot run against this client at all: it drives
  every request over HTTPS against a self-signed certificate. Behavioural coverage was written over HTTP
  instead, which is also the configuration measured here, so **this client's TLS behaviour is essentially
  untested**.

### Correctness
`SdkHttpClientTestSuite` 20/20 for the sync client (with `testTrustAllWorks` overridden and documented as
unsupported), 9 behavioural tests for the async client covering status, headers, body integrity at 512 KB,
HEAD, connection reuse, request bodies and failure propagation. Checkstyle 0, spotbugs 0.

Four bugs the SPI conformance work exposed, all fixed: HEAD responses must expose no body *and* must not
be read (reading blocks for bytes that never arrive); smithy wraps TLS handshake failures in a generic
`IOException`, so the handshake exception is pulled back out of the cause chain for the retry policy;
a rejected execution on a closed async client was thrown synchronously instead of reported through the
returned future; and request headers were being copied with `headers()`, which deep-copies on every
request — caught by the SDK's own spotbugs rule, and exactly the sort of per-request cost this client
exists to remove.

### Follow-ups
- **Profile the sync arm to test the translation hypothesis.** If `SmithyHttpClient`'s conversion shows up
  as a meaningful frame, the adapter boundary is the ceiling and the number is what it is; if it does not,
  the missing saving is elsewhere and worth chasing.
- **CRT's 26 µs/op on small operations.** The async comparison says more about the current async default
  than about smithy. Worth its own investigation at concurrency 1 and higher.
- Not shippable as-is, for the reasons above. What it establishes is the size of the prize and where the
  boundary of an adapter-shaped solution lies.

## Investigation — where CRT's async small-operation premium lives

E16 left a follow-up: at concurrency 1, `v2-async` on CRT costs ~26 µs/op more than the same client
on smithy's blocking transport bridged over a virtual thread, and ~50 µs/op more than sync Apache5.
This investigation attributes that premium. It is an investigation, not a phase — no SDK change, no
verdict to keep or revert.

- Raw: `raw/host-crt-invest-20260905-154639` (local copy; also on the host under
  `~/racecar/runs/crt-invest-20260905-154639`), jar `racecar-e16-smithyhttp-422bfc449e7`.
- **Host note:** the original c6g.metal was terminated; this and all later runs are on a
  reprovisioned instance of the same type and setup (i-0cec9172b237e9113, JDK 25.0.4,
  `ec2-52-87-223-88.compute-1.amazonaws.com`). Within-session comparisons are unaffected; absolute
  numbers may drift a percent or two against older sections.

### Method

One profiled JVM per arm (`small-get`, concurrency 1, 300k iterations, quiescence warmup, pinned
32–47 / server 0–15, standard TUNE args), async-profiler CPU in JFR, converted to collapsed stacks.
Attribution is by **self frame**, grouped into functional buckets, rather than by whole-stack
category: response processing runs on whatever thread completes the future, so stack-marker
categorization bleeds SDK work into transport buckets — self-frame grouping does not. `epoll.available`
verified `true` before trusting the smithy arm. Absolute µs/op figures scale each profile's shares by
its own run's `app_cpu_us_per_op`; both async runs carried residual JIT (3.9% and 2.7% of wall,
flagged not-steady, chronic for async), so treat the µs values as ±a few percent. The check that the
method holds: the SDK-side groups (`sdk-java` + `jdk-stdlib`) come out equal across all three arms —
66.4 / 64.4 / 66.0 µs/op — exactly as they should, since the pipeline above the transport is
identical.

### Result

`app_cpu_us_per_op`: CRT 154.8, smithy-http 130.3, sync Apache5 110.1. Premium under attribution:
CRT − smithy = 24.5 µs/op. Self-frame groups, µs/op:

| group | CRT | smithy-http | sync apache5 |
|---|---:|---:|---:|
| sdk-java + jdk-stdlib (pipeline, serde, signing…) | 66.4 | 64.4 | 66.0 |
| syscall-io (send/recv/read/write/epoll) | 28.8 | 16.4 | 21.9 |
| thread-signal (futex/cond/park) | 10.5 | 17.2 | 0.1 |
| async-machinery (CompletableFuture, FJP, continuations) | 3.6 | 13.4 | 0.0 |
| crt-native-code (self time inside libaws-crt-jni) | 15.3 | – | – |
| jni-bridge + native-mem (transitions, malloc/free) | 7.6 | 0.3 | 0.1 |
| other-native | 16.0 | 10.6 | 18.1 |
| jvm-stubs | 4.2 | 2.9 | 3.9 |

The premium decomposes as: **+15.3 native code execution, +7.3 JNI transitions and native
malloc/free, +12.4 extra syscalls, +5.4 other-native, minus a 16.5 µs coordination advantage** —
CRT's event-loop handoff (14.1 µs of signal + future machinery) is actually cheaper than the virtual
thread bridge's (30.6 µs). Sums to ~24 µs against the measured 24.5.

The syscall shape explains itself: the smithy arm's blocking socket does essentially
send + recv per operation (11.3 + 3.0 µs, epoll 1.1). CRT's cross-thread submission does
send (12.4) **plus a full event-loop wake cycle per operation** — epoll_pwait 6.7, an
eventfd-style write 2.8, a separate read 4.7. At concurrency 1 every request pays the whole wake
cycle; under load those wakes batch, which is presumably where this design earns its keep.

Allocation is not part of the story: 41.4 vs 40.8 KB/op whole-process sampled bytes over equal
35k-operation runs.

### What this settles

- **The premium is below the Java boundary.** The SDK adapter's own self time is ~2.5 µs/op — E12
  already trimmed it, and there is nothing meaningful left to take on the Java side. No SDK-side
  change can recover the ~24 µs; it belongs to CRT's native execution, its JNI crossings, and its
  per-request wake pattern at low concurrency.
- **CRT's coordination model is good** — better than a virtual-thread bridge at handing work across
  threads. What it pays for that is native-side cost that a pure-Java blocking transport simply does
  not have at this concurrency.
- Practical reading for the transport strategy: at low concurrency, a lean blocking client (on a
  platform or virtual thread) is structurally cheaper per operation than any event-loop transport
  crossed per request; CRT's costs should amortize with concurrency, and a follow-up sweep
  (CRT vs smithy arm at c = 4/16/32) would show where the lines cross. That, not adapter work, is
  the decision-relevant next measurement.

## Phase H1 — flat strided-array request header storage

The first slice of the "compact flat-array HTTP headers" strategic item (sol report §7.1): replace
the representation behind `DefaultSdkHttpFullRequest`, keep every public contract, change no
consumer. The point of this phase is as much to prove the representation swap is safe and cheap as
to bank its direct win — the direct consumer paths (signer, transport adapters) build on it.

- Commits: `b440db2c035` (the store), `1a3cea4f721` (drop the `Lazy` wrapper from the map cache;
  see mechanism check below)
- Raw: `paired/host-20260905-1623` (small ops, 7 reps, 84 runs), `paired/host-20260905-1727`
  (batch, 5 reps, 40 runs), `raw/host-flathdr-alloc-20260905-180306` (mechanism profiles)
- Jars: `racecar-flatHdrBase-bdc9a156fb1` vs `racecar-flatHdr-b440db2c035`, identical harness
  commit, both smoke-tested locally before deploy

### What changed

`StridedHeaders` (new, `http-client-spi` internal): request headers as a flat `String[]` of
alternating name/value pairs, sorted case-insensitively and stable within a name, with the same
`ForBuilder`/`ForBuildable` copy-on-write split as `LowCopyListMap`. The TreeMap representation paid
a red-black node per header on every lookup and **a full tree clone on the first mutation of every
builder derived from a built request** — a boundary the pipeline crosses 3–4 times per call
(transaction-id stamp, signer, async content-length round trip). That transition is now one array
copy; lookups are binary searches; `headers()` still materializes a lazy, cached, deeply
unmodifiable case-insensitive `TreeMap` so external readers see exactly what they saw before.

Behavior parity was pinned in a 31-test suite (aliasing matrix, casing retention on replace,
empty-value-list sentinel, multi-value order, sorted iteration, case-insensitive map `get`,
equality across casings). One deliberate delta: `Builder.headers()` returns a per-call snapshot
rather than a live unmodifiable view (not on any hot path; the legacy `SignerUtils.addHostHeader`
reads it once per signing pass on the non-fast path).

Query parameters keep `LowCopyListMap`; responses are untouched.

### Measurement (paired, host, 200k/30k small + 80k/15k batch, concurrency 1)

Application CPU per op, with the smithy arm as an untouched control:

| client | scenario | base | flatHdr | delta | spread | wins | latency |
|--------|----------|-----:|--------:|------:|-------:|-----:|--------:|
| v2-async | small-get | 155.2 | 150.7 | **−2.9%** | ±1.6% | 7/7 | −2.3% (6/7) |
| v2-async | small-put | 149.8 | 146.9 | −1.8% | ±5.3% | 4/7 | −2.0% |
| v2-sync | small-get | 108.3 | 107.2 | −0.9% | ±3.9% | 4/7 | −0.4% |
| v2-sync | small-put | 104.4 | 103.2 | −1.0% | ±6.1% | 4/7 | −0.2% |
| smithy (control) | small-get | 49.1 | 50.8 | +4.3% | ±12.4% | 3/7 | +1.5% |
| smithy (control) | small-put | 46.8 | 48.1 | +3.1% | ±6.2% | 3/7 | +1.8% |

Batch scenarios are neutral (−0.7% to +0.3%, all inside their pair spreads at 2–4/5 wins), which is
the expected shape: headers are a per-request fixed cost. The control reads zero within its (wide)
spread, certifying the session. Async small runs were flagged not-steady on 28/84 (chronic);
latency agrees with CPU there. The one clean cell is async small-get: −2.9% at 7/7 with ±1.6%
spread — above this rig's floor. Sync small ops read −1% at 4/7: consistent in sign, below the
floor, not claimable on timing alone.

### Mechanism check (equal 40k-op alloc profiles per arm)

| site / type (B/op) | sync base | sync flatHdr | async base | async flatHdr |
|---|---:|---:|---:|---:|
| `TreeMap$Entry` | 1,075 | 131 | 1,429 | 354 |
| stacks through `putHeader` | 1,783 | 642 | 3,342 | 1,285 |
| stacks through `LowCopyListMap` | 2,753 | 1,153 | 2,595 | 1,088 |
| stacks through `DefaultSdkHttpFullRequest` | 5,348 | 3,932 | 7,392 | 5,308 |
| **profile total** | 34,426 | 34,518 | 41,045 | **38,345** |

The TreeMap machinery is gone as designed. Async banks the full −2.7 KB/op because it crosses the
mutation boundary once more per call (the content-length round trip in `MakeAsyncHttpRequestStage`)
and re-reads headers in the CRT adapter; sync's savings were offset almost exactly by the new
store's own costs — splice copies (+446 B/op of `String[]`), per-build `ForBuildable` + `Lazy` +
lambda (~650 B/op), and `forEach` singleton wrappers. The `Lazy` + lambda part (~400 B/op) was
avoidable and `1a3cea4f721` removes it (exact-mechanism change, verified in the profile sites, not
separately host-measured).

### Verdict

Kept. The async −2.9% CPU at 7/7 plus the exact mechanism removal carries it; sync is
allocation-neutral and timing-neutral-to-slightly-positive, i.e. the representation swap is free
where it doesn't yet pay. That is the result this phase needed: the strided store is behaviorally
safe (81 pinning tests, sdk-core 651, protocol-tests 726, signer differential, japicmp clean) and
costs nothing, so the consumers can now be moved onto it one commit at a time.

### Follow-ups, in value order

1. **Direct consumer paths (H2).** Expose per-entry iteration (`forEachEntry(String,String)`-shaped,
   internal) plus a bulk path for the signer: `FastV4HeaderSigner.collectSourceHeaders` can filter-copy
   the strided array straight into `V4SigningResources`' strided buffer (same layout, no `List`
   wrappers, no lambda), and the Apache5/CRT adapters can iterate entries without per-name list
   materialization. The remaining `forEachHeader` wrapper costs (~0.7–3 KB/op) and the signer's
   collection pass are the target.
2. **Response-side store.** Response headers are built once by the transport and read by
   metadata/unmarshalling; the same representation applies, with the `Serializable`/transient quirk
   of `DefaultSdkHttpFullResponse` to preserve.
3. **Query parameters**, for query-protocol services (out of DynamoDB's blast radius).

## Investigation follow-up — the CRT premium does not amortize with concurrency

The attribution above ended with a hypothesis: CRT's per-request event-loop wake cycle should
amortize under load, so the ~24 µs/op premium ought to shrink as concurrency rises. A sweep tests
it directly: `v2-async` (CRT) vs `v2-async-smithy`, small-get, c = 1…32, in **both** drive modes —
in-flight (one submitter, N outstanding; known submitter ceiling) and join (N threads, no
submitter ceiling, thread-per-request overhead common to both arms). 300k iterations per point,
client pinned to 32 cores (16–47) fixed across levels, server 0–15.

- Raw: `sweeps/host-crt-sweep-20260905-183815` (24 runs, jar `racecar-e16-smithyhttp-422bfc449e7`)

CRT's application CPU per op relative to smithy's, and the throughput ratio:

| mode | c=1 | c=2 | c=4 | c=8 | c=16 | c=32 |
|---|---:|---:|---:|---:|---:|---:|
| inflight, CPU premium | +27.7 (1.22×) | +18.4 | +30.7 | +31.1 | +27.9 | +32.1 (1.31×) |
| join, CPU premium | +32.2 (1.26×) | +30.9 | +29.3 | +17.0 | +37.3 | +30.9 (1.19×) |
| inflight, throughput | 0.82× | 0.89× | 0.81× | 0.71× | 0.72× | 0.70× |
| join, throughput | 0.82× | 0.82× | 0.85× | 0.87× | 0.80× | 0.86× |

**The hypothesis is rejected.** The premium is ~17–37 µs/op at every level in both modes, with no
downward trend — it is per-operation and structural, not a fixed cost being divided across more
in-flight work. Absolute throughput tells the same story from the other side: in-flight mode
plateaus at ~26k ops/s for CRT against ~36k for smithy (both submitter-bound, but the ceiling
differs because per-op submit cost differs), and in join mode both scale to c=32 with smithy ahead
throughout (129k vs 111k ops/s).

Caveats, honestly stated: every async run at 300k iterations on these cores carries residual JIT
(all 24 flagged not-steady), so absolute CPU values are a few percent contaminated — but the
cross-arm comparison at each level is like-for-like and the effect is an order of magnitude above
that. One run per cell, no reps; at 13–31% the effect dwarfs the measured ±2.5% floor. And the
scope qualifier matters: this is plaintext HTTP/1.1 over loopback. CRT's value proposition includes
TLS (aws-lc), connection management at scale, and off-heap memory behavior — none of which this
apparatus measures. What it does establish: **on per-operation CPU efficiency for small requests,
plaintext, the CRT transport is structurally more expensive than a lean blocking client at every
concurrency level tested, and the gap does not close under load.** Any TLS-inclusive re-run should
be done before drawing transport-strategy conclusions beyond this workload.

## Phase H2 — per-entry header iteration for flat-stored requests

The consumer half of H1: requests backed by the strided store now expose per-entry iteration
(`FlatHeaderAccess.forEachHeaderEntry`, internal), and the three hot-path consumers use it with
their `forEachHeader` fallbacks intact — `FastV4HeaderSigner.collectSourceHeaders` feeds the
strided signing buffer pair-by-pair, `Apache5HttpRequestFactory` adds headers per entry, and
`CrtRequestAdapter`'s array builder implements both consumer shapes. No per-name `List`
materialization, identical iteration order, names with empty value lists skipped (matching an
empty-list callback contributing nothing).

- Commit: `6a37d10d7b4`
- Raw: `paired/host-20260905-1855` (small, 7 reps), `host-20260905-1959` (batch-put, 5 reps),
  `raw/host-h2-alloc-20260905-201630` (mechanism profiles)
- Correctness: the signer differential suite exercises the fast path through real request objects
  (canonical request byte-identical by construction and by test); 3 new `forEachEntry` tests
  including an exact equivalence check against `forEach`; apache5/crt suites green; smoke 8/8 with
  metric-set identity.

### Measurement

Timing is **flat**: every small-op and batch cell within its pair spread (deltas −0.5% to +2.6%,
wins 2–5/7), smithy control zero. The mechanism moved as designed, at modest size — equal
40k-op profiles:

| B/op | sync base | sync H2 | async base | async H2 |
|---|---:|---:|---:|---:|
| stacks through `forEachHeader` | 1,062 | 524 | 3,473 | 2,687 |
| stacks through `StridedHeaders` (forEach wrappers) | 1,940 | 1,468 | 3,906 | 2,870 |
| signer header collection | 2,386 | 2,005 | 2,189 | 2,372 † |
| **profile total** | 33,050 | **32,578** | 38,817 | **37,323** |

† async signer site is sampling noise across arms; the async win concentrates in the CRT adapter
and forEach wrappers.

### Verdict

Kept, on the E12/E13 standard: an exact allocation-mechanism win (−0.5 KB/op sync, −1.5 KB/op
async) with no timing effect claimable at this rig's floor and no regression anywhere. The
0.5–1.5 KB/op is 1.5–4% of small-op allocation, which the established alloc→CPU conversion
(~1:4 at best on small ops) puts well under the ±2.5% CPU floor — the flat timing is the expected
result, not a disappointment. H1+H2 together close out the request-side header representation;
the remaining strided-store items are the response side and query parameters, both smaller.

## Phase H3 — one-object structure construction: negative result, reverted

Asked for directly as the next item after H1/H2: the sol report's §7.3 "one-object generated response
construction", i.e. give non-union structures the treatment E14 gave unions. E14 removed a union's
builder and its megamorphic member dispatch and bought **−16% CPU on batch-get**, so the structure
counterpart looked like the largest remaining codegen lever.

It is not. The mechanism was half achieved and bought nothing end to end.

- Commits: `f32c3064155` (the scenario — **kept**), `29e342d45e1` (the codegen change),
  `0b89a6bdd02` (revert)
- Raw: `paired/host-20260910-1548` (describe-table + small-put control, 7 reps),
  `host-20260910-1639` (batch-get + small-get, 5 reps),
  `raw/host-h3-prof-20260910-170716` (alloc + CPU profiles, both arms)
- Host note: measurements on the restarted c6g.metal (`ec2-34-226-246-228`), same setup.

### The existing scenarios could not have measured this, which is the first finding

A DynamoDB item is a map of `AttributeValue`, and `AttributeValue` is a **union**. So a
get/put/batch response materializes hundreds of unions and *essentially one structure* — the response
shape itself. Everything the four item scenarios can say about generated deserialization, E14 already
said. Nothing in the matrix could resolve a change to the structure path, and running it there would
have produced a confident-looking zero.

So the phase started by building a scenario that can: **`describe-table`**, still DynamoDB, still a
canned byte-identical response, but 4,347 bytes containing **45 nested structures and no unions** — a
`TableDescription`, 6 `AttributeDefinition`, 2 `KeySchemaElement`, a
`ProvisionedThroughputDescription`, 5 GSIs and 2 LSIs each with their own key schema, projection and
throughput, plus stream and SSE descriptions. Verified to deserialize rather than silently parse
empty (collection sizes, nested non-key attributes, per-index throughput, timestamp and SSE enum all
land). It is deliberately outside `--scenario all`, so no existing collection changes meaning.

The allocation profile confirms the design: **~46–49 builders per operation**, against ~1 for the item
scenarios.

### What changed, and what the JIT did with it

`$readJson` for a non-union structure drove the member loop in its own frame with `beginStruct`/
`nextMember` — E14's primitives — writing directly into a frame-local builder and ending in a
monomorphic `build()`. The builder was kept deliberately, on the argument that once it no longer
escapes it is free, and that defaults, `SdkField` metadata and the auto-construct sentinels stay in
one place. All 194 DynamoDB shapes moved to the new path.

That argument was wrong, and the profiles say so precisely:

| | base | candidate | delta |
|---|---:|---:|---:|
| itable stub (megamorphic dispatch), % client CPU | 4.51% | 2.84% | **−1.67pp** |
| `nextMember` frames, % client CPU | 0.00% | 5.08% | **+5.08pp** |
| `$readJson` frames, % client CPU | 14.62% | 16.66% | +2.04pp |
| `BuilderImpl` allocation, B/op | 1,953 | 1,848 | −105 (−5%) |
| total allocation, B/op | 42,159 | 42,959 | +800 (within cross-run noise) |

1. **The dispatch really was removed** — itable stubs fell by a third.
2. **The builder was not scalar-replaced.** Both arms allocate ~46–49 of them per operation. Removing
   the consumer boundary was supposed to let escape analysis delete it; it did not, so the premise for
   keeping the builder instead of generating positional constructors failed.
3. **The saving was spent driving the loop.** `nextMember` appears at 5.08% of client CPU where the
   base had none, and `$readJson` frames rise 2.04pp: `readStruct`'s internal loop is simply better at
   this than a generated caller-driven one calling back into the reader per member.

### Measurement

Whole-call application CPU per operation, paired, concurrency 1:

| client | scenario | base | H3 | delta | spread | wins |
|--------|----------|-----:|---:|------:|-------:|-----:|
| v2-sync | describe-table | 131.9 | 132.8 | +0.7% | ±3.0% | 4/7 |
| v2-async | describe-table | 174.0 | 177.8 | +2.2% † | ±3.3% | 2/7 |
| v2-sync | small-put (control) | 101.0 | 101.3 | +0.5% | ±5.2% | 5/7 |
| v2-async | small-put (control) | 147.0 | 145.6 | −0.9% | ±3.7% | 4/7 |
| v2-sync | batch-get | 329.7 | 328.9 | −0.2% | ±1.1% | 2/5 |

† async runs flagged not-steady; latency agrees in direction (+1.5%, 2/7).

The two independent quiescence-warmed profiling runs agree: 131.4 → 133.8 µs/op, both
`steady_state=true`. Nothing here is outside the floor, and every sign that is resolvable points the
wrong way.

### Reverted, and why the rest of Option B was not attempted

The obvious next move is to finish the job — generate positional constructors so the builder really
disappears. The numbers say don't bother: the builders are **1,848 B/op of 42,159**, i.e. 4.4% of
allocation on a response deliberately built to maximize them. At this project's measured
allocation→CPU conversion (~1:4 on small operations, worse on large ones) that is ~1% of the call,
against a ±3% floor — unmeasurable even if perfectly executed, and it would cost a positional
constructor per generated shape plus special handling for response shapes, which need
`super(builder)`.

**The sol report's sizing of §7.3 was drawn from the pre-E4, pre-E14 profile**, where the generated
model layer was 332 KB/op on batch-get and "a builder-to-immutable transition for every nested value"
was a real cost. E4 (collection adoption) and E14 (direct union construction) already collected that.
What remains is ~40 bytes per structure, and DynamoDB responses contain few structures.

### What is kept

- `describe-table`, which is independently valuable: the project had measured four shapes of one
  protocol, all union-dominated, and this is the first scenario that exercises structure
  deserialization at all. Any future codegen read-path work should be measured here.
- The finding that **escape analysis does not remove a generated builder even when the consumer
  boundary is gone.** That kills the "keep the builder, it's free once it doesn't escape" design for
  any future attempt: the only version of one-object construction worth building is the one that
  never allocates a builder.
- The corollary for method choice: **removing a megamorphic dispatch is not automatically a win.** Here
  the replacement cost more than the dispatch, and only an e2e paired measurement could show it — the
  same lesson E14 taught in the opposite direction, where a component benchmark was blind to the gain.

## The TLS-inclusive transport comparison

Every number in this document up to here was plaintext HTTP/1.1 over loopback. That left the
transport conclusion explicitly open, because plaintext removes the one dimension in which these
stacks differ most: **CRT encrypts natively in aws-lc, Apache5 and the smithy client go through the
JDK's SSLEngine.** The sweep's own caveat said a TLS re-run was a prerequisite for any
transport-strategy conclusion. This is that run.

- Commits: `feb0f8247b0` (an adapter bug this found — see below), `4f063479471` (the TLS harness)
- Raw: `raw/host-tls-compare-20260910-180611` (one jar, 4 transports × {plaintext, TLS} ×
  {small-get, batch-get}; 5 reps small, 3 reps batch, cells cycled within each rep and client order
  reversed on even reps so session drift cannot land on one cell)

### The apparatus, and the fairness checks that matter here

`--tls` generates a throwaway certificate and truststore with `keytool`, gives the keystore to the
server and the truststore to the client JVM. The certificate's SAN names `127.0.0.1` on purpose:
trusting an issuer and matching a hostname are separate things, and smithy's TLS provider forces
hostname verification regardless of configuration, so naming the endpoint is what lets every client
be measured without each one weakening verification differently.

Two checks, because assuming either would have invalidated the result:

- **Same crypto everywhere.** The server reports the negotiated suite from its own view. All four
  transports come out on `TLS_AES_256_GCM_SHA384`, so no part of the difference below is one client
  quietly using a cheaper cipher.
- **The one asymmetry, and why it is harmless.** CRT ignores the JDK truststore and the SDK exposes
  no CA hook for it, so CRT gets trust-all while the others verify a real chain. Certificate
  verification happens once per *handshake*, and these runs reuse pooled connections across 200,000
  operations, so that cost is divided by five orders of magnitude. It would matter for a
  handshake-per-request workload; that is a different measurement.

**Scope, stated plainly: this measures steady-state encrypted transfer on established connections.**
Handshake cost is amortized away by construction. A connection-churning workload is not covered.

### Result

Application CPU per operation, mean of reps:

**small-get** (≈500 B request, ≈500 B response — the fixed-cost scenario)

| transport | plaintext | TLS | TLS adds | as % |
|---|---:|---:|---:|---:|
| v2-sync (apache5, JDK TLS) | 106.6 | 118.8 | **+12.3** | +11.5% |
| v2-sync (smithy-http, JDK TLS) | 104.2 | 114.2 | **+10.0** | +9.6% |
| v2-async (CRT, native TLS) | 150.5 | 163.9 | **+13.4** | +8.9% |
| v2-async (smithy-http, JDK TLS) | 124.0 | 135.0 | **+11.0** | +8.9% |

**batch-get** (≈38 KB response — the byte-scaled scenario)

| transport | plaintext | TLS | TLS adds | as % |
|---|---:|---:|---:|---:|
| v2-sync (apache5) | 331.3 | 389.9 | +58.5 | +17.7% |
| v2-sync (smithy-http) | 325.7 | 386.9 | +61.2 | +18.8% |
| v2-async (CRT) | 400.0 | 449.7 | **+49.8** | +12.4% |
| v2-async (smithy-http) | 383.1 | 445.0 | +61.9 | +16.2% |

The comparison the run exists to settle:

| | plaintext | TLS |
|---|---:|---:|
| CRT vs smithy-http (sync), small-get | +46.3 µs (1.44×) | +49.7 µs (1.43×) |
| CRT vs apache5, small-get | +43.9 µs (1.41×) | +45.0 µs (1.38×) |
| CRT vs smithy-http (sync), batch-get | +74.2 µs (1.23×) | +62.8 µs (**1.16×**) |
| CRT vs apache5, batch-get | +68.6 µs (1.21×) | +59.9 µs (**1.15×**) |

### What it settles

1. **TLS does not rescue CRT on small operations.** The ratio moves from 1.44× to 1.43× against the
   blocking smithy client and 1.41× to 1.38× against Apache5 — and the *absolute* gap slightly
   widens. The earlier plaintext finding stands: for small requests, CRT's per-operation cost is
   structurally higher, and encryption does not change that.
2. **On large payloads native TLS is genuinely cheaper per byte.** CRT's TLS increment on batch-get
   is +49.8 µs against +58.5 to +61.9 for the three JDK-TLS arms — consistent across all of them, so
   it is aws-lc versus SSLEngine rather than noise. It recovers roughly 9–12 µs of a ~70 µs gap,
   narrowing the ratio to 1.15–1.16×. Real, directionally in CRT's favour, and about one sixth of
   the gap: it does not overturn the conclusion, it qualifies it.
3. **The per-request TLS tax is mostly not cipher work.** 10–13 µs/op for ~1 KB of payload is far
   more than AES-GCM on 1 KB can account for. The profile pair agrees: of Apache5's increment, only
   ~3.7 µs lands in crypto (`jdk-crypto` + intrinsics), with the rest in buffer and engine handling
   (+4.9 µs of JDK stdlib frames) and extra socket work (+3.2 µs). That is *why* the small-operation
   increment barely differs across stacks — they are all paying framing and plumbing, not
   arithmetic. On batch-get, where bulk encryption dominates, the stacks separate.
4. **Therefore the transport recommendation is payload-shaped, not TLS-shaped.** For small-request,
   high-rate workloads a lean blocking client remains the cheaper per-operation choice even
   encrypted; CRT's native TLS earns its keep as payloads grow. Nothing here speaks to connection
   churn, HTTP/2, or CRT's operational advantages at scale.

Async caveats unchanged: the CRT and smithy-async arms are chronically `steady_state=false` on this
host, so their absolute per-operation CPU is soft. Latency agrees on direction throughout (p50
increments +21.9 µs apache5, +22.0 µs CRT on small-get). Rep spread was 1.5–9.8%.

### The bug this found, which is the other reason to run it

Pointing the benchmark at HTTPS broke both smithy-transport arms outright: every request failed with
`Expected H2 connection but got HTTP/1.1`. The adapter mapped only its own `http2Enabled` flag onto
smithy's version policy and ignored the SDK's `PROTOCOL` option, so with neither set it inherited
smithy's default, which prefers HTTP/2 and offers `h2` first via ALPN. Against a server that does not
negotiate ALPN the connection stays on HTTP/1.1 while the pool has already committed to its H2 path.

**This client was the only SDK transport whose wire protocol changed as soon as the endpoint became
`https`** — exactly the TLS path E16 recorded as untested. Fixed in `feb0f8247b0`: `PROTOCOL` decides
when the explicit flag is unset, and the fallback is pinned to HTTP/1.1. TLS failure unwrapping was
broadened from `SSLHandshakeException` to any `SSLException` at the same time, since a name mismatch
arrives as `SSLPeerUnverifiedException` and is equally a certificate problem.

One conformance test was weakened as a consequence, documented on the override: smithy's HTTP/1.1
connect path keeps only the last address's failure, so on a host resolving to both `::1` and
`127.0.0.1` the surviving cause can be a connection failure rather than the certificate failure. The
request still fails closed; only the exception type is less specific.

## Interlude — the bridged-pipeline comparison lives in `pipeline_benchmark3/`

Between the TLS run and Phase H4 the optimized pipeline was put head-to-head with a prototype that
routes V2 clients through smithy-java's pipeline instead ("v2-bridged"), alongside v1, stock 2.54.0
and native smithy-java, all from one harness commit with process-isolated jars and runtime
assertions that each arm really runs the pipeline it claims. That report is
[`pipeline_benchmark3/results.md`](../pipeline_benchmark3/results.md); the one-line result is that
the bridge is 0.70×/0.67× the optimized pipeline on small ops (it does not run V2's per-call
contracts at all) and 1.44×/1.49× *more* expensive on batch (POJO-to-schema translation scales with
payload), while optimized V2 sits at 1.03×/0.96× of native smithy-java on batch. It also found that
the bridge returns DynamoDB `NULL` attributes with no member set. Two things from it drive the
phases below: the bridge's framework+signing win pointed at *plumbing*, not algorithms, as V2's
remaining small-op cost (H4, H5), and its stock-2.46-vs-2.54 baseline pair exposed a drift in the
stock SDK worth explaining (the investigation after H4).

## Phase H4 — auth-scheme and sign-request plumbing

Asked for as "optimize the signer even further, borrowing from smithy-java". The profile-driven gap
analysis said the *algorithm* was already at parity — `FastV4HeaderSigner` 7.76 µs/op against smithy's
`SigV4Signer` 7.50 — and the 12.9-vs-4.5 µs "signing" gap bench3 measured was almost entirely the SPI
plumbing around it: the auth-scheme option graph rebuilt per call (6.20 µs vs smithy's 1.39), the
`SignRequest` object graph (1.26), and the interceptor-context rebuild after signing (1.28). So the
signer was left alone and the plumbing was fixed.

- Commits: `291843a37c6` (auth-scheme option graph), `7fe1f0973d4` (interceptor-context swap),
  plus `8bab8409dd1`, a cherry-pick of upstream PR 7367 (see the investigation below for why)
- Raw: `paired/host-20260912-1753` (small ops, 7 reps), `host-20260912-1857` (batch-put, 5 reps),
  `raw/host-h4-profiles` (equal-op alloc + CPU profiles per arm), `paired/host-20260912-1926`
  (7367 on top of H4, 7 reps)
- Correctness: http-auth-spi 36, aws-core 321, http-auth-aws 440, sdk-core 2210, protocol-tests
  726, japicmp clean, smoke 10/10 with metric-set identity; 2 new test classes (`PlaceholderAuth
  SchemeTest`, `DefaultSignRequestPropertiesTest`) pin the identity of the cached graph with what
  the legacy path built and the read-through precedence rules

### What changed

Three per-call rebuilds of client-constant state became per-client state or no-ops:

1. **The placeholder auth scheme is built once per client.** The legacy `SERVICE_SIGNING_NAME` and
   `SIGNING_REGION` execution-attribute writes are *mapped*: each write constructs an `"unset"`
   `SelectedAuthScheme` carrying the values as signer properties, for interceptors and old-style
   signers that read them back. Both values are client constants, so the object graph (an option
   build, a copy of it to add the second property, a completed future, two sentinels) was identical
   on every call. `AwsDefaultClientBuilder` now resolves it once as a lazy internal option
   (`AwsInternalClientOption.PLACEHOLDER_AUTH_SCHEME`), built by replaying the same two writes on
   scratch attributes so it is identical by construction; `AwsExecutionContextBuilder` installs it
   with two plain puts when no request-level scheme is already present. A second mapped write,
   `signerChecksumWriteMapping`, was rebuilding the scheme for a null-over-null no-op; it now
   returns early.
2. **`SignRequest` reads signer properties through the `AuthSchemeOption`** instead of copying them
   into its own `HashMap` — `DefaultBaseSignRequest.BuilderImpl.signerProperties()` consults the
   option first, preserving the old copy-after-puts precedence, and `SigningStage`/`AsyncSigningStage`
   use the read-through builder. Third-party builders still get the copy.
3. **`InterceptorContext.withHttpRequest`** replaces the builder round-trip at the four sites that
   swap only the HTTP request into the context (both signing stages, `AmazonSyncHttpClient`,
   `BaseClientHandler`).

Rejected on the way: rewriting `FastV4HeaderSigner` further (already at parity — the bench3 profiles
were the evidence), and a static `(name, region)`-keyed cache for the placeholder (unbounded and
process-global; per-client is the right lifetime).

### Measurement (paired, host, 200k/30k small × 7 reps, 80k/15k batch × 5 reps, concurrency 1)

| client | scenario | h4Base | h4 | Δ app CPU | pair spread | wins | Δ latency |
|---|---|---:|---:|---:|---:|---:|---:|
| v2-sync | small-get | 110.4 | 99.8 | **−9.1%** | ±7.1% | 6/7 | −6.0% |
| v2-sync | small-put | 104.6 | 96.3 | **−7.9%** | ±2.7% | 7/7 | −5.6% |
| v2-async | small-get | 150.4 | 144.3 | **−4.0%** | ±1.8% | 7/7 | −4.0% |
| v2-async | small-put | 149.4 | 141.3 | **−5.5%** | ±1.5% | 7/7 | −5.2% |
| smithy (control) | small-get | 50.9 | 50.3 | −0.6% | ±10.3% | 4/7 | −0.8% |
| smithy (control) | small-put | 46.9 | 48.1 | +2.6% | ±4.2% | 2/7 | +1.3% |
| v2-sync | batch-put | 293.3 | 285.3 | −2.7% | ±1.4% | 5/5 | −1.6% |
| v2-async | batch-put | 345.1 | 335.3 | −2.8% | ±0.7% | 5/5 | −2.6% |

Prediction was −3 to −6 µs/op; measured is **−8 to −11 µs on sync small ops and −6 to −8 async**,
with the untouched smithy arm flat, so the effect is the SDK's. Process CPU (which folds in GC)
moved further, −9.2%/−7.7% sync, consistent with the allocation mechanism below. Batch-put's −2.7%
is the same fixed saving diluted by payload work.

### Mechanism check (equal 35k-op alloc profiles; 300k-op CPU profiles)

Total allocation on sync small-get fell **39,149 → 34,460 B/op (−4.7 KB, −12%)**, more than the
−1.5 to −2.5 KB predicted, and the sites that were supposed to vanish did:

| site (B/op) | base | H4 |
|---|---:|---:|
| `DefaultAuthSchemeOption.<init>` HashMap + nodes + table | 959 | **0** |
| `DefaultAuthSchemeOption$BuilderImpl.<init>` HashMap + nodes | 540 | **0** |
| `DefaultBaseSignRequest.<init>` HashMap nodes | 195 | **0** |
| `DefaultAuthSchemeOption` object | 120 | **0** |
| `InterceptorContext` + `$Builder` | 600 | 300 |
| everything reached via auth-option / sign-request / interceptor-context frames | 6,621 | 3,595 |

CPU agrees: inclusive share under `DefaultAuthSchemeOption` 3.45% → 0.40% (≈3.7 → 0.4 µs),
`InterceptorContext$Builder` 1.20% → 0.49%, while `FastV4HeaderSigner`'s absolute time is unchanged
(7.4 → 7.7 µs; its *share* rises because the total shrank — the algorithm was never the problem).

### PR 7367 on JSON

Upstream PR 7367 replaces a per-call `SdkHttpRequest.getUri()` snapshot (a `java.net.URI` build) with
a lightweight `EndpointUrl`. It was merged for a query/EC2-protocol regression but the snapshot ran on
every protocol; it applies cleanly and is in neither 2.54.0 nor the racecar base, so it was
cherry-picked onto H4 and measured on its own: sync small-get **−1.8%** (5/7, spread ±6.5%), async
−1.3% (5/7, ±1.8%), smithy control +0.3%. Directionally what a ~1 µs saving looks like at this rig's
floor; kept, and it is part of the H5 base.

## Investigation — where the stock SDK's 2.46 → 2.54 small-op drift comes from

bench3's two stock baselines disagreed by more than noise: 2.46.10 at 148.6 µs/op sync small-get
against 2.54.0 at 154.6 (+4.0%), async +4.3%, batch-put flat. Seven minor releases sit between them.
Asked to find the change, with the hint that PR 7367 fixed one known regression (query/EC2).

- Raw: `pipeline_benchmark3/data/bisect/results.csv` (nine published releases, 3 scenarios × 3 reps
  each, one-off cells), `data/bisect/local-prof/` (per-release local CPU profiles; run logs committed, collapsed stacks kept locally),
  `paired/host-20260912-1958` (2.46.10 vs 2.47.0 paired, 7 reps, v1 control),
  `raw/host-bisect-profiles/` (host CPU profiles of 2.46.10, 2.47.0, 2.54.0)
- Scripts: `pipeline_benchmark3/scripts/bisect_table.py`, `frame_track.py`, `frame_diff.py`

### Method — coarse grid first, then differential frames, then one paired run

Patch-level bisection would have been the wrong tool: the per-step change is below the ±2–3% a
single cell can resolve, and 40-odd patch releases at 3 reps each is a day of host time to learn
nothing. Instead: (1) a one-off sweep across every minor release (`.0` of each, plus the two
endpoints) to see the *shape* of the drift; (2) track the inclusive share of candidate frames across
the nine profiles, since a share that steps at one release is a mechanism even when the µs total is
within noise; (3) confirm the one candidate step with a proper paired run, with v1 as the control
arm.

### Result

**The drift is gradual, not a cliff.** Sync small-get by release (mean of 3, one-off cells, this
host): 148.6 → 150.3 → 149.4 → 150.4 → 152.5 → 155.2 → 153.4 → 158.5 → 154.6. Every step is inside
the cell spread; the trend is not. Async has the same shape (197.1 → 205.5); batch-put is flat
(799.5 → 809.8, +1.3%, within spread) — so this is per-call fixed cost, not per-byte.

**The one step that stands out is 2.47.0**, and the host profile pair says what it is. 2.47.0
carried the endpoint/auth-resolution refactor (#7017) that moved endpoint and auth-scheme resolution
from service interceptors into pipeline stages. Differential frames, 2.46.10 → 2.47.0, share of
SDK-rooted client samples (≈1.45 µs per point at this arm's 145 µs/op):

| moved out | | moved in | |
|---|---:|---|---:|
| `AwsExecutionContextBuilder.runInitialInterceptors` | 5.10 → 0.25 | `EndpointResolutionStage.execute` | new, 4.61 |
| `DynamoDbResolveEndpointInterceptor.modifyRequest` (+ `ruleParams`) | 3.26 → gone | `DefaultDynamoDbClient.resolveEndpoint` lambda path | new, 3.89 |
| `ExecutionInterceptorChain.modifyRequest` | 3.43 → 0.06 | `AuthSchemeResolutionStage.execute` | new, 2.21 |
| | | `AwsEndpointProviderUtils.endpointBuiltIn` | new, 1.88 |
| | | `DefaultAuthSchemeOption.forEachSignerProperty` (property replay/merge) | 0.17 → 1.32 |
| | | `EndpointResolutionStage.reapplyInterceptorModifiedAuthProperties`, `AuthSchemeResolver.mergePreExistingAuthSchemeProperties` | new, 0.43 + 0.43 |

The same work left the chain and came back as stages, plus about two points of *new* work the
interceptor version did not do: the stage re-derives endpoint built-ins per call and replays/merges
auth-scheme properties across the interceptor boundary (`forEachSignerProperty`, `reapply…`,
`mergePreExisting…`). Net ≈ +2 points ≈ +3 µs — which is the paired number. Paired, 7 reps, 200k
ops, v1 as control:

| client | 2.46.10 | 2.47.0 | Δ app CPU | pair spread | wins |
|---|---:|---:|---:|---:|---:|
| v2-sync small-get | 143.9 | 146.8 | **+2.1%** (+3.0 µs) | ±1.4% | 0/7 |
| v1 small-get (control) | 116.4 | 115.6 | −0.6% | ±1.5% | 4/7 |

2.47.0 is slower in all seven pairs with the control flat: about **+3 µs/op, roughly half of the
+6 µs total**. The other half accumulates in sub-floor steps across 2.49–2.53 that no single
measurement here can attribute individually; the 2.47.0 → 2.54.0 profile pair shows the endpoint
rules evaluating more per call (`RulesFunctions.parseURL`/`RuleUrl.parse` new at 0.94 points,
`DefaultDynamoDbEndpointProvider.resolveEndpoint` 0.52 → 1.40 — a ruleset that now parses the
endpoint URL each call) and `ChecksumUtil.checksummer` +0.8 points, with the rest diffuse. Changelog
candidates for those: #7150 (2.49.0), the `EndpointUrl` groundwork #7176 (2.50.0), #7226 (2.51.1),
#7161 (2.53.0). The sweep is honest about its floor: 2.53.0 → 2.54.0 reads −2.5% one-off, which is
noise, not a fix.

A note on method: the *local* (macOS) differential profiles pointed at `java.net.URI.<init>`
(0.36% → 1.1%) and header `deepCopyMap` copies (0.87% → 1.99%) as the 2.47 step. Neither
reproduces on the host — both are flat there (1.8%/1.1% throughout) — so they were JIT/platform
artefacts of the local run, and the mechanism table above uses the host profiles only. This is the
second time in this project a local profile has told a different story from the host one; host
profiles are the ones to believe.

### What this means for racecar

- **7367 is relevant to JSON**, not just query/EC2 — the `getUri()` snapshot it removes runs on
  every protocol (the JSON `getUri` share is 0.6–1.0% in the stock host profiles) — which is why it
  was cherry-picked and measured above (−1.8% sync, at the floor).
- **Half of the 2.47 step is structure H4 has since replaced; the other half is a live target.** The
  auth-scheme property replay/merge the stage refactor added is exactly the per-call option-graph
  churn H4 removed (its share in the optimized arm: 2.19% → 0.43% across H4). The endpoint half is
  *not* gone: in the H4 arm `EndpointResolutionStage` + `DefaultDynamoDbClient.resolveEndpoint` +
  `endpointBuiltIn` + `ruleParams` still take ~6% of client samples, **≈5 µs/op to resolve an
  endpoint that is a per-client constant for this workload**. That is the next fixed-cost item after
  H5 (below), and the reason the stock-vs-stock comparisons in bench3 read "2.54 is ~4% worse than
  2.46" has nothing to do with the bridge.
- **It also motivates H5**: a refactor that moved work *out* of interceptors still left the chain
  costing 4.3 µs/op on a client with no interceptors of its own, which is what the next phase is
  about.

## Phase H5 — an empty interceptor chain costs nothing

Asked for as "move the SDK-owned interceptors out, so we avoid any cost of the interceptor interfaces
when none are set". Sizing first: on the optimized small-get profile **4.27 µs/op (4.1%) went
through `ExecutionInterceptorChain`** on a DynamoDB client with *zero* customer interceptors. The
chain carried four SDK-owned interceptors — `HttpChecksumValidationInterceptor` (sdk-core, every
client), `HelpfulUnknownHostExceptionInterceptor`, `EventStreamInitialRequestInterceptor` and
`TraceIdExecutionInterceptor` (aws-core, every AWS client) — and for each of them called all ~16 hooks
per request, each returning an `Optional` or the unchanged message to be compared. A quarter of the
cost was one of them: `TraceIdExecutionInterceptor` read the `AWS_LAMBDA_FUNCTION_NAME` environment
variable through `SystemSetting` in three hooks per call (0.75% of CPU, plus a `byte[]` per read) to
conclude it was not in Lambda. The chain itself was also rebuilt per call (`new ArrayList<>(...)` plus
a debug-log lambda, 0.24 µs) and used a capturing lambda per `forEach` hook.

- Commits: `634044a78c2` (chain built once per client), `414f1d62225` (per-hook dispatch),
  `943673acf8a` (checksum validation as a direct call), `dbc7588b125` (trace ID decided once)
- Raw: `paired/host-20260912-2029` (small ops, 7 reps), `host-20260912-2132` (batch-put, 5 reps),
  `raw/host-h5-profiles` (equal-op alloc + CPU profiles)
- Correctness: sdk-core 1586 + 651, aws-core 321, protocol-tests 726, codegen-generated-classes
  `HttpChecksumValidationTest` 40 (sync and async response validation end to end through the new
  call sites) and `TraceIdTest` 7 (Lambda wiring), japicmp clean on sdk-core and aws-core, smoke
  10/10 with metric-set identity. New: `ExecutionInterceptorChainTest` (9) pins forward/reverse
  order for every hook, relative order across interceptors implementing different hooks, and the
  conservative detection cases; `DefaultClientBuilderTest` pins the chain cache and its
  re-derivation.

### The design choice: dispatch by hook, then take the two hot ones off

"Move them out" literally — direct calls at the same pipeline points — runs into module boundaries:
three of the four live in aws-core while every call site is in sdk-core, and re-inventing a hook
mechanism to bridge that would just be a second interceptor chain. What was done instead has two
layers:

1. **The chain dispatches per hook.** On construction (now once per client, as a lazy internal option
   derived from `EXECUTION_INTERCEPTORS`, so a request-level plugin that changes the list still gets
   a matching chain) it works out which interceptors *declare* which hook methods and keeps a target
   array per hook, iterated with indexed loops in the documented forward/reverse order. An
   interceptor that only implements `modifyException` costs nothing on the request path; a hook with
   no overriders is a zero-iteration loop. This is generic — customers' interceptors typically
   implement one or two hooks too — and it preserves relative order exactly, because it filters the
   same list rather than reordering it. Detection is deliberately conservative: any declaration in
   the class hierarchy other than `ExecutionInterceptor` itself counts (superclass, interface default,
   proxy, mock), and if the hierarchy cannot be inspected the interceptor is visited everywhere, as
   before. The failure mode is therefore only "visited needlessly", never "skipped".
2. **The two interceptors that were hot on every call left the chain.** Response checksum validation
   (sdk-core, so no boundary problem) became a direct call, `ResponseChecksumValidation.validating`,
   at the two points where the chain invoked it — after the chain's `modifyHttpResponse` in the sync
   after-transmission stage and after `modifyAsyncHttpResponse` in the async `onStream` handler —
   which is *after* every configured interceptor, exactly where it ran before (it was at the head of
   the list; response hooks run in reverse). `TraceIdExecutionInterceptor` evaluates the Lambda
   variable once in its constructor and `AwsDefaultClientBuilder` registers it only when that says
   Lambda; inside Lambda the behaviour is identical, outside it the chain no longer carries it.

What remains on a plain DynamoDB client, verified by reflection against the built client:
`EXECUTION_INTERCEPTORS = [HelpfulUnknownHostExceptionInterceptor, EventStreamInitialRequest
Interceptor]`; every hook's target array is empty except `modifyException` (the unknown-host helper,
failure path only) and `modifyHttpRequestAndHttpContent` (the event-stream interceptor's one
attribute lookup — a codegen-level "register only for services with initial-request event streams"
would remove it, noted below).

Two things are observable and worth stating. `EXECUTION_INTERCEPTORS` no longer lists the checksum
interceptor or, outside Lambda, the trace-ID one; the former was `@SdkInternalApi` and the module's
japicmp excludes were fixed to honour the parent's `*.internal.*` rule (they replaced the parent's
list instead of appending to it, which is why an internal-class removal tripped the gate). And a
customer who *sets* `AWS_LAMBDA_FUNCTION_NAME` after building a client would no longer get trace
propagation on that client; the Lambda runtime sets it before the process starts, and the SDK's own
test for the wiring builds its client after setting the variable, as any real caller must.

### Measurement (paired, host, 200k/30k small × 7 reps, 80k/15k batch × 5 reps, concurrency 1)

Base is H4 + 7367 (`8bab8409dd1`), so this is H5's effect alone.

| client | scenario | h4b | h5 | Δ app CPU | pair spread | wins | Δ latency |
|---|---|---:|---:|---:|---:|---:|---:|
| v2-sync | small-get | 97.1 | 93.8 | **−3.3%** | ±3.3% | 5/7 | −2.1% |
| v2-sync | small-put | 96.6 | 86.0 | **−10.9%** † | ±5.4% | 7/7 | −7.4% |
| v2-async | small-get | 142.4 | 137.9 | **−3.2%** | ±2.3% | 6/7 | −3.2% |
| v2-async | small-put | 140.4 | 134.3 | **−4.2%** | ±3.5% | 7/7 | −3.2% |
| smithy (control) | small-get | 52.1 | 50.8 | −2.3% | ±8.5% | 4/7 | −2.8% |
| smithy (control) | small-put | 48.2 | 48.1 | −0.1% | ±3.4% | 4/7 | +0.0% |
| v2-sync | batch-put | 284.5 | 278.0 | −2.3% | ±1.7% | 4/5 | −1.6% |
| v2-async | batch-put | 335.1 | 331.2 | −1.2% | ±1.0% | 4/5 | −1.0% |

The sizing said 4.3 µs/op; small-get delivers **−3.3 µs sync, −4.5 µs async**, and batch-put's
−6.5/−3.9 µs is the same fixed saving under payload work. The control is flat on small-put and its
small-get −2.3% is a 4/7 split with ±8.5% spread, i.e. nothing.

† Sync small-put is the one cell that reads bigger than the mechanism. All seven pairs favour H5,
but the per-pair deltas run −5.6% to −19.1% (median −8.5%): two H5 reps landed at 76.6 and 83.7
µs/op against a base that never went below 92.8, which is the compiled-code-shape lottery this rig
has shown before (Phase H3), not a 10 µs mechanism. The defensible claim for that cell is the
median's ≈ −8 µs with the top end unexplained; the small-get cells and the profiles below are the
ones to quote.

### Mechanism check (equal 35k-op alloc profiles; 300k-op CPU profiles)

| | sync base | sync H5 | async base | async H5 |
|---|---:|---:|---:|---:|
| CPU share under `ExecutionInterceptorChain.*` | 3.69% | **0.10%** | 4.40% | **0.26%** |
| CPU share under `TraceIdExecutionInterceptor` | 0.77% | 0 | 0.99% | 0 |
| CPU share under env-var lookup | 0.24% | 0 | 0.53% | 0 |
| CPU share under checksum validation (interceptor → direct call) | 0.30% | 0.16% | 0.42% | 0.20% |
| allocation reached through any interceptor frame (B/op) | 704 | **0** | | |
| env-var lookup `byte[]` + `ProcessEnvironment$Variable` (B/op) | 390 | **0** | | |
| profile total (B/op) | 35,539 | **33,876** | | |

Chain cost 3.7 → 0.1 µs/op sync, which is the whole 4.27 µs sizing minus the one remaining
event-stream attribute check; −1.7 KB/op allocation (−4.7%). The residual 0.10%/0.26% is the
`modifyHttpRequestAndHttpContent` visit to `EventStreamInitialRequestInterceptor` plus the
`modifyException` array on the failure path.

### Verdict

Kept: a fixed −3 to −4.5 µs/op on every AWS client with no interceptors of its own, the chain's cost
now proportional to what customers actually register rather than to the hook count, and the two
formerly-hot SDK interceptors doing their work as direct calls at the same points. Racecar's optimized
sync small-get is now **≈ 94 µs/op app CPU** against stock 2.54.0's ≈ 150 on this host.

Follow-ups, in value order:

1. **Endpoint resolution per call (~5 µs/op, see the investigation above)** — the biggest remaining
   fixed cost on a client whose endpoint is a per-client constant for this workload.
2. **`EventStreamInitialRequestInterceptor` registered by codegen only for services with an
   initial-request event stream** (Transcribe streaming and the like) rather than by aws-core for
   every service. Removes the last hot-path visit; ~0.1 µs, so it is tidiness more than speed.
3. The response-side stages still `copy()` the interceptor context to add the HTTP response even
   when no interceptor will read it; that copy feeds later stages so it is not free to drop, but the
   `withHttpRequest`-style single-field swap from H4 would cut it to one small object.

## Investigation, part 2 — the 2.46 → 2.54 drift attributed step by step

The first pass (above) found one clean step at 2.47.0 and called the rest "gradual, below the
one-off floor". Asked to attribute it properly: every adjacent pair of the nine published releases was
measured *paired* (7 reps, 200k ops, v2-sync small-get, arms interleaved), and each release got a host
CPU profile so the steps could be tied to frames and the frames to commits.

- Raw: `paired/host-20260913-0033` … `host-20260913-0218` (eight adjacent pairs),
  `raw/host-bisect-profiles/` (host CPU profiles of all nine releases)
- Scripts: `pipeline_benchmark3/scripts/ladder_table.py` (the table below),
  `ladder_commits.sh` (request-path commits per step), `frame_diff.py`, `frame_track.py`

### The ladder

| step | base | cand | Δ app CPU | median | pair spread | wins (cand faster) |
|---|---:|---:|---:|---:|---:|---:|
| 2.46.10 → 2.47.0 | 146.5 | 149.8 | **+2.3%** | +2.0% | ±2.0% | **0/7** |
| 2.47.0 → 2.48.0 | 144.4 | 145.2 | +0.6% | +1.6% | ±2.6% | 3/7 |
| 2.48.0 → 2.49.0 | 148.4 | 146.6 | −1.1% | −1.0% | ±3.8% | 5/7 |
| 2.49.0 → 2.50.0 | 147.1 | 148.7 | +1.1% | +2.3% | ±4.2% | 2/7 |
| 2.50.0 → 2.51.0 | 147.4 | 151.9 | **+3.1%** | +3.2% | ±2.5% | **0/7** |
| 2.51.0 → 2.52.0 | 150.1 | 149.6 | −0.3% | −0.1% | ±2.7% | 4/7 |
| 2.52.0 → 2.53.0 | 153.2 | 156.1 | +2.0% | +2.9% | ±4.4% | 2/7 |
| 2.53.0 → 2.54.0 | 153.2 | 152.7 | −0.3% | −0.9% | ±3.3% | 5/7 |

End to end the ladder's arm means go 146.5 → 152.7 (**+4.2%**, matching the earlier sweep's +4.0%).
Two steps are unambiguous — every one of seven pairs went the same way, with the pair spread well
inside the effect — and together (+2.3% and +3.1% ≈ +8 µs) they account for the whole end-to-end
drift; the other six are within their own noise, and 2.53.0 is the only one worth a second look.

### Step 1, 2.47.0: the endpoint/auth stage refactor (#7017) — a core change

Same finding as part 1, now reproduced in a second independent session (+2.1% and +2.3%, 0/7 both
times). Endpoint and auth-scheme resolution moved from service interceptors into pipeline stages, and
the stage version does more per call than the interceptor did: it re-derives the endpoint built-ins
(`AwsEndpointProviderUtils.endpointBuiltIn`, new at 1.9 points) and replays/merges auth-scheme
properties across the interceptor boundary (`DefaultAuthSchemeOption.forEachSignerProperty` 0.17 →
1.32, `reapplyInterceptorModifiedAuthProperties` and `mergePreExistingAuthSchemeProperties` new).

Fixed where? The property replay is exactly what H4 removed in racecar (its share fell 2.19% → 0.43%
across H4). Upstream's #7282 "Cache auth scheme resolution" (2.54.0) targets the same area, but
2.53.0 → 2.54.0 measures −0.3% (5/7), so whatever it recovered is under this rig's floor. The
per-call endpoint evaluation is what the BDD provider with its result cache (#7345, #7364, #7371)
removes — measured as H6 below.

### Step 2, 2.51.0: DynamoDB's endpoint ruleset grew — a *model* change, not a code change

This one was invisible to the one-off sweep and to a commit search. `ladder_commits.sh` finds **no
request-path code change at all** between 2.50.0 and 2.51.0 — one revert (#7223, an `available()`
change), one annotation (#7203). What did change is DynamoDB's own model: the vector-search launch
(`bfe3301efdb`, "Merge customizations for DynamoDB") rewrote `endpoint-rule-set.json` (+466 lines):
a new `IsSearchOperation` parameter, a `search-ddb` endpoint tier, and account-ID/ARN-based routing
tiers threaded through every branch. In this era the ruleset is walked on every call, so a bigger
ruleset is a slower call. The profile pair 2.50.0 → 2.51.0 says exactly that and nothing else:

| frame (share of client samples) | 2.50.0 | 2.51.0 |
|---|---:|---:|
| `EndpointResolutionStage.execute` | 3.46% | **5.47%** |
| `DefaultDynamoDbEndpointProvider.resolveEndpoint` | 0.61% | **1.75%** |
| `DefaultDynamoDbEndpointProvider.endpointRule0` / `endpointRule1` | 0.59 / 0.57 | 1.57 / 1.56 |
| `RulesFunctions.parseURL` → `RuleUrl.parse` | — | **new, 0.93%** |
| `DynamoDbEndpointResolverUtils.ruleParams` | 2.24% | 2.59% |

The `RuleUrl.parse` frame is the tell: the new ruleset parses the endpoint override URL on every call
(the benchmark has one, like any VPC-endpoint, local-DynamoDB or test setup), and that alone is
~1.3 µs. +2 points ≈ +3 µs — the paired number. **Every DynamoDB caller on 2.51+ pays this whether or
not they use vector search.** It is also why the bench3 "stock 2.46 vs stock 2.54" baselines
disagreed: half of that gap is DynamoDB's ruleset, not the SDK core.

Fixed where? This is precisely the case for BDD + result cache: after the first call the params
compare equal and the walk (URL parse included) is skipped. #7371 turns that on for DynamoDB. H6 below
measures it.

### Step 3, 2.53.0: suggestive, not established

+2.0% mean but only 2/7 pairs against and ±4.4% spread, so it fails the bar the other two clear. The
profile pair shows `ExecutionAttributes.putAttribute` 1.53% → 2.72% inside
`AwsExecutionContextBuilder`, `AttributeMap.get` +0.5 and `ExecutionAttribute$DerivationValueStorage.
set` +0.5 — consistent with #7161 ("Remove ServiceMetadata from client creation and request paths")
turning `SIGNING_REGION` into a lazily-derived client option read per call, which then feeds the
mapped-attribute derivation H4 later made cheap. Plausible mechanism, sub-floor effect; recorded as a
candidate, not a finding. The remaining +1 point in that pair is unmarshalling frames, which is
single-profile JIT-shape noise.

### What is fixable, and by whom

| step | cause | fix in racecar | fix for the stock SDK |
|---|---|---|---|
| 2.47.0 +2.3% | stage refactor re-derives built-ins and replays auth properties per call | H4 (auth properties), BDD cache via merge (endpoint) | #7282 did not measurably recover it; the H4 placeholder-auth-scheme approach and BDD (#7371) are the candidates to upstream |
| 2.51.0 +3.1% | DynamoDB ruleset growth walked per call, incl. endpoint-URL parse | BDD provider + result cache (#7371, via the master merge) | ship #7371; the same applies to any service whose ruleset grows |
| 2.53.0 +2.0%? | possibly per-call lazy `SIGNING_REGION` derivation | H4 already caches the derived scheme | needs a clean reproduction first |

The uncomfortable conclusion for the stock SDK is that neither regression was a "bug" anyone would
have caught in review: one is a reasonable refactor whose per-call cost was not measured, the other is
a service team adding endpoint rules. Both are the kind of thing only a paired benchmark on a fixed
rig sees, and both are addressed by the same structural answer — resolve once, cache, and stop paying
per call for things that are per-client constants.

## Phase H6 — onto master, with BDD endpoint resolution and its result cache

Asked for directly: bring the branch to the latest master and apply PR #7371 ("[Endpoints BDD] —
Release phase 2"), then confirm DynamoDB really resolves endpoints through the BDD provider and really
uses the cache. #7371 itself is eight `endpoint-bdd-1.json` model files (DynamoDB among them); the
machinery it switches on is already in master — the BDD codegen (#7345: node-per-method evaluator,
peephole-optimised conditions, and a single-entry `params → endpoint` result cache generated into each
`Default{Service}EndpointProvider`), its phase-1 rollout (#7364) and code-generated rules for every
service (#7265, #7294).

- Commits: `2f26679488e` (merge of `origin/master` at `ca8030a7275`, 2.54.18-SNAPSHOT),
  `88f25102bd7` (cherry-pick of #7371), `880747878ba` (harness follows the version bump)
- Raw: `paired/host-20260913-0242` (small ops, 7 reps), `host-20260913-0344` (batch-put, 5 reps),
  `raw/host-h6-profiles` (equal-op alloc + CPU profiles)
- Correctness: sdk-core 1620 + 651, aws-core 334, dynamodb 61 (its generated endpoint conformance
  suite now runs against the BDD provider), codegen-generated-classes 781 (endpoint, checksum,
  trace-ID, metric tests), protocol-tests 726, japicmp clean on sdk-core and aws-core, smoke 10/10
  with metric-set identity

### Merge, not rebase

The branch carries 136 measured commits whose SHAs are cited throughout this document and stamped
into every jar; a rebase would have rewritten all of them and replayed each through master's codegen
rewrite. A merge gives the same tree with the record intact. Six conflicts, all resolved in favour of
the racecar structure with master's intent ported in: the two HTTP clients keep the straight-line
`SyncApiCallPipeline`/`AsyncApiCallPipeline` (G1/G2) and master's removal of the
`ApiCallMetricCollectionStage` wrappers (#7338 moved `API_CALL_DURATION` into the handlers) is applied
inside them; `SimpleHttpContentPublisher` is ours because ours *is* master's #7323 plus the Phase A
buffered-content fast path; `CrtRequestAdapter` keeps the E12/H1 array builder and gains master's
`onBodyError` consumer (#7307); both sides had made the identical sdk-core japicmp fix. Safety refs:
tag `racecar/pre-master-merge-h5`, branch `backup/benchmark3-pre-merge`.

Two things bit during the build and are now fixed in the harness: the version bump to 2.54.18
meant `build-jar.sh --skip-sdk-build` silently packaged the *pre-merge* artifacts still in `~/.m2`
(the harness pom pinned 2.54.4-SNAPSHOT — the jar's own provenance stamp is how it was caught), and
the consistent-install recipe must now build `codegen-maven-plugin` in-reactor, because with it
excluded Maven's reactor reader reports the plugin missing instead of resolving it from `~/.m2`.

### Verified, not assumed

A probe against the built client (`/tmp/EndpointProbe.java`, reflection on the client's
`ENDPOINT_PROVIDER` option) rather than a reading of the generated source:

- the client's provider is `DefaultDynamoDbEndpointProvider` with a `volatile CacheEntry cache`
  field — the BDD variant, not the rules2 one;
- the cache is `null` before the first call, populated after it, and the **same `CacheEntry`
  instance** is observed after calls 1, 2 and 3 (an interceptor aborted each call just before
  transmission, after endpoint resolution) — no re-resolution;
- directly, equal params return the identical `Endpoint` instance and a different region misses.

The cache key is `Objects.equals` over every declared parameter (`useDualStack`, `useFips`,
`isSearchOperation`, `region`, `endpoint`, `accountId`, `accountIdEndpointMode`, `resourceArn`, first
element of `resourceArnList`), so a request that legitimately changes the endpoint — a different
account-ID mode, an ARN-routed operation — misses and re-walks; it is a last-value cache, not a memo.

### Measurement (paired, host, 200k/30k small × 7 reps, 80k/15k batch × 5 reps, concurrency 1)

Base is H5 (`dbc7588b125`) rebuilt on the same harness commit; the candidate is the merged tree with
#7371, so this measures master's changes *plus* BDD together — everything between H5 and here.

| client | scenario | h5 | h6 | Δ app CPU | pair spread | wins | Δ latency |
|---|---|---:|---:|---:|---:|---:|---:|
| v2-sync | small-get | 90.5 | 82.2 | **−9.0%** | ±5.4% | 6/7 | −6.5% |
| v2-sync | small-put | 87.7 | 77.0 | **−12.0%** | ±6.0% | 6/7 | −7.9% |
| v2-async | small-get | 138.1 | 131.5 | **−4.8%** | ±1.8% | 7/7 | −3.8% |
| v2-async | small-put | 132.7 | 129.7 | −2.2% | ±2.7% | 6/7 | −1.6% |
| smithy (control) | small-get | 50.6 | 52.0 | +2.9% | ±6.2% | 1/7 | +2.0% |
| smithy (control) | small-put | 46.7 | 48.9 | +5.3% | ±8.5% | 1/7 | +4.1% |
| v2-sync | batch-put | 280.0 | 268.2 | **−4.2%** | ±0.6% | 5/5 | −3.3% |
| v2-async | batch-put | 329.1 | 322.0 | −2.2% | ±1.1% | 5/5 | −2.0% |

The H5 write-up sized per-call endpoint resolution at ~5 µs/op on the optimized sync path; H6 delivers
**−8.3 µs on small-get and −10.7 on small-put**, i.e. that plus the rest of master's changes. The
smithy control drifted *up* 3–5% in this session (1/7), which if anything means the V2 deltas are
understated, not inflated. Batch-put's −11.8 µs (5/5, ±0.6%) is the same fixed saving under payload
work.

### Mechanism check (equal 35k-op alloc profiles; 300k-op CPU profiles)

| | sync H5 | sync H6 | async H5 | async H6 |
|---|---:|---:|---:|---:|
| CPU share under `EndpointResolutionStage.execute` | 5.25% | **1.10%** | 5.63% | **1.05%** |
| … under `DefaultDynamoDbEndpointProvider.resolveEndpoint` | 1.93% | 0.13% | 2.36% | 0.17% |
| … under the BDD evaluator (`nodeP*`) / old `endpointRule*` | 1.93% | 0.10% | 2.29% | 0.08% |
| … under `RuleUrl.parse` (the 2.51 per-call URL parse) | 1.20% | 0.03% | 1.62% | 0.08% |
| … under `endpointBuiltIn` | 1.96% | 0.07% | 2.21% | 0 |
| … under `ruleParams` (params object, still built for the key) | 2.66% | 0.73% | 3.03% | 0.80% |
| allocation reached through endpoint frames (B/op) | 1,603 | **195** | | |

The endpoint stage went from 5.3% to 1.1% of client CPU; what remains is building the params object
that the cache key compares (0.7%) and the compare itself (0.03%). Total allocation is flat (32,977 →
33,502 B/op, +1.6%, all in unrelated JIT-shape sites — `FastJsonGenerator` and header arrays — not in
anything this change touched). The 2.51.0 regression's signature frame, `RuleUrl.parse`, is gone,
which closes the loop on the ladder: **the drift attributed to DynamoDB's ruleset growth is fixed by
#7371**, and the 2.47.0 step's endpoint half with it.

### Where this leaves the optimized pipeline

Sync small-get **≈ 82 µs/op** application CPU on this host against stock 2.54.0's ≈ 150 (0.55×), and
against the bridged prototype's 74 (bench3) the gap is now 8 µs rather than 31. The largest remaining
fixed costs on the sync small-op path, from the H6 profile: `AuthSchemeResolutionStage` still ~1.8%
(master's #7282 cache did not measurably move it here — a candidate for the same treatment the
endpoint just got), the `ruleParams` object built per call only to be compared (0.7%), and the
response-side interceptor-context copies from the H5 follow-ups.

## Phase H7 — auth scheme resolution: resolve once, fetch identity per call

Asked for as the natural follow-on to H6: the auth-resolution stage was the largest fixed cost left on
the sync small-op path (1.47 µs/op, 1.8%), and the request was to look at three layers — caching the
resolution itself, the generated `resolveAuthSchemeOptions`, and above all `AuthSchemeResolver.
selectAuthScheme` — with the caution that auth differs from endpoints because request-level overrides
have to keep working, and that scoping to non-endpoint-based providers is acceptable if a general
answer is not available.

- Commits: `d1268ef2e7b` (sdk-core: the cache), `0dd827e80c5` (codegen: resolver callbacks as fields)
- Raw: `paired/host-20260913-1546` (small ops, 7 reps), `host-20260913-1647` (batch-put, 5 reps),
  `raw/host-h7-profiles` (equal-op alloc + CPU profiles)
- Correctness: sdk-core 1628 + 651, aws-core 334, dynamodb 61, codegen 708 (25 client fixtures
  updated), codegen-generated-classes 789 (auth, signer, endpoint, credential suites),
  protocol-tests 726, japicmp clean, smoke 10/10 with metric-set identity (the identity-fetch metric
  is still reported per call). New `AuthSchemeResolutionStageCacheTest` (8) pins what a second call
  reuses and every way a call must miss.

### What the stage was doing per call, and which of it is per-client

From the H6 profile, inside `AuthSchemeResolutionStage` (µs/op, sync): the generated
`resolveAuthSchemeOptions` 0.48 (three attribute lookups, the request-override `Optional` chain, and
#7282's `ConcurrentHashMap` lookup of the cached options list); `selectAuthScheme` 0.40 — a
`discardedReasons` list, `authSchemes.get(schemeId)`, `scheme.identityProvider(providers)`,
`scheme.signer()`, a `ResolveIdentityRequest` built from the option's identity properties, then
`resolveIdentity`; `mergePreExistingAuthSchemeProperties` 0.27 — probing whether the H4 placeholder
carries a property the resolved option lacks (it never does, and the probe costs two `HashMap.get`s via
a lambda-based `forEach` to find that out); and 0.16 of identity-fetch metrics.

Everything in the middle two is a pure function of four inputs: the options list, the client's
`AuthScheme` map, the `IdentityProviders` in effect, and the option of the scheme already on the
attributes before resolution (the placeholder). Only the identity future is genuinely per call —
credentials providers are per-call by contract, and the cache must not touch that.

### The design: identity-keyed, so overrides miss by construction

Rather than deciding *which* services are safe to cache (the non-endpoint-based scoping offered), the
cache keys on the **identity** of those four inputs. That turns the correctness question into a
property of the inputs, not of the service: #7282 returns the same unmodifiable options-list
*instance* for the same parameters when the default provider is in use, and the other three are the
same instances on every call of a client, so the common call hits. Every way the answer can
legitimately change arrives as a different instance:

| change | how it shows up | result |
|---|---|---|
| request-level `authSchemeProvider` override | generated code bypasses its list cache → fresh list | miss, ordinary path |
| request-level `credentialsProvider` override | `AwsRequestIdentityProviderResolver` returns a new `IdentityProviders` | miss; override's provider used |
| request-level plugin changes auth schemes | new `AUTH_SCHEMES` map instance | miss |
| interceptor edited `SELECTED_AUTH_SCHEME` | pre-interceptor snapshot no longer `==` the existing option | merge not cacheable; full diff path |
| operation with per-operation auth | #7282 keys its list on operation → different list instance | miss (then its own hit) |

A miss is never wrong, only slower. Endpoint-based providers (S3) build a fresh options list per call
and would miss every time; if the cache stored on every miss, every call would write a shared field —
the cache-line contention the BDD work measured and rejected. So after 8 consecutive misses the cache
stops storing for that client and its per-call cost is one volatile read plus one reference compare;
a hit resets the streak, so a client whose early calls used an override still settles into hits. That
is the "general solution" the request left room for: S3 does not benefit yet, but it is not made
worse, and it *would* benefit the day its generated resolver hands back a stable list per `Endpoint`
instance (which BDD caching now makes possible — a codegen follow-up).

Implementation: `AuthSchemeResolver.selectAuthScheme` is split into `resolve(...)`, which returns a
`Resolution` (option, signer, identity provider, identity request, identity metric), and
`Resolution.select(metricCollector)`, which fetches the identity and builds the `SelectedAuthScheme`;
the merge is factored into an option-level `mergePreExistingProperties`. Both public methods keep
their exact behaviour as wrappers, which is what lets the cached and uncached paths share one
definition instead of drifting. The identity request is deliberately built from the *resolved*
option, not the merged one — that is what selection always did (it ran before the merge), so the
identity fetched is unchanged. The cache lives on `HttpClientDependencies` because our stages are
constructed per call, and is carried across its `toBuilder()`.

The codegen change is small and separate: every generated operation passed
`this::resolveAuthSchemeOptions` and `this::resolveEndpoint` into its execution params, and a bound
method reference is a new object each evaluation — two allocations per call for two callbacks that
never change. They are two `final` fields now. `PoetMatchers` gained an opt-in fixture writer
(`-Dcodegen.updateFixtures=true`), used to *locate* the changes and then discarded: the formatter
re-wraps every fixture, and replacing them wholesale would have buried the six-line change per file
in a 4,000-line whitespace diff, so the 25 fixtures were patched minimally instead.

### Verified on a real client before measuring

A probe on a built `DynamoDbClient` with static credentials: calls 1–3 carry the same
`AuthSchemeOption` and `HttpSigner` instances and a *different* identity future each; the merged
placeholder properties (`REGION_NAME`, `SERVICE_SIGNING_NAME`) are present; a call with a
request-level `credentialsProvider` override resolves its own identity (`AKIAREQUEST`) and the next
default call is served from the cache again (`AKIACLIENT`, same option instance); the cache holds one
entry.

### Measurement (paired, host, 200k/30k small × 7 reps, 80k/15k batch × 5 reps, concurrency 1)

Base is H6 rebuilt on the same harness commit (installed under version `2.54.18-H6`).

| client | scenario | h6 | h7 | Δ app CPU | pair spread | wins |
|---|---|---:|---:|---:|---:|---:|
| v2-sync | small-get | 82.7 | 83.5 | +1.1% | ±6.1% | 3/7 |
| v2-sync | small-put | 78.3 | 76.5 | −2.1% | ±4.8% | 5/7 |
| v2-async | small-get | 131.6 | 131.4 | −0.1% | ±3.4% | 3/7 |
| v2-async | small-put | 127.6 | 126.5 | −0.8% | ±4.3% | 5/7 |
| smithy (control) | small-get / small-put | 49.9 / 46.8 | 48.8 / 48.7 | −1.9% / +4.6% | ±8.6% / ±9.7% | 4/7, 2/7 |
| v2-sync / v2-async | batch-put | 265.8 / 319.9 | 265.5 / 319.3 | −0.1% / −0.2% | ±1.0% / ±0.4% | 2/5, 2/5 |

**Timing is flat**, and it should be: the mechanism is worth ~0.5 µs/op on an ~80 µs call, a fifth of
this rig's floor. Claiming a timing win here would be reading noise. The mechanism check is where this
phase is judged.

### Mechanism check (equal 35k-op alloc profiles; 300k-op CPU profiles)

| | sync H6 | sync H7 | async H6 | async H7 |
|---|---:|---:|---:|---:|
| CPU share under `AuthSchemeResolutionStage.execute` | 1.65% | **1.22%** | 1.63% | **0.97%** |
| … under `AuthSchemeResolver.resolve` / `tryResolve` / `selectAuthScheme` | 0.64% | **0** | 0.71% | **0** |
| … under `mergePreExisting*` / `hasPropertyAbsentFrom` | 0.51% | **0** | 0.16% | **0** |
| … under the generated `resolveAuthSchemeOptions` (#7282 list lookup) | 0.35% | 0.39% | — | — |
| … under identity fetch (`Resolution.select` / `resolveIdentity`) | 0.44% | 0.57% | 0.52% | 0.53% |
| … under `AuthSchemeResolutionCache` (the lookup itself) | — | 0.04% | — | 0.00% |
| allocation reached through auth-resolution frames (B/op) | 614 | **300** | | |
| profile total (B/op) | 33,097 | **32,258** | | |

The two frames the cache exists to remove are gone in both clients; what is left in the stage is the
identity fetch with its metric (per call by contract), the generated options lookup (~0.3 µs, #7282's
String key + `ConcurrentHashMap`; an identity-first fast path in codegen would trim it, judged not
worth a second fixture churn for ~0.15 µs), and the business-metric/signer-override checks. The
selected-scheme allocation that remains (300 B/op) is the `SelectedAuthScheme`, the identity future
and the `Duration` metric — again the per-call part.

### Verdict

Kept, on the H2 standard: an exact mechanism win — the call-independent half of auth resolution no
longer runs per call, ~0.45 µs/op and ~300 B/op on the sync path, more on async — with no timing
effect claimable at this rig's floor and no regression anywhere. The stage is now ~1.0 µs/op, of which
the identity fetch and its metric are most of what is left.

Follow-ups, in value order:

1. **A stable options list per `Endpoint` for endpoint-based providers.** With the BDD cache
   returning the same `Endpoint` instance per call, the generated `resolveAuthSchemeOptions` for S3
   could keep a single-entry `(Endpoint identity → options list)` cache; the stage cache above would
   then hit for S3 too with no further core change. Codegen-only.
2. **`MetricCollector.create("ApiCall")` when nothing will publish.** `AwsExecutionContextBuilder`
   substitutes a real collector whenever the params carry none, so every `reportDuration` (identity
   fetch, attempt, call) does its `nanoTime` pair and `Duration` allocation into a collector that is
   discarded. Substituting the no-op collector when the client has no metric publishers would make
   all of those free; needs a check that nothing reads the collector through the interceptor-visible
   `API_CALL_METRIC_COLLECTOR` attribute.
3. The generated options lookup's identity-first fast path, if (1) is done and the fixtures are being
   touched anyway.

## Phase H8 — measuring into a no-op collector

Asked for directly after H7 flagged it. The premise as flagged turned out to be half wrong, and the
half that was right was larger than expected.

- Commits: `bd676b62ae2` (core: skip the measurement when nothing collects), `91a74f46b99` (codegen:
  no publish stage chained onto async calls without a publisher), `dffd6c3bd51` (harness:
  `paired-ab.sh --metrics`)
- Raw: `paired/host-20260913-1857` (metrics off, small ops 7 reps), `host-20260913-1957` (batch-put, 5 reps),
  `host-20260913-2014` (metrics **on**, small ops 5 reps — the regression check), `raw/host-h8-profiles`
- Correctness: utils 555/556 (the one failure, `joinLikeSync_canceled_throwsCancellationException`,
  fails identically with these changes stashed — a JDK 25 cause-chaining difference, unrelated),
  sdk-core 1638 + 651, aws-core 334, apache5-client 270, codegen 708 (14 async fixtures), protocol-tests
  726, codegen-generated-classes 870 **including every metrics suite** (`CoreMetricsTest`, the async
  core-metrics tests, business-metric and endpoint-metric tests, all run with a publisher attached and
  assert each metric arrives), japicmp clean, smoke 10/10 with the 11-family metric set intact under
  `--metrics`. New `MetricsDisabledStagesTest` (10) pins both sides of the switch.

### What was actually there

The collector was never the problem. Since 2020 (`89784a929f9`) the generated clients hand the
pipeline `NoOpMetricCollector` whenever neither the client nor the request has a metric publisher, and
every `createChild` of a no-op is the same no-op instance, so `reportMetric` was already free on every
call. The `MetricCollector.create("ApiCall")` fallback in `AwsExecutionContextBuilder` that H7's note
pointed at only fires for callers that pass no collector at all, which generated clients never do.

What was *not* free was everything that produces the values. Self time in metric machinery on the H7
profile — the machinery's own frames plus the JDK they call, excluding the wrapped work — was
**1.2 µs/op sync and 4.6 µs/op async**, of which ~0.5/~1.1 is the user-agent business-metrics string
(a real header, not discardable) and the rest is measurement into the no-op:

| per call, into a collector nobody reads | sync | async |
|---|---|---|
| `nanoTime` pair + `Duration` + `Pair` + lambda around marshalling, signing, the HTTP call, unmarshalling, identity fetch | ✓ | ✓ (plus a `whenComplete` per future) |
| `new AtomicLong` + `new RequestBodyMetrics` (three `AtomicLong`s) per attempt, two `putAttribute`s | ✓ | ✓ |
| **the request rebuilt** (`toBuilder().contentStreamProvider(...).build()`) to install a byte-counting provider | ✓ | — |
| **the response rebuilt** around a `BytesReadTrackingInputStream`, and every body read through it | ✓ | `BytesReadTrackingPublisher` wrapper |
| `whenComplete` on the attempt future + its `forwardExceptionTo` twin; same again on the HTTP-client future | — | ✓ ✓ |
| `ReadMetricsTrackingResponseHandler` decorator (`nanoTime` + boxed `putAttribute` on headers) | — | ✓ |
| two `Instant.now()` + `Duration.between` per Apache connection lease | ✓ | — |
| generated client: `whenComplete(publishMetrics)` + `forwardExceptionTo` onto the call future, to publish to an empty list | — | ✓ ✓ |

### What changed

One check, `MetricUtils.collectsMetrics(collector)` — the `instanceof NoOpMetricCollector` test that
the handlers, `collectServiceEndpointMetrics`, Apache5's pool metric and the CRT client were already
using, given a name — consulted at each measurement site before any of the above. `measureAndReport`
and `reportDuration` short-circuit centrally (which also covers the H7 identity fetch and the async
signing stage without touching them); the two attempt stages return the wrapped result directly; the
HTTP-request and response stages skip the counters and decorators; the async HTTP stage passes the raw
publisher and handler through; Apache5 reads the thread-local collector before touching the clock.
Every internal timing attribute the stages exchange is read only by the stages that report it, and they
make the same check, so nothing downstream is left reading an attribute that was never written.

Two things were kept on purpose:

- **Content-Length enforcement was riding inside the metrics decorator.** `TrackingContentStreamProvider`
  did two jobs — count bytes and wrap each stream in `LengthAwareInputStream` (a correctness check
  from `5f2af7768e7`). Skipping the decorator wholesale would have dropped the check, and the
  pre-existing `execute_testLengthChecking` caught exactly that. It now applies with or without a
  collector; only the counting stream is conditional. And since every HTTP client reads the provider
  from `HttpExecuteRequest`, not from the `SdkHttpFullRequest`, the decorated provider goes there and
  **the request is no longer rebuilt on any call, metrics on or off** — a saving that predates the
  metrics question.
- **With a publisher, nothing changes.** Same measurements, same values, same chain of futures. The
  paired run includes a metrics-*on* pass for exactly this reason.

### Measurement (paired, host, 200k/30k small × 7 reps, 80k/15k batch × 5 reps, concurrency 1)

Base is H7 rebuilt on the same harness commit (installed as `2.54.18-H7`). The default configuration —
no publisher — is the treatment:

| client | scenario | h7 | h8 | Δ app CPU | pair spread | wins | Δ latency |
|---|---|---:|---:|---:|---:|---:|---:|
| v2-sync | small-get | 81.7 | 78.7 | **−3.7%** | ±2.3% | **7/7** | −2.2% |
| v2-sync | small-put | 77.6 | 73.5 | **−5.1%** | ±5.1% | 6/7 | −3.1% |
| v2-async | small-get | 129.6 | 126.2 | **−2.6%** | ±2.2% | 5/7 | −1.4% |
| v2-async | small-put | 127.2 | 121.6 | **−4.3%** | ±4.3% | 6/7 | −2.6% |
| smithy (control) | small-get / small-put | 51.1 / 45.6 | 50.0 / 45.7 | −1.7% / +0.4% | ±9.5% / ±7.5% | 5/7, 4/7 | |
| v2-sync / v2-async | batch-put | 267.6 / 320.8 | 262.9 / 313.1 | **−1.7% / −2.4%** | ±0.8% / ±1.0% | **5/5, 5/5** | −1.1% / −1.4% |

Small-put moving more than small-get (−4.1 vs −3.0 µs sync) is the mechanism showing through: it is
the scenario with a request body, where the byte-counting provider used to be installed. Batch-put's
5/5 at ±1% is the same fixed saving under payload work. The control is flat.

And the pass that had to be flat — a publisher **attached**, 5 reps, small-get:

| client | h7 | h8 | Δ app CPU | pair spread | wins |
|---|---:|---:|---:|---:|---:|
| v2-sync | 97.2 | 98.0 | +0.9% | ±4.1% | 3/5 |
| v2-async | 147.9 | 148.1 | +0.1% | ±1.4% | 2/5 |

No regression where metrics are on. That pass also puts a number on something worth knowing in its own
right: with a publisher attached, the sync small-get costs **97 µs against 79 without — metrics are ~18
µs/op, a fifth of the call** — which is what the pipeline was spending to produce numbers nobody
collected, and is the ceiling on what this class of change could ever recover.

### Mechanism check (equal 35k-op alloc profiles; 300k-op CPU profiles)

| | sync H7 | sync H8 | async H7 | async H8 |
|---|---:|---:|---:|---:|
| self time in metric machinery (µs/op) | 1.36 | **0.91** | 4.69 | **1.83** |
| … of which the user-agent business-metrics string (kept: it is a header) | ~0.5 | ~0.5 | ~1.1 | ~1.1 |
| allocation at metric-machinery sites (B/op) | 839 | **210** | 2,307 | **1,168** |
| `RequestBodyMetrics` + its `AtomicLong`s | 180 | 0 | 105 | 0 |
| `Duration` / `Pair` / boxed `Long` from measure-and-report | 165 | 30 | 150 | 0 |
| `Instant` per Apache connection lease | 75 | 0 | — | — |
| `whenComplete` stages: attempt, HTTP future, publish, and their `forwardExceptionTo` twins | — | — | ~900 | ~390 |
| `BytesReadTrackingPublisher` / `ReadMetricsTrackingResponseHandler` | — | — | 60 + | 0 |
| profile total (B/op) | 31,180 | **29,562** | 36,033 | 35,464 |

What remains in the async column under `forwardExceptionTo` is the pipeline's other, non-metric uses of
it. The remaining metric-machinery time is almost entirely the business-metrics string, which is
correct to keep.

### Verdict

Kept: **−3 to −4 µs/op on every call of every client that has not configured a metric publisher**,
which is the default and the overwhelmingly common configuration, with the enabled path measured and
unchanged. Sync small-get is now **≈ 79 µs/op** on this host against stock 2.54.0's ≈ 150, and the
gap to the bridged prototype's 74 is down to ~5 µs.

The general lesson is the same one as H4/H5/H6/H7, from a different angle: the SDK had made the
*sink* cheap and left the *sources* running. A no-op collector is only a win if the code that feeds it
also asks whether anyone is listening.

Follow-ups:

1. The user-agent business-metrics string (~0.5 µs sync, ~1.1 async) is rebuilt per call from the
   `BusinessMetricCollection`; for a given client and operation most of it is constant. Not a metrics
   question — it is a header — but the same resolve-once shape applies.
2. `forwardExceptionTo`'s remaining async uses each cost a dependent stage; where the source future is
   already complete (the de-futured paths from Phase C), a direct completion would avoid it.
