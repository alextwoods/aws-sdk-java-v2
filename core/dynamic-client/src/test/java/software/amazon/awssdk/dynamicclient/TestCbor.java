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

package software.amazon.awssdk.dynamicclient;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal CBOR (RFC 8949) encoder and decoder for test fixtures, independent of the SDK's CBOR codec, so that
 * rpcv2Cbor responses are written and requests are read without the SDK's own serde.
 */
final class TestCbor {
    private static final int BREAK = 0xff;

    private TestCbor() {
    }

    /**
     * Encodes a definite-length map of text string keys to text string values, in the given order.
     */
    static byte[] textMap(String... keysAndValues) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeHead(out, 5, keysAndValues.length / 2);
        for (String text : keysAndValues) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            writeHead(out, 3, bytes.length);
            out.write(bytes, 0, bytes.length);
        }
        return out.toByteArray();
    }

    /**
     * Decodes one data item: maps become {@link LinkedHashMap}s, arrays {@link List}s, text strings {@link String}s,
     * byte strings {@link ByteBuffer}s (which compare by content), integers {@link Long}s, floats {@link Double}s,
     * and {@code true}, {@code false} and {@code null} their Java values. Tags are skipped.
     */
    static Object decode(byte[] bytes) {
        Reader reader = new Reader(bytes);
        Object value = reader.item();
        if (reader.position != bytes.length) {
            throw new IllegalArgumentException("Trailing bytes after the CBOR item at " + reader.position);
        }
        return value;
    }

    private static void writeHead(ByteArrayOutputStream out, int majorType, long argument) {
        int major = majorType << 5;
        if (argument < 24) {
            out.write(major | (int) argument);
        } else if (argument < 0x100) {
            out.write(major | 24);
            out.write((int) argument);
        } else if (argument < 0x10000) {
            out.write(major | 25);
            out.write((int) (argument >> 8));
            out.write((int) argument);
        } else {
            out.write(major | 26);
            for (int shift = 24; shift >= 0; shift -= 8) {
                out.write((int) (argument >> shift));
            }
        }
    }

    private static final class Reader {
        private final byte[] bytes;
        private int position;

        private Reader(byte[] bytes) {
            this.bytes = bytes;
        }

        private Object item() {
            int initial = next();
            int major = initial >> 5;
            int info = initial & 0x1f;
            switch (major) {
                case 0:
                    return argument(info);
                case 1:
                    return -1 - argument(info);
                case 2:
                    return ByteBuffer.wrap(string(info, 2));
                case 3:
                    return new String(string(info, 3), StandardCharsets.UTF_8);
                case 4:
                    return array(info);
                case 5:
                    return map(info);
                case 6:
                    argument(info);
                    return item();
                default:
                    return simple(info);
            }
        }

        private List<Object> array(int info) {
            List<Object> values = new ArrayList<>();
            if (info == 31) {
                while (peek() != BREAK) {
                    values.add(item());
                }
                position++;
            } else {
                for (long i = argument(info); i > 0; i--) {
                    values.add(item());
                }
            }
            return values;
        }

        private Map<Object, Object> map(int info) {
            Map<Object, Object> values = new LinkedHashMap<>();
            if (info == 31) {
                while (peek() != BREAK) {
                    values.put(item(), item());
                }
                position++;
            } else {
                for (long i = argument(info); i > 0; i--) {
                    values.put(item(), item());
                }
            }
            return values;
        }

        private byte[] string(int info, int major) {
            if (info != 31) {
                int length = (int) argument(info);
                byte[] value = new byte[length];
                System.arraycopy(bytes, position, value, 0, length);
                position += length;
                return value;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (peek() != BREAK) {
                int chunk = next();
                if (chunk >> 5 != major) {
                    throw new IllegalArgumentException("Mixed chunk types in an indefinite-length string");
                }
                byte[] value = string(chunk & 0x1f, major);
                out.write(value, 0, value.length);
            }
            position++;
            return out.toByteArray();
        }

        private Object simple(int info) {
            switch (info) {
                case 20:
                    return false;
                case 21:
                    return true;
                case 22:
                case 23:
                    return null;
                case 25:
                    return halfFloat((int) bigEndian(2));
                case 26:
                    return (double) Float.intBitsToFloat((int) bigEndian(4));
                case 27:
                    return Double.longBitsToDouble(bigEndian(8));
                default:
                    throw new IllegalArgumentException("Unsupported CBOR simple value " + info);
            }
        }

        private long argument(int info) {
            if (info < 24) {
                return info;
            }
            switch (info) {
                case 24:
                    return bigEndian(1);
                case 25:
                    return bigEndian(2);
                case 26:
                    return bigEndian(4);
                case 27:
                    return bigEndian(8);
                default:
                    throw new IllegalArgumentException("Unsupported CBOR argument " + info);
            }
        }

        private long bigEndian(int length) {
            long value = 0;
            for (int i = 0; i < length; i++) {
                value = (value << 8) | next();
            }
            return value;
        }

        private int next() {
            return bytes[position++] & 0xff;
        }

        private int peek() {
            return bytes[position] & 0xff;
        }

        private static double halfFloat(int bits) {
            int exponent = (bits >> 10) & 0x1f;
            int mantissa = bits & 0x3ff;
            double value;
            if (exponent == 0) {
                value = mantissa * Math.pow(2, -24);
            } else if (exponent == 31) {
                value = mantissa == 0 ? Double.POSITIVE_INFINITY : Double.NaN;
            } else {
                value = (mantissa + 1024) * Math.pow(2, exponent - 25);
            }
            return (bits & 0x8000) != 0 ? -value : value;
        }
    }
}
