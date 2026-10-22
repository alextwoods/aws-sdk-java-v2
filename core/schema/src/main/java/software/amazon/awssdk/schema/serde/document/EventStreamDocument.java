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

package software.amazon.awssdk.schema.serde.document;

import java.util.Map;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.Validate;

/**
 * A document that holds the events of an event stream member (a union with {@code smithy.api#streaming}) as a
 * {@link Publisher} of documents, one per event. Its type is {@link ShapeType#UNION}, but it has no members:
 * {@link #asMap()} throws, and {@link #asEventStream()} and {@link #asObject()} return the publisher. Two event stream
 * documents are equal when they hold the same publisher instance.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.document.EventStreamDocument}, holding a
 * {@code Publisher<Document>} in place of smithy-java's {@code EventStream}, which the SDK does not port.
 */
@SdkInternalApi
final class EventStreamDocument extends Documents.AbstractDocument {
    private final Publisher<Document> events;

    EventStreamDocument(Schema schema, Publisher<Document> events) {
        super(Validate.paramNotNull(schema, "schema"));
        this.events = Validate.paramNotNull(events, "events");
    }

    @Override
    public ShapeType type() {
        return ShapeType.UNION;
    }

    @Override
    public Publisher<Document> asEventStream() {
        return events;
    }

    @Override
    public Map<String, Document> asMap() {
        throw new SerializationException("An event stream document has no members; read it with asEventStream()");
    }

    @Override
    public Object asObject() {
        return events;
    }

    /**
     * Throws: the events are not a data model value, and a codec cannot write them. Each event is encoded as one event
     * stream message instead (smithy-java writes the stream with {@code writeEventStream}, which no SDK serializer
     * has).
     */
    @Override
    public void serializeContents(ShapeSerializer serializer) {
        throw new SerializationException("An event stream document cannot be serialized; each event is sent as an "
                                         + "event stream message");
    }

    @Override
    public String toString() {
        return describe("event stream");
    }
}
