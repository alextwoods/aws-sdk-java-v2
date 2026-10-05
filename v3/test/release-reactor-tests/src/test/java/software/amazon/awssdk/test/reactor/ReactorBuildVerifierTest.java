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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the verifier against a synthetic two-module reactor so that each violation class is known to be caught. */
class ReactorBuildVerifierTest {
    private static final String CLASS_FILE = "example/Service.class";
    private static final String GENERATED_CLASS_FILE = "example/GeneratedServiceInfo.class";
    private static final String SERVICE_METADATA = "META-INF/aws-sdk-java-v3/services/example/";
    private static final String PROVENANCE = SERVICE_METADATA + "model-provenance.json";
    private static final String MANIFEST = SERVICE_METADATA + "generation-manifest.json";

    @TempDir
    Path v3Root;

    private List<String> log;

    @BeforeEach
    void createReactor() throws IOException {
        pom(v3Root, "<artifactId>root</artifactId><version>3.0.0</version><packaging>pom</packaging>"
                    + "<profiles><profile><id>v3-all</id><modules><module>svc</module></modules></profile></profiles>");
        pom(v3Root.resolve("svc"), "<parent><artifactId>v3-services</artifactId><version>3.0.0</version>"
                                   + "<relativePath/></parent><artifactId>svc</artifactId>");
        Path module = v3Root.resolve("svc");
        writeClass(module.resolve("target/classes").resolve(CLASS_FILE), 65);
        writeClass(module.resolve("target/classes").resolve(GENERATED_CLASS_FILE), 65);
        Files.createDirectories(module.resolve("target/generated-sources/aws-codegen/example"));
        Files.writeString(module.resolve("target/generated-sources/aws-codegen/example/GeneratedServiceInfo.java"), "");
        Path metadata = Files.createDirectories(module.resolve("target/generated-resources/aws-codegen")
                                                      .resolve(SERVICE_METADATA));
        Files.writeString(metadata.resolve("model-provenance.json"), "{}");
        Files.writeString(metadata.resolve("generation-manifest.json"), "{}");
        jar(module.resolve("target/svc-3.0.0.jar"), CLASS_FILE, GENERATED_CLASS_FILE, PROVENANCE, MANIFEST);

        log = new ArrayList<>(List.of(
            "[INFO] --- enforcer:3.6.3:enforce (v3-build-policy) @ root ---",
            "[INFO] --- enforcer:3.6.3:enforce (v3-build-policy) @ svc ---",
            "[INFO] --- aws-codegen:3.0.0:generate (aws-codegen-generate) @ svc ---",
            "[INFO] --- compiler:3.16.0:compile (default-compile) @ svc ---",
            "[INFO] --- surefire:3.6.0:test (default-test) @ svc ---",
            "[INFO] --- jar:3.5.1:jar (default-jar) @ svc ---",
            "[INFO] BUILD SUCCESS"));
    }

    @Test
    void completeBuildHasNoViolations() {
        assertEquals(List.of(), verify());
    }

    @Test
    void reportsFailedBuild() {
        log.remove("[INFO] BUILD SUCCESS");
        assertViolation("BUILD SUCCESS");
    }

    @Test
    void reportsMissingJar() throws IOException {
        Files.delete(v3Root.resolve("svc/target/svc-3.0.0.jar"));
        assertViolation("svc-3.0.0.jar is missing");
    }

    @Test
    void reportsClassMissingFromJar() throws IOException {
        jar(v3Root.resolve("svc/target/svc-3.0.0.jar"), GENERATED_CLASS_FILE);
        assertViolation(CLASS_FILE + " is missing from svc-3.0.0.jar");
    }

    @Test
    void reportsNonRelease21Bytecode() throws IOException {
        writeClass(v3Root.resolve("svc/target/classes").resolve(CLASS_FILE), 52);
        assertViolation("major version 52");
    }

    @Test
    void reportsSkippedTestPhase() {
        log.removeIf(line -> line.contains("surefire"));
        assertViolation("surefire:test (default-test) did not run");
    }

    @Test
    void reportsFailingOrMissingTestReports() throws IOException {
        Path module = v3Root.resolve("svc");
        Files.createDirectories(module.resolve("src/test/java"));
        Files.writeString(module.resolve("src/test/java/ServiceTest.java"), "");
        assertViolation("surefire reported no executed tests");

        Files.createDirectories(module.resolve("target/surefire-reports"));
        Files.writeString(module.resolve("target/surefire-reports/TEST-ServiceTest.xml"),
                          "<testsuite tests=\"2\" failures=\"1\" errors=\"0\" skipped=\"0\"/>");
        assertViolation("reports 1 failing test(s)");
    }

    @Test
    void reportsGenerationAfterCompilation() {
        log.remove("[INFO] --- aws-codegen:3.0.0:generate (aws-codegen-generate) @ svc ---");
        log.add(5, "[INFO] --- aws-codegen:3.0.0:generate (aws-codegen-generate) @ svc ---");
        assertViolation("aws-codegen:generate ran after compilation");
    }

    @Test
    void reportsGeneratedSourceNotCompiled() throws IOException {
        jar(v3Root.resolve("svc/target/svc-3.0.0.jar"), CLASS_FILE);
        Files.delete(v3Root.resolve("svc/target/classes").resolve(GENERATED_CLASS_FILE));
        assertViolation("generated source example/GeneratedServiceInfo.java was not compiled into the jar");
    }

    @Test
    void reportsGeneratedResourceMissingFromJar() throws IOException {
        jar(v3Root.resolve("svc/target/svc-3.0.0.jar"), CLASS_FILE, GENERATED_CLASS_FILE, MANIFEST);
        assertViolation("generated resource " + PROVENANCE + " is missing from the jar");
    }

    @Test
    void reportsMissingModelProvenance() throws IOException {
        Files.delete(v3Root.resolve("svc/target/generated-resources/aws-codegen").resolve(PROVENANCE));
        assertViolation("no generated model-provenance.json under target/generated-resources/aws-codegen");
    }

    @Test
    void reportsModuleMissingFromReactorBuild() {
        log.removeIf(line -> line.endsWith("@ svc ---"));
        assertViolation("svc (svc): enforcer:enforce (v3-build-policy) did not run");
    }

    private List<String> verify() {
        return ReactorBuildVerifier.verify(ReactorInventory.load(v3Root), log);
    }

    private void assertViolation(String fragment) {
        List<String> problems = verify();
        assertTrue(problems.stream().anyMatch(p -> p.contains(fragment)), () -> "expected '" + fragment + "' in "
                                                                                + problems);
    }

    private static void pom(Path directory, String body) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("pom.xml"), "<project><groupId>software.amazon.awssdk</groupId>" + body
                                                        + "</project>");
    }

    private static void writeClass(Path file, int majorVersion) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, (byte) majorVersion});
    }

    private static void jar(Path file, String... entries) throws IOException {
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (String entry : entries) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.closeEntry();
            }
        }
    }
}
