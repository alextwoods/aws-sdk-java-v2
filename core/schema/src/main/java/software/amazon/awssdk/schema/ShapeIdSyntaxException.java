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

package software.amazon.awssdk.schema;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Thrown when a string is not a valid Smithy shape ID, or when a namespace, shape name or member name passed to a
 * {@link ShapeId} factory is not a valid Smithy identifier.
 *
 * <p>The message always contains the offending input.
 */
@SdkPublicApi
public final class ShapeIdSyntaxException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message the message, which should contain the offending input
     */
    public ShapeIdSyntaxException(String message) {
        super(message);
    }
}
