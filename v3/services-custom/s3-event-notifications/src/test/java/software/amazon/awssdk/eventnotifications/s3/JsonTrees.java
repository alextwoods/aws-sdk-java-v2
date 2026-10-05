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

package software.amazon.awssdk.eventnotifications.s3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import software.amazon.awssdk.eventnotifications.s3.internal.JsonTree;

/**
 * Test helpers over {@link JsonTree} values: parsing, a minimal compact serializer, random JSON values, and the
 * injection of unknown members into every fixed-schema object of an event notification message.
 */
final class JsonTrees {
    /** Objects whose member names are data, not schema: unknown members would change their value. */
    private static final Set<String> DYNAMIC_OBJECTS = Set.of("awsGeneratedTags", "eventHoldDuration");

    private JsonTrees() {
    }

    static Object parse(String json) {
        try {
            return JsonTree.parse(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                quote((String) e.getKey(), out);
                out.append(':');
                write(e.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                write(list.get(i), out);
            }
            out.append(']');
        } else if (value instanceof String s) {
            quote(s, out);
        } else {
            out.append(value);
        }
    }

    private static void quote(String s, StringBuilder out) {
        out.append('"');
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        out.append('"');
    }

    /** A copy of {@code tree} with random unknown members added to every fixed-schema object. */
    static Object withUnknownMembers(Object tree, Random random) {
        return inject(tree, null, random);
    }

    private static Object inject(Object value, String fieldName, Random random) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            boolean dynamic = fieldName != null && DYNAMIC_OBJECTS.contains(fieldName);
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!dynamic && random.nextBoolean()) {
                    copy.put("unknown" + random.nextInt(1000), randomValue(random, 3));
                }
                copy.put((String) e.getKey(), dynamic ? e.getValue() : inject(e.getValue(), (String) e.getKey(), random));
            }
            if (!dynamic) {
                copy.put("unknownLast" + random.nextInt(1000), randomValue(random, 3));
            }
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            list.forEach(e -> copy.add(inject(e, fieldName, random)));
            return copy;
        }
        return value;
    }

    /** A random JSON value of at most {@code depth} nested levels. */
    static Object randomValue(Random random, int depth) {
        int kind = random.nextInt(depth > 0 ? 9 : 7);
        return switch (kind) {
            case 0 -> JsonTree.NULL;
            case 1 -> random.nextBoolean();
            case 2 -> new JsonTree.JsonNumber(Long.toString(random.nextLong()));
            case 3 -> new JsonTree.JsonNumber("-1.5e-3");
            case 4 -> new JsonTree.JsonNumber("0.25");
            case 5 -> "str\"\\\n" + random.nextInt(10);
            case 6 -> "";
            case 7 -> {
                Map<String, Object> map = new LinkedHashMap<>();
                int size = random.nextInt(3);
                for (int i = 0; i < size; i++) {
                    map.put("k" + i, randomValue(random, depth - 1));
                }
                yield map;
            }
            default -> {
                List<Object> list = new ArrayList<>();
                int size = random.nextInt(3);
                for (int i = 0; i < size; i++) {
                    list.add(randomValue(random, depth - 1));
                }
                yield list;
            }
        };
    }
}
