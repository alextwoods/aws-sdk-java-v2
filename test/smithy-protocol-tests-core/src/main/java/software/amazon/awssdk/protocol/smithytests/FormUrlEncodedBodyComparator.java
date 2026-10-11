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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares {@code application/x-www-form-urlencoded} bodies as multisets of percent-decoded {@code name=value} pairs, so
 * pair order does not matter but every pair must appear the same number of times on both sides. Line breaks in the
 * expected string (Smithy test cases often wrap long query bodies) are removed before parsing.
 */
final class FormUrlEncodedBodyComparator implements BodyComparator {

    @Override
    public void assertMatches(String expected, byte[] actual) {
        String actualText = new String(actual, StandardCharsets.UTF_8);
        Map<QueryParameter, Integer> expectedPairs = multiset(expected.replace("\r", "").replace("\n", ""));
        Map<QueryParameter, Integer> actualPairs = multiset(actualText);
        if (expectedPairs.equals(actualPairs)) {
            return;
        }
        List<String> missing = difference(expectedPairs, actualPairs);
        List<String> unexpected = difference(actualPairs, expectedPairs);
        throw new AssertionError("Form-urlencoded bodies differ."
                                 + (missing.isEmpty() ? "" : "\n  missing pairs: " + missing)
                                 + (unexpected.isEmpty() ? "" : "\n  unexpected pairs: " + unexpected)
                                 + "\nExpected:\n" + expected + "\nActual:\n" + actualText);
    }

    private static Map<QueryParameter, Integer> multiset(String body) {
        Map<QueryParameter, Integer> counts = new LinkedHashMap<>();
        for (QueryParameter pair : QueryParameter.parseAll(body)) {
            counts.merge(pair, 1, Integer::sum);
        }
        return counts;
    }

    private static List<String> difference(Map<QueryParameter, Integer> left, Map<QueryParameter, Integer> right) {
        List<String> result = new ArrayList<>();
        left.forEach((pair, count) -> {
            int extra = count - right.getOrDefault(pair, 0);
            for (int i = 0; i < extra; i++) {
                result.add(pair.toString());
            }
        });
        return result;
    }
}
