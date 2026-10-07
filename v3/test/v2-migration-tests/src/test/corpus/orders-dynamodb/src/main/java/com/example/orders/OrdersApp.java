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

package com.example.orders;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/** Creates, reads and conflicts on orders; {@link #run} returns what happened, one line per step. */
public final class OrdersApp {
    private OrdersApp() {
    }

    /** Runs the order workflow against the DynamoDB endpoint {@code endpoint}. */
    public static String run(String endpoint) {
        IdentityProvider<AwsCredentialsIdentity> credentials = new IdentityProvider<>() {
            @Override
            public Class<AwsCredentialsIdentity> identityType() {
                return AwsCredentialsIdentity.class;
            }

            @Override
            public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
                return CompletableFuture.completedFuture(AwsCredentialsIdentity.create("AKIDCORPUS", "corpus-secret"));
            }
        };
        List<String> transcript = new ArrayList<>();
        try (DynamoDbClient dynamo = DynamoDbClient.builder()
                                                   .region(Region.of("us-east-1"))
                                                   .credentialsProvider(credentials)
                                                   .endpointOverride(URI.create(endpoint))
                                                   .build()) {
            OrderStore store = new OrderStore(dynamo, "orders");
            transcript.add("create order-1: " + store.create(new Order("order-1", "alice", 3)));
            transcript.add("create order-2: " + store.create(new Order("order-2", "bob", 1)));
            transcript.add("create order-1 again: " + store.create(new Order("order-1", "carol", 9)));
            transcript.add("find order-1: " + store.find("order-1").map(Order::toString).orElse("absent"));
            transcript.add("find order-3: " + store.find("order-3").map(Order::toString).orElse("absent"));
            try {
                new OrderStore(dynamo, "missing-table").find("order-1");
                transcript.add("missing table: no error");
            } catch (DynamoDbException e) {
                transcript.add("missing table: " + e.statusCode() + " " + e.awsErrorDetails().errorCode());
            }
        }
        return String.join("\n", transcript);
    }
}
