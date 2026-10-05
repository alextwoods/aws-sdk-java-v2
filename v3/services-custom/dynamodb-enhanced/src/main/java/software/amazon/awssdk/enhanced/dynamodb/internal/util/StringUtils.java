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

/** The V2 {@code software.amazon.awssdk.utils.StringUtils} operations the enhanced client uses. */
public final class StringUtils {
    private StringUtils() {
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /** {@code str} with its first code point in title case. */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        int first = str.codePointAt(0);
        int title = Character.toTitleCase(first);
        if (first == title) {
            return str;
        }
        return new StringBuilder(str.length()).appendCodePoint(title)
                                              .append(str, Character.charCount(first), str.length())
                                              .toString();
    }
}
