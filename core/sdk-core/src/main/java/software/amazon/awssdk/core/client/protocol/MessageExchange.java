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

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Identifies the pair of message types that a {@link ClientProtocol} produces and consumes and that a
 * {@link ClientTransport} or {@link AsyncClientTransport} sends and receives.
 *
 * <p>A protocol and a transport can be used together only when their message exchanges are equal. Implementations are
 * usually singletons, such as {@link HttpMessageExchange#INSTANCE}, so equality is identity. An implementation's
 * {@link Object#toString()} should name its request and response types, because a client build that finds a mismatch
 * reports both exchanges.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.core.MessageExchange}.
 *
 * @param <RequestT> the request message type
 * @param <ResponseT> the response message type
 */
@SdkPublicApi
public interface MessageExchange<RequestT, ResponseT> {
}
