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

package software.amazon.awssdk.checksums.compression;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.checksums.internal.FeatureSupport;
import software.amazon.awssdk.checksums.internal.GzipTransform;
import software.amazon.awssdk.checksums.internal.TransformedBodies;
import software.amazon.awssdk.core.exception.ModelConflictException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.RequestCompressionTrait;

/**
 * The request compression and content-encoding feature plugin of one operation (Requirements 13.8 and 13.18): the
 * Smithy {@code requestCompression} trait.
 *
 * <p>{@link #forOperation} builds the plan once per client and operation: it selects the first encoding of the
 * modeled priority list this runtime supports ({@code gzip}) and ignores later ones, and rejects an input whose
 * streaming member also has {@code @requiresLength}, which the trait forbids. Per attempt, after serialization and
 * before the request checksum and signing, {@link #apply}:
 * <ul>
 *   <li>does nothing when compression is disabled, the payload is empty, or a non-streaming payload is smaller than
 *       the minimum compression size; a modeled streaming payload is compressed regardless of size;</li>
 *   <li>compresses the payload and appends the coding to {@code Content-Encoding} after any caller-provided codings
 *       (RFC 9110 section 8.4);</li>
 *   <li>sets {@code Content-Length} to the compressed length of a buffered payload, and removes it for a compressed
 *       stream, whose length is unknown until sent (the transport then frames it with chunked transfer
 *       coding).</li>
 * </ul>
 * A caller-supplied payload checksum header would not match the compressed payload, so it is a conflict that blocks
 * transmission. The feature is stateless and thread-safe.
 */
public final class RequestCompressionFeature {
    /** The one compression coding this runtime supports. */
    public static final String GZIP = "gzip";

    private static final TraitKey<RequestCompressionTrait> REQUEST_COMPRESSION =
        TraitKey.get(RequestCompressionTrait.class);

    private final ShapeId operation;
    private final String encoding;
    private final boolean streamingInput;

    private RequestCompressionFeature(ShapeId operation, String encoding, boolean streamingInput) {
        this.operation = operation;
        this.encoding = encoding;
        this.streamingInput = streamingInput;
    }

    /**
     * The compression plan of an operation, or empty if the operation has no {@code requestCompression} trait, lists
     * no supported encoding, or has event stream input.
     *
     * @throws ModelConflictException if the input's streaming member also has {@code @requiresLength}
     */
    public static Optional<RequestCompressionFeature> forOperation(Schema operationSchema, Schema inputSchema) {
        Objects.requireNonNull(operationSchema, "operationSchema");
        Objects.requireNonNull(inputSchema, "inputSchema");
        RequestCompressionTrait trait = operationSchema.getTrait(REQUEST_COMPRESSION);
        if (trait == null || FeatureSupport.hasEventStream(inputSchema)) {
            return Optional.empty();
        }
        String selected = null;
        for (String candidate : trait.getEncodings()) {
            if (GZIP.equals(candidate.toLowerCase(Locale.ROOT))) {
                selected = GZIP;
                break;
            }
        }
        if (selected == null) {
            return Optional.empty();
        }
        Optional<Schema> streaming = FeatureSupport.streamingBlobMember(inputSchema);
        if (streaming.isPresent() && FeatureSupport.requiresLength(streaming.get())) {
            throw ModelConflictException.create(operationSchema.id(), "requestCompression",
                                                "the streaming member " + streaming.get().memberName()
                                                + " has @requiresLength, which request compression forbids");
        }
        return Optional.of(new RequestCompressionFeature(operationSchema.id(), selected, streaming.isPresent()));
    }

    /** Absolute shape ID of the operation. */
    public ShapeId operation() {
        return operation;
    }

    /** The selected content coding. */
    public String encoding() {
        return encoding;
    }

    /** True if {@link #apply} would compress {@code request} under {@code settings}. */
    public boolean compresses(SdkHttpRequest request, CompressionSettings settings) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(settings, "settings");
        HttpBody body = request.body();
        if (!settings.enabled() || body.kind() == HttpBody.Kind.EMPTY) {
            return false;
        }
        if (body.kind() == HttpBody.Kind.STREAMING && (streamingInput || body.contentLength().isEmpty())) {
            return true;
        }
        return body.contentLength().orElse(0) >= settings.minCompressionSizeBytes();
    }

    /**
     * {@code request} with its payload compressed and the coding appended to {@code Content-Encoding}, or
     * {@code request} itself if it is not compressed.
     *
     * @throws ModelConflictException if the request already carries a payload checksum header
     */
    public SdkHttpRequest apply(SdkHttpRequest request, CompressionSettings settings) {
        if (!compresses(request, settings)) {
            return request;
        }
        Optional<String> integrity = FeatureSupport.presentIntegrityHeader(request.headers());
        if (integrity.isPresent()) {
            throw ModelConflictException.create(operation, integrity.get(),
                                                "the checksum it carries would not match the payload after "
                                                + "request compression (" + encoding + "); disable request "
                                                + "compression or omit the checksum header");
        }
        HttpBody body = request.body();
        SdkHttpRequest.Builder builder = request.toBuilder();
        HttpBody compressed;
        if (body.kind() == HttpBody.Kind.STREAMING) {
            StreamingBody source = body.streamingBody();
            compressed = HttpBody.streaming(TransformedBodies.transform(source, -1, GzipTransform::new));
            FeatureSupport.contentLength(builder, -1);
        } else {
            compressed = HttpBody.fromBytes(GzipTransform.compress(body.asByteArray()));
            FeatureSupport.contentLength(builder, compressed.contentLength().orElse(0));
        }
        FeatureSupport.appendContentEncoding(builder, request.headers(), encoding);
        return builder.body(compressed).build();
    }

    @Override
    public String toString() {
        return "RequestCompressionFeature[" + operation + ", " + encoding + "]";
    }
}
