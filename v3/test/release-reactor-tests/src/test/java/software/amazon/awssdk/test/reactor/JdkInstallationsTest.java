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

package software.amazon.awssdk.test.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.test.reactor.JdkInstallations.Jdk;

class JdkInstallationsTest {
    @TempDir
    Path root;

    @Test
    void featureVersionOfLegacyCurrentAndEarlyAccessVersions() {
        assertEquals(8, JdkInstallations.feature("1.8.0_392"));
        assertEquals(17, JdkInstallations.feature("17.0.19"));
        assertEquals(21, JdkInstallations.feature("21"));
        assertEquals(27, JdkInstallations.feature("27-ea"));
    }

    @Test
    void theRunningJdkIsAlwaysDiscovered() {
        JdkInstallations jdks = JdkInstallations.discover(List.of(), Path.of(System.getProperty("java.home")),
                                                          Map.of(), List.of());
        Jdk running = jdks.feature(Runtime.version().feature()).orElseThrow();
        assertEquals(System.getProperty("java.version"), running.version());
        assertTrue(Files.isRegularFile(running.javaExecutable()));
    }

    @Test
    void onePerFeaturePreferringExplicitHomesThenTheNewestScannedUpdate() throws IOException {
        Path explicit21 = jdk("explicit/jdk-21", "21.0.1");
        Path installs = root.resolve("installs");
        jdk("installs/jdk-21.0.5", "21.0.5");
        jdk("installs/jdk-25.0.1", "25.0.1");
        jdk("installs/jdk-25.0.2", "25.0.2");
        jdk("installs/jdk-26-ea", "26-ea");
        Files.createDirectories(installs.resolve("not-a-jdk"));
        Files.createSymbolicLink(installs.resolve("latest"), installs.resolve("jdk-25.0.2"));
        Path macos = jdk("installs/macos.jdk/Contents/Home", "17.0.9");

        JdkInstallations jdks = JdkInstallations.discover(List.of(explicit21), root.resolve("missing"),
                                                          Map.of("JAVA_HOME_11_X64", root.resolve("nope").toString()),
                                                          List.of(installs));
        assertEquals(List.of(17, 21, 25), jdks.all().stream().map(Jdk::feature).toList());
        assertEquals(explicit21.toRealPath(), jdks.feature(21).orElseThrow().home());
        assertEquals("25.0.2", jdks.feature(25).orElseThrow().version());
        assertEquals(macos.toRealPath(), jdks.feature(17).orElseThrow().home());
        assertEquals(List.of(25), jdks.newerThan(21).stream().map(Jdk::feature).toList());
        assertEquals(List.of(17), jdks.olderThan(21, 8).stream().map(Jdk::feature).toList());
    }

    @Test
    void earlyAccessBuildsAreUsedOnlyWhenListedExplicitly() throws IOException {
        Path ea = jdk("ea/jdk-27", "27-ea");
        JdkInstallations jdks = JdkInstallations.discover(List.of(ea), root.resolve("missing"), Map.of(), List.of());
        assertTrue(jdks.feature(27).orElseThrow().earlyAccess());
    }

    @Test
    void anExplicitHomeThatIsNotAJdkIsAConfigurationError() {
        assertThrows(IllegalArgumentException.class,
                     () -> JdkInstallations.discover(List.of(root.resolve("nothing")), root, Map.of(), List.of()));
    }

    private Path jdk(String path, String version) throws IOException {
        Path home = root.resolve(path);
        Files.createDirectories(home.resolve("bin"));
        String suffix = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
        Files.writeString(home.resolve("bin/java" + suffix), "");
        Files.writeString(home.resolve("bin/javac" + suffix), "");
        Files.writeString(home.resolve("release"), "IMPLEMENTOR=\"Example\"\nJAVA_VERSION=\"" + version + "\"\n");
        return home;
    }
}
