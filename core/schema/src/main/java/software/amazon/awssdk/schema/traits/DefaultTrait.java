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
 * The {@code smithy.api#default} trait: the default value of a shape or member.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.DefaultTrait}. The value is any document; a null
 * value represents {@code @default(null)}, which removes an inherited default.
 */
@SdkPublicApi
public final class DefaultTrait implements Trait {

    /**
     * The trait's shape ID, {@code smithy.api#default}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "default");

    private final Document value;

    /**
     * Creates the trait.
     *
     * @param value the default value; null represents {@code @default(null)}
     */
    public DefaultTrait(Document value) {
        this.value = value;
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns the default value.
     *
     * @return the default value, or null for {@code @default(null)}
     */
    public Document getValue() {
        return value;
    }

    @Override
    public Document toDocument() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof DefaultTrait && Document.equals(value, ((DefaultTrait) o).value);
    }

    @Override
    public int hashCode() {
        return Document.hash(value);
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultTrait").add("value", value).build();
    }

    static DefaultTrait fromDocument(Document value) {
        return new DefaultTrait(value);
    }
}
