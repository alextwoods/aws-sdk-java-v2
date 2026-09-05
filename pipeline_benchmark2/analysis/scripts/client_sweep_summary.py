#!/usr/bin/env python3
"""Summarize a within-jar client sweep from test/standalone-e2e-benchmarks/scripts/collect.sh.

Companion to paired_ab_summary.py, which compares two *jars* (arms identified by the stamped `phase`
label). This one compares *clients inside a single jar* — the case that matters when the thing under
test is a client-construction knob, e.g. the `awssdk.bridge.strip*` properties that remove one bridge
component each.

The pairing argument carries over: collect.sh interleaves repetitions (rep 1 of every case, then rep
2, ...), so rep k of client X and rep k of the reference client ran within the same few minutes and
saw the same machine conditions. Per-rep ratios against the reference are therefore more stable than
either client's absolute number, and the headline statistic is the mean of those ratios.

The reference client is whichever appears first in results.csv, i.e. the first entry in
collect.sh --clients. Deltas are candidate-vs-reference, so a negative number means the candidate is
cheaper.

Usage: client_sweep_summary.py RUNDIR
"""
import statistics
import sys
from collections import OrderedDict, defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paired_ab_summary import METRICS, load  # noqa: E402


def main(argv):
    if len(argv) != 1:
        raise SystemExit(__doc__)
    rundir = Path(argv[0])
    rows = load(rundir)

    clients = list(OrderedDict.fromkeys(r["client"] for r in rows))
    scenarios = list(OrderedDict.fromkeys(r["scenario"] for r in rows))
    if len(clients) < 2:
        raise SystemExit(f"only one client ({clients[0]!r}) present — nothing to compare")

    # A sweep is only interpretable if every row came from the same artifact: the whole point is that
    # the SDK bytes are fixed and only the client differs.
    phases = {r["phase"] for r in rows}
    sdk = {r.get("sdk_commit", "?") for r in rows}

    # client -> scenario -> [rows in rep order]
    data = defaultdict(lambda: defaultdict(list))
    for r in rows:
        data[r["client"]][r["scenario"]].append(r)

    ref = clients[0]
    reps = len(next(iter(data[ref].values()), []))

    print(f"# Client sweep summary — `{rundir.name}`\n")
    print(f"Reference client: `{ref}`. {reps} interleaved reps per case, "
          f"{len(rows)} runs total.\n")
    if len(phases) == 1 and len(sdk) == 1:
        print(f"All runs from one artifact (phase `{phases.pop()}`, sdk_commit `{sdk.pop()}`), so the "
              f"client is the only difference.\n")
    else:
        print(f"**WARNING: runs span multiple artifacts (phases {', '.join(sorted(phases))}; "
              f"sdk_commits {', '.join(sorted(sdk))}). Client differences are confounded with SDK "
              f"differences — use paired_ab_summary.py for cross-jar comparison.**\n")

    not_steady = [r for r in rows if str(r.get("steady_state", "true")).lower() == "false"]
    if not_steady:
        cases = sorted({f'{r["client"]}/{r["scenario"]}' for r in not_steady})
        print(f"**{len(not_steady)} of {len(rows)} runs were not steady-state** (JIT still compiling "
              f"inside the measured window): {', '.join(cases)}. Per-operation CPU for those cases is "
              f"unreliable; raise --iterations or --warmup-max-seconds.\n")

    for field, label, extract, lower_better in METRICS:
        if extract(rows[0]) is None:
            continue
        print(f"## {label}\n")
        header = "| client |"
        divider = "|--------|"
        for s in scenarios:
            header += f" {s} mean | vs {ref} | spread | wins |"
            divider += "----:|----:|----:|----:|"
        print(header)
        print(divider)

        for client in clients:
            cells = []
            for s in scenarios:
                cand_vals = [extract(r) for r in data[client].get(s, [])]
                ref_vals = [extract(r) for r in data[ref].get(s, [])]
                if not cand_vals or any(v is None for v in cand_vals):
                    cells += ["-", "-", "-", "-"]
                    continue
                cells.append(f"{statistics.fmean(cand_vals):,.1f}")
                if client == ref:
                    cells += ["—", "—", "—"]
                    continue
                n = min(len(cand_vals), len(ref_vals))
                ratios = [cand_vals[k] / ref_vals[k] for k in range(n) if ref_vals[k]]
                delta = (statistics.fmean(ratios) - 1.0) * 100
                spread = statistics.stdev(ratios) * 100 if len(ratios) > 1 else 0.0
                better = sum(1 for x in ratios if (x < 1.0) == lower_better and x != 1.0)
                cells += [f"{delta:+.1f}%", f"±{spread:.1f}%", f"{better}/{len(ratios)}"]
            print(f"| `{client}` | " + " | ".join(cells) + " |")
        print()

    print("## Per-client run-to-run spread\n")
    print("Unpaired noise, for comparison with the paired spreads above. wall µs/op.\n")
    print("| client | scenario | min | mean | max | spread |")
    print("|--------|----------|----:|----:|----:|-------:|")
    for client in clients:
        for s in scenarios:
            vals = [float(r["avg_us_per_op"]) for r in data[client].get(s, [])]
            if not vals:
                continue
            lo, hi = min(vals), max(vals)
            print(f"| `{client}` | {s} | {lo:,.1f} | {statistics.fmean(vals):,.1f} | {hi:,.1f} "
                  f"| {(hi - lo) / lo * 100 if lo else 0.0:.1f}% |")
    print()


if __name__ == "__main__":
    main(sys.argv[1:])
