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

package software.amazon.awssdk.protocols.client.awsquery;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.xml.XmlCodec;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactory;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.Ec2QueryTrait;

/**
 * The client protocol for {@code aws.protocols#ec2Query}: the
 * <a href="https://smithy.io/2.0/aws/protocols/aws-ec2-query-protocol.html">AWS EC2 query protocol</a>.
 *
 * <p>Requests are form-encoded like awsQuery requests, with the EC2 key rules: {@code ec2QueryName}, else the
 * capitalized {@code xmlName} or member name, and lists always as {@code Name.N}.
 *
 * <p>The output is read by {@link XmlCodec} from {@code <OpResponse>} (no result wrapper), and the request ID of the
 * response metadata from its {@code <requestId>} child. Errors are resolved from the {@code Code} of the first
 * {@code <Response><Errors><Error>}: an {@code awsQueryError} custom code first, then the shape name in the service
 * namespace. The message is the error's {@code Message}, and the request ID the {@code RequestID} next to
 * {@code <Errors>}.
 *
 * <p>{@link #payloadCodec()} returns null.
 *
 * <p>Instances are created once per client and are safe to use from multiple threads. Create one with
 * {@link #Ec2QueryClientProtocol(ProtocolSettings)}, or through {@link Factory}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.client.awsquery.Ec2QueryClientProtocol}.
 */
@SdkPublicApi
@ThreadSafe
public final class Ec2QueryClientProtocol extends QueryClientProtocol {
    /**
     * Creates the protocol for a service.
     *
     * @param settings the settings of the service. The service version is the {@code Version} of every request, the
     *     service namespace resolves error codes, and the base exception builder creates the exception for errors that
     *     do not resolve to a modeled error
     * @throws NullPointerException if {@code settings} or its service version is null
     */
    public Ec2QueryClientProtocol(ProtocolSettings settings) {
        super(Ec2QueryTrait.ID, settings, true);
    }

    @Override
    public String toString() {
        return "Ec2QueryClientProtocol(" + service() + ")";
    }

    /**
     * Creates {@link Ec2QueryClientProtocol}s. Generated clients use {@link #INSTANCE} directly; the factory is also
     * registered for {@link software.amazon.awssdk.core.client.protocol.ClientProtocolFactories#forId(ShapeId)}.
     */
    @SdkPublicApi
    @ThreadSafe
    public static final class Factory implements ClientProtocolFactory<Ec2QueryTrait> {
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
         * Returns {@link Ec2QueryTrait#ID}.
         *
         * @return {@code aws.protocols#ec2Query}
         */
        @Override
        public ShapeId id() {
            return Ec2QueryTrait.ID;
        }

        /**
         * Creates the protocol. The trait has no settings.
         *
         * @param settings the settings of the service, with its version
         * @param protocolTrait the protocol trait; may be null
         * @return a new {@link Ec2QueryClientProtocol}
         * @throws NullPointerException if {@code settings} or its service version is null
         */
        @Override
        public Ec2QueryClientProtocol createProtocol(ProtocolSettings settings, Ec2QueryTrait protocolTrait) {
            return new Ec2QueryClientProtocol(settings);
        }
    }
}
