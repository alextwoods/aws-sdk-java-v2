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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.io.DataStream;
import software.amazon.awssdk.schema.serde.RecordingShapeSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;

/**
 * Data stream and event stream documents: the values of streaming members.
 */
class StreamDocumentTest {

    @Test
    void of_dataStream_isABlobDocumentHoldingTheStream() {
        DataStream stream = new TestDataStream();

        Document document = Document.of(PreludeSchemas.BLOB, stream);

        assertThat(document.type()).isEqualTo(ShapeType.BLOB);
        assertThat(document.asDataStream()).isSameAs(stream);
        assertThat(document.asObject()).isSameAs(stream);
        assertThat(document).hasToString("Document(blob: data stream)");
        assertThatThrownBy(document::asBlob)
            .isInstanceOf(SerializationException.class)
            .hasMessage("A data stream document has no bytes in memory; read it with asDataStream()");
        assertThatThrownBy(document::asEventStream)
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected an event stream document, but found blob");
        assertThatThrownBy(() -> document.serializeContents(new RecordingShapeSerializer()))
            .isInstanceOf(SerializationException.class)
            .hasMessage("A data stream document cannot be serialized; a streaming blob member is sent as the request "
                        + "body");
    }

    @Test
    void ofEventStream_isAUnionDocumentHoldingThePublisher() {
        Publisher<Document> events = new NoEvents();

        Document document = Document.ofEventStream(PreludeSchemas.DOCUMENT, events);

        assertThat(document.type()).isEqualTo(ShapeType.UNION);
        assertThat(document.asEventStream()).isSameAs(events);
        assertThat(document.asObject()).isSameAs(events);
        assertThat(document).hasToString("Document(union: event stream)");
        assertThatThrownBy(document::asMap)
            .isInstanceOf(SerializationException.class)
            .hasMessage("An event stream document has no members; read it with asEventStream()");
        assertThatThrownBy(document::asDataStream)
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a data stream document, but found union");
        assertThatThrownBy(() -> document.serializeContents(new RecordingShapeSerializer()))
            .isInstanceOf(SerializationException.class)
            .hasMessage("An event stream document cannot be serialized; each event is sent as an event stream message");
    }

    @Test
    void equals_streamDocuments_compareTheStreamInstances() {
        DataStream stream = new TestDataStream();
        Publisher<Document> events = new NoEvents();
        Document data = Document.of(PreludeSchemas.BLOB, stream);
        Document eventDocument = Document.ofEventStream(PreludeSchemas.DOCUMENT, events);

        assertThat(data).isEqualTo(Document.of(PreludeSchemas.STRING, stream)).hasSameHashCodeAs(
            Document.of(PreludeSchemas.STRING, stream));
        assertThat(data).isNotEqualTo(Document.of(PreludeSchemas.BLOB, new TestDataStream()));
        assertThat(data).isNotEqualTo(Document.of(new byte[0]));
        assertThat(Document.of(new byte[0])).isNotEqualTo(data);
        assertThat(eventDocument).isEqualTo(Document.ofEventStream(PreludeSchemas.DOCUMENT, events))
                                 .hasSameHashCodeAs(Document.ofEventStream(PreludeSchemas.DOCUMENT, events));
        assertThat(eventDocument).isNotEqualTo(Document.ofEventStream(PreludeSchemas.DOCUMENT, new NoEvents()));
        assertThat(Document.of(Collections.singletonMap("a", eventDocument)))
            .isEqualTo(Document.of(Collections.singletonMap("a", Document.ofEventStream(PreludeSchemas.DOCUMENT,
                                                                                         events))))
            .isNotEqualTo(Document.of(Collections.singletonMap("a", Document.of(Collections.emptyMap()))));
    }

    @Test
    void asStreams_otherDocuments_throw() {
        assertThatThrownBy(() -> Document.of(new byte[] {1}).asDataStream())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a data stream document, but found blob");
        assertThatThrownBy(() -> Document.EMPTY_MAP.asEventStream())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected an event stream document, but found map");
    }

    @Test
    void factories_nullArguments_throw() {
        assertThatThrownBy(() -> Document.of(null, new TestDataStream())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Document.of(PreludeSchemas.BLOB, (DataStream) null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Document.ofEventStream(PreludeSchemas.DOCUMENT, null))
            .isInstanceOf(NullPointerException.class);
    }

    private static final class TestDataStream implements DataStream {
        @Override
        public long contentLength() {
            return -1;
        }

        @Override
        public String contentType() {
            return null;
        }

        @Override
        public boolean isReplayable() {
            return false;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class NoEvents implements Publisher<Document> {
        @Override
        public void subscribe(Subscriber<? super Document> subscriber) {
            throw new UnsupportedOperationException();
        }
    }
}
