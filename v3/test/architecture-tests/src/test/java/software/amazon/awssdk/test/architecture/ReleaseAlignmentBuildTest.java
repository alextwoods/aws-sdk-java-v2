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

package software.amazon.awssdk.test.architecture;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Requirement 15.10: a build whose dependency tree contains V3-group artifacts at any version other than the exact
 * release version fails in the validate phase, and the diagnostic identifies every conflicting module and version,
 * direct and transitive. Forks Maven on a generated fixture reactor that inherits the real V3 parent POM; every
 * fixture artifact is a reactor module, so no remote artifacts are needed.
 */
class ReleaseAlignmentBuildTest {

    private static final String GROUP = "software.amazon.awssdk";

    @Test
    void misalignedDependenciesFailTheBuildWithEveryConflictListed() throws Exception {
        String release = System.getProperty("v3.releaseVersion");
        Path v3Root = Path.of(System.getProperty("v3.root")).toAbsolutePath().normalize();
        Path fixture = Path.of(System.getProperty("v3.architectureTests.workDirectory"), "release-alignment-fixture")
                           .toAbsolutePath().normalize();
        MavenFork.deleteRecursively(fixture);
        Files.createDirectories(fixture);

        // Maven resolves <relativePath> against the project directory, so it must be relative.
        String parentPath = fixture.relativize(v3Root.resolve("parent/pom.xml")).toString().replace('\\', '/');
        MavenFork.write(fixture.resolve("pom.xml"), project(
            "<parent><groupId>" + GROUP + "</groupId><artifactId>aws-sdk-java-v3-parent</artifactId>"
            + "<version>" + release + "</version><relativePath>" + parentPath + "</relativePath></parent>"
            + "<artifactId>release-alignment-fixture</artifactId><packaging>pom</packaging><modules>"
            + "<module>v3-transitive</module><module>v2-core</module><module>sdk-core-other</module>"
            + "<module>aligned</module><module>misaligned-client</module><module>aligned-client</module>"
            + "</modules>"));
        // Stand-alone (parentless) artifacts outside the release version: a V2-style artifact that transitively
        // brings a V3 artifact at another 3.x version.
        MavenFork.write(fixture.resolve("v3-transitive/pom.xml"), standalone("fixture-v3-transitive", "3.0.2", ""));
        MavenFork.write(fixture.resolve("v2-core/pom.xml"),
              standalone("fixture-v2-core", "2.99.0", dependency("fixture-v3-transitive", "3.0.2")));
        MavenFork.write(fixture.resolve("sdk-core-other/pom.xml"), child("fixture-sdk-core", "3.0.1", ""));
        MavenFork.write(fixture.resolve("aligned/pom.xml"), child("fixture-aligned", release, ""));
        MavenFork.write(fixture.resolve("misaligned-client/pom.xml"),
              child("fixture-misaligned-client", release,
                    dependency("fixture-sdk-core", "3.0.1") + dependency("fixture-v2-core", "2.99.0")
                    + dependency("fixture-aligned", release)));
        MavenFork.write(fixture.resolve("aligned-client/pom.xml"),
              child("fixture-aligned-client", release, dependency("fixture-aligned", release)));

        MavenFork.Result result = MavenFork.run(fixture, "validate");

        assertNotEquals(0, result.exitCode(), result.output());
        String out = result.output();
        assertTrue(out.contains("Incompatible AWS SDK for Java runtime versions in " + GROUP
                                + ":fixture-misaligned-client:" + release), out);
        assertBanned(out, "fixture-sdk-core", "3.0.1");
        assertBanned(out, "fixture-v2-core", "2.99.0");
        assertBanned(out, "fixture-v3-transitive", "3.0.2");
        assertTrue(!Pattern.compile(Pattern.quote(GROUP + ":fixture-aligned:pom:" + release) + " <--- banned")
                          .matcher(out).find(), out);
        assertTrue(Pattern.compile("fixture-aligned-client " + Pattern.quote(release) + " \\.+ SUCCESS")
                          .matcher(out).find(), "The aligned module must pass:\n" + out);
    }

    private static void assertBanned(String output, String artifactId, String version) {
        String banned = GROUP + ":" + artifactId + ":pom:" + version + " <--- banned via the exclude/include list";
        assertTrue(output.contains(banned), "Missing '" + banned + "' in:\n" + output);
    }

    private static String project(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
               + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion>"
               + body + "</project>\n";
    }

    private static String standalone(String artifactId, String version, String dependencies) {
        return project("<groupId>" + GROUP + "</groupId><artifactId>" + artifactId + "</artifactId><version>"
                       + version + "</version><packaging>pom</packaging><dependencies>" + dependencies
                       + "</dependencies>");
    }

    private static String child(String artifactId, String version, String dependencies) {
        String release = System.getProperty("v3.releaseVersion");
        return project("<parent><groupId>" + GROUP + "</groupId><artifactId>release-alignment-fixture</artifactId>"
                       + "<version>" + release + "</version></parent><artifactId>" + artifactId + "</artifactId>"
                       + "<version>" + version + "</version><packaging>pom</packaging><dependencies>"
                       + dependencies + "</dependencies>");
    }

    private static String dependency(String artifactId, String version) {
        return "<dependency><groupId>" + GROUP + "</groupId><artifactId>" + artifactId + "</artifactId><version>"
               + version + "</version><type>pom</type></dependency>";
    }
}
