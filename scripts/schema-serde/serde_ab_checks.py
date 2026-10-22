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
"""Comparability checks for the serde A/B of scripts/schema-serde/serde-ab.sh (serde benchmark plan, D4).

Each check proves, by running on the built artifacts, that two arms of the serde A/B measure the same thing: the
same models and payloads, the same request objects and responses, and the expected code path at runtime. Python 3
standard library only; reuses hot_path_frames.py (same directory) for async-profiler collapsed stacks.

Usage:
  serde_ab_checks.py manifest SOURCE
      Prints the sha256 manifest of the serde resources of SOURCE: a benchmarks.jar (entries under serde-tests/ and
      codegen-resources/<the 5 serde services>/), a checkout or worktree root, or a module resources directory.
  serde_ab_checks.py resources A B
      Compares the manifests of A and B (each a jar, a directory or a manifest file written by `manifest`). The
      jar does not package codegen-resources/, so serde-ab.sh also compares the worktree manifests it records next
      to each jar. Fails if either side lacks the serde-tests/ files of the 5 protocols, or, for a source tree or
      manifest, any of the 5 codegen-resources directories.
  serde_ab_checks.py models JAR_A JAR_B [--strict]
      Compares a normalized projection of the 5 intermediate models packaged in each jar
      (models/<service>-1999-12-31-intermediate.json): per shape its marshaller/unmarshaller metadata, enum values,
      required members and per member the c2jName, c2jShape, variable type, marshalling type, list/map element types
      and location names, http location/marshallLocationName/isPayload/isStreaming/greedy/flattened, timestampFormat,
      xml namespace/attribute, required, idempotency token and sensitivity; per operation the input/output shapes.
      Prints every differing <service>:<shape>.<member> path. Two value-checked differences are KNOWN instead of
      failing (unless --strict), see KNOWN_MODEL_DIFFERENCES: a greedy label ({Key+}) marked on one arm only, and
      the rest-xml service namespace on the input shapes of two operations no rest-xml case uses.
  serde_ab_checks.py wire DUMP_A DUMP_B [--strict]
      Compares two SerdeWireDump.java outputs row by row (key: class, case). One difference is known and reported
      as KNOWN instead of failing (unless --strict): on the feature arm the LEGACY marshallers run over schema-derived
      SdkFields, and SdkFieldsFromSchema maps greedy httpLabel members to PATH, so a greedy label such as {Key+}
      stays unexpanded. It is accepted only when every other column is equal and the URI matches the other side's
      URI with each {Name+} placeholder standing for a non-empty value. The schema path the feature benchmarks
      measure expands it (SerdeBenchmarkEquivalenceTest).
  serde_ab_checks.py path --expect legacy|schema DIR_OR_FILE...
      Per async-profiler collapsed file (collapsed-*.csv, found recursively): counts the samples above the JMH stub
      that contain schema-path frames (protocols.client, codecs, httpbinding) or legacy serde frames (the
      protocols.{json,xml,query,cbor,rpcv2}.internal marshall/unmarshall packages and the legacy marshaller classes).
      schema: schema > 0 and legacy == 0; legacy: the reverse. Warns when a schema arm shows
      core.schema.SdkFieldsFromSchema frames (legacy-over-schema SdkFields).
  serde_ab_checks.py classload --expect legacy|schema LOG...
      The same expectation over -Xlog:class+load logs (works without async-profiler): the expected path's classes
      must be loaded and the other path's classes must not be.
  serde_ab_checks.py --self-test
      Runs the fixture tests below.

Exit status: 0 pass, 1 a check failed, 2 usage error.
"""
import csv
import hashlib
import json
import os
import re
import sys
import zipfile
from collections import OrderedDict

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import hot_path_frames as hpf  # noqa: E402

SERDE_SERVICES = ("json-rpc-1-0", "query", "rest-json", "rest-xml", "rpc-v2-cbor")
SERDE_TEST_PROTOCOLS = SERDE_SERVICES  # serde-tests/<protocol>/{input,output}/*.json use the same directory names
MODULE_RESOURCES = os.path.join("test", "sdk-standard-benchmarks", "src", "main", "resources")
MODEL_SERVICES = ("awsjsonrpc10dataplane", "awsrestjsondataplane", "awsrestxmldataplane", "awsquerydataplane",
                  "smithyrpcv2cbordataplane")
MANIFEST_HEADER = "# serde-ab resources manifest v1"

# Runtime code path, as dotted frame or class names.
SCHEMA_PREFIXES = ("software.amazon.awssdk.protocols.client.", "software.amazon.awssdk.codecs.",
                   "software.amazon.awssdk.httpbinding.")
LEGACY_PATTERN = re.compile(
    r"^software\.amazon\.awssdk\.protocols\.(json|xml|query|cbor|rpcv2)\.internal\.(marshall|unmarshall)\."
    r"|^software\.amazon\.awssdk\.protocols\.core\.(AbstractMarshallingRegistry|ProtocolMarshaller)\b"
    r"|^software\.amazon\.awssdk\.protocols\.json\.(AwsJsonProtocolFactory|BaseAwsJsonProtocolFactory)\b")
SDK_FIELDS_FROM_SCHEMA = "software.amazon.awssdk.core.schema.SdkFieldsFromSchema"
PATHS = ("legacy", "schema")


class CheckError(Exception):
    """A usage or input problem (exit 2), as opposed to a failed check (exit 1)."""


def _sha256(data):
    return hashlib.sha256(data).hexdigest()


# ==================== resources ====================

def _wanted(rel):
    rel = rel.replace(os.sep, "/")
    if rel.endswith("/"):
        return False
    if rel.startswith("serde-tests/"):
        return True
    return any(rel.startswith(f"codegen-resources/{s}/") for s in SERDE_SERVICES)


def manifest(source):
    """{relative path: sha256} of the serde resources of a jar, a source tree or a manifest file."""
    if os.path.isfile(source) and zipfile.is_zipfile(source):
        with zipfile.ZipFile(source) as z:
            return OrderedDict((n, _sha256(z.read(n))) for n in sorted(z.namelist()) if _wanted(n))
    if os.path.isfile(source):
        with open(source) as f:
            lines = f.read().splitlines()
        if not lines or lines[0] != MANIFEST_HEADER:
            raise CheckError(f"{source}: not a jar, a directory or a manifest ({MANIFEST_HEADER!r})")
        out = OrderedDict()
        for line in lines[1:]:
            if line.strip():
                digest, _, rel = line.partition("\t")
                out[rel] = digest
        return out
    if os.path.isdir(source):
        root = os.path.join(source, MODULE_RESOURCES) if os.path.isdir(os.path.join(source, MODULE_RESOURCES)) \
            else source
        out = OrderedDict()
        for top in ["serde-tests"] + [f"codegen-resources/{s}" for s in SERDE_SERVICES]:
            base = os.path.join(root, top)
            for dirpath, dirnames, filenames in os.walk(base):
                dirnames.sort()
                for name in sorted(filenames):
                    path = os.path.join(dirpath, name)
                    rel = os.path.relpath(path, root).replace(os.sep, "/")
                    with open(path, "rb") as f:
                        out[rel] = _sha256(f.read())
        return OrderedDict(sorted(out.items()))
    raise CheckError(f"{source}: not found")


def render_manifest(entries):
    return "\n".join([MANIFEST_HEADER] + [f"{d}\t{rel}" for rel, d in entries.items()]) + "\n"


def _coverage_problems(name, entries, need_codegen):
    problems = []
    for p in SERDE_TEST_PROTOCOLS:
        for kind in ("input", "output"):
            if not any(rel.startswith(f"serde-tests/{p}/{kind}/") for rel in entries):
                problems.append(f"{name}: no serde-tests/{p}/{kind}/ files")
    if need_codegen:
        for s in SERDE_SERVICES:
            if not any(rel.startswith(f"codegen-resources/{s}/") for rel in entries):
                problems.append(f"{name}: no codegen-resources/{s}/ files")
    return problems


def compare_resources(a, b, name_a="A", name_b="B", need_codegen=False):
    """List of difference lines between two manifests (empty: identical and complete)."""
    out = _coverage_problems(name_a, a, need_codegen) + _coverage_problems(name_b, b, need_codegen)
    for rel in sorted(set(a) | set(b)):
        if rel not in b:
            out.append(f"only in {name_a}: {rel}")
        elif rel not in a:
            out.append(f"only in {name_b}: {rel}")
        elif a[rel] != b[rel]:
            out.append(f"differs: {rel} ({name_a} {a[rel][:12]}, {name_b} {b[rel][:12]})")
    return out


def _is_jar(path):
    return os.path.isfile(path) and zipfile.is_zipfile(path)


def resources_main(argv):
    if len(argv) != 2:
        raise CheckError("resources needs A B")
    a, b = manifest(argv[0]), manifest(argv[1])
    need_codegen = not (_is_jar(argv[0]) and _is_jar(argv[1]))
    diffs = compare_resources(a, b, argv[0], argv[1], need_codegen)
    for d in diffs:
        print(f"FAIL {d}")
    if diffs:
        return 1
    scope = "serde-tests/" + ("" if not need_codegen else " and codegen-resources/{" + ",".join(SERDE_SERVICES) + "}")
    print(f"PASS resources: {len(a)} files identical ({scope})")
    return 0


# ==================== models ====================

def _sub(d, keys):
    d = d or {}
    return OrderedDict((k, d.get(k)) for k in keys)


def _var_type(member_model):
    return ((member_model or {}).get("variable") or {}).get("variableType")


def project_member(m):
    list_model = m.get("listModel")
    map_model = m.get("mapModel")
    return OrderedDict([
        ("c2jShape", m.get("c2jShape")),
        ("variableType", _var_type(m)),
        ("simpleType", (m.get("variable") or {}).get("simpleType")),
        ("marshallingType", m.get("marshallingType")),
        ("list", None if not list_model else _sub(list_model, ("memberType", "memberLocationName",
                                                              "memberAdditionalMarshallingPath",
                                                              "memberAdditionalUnmarshallingPath"))),
        ("map", None if not map_model else OrderedDict([
            ("keyType", _var_type(map_model.get("keyModel"))),
            ("valueType", _var_type(map_model.get("valueModel"))),
            ("keyLocationName", map_model.get("keyLocationName")),
            ("valueLocationName", map_model.get("valueLocationName"))])),
        ("http", _sub(m.get("http"), ("location", "marshallLocation", "marshallLocationName",
                                      "unmarshallLocationName", "additionalMarshallingPath",
                                      "additionalUnmarshallingPath", "isPayload", "isStreaming", "greedy",
                                      "flattened", "requiresLength"))),
        ("timestampFormat", m.get("timestampFormat")),
        ("xmlNameSpaceUri", m.get("xmlNameSpaceUri")),
        ("xmlAttribute", m.get("xmlAttribute")),
        ("required", m.get("required")),
        ("idempotencyToken", m.get("idempotencyToken")),
        ("jsonValue", m.get("jsonValue")),
        ("sensitive", m.get("sensitive")),
        ("isBinary", m.get("isBinary")),
    ])


def project_model(doc):
    """The comparability-relevant part of an intermediate model as plain nested dicts."""
    shapes = OrderedDict()
    for name in sorted(doc.get("shapes") or {}):
        s = doc["shapes"][name]
        enums = s.get("enums")
        shapes[name] = OrderedDict([
            ("c2jName", s.get("c2jName")),
            ("required", sorted(s.get("required") or [])),
            ("enums", None if enums is None else [e.get("value") for e in enums]),
            ("union", s.get("union")),
            ("document", s.get("document")),
            ("eventStream", s.get("eventStream")),
            ("xmlNamespace", s.get("xmlNamespace")),
            ("errorCode", s.get("errorCode")),
            ("httpStatusCode", s.get("httpStatusCode")),
            ("marshaller", None if not s.get("marshaller") else _sub(
                s["marshaller"], ("action", "verb", "target", "requestUri", "locationName", "xmlNameSpaceUri",
                                  "protocol"))),
            ("unmarshaller", None if not s.get("unmarshaller") else OrderedDict(
                sorted((k, v) for k, v in s["unmarshaller"].items() if k != "documentation"))),
            ("members", OrderedDict((m.get("c2jName"), project_member(m)) for m in (s.get("members") or []))),
        ])
    operations = OrderedDict()
    for name in sorted(doc.get("operations") or {}):
        o = doc["operations"][name]
        operations[name] = OrderedDict([
            ("input", _var_type({"variable": o.get("input")})),
            ("returnType", (o.get("returnType") or {}).get("returnType")),
            ("serviceProtocol", o.get("serviceProtocol")),
            ("hasBlobMemberAsPayload", o.get("hasBlobMemberAsPayload")),
            ("hasStringMemberAsPayload", o.get("hasStringMemberAsPayload")),
        ])
    metadata = _sub(doc.get("metadata"), ("protocol", "jsonVersion", "targetPrefix", "apiVersion", "xmlNamespace",
                                          "contentType", "endpointPrefix", "signingName"))
    return OrderedDict([("metadata", metadata), ("operations", operations), ("shapes", shapes)])


_ONLY_A, _ONLY_B = object(), object()


def diff_tree(a, b, path=""):
    """[(dotted path, value A, value B)] where two projections differ; _ONLY_A/_ONLY_B mark a missing key."""
    if isinstance(a, dict) and isinstance(b, dict):
        out = []
        for k in list(a) + [k for k in b if k not in a]:
            p = f"{path}.{k}" if path else str(k)
            if k not in b:
                out.append((p, a[k], _ONLY_A))
            elif k not in a:
                out.append((p, _ONLY_B, b[k]))
            else:
                out += diff_tree(a[k], b[k], p)
        return out
    return [] if a == b else [(path, a, b)]


def _render_diff(path, a, b):
    if b is _ONLY_A:
        return f"{path}: only in A"
    if a is _ONLY_B:
        return f"{path}: only in B"
    return f"{path}: A={json.dumps(a)} B={json.dumps(b)}"


def _greedy_known(service, path, a, b, proj):
    """A greedy label marked on one arm only, for a member bound to a {Name+} label of the shape's request URI."""
    m = re.fullmatch(r"shapes\.([^.]+)\.members\.([^.]+)\.http\.(greedy|marshallLocation)", path)
    if not m or {a, b} not in ({True, False}, {"GREEDY_PATH", "PATH"}):
        return False
    shape = proj["shapes"][m.group(1)]
    uri = (shape.get("marshaller") or {}).get("requestUri") or ""
    name = shape["members"][m.group(2)]["http"].get("marshallLocationName")
    return f"{{{name}+}}" in uri


def _xml_namespace_known(service, path, a, b, proj):
    m = re.fullmatch(r"shapes\.([^.]+)\.xmlNamespace", path)
    if service != "awsrestxmldataplane" or not m or m.group(1) not in UNUSED_REST_XML_INPUTS:
        return False
    values = [a, b]
    return None in values and any(isinstance(v, dict) and v.get("uri") == REST_XML_NAMESPACE for v in values)


REST_XML_NAMESPACE = "https://awsrestxmldataplane.amazonaws.com"
# Input shapes of the rest-xml model's GetMetricData/PutMetricData operations: no rest-xml serde case uses them (the
# RestXml cases are CopyObject and PutObject).
UNUSED_REST_XML_INPUTS = ("GetMetricDataRequest", "PutMetricDataRequest")
# (rule, note). Each rule checks the values, not just the path; anything else fails.
KNOWN_MODEL_DIFFERENCES = (
    (_greedy_known, "greedy httpLabel: the feature codegen's intermediate model marks the {Key+} member PATH, not "
                    "GREEDY_PATH, for the same request URI. The schema path the feature benchmarks measure expands "
                    "the label (SerdeBenchmarkEquivalenceTest); the feature arm's legacy path does not (wire KNOWN)"),
    (_xml_namespace_known, "the feature codegen records the service XML namespace on these input shapes; no "
                           "rest-xml serde case uses their operations"),
)


def load_models(jar):
    out = OrderedDict()
    with zipfile.ZipFile(jar) as z:
        names = set(z.namelist())
        for service in MODEL_SERVICES:
            entry = f"models/{service}-1999-12-31-intermediate.json"
            if entry not in names:
                raise CheckError(f"{jar}: {entry} not found")
            out[service] = json.loads(z.read(entry).decode("utf-8"))
    return out


def compare_models(models_a, models_b):
    """(failures, known) lists of "<service>:<path>: ..." lines."""
    failures, known = [], []
    for service in MODEL_SERVICES:
        proj_a, proj_b = project_model(models_a[service]), project_model(models_b[service])
        for path, a, b in diff_tree(proj_a, proj_b):
            line = f"{service}:{_render_diff(path, a, b)}"
            note = next((n for rule, n in KNOWN_MODEL_DIFFERENCES
                         if a is not _ONLY_B and b is not _ONLY_A and rule(service, path, a, b, proj_a)), None)
            if note:
                known.append(f"{line} ({note})")
            else:
                failures.append(line)
    return failures, known


def models_main(argv):
    strict = "--strict" in argv
    paths = [x for x in argv if x != "--strict"]
    if len(paths) != 2:
        raise CheckError("models needs JAR_A JAR_B")
    a, b = load_models(paths[0]), load_models(paths[1])
    failures, known = compare_models(a, b)
    for k in known:
        print(f"{'FAIL' if strict else 'KNOWN'} {k}")
    for f in failures:
        print(f"FAIL {f}")
    if failures or (strict and known):
        return 1
    shapes = sum(len(a[s].get("shapes") or {}) for s in MODEL_SERVICES)
    print(f"PASS models: {len(MODEL_SERVICES)} intermediate models, {shapes} shapes identical after projection"
          + (f" except {len(known)} known differences" if known else ""))
    return 0


# ==================== wire ====================

WIRE_COLUMNS = ("class", "case", "kind", "method", "uri", "headers", "body_length", "body_sha256")
GREEDY = re.compile(r"\{[A-Za-z0-9_]+\+\}")
GREEDY_NOTE = ("greedy httpLabel left unexpanded by the legacy marshaller over schema-derived SdkFields "
               "(SdkFieldsFromSchema maps greedy labels to PATH); not on the schema path the feature measures")


def read_dump(path):
    with open(path, newline="") as f:
        rows = list(csv.DictReader(f, delimiter="\t", quoting=csv.QUOTE_NONE))
    if rows and tuple(rows[0].keys()) != WIRE_COLUMNS:
        raise CheckError(f"{path}: columns {list(rows[0].keys())}, expected {list(WIRE_COLUMNS)}")
    out = OrderedDict()
    for r in rows:
        key = (r["class"], r["case"])
        if key in out:
            raise CheckError(f"{path}: duplicate row {key}")
        out[key] = r
    return out


def _greedy_match(with_placeholder, expanded):
    if not GREEDY.search(with_placeholder):
        return False
    parts = GREEDY.split(with_placeholder)
    pattern = "[^?]+".join(re.escape(p) for p in parts)
    return re.fullmatch(pattern, expanded) is not None


def compare_wire(a, b):
    """(failures, known) lists of difference lines."""
    failures, known = [], []
    if not a or not b:
        failures.append("a dump has no rows")
    for key in list(a) + [k for k in b if k not in a]:
        name = f"{key[0]} {key[1]}"
        if key not in b:
            failures.append(f"{name}: only in A")
            continue
        if key not in a:
            failures.append(f"{name}: only in B")
            continue
        ra, rb = a[key], b[key]
        cols = [c for c in WIRE_COLUMNS if ra[c] != rb[c]]
        if not cols:
            continue
        if cols == ["uri"] and (_greedy_match(ra["uri"], rb["uri"]) or _greedy_match(rb["uri"], ra["uri"])):
            known.append(f"{name}: uri A={ra['uri']} B={rb['uri']} ({GREEDY_NOTE})")
            continue
        failures.append(f"{name}: " + "; ".join(f"{c} A={ra[c][:120]} B={rb[c][:120]}" for c in cols))
    return failures, known


def wire_main(argv):
    strict = "--strict" in argv
    paths = [x for x in argv if x != "--strict"]
    if len(paths) != 2:
        raise CheckError("wire needs DUMP_A DUMP_B")
    a, b = read_dump(paths[0]), read_dump(paths[1])
    failures, known = compare_wire(a, b)
    for k in known:
        print(f"{'FAIL' if strict else 'KNOWN'} {k}")
    for f in failures:
        print(f"FAIL {f}")
    if failures or (strict and known):
        return 1
    print(f"PASS wire: {len(a)} rows identical" + (f" except {len(known)} known differences" if known else ""))
    return 0


# ==================== runtime code path ====================

def frame_kind(name):
    if name.startswith(SCHEMA_PREFIXES):
        return "schema"
    if LEGACY_PATTERN.search(name):
        return "legacy"
    return None


def path_counts(lines):
    """{"total", "schema", "legacy", "sdk_fields_from_schema"} sample counts above the JMH stub."""
    counts = {"total": 0, "schema": 0, "legacy": 0, "sdk_fields_from_schema": 0}
    for line in lines:
        parsed = hpf.parse_line(line)
        if parsed is None:
            continue
        frames, n = parsed
        hot = hpf.hot_frames(frames)
        if hot is None:
            continue
        counts["total"] += n
        kinds = {frame_kind(f) for f in hot}
        for kind in ("schema", "legacy"):
            if kind in kinds:
                counts[kind] += n
        if any(f.startswith(SDK_FIELDS_FROM_SCHEMA) for f in hot):
            counts["sdk_fields_from_schema"] += n
    return counts


def path_verdict(expect, schema, legacy, total):
    """(ok, message) for one profile or log."""
    other = "legacy" if expect == "schema" else "schema"
    found = {"schema": schema, "legacy": legacy}
    if total == 0:
        return False, "no benchmark samples or classes"
    if found[expect] == 0:
        return False, f"no {expect}-path hits"
    if found[other] != 0:
        return False, f"{found[other]} {other}-path hits on an expected {expect} arm"
    return True, f"{expect} {found[expect]}, {other} 0"


def _collapsed_files(targets):
    files = []
    for t in targets:
        if os.path.isdir(t):
            for dirpath, _, names in os.walk(t):
                files += [os.path.join(dirpath, n) for n in names if n.startswith("collapsed-") and n.endswith(".csv")]
        elif os.path.isfile(t):
            files.append(t)
        else:
            raise CheckError(f"{t}: not found")
    return sorted(files)


def _expect(argv):
    if len(argv) < 3 or argv[0] != "--expect" or argv[1] not in PATHS:
        raise CheckError("needs --expect legacy|schema followed by at least one path")
    return argv[1], argv[2:]


def path_main(argv):
    expect, targets = _expect(argv)
    files = _collapsed_files(targets)
    if not files:
        print(f"FAIL path: no collapsed-*.csv under {' '.join(targets)}")
        return 1
    failed = False
    for path in files:
        with open(path) as f:
            c = path_counts(f)
        ok, message = path_verdict(expect, c["schema"], c["legacy"], c["total"])
        failed |= not ok
        print(f"{'PASS' if ok else 'FAIL'} path: {message} of {c['total']} benchmark samples ({path})")
        if expect == "schema" and c["sdk_fields_from_schema"]:
            print(f"WARNING {path}: {c['sdk_fields_from_schema']} samples in {SDK_FIELDS_FROM_SCHEMA}")
    return 1 if failed else 0


CLASS_LOAD = re.compile(r"\[class,load\s*\]\s+(\S+)\s")


def classload_counts(lines):
    counts = {"total": 0, "schema": 0, "legacy": 0, "sdk_fields_from_schema": 0, "legacy_classes": [],
              "schema_classes": []}
    for line in lines:
        m = CLASS_LOAD.search(line)
        if not m:
            continue
        name = m.group(1)
        counts["total"] += 1
        kind = frame_kind(name)
        if kind:
            counts[kind] += 1
            counts[f"{kind}_classes"].append(name)
        if name.startswith(SDK_FIELDS_FROM_SCHEMA):
            counts["sdk_fields_from_schema"] += 1
    return counts


def classload_main(argv):
    expect, logs = _expect(argv)
    failed = False
    for path in logs:
        if not os.path.isfile(path):
            raise CheckError(f"{path}: not found")
        with open(path, errors="replace") as f:
            c = classload_counts(f)
        ok, message = path_verdict(expect, c["schema"], c["legacy"], c["total"])
        failed |= not ok
        print(f"{'PASS' if ok else 'FAIL'} classload: {message} of {c['total']} loaded classes ({path})")
        other = "legacy" if expect == "schema" else "schema"
        for name in c[f"{other}_classes"][:5]:
            print(f"    unexpected {other} class: {name}")
    return 1 if failed else 0


# ==================== CLI ====================

def main(argv):
    if argv[:1] == ["--self-test"]:
        return self_test()
    commands = {"resources": resources_main, "models": models_main, "wire": wire_main, "path": path_main,
                "classload": classload_main}
    try:
        if len(argv) == 2 and argv[0] == "manifest":
            sys.stdout.write(render_manifest(manifest(argv[1])))
            return 0
        if argv and argv[0] in commands:
            return commands[argv[0]](argv[1:])
    except CheckError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 2
    print(__doc__, file=sys.stderr)
    return 2


# ==================== self-test ====================

def self_test():
    import contextlib
    import io
    import tempfile
    import unittest

    def run(args):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            status = main(args)
        return status, out.getvalue()

    def write(path, data):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb" if isinstance(data, bytes) else "w") as f:
            f.write(data)

    def resource_files(variant=""):
        files = {}
        for p in SERDE_SERVICES:
            files[f"serde-tests/{p}/input/{p}.json"] = '[{"id": "x"}]'
            files[f"serde-tests/{p}/output/{p}.json"] = '[{"id": "y"}]' + variant
            files[f"codegen-resources/{p}/service-2.json"] = "{}"
        files["codegen-resources/schema-other/service-2.json"] = "ignored" + variant
        return files

    def member(name, location="header", **extra):
        m = {"c2jName": name, "c2jShape": "String", "documentation": "doc",
             "variable": {"variableType": "String", "simpleType": "String"}, "marshallingType": "STRING",
             "http": {"location": location, "marshallLocationName": name, "isPayload": False}, "required": False}
        m.update(extra)
        return m

    def model(members):
        return {"metadata": {"protocol": "rest-json"}, "operations": {"PutObject": {
            "input": {"variableType": "PutObjectRequest"}, "returnType": {"returnType": "PutObjectResponse"}}},
            "shapes": {"PutObjectRequest": {"c2jName": "PutObjectRequest", "documentation": "x",
                                            "members": members}}}

    def jar(path, files=None, models=None):
        with zipfile.ZipFile(path, "w") as z:
            for name, data in (files or {}).items():
                z.writestr(name, data)
            for service in MODEL_SERVICES:
                z.writestr(f"models/{service}-1999-12-31-intermediate.json",
                           json.dumps((models or {}).get(service, model([member("Bucket")]))))
        return path

    header = "\t".join(WIRE_COLUMNS) + "\n"
    stub = "java/lang/Thread.run;org/openjdk/jmh/X.run;b/jmh_generated/B_marshall_jmhTest.marshall_sample_jmhStub"

    class SerdeAbChecksTest(unittest.TestCase):
        def test_resources_whenJarsIdentical_passes(self):
            with tempfile.TemporaryDirectory() as d:
                a = jar(os.path.join(d, "a.jar"), resource_files())
                b = jar(os.path.join(d, "b.jar"), resource_files())
                status, out = run(["resources", a, b])
            self.assertEqual(status, 0, out)
            self.assertIn("PASS resources: 15 files", out)

        def test_resources_whenPayloadDiffers_failsAndIgnoresOtherModels(self):
            with tempfile.TemporaryDirectory() as d:
                a = jar(os.path.join(d, "a.jar"), resource_files())
                b = jar(os.path.join(d, "b.jar"), resource_files(" "))
                status, out = run(["resources", a, b])
            self.assertEqual(status, 1)
            self.assertEqual(out.count("FAIL differs: serde-tests/"), 5)
            self.assertNotIn("schema-other", out)

        def test_resources_whenTreeAndManifest_comparesCodegenResources(self):
            with tempfile.TemporaryDirectory() as d:
                for rel, data in resource_files().items():
                    write(os.path.join(d, "wt", MODULE_RESOURCES, rel), data)
                manifest_path = os.path.join(d, "a.tsv")
                status, text = run(["manifest", os.path.join(d, "wt")])
                write(manifest_path, text)
                self.assertEqual(run(["resources", manifest_path, os.path.join(d, "wt")])[0], 0)
                os.remove(os.path.join(d, "wt", MODULE_RESOURCES, "codegen-resources/rest-xml/service-2.json"))
                status, out = run(["resources", manifest_path, os.path.join(d, "wt")])
            self.assertEqual(status, 1)
            self.assertIn("no codegen-resources/rest-xml/ files", out)
            self.assertIn("only in", out)

        def test_resources_whenJarLacksSerdeTests_fails(self):
            with tempfile.TemporaryDirectory() as d:
                a = jar(os.path.join(d, "a.jar"), {})
                status, out = run(["resources", a, a])
            self.assertEqual(status, 1)
            self.assertIn("no serde-tests/query/input/ files", out)

        def test_models_whenOnlyDocumentationDiffers_passes(self):
            with tempfile.TemporaryDirectory() as d:
                a = jar(os.path.join(d, "a.jar"))
                b = jar(os.path.join(d, "b.jar"), models={"awsrestjsondataplane": model(
                    [member("Bucket", documentation="other docs")])})
                status, out = run(["models", a, b])
            self.assertEqual(status, 0, out)

        def test_models_whenMemberRemovedOrLocationChanged_listsPaths(self):
            with tempfile.TemporaryDirectory() as d:
                a = jar(os.path.join(d, "a.jar"), models={"awsrestjsondataplane": model(
                    [member("Bucket"), member("ContentLength")])})
                b = jar(os.path.join(d, "b.jar"), models={"awsrestjsondataplane": model(
                    [member("Bucket", location="uri")])})
                status, out = run(["models", a, b])
            self.assertEqual(status, 1)
            self.assertIn("awsrestjsondataplane:shapes.PutObjectRequest.members.ContentLength: only in A", out)
            self.assertIn('members.Bucket.http.location: A="header" B="uri"', out)

        def test_models_whenGreedyLabelOrUnusedNamespaceDiffers_reportsKnownOnlyWhenValuesMatch(self):
            def greedy(flag, uri="/{Bucket}/{Key+}"):
                m = member("Key", location="uri")
                m["http"].update({"greedy": flag, "marshallLocation": "GREEDY_PATH" if flag else "PATH"})
                doc = model([m])
                doc["shapes"]["PutObjectRequest"]["marshaller"] = {"requestUri": uri}
                return doc

            def namespace(uri):
                doc = model([member("Bucket")])
                doc["shapes"]["PutMetricDataRequest"] = {"c2jName": "PutMetricDataRequest", "members": [],
                                                         "xmlNamespace": uri and {"prefix": None, "uri": uri}}
                return doc

            with tempfile.TemporaryDirectory() as d:
                a = jar(os.path.join(d, "a.jar"), models={"awsrestjsondataplane": greedy(True),
                                                          "awsrestxmldataplane": namespace(None)})
                b = jar(os.path.join(d, "b.jar"), models={"awsrestjsondataplane": greedy(False),
                                                          "awsrestxmldataplane": namespace(REST_XML_NAMESPACE)})
                status, out = run(["models", a, b])
                strict, _ = run(["models", a, b, "--strict"])
                c = jar(os.path.join(d, "c.jar"), models={"awsrestjsondataplane": greedy(True, "/{Bucket}/{Key}"),
                                                          "awsrestxmldataplane": namespace("https://other")})
                d_ = jar(os.path.join(d, "d.jar"), models={"awsrestjsondataplane": greedy(False, "/{Bucket}/{Key}"),
                                                           "awsrestxmldataplane": namespace(None)})
                unknown, out2 = run(["models", c, d_])
            self.assertEqual(status, 0, out)
            self.assertEqual(sum(1 for x in out.splitlines() if x.startswith("KNOWN ")), 3)
            self.assertIn("except 3 known differences", out)
            self.assertEqual(strict, 1)
            self.assertEqual(unknown, 1)
            self.assertEqual(sum(1 for x in out2.splitlines() if x.startswith("FAIL ")), 3)

        def test_wire_whenIdentical_passes(self):
            row = "C\tc1\tmarshall\tPOST\thttp://localhost/\tcontent-length=2\t2\tab\n"
            with tempfile.TemporaryDirectory() as d:
                write(os.path.join(d, "a.tsv"), header + row)
                write(os.path.join(d, "b.tsv"), header + row)
                status, out = run(["wire", os.path.join(d, "a.tsv"), os.path.join(d, "b.tsv")])
            self.assertEqual(status, 0, out)
            self.assertIn("PASS wire: 1 rows identical", out)

        def test_wire_whenOnlyGreedyLabelUnexpanded_reportsKnown(self):
            a = "R\tcopy\tmarshall\tPUT\thttp://localhost/b/test-key?x-id=CopyObject\th=1\t0\te3\n"
            b = "R\tcopy\tmarshall\tPUT\thttp://localhost/b/{Key+}?x-id=CopyObject\th=1\t0\te3\n"
            with tempfile.TemporaryDirectory() as d:
                write(os.path.join(d, "a.tsv"), header + a)
                write(os.path.join(d, "b.tsv"), header + b)
                status, out = run(["wire", os.path.join(d, "a.tsv"), os.path.join(d, "b.tsv")])
                strict, _ = run(["wire", os.path.join(d, "a.tsv"), os.path.join(d, "b.tsv"), "--strict"])
            self.assertEqual(status, 0, out)
            self.assertIn("KNOWN R copy: uri", out)
            self.assertEqual(strict, 1)

        def test_wire_whenBodyOrUriOrRowsDiffer_fails(self):
            a = "R\tcopy\tmarshall\tPUT\thttp://localhost/b/test-key\th=1\t0\te3\nR\tonly\tmarshall\tPUT\tu\th\t0\tx\n"
            b = "R\tcopy\tmarshall\tPUT\thttp://localhost/c/{Key+}\th=1\t1\te4\n"
            with tempfile.TemporaryDirectory() as d:
                write(os.path.join(d, "a.tsv"), header + a)
                write(os.path.join(d, "b.tsv"), header + b)
                status, out = run(["wire", os.path.join(d, "a.tsv"), os.path.join(d, "b.tsv")])
            self.assertEqual(status, 1)
            self.assertIn("FAIL R copy: uri", out)
            self.assertIn("body_sha256 A=e3 B=e4", out)
            self.assertIn("FAIL R only: only in A", out)

        def test_path_whenSchemaArmShowsOnlySchemaFrames_passesAndWarnsOnSdkFieldsFromSchema(self):
            lines = [stub + ";b/B.marshall;software/amazon/awssdk/protocols/client/restjson/P.serializeRequest;"
                            "software/amazon/awssdk/codecs/json/W.write_[j] 40\n",
                     stub + ";b/B.marshall;software/amazon/awssdk/core/schema/SdkFieldsFromSchema.fields 2\n",
                     "java/lang/Thread.run;software/amazon/awssdk/protocols/json/internal/marshall/M.m 9\n"]
            with tempfile.TemporaryDirectory() as d:
                write(os.path.join(d, "x", "collapsed-cpu.csv"), "".join(lines))
                status, out = run(["path", "--expect", "schema", d])
                legacy, out2 = run(["path", "--expect", "legacy", d])
            self.assertEqual(status, 0, out)
            self.assertIn("PASS path: schema 40, legacy 0 of 42 benchmark samples", out)
            self.assertIn("WARNING", out)
            self.assertEqual(legacy, 1)
            self.assertIn("no legacy-path hits", out2)

        def test_path_whenLegacyFramesOnSchemaArm_fails(self):
            lines = [stub + ";b/B.marshall;software/amazon/awssdk/protocols/client/C.s 5\n",
                     stub + ";b/B.marshall;software/amazon/awssdk/protocols/xml/internal/marshall/X.m 3\n",
                     stub + ";b/B.marshall;software/amazon/awssdk/protocols/core/AbstractMarshallingRegistry.get 1\n"]
            with tempfile.TemporaryDirectory() as d:
                write(os.path.join(d, "collapsed-cpu.csv"), "".join(lines))
                status, out = run(["path", "--expect", "schema", os.path.join(d, "collapsed-cpu.csv")])
                legacy, out2 = run(["path", "--expect", "legacy", d])
            self.assertEqual(status, 1)
            self.assertIn("4 legacy-path hits on an expected schema arm", out)
            self.assertEqual(legacy, 1)
            self.assertIn("5 schema-path hits", out2)

        def test_classload_whenLegacyArm_requiresLegacyAndForbidsSchemaClasses(self):
            log = ("[0.1s][info][class,load] java.lang.Object source: shared objects file\n"
                   "[0.2s][info][class,load] software.amazon.awssdk.protocols.query.internal.marshall."
                   "QueryProtocolMarshaller source: jar:file:/b.jar!/\n")
            bad = log + "[0.3s][info][class,load] software.amazon.awssdk.codecs.json.JsonCodec source: x\n"
            with tempfile.TemporaryDirectory() as d:
                write(os.path.join(d, "ok.log"), log)
                write(os.path.join(d, "bad.log"), bad)
                ok, out = run(["classload", "--expect", "legacy", os.path.join(d, "ok.log")])
                failed, out2 = run(["classload", "--expect", "legacy", os.path.join(d, "bad.log")])
                schema, _ = run(["classload", "--expect", "schema", os.path.join(d, "ok.log")])
            self.assertEqual(ok, 0, out)
            self.assertIn("PASS classload: legacy 1, schema 0 of 2 loaded classes", out)
            self.assertEqual(failed, 1)
            self.assertIn("unexpected schema class: software.amazon.awssdk.codecs.json.JsonCodec", out2)
            self.assertEqual(schema, 1)

        def test_main_whenUsageWrong_exits2(self):
            self.assertEqual(run(["path", "--expect", "other", "x"])[0], 2)
            self.assertEqual(run(["classload", "--expect", "schema", "/nonexistent.log"])[0], 2)

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(SerdeAbChecksTest)
    return 0 if unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
