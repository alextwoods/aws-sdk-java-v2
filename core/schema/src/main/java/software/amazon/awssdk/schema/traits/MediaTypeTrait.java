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
 * The {@code smithy.api#mediaType} trait. The media type of a blob or string value.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.MediaTypeTrait}.
 */
@SdkPublicApi
public final class MediaTypeTrait extends StringTrait {

    /**
     * The trait's shape ID, {@code smithy.api#mediaType}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "mediaType");

    /**
     * Creates the trait.
     *
     * @param value the media type, for example {@code application/json}
     * @throws NullPointerException if {@code value} is null
     */
    public MediaTypeTrait(String value) {
        super(ID, value);
    }
}
