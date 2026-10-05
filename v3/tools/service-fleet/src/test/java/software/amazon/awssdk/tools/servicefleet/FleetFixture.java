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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A miniature V2 checkout (with a V3 tree) and canonical aws-models checkout:
 *
 * <ul>
 *   <li>{@code alpha}: one service, serviceId {@code Alpha Service}, modeled in aws-models' {@code alpha-dir};</li>
 *   <li>{@code bundle}: V2 bundles {@code Bundle} and, in the {@code streams} sub-package with a shared model and a
 *       renamed base exception, {@code Bundle Streams};</li>
 *   <li>{@code ghost}: serviceId {@code Ghost}, which no canonical model has;</li>
 *   <li>aws-models' {@code extra}: a model of a service the baseline does not have.</li>
 * </ul>
 */
final class FleetFixture {
    static final String REVISION = "1111111111111111111111111111111111111111";

    final Path repository;
    final Path v3;
    final Path models;

    FleetFixture(Path root) throws IOException {
        repository = root.resolve("repo");
        v3 = repository.resolve("v3");
        models = root.resolve("aws-models");
        write(repository.resolve("services/pom.xml"),
              "<project><modules><module>alpha</module><module>bundle</module><module>ghost</module></modules>"
              + "</project>");
        c2j("alpha", "", "Alpha Service", null);
        c2j("bundle", "bundle/", "Bundle", "{\"sdkModeledExceptionBaseClassName\": \"BundleException\"}");
        c2j("bundle", "bundlestreams/", "Bundle Streams",
            "{\"shareModelConfig\": {\"shareModelWith\": \"bundle\", \"packageName\": \"streams\"},"
            + " \"sdkModeledExceptionBaseClassName\": \"BundleException\"}");
        c2j("ghost", "", "Ghost", "{\"skipSyncClientGeneration\": true}");
        model("alpha-dir", "com.amazonaws.alpha#AlphaService", "Alpha Service", "aws.protocols#awsJson1_0");
        model("bundle", "com.amazonaws.bundle#Bundle_20120810", "Bundle", "aws.protocols#awsJson1_0");
        model("bundle-streams", "com.amazonaws.bundlestreams#Streams_20120810", "Bundle Streams",
              "smithy.protocols#rpcv2Cbor\": {}, \"aws.protocols#awsJson1_0");
        model("extra", "com.amazonaws.extra#Extra", "Extra", "aws.protocols#restJson1");
        write(v3.resolve("services/pom.xml"), """
            <project>
                <modules>
                    <module>hand-written</module>
                    <!-- service-fleet:begin (generated from services/service-fleet.json; do not edit) -->
                    <!-- service-fleet:end -->
                </modules>
                <properties>
                    <aws.models.revision>0000000000000000000000000000000000000000</aws.models.revision>
                </properties>
            </project>
            """);
        write(v3.resolve("bom/pom.xml"), """
            <project>
                <dependencyManagement>
                    <dependencies>
                        <!-- service-fleet:begin (generated from services/service-fleet.json; do not edit) -->
                        <!-- service-fleet:end -->
                    </dependencies>
                </dependencyManagement>
            </project>
            """);
        write(v3.resolve("services/hand-written/pom.xml"), "<project><artifactId>hand-written</artifactId></project>");
    }

    List<V2Service> baseline() {
        return V2Service.scan(repository);
    }

    FleetPlan plan() {
        return ServiceFleetPlanner.plan(baseline(), CanonicalModel.scan(models), REVISION);
    }

    private void c2j(String module, String subdirectory, String serviceId, String customization) throws IOException {
        Path directory = repository.resolve("services").resolve(module)
                                   .resolve("src/main/resources/codegen-resources").resolve(subdirectory);
        write(directory.resolve("service-2.json"),
              "{\"metadata\": {\"protocol\": \"json\", \"serviceId\": \"" + serviceId + "\"}, \"operations\": {}}");
        if (customization != null) {
            write(directory.resolve("customization.config"), customization);
        }
    }

    private void model(String directory, String service, String sdkId, String protocol) throws IOException {
        write(models.resolve(directory).resolve("smithy/model.json"),
              "{\"smithy\": \"2.0\", \"shapes\": {\"" + service + "\": {\"type\": \"service\", \"version\": \"1\", "
              + "\"traits\": {\"aws.api#service\": {\"sdkId\": \"" + sdkId + "\"}, \"" + protocol + "\": {}}}}}\n");
    }

    static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
