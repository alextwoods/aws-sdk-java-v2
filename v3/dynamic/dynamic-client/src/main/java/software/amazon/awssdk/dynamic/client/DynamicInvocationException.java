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

import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A dynamic call was rejected before anything was sent (Requirements 14.11, 14.12, 14.15, 14.19): its input does not
 * conform to the operation's input schema, the call's streaming arguments do not match the operation's streaming
 * members, or the waiter it runs cannot be evaluated.
 *
 * <p>The error names the selected service, the operation and, for {@link Reason#INVALID_INPUT}, every invalid
 * element of the input with its path ({@link #violations()}). No interceptor, retry token or transmission was involved,
 * so it is never retryable. An unknown service or operation name is a
 * {@link software.amazon.awssdk.dynamic.model.DynamicModelException} instead.
 */
public final class DynamicInvocationException extends SdkClientException {
    private static final long serialVersionUID = 1L;
    /** How many violations the message lists; {@link #violations()} has all of them. */
    private static final int LISTED_VIOLATIONS = 10;

    /** Why the call was rejected. */
    public enum Reason {
        /** The input document does not conform to the operation's input schema. */
        INVALID_INPUT,
        /**
         * The operation has a streaming blob input or output member, which a finite-document call cannot carry: it
         * needs the request-body or response-transformer overload. Likewise an async call of an operation with an
         * output event stream needs the event-stream-response-handler overload. (Other event stream members are
         * carried by documents.)
         */
        REQUIRES_STREAMING_CALL,
        /**
         * The call supplied a request body or response transformer, but the operation has no streaming blob input
         * or output member to bind it to.
         */
        NO_STREAMING_MEMBER,
        /**
         * The modeled waiter has an acceptor path outside the JMESPath subset waiters support (the subset
         * {@code aws-codegen} generates waiters for).
         */
        UNSUPPORTED_WAITER
    }

    /**
     * One invalid element of an input document.
     *
     * @param path the element's path from the input root, for example {@code input.items[2].name} or
     *        {@code input.tags["owner"]}
     * @param problem what is wrong with it
     */
    public record Violation(String path, String problem) {
        public Violation {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(problem, "problem");
        }

        @Override
        public String toString() {
            return path + ": " + problem;
        }
    }

    private final Reason reason;
    private final transient ShapeId service;
    private final transient ShapeId operation;
    private final String operationName;
    private final List<Violation> violations;

    private DynamicInvocationException(Reason reason, String message, ShapeId service, ShapeId operation,
                                       String operationName, List<Violation> violations, Throwable cause) {
        super(SdkClientException.builder().message(message).cause(cause));
        this.reason = reason;
        this.service = service;
        this.operation = operation;
        this.operationName = operationName;
        this.violations = List.copyOf(violations);
    }

    static DynamicInvocationException invalidInput(ShapeId service, ShapeId operation, String operationName,
                                                   ShapeId input, List<Violation> violations, Throwable cause) {
        StringBuilder message = new StringBuilder();
        message.append("Invalid input for operation '").append(operationName).append("' (").append(operation)
               .append(") of service ").append(service).append(": the document does not conform to ").append(input)
               .append(" (").append(violations.size()).append(violations.size() == 1 ? " violation)" : " violations)");
        for (int i = 0; i < Math.min(violations.size(), LISTED_VIOLATIONS); i++) {
            message.append("\n  ").append(violations.get(i));
        }
        if (violations.size() > LISTED_VIOLATIONS) {
            message.append("\n  ... and ").append(violations.size() - LISTED_VIOLATIONS).append(" more");
        }
        return new DynamicInvocationException(Reason.INVALID_INPUT, message.toString(), service, operation,
                                              operationName, violations, cause);
    }

    static DynamicInvocationException requiresStreamingCall(ShapeId service, ShapeId operation, String operationName,
                                                            String direction, String member) {
        return new DynamicInvocationException(
            Reason.REQUIRES_STREAMING_CALL,
            "Operation '" + operationName + "' (" + operation + ") of service " + service + " has the streaming "
            + direction + " member '" + member + "', which a finite-document call cannot carry; call it with a "
            + switch (direction) {
                case "input" -> "request body";
                case "output" -> "response transformer";
                default -> "event stream response handler";
            },
            service, operation, operationName, List.of(), null);
    }

    static DynamicInvocationException noStreamingMember(ShapeId service, ShapeId operation, String operationName,
                                                        String direction, String argument) {
        return new DynamicInvocationException(
            Reason.NO_STREAMING_MEMBER,
            "Operation '" + operationName + "' (" + operation + ") of service " + service + " has no streaming blob "
            + direction + " member to bind the call's " + argument + " to",
            service, operation, operationName, List.of(), null);
    }

    static DynamicInvocationException unsupportedWaiter(ShapeId service, ShapeId operation, String operationName,
                                                        String waiter, String detail, Throwable cause) {
        return new DynamicInvocationException(
            Reason.UNSUPPORTED_WAITER,
            "Waiter '" + waiter + "' of operation '" + operationName + "' (" + operation + ") of service " + service
            + " cannot be run: " + detail,
            service, operation, operationName, List.of(), cause);
    }

    /** Why the call was rejected. */
    public Reason reason() {
        return reason;
    }

    /** Absolute shape ID of the selected service. */
    public ShapeId service() {
        return service;
    }

    /** Absolute shape ID of the operation. */
    public ShapeId operation() {
        return operation;
    }

    /** The exact modeled operation name the call used. */
    public String operationName() {
        return operationName;
    }

    /**
     * Every invalid element of the input, in traversal order, for {@link Reason#INVALID_INPUT}; empty otherwise.
     * Immutable.
     */
    public List<Violation> violations() {
        return violations;
    }

    /** Not retryable: the same call is rejected on every attempt. */
    @Override
    public boolean retryable() {
        return false;
    }
}
