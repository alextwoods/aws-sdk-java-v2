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

/**
 * The {@code smithy.api#unitType} annotation trait.
 * Marks the prelude {@code smithy.api#Unit} shape.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.UnitTypeTrait}.
 * The trait has a single instance, {@link #INSTANCE}.
 */
@SdkPublicApi
public final class UnitTypeTrait extends AnnotationTrait {

    /**
     * The trait's shape ID, {@code smithy.api#unitType}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "unitType");

    /**
     * The single instance of the trait.
     */
    public static final UnitTypeTrait INSTANCE = new UnitTypeTrait();

    private UnitTypeTrait() {
        super(ID);
    }
}
