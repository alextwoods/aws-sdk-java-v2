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

package software.amazon.awssdk.benchmark.schema;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.testutils.mock.ResponseGenerator;
import software.amazon.awssdk.utils.IoUtils;

/**
 * One protocol and payload of the schema-serde benchmarks: the client protocol, the payload as the generated input and
 * output, and the payload as the HTTP response the Response_Generator produces for the protocol.
 *
 * <p>Marshalling runs {@link ClientProtocol#serializeRequest} from the input POJO to the {@link SdkHttpFullRequest};
 * unmarshalling runs {@link ClientProtocol#deserializeResponse} from the response bytes to the output POJO (Requirement
 * 34.1). Neither signs nor sends.
 */
public final class SchemaSerdeFixture {
    /**
     * The endpoint passed to {@code serializeRequest}, as the pipeline passes the client endpoint.
     */
    public static final URI ENDPOINT = URI.create("https://schemabench.us-west-2.amazonaws.com");

    private final ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol;
    private final ApiOperation<?, ?> putPayload;
    private final ApiOperation<?, ?> getPayload;
    private final SerializableStruct input;
    private final SerializableStruct output;
    private final SdkHttpFullResponse response;
    private final byte[] responseBody;
    private final ExecutionAttributes attributes = new ExecutionAttributes();

    public SchemaSerdeFixture(SchemaSerdeProtocol protocol, SchemaSerdePayload payload) {
        this.protocol = protocol.createProtocol();
        this.putPayload = protocol.putPayload();
        this.getPayload = protocol.getPayload();
        this.input = payload.input(protocol);
        this.output = payload.output(protocol);
        SdkHttpFullResponse generated = ResponseGenerator.forProtocol(this.protocol.id())
                                                         .generateOutput(getPayload, output);
        this.responseBody = generated.content().map(SchemaSerdeFixture::readAll).orElse(new byte[0]);
        this.response = generated.toBuilder().content(null).build();
    }

    /**
     * Serializes the payload as the {@code PutPayload} request.
     */
    public SdkHttpFullRequest marshall() {
        return serialize(protocol, putPayload, input, attributes);
    }

    /**
     * Deserializes the {@code GetPayload} response, from a new stream over the response bytes.
     */
    public SerializableStruct unmarshall() {
        SdkHttpFullResponse withBody = response.toBuilder()
                                               .content(AbortableInputStream.create(
                                                   new ByteArrayInputStream(responseBody)))
                                               .build();
        return deserialize(protocol, getPayload, attributes, withBody);
    }

    /**
     * The payload as the generated output, for comparing with {@link #unmarshall()}.
     */
    public SerializableStruct output() {
        return output;
    }

    /**
     * The size of the serialized request body, in bytes.
     */
    public int requestBodySize() {
        return marshall().contentStreamProvider().map(p -> readAll(p.newStream()).length).orElse(0);
    }

    /**
     * The size of the response body, in bytes.
     */
    public int responseBodySize() {
        return responseBody.length;
    }

    @SuppressWarnings("unchecked")
    private static <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest serialize(
        ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol, ApiOperation<?, ?> operation,
        SerializableStruct input, ExecutionAttributes attributes) {
        ApiOperation<I, O> typed = (ApiOperation<I, O>) operation;
        return protocol.serializeRequest(typed, (I) input, attributes, ENDPOINT);
    }

    @SuppressWarnings("unchecked")
    private static <I extends SerializableStruct, O extends SerializableStruct> O deserialize(
        ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol, ApiOperation<?, ?> operation,
        ExecutionAttributes attributes, SdkHttpFullResponse response) {
        ApiOperation<I, O> typed = (ApiOperation<I, O>) operation;
        return protocol.deserializeResponse(typed, attributes, typed.errorRegistry(), null, response);
    }

    private static byte[] readAll(InputStream stream) {
        try (InputStream in = stream) {
            return IoUtils.toByteArray(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
