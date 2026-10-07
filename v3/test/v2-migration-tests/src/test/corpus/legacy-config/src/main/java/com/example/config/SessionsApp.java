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

package com.example.config;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Records user sessions; {@link #run} returns what happened, one line per step. */
public final class SessionsApp {
    private SessionsApp() {
    }

    /** Runs the session workflow against the DynamoDB endpoint {@code endpoint}. */
    public static String run(String endpoint) {
        ClientOverrideConfiguration overrides = ClientOverrideConfiguration.builder()
                                                                           .apiCallTimeout(Duration.ofSeconds(30))
                                                                           .putHeader("x-corpus-app", "sessions")
                                                                           .build();
        List<String> transcript = new ArrayList<>();
        try (DynamoDbClient dynamo = DynamoDbClient.builder()
                                                   .region(Region.US_WEST_2)
                                                   .credentialsProvider(StaticCredentialsProvider.create(
                                                       AwsBasicCredentials.create("AKIDSESSIONS", "sessions-secret")))
                                                   .overrideConfiguration(overrides)
                                                   .endpointOverride(URI.create(endpoint))
                                                   .build()) {
            for (String user : List.of("dana", "eli", "fay")) {
                dynamo.putItem(r -> r.tableName("sessions")
                                     .item(Map.of("user", AttributeValue.builder().s(user).build())));
            }
            List<String> users = new ArrayList<>();
            dynamo.scanPaginator(r -> r.tableName("sessions").limit(2))
                  .items()
                  .forEach(item -> users.add(item.get("user").s()));
            users.sort(null);
            transcript.add("sessions: " + users);
        }
        return String.join("\n", transcript);
    }
}
