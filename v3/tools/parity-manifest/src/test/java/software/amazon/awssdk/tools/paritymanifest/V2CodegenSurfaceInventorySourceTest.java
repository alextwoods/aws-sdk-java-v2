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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The V2 API that V2 generates from paginator sidecars and customizations, which V3 loses when a canonical model lacks
 * it (the 13.2 hand-off: sidecar-only paginators and V2 member renames). Checked on a synthetic V2 module, and against
 * the released V2 bytecode of services whose customizations use every rename form.
 */
class V2CodegenSurfaceInventorySourceTest {
    private static final String PKG = "software.amazon.awssdk.services.demo";

    @TempDir
    Path temp;

    @Test
    void namesPaginatorsAndRenamesAsV2GeneratesThem() throws Exception {
        Path root = repository();

        Map<String, InventoryItem> items = scan(root, Map.of());

        assertEquals(new java.util.TreeSet<>(List.of(
            "api:" + PKG + ".paginators.ListThingsIterable",
            "api:" + PKG + ".paginators.ListThingsPublisher",
            "api:" + PKG + ".paginators.ListOldThingsIterable",
            "api:" + PKG + ".paginators.ListOldThingsPublisher",
            "api:" + PKG + ".paginators.ListThingsPublisher",
            "api:" + PKG + ".model.ListThingsRequest#stateFiltersAsStrings()Ljava/util/List;",
            "api:" + PKG + ".model.ListThingsResponse#returnValue()Ljava/lang/Boolean;",
            "api:" + PKG + ".model.Thing#awsAccountId()Ljava/lang/String;",
            "api:" + PKG + ".model.Thing#detail()L" + PKG.replace('.', '/') + "/model/DemoDetail;",
            "api:" + PKG + ".model.Thing#stateAsString()Ljava/lang/String;",
            "api:" + PKG + ".model.BadThingException#demoRequestId()Ljava/lang/String;",
            "api:" + PKG + ".model.State#FIVE_ONE:L" + PKG.replace('.', '/') + "/model/State;",
            "api:" + PKG + ".model.DemoDetail",
            "api:software.amazon.awssdk.services.demo.regional.paginators.ListThingsPublisher")),
                     new java.util.TreeSet<>(items.keySet()));
        assertTrue(items.get("api:" + PKG + ".paginators.ListOldThingsIterable").deprecated());
        assertEquals("services/demo/src/main/resources/codegen-resources/demo/paginators-1.json",
                     items.get("api:" + PKG + ".paginators.ListThingsIterable").source().path());
        assertEquals("demo", items.get("api:" + PKG + ".model.DemoDetail").source().artifact());

        // A module whose released jar the public API source scans is inventoried exactly there instead.
        assertEquals(Map.of(), scan(root, Map.of(PublicApiInventorySource.MODULES_OPTION, "demo")));
    }

    @Test
    void sourceDerivedSurfaceIsTheReleasedV2Bytecode() throws Exception {
        Path repository = Path.of("").toAbsolutePath().getParent().getParent().getParent();
        assumeTrue(Files.isDirectory(repository.resolve("services/s3/src/main/resources/codegen-resources")),
                   "not inside the V2 repository");
        String version = System.getProperty("v3.fixture.v2Version");
        Map<String, Path> jars = Map.of(
            "dynamodb", Path.of(System.getProperty("v3.fixture.v2Api"), "dynamodb-" + version + ".jar"),
            "s3", Path.of(System.getProperty("v3.fixture.v2SurfaceApi"), "s3-" + version + ".jar"),
            "cognitoidentityprovider", Path.of(System.getProperty("v3.fixture.v2SurfaceApi"),
                                               "cognitoidentityprovider-" + version + ".jar"));
        List<String> unconfirmed = new ArrayList<>();
        int confirmed = 0;
        Set<String> all = new java.util.TreeSet<>();
        for (Map.Entry<String, Path> module : jars.entrySet()) {
            Set<String> released = PublicApiInventorySource.scanJar(module.getValue(), module.getKey(), "jar").stream()
                                                           .map(InventoryItem::id).collect(Collectors.toSet());
            Path resources = repository.resolve("services").resolve(module.getKey())
                                       .resolve("src/main/resources/codegen-resources");
            for (Path directory : V2CodegenSurfaceInventorySource.serviceDirectories(resources)) {
                for (InventoryItem item : new V2CodegenSurfaceInventorySource.Service(module.getKey(), directory, "x")
                                              .items()) {
                    all.add(item.id());
                    if (released.contains(item.id())) {
                        confirmed++;
                    } else if (!newSinceBaseline(item.id(), released)) {
                        unconfirmed.add(item.id());
                    }
                }
            }
        }
        assertEquals(List.of(), unconfirmed, "source-derived V2 API that the released " + version + " jars lack");
        for (String anchor : List.of(
            "api:software.amazon.awssdk.services.s3.model.CopyObjectRequest#destinationBucket()Ljava/lang/String;",
            "api:software.amazon.awssdk.services.s3.model.S3Object",
            "api:software.amazon.awssdk.services.s3.paginators.ListObjectVersionsIterable",
            "api:software.amazon.awssdk.services.dynamodb.model.AttributeValue#nul()Ljava/lang/Boolean;",
            "api:software.amazon.awssdk.services.dynamodb.paginators.BatchGetItemPublisher",
            "api:software.amazon.awssdk.services.cognitoidentityprovider.model.AccountTakeoverActionType"
            + "#sendNotification()Ljava/lang/Boolean;")) {
            assertTrue(all.contains(anchor), anchor);
        }
        // 55 at 2.54.0: the S3 and DynamoDB paginators and every rename form.
        assertTrue(confirmed >= 50, "confirmed " + confirmed);
    }

    /** A paginator of an operation the released jar does not have yet (added to the V2 source since the baseline). */
    private static boolean newSinceBaseline(String id, Set<String> released) {
        int paginators = id.indexOf(".paginators.");
        if (paginators < 0) {
            return false;
        }
        String operation = id.substring(paginators + ".paginators.".length()).replaceAll("(Iterable|Publisher)$", "");
        return released.stream().noneMatch(r -> r.endsWith(".model." + operation + "Request"));
    }

    private static Map<String, InventoryItem> scan(Path root, Map<String, String> options) throws Exception {
        InventorySource.Context context = new InventorySource.Context(root, Entries.BASELINE, options);
        Map<String, InventoryItem> items = new TreeMap<>();
        new V2CodegenSurfaceInventorySource().scan(context).forEach(i -> items.put(i.id(), i));
        return items;
    }

    /** A V2 repository with one module that bundles {@code demo} and, sharing its model, {@code demo.regional}. */
    private Path repository() throws Exception {
        Path root = temp.resolve("v2");
        CandidateArtifacts.write(root, "pom.xml", pom("aws-sdk-java-pom", "<modules><module>services</module></modules>"));
        CandidateArtifacts.write(root, "services/pom.xml", pom("services", "<modules><module>demo</module></modules>"));
        CandidateArtifacts.write(root, "services/demo/pom.xml", pom("demo", ""));
        String resources = "services/demo/src/main/resources/codegen-resources/";
        CandidateArtifacts.write(root, resources + "demo/service-2.json", """
            {"metadata": {"serviceId": "Amazon Demo Service"},
             "operations": {
               "ListThings": {"input": {"shape": "ListThingsInput"}, "output": {"shape": "ListThingsResult"}},
               "ListOldThings": {"input": {"shape": "ListOldThingsInput"}, "deprecated": true},
               "ListHidden": {"input": {"shape": "ListHiddenInput"}},
               "ListBroken": {"input": {"shape": "ListBrokenInput"}}},
             "shapes": {
               "ListThingsInput": {"type": "structure", "members": {"StateFilter": {"shape": "StateList"}}},
               "ListOldThingsInput": {"type": "structure", "members": {}},
               "ListHiddenInput": {"type": "structure", "members": {}},
               "ListBrokenInput": {"type": "structure", "members": {}},
               "ListThingsResult": {"type": "structure", "members": {"Return": {"shape": "Flag"}}},
               "Thing": {"type": "structure", "members": {"State": {"shape": "State"},
                                                          "AWSAccount": {"shape": "Id"},
                                                          "Detail": {"shape": "Detail"},
                                                          "Body": {"shape": "Stream"}}},
               "Detail": {"type": "structure", "members": {}},
               "BadThingFault": {"type": "structure", "exception": true, "members": {"RequestId": {"shape": "Id"}}},
               "StateList": {"type": "list", "member": {"shape": "State"}},
               "State": {"type": "string", "enum": ["on", "5.1"]},
               "Flag": {"type": "boolean"},
               "Id": {"type": "string"},
               "Stream": {"type": "blob", "streaming": true}}}
            """);
        CandidateArtifacts.write(root, resources + "demo/paginators-1.json", """
            {"pagination": {
              "ListThings": {"input_token": "NextToken", "output_token": "NextToken", "result_key": "Things"},
              "ListOldThings": {"input_token": ["A", "B"], "output_token": ["Next.A", "Next.B"]},
              "ListHidden": {"input_token": "NextToken", "output_token": "NextToken"},
              "ListBroken": {"input_token": "NextToken", "output_token": "Things[-1].Id"},
              "NotAnOperation": {"input_token": "NextToken", "output_token": "NextToken"}}}
            """);
        CandidateArtifacts.write(root, resources + "demo/customization.config", """
            {
              // V2 customization files may carry comments.
              "renameShapes": {"Detail": "DemoDetail"},
              "operationModifiers": {"ListHidden": {"exclude": true}},
              "shapeModifiers": {
                "ListThingsInput": {"modify": [{"StateFilter": {"emitPropertyName": "StateFilters"}}]},
                "ListThingsResult": {"modify": [{"Return": {"emitPropertyName": "ReturnValue"}}]},
                "Thing": {"modify": [{"State": {"emitPropertyName": "State"}},
                                     {"AWSAccount": {"emitPropertyName": "AWSAccountId"}},
                                     {"Detail": {"emitPropertyName": "Detail"}},
                                     {"Body": {"emitPropertyName": "Payload"}}]},
                "BadThingFault": {"modify": [{"RequestId": {"emitPropertyName": "demoRequestId"}}]},
                "State": {"modify": [{"5.1": {"emitEnumName": "FIVE_ONE"}}]},
                "Missing": {"modify": [{"X": {"emitPropertyName": "Y"}}]}
              }
            }
            """);
        CandidateArtifacts.write(root, resources + "regional/service-2.json", """
            {"metadata": {"serviceId": "Demo Regional"},
             "operations": {"ListThings": {"input": {"shape": "ListThingsInput"}}},
             "shapes": {"ListThingsInput": {"type": "structure", "members": {}}}}
            """);
        CandidateArtifacts.write(root, resources + "regional/paginators-1.json", """
            {"pagination": {"ListThings": {"input_token": "NextToken", "output_token": "NextToken"}}}
            """);
        CandidateArtifacts.write(root, resources + "regional/customization.config", """
            {"skipSyncClientGeneration": true,
             "shareModelConfig": {"shareModelWith": "demo", "packageName": "regional"}}
            """);
        return root;
    }

    private static String pom(String artifactId, String body) {
        return "<project><groupId>software.amazon.awssdk</groupId><artifactId>" + artifactId + "</artifactId>"
               + body + "</project>";
    }
}
