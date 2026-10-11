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

package software.amazon.awssdk.protocol.smithytests;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Structural comparison of decoded data-model trees, used by the JSON and CBOR comparators.
 *
 * <p>A tree node is one of: {@code null}, {@link Boolean}, {@link String}, {@code byte[]}, {@link BigInteger} (integers),
 * {@link Double} (floats), {@link BigDecimal} (JSON numbers and CBOR decimal fractions), {@code List<Object>} or
 * {@code Map<String, Object>}. Maps compare without regard to key order, lists in order, byte arrays by content,
 * {@code Double}s with {@link Double#equals} (so NaN equals NaN), and {@code BigDecimal}s by numeric value.
 */
final class TreeComparison {
    private static final int MAX_REPORTED = 20;

    private TreeComparison() {
    }

    /**
     * Throw an {@link AssertionError} listing up to 20 differences (each with its path) if the trees differ.
     */
    static void assertTreesEqual(String format, Object expected, Object actual, String expectedText, String actualText) {
        List<String> differences = new ArrayList<>();
        compare("$", expected, actual, differences);
        if (!differences.isEmpty()) {
            StringBuilder message = new StringBuilder(format).append(" bodies differ:");
            for (int i = 0; i < Math.min(differences.size(), MAX_REPORTED); i++) {
                message.append("\n  ").append(differences.get(i));
            }
            if (differences.size() > MAX_REPORTED) {
                message.append("\n  ... and ").append(differences.size() - MAX_REPORTED).append(" more");
            }
            message.append("\nExpected:\n").append(expectedText).append("\nActual:\n").append(actualText);
            throw new AssertionError(message.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static void compare(String path, Object expected, Object actual, List<String> differences) {
        if (expected == null || actual == null) {
            if (expected != actual) {
                differences.add(path + ": expected " + describe(expected) + " but was " + describe(actual));
            }
            return;
        }
        if (expected instanceof Map && actual instanceof Map) {
            compareMaps(path, (Map<String, Object>) expected, (Map<String, Object>) actual, differences);
        } else if (expected instanceof List && actual instanceof List) {
            compareLists(path, (List<Object>) expected, (List<Object>) actual, differences);
        } else if (expected instanceof BigDecimal && actual instanceof BigDecimal) {
            if (((BigDecimal) expected).compareTo((BigDecimal) actual) != 0) {
                differences.add(path + ": expected " + describe(expected) + " but was " + describe(actual));
            }
        } else if (expected instanceof byte[] && actual instanceof byte[]) {
            if (!Arrays.equals((byte[]) expected, (byte[]) actual)) {
                differences.add(path + ": expected " + describe(expected) + " but was " + describe(actual));
            }
        } else if (!expected.equals(actual)) {
            differences.add(path + ": expected " + describe(expected) + " but was " + describe(actual));
        }
    }

    private static void compareMaps(String path, Map<String, Object> expected, Map<String, Object> actual,
                                    List<String> differences) {
        Set<String> keys = new TreeSet<>(expected.keySet());
        keys.addAll(actual.keySet());
        for (String key : keys) {
            String childPath = path + "." + key;
            if (!actual.containsKey(key)) {
                differences.add(childPath + ": missing (expected " + describe(expected.get(key)) + ")");
            } else if (!expected.containsKey(key)) {
                differences.add(childPath + ": unexpected member " + describe(actual.get(key)));
            } else {
                compare(childPath, expected.get(key), actual.get(key), differences);
            }
        }
    }

    private static void compareLists(String path, List<Object> expected, List<Object> actual, List<String> differences) {
        if (expected.size() != actual.size()) {
            differences.add(path + ": expected " + expected.size() + " elements but was " + actual.size());
            return;
        }
        for (int i = 0; i < expected.size(); i++) {
            compare(path + "[" + i + "]", expected.get(i), actual.get(i), differences);
        }
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof byte[]) {
            return "blob(base64:" + Base64.getEncoder().encodeToString((byte[]) value) + ")";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        if (value instanceof Map) {
            return "object" + ((Map<?, ?>) value).keySet();
        }
        if (value instanceof List) {
            return "array(" + ((List<?>) value).size() + ")";
        }
        return value.getClass().getSimpleName().toLowerCase(Locale.ROOT) + " " + value;
    }
}
