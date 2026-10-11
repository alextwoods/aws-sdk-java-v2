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
 * A trait that has no typed runtime class. It holds the trait's shape ID and its raw value as a document.
 *
 * <p>This is the SDK replacement for {@code software.amazon.smithy.model.traits.DynamicTrait}.
 * {@link TraitFactories#create(ShapeId, Document)} returns a generic trait for any trait ID that has no registered
 * {@link TraitFactory}.
 */
@SdkPublicApi
public final class GenericTrait implements Trait {
    private final ShapeId traitId;
    private final Document value;

    /**
     * Creates the trait.
     *
     * @param traitId the trait's shape ID
     * @param value the trait's value; null represents a Smithy {@code null} value
     * @throws NullPointerException if {@code traitId} is null
     */
    public GenericTrait(ShapeId traitId, Document value) {
        this.traitId = Validate.paramNotNull(traitId, "traitId");
        this.value = value;
    }

    @Override
    public ShapeId traitId() {
        return traitId;
    }

    /**
     * Returns the trait value passed to the constructor.
     *
     * @return the trait value, which may be null
     */
    public Document value() {
        return value;
    }

    @Override
    public Document toDocument() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GenericTrait)) {
            return false;
        }
        GenericTrait other = (GenericTrait) o;
        return traitId.equals(other.traitId) && Document.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return 31 * traitId.hashCode() + Document.hash(value);
    }

    @Override
    public String toString() {
        return ToString.builder("GenericTrait").add("traitId", traitId).add("value", value).build();
    }
}
