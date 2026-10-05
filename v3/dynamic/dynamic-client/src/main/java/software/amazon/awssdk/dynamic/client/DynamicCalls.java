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

package software.amazon.awssdk.dynamic.client;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.dynamic.model.DynamicPagination;
import software.amazon.awssdk.dynamic.model.DynamicService;
import software.amazon.awssdk.pipeline.AwsCallEngine;
import software.amazon.awssdk.pipeline.PipelinePlan;
import software.amazon.awssdk.pipeline.client.AwsClientParts;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.StructDocument;

/**
 * What the sync and async dynamic clients, paginators and waiters share: the service binding and the client's
 * {@link AwsClientParts} (runtime and engine), and the one pre-transport preparation of a call. Preparing a call
 * resolves the operation by exact name, checks the call's streaming arguments against the operation's streaming blob
 * members, validates and converts the input, and takes the operation's plan from the client's runtime, which builds
 * it once per client and operation. The call itself is then one direct engine call, as in a generated client; a
 * paginator page or a waiter poll is one such call.
 *
 * <p>Thread safe; holds no per-call state.
 */
final class DynamicCalls implements AutoCloseable {
    private final DynamicServiceBinding binding;
    private final AwsClientParts parts;

    DynamicCalls(DynamicServiceBinding binding, AwsClientParts parts) {
        this.binding = Objects.requireNonNull(binding, "binding");
        this.parts = Objects.requireNonNull(parts, "parts");
    }

    /**
     * A call of {@code operationName} with {@code input}, ready for the engine; nothing has been sent.
     *
     * @param plan the operation's plan in this client's runtime
     * @param input the validated input, as a schema-guided document of the exact input schema
     * @param operation the operation's binding
     */
    record Call(PipelinePlan<StructDocument, StructDocument> plan, StructDocument input,
                DynamicOperationBinding operation) {
    }

    /**
     * Prepares one call without a request body or response transformer.
     *
     * @see #prepare(String, Document, boolean, boolean)
     */
    Call prepare(String operationName, Document input) {
        return prepare(operationName, input, false, false);
    }

    /**
     * Prepares one call.
     *
     * @param requestBody whether the call supplies a streaming blob request body
     * @param responseTransformer whether the call supplies a streaming blob response transformer
     * @throws software.amazon.awssdk.dynamic.model.DynamicModelException if the service has no operation named
     *         {@code operationName}
     * @throws DynamicInvocationException if the streaming arguments do not match the operation's streaming blob
     *         members or the input does not conform
     * @throws software.amazon.awssdk.core.exception.ModelConflictException if the operation's modeled features
     *         conflict, so it cannot be planned
     */
    Call prepare(String operationName, Document input, boolean requestBody, boolean responseTransformer) {
        DynamicOperationBinding operation = binding.operation(operationName);
        operation.requireCall(requestBody, responseTransformer);
        StructDocument request = operation.input(input);
        return new Call(parts.runtime().plan(operation.pipeline()), request, operation);
    }

    /**
     * Prepares the first page call of a paginated operation.
     *
     * @throws software.amazon.awssdk.dynamic.model.DynamicModelException if the service has no such operation or it
     *         is not paginated
     * @throws DynamicInvocationException as for {@link #prepare(String, Document)}
     */
    DynamicPaginators.Paged paginate(String operationName, Document input) {
        DynamicPagination pagination = binding.service().pagination(operationName);
        return new DynamicPaginators.Paged(pagination, prepare(operationName, input));
    }

    /** The compiled waiter named exactly {@code waiterName}; see {@link DynamicServiceBinding#waiter}. */
    DynamicWaiters.Compiled waiter(String waiterName) {
        return binding.waiter(waiterName);
    }

    /** {@code event} as an event of {@code operationName}'s input event stream; see DynamicOperationBinding. */
    StructDocument inputEvent(String operationName, Document event) {
        return binding.operation(operationName).inputEvent(event);
    }

    DynamicService service() {
        return binding.service();
    }

    AwsCallEngine engine() {
        return parts.engine();
    }

    SdkClientConfiguration configuration() {
        return parts.configuration();
    }

    /** The scheduler of the client's non-blocking retry delays; present for every async client. */
    Optional<RetryScheduler> retryScheduler() {
        return parts.runtime().retryScheduler();
    }

    @Override
    public void close() {
        parts.close();
    }
}
