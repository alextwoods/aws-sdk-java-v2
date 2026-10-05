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

import java.util.Objects;
import software.amazon.smithy.waiters.Waiter;

/**
 * One modeled waiter of a runtime service (Requirement 14.15): a {@code smithy.waiters#waitable} waiter of one of its
 * operations, with its acceptors in model order and its delays exactly as {@code aws-codegen} reads them to generate
 * the service's waiters.
 *
 * @param name the exact modeled waiter name, unique within the service
 * @param operationName the exact modeled name of the polled operation
 * @param waiter the modeled waiter: acceptors in model order, {@code minDelay}, {@code maxDelay}
 */
public record DynamicWaiterDefinition(String name, String operationName, Waiter waiter) {

    public DynamicWaiterDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(waiter, "waiter");
    }
}
