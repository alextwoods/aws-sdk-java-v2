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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/** Stores orders in a DynamoDB table keyed by order ID; an order is written at most once. */
public final class OrderStore {
    private final DynamoDbClient dynamo;
    private final String table;

    public OrderStore(DynamoDbClient dynamo, String table) {
        this.dynamo = dynamo;
        this.table = table;
    }

    /** Writes {@code order}; returns false if an order with its ID already exists. */
    public boolean create(Order order) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("orderId", AttributeValue.builder().s(order.id()).build());
        item.put("customer", AttributeValue.builder().s(order.customer()).build());
        item.put("quantity", AttributeValue.builder().n(Integer.toString(order.quantity())).build());
        PutItemRequest request = PutItemRequest.builder()
                                               .tableName(table)
                                               .item(item)
                                               .conditionExpression("attribute_not_exists(orderId)")
                                               .build();
        try {
            dynamo.putItem(request);
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    /** The order with {@code id}, if one exists. */
    public Optional<Order> find(String id) {
        GetItemResponse response = dynamo.getItem(GetItemRequest.builder()
                                                                .tableName(table)
                                                                .key(Map.of("orderId", AttributeValue.builder()
                                                                                                     .s(id)
                                                                                                     .build()))
                                                                .consistentRead(true)
                                                                .build());
        if (!response.hasItem()) {
            return Optional.empty();
        }
        Map<String, AttributeValue> item = response.item();
        return Optional.of(new Order(item.get("orderId").s(), item.get("customer").s(),
                                     Integer.parseInt(item.get("quantity").n())));
    }
}
