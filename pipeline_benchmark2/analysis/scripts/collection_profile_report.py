#!/usr/bin/env python3
"""Aggregate every profile in a collect.sh run into one compact text report.

Runs where the collection lives (typically the benchmark host), so that only a few KB of aggregated
tables travel instead of hundreds of MB of .jfr recordings. Converts each recording to collapsed
form on demand via jfrconv, then emits, per case:

  - CPU: share of client-code samples by category, plus the top self frames
  - Allocation: bytes per operation by category, plus the top allocation sites

"Client code" excludes JIT, GC/VM and benchmark-harness stacks, matching the convention used by
phase_alloc_compare.py, so numbers here are comparable with the rest of the project's analysis.

Operation counts are read PER CASE from the run log, not assumed. A profiler recording covers warmup
plus the measured window, and quiescence warmup runs however many operations that client needs to
settle — 85k for one client, 315k for another. Dividing every case by one nominal figure therefore
mis-scales bytes/op differently per client, which is fatal for a cross-SDK absolute comparison and
merely inconvenient for a same-client A/B. `--ops` remains as an override/fallback.

Usage: collection_profile_report.py RUNDIR [--ops N] [--top N]
       RUNDIR is a collect.sh output directory (contains <client>_<scenario>/ subdirectories).
"""
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from profile_agg import alloc_site, categorize, clean  # noqa: E402

JVM_CATS = {"jit-compiler", "gc-vm", "benchmark-harness"}


def collapsed_for(jfr, mode):
    """Convert a .jfr to collapsed form once, caching next to it."""
    out = jfr.with_suffix(f".{mode}.collapsed")
    if not out.exists() or out.stat().st_mtime < jfr.stat().st_mtime:
        flag = "--alloc" if mode == "alloc" else "--cpu"
        cmd = ["jfrconv", flag]
        if mode == "alloc":
            cmd.append("--total")
        cmd += ["-o", "collapsed", str(jfr), str(out)]
        subprocess.run(cmd, check=True, capture_output=True)
    return out


WARMUP_RX = re.compile(r"^=== warmup \S+: ops=([\d,]+)", re.M)
ITER_RX = re.compile(r"iterations=([\d,]+)")


def ops_for(logfile, fallback):
    """warmup + measured operations actually covered by this recording."""
    if not logfile.exists():
        return fallback, "fallback"
    txt = logfile.read_text()
    w = WARMUP_RX.search(txt)
    i = ITER_RX.search(txt)
    if not (w and i):
        return fallback, "fallback"
    warm = int(w.group(1).replace(",", ""))
    iters = int(i.group(1).replace(",", ""))
    return warm + iters, f"{warm:,}+{iters:,}"


def load(jfr, mode):
    cats = defaultdict(int)
    leaves = defaultdict(int)
    sites = defaultdict(int)
    jvm = defaultdict(int)
    for line in collapsed_for(jfr, mode).read_text().splitlines():
        idx = line.rfind(" ")
        if idx < 0:
            continue
        frames = line[:idx].split(";")
        w = int(line[idx + 1:])
        cat = categorize(frames)
        if cat in JVM_CATS:
            jvm[cat] += w
            continue
        cats[cat] += w
        leaves[clean(frames[-1])] += w
        if mode == "alloc":
            sites[alloc_site(frames)] += w
    return cats, leaves, sites, jvm


def main(argv):
    if not argv:
        raise SystemExit(__doc__)
    rundir = Path(argv[0])
    ops = 220_000
    top = 12
    i = 1
    while i < len(argv):
        if argv[i] == "--ops":
            ops = int(argv[i + 1]); i += 2
        elif argv[i] == "--top":
            top = int(argv[i + 1]); i += 2
        else:
            raise SystemExit(f"unexpected argument: {argv[i]}")

    cases = sorted(d.name for d in rundir.iterdir() if d.is_dir() and "_" in d.name)
    print(f"# Profile report — {rundir.name}")
    print(f"\nops per case (warmup+measured): {ops:,}. Client code only: JIT, GC/VM and harness "
          f"stacks excluded.\n")

    for mode, unit in (("cpu", "samples"), ("alloc", "bytes")):
        print(f"\n{'=' * 78}\n## {mode.upper()}\n{'=' * 78}")
        for case in cases:
            jfr = rundir / case / f"{mode}.jfr"
            if not jfr.exists():
                continue
            cats, leaves, sites, jvm = load(jfr, mode)
            total = sum(cats.values())
            if total == 0:
                continue
            case_ops, how = ops_for(rundir / case / f"{mode}.log", ops)
            print(f"\n### {case}")
            if mode == "cpu":
                print(f"client-code {unit}: {total:,}   (jvm/harness excluded: {sum(jvm.values()):,})"
                      f"   ops={case_ops:,} [{how}]")
            else:
                print(f"client-code bytes: {total:,}   = {total / case_ops:,.0f} bytes/op"
                      f"   ops={case_ops:,} [{how}]")
            print("  categories:")
            for name, w in sorted(cats.items(), key=lambda kv: -kv[1]):
                extra = f"  {w / case_ops:>9,.0f} B/op" if mode == "alloc" else ""
                print(f"    {w / total * 100:6.2f}%  {name}{extra}")
            label = "top self frames" if mode == "cpu" else "top allocation sites"
            src = leaves if mode == "cpu" else sites
            print(f"  {label}:")
            for name, w in sorted(src.items(), key=lambda kv: -kv[1])[:top]:
                extra = f"  {w / case_ops:>9,.0f} B/op" if mode == "alloc" else ""
                print(f"    {w / total * 100:6.2f}%{extra}  {name}")


if __name__ == "__main__":
    main(sys.argv[1:])
