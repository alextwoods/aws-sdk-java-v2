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

package software.amazon.awssdk.test.v2migration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * An in-JVM DynamoDB (awsJson1.0) on the loopback interface, serving PutItem (with {@code attribute_not_exists}
 * conditions), GetItem and Scan (with {@code Limit} and {@code ExclusiveStartKey} paging) over the corpus tables. A
 * table that is not one of them answers {@code ResourceNotFoundException}; the first request to the {@code flaky}
 * table answers a retryable {@code InternalServerError}. Every request is recorded with its operation, parsed body,
 * audit headers and whether it was SigV4-signed, so a V2 and a V3 run of the same application can be compared.
 */
final class FakeDynamoDb implements AutoCloseable {
    private static final String ERROR_PREFIX = "com.amazonaws.dynamodb.v20120810#";
    private static final Map<String, String> TABLE_KEYS = Map.of("orders", "orderId", "catalog", "sku",
                                                                 "sessions", "user", "audit", "id", "flaky", "id");

    /** One received request. */
    record Request(String operation, ObjectNode body, Map<String, String> auditHeaders, boolean signed) {
        @Override
        public String toString() {
            return operation + " " + Node.printJson(body) + " " + auditHeaders + (signed ? " signed" : " unsigned");
        }
    }

    private final HttpServer server;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "fake-dynamodb");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, Map<String, ObjectNode>> tables = new ConcurrentHashMap<>();
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private final AtomicInteger flakyFailures = new AtomicInteger(1);

    FakeDynamoDb() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.setExecutor(executor);
        TABLE_KEYS.keySet().forEach(t -> tables.put(t, new TreeMap<>()));
        server.start();
    }

    String endpoint() {
        return "http://" + server.getAddress().getAddress().getHostAddress() + ":" + server.getAddress().getPort();
    }

    List<Request> requests() {
        return List.copyOf(requests);
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String target = Optional.ofNullable(exchange.getRequestHeaders().getFirst("X-Amz-Target")).orElse("");
            String operation = target.substring(target.indexOf('.') + 1);
            String text = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ObjectNode body = text.isBlank() ? Node.objectNode() : Node.parse(text).expectObjectNode();
            Map<String, String> audit = new TreeMap<>();
            exchange.getRequestHeaders().forEach((name, values) -> {
                String lower = name.toLowerCase();
                if (lower.startsWith("x-audit") || lower.startsWith("x-corpus")) {
                    audit.put(lower, String.join(",", values));
                }
            });
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            requests.add(new Request(operation, body, audit,
                                     authorization != null && authorization.startsWith("AWS4-HMAC-SHA256")));
            Answer answer;
            try {
                answer = answer(operation, body);
            } catch (RuntimeException e) {
                // A request the fake cannot interpret is a corpus failure, reported to the client as a modeled error.
                answer = error(400, "ValidationException", "The fake service cannot serve this request: " + e);
            }
            byte[] bytes = Node.printJson(answer.body()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/x-amz-json-1.0");
            exchange.getResponseHeaders().add("x-amzn-RequestId", "corpus-" + requests.size());
            exchange.sendResponseHeaders(answer.status(), bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    private record Answer(int status, ObjectNode body) {
    }

    private Answer answer(String operation, ObjectNode body) {
        String tableName = body.getStringMemberOrDefault("TableName", "");
        Map<String, ObjectNode> table = tables.get(tableName);
        if (table == null) {
            return error(400, "ResourceNotFoundException", "Requested resource not found");
        }
        if (tableName.equals("flaky") && flakyFailures.getAndDecrement() > 0) {
            return error(500, "InternalServerError", "Transient failure");
        }
        String keyName = TABLE_KEYS.get(tableName);
        switch (operation) {
            case "PutItem": {
                ObjectNode item = body.expectObjectMember("Item");
                String key = key(item, keyName);
                String condition = body.getStringMemberOrDefault("ConditionExpression", null);
                if (condition != null) {
                    if (!condition.equals("attribute_not_exists(" + keyName + ")")) {
                        return error(400, "ValidationException", "Unsupported condition " + condition);
                    }
                    if (table.containsKey(key)) {
                        return error(400, "ConditionalCheckFailedException", "The conditional request failed");
                    }
                }
                table.put(key, item);
                return new Answer(200, Node.objectNode());
            }
            case "GetItem": {
                ObjectNode item = table.get(key(body.expectObjectMember("Key"), keyName));
                return new Answer(200, item == null ? Node.objectNode() : Node.objectNode().withMember("Item", item));
            }
            case "Scan": {
                int limit = body.getNumberMemberOrDefault("Limit", Integer.MAX_VALUE).intValue();
                String after = body.getObjectMember("ExclusiveStartKey").map(k -> key(k, keyName)).orElse(null);
                List<Node> page = new ArrayList<>();
                String last = null;
                for (Map.Entry<String, ObjectNode> e : table.entrySet()) {
                    if (after != null && e.getKey().compareTo(after) <= 0) {
                        continue;
                    }
                    if (page.size() == limit) {
                        break;
                    }
                    page.add(e.getValue());
                    last = e.getKey();
                }
                ObjectNode.Builder out = Node.objectNodeBuilder()
                                             .withMember("Items", ArrayNode.fromNodes(page))
                                             .withMember("Count", page.size())
                                             .withMember("ScannedCount", page.size());
                if (page.size() == limit && last != null) {
                    out.withMember("LastEvaluatedKey", Node.objectNode().withMember(
                        keyName, Node.objectNode().withMember("S", last)));
                }
                return new Answer(200, out.build());
            }
            default:
                return error(400, "UnknownOperationException", "Unsupported operation " + operation);
        }
    }

    private static String key(ObjectNode item, String keyName) {
        return item.expectObjectMember(keyName).getMember("S").map(n -> n.expectStringNode().getValue())
                   .orElseThrow(() -> new IllegalArgumentException("Key " + keyName + " is not a string"));
    }

    private static Answer error(int status, String code, String message) {
        return new Answer(status, Node.objectNodeBuilder()
                                      .withMember("__type", StringNode.from(ERROR_PREFIX + code))
                                      .withMember("message", message)
                                      .build());
    }
}
