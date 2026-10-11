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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Base class for traits whose value is a single string, such as {@code smithy.api#jsonName}.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.StringTrait}. Two string traits are equal when
 * they have the same class, trait ID and value.
 */
@SdkPublicApi
public abstract class StringTrait implements Trait {
    private final ShapeId traitId;
    private final String value;

    /**
     * Creates the trait.
     *
     * @param traitId the trait's shape ID
     * @param value the trait's value
     * @throws NullPointerException if either argument is null
     */
    protected StringTrait(ShapeId traitId, String value) {
        this.traitId = Validate.paramNotNull(traitId, "traitId");
        this.value = Validate.paramNotNull(value, "value");
    }

    @Override
    public final ShapeId traitId() {
        return traitId;
    }

    /**
     * Returns the trait's string value.
     *
     * @return the value
     */
    public final String getValue() {
        return value;
    }

    /**
     * Returns the value as a string document.
     *
     * @return the value document
     */
    @Override
    public final Document toDocument() {
        return Document.of(value);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        StringTrait other = (StringTrait) o;
        return traitId.equals(other.traitId) && value.equals(other.value);
    }

    @Override
    public final int hashCode() {
        return 31 * traitId.hashCode() + value.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder(getClass().getSimpleName()).add("value", value).build();
    }
}
