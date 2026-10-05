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

package software.amazon.awssdk.dynamic.model;

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A runtime model, service selection, operation name, paginated operation or waiter name is invalid
 * (Requirements 14.4, 14.5, 14.7, 14.15, 14.19).
 *
 * <p>{@link #reason()} identifies the invalid element. The diagnostics name it exactly: the selected or requested
 * service shape ID, the unknown operation or waiter name, and every available service shape ID, operation name,
 * paginated operation name or waiter name in lexical order. The error is raised before any request is built, so it
 * is never retryable.
 */
public final class DynamicModelException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    /** The invalid element. */
    public enum Reason {
        /** The model is not a valid Smithy JSON AST, or assembling it produced error or danger events. */
        INVALID_MODEL,
        /** The model contains no service shape. */
        NO_SERVICE,
        /** The model contains several services and no service shape ID was supplied. */
        AMBIGUOUS_SERVICE,
        /** The supplied service shape ID is not a service of the model. */
        UNKNOWN_SERVICE,
        /** The operation name is not an operation of the selected service. */
        UNKNOWN_OPERATION,
        /** The operation is not paginated: it has no {@code smithy.api#paginated} input and output token. */
        NOT_PAGINATED,
        /** The waiter name is not a {@code smithy.waiters#waitable} waiter of the selected service. */
        UNKNOWN_WAITER
    }

    private final Reason reason;
    private final transient ShapeId service;
    private final String operationName;
    private final String waiterName;
    private final List<String> available;

    private DynamicModelException(Reason reason, String message, ShapeId service, String operationName,
                                  List<String> available, Throwable cause) {
        this(reason, message, service, operationName, null, available, cause);
    }

    private DynamicModelException(Reason reason, String message, ShapeId service, String operationName,
                                  String waiterName, List<String> available, Throwable cause) {
        super(SdkClientException.builder().message(message).cause(cause));
        this.reason = reason;
        this.service = service;
        this.operationName = operationName;
        this.waiterName = waiterName;
        this.available = List.copyOf(available);
    }

    static DynamicModelException invalidModel(String source, String detail, Throwable cause) {
        return new DynamicModelException(Reason.INVALID_MODEL, "Invalid runtime model " + source + ": " + detail,
                                         null, null, List.of(), cause);
    }

    static DynamicModelException noService() {
        return new DynamicModelException(Reason.NO_SERVICE, "The runtime model contains no service shape", null,
                                         null, List.of(), null);
    }

    static DynamicModelException ambiguousService(List<String> services) {
        return new DynamicModelException(Reason.AMBIGUOUS_SERVICE,
                                         "The runtime model contains " + services.size() + " services; select one "
                                         + "by shape ID: " + String.join(", ", services),
                                         null, null, services, null);
    }

    static DynamicModelException unknownService(ShapeId requested, List<String> services) {
        return new DynamicModelException(Reason.UNKNOWN_SERVICE,
                                         "Service " + requested + " is not a service of the runtime model; "
                                         + "available services: " + list(services),
                                         requested, null, services, null);
    }

    static DynamicModelException unknownOperation(ShapeId service, String name, List<String> operations) {
        return new DynamicModelException(Reason.UNKNOWN_OPERATION,
                                         "Service " + service + " has no operation named '" + name + "'; available "
                                         + "operations: " + list(operations),
                                         service, name, operations, null);
    }

    static DynamicModelException notPaginated(ShapeId service, String name, List<String> paginated) {
        return new DynamicModelException(Reason.NOT_PAGINATED,
                                         "Operation '" + name + "' of service " + service + " is not paginated; "
                                         + "paginated operations: " + list(paginated),
                                         service, name, paginated, null);
    }

    static DynamicModelException unknownWaiter(ShapeId service, String name, List<String> waiters) {
        return new DynamicModelException(Reason.UNKNOWN_WAITER,
                                         "Service " + service + " has no waiter named '" + name + "'; available "
                                         + "waiters: " + list(waiters),
                                         service, null, name, waiters, null);
    }

    /** The invalid element. */
    public Reason reason() {
        return reason;
    }

    /**
     * The service the error concerns: the requested service for {@link Reason#UNKNOWN_SERVICE}, the selected service
     * for {@link Reason#UNKNOWN_OPERATION}, {@link Reason#NOT_PAGINATED} and {@link Reason#UNKNOWN_WAITER}, empty
     * otherwise.
     */
    public Optional<ShapeId> service() {
        return Optional.ofNullable(service);
    }

    /**
     * The unknown operation name, for {@link Reason#UNKNOWN_OPERATION}, or the operation that is not paginated, for
     * {@link Reason#NOT_PAGINATED}.
     */
    public Optional<String> operationName() {
        return Optional.ofNullable(operationName);
    }

    /** The unknown waiter name, for {@link Reason#UNKNOWN_WAITER}. */
    public Optional<String> waiterName() {
        return Optional.ofNullable(waiterName);
    }

    /**
     * The available choices in lexical order: every service shape ID of the model for
     * {@link Reason#AMBIGUOUS_SERVICE} and {@link Reason#UNKNOWN_SERVICE}, every operation name of the selected service
     * for {@link Reason#UNKNOWN_OPERATION}, every paginated operation name for {@link Reason#NOT_PAGINATED}, every
     * waiter name for {@link Reason#UNKNOWN_WAITER}, empty otherwise.
     */
    public List<String> available() {
        return available;
    }

    /** Not retryable: the model, selection or name is invalid on every attempt. */
    @Override
    public boolean retryable() {
        return false;
    }

    private static String list(List<String> values) {
        return values.isEmpty() ? "(none)" : String.join(", ", values);
    }
}
