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

package com.example.legacy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Writes through a legacy-configured client; {@link #run} returns what happened, one line per step. */
public final class LegacyApp {
    private LegacyApp() {
    }

    /** Runs the legacy workflow against the DynamoDB endpoint {@code endpoint}. */
    public static String run(String endpoint) {
        RetryPolicy retryPolicy = RetryPolicy.builder()
                                             .numRetries(3)
                                             .backoffStrategy(BackoffStrategy.none())
                                             .build();
        List<String> transcript = new ArrayList<>();
        try (DynamoDbClient dynamo = DynamoDbClient.builder()
                                                   .region(Region.of("ap-south-1"))
                                                   .credentialsProvider(AnonymousCredentialsProvider.create())
                                                   .endpointOverride(URI.create(endpoint))
                                                   .overrideConfiguration(o -> o.retryPolicy(retryPolicy)
                                                                                .putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                                                                   Aws4Signer.create()))
                                                   .build()) {
            dynamo.putItem(r -> r.tableName("flaky").item(Map.of("id", AttributeValue.builder().s("l-1").build())));
            transcript.add("retries: " + retryPolicy.numRetries());
            transcript.add("written after a transient failure: l-1");
        }
        return String.join("\n", transcript);
    }
}
