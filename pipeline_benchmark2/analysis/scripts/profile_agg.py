#!/usr/bin/env python3
"""Aggregate collapsed-stack profiles (async-profiler jfrconv output) for the benchmark cases.

Frame format notes (asprof 4.x / jfrconv):
- Java frames use '/' package separators and carry _[j]/_[i]/_[0]/_[1] suffixes.
- Native frames are plain symbols (e.g. 'write', 'CompileBroker::compiler_thread_loop').
- Alloc profiles (--total): weight is bytes; the LEAF frame is the allocated type (dotted).

CPU: total samples, category buckets, top self frames.
Alloc: total bytes, category buckets, top allocated types, top SDK-code allocation sites.

Usage: profile_agg.py cpu|alloc <collapsed-file> [topN]
"""
import re
import sys

SUFFIX = re.compile(r"_\[[a-z0-9]\]$")

# Whole-stack thread-role checks (applied to root frames first).
JIT_RX = re.compile(r"CompileBroker::compiler_thread_loop")
GCVM_RX = re.compile(r"WorkerThread::run|ConcurrentGCThread::run|VMThread::run|GCTaskThread")

# Java/native categories, checked deepest-frame-first; first match wins.
CATEGORIES = [
    # Syscall leaves. macOS reports bare names (write, read, kevent); glibc on Linux reports
    # wrapper symbols (__GI___libc_write, __poll, __read), and the vDSO shows as [vdso]. Matching
    # only the macOS spelling silently pushed all syscall CPU up the stack into whatever Java
    # category matched next, which on Linux inflated pipeline-framework from ~18% to ~43%.
    ("socket-syscall", re.compile(r"^(__GI_)?(__libc_)?(__)?"
                                  r"(write|read|pwrite|pread|writev|readv|"
                                  r"poll|ppoll|epoll_wait|epoll_pwait|kevent|"
                                  r"send|sendto|sendmsg|recv|recvfrom|recvmsg|"
                                  r"connect|close|fcntl|accept|accept4|shutdown)$"
                                  r"|^\[vdso\]$|^syscall$")),
    ("crypto", re.compile(r"sha256|SHA2|MessageDigest|HmacCore|Hmac|implCompress|Mac\.")),
    ("json", re.compile(r"jackson|smithy/java/json|JsonParser|JsonGenerator|JsonFactory|"
                        r"JsonToken|JsonNode")),
    ("signing", re.compile(r"[Ss]igner|SigningStage|[Ss]igv4|SigV4|http/auth/aws|auth/aws/")),
    # unmarshall MUST precede marshall: "Unmarshaller" contains "marshall", "Deserializer"
    # contains "serializ".
    ("unmarshall", re.compile(r"[Uu]nmarshall|[Dd]eserializ|ResponseHandler|JsonResponseHandler")),
    ("marshall", re.compile(r"(?<![Uu]n)[Mm]arshall|internal/marshall|(?<![Dd]e)[Ss]erializ|serde")),
    ("thread-sync", re.compile(r"__psynch_cvwait|pthread_cond|Unsafe_Park|ObjectMonitor|"
                               r"Parker::|LockSupport|pthread_mutex|futex")),
    ("endpoint-rules", re.compile(r"endpoints/|EndpointResol|rulesengine|EndpointProvider|"
                                  r"EndpointRule")),
    ("retry", re.compile(r"[Rr]etry|TokenBucket|RateLimiter")),
    # org/apache/hc covers Apache5 (httpcore5/httpclient5), which is what v2-sync actually uses --
    # the older org/apache/http pattern alone matches only Apache 4.x and left Apache5's frames
    # falling through to "other". awssdk/http/apache covers both SDK adapters (apache, apache5).
    ("http-client", re.compile(r"org/apache/http|org/apache/hc|awssdk/http/apache|"
                               r"com/amazonaws/http|apache/internal|"
                               r"smithy/java/http|H1Exchange|/crt/|AwsCrt|sun/nio|NioSocketImpl|"
                               r"SocketDispatcher|Socket(In|Out)putStream|libsystem_kernel")),
    ("pipeline-framework", re.compile(r"pipeline/stages|AmazonHttpClient|ClientHandler|"
                                      r"ClientPipeline|ClientCall|Interceptor|RequestExecution|"
                                      r"client/core|MetricUtils|MetricCollect")),
    ("benchmark-harness", re.compile(r"benchmark/e2e")),
]


def categorize(frames):
    head = ";".join(frames[:6])
    if JIT_RX.search(head):
        return "jit-compiler"
    if GCVM_RX.search(head):
        return "gc-vm"
    for frame in reversed(frames):
        f = SUFFIX.sub("", frame)
        for name, rx in CATEGORIES:
            if rx.search(f):
                return name
    return "other"


def clean(frame):
    return SUFFIX.sub("", frame)


def alloc_site(frames):
    """Nearest caller of the allocation in SDK/benchmark-relevant code (skip JDK plumbing)."""
    skip = re.compile(r"^(java/|jdk/|sun/|javax/|com/sun/|byte\[|char\[|int\[|long\[|"
                      r"java\.|Object\[)")
    for frame in reversed(frames[:-1]):
        f = clean(frame)
        if not skip.match(f):
            return f
    return clean(frames[0]) if frames else "?"


def main():
    mode, path = sys.argv[1], sys.argv[2]
    top_n = int(sys.argv[3]) if len(sys.argv) > 3 else 25

    total = 0
    cats = {}
    self_w = {}
    sites = {}
    types = {}

    with open(path) as f:
        for line in f:
            line = line.rstrip("\n")
            idx = line.rfind(" ")
            if idx < 0:
                continue
            stack, weight = line[:idx], int(line[idx + 1:])
            frames = stack.split(";")
            total += weight
            cat = categorize(frames)
            cats[cat] = cats.get(cat, 0) + weight
            if mode == "cpu":
                leaf = clean(frames[-1])
                self_w[leaf] = self_w.get(leaf, 0) + weight
            else:
                t = clean(frames[-1])
                types[t] = types.get(t, 0) + weight
                s = alloc_site(frames)
                sites[s] = sites.get(s, 0) + weight

    unit = "samples" if mode == "cpu" else "bytes"
    print(f"TOTAL {total} {unit}")
    print("\nCATEGORIES (deepest-frame match, whole-process):")
    for name, w in sorted(cats.items(), key=lambda kv: -kv[1]):
        print(f"  {w / total * 100:6.2f}%  {w:>14,}  {name}")

    if mode == "cpu":
        print(f"\nTOP {top_n} SELF FRAMES:")
        for frame, w in sorted(self_w.items(), key=lambda kv: -kv[1])[:top_n]:
            print(f"  {w / total * 100:6.2f}%  {w:>10,}  {frame}")
    else:
        print(f"\nTOP {top_n} ALLOCATED TYPES:")
        for t, w in sorted(types.items(), key=lambda kv: -kv[1])[:top_n]:
            print(f"  {w / total * 100:6.2f}%  {w:>14,}  {t}")
        print(f"\nTOP {top_n} ALLOCATION SITES (nearest non-JDK caller):")
        for s, w in sorted(sites.items(), key=lambda kv: -kv[1])[:top_n]:
            print(f"  {w / total * 100:6.2f}%  {w:>14,}  {s}")


if __name__ == "__main__":
    main()
