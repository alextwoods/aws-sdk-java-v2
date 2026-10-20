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
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.AccountFixture;
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.ClientSettings;
import software.amazon.awssdk.services.account.AccountAsyncClient;
import software.amazon.awssdk.services.account.AccountClient;
import software.amazon.awssdk.services.account.model.AlternateContactType;
import software.amazon.awssdk.services.account.model.GetAlternateContactRequest;
import software.amazon.awssdk.services.account.model.ListRegionsResponse;
import software.amazon.awssdk.services.account.model.Region;
import software.amazon.awssdk.services.account.model.RegionOptStatus;
import software.amazon.awssdk.testutils.mock.MockPlugin;

/**
 * Runs {@link AccountFixture} against mocked Account clients (restJson1, generated from the native Smithy model).
 */
class AccountSourceCompatTest {
    private final ClientSettings.CallCounter counter = new ClientSettings.CallCounter();

    @Test
    void getAlternateContact_sync_readsNestedStructureAndEnum() {
        MockPlugin plugin = MockedCalls.plugin(AccountFixture.getAlternateContactResponse());
        try (AccountClient client = AccountFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(AccountFixture.getAlternateContact(client))
                .isEqualTo(AccountFixture.summarize(AccountFixture.getAlternateContactResponse()))
                .isEqualTo("BILLING/BILLING Jane jane@example.com Billing");
        }
        MockedCalls.assertSentWithClientSettings(plugin, 1);
        assertThat(counter.calls()).isEqualTo(1);
    }

    @Test
    void getContactInformation_async_readsNestedStructure() {
        MockPlugin plugin = MockedCalls.plugin(AccountFixture.getContactInformationResponse());
        try (AccountAsyncClient client = AccountFixture.asyncClient(new NoNetworkAsyncHttpClient(), plugin)) {
            assertThat(AccountFixture.getContactInformationAsync(client).join()).isEqualTo("Jane Doe Seattle US");
        }
    }

    @Test
    void listRegionsPaginator_readsItemsOfEveryPage() {
        MockPlugin plugin = MockedCalls.plugin(
            ListRegionsResponse.builder()
                               .regions(Region.builder().regionName("us-west-2").regionOptStatus(RegionOptStatus.ENABLED_BY_DEFAULT).build())
                               .nextToken("p2")
                               .build(),
            ListRegionsResponse.builder()
                               .regions(Region.builder().regionName("af-south-1").regionOptStatus(RegionOptStatus.ENABLED).build())
                               .build());
        try (AccountClient client = AccountFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(AccountFixture.regions(client))
                .containsExactly("us-west-2=ENABLED_BY_DEFAULT/ENABLED_BY_DEFAULT", "af-south-1=ENABLED/ENABLED");
        }
    }

    @Test
    void getAlternateContact_modeledError_caughtByType() {
        MockPlugin plugin = MockedCalls.plugin(AccountFixture.resourceNotFound());
        try (AccountClient client = AccountFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(AccountFixture.getMissingAlternateContact(client))
                .isEqualTo("ResourceNotFoundException 404 ResourceNotFoundException");
        }
    }

    @Test
    void toBuilder_copiesRequestAndOverridesMember() {
        GetAlternateContactRequest request = AccountFixture.getAlternateContactRequest();
        GetAlternateContactRequest copy = request.toBuilder().alternateContactType(AlternateContactType.OPERATIONS).build();

        assertThat(copy.alternateContactTypeAsString()).isEqualTo("OPERATIONS");
        assertThat(copy.accountId()).isEqualTo("123456789012");
        assertThat(copy.toBuilder().alternateContactType("BILLING").build()).isEqualTo(request);
    }
}
