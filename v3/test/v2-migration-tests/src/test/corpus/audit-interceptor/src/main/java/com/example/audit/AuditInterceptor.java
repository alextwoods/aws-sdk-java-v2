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

package com.example.audit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpRequest;

/** Tags every request with an audit header and records the name of every completed operation. */
public final class AuditInterceptor implements ExecutionInterceptor {
    private final List<String> operations = new CopyOnWriteArrayList<>();

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        return context.httpRequest().toBuilder().putHeader("x-audit", "corpus").build();
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        operations.add(executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME));
    }

    /** The completed operations, in completion order. */
    public List<String> operations() {
        return List.copyOf(operations);
    }
}
