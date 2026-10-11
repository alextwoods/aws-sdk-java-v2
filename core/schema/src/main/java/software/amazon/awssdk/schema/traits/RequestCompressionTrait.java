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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;

/**
 * The {@code smithy.api#requestCompression} trait: the compression encodings an operation's request body supports.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.RequestCompressionTrait}.
 */
@SdkPublicApi
public final class RequestCompressionTrait implements Trait {

    /**
     * The trait's shape ID, {@code smithy.api#requestCompression}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "requestCompression");

    private static final String ENCODINGS = "encodings";

    private final List<String> encodings;

    /**
     * Creates the trait.
     *
     * @param encodings the supported encodings in priority order, for example {@code ["gzip"]}
     * @throws NullPointerException if {@code encodings} or one of its elements is null
     */
    public RequestCompressionTrait(List<String> encodings) {
        this.encodings = TraitValues.copyOf(encodings, "encodings");
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns the supported encodings in priority order.
     *
     * @return an unmodifiable list of encodings
     */
    public List<String> getEncodings() {
        return encodings;
    }

    @Override
    public Document toDocument() {
        return Document.of(Collections.singletonMap(ENCODINGS, TraitValues.stringList(encodings)));
    }

    @Override
    public boolean equals(Object o) {
        return this == o
               || o instanceof RequestCompressionTrait && encodings.equals(((RequestCompressionTrait) o).encodings);
    }

    @Override
    public int hashCode() {
        return encodings.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("RequestCompressionTrait").add("encodings", encodings).build();
    }

    static RequestCompressionTrait fromDocument(Document value) {
        Map<String, Document> object = TraitValues.expectObject(ID, value);
        return new RequestCompressionTrait(TraitValues.requiredStringList(ID, object, ENCODINGS));
    }
}
