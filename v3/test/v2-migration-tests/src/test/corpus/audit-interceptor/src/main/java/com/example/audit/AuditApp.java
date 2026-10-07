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

package com.example.audit;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Calls DynamoDB through the auditing interceptor; {@link #run} returns what happened, one line per step. */
public final class AuditApp {
    private AuditApp() {
    }

    /** Runs the audited workflow against the DynamoDB endpoint {@code endpoint}. */
    public static String run(String endpoint) {
        AuditInterceptor audit = new AuditInterceptor();
        List<String> transcript = new ArrayList<>();
        try (DynamoDbClient dynamo = DynamoDbClient.builder()
                                                   .region(Region.of("us-east-2"))
                                                   .credentialsProvider(new Credentials())
                                                   .endpointOverride(URI.create(endpoint))
                                                   .httpClientBuilder(UrlConnectionHttpClient.builder()
                                                                                             .socketTimeout(Duration.ofSeconds(20)))
                                                   .overrideConfiguration(o -> o.addExecutionInterceptor(audit))
                                                   .build()) {
            dynamo.putItem(r -> r.tableName("audit").item(Map.of("id", AttributeValue.builder().s("a-1").build())));
            String found = dynamo.getItem(r -> r.tableName("audit")
                                                .key(Map.of("id", AttributeValue.builder().s("a-1").build())))
                                 .item().get("id").s();
            transcript.add("found: " + found);
            transcript.add("audited: " + audit.operations());
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
            return CompletableFuture.completedFuture(AwsCredentialsIdentity.create("AKIDAUDIT", "audit-secret"));
        }
    }
}
