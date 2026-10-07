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

package com.example.catalog;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

/** Loads a product catalog and lists it page by page; {@link #run} returns what happened, one line per step. */
public final class CatalogApp {
    private CatalogApp() {
    }

    /** Runs the catalog workflow against the DynamoDB endpoint {@code endpoint}. */
    public static String run(String endpoint) {
        List<String> transcript = new ArrayList<String>();
        DynamoDbClient dynamo = DynamoDbClient.builder()
                                              .region(Region.of("eu-west-1"))
                                              .credentialsProvider(new FixedCredentials())
                                              .endpointOverride(URI.create(endpoint))
                                              .build();
        try {
            String[] products = {"anvil", "bolt", "crate", "drill", "easel"};
            for (int i = 0; i < products.length; i++) {
                final Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
                item.put("sku", AttributeValue.builder().s("sku-" + i).build());
                item.put("name", AttributeValue.builder().s(products[i]).build());
                dynamo.putItem(r -> r.tableName("catalog").item(item));
            }
            int pages = 0;
            List<String> names = new ArrayList<String>();
            for (ScanResponse page : dynamo.scanPaginator(r -> r.tableName("catalog").limit(2))) {
                pages++;
                for (Map<String, AttributeValue> item : page.items()) {
                    names.add(item.get("name").s());
                }
            }
            Collections.sort(names);
            transcript.add("pages: " + pages);
            transcript.add("products: " + names);
        } finally {
            dynamo.close();
        }
        return String.join("\n", transcript);
    }

    /** Static credentials as an identity provider. */
    private static final class FixedCredentials implements IdentityProvider<AwsCredentialsIdentity> {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(AwsCredentialsIdentity.create("AKIDCATALOG", "catalog-secret"));
        }
    }
}
