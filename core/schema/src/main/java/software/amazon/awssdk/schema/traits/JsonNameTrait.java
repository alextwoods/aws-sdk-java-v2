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
 * The {@code smithy.api#jsonName} trait. The JSON object key of a structure or union member.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.JsonNameTrait}.
 */
@SdkPublicApi
public final class JsonNameTrait extends StringTrait {

    /**
     * The trait's shape ID, {@code smithy.api#jsonName}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "jsonName");

    /**
     * Creates the trait.
     *
     * @param value the JSON key
     * @throws NullPointerException if {@code value} is null
     */
    public JsonNameTrait(String value) {
        super(ID, value);
    }
}
