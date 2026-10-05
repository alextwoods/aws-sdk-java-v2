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

package software.amazon.awssdk.core.pagination;

import java.util.Collection;
import java.util.Map;

/** The modeled continuation condition of {@code smithy.api#paginated} output tokens. */
public final class PaginationTokens {
    private PaginationTokens() {
    }

    /**
     * Whether {@code token} continues pagination: it is present and, for a string, collection or map, non-empty.
     */
    public static boolean hasToken(Object token) {
        if (token == null) {
            return false;
        }
        if (token instanceof CharSequence string) {
            return !string.isEmpty();
        }
        if (token instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (token instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        return true;
    }
}
