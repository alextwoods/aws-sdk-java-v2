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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@link BuildAudit} against the checked-in V3 tree (every build) and against synthetic inputs, so that each violation
 * class is known to be caught and mere mentions of Gradle are known not to be.
 */
class BuildAuditTest {
    private static final Path V3_ROOT = Path.of(System.getProperty("v3.root")).toAbsolutePath().normalize();

    @Test
    void v3SourceTreeNeverInvokesGradle() {
        assertEquals(List.of(), BuildAudit.gradleInSourceTree(V3_ROOT));
    }

    @Test
    void everyReleaseReactorModuleRejectsUnsupportedJdksInTheValidatePhase() {
        assertEquals(List.of(), BuildAudit.buildPolicyCoverage(ReactorInventory.load(V3_ROOT)));
    }

    @Nested
    class GradleInSourceTree {
        @TempDir
        Path root;

        @Test
        void mentionsInCommentsDescriptionsAndMessagesAreAllowed() throws IOException {
            write("pom.xml", project("<!-- The V3 build is Maven-only; no Gradle executable (gradle, ./gradlew) is "
                                     + "invoked or required. -->"
                                     + "<description>Maven, not gradle</description>"
                                     + "<properties><v3.note>Gradle-free</v3.note></properties>"
                                     + plugin("maven-enforcer-plugin", "<configuration><rules><bannedDependencies>"
                                              + "<message>do not run gradle</message><excludes>"
                                              + "<exclude>org.gradle:*</exclude></excludes></bannedDependencies>"
                                              + "</rules></configuration>")
                                     + plugin("maven-resources-plugin", "<configuration><resources><resource>"
                                              + "<directory>src/test/resources/gradle/</directory>"
                                              + "<include>build.gradle</include><include>gradle.properties</include>"
                                              + "</resource></resources></configuration>")));
            write("scripts/release.sh", "#!/bin/sh\n# never call gradle or ./gradlew here\nmvn -f v3/pom.xml verify "
                                        + "# not gradle\n");
            write("scripts/release.cmd", "REM never call gradlew.bat\r\n:: nor gradle\r\nmvn verify\r\n");
            write("tools/v2-migration/src/test/resources/project/build.gradle", "plugins { id 'java' }\n");
            write("tools/v2-migration/src/test/resources/project/run.sh", "./gradlew build\n");
            write("module/target/gradlew", "exec gradle \"$@\"\n");
            assertEquals(List.of(), BuildAudit.gradleInSourceTree(root));
        }

        @Test
        void reportsGradleBuildAndWrapperFilesOutsideSources() throws IOException {
            write("build.gradle", "");
            write("module/settings.gradle.kts", "");
            write("gradlew", "");
            write("gradle/wrapper/gradle-wrapper.properties", "");
            assertEquals(List.of("build.gradle: Gradle build file; the V3 build is Maven-only",
                                 "gradle/wrapper/gradle-wrapper.properties: Gradle build file; the V3 build is "
                                 + "Maven-only",
                                 "gradlew: Gradle build file; the V3 build is Maven-only",
                                 "module/settings.gradle.kts: Gradle build file; the V3 build is Maven-only"),
                         BuildAudit.gradleInSourceTree(root));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "<plugin><groupId>org.codehaus.mojo</groupId><artifactId>exec-maven-plugin</artifactId><configuration>"
            + "<executable>gradle</executable></configuration></plugin>",
            "<plugin><groupId>org.codehaus.mojo</groupId><artifactId>exec-maven-plugin</artifactId><configuration>"
            + "<executable>sh</executable><arguments><argument>-c</argument><argument>./gradlew build</argument>"
            + "</arguments></configuration></plugin>",
            "<plugin><artifactId>maven-antrun-plugin</artifactId><executions><execution><configuration><target>"
            + "<exec executable=\"${basedir}/gradlew.bat\"/></target></configuration></execution></executions>"
            + "</plugin>",
            "<plugin><artifactId>maven-antrun-plugin</artifactId><configuration><target><java "
            + "classname=\"org.gradle.wrapper.GradleWrapperMain\"/></target></configuration></plugin>",
            "<plugin><groupId>org.fortasoft</groupId><artifactId>gradle-maven-plugin</artifactId></plugin>"
        })
        void reportsPluginsThatInvokeGradle(String plugin) throws IOException {
            write("pom.xml", project("<build><plugins>" + plugin + "</plugins></build>"));
            assertViolation(BuildAudit.gradleInSourceTree(root), "pom.xml: ");
        }

        @Test
        void reportsGradleExecutablesInPropertiesAndGradleDependencies() throws IOException {
            write("pom.xml", project("<properties><exec.executable>/opt/gradle/bin/gradle</exec.executable>"
                                     + "</properties><dependencies><dependency><groupId>org.gradle</groupId>"
                                     + "<artifactId>gradle-tooling-api</artifactId></dependency></dependencies>"));
            List<String> problems = BuildAudit.gradleInSourceTree(root);
            assertViolation(problems, "<exec.executable> invokes Gradle: /opt/gradle/bin/gradle");
            assertViolation(problems, "dependency org.gradle:gradle-tooling-api launches Gradle");
        }

        @Test
        void reportsGradleInScriptCode() throws IOException {
            write("scripts/build.sh", "#!/bin/sh\nset -e\n./gradlew publish\n");
            write("scripts/build.bat", "@echo off\r\ncall gradlew.bat build\r\n");
            write("Jenkinsfile", "// release\nsh 'gradle build' // run it\n");
            List<String> problems = BuildAudit.gradleInSourceTree(root);
            assertViolation(problems, "scripts/build.sh:3 invokes Gradle");
            assertViolation(problems, "scripts/build.bat:2 invokes Gradle");
            assertViolation(problems, "Jenkinsfile:2 invokes Gradle");
            assertEquals(3, problems.size(), problems::toString);
        }

        private void write(String path, String content) throws IOException {
            Path file = root.resolve(path);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        }
    }

    @Test
    void gradleExecutableTokenIgnoresLongerNamesAndProse() {
        for (String allowed : List.of("Gradle", "build.gradle", "gradle.properties", "gradle-wrapper.jar",
                                      "org.gradle", "src/gradle/x", "no-gradle", "gradlewrapper")) {
            assertTrue(!BuildAudit.GRADLE_EXECUTABLE.matcher(allowed).find(), allowed);
        }
        for (String invocation : List.of("gradle", "gradle build", "./gradlew", "gradlew.bat", "\"gradle\"",
                                         "/opt/gradle/bin/gradle", "sh -c 'gradle'")) {
            assertTrue(BuildAudit.GRADLE_EXECUTABLE.matcher(invocation).find(), invocation);
        }
    }

    @Test
    void gradleInBuildLogFindsGradleOutputAndGradleMojos() {
        List<String> log = List.of(
            "[INFO] --- compiler:3.16.0:compile (default-compile) @ core ---",
            "[INFO] The V3 build does not use Gradle.",
            "Starting a Gradle Daemon (subsequent builds will be faster)",
            "> Task :compileJava",
            "BUILD SUCCESSFUL in 3s",
            "[INFO] --- gradle:1.0:invoke (default) @ core ---");
        assertEquals(4, BuildAudit.gradleInBuildLog(log).size(), BuildAudit.gradleInBuildLog(log)::toString);
    }

    @Test
    void gradleProcessesMatchesExecutablesAndLauncherClassesOnly() {
        List<String> problems = BuildAudit.gradleProcesses(List.of(
            "/usr/bin/java -classpath /tmp/surefire.jar org.apache.maven.surefire.booter.ForkedBooter",
            "/bin/sh /work/gradlew build",
            "/usr/bin/java -cp gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build",
            "/opt/tools/gradle/bin/gradle --version"));
        assertEquals(3, problems.size(), problems::toString);
    }

    @Test
    void stripCommentKeepsShebangsAndCode() {
        assertEquals("#!/usr/bin/env bash", BuildAudit.stripComment("#!/usr/bin/env bash", "sh"));
        assertEquals("", BuildAudit.stripComment("  # gradle", "sh"));
        assertEquals("mvn verify", BuildAudit.stripComment("mvn verify # gradle", "sh"));
        assertEquals("", BuildAudit.stripComment("rem gradle", "bat"));
        assertEquals("sh 'mvn'", BuildAudit.stripComment("sh 'mvn' // gradle", "groovy"));
    }

    @Nested
    class BuildPolicyCoverage {
        @TempDir
        Path root;

        @Test
        void reportsModulesOutsideTheParentAndEnforcerOverrides() throws IOException {
            pom("", "<artifactId>root</artifactId><version>3.0.0</version><packaging>pom</packaging>"
                    + "<parent><artifactId>aws-sdk-java-v3-parent</artifactId></parent>"
                    + "<profiles><profile><id>v3-all</id><modules><module>parent</module><module>ok</module>"
                    + "<module>orphan</module><module>skipping</module><module>late</module></modules></profile>"
                    + "</profiles>");
            pom("parent", "<artifactId>aws-sdk-java-v3-parent</artifactId><version>3.0.0</version>"
                          + "<packaging>pom</packaging>");
            pom("ok", child("ok", ""));
            pom("orphan", "<parent><artifactId>some-other-parent</artifactId><version>1</version></parent>"
                          + "<artifactId>orphan</artifactId>");
            pom("skipping", child("skipping", "<properties><enforcer.skip>true</enforcer.skip></properties>"));
            pom("late", child("late", "<build><plugins><plugin><artifactId>maven-enforcer-plugin</artifactId>"
                                      + "<executions><execution><id>v3-build-policy</id><phase>compile</phase>"
                                      + "</execution></executions></plugin></plugins></build>"));
            List<String> problems = BuildAudit.buildPolicyCoverage(ReactorInventory.load(root));
            assertViolation(problems, "orphan (orphan): parent some-other-parent is not a V3 reactor module");
            assertViolation(problems, "skipping (skipping): sets enforcer.skip=true");
            assertViolation(problems, "late (late): binds v3-build-policy to phase compile instead of validate");
            assertEquals(3, problems.size(), problems::toString);
        }

        private String child(String artifactId, String body) {
            return "<parent><artifactId>aws-sdk-java-v3-parent</artifactId><version>3.0.0</version>"
                   + "<relativePath>../parent/pom.xml</relativePath></parent><artifactId>" + artifactId
                   + "</artifactId>" + body;
        }

        private void pom(String directory, String body) throws IOException {
            Path dir = root.resolve(directory);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("pom.xml"), "<project><groupId>software.amazon.awssdk</groupId>" + body
                                                      + "</project>");
        }
    }

    @Nested
    class RejectedBeforeCompilation {
        @TempDir
        Path checkout;

        private final List<String> log = new ArrayList<>(List.of(
            "[INFO] --- enforcer:3.6.3:enforce (v3-build-policy) @ aws-sdk-java-v3-parent ---",
            "[INFO] BUILD FAILURE",
            "[ERROR] Failed to execute goal org.apache.maven.plugins:maven-enforcer-plugin:3.6.3:enforce "
            + "(v3-build-policy) on project aws-sdk-java-v3-parent: ",
            "[ERROR] The AWS SDK for Java V3 requires JDK 21 or newer (minimum supported JDK: 21). The current JDK is "
            + "17.0.19 at /jdk17."));

        @Test
        void acceptsARejectionInTheBuildPolicyCheck() {
            assertEquals(List.of(), BuildAudit.rejectedBeforeCompilation(log, checkout, 21));
        }

        @Test
        void reportsAWrongMinimumInTheDiagnostic() {
            assertViolation(BuildAudit.rejectedBeforeCompilation(log, checkout, 22), "identifies JDK 22");
        }

        @Test
        void reportsMojosThatRanBeforeTheRejectionAndCompilation() {
            log.add(1, "[INFO] --- aws-codegen:3.0.0-SNAPSHOT:generate (aws-codegen-generate) @ svc ---");
            log.add(2, "[INFO] --- compiler:3.16.0:compile (default-compile) @ svc ---");
            log.add(3, "[INFO] Compiling 2 source files with javac [debug release 21] to target/classes");
            List<String> problems = BuildAudit.rejectedBeforeCompilation(log, checkout, 21);
            assertViolation(problems, "aws-codegen:generate (aws-codegen-generate) ran in svc");
            assertViolation(problems, "compiler:compile (default-compile) ran in svc");
            assertViolation(problems, "shows compilation");
        }

        @Test
        void reportsClassFilesAndBuildOutput() throws IOException {
            Files.createDirectories(checkout.resolve("v3/core/target/classes/a"));
            Files.writeString(checkout.resolve("v3/core/target/classes/a/A.class"), "");
            Files.createDirectories(checkout.resolve("v3/svc/target/generated-sources/aws-codegen"));
            Files.writeString(checkout.resolve("v3/svc/target/generated-sources/aws-codegen/G.java"), "");
            List<String> problems = BuildAudit.rejectedBeforeCompilation(log, checkout, 21);
            assertViolation(problems, "v3/core/target/classes/a/A.class: class file produced by a rejected build");
            assertViolation(problems, "v3/svc/target/generated-sources/aws-codegen/G.java: build output");
        }

        @Test
        void reportsASuccessfulOrDifferentlyFailingBuild() {
            List<String> other = List.of("[INFO] --- compiler:3.16.0:compile (default-compile) @ core ---",
                                         "[INFO] BUILD SUCCESS");
            List<String> problems = BuildAudit.rejectedBeforeCompilation(other, checkout, 21);
            assertViolation(problems, "does not report BUILD FAILURE");
            assertViolation(problems, "did not fail in the enforcer:enforce (v3-build-policy) execution");
        }
    }

    @Nested
    class LifecycleOrdering {
        @TempDir
        Path v3Root;

        private List<String> log;

        @BeforeEach
        void createReactor() throws IOException {
            pom("", "<artifactId>aws-sdk-java-v3-parent</artifactId><version>3.0.0</version><packaging>pom</packaging>"
                    + "<profiles><profile><id>v3-all</id><modules><module>plugin</module><module>services</module>"
                    + "<module>svc</module></modules></profile></profiles>");
            pom("plugin", "<parent><artifactId>aws-sdk-java-v3-parent</artifactId><version>3.0.0</version></parent>"
                          + "<artifactId>aws-codegen-maven-plugin</artifactId><packaging>maven-plugin</packaging>");
            pom("services", "<parent><artifactId>aws-sdk-java-v3-parent</artifactId><version>3.0.0</version>"
                            + "</parent><artifactId>v3-services</artifactId><packaging>pom</packaging><build>"
                            + "<plugins><plugin><groupId>software.amazon.awssdk</groupId>"
                            + "<artifactId>aws-codegen-maven-plugin</artifactId><version>${project.version}"
                            + "</version></plugin></plugins></build>");
            pom("svc", "<parent><artifactId>v3-services</artifactId><version>3.0.0</version>"
                       + "<relativePath>../services/pom.xml</relativePath></parent><artifactId>svc</artifactId>");
            log = new ArrayList<>(List.of(
                mojo("enforcer", "enforce", "v3-build-policy", "aws-sdk-java-v3-parent"),
                mojo("clean", "clean", "default-clean", "aws-codegen-maven-plugin"),
                mojo("enforcer", "enforce", "v3-build-policy", "aws-codegen-maven-plugin"),
                mojo("compiler", "compile", "default-compile", "aws-codegen-maven-plugin"),
                mojo("surefire", "test", "default-test", "aws-codegen-maven-plugin"),
                mojo("jar", "jar", "default-jar", "aws-codegen-maven-plugin"),
                mojo("enforcer", "enforce", "v3-build-policy", "v3-services"),
                mojo("aws-codegen", "generate", "aws-codegen-generate", "v3-services"),
                mojo("enforcer", "enforce", "v3-build-policy", "svc"),
                mojo("aws-codegen", "generate", "aws-codegen-generate", "svc"),
                mojo("compiler", "compile", "default-compile", "svc"),
                mojo("surefire", "test", "default-test", "svc"),
                mojo("jar", "jar", "default-jar", "svc")));
        }

        @Test
        void acceptsAnOrderedBuild() {
            assertEquals(List.of(), ordering());
        }

        @Test
        void reportsGenerationAfterCompilationAndPackagingBeforeTests() {
            log.remove(mojo("aws-codegen", "generate", "aws-codegen-generate", "svc"));
            log.add(mojo("aws-codegen", "generate", "aws-codegen-generate", "svc"));
            log.remove(mojo("jar", "jar", "default-jar", "aws-codegen-maven-plugin"));
            log.add(4, mojo("jar", "jar", "default-jar", "aws-codegen-maven-plugin"));
            List<String> problems = ordering();
            assertViolation(problems, "svc (svc): aws-codegen:generate (aws-codegen-generate) ran after "
                                      + "compiler:compile (default-compile)");
            assertViolation(problems, "plugin (aws-codegen-maven-plugin): surefire:test (default-test) ran after "
                                      + "jar:jar (default-jar)");
        }

        @Test
        void reportsWorkBeforeTheBuildPolicyCheck() {
            log.add(8, mojo("antrun", "run", "v3-runtime-module-version", "svc"));
            assertViolation(ordering(), "svc (svc): antrun:run (v3-runtime-module-version) ran before "
                                        + BuildAudit.BUILD_POLICY_EXECUTION);
        }

        @Test
        void reportsServicesBuiltBeforeTheCodeGeneratorPluginIsPackaged() {
            List<String> plugin = new ArrayList<>(log.subList(1, 6));
            log.removeAll(plugin);
            log.addAll(plugin);
            List<String> problems = ordering();
            assertViolation(problems, "services (v3-services): started before upstream module "
                                      + "aws-codegen-maven-plugin finished");
        }

        @Test
        void reportsMissingAndUnexpectedModules() {
            log.removeIf(line -> line.endsWith("@ svc ---"));
            log.add(mojo("enforcer", "enforce", "v3-build-policy", "stray"));
            List<String> problems = ordering();
            assertViolation(problems, "svc (svc): was not built");
            assertViolation(problems, "stray was built but is not part of the expected reactor");
        }

        private List<String> ordering() {
            return BuildAudit.lifecycleOrdering(ReactorInventory.load(v3Root), log);
        }

        private void pom(String directory, String body) throws IOException {
            Path dir = v3Root.resolve(directory);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("pom.xml"), "<project><groupId>software.amazon.awssdk</groupId>" + body
                                                      + "</project>");
        }

        private static String mojo(String prefix, String goal, String execution, String artifactId) {
            return "[INFO] --- " + prefix + ":1.0:" + goal + " (" + execution + ") @ " + artifactId + " ---";
        }
    }

    private static String project(String body) {
        return "<project><groupId>software.amazon.awssdk</groupId><artifactId>fixture</artifactId>" + body
               + "</project>";
    }

    private static String plugin(String artifactId, String body) {
        return "<build><plugins><plugin><artifactId>" + artifactId + "</artifactId>" + body
               + "</plugin></plugins></build>";
    }

    private static void assertViolation(List<String> problems, String fragment) {
        assertTrue(problems.stream().anyMatch(p -> p.contains(fragment)),
                   () -> "expected '" + fragment + "' in " + problems);
    }
}
