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
import java.util.Arrays;
import java.util.Base64;

/**
 * Compares bodies byte for byte against the UTF-8 encoding of the expected test case string.
 */
final class BytesBodyComparator implements BodyComparator {

    @Override
    public void assertMatches(String expected, byte[] actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        if (!Arrays.equals(expectedBytes, actual)) {
            throw new AssertionError("Bodies differ byte for byte (expected " + expectedBytes.length + " bytes, actual "
                                     + actual.length + " bytes)."
                                     + "\nExpected (UTF-8):\n" + expected
                                     + "\nActual (UTF-8):\n" + new String(actual, StandardCharsets.UTF_8)
                                     + "\nActual (base64): " + Base64.getEncoder().encodeToString(actual));
        }
    }
}
