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

package software.amazon.awssdk.checksums;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.checksums.ResolvedRequestChecksum.Location;
import software.amazon.awssdk.checksums.internal.AwsChunkedTrailerTransform;
import software.amazon.awssdk.checksums.internal.FeatureSupport;
import software.amazon.awssdk.checksums.internal.TransformedBodies;
import software.amazon.awssdk.core.exception.ModelConflictException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;

/**
 * The request checksum feature plugin of one operation (Requirements 13.6 and 13.18): the Smithy
 * {@code aws.protocols#httpChecksum} request behavior and the {@code httpChecksumRequired} trait.
 *
 * <p>A plan is built once per client and operation by {@link #forOperation}, which reads the operation schema and
 * the trait metadata and rejects a model the runtime cannot honor. Per attempt, after serialization and request
 * compression and before signing, {@link #apply} adds the checksum at its modeled location:
 * <ul>
 *   <li><b>Algorithm.</b> The caller's value of {@code requestAlgorithmMember}; else, when the operation requires a
 *       checksum or {@link RequestChecksumCalculation#WHEN_SUPPORTED} applies, {@link #DEFAULT_ALGORITHM}, whose ID
 *       is then also sent in the member's {@code @httpHeader} as the implicit member value. An algorithm the model
 *       does not define, MD5 (as in V2) or one this runtime does not implement fails the call with
 *       {@link ModelConflictException} before transmission. An operation that requires a checksum but models no
 *       algorithm member gets the MD5 {@code Content-MD5} header.</li>
 *   <li><b>Explicit checksums win.</b> If the request already has a checksum header ({@code x-amz-checksum-*} or an
 *       {@code x-amz-trailer} declaration; {@code Content-MD5} for the MD5 case), nothing is computed.</li>
 *   <li><b>Scope and location.</b> The checksum covers exactly the HTTP payload bytes as they are when this feature
 *       runs. A normal payload, or a streaming payload under {@link PayloadSigning#SIGNED}, gets a header. A modeled
 *       streaming payload under {@link PayloadSigning#UNSIGNED} is sent {@code aws-chunked} with the checksum in an
 *       unsigned trailer ({@code STREAMING-UNSIGNED-PAYLOAD-TRAILER}); under {@link PayloadSigning#STREAMING_SIGNED}
 *       only {@code x-amz-trailer} is declared and the chunk signer frames the body and writes the trailer.</li>
 * </ul>
 * A header checksum of a streaming payload requires reading a replayable body once: {@link #apply} reads it through
 * its blocking view (off event loops only), while {@link #prepare} leaves that read to the caller, which the pipeline
 * does once for every digest the payload needs, without blocking in async calls. The feature is stateless and
 * thread-safe.
 */
public final class RequestChecksumFeature {
    /** The SDK default request checksum algorithm, as in V2. */
    public static final ChecksumAlgorithm DEFAULT_ALGORITHM = ChecksumAlgorithm.CRC32;

    private static final TraitKey<HttpChecksumRequiredTrait> HTTP_CHECKSUM_REQUIRED =
        TraitKey.get(HttpChecksumRequiredTrait.class);

    private final ShapeId operation;
    private final boolean required;
    private final Schema algorithmMember;
    private final String algorithmHeader;
    private final boolean streamingInput;
    private final int chunkSize;

    private RequestChecksumFeature(ShapeId operation, boolean required, Schema algorithmMember, boolean streamingInput,
                                   int chunkSize) {
        this.operation = operation;
        this.required = required;
        this.algorithmMember = algorithmMember;
        this.algorithmHeader = algorithmMember == null ? null
                                                       : FeatureSupport.headerBinding(algorithmMember).orElse(null);
        this.streamingInput = streamingInput;
        this.chunkSize = chunkSize;
    }

    /**
     * The request checksum plan of an operation, or empty if the operation has no request checksum behavior.
     *
     * @param operationSchema the operation schema, whose {@code httpChecksumRequired} trait is read
     * @param inputSchema the operation's input structure schema
     * @param httpChecksum the operation's {@code httpChecksum} trait value, or {@code null} if it has none
     * @throws ModelConflictException if {@code requestAlgorithmMember} names no input member
     */
    public static Optional<RequestChecksumFeature> forOperation(Schema operationSchema, Schema inputSchema,
                                                                HttpChecksumMetadata httpChecksum) {
        return forOperation(operationSchema, inputSchema, httpChecksum, AwsChunkedTrailerTransform.DEFAULT_CHUNK_SIZE);
    }

    /** {@link #forOperation(Schema, Schema, HttpChecksumMetadata)} with {@code chunkSize} data bytes per frame. */
    public static Optional<RequestChecksumFeature> forOperation(Schema operationSchema, Schema inputSchema,
                                                                HttpChecksumMetadata httpChecksum, int chunkSize) {
        Objects.requireNonNull(operationSchema, "operationSchema");
        Objects.requireNonNull(inputSchema, "inputSchema");
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("A chunk size is positive: " + chunkSize);
        }
        ShapeId operation = operationSchema.id();
        boolean legacyRequired = operationSchema.hasTrait(HTTP_CHECKSUM_REQUIRED);
        boolean modeledRequired = httpChecksum != null && httpChecksum.requestChecksumRequired();
        Optional<String> memberName = httpChecksum == null ? Optional.empty() : httpChecksum.requestAlgorithmMember();
        if (!legacyRequired && !modeledRequired && memberName.isEmpty()) {
            return Optional.empty();
        }
        Schema member = memberName.map(name -> FeatureSupport.requireMember(
            operation, inputSchema, name, "httpChecksum.requestAlgorithmMember")).orElse(null);
        boolean streaming = FeatureSupport.streamingBlobMember(inputSchema).isPresent();
        return Optional.of(new RequestChecksumFeature(operation, legacyRequired || modeledRequired, member, streaming,
                                                      chunkSize));
    }

    /** Absolute shape ID of the operation. */
    public ShapeId operation() {
        return operation;
    }

    /** True if the model requires a request checksum ({@code requestChecksumRequired} or httpChecksumRequired). */
    public boolean checksumRequired() {
        return required;
    }

    /** Name of the modeled algorithm member, if the model has one. */
    public Optional<String> algorithmMember() {
        return Optional.ofNullable(algorithmMember).map(Schema::memberName);
    }

    /**
     * The checksum this attempt sends, or empty if none is sent: the caller supplied one, or the checksum is optional
     * and {@code calculation} is {@link RequestChecksumCalculation#WHEN_REQUIRED}.
     *
     * @throws ModelConflictException if the caller selected an algorithm the model or runtime does not support
     */
    public Optional<ResolvedRequestChecksum> resolve(SerializableStruct input, SdkHttpRequest request,
                                                     RequestChecksumCalculation calculation, PayloadSigning signing) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(calculation, "calculation");
        Objects.requireNonNull(signing, "signing");
        if (algorithmMember == null) {
            // Required checksum without an algorithm member: MD5 in Content-MD5, always in a header.
            if (request.headers().contains(FeatureSupport.CONTENT_MD5)) {
                return Optional.empty();
            }
            return Optional.of(new ResolvedRequestChecksum(ChecksumAlgorithm.MD5, FeatureSupport.CONTENT_MD5,
                                                           Location.HEADER, true));
        }
        if (hasExplicitFlexibleChecksum(request)) {
            return Optional.empty();
        }
        Optional<String> selected = FeatureSupport.stringValue(input, algorithmMember);
        ChecksumAlgorithm algorithm;
        boolean defaulted;
        if (selected.isPresent()) {
            algorithm = requireSupported(selected.get());
            defaulted = false;
        } else if (required || calculation == RequestChecksumCalculation.WHEN_SUPPORTED) {
            algorithm = DEFAULT_ALGORITHM;
            defaulted = true;
        } else {
            return Optional.empty();
        }
        boolean streamingPayload = streamingInput && request.body().kind() == HttpBody.Kind.STREAMING;
        Location location = streamingPayload && signing != PayloadSigning.SIGNED ? Location.TRAILER : Location.HEADER;
        return Optional.of(new ResolvedRequestChecksum(algorithm, algorithm.headerName(), location, defaulted));
    }

    /**
     * {@code request} with this attempt's checksum added at its resolved location, or {@code request} itself if no
     * checksum is sent.
     *
     * @throws ModelConflictException if the caller selected an unsupported algorithm, or a trailer checksum is needed
     *     but the request already declares the {@code aws-chunked} content coding
     * @throws SdkClientException if a header checksum of a non-replayable streaming payload is needed
     */
    public SdkHttpRequest apply(SerializableStruct input, SdkHttpRequest request,
                                RequestChecksumCalculation calculation, PayloadSigning signing) {
        Prepared prepared = prepare(input, request, calculation, signing);
        if (prepared.pendingHeaderChecksum().isEmpty()) {
            return prepared.request();
        }
        ResolvedRequestChecksum checksum = prepared.pendingHeaderChecksum().get();
        return withHeaderChecksum(prepared.request(), checksum, digest(request.body(), checksum));
    }

    /**
     * {@link #apply}, except that the header checksum of a streaming payload is not computed: reading the body is
     * left to the caller, which can do it once for every digest the payload needs and without blocking. The result's
     * request has every other change of {@link #apply} (the defaulted algorithm member header, or the trailer
     * encoding); if its {@link Prepared#pendingHeaderChecksum()} is present, the caller computes that checksum over
     * every byte of the request's body and adds it with {@link #withHeaderChecksum}.
     *
     * @throws ModelConflictException if the caller selected an unsupported algorithm, or a trailer checksum is needed
     *     but the request already declares the {@code aws-chunked} content coding
     */
    public Prepared prepare(SerializableStruct input, SdkHttpRequest request, RequestChecksumCalculation calculation,
                            PayloadSigning signing) {
        Optional<ResolvedRequestChecksum> resolved = resolve(input, request, calculation, signing);
        if (resolved.isEmpty()) {
            return new Prepared(request, Optional.empty());
        }
        ResolvedRequestChecksum checksum = resolved.get();
        SdkHttpRequest.Builder builder = request.toBuilder();
        if (checksum.defaulted() && algorithmHeader != null && !request.headers().contains(algorithmHeader)) {
            builder.putHeader(algorithmHeader, checksum.algorithm().algorithmId());
        }
        if (checksum.location() == Location.HEADER) {
            if (request.body().kind() == HttpBody.Kind.STREAMING) {
                return new Prepared(builder.build(), resolved);
            }
            builder.putHeader(checksum.name(), Base64.getEncoder().encodeToString(digest(request.body(), checksum)));
            return new Prepared(builder.build(), Optional.empty());
        }
        return new Prepared(trailer(request, builder, checksum, signing), Optional.empty());
    }

    /** {@code request} with the header {@code checksum}, whose {@code digest} the caller computed over its payload. */
    public static SdkHttpRequest withHeaderChecksum(SdkHttpRequest request, ResolvedRequestChecksum checksum,
                                                    byte[] digest) {
        Objects.requireNonNull(checksum, "checksum");
        if (checksum.location() != Location.HEADER) {
            throw new IllegalArgumentException("The checksum " + checksum.name() + " is sent in a trailer");
        }
        return request.toBuilder().putHeader(checksum.name(), Base64.getEncoder().encodeToString(digest)).build();
    }

    /**
     * The outcome of {@link #prepare}: the request, and the header checksum of its streaming payload still to be
     * computed, if any.
     */
    public record Prepared(SdkHttpRequest request, Optional<ResolvedRequestChecksum> pendingHeaderChecksum) {
        public Prepared {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(pendingHeaderChecksum, "pendingHeaderChecksum");
        }
    }

    private SdkHttpRequest trailer(SdkHttpRequest request, SdkHttpRequest.Builder builder,
                                   ResolvedRequestChecksum checksum, PayloadSigning signing) {
        if (FeatureSupport.contentEncodings(request.headers()).contains(FeatureSupport.AWS_CHUNKED)) {
            throw ModelConflictException.create(operation, FeatureSupport.CONTENT_ENCODING,
                                                "the request already declares the aws-chunked content coding that "
                                                + "the trailer checksum " + checksum.name() + " requires");
        }
        builder.putHeader(FeatureSupport.X_AMZ_TRAILER, checksum.name());
        if (signing == PayloadSigning.STREAMING_SIGNED) {
            // The chunk signer frames the body and writes the declared trailer.
            return builder.build();
        }
        StreamingBody source = request.body().streamingBody();
        long decodedLength = source.contentLength().orElse(-1);
        ChecksumAlgorithm algorithm = checksum.algorithm();
        long encodedLength = decodedLength < 0 ? -1
                                               : AwsChunkedTrailerTransform.encodedLength(decodedLength, algorithm,
                                                                                          chunkSize);
        StreamingBody encoded = TransformedBodies.transform(source, encodedLength,
                                                            () -> new AwsChunkedTrailerTransform(algorithm, chunkSize));
        FeatureSupport.appendContentEncoding(builder, request.headers(), FeatureSupport.AWS_CHUNKED);
        builder.putHeader(FeatureSupport.X_AMZ_CONTENT_SHA256, FeatureSupport.STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        if (decodedLength >= 0) {
            builder.putHeader(FeatureSupport.X_AMZ_DECODED_CONTENT_LENGTH, Long.toString(decodedLength));
        }
        FeatureSupport.contentLength(builder, encodedLength);
        return builder.body(HttpBody.streaming(encoded)).build();
    }

    private boolean hasExplicitFlexibleChecksum(SdkHttpRequest request) {
        return FeatureSupport.presentIntegrityHeader(request.headers())
                             .filter(name -> !name.equalsIgnoreCase(FeatureSupport.CONTENT_MD5))
                             .isPresent();
    }

    private ChecksumAlgorithm requireSupported(String algorithmId) {
        String setting = algorithmMember.memberName();
        ChecksumAlgorithm algorithm =
            ChecksumAlgorithm.fromAlgorithmId(algorithmId)
                             .orElseThrow(() -> ModelConflictException.create(
                                 operation, setting, "'" + algorithmId + "' is not a checksum algorithm of the "
                                                     + "httpChecksum trait"));
        if (algorithm == ChecksumAlgorithm.MD5) {
            throw ModelConflictException.create(operation, setting, "MD5 is not supported for flexible checksums; "
                                                                    + "select another algorithm or supply a "
                                                                    + "precomputed checksum header");
        }
        if (!algorithm.supportsFlexibleChecksums()) {
            throw ModelConflictException.create(operation, setting, "the " + algorithm.algorithmId() + " checksum "
                                                                    + "algorithm is not supported by this SDK "
                                                                    + "runtime; supply a precomputed "
                                                                    + algorithm.headerName() + " header");
        }
        return algorithm;
    }

    private byte[] digest(HttpBody body, ResolvedRequestChecksum checksum) {
        if (body.kind() == HttpBody.Kind.STREAMING && !body.streamingBody().isReplayable()) {
            throw SdkClientException.create("Operation " + operation + " needs the " + checksum.name() + " header "
                                            + "checksum of its streaming payload before transmission, but the "
                                            + "payload is not replayable and would be consumed; use a replayable "
                                            + "body or supply the checksum header");
        }
        return TransformedBodies.digest(body, checksum.algorithm());
    }

    @Override
    public String toString() {
        return "RequestChecksumFeature[" + operation + ", required=" + required + ", algorithmMember="
               + algorithmMember().orElse(null) + "]";
    }
}
