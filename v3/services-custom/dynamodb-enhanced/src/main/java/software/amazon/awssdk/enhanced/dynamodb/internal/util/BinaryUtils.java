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
package software.amazon.awssdk.enhanced.dynamodb.internal.util;

import java.util.Base64;

/** Base64 conversion with the V2 {@code software.amazon.awssdk.utils.BinaryUtils} null handling. */
public final class BinaryUtils {
    private BinaryUtils() {
    }

    public static String toBase64(byte[] data) {
        return data == null ? null : Base64.getEncoder().encodeToString(data);
    }

    public static byte[] fromBase64(String b64Data) {
        return b64Data == null ? null : Base64.getDecoder().decode(b64Data);
    }
}
