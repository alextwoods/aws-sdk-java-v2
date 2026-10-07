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

package com.example.retry;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Writes through a service that fails transiently; {@link #run} returns what happened, one line per step. */
public final class RetryApp {
    private RetryApp() {
    }

    /** Runs the retried workflow against the DynamoDB endpoint {@code endpoint}. */
    public static String run(String endpoint) {
        RetryStrategy retryStrategy = StandardRetryStrategy.builder().maxAttempts(4).build();
        List<String> transcript = new ArrayList<>();
        try (DynamoDbClient dynamo = DynamoDbClient.builder()
                                                   .region(Region.of("us-west-1"))
                                                   .credentialsProvider(new Credentials())
                                                   .endpointOverride(URI.create(endpoint))
                                                   .overrideConfiguration(o -> o.retryStrategy(retryStrategy))
                                                   .build()) {
            dynamo.putItem(r -> r.tableName("flaky").item(Map.of("id", AttributeValue.builder().s("r-1").build())));
            transcript.add("max attempts: " + retryStrategy.maxAttempts());
            transcript.add("written after a transient failure: r-1");
        }
        return String.join("\n", transcript);
    }

    private static final class Credentials implements IdentityProvider<AwsCredentialsIdentity> {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(AwsCredentialsIdentity.create("AKIDRETRY", "retry-secret"));
        }
    }
}
