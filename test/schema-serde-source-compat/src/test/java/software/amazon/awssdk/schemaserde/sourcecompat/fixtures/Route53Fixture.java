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

package software.amazon.awssdk.schemaserde.sourcecompat.fixtures;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53AsyncClient;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ChangeInfo;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ChangeStatus;
import software.amazon.awssdk.services.route53.model.DelegationSet;
import software.amazon.awssdk.services.route53.model.GetChangeResponse;
import software.amazon.awssdk.services.route53.model.GetHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.HostedZoneConfig;
import software.amazon.awssdk.services.route53.model.NoSuchHostedZoneException;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;
import software.amazon.awssdk.services.route53.model.Route53Exception;
import software.amazon.awssdk.services.route53.model.VPC;
import software.amazon.awssdk.services.route53.model.VPCRegion;
import software.amazon.awssdk.services.route53.waiters.Route53Waiter;

/**
 * Route 53, written against the Baseline_Build public API (Requirement 16.8).
 */
public final class Route53Fixture {
    public static final String ZONE_ID = "Z0FIXTURE";

    private Route53Fixture() {
    }

    public static Route53Client syncClient(SdkHttpClient httpClient, SdkPlugin plugin,
                                           ClientSettings.CallCounter counter) {
        return Route53Client.builder()
                            .region(Region.AWS_GLOBAL)
                            .credentialsProvider(ClientSettings.credentials())
                            .overrideConfiguration(ClientSettings.overrideConfiguration(counter))
                            .endpointOverride(ClientSettings.ENDPOINT)
                            .httpClient(httpClient)
                            .addPlugin(plugin)
                            .build();
    }

    public static Route53AsyncClient asyncClient(SdkAsyncHttpClient httpClient, SdkPlugin plugin) {
        return Route53AsyncClient.builder()
                                 .region(Region.AWS_GLOBAL)
                                 .credentialsProvider(ClientSettings.credentials())
                                 .endpointOverride(ClientSettings.ENDPOINT)
                                 .httpClient(httpClient)
                                 .addPlugin(plugin)
                                 .build();
    }

    public static ChangeResourceRecordSetsRequest upsertRequest() {
        ResourceRecordSet recordSet = ResourceRecordSet.builder()
                                                       .name("www.example.com.")
                                                       .type(RRType.A)
                                                       .ttl(300L)
                                                       .resourceRecords(ResourceRecord.builder().value("192.0.2.1").build())
                                                       .build();
        return ChangeResourceRecordSetsRequest.builder()
                                              .hostedZoneId(ZONE_ID)
                                              .changeBatch(b -> b.comment("fixture")
                                                                 .changes(Change.builder()
                                                                                .action(ChangeAction.UPSERT)
                                                                                .resourceRecordSet(recordSet)
                                                                                .build()))
                                              .build();
    }

    public static GetHostedZoneResponse getHostedZoneResponse() {
        return GetHostedZoneResponse.builder()
                                    .hostedZone(HostedZone.builder()
                                                          .id("/hostedzone/" + ZONE_ID)
                                                          .name("example.com.")
                                                          .callerReference("ref-1")
                                                          .config(HostedZoneConfig.builder().comment("c").privateZone(true).build())
                                                          .resourceRecordSetCount(4L)
                                                          .build())
                                    .delegationSet(DelegationSet.builder()
                                                                .nameServers("ns-1.example.net", "ns-2.example.org")
                                                                .build())
                                    .vpCs(VPC.builder().vpcId("vpc-1").vpcRegion(VPCRegion.US_WEST_2).build())
                                    .build();
    }

    /**
     * Reads a response through its getters: nested structures, a list of strings, a list of structures with an enum.
     */
    public static String summarize(GetHostedZoneResponse response) {
        HostedZone zone = response.hostedZone();
        return zone.id() + " " + zone.name() + " " + zone.config().privateZone() + " " + zone.resourceRecordSetCount()
               + " " + response.delegationSet().nameServers() + " " + response.vpCs().get(0).vpcId() + " "
               + response.vpCs().get(0).vpcRegion() + "/" + response.vpCs().get(0).vpcRegionAsString();
    }

    public static String getHostedZone(Route53Client client) {
        return summarize(client.getHostedZone(r -> r.id(ZONE_ID)));
    }

    public static GetChangeResponse changeResponse(ChangeStatus status) {
        return GetChangeResponse.builder()
                                .changeInfo(ChangeInfo.builder()
                                                      .id("/change/C1")
                                                      .status(status)
                                                      .submittedAt(Instant.ofEpochSecond(1_700_000_000L))
                                                      .build())
                                .build();
    }

    public static CompletableFuture<String> upsertAsync(Route53AsyncClient client) {
        return client.changeResourceRecordSets(upsertRequest())
                     .thenApply(r -> r.changeInfo().id() + " " + r.changeInfo().status() + "/"
                                     + r.changeInfo().statusAsString() + " " + r.changeInfo().submittedAt());
    }

    public static boolean waitUntilChangeIsInSync(Route53Client client) {
        try (Route53Waiter waiter = client.waiter()) {
            WaiterResponse<GetChangeResponse> response = waiter.waitUntilResourceRecordSetsChanged(r -> r.id("C1"));
            return response.matched().response().map(r -> r.changeInfo().status() == ChangeStatus.INSYNC).orElse(false);
        }
    }

    public static List<String> hostedZoneNames(Route53Client client) {
        List<String> names = new ArrayList<>();
        client.listHostedZonesPaginator().hostedZones().forEach(z -> names.add(z.name()));
        return names;
    }

    public static NoSuchHostedZoneException noSuchHostedZone() {
        return NoSuchHostedZoneException.builder().message("No hosted zone found with ID: Z0MISSING").statusCode(404).build();
    }

    public static String getMissingHostedZone(Route53Client client) {
        try {
            client.getHostedZone(r -> r.id("Z0MISSING"));
            return "no exception";
        } catch (NoSuchHostedZoneException e) {
            return "NoSuchHostedZoneException " + e.statusCode() + " " + e.awsErrorDetails().errorCode();
        } catch (Route53Exception e) {
            return "Route53Exception " + e.awsErrorDetails().errorCode();
        }
    }
}
