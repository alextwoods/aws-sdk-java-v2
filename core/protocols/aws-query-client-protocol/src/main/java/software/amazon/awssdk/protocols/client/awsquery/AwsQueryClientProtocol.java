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
import software.amazon.awssdk.schema.traits.AwsQueryTrait;

/**
 * The client protocol for {@code aws.protocols#awsQuery}: the
 * <a href="https://smithy.io/2.0/aws/protocols/aws-query-protocol.html">AWS query protocol</a>.
 *
 * <p>A request for operation {@code Publish} of a service with version {@code 2010-03-31} is sent as
 * <pre>
 * POST /
 * Content-Type: application/x-www-form-urlencoded
 * Content-Length: &lt;length&gt;
 *
 * Action=Publish&amp;Version=2010-03-31&amp;TopicArn=...&amp;Message=hello
 * </pre>
 * with form keys built from member names and {@code xmlName}, {@code Name.member.N} lists ({@code Name.N} when
 * {@code xmlFlattened}) and {@code Name.entry.N.key}/{@code .value} maps.
 *
 * <p>The output is read by {@link XmlCodec} from {@code <PublishResponse><PublishResult>}, and the request ID of the
 * response metadata from {@code <PublishResponse><ResponseMetadata><RequestId>}. Errors are resolved from the
 * {@code Code} of {@code <ErrorResponse><Error>}: an {@code awsQueryError} custom code first, then the shape name in
 * the service namespace. The message is the error's {@code Message}, and the request ID the {@code RequestId} next to
 * {@code <Error>}.
 *
 * <p>{@link #payloadCodec()} returns null.
 *
 * <p>Instances are created once per client and are safe to use from multiple threads. Create one with
 * {@link #AwsQueryClientProtocol(ProtocolSettings)}, or through {@link Factory}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.client.awsquery.AwsQueryClientProtocol}.
 */
@SdkPublicApi
@ThreadSafe
public final class AwsQueryClientProtocol extends QueryClientProtocol {
    /**
     * Creates the protocol for a service.
     *
     * @param settings the settings of the service. The service version is the {@code Version} of every request, the
     *     service namespace resolves error codes, and the base exception builder creates the exception for errors that
     *     do not resolve to a modeled error
     * @throws NullPointerException if {@code settings} or its service version is null
     */
    public AwsQueryClientProtocol(ProtocolSettings settings) {
        super(AwsQueryTrait.ID, settings, false);
    }

    @Override
    public String toString() {
        return "AwsQueryClientProtocol(" + service() + ")";
    }

    /**
     * Creates {@link AwsQueryClientProtocol}s. Generated clients use {@link #INSTANCE} directly; the factory is also
     * registered for {@link software.amazon.awssdk.core.client.protocol.ClientProtocolFactories#forId(ShapeId)}.
     */
    @SdkPublicApi
    @ThreadSafe
    public static final class Factory implements ClientProtocolFactory<AwsQueryTrait> {
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
         * Returns {@link AwsQueryTrait#ID}.
         *
         * @return {@code aws.protocols#awsQuery}
         */
        @Override
        public ShapeId id() {
            return AwsQueryTrait.ID;
        }

        /**
         * Creates the protocol. The trait has no settings.
         *
         * @param settings the settings of the service, with its version
         * @param protocolTrait the protocol trait; may be null
         * @return a new {@link AwsQueryClientProtocol}
         * @throws NullPointerException if {@code settings} or its service version is null
         */
        @Override
        public AwsQueryClientProtocol createProtocol(ProtocolSettings settings, AwsQueryTrait protocolTrait) {
            return new AwsQueryClientProtocol(settings);
        }
    }
}
