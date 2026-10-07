/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * Every public API of the V2 baseline {@code metrics-spi} release, and V2 {@code sdk-core}'s {@code CoreMetric}, is
 * shipped by {@code telemetry-spi} with the same fully qualified type name, member name and JVM descriptor
 * (Requirements 16.10, 17.1; the Parity Manifest's {@code source} dimension of
 * {@code api:software.amazon.awssdk.metrics.*} and {@code api:software.amazon.awssdk.core.metrics.*}), except the
 * one ADAPTED member: {@code LoggingMetricPublisher.create(org.slf4j.event.Level, Format)}, whose replacement takes
 * the V3 {@code LoggingMetricPublisher.LogLevel} (DEBUG or TRACE).
 *
 * <p>The baseline jars ({@code v3.v2Baseline.dir}, copied by the build and kept off the class path) are compared with
 * this module's compiled classes by {@code javap -protected -s}, as {@code EnhancedV2ApiDescriptorParityTest} does.
 */
class MetricsV2ApiDescriptorParityTest {
    private static final Pattern SYNTHETIC = Pattern.compile(".*\\$[0-9]+\\.class$");
    private static final String LOGGING_PUBLISHER = "software.amazon.awssdk.metrics.LoggingMetricPublisher";
    private static final String FORMAT = "Lsoftware/amazon/awssdk/metrics/LoggingMetricPublisher$Format;";
    /** ADAPTED V2 member to the V3 member that replaces it. */
    private static final Map<String, String> ADAPTED = Map.of(
        "create (Lorg/slf4j/event/Level;" + FORMAT + ")Lsoftware/amazon/awssdk/metrics/LoggingMetricPublisher;",
        "create (Lsoftware/amazon/awssdk/metrics/LoggingMetricPublisher$LogLevel;" + FORMAT
        + ")Lsoftware/amazon/awssdk/metrics/LoggingMetricPublisher;");

    @Test
    void everyV2PublicMemberDescriptorExistsInV3() throws Exception {
        Path dir = Path.of(System.getProperty("v3.v2Baseline.dir"));
        String version = System.getProperty("v3.v2Baseline.version");
        Path metricsSpi = dir.resolve("metrics-spi-" + version + ".jar");
        Path sdkCore = dir.resolve("sdk-core-" + version + ".jar");
        assertThat(metricsSpi).isRegularFile();
        assertThat(sdkCore).isRegularFile();
        Path v3Classes = Path.of(MetricPublisher.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        List<String> metricsTypes = publicTypes(metricsSpi, "software/amazon/awssdk/metrics/");
        assertThat(metricsTypes).contains("software.amazon.awssdk.metrics.MetricPublisher", LOGGING_PUBLISHER,
                                          LOGGING_PUBLISHER + "$Format")
                                .hasSizeGreaterThanOrEqualTo(10);
        List<String> coreTypes = List.of("software.amazon.awssdk.core.metrics.CoreMetric");

        Map<String, Set<String>> v2 = new TreeMap<>(members(metricsSpi.toString(), metricsTypes));
        v2.putAll(members(sdkCore.toString(), coreTypes));
        List<String> types = new ArrayList<>(metricsTypes);
        types.addAll(coreTypes);
        Map<String, Set<String>> v3 = members(v3Classes.toString(), types);

        Map<String, Set<String>> missing = new TreeMap<>();
        int memberCount = 0;
        for (String type : types) {
            Set<String> expected = v2.get(type);
            assertThat(expected).as("javap output for V2 " + type).isNotNull();
            memberCount += expected.size();
            Set<String> actual = v3.get(type);
            if (actual == null) {
                missing.put(type, Set.of("<type not shipped>"));
                continue;
            }
            Set<String> absent = new TreeSet<>(expected);
            absent.removeAll(actual);
            if (type.equals(LOGGING_PUBLISHER)) {
                ADAPTED.forEach((v2Member, replacement) -> {
                    assertThat(expected).as("the ADAPTED V2 member").contains(v2Member);
                    assertThat(actual).as("the replacement of " + v2Member).contains(replacement);
                    absent.remove(v2Member);
                });
            }
            if (!absent.isEmpty()) {
                missing.put(type, absent);
            }
        }
        assertThat(memberCount).isGreaterThan(60);
        assertThat(missing).isEmpty();
    }

    private static List<String> publicTypes(Path jar, String prefix) throws IOException {
        List<String> types = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            zip.stream().map(ZipEntry::getName)
               .filter(n -> n.startsWith(prefix) && n.endsWith(".class") && !n.contains("/internal/")
                            && !n.endsWith("package-info.class") && !SYNTHETIC.matcher(n).matches())
               .map(n -> n.substring(0, n.length() - ".class".length()).replace('/', '.'))
               .sorted().forEach(types::add);
        }
        return types;
    }

    /** Type name to its {@code name descriptor} members, from one {@code javap} run per type over {@code classPath}. */
    private static Map<String, Set<String>> members(String classPath, List<String> types) {
        ToolProvider javap = ToolProvider.findFirst("javap").orElseThrow();
        Map<String, Set<String>> result = new TreeMap<>();
        for (String type : types) {
            StringWriter out = new StringWriter();
            int status = javap.run(new PrintWriter(out), new PrintWriter(new StringWriter()), "-protected", "-s",
                                   "-cp", classPath, type);
            if (status == 0) {
                result.put(type, parse(out.toString()));
            }
        }
        return result;
    }

    private static Set<String> parse(String javapOutput) {
        Set<String> members = new TreeSet<>();
        String[] lines = javapOutput.split("\\R");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].strip();
            if (!line.startsWith("descriptor: ")) {
                continue;
            }
            String declaration = lines[i - 1].strip();
            int paren = declaration.indexOf('(');
            String head = paren >= 0 ? declaration.substring(0, paren) : declaration.replaceAll(";$", "");
            String[] words = head.split("\\s+");
            members.add(words[words.length - 1] + " " + line.substring("descriptor: ".length()));
        }
        return members;
    }
}
