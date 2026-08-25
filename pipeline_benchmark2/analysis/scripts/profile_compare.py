#!/usr/bin/env python3
"""Cross-client comparison tables from collapsed profiles.

Emits, per scenario:
- CPU category table: % of client-code samples (JIT/GC-VM/harness excluded), with the
  JVM-thread share shown separately.
- Alloc table: bytes/op by category plus total bytes/op (220k ops = 20k warmup + 200k measured).

Usage: profile_compare.py <rundir> [ops_per_run]
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from profile_agg import categorize  # noqa: E402

CLIENTS = ["v1", "v2-sync", "v2-async", "smithy"]
SCENARIOS = ["small-get", "small-put", "batch-get", "batch-put"]
JVM_CATS = {"jit-compiler", "gc-vm", "benchmark-harness"}


def load(path):
    cats = {}
    total = 0
    for line in path.read_text().splitlines():
        idx = line.rfind(" ")
        if idx < 0:
            continue
        stack, weight = line[:idx], int(line[idx + 1:])
        cat = categorize(stack.split(";"))
        cats[cat] = cats.get(cat, 0) + weight
        total += weight
    return cats, total


def main(rundir, ops):
    rundir = Path(rundir)

    print("## CPU profile: % of client-code samples by category\n")
    print("(JIT/GC/VM threads and benchmark harness excluded from the base; their share of all")
    print("samples is shown in the bottom rows. Whole-process asprof CPU samples, includes warmup.)\n")
    for scenario in SCENARIOS:
        data = {}
        for client in CLIENTS:
            f = rundir / f"{client}_{scenario}" / "cpu.collapsed"
            data[client] = load(f)
        all_cats = sorted({c for cats, _ in data.values() for c in cats if c not in JVM_CATS},
                          key=lambda c: -sum(cats.get(c, 0) for cats, _ in data.values()))
        print(f"### {scenario}\n")
        print("| category | " + " | ".join(CLIENTS) + " |")
        print("|----------|" + "----:|" * len(CLIENTS))
        for cat in all_cats:
            cells = []
            for client in CLIENTS:
                cats, total = data[client]
                base = sum(w for c, w in cats.items() if c not in JVM_CATS)
                cells.append(f"{cats.get(cat, 0) / base * 100:.1f}%" if base else "-")
            print(f"| {cat} | " + " | ".join(cells) + " |")
        for jvm_cat in ["jit-compiler", "gc-vm"]:
            cells = []
            for client in CLIENTS:
                cats, total = data[client]
                cells.append(f"{cats.get(jvm_cat, 0) / total * 100:.1f}%" if total else "-")
            print(f"| _{jvm_cat} (% of all samples)_ | " + " | ".join(cells) + " |")
        cells = []
        for client in CLIENTS:
            cats, total = data[client]
            print_total = sum(w for c, w in cats.items() if c not in JVM_CATS)
            cells.append(f"{print_total:,}")
        print("| _client-code samples_ | " + " | ".join(cells) + " |")
        print()

    print("\n## Allocation profile: bytes per operation by category\n")
    print(f"(asprof alloc, --total bytes; divided by {ops:,} ops = warmup + measured. Includes")
    print("one-time setup allocations, which are negligible at this op count.)\n")
    for scenario in SCENARIOS:
        data = {}
        for client in CLIENTS:
            f = rundir / f"{client}_{scenario}" / "alloc.collapsed"
            data[client] = load(f)
        all_cats = sorted({c for cats, _ in data.values() for c in cats if c not in JVM_CATS},
                          key=lambda c: -sum(cats.get(c, 0) for cats, _ in data.values()))
        print(f"### {scenario}\n")
        print("| category | " + " | ".join(CLIENTS) + " |")
        print("|----------|" + "----:|" * len(CLIENTS))
        for cat in all_cats:
            cells = []
            for client in CLIENTS:
                cats, _ = data[client]
                cells.append(f"{cats.get(cat, 0) / ops:,.0f}")
            print(f"| {cat} | " + " | ".join(cells) + " |")
        cells = []
        for client in CLIENTS:
            cats, _ = data[client]
            base = sum(w for c, w in cats.items() if c not in JVM_CATS)
            cells.append(f"**{base / ops:,.0f}**")
        print("| **total (client code)** | " + " | ".join(cells) + " |")
        print()


if __name__ == "__main__":
    main(sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 220_000)
