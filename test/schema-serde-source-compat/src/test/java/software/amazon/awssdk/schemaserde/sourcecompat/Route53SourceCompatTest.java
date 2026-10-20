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

package software.amazon.awssdk.schemaserde.sourcecompat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schemaserde.sourcecompat.MockedCalls.NoNetworkAsyncHttpClient;
import software.amazon.awssdk.schemaserde.sourcecompat.MockedCalls.NoNetworkHttpClient;
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.ClientSettings;
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.Route53Fixture;
import software.amazon.awssdk.services.route53.Route53AsyncClient;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.ChangeStatus;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.ListHostedZonesResponse;
import software.amazon.awssdk.testutils.mock.MockPlugin;

/**
 * Runs {@link Route53Fixture} against mocked Route 53 clients (restXml).
 */
class Route53SourceCompatTest {
    private final ClientSettings.CallCounter counter = new ClientSettings.CallCounter();

    @Test
    void getHostedZone_sync_readsNestedStructuresAndLists() {
        MockPlugin plugin = MockedCalls.plugin(Route53Fixture.getHostedZoneResponse());
        try (Route53Client client = Route53Fixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            // Route53IdInterceptor (a customization in the Baseline_Build too) strips the "/hostedzone/" prefix.
            assertThat(Route53Fixture.summarize(Route53Fixture.getHostedZoneResponse())).startsWith("/hostedzone/Z0FIXTURE ");
            assertThat(Route53Fixture.getHostedZone(client))
                .isEqualTo("Z0FIXTURE example.com. true 4 [ns-1.example.net, ns-2.example.org] vpc-1 "
                           + "us-west-2/us-west-2");
        }
        MockedCalls.assertSentWithClientSettings(plugin, 1);
        assertThat(counter.calls()).isEqualTo(1);
    }

    @Test
    void changeResourceRecordSets_async_readsEnumAndTimestamp() {
        ChangeResourceRecordSetsResponse response =
            ChangeResourceRecordSetsResponse.builder()
                                            .changeInfo(Route53Fixture.changeResponse(ChangeStatus.PENDING).changeInfo())
                                            .build();
        MockPlugin plugin = MockedCalls.plugin(response);
        try (Route53AsyncClient client = Route53Fixture.asyncClient(new NoNetworkAsyncHttpClient(), plugin)) {
            // Route53IdInterceptor strips the "/change/" prefix.
            assertThat(Route53Fixture.upsertAsync(client).join()).isEqualTo("C1 PENDING/PENDING 2023-11-14T22:13:20Z");
        }
    }

    @Test
    void waitUntilResourceRecordSetsChanged_matchesSuccessAcceptor() {
        MockPlugin plugin = MockedCalls.plugin(Route53Fixture.changeResponse(ChangeStatus.INSYNC));
        try (Route53Client client = Route53Fixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(Route53Fixture.waitUntilChangeIsInSync(client)).isTrue();
        }
    }

    @Test
    void listHostedZonesPaginator_readsItemsOfEveryPage() {
        MockPlugin plugin = MockedCalls.plugin(
            ListHostedZonesResponse.builder().hostedZones(zone("a.example.")).isTruncated(true).nextMarker("m2")
                                   .marker("").maxItems("1").build(),
            ListHostedZonesResponse.builder().hostedZones(zone("b.example.")).isTruncated(false)
                                   .marker("m2").maxItems("1").build());
        try (Route53Client client = Route53Fixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(Route53Fixture.hostedZoneNames(client)).containsExactly("a.example.", "b.example.");
        }
    }

    @Test
    void getHostedZone_modeledError_caughtByType() {
        MockPlugin plugin = MockedCalls.plugin(Route53Fixture.noSuchHostedZone());
        try (Route53Client client = Route53Fixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(Route53Fixture.getMissingHostedZone(client)).isEqualTo("NoSuchHostedZoneException 404 NoSuchHostedZone");
        }
    }

    @Test
    void toBuilder_copiesRequestAndOverridesMember() {
        ChangeResourceRecordSetsRequest request = Route53Fixture.upsertRequest();
        ChangeResourceRecordSetsRequest copy = request.toBuilder().hostedZoneId("Z0OTHER").build();

        assertThat(copy.hostedZoneId()).isEqualTo("Z0OTHER");
        assertThat(copy.changeBatch().changes().get(0).actionAsString()).isEqualTo("UPSERT");
        assertThat(copy.changeBatch().changes().get(0).resourceRecordSet().type().toString()).isEqualTo("A");
        assertThat(copy.toBuilder().hostedZoneId(Route53Fixture.ZONE_ID).build()).isEqualTo(request);
    }

    private static HostedZone zone(String name) {
        return HostedZone.builder().id("/hostedzone/" + name).name(name).callerReference("r").build();
    }
}
