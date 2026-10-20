# Optimization experiment: racecar F

> **PRELIMINARY — smoke run not executed, no data.** The smoke run was skipped because it would take more than about 10 minutes (see below). There is no verdict, and the YAML row is unchanged apart from its note.

Racecar F: SigV4 fast-path header signer (`FastV4HeaderSigner`, `V4SigningResources`, `V4SigningKeyCache`). Task 9.20. Current decision: Deferred (later individual optimization).

| arm | ref | commit | SDK version |
|---|---|---|---|
| `feature` | `HEAD` (`feature/pov/schema-decoupling`) | `96224211c70` | 2.49.7-SNAPSHOT |
| `candidate` | `opt/F` | `d1161cfc5ac` | 2.49.7-SNAPSHOT |

## Candidate

One commit on `opt/F`, on top of the feature build: `perf(auth): Re-implement the SigV4 fast-path header signer`. It lives in the worktree `/tmp/aws-sdk-opt-F` and is not merged.

- `DefaultAwsV4HttpSigner.sign` uses `FastV4HeaderSigner` for header auth with non-anonymous credentials and no presigning, chunk encoding, `CHECKSUM_ALGORITHM` or event streaming. It also requires DEBUG logging to be off for `DefaultV4RequestSigner`. Every other case, and `signAsync`, stays on the composed path (`signWithoutFastPath`).
- `V4SigningResources` holds per-thread scratch state: the SHA-256 digest, two text builders, byte buffers, the header table and the last date strings. The HMAC `Mac` is the existing per-thread one from `SigningAlgorithm`.
- `V4SigningKeyCache` is a `BoundedCache` (300 entries) keyed by secret key, date stamp, region and service.
- Correctness: `FastV4HeaderSignerDifferentialTest` signs 10,000 randomized requests (seed 20260920) and 4,000 more on four threads (seeds 20260921–20260924). Each request is signed on both paths, and the test requires the same exception or the same headers, method, URI parts and payload object. The http-auth-aws module tests run unmodified.

## Smoke run (not executed)

The experiment needs two SDK builds that are not cached. One is the feature build at `96224211c70`; the 9.19 worktree `$TMPDIR/aws-sdk-paired-ab-fed6d903057d` is at `fed6d903057`, and no prebuilt jars are cached for `96224211c70`. The other is the candidate build at `d1161cfc5ac`. Each also needs a `test/sdk-benchmarks` `benchmarks.jar`, and that module depends on `ec2`, `cloudfront` and `protocol-tests`, which are not in the local repository. This is more than the roughly 10 minutes the smoke run was allowed. Command, from the feature checkout:

```
scripts/schema-serde/optimization_experiment.sh F opt/F --preliminary \
  --record docs/design/schema-serde/optimizations/F-smoke.md \
  --reps 2 --iterations 300 --warmup 200 --clients v2-sync --scenarios small-get \
  --jmh-opts "-wi 1 -i 2 -f 1 -w 1s -r 1s"
```

The full measurement on a quiet host uses the defaults: `scripts/schema-serde/optimization_experiment.sh F opt/F --pin-client 0-15 --pin-server 16-31`.

**Before either run:** the JMH target is now `AwsV4HttpSignerBenchmark` (see `targets.json`). That benchmark exists only in the candidate commit, so the feature arm's `benchmarks.jar` would not contain it, and the target would have no feature result or floor. Bring `test/sdk-benchmarks/.../signer/AwsV4HttpSignerBenchmark.java` to the feature build first, for example as its own tooling commit, and rebase `opt/F` on it.
