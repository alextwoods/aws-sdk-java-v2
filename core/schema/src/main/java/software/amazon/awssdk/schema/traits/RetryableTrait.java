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

package software.amazon.awssdk.schema.traits;

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;

/**
 * The {@code smithy.api#retryable} trait: marks an error that can be retried, and whether it is a throttling error.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.RetryableTrait}.
 */
@SdkPublicApi
public final class RetryableTrait implements Trait {

    /**
     * The trait's shape ID, {@code smithy.api#retryable}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "retryable");

    private static final String THROTTLING = "throttling";

    private final boolean throttling;

    /**
     * Creates the trait.
     *
     * @param throttling whether the error is a throttling error
     */
    public RetryableTrait(boolean throttling) {
        this.throttling = throttling;
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns whether the error is a throttling error.
     *
     * @return true for a throttling error
     */
    public boolean getThrottling() {
        return throttling;
    }

    /**
     * Returns {@code {"throttling": true}} for a throttling error, and an empty map otherwise.
     *
     * @return the trait value
     */
    @Override
    public Document toDocument() {
        return throttling ? Document.of(Collections.singletonMap(THROTTLING, Document.of(true))) : Document.EMPTY_MAP;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof RetryableTrait && throttling == ((RetryableTrait) o).throttling;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(throttling);
    }

    @Override
    public String toString() {
        return ToString.builder("RetryableTrait").add("throttling", throttling).build();
    }

    static RetryableTrait fromDocument(Document value) {
        Map<String, Document> object = TraitValues.expectObject(ID, value);
        return new RetryableTrait(TraitValues.optionalBoolean(ID, object, THROTTLING));
    }
}
