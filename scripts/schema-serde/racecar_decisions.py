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
"""Writes the racecar decision table (design D.6, Requirement 37.1) from its YAML source.

Usage: racecar_decisions.py [--check | --list-classes] [--yaml PATH] [--markdown PATH]

Reads docs/design/schema-serde/racecar-decisions.yaml, validates it and writes
docs/design/schema-serde/racecar-decisions.md. The YAML file's header describes its format and keys.

  --check         Write nothing; exit with status 1 when the YAML is invalid or the Markdown differs from the
                  generated output.
  --list-classes  Validate, then print "<phase id><TAB><class name>" for every class of an Applied or Superseded
                  entry (RacecarDecisionTableTest loads each one).

Validation: every racecar phase id is present exactly once, each decision is in the D.6 vocabulary, required fields are
non-empty, D2, H1 and H5 are Deferred, and an entry applied in the current phase links an existing A/B record.
"""
import collections
import difflib
import json
import os
import sys

REPO_ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
DEFAULT_YAML = os.path.join(REPO_ROOT, "docs", "design", "schema-serde", "racecar-decisions.yaml")
DEFAULT_MARKDOWN = os.path.join(REPO_ROOT, "docs", "design", "schema-serde", "racecar-decisions.md")

APPLIED = "Applied (re-implemented here)"
SUPERSEDED = "Superseded"
DEFERRED = "Deferred (later individual optimization)"
REJECTED = "Rejected"
DECISIONS = (APPLIED, SUPERSEDED, DEFERRED, REJECTED)

# Every racecar phase id, in the order of the D.6 table. Racecar records no E2.5.
PHASE_IDS = (
    "F", "B1", "A", "E1", "D1", "G1", "G2",
    "E2.1", "E2.2", "E2.3", "E2.4", "E2.6", "E3.1", "E3.2", "E3.3", "E3.3 gate",
    "D2", "B2.a", "C", "B2.b",
    "E4", "E5", "E6", "E7", "E8", "E9", "E10", "E11", "E12", "E13", "E14", "E15", "E16",
    "H1", "H2", "H3", "H4", "H5", "H6", "H7", "H8", "H9", "H10", "H11",
)
# Racecar changes to public behavior; each needs its own API review before it is applied (D.6).
PUBLIC_BEHAVIOR_CHANGES = ("D2", "H1", "H5")
# The phase that re-implements Deferred entries one at a time; an entry applied in it needs its A/B record (task 9.19).
EXPERIMENT_PHASE = 5

REQUIRED = ("id", "optimization", "decision", "component", "reason")
OPTIONAL = ("classes", "applied_in_phase", "ab_record", "task", "d6_decision", "note")


class InvalidDecisions(Exception):
    pass


def parse(path):
    """Reads the YAML subset described in the file's header: a list of flat entries with JSON literal values."""
    entries = []
    errors = []
    with open(path, encoding="utf-8") as f:
        for number, raw in enumerate(f, 1):
            line = raw.rstrip("\n")
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            if line.startswith("- "):
                entries.append(collections.OrderedDict([("_line", number)]))
                body = line[2:]
            elif line.startswith("  ") and not line.startswith("   ") and entries:
                body = line[2:]
            else:
                errors.append("line %d: expected '- key: value' or '  key: value'" % number)
                continue
            key, sep, value = body.partition(": ")
            if not sep or not key or key != key.strip():
                errors.append("line %d: expected 'key: value'" % number)
                continue
            if key not in REQUIRED and key not in OPTIONAL:
                errors.append("line %d: unknown key %r" % (number, key))
                continue
            if key in entries[-1]:
                errors.append("line %d: duplicate key %r" % (number, key))
                continue
            try:
                entries[-1][key] = json.loads(value)
            except ValueError as e:
                errors.append("line %d: %s is not a JSON literal (%s)" % (number, key, e))
    if errors:
        raise InvalidDecisions("\n".join(errors))
    return entries


def _is_text(value):
    return isinstance(value, str) and value.strip() != "" and "\n" not in value


def validate(entries, base_dir):
    errors = []
    seen = collections.Counter()
    for entry in entries:
        where = "line %d (%s)" % (entry["_line"], entry.get("id", "no id"))
        for key in REQUIRED:
            if not _is_text(entry.get(key)):
                errors.append("%s: %s is missing or empty" % (where, key))
        for key in ("task", "note", "ab_record"):
            if key in entry and not _is_text(entry[key]):
                errors.append("%s: %s must be a non-empty one-line string" % (where, key))
        entry_id = entry.get("id")
        if entry_id is not None:
            seen[entry_id] += 1
            if entry_id not in PHASE_IDS:
                errors.append("%s: %r is not a racecar phase id" % (where, entry_id))
        decision = entry.get("decision")
        if decision is not None and decision not in DECISIONS:
            errors.append("%s: decision %r is not one of %s" % (where, decision, ", ".join(DECISIONS)))
        if "d6_decision" in entry:
            if entry["d6_decision"] not in DECISIONS or entry["d6_decision"] == decision:
                errors.append("%s: d6_decision must be a decision other than %r" % (where, decision))
            if "note" not in entry:
                errors.append("%s: a decision that differs from D.6 needs a note" % where)
        if entry_id in PUBLIC_BEHAVIOR_CHANGES and decision != DEFERRED:
            errors.append("%s: %s changes public behavior and must stay %r until its API review"
                          % (where, entry_id, DEFERRED))

        classes = entry.get("classes")
        if decision in (APPLIED, SUPERSEDED):
            if not isinstance(classes, list) or not classes or not all(_is_text(c) for c in classes):
                errors.append("%s: %r entries need a non-empty classes list" % (where, decision))
        elif classes is not None:
            errors.append("%s: classes is only allowed on Applied and Superseded entries" % where)

        phase = entry.get("applied_in_phase")
        if decision == APPLIED:
            if not isinstance(phase, int) or isinstance(phase, bool) or not 2 <= phase <= EXPERIMENT_PHASE:
                errors.append("%s: an Applied entry needs applied_in_phase (2..%d)" % (where, EXPERIMENT_PHASE))
            elif phase == EXPERIMENT_PHASE and "ab_record" not in entry:
                errors.append("%s: applied in phase %d without an ab_record link (task 9.19)"
                              % (where, EXPERIMENT_PHASE))
        elif phase is not None:
            errors.append("%s: applied_in_phase is only allowed on Applied entries" % where)
        record = entry.get("ab_record")
        if _is_text(record):
            target = os.path.normpath(os.path.join(base_dir, record))
            if os.path.isabs(record) or not os.path.isfile(target):
                errors.append("%s: ab_record %r is not a file relative to %s" % (where, record, base_dir))

    for phase_id in PHASE_IDS:
        if seen[phase_id] == 0:
            errors.append("phase id %r has no entry" % phase_id)
        elif seen[phase_id] > 1:
            errors.append("phase id %r has %d entries" % (phase_id, seen[phase_id]))
    if errors:
        raise InvalidDecisions("\n".join(errors))
    by_id = {e["id"]: e for e in entries}
    return [by_id[phase_id] for phase_id in PHASE_IDS]


def _cell(text):
    return text.replace("|", "\\|")


def _decision_cell(entry):
    text = entry["decision"]
    if "ab_record" in entry:
        text += " ([A/B record](%s))" % entry["ab_record"]
    return _cell(text)


def _task_cell(entry):
    task = entry.get("task")
    if entry["decision"] == APPLIED:
        phase = "Phase %d" % entry["applied_in_phase"]
        return _cell("%s (%s)" % (phase, task) if task else phase)
    return _cell(task) if task else "—"


def _sentence(text):
    return text if text.endswith(".") else text + "."


def render(entries):
    counts = collections.Counter(e["decision"] for e in entries)
    out = [
        "<!-- Generated by scripts/schema-serde/racecar_decisions.py from racecar-decisions.yaml. Do not edit. -->",
        "",
        "# Racecar decisions",
        "",
        "One row per racecar phase (design D.6, Requirement 37.1). Racecar (`feature/poc/racecar`) is a reference proof "
        "of concept and is not merged (OD-16); no racecar code is on this branch.",
        "",
        "- **%s**: the concept is written fresh in the new pipeline path." % APPLIED,
        "- **%s**: a different mechanism of this design does the same job." % SUPERSEDED,
        "- **%s**: it can be taken from racecar later as its own change, after a paired A/B measurement (task 9.19) "
        "and, where it changes public behavior, an API review." % DEFERRED,
        "- **%s**: racecar measured no gain, or it cannot ship in a Java 8 release." % REJECTED,
        "",
        "Edit `racecar-decisions.yaml` and regenerate this file with `python3 scripts/schema-serde/racecar_decisions.py`. "
        "`RacecarDecisionTableTest` (`test/architecture-tests`) runs the script's `--check` mode and fails when this "
        "file is out of date or the YAML is invalid.",
        "",
        "## Summary",
        "",
        "| Decision | Rows |",
        "|---|---:|",
    ]
    for decision in DECISIONS:
        out.append("| %s | %d |" % (decision, counts[decision]))
    out += [
        "| Total | %d |" % len(entries),
        "",
        "## Decisions",
        "",
        "| Phase | Optimization | Decision | Component or replacing mechanism | Reason | Task |",
        "|---|---|---|---|---|---|",
    ]
    for e in entries:
        out.append("| %s | %s | %s | %s | %s | %s |" % (
            _cell(e["id"]), _cell(e["optimization"]), _decision_cell(e), _cell(e["component"]), _cell(e["reason"]),
            _task_cell(e)))
    changed = [e for e in entries if "d6_decision" in e]
    out += [
        "",
        "## Differences from the design D.6 table",
        "",
        "Where the implementation differs from D.6, the table above records the implementation. Decisions changed from "
        "D.6:",
        "",
        "| Phase | D.6 decision | Decision |",
        "|---|---|---|",
    ]
    out += ["| %s | %s | %s |" % (_cell(e["id"]), e["d6_decision"], e["decision"]) for e in changed]
    out += ["", "Notes:", ""]
    out += ["- **%s**: %s" % (e["id"], _sentence(e["note"])) for e in entries if "note" in e]
    out += [
        "",
        "## Racecar public-behavior changes",
        "",
        "Each stays Deferred until its own API review. The new path keeps today's behavior.",
        "",
    ]
    by_id = {e["id"]: e for e in entries}
    out += ["- **%s**: %s. %s" % (i, by_id[i]["optimization"], _sentence(by_id[i]["reason"]))
            for i in PUBLIC_BEHAVIOR_CHANGES]
    return "\n".join(out) + "\n"


def main(argv):
    args = argv[1:]
    mode = None
    yaml_path, markdown_path = DEFAULT_YAML, DEFAULT_MARKDOWN
    while args:
        arg = args.pop(0)
        if arg in ("--check", "--list-classes") and mode is None:
            mode = arg
        elif arg in ("--yaml", "--markdown") and args:
            if arg == "--yaml":
                yaml_path = args.pop(0)
            else:
                markdown_path = args.pop(0)
        else:
            sys.stderr.write(__doc__)
            return 2
    try:
        entries = validate(parse(yaml_path), os.path.dirname(os.path.abspath(yaml_path)))
    except InvalidDecisions as e:
        sys.stderr.write("%s is invalid:\n%s\n" % (yaml_path, e))
        return 1

    if mode == "--list-classes":
        for e in entries:
            for name in e.get("classes", ()):
                print("%s\t%s" % (e["id"], name))
        return 0
    expected = render(entries)
    if mode == "--check":
        actual = None
        if os.path.isfile(markdown_path):
            with open(markdown_path, encoding="utf-8", newline="") as f:
                actual = f.read()
        if actual != expected:
            sys.stderr.write("%s is out of date; regenerate it with python3 scripts/schema-serde/racecar_decisions.py\n"
                             % markdown_path)
            sys.stderr.writelines(difflib.unified_diff((actual or "").splitlines(True), expected.splitlines(True),
                                                       "committed", "generated"))
            return 1
        print("%s is up to date (%d phases)" % (markdown_path, len(entries)))
        return 0
    with open(markdown_path, "w", encoding="utf-8", newline="\n") as f:
        f.write(expected)
    print("Wrote %s (%d phases)" % (markdown_path, len(entries)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
