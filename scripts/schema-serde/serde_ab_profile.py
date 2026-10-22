#!/usr/bin/env python3
#
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#  http://aws.amazon.com/apache2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
#
"""Side-by-side async-profiler comparison of the serde A/B arms (serde-ab.sh --profile; serde benchmark plan D9).

The profiling pass of scripts/schema-serde/serde-ab.sh runs every serde class, param, arm and event (cpu, alloc)
under JMH's async-profiler integration and records them in <run dir>/profile-runs.tsv. This script maps the samples
of BOTH architectures (legacy SdkField marshallers on the baseline, ClientProtocol + ported codecs on the feature
arm) onto the same logical work, so that one can check that both arms do the same net work per operation and see
where time and allocation go. Python 3 standard library only; reuses hot_path_frames.py (parse_line, normalize,
hot_frames) and serde_ab_summary.py (timing means).

Usage:
  serde_ab_profile.py compare RUNDIR [--timing RUN_JSON] [--out FILE]
      Writes RUNDIR/profile-comparison.md (or FILE). --timing is a serde-ab.json of a timing run of the same jars
      (serde_ab_summary.py collect); its mean ns/op and gc.alloc.rate.norm turn shares into ns/op and B/op per
      category. Without it the profiling runs' own JMH results are used (perturbed by the profiler; marked).
  serde_ab_profile.py --self-test
      Runs the fixture tests below.

Samples: only stacks above the JMH stub (`..._jmhStub`, i.e. inside the benchmark method) count; frames are
normalized (JIT suffixes `_[j]`, `_[i]`; lambda and hidden-class names are cut at `$$Lambda`). Allocation profiles
are recorded with async-profiler `total`, so their counts are bytes; their last frame is the allocated type, which
is dropped before categorization (the allocation site decides).

Categories: each sample gets exactly ONE category, in two passes over its frames.
  1. Scope rules, root to leaf: the first frame that matches one decides, so everything below it belongs to it.
       harness       JMH frames above the stub (Blackhole.consume)
       input         test input / response construction in the timed method: an SdkHttpFullResponse builder,
                     AbortableInputStream or ByteArrayInputStream called directly by the benchmark class
       construction  per-call marshaller or protocol construction (the legacy arm builds its marshaller inside the
                     timed method; the feature arm resolves its protocol once per trial)
       metadata      response metadata: request id and AwsResponseMetadata/SdkHttpResponse attachment
  2. Work rules, leaf to root: the innermost frame that matches a rule decides (rule order breaks ties within
     one frame).
       format        string/number/timestamp/base64 formatting and parsing (java.time, DateUtils, NumberCodec ...)
       bytes         byte buffers and array copies (arraycopy stubs, Arrays.copyOf, java.nio, byte streams, IoUtils)
       reflection    java.lang.reflect, jdk.internal.reflect, Class.forName/getMethod
       http          HTTP request construction: SdkHttpFullRequest/Response, headers, URI, path/query binding
                     (httpbinding, legacy Header/Path/QueryParam marshallers), content length
       codec_write   JSON/CBOR/XML/query writers (Jackson generators, codecs *Serializer/*Writer, XmlGenerator,
                     QueryFormSerializer)
       codec_read    parsers and tokenizers (Jackson parsers, StAX/Xerces, codecs *Deserializer, XmlDomParser)
       traversal     member traversal and dispatch: SdkField + marshalling registry and the legacy
                     protocols.*.internal.(un)marshall classes vs schemas, generated *SharedSerde, serializeMembers,
                     deserializeMember, InnerDeserializer, XmlMemberLookup
       pojo          POJO building: generated model classes and builders, auto-construct lists/maps, copiers
       vm            GC, allocation and VM frames (C++ frames, zeroing)
  3. Weak rules, leaf to root, only when no frame matched a work rule:
       protocol      protocol entry points not covered above (protocols.client.*, protocols.core.*)
       harness       the benchmark class's own frames and SampleTime's timer (System.nanoTime)
  A sample no rule matches is vm ("other"); a sample with no frame above the stub is harness.
  The mapping is by package and class-name role, so it is approximate where one architecture does in one place
  what the other splits: e.g. the feature arm's header/path binding traversal runs inside httpbinding (http),
  and list building inside generated *SharedSerde lands in traversal.

Per combination (class, param) and per class (mean over its params), side by side per arm: CPU share per
category, estimated ns/op per category (share x the arm's JMH score), alloc B/op per category (alloc share x the
arm's gc.alloc.rate.norm), the top 15 self and inclusive frames, and the one-sided list: frames and categories at
>= 1% in one arm and < 0.1% in the other. Categories whose estimated ns/op (or B/op) differ more than 2x, with at
least 1% share on one side, are flagged too. Allocation cross-check: the alloc profile's bytes per second (stacks in
the benchmark method) over the JVM's gc.alloc.rate of the same run should be roughly 1 (flagged outside 0.67-1.5).
Every flag must be explained in the comparability matrix (expected architectural difference, extra overhead, or
missing work).
"""
import csv
import json
import os
import re
import statistics
import sys
from collections import Counter, OrderedDict

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import hot_path_frames as hpf  # noqa: E402
import serde_ab_summary as sab  # noqa: E402

ONE_SIDED_HIGH = 1.0     # percent
ONE_SIDED_LOW = 0.1      # percent
RATIO_FLAG = 2.0
RATIO_MIN_SHARE = 1.0    # percent: a >2x difference below this share on both sides is noise
ALLOC_CHECK = (0.67, 1.5)
TOP = 15
MB = 1024.0 * 1024.0     # JMH's gc.alloc.rate unit

CATEGORIES = OrderedDict([
    ("harness", "JMH harness / benchmark method / timer"),
    ("input", "input or response construction in the timed method"),
    ("construction", "per-call marshaller / protocol construction"),
    ("metadata", "response metadata (request id, SdkHttpResponse)"),
    ("http", "HTTP request construction (URI, headers, path/query binding)"),
    ("traversal", "member traversal / dispatch (SdkField registry vs schema serde)"),
    ("codec_write", "codec write"),
    ("codec_read", "codec read / tokenize"),
    ("format", "string/number/timestamp/base64 formatting and parsing"),
    ("pojo", "POJO building (builders, setters, collections)"),
    ("reflection", "reflection"),
    ("bytes", "byte buffers / array copies"),
    ("protocol", "protocol entry points (other)"),
    ("vm", "GC / VM / allocation / other"),
])

BENCH_FRAME = re.compile(r"^software\.amazon\.awssdk\.benchmark\.")
_S = r"software\.amazon\.awssdk\."

# Scope rules: (category, frame regex, parent regex or None). Checked root to leaf.
SCOPE_RULES = [
    ("harness", re.compile(r"^org\.openjdk\.jmh\."), None),
    ("input", re.compile(r"^" + _S + r"http\.(SdkHttpFullResponse|DefaultSdkHttpFullResponse|AbortableInputStream)"
                         r"|^java\.io\.ByteArrayInputStream\.<init>$"), BENCH_FRAME),
    ("construction", re.compile(
        r"(ProtocolMarshaller|ProtocolUnmarshaller|JsonProtocolMarshallerBuilder|ProtocolFactory|ProtocolMetadata)"
        r"(\$Builder|\$BuilderImpl)?\.(<init>|<clinit>|build|builder|create|createProtocolMarshaller)$"
        r"|" + _S + r"core\.client\.config\.SdkClientConfiguration"
        r"|" + _S + r"protocols\.xml\.internal\.marshall\.XmlGenerator\.create$"), None),
    ("metadata", re.compile(r"\.readRequestId$|ResponseMetadata|\.parseMetadata$|\.responseMetadata$"
                            r"|SdkResponse\$Builder\.sdkHttpResponse$|\.sdkHttpResponse$"), None),
]

# Work rules: (category, frame regex). Checked leaf to root; the first frame matching any rule decides.
WORK_RULES = [
    ("format", re.compile(r"^java\.time\.|" + _S + r"utils\.DateUtils\.|StringToInstant|InstantToString"
                          r"|StringToValueConverter|ValueToStringConverter|TimestampFormat|Base64|BinaryUtils\.(to|from)"
                          r"|NumberCodec|codecs\.commons\.internal\.(Timestamp|Number|Base64)|^jdk\.internal\.math\."
                          r"|^java\.lang\.(Double|Float|Long|Integer|Short|Byte|Boolean)\.(parse|toString|valueOf)"
                          r"|^java\.math\.Big")),
    ("bytes", re.compile(r"arraycopy|copy_longs|_copy_|^jbyte_|^jlong_|^jint_|disjoint|^java\.util\.Arrays\.copyOf"
                         r"|^java\.nio\.|^java\.io\.(ByteArray(Input|Output)Stream|FilterInputStream"
                         r"|BufferedInputStream)\.|" + _S + r"utils\.IoUtils\.|ResponseBodies|LookaheadInputStream"
                         r"|" + _S + r"core\.SdkBytes|" + _S + r"core\.BytesWrapper|BinaryUtils\.copy"
                         r"|ByteBufferOutputStream|SharedArrayContentStreamProvider")),
    ("reflection", re.compile(r"^java\.lang\.reflect\.|^jdk\.internal\.reflect\."
                              r"|^java\.lang\.Class\.(forName|getMethod|getDeclared)")),
    ("http", re.compile(r"^" + _S + r"httpbinding\.|^" + _S + r"http\.|^" + _S + r"internal\.http\."
                        r"|^" + _S + r"utils\.http\.|^java\.net\.URI|SdkUri"
                        r"|\.(Header|Path|QueryParam|StatusCode)(Un)?[Mm]arshaller\b"
                        r"|ProtocolUtils\.(createSdkHttpRequest|addStaticQueryParameters)|UrlEncoding")),
    ("codec_write", re.compile(r"jackson\..*(Generator|JsonFactory\.create|CBORFactory\.create)"
                               r"|codecs\.[a-z]+\.internal\.\w*(Serializer|Writer|Generator|Encoder)"
                               r"|" + _S + r"protocols\.json\.(SdkJsonGenerator|StructuredJsonGenerator)"
                               r"|Sdk\w*Cbor\w*Generator|" + _S + r"protocols\.xml\.internal\.marshall\.Xml(Generator|Writer)"
                               r"|QueryFormSerializer|StructuredJsonFactory\.createWriter|\.createWriter$"
                               r"|codecs\..*\.(create|new)Serializer$")),
    ("codec_read", re.compile(r"jackson\..*(Parser|Bootstrapper|\.sym\.|\.io\.|JsonFactory\._?create)"
                              r"|^com\.sun\.org\.apache\.xerces\.|^com\.sun\.xml\.internal\.stream\.|^javax\.xml\."
                              r"|^jdk\.xml\.|codecs\.[a-z]+\.internal\.\w*(Deserializer|Parser|Reader|Decoder|Lexer)"
                              r"|XmlDomParser|" + _S + r"protocols\.query\.unmarshall\.XmlElement|JsonUnmarshallingParser"
                              r"|XmlResponseParserUtils|JsonNodeParser|" + _S + r"protocols\.jsoncore\."
                              r"|codecs\..*\.(create|new)Deserializer$")),
    ("traversal", re.compile(r"^" + _S + r"core\.SdkField|^" + _S + r"core\.protocol\.|MarshallingType"
                             r"|AbstractMarshallingRegistry|MarshallerRegistry|UnmarshallerRegistry"
                             r"|^" + _S + r"protocols\.(json|xml|query|cbor|rpcv2)\.internal\."
                             r"|^" + _S + r"schema\.|^" + _S + r"core\.schema\.|SharedSerde|\$InnerDeserializer"
                             r"|XmlMemberLookup|codecs\.[a-z]+\.internal\.\w*(MemberLookup|Members)"
                             r"|" + _S + r"services\.[^.]+\.model\.[^.$]+(\$BuilderImpl)?\."
                             r"(serializeMembers|serialize|deserializeMember|deserialize|sdkFields|SDK_FIELDS)$")),
    ("pojo", re.compile(r"^" + _S + r"services\.[^.]+\.model\.|^" + _S + r"core\.util\.(DefaultSdkAuto|SdkAuto)"
                        r"|^" + _S + r"core\.(SdkPojo|SdkResponse|SdkRequest)|^" + _S + r"awscore\.Aws(Request|Response)"
                        r"|^java\.util\.Collections\.unmodifiable|^" + _S + r"utils\.CollectionUtils\.")),
    ("vm", re.compile(r"::|^zero_blocks|_new_array|^G1|^Thread")),
]

# Weak work rules: catch-alls, used only when no frame of the sample matches a work rule above.
WEAK_RULES = [
    ("protocol", re.compile(r"^" + _S + r"protocols\.")),
    ("harness", re.compile(r"^" + _S + r"benchmark\.|^java\.lang\.System\.nanoTime|mach_absolute_time|clock_gettime"
                           r"|javaTimeNanos|^org\.openjdk\.jmh\.")),
]

LAMBDA = re.compile(r"\$\$Lambda.*$")


def normalize(frame):
    return LAMBDA.sub("$$Lambda", hpf.normalize(frame))


def hot_stack(frames, alloc):
    """Frames above the JMH stub (normalized), without the allocated type for allocation samples; None if the
    sample is not in the benchmark method."""
    hot = hpf.hot_frames(frames)
    if hot is None:
        return None
    hot = [normalize(f) for f in hot]
    if alloc and hot:
        hot = hot[:-1]
    return hot


def categorize(hot):
    """The category of one sample (frames above the stub, root first)."""
    if not hot:
        return "harness"
    for i, frame in enumerate(hot):
        parent = hot[i - 1] if i else ""
        for category, pattern, parent_pattern in SCOPE_RULES:
            if pattern.search(frame) and (parent_pattern is None or parent_pattern.search(parent)):
                return category
    for rules in (WORK_RULES, WEAK_RULES):
        for frame in reversed(hot):
            for category, pattern in rules:
                if pattern.search(frame):
                    return category
    return "vm"


def profile_counts(lines, alloc=False):
    """{"total", "categories": Counter, "self": Counter, "inclusive": Counter} over collapsed lines."""
    out = {"total": 0, "categories": Counter(), "self": Counter(), "inclusive": Counter()}
    for line in lines:
        parsed = hpf.parse_line(line)
        if parsed is None:
            continue
        frames, n = parsed
        hot = hot_stack(frames, alloc)
        if hot is None:
            continue
        out["total"] += n
        out["categories"][categorize(hot)] += n
        out["self"][hot[-1] if hot else "(JMH stub)"] += n
        for f in set(hot):
            out["inclusive"][f] += n
    return out


def share(counts, key, kind="categories"):
    return 100.0 * counts[kind][key] / counts["total"] if counts and counts["total"] else 0.0


def one_sided(a, b, kind):
    """Keys at >= 1% in exactly one of two count sets and < 0.1% in the other: [(key, share a, share b)]."""
    out = []
    for key in set(a[kind]) | set(b[kind]):
        sa, sb = share(a, key, kind), share(b, key, kind)
        if (sa >= ONE_SIDED_HIGH and sb < ONE_SIDED_LOW) or (sb >= ONE_SIDED_HIGH and sa < ONE_SIDED_LOW):
            out.append((key, sa, sb))
    return sorted(out, key=lambda x: -max(x[1], x[2]))


def ratio_flags(shares_a, shares_b, scale_a, scale_b):
    """Categories whose estimated value (share x scale) differs more than 2x, with >= 1% share on one side."""
    out = []
    if not scale_a or not scale_b:
        return out
    for category in CATEGORIES:
        sa, sb = shares_a.get(category, 0.0), shares_b.get(category, 0.0)
        if max(sa, sb) < RATIO_MIN_SHARE:
            continue
        va, vb = sa * scale_a / 100.0, sb * scale_b / 100.0
        low, high = min(va, vb), max(va, vb)
        if low == 0 or high / low > RATIO_FLAG:
            out.append((category, va, vb))
    return out


# ==================== inputs ====================

def _read_tsv(path):
    with open(path, newline="") as f:
        return list(csv.DictReader(f, delimiter="\t"))


def _collapsed(directory):
    files = []
    for dirpath, _, names in os.walk(directory):
        files += [os.path.join(dirpath, n) for n in names if n.startswith("collapsed-") and n.endswith(".csv")]
    return sorted(files)


def _jmh(directory):
    """(benchmark, score, alloc norm, alloc rate B/s, measured seconds) from <dir>/jmh.json, or None."""
    path = os.path.join(directory, "jmh.json")
    if not os.path.isfile(path) or os.path.getsize(path) == 0:
        return None
    with open(path) as f:
        rows = json.load(f)
    if not rows:
        return None
    row = rows[0]
    secondary = {k.lstrip("\u00b7"): v for k, v in (row.get("secondaryMetrics") or {}).items()}
    seconds = None
    m = re.match(r"([\d.]+)\s*(s|ms)", str(row.get("measurementTime", "")))
    if m:
        unit = 1.0 if m.group(2) == "s" else 0.001
        seconds = float(m.group(1)) * unit * int(row.get("measurementIterations", 1)) * int(row.get("forks", 1))
    rate = secondary.get("gc.alloc.rate", {}).get("score")
    return {"benchmark": row["benchmark"], "score": row["primaryMetric"]["score"],
            "alloc_norm": secondary.get("gc.alloc.rate.norm", {}).get("score"),
            "alloc_rate": None if rate is None else rate * MB, "seconds": seconds}


def load_profiles(rundir):
    """{(class, param): {arm: {event: {"counts", "jmh", "dir", "status"}}}} from profile-runs.tsv."""
    out = OrderedDict()
    for row in _read_tsv(os.path.join(rundir, "profile-runs.tsv")):
        directory = os.path.join(rundir, row["dir"])
        alloc = row["event"] == "alloc"
        counts = None
        files = _collapsed(directory) if os.path.isdir(directory) else []
        if row["status"] == "ok" and files:
            counts = {"total": 0, "categories": Counter(), "self": Counter(), "inclusive": Counter()}
            for path in files:
                with open(path) as f:
                    c = profile_counts(f, alloc)
                for k in ("categories", "self", "inclusive"):
                    counts[k].update(c[k])
                counts["total"] += c["total"]
        entry = out.setdefault((row["class"], row["param"]), OrderedDict()).setdefault(row["arm"], {})
        entry[row["event"]] = {"counts": counts, "jmh": _jmh(directory), "dir": row["dir"], "status": row["status"]}
    return out


def timing_means(timing_doc):
    """{(benchmark, param, arm): {"score": mean, "alloc": mean}} from a serde-ab.json."""
    out = {}
    if not timing_doc:
        return out
    for key in sab.keys(timing_doc):
        param = key[1].split("=", 1)[-1]
        for arm in sab.labels(timing_doc):
            s = sab.stats(sab.samples(timing_doc, arm, key, "score"))
            a = sab.stats(sab.samples(timing_doc, arm, key, "alloc"))
            out[(key[0], param, arm)] = {"score": s and s["mean"], "alloc": a and a["mean"]}
    return out


# ==================== analysis ====================

def analyze_combo(per_arm, arms, timing, param):
    """Per arm: cpu/alloc shares, scales (ns/op, B/op), their sources and the alloc cross-check."""
    out = OrderedDict()
    for arm in arms:
        events = per_arm.get(arm, {})
        cpu, alloc = events.get("cpu", {}), events.get("alloc", {})
        cpu_counts, alloc_counts = cpu.get("counts"), alloc.get("counts")
        benchmark = ((cpu.get("jmh") or alloc.get("jmh")) or {}).get("benchmark")
        t = timing.get((benchmark, param, arm), {})
        score = t.get("score") or (cpu.get("jmh") or {}).get("score")
        norm = t.get("alloc") or (alloc.get("jmh") or {}).get("alloc_norm")
        check = None
        aj = alloc.get("jmh") or {}
        if alloc_counts and aj.get("alloc_rate") and aj.get("seconds"):
            check = alloc_counts["total"] / aj["seconds"] / aj["alloc_rate"]
        cpu_shares = {c: share(cpu_counts, c) for c in CATEGORIES} if cpu_counts else {}
        alloc_shares = {c: share(alloc_counts, c) for c in CATEGORIES} if alloc_counts else {}
        out[arm] = {
            "cpu": cpu_counts, "alloc": alloc_counts, "cpu_shares": cpu_shares, "alloc_shares": alloc_shares,
            "ns": {c: v * score / 100.0 for c, v in cpu_shares.items()} if score is not None else {},
            "bytes": {c: v * norm / 100.0 for c, v in alloc_shares.items()} if norm is not None else {},
            "score": score, "score_source": "timing" if t.get("score") else "profile run",
            "norm": norm, "norm_source": "timing" if t.get("alloc") else "profile run",
            "alloc_check": check,
            "missing": [e for e in ("cpu", "alloc") if not events.get(e, {}).get("counts")],
        }
    return out


def combo_flags(analysis, arms):
    """Net-work flags of one combination, comparing every arm with the first."""
    flags = []
    base = arms[0]
    for arm in arms:
        a = analysis[arm]
        if a["missing"]:
            flags.append(f"`{arm}`: no {'/'.join(a['missing'])} profile")
        if a["alloc_check"] is not None and not ALLOC_CHECK[0] <= a["alloc_check"] <= ALLOC_CHECK[1]:
            flags.append(f"`{arm}`: alloc profile bytes/s = {a['alloc_check']:.2f} x gc.alloc.rate "
                         "(the profile may miss allocations)")
    for arm in arms[1:]:
        a, b = analysis[base], analysis[arm]
        for kind, label in (("cpu", "CPU"), ("alloc", "alloc")):
            if a[kind] and b[kind]:
                for cat, sa, sb in one_sided(a[kind], b[kind], "categories"):
                    flags.append(f"one-sided {label} category {cat}: `{base}` {sa:.1f}%, `{arm}` {sb:.1f}%")
        for cat, va, vb in ratio_flags(a["cpu_shares"], b["cpu_shares"], a["score"], b["score"]):
            flags.append(f">2x ns/op in {cat}: `{base}` {va:,.0f}, `{arm}` {vb:,.0f}")
        for cat, va, vb in ratio_flags(a["alloc_shares"], b["alloc_shares"], a["norm"], b["norm"]):
            flags.append(f">2x B/op in {cat}: `{base}` {va:,.0f}, `{arm}` {vb:,.0f}")
    return flags


# ==================== Markdown ====================

def _f(value, digits=1):
    return "-" if value is None else f"{value:,.{digits}f}"


def category_table(summaries, arms):
    """summaries: {arm: {"cpu_shares", "alloc_shares", "ns", "bytes" (per category), "score", "norm"}}."""
    head = "| category |" + "".join(f" {a} CPU % | {a} ns/op | {a} alloc % | {a} B/op |" for a in arms)
    lines = [head, "|---|" + "---:|---:|---:|---:|" * len(arms)]
    for cat, label in CATEGORIES.items():
        cells = []
        nonzero = False
        for arm in arms:
            a = summaries[arm]
            cs, al = a["cpu_shares"].get(cat), a["alloc_shares"].get(cat)
            nonzero |= bool(cs) or bool(al)
            cells += [_f(cs), _f(a["ns"].get(cat)), _f(al), _f(a["bytes"].get(cat), 0)]
        if nonzero:
            lines.append(f"| {cat} ({label}) | " + " | ".join(cells) + " |")
    totals = []
    for arm in arms:
        a = summaries[arm]
        totals += ["100.0" if a["cpu_shares"] else "-", _f(a["score"]),
                   "100.0" if a["alloc_shares"] else "-", _f(a["norm"], 0)]
    lines.append("| **total** | " + " | ".join(totals) + " |")
    return lines


def class_summary(analyses, arm):
    """Mean over a class's params of every share, ns/op and B/op."""
    def mean(values):
        values = [v for v in values if v is not None]
        return statistics.fmean(values) if values else None

    out = {}
    for key in ("cpu_shares", "alloc_shares", "ns", "bytes"):
        cats = [c for c in CATEGORIES if any(c in x[arm][key] for x in analyses)]
        out[key] = {c: mean([x[arm][key].get(c) for x in analyses]) for c in cats}
    out["score"] = mean([x[arm]["score"] for x in analyses])
    out["norm"] = mean([x[arm]["norm"] for x in analyses])
    return out


def frames_table(analysis, arms, kind, title):
    lines = [f"Top {TOP} {title} frames (CPU):", "", "| rank |" + "".join(f" {a} | % |" for a in arms),
             "|---:|" + "---|---:|" * len(arms)]
    tops = {a: (analysis[a]["cpu"][kind].most_common(TOP) if analysis[a]["cpu"] else []) for a in arms}
    for i in range(TOP):
        cells = []
        for a in arms:
            if i < len(tops[a]):
                frame, n = tops[a][i]
                cells += [f"`{frame}`", f"{100.0 * n / analysis[a]['cpu']['total']:.1f}"]
            else:
                cells += ["", ""]
        if any(cells):
            lines.append(f"| {i + 1} | " + " | ".join(cells) + " |")
    return lines


def one_sided_frames(analysis, arms):
    lines = []
    base = arms[0]
    for arm in arms[1:]:
        a, b = analysis[base]["cpu"], analysis[arm]["cpu"]
        if not a or not b:
            continue
        found = one_sided(a, b, "inclusive")
        lines += [f"One-sided inclusive frames (CPU, >= {ONE_SIDED_HIGH:g}% vs < {ONE_SIDED_LOW:g}%), "
                  f"`{base}` vs `{arm}`: {len(found)}", ""]
        for side, label in ((1, base), (2, arm)):
            rows = [x for x in found if x[side] >= ONE_SIDED_HIGH][:TOP]
            if rows:
                lines.append(f"- only in `{label}`: " + "; ".join(f"`{x[0]}` {x[side]:.1f}%" for x in rows))
        lines.append("")
    return lines


def render(rundir, profiles, arms, timing_doc):
    timing = timing_means(timing_doc)
    params = {}
    if os.path.isfile(os.path.join(rundir, "params.tsv")):
        params = {r["key"]: r["value"] for r in _read_tsv(os.path.join(rundir, "params.tsv"))}
    lines = [f"# Serde A/B profile comparison `{params.get('run_id', os.path.basename(rundir))}`", "",
             f"Arms: {', '.join(f'`{a}`' for a in arms)} (baseline `{arms[0]}`). Profile: {params.get('profile', '?')}. "
             f"JVM args `{params.get('jvm_args', '')}` + `-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints`. "
             f"Host: {params.get('host', '?')}; {params.get('java', '?')}.", "",
             "ns/op and B/op per category = share x the arm's JMH score / gc.alloc.rate.norm from "
             + ("the timing run `" + timing_doc["params"].get("run_id", "?") + "`" if timing_doc else
                "the profiling runs (perturbed by the profiler; pass --timing for timing-run numbers)")
             + ". Category rules and flags: `scripts/schema-serde/serde_ab_profile.py` (docstring). Every flag "
             "must be explained in the comparability matrix.", ""]
    combos = OrderedDict()
    for (cls, param), per_arm in profiles.items():
        analysis = analyze_combo(per_arm, arms, timing, param)
        combos[(cls, param)] = (analysis, combo_flags(analysis, arms))

    lines += ["## Flags", ""]
    any_flag = False
    for (cls, param), (_, flags) in combos.items():
        for flag in flags:
            lines.append(f"- {cls} [{param}]: {flag}")
            any_flag = True
    if not any_flag:
        lines.append("None.")
    lines.append("")

    classes = OrderedDict()
    for (cls, param), (analysis, _) in combos.items():
        classes.setdefault(cls, []).append(analysis)
    lines += ["## Per class (mean over params)", ""]
    for cls, analyses in classes.items():
        summaries = {arm: class_summary(analyses, arm) for arm in arms}
        lines += [f"### {cls} ({len(analyses)} params)", ""] + category_table(summaries, arms) + [""]

    lines += ["## Per combination", ""]
    for (cls, param), (analysis, flags) in combos.items():
        lines += [f"### {cls} [{param}]", ""]
        for arm in arms:
            a = analysis[arm]
            lines.append(f"- `{arm}`: score {_f(a['score'])} ns/op ({a['score_source']}), "
                         f"{_f(a['norm'], 0)} B/op ({a['norm_source']}), CPU samples "
                         f"{a['cpu']['total'] if a['cpu'] else '-'}, alloc bytes "
                         f"{a['alloc']['total'] if a['alloc'] else '-'}, alloc check "
                         f"{_f(a['alloc_check'], 2)}")
        lines += [""] + category_table(analysis, arms) + [""]
        lines += frames_table(analysis, arms, "self", "self") + [""]
        lines += frames_table(analysis, arms, "inclusive", "inclusive") + [""]
        lines += one_sided_frames(analysis, arms)
        lines += ["Flags: " + ("; ".join(flags) if flags else "none."), ""]
    return "\n".join(lines) + "\n"


def compare_main(argv):
    if not argv:
        raise SystemExit(__doc__)
    rundir, rest = argv[0], argv[1:]
    opts = {"--timing": None, "--out": None}
    i = 0
    while i < len(rest):
        if rest[i] in opts and i + 1 < len(rest):
            opts[rest[i]] = rest[i + 1]
            i += 2
        else:
            raise SystemExit(f"unknown option {rest[i]}\n{__doc__}")
    arms = [r["label"] for r in _read_tsv(os.path.join(rundir, "arms.tsv"))]
    profiles = load_profiles(rundir)
    timing_doc = sab.load(opts["--timing"]) if opts["--timing"] else None
    out = opts["--out"] or os.path.join(rundir, "profile-comparison.md")
    with open(out, "w") as f:
        f.write(render(rundir, profiles, arms, timing_doc))
    print(f"Wrote {out}", file=sys.stderr)
    return 0


def main(argv):
    if argv[:1] == ["--self-test"]:
        return self_test()
    if argv[:1] == ["compare"]:
        return compare_main(argv[1:])
    raise SystemExit(__doc__)


# ==================== self-test ====================

def self_test():
    import tempfile
    import unittest

    stub = "java/lang/Thread.run;org/openjdk/jmh/X.run;b/jmh_generated/B_m_jmhTest.m_SampleTime_jmhStub"
    bench = "software/amazon/awssdk/benchmark/serde/RestJsonMarshallBenchmark.marshall"

    def cat(*frames, alloc=False):
        return categorize(hot_stack(parse(stub + ";" + ";".join(frames) + " 1")[0], alloc))

    def parse(line):
        return hpf.parse_line(line)

    class SerdeAbProfileTest(unittest.TestCase):
        def test_categorize_whenLegacyAndSchemaWrite_mapBothToCodecWrite(self):
            self.assertEqual(cat(bench, "software/amazon/awssdk/protocols/json/internal/marshall/JsonProtocolMarshaller"
                                        ".marshall", "software/amazon/awssdk/protocols/json/SdkJsonGenerator.writeValue",
                                 "software/amazon/awssdk/thirdparty/jackson/core/json/UTF8JsonGenerator.writeString_[j]"),
                             "codec_write")
            self.assertEqual(cat(bench, "software/amazon/awssdk/protocols/client/restjson/RestJsonClientProtocol"
                                        ".serializeRequest",
                                 "software/amazon/awssdk/codecs/json/internal/SmithyJsonSerializer.writeString"),
                             "codec_write")

        def test_categorize_whenInnermostFrameDecides_attributesLeafWork(self):
            # Unmatched leaf (HashMap) under the registry: traversal; String.<init> under a parser: codec read.
            self.assertEqual(cat(bench, "software/amazon/awssdk/protocols/json/internal/marshall/JsonProtocolMarshaller"
                                        ".doMarshall", "software/amazon/awssdk/protocols/core/"
                                        "AbstractMarshallingRegistry.get", "java/util/HashMap.getNode"), "traversal")
            self.assertEqual(cat(bench, "software/amazon/awssdk/codecs/xml/internal/SmithyXmlDeserializer.readString",
                                 "java/lang/String.<init>"), "codec_read")
            self.assertEqual(cat(bench, "software/amazon/awssdk/codecs/xml/internal/SmithyXmlDeserializer.readTimestamp",
                                 "software/amazon/awssdk/codecs/commons/internal/NumberCodec.parseDouble"), "format")
            self.assertEqual(cat(bench, "software/amazon/awssdk/codecs/json/internal/X.y", "jbyte_disjoint_arraycopy"),
                             "bytes")
            self.assertEqual(cat(bench, "software/amazon/awssdk/services/s/model/PutItemRequest$BuilderImpl.item",
                                 "java/util/HashMap.put"), "pojo")
            self.assertEqual(cat(bench, "software/amazon/awssdk/services/s/model/PutItemRequest.serializeMembers"),
                             "traversal")
            self.assertEqual(cat(bench, "software/amazon/awssdk/httpbinding/internal/HttpBindingSerializer.writeHeader",
                                 "software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader",
                                 "java/util/TreeMap.put"), "http")
            self.assertEqual(cat(bench, "itable stub"), "harness")
            # A protocols.* leaf under a codec frame: the codec decides (protocol is a weak catch-all).
            self.assertEqual(cat(bench, "software/amazon/awssdk/protocols/client/awsquery/internal/QueryFormSerializer"
                                        "$MemberSerializer.memberName", "software/amazon/awssdk/protocols/client/"
                                        "awsquery/internal/AwsQuerySchemaExtensions$QueryMemberBinding.name"),
                             "codec_write")
            self.assertEqual(cat(bench, "software/amazon/awssdk/protocols/client/http/HttpClientProtocol.serialize"),
                             "protocol")
            self.assertEqual(cat("unknown/Frame.x"), "vm")
            self.assertEqual(categorize([]), "harness")

        def test_categorize_whenScopeFrameOnStack_outermostScopeWins(self):
            # The legacy marshaller built in the timed method: everything below build() is construction.
            self.assertEqual(cat(bench, "software/amazon/awssdk/protocols/json/internal/marshall/"
                                        "JsonProtocolMarshallerBuilder.build",
                                 "software/amazon/awssdk/protocols/json/internal/marshall/JsonProtocolMarshaller.<init>",
                                 "java/util/HashMap.put"), "construction")
            # Response built directly by the benchmark: input; the same builder inside the protocol: http.
            unm = "software/amazon/awssdk/benchmark/serde/RestJsonUnmarshallBenchmark.unmarshallOnce"
            self.assertEqual(cat(unm, "software/amazon/awssdk/http/DefaultSdkHttpFullResponse$Builder.putHeader",
                                 "java/util/TreeMap.put"), "input")
            self.assertEqual(cat(unm, "software/amazon/awssdk/protocols/client/http/HttpClientProtocol.deserialize",
                                 "software/amazon/awssdk/http/DefaultSdkHttpFullResponse$Builder.putHeader"), "http")
            self.assertEqual(cat(unm, "software/amazon/awssdk/protocols/client/awsquery/QueryClientProtocol"
                                      ".readRequestId",
                                 "software/amazon/awssdk/codecs/xml/internal/SmithyXmlDeserializer.skipElement"),
                             "metadata")
            self.assertEqual(cat(bench, "org/openjdk/jmh/infra/Blackhole.consume"), "harness")

        def test_hotStack_whenAllocSample_dropsAllocatedType(self):
            frames = parse(stub + ";" + bench + ";software/amazon/awssdk/codecs/json/internal/SmithyJsonSerializer"
                                                ".writeString;byte[]_[k] 64")[0]
            self.assertEqual(hot_stack(frames, True)[-1],
                             "software.amazon.awssdk.codecs.json.internal.SmithyJsonSerializer.writeString")
            self.assertEqual(categorize(hot_stack(frames, True)), "codec_write")
            self.assertEqual(normalize("a/B$$Lambda.0x0000.apply_[j]"), "a.B$$Lambda")

        def test_oneSided_whenShareAboveOneAndBelowPointOne_isFlagged(self):
            a = profile_counts([stub + ";" + bench + ";x/Legacy.m 990\n", stub + ";" + bench + ";x/Both.m 10\n"])
            b = profile_counts([stub + ";" + bench + ";x/Schema.m 995\n", stub + ";" + bench + ";x/Both.m 5\n",
                                stub + ";" + bench + ";x/Legacy.m 0\n"])
            found = {k for k, _, _ in one_sided(a, b, "inclusive")}
            self.assertIn("x.Legacy.m", found)
            self.assertIn("x.Schema.m", found)
            self.assertNotIn("x.Both.m", found)       # 1.0% vs 0.5%: present on both sides
            self.assertNotIn(bench.replace("/", "."), found)

        def test_ratioFlags_whenMoreThanTwiceAndShareAboveOnePercent_flags(self):
            flags = ratio_flags({"codec_write": 50.0, "http": 0.5, "format": 10.0},
                                {"codec_write": 20.0, "http": 0.1, "format": 9.0}, 1000.0, 1000.0)
            self.assertEqual([f[0] for f in flags], ["codec_write"])  # 500 vs 200 ns; http below 1%; format 1.1x
            # Category present on one side only (with >= 1% share) is also flagged.
            self.assertEqual([f[0] for f in ratio_flags({"construction": 5.0}, {}, 100.0, 100.0)], ["construction"])
            self.assertEqual(ratio_flags({"x": 50.0}, {"x": 50.0}, None, 100.0), [])

        def test_compare_whenRunDirComplete_writesTablesFramesAndOneSidedList(self):
            def jmh_json(score, norm, rate_mb):
                return [{"benchmark": "software.amazon.awssdk.benchmark.serde.RestJsonMarshallBenchmark.marshall",
                         "measurementIterations": 2, "measurementTime": "1 s", "forks": 1,
                         "primaryMetric": {"score": score},
                         "secondaryMetrics": {"\u00b7gc.alloc.rate.norm": {"score": norm},
                                              "\u00b7gc.alloc.rate": {"score": rate_mb}}}]
            legacy = [stub + ";" + bench + ";software/amazon/awssdk/protocols/json/internal/marshall/"
                                           "JsonProtocolMarshallerBuilder.build 300\n",
                      stub + ";" + bench + ";software/amazon/awssdk/protocols/json/SdkJsonGenerator.writeValue 700\n"]
            schema = [stub + ";" + bench + ";software/amazon/awssdk/codecs/json/internal/SmithyJsonSerializer"
                                           ".writeString 1000\n"]
            alloc = [stub + ";" + bench + ";software/amazon/awssdk/codecs/json/internal/SmithyJsonSerializer.w;"
                                          "byte[]_[i] " + str(2 * 1024 * 1024 * 100) + "\n"]
            with tempfile.TemporaryDirectory() as d:
                with open(os.path.join(d, "arms.tsv"), "w") as f:
                    f.write("label\tref\nbaseline\tfc8b\nfeature\tHEAD\n")
                with open(os.path.join(d, "params.tsv"), "w") as f:
                    f.write("key\tvalue\nrun_id\tfixture\n")
                rows = ["arm\tclass\tparam\tevent\tstatus\tdir"]
                for arm, cpu_lines in (("baseline", legacy), ("feature", schema)):
                    for event, lines in (("cpu", cpu_lines), ("alloc", alloc)):
                        rel = f"profile/{arm}/RestJsonMarshallBenchmark/restJson1_PutObject_S/{event}"
                        os.makedirs(os.path.join(d, rel, "sub"))
                        with open(os.path.join(d, rel, "sub", f"collapsed-{event}.csv"), "w") as f:
                            f.writelines(lines)
                        with open(os.path.join(d, rel, "jmh.json"), "w") as f:
                            json.dump(jmh_json(1000.0 if arm == "baseline" else 400.0, 640.0, 100.0), f)
                        rows.append(f"{arm}\tRestJsonMarshallBenchmark\trestJson1_PutObject_S\t{event}\tok\t{rel}")
                with open(os.path.join(d, "profile-runs.tsv"), "w") as f:
                    f.write("\n".join(rows) + "\n")
                self.assertEqual(compare_main([d]), 0)
                with open(os.path.join(d, "profile-comparison.md")) as f:
                    text = f.read()
            self.assertIn("## Per class (mean over params)", text)
            self.assertIn("### RestJsonMarshallBenchmark [restJson1_PutObject_S]", text)
            self.assertIn("| construction (per-call marshaller / protocol construction) | 30.0 | 300.0 |", text)
            self.assertIn("one-sided CPU category construction: `baseline` 30.0%, `feature` 0.0%", text)
            self.assertNotIn(">2x ns/op in codec_write", text)  # 700 vs 400 ns/op is within 2x
            self.assertIn("Top 15 self frames (CPU):", text)
            self.assertIn("- only in `baseline`: `software.amazon.awssdk.protocols.json.SdkJsonGenerator.writeValue` "
                          "70.0%; `software.amazon.awssdk.protocols.json.internal.marshall."
                          "JsonProtocolMarshallerBuilder.build` 30.0%", text)
            self.assertIn("- only in `feature`: `software.amazon.awssdk.codecs.json.internal.SmithyJsonSerializer"
                          ".writeString` 100.0%", text)
            self.assertIn("alloc check 1.00", text)  # 200 MB over 2 s vs 100 MB/s
            self.assertIn("score 1,000.0 ns/op (profile run)", text)

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(SerdeAbProfileTest)
    return 0 if unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
