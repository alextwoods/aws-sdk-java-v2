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

package software.amazon.awssdk.tools.paritymanifest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Scans the real V2 baseline (this repository, default bounded API module set) with every registered scanner, and
 * writes the manifest to {@code target/parity-manifest/v2-baseline-manifest.json}. The scan must produce zero
 * problems, contain the anchor entries, and be byte-reproducible.
 */
class V2BaselineScanTest {

    private static final Path OUTPUT = Path.of("target", "parity-manifest", "v2-baseline-manifest.json");

    @Test
    void defaultBaselineScanIsCompleteClassifiedAndReproducible() throws Exception {
        Path root = Path.of("").toAbsolutePath().getParent().getParent().getParent();
        assumeTrue(Files.isRegularFile(root.resolve("core/sdk-core/pom.xml")), "not inside the V2 repository");
        InventorySource.Context probe = new InventorySource.Context(root, baseline(root));
        try {
            PublicApiInventorySource.defaultVersion(PublicApiInventorySource.mavenRepository(probe),
                                                    PublicApiInventorySource.DEFAULT_MODULES,
                                                    probe.baseline().v2Version());
        } catch (IOException e) {
            assumeTrue(false, "released V2 jars are not in the local Maven repository: " + e.getMessage());
        }

        ManifestGenerator generator = new ManifestGenerator(
            ServiceLoader.load(InventorySource.class).stream().map(ServiceLoader.Provider::get).toList(),
            ClassificationRules.loadDefault());
        // generate() throws with every problem if there are any.
        ParityManifest manifest = generator.generate(root, baseline(root), null, ManifestValidator.Options.DEFAULT);
        Files.createDirectories(OUTPUT.getParent());
        ManifestJson.write(manifest, OUTPUT);
        byte[] first = Files.readAllBytes(OUTPUT);
        Path second = OUTPUT.resolveSibling("v2-baseline-manifest.rerun.json");
        ManifestJson.write(generator.generate(root, baseline(root), null, ManifestValidator.Options.DEFAULT), second);
        assertArrayEquals(first, Files.readAllBytes(second), "generation is byte-reproducible");
        Files.delete(second);

        Map<String, ManifestEntry> entries = manifest.entries().stream()
                                                     .collect(Collectors.toMap(ManifestEntry::id, Function.identity()));
        for (String anchor : new String[] {"artifact:software.amazon.awssdk:s3", "service:s3", "service:dynamodb",
                                           "http-mode:netty-nio-client:async", "http-mode:apache-client:sync",
                                           "http-mode:apache5-client:sync", "http-mode:url-connection-client:sync",
                                           "http-mode:aws-crt-client:sync", "http-mode:aws-crt-client:async",
                                           "config:env:AWS_REGION", "config:sysprop:aws.region",
                                           "config:profile:region", "api:software.amazon.awssdk.core.SdkBytes",
                                           "behavior:retry.standard-strategy"}) {
            assertNotNull(entries.get(anchor), anchor);
            assertEquals(Classification.PRESERVED, entries.get(anchor).classification(), anchor);
        }
        assertEquals(Classification.EXCLUDED,
                     entries.get("api:software.amazon.awssdk.core.retry.RetryPolicy").classification());
        assertTrue(entries.keySet().stream().noneMatch(id -> id.contains(".internal.")), "no internal packages");
        assertTrue(entries.keySet().stream().noneMatch(id -> id.startsWith("artifact:software.amazon.awssdk:v3")
                                                            || id.equals("artifact:software.amazon.awssdk:test-utils")),
                   "no V3 or test modules");
    }

    private static BaselineIdentity baseline(Path root) throws IOException {
        return new BaselineIdentity(ParityManifestCli.v2Version(root), ParityManifestCli.gitHead(root), null, null,
                                    "parity-manifest/test");
    }
}
