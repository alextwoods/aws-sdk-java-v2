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

/**
 * The {@code smithy.api#httpError} trait: the HTTP status code of an error shape.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.HttpErrorTrait}.
 */
@SdkPublicApi
public final class HttpErrorTrait implements Trait {

    /**
     * The trait's shape ID, {@code smithy.api#httpError}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "httpError");

    private final int code;

    /**
     * Creates the trait.
     *
     * @param code the HTTP status code
     */
    public HttpErrorTrait(int code) {
        this.code = code;
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code
     */
    public int getCode() {
        return code;
    }

    @Override
    public Document toDocument() {
        return Document.of(code);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HttpErrorTrait && code == ((HttpErrorTrait) o).code;
    }

    @Override
    public int hashCode() {
        return code;
    }

    @Override
    public String toString() {
        return ToString.builder("HttpErrorTrait").add("code", code).build();
    }

    static HttpErrorTrait fromDocument(Document value) {
        return new HttpErrorTrait(TraitValues.expectInteger(ID, value));
    }
}
