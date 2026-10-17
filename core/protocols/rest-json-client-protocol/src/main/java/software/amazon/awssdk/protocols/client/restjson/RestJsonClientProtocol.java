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

package software.amazon.awssdk.protocols.client.restjson;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactory;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.protocols.client.http.AmznErrorHeaderExtractor;
import software.amazon.awssdk.protocols.client.http.HttpBindingClientProtocol;
import software.amazon.awssdk.protocols.client.http.HttpBindingErrorFactory;
import software.amazon.awssdk.protocols.client.http.HttpErrorDeserializer;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.RestJson1Trait;
import software.amazon.awssdk.utils.Validate;

/**
 * The client protocol for {@code aws.protocols#restJson1}: the
 * <a href="https://smithy.io/2.0/aws/protocols/aws-restjson1-protocol.html">AWS restJson1 protocol</a>. Members are
 * placed by their HTTP binding traits, and the body is serialized by {@link JsonCodec} with
 * {@link JsonSettings#REST_JSON_1} ({@code jsonName}, {@code timestampFormat}, epoch-seconds by default) as
 * {@code application/json}.
 *
 * <p>Like the earlier SDK restJson1 marshallers:
 * <ul>
 *     <li>an input without body members and without a payload member is sent without a body;</li>
 *     <li>an input whose structure {@code @httpPayload} member is null is sent with {@code {}};</li>
 *     <li>one leading {@code /} of a greedy label value is dropped ({@code /a/{key+}} with {@code key = "/b"} is
 *     {@code /a/b});</li>
 *     <li>the response {@code Content-Type} is not checked, and the body of an output without body or payload members is
 *     not read.</li>
 * </ul>
 * Error responses are resolved from the {@code X-Amzn-Errortype} header, then the body's {@code __type} member, then its
 * {@code code} member, with anything from the first {@code :} dropped; the message comes from the
 * {@code X-Amzn-Error-Message} header, then the body's {@code message}, {@code Message} or {@code errorMessage} member.
 * A modeled error's members are read through their HTTP binding traits ({@link HttpBindingErrorFactory}).
 *
 * <p>Streaming payloads and event streams are not supported yet.
 *
 * <p>Instances are created once per client and are safe to use from multiple threads. Create one with
 * {@link #RestJsonClientProtocol(ProtocolSettings)}, or through {@link Factory}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.client.restjson.RestJsonClientProtocol}.
 */
@SdkPublicApi
@ThreadSafe
public final class RestJsonClientProtocol extends HttpBindingClientProtocol {
    private static final String MEDIA_TYPE = "application/json";

    private final ShapeId service;
    private final JsonCodec codec;
    private final HttpErrorDeserializer errorDeserializer;

    /**
     * Creates the protocol for a service.
     *
     * @param settings the settings of the service. The service namespace resolves relative error types, and the base
     *     exception builder creates the exception for errors that do not resolve to a modeled error
     * @throws NullPointerException if {@code settings} is null
     */
    public RestJsonClientProtocol(ProtocolSettings settings) {
        super(RestJson1Trait.ID);
        Validate.paramNotNull(settings, "settings");
        this.service = settings.service();
        this.codec = JsonCodec.builder()
                              .settings(JsonSettings.REST_JSON_1)
                              .defaultNamespace(service.getNamespace())
                              .build();
        this.errorDeserializer = HttpErrorDeserializer.builder()
                                                      .codec(codec)
                                                      .protocolSettings(settings)
                                                      .headerErrorExtractor(AmznErrorHeaderExtractor.INSTANCE)
                                                      .errorPayloadParser(
                                                          HttpErrorDeserializer.DEFAULT_ERROR_PAYLOAD_PARSER)
                                                      .knownErrorFactory(new HttpBindingErrorFactory(httpBinding()))
                                                      .build();
    }

    @Override
    public Codec payloadCodec() {
        return codec;
    }

    @Override
    protected String payloadMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    protected HttpErrorDeserializer errorDeserializer() {
        return errorDeserializer;
    }

    /**
     * Returns true: an input without body or payload members is sent without a body.
     */
    @Override
    protected boolean omitEmptyPayload() {
        return true;
    }

    /**
     * Returns true when the input has a structure {@code @httpPayload} member, so a null payload is sent as {@code {}}.
     */
    @Override
    protected boolean allowEmptyStructPayload(Schema inputSchema) {
        return httpBinding().hasStructPayload(inputSchema);
    }

    /**
     * Returns true, as the earlier SDK restJson1 marshallers ({@code PathMarshaller.GREEDY}).
     */
    @Override
    protected boolean trimGreedyLabelLeadingSlash() {
        return true;
    }

    @Override
    public String toString() {
        return "RestJsonClientProtocol(" + service + ")";
    }

    /**
     * Creates {@link RestJsonClientProtocol}s. Generated clients use {@link #INSTANCE} directly; the factory is also
     * registered for {@link software.amazon.awssdk.core.client.protocol.ClientProtocolFactories#forId(ShapeId)}.
     */
    @SdkPublicApi
    @ThreadSafe
    public static final class Factory implements ClientProtocolFactory<RestJson1Trait> {
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
         * Returns {@link RestJson1Trait#ID}.
         *
         * @return {@code aws.protocols#restJson1}
         */
        @Override
        public ShapeId id() {
            return RestJson1Trait.ID;
        }

        /**
         * Creates the protocol. The trait has no settings that change the protocol's behavior; its {@code http} and
         * {@code eventStreamHttp} lists are for the HTTP client.
         *
         * @param settings the settings of the service
         * @param protocolTrait the protocol trait; may be null
         * @return a new {@link RestJsonClientProtocol}
         * @throws NullPointerException if {@code settings} is null
         */
        @Override
        public RestJsonClientProtocol createProtocol(ProtocolSettings settings, RestJson1Trait protocolTrait) {
            return new RestJsonClientProtocol(settings);
        }
    }
}
