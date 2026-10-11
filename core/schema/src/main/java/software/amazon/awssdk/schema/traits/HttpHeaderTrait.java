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
 * The {@code smithy.api#httpHeader} trait. Binds a member to an HTTP header.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.HttpHeaderTrait}.
 */
@SdkPublicApi
public final class HttpHeaderTrait extends StringTrait {

    /**
     * The trait's shape ID, {@code smithy.api#httpHeader}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "httpHeader");

    /**
     * Creates the trait.
     *
     * @param value the header name
     * @throws NullPointerException if {@code value} is null
     */
    public HttpHeaderTrait(String value) {
        super(ID, value);
    }
}
