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

package software.amazon.awssdk.core.client.protocol;

import java.net.URI;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.traits.Trait;

/**
 * A protocol factory registered in the test {@code META-INF/services} file, so that
 * {@link ClientProtocolFactories#forId(ShapeId)} can find it.
 */
public final class TestClientProtocolFactory implements ClientProtocolFactory<Trait> {
    static final ShapeId ID = ShapeId.from("com.example.test#testProtocol");

    @Override
    public ShapeId id() {
        return ID;
    }

    @Override
    public ClientProtocol<?, ?> createProtocol(ProtocolSettings settings, Trait protocolTrait) {
        return new TestClientProtocol(settings);
    }

    static final class TestClientProtocol implements ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> {
        private final ProtocolSettings settings;

        TestClientProtocol(ProtocolSettings settings) {
            this.settings = settings;
        }

        ProtocolSettings settings() {
            return settings;
        }

        @Override
        public ShapeId id() {
            return ID;
        }

        @Override
        public Codec payloadCodec() {
            return null;
        }

        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest serializeRequest(
            ApiOperation<I, O> operation, I input, ExecutionAttributes context, URI endpoint) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SdkHttpFullRequest updateServiceEndpoint(SdkHttpFullRequest request, Endpoint endpoint) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <I extends SerializableStruct, O extends SerializableStruct> O deserializeResponse(
            ApiOperation<I, O> operation, ExecutionAttributes context, TypeRegistry errorRegistry,
            SdkHttpFullRequest request, SdkHttpFullResponse response) {
            throw new UnsupportedOperationException();
        }
    }
}
