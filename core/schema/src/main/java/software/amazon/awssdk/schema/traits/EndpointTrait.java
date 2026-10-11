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
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * The {@code smithy.api#endpoint} trait: the host prefix that an operation adds to the endpoint host.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.EndpointTrait}. smithy-model parses the host
 * prefix into a {@code SmithyPattern}; this class keeps the unparsed pattern string, for example
 * {@code "{AccountId}."}, and leaves parsing to the runtime component that applies it.
 */
@SdkPublicApi
public final class EndpointTrait implements Trait {

    /**
     * The trait's shape ID, {@code smithy.api#endpoint}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "endpoint");

    private static final String HOST_PREFIX = "hostPrefix";

    private final String hostPrefix;

    /**
     * Creates the trait.
     *
     * @param hostPrefix the host prefix pattern, for example {@code "{AccountId}."}
     * @throws NullPointerException if {@code hostPrefix} is null
     */
    public EndpointTrait(String hostPrefix) {
        this.hostPrefix = Validate.paramNotNull(hostPrefix, "hostPrefix");
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns the host prefix pattern.
     *
     * @return the host prefix pattern
     */
    public String getHostPrefix() {
        return hostPrefix;
    }

    @Override
    public Document toDocument() {
        return Document.of(Collections.singletonMap(HOST_PREFIX, Document.of(hostPrefix)));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof EndpointTrait && hostPrefix.equals(((EndpointTrait) o).hostPrefix);
    }

    @Override
    public int hashCode() {
        return hostPrefix.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("EndpointTrait").add("hostPrefix", hostPrefix).build();
    }

    static EndpointTrait fromDocument(Document value) {
        Map<String, Document> object = TraitValues.expectObject(ID, value);
        return new EndpointTrait(TraitValues.requiredString(ID, object, HOST_PREFIX));
    }
}
