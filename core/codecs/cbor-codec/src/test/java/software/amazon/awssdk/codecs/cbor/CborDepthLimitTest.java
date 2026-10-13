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

package software.amazon.awssdk.codecs.cbor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;

/**
 * Port of smithy-java {@code software.amazon.smithy.java.cbor.CborDepthLimitTest}.
 */
class CborDepthLimitTest {
    private static final int MAX_DEPTH = 1000;

    // CBOR indefinite-length array: 0x9F start, 0xFF break
    // CBOR indefinite-length map: 0xBF start, 0xFF break
    // CBOR single-byte text string "a": 0x61 0x61

    private static byte[] nestedArrays(int depth) {
        // Each level: 0x9F (start array), innermost: 0x01 (integer 1), then depth x 0xFF (breaks)
        byte[] payload = new byte[depth + 1 + depth];
        for (int i = 0; i < depth; i++) {
            payload[i] = (byte) 0x9F;
        }
        // integer 1 at the bottom
        payload[depth] = 0x01;
        for (int i = 0; i < depth; i++) {
            payload[depth + 1 + i] = (byte) 0xFF;
        }
        return payload;
    }

    private static byte[] nestedMaps(int depth) {
        // Each level: 0xBF (start map) + key "a" (0x61 0x61), innermost: 0x01, then depth x 0xFF
        byte[] payload = new byte[depth * 3 + 1 + depth];
        int pos = 0;
        for (int i = 0; i < depth; i++) {
            payload[pos++] = (byte) 0xBF;
            payload[pos++] = 0x61;
            payload[pos++] = 0x61;
        }
        payload[pos++] = 0x01;
        for (int i = 0; i < depth; i++) {
            payload[pos++] = (byte) 0xFF;
        }
        return payload;
    }

    private static ShapeDeserializer deserializer(byte[] payload) {
        return Rpcv2CborCodec.builder().build().createDeserializer(payload);
    }

    @Test
    void nestedArraysAtLimitSucceeds() {
        assertNotNull(deserializer(nestedArrays(MAX_DEPTH)).readDocument());
    }

    @Test
    void nestedArraysOverLimitThrows() {
        ShapeDeserializer de = deserializer(nestedArrays(MAX_DEPTH + 1));
        assertThrows(SerializationException.class, de::readDocument);
    }

    @Test
    void nestedMapsAtLimitSucceeds() {
        assertNotNull(deserializer(nestedMaps(MAX_DEPTH)).readDocument());
    }

    @Test
    void nestedMapsOverLimitThrows() {
        ShapeDeserializer de = deserializer(nestedMaps(MAX_DEPTH + 1));
        assertThrows(SerializationException.class, de::readDocument);
    }

    @Test
    void siblingContainersDoNotAccumulateDepth() {
        // A flat array containing many empty nested arrays should not trigger the limit:
        // [ [], [], [], ... ] with MAX_DEPTH + 10 siblings
        int siblings = MAX_DEPTH + 10;
        byte[] payload = new byte[1 + siblings * 2 + 1];
        int pos = 0;
        payload[pos++] = (byte) 0x9F;
        for (int i = 0; i < siblings; i++) {
            payload[pos++] = (byte) 0x9F;
            payload[pos++] = (byte) 0xFF;
        }
        payload[pos] = (byte) 0xFF;
        assertNotNull(deserializer(payload).readDocument());
    }

    @Test
    void shallowPayloadSucceeds() {
        // {"a": [1, 2, 3]}
        byte[] payload = {
            (byte) 0xBF, // start map
            0x61,
            0x61, // key "a"
            (byte) 0x9F, // start array
            0x01,
            0x02,
            0x03, // integers 1, 2, 3
            (byte) 0xFF, // end array
            (byte) 0xFF // end map
        };
        assertNotNull(deserializer(payload).readDocument());
    }
}
