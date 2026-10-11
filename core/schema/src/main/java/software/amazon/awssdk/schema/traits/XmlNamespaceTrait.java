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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * The {@code smithy.api#xmlNamespace} trait: the XML namespace of a shape or member, with an optional prefix.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.XmlNamespaceTrait}.
 */
@SdkPublicApi
public final class XmlNamespaceTrait implements Trait {

    /**
     * The trait's shape ID, {@code smithy.api#xmlNamespace}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "xmlNamespace");

    private static final String URI = "uri";
    private static final String PREFIX = "prefix";

    private final String uri;
    private final String prefix;

    /**
     * Creates the trait with no prefix.
     *
     * @param uri the namespace URI
     * @throws NullPointerException if {@code uri} is null
     */
    public XmlNamespaceTrait(String uri) {
        this(uri, null);
    }

    /**
     * Creates the trait.
     *
     * @param uri the namespace URI
     * @param prefix the namespace prefix, or null for none
     * @throws NullPointerException if {@code uri} is null
     */
    public XmlNamespaceTrait(String uri, String prefix) {
        this.uri = Validate.paramNotNull(uri, "uri");
        this.prefix = prefix;
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns the namespace URI.
     *
     * @return the URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Returns the namespace prefix.
     *
     * @return the prefix, if set
     */
    public Optional<String> getPrefix() {
        return Optional.ofNullable(prefix);
    }

    @Override
    public Document toDocument() {
        Map<String, Document> members = new LinkedHashMap<>();
        members.put(URI, Document.of(uri));
        if (prefix != null) {
            members.put(PREFIX, Document.of(prefix));
        }
        return Document.of(members);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof XmlNamespaceTrait)) {
            return false;
        }
        XmlNamespaceTrait other = (XmlNamespaceTrait) o;
        return uri.equals(other.uri) && Objects.equals(prefix, other.prefix);
    }

    @Override
    public int hashCode() {
        return 31 * uri.hashCode() + Objects.hashCode(prefix);
    }

    @Override
    public String toString() {
        return ToString.builder("XmlNamespaceTrait").add("uri", uri).add("prefix", prefix).build();
    }

    static XmlNamespaceTrait fromDocument(Document value) {
        Map<String, Document> object = TraitValues.expectObject(ID, value);
        return new XmlNamespaceTrait(TraitValues.requiredString(ID, object, URI),
                                     TraitValues.optionalString(ID, object, PREFIX));
    }
}
