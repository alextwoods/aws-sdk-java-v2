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

package software.amazon.awssdk.core.sync;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import software.amazon.awssdk.core.ResponseBytes;

/**
 * Consumes the streaming payload of a sync streaming-output operation and produces the call's result, source
 * compatible with the V2 {@code ResponseTransformer} factories {@code toFile}, {@code toOutputStream} and
 * {@code toBytes}.
 *
 * <p>The transformer owns the consumption policy (design, "Generated sync client"): the SDK closes the stream after
 * {@link #transform} returns unless {@link #needsConnectionLeftOpen()} is true, in which case body-close responsibility
 * passes to the returned value.
 *
 * <p>ADAPTED from V2: {@link #transform} receives a plain {@link InputStream} (V2 {@code AbortableInputStream} and the
 * {@code toInputStream}/{@code ResponseInputStream} path arrive with the pipeline's abort handles), and read failures
 * are thrown as {@link IOException}; whether one is retried is the retry strategy's decision, not a
 * {@code RetryableException} raised here.
 *
 * @param <ResponseT> the unmarshalled response type
 * @param <ReturnT> the result type
 */
@FunctionalInterface
public interface ResponseTransformer<ResponseT, ReturnT> {
    /**
     * Consumes {@code inputStream}, the streaming payload of {@code response}, and returns the call's result.
     *
     * @throws Exception if consumption fails; the SDK rethrows it, possibly wrapped in an SDK client exception
     */
    ReturnT transform(ResponseT response, InputStream inputStream) throws Exception;

    /** True if the payload stream must stay open after {@link #transform} returns. */
    default boolean needsConnectionLeftOpen() {
        return false;
    }

    /** Diagnostic name of the transformer. */
    default String name() {
        return "UNKNOWN";
    }

    /**
     * Writes the payload to a new file at {@code path} and returns the response. The file must not exist, and its
     * parent directories must. A partially written file is deleted when the copy fails.
     */
    static <ResponseT> ResponseTransformer<ResponseT, ResponseT> toFile(Path path) {
        Objects.requireNonNull(path, "path");
        return new ResponseTransformer<>() {
            @Override
            public ResponseT transform(ResponseT response, InputStream inputStream) throws IOException {
                try {
                    Files.copy(inputStream, path);
                    return response;
                } catch (IOException e) {
                    // Files.copy creates the target first, so a failed copy may leave a partial file behind; a
                    // FileAlreadyExistsException means the file was not ours, and it is left alone.
                    if (!(e instanceof FileAlreadyExistsException)) {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException deletion) {
                            e.addSuppressed(deletion);
                        }
                    }
                    throw e;
                }
            }

            @Override
            public String name() {
                return "FILE";
            }
        };
    }

    /** {@link #toFile(Path)} of {@code file}. */
    static <ResponseT> ResponseTransformer<ResponseT, ResponseT> toFile(File file) {
        return toFile(file.toPath());
    }

    /** Copies the payload to the caller-owned {@code outputStream}, which is not closed, and returns the response. */
    static <ResponseT> ResponseTransformer<ResponseT, ResponseT> toOutputStream(OutputStream outputStream) {
        Objects.requireNonNull(outputStream, "outputStream");
        return new ResponseTransformer<>() {
            @Override
            public ResponseT transform(ResponseT response, InputStream inputStream) throws IOException {
                inputStream.transferTo(outputStream);
                return response;
            }

            @Override
            public String name() {
                return "STREAM";
            }
        };
    }

    /** Reads the payload into memory. */
    static <ResponseT> ResponseTransformer<ResponseT, ResponseBytes<ResponseT>> toBytes() {
        return new ResponseTransformer<>() {
            @Override
            public ResponseBytes<ResponseT> transform(ResponseT response, InputStream inputStream) throws IOException {
                return ResponseBytes.fromByteArrayUnsafe(response, inputStream.readAllBytes());
            }

            @Override
            public String name() {
                return "BYTES";
            }
        };
    }
}
