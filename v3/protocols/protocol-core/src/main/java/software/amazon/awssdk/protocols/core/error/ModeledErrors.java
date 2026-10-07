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

package software.amazon.awssdk.protocols.core.error;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.core.AwsQueryErrorCode;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.awssdk.utils.Redaction;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The per-client index of a service's modeled errors, shared by every protocol runtime: resolves a wire error code
 * to the error shape through the operation's errors and the service's {@link SchemaTypeRegistry}, and completes the
 * modeled or generic {@link AwsServiceException}.
 *
 * <p>Resolution order for a wire error type (already {@linkplain ErrorCodes#sanitizeErrorType sanitized}, or an
 * absolute shape ID):
 * <ol>
 *   <li>an absolute shape ID that the operation or registry declares, exactly;</li>
 *   <li>the operation's errors, by shape name;</li>
 *   <li>the registry's error shapes, by shape name, if the name is unique among them;</li>
 *   <li>for {@code awsQuery}/{@code ec2Query} and {@code awsQueryCompatible} services, the {@code awsQueryError}
 *       code ({@link #byQueryCode}).</li>
 * </ol>
 *
 * <p>Immutable and thread safe after construction.
 */
public final class ModeledErrors {
    private final SchemaTypeRegistry registry;
    private final String serviceName;
    private final Map<String, ShapeId> registryErrorsByName;
    private final Map<String, ShapeId> errorsByQueryCode;

    private ModeledErrors(ProtocolContext context) {
        this.registry = context.typeRegistry();
        this.serviceName = context.service().serviceId().getName();
        // Shape IDs only: registry partitions (and their schemas) stay unloaded until an error of theirs is seen.
        // Smithy requires shape names to be unique within a service closure; names that still collide (a composed
        // registry) are resolvable only by absolute shape ID.
        Map<String, ShapeId> byName = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (ShapeId id : registry.shapeIds()) {
            if (byName.putIfAbsent(id.getName(), id) != null) {
                ambiguous.add(id.getName());
            }
        }
        byName.keySet().removeAll(ambiguous);
        this.registryErrorsByName = Collections.unmodifiableMap(byName);
        Map<String, ShapeId> byCode = new HashMap<>();
        for (AwsQueryErrorCode code : context.service().awsQueryErrors().values()) {
            byCode.put(code.code(), code.error());
        }
        this.errorsByQueryCode = Collections.unmodifiableMap(byCode);
    }

    /** The index of the client described by {@code context}. */
    public static ModeledErrors of(ProtocolContext context) {
        return new ModeledErrors(Objects.requireNonNull(context, "context"));
    }

    /** The service's shape name, used as {@link AwsErrorDetails#serviceName()}. */
    public String serviceName() {
        return serviceName;
    }

    /** The name index of one operation's errors, to build once per operation plan. */
    public static Map<String, ShapeId> operationErrorsByName(OperationSchemas operation) {
        Map<String, ShapeId> out = new LinkedHashMap<>();
        for (Schema error : operation.errors()) {
            out.putIfAbsent(error.id().getName(), error.id());
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * The modeled error for wire error type {@code code} (see the class documentation, steps 1-3).
     *
     * @param operationErrors {@link #operationErrorsByName} of the operation
     */
    public Optional<ShapeId> byName(Map<String, ShapeId> operationErrors, String code) {
        if (code == null || code.isEmpty()) {
            return Optional.empty();
        }
        int hash = code.indexOf('#');
        if (hash > 0) {
            try {
                ShapeId id = ShapeId.from(code);
                if (operationErrors.containsValue(id) || registry.contains(id)) {
                    return Optional.of(id);
                }
            } catch (RuntimeException e) {
                // Not a shape ID; fall through to name lookup.
            }
            code = code.substring(hash + 1);
        }
        ShapeId id = operationErrors.get(code);
        if (id == null) {
            id = registryErrorsByName.get(code);
            if (id != null && !isError(id)) {
                id = null;
            }
        }
        return Optional.ofNullable(id);
    }

    private boolean isError(ShapeId id) {
        return registry.schema(id).map(schema -> schema.hasTrait(TraitKey.ERROR_TRAIT)).orElse(false);
    }

    /** The modeled error whose {@code awsQueryError} code is {@code code}. */
    public Optional<ShapeId> byQueryCode(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(errorsByQueryCode.get(code));
    }

    /**
     * A fresh builder of registered type {@code error}: a modeled error, or an event stream union.
     *
     * @throws IllegalStateException if the registry has no entry for it
     */
    public ShapeBuilder<?> newBuilder(ShapeId error) {
        if (!registry.contains(error)) {
            throw new IllegalStateException("The type registry has no entry for modeled error " + error);
        }
        return registry.createBuilder(error);
    }

    /**
     * Completes a deserialized modeled error builder with the response metadata and builds it. If the error's
     * {@code message} member is {@code @sensitive}, the exception message and {@link AwsErrorDetails#errorMessage()}
     * are redacted (Requirement 21.10).
     *
     * @throws IllegalStateException if the registered type is not an {@link AwsServiceException}
     */
    public AwsServiceException complete(ShapeBuilder<?> builder, ErrorResponse response) {
        if (!(builder instanceof AwsServiceException.Builder error)) {
            throw new IllegalStateException("The registered builder of " + builder.schema().id()
                                            + " does not build an AwsServiceException");
        }
        if (hasSensitiveMessage(builder.schema())) {
            // Requirement 21.10: a @sensitive message is neither the exception message nor the error details'
            // message (both are rendered by getMessage and toString); the raw response keeps the wire value.
            if (error.message() != null) {
                error.message(Redaction.REDACTED);
            }
            AwsErrorDetails details = response.details();
            response = new ErrorResponse(details.toBuilder().errorMessage(Redaction.secret(details.errorMessage()))
                                                .build(),
                                         response.requestId(), response.extendedRequestId(), response.statusCode());
        }
        response.applyTo(error);
        Object built = builder.errorCorrection().build();
        if (!(built instanceof AwsServiceException exception)) {
            throw new IllegalStateException("The registered type of " + builder.schema().id()
                                            + " is not an AwsServiceException");
        }
        return exception;
    }

    /**
     * Whether the error's message, the string member named {@code message} in any case (the member generated error
     * types hold as their exception message), is {@code @sensitive}.
     */
    private static boolean hasSensitiveMessage(Schema error) {
        for (Schema member : error.members()) {
            boolean sensitive = member.hasTrait(TraitKey.SENSITIVE_TRAIT)
                                || member.memberTarget().hasTrait(TraitKey.SENSITIVE_TRAIT);
            if (sensitive && member.memberName().equalsIgnoreCase("message")) {
                return true;
            }
        }
        return false;
    }

    /** The generic exception of an error response that identifies no modeled error. */
    public AwsServiceException generic(ErrorResponse response) {
        AwsServiceException.Builder builder = AwsServiceException.builder();
        response.applyTo(builder);
        builder.message(response.details().errorMessage());
        return builder.build();
    }

    /**
     * The response metadata of an error response.
     *
     * @param code the wire error code reported to callers (the {@code awsQueryError} code when the service is
     *        {@code awsQueryCompatible} and the response carries one); may be {@code null}
     * @param message the wire error message; may be {@code null}
     * @param response the HTTP response
     */
    public ErrorResponse errorResponse(String code, String message, SdkHttpResponse response) {
        AwsErrorDetails details = AwsErrorDetails.builder()
                                                 .errorCode(code)
                                                 .errorMessage(message)
                                                 .serviceName(serviceName)
                                                 .rawResponse(SdkBytes.fromByteArrayUnsafe(
                                                     response.body().kind() == HttpBody.Kind.STREAMING
                                                     ? new byte[0] : response.body().asByteArray()))
                                                 .sdkHttpResponse(response)
                                                 .build();
        return new ErrorResponse(details, ErrorCodes.requestId(response.headers()).orElse(null),
                                 ErrorCodes.extendedRequestId(response.headers()).orElse(null),
                                 response.statusCode());
    }

    /**
     * Error metadata applied to every exception built from an error response.
     *
     * @param details the AWS error details
     * @param requestId the request ID, or {@code null}
     * @param extendedRequestId the extended request ID, or {@code null}
     * @param statusCode the HTTP status code
     */
    public record ErrorResponse(AwsErrorDetails details, String requestId, String extendedRequestId,
                                int statusCode) {
        public ErrorResponse {
            Objects.requireNonNull(details, "details");
        }

        void applyTo(AwsServiceException.Builder builder) {
            builder.awsErrorDetails(details);
            builder.requestId(requestId);
            builder.extendedRequestId(extendedRequestId);
            builder.statusCode(statusCode);
        }
    }
}
