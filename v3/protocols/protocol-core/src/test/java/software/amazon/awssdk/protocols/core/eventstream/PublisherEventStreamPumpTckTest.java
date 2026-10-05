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

package software.amazon.awssdk.protocols.core.eventstream;

import java.io.IOException;
import java.io.InputStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.event.EventStream;

/**
 * Reactive Streams TCK (TestNG) for the subscriber {@link PublisherEventStream} subscribes to a caller's publisher:
 * each subscriber writes into an activated writer whose frames a background thread drains, as a transport does.
 */
public final class PublisherEventStreamPumpTckTest extends SubscriberBlackboxVerification<SerializableStruct> {
    public PublisherEventStreamPumpTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<SerializableStruct> createSubscriber() {
        EventStream<SerializableStruct> writer = EventStream.newWriter();
        InputStream frames = EventStreams.requestBody(writer, PublisherEventStreamTest.CODEC, null).openStream();
        Thread.ofVirtual().start(() -> drain(frames));
        return new PublisherEventStream.Pump<>(writer.asWriter());
    }

    @Override
    public SerializableStruct createElement(int element) {
        return PublisherEventStreamTest.event("e" + element);
    }

    private static void drain(InputStream frames) {
        byte[] discard = new byte[8192];
        try (frames) {
            while (frames.read(discard) >= 0) {
                // Discard the encoded frames.
            }
        } catch (IOException | RuntimeException e) {
            // The stream ended with an error (a TCK publisher error) or was cancelled.
        }
    }
}
