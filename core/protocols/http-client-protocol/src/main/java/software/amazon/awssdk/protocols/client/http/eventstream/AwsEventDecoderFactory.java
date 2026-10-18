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

package software.amazon.awssdk.protocols.client.http.eventstream;

import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.event.EventDecoder;
import software.amazon.awssdk.schema.serde.event.EventDecoderFactory;
import software.amazon.awssdk.schema.serde.event.FrameDecoder;
import software.amazon.awssdk.utils.Validate;

/**
 * Creates decoders of the AWS event stream encoding ({@code application/vnd.amazon.eventstream}) for an operation's
 * input or output event stream.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.events.AwsEventDecoderFactory}, without the
 * {@code FrameProcessor}. SDK additions: {@link #withEventBuilders(Function)} gives the builder of the event class of
 * each union member (the generated per-member event classes), and {@link #withUnknownEvent(Supplier)} the value
 * delivered for an unknown {@code :event-type} (the generated visitor's unknown-event value).
 */
@SdkPublicApi
public final class AwsEventDecoderFactory implements EventDecoderFactory<AwsEventFrame> {
    private final Supplier<? extends ShapeBuilder<?>> initialEventBuilder;
    private final Supplier<? extends ShapeBuilder<?>> unionBuilder;
    private final Function<String, ? extends ShapeBuilder<?>> memberBuilders;
    private final Supplier<?> unknownEvent;
    private final Schema eventSchema;
    private final Codec codec;

    private AwsEventDecoderFactory(Supplier<? extends ShapeBuilder<?>> initialEventBuilder,
                                   Supplier<? extends ShapeBuilder<?>> unionBuilder,
                                   Function<String, ? extends ShapeBuilder<?>> memberBuilders,
                                   Supplier<?> unknownEvent,
                                   Schema eventSchema,
                                   Codec codec) {
        this.initialEventBuilder = Validate.paramNotNull(initialEventBuilder, "initialEventBuilder");
        this.unionBuilder = unionBuilder;
        this.memberBuilders = memberBuilders;
        this.unknownEvent = unknownEvent;
        Validate.paramNotNull(eventSchema, "eventSchema");
        this.eventSchema = eventSchema.isMember() ? eventSchema.memberTarget() : eventSchema;
        this.codec = Validate.paramNotNull(codec, "codec");
    }

    /**
     * Creates a decoder factory for the operation's input event stream.
     *
     * @param operation the operation, which has an input event stream
     * @param codec the codec of the payloads
     * @return the factory
     */
    public static AwsEventDecoderFactory forInputStream(ApiOperation<?, ?> operation, Codec codec) {
        return new AwsEventDecoderFactory(operation::inputBuilder, operation.inputEventBuilderSupplier(), null, null,
                                          AwsEventEncoderFactory.streamMember(operation.inputStreamMember()), codec);
    }

    /**
     * Creates a decoder factory for the operation's output event stream.
     *
     * @param operation the operation, which has an output event stream
     * @param codec the codec of the payloads
     * @return the factory
     */
    public static AwsEventDecoderFactory forOutputStream(ApiOperation<?, ?> operation, Codec codec) {
        return new AwsEventDecoderFactory(operation::outputBuilder, operation.outputEventBuilderSupplier(), null, null,
                                          AwsEventEncoderFactory.streamMember(operation.outputStreamMember()), codec);
    }

    /**
     * Returns a factory whose decoders build each event with the builder {@code memberBuilders} returns for the union
     * member name, or with the member target's builder when it returns null. The events are not wrapped in a union.
     *
     * @param memberBuilders the builders by union member name
     * @return the new factory
     */
    public AwsEventDecoderFactory withEventBuilders(Function<String, ? extends ShapeBuilder<?>> memberBuilders) {
        return new AwsEventDecoderFactory(initialEventBuilder, null, Validate.paramNotNull(memberBuilders,
                                                                                           "memberBuilders"),
                                          unknownEvent, eventSchema, codec);
    }

    /**
     * Returns a factory whose decoders return {@code unknownEvent}'s value for an event whose {@code :event-type} is
     * not a member of the event stream union, instead of failing.
     *
     * @param unknownEvent supplies the unknown-event value
     * @return the new factory
     */
    public AwsEventDecoderFactory withUnknownEvent(Supplier<?> unknownEvent) {
        return new AwsEventDecoderFactory(initialEventBuilder, unionBuilder, memberBuilders,
                                          Validate.paramNotNull(unknownEvent, "unknownEvent"), eventSchema, codec);
    }

    @Override
    public EventDecoder<AwsEventFrame> newEventDecoder() {
        return new AwsEventShapeDecoder(initialEventBuilder, unionBuilder, memberBuilders, unknownEvent, eventSchema,
                                        codec);
    }

    @Override
    public FrameDecoder<AwsEventFrame> newFrameDecoder() {
        return AwsFrameCodec.newDecoder();
    }
}
