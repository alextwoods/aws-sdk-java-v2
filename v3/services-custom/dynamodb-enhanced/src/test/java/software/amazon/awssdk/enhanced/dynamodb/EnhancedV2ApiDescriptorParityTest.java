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

package software.amazon.awssdk.enhanced.dynamodb;

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
 * Every public API of the V2 baseline {@code dynamodb-enhanced} release is shipped by the V3 module with the same
 * fully qualified type name, member name and JVM descriptor (Requirements 16.6, 17.1; the Parity Manifest's
 * {@code source} dimension for {@code api:software.amazon.awssdk.enhanced.dynamodb.*}).
 *
 * <p>The baseline jar ({@code v3.v2Baseline.dir}, copied by the build and kept off the class path) is compared with
 * this module's compiled classes by {@code javap -protected -s}: for each V2 type outside {@code internal} packages
 * (synthetic {@code $n} classes excluded), every public and protected field, constructor and method descriptor must
 * exist on the V3 type. V3 may add members. Type references in descriptors keep their V2 names (for example
 * {@code software.amazon.awssdk.core.SdkBytes}, {@code SdkIterable}, {@code SdkPublisher} and the DynamoDB model
 * types), which the V3 core and generated DynamoDB artifacts ship under the same names.
 */
class EnhancedV2ApiDescriptorParityTest {
    private static final Pattern SYNTHETIC = Pattern.compile(".*\\$[0-9]+\\.class$");

    @Test
    void everyV2PublicMemberDescriptorExistsInV3() throws Exception {
        Path baseline = Path.of(System.getProperty("v3.v2Baseline.dir"))
                            .resolve("dynamodb-enhanced-" + System.getProperty("v3.v2Baseline.version") + ".jar");
        assertThat(baseline).isRegularFile();
        Path v3Classes = Path.of(DynamoDbEnhancedClient.class.getProtectionDomain().getCodeSource().getLocation()
                                                              .toURI());

        List<String> types = new ArrayList<>();
        try (ZipFile jar = new ZipFile(baseline.toFile())) {
            jar.stream().map(ZipEntry::getName)
               .filter(n -> n.endsWith(".class") && !n.contains("/internal/") && !n.endsWith("package-info.class")
                            && !SYNTHETIC.matcher(n).matches())
               .map(n -> n.substring(0, n.length() - ".class".length()).replace('/', '.'))
               .sorted().forEach(types::add);
        }
        assertThat(types).hasSizeGreaterThan(150);

        Map<String, Set<String>> v2 = members(baseline.toString(), types);
        Map<String, Set<String>> v3 = members(v3Classes.toString(), types);
        Map<String, Set<String>> missing = new TreeMap<>();
        int memberCount = 0;
        for (String type : types) {
            Set<String> expected = v2.get(type);
            assertThat(expected).as("javap output for V2 " + type).isNotNull();
            memberCount += expected.size();
            Set<String> actual = v3.get(type);
            Set<String> absent = new TreeSet<>(expected);
            if (actual == null) {
                missing.put(type, Set.of("<type not shipped>"));
                continue;
            }
            absent.removeAll(actual);
            if (!absent.isEmpty()) {
                missing.put(type, absent);
            }
        }
        assertThat(memberCount).isGreaterThan(1000);
        assertThat(missing).isEmpty();
    }

    /** Type name to its {@code name descriptor} members, from one {@code javap} run over {@code classPath}. */
    private static Map<String, Set<String>> members(String classPath, List<String> types) throws IOException {
        ToolProvider javap = ToolProvider.findFirst("javap").orElseThrow();
        Map<String, Set<String>> result = new TreeMap<>();
        // One run per type, so that a type V3 does not ship is reported instead of failing the whole run.
        for (String type : types) {
            StringWriter out = new StringWriter();
            int status = javap.run(new PrintWriter(out), new PrintWriter(new StringWriter()), "-protected", "-s",
                                   "-cp", classPath, type);
            if (status != 0) {
                continue;
            }
            result.put(type, parse(out.toString()));
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
