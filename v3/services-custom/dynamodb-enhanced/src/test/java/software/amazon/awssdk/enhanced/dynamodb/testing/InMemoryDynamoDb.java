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

package software.amazon.awssdk.enhanced.dynamodb.testing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonNode;
import software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonWriter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An in-process DynamoDB: tables with hash/range keys and global/local secondary indexes, and the item, query, scan,
 * batch, transaction and table operations the enhanced client calls, evaluated over awsJson1.0 request documents.
 * Conditions, filters, key conditions, updates and projections use {@link Expressions}. Every operation is atomic
 * (the service is serialized), as DynamoDB item operations and transactions are.
 *
 * <p>Deterministic knobs: {@link #batchGetKeysPerCall} and {@link #batchWriteItemsPerCall} bound how much of a batch
 * one call processes, returning the rest as unprocessed keys/items, so batch pagination and retry behavior is
 * observable.
 */
public final class InMemoryDynamoDb {
    private final Map<String, Table> tables = new LinkedHashMap<>();
    private volatile int batchGetKeysPerCall = 100;
    private volatile int batchWriteItemsPerCall = 25;

    public InMemoryDynamoDb batchGetKeysPerCall(int keys) {
        this.batchGetKeysPerCall = keys;
        return this;
    }

    public InMemoryDynamoDb batchWriteItemsPerCall(int items) {
        this.batchWriteItemsPerCall = items;
        return this;
    }

    /** A snapshot of a table's items in storage order. */
    public synchronized List<Map<String, AttributeValue>> items(String tableName) {
        return table(tableName).items.values().stream().map(Av::copy).toList();
    }

    /** Executes {@code operation} (the {@code X-Amz-Target} operation name) and returns the response body. */
    public synchronized String execute(String operation, JsonNode request) {
        Req r = new Req(request);
        return switch (operation) {
            case "CreateTable" -> createTable(r);
            case "DescribeTable" -> describeTable(r);
            case "DeleteTable" -> deleteTable(r);
            case "PutItem" -> putItem(r);
            case "GetItem" -> getItem(r);
            case "DeleteItem" -> deleteItem(r);
            case "UpdateItem" -> updateItem(r);
            case "Query" -> query(r);
            case "Scan" -> scan(r);
            case "BatchGetItem" -> batchGetItem(r);
            case "BatchWriteItem" -> batchWriteItem(r);
            case "TransactGetItems" -> transactGetItems(r);
            case "TransactWriteItems" -> transactWriteItems(r);
            default -> throw new DynamoDbError(400, "UnknownOperationException",
                                               "The in-process DynamoDB does not support " + operation, null);
        };
    }

    // ---- tables ----------------------------------------------------------------------------------------------------

    private String createTable(Req r) {
        String name = r.str("TableName");
        if (tables.containsKey(name)) {
            throw new DynamoDbError(400, "ResourceInUseException", "Table already exists: " + name, null);
        }
        Table table = new Table(name, r.node);
        tables.put(name, table);
        return response(w -> {
            w.writeFieldName("TableDescription");
            table.describe(w, "ACTIVE");
        });
    }

    private String describeTable(Req r) {
        Table table = table(r.str("TableName"));
        return response(w -> {
            w.writeFieldName("Table");
            table.describe(w, "ACTIVE");
        });
    }

    private String deleteTable(Req r) {
        Table table = table(r.str("TableName"));
        tables.remove(table.name);
        return response(w -> {
            w.writeFieldName("TableDescription");
            table.describe(w, "DELETING");
        });
    }

    private Table table(String name) {
        Table table = tables.get(name);
        if (table == null) {
            throw DynamoDbError.resourceNotFound("Requested resource not found: Table: " + name + " not found");
        }
        return table;
    }

    // ---- items -----------------------------------------------------------------------------------------------------

    private String putItem(Req r) {
        Table table = table(r.str("TableName"));
        Map<String, AttributeValue> item = Av.mapFromJson(r.get("Item"));
        String key = table.keyOf(item);
        Map<String, AttributeValue> old = table.items.get(key);
        checkCondition(r, old);
        table.items.put(key, item);
        return response(w -> {
            if (old != null && "ALL_OLD".equals(r.str("ReturnValues"))) {
                w.writeFieldName("Attributes");
                Av.writeMap(w, old);
            }
            consumedCapacity(w, r, table.name);
        });
    }

    private String getItem(Req r) {
        Table table = table(r.str("TableName"));
        Map<String, AttributeValue> key = Av.mapFromJson(r.get("Key"));
        Map<String, AttributeValue> item = table.items.get(table.keyOfKey(key));
        return response(w -> {
            if (item != null) {
                w.writeFieldName("Item");
                Av.writeMap(w, project(r, item));
            }
            consumedCapacity(w, r, table.name);
        });
    }

    private String deleteItem(Req r) {
        Table table = table(r.str("TableName"));
        String key = table.keyOfKey(Av.mapFromJson(r.get("Key")));
        Map<String, AttributeValue> old = table.items.get(key);
        checkCondition(r, old);
        table.items.remove(key);
        return response(w -> {
            if (old != null && "ALL_OLD".equals(r.str("ReturnValues"))) {
                w.writeFieldName("Attributes");
                Av.writeMap(w, old);
            }
            consumedCapacity(w, r, table.name);
        });
    }

    private String updateItem(Req r) {
        Table table = table(r.str("TableName"));
        Map<String, AttributeValue> key = Av.mapFromJson(r.get("Key"));
        String storageKey = table.keyOfKey(key);
        Map<String, AttributeValue> old = table.items.get(storageKey);
        checkCondition(r, old);
        Map<String, AttributeValue> updated = applyUpdate(table, r, key, old);
        table.items.put(storageKey, updated);
        String returnValues = r.str("ReturnValues");
        return response(w -> {
            Map<String, AttributeValue> attributes = switch (returnValues == null ? "NONE" : returnValues) {
                case "ALL_NEW" -> updated;
                case "ALL_OLD" -> old;
                case "UPDATED_NEW" -> changed(updated, old);
                case "UPDATED_OLD" -> old == null ? null : changed(old, updated);
                default -> null;
            };
            if (attributes != null) {
                w.writeFieldName("Attributes");
                Av.writeMap(w, attributes);
            }
            consumedCapacity(w, r, table.name);
        });
    }

    private static Map<String, AttributeValue> applyUpdate(Table table, Req r, Map<String, AttributeValue> key,
                                                           Map<String, AttributeValue> old) {
        Map<String, AttributeValue> updated = old == null ? new LinkedHashMap<>(key) : Av.copy(old);
        String expression = r.str("UpdateExpression");
        if (expression != null) {
            Expressions.update(expression, r.names(), r.values(), updated);
        }
        for (String keyAttribute : table.keyAttributes()) {
            if (!Av.same(updated.get(keyAttribute), key.get(keyAttribute))) {
                throw DynamoDbError.validation("One or more parameter values were invalid: Cannot update attribute "
                                               + keyAttribute + ". This attribute is part of the key");
            }
        }
        return updated;
    }

    private static Map<String, AttributeValue> changed(Map<String, AttributeValue> source,
                                                       Map<String, AttributeValue> other) {
        Map<String, AttributeValue> result = new LinkedHashMap<>();
        source.forEach((k, v) -> {
            if (other == null || !Av.same(v, other.get(k))) {
                result.put(k, v);
            }
        });
        return result;
    }

    private static void checkCondition(Req r, Map<String, AttributeValue> existing) {
        String condition = r.str("ConditionExpression");
        if (condition == null) {
            return;
        }
        Predicate<Map<String, AttributeValue>> test = Expressions.condition(condition, r.names(), r.values());
        if (!test.test(existing == null ? Map.of() : existing)) {
            boolean returnItem = existing != null && "ALL_OLD".equals(r.str("ReturnValuesOnConditionCheckFailure"));
            throw new DynamoDbError(400, "ConditionalCheckFailedException", "The conditional request failed",
                                    returnItem ? w -> {
                                        w.writeFieldName("Item");
                                        Av.writeMap(w, existing);
                                    } : null);
        }
    }

    // ---- query and scan --------------------------------------------------------------------------------------------

    private String query(Req r) {
        Table table = table(r.str("TableName"));
        Index index = table.index(r.str("IndexName"));
        Predicate<Map<String, AttributeValue>> keyCondition =
            Expressions.condition(r.str("KeyConditionExpression"), r.names(), r.values());
        List<Map<String, AttributeValue>> candidates = new ArrayList<>();
        for (Map<String, AttributeValue> item : table.items.values()) {
            if (index.covers(item) && keyCondition.test(item)) {
                candidates.add(item);
            }
        }
        Comparator<Map<String, AttributeValue>> order = index.order(table);
        candidates.sort(Boolean.FALSE.equals(r.bool("ScanIndexForward")) ? order.reversed() : order);
        return page(r, table, index, candidates);
    }

    private String scan(Req r) {
        Table table = table(r.str("TableName"));
        Index index = table.index(r.str("IndexName"));
        Integer segment = r.integer("Segment");
        Integer totalSegments = r.integer("TotalSegments");
        List<Map<String, AttributeValue>> candidates = new ArrayList<>();
        for (Map.Entry<String, Map<String, AttributeValue>> e : table.items.entrySet()) {
            if (!index.covers(e.getValue())) {
                continue;
            }
            if (segment != null && totalSegments != null
                && Math.floorMod(e.getKey().hashCode(), totalSegments) != segment) {
                continue;
            }
            candidates.add(e.getValue());
        }
        return page(r, table, index, candidates);
    }

    private String page(Req r, Table table, Index index, List<Map<String, AttributeValue>> candidates) {
        int start = 0;
        JsonNode exclusiveStartKey = r.get("ExclusiveStartKey");
        if (exclusiveStartKey != null) {
            String startKey = table.keyOfKey(Av.mapFromJson(exclusiveStartKey));
            for (int i = 0; i < candidates.size(); i++) {
                if (table.keyOf(candidates.get(i)).equals(startKey)) {
                    start = i + 1;
                    break;
                }
            }
        }
        Integer limit = r.integer("Limit");
        int end = limit == null ? candidates.size() : Math.min(candidates.size(), start + limit);
        Predicate<Map<String, AttributeValue>> filter =
            r.str("FilterExpression") == null ? item -> true
                                              : Expressions.condition(r.str("FilterExpression"), r.names(),
                                                                      r.values());
        List<Map<String, AttributeValue>> matched = new ArrayList<>();
        for (int i = start; i < end; i++) {
            if (filter.test(candidates.get(i))) {
                matched.add(candidates.get(i));
            }
        }
        Map<String, AttributeValue> lastEvaluated = end < candidates.size() ? candidates.get(end - 1) : null;
        boolean count = "COUNT".equals(r.str("Select"));
        int scanned = end - start;
        return response(w -> {
            if (!count) {
                w.writeFieldName("Items").writeStartArray();
                matched.forEach(item -> Av.writeMap(w, project(r, item)));
                w.writeEndArray();
            }
            w.writeFieldName("Count").writeNumber(Integer.toString(matched.size()));
            w.writeFieldName("ScannedCount").writeNumber(Integer.toString(scanned));
            if (lastEvaluated != null) {
                w.writeFieldName("LastEvaluatedKey");
                Av.writeMap(w, index.keyOf(table, lastEvaluated));
            }
            consumedCapacity(w, r, table.name);
        });
    }

    private static Map<String, AttributeValue> project(Req r, Map<String, AttributeValue> item) {
        String projection = r.str("ProjectionExpression");
        return projection == null ? item : Expressions.project(projection, r.names(), item);
    }

    // ---- batches ---------------------------------------------------------------------------------------------------

    private String batchGetItem(Req r) {
        Map<String, List<Map<String, AttributeValue>>> responses = new LinkedHashMap<>();
        Map<String, List<JsonNode>> unprocessed = new LinkedHashMap<>();
        Map<String, Req> tableRequests = new LinkedHashMap<>();
        int budget = batchGetKeysPerCall;
        for (Map.Entry<String, JsonNode> e : r.get("RequestItems").asObject().entrySet()) {
            Table table = table(e.getKey());
            Req tableRequest = new Req(e.getValue());
            tableRequests.put(table.name, tableRequest);
            List<Map<String, AttributeValue>> found = responses.computeIfAbsent(table.name, k -> new ArrayList<>());
            for (JsonNode keyNode : tableRequest.get("Keys").asArray()) {
                if (budget-- <= 0) {
                    unprocessed.computeIfAbsent(table.name, k -> new ArrayList<>()).add(keyNode);
                    continue;
                }
                Map<String, AttributeValue> item = table.items.get(table.keyOfKey(Av.mapFromJson(keyNode)));
                if (item != null) {
                    found.add(project(tableRequest, item));
                }
            }
        }
        return response(w -> {
            w.writeFieldName("Responses").writeStartObject();
            responses.forEach((table, items) -> {
                w.writeFieldName(table).writeStartArray();
                items.forEach(item -> Av.writeMap(w, item));
                w.writeEndArray();
            });
            w.writeEndObject();
            w.writeFieldName("UnprocessedKeys").writeStartObject();
            unprocessed.forEach((table, keys) -> {
                w.writeFieldName(table).writeStartObject();
                tableRequests.get(table).node.asObject().forEach((member, value) -> {
                    if (!member.equals("Keys")) {
                        w.writeFieldName(member).writeNode(value);
                    }
                });
                w.writeFieldName("Keys").writeStartArray();
                keys.forEach(w::writeNode);
                w.writeEndArray().writeEndObject();
            });
            w.writeEndObject();
            consumedCapacityList(w, r, responses.keySet());
        });
    }

    private String batchWriteItem(Req r) {
        Map<String, List<JsonNode>> unprocessed = new LinkedHashMap<>();
        int budget = batchWriteItemsPerCall;
        for (Map.Entry<String, JsonNode> e : r.get("RequestItems").asObject().entrySet()) {
            Table table = table(e.getKey());
            for (JsonNode writeRequest : e.getValue().asArray()) {
                if (budget-- <= 0) {
                    unprocessed.computeIfAbsent(table.name, k -> new ArrayList<>()).add(writeRequest);
                    continue;
                }
                Req write = new Req(writeRequest);
                if (write.get("PutRequest") != null) {
                    Map<String, AttributeValue> item = Av.mapFromJson(new Req(write.get("PutRequest")).get("Item"));
                    table.items.put(table.keyOf(item), item);
                } else {
                    Map<String, AttributeValue> key = Av.mapFromJson(new Req(write.get("DeleteRequest")).get("Key"));
                    table.items.remove(table.keyOfKey(key));
                }
            }
        }
        return response(w -> {
            w.writeFieldName("UnprocessedItems").writeStartObject();
            unprocessed.forEach((table, writes) -> {
                w.writeFieldName(table).writeStartArray();
                writes.forEach(w::writeNode);
                w.writeEndArray();
            });
            w.writeEndObject();
            consumedCapacityList(w, r, r.get("RequestItems").asObject().keySet());
        });
    }

    // ---- transactions ----------------------------------------------------------------------------------------------

    private String transactGetItems(Req r) {
        List<Map<String, AttributeValue>> results = new ArrayList<>();
        for (JsonNode entry : r.get("TransactItems").asArray()) {
            Req get = new Req(new Req(entry).get("Get"));
            Table table = table(get.str("TableName"));
            Map<String, AttributeValue> item = table.items.get(table.keyOfKey(Av.mapFromJson(get.get("Key"))));
            results.add(item == null ? null : project(get, item));
        }
        return response(w -> {
            w.writeFieldName("Responses").writeStartArray();
            for (Map<String, AttributeValue> item : results) {
                w.writeStartObject();
                if (item != null) {
                    w.writeFieldName("Item");
                    Av.writeMap(w, item);
                }
                w.writeEndObject();
            }
            w.writeEndArray();
        });
    }

    private String transactWriteItems(Req r) {
        List<JsonNode> entries = r.get("TransactItems").asArray();
        List<String> codes = new ArrayList<>();
        List<Map<String, AttributeValue>> failedItems = new ArrayList<>();
        List<Runnable> writes = new ArrayList<>();
        boolean failed = false;
        for (JsonNode entry : entries) {
            Map.Entry<String, JsonNode> action = entry.asObject().entrySet().iterator().next();
            Req op = new Req(action.getValue());
            Table table = table(op.str("TableName"));
            Map<String, AttributeValue> key = action.getKey().equals("Put")
                                              ? table.keyAttributesOf(Av.mapFromJson(op.get("Item")))
                                              : Av.mapFromJson(op.get("Key"));
            String storageKey = table.keyOfKey(key);
            Map<String, AttributeValue> existing = table.items.get(storageKey);
            String condition = op.str("ConditionExpression");
            boolean ok = condition == null
                         || Expressions.condition(condition, op.names(), op.values())
                                       .test(existing == null ? Map.of() : existing);
            codes.add(ok ? "None" : "ConditionalCheckFailed");
            failedItems.add(!ok && existing != null && "ALL_OLD".equals(op.str("ReturnValuesOnConditionCheckFailure"))
                            ? existing : null);
            failed |= !ok;
            switch (action.getKey()) {
                case "Put" -> writes.add(() -> table.items.put(storageKey, Av.mapFromJson(op.get("Item"))));
                case "Delete" -> writes.add(() -> table.items.remove(storageKey));
                case "Update" -> writes.add(() -> table.items.put(storageKey,
                                                                  applyUpdate(table, op, key, table.items.get(storageKey))));
                case "ConditionCheck" -> {
                }
                default -> throw DynamoDbError.validation("Unknown transaction action " + action.getKey());
            }
        }
        if (failed) {
            throw new DynamoDbError(400, "TransactionCanceledException",
                                    "Transaction cancelled, please refer cancellation reasons for specific reasons "
                                    + codes, w -> {
                                        w.writeFieldName("CancellationReasons").writeStartArray();
                                        for (int i = 0; i < codes.size(); i++) {
                                            w.writeStartObject();
                                            w.writeFieldName("Code").writeValue(codes.get(i));
                                            if (!codes.get(i).equals("None")) {
                                                w.writeFieldName("Message")
                                                 .writeValue("The conditional request failed");
                                            }
                                            if (failedItems.get(i) != null) {
                                                w.writeFieldName("Item");
                                                Av.writeMap(w, failedItems.get(i));
                                            }
                                            w.writeEndObject();
                                        }
                                        w.writeEndArray();
                                    });
        }
        writes.forEach(Runnable::run);
        return response(w -> { });
    }

    // ---- response helpers ------------------------------------------------------------------------------------------

    private static String response(java.util.function.Consumer<JsonWriter> members) {
        JsonWriter w = new JsonWriter().writeStartObject();
        members.accept(w);
        return w.writeEndObject().toString();
    }

    private static void consumedCapacity(JsonWriter w, Req r, String table) {
        String mode = r.str("ReturnConsumedCapacity");
        if (mode != null && !mode.equals("NONE")) {
            w.writeFieldName("ConsumedCapacity");
            capacity(w, table);
        }
    }

    private static void consumedCapacityList(JsonWriter w, Req r, Iterable<String> tables) {
        String mode = r.str("ReturnConsumedCapacity");
        if (mode != null && !mode.equals("NONE")) {
            w.writeFieldName("ConsumedCapacity").writeStartArray();
            tables.forEach(t -> capacity(w, t));
            w.writeEndArray();
        }
    }

    private static void capacity(JsonWriter w, String table) {
        w.writeStartObject().writeFieldName("TableName").writeValue(table)
         .writeFieldName("CapacityUnits").writeNumber("1.0").writeEndObject();
    }

    // ---- model -----------------------------------------------------------------------------------------------------

    /** Typed access to a request object's members. */
    static final class Req {
        final JsonNode node;

        Req(JsonNode node) {
            this.node = node;
        }

        JsonNode get(String member) {
            JsonNode v = node.asObject().get(member);
            return v == null || v.isNull() ? null : v;
        }

        String str(String member) {
            JsonNode v = get(member);
            return v == null ? null : v.asString();
        }

        Boolean bool(String member) {
            JsonNode v = get(member);
            return v == null ? null : v.asBoolean();
        }

        Integer integer(String member) {
            JsonNode v = get(member);
            return v == null ? null : Integer.valueOf(v.asNumber());
        }

        Map<String, String> names() {
            JsonNode v = get("ExpressionAttributeNames");
            Map<String, String> result = new LinkedHashMap<>();
            if (v != null) {
                v.asObject().forEach((k, n) -> result.put(k, n.asString()));
            }
            return result;
        }

        Map<String, AttributeValue> values() {
            JsonNode v = get("ExpressionAttributeValues");
            return v == null ? Map.of() : Av.mapFromJson(v);
        }
    }

    private static final class Table {
        final String name;
        final JsonNode definition;
        final String hashKey;
        final String rangeKey;
        final Map<String, Index> indexes = new LinkedHashMap<>();
        final Map<String, Map<String, AttributeValue>> items = new LinkedHashMap<>();
        final long created = System.currentTimeMillis() / 1000;

        Table(String name, JsonNode definition) {
            this.name = name;
            this.definition = definition;
            Req r = new Req(definition);
            String[] keys = keySchema(r.get("KeySchema"));
            this.hashKey = keys[0];
            this.rangeKey = keys[1];
            for (String kind : List.of("GlobalSecondaryIndexes", "LocalSecondaryIndexes")) {
                JsonNode list = r.get(kind);
                if (list != null) {
                    for (JsonNode index : list.asArray()) {
                        Req ir = new Req(index);
                        String[] indexKeys = keySchema(ir.get("KeySchema"));
                        indexes.put(ir.str("IndexName"), new Index(indexKeys[0], indexKeys[1]));
                    }
                }
            }
        }

        static String[] keySchema(JsonNode keySchema) {
            String[] result = new String[2];
            for (JsonNode element : keySchema.asArray()) {
                Req e = new Req(element);
                result["HASH".equals(e.str("KeyType")) ? 0 : 1] = e.str("AttributeName");
            }
            return result;
        }

        Index index(String indexName) {
            if (indexName == null) {
                return new Index(hashKey, rangeKey);
            }
            Index index = indexes.get(indexName);
            if (index == null) {
                throw DynamoDbError.validation("The table does not have the specified index: " + indexName);
            }
            return index;
        }

        List<String> keyAttributes() {
            return rangeKey == null ? List.of(hashKey) : List.of(hashKey, rangeKey);
        }

        Map<String, AttributeValue> keyAttributesOf(Map<String, AttributeValue> item) {
            Map<String, AttributeValue> key = new LinkedHashMap<>();
            for (String attribute : keyAttributes()) {
                AttributeValue value = item.get(attribute);
                if (value == null) {
                    throw DynamoDbError.validation("One or more parameter values were invalid: Missing the key "
                                                   + attribute + " in the item");
                }
                key.put(attribute, value);
            }
            return key;
        }

        String keyOf(Map<String, AttributeValue> item) {
            return keyOfKey(keyAttributesOf(item));
        }

        String keyOfKey(Map<String, AttributeValue> key) {
            StringBuilder s = new StringBuilder();
            for (String attribute : keyAttributes()) {
                AttributeValue value = key.get(attribute);
                if (value == null) {
                    throw DynamoDbError.validation("The provided key element does not match the schema");
                }
                s.append(value.type()).append(':').append(scalar(value)).append('\u0000');
            }
            return s.toString();
        }

        private static String scalar(AttributeValue value) {
            return switch (value.type()) {
                case S -> value.s();
                case N -> new java.math.BigDecimal(value.n()).stripTrailingZeros().toPlainString();
                case B -> java.util.Base64.getEncoder().encodeToString(value.b().asByteArray());
                default -> throw DynamoDbError.validation("The provided key element does not match the schema");
            };
        }

        void describe(JsonWriter w, String status) {
            Req r = new Req(definition);
            w.writeStartObject();
            w.writeFieldName("TableName").writeValue(name);
            w.writeFieldName("TableStatus").writeValue(status);
            w.writeFieldName("TableArn").writeValue("arn:aws:dynamodb:us-east-1:123456789012:table/" + name);
            w.writeFieldName("CreationDateTime").writeNumber(Long.toString(created));
            w.writeFieldName("ItemCount").writeNumber(Integer.toString(items.size()));
            w.writeFieldName("KeySchema").writeNode(r.get("KeySchema"));
            if (r.get("AttributeDefinitions") != null) {
                w.writeFieldName("AttributeDefinitions").writeNode(r.get("AttributeDefinitions"));
            }
            for (String kind : List.of("GlobalSecondaryIndexes", "LocalSecondaryIndexes")) {
                JsonNode list = r.get(kind);
                if (list == null) {
                    continue;
                }
                w.writeFieldName(kind).writeStartArray();
                for (JsonNode index : list.asArray()) {
                    w.writeStartObject();
                    index.asObject().forEach((member, value) -> {
                        if (!member.equals("ProvisionedThroughput")) {
                            w.writeFieldName(member).writeNode(value);
                        }
                    });
                    if (kind.startsWith("Global")) {
                        w.writeFieldName("IndexStatus").writeValue("ACTIVE");
                    }
                    w.writeEndObject();
                }
                w.writeEndArray();
            }
            if (r.get("StreamSpecification") != null) {
                w.writeFieldName("StreamSpecification").writeNode(r.get("StreamSpecification"));
            }
            w.writeEndObject();
        }
    }

    private record Index(String hashKey, String rangeKey) {
        boolean covers(Map<String, AttributeValue> item) {
            return item.containsKey(hashKey) && (rangeKey == null || item.containsKey(rangeKey));
        }

        Comparator<Map<String, AttributeValue>> order(Table table) {
            Comparator<Map<String, AttributeValue>> byRange = (a, b) -> {
                if (rangeKey == null) {
                    return 0;
                }
                Integer c = Av.compare(a.get(rangeKey), b.get(rangeKey));
                return c == null ? 0 : c;
            };
            return byRange.thenComparing(table::keyOf);
        }

        Map<String, AttributeValue> keyOf(Table table, Map<String, AttributeValue> item) {
            Map<String, AttributeValue> key = new LinkedHashMap<>(table.keyAttributesOf(item));
            key.put(hashKey, item.get(hashKey));
            if (rangeKey != null) {
                key.put(rangeKey, item.get(rangeKey));
            }
            return key;
        }
    }
}
