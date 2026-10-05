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

package software.amazon.awssdk.pipeline;

import java.util.Optional;
import software.amazon.awssdk.auth.api.SelectedAuthScheme;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * The typed context an {@link ExecutionInterceptor} hook receives (Requirements 11.1, 11.2, 11.5; design
 * "Interceptors and plugins"): one immutable record per hook, created only when at least one interceptor implements
 * the hook, holding the exact values the pipeline produced for that point by identity.
 *
 * <p>Every context exposes the {@link AwsOperation} (and so its schemas), the call's read-only {@link CallOptions}, the
 * modeled request every attempt serializes, and the typed context of its scope. {@link Execution} contexts belong to
 * once-per-call hooks and expose the call's {@link ExecutionContext}, which survives every attempt (Requirement 11.6).
 * {@link Attempt} contexts belong to once-per-attempt hooks and expose the attempt's {@link AttemptContext}: its values
 * are visible only to later hooks of the same attempt (Requirement 11.7), and it reads the resolved endpoint, auth
 * scheme and identity of exactly this attempt.
 *
 * <p>The hook values are read-only: a modify hook changes the call by returning a replacement, never by mutating
 * them. Each interceptor of a modify hook receives a context holding the previous interceptor's replacement. The
 * typed contexts are the extension's only mutable state, written through their typed {@code put} methods.
 */
public sealed interface HookContext {
    /** The operation of the call. */
    AwsOperation<?, ?> operation();

    /** The call's options: its override deltas over the client's resolved configuration. Read-only. */
    CallOptions options();

    /** The modeled request every attempt of the call serializes. */
    SerializableStruct request();

    /** The call's execution-scoped context; it survives every attempt. */
    ExecutionContext execution();

    /** The operation's schema. */
    default Schema operationSchema() {
        return operation().schemas().operation();
    }

    /** The context of a hook that runs at most once per call. */
    sealed interface Execution extends HookContext {
    }

    /** The context of a hook that runs at most once per attempt. */
    sealed interface Attempt extends HookContext {
        /** The attempt's context; closed, and unreadable, once the attempt completes. */
        AttemptContext attempt();

        @Override
        default ExecutionContext execution() {
            return attempt().execution();
        }

        /** The one-based number of the attempt. */
        default int attemptNumber() {
            return attempt().attemptNumber();
        }

        /** The endpoint this attempt transmits to, once resolved; the exact value the transport receives. */
        default Optional<Endpoint> endpoint() {
            return Optional.ofNullable(attempt().get(PipelineKeys.ENDPOINT));
        }

        /** The auth scheme selected for this attempt, if the call is signed. */
        default Optional<SelectedAuthScheme<?>> authScheme() {
            return Optional.ofNullable(attempt().get(PipelineKeys.AUTH_SCHEME));
        }

        /** The identity this attempt signs with, if the call is signed. */
        default Optional<Identity> identity() {
            return Optional.ofNullable(attempt().get(PipelineKeys.IDENTITY));
        }
    }

    /** {@link ExecutionInterceptor#beforeExecution}: the call is validated; nothing has run. */
    record BeforeExecution(AwsOperation<?, ?> operation, CallOptions options, ExecutionContext execution,
                           SerializableStruct request) implements Execution {
    }

    /** {@link ExecutionInterceptor#modifyRequest}: {@code request} is the current modeled request. */
    record ModifyRequest(AwsOperation<?, ?> operation, CallOptions options, ExecutionContext execution,
                         SerializableStruct request) implements Execution {
    }

    /** {@link ExecutionInterceptor#beforeAttempt}: the attempt started; nothing of it is resolved yet. */
    record BeforeAttempt(AwsOperation<?, ?> operation, CallOptions options, AttemptContext attempt,
                         SerializableStruct request) implements Attempt {
    }

    /** {@link ExecutionInterceptor#afterSerialization}: {@code httpRequest} is the serialized request. */
    record AfterSerialization(AwsOperation<?, ?> operation, CallOptions options, AttemptContext attempt,
                              SerializableStruct request, SdkHttpRequest httpRequest) implements Attempt {
    }

    /**
     * {@link ExecutionInterceptor#modifyBeforeSigning}: {@code httpRequest} is the unsigned request after the request
     * features (compression, checksums); the returned request is signed.
     */
    record ModifyBeforeSigning(AwsOperation<?, ?> operation, CallOptions options, AttemptContext attempt,
                               SerializableStruct request, SdkHttpRequest httpRequest) implements Attempt {
    }

    /** {@link ExecutionInterceptor#modifyAfterSigning}: {@code signedRequest} is the signed request. */
    record ModifyAfterSigning(AwsOperation<?, ?> operation, CallOptions options, AttemptContext attempt,
                              SerializableStruct request, SignedRequest signedRequest) implements Attempt {
    }

    /** {@link ExecutionInterceptor#beforeTransmission}: {@code signedRequest} is exactly what is transmitted. */
    record BeforeTransmission(AwsOperation<?, ?> operation, CallOptions options, AttemptContext attempt,
                              SerializableStruct request, SignedRequest signedRequest) implements Attempt {
    }

    /**
     * {@link ExecutionInterceptor#afterTransmission}: the transmission ended with {@code httpResponse} (status,
     * headers and body as received) or, if the transport failed after transmission began, {@code failure}; exactly
     * one is present.
     */
    record AfterTransmission(AwsOperation<?, ?> operation, CallOptions options, AttemptContext attempt,
                             SerializableStruct request, SignedRequest signedRequest,
                             Optional<SdkHttpResponse> httpResponse, Optional<Throwable> failure)
        implements Attempt {
    }

    /** {@link ExecutionInterceptor#modifyHttpResponse}: {@code httpResponse} is the current response. */
    record ModifyHttpResponse(AwsOperation<?, ?> operation, CallOptions options, AttemptContext attempt,
                              SerializableStruct request, SdkHttpResponse httpResponse) implements Attempt {
    }

    /** {@link ExecutionInterceptor#modifyResponseBody}: {@code body} is the current body of {@code httpResponse}. */
    record ModifyResponseBody(AwsOperation<?, ?> operation, CallOptions options, AttemptContext attempt,
                              SerializableStruct request, SdkHttpResponse httpResponse, HttpBody body)
        implements Attempt {
    }

    /** {@link ExecutionInterceptor#beforeDeserialization}: {@code httpResponse} is exactly what is deserialized. */
    record BeforeDeserialization(AwsOperation<?, ?> operation, CallOptions options, AttemptContext attempt,
                                 SerializableStruct request, SdkHttpResponse httpResponse) implements Attempt {
    }

    /** {@link ExecutionInterceptor#modifyAttemptException}: {@code failure} is the attempt's current failure. */
    record ModifyAttemptException(AwsOperation<?, ?> operation, CallOptions options, AttemptContext attempt,
                                  SerializableStruct request, Throwable failure) implements Attempt {
    }

    /**
     * {@link ExecutionInterceptor#afterAttempt}: the attempt ended with the deserialized {@code response} or with
     * {@code failure} (the replacement of {@code modifyAttemptException}, before classification); exactly one is
     * present.
     */
    record AfterAttempt(AwsOperation<?, ?> operation, CallOptions options, AttemptContext attempt,
                        SerializableStruct request, Optional<SerializableStruct> response,
                        Optional<Throwable> failure) implements Attempt {
    }

    /** {@link ExecutionInterceptor#modifyResponse}: {@code response} is the current modeled output. */
    record ModifyResponse(AwsOperation<?, ?> operation, CallOptions options, ExecutionContext execution,
                          SerializableStruct request, SerializableStruct response) implements Execution {
    }

    /** {@link ExecutionInterceptor#modifyException}: {@code failure} is the call's mapped terminal failure. */
    record ModifyException(AwsOperation<?, ?> operation, CallOptions options, ExecutionContext execution,
                           SerializableStruct request, Throwable failure) implements Execution {
    }

    /**
     * {@link ExecutionInterceptor#afterExecution}: the call ends with the modeled {@code response} or the
     * {@code failure} it surfaces; exactly one is present.
     */
    record AfterExecution(AwsOperation<?, ?> operation, CallOptions options, ExecutionContext execution,
                          SerializableStruct request, Optional<SerializableStruct> response,
                          Optional<Throwable> failure) implements Execution {
    }
}
