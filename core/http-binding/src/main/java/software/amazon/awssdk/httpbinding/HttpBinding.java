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

package software.amazon.awssdk.httpbinding;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions;
import software.amazon.awssdk.schema.Schema;

/**
 * Entry point for HTTP binding serde: creates the serializers and deserializers that map a structure's HTTP binding
 * traits ({@code httpLabel}, {@code httpQuery}, {@code httpQueryParams}, {@code httpHeader},
 * {@code httpPrefixHeaders}, {@code httpPayload}, {@code httpResponseCode}) to an HTTP message, and its unbound members
 * to the body through a protocol codec.
 *
 * <p>Stateless and thread-safe; the serializers and deserializers it creates serialize one message each.
 */
@SdkPublicApi
public final class HttpBinding {
    private static final HttpBinding INSTANCE = new HttpBinding();

    private HttpBinding() {
    }

    /**
     * Returns the HTTP binding entry point.
     *
     * @return the instance
     */
    public static HttpBinding create() {
        return INSTANCE;
    }

    /**
     * Creates an HTTP binding request serializer.
     *
     * @return a new serializer
     */
    public RequestSerializer requestSerializer() {
        return new RequestSerializer();
    }

    /**
     * Creates an HTTP binding response serializer.
     *
     * @return a new serializer
     */
    public ResponseSerializer responseSerializer() {
        return new ResponseSerializer();
    }

    /**
     * Creates an HTTP binding request deserializer.
     *
     * @return a new deserializer
     */
    public RequestDeserializer requestDeserializer() {
        return new RequestDeserializer();
    }

    /**
     * Creates an HTTP binding response deserializer.
     *
     * @return a new deserializer
     */
    public ResponseDeserializer responseDeserializer() {
        return new ResponseDeserializer();
    }

    /**
     * Returns whether the input structure has an {@code @httpPayload} member that targets a structure, so the request
     * body is that one codec-serialized structure.
     *
     * @param inputSchema the operation input schema
     * @return true if the schema has a structure-typed {@code @httpPayload} member
     */
    public boolean hasStructPayload(Schema inputSchema) {
        return HttpBindingSchemaExtensions.hasStructPayload(inputSchema);
    }

    /**
     * Returns whether the structure's {@code @httpPayload} member is {@code @streaming} in the given direction. The
     * serializers leave a streaming payload to the protocol: no request body is written for it, and the response
     * deserializer does not read the body.
     *
     * @param schema the input, output or error schema
     * @param response true for the response direction, false for the request direction
     * @return true if the payload member is streaming
     */
    public boolean hasStreamingPayload(Schema schema, boolean response) {
        return HttpBindingSchemaExtensions.hasStreamingPayload(schema, response);
    }
}
