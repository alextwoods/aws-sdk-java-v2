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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Value constructors used by generated protocol tests for test case {@code params} literals.
 */
public final class ProtocolTestValues {

    private ProtocolTestValues() {
    }

    /**
     * An unmodifiable map of alternating keys and values, in argument order. Values may be null, as in sparse maps.
     * Generated code passes an explicit type witness, for example {@code ProtocolTestValues.<Integer>mapOf("a", 1)}.
     *
     * @throws IllegalArgumentException if the argument count is odd, a key is not a string, or a key repeats
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<String, V> mapOf(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("Expected alternating keys and values, but got " + keysAndValues.length
                                               + " arguments");
        }
        Map<String, V> result = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            Object key = keysAndValues[i];
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("Map key at argument " + i + " is not a string: " + key);
            }
            if (result.containsKey(key)) {
                throw new IllegalArgumentException("Map key \"" + key + "\" repeats");
            }
            result.put((String) key, (V) keysAndValues[i + 1]);
        }
        return Collections.unmodifiableMap(result);
    }
}
