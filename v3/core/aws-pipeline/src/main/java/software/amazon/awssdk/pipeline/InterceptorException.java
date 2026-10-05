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

import java.util.Objects;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * An {@link ExecutionInterceptor} hook failed (Requirements 11.13, 11.14): it threw, and the thrown exception is the
 * {@linkplain #getCause() cause} by identity, or it returned a replacement incompatible with the hook, and there is no
 * cause. {@link #interceptorClass()} and {@link #hook()} (the hook method's name, for example
 * {@code "modifyBeforeSigning"}) identify the source; {@link #operation()} is the call's operation.
 *
 * <p>Not retryable by itself: the retry strategy classifies it like any other client failure.
 */
public final class InterceptorException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private final ShapeId operation;
    private final transient Class<?> interceptorClass;
    private final String hook;

    private InterceptorException(BuilderImpl builder) {
        super(builder);
        this.operation = builder.operation;
        this.interceptorClass = builder.interceptorClass;
        this.hook = builder.hook;
    }

    /** The failure of {@code interceptor}'s {@code hook}, which threw {@code thrown}. */
    static InterceptorException thrown(ShapeId operation, ExecutionInterceptor interceptor, String hook,
                                       Throwable thrown) {
        Objects.requireNonNull(thrown, "thrown");
        String message = thrown.getMessage();
        BuilderImpl builder = new BuilderImpl(operation, interceptor.getClass(), hook);
        builder.message(describe(operation, interceptor.getClass(), hook,
                                 "threw " + thrown.getClass().getName() + (message == null ? "" : ": " + message)))
               .cause(thrown);
        return builder.build();
    }

    /** The failure of {@code interceptor}'s {@code hook}, which returned a replacement {@code detail} describes. */
    static InterceptorException incompatible(ShapeId operation, ExecutionInterceptor interceptor, String hook,
                                             String detail) {
        BuilderImpl builder = new BuilderImpl(operation, interceptor.getClass(), hook);
        builder.message(describe(operation, interceptor.getClass(), hook, "returned an incompatible value: " + detail));
        return builder.build();
    }

    /** The call's operation. */
    public ShapeId operation() {
        return operation;
    }

    /** The class of the interceptor whose hook failed; {@code null} only after deserialization. */
    public Class<?> interceptorClass() {
        return interceptorClass;
    }

    /** The name of the hook method that failed, for example {@code "modifyBeforeSigning"}. */
    public String hook() {
        return hook;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    private static String describe(ShapeId operation, Class<?> interceptorClass, String hook, String detail) {
        return "Interceptor " + interceptorClass.getName() + " failed in " + hook + " of " + operation + ": "
               + detail;
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl {
        private final ShapeId operation;
        private final Class<?> interceptorClass;
        private final String hook;

        private BuilderImpl(ShapeId operation, Class<?> interceptorClass, String hook) {
            this.operation = Objects.requireNonNull(operation, "operation");
            this.interceptorClass = Objects.requireNonNull(interceptorClass, "interceptorClass");
            this.hook = Objects.requireNonNull(hook, "hook");
        }

        private BuilderImpl(InterceptorException ex) {
            super(ex);
            this.operation = ex.operation;
            this.interceptorClass = ex.interceptorClass;
            this.hook = ex.hook;
        }

        @Override
        public InterceptorException build() {
            return new InterceptorException(this);
        }
    }
}
