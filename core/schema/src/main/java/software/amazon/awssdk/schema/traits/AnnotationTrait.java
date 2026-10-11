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
import software.amazon.awssdk.utils.Validate;

/**
 * Base class for annotation traits: traits whose value is an empty object, such as {@code smithy.api#required}.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.AnnotationTrait}. The SDK's annotation traits are
 * singletons with no value, so {@link #toDocument()} always returns an empty map. Two annotation traits are equal when
 * they have the same class and trait ID.
 */
@SdkPublicApi
public abstract class AnnotationTrait implements Trait {
    private final ShapeId traitId;

    /**
     * Creates the trait.
     *
     * @param traitId the trait's shape ID
     * @throws NullPointerException if {@code traitId} is null
     */
    protected AnnotationTrait(ShapeId traitId) {
        this.traitId = Validate.paramNotNull(traitId, "traitId");
    }

    @Override
    public final ShapeId traitId() {
        return traitId;
    }

    /**
     * Returns an empty map document, the value of an annotation trait in the Smithy JSON AST.
     *
     * @return {@link Document#EMPTY_MAP}
     */
    @Override
    public final Document toDocument() {
        return Document.EMPTY_MAP;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o != null && o.getClass() == getClass() && traitId.equals(((AnnotationTrait) o).traitId);
    }

    @Override
    public final int hashCode() {
        return traitId.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
