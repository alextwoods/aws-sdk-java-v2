# Racecar optimization experiments (task 9.19)

Phase 5 re-implements selected racecar optimizations that the decision table
(`../racecar-decisions.yaml`, design D.6, Requirement 37.1) marks
`Deferred (later individual optimization)`. Each one is kept only if a measurement shows it helps.
This directory holds the experiment records (`<id>.md`) and the target mapping (`targets.json`).

## One optimization at a time

Tasks 9.20–9.26 (F, E6, E7, B1, D1, E12, E13) run in sequence. Each candidate is a single commit on
top of the current feature build. The feature build is the branch with every earlier candidate
already kept or reverted. A measurement then shows the effect of that one change. Do not start the
next candidate until the previous row has its final decision in the YAML.

## Procedure

1. Commit the candidate on its own branch or as a detached commit on top of the feature build, for
   example `git switch -c opt/E6` and then commit. The candidate's own tests must pass first (the
   task text lists them).
2. Run the experiment from the feature checkout:

   ```
   scripts/schema-serde/optimization_experiment.sh E6 opt/E6 --pin-client 0-15 --pin-server 16-31
   ```

   The script:
   - checks the id against `racecar-decisions.yaml` and `targets.json`;
   - runs the null experiment `paired-ab.sh --null feature=<feature-ref>`, which gives the e2e noise
     floor per client, scenario and metric;
   - runs the paired A/B `paired-ab.sh --jar feature=<the null run's jar> --arm candidate=<ref>`
     with the same settings;
   - if the id has JMH benchmarks, builds `benchmarks.jar` for each commit (in the worktree paired-ab
     uses for that commit, `$TMPDIR/aws-sdk-paired-ab-<sha12>`, with the same local repository
     chaining). It then runs the jars in the order feature, candidate, feature again, with
     `-prof gc -rf json`;
   - writes `docs/design/schema-serde/optimizations/<id>.md` with both runs, the tables and the
     verdict. It exits 0 for KEEP and 3 for REVERT.

   Defaults are paired-ab's: 7 repetitions, 50,000 operations, both clients and the five scenarios.
   JMH uses the benchmark class's defaults. For a smoke run, pass `--reps`, `--iterations`,
   `--warmup`, `--clients`, `--scenarios`, `--jmh-param` and `--jmh-opts "-wi 1 -i 2 -f 1 -w 1s -r 1s"`.
   Prebuilt jars can be passed with `--feature-jar`, `--candidate-jar`, `--feature-jmh-jar` and
   `--candidate-jmh-jar`. `--skip-build` reuses jars from an earlier run when given the same `--out`
   directory. `--help` lists every option.
3. Apply the decision by hand. The script prints the YAML change and never edits the YAML itself.
   - **KEEP**: merge the candidate. Set the row's `decision` to `Applied (re-implemented here)`, set
     `classes` (the runtime classes that implement it) and `applied_in_phase: 5`, and set
     `ab_record: "optimizations/<id>.md"`.
   - **REVERT**: do not merge the candidate (or `git revert <commit>` if it is already on the
     branch). Add `ab_record` and the measured numbers to the reason. The row then either stays
     `Deferred (later individual optimization)` or becomes `Rejected` (with `component: "—"`), as
     the record suggests.

   Then regenerate and check the table:

   ```
   python3 scripts/schema-serde/racecar_decisions.py
   python3 scripts/schema-serde/racecar_decisions.py --check
   ```

   Commit the record, the YAML and the regenerated Markdown together with the candidate (KEEP) or
   on their own (REVERT). `RacecarDecisionTableTest` fails if a phase 5 Applied row has no existing
   `ab_record`.

## Keep rule

Lower is better for every metric. For each experiment the measured metrics fall into two groups:

- **Targets** come from `targets.json` for the id: the e2e client/scenario/metric combinations and
  the JMH benchmarks that the optimization should improve.
- **Guards** are every other measured case for the guard metrics: e2e application CPU µs/op and
  allocated B/op, and JMH score and `gc.alloc.rate.norm`.

The candidate is **kept** only if all of these hold:

- every measured target improves by more than its floor (change < −floor);
- no guard regresses by more than its floor (change > +floor);
- every measured case has a floor;
- neither run reports a problem (failed runs, or mixed harness commits).

Anything else is **REVERT**. The script stops with an error if no target was measured or if the
null run is missing.

The suggested final decision after a REVERT works as follows:

- **Rejected**: the targets were measured cleanly and showed no gain beyond the floor.
- **Stays Deferred (rework)**: a target gained, but a guard regressed.
- **Stays Deferred (undecided)**: a floor is missing, a target floor is wider than 5%, or a run
  failed. Rerun on a quiet host before deciding.

**Floors.**
- E2E floors come from the null experiment: max(|ratio − 1|, the spread of the paired ratios), per
  client, scenario and metric (`paired_ab_summary.floor_pct`).
- JMH floors come from the feature jar run twice: max(|second/first − 1|, each run's relative
  99.9% error as JMH reports it). The two feature runs bracket the candidate run, so drift during
  the experiment also widens the floor.
- A benchmark whose feature B/op is below 1 B/op (the 9.16 zero-allocation benchmarks) is compared
  absolutely: the candidate regresses if it allocates 1 B/op or more.

## Targets (`targets.json`)

| id | task | e2e targets | JMH targets |
|---|---|---|---|
| F | 9.20 | small-get, small-put, application CPU, both clients | `sdk-benchmarks` `AwsV4HttpSignerBenchmark` score (`Sigv4SignerBenchmark` is a guard) |
| E6 | 9.21 | — (e2e is guard only) | `SchemaMarshallBenchmark` score, JSON protocols |
| E7 | 9.22 | — (e2e is guard only) | `SchemaUnmarshallBenchmark` score, JSON protocols |
| B1 | 9.23 | small-get, small-put, B/op, both clients | — |
| D1 | 9.24 | small-get, small-put, application CPU, `v2-sync` (Apache5) | — |
| E12 | 9.25 | small-get, small-put, B/op, `v2-async` (CRT) | — |
| E13 | 9.26 | small-get, small-put, B/op, both clients | — |

The script reruns the mapping each time, so a change to `targets.json` should be committed before
the experiment it affects, and the record notes the mapping's note for the id.

## Tests

`python3 scripts/schema-serde/optimization_experiment.py --self-test` runs the verdict rules on
fixture data.

`example-smoke.md` is a smoke run of the procedure, not a measurement. It compares the feature
build with itself on one scenario and one JMH case.
