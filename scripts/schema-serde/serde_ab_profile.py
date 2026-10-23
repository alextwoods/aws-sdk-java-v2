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
  serde_ab_profile.py compare RUNDIR [--timing RUN_JSON] [--out FILE] [--out-dir DIR]
      Writes DIR/profile-comparison.md (or FILE), DIR/boundary.tsv and DIR/profile-path.tsv (see below); DIR
      defaults to RUNDIR and is created if missing (inputs are always read from RUNDIR, so --out-dir recomputes a
      fetched run without touching it). Exits 1 if a runtime code-path check fails. --timing is a serde-ab.json of a
      timing run of the same jars (serde_ab_summary.py collect); its mean ns/op and gc.alloc.rate.norm turn shares
      into ns/op and B/op per category. Without it the profiling runs' own JMH results are used (perturbed by the
      profiler; marked).
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

Work-boundary items (comparability-matrix.md): work inside one arm's timed method that the other arm's client does
outside the serde call (A1-objects, A2, A5), plus A1-request, work that both arms do but that the baseline does
inside its legacy marshaller construction, kept separate so that it is not removed with A1-objects. Each sample
belongs to at most one item, checked in this order (frames above the stub, the allocated type already dropped, so
the allocation site decides):
  A1-objects  per-call legacy marshaller objects (baseline marshall): a sample of category construction that no
              A1-request rule matches. The marshaller, its builder and context (JsonProtocolMarshallerBuilder,
              JsonProtocolMarshaller, JsonMarshallerContext, NullAsEmptyBodyProtocolRequestMarshaller,
              Query/XmlProtocolMarshaller and their builders, XmlMarshallerContext), OperationInfo checks, the
              endpoint lookup (BaseAwsJsonProtocolFactory.endpoint > SdkClientConfiguration.option) and VM frames
              under them. The feature arm builds the equivalent once per client.
  A1-request  request work the legacy marshaller constructor does that the feature arm also does inside its serde
              call (baseline marshall): a sample of category construction with a frame that matches one of the
              ordered A1_REQUEST_RULES. Rules are tried in order; the first rule whose regex matches ANY frame of the
              sample names the part (a1_part):
                generator  the JSON generator / XML writer the constructor creates:
                           BaseAwsJsonProtocolFactory.createGenerator, *.createWriter, SdkJsonGenerator.<init>,
                           Jackson JsonFactory / BufferRecycler frames, XmlGenerator.create, XmlWriter.<init>,
                           SdkByteArrayOutputStream
                request    the request builder, URI and static headers / query: *ProtocolMarshaller
                           .fillBasicRequestParams, ProtocolUtils.createSdkHttpRequest /
                           addStaticQueryParametersToRequest, (Default)SdkHttpFullRequest frames, LowCopyListMap,
                           SdkHttpUtils (appendUri)
              The generator rule comes first, so a sample under both (none are expected) counts as generator. A
              legacy restJson1 marshaller's generator is never closed on a blob-payload operation (PutObject), so its
              Jackson buffers are allocated per call; that stays in the baseline, as it does in a legacy client.
  A2          feature-only response handling (feature unmarshall): a HttpClientProtocol.isErrorResponse or
              HttpClientProtocol.responseMetadata frame, or a .responseMetadata / .sdkHttpResponse frame whose parent
              frame is HttpClientProtocol.buildOutput or SchemaSerdeSupport.buildResponse (the setters that attach
              the metadata and SdkHttpResponse). The legacy client does this in its response handler, outside the
              timed method. The category metadata is no longer used: it also holds the awsQuery request-id read
              (QueryClientProtocol.readRequestId on the feature, QueryProtocolUnmarshaller.parseMetadata on the
              baseline), which both timed methods do, and the model getters that toBuilder() and the response
              constructors call on both arms (POJO copying, under AwsResponse / SdkResponse(.BuilderImpl).<init>).
              Those stay in both arms.
  A5          Query form percent-encoding: a QueryFormSerializer writeUrlEncoded / writeUrlEncodedAscii /
              writePercentEncoded / percentEncode frame (feature Query marshall). A lower bound: appending the pairs
              to the body buffer stays in, as the baseline's equivalent (putting them into the raw query map) does;
              the Content-Length header put is not separated from the Content-Type put. Key building
              (QueryFormSerializer.writeKey) is not A5: the baseline builds the same keys.
compare writes their CPU and alloc shares per (class, param, arm) to DIR/boundary.tsv and a table to
profile-comparison.md. boundary.tsv format 2 (this version) has the columns benchmark class param arm cpu_samples
alloc_bytes, then <item>_cpu_pct and <item>_alloc_pct for A1-objects, A1-request, A2 and A5; there are no A1_*
columns, so a reader that sums every *_cpu_pct column still gets "every item" without double counting. Format 1
(earlier versions) has A1_*, A2_* (the category-metadata rule) and A5_* columns instead; a file is format 2 when it
has an A1-objects_cpu_pct column. serde_ab_summary.py summarize --boundary removes A1-objects + A2 + A5 for its
comparable estimate (A1-request stays in the baseline) and every item for its serde-only reference.

Runtime code path: when arms.tsv has an expect_path column (serde-ab.sh writes it), compare runs serde_ab_checks.py's
path check over every cpu profile (every class, param and arm), writes RUNDIR/profile-path.tsv and a per-class table
to profile-comparison.md, and exits 1 if any profile is not PASS (a missing or failed cpu profile included).
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
import serde_ab_checks as sac  # noqa: E402
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

# Work-boundary items (comparability-matrix.md A1, A2, A5; A1 split in two). Each sample belongs to at most one
# item, checked in this order. See the module docstring.
BOUNDARY = OrderedDict([
    ("A1-objects", "per-call legacy marshaller objects (marshaller, builder, context, config lookups)"),
    ("A1-request", "request work the legacy marshaller constructor does that the feature does inside its serde call "
                   "(request builder, URI, static headers and query, JSON generator / XML writer)"),
    ("A2", "feature-only response handling (status check, response metadata and SdkHttpResponse attachment)"),
    ("A5", "Query form percent-encoding (QueryFormSerializer.writeUrlEncoded / percent-encode frames)"),
])
# A1-request rules, in order: within category construction, the first rule whose regex matches any frame names the
# part; no match = A1-objects.
A1_REQUEST_RULES = OrderedDict([
    ("generator", re.compile(
        r"^" + _S + r"protocols\.json\.BaseAwsJsonProtocolFactory\.createGenerator$"
        r"|StructuredJsonFactory\w*\.createWriter$|\.createWriter$"
        r"|^" + _S + r"protocols\.json\.SdkJsonGenerator\.<init>$"
        r"|^" + _S + r"thirdparty\.jackson\.core\.(JsonFactory|util\.BufferRecycler)\."
        r"|^" + _S + r"protocols\.xml\.internal\.marshall\.(XmlGenerator\.create|XmlWriter\.<init>)$"
        r"|SdkByteArrayOutputStream")),
    ("request", re.compile(
        r"ProtocolMarshaller\.fillBasicRequestParams$"
        r"|^" + _S + r"protocols\.core\.ProtocolUtils\.(createSdkHttpRequest|addStaticQueryParametersToRequest)$"
        r"|^" + _S + r"http\.(Default)?SdkHttpFullRequest"
        r"|^" + _S + r"internal\.http\.LowCopyListMap"
        r"|^" + _S + r"utils\.http\.SdkHttpUtils\.")),
])
# A2: a status-check / metadata frame of HttpClientProtocol, or a metadata setter called by the feature's output
# builder (BOUNDARY_A2_CHILD directly under BOUNDARY_A2_PARENT).
BOUNDARY_A2_FRAME = re.compile(r"^" + _S + r"protocols\.client\.http\.HttpClientProtocol\."
                               r"(isErrorResponse|responseMetadata)$")
BOUNDARY_A2_PARENT = re.compile(r"^" + _S + r"(protocols\.client\.http\.HttpClientProtocol\.buildOutput"
                                r"|core\.schema\.SchemaSerdeSupport\.buildResponse)$")
BOUNDARY_A2_CHILD = re.compile(r"\.(responseMetadata|sdkHttpResponse)$")
BOUNDARY_A5_FRAME = re.compile(r"^" + _S + r"protocols\.client\.awsquery\.internal\.QueryFormSerializer\."
                               r"(writeUrlEncoded|writeUrlEncodedAscii|writePercentEncoded|percentEncode)$")

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


def a1_part(hot):
    """The A1_REQUEST_RULES part ("generator", "request") of a construction sample, or None (A1-objects)."""
    for part, pattern in A1_REQUEST_RULES.items():
        if any(pattern.search(f) for f in hot):
            return part
    return None


def is_a2(hot):
    """True if the sample is feature-only response handling (A2; see the module docstring)."""
    for i, frame in enumerate(hot):
        if BOUNDARY_A2_FRAME.search(frame):
            return True
        if i and BOUNDARY_A2_CHILD.search(frame) and BOUNDARY_A2_PARENT.search(hot[i - 1]):
            return True
    return False


def boundary_item(hot, category):
    """The work-boundary item (A1-objects, A1-request, A2, A5) of one sample, or None."""
    if category == "construction":
        return "A1-request" if a1_part(hot) else "A1-objects"
    if is_a2(hot):
        return "A2"
    if any(BOUNDARY_A5_FRAME.search(f) for f in hot):
        return "A5"
    return None


COUNT_KINDS = ("categories", "self", "inclusive", "boundary")


def _empty_counts():
    out = {"total": 0}
    out.update({k: Counter() for k in COUNT_KINDS})
    return out


def profile_counts(lines, alloc=False):
    """{"total", "categories", "self", "inclusive", "boundary": Counter} over collapsed lines."""
    out = _empty_counts()
    for line in lines:
        parsed = hpf.parse_line(line)
        if parsed is None:
            continue
        frames, n = parsed
        hot = hot_stack(frames, alloc)
        if hot is None:
            continue
        out["total"] += n
        category = categorize(hot)
        out["categories"][category] += n
        item = boundary_item(hot, category)
        if item:
            out["boundary"][item] += n
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
            counts = _empty_counts()
            for path in files:
                with open(path) as f:
                    c = profile_counts(f, alloc)
                for k in COUNT_KINDS:
                    counts[k].update(c[k])
                counts["total"] += c["total"]
        entry = out.setdefault((row["class"], row["param"]), OrderedDict()).setdefault(row["arm"], {})
        entry[row["event"]] = {"counts": counts, "jmh": _jmh(directory), "dir": row["dir"], "status": row["status"],
                               "files": files}
    return out


def path_checks(profiles, expects):
    """The runtime code-path check of serde_ab_checks.py over every cpu profile: one row per (class, param, arm)
    with an expected path ({arm: legacy|schema}). A missing or failed cpu profile is a FAIL."""
    rows = []
    for (cls, param), per_arm in profiles.items():
        for arm, events in per_arm.items():
            expect = expects.get(arm)
            if expect not in sac.PATHS:
                continue
            cpu = events.get("cpu")
            if cpu is None:
                continue
            c = {"total": 0, "schema": 0, "legacy": 0, "sdk_fields_from_schema": 0}
            for path in cpu["files"] if cpu["status"] == "ok" else []:
                with open(path) as f:
                    for k, v in sac.path_counts(f).items():
                        c[k] += v
            ok, message = sac.path_verdict(expect, c["schema"], c["legacy"], c["total"])
            other = "legacy" if expect == "schema" else "schema"
            rows.append({"arm": arm, "class": cls, "param": param, "expect": expect,
                         "result": "PASS" if ok else "FAIL", "expected_samples": c[expect],
                         "other_samples": c[other], "total": c["total"],
                         "sdk_fields_from_schema": c["sdk_fields_from_schema"], "message": message})
    return rows


def write_tsv(path, rows, columns):
    with open(path, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=columns, delimiter="\t", lineterminator="\n")
        w.writeheader()
        for row in rows:
            w.writerow({c: row.get(c, "") for c in columns})


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
        cpu_boundary = {b: share(cpu_counts, b, "boundary") for b in BOUNDARY} if cpu_counts else {}
        alloc_boundary = {b: share(alloc_counts, b, "boundary") for b in BOUNDARY} if alloc_counts else {}
        out[arm] = {
            "benchmark": benchmark,
            "cpu": cpu_counts, "alloc": alloc_counts, "cpu_shares": cpu_shares, "alloc_shares": alloc_shares,
            "ns": {c: v * score / 100.0 for c, v in cpu_shares.items()} if score is not None else {},
            "bytes": {c: v * norm / 100.0 for c, v in alloc_shares.items()} if norm is not None else {},
            "cpu_boundary": cpu_boundary, "alloc_boundary": alloc_boundary,
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


BOUNDARY_COLUMNS = (["benchmark", "class", "param", "arm", "cpu_samples", "alloc_bytes"]
                    + [f"{b}_{k}_pct" for b in BOUNDARY for k in ("cpu", "alloc")])
PATH_COLUMNS = ["arm", "class", "param", "expect", "result", "expected_samples", "other_samples", "total",
                "sdk_fields_from_schema", "message"]


def boundary_rows(combos, arms):
    """One boundary.tsv row per (class, param, arm): the CPU and alloc share of every work-boundary item."""
    rows = []
    for (cls, param), (analysis, _) in combos.items():
        for arm in arms:
            a = analysis[arm]
            if not a["cpu"] and not a["alloc"]:
                continue
            row = {"benchmark": a["benchmark"] or "", "class": cls, "param": param, "arm": arm,
                   "cpu_samples": a["cpu"]["total"] if a["cpu"] else "",
                   "alloc_bytes": a["alloc"]["total"] if a["alloc"] else ""}
            for b in BOUNDARY:
                for kind in ("cpu", "alloc"):
                    shares = a[f"{kind}_boundary"]
                    row[f"{b}_{kind}_pct"] = f"{shares[b]:.3f}" if shares else ""
            rows.append(row)
    return rows


def boundary_section(combos, arms):
    lines = ["## Work-boundary items (A1-objects, A1-request, A2, A5)", "",
             "Work inside one arm's timed method that the other arm's client does outside the serde call, plus "
             "A1-request, work both arms do that the baseline does inside its legacy marshaller construction "
             "(`comparability-matrix.md`; rules in the `serde_ab_profile.py` docstring): "
             + "; ".join(f"{b}: {d}" for b, d in BOUNDARY.items())
             + ". Share of CPU samples and allocated bytes (all items together), and the estimate in ns/op and B/op "
             "(share x score / norm). `serde_ab_summary.py summarize --boundary <run>/boundary.tsv` removes "
             "A1-objects + A2 + A5 for its comparable estimate and every item for its serde-only reference. Rows "
             "where every item is below 1% on every arm are left out.", "",
             "| class | case |" + "".join(f" {a} CPU % | {a} ns/op | {a} alloc % | {a} B/op | {a} items |" for a in arms),
             "|---|---|" + "---:|---:|---:|---:|---|" * len(arms)]
    for (cls, param), (analysis, _) in combos.items():
        cells, show = [], False
        for arm in arms:
            a = analysis[arm]
            cpu, alloc = sum(a["cpu_boundary"].values()), sum(a["alloc_boundary"].values())
            show |= cpu >= 1.0 or alloc >= 1.0
            items = ", ".join(b for b in BOUNDARY if a["cpu_boundary"].get(b, 0) >= 1.0
                              or a["alloc_boundary"].get(b, 0) >= 1.0)
            cells += [_f(cpu if a["cpu"] else None), _f(None if a["score"] is None or not a["cpu"]
                                                        else cpu * a["score"] / 100.0),
                      _f(alloc if a["alloc"] else None), _f(None if a["norm"] is None or not a["alloc"]
                                                            else alloc * a["norm"] / 100.0, 0), items or "-"]
        if show:
            lines.append(f"| {cls} | {param} | " + " | ".join(cells) + " |")
    return lines + [""]


def path_section(path_rows):
    lines = ["## Runtime code path (every param)", ""]
    if path_rows is None:
        return lines + ["Not run: `arms.tsv` has no `expect_path` column.", ""]
    if not path_rows:
        return lines + ["Not run: no cpu profile in this run.", ""]
    lines += ["`serde_ab_checks.py path` over every cpu profile (stacks above the JMH stub): the expected path's "
              "frames present, the other path's absent. Rows: `profile-path.tsv`.", "",
              "| arm | class | expect | params | PASS | min expected-path share | other-path samples "
              "| SdkFieldsFromSchema samples |", "|---|---|---|---:|---:|---:|---:|---:|"]
    groups = OrderedDict()
    for r in path_rows:
        groups.setdefault((r["arm"], r["class"], r["expect"]), []).append(r)
    for (arm, cls, expect), rows in groups.items():
        shares = [r["expected_samples"] / r["total"] for r in rows if r["total"]]
        lines.append(f"| {arm} | {cls} | {expect} | {len(rows)} | {sum(r['result'] == 'PASS' for r in rows)} "
                     f"| {_f(min(shares) if shares else None, 2)} | {sum(r['other_samples'] for r in rows)} "
                     f"| {sum(r['sdk_fields_from_schema'] for r in rows)} |")
    failed = [r for r in path_rows if r["result"] != "PASS"]
    lines += ["", f"Result: {'PASS' if not failed else 'FAIL'} ({len(path_rows) - len(failed)}/{len(path_rows)})."]
    lines += [f"- FAIL {r['arm']} {r['class']} [{r['param']}]: {r['message']}" for r in failed]
    return lines + [""]


def analyze_all(profiles, arms, timing_doc):
    """{(class, param): (analysis, flags)}."""
    timing = timing_means(timing_doc)
    combos = OrderedDict()
    for (cls, param), per_arm in profiles.items():
        analysis = analyze_combo(per_arm, arms, timing, param)
        combos[(cls, param)] = (analysis, combo_flags(analysis, arms))
    return combos


def render(rundir, combos, arms, timing_doc, path_rows=None):
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
    lines += path_section(path_rows)
    lines += boundary_section(combos, arms)

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
    opts = {"--timing": None, "--out": None, "--out-dir": None}
    i = 0
    while i < len(rest):
        if rest[i] in opts and i + 1 < len(rest):
            opts[rest[i]] = rest[i + 1]
            i += 2
        else:
            raise SystemExit(f"unknown option {rest[i]}\n{__doc__}")
    arm_rows = _read_tsv(os.path.join(rundir, "arms.tsv"))
    arms = [r["label"] for r in arm_rows]
    profiles = load_profiles(rundir)
    timing_doc = sab.load(opts["--timing"]) if opts["--timing"] else None
    combos = analyze_all(profiles, arms, timing_doc)
    out_dir = opts["--out-dir"] or rundir
    os.makedirs(out_dir, exist_ok=True)
    path_rows = None
    if arm_rows and "expect_path" in arm_rows[0]:
        path_rows = path_checks(profiles, {r["label"]: r["expect_path"] for r in arm_rows})
        write_tsv(os.path.join(out_dir, "profile-path.tsv"), path_rows, PATH_COLUMNS)
    write_tsv(os.path.join(out_dir, "boundary.tsv"), boundary_rows(combos, arms), BOUNDARY_COLUMNS)
    out = opts["--out"] or os.path.join(out_dir, "profile-comparison.md")
    with open(out, "w") as f:
        f.write(render(rundir, combos, arms, timing_doc, path_rows))
    print(f"Wrote {out}, {os.path.join(out_dir, 'boundary.tsv')}"
          + (f", {os.path.join(out_dir, 'profile-path.tsv')}" if path_rows is not None else ""), file=sys.stderr)
    failed = [r for r in path_rows or [] if r["result"] != "PASS"]
    if failed:
        print(f"FAIL runtime code path: {len(failed)} of {len(path_rows)} cpu profiles; see {out}", file=sys.stderr)
        return 1
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
            self.assertIn("## Work-boundary items (A1-objects, A1-request, A2, A5)", text)
            # Baseline: construction 30% of CPU = 300 ns/op of A1-objects (no request or generator frame).
            self.assertIn("| RestJsonMarshallBenchmark | restJson1_PutObject_S | 30.0 | 300.0 | 0.0 | 0 | A1-objects "
                          "| 0.0 | 0.0 | 0.0 | 0 | - |", text)
            self.assertIn("Not run: `arms.tsv` has no `expect_path` column.", text)

        def test_boundaryItem_whenConstructionMetadataStatusOrPercentEncoding_assignsItem(self):
            def item(*frames):
                hot = hot_stack(parse(stub + ";" + ";".join(frames) + " 1")[0], False)
                return boundary_item(hot, categorize(hot))
            self.assertEqual(item(bench, "software/amazon/awssdk/protocols/json/internal/marshall/"
                                         "JsonProtocolMarshallerBuilder.build"), "A1-objects")
            unm = "software/amazon/awssdk/benchmark/serde/QueryUnmarshallBenchmark.unmarshall"
            self.assertEqual(item(unm, "software/amazon/awssdk/protocols/client/http/HttpClientProtocol.responseMetadata",
                                  "java/util/TreeMap.get"), "A2")
            self.assertEqual(item(unm, "software/amazon/awssdk/protocols/client/http/HttpClientProtocol.isErrorResponse",
                                  "software/amazon/awssdk/http/SdkHttpResponse.isSuccessful"), "A2")
            qfs = "software/amazon/awssdk/protocols/client/awsquery/internal/QueryFormSerializer"
            self.assertEqual(item(bench, qfs + "$ValueSerializer.writeString", qfs + ".writeUrlEncoded",
                                  "java/lang/String.charAt"), "A5")
            self.assertIsNone(item(bench, qfs + "$ValueSerializer.writeString", qfs + ".writeKey"))
            self.assertIsNone(item(bench, "software/amazon/awssdk/codecs/json/internal/SmithyJsonSerializer.w"))

        def test_boundaryItem_whenConstruction_splitsObjectsAndRequest(self):
            def hot(*frames, alloc=False):
                return hot_stack(parse(stub + ";" + ";".join(frames) + " 1")[0], alloc)

            def item(*frames, alloc=False):
                h = hot(*frames, alloc=alloc)
                return boundary_item(h, categorize(h))
            jm = "software/amazon/awssdk/protocols/json/internal/marshall/"
            build = jm + "JsonProtocolMarshallerBuilder.build"
            init = jm + "JsonProtocolMarshaller.<init>"
            fill = jm + "JsonProtocolMarshaller.fillBasicRequestParams"
            create = "software/amazon/awssdk/protocols/core/ProtocolUtils.createSdkHttpRequest"
            self.assertEqual(item(bench, build, init, jm + "JsonMarshallerContext.builder"), "A1-objects")
            self.assertEqual(item(bench, build, init, fill, create,
                                  "software/amazon/awssdk/http/SdkHttpFullRequest.builder"), "A1-request")
            self.assertEqual(a1_part(hot(bench, build, init, fill, create,
                                         "software/amazon/awssdk/http/SdkHttpFullRequest.builder")), "request")
            self.assertEqual(item(bench, build, init, fill,
                                  "software/amazon/awssdk/http/SdkHttpFullRequest$Builder.putHeader",
                                  "java/util/TreeMap.put"), "A1-request")
            qm = "software/amazon/awssdk/protocols/query/internal/marshall/QueryProtocolMarshaller"
            self.assertEqual(item(bench, qm + "$Builder.build", qm + ".<init>", qm + ".fillBasicRequestParams",
                                  "software/amazon/awssdk/http/SdkHttpFullRequest$Builder.putRawQueryParameter"),
                             "A1-request")
            factory = "software/amazon/awssdk/protocols/json/BaseAwsJsonProtocolFactory"
            generator = hot(bench, factory + ".createProtocolMarshaller", factory + ".createGenerator",
                            "software/amazon/awssdk/protocols/json/SdkJsonGenerator.<init>",
                            "software/amazon/awssdk/thirdparty/jackson/core/json/UTF8JsonGenerator.<init>",
                            "software/amazon/awssdk/thirdparty/jackson/core/util/BufferRecycler.calloc")
            self.assertEqual(boundary_item(generator, categorize(generator)), "A1-request")
            self.assertEqual(a1_part(generator), "generator")
            self.assertEqual(item(bench, factory + ".createProtocolMarshaller", factory + ".endpoint",
                                  "software/amazon/awssdk/core/client/config/SdkClientConfiguration.option"),
                             "A1-objects")
            xml = "software/amazon/awssdk/protocols/xml/internal/marshall/"
            writer = hot(bench, xml + "XmlGenerator.create", xml + "XmlWriter.<init>")
            self.assertEqual(boundary_item(writer, categorize(writer)), "A1-request")
            self.assertEqual(a1_part(writer), "generator")
            # CPU VM leaves: the frames above them decide.
            self.assertEqual(item(bench, build, init, fill, create,
                                  "software/amazon/awssdk/http/SdkHttpFullRequest$Builder.uri", "zero_blocks"),
                             "A1-request")
            self.assertEqual(item(bench, build, "C2 Runtime new_instance", "OptoRuntime::new_instance_C"),
                             "A1-objects")
            # Allocation sample: the allocated type is dropped, so a request type alone does not make A1-request.
            self.assertEqual(item(bench, build, "software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder_[i]",
                                  alloc=True), "A1-objects")

        def test_boundaryItem_whenMetadata_onlyFeatureOnlyWorkIsA2(self):
            def item(*frames):
                h = hot_stack(parse(stub + ";" + ";".join(frames) + " 1")[0], False)
                return boundary_item(h, categorize(h))
            unm = "software/amazon/awssdk/benchmark/serde/QueryUnmarshallBenchmark.unmarshall"
            hcp = "software/amazon/awssdk/protocols/client/http/HttpClientProtocol"
            self.assertEqual(item(unm, hcp + ".buildOutput",
                                  "software/amazon/awssdk/services/s/model/XResponse$BuilderImpl.responseMetadata"),
                             "A2")
            self.assertEqual(item(unm, "software/amazon/awssdk/core/schema/SchemaSerdeSupport.buildResponse",
                                  "software/amazon/awssdk/core/SdkResponse$BuilderImpl.sdkHttpResponse"), "A2")
            # Request-id extraction: both timed methods do it, so it is category metadata but no boundary item.
            read_id = hot_stack(parse(stub + ";" + unm + ";software/amazon/awssdk/protocols/client/awsquery/"
                                      "QueryClientProtocol.deserializeOutput;software/amazon/awssdk/protocols/client/"
                                      "awsquery/QueryClientProtocol.readRequestId;software/amazon/awssdk/codecs/xml/"
                                      "internal/SmithyXmlDeserializer.skipElement 1")[0], False)
            self.assertEqual(categorize(read_id), "metadata")
            self.assertIsNone(boundary_item(read_id, categorize(read_id)))
            qu = "software/amazon/awssdk/protocols/query/internal/unmarshall/QueryProtocolUnmarshaller"
            self.assertIsNone(item(unm, qu + ".unmarshall", qu + ".parseMetadata"))
            # toBuilder() getters on both arms: POJO copying, not A2.
            self.assertIsNone(item(unm, "software/amazon/awssdk/awscore/AwsResponse$BuilderImpl.<init>",
                                   "software/amazon/awssdk/services/s/model/XResponse.responseMetadata"))

        def test_compare_whenOutDirGiven_writesThereAndLeavesRunDir(self):
            jm = "software/amazon/awssdk/protocols/json/internal/marshall/"
            build = bench + ";" + jm + "JsonProtocolMarshallerBuilder.build;" + jm + "JsonProtocolMarshaller.<init>"
            lines = {"cpu": [stub + ";" + build + ";" + jm + "JsonMarshallerContext.builder 25\n",
                             stub + ";" + build + ";" + jm + "JsonProtocolMarshaller.fillBasicRequestParams;"
                             "software/amazon/awssdk/http/SdkHttpFullRequest.builder 75\n"],
                     "alloc": [stub + ";" + build + ";" + jm + "JsonMarshallerContext.builder;java/lang/Object 40\n",
                               stub + ";" + build + ";" + jm + "JsonProtocolMarshaller.fillBasicRequestParams;"
                               "java/util/TreeMap 60\n"]}
            with tempfile.TemporaryDirectory() as d, tempfile.TemporaryDirectory() as parent:
                out = os.path.join(parent, "new")
                with open(os.path.join(d, "arms.tsv"), "w") as f:
                    f.write("label\tref\nbaseline\tfc8b\nfeature\tHEAD\n")
                rows = ["arm\tclass\tparam\tevent\tstatus\tdir"]
                for arm in ("baseline", "feature"):
                    for event in ("cpu", "alloc"):
                        rel = f"profile/{arm}/RestJsonMarshallBenchmark/S/{event}"
                        os.makedirs(os.path.join(d, rel))
                        with open(os.path.join(d, rel, f"collapsed-{event}.csv"), "w") as f:
                            f.writelines(lines[event])
                        rows.append(f"{arm}\tRestJsonMarshallBenchmark\tS\t{event}\tok\t{rel}")
                with open(os.path.join(d, "profile-runs.tsv"), "w") as f:
                    f.write("\n".join(rows) + "\n")
                self.assertEqual(compare_main([d, "--out-dir", out]), 0)
                self.assertTrue(os.path.isfile(os.path.join(out, "profile-comparison.md")))
                self.assertFalse(os.path.exists(os.path.join(d, "boundary.tsv")))
                self.assertFalse(os.path.exists(os.path.join(d, "profile-comparison.md")))
                with open(os.path.join(out, "boundary.tsv")) as f:
                    header = f.readline().rstrip("\n").split("\t")
                boundary = _read_tsv(os.path.join(out, "boundary.tsv"))
            self.assertEqual(header, BOUNDARY_COLUMNS)
            self.assertIn("A1-objects_cpu_pct", header)
            self.assertNotIn("A1_cpu_pct", header)
            row = boundary[0]
            self.assertEqual((row["A1-objects_cpu_pct"], row["A1-request_cpu_pct"]), ("25.000", "75.000"))
            self.assertEqual((row["A1-objects_alloc_pct"], row["A1-request_alloc_pct"]), ("40.000", "60.000"))
            self.assertEqual((row["A2_cpu_pct"], row["A5_alloc_pct"]), ("0.000", "0.000"))

        def test_compare_whenExpectPathGiven_checksEveryCpuProfileAndWritesBoundaryTsv(self):
            legacy = stub + ";" + bench + ";software/amazon/awssdk/protocols/json/internal/marshall/" \
                                          "JsonProtocolMarshaller.marshall 100\n"
            schema = stub + ";" + bench + ";software/amazon/awssdk/protocols/client/restjson/RestJsonClientProtocol" \
                                          ".serializeRequest 100\n"
            with tempfile.TemporaryDirectory() as d:
                with open(os.path.join(d, "arms.tsv"), "w") as f:
                    f.write("label\tref\texpect_path\nbaseline\tfc8b\tlegacy\nfeature\tHEAD\tschema\n")
                rows = ["arm\tclass\tparam\tevent\tstatus\tdir"]
                # Param S: both arms on their expected path. Param M: the feature profile shows legacy frames.
                for param, feature_lines in (("S", schema), ("M", legacy)):
                    for arm, line in (("baseline", legacy), ("feature", feature_lines)):
                        rel = f"profile/{arm}/RestJsonMarshallBenchmark/{param}/cpu"
                        os.makedirs(os.path.join(d, rel))
                        with open(os.path.join(d, rel, "collapsed-cpu.csv"), "w") as f:
                            f.write(line)
                        rows.append(f"{arm}\tRestJsonMarshallBenchmark\t{param}\tcpu\tok\t{rel}")
                rows.append("feature\tRestJsonMarshallBenchmark\tL\tcpu\tfailed\tprofile/feature/x/L/cpu")
                rows.append("baseline\tRestJsonMarshallBenchmark\tL\tcpu\tok\tprofile/baseline/x/L/cpu")
                with open(os.path.join(d, "profile-runs.tsv"), "w") as f:
                    f.write("\n".join(rows) + "\n")
                self.assertEqual(compare_main([d]), 1)
                path_rows = _read_tsv(os.path.join(d, "profile-path.tsv"))
                boundary = _read_tsv(os.path.join(d, "boundary.tsv"))
                with open(os.path.join(d, "profile-comparison.md")) as f:
                    text = f.read()
            results = {(r["arm"], r["param"]): r["result"] for r in path_rows}
            self.assertEqual(results, {("baseline", "S"): "PASS", ("feature", "S"): "PASS",
                                       ("baseline", "M"): "PASS", ("feature", "M"): "FAIL",
                                       ("baseline", "L"): "FAIL", ("feature", "L"): "FAIL"})
            self.assertIn("Result: FAIL (3/6).", text)
            self.assertIn("- FAIL feature RestJsonMarshallBenchmark [M]: no schema-path hits", text)
            self.assertIn("| feature | RestJsonMarshallBenchmark | schema | 3 | 1 | 0.00 | 100 | 0 |", text)
            self.assertIn("- FAIL feature RestJsonMarshallBenchmark [L]: no benchmark samples or classes", text)
            self.assertEqual(set(boundary[0]), set(BOUNDARY_COLUMNS))
            self.assertEqual({(r["arm"], r["param"]) for r in boundary}, {("baseline", "S"), ("feature", "S"),
                                                                          ("baseline", "M"), ("feature", "M")})

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(SerdeAbProfileTest)
    return 0 if unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
