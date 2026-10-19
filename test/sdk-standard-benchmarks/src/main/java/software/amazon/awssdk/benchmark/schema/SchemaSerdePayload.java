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

package software.amazon.awssdk.benchmark.schema;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.services.schemabenchjson10.model.Inner;
import software.amazon.awssdk.services.schemabenchjson10.model.Item;
import software.amazon.awssdk.services.schemabenchjson10.model.PutPayloadRequest;

/**
 * The payloads of the schema-serde benchmarks: a structure with a string, a timestamp, a blob, a nested structure, a
 * list of structures and a map of structures; each list item has a long, a double, a timestamp, a blob, a string map,
 * a double list and a nested structure with a string list.
 *
 * <p>The payload is built once with the generated classes of one benchmark service and copied into the generated
 * classes of each other service through {@link JsonCodec}: every service has the same member names, so the values are
 * equal, and the benchmarks measure each protocol on its own generated classes.
 */
public enum SchemaSerdePayload {
    /**
     * A serialized body of 1 KB or less in every protocol (Requirement 34.1).
     */
    SMALL(1, 1, 1, 1, 8),
    /**
     * A serialized body of 100 KB or more in every protocol (Requirement 34.1).
     */
    LARGE(400, 4, 8, 4, 96);

    private final int items;
    private final int attributes;
    private final int values;
    private final int tags;
    private final int blobBytes;
    private final byte[] json;

    SchemaSerdePayload(int items, int attributes, int values, int tags, int blobBytes) {
        this.items = items;
        this.attributes = attributes;
        this.values = values;
        this.tags = tags;
        this.blobBytes = blobBytes;
        this.json = toBytes(Constants.CODEC.serialize(build()));
    }

    /**
     * The payload as the input of {@code protocol}'s {@code PutPayload}.
     */
    public SerializableStruct input(SchemaSerdeProtocol protocol) {
        return Constants.CODEC.deserializeShape(json, protocol.putPayload().inputBuilder());
    }

    /**
     * The payload as the output of {@code protocol}'s {@code GetPayload}.
     */
    public SerializableStruct output(SchemaSerdeProtocol protocol) {
        ApiOperation<?, ?> operation = protocol.getPayload();
        return Constants.CODEC.deserializeShape(json, operation.outputBuilder());
    }

    private PutPayloadRequest build() {
        List<Item> itemList = new ArrayList<>(items);
        for (int i = 0; i < items; i++) {
            Map<String, String> attributeMap = new LinkedHashMap<>();
            for (int a = 0; a < attributes; a++) {
                attributeMap.put("key-" + a, "value-" + i + "-" + a);
            }
            List<Double> valueList = new ArrayList<>(values);
            for (int v = 0; v < values; v++) {
                valueList.add(i * 1.5 + v);
            }
            itemList.add(Item.builder()
                             .name("item-" + i)
                             .value(1_000_000L + i)
                             .score(i / 3.0)
                             .updatedAt(Constants.TIME.plusSeconds(i))
                             .data(blob(i))
                             .attributes(attributeMap)
                             .values(valueList)
                             .inner(inner(i))
                             .build());
        }
        Map<String, Inner> tagMap = new LinkedHashMap<>();
        for (int t = 0; t < tags; t++) {
            tagMap.put("tag-" + t, inner(t));
        }
        return PutPayloadRequest.builder()
                                .id("payload-" + name())
                                .createdAt(Constants.TIME)
                                .data(blob(-1))
                                .nested(inner(-1))
                                .items(itemList)
                                .tags(tagMap)
                                .build();
    }

    private Inner inner(int i) {
        return Inner.builder().label("inner-" + i).count(i).enabled(i % 2 == 0).flags("flag-a").build();
    }

    private SdkBytes blob(int seed) {
        byte[] bytes = new byte[blobBytes];
        for (int b = 0; b < bytes.length; b++) {
            bytes[b] = (byte) (seed * 31 + b);
        }
        return SdkBytes.fromByteArrayUnsafe(bytes);
    }

    private static byte[] toBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    /**
     * Constants the enum constructors use; enum constants are initialized before the enum's own static fields.
     */
    private static final class Constants {
        private static final Instant TIME = Instant.ofEpochMilli(1_700_000_000_123L);
        private static final JsonCodec CODEC = JsonCodec.builder().build();
    }
}
