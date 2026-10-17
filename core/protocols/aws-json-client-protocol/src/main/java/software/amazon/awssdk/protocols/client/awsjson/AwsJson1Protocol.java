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

package software.amazon.awssdk.protocols.client.awsjson;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactory;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.AwsJson1_0Trait;

/**
 * The client protocol for {@code aws.protocols#awsJson1_0}: the
 * <a href="https://smithy.io/2.0/aws/protocols/aws-json-1_0-protocol.html">AWS JSON 1.0 protocol</a>, with payloads
 * serialized by {@link JsonCodec} with {@link JsonSettings#AWS_JSON}.
 *
 * <p>A request for operation {@code GetQueueUrl} of a service whose shape name is {@code AmazonSQS} is sent as
 * <pre>
 * POST /
 * X-Amz-Target: AmazonSQS.GetQueueUrl
 * Content-Type: application/x-amz-json-1.0
 * Content-Length: &lt;length&gt;
 *
 * {"QueueName":"my-queue"}
 * </pre>
 * Error responses are resolved from the {@code X-Amzn-Errortype} header, then the body's {@code __type} member, then
 * its {@code code} member.
 *
 * <p>Instances are created once per client and are safe to use from multiple threads. Create one with
 * {@link #AwsJson1Protocol(ProtocolSettings)}, or through {@link Factory}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol}.
 */
@SdkPublicApi
@ThreadSafe
public final class AwsJson1Protocol extends AwsJsonProtocol {
    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";

    /**
     * Creates the protocol for a service.
     *
     * @param settings the settings of the service. The service shape name forms {@code X-Amz-Target}; the service
     *     schema, when set, tells whether the service is {@code aws.protocols#awsQueryCompatible}; and the base exception
     *     builder creates the exception for errors that do not resolve to a modeled error
     * @throws NullPointerException if {@code settings} is null
     */
    public AwsJson1Protocol(ProtocolSettings settings) {
        super(AwsJson1_0Trait.ID, settings, CONTENT_TYPE);
    }

    @Override
    public String toString() {
        return "AwsJson1Protocol(" + service() + ")";
    }

    /**
     * Creates {@link AwsJson1Protocol}s. Generated clients use {@link #INSTANCE} directly; the factory is also
     * registered for {@link software.amazon.awssdk.core.client.protocol.ClientProtocolFactories#forId(ShapeId)}.
     */
    @SdkPublicApi
    @ThreadSafe
    public static final class Factory implements ClientProtocolFactory<AwsJson1_0Trait> {
        /**
         * The shared factory instance.
         */
        public static final Factory INSTANCE = new Factory();

        /**
         * Creates a factory. Used by {@link java.util.ServiceLoader}; prefer {@link #INSTANCE}.
         */
        public Factory() {
        }

        /**
         * Returns {@link AwsJson1_0Trait#ID}.
         *
         * @return {@code aws.protocols#awsJson1_0}
         */
        @Override
        public ShapeId id() {
            return AwsJson1_0Trait.ID;
        }

        /**
         * Creates the protocol. The trait has no settings that change the protocol's behavior; its {@code http} and
         * {@code eventStreamHttp} lists are for the HTTP client.
         *
         * @param settings the settings of the service
         * @param protocolTrait the protocol trait; may be null
         * @return a new {@link AwsJson1Protocol}
         * @throws NullPointerException if {@code settings} is null
         */
        @Override
        public AwsJson1Protocol createProtocol(ProtocolSettings settings, AwsJson1_0Trait protocolTrait) {
            return new AwsJson1Protocol(settings);
        }
    }
}
