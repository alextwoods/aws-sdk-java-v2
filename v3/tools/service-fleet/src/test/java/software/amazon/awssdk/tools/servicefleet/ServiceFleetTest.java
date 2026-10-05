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

package software.amazon.awssdk.tools.servicefleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The fleet plan, its rendering and writing, and the fleet gate, over {@link FleetFixture}. */
class ServiceFleetTest {

    @TempDir
    Path temp;

    private FleetFixture fixture;

    @BeforeEach
    void setUp() throws IOException {
        fixture = new FleetFixture(temp);
    }

    @Test
    void v2ServiceNamesFollowDefaultNamingStrategy() {
        Map<String, String> expected = Map.of("DynamoDB Streams", "DynamoDbStreams", "S3", "S3",
                                              "Auto Scaling", "AutoScaling", "ACM PCA", "AcmPca",
                                              "Elastic Load Balancing v2", "ElasticLoadBalancingV2", "SESv2", "SesV2",
                                              "API Gateway", "ApiGateway", "Alpha Service", "Alpha",
                                              "IoT Data Plane", "IotDataPlane", "CloudWatch Logs", "CloudWatchLogs");
        expected.forEach((id, name) -> assertEquals(name, new V2Service("m", null, id, true, false, null)
                                                              .serviceName(), id));
    }

    @Test
    void theBaselineIsEveryServiceOfEveryV2ModuleWithItsV2ClientTypes() {
        Map<String, V2Service> baseline = fixture.baseline().stream()
                                                 .collect(Collectors.toMap(V2Service::name, s -> s));
        assertEquals(List.of("alpha", "bundle", "bundle.streams", "ghost"), baseline.keySet().stream().sorted().toList());
        String alpha = "software.amazon.awssdk.services.alpha.";
        assertEquals(List.of(alpha + "AlphaAsyncClient", alpha + "AlphaAsyncClientBuilder", alpha + "AlphaClient",
                             alpha + "AlphaClientBuilder", alpha + "model.AlphaException"),
                     baseline.get("alpha").requiredTypes());
        // A renamed base exception is not required under the default name; a shared model package is not either.
        String streams = "software.amazon.awssdk.services.bundle.streams.BundleStreams";
        assertEquals(List.of(streams + "AsyncClient", streams + "AsyncClientBuilder", streams + "Client",
                             streams + "ClientBuilder"), baseline.get("bundle.streams").requiredTypes());
        // skipSyncClientGeneration: V2 has no sync client to preserve.
        assertEquals(List.of("software.amazon.awssdk.services.ghost.GhostAsyncClient",
                             "software.amazon.awssdk.services.ghost.GhostAsyncClientBuilder",
                             "software.amazon.awssdk.services.ghost.model.GhostException"),
                     baseline.get("ghost").requiredTypes());
    }

    @Test
    void servicesMapToTheCanonicalModelWithTheirSdkIdAndUnmappedServicesAreMissing() {
        FleetPlan plan = fixture.plan();
        Map<String, FleetService> services = plan.services().stream()
                                                 .collect(Collectors.toMap(FleetService::name, s -> s));
        assertEquals(List.of("alpha", "bundle", "bundle.streams"), services.keySet().stream().sorted().toList());
        FleetService alpha = services.get("alpha");
        assertEquals("com.amazonaws.alpha#AlphaService", alpha.service());
        assertEquals("alpha-dir", alpha.canonicalDirectory());
        assertEquals("src/main/smithy", alpha.modelDirectory());
        assertEquals(List.of("aws-json-protocol"), alpha.protocolModules());
        FleetService streams = services.get("bundle.streams");
        assertEquals("bundle", streams.module());
        assertEquals("src/main/smithy-streams", streams.modelDirectory());
        assertEquals(List.of("aws-json-protocol", "rpcv2-cbor-protocol"), streams.protocolModules());
        assertEquals(List.of("ghost: no canonical model has sdkId \"Ghost\" at revision " + FleetFixture.REVISION),
                     plan.missing());
        assertEquals(List.of("extra (sdkId Extra, service com.amazonaws.extra#Extra)"),
                     plan.unmappedCanonicalModels());
        assertEquals(List.of("alpha", "bundle"), List.copyOf(plan.modules().keySet()));
        assertEquals(List.of("bundle", "bundle.streams"),
                     plan.modules().get("bundle").stream().map(FleetService::name).toList());
    }

    @Test
    void theLockRoundTripsAndIsDeterministic() {
        FleetPlan plan = fixture.plan();
        String json = ServiceFleetLock.write(plan);
        assertEquals(plan, ServiceFleetLock.read(json));
        assertEquals(json, ServiceFleetLock.write(ServiceFleetLock.read(json)));
    }

    @Test
    void aWrittenFleetPassesTheGateExceptForTheMissingService() throws IOException {
        FleetPlan plan = fixture.plan();
        List<String> changed = ServiceFleetWriter.write(plan, fixture.v3, fixture.models);
        assertTrue(changed.contains("services/bundle/src/main/smithy-streams/model.json"), changed.toString());
        assertEquals(List.of(), ServiceFleetWriter.write(plan, fixture.v3, fixture.models), "writing is idempotent");

        String aggregator = Files.readString(fixture.v3.resolve("services/pom.xml"));
        assertTrue(aggregator.contains("<module>hand-written</module>"), aggregator);
        assertTrue(aggregator.contains("<module>alpha</module>\n        <module>bundle</module>\n"), aggregator);
        assertTrue(aggregator.contains("<aws.models.revision>" + FleetFixture.REVISION + "</aws.models.revision>"));
        String bundle = Files.readString(fixture.v3.resolve("services/bundle/pom.xml"));
        assertTrue(bundle.contains("<id>aws-codegen-generate-streams</id>"), bundle);
        assertTrue(bundle.contains("<modelDirectory>${project.basedir}/src/main/smithy-streams</modelDirectory>"));
        assertTrue(bundle.contains("<artifactId>rpcv2-cbor-protocol</artifactId>"), bundle);
        assertTrue(Files.readString(fixture.v3.resolve("bom/pom.xml")).contains("<artifactId>bundle</artifactId>"));

        ServiceFleetGate.Result result = ServiceFleetGate.check(fixture.repository, fixture.v3, null);
        assertEquals(List.of("MISSING_CLIENT: V2 baseline service ghost (serviceId Ghost) has no generated V3 service "
                             + "module: no canonical model has sdkId \"Ghost\" at revision " + FleetFixture.REVISION),
                     result.problems());
        assertEquals(1, result.notes().size());
    }

    @Test
    void aChangedVendoredModelIsAProvenanceMismatch() throws IOException {
        ServiceFleetWriter.write(fixture.plan(), fixture.v3, fixture.models);
        Path model = fixture.v3.resolve("services/alpha/src/main/smithy/model.json");
        Files.writeString(model, Files.readString(model).replace("\"1\"", "\"2\""));
        Files.writeString(model.resolveSibling("extra.smithy"), "$version: \"2\"\n");
        List<String> problems = gateProblemsWithoutMissing();
        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).startsWith("PROVENANCE_MISMATCH: alpha: the model files under "
                                              + "services/alpha/src/main/smithy are "), problems.get(0));
    }

    @Test
    void aHandEditedModulePomAggregatorOrRevisionFailsTheGate() throws IOException {
        ServiceFleetWriter.write(fixture.plan(), fixture.v3, fixture.models);
        Path pom = fixture.v3.resolve("services/alpha/pom.xml");
        Files.writeString(pom, Files.readString(pom).replace("aws-json-protocol", "rest-json-protocol"));
        Path aggregator = fixture.v3.resolve("services/pom.xml");
        Files.writeString(aggregator, Files.readString(aggregator)
                                           .replace("<module>bundle</module>\n", "")
                                           .replace(FleetFixture.REVISION, "2".repeat(40)));
        List<String> problems = gateProblemsWithoutMissing();
        assertEquals(List.of("PROVENANCE_MISMATCH: services/pom.xml pins canonical model revision " + "2".repeat(40)
                             + ", but services/service-fleet.json was planned from revision " + FleetFixture.REVISION,
                             "STALE_BUILD: services/alpha/pom.xml is not the POM services/service-fleet.json renders",
                             "STALE_BUILD: the module list of services/pom.xml is not the one "
                             + "services/service-fleet.json renders"),
                     problems);
    }

    @Test
    void aDroppedModuleIsAMissingClientAndALeftoverGeneratedModuleIsStale() throws IOException {
        FleetPlan plan = fixture.plan();
        ServiceFleetWriter.write(plan, fixture.v3, fixture.models);
        FleetPlan withoutAlpha = new FleetPlan(plan.revision(), plan.services().stream()
                                                                    .filter(s -> !s.name().equals("alpha")).toList(),
                                               plan.missing(), plan.unmappedCanonicalModels());
        Files.writeString(fixture.v3.resolve(ServiceFleetLock.PATH), ServiceFleetLock.write(withoutAlpha));
        List<String> problems = ServiceFleetGate.check(fixture.repository, fixture.v3, null).problems();
        assertTrue(problems.contains("MISSING_CLIENT: V2 baseline service alpha (serviceId Alpha Service) has no "
                                     + "generated V3 service module: it is not in services/service-fleet.json"),
                   problems.toString());
        assertTrue(problems.contains("STALE_BUILD: services/alpha is a generated service module that "
                                     + "services/service-fleet.json does not list"), problems.toString());
        // Rewriting removes the generated module but never the hand-written one.
        ServiceFleetWriter.write(withoutAlpha, fixture.v3, fixture.models);
        assertFalse(Files.exists(fixture.v3.resolve("services/alpha")));
        assertTrue(Files.exists(fixture.v3.resolve("services/hand-written/pom.xml")));
    }

    @Test
    void theCanonicalCheckoutMustBeAtThePinnedRevisionWithThePinnedFiles() throws IOException {
        git("init", "-q");
        git("add", "-A");
        git("-c", "user.email=fleet@example.com", "-c", "user.name=fleet", "commit", "-q", "-m", "models");
        String head = ServiceFleetGate.git(fixture.models, "rev-parse", "HEAD").trim();
        FleetPlan plan = ServiceFleetPlanner.plan(fixture.baseline(), CanonicalModel.scan(fixture.models), head);
        ServiceFleetWriter.write(plan, fixture.v3, fixture.models);
        assertEquals(List.of(), gateProblemsWithoutMissing(fixture.models));

        Path model = fixture.models.resolve("alpha-dir/smithy/model.json");
        Files.writeString(model, Files.readString(model).replace("\"1\"", "\"2\""));
        List<String> dirty = gateProblemsWithoutMissing(fixture.models);
        assertTrue(dirty.contains("PROVENANCE_MISMATCH: the canonical checkout has local changes in "
                                  + "alpha-dir/smithy/model.json, which revision " + head + " does not contain"),
                   dirty.toString());
        assertTrue(dirty.stream().anyMatch(p -> p.startsWith("PROVENANCE_MISMATCH: alpha is pinned as ")),
                   dirty.toString());

        git("-c", "user.email=fleet@example.com", "-c", "user.name=fleet", "commit", "-q", "-am", "next");
        List<String> moved = gateProblemsWithoutMissing(fixture.models);
        assertEquals(1, moved.size(), moved.toString());
        assertTrue(moved.get(0).startsWith("PROVENANCE_MISMATCH: the canonical checkout " + fixture.models
                                           + " is at revision "), moved.get(0));
    }

    private List<String> gateProblemsWithoutMissing() {
        return gateProblemsWithoutMissing(null);
    }

    private List<String> gateProblemsWithoutMissing(Path models) {
        return ServiceFleetGate.check(fixture.repository, fixture.v3, models).problems().stream()
                               .filter(p -> !p.startsWith("MISSING_CLIENT: V2 baseline service ghost "))
                               .toList();
    }

    private void git(String... args) throws IOException {
        ServiceFleetGate.git(fixture.models, args);
    }
}
