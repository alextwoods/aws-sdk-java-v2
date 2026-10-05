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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.awssdk.checksums.ChecksumValidationResult.Status;
import software.amazon.awssdk.checksums.internal.ChecksumValidatingTransform;
import software.amazon.awssdk.checksums.internal.FeatureSupport;
import software.amazon.awssdk.checksums.internal.TransformedBodies;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.core.exception.ModelConflictException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The response checksum feature plugin of one operation (Requirements 13.7 and 13.18): the Smithy
 * {@code aws.protocols#httpChecksum} response behavior.
 *
 * <p>{@link #forOperation} builds the plan once per client and operation; it keeps the modeled
 * {@code responseAlgorithms} in modeled order, drops the ones this runtime does not validate (MD5, XXHash), and rejects
 * algorithm IDs the trait cannot define. Per call:
 * <ol>
 *   <li>before transmission, {@link #prepareRequest} implicitly sets the validation-mode member to {@code ENABLED}
 *       through its {@code @httpHeader} binding when the caller left it unset and
 *       {@link ResponseChecksumValidation#WHEN_SUPPORTED} applies;</li>
 *   <li>after the response is received and before it is deserialized, {@link #validate} picks exactly one checksum:
 *       the first modeled, supported algorithm whose header the response carries (headers of unmodeled algorithms
 *       are ignored, composite {@code <base64>-<parts>} values are skipped). A buffered payload is validated at once
 *       and a mismatch throws {@link ChecksumMismatchException}, so no modeled response is returned. The payload of a
 *       streaming output is validated as it is read, failing the final read instead of signalling end-of-stream.</li>
 * </ol>
 * Only success responses are validated. The feature is stateless and thread-safe.
 */
public final class ResponseChecksumFeature {
    /** The validation-mode member value that enables response validation. */
    public static final String ENABLED = "ENABLED";

    /** The attempt-scoped context slot the pipeline records each attempt's validation result in. */
    public static final ContextKey<ChecksumValidationResult> VALIDATION_RESULT =
        ContextKey.attempt("ResponseChecksumValidation", ChecksumValidationResult.class);

    private static final Pattern COMPOSITE = Pattern.compile("[A-Za-z0-9+/]+={0,2}-[0-9]+");

    private final ShapeId operation;
    private final Schema modeMember;
    private final String modeHeader;
    private final List<ChecksumAlgorithm> algorithms;
    private final boolean streamingOutput;

    private ResponseChecksumFeature(ShapeId operation, Schema modeMember, List<ChecksumAlgorithm> algorithms,
                                    boolean streamingOutput) {
        this.operation = operation;
        this.modeMember = modeMember;
        this.modeHeader = FeatureSupport.headerBinding(modeMember).orElse(null);
        this.algorithms = List.copyOf(algorithms);
        this.streamingOutput = streamingOutput;
    }

    /**
     * The response checksum plan of an operation, or empty if it has no response checksum behavior.
     *
     * @param operationSchema the operation schema
     * @param inputSchema the operation's input structure schema, which holds the validation-mode member
     * @param outputSchema the operation's output structure schema, which decides whether the payload streams
     * @param httpChecksum the operation's {@code httpChecksum} trait value, or {@code null} if it has none
     * @throws ModelConflictException if the validation-mode member does not exist or a response algorithm is not one
     *     the trait defines
     */
    public static Optional<ResponseChecksumFeature> forOperation(Schema operationSchema, Schema inputSchema,
                                                                 Schema outputSchema,
                                                                 HttpChecksumMetadata httpChecksum) {
        Objects.requireNonNull(operationSchema, "operationSchema");
        Objects.requireNonNull(inputSchema, "inputSchema");
        Objects.requireNonNull(outputSchema, "outputSchema");
        if (httpChecksum == null || httpChecksum.requestValidationModeMember().isEmpty()) {
            return Optional.empty();
        }
        ShapeId operation = operationSchema.id();
        Schema member = FeatureSupport.requireMember(operation, inputSchema,
                                                     httpChecksum.requestValidationModeMember().get(),
                                                     "httpChecksum.requestValidationModeMember");
        List<ChecksumAlgorithm> supported = new ArrayList<>();
        for (String id : httpChecksum.responseAlgorithms()) {
            ChecksumAlgorithm algorithm =
                ChecksumAlgorithm.fromAlgorithmId(id)
                                 .orElseThrow(() -> ModelConflictException.create(
                                     operation, "httpChecksum.responseAlgorithms",
                                     "'" + id + "' is not a checksum algorithm of the httpChecksum trait"));
            if (algorithm.supportsFlexibleChecksums()) {
                supported.add(algorithm);
            }
        }
        boolean streaming = FeatureSupport.streamingBlobMember(outputSchema).isPresent();
        return Optional.of(new ResponseChecksumFeature(operation, member, supported, streaming));
    }

    /** Absolute shape ID of the operation. */
    public ShapeId operation() {
        return operation;
    }

    /** The modeled response algorithms this runtime validates, in the order they are looked for. */
    public List<ChecksumAlgorithm> validationAlgorithms() {
        return algorithms;
    }

    /** True if the caller set the validation-mode member to {@code ENABLED}, or left it unset under WHEN_SUPPORTED. */
    public boolean validationEnabled(SerializableStruct input, ResponseChecksumValidation validation) {
        Objects.requireNonNull(validation, "validation");
        Optional<String> mode = FeatureSupport.stringValue(input, modeMember);
        return mode.map(ENABLED::equals).orElse(validation == ResponseChecksumValidation.WHEN_SUPPORTED);
    }

    /**
     * {@code request} with the implicit {@code ENABLED} validation mode in the member's header, when the caller left
     * the member unset and {@code validation} is {@link ResponseChecksumValidation#WHEN_SUPPORTED}; otherwise
     * {@code request} itself. A member without an {@code @httpHeader} binding has no wire form to set.
     */
    public SdkHttpRequest prepareRequest(SerializableStruct input, SdkHttpRequest request,
                                         ResponseChecksumValidation validation) {
        Objects.requireNonNull(request, "request");
        if (validation != ResponseChecksumValidation.WHEN_SUPPORTED || modeHeader == null
            || FeatureSupport.stringValue(input, modeMember).isPresent() || request.headers().contains(modeHeader)) {
            return request;
        }
        return request.toBuilder().putHeader(modeHeader, ENABLED).build();
    }

    /**
     * Validates {@code response} when {@code enabled}.
     *
     * @return the response to deserialize (its body buffered, or wrapped to validate while streaming) and the result
     * @throws ChecksumMismatchException if a buffered payload does not match the selected checksum
     * @throws java.io.UncheckedIOException if buffering the payload fails
     */
    public ValidatedResponse validate(SdkHttpResponse response, boolean enabled) {
        Objects.requireNonNull(response, "response");
        if (!enabled) {
            return new ValidatedResponse(response, ChecksumValidationResult.of(Status.NOT_ENABLED));
        }
        if (!response.isSuccessful()) {
            return new ValidatedResponse(response, ChecksumValidationResult.of(Status.NOT_SUCCESS_RESPONSE));
        }
        boolean composite = false;
        for (ChecksumAlgorithm algorithm : algorithms) {
            Optional<String> header = response.headers().firstValue(algorithm.headerName()).map(String::trim);
            if (header.isEmpty()) {
                continue;
            }
            if (COMPOSITE.matcher(header.get()).matches()) {
                composite = true;
                continue;
            }
            return validate(response, algorithm, header.get());
        }
        return new ValidatedResponse(response, ChecksumValidationResult.of(composite ? Status.COMPOSITE_CHECKSUM_SKIPPED
                                                                                     : Status.CHECKSUM_NOT_FOUND));
    }

    private ValidatedResponse validate(SdkHttpResponse response, ChecksumAlgorithm algorithm, String expected) {
        HttpBody body = response.body();
        if (body.kind() == HttpBody.Kind.STREAMING && streamingOutput) {
            StreamingBody source = body.streamingBody();
            StreamingBody validating = TransformedBodies.transform(
                source, source.contentLength().orElse(-1), () -> new ChecksumValidatingTransform(algorithm, expected));
            return new ValidatedResponse(response.toBuilder().body(HttpBody.streaming(validating)).build(),
                                         ChecksumValidationResult.of(Status.VALIDATING_STREAM, algorithm));
        }
        HttpBody buffered = body.buffered();
        String computed = Base64.getEncoder().encodeToString(TransformedBodies.digest(buffered, algorithm));
        if (!computed.equals(expected)) {
            throw ChecksumMismatchException.create(algorithm, expected, computed);
        }
        SdkHttpResponse validated = buffered == body ? response : response.toBuilder().body(buffered).build();
        return new ValidatedResponse(validated, ChecksumValidationResult.of(Status.VALIDATED, algorithm));
    }

    @Override
    public String toString() {
        return "ResponseChecksumFeature[" + operation + ", validationModeMember=" + modeMember.memberName()
               + ", algorithms=" + algorithms + "]";
    }

    /**
     * A response after validation.
     *
     * @param response the response to deserialize
     * @param result whether and how the checksum was validated
     */
    public record ValidatedResponse(SdkHttpResponse response, ChecksumValidationResult result) {
        public ValidatedResponse {
            Objects.requireNonNull(response, "response");
            Objects.requireNonNull(result, "result");
        }
    }
}
