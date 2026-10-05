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

package software.amazon.awssdk.pipeline;

import java.util.Objects;

/**
 * A typed key of derived, client-constant metadata that an {@link AwsFeaturePlugin} contributes to a
 * {@link PipelinePlan} at plan-build time (for example a precomputed header or a feature's operation subplan). Values
 * must be immutable and must not hold customer data. Keys are compared by identity.
 *
 * @param <T> the value type
 */
public final class PlanAttribute<T> {
    private final String name;
    private final Class<T> valueType;

    private PlanAttribute(String name, Class<T> valueType) {
        this.name = Objects.requireNonNull(name, "name");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
    }

    /** A new key named {@code name} whose values are {@code valueType} instances. */
    public static <T> PlanAttribute<T> create(String name, Class<T> valueType) {
        return new PlanAttribute<>(name, valueType);
    }

    /** Diagnostic name of the key. */
    public String name() {
        return name;
    }

    /** {@code value} as this key's value type. */
    T cast(Object value) {
        return valueType.cast(value);
    }

    @Override
    public String toString() {
        return "PlanAttribute(" + name + ": " + valueType.getSimpleName() + ")";
    }
}
