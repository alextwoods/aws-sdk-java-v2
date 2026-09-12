#!/usr/bin/env python3
"""Allocation diff between two collapsed alloc profiles (async-profiler / jfrconv --alloc), in bytes per op.

usage: alloc_diff.py BASE.collapsed CAND.collapsed [--ops N] [--top K] [--filter REGEX]

Ops are read from the sibling .log (RESULT iterations=...) when --ops is not given. Prints total B/op per arm,
then the top sites by absolute change, keyed by allocated type and the nearest SDK caller frame, and optionally
the SDK-caller-chain rows matching --filter (for checking a specific mechanism).
"""
import argparse
import os
import re
import sys

FRAME_SUFFIX = re.compile(r"_\[[a-z0-9]\]$")
JDK = re.compile(r"^(java|jdk|sun|javax)/")


def ops_for(path):
    log = re.sub(r"\.collapsed(\.gz)?$", ".log", path)
    if os.path.exists(log):
        for line in open(log):
            m = re.search(r"RESULT .*iterations=(\d+)", line)
            if m:
                return int(m.group(1))
    return None


def load(path, ops, key_fn):
    total = 0
    sites = {}
    for line in open(path):
        line = line.rstrip("\n")
        i = line.rfind(" ")
        stack, w = line[:i], int(line[i + 1:])
        frames = [FRAME_SUFFIX.sub("", f) for f in stack.split(";")]
        total += w
        k = key_fn(frames)
        if k is not None:
            sites[k] = sites.get(k, 0) + w
    return total / ops, {k: v / ops for k, v in sites.items()}


def site_key(frames):
    leaf = frames[-1].split(".")[-1]
    callers = [f.split("/")[-1] for f in reversed(frames[:-1]) if not JDK.match(f)]
    return leaf + "  <- " + (callers[0] if callers else "?")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("base")
    ap.add_argument("cand")
    ap.add_argument("--ops", type=int)
    ap.add_argument("--top", type=int, default=30)
    ap.add_argument("--filter", help="regex on any frame; prints matching site totals per arm")
    a = ap.parse_args()

    ops_b = a.ops or ops_for(a.base)
    ops_c = a.ops or ops_for(a.cand)
    if not ops_b or not ops_c:
        sys.exit("could not determine op counts; pass --ops")

    tot_b, sites_b = load(a.base, ops_b, site_key)
    tot_c, sites_c = load(a.cand, ops_c, site_key)
    print(f"total allocation: base {tot_b:,.0f} B/op  cand {tot_c:,.0f} B/op  delta {tot_c - tot_b:+,.0f} B/op "
          f"({(tot_c - tot_b) / tot_b * 100:+.1f}%)   [ops base={ops_b} cand={ops_c}]")

    keys = set(sites_b) | set(sites_c)
    rows = sorted(((sites_c.get(k, 0) - sites_b.get(k, 0), k) for k in keys), key=lambda r: -abs(r[0]))
    print(f"\ntop {a.top} sites by |delta| (B/op): base -> cand")
    for d, k in rows[:a.top]:
        print(f"  {sites_b.get(k, 0):7.0f} -> {sites_c.get(k, 0):7.0f}  {d:+7.0f}  {k[:140]}")

    if a.filter:
        rx = re.compile(a.filter)

        def fkey(frames):
            return "match" if any(rx.search(f) for f in frames) else None

        _, fb = load(a.base, ops_b, fkey)
        _, fc = load(a.cand, ops_c, fkey)
        print(f"\nframes matching /{a.filter}/: base {fb.get('match', 0):.0f} B/op  cand {fc.get('match', 0):.0f} B/op")


if __name__ == "__main__":
    main()
