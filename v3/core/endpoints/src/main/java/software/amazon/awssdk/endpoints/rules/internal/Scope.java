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

package software.amazon.awssdk.endpoints.rules.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * The names visible to an expression: parameter values (after defaults) and the results assigned by the conditions of
 * the enclosing rules. A condition's assignment is visible to later conditions and to the body of its rule, and is
 * removed when evaluation leaves the rule. One scope belongs to one resolution and is not thread-safe.
 */
public final class Scope {
    private final Map<String, Object> values;

    Scope(int expectedSize) {
        this.values = new HashMap<>(Math.max(16, expectedSize * 2));
    }

    /** The value of {@code name}, or {@code null} (empty) if it is unset. */
    public Object get(String name) {
        return values.get(name);
    }

    void put(String name, Object value) {
        values.put(name, value);
    }

    void remove(String name) {
        values.remove(name);
    }
}
