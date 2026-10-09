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
package software.amazon.awssdk.test.boundedmemory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Factory;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
/**
 * Reactive Streams TCK (TestNG: subscriber rules, including demand, cancellation and early termination) for the
 * payload subscribers of the public {@link AsyncResponseTransformer} factories, {@code toBytes()} and
 * {@code toFile(Path)}: each subscriber under test is the one the transformer subscribes to the payload publisher it is
 * handed in {@link AsyncResponseTransformer#onStream}, after {@code prepare()} and {@code onResponse()} as the SDK
 * calls them.
 */
public final class AsyncResponseTransformerSubscriberTckTest extends SubscriberBlackboxVerification<ByteBuffer> {
    private static final AtomicInteger FILES = new AtomicInteger();
    private final boolean toFile;
    private final Path directory;
    public AsyncResponseTransformerSubscriberTckTest(boolean toFile) {
        super(new TestEnvironment());
        this.toFile = toFile;
        try {
            this.directory = Files.createTempDirectory("transformer-tck");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    @Factory
    public static Object[] factories() {
        return new Object[] {new AsyncResponseTransformerSubscriberTckTest(false),
                             new AsyncResponseTransformerSubscriberTckTest(true)};
    }
    @Override
    public Subscriber<ByteBuffer> createSubscriber() {
        AsyncResponseTransformer<String, ?> transformer =
            toFile ? AsyncResponseTransformer.toFile(directory.resolve("payload-" + FILES.incrementAndGet()))
                   : AsyncResponseTransformer.toBytes();
        transformer.prepare();
        transformer.onResponse("response");
        AtomicReference<Subscriber<? super ByteBuffer>> captured = new AtomicReference<>();
        transformer.onStream(captured::set);
        @SuppressWarnings("unchecked")
        Subscriber<ByteBuffer> subscriber = (Subscriber<ByteBuffer>) captured.get();
        if (subscriber == null) {
            throw new IllegalStateException("The transformer did not subscribe to the payload in onStream");
        }
        return subscriber;
    }
    @Override
    public ByteBuffer createElement(int element) {
        return ByteBuffer.wrap(new byte[] {(byte) element, (byte) (element >>> 8), 7});
    }
    @AfterClass
    public void deleteFiles() throws IOException {
        try (var files = Files.list(directory)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                Files.deleteIfExists(file);
            }
        }
        Files.deleteIfExists(directory);
    }
    @Override
    public String toString() {
        return "AsyncResponseTransformerSubscriberTckTest[" + (toFile ? "toFile" : "toBytes") + "]";
    }
}
