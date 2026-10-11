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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * The {@code smithy.api#http} trait: the HTTP method, URI pattern and success status code of an operation.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.HttpTrait}. smithy-model parses the URI into a
 * {@code UriPattern}; this class keeps the unparsed pattern string, for example {@code "/{Bucket}/{Key+}?x-id=Get"},
 * and leaves parsing to the HTTP binding serde.
 */
@SdkPublicApi
public final class HttpTrait implements Trait {

    /**
     * The trait's shape ID, {@code smithy.api#http}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "http");

    private static final int DEFAULT_CODE = 200;

    private final String method;
    private final String uri;
    private final int code;

    /**
     * Creates the trait with the default success status code, 200.
     *
     * @param method the HTTP method
     * @param uri the URI pattern
     * @throws NullPointerException if either argument is null
     */
    public HttpTrait(String method, String uri) {
        this(method, uri, DEFAULT_CODE);
    }

    /**
     * Creates the trait.
     *
     * @param method the HTTP method
     * @param uri the URI pattern
     * @param code the success status code
     * @throws NullPointerException if {@code method} or {@code uri} is null
     */
    public HttpTrait(String method, String uri, int code) {
        this.method = Validate.paramNotNull(method, "method");
        this.uri = Validate.paramNotNull(uri, "uri");
        this.code = code;
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns the HTTP method, for example {@code GET}.
     *
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the unparsed URI pattern.
     *
     * @return the URI pattern
     */
    public String getUri() {
        return uri;
    }

    /**
     * Returns the success status code.
     *
     * @return the status code
     */
    public int getCode() {
        return code;
    }

    @Override
    public Document toDocument() {
        Map<String, Document> members = new LinkedHashMap<>();
        members.put("method", Document.of(method));
        members.put("uri", Document.of(uri));
        members.put("code", Document.of(code));
        return Document.of(members);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HttpTrait)) {
            return false;
        }
        HttpTrait other = (HttpTrait) o;
        return code == other.code && method.equals(other.method) && uri.equals(other.uri);
    }

    @Override
    public int hashCode() {
        int result = method.hashCode();
        result = 31 * result + uri.hashCode();
        return 31 * result + code;
    }

    @Override
    public String toString() {
        return ToString.builder("HttpTrait").add("method", method).add("uri", uri).add("code", code).build();
    }

    static HttpTrait fromDocument(Document value) {
        Map<String, Document> object = TraitValues.expectObject(ID, value);
        return new HttpTrait(TraitValues.requiredString(ID, object, "method"),
                             TraitValues.requiredString(ID, object, "uri"),
                             TraitValues.optionalInteger(ID, object, "code", DEFAULT_CODE));
    }
}
