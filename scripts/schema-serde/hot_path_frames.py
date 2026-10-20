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
"""Checks async-profiler collapsed stacks of the schema-serde JMH benchmarks for hot-path violations (task 9.18).

Requirement 34.6 / design D.3: once a schema is initialized, the serde hot path makes no java.lang.reflect calls
and no trait or codec lookups keyed by ShapeId or Class. scripts/schema-serde/hot_path_check.sh runs the benchmarks
under JMH's `-prof async` (which profiles measurement iterations only, so warm-up is excluded) with collapsed
output and passes the files here. Python 3 standard library only.

Usage:
  hot_path_frames.py FILE...      prints each violation with its sample count; exits 1 if any were found
  hot_path_frames.py --self-test  runs the fixture tests below

Only frames above the JMH stub (`..._jmhStub`) are examined: JMH itself calls the stub through Method.invoke once
per iteration, and other threads (compiler, GC, JMH control) are not the hot path. Rules:
  reflect          any frame in java.lang.reflect or jdk.internal.reflect
  shapeid-lookup   ShapeId.hashCode/equals called under a HashMap/ConcurrentHashMap lookup
  class-lookup     ClassValue.get, or Class.hashCode/Object.hashCode/System.identityHashCode called under a map lookup
  lambda-alloc     (warning only, allocation profiles) a `$$Lambda` instance allocated on the hot path
Limitation: identity hash codes are intrinsics, so a Class-keyed HashMap.get can leave no hash frame; the 0 B/op
assertions of TraitLookupBenchmark/SchemaExtensionBenchmark (task 9.16) cover the trait and extension paths too.
"""
import re
import sys

SHAPE_ID = "software.amazon.awssdk.schema.ShapeId"
MAP_LOOKUP = re.compile(r"^java\.util\.(concurrent\.ConcurrentHashMap|HashMap|LinkedHashMap|Hashtable)"
                        r"\.(get|getNode|getOrDefault|containsKey|computeIfAbsent|hash)$")
REFLECT = re.compile(r"^(java\.lang\.reflect|jdk\.internal\.reflect)\.")
SHAPE_ID_HASH = re.compile(r"^" + re.escape(SHAPE_ID) + r"\.(hashCode|equals)$")
CLASS_HASH = re.compile(r"^(java\.lang\.Class\.hashCode|java\.lang\.Object\.hashCode|java\.lang\.System\.identityHashCode)$")
CLASS_VALUE = re.compile(r"^java\.lang\.ClassValue\.")
LAMBDA = re.compile(r"\$\$Lambda")
FAILING = ("reflect", "shapeid-lookup", "class-lookup")


def normalize(frame):
    """async-profiler writes java/util/HashMap.get and may append _[j], _[i], _[k] (JIT, inlined, kernel)."""
    frame = re.sub(r"_\[[a-z0-9]\]$", "", frame.strip())
    return frame.replace("/", ".")


def parse_line(line):
    """'a;b;c 12' -> (['a', 'b', 'c'], 12); None for blank or malformed lines."""
    line = line.rstrip("\n")
    stack, _, count = line.rpartition(" ")
    if not stack or not count.isdigit():
        return None
    return [normalize(f) for f in stack.split(";")], int(count)


def hot_frames(frames):
    """Frames above the last JMH stub frame, or None if this is not a benchmark-thread sample."""
    stub = None
    for i, f in enumerate(frames):
        if "_jmhStub" in f:
            stub = i
    return None if stub is None else frames[stub + 1:]


def classify(frames):
    """The set of rules one sample violates."""
    hot = hot_frames(frames)
    if hot is None:
        return set()
    found = set()
    under_lookup = False
    for f in hot:
        if REFLECT.match(f):
            found.add("reflect")
        if CLASS_VALUE.match(f):
            found.add("class-lookup")
        if MAP_LOOKUP.match(f):
            under_lookup = True
            continue
        if under_lookup and SHAPE_ID_HASH.match(f):
            found.add("shapeid-lookup")
        if under_lookup and CLASS_HASH.match(f):
            found.add("class-lookup")
    if hot and LAMBDA.search(hot[-1]):
        found.add("lambda-alloc")
    return found


def scan(lines):
    """{rule: [(count, stack)]} over collapsed lines."""
    out = {}
    for line in lines:
        parsed = parse_line(line)
        if parsed is None:
            continue
        frames, count = parsed
        for rule in classify(frames):
            out.setdefault(rule, []).append((count, ";".join(hot_frames(frames))))
    return out


def main(argv):
    if argv[:1] == ["--self-test"]:
        return self_test()
    if not argv:
        raise SystemExit(__doc__)
    failed = False
    for path in argv:
        with open(path) as f:
            found = scan(f)
        total = sum(p[1] for p in (parse_line(x) for x in open(path)) if p)
        for rule in sorted(found):
            samples = sum(c for c, _ in found[rule])
            level = "VIOLATION" if rule in FAILING else "WARNING"
            failed |= rule in FAILING
            print(f"{level} {rule}: {samples} of {total} samples in {path}")
            for count, stack in sorted(found[rule], reverse=True)[:3]:
                print(f"    {count:>6}  ...;{';'.join(stack.split(';')[-6:])}")
        if not found:
            print(f"OK: no hot-path violations in {total} samples of {path}")
    return 1 if failed else 0


def self_test():
    import unittest

    stub = "java/lang/reflect/Method.invoke;org/openjdk/jmh/X.run;bench/jmh_generated/B_m_jmhTest.m_avgt_jmhStub"

    class HotPathFramesTest(unittest.TestCase):
        def test_classify_whenReflectionOnlyBelowJmhStub_ignoresIt(self):
            self.assertEqual(classify(parse_line(stub + ";a/B.serialize 5")[0]), set())

        def test_classify_whenReflectionAboveStub_flagsReflect(self):
            frames = parse_line(stub + ";a/B.serialize;java/lang/reflect/Field.get_[j] 3")[0]
            self.assertEqual(classify(frames), {"reflect"})
            frames = parse_line(stub + ";jdk/internal/reflect/DirectMethodHandleAccessor.invoke 1")[0]
            self.assertEqual(classify(frames), {"reflect"})

        def test_classify_whenShapeIdHashedUnderHashMapGet_flagsShapeIdLookup(self):
            line = stub + ";a/Codec.write;java/util/HashMap.get;java/util/HashMap.hash;" \
                          "software/amazon/awssdk/schema/ShapeId.hashCode_[i] 7"
            self.assertEqual(classify(parse_line(line)[0]), {"shapeid-lookup"})
            # ShapeId.hashCode outside a map lookup (e.g. building a schema) is not a lookup.
            line = stub + ";a/Codec.write;software/amazon/awssdk/schema/ShapeId.hashCode 7"
            self.assertEqual(classify(parse_line(line)[0]), set())

        def test_classify_whenClassKeyedLookup_flagsClassLookup(self):
            line = stub + ";a/Codec.write;java/lang/ClassValue.get 2"
            self.assertEqual(classify(parse_line(line)[0]), {"class-lookup"})
            line = stub + ";a/T.getTrait;java/util/concurrent/ConcurrentHashMap.get;java/lang/Object.hashCode 2"
            self.assertEqual(classify(parse_line(line)[0]), {"class-lookup"})

        def test_classify_whenNotBenchmarkThread_ignoresIt(self):
            self.assertEqual(classify(parse_line("java/lang/Thread.run;java/lang/reflect/Method.invoke 9")[0]),
                             set())

        def test_scan_whenLambdaAllocated_warnsAndMainPassesOnlyOnWarnings(self):
            found = scan([stub + ";a/B.write;a/B$$Lambda.0x123 4\n", "\n", stub + ";a/B.write 10\n"])
            self.assertEqual(list(found), ["lambda-alloc"])
            self.assertEqual(found["lambda-alloc"][0][0], 4)

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(HotPathFramesTest)
    return 0 if unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
