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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;

/**
 * Member-by-member comparison of generated shapes for {@link ProtocolAsserts#outputEquals}. The rules are listed there.
 *
 * <p>Documents are the SDK's current {@link Document}; the schema-based Document (with its {@code NUMBER_PROMOTION}
 * equality flag) is added in phase 7 and gets the same number-promotion semantics here when it lands.
 */
final class ShapeComparison {
    private static final int MAX_REPORTED = 20;

    private ShapeComparison() {
    }

    static void assertShapesEqual(Object expected, Object actual) {
        List<String> differences = new ArrayList<>();
        compare("$", expected, actual, differences);
        if (!differences.isEmpty()) {
            StringBuilder message = new StringBuilder("Output differs from the expected params:");
            for (int i = 0; i < Math.min(differences.size(), MAX_REPORTED); i++) {
                message.append("\n  ").append(differences.get(i));
            }
            if (differences.size() > MAX_REPORTED) {
                message.append("\n  ... and ").append(differences.size() - MAX_REPORTED).append(" more");
            }
            message.append("\nExpected: ").append(expected).append("\nActual:   ").append(actual);
            throw new AssertionError(message.toString());
        }
    }

    private static void compare(String path, Object expected, Object actual, List<String> differences) {
        if (expected == null || actual == null) {
            if (expected != actual) {
                differences.add(mismatch(path, expected, actual));
            }
            return;
        }
        if (isBlob(expected) && isBlob(actual)) {
            if (!Arrays.equals(bytes(expected), bytes(actual))) {
                differences.add(mismatch(path, expected, actual));
            }
            return;
        }
        if (expected instanceof Document && actual instanceof Document) {
            compareDocuments(path, (Document) expected, (Document) actual, differences);
            return;
        }
        if (expected instanceof List && actual instanceof List) {
            compareLists(path, (List<?>) expected, (List<?>) actual, differences);
            return;
        }
        if (expected instanceof Map && actual instanceof Map) {
            compareMaps(path, (Map<?, ?>) expected, (Map<?, ?>) actual, differences);
            return;
        }
        if (expected.getClass() != actual.getClass()) {
            differences.add(path + ": expected type " + expected.getClass().getName() + " but was "
                            + actual.getClass().getName());
            return;
        }
        if (expected instanceof SdkPojo) {
            comparePojos(path, (SdkPojo) expected, (SdkPojo) actual, differences);
        } else if (expected instanceof BigDecimal) {
            if (((BigDecimal) expected).compareTo((BigDecimal) actual) != 0) {
                differences.add(mismatch(path, expected, actual));
            }
        } else if (!expected.equals(actual)) {
            // Float.equals and Double.equals treat NaN as equal to NaN; Instant.equals compares instants.
            differences.add(mismatch(path, expected, actual));
        }
    }

    private static void comparePojos(String path, SdkPojo expected, SdkPojo actual, List<String> differences) {
        for (SdkField<?> field : expected.sdkFields()) {
            compare(path + "." + field.memberName(), field.getValueOrDefault(expected), field.getValueOrDefault(actual),
                    differences);
        }
    }

    private static void compareLists(String path, List<?> expected, List<?> actual, List<String> differences) {
        if (expected.size() != actual.size()) {
            differences.add(path + ": expected " + expected.size() + " elements but was " + actual.size()
                            + " (expected " + expected + ", actual " + actual + ")");
            return;
        }
        for (int i = 0; i < expected.size(); i++) {
            compare(path + "[" + i + "]", expected.get(i), actual.get(i), differences);
        }
    }

    private static void compareMaps(String path, Map<?, ?> expected, Map<?, ?> actual, List<String> differences) {
        Set<Object> keys = new LinkedHashSet<>(expected.keySet());
        keys.addAll(actual.keySet());
        for (Object key : keys) {
            String childPath = path + "[" + key + "]";
            if (!actual.containsKey(key)) {
                differences.add(childPath + ": missing (expected " + expected.get(key) + ")");
            } else if (!expected.containsKey(key)) {
                differences.add(childPath + ": unexpected entry " + actual.get(key));
            } else {
                compare(childPath, expected.get(key), actual.get(key), differences);
            }
        }
    }

    private static void compareDocuments(String path, Document expected, Document actual, List<String> differences) {
        if (expected.isNumber() && actual.isNumber()) {
            if (expected.asNumber().bigDecimalValue().compareTo(actual.asNumber().bigDecimalValue()) != 0) {
                differences.add(mismatch(path, expected, actual));
            }
        } else if (expected.isList() && actual.isList()) {
            compareLists(path, expected.asList(), actual.asList(), differences);
        } else if (expected.isMap() && actual.isMap()) {
            compareMaps(path, expected.asMap(), actual.asMap(), differences);
        } else if (!expected.equals(actual)) {
            differences.add(mismatch(path, expected, actual));
        }
    }

    private static boolean isBlob(Object value) {
        return value instanceof SdkBytes || value instanceof byte[] || value instanceof ByteBuffer;
    }

    private static byte[] bytes(Object blob) {
        if (blob instanceof SdkBytes) {
            return ((SdkBytes) blob).asByteArrayUnsafe();
        }
        if (blob instanceof byte[]) {
            return (byte[]) blob;
        }
        ByteBuffer buffer = ((ByteBuffer) blob).asReadOnlyBuffer();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    private static String mismatch(String path, Object expected, Object actual) {
        return path + ": expected " + describe(expected) + " but was " + describe(actual);
    }

    private static String describe(Object value) {
        if (isBlob(value)) {
            return "blob(base64:" + Base64.getEncoder().encodeToString(bytes(value)) + ")";
        }
        return String.valueOf(value);
    }
}
