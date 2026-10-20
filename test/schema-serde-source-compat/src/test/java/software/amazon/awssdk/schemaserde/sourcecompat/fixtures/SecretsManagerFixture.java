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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.Filter;
import software.amazon.awssdk.services.secretsmanager.model.FilterNameStringType;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.RotationRulesType;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
import software.amazon.awssdk.services.secretsmanager.model.Tag;

/**
 * Secrets Manager, written against the Baseline_Build public API (Requirement 16.8).
 */
public final class SecretsManagerFixture {
    public static final String SECRET_ID = "fixture/secret";

    private SecretsManagerFixture() {
    }

    public static SecretsManagerClient syncClient(SdkHttpClient httpClient, SdkPlugin plugin,
                                                  ClientSettings.CallCounter counter) {
        return SecretsManagerClient.builder()
                                   .region(ClientSettings.REGION)
                                   .credentialsProvider(ClientSettings.credentials())
                                   .overrideConfiguration(ClientSettings.overrideConfiguration(counter))
                                   .endpointOverride(ClientSettings.ENDPOINT)
                                   .httpClient(httpClient)
                                   .addPlugin(plugin)
                                   .build();
    }

    public static SecretsManagerAsyncClient asyncClient(SdkAsyncHttpClient httpClient, SdkPlugin plugin) {
        return SecretsManagerAsyncClient.builder()
                                        .region(ClientSettings.REGION)
                                        .credentialsProvider(ClientSettings.credentials())
                                        .endpointOverride(ClientSettings.ENDPOINT)
                                        .httpClient(httpClient)
                                        .addPlugin(plugin)
                                        .build();
    }

    public static GetSecretValueRequest getSecretValueRequest() {
        return GetSecretValueRequest.builder().secretId(SECRET_ID).versionStage("AWSCURRENT").build();
    }

    public static GetSecretValueResponse getSecretValueResponse() {
        return GetSecretValueResponse.builder()
                                     .arn("arn:aws:secretsmanager:us-west-2:123456789012:secret:fixture")
                                     .name(SECRET_ID)
                                     .secretString("{\"password\":\"hunter2\"}")
                                     .secretBinary(SdkBytes.fromUtf8String("binary"))
                                     .versionStages("AWSCURRENT", "AWSPENDING")
                                     .createdDate(Instant.ofEpochSecond(1_700_000_000L))
                                     .build();
    }

    /**
     * Reads a response through its getters: strings, a blob, a list and a timestamp. {@code toString} redacts the
     * sensitive members.
     */
    public static String summarize(GetSecretValueResponse response) {
        return response.name() + " " + response.secretString() + " " + response.secretBinary().asUtf8String()
               + " " + response.versionStages() + " " + response.createdDate() + " " + response.toString();
    }

    public static String getSecretValue(SecretsManagerClient client) {
        return summarize(client.getSecretValue(getSecretValueRequest()));
    }

    public static DescribeSecretResponse describeSecretResponse() {
        Map<String, List<String>> versions = new TreeMap<>();
        versions.put("v1", Arrays.asList("AWSPREVIOUS"));
        versions.put("v2", Arrays.asList("AWSCURRENT"));
        return DescribeSecretResponse.builder()
                                     .name(SECRET_ID)
                                     .rotationEnabled(true)
                                     .rotationRules(RotationRulesType.builder().automaticallyAfterDays(30L).build())
                                     .tags(Tag.builder().key("team").value("sdk").build())
                                     .versionIdsToStages(versions)
                                     .build();
    }

    public static CompletableFuture<String> describeSecretAsync(SecretsManagerAsyncClient client) {
        return client.describeSecret(r -> r.secretId(SECRET_ID))
                     .thenApply(r -> r.name() + " " + r.rotationEnabled() + " "
                                     + r.rotationRules().automaticallyAfterDays() + " " + r.tags().get(0).key() + "="
                                     + r.tags().get(0).value() + " " + new TreeMap<>(r.versionIdsToStages()));
    }

    public static List<String> secretNames(SecretsManagerClient client) {
        List<String> names = new ArrayList<>();
        Filter filter = Filter.builder().key(FilterNameStringType.NAME).values("fixture").build();
        for (ListSecretsResponse page : client.listSecretsPaginator(r -> r.filters(filter).maxResults(10))) {
            for (SecretListEntry entry : page.secretList()) {
                names.add(entry.name());
            }
        }
        return names;
    }

    public static ResourceNotFoundException resourceNotFound() {
        return ResourceNotFoundException.builder()
                                        .message("Secrets Manager can't find the specified secret.")
                                        .statusCode(400)
                                        .build();
    }

    public static String getMissingSecret(SecretsManagerClient client) {
        try {
            client.getSecretValue(getSecretValueRequest().toBuilder().secretId("missing").build());
            return "no exception";
        } catch (ResourceNotFoundException e) {
            return "ResourceNotFoundException " + e.statusCode() + " " + e.awsErrorDetails().errorCode();
        } catch (SecretsManagerException e) {
            return "SecretsManagerException " + e.awsErrorDetails().errorCode();
        }
    }
}
