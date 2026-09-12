#!/usr/bin/env python3
"""Per-arm CPU-category and allocation tables for pipeline_benchmark3.

Reuses pipeline_benchmark2's categorizer (profile_agg.py) so the category definitions are identical to
the ones the cross-SDK report used, and adds the categories the bridge needs: the bridge's own adapter
layer and the smithy-java runtime it delegates to, which would otherwise be scattered across "other"
and the serde buckets.

Allocation is divided by the operations each recording actually covered (warmup + measured), read from
that profile's own log -- the same correction the cross-SDK report needed, because warmup length differs
per client and a single nominal divisor silently misstates bytes per operation.

Usage: bench3_profiles.py <profiles-dir>
"""
import re
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "pipeline_benchmark2" / "analysis" / "scripts"))
import profile_agg  # noqa: E402  (path set above)

ARMS = ["v1", "v2-sync-stock254", "v2-async-stock254", "v2-sync-opt", "v2-async-opt",
        "v2-bridged", "smithy"]
SCENARIOS = ["small-get", "small-put", "batch-get", "batch-put"]

# Checked before the inherited categories: a bridged call runs through the adapter layer and then into
# smithy-java, and both must be visible as themselves rather than folded into neighbouring buckets.
BRIDGE_RX = re.compile(r"awssdk/bridge/smithyjava|awssdk\.bridge\.smithyjava")
SMITHY_RT_RX = re.compile(r"smithy/java/|smithy\.java\.")

CATEGORY_ORDER = ["socket-syscall", "http-client", "pipeline-framework", "bridge-adapter",
                  "smithy-runtime", "signing", "crypto", "marshall", "unmarshall", "json",
                  "retry", "endpoint-rules", "thread-sync", "other"]


def categorize(frames):
    """Inherited categories first; the bridge/smithy buckets are a fallback for what they leave over.

    Order matters and the obvious order is wrong. A bridged or native-smithy call is *entirely* inside
    those packages, so testing for them first swallows the whole profile -- transport, signing, serde and
    syscalls alike -- and the arm becomes one undifferentiated block. Letting the inherited categorizer
    run first keeps the arms comparable (its syscall, crypto, json and signer patterns already match
    smithy's frames, and `client/core` maps smithy's per-call orchestration onto pipeline-framework), and
    leaves these two buckets to mean what is actually wanted: work in the bridge's adapter layer, or in
    the smithy runtime, that belongs to no recognized stage.
    """
    base = profile_agg.categorize(frames)
    if base != "other":
        return base
    for frame in reversed(frames):
        f = profile_agg.SUFFIX.sub("", frame)
        if BRIDGE_RX.search(f):
            return "bridge-adapter"
        if SMITHY_RT_RX.search(f):
            return "smithy-runtime"
    return base


def ops_from_log(log):
    """warmup + measured operations for this recording, taken from its RESULT line.

    Read from the RESULT line specifically, not the first match in the file: the human-readable run
    header prints thousands separators (`iterations=35,000`), so a `(\\d+)` search anywhere in the log
    happily returns 35 and inflates every bytes-per-operation figure by three orders of magnitude.
    """
    if not log.exists():
        return None
    result = None
    for line in log.read_text(errors="ignore").splitlines():
        if line.startswith("RESULT "):
            result = line
    if result is None:
        return None
    m_it = re.search(r"\biterations=(\d+)", result)
    m_wu = re.search(r"\bwarmup_ops=(\d+)", result)
    if not m_it:
        return None
    return int(m_it.group(1)) + (int(m_wu.group(1)) if m_wu else 0)


def app_cpu(log):
    if not log.exists():
        return None
    m = re.search(r"app_cpu_us_per_op=([0-9.]+)", log.read_text(errors="ignore"))
    return float(m.group(1)) if m else None


def scan(path):
    total = 0
    cats = defaultdict(int)
    with open(path) as fh:
        for line in fh:
            line = line.rstrip("\n")
            i = line.rfind(" ")
            if i < 0:
                continue
            stack, w = line[:i], int(line[i + 1:])
            frames = stack.split(";")
            cat = categorize(frames)
            if cat in ("jit-compiler", "gc-vm", "benchmark-harness"):
                continue
            total += w
            cats[cat] += w
    return total, cats


def main():
    base = Path(sys.argv[1])
    for scenario in SCENARIOS:
        print(f"\n\n########## {scenario}")
        print("\n--- CPU by category (% of client-code samples), and us/op scaled by the run's app CPU")
        rows = {}
        for arm in ARMS:
            cpu = base / f"{arm}-{scenario}-cpu.collapsed"
            if not cpu.exists():
                continue
            total, cats = scan(cpu)
            us = app_cpu(base / f"{arm}-{scenario}-cpu.log")
            rows[arm] = (total, cats, us)
        if not rows:
            continue
        hdr = "".join(f"{a:>21s}" for a in rows)
        print(f"{'category':18s}{hdr}")
        for cat in CATEGORY_ORDER:
            line = f"{cat:18s}"
            any_val = False
            for arm, (total, cats, us) in rows.items():
                w = cats.get(cat, 0)
                if w:
                    any_val = True
                pct = w / total * 100 if total else 0
                cell = f"{pct:6.1f}%" + (f" /{w / total * us:7.1f}us" if us else "")
                line += f"{cell:>21s}"
            if any_val:
                print(line)
        line = f"{'TOTAL app cpu':18s}"
        for arm, (total, cats, us) in rows.items():
            line += f"{(f'{us:.1f}us' if us else '-'):>21s}"
        print(line)

        print("\n--- allocation bytes/op by category")
        arows = {}
        for arm in ARMS:
            al = base / f"{arm}-{scenario}-alloc.collapsed"
            if not al.exists():
                continue
            ops = ops_from_log(base / f"{arm}-{scenario}-alloc.log")
            if not ops:
                continue
            total, cats = scan(al)
            arows[arm] = (total, cats, ops)
        if not arows:
            continue
        hdr = "".join(f"{a:>16s}" for a in arows)
        print(f"{'category':18s}{hdr}")
        for cat in CATEGORY_ORDER:
            line = f"{cat:18s}"
            any_val = False
            for arm, (total, cats, ops) in arows.items():
                w = cats.get(cat, 0)
                if w:
                    any_val = True
                line += f"{w / ops:16.0f}"
            if any_val:
                print(line)
        line = f"{'TOTAL B/op':18s}"
        for arm, (total, cats, ops) in arows.items():
            line += f"{total / ops:16.0f}"
        print(line)


if __name__ == "__main__":
    main()
