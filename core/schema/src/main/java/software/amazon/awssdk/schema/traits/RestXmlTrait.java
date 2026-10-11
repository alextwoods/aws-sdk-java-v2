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
 * The {@code restXml} protocol definition trait.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.aws.traits.protocols.RestXmlTrait}.
 */
@SdkPublicApi
public final class RestXmlTrait extends AwsProtocolTrait {

    /**
     * The trait's shape ID, {@code aws.protocols#restXml}.
     */
    public static final ShapeId ID = ShapeId.fromParts("aws.protocols", "restXml");

    private static final String NO_ERROR_WRAPPING = "noErrorWrapping";

    private final boolean noErrorWrapping;

    /**
     * Creates the trait with empty {@code http} and {@code eventStreamHttp} lists and error wrapping enabled.
     */
    public RestXmlTrait() {
        this(Collections.<String>emptyList(), Collections.<String>emptyList(), false);
    }

    /**
     * Creates the trait.
     *
     * @param http the supported HTTP protocol versions in priority order
     * @param eventStreamHttp the HTTP protocol versions, in priority order, required when using event streams
     * @param noErrorWrapping true if error responses are not wrapped in an {@code ErrorResponse} element
     * @throws NullPointerException if either list or one of its elements is null
     */
    public RestXmlTrait(List<String> http, List<String> eventStreamHttp, boolean noErrorWrapping) {
        super(ID, http, eventStreamHttp);
        this.noErrorWrapping = noErrorWrapping;
    }

    /**
     * Returns whether error responses are sent without the {@code ErrorResponse} wrapping element.
     *
     * @return true if errors are not wrapped
     */
    public boolean isNoErrorWrapping() {
        return noErrorWrapping;
    }

    @Override
    void addMembers(Map<String, Document> members) {
        if (noErrorWrapping) {
            members.put(NO_ERROR_WRAPPING, Document.of(true));
        }
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && noErrorWrapping == ((RestXmlTrait) o).noErrorWrapping;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Boolean.hashCode(noErrorWrapping);
    }

    @Override
    public String toString() {
        return ToString.builder("RestXmlTrait")
                       .add("http", getHttp())
                       .add("eventStreamHttp", getEventStreamHttp())
                       .add("noErrorWrapping", noErrorWrapping)
                       .build();
    }

    static RestXmlTrait fromDocument(Document value) {
        Map<String, Document> object = TraitValues.expectObject(ID, value);
        return new RestXmlTrait(http(ID, object), eventStreamHttp(ID, object),
                                TraitValues.optionalBoolean(ID, object, NO_ERROR_WRAPPING));
    }
}
