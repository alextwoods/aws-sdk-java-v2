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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterMatchers;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.dynamic.model.DynamicAwsOperation;
import software.amazon.awssdk.dynamic.model.DynamicWaiterDefinition;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.waiters.Acceptor;
import software.amazon.smithy.waiters.Matcher;
import software.amazon.smithy.waiters.PathMatcher;

/**
 * Modeled waiters compiled to the runtime waiter generated waiters run on (Requirement 14.15): each
 * {@code smithy.waiters#waitable} waiter becomes sdk-core {@link WaiterAcceptor}s in model order and the modeled
 * {@code minDelay}/{@code maxDelay}, for a sdk-core {@link Waiter} per wait, exactly as {@code aws-codegen}'s
 * generated {@code Default<Service>Waiter}s build them:
 * <ul>
 *   <li>{@code output} and {@code inputOutput} path matchers evaluate the {@link DynamicPaths} subset and compare
 *       with the {@link WaiterMatchers} comparator of the same name;</li>
 *   <li>{@code success: true} matches every response and {@code success: false} every exception;</li>
 *   <li>{@code errorType} matches a {@link DynamicServiceException} of the modeled error it names (by absolute shape
 *       ID or shape name among the operation's and service's errors), or any {@link AwsServiceException} whose
 *       error code is the value.</li>
 * </ul>
 * The sync waiter polls on the calling thread and the async waiter never blocks, both through
 * {@link Waiter#run}/{@link Waiter#runAsync}; nothing here schedules or polls.
 */
final class DynamicWaiters {

    private DynamicWaiters() {
    }

    /**
     * One compiled waiter: immutable and thread safe.
     *
     * @param definition the modeled waiter
     * @param operation the polled operation
     * @param acceptors the acceptor factories, in model order
     */
    record Compiled(DynamicWaiterDefinition definition, DynamicAwsOperation operation,
                    List<AcceptorFactory> acceptors) {

        Compiled {
            acceptors = List.copyOf(acceptors);
        }

        /** The polled operation's exact modeled name. */
        String operationName() {
            return definition.operationName();
        }

        /** The runtime waiter of one wait for {@code input}, with the waiter-level {@code overrides}. */
        Waiter<Document> waiter(Document input, WaiterOverrideConfiguration overrides) {
            List<WaiterAcceptor<Document>> bound = new ArrayList<>(acceptors.size());
            for (AcceptorFactory acceptor : acceptors) {
                bound.add(acceptor.bind(input));
            }
            return Waiter.of(definition.name(), bound, Duration.ofSeconds(definition.waiter().getMinDelay()),
                             Duration.ofSeconds(definition.waiter().getMaxDelay()), overrides);
        }
    }

    /** The acceptor of one modeled acceptor, given the wait's input. */
    @FunctionalInterface
    interface AcceptorFactory {
        WaiterAcceptor<Document> bind(Document input);
    }

    /**
     * Compiles {@code definition} against {@code binding}'s service.
     *
     * @throws DynamicInvocationException ({@link DynamicInvocationException.Reason#UNSUPPORTED_WAITER}) if a matcher
     *         or path is outside the supported subset
     */
    static Compiled compile(DynamicServiceBinding binding, DynamicWaiterDefinition definition) {
        DynamicAwsOperation operation = binding.service().operation(definition.operationName());
        List<AcceptorFactory> acceptors = new ArrayList<>();
        try {
            for (Acceptor acceptor : definition.waiter().getAcceptors()) {
                acceptors.add(acceptor(operation, acceptor));
            }
        } catch (DynamicPaths.UnsupportedPathException e) {
            throw DynamicInvocationException.unsupportedWaiter(binding.service().id(), operation.id(),
                                                               operation.name(), definition.name(), e.getMessage(),
                                                               e);
        } catch (IllegalArgumentException e) {
            throw DynamicInvocationException.unsupportedWaiter(binding.service().id(), operation.id(),
                                                               operation.name(), definition.name(), e.getMessage(),
                                                               null);
        }
        return new Compiled(definition, operation, acceptors);
    }

    private static AcceptorFactory acceptor(DynamicAwsOperation operation, Acceptor acceptor) {
        WaiterState state = WaiterState.valueOf(acceptor.getState().toString().toUpperCase(Locale.ROOT));
        Matcher<?> matcher = acceptor.getMatcher();
        if (matcher instanceof Matcher.OutputMember member) {
            Predicate<Object> test = comparison(member.getValue());
            Function<Object, Object> path = DynamicPaths.compile(member.getValue().getPath());
            WaiterAcceptor<Document> bound = WaiterAcceptor.onResponse(state,
                                                                       output -> test.test(path.apply(output)));
            return input -> bound;
        }
        if (matcher instanceof Matcher.InputOutputMember member) {
            Predicate<Object> test = comparison(member.getValue());
            Function<Object, Object> path = DynamicPaths.compile(member.getValue().getPath());
            return input -> WaiterAcceptor.onResponse(
                state, output -> test.test(path.apply(DynamicPaths.inputOutput(input, output))));
        }
        if (matcher instanceof Matcher.SuccessMember member) {
            WaiterAcceptor<Document> bound = member.getValue()
                                             ? WaiterAcceptor.onResponse(state, output -> true)
                                             : WaiterAcceptor.onException(state, exception -> true);
            return input -> bound;
        }
        if (matcher instanceof Matcher.ErrorTypeMember member) {
            String errorType = member.getValue();
            ShapeId error = errorShape(operation, errorType);
            WaiterAcceptor<Document> bound = WaiterAcceptor.onException(state, exception -> {
                if (error != null && exception instanceof DynamicServiceException modeled
                    && error.equals(modeled.errorShapeId())) {
                    return true;
                }
                return exception instanceof AwsServiceException service && service.awsErrorDetails() != null
                       && errorType.equals(service.awsErrorDetails().errorCode());
            });
            return input -> bound;
        }
        throw new IllegalArgumentException("unsupported acceptor matcher " + matcher.getMemberName());
    }

    /** The test of a path matcher's value: its {@link WaiterMatchers} comparator against the expected value. */
    private static Predicate<Object> comparison(PathMatcher matcher) {
        String expected = matcher.getExpected();
        return switch (matcher.getComparator().toString()) {
            case "stringEquals" -> value -> WaiterMatchers.stringEquals(value, expected);
            case "booleanEquals" -> {
                boolean expectedBoolean = Boolean.parseBoolean(expected);
                yield value -> WaiterMatchers.booleanEquals(value, expectedBoolean);
            }
            case "allStringEquals" -> value -> WaiterMatchers.allStringEquals(value, expected);
            case "anyStringEquals" -> value -> WaiterMatchers.anyStringEquals(value, expected);
            default -> throw new IllegalArgumentException("unsupported comparator " + matcher.getComparator());
        };
    }

    /** The modeled error an {@code errorType} names (by absolute shape ID or shape name), or {@code null}. */
    private static ShapeId errorShape(DynamicAwsOperation operation, String errorType) {
        for (ShapeId error : operation.descriptor().errorIds()) {
            if (error.toString().equals(errorType) || error.getName().equals(errorType)) {
                return error;
            }
        }
        return null;
    }
}
