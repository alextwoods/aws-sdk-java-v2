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
"""Compares the effective javac configuration of two effective POMs (Requirement 36.7).

Usage: compiler_config.py <baseline-effective-pom.xml> <feature-effective-pom.xml>

Prints a one-line summary of the compiler options when both POMs configure maven-compiler-plugin the same way, and
exits with status 1 (printing both configurations) when they differ. Source include/exclude lists are not compiler
options and are ignored.
"""
import sys
import xml.etree.ElementTree as ET

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
IGNORED = {"excludes", "includes", "testExcludes", "testIncludes"}


def _local(tag):
    return tag.split("}", 1)[-1]


def _flatten(element, prefix, out):
    children = list(element)
    if not children:
        out.append("%s=%s" % (prefix, (element.text or "").strip()))
        return
    for child in children:
        name = _local(child.tag)
        if name in IGNORED:
            continue
        _flatten(child, "%s.%s" % (prefix, name) if prefix else name, out)


def compiler_config(path):
    root = ET.parse(path).getroot()
    entries = []
    for prop in root.findall("m:properties/*", NS):
        name = _local(prop.tag)
        if name.startswith("maven.compiler."):
            entries.append("%s=%s" % (name, (prop.text or "").strip()))
    for plugin in root.findall("m:build/m:plugins/m:plugin", NS):
        if plugin.findtext("m:artifactId", default="", namespaces=NS) != "maven-compiler-plugin":
            continue
        entries.append("maven-compiler-plugin=%s" % plugin.findtext("m:version", default="", namespaces=NS))
        plugin_level = []
        configuration = plugin.find("m:configuration", NS)
        if configuration is not None:
            _flatten(configuration, "", plugin_level)
        entries.extend(plugin_level)
        for execution in plugin.findall("m:executions/m:execution", NS):
            exec_id = execution.findtext("m:id", default="", namespaces=NS)
            exec_conf = execution.find("m:configuration", NS)
            if exec_conf is None:
                continue
            exec_level = []
            _flatten(exec_conf, "", exec_level)
            # Executions inherit the plugin-level configuration; record only what an execution adds or changes.
            entries.extend("execution[%s].%s" % (exec_id, e) for e in exec_level if e not in plugin_level)
    # The root pom repeats <compilerArgument>; keep the first occurrence of each entry.
    seen = set()
    return [e for e in entries if not (e in seen or seen.add(e))]


def main(argv):
    if len(argv) != 3:
        sys.stderr.write(__doc__)
        return 2
    baseline = compiler_config(argv[1])
    feature = compiler_config(argv[2])
    if baseline != feature:
        sys.stderr.write("Compiler configuration differs.\nBaseline_Build:\n  %s\nFeature build:\n  %s\n"
                         % ("\n  ".join(baseline), "\n  ".join(feature)))
        return 1
    if not any(e.startswith("maven-compiler-plugin=") for e in baseline):
        sys.stderr.write("No maven-compiler-plugin in %s\n" % argv[1])
        return 1
    print("; ".join(baseline))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
