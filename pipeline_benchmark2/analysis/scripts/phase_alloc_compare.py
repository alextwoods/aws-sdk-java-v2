#!/usr/bin/env python3
"""Compare per-operation allocation across optimization phases.

Allocation (asprof alloc --total, bytes) is far less sensitive to machine contention than wall
clock or CPU time, so it is the primary signal for judging allocation-focused optimizations.

Converts each case's alloc.jfr to alloc.collapsed on demand (via jfrconv), aggregates bytes by
category, and reports per-phase totals plus deltas against the first phase given.

Usage:
    phase_alloc_compare.py [--ops N] [--clients LIST] [--top N] LABEL=RUNDIR [LABEL=RUNDIR ...]
"""
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from profile_agg import alloc_site, categorize  # noqa: E402

SCENARIOS = ["small-get", "small-put", "batch-get", "batch-put"]
JVM_CATS = {"jit-compiler", "gc-vm", "benchmark-harness"}


def collapsed_for(jfr):
    """Path to the .collapsed for a .jfr, converting if absent or stale."""
    collapsed = jfr.with_suffix(".collapsed")
    if not collapsed.exists() or collapsed.stat().st_mtime < jfr.stat().st_mtime:
        subprocess.run(["jfrconv", "--alloc", "--total", "-o", "collapsed", str(jfr),
                        str(collapsed)], check=True, capture_output=True)
    return collapsed


def load(jfr):
    """(category -> bytes, site -> bytes) for one case."""
    cats = defaultdict(int)
    sites = defaultdict(int)
    for line in collapsed_for(jfr).read_text().splitlines():
        idx = line.rfind(" ")
        if idx < 0:
            continue
        frames = line[:idx].split(";")
        weight = int(line[idx + 1:])
        cat = categorize(frames)
        cats[cat] += weight
        if cat not in JVM_CATS:
            sites[alloc_site(frames)] += weight
    return cats, sites


def client_bytes(cats):
    return sum(w for c, w in cats.items() if c not in JVM_CATS)


def main(argv):
    ops = 220_000
    clients = ["v2-sync", "v2-async"]
    top = 12
    phases = []

    i = 0
    while i < len(argv):
        a = argv[i]
        if a == "--ops":
            ops = int(argv[i + 1]); i += 2
        elif a == "--clients":
            clients = argv[i + 1].split(","); i += 2
        elif a == "--top":
            top = int(argv[i + 1]); i += 2
        elif "=" in a:
            label, rundir = a.split("=", 1)
            phases.append((label, Path(rundir))); i += 1
        else:
            raise SystemExit(f"unexpected argument: {a}")

    if not phases:
        raise SystemExit(__doc__)

    print("# Allocation per operation by phase\n")
    print("Runs compared (first is the baseline):\n")
    for label, rundir in phases:
        print(f"- `{label}`: `{rundir}`")
    print()
    print(f"asprof alloc `--total` bytes divided by {ops:,} ops (warmup + measured). Client-code")
    print("bytes only: JIT, GC/VM and benchmark-harness stacks are excluded.\n")

    # phase -> client -> scenario -> (cats, sites)
    data = {}
    for label, rundir in phases:
        data[label] = {}
        for client in clients:
            data[label][client] = {}
            for scenario in SCENARIOS:
                jfr = rundir / f"{client}_{scenario}" / "alloc.jfr"
                if jfr.exists():
                    data[label][client][scenario] = load(jfr)

    base_label = phases[0][0]

    print("## Totals (bytes/op, client code)\n")
    header = "| client | scenario | " + f"{base_label} |"
    divider = "|--------|----------|" + "----:|"
    for label, _ in phases[1:]:
        header += f" {label} | delta |"
        divider += "----:|----:|"
    print(header)
    print(divider)
    for client in clients:
        for scenario in SCENARIOS:
            if scenario not in data[base_label].get(client, {}):
                continue
            base = client_bytes(data[base_label][client][scenario][0]) / ops
            cells = [f"{base:,.0f}"]
            for label, _ in phases[1:]:
                entry = data[label].get(client, {}).get(scenario)
                if entry is None:
                    cells += ["-", "-"]
                    continue
                cur = client_bytes(entry[0]) / ops
                cells += [f"{cur:,.0f}", f"{(cur - base) / base * 100:+.1f}%"]
            print(f"| {client} | {scenario} | " + " | ".join(cells) + " |")
    print()

    print("## By category (bytes/op)\n")
    for client in clients:
        for scenario in SCENARIOS:
            if scenario not in data[base_label].get(client, {}):
                continue
            print(f"### {client} / {scenario}\n")
            all_cats = set()
            for label, _ in phases:
                entry = data[label].get(client, {}).get(scenario)
                if entry:
                    all_cats |= {c for c in entry[0] if c not in JVM_CATS}
            ordered = sorted(all_cats,
                             key=lambda c: -data[base_label][client][scenario][0].get(c, 0))
            header = "| category | " + " | ".join(label for label, _ in phases) + " | delta |"
            divider = "|----------|" + "----:|" * (len(phases) + 1)
            print(header)
            print(divider)
            for cat in ordered:
                cells = []
                for label, _ in phases:
                    entry = data[label].get(client, {}).get(scenario)
                    cells.append(f"{entry[0].get(cat, 0) / ops:,.0f}" if entry else "-")
                first = data[base_label][client][scenario][0].get(cat, 0) / ops
                last_entry = data[phases[-1][0]].get(client, {}).get(scenario)
                if last_entry and first > 0:
                    last = last_entry[0].get(cat, 0) / ops
                    cells.append(f"{(last - first) / first * 100:+.1f}%")
                else:
                    cells.append("-")
                print(f"| {cat} | " + " | ".join(cells) + " |")
            print()

    print(f"## Top {top} allocation sites, baseline vs final phase (bytes/op)\n")
    final_label = phases[-1][0]
    for client in clients:
        for scenario in SCENARIOS:
            base_entry = data[base_label].get(client, {}).get(scenario)
            final_entry = data[final_label].get(client, {}).get(scenario)
            if not base_entry:
                continue
            print(f"### {client} / {scenario}\n")
            print(f"| site | {base_label} | {final_label} | delta |")
            print("|------|----:|----:|----:|")
            ranked = sorted(base_entry[1].items(), key=lambda kv: -kv[1])[:top]
            for site, bytes_ in ranked:
                b = bytes_ / ops
                if final_entry:
                    f = final_entry[1].get(site, 0) / ops
                    delta = f"{(f - b) / b * 100:+.1f}%" if b else "-"
                    print(f"| `{site}` | {b:,.0f} | {f:,.0f} | {delta} |")
                else:
                    print(f"| `{site}` | {b:,.0f} | - | - |")
            print()


if __name__ == "__main__":
    main(sys.argv[1:])
