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
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Creates a typed {@link Trait} from its document value.
 *
 * <p>The SDK provides a factory for every typed trait in this package. A third party can add a factory for a trait
 * that the SDK does not cover by listing the implementation class in
 * {@code META-INF/services/software.amazon.awssdk.schema.traits.TraitFactory}. Implementations must have a public
 * no-argument constructor and be thread-safe. At most one factory may exist for a trait ID; see
 * {@link TraitFactories}.
 *
 * @param <T> the trait class
 */
@SdkPublicApi
public interface TraitFactory<T extends Trait> {

    /**
     * Returns the shape ID of the trait this factory creates.
     *
     * @return the trait's shape ID
     */
    ShapeId traitId();

    /**
     * Returns the class of the trait this factory creates.
     *
     * @return the trait class
     */
    Class<T> traitClass();

    /**
     * Creates the trait from its value, in the form of the trait's value in the Smithy JSON AST.
     *
     * @param value the trait value; null represents a Smithy {@code null} value
     * @return the trait
     * @throws SerializationException if the value does not match the trait's modeled shape. The message must name the
     *                                trait's shape ID.
     */
    T create(Document value);
}
