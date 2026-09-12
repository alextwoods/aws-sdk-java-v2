#!/usr/bin/env python3
"""Differential frame attribution between two CPU profiles of the same workload.

For every SDK frame (any depth), sums the client-code samples of stacks containing it in each profile and
prints the frames whose share moved most. Frames present in only one profile are the direct fingerprint of
a code change; frames present in both but heavier are where existing work grew.

Usage: frame_diff.py <old.collapsed> <old_label> <new.collapsed> <new_label> [min_pp]
"""
import re
import sys

old_p, old_l, new_p, new_l = sys.argv[1:5]
min_pp = float(sys.argv[5]) if len(sys.argv) > 5 else 0.25
JITGC = re.compile(r"CompileBroker::compiler_thread_loop|WorkerThread::run|ConcurrentGCThread::run|VMThread::run")
HARNESS = re.compile(r"benchmark/e2e")
SDK = re.compile(r"^software/amazon/awssdk/")


def scan(path):
    total = 0
    incl = {}
    with open(path) as fh:
        for line in fh:
            line = line.rstrip("\n")
            i = line.rfind(" ")
            stack, w = line[:i], int(line[i + 1:])
            if JITGC.search(stack):
                continue
            frames = [re.sub(r"\$\$Lambda\.0x[0-9a-f]+", "$$Lambda", re.sub(r"_\[[a-z0-9]\]$", "", f)) for f in stack.split(";")]
            if not any(SDK.match(f) for f in frames):
                continue
            total += w
            seen = set()
            for f in frames:
                if SDK.match(f) and f not in seen:
                    seen.add(f)
                    incl[f] = incl.get(f, 0) + w
    return total, incl


ot, oi = scan(old_p)
nt, ni = scan(new_p)
rows = []
for f in set(oi) | set(ni):
    o = oi.get(f, 0) / ot * 100
    n = ni.get(f, 0) / nt * 100
    if abs(n - o) >= min_pp:
        rows.append((n - o, o, n, f))
rows.sort(key=lambda r: -abs(r[0]))

def short(f):
    return f.replace("software/amazon/awssdk/", "").replace("/", ".")

print(f"inclusive share of SDK-rooted client samples, {old_l} -> {new_l}  (pp = percentage points)")
print(f"\n### heavier or new in {new_l}")
for d, o, n, f in rows:
    if d > 0:
        tag = "NEW " if o == 0 else "    "
        print(f"  {tag}{o:6.2f}% -> {n:6.2f}%  {d:+6.2f}pp  {short(f)}")
print(f"\n### lighter or gone in {new_l}")
for d, o, n, f in rows:
    if d < 0:
        tag = "GONE" if n == 0 else "    "
        print(f"  {tag}{o:6.2f}% -> {n:6.2f}%  {d:+6.2f}pp  {short(f)}")
