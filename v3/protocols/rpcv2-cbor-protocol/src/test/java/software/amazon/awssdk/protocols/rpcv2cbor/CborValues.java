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

package software.amazon.awssdk.protocols.rpcv2cbor;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Test support: an independent CBOR (RFC 8949) reader and a minimal writer, so that test oracles do not depend on the
 * codec under test.
 *
 * <p>{@link #decode} returns the semantic value of a CBOR data item, in which values that the rpcv2Cbor protocol
 * treats as equal compare equal with {@link Object#equals}: definite and indefinite lengths are not distinguished,
 * chunked strings are joined, map entry order is insignificant, and every finite number (integer, half/single/double
 * float, bignum, decimal fraction) is its exact numeric value as a {@link BigDecimal} without trailing zeros, so that
 * {@code 1}, {@code 1.0} and a float16 {@code 1.0} are equal. Non-finite floats are {@link Double}s, byte strings
 * {@link Bytes}, tag-1 timestamps {@link Timestamp}s and other tags {@link Tagged}. {@code null} and
 * {@code undefined} are {@code null}.
 */
final class CborValues {
    /** A byte string. */
    record Bytes(String hex) {
        static Bytes of(byte[] bytes) {
            return new Bytes(HexFormat.of().formatHex(bytes));
        }
    }

    /** A tag-1 (epoch-seconds) timestamp; {@code seconds} is a semantic number. */
    record Timestamp(Object seconds) {
    }

    /** A data item with a tag this reader does not interpret. */
    record Tagged(long tag, Object value) {
    }

    private static final Object BREAK = new Object();

    private CborValues() {
    }

    /**
     * The semantic value of {@code bytes}, which must hold exactly one well-formed data item.
     *
     * @throws IllegalArgumentException if they do not
     */
    static Object decode(byte[] bytes) {
        Reader reader = new Reader(bytes);
        Object value = reader.read();
        if (reader.pos != bytes.length) {
            throw new IllegalArgumentException("Trailing bytes after the CBOR data item at offset " + reader.pos);
        }
        return value;
    }

    /** The semantic value of a number. */
    static Object number(BigDecimal value) {
        return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }

    /**
     * Encodes a value as definite-length CBOR: {@link Map} (string keys, iteration order), {@link List},
     * {@link String}, {@link byte[]}, {@link Boolean}, {@link Integer}/{@link Long}, {@link Double} (as a double),
     * {@link Timestamp} (tag 1 over its number) and {@code null}.
     */
    static byte[] encode(Object value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, value);
        return out.toByteArray();
    }

    /** A map of alternating keys and values, in order, for {@link #encode}. */
    static Map<String, Object> map(Object... entries) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            out.put((String) entries[i], entries[i + 1]);
        }
        return out;
    }

    private static void write(ByteArrayOutputStream out, Object value) {
        if (value == null) {
            out.write(0xF6);
        } else if (value instanceof Boolean b) {
            out.write(b ? 0xF5 : 0xF4);
        } else if (value instanceof Integer || value instanceof Long) {
            long n = ((Number) value).longValue();
            if (n >= 0) {
                head(out, 0, n);
            } else {
                head(out, 1, -1 - n);
            }
        } else if (value instanceof Double d) {
            out.write(0xFB);
            long bits = Double.doubleToLongBits(d);
            for (int shift = 56; shift >= 0; shift -= 8) {
                out.write((int) (bits >>> shift) & 0xFF);
            }
        } else if (value instanceof String s) {
            byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
            head(out, 3, utf8.length);
            out.writeBytes(utf8);
        } else if (value instanceof byte[] bytes) {
            head(out, 2, bytes.length);
            out.writeBytes(bytes);
        } else if (value instanceof Timestamp t) {
            head(out, 6, 1);
            write(out, t.seconds());
        } else if (value instanceof List<?> list) {
            head(out, 4, list.size());
            list.forEach(element -> write(out, element));
        } else if (value instanceof Map<?, ?> map) {
            head(out, 5, map.size());
            map.forEach((k, v) -> {
                write(out, k);
                write(out, v);
            });
        } else {
            throw new IllegalArgumentException("Cannot encode " + value.getClass());
        }
    }

    private static void head(ByteArrayOutputStream out, int major, long argument) {
        int type = major << 5;
        if (argument < 24) {
            out.write(type | (int) argument);
        } else if (argument < 0x100) {
            out.write(type | 24);
            out.write((int) argument);
        } else if (argument < 0x10000) {
            out.write(type | 25);
            out.write((int) (argument >>> 8));
            out.write((int) argument & 0xFF);
        } else if (argument < 0x100000000L) {
            out.write(type | 26);
            for (int shift = 24; shift >= 0; shift -= 8) {
                out.write((int) (argument >>> shift) & 0xFF);
            }
        } else {
            out.write(type | 27);
            for (int shift = 56; shift >= 0; shift -= 8) {
                out.write((int) (argument >>> shift) & 0xFF);
            }
        }
    }

    private static final class Reader {
        private final byte[] bytes;
        private int pos;

        Reader(byte[] bytes) {
            this.bytes = bytes;
        }

        Object read() {
            Object value = item();
            if (value == BREAK) {
                throw new IllegalArgumentException("Unexpected break at offset " + (pos - 1));
            }
            return value;
        }

        private Object item() {
            int initial = u8();
            int major = initial >>> 5;
            int minor = initial & 0x1F;
            if (major == 7) {
                return simple(minor);
            }
            if (minor == 31) {
                return indefinite(major);
            }
            BigInteger argument = argument(minor);
            return switch (major) {
                case 0 -> number(new BigDecimal(argument));
                case 1 -> number(new BigDecimal(argument.negate().subtract(BigInteger.ONE)));
                case 2 -> Bytes.of(take(argument));
                case 3 -> new String(take(argument), StandardCharsets.UTF_8);
                case 4 -> {
                    List<Object> list = new ArrayList<>();
                    for (long i = 0, n = argument.longValueExact(); i < n; i++) {
                        list.add(read());
                    }
                    yield list;
                }
                case 5 -> {
                    Map<Object, Object> map = new HashMap<>();
                    for (long i = 0, n = argument.longValueExact(); i < n; i++) {
                        put(map, read(), read());
                    }
                    yield map;
                }
                case 6 -> tag(argument.longValueExact(), read());
                default -> throw new IllegalStateException("major type " + major);
            };
        }

        private Object indefinite(int major) {
            switch (major) {
                case 2, 3 -> {
                    ByteArrayOutputStream joined = new ByteArrayOutputStream();
                    for (Object chunk = item(); chunk != BREAK; chunk = item()) {
                        if (major == 2 && chunk instanceof Bytes b) {
                            joined.writeBytes(HexFormat.of().parseHex(b.hex()));
                        } else if (major == 3 && chunk instanceof String s) {
                            joined.writeBytes(s.getBytes(StandardCharsets.UTF_8));
                        } else {
                            throw new IllegalArgumentException("Bad chunk in an indefinite-length string");
                        }
                    }
                    return major == 2 ? Bytes.of(joined.toByteArray())
                                      : new String(joined.toByteArray(), StandardCharsets.UTF_8);
                }
                case 4 -> {
                    List<Object> list = new ArrayList<>();
                    for (Object element = item(); element != BREAK; element = item()) {
                        list.add(element);
                    }
                    return list;
                }
                case 5 -> {
                    Map<Object, Object> map = new HashMap<>();
                    for (Object key = item(); key != BREAK; key = item()) {
                        put(map, key, read());
                    }
                    return map;
                }
                default -> throw new IllegalArgumentException("Major type " + major + " has no indefinite length");
            }
        }

        private static void put(Map<Object, Object> map, Object key, Object value) {
            if (map.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate map key " + key);
            }
            map.put(key, value);
        }

        private Object simple(int minor) {
            return switch (minor) {
                case 20 -> Boolean.FALSE;
                case 21 -> Boolean.TRUE;
                case 22, 23 -> null;
                case 25 -> floating(Float.float16ToFloat((short) uint(2)));
                case 26 -> floating(Float.intBitsToFloat((int) uint(4)));
                case 27 -> floating(Double.longBitsToDouble(uint(8)));
                case 31 -> BREAK;
                default -> throw new IllegalArgumentException("Unsupported simple value " + minor);
            };
        }

        private static Object floating(double value) {
            return Double.isFinite(value) ? number(new BigDecimal(value)) : Double.valueOf(value);
        }

        private static Object tag(long tag, Object value) {
            if (tag == 1) {
                return new Timestamp(value);
            }
            if ((tag == 2 || tag == 3) && value instanceof Bytes b) {
                BigInteger n = new BigInteger(1, HexFormat.of().parseHex(b.hex()));
                return number(new BigDecimal(tag == 2 ? n : n.negate().subtract(BigInteger.ONE)));
            }
            if (tag == 4 && value instanceof List<?> pair && pair.size() == 2
                && pair.get(0) instanceof BigDecimal exponent && pair.get(1) instanceof BigDecimal mantissa) {
                return number(mantissa.scaleByPowerOfTen(exponent.intValueExact()));
            }
            return new Tagged(tag, value);
        }

        private BigInteger argument(int minor) {
            if (minor < 24) {
                return BigInteger.valueOf(minor);
            }
            int length = switch (minor) {
                case 24 -> 1;
                case 25 -> 2;
                case 26 -> 4;
                case 27 -> 8;
                default -> throw new IllegalArgumentException("Reserved additional information " + minor);
            };
            return new BigInteger(1, take(BigInteger.valueOf(length)));
        }

        private long uint(int length) {
            long value = 0;
            for (int i = 0; i < length; i++) {
                value = value << 8 | u8();
            }
            return value;
        }

        private byte[] take(BigInteger length) {
            int n = length.intValueExact();
            if (n > bytes.length - pos) {
                throw new IllegalArgumentException("Truncated CBOR at offset " + pos);
            }
            byte[] out = java.util.Arrays.copyOfRange(bytes, pos, pos + n);
            pos += n;
            return out;
        }

        private int u8() {
            if (pos >= bytes.length) {
                throw new IllegalArgumentException("Truncated CBOR at offset " + pos);
            }
            return bytes[pos++] & 0xFF;
        }
    }
}
