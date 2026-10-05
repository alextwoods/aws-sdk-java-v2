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

package software.amazon.awssdk.benchmark.dynamodb;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

/**
 * Canonical rendering of a deserialized response, ported from racecar's {@code ResponseDigest}: a timing comparison
 * is only meaningful if every arm produced the same result, and the timing loop discards responses.
 *
 * <p>Rendering is field by field with map keys sorted and every attribute value written with its type tag, so two
 * arms that differ only in map iteration order compare equal and anything else does not. {@link #expected(Scenario)}
 * renders the same fields from {@link DynamoDbItems}' model objects, which is what the canned response encodes; an
 * arm's digest must match it (so a wrong response is rejected, not just a response that differs between arms).
 */
final class ResponseDigest {

    private ResponseDigest() {
    }

    static String render(Scenario scenario, Object output) {
        return switch (scenario) {
            case SMALL_GET -> {
                GetItemResponse r = (GetItemResponse) output;
                yield "getItem.hasItem=" + r.hasItem() + "\ngetItem.item=" + renderItem(r.item())
                      + "\ngetItem.consumedCapacity=" + r.consumedCapacity();
            }
            case SMALL_PUT -> {
                PutItemResponse r = (PutItemResponse) output;
                yield "putItem.hasAttributes=" + r.hasAttributes() + "\nputItem.consumedCapacity="
                      + r.consumedCapacity() + "\nputItem.itemCollectionMetrics=" + r.itemCollectionMetrics();
            }
            case BATCH_GET -> {
                BatchGetItemResponse r = (BatchGetItemResponse) output;
                StringBuilder sb = new StringBuilder();
                sb.append("batchGetItem.tables=").append(new TreeMap<>(r.responses()).keySet());
                List<Map<String, AttributeValue>> items = r.responses().get(DynamoDbItems.TABLE_NAME);
                sb.append("\nbatchGetItem.itemCount=").append(items == null ? -1 : items.size());
                sb.append("\nbatchGetItem.hasUnprocessedKeys=").append(r.hasUnprocessedKeys())
                  .append(" size=").append(r.unprocessedKeys().size());
                if (items != null) {
                    for (int i = 0; i < items.size(); i++) {
                        sb.append("\nbatchGetItem.item[").append(i).append("]=").append(renderItem(items.get(i)));
                    }
                }
                yield sb.toString();
            }
            case BATCH_PUT -> {
                BatchWriteItemResponse r = (BatchWriteItemResponse) output;
                yield "batchWriteItem.hasUnprocessedItems=" + r.hasUnprocessedItems() + " size="
                      + r.unprocessedItems().size() + "\nbatchWriteItem.hasConsumedCapacity=" + r.hasConsumedCapacity()
                      + "\nbatchWriteItem.hasItemCollectionMetrics=" + r.hasItemCollectionMetrics();
            }
        };
    }

    /** The rendering the canned response must deserialize to, built from the model objects it was written from. */
    static String expected(Scenario scenario) {
        return switch (scenario) {
            case SMALL_GET -> "getItem.hasItem=true\ngetItem.item=" + renderItem(DynamoDbItems.smallItem())
                              + "\ngetItem.consumedCapacity=null";
            case SMALL_PUT -> "putItem.hasAttributes=false\nputItem.consumedCapacity=null"
                              + "\nputItem.itemCollectionMetrics=null";
            case BATCH_GET -> {
                StringBuilder sb = new StringBuilder();
                sb.append("batchGetItem.tables=[").append(DynamoDbItems.TABLE_NAME).append(']');
                sb.append("\nbatchGetItem.itemCount=").append(DynamoDbItems.BATCH_SIZE);
                sb.append("\nbatchGetItem.hasUnprocessedKeys=true size=0");
                List<Map<String, AttributeValue>> items = DynamoDbItems.batchItems();
                for (int i = 0; i < items.size(); i++) {
                    sb.append("\nbatchGetItem.item[").append(i).append("]=").append(renderItem(items.get(i)));
                }
                yield sb.toString();
            }
            case BATCH_PUT -> "batchWriteItem.hasUnprocessedItems=true size=0\nbatchWriteItem.hasConsumedCapacity=false"
                              + "\nbatchWriteItem.hasItemCollectionMetrics=false";
        };
    }

    static String sha256(String rendering) {
        return sha256Bytes(rendering.getBytes(StandardCharsets.UTF_8));
    }

    static String sha256Bytes(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String renderItem(Map<String, AttributeValue> item) {
        StringBuilder sb = new StringBuilder();
        appendMap(sb, item);
        return sb.toString();
    }

    private static void appendMap(StringBuilder sb, Map<String, AttributeValue> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, AttributeValue> e : new TreeMap<>(map).entrySet()) {
            sb.append(first ? "" : ",").append(e.getKey()).append('=');
            first = false;
            appendValue(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void appendValue(StringBuilder sb, AttributeValue v) {
        sb.append(v.type()).append(':');
        switch (v.type()) {
            case S -> sb.append(v.s());
            case N -> sb.append(v.n());
            case B -> sb.append(Base64.getEncoder().encodeToString(v.b().asByteArray()));
            case BOOL -> sb.append(v.bool());
            case NULL -> sb.append(v.nullValue());
            case SS -> sb.append(v.ss());
            case NS -> sb.append(v.ns());
            case BS -> sb.append(v.bs().stream().map(b -> Base64.getEncoder().encodeToString(b.asByteArray())).toList());
            case M -> appendMap(sb, v.m());
            case L -> {
                sb.append('[');
                for (int i = 0; i < v.l().size(); i++) {
                    sb.append(i > 0 ? "," : "");
                    appendValue(sb, v.l().get(i));
                }
                sb.append(']');
            }
            default -> sb.append("UNKNOWN");
        }
    }
}
