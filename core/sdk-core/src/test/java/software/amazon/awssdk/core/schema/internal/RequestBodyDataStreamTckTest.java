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

package software.amazon.awssdk.core.schema.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * Reactive Streams TCK for the publisher of a {@link RequestBodyDataStream}: one element per chunk of the body.
 */
public class RequestBodyDataStreamTckTest extends PublisherVerification<ByteBuffer> {
    private static final int MAX_ELEMENTS = 100;

    public RequestBodyDataStreamTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public long maxElementsFromPublisher() {
        return MAX_ELEMENTS;
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        byte[] content = new byte[(int) elements * RequestBodyDataStream.CHUNK_SIZE];
        return new RequestBodyDataStream(RequestBody.fromBytes(content));
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return new RequestBodyDataStream(RequestBody.fromContentProvider(() -> new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("failed");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("failed");
            }
        }, "application/octet-stream"));
    }
}
