#!/usr/bin/env python3
"""Summarize JMH JSON results: mean, p50, p99 (ns/op) and gc alloc B/op per (benchmark, testCaseId).

Usage: jmh_summary.py result.json [result2.json ...]
       jmh_summary.py --compare base.json cand.json   (paired delta per case)
"""
import json
import sys


def load(path):
    with open(path) as f:
        data = json.load(f)
    rows = {}
    for entry in data:
        bench = entry["benchmark"].split(".")[-2]  # class name
        case = entry.get("params", {}).get("testCaseId", "-")
        pm = entry["primaryMetric"]
        percentiles = pm.get("scorePercentiles", {})
        alloc = None
        sec = entry.get("secondaryMetrics", {})
        if "gc.alloc.rate.norm" in sec:
            alloc = sec["gc.alloc.rate.norm"]["score"]
        rows[(bench, case)] = {
            "mean": pm["score"],
            "p50": percentiles.get("50.0"),
            "p99": percentiles.get("99.0"),
            "alloc": alloc,
        }
    return rows


def fmt(v, digits=0):
    if v is None:
        return "-"
    return f"{v:,.{digits}f}"


def print_single(path):
    rows = load(path)
    print(f"\n== {path} ==")
    print(f"{'case':<45} {'mean ns':>10} {'p50 ns':>10} {'p99 ns':>10} {'alloc B/op':>12}")
    for (bench, case), r in sorted(rows.items()):
        print(f"{case:<45} {fmt(r['mean']):>10} {fmt(r['p50']):>10} {fmt(r['p99']):>10} {fmt(r['alloc']):>12}")


def print_compare(base_path, cand_path):
    base, cand = load(base_path), load(cand_path)
    print(f"\n== {cand_path} vs {base_path} ==")
    print(f"{'case':<45} {'base ns':>10} {'cand ns':>10} {'d-time':>8} {'base B':>10} {'cand B':>10} {'d-alloc':>8}")
    for key in sorted(base):
        if key not in cand:
            continue
        b, c = base[key], cand[key]
        dt = (c["mean"] - b["mean"]) / b["mean"] * 100 if b["mean"] else None
        da = None
        if b["alloc"] and c["alloc"]:
            da = (c["alloc"] - b["alloc"]) / b["alloc"] * 100
        print(f"{key[1]:<45} {fmt(b['mean']):>10} {fmt(c['mean']):>10} "
              f"{fmt(dt, 1) + '%' if dt is not None else '-':>8} "
              f"{fmt(b['alloc']):>10} {fmt(c['alloc']):>10} "
              f"{fmt(da, 1) + '%' if da is not None else '-':>8}")


if __name__ == "__main__":
    args = sys.argv[1:]
    if args and args[0] == "--compare":
        print_compare(args[1], args[2])
    elif not (args and args[0] == "--paired"):
        for p in args:
            print_single(p)


def paired(dirpath, prefix, base_arm="base", cand_arm="cand"):
    """Paired multi-rep comparison: <prefix>-<arm>-rep<N>.json in dirpath."""
    import glob as g
    import statistics as st

    def collect(arm):
        runs = {}
        for path in sorted(g.glob(f"{dirpath}/{prefix}-{arm}-rep*.json")):
            for key, r in load(path).items():
                runs.setdefault(key, []).append(r)
        return runs

    base, cand = collect(base_arm), collect(cand_arm)
    print(f"\n== paired {prefix}: {cand_arm} vs {base_arm} ({dirpath}) ==")
    hdr = (f"{'bench/case':<58} {'base ns':>9} {'cand ns':>9} {'d-time':>8} {'spread':>7} "
           f"{'base B':>9} {'cand B':>9} {'d-alloc':>8}")
    print(hdr)
    for key in sorted(base):
        if key not in cand:
            continue
        b_runs, c_runs = base[key], cand[key]
        n = min(len(b_runs), len(c_runs))
        deltas = [(c_runs[i]["mean"] - b_runs[i]["mean"]) / b_runs[i]["mean"] * 100 for i in range(n)]
        bm = st.mean(r["mean"] for r in b_runs)
        cm = st.mean(r["mean"] for r in c_runs)
        dt = (cm - bm) / bm * 100
        spread = (max(deltas) - min(deltas)) / 2 if n > 1 else 0
        ba = st.mean(r["alloc"] for r in b_runs if r["alloc"]) if any(r["alloc"] for r in b_runs) else None
        ca = st.mean(r["alloc"] for r in c_runs if r["alloc"]) if any(r["alloc"] for r in c_runs) else None
        da = (ca - ba) / ba * 100 if ba and ca else None
        label = key[1] if key[0].startswith(("JsonRpc10", "RpcV2")) else f"{key[0]}:{key[1]}"
        # include method name when multiple methods present
        print(f"{label:<58} {fmt(bm):>9} {fmt(cm):>9} {dt:>+7.1f}% {'±' + format(spread, '.1f') + '%':>7} "
              f"{fmt(ba):>9} {fmt(ca):>9} "
              f"{format(da, '+.1f') + '%' if da is not None else '-':>8}")


def load_with_method(path):
    """Like load() but keys include the method name."""
    with open(path) as f:
        data = json.load(f)
    rows = {}
    for entry in data:
        parts = entry["benchmark"].split(".")
        bench, method = parts[-2], parts[-1]
        case = entry.get("params", {}).get("testCaseId", "-")
        pm = entry["primaryMetric"]
        alloc = None
        sec = entry.get("secondaryMetrics", {})
        if "gc.alloc.rate.norm" in sec:
            alloc = sec["gc.alloc.rate.norm"]["score"]
        rows[(f"{bench}.{method}", case)] = {
            "mean": pm["score"],
            "p50": pm.get("scorePercentiles", {}).get("50.0"),
            "p99": pm.get("scorePercentiles", {}).get("99.0"),
            "alloc": alloc,
        }
    return rows


if __name__ == "__main__" and sys.argv[1:2] == ["--paired"]:
    # jmh_summary.py --paired DIR PREFIX [BASE_ARM CAND_ARM]
    load = load_with_method  # noqa: F811 — use method-aware keys for paired mode
    a = sys.argv[2:]
    paired(a[0], a[1], *(a[2:4] or ["base", "cand"]))
