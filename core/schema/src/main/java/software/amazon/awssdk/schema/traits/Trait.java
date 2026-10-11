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

/**
 * The runtime form of a Smithy trait applied to a shape or member.
 *
 * <p>This is the SDK runtime replacement for {@code software.amazon.smithy.model.traits.Trait}, which generated
 * clients cannot use because they must not depend on {@code smithy-model}. A trait is either a typed class in this
 * package (or a third-party class created by a {@link TraitFactory}), or a {@link GenericTrait} that holds the raw
 * value.
 *
 * <p>Implementations must be immutable and must implement value-based {@code equals} and {@code hashCode}.
 */
@SdkPublicApi
public interface Trait {

    /**
     * Returns the shape ID of the trait, for example {@code smithy.api#jsonName}.
     *
     * @return the trait's shape ID
     */
    ShapeId traitId();

    /**
     * Returns the trait's value as a document, in the form of the trait's value in the Smithy JSON AST. Creating a
     * trait from this document with the trait's {@link TraitFactory} returns an equal trait.
     *
     * @return the trait value, or null when the value is a Smithy {@code null}
     */
    Document toDocument();
}
