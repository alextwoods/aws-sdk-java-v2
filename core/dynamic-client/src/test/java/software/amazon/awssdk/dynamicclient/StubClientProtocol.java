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

package software.amazon.awssdk.dynamicclient;

import java.net.URI;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.TypeRegistry;

/**
 * A protocol that serializes nothing, with a chosen message exchange. Construction tests only store and validate it.
 */
final class StubClientProtocol implements ClientProtocol<Object, Object> {
    /**
     * A message exchange that no SDK transport uses.
     */
    static final MessageExchange<Object, Object> OTHER_EXCHANGE = new MessageExchange<Object, Object>() {
        @Override
        public String toString() {
            return "OtherExchange";
        }
    };

    private final MessageExchange<Object, Object> exchange;

    @SuppressWarnings("unchecked")
    private StubClientProtocol(MessageExchange<?, ?> exchange) {
        this.exchange = (MessageExchange<Object, Object>) exchange;
    }

    static StubClientProtocol http() {
        return new StubClientProtocol(HttpMessageExchange.INSTANCE);
    }

    static StubClientProtocol otherExchange() {
        return new StubClientProtocol(OTHER_EXCHANGE);
    }

    @Override
    public ShapeId id() {
        return ShapeId.from("test.stub#stubProtocol");
    }

    @Override
    public Codec payloadCodec() {
        return null;
    }

    @Override
    public MessageExchange<Object, Object> messageExchange() {
        return exchange;
    }

    @Override
    public <I extends SerializableStruct, O extends SerializableStruct> Object serializeRequest(
        ApiOperation<I, O> operation, I input, ExecutionAttributes context, URI endpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object updateServiceEndpoint(Object request, Endpoint endpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <I extends SerializableStruct, O extends SerializableStruct> O deserializeResponse(
        ApiOperation<I, O> operation, ExecutionAttributes context, TypeRegistry errorRegistry, Object request,
        Object response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "StubClientProtocol(" + exchange + ")";
    }
}
