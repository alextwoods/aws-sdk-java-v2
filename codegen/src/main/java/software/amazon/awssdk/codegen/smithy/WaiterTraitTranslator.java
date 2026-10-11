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

package software.amazon.awssdk.codegen.smithy;

import com.fasterxml.jackson.jr.stree.JrsBoolean;
import com.fasterxml.jackson.jr.stree.JrsNumber;
import com.fasterxml.jackson.jr.stree.JrsString;
import com.fasterxml.jackson.jr.stree.JrsValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.model.service.Acceptor;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.waiters.Matcher;
import software.amazon.smithy.waiters.PathMatcher;
import software.amazon.smithy.waiters.WaitableTrait;
import software.amazon.smithy.waiters.Waiter;

/**
 * Builds the intermediate model's waiter view from {@code smithy.waiters#waitable}. Smithy counterpart to reading
 * {@code waiters-2.json}.
 *
 * <p>{@code minDelay} becomes the delay. Acceptor matchers map to the C2J matchers the waiter generators read:
 * {@code output} with {@code stringEquals}, {@code allStringEquals}, {@code anyStringEquals} or
 * {@code booleanEquals} to {@code path}, {@code pathAll}, {@code pathAny} and {@code path} with a boolean;
 * {@code errorType} to {@code error} with the code; {@code success: true} to {@code status} 200, and
 * {@code success: false} to {@code error} with {@code true}. A waiter with an {@code inputOutput} matcher is not
 * supported by the generators and is skipped.
 *
 * <p>Smithy waiters bound waiting by time, not attempts; the generated waiters need an attempt count, so every waiter
 * gets {@link #DEFAULT_MAX_ATTEMPTS}.
 */
public final class WaiterTraitTranslator {
    /**
     * The attempt count of a waiter generated from {@code smithy.waiters#waitable}, which has no attempt count.
     */
    public static final int DEFAULT_MAX_ATTEMPTS = 25;

    private static final Logger log = LoggerFactory.getLogger(WaiterTraitTranslator.class);

    private WaiterTraitTranslator() {
    }

    /**
     * The waiter definitions of the service's operations, keyed by waiter name: operations in shape ID order, and the
     * waiters of one operation in trait order.
     */
    public static Map<String, WaiterDefinition> waiters(Model model, ServiceShape service) {
        Map<String, WaiterDefinition> waiters = new LinkedHashMap<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            operation.getTrait(WaitableTrait.class).ifPresent(trait -> trait.getWaiters().forEach((name, waiter) -> {
                WaiterDefinition definition = definition(operation, name, waiter);
                if (definition != null) {
                    waiters.put(name, definition);
                }
            }));
        }
        return waiters;
    }

    private static WaiterDefinition definition(OperationShape operation, String name, Waiter waiter) {
        List<Acceptor> acceptors = new ArrayList<>();
        for (software.amazon.smithy.waiters.Acceptor smithyAcceptor : waiter.getAcceptors()) {
            Acceptor acceptor = acceptor(smithyAcceptor);
            if (acceptor == null) {
                log.warn("Waiter {} of {} has an acceptor that cannot be generated; the waiter is skipped", name,
                         operation.getId());
                return null;
            }
            acceptors.add(acceptor);
        }
        WaiterDefinition definition = new WaiterDefinition();
        definition.setOperation(operation.getId().getName());
        definition.setDelay(waiter.getMinDelay());
        definition.setMaxAttempts(DEFAULT_MAX_ATTEMPTS);
        definition.setAcceptors(acceptors);
        return definition;
    }

    private static Acceptor acceptor(software.amazon.smithy.waiters.Acceptor smithyAcceptor) {
        Matcher<?> matcher = smithyAcceptor.getMatcher();
        Acceptor acceptor = new Acceptor();
        acceptor.setState(smithyAcceptor.getState().toString());
        if (matcher instanceof Matcher.OutputMember) {
            PathMatcher path = ((Matcher.OutputMember) matcher).getValue();
            acceptor.setArgument(path.getPath());
            switch (path.getComparator()) {
                case STRING_EQUALS:
                    return path(acceptor, "path", new JrsString(path.getExpected()));
                case ALL_STRING_EQUALS:
                    return path(acceptor, "pathAll", new JrsString(path.getExpected()));
                case ANY_STRING_EQUALS:
                    return path(acceptor, "pathAny", new JrsString(path.getExpected()));
                case BOOLEAN_EQUALS:
                    return path(acceptor, "path", Boolean.parseBoolean(path.getExpected()) ? JrsBoolean.TRUE
                                                                                            : JrsBoolean.FALSE);
                default:
                    return null;
            }
        }
        if (matcher instanceof Matcher.ErrorTypeMember) {
            return path(acceptor, "error", new JrsString(((Matcher.ErrorTypeMember) matcher).getValue()));
        }
        if (matcher instanceof Matcher.SuccessMember) {
            return ((Matcher.SuccessMember) matcher).getValue()
                   ? path(acceptor, "status", new JrsNumber(200))
                   : path(acceptor, "error", JrsBoolean.TRUE);
        }
        return null;
    }

    private static Acceptor path(Acceptor acceptor, String matcher, JrsValue expected) {
        acceptor.setMatcher(matcher);
        acceptor.setExpected(expected);
        return acceptor;
    }
}
