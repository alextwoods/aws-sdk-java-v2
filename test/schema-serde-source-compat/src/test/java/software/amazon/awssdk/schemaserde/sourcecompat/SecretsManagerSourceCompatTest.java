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
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.SecretsManagerFixture;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;
import software.amazon.awssdk.testutils.mock.MockPlugin;

/**
 * Runs {@link SecretsManagerFixture} against mocked Secrets Manager clients (awsJson1_1).
 */
class SecretsManagerSourceCompatTest {
    private final ClientSettings.CallCounter counter = new ClientSettings.CallCounter();

    @Test
    void getSecretValue_sync_readsMockedOutputThroughGetters() {
        MockPlugin plugin = MockedCalls.plugin(SecretsManagerFixture.getSecretValueResponse());
        try (SecretsManagerClient client = SecretsManagerFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(SecretsManagerFixture.getSecretValue(client))
                .isEqualTo(SecretsManagerFixture.summarize(SecretsManagerFixture.getSecretValueResponse()))
                .startsWith("fixture/secret {\"password\":\"hunter2\"} binary [AWSCURRENT, AWSPENDING] "
                            + "2023-11-14T22:13:20Z GetSecretValueResponse(")
                .contains("SecretString=*** Sensitive Data Redacted ***");
        }
        MockedCalls.assertSentWithClientSettings(plugin, 1);
        assertThat(counter.calls()).isEqualTo(1);
    }

    @Test
    void describeSecret_async_readsMapOfListsAndNestedStructure() {
        MockPlugin plugin = MockedCalls.plugin(SecretsManagerFixture.describeSecretResponse());
        try (SecretsManagerAsyncClient client = SecretsManagerFixture.asyncClient(new NoNetworkAsyncHttpClient(), plugin)) {
            assertThat(SecretsManagerFixture.describeSecretAsync(client).join())
                .isEqualTo("fixture/secret true 30 team=sdk {v1=[AWSPREVIOUS], v2=[AWSCURRENT]}");
        }
    }

    @Test
    void listSecretsPaginator_readsItemsOfEveryPage() {
        MockPlugin plugin = MockedCalls.plugin(
            ListSecretsResponse.builder().secretList(SecretListEntry.builder().name("s1").build()).nextToken("p2").build(),
            ListSecretsResponse.builder().secretList(SecretListEntry.builder().name("s2").build()).build());
        try (SecretsManagerClient client = SecretsManagerFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(SecretsManagerFixture.secretNames(client)).containsExactly("s1", "s2");
        }
    }

    @Test
    void getSecretValue_modeledError_caughtByType() {
        MockPlugin plugin = MockedCalls.plugin(SecretsManagerFixture.resourceNotFound());
        try (SecretsManagerClient client = SecretsManagerFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(SecretsManagerFixture.getMissingSecret(client))
                .isEqualTo("ResourceNotFoundException 400 ResourceNotFoundException");
        }
    }

    @Test
    void toBuilder_copiesRequestAndOverridesMember() {
        GetSecretValueRequest request = SecretsManagerFixture.getSecretValueRequest();
        GetSecretValueRequest copy = request.toBuilder().versionId("v1").build();

        assertThat(copy.versionId()).isEqualTo("v1");
        assertThat(copy.versionStage()).isEqualTo("AWSCURRENT");
        assertThat(copy.toBuilder().versionId(null).build()).isEqualTo(request);
    }
}
