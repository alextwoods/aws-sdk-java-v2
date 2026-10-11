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
 * The {@code smithy.api#error} trait: marks a structure as an error, caused either by the client or by the server.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.ErrorTrait}.
 */
@SdkPublicApi
public final class ErrorTrait extends StringTrait {

    /**
     * The trait's shape ID, {@code smithy.api#error}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "error");

    private static final String CLIENT = "client";
    private static final String SERVER = "server";

    /**
     * Creates the trait.
     *
     * @param value {@code client} or {@code server}
     * @throws NullPointerException if {@code value} is null
     * @throws IllegalArgumentException if {@code value} is neither {@code client} nor {@code server}
     */
    public ErrorTrait(String value) {
        super(ID, value);
        if (!isClientError() && !isServerError()) {
            throw new IllegalArgumentException("error trait must be set to client or server, found `" + value + "`");
        }
    }

    /**
     * Returns whether the error is caused by the client.
     *
     * @return true for {@code client}
     */
    public boolean isClientError() {
        return CLIENT.equals(getValue());
    }

    /**
     * Returns whether the error is caused by the server.
     *
     * @return true for {@code server}
     */
    public boolean isServerError() {
        return SERVER.equals(getValue());
    }

    /**
     * Returns the default HTTP status code of the error: 400 for client errors and 500 for server errors.
     *
     * @return the default status code
     */
    public int getDefaultHttpStatusCode() {
        return isClientError() ? 400 : 500;
    }

    static ErrorTrait fromDocument(Document value) {
        String fault = TraitValues.expectString(ID, value);
        if (!CLIENT.equals(fault) && !SERVER.equals(fault)) {
            throw TraitValues.invalid(ID, "value must be client or server, but found `" + fault + "`");
        }
        return new ErrorTrait(fault);
    }
}
