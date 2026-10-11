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

/**
 * The {@code restJson1} protocol definition trait.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.aws.traits.protocols.RestJson1Trait}.
 */
@SdkPublicApi
public final class RestJson1Trait extends AwsProtocolTrait {

    /**
     * The trait's shape ID, {@code aws.protocols#restJson1}.
     */
    public static final ShapeId ID = ShapeId.fromParts("aws.protocols", "restJson1");

    /**
     * Creates the trait with empty {@code http} and {@code eventStreamHttp} lists.
     */
    public RestJson1Trait() {
        this(Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    /**
     * Creates the trait.
     *
     * @param http the supported HTTP protocol versions in priority order
     * @param eventStreamHttp the HTTP protocol versions, in priority order, required when using event streams
     * @throws NullPointerException if either list or one of its elements is null
     */
    public RestJson1Trait(List<String> http, List<String> eventStreamHttp) {
        super(ID, http, eventStreamHttp);
    }

    static RestJson1Trait fromDocument(Document value) {
        Map<String, Document> object = TraitValues.expectObject(ID, value);
        return new RestJson1Trait(http(ID, object), eventStreamHttp(ID, object));
    }
}
