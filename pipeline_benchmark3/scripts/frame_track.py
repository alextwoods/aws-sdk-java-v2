#!/usr/bin/env python3
"""Track the inclusive share of chosen frames across a sequence of profiles (one per SDK version).

Shares are of SDK-rooted client samples, so they are comparable across JVMs even where absolute CPU is not.
Usage: frame_track.py <dir> <version>... -- pattern...
"""
import re
import sys
from pathlib import Path

args = sys.argv[1:]
sep = args.index("--")
d, versions, patterns = Path(args[0]), args[1:sep], args[sep + 1:]
JITGC = re.compile(r"CompileBroker::compiler_thread_loop|WorkerThread::run|ConcurrentGCThread::run|VMThread::run")
SDK = re.compile(r"^software/amazon/awssdk/")
rx = [re.compile(p) for p in patterns]


def scan(path):
    total = 0
    hit = [0] * len(rx)
    for line in open(path):
        line = line.rstrip("\n")
        i = line.rfind(" ")
        stack, w = line[:i], int(line[i + 1:])
        if JITGC.search(stack):
            continue
        frames = [re.sub(r"_\[[a-z0-9]\]$", "", f) for f in stack.split(";")]
        if not any(SDK.match(f) for f in frames):
            continue
        total += w
        joined = ";".join(frames)
        for k, r in enumerate(rx):
            if r.search(joined):
                hit[k] += w
    return total, hit


print(f"{'version':9s}" + "".join(f"{p[:22]:>24s}" for p in patterns))
for v in versions:
    p = d / f"{v}-cpu.collapsed"
    if not p.exists():
        continue
    total, hit = scan(p)
    print(f"{v:9s}" + "".join(f"{h / total * 100:23.2f}%" for h in hit))
