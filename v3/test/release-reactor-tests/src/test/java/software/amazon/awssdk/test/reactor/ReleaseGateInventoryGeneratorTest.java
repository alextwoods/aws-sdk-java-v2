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
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.DeploymentMode;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.ExpectedCase;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.ExpectedSuite;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.Inventory;

/** {@link ReleaseGateInventory} over a synthetic V3 tree. */
class ReleaseGateInventoryGeneratorTest {
    @TempDir
    Path root;

    @BeforeEach
    void syntheticTree() throws IOException {
        write(ReleaseGateInventory.PROTOCOL_IDS, "class ProtocolIds { A = ShapeId.from(\"aws.protocols#restJson1\");"
                                                 + " B = ShapeId.from(\"aws.protocols#restXml\"); }");
        write("protocols/protocol-core/pom.xml", "<project/>");
        write("protocols/rest-json-protocol/pom.xml", "<project/>");
        testClass("protocols/rest-json-protocol", "p.RestJsonProtocolConformanceTest");
        write("protocols/rest-xml-protocol/pom.xml", "<project/>");
        testClass("protocols/rest-xml-protocol", "p.RestXmlProtocolConformanceTest");
        write(ReleaseGateInventory.SERVICE_FLEET, "{\"services\": [{\"name\": \"s3\", \"protocolModules\": "
                                                  + "[\"rest-xml-protocol\"]}, {\"name\": \"lambda\", "
                                                  + "\"protocolModules\": [\"rest-json-protocol\"]}]}");
        write("http-clients/netty-nio-client/pom.xml", "<project/>");
        testClass("http-clients/netty-nio-client", "n.ResponseBodyPublisherTckTest");
        write(ReleaseGateInventory.LAUNCH_EVIDENCE_HOOKS, "# hooks\nNettyAsyncTckTest http-mode:netty-nio-client:async\n"
                                                          + "SdkNumberTest api:x\n");
        write(ReleaseGateInventory.COMPATIBILITY_HOOKS, "ApiShapeTest api:y\n");
        write(ReleaseGateInventory.CLASSIFICATION_RULES, "{\"rules\": [{\"evidenceIds\": [\"BehaviorTest\"]}]}");
        testClass("core/sdk-core", "c.SdkNumberTest");
        testClass("test/codegen-generated-classes-test", "g.ApiShapeTest");
        testClass("test/codegen-generated-classes-test", "g.BehaviorTest");
        write("test/protocol-tests/src/test/java/software/amazon/awssdk/test/protocoltests/support/Conformance.java",
              "class Conformance { static final Duration CASE_TIMEOUT = Duration.ofSeconds(30); }");
        testClass("test/protocol-tests", ReleaseGateInventory.PROTOCOL_CONFORMANCE_SUITE);
        testClass("test/endpoint-tests", ReleaseGateInventory.ENDPOINT_SUITE);
        testClass("test/fleet-differential-tests", ReleaseGateInventory.SCHEMA_FIDELITY_SUITE);
        testClass("test/fleet-differential-tests", "software.amazon.awssdk.test.fleet.WireDifferentialTest");
        testClass("test/dynamic-differential-tests",
                  "software.amazon.awssdk.test.differential.GeneratedDynamicDifferentialMatrixTest");
        testClass("test/fault-injection-tests", "software.amazon.awssdk.test.fault.PipelineFaultMatrixTest");
        testClass("test/concurrency-tests", "x.RetryConcurrencyTest");
        write("test/concurrency-tests/src/test/java/x/support/Helper.java", "class Helper {}");
        write("test/concurrency-tests/src/test/java/x/BaseTest.java", "abstract class BaseTest {}");
        testClass("test/bounded-memory-tests", "b.StreamingBoundedMemoryTest");
        write("test/bounded-memory-tests/pom.xml", "<project><build><plugins><plugin>"
              + "<artifactId>maven-surefire-plugin</artifactId><configuration><excludes>"
              + "<exclude>**/*BoundedMemoryTest.java</exclude></excludes></configuration><executions><execution>"
              + "<configuration><includes><include>**/*BoundedMemoryTest.java</include></includes>"
              + "<forkedProcessTimeoutInSeconds>900</forkedProcessTimeoutInSeconds>"
              + "<reportNameSuffix>bounded-memory</reportNameSuffix></configuration></execution></executions>"
              + "</plugin></plugins></build></project>");
        testClass("test/http-client-tests", "software.amazon.awssdk.http.tck.TransportConcurrencyTest");
        testClass("test/http-client-tests", "software.amazon.awssdk.http.tck.NettyAsyncTckTest");
        testClass("test/module-path-tests", "software.amazon.awssdk.test.modulepath.ModulePathSmokeTest");
        testClass("test/module-path-tests", "software.amazon.awssdk.test.modulepath.NativeImageSmokeTest");
        // A fixture project nested in a module's src tree is not part of the reactor.
        testClass("test/module-path-tests/src/test/fixtures/app", "f.FixtureTest");
    }

    @Test
    void theInventoryIsGeneratedFromEverySource() {
        Inventory inventory = ReleaseGateInventory.generate(root);
        assertEquals(List.of(), inventory.problems());
        assertEquals(List.of("aws.protocols#restJson1", "aws.protocols#restXml"), inventory.launchProtocols());
        assertEquals(List.of("s3", "lambda"), inventory.services());
        assertEquals(List.of("netty-nio-client"), inventory.transports());
        assertEquals(18, inventory.suites().size(), inventory.suites().stream().map(s -> s.className()).toList()
                                                              ::toString);
        assertTrue(inventory.suite("f.FixtureTest").isEmpty());
        assertTrue(inventory.suite("x.BaseTest").isEmpty());

        ExpectedSuite fidelity = inventory.suite(ReleaseGateInventory.SCHEMA_FIDELITY_SUITE).orElseThrow();
        assertEquals(List.of(new ExpectedCase("everyGeneratedSchemaMatchesItsCanonicalShape()[1]", "service 1 of 2"),
                             new ExpectedCase("everyGeneratedSchemaMatchesItsCanonicalShape()[2]", "service 2 of 2")),
                     fidelity.cases());
        assertEquals(2, inventory.suite(ReleaseGateInventory.ENDPOINT_SUITE).orElseThrow().cases().size());

        ExpectedSuite conformance = inventory.suite(ReleaseGateInventory.PROTOCOL_CONFORMANCE_SUITE).orElseThrow();
        assertTrue(conformance.sources().contains("protocol:aws.protocols#restXml"), conformance.sources()::toString);
        assertEquals(Duration.ofSeconds(30), conformance.caseTimeout());
        assertTrue(inventory.suite("p.RestXmlProtocolConformanceTest").orElseThrow().sources()
                            .contains("protocol-module:rest-xml-protocol"));

        assertTrue(inventory.suite("c.SdkNumberTest").orElseThrow().sources().contains("manifest-hook:SdkNumberTest"));
        assertTrue(inventory.suite("g.BehaviorTest").isPresent());
        assertTrue(inventory.suite("g.ApiShapeTest").isPresent());
        assertTrue(inventory.suite("software.amazon.awssdk.http.tck.NettyAsyncTckTest").orElseThrow().sources()
                            .contains("manifest-entry:http-mode:netty-nio-client:async"));
        assertTrue(inventory.suite("n.ResponseBodyPublisherTckTest").orElseThrow().sources()
                            .contains("transport:netty-nio-client"));

        ExpectedSuite bounded = inventory.suite("b.StreamingBoundedMemoryTest").orElseThrow();
        assertEquals("bounded-memory", bounded.suite().reportNameSuffix());
        assertEquals(Duration.ofSeconds(900), bounded.suiteTimeout());
        assertEquals(ReleaseGateInventory.DEFAULT_SUITE_TIMEOUT,
                     inventory.suite("x.RetryConcurrencyTest").orElseThrow().suiteTimeout());

        assertEquals(DeploymentMode.NATIVE_IMAGE, inventory.suite("software.amazon.awssdk.test.modulepath."
                                                                  + "NativeImageSmokeTest").orElseThrow().mode());
        assertEquals(DeploymentMode.MODULE_PATH, inventory.suite("software.amazon.awssdk.test.modulepath."
                                                                 + "ModulePathSmokeTest").orElseThrow().mode());
    }

    @Test
    void inconsistentSourcesAreProblems() throws IOException {
        write("http-clients/apache-client/pom.xml", "<project/>");
        write("protocols/aws-query-protocol/pom.xml", "<project/>");
        write(ReleaseGateInventory.CLASSIFICATION_RULES, "{\"rules\": [{\"evidenceIds\": [\"GoneTest\"]}]}");
        Files.delete(root.resolve("test/concurrency-tests/src/test/java/x/RetryConcurrencyTest.java"));
        List<String> problems = ReleaseGateInventory.generate(root).problems();
        assertEquals(4, problems.size(), problems::toString);
        assertTrue(problems.stream().anyMatch(p -> p.contains("http-clients/apache-client")), problems::toString);
        assertTrue(problems.stream().anyMatch(p -> p.contains("protocols/aws-query-protocol")), problems::toString);
        assertTrue(problems.stream().anyMatch(p -> p.contains("GoneTest")), problems::toString);
        assertTrue(problems.stream().anyMatch(p -> p.contains("test/concurrency-tests")), problems::toString);
    }

    private void testClass(String module, String className) throws IOException {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        String pkg = className.substring(0, className.lastIndexOf('.'));
        write(module + "/src/test/java/" + className.replace('.', '/') + ".java",
              "package " + pkg + ";\nclass " + simpleName + " {}\n");
    }

    private void write(String relative, String content) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
