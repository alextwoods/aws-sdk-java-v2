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

package software.amazon.awssdk.core.interceptor;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.interceptor.DefaultFailedExecutionContext;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A wrapper for a list of {@link ExecutionInterceptor}s that ensures the interceptors are executed in the correct order as it
 * is documented in the {@link ExecutionInterceptor} documentation.
 *
 * Interceptors are invoked in forward order up to {@link #beforeTransmission} and in reverse order after (and including)
 * {@link #afterTransmission}. This ensures the last interceptors to modify the request are the first interceptors to see the
 * response.
 *
 * <p>A chain is immutable and safe to share across concurrent calls. On construction it works out, once, which interceptors
 * override which hook, so that each hook only visits the interceptors that have something to do at that point: an interceptor
 * that only implements {@code modifyException} costs nothing on the request path, and a client with no interceptors
 * costs nothing at all. The detection is deliberately conservative: any declaration of a hook method anywhere in an
 * interceptor's class hierarchy (other than {@link ExecutionInterceptor} itself) counts as an override, and if the hierarchy
 * cannot be inspected the interceptor is invoked for every hook, exactly as before.
 */
@SdkProtectedApi
public class ExecutionInterceptorChain {
    private static final Logger LOG = Logger.loggerFor(ExecutionInterceptorChain.class);

    private static final ExecutionInterceptor[] NONE = new ExecutionInterceptor[0];

    private final List<ExecutionInterceptor> interceptors;

    private final ExecutionInterceptor[] beforeExecution;
    private final ExecutionInterceptor[] modifyRequest;
    private final ExecutionInterceptor[] beforeMarshalling;
    private final ExecutionInterceptor[] afterMarshalling;
    private final ExecutionInterceptor[] modifyHttpRequestAndHttpContent;
    private final ExecutionInterceptor[] beforeTransmission;
    private final ExecutionInterceptor[] afterTransmission;
    private final ExecutionInterceptor[] modifyHttpResponse;
    private final ExecutionInterceptor[] modifyAsyncHttpResponse;
    private final ExecutionInterceptor[] beforeUnmarshalling;
    private final ExecutionInterceptor[] afterUnmarshalling;
    private final ExecutionInterceptor[] modifyResponse;
    private final ExecutionInterceptor[] afterExecution;
    private final ExecutionInterceptor[] modifyException;
    private final ExecutionInterceptor[] onExecutionFailure;

    /**
     * Create a chain that will execute the provided interceptors in the order they are provided.
     */
    public ExecutionInterceptorChain(List<ExecutionInterceptor> interceptors) {
        Validate.paramNotNull(interceptors, "interceptors");
        this.interceptors = Collections.unmodifiableList(new ArrayList<>(interceptors));
        LOG.debug(() -> "Creating an interceptor chain that will apply interceptors in the following order: " + interceptors);

        List<Set<String>> overrides = new ArrayList<>(this.interceptors.size());
        for (ExecutionInterceptor interceptor : this.interceptors) {
            overrides.add(overriddenHooks(interceptor));
        }

        this.beforeExecution = select(overrides, "beforeExecution");
        this.modifyRequest = select(overrides, "modifyRequest");
        this.beforeMarshalling = select(overrides, "beforeMarshalling");
        this.afterMarshalling = select(overrides, "afterMarshalling");
        this.modifyHttpRequestAndHttpContent = select(overrides, "modifyHttpRequest", "modifyHttpContent",
                                                     "modifyAsyncHttpContent");
        this.beforeTransmission = select(overrides, "beforeTransmission");
        this.afterTransmission = select(overrides, "afterTransmission");
        this.modifyHttpResponse = select(overrides, "modifyHttpResponse", "modifyHttpResponseContent");
        this.modifyAsyncHttpResponse = select(overrides, "modifyAsyncHttpResponseContent");
        this.beforeUnmarshalling = select(overrides, "beforeUnmarshalling");
        this.afterUnmarshalling = select(overrides, "afterUnmarshalling");
        this.modifyResponse = select(overrides, "modifyResponse");
        this.afterExecution = select(overrides, "afterExecution");
        this.modifyException = select(overrides, "modifyException");
        this.onExecutionFailure = select(overrides, "onExecutionFailure");
    }

    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        ExecutionInterceptor[] targets = beforeExecution;
        for (int i = 0; i < targets.length; i++) {
            targets[i].beforeExecution(context, executionAttributes);
        }
    }

    public InterceptorContext modifyRequest(InterceptorContext context, ExecutionAttributes executionAttributes) {
        InterceptorContext result = context;
        ExecutionInterceptor[] targets = modifyRequest;
        for (int i = 0; i < targets.length; i++) {
            ExecutionInterceptor interceptor = targets[i];
            SdkRequest interceptorResult = interceptor.modifyRequest(result, executionAttributes);

            if (interceptorResult != result.request()) {
                validateInterceptorResult(result.request(), interceptorResult, interceptor, "modifyRequest");
                result = result.copy(b -> b.request(interceptorResult));
            }
        }
        return result;
    }

    public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
        ExecutionInterceptor[] targets = beforeMarshalling;
        for (int i = 0; i < targets.length; i++) {
            targets[i].beforeMarshalling(context, executionAttributes);
        }
    }

    public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
        ExecutionInterceptor[] targets = afterMarshalling;
        for (int i = 0; i < targets.length; i++) {
            targets[i].afterMarshalling(context, executionAttributes);
        }
    }

    public InterceptorContext modifyHttpRequestAndHttpContent(InterceptorContext context,
                                                              ExecutionAttributes executionAttributes) {
        InterceptorContext result = context;
        ExecutionInterceptor[] targets = modifyHttpRequestAndHttpContent;
        for (int i = 0; i < targets.length; i++) {
            ExecutionInterceptor interceptor = targets[i];
            AsyncRequestBody asyncRequestBody = interceptor.modifyAsyncHttpContent(result, executionAttributes).orElse(null);
            RequestBody requestBody = interceptor.modifyHttpContent(result, executionAttributes).orElse(null);
            SdkHttpRequest interceptorResult = interceptor.modifyHttpRequest(result, executionAttributes);

            if (asyncRequestBody != result.asyncRequestBody().orElse(null) ||
                requestBody != result.requestBody().orElse(null) ||
                interceptorResult != result.httpRequest()) {

                validateInterceptorResult(result.httpRequest(), interceptorResult, interceptor, "modifyHttpRequest");
                result = result.copy(r -> r.httpRequest(interceptorResult)
                                           .asyncRequestBody(asyncRequestBody)
                                           .requestBody(requestBody));
            }
        }
        return result;
    }

    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        ExecutionInterceptor[] targets = beforeTransmission;
        for (int i = 0; i < targets.length; i++) {
            targets[i].beforeTransmission(context, executionAttributes);
        }
    }

    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
        ExecutionInterceptor[] targets = afterTransmission;
        for (int i = targets.length - 1; i >= 0; i--) {
            targets[i].afterTransmission(context, executionAttributes);
        }
    }

    public InterceptorContext modifyHttpResponse(InterceptorContext context,
                                                 ExecutionAttributes executionAttributes) {
        InterceptorContext result = context;
        ExecutionInterceptor[] targets = modifyHttpResponse;
        for (int i = targets.length - 1; i >= 0; i--) {
            ExecutionInterceptor interceptor = targets[i];
            SdkHttpResponse interceptorResult = interceptor.modifyHttpResponse(result, executionAttributes);
            InputStream response = interceptor.modifyHttpResponseContent(result, executionAttributes).orElse(null);

            if (interceptorResult != result.httpResponse() || response != result.responseBody().orElse(null)) {
                validateInterceptorResult(result.httpResponse(), interceptorResult, interceptor, "modifyHttpResponse");
                result = result.copy(r -> r.httpResponse(interceptorResult)
                                           .responseBody(response));
            }
        }

        return result;
    }

    public InterceptorContext modifyAsyncHttpResponse(InterceptorContext context,
                                                      ExecutionAttributes executionAttributes) {
        InterceptorContext result = context;
        ExecutionInterceptor[] targets = modifyAsyncHttpResponse;
        for (int i = targets.length - 1; i >= 0; i--) {
            ExecutionInterceptor interceptor = targets[i];

            Publisher<ByteBuffer> newResponsePublisher =
                interceptor.modifyAsyncHttpResponseContent(result, executionAttributes).orElse(null);

            if (newResponsePublisher != result.responsePublisher().orElse(null)) {
                result = result.copy(r -> r.responsePublisher(newResponsePublisher));
            }
        }

        return result;
    }

    public void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {
        ExecutionInterceptor[] targets = beforeUnmarshalling;
        for (int i = targets.length - 1; i >= 0; i--) {
            targets[i].beforeUnmarshalling(context, executionAttributes);
        }
    }

    public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
        ExecutionInterceptor[] targets = afterUnmarshalling;
        for (int i = targets.length - 1; i >= 0; i--) {
            targets[i].afterUnmarshalling(context, executionAttributes);
        }
    }

    public InterceptorContext modifyResponse(InterceptorContext context, ExecutionAttributes executionAttributes) {
        InterceptorContext result = context;
        ExecutionInterceptor[] targets = modifyResponse;
        for (int i = targets.length - 1; i >= 0; i--) {
            ExecutionInterceptor interceptor = targets[i];
            SdkResponse interceptorResult = interceptor.modifyResponse(result, executionAttributes);

            if (interceptorResult != result.response()) {
                validateInterceptorResult(result.response(), interceptorResult, interceptor, "modifyResponse");
                result = result.copy(b -> b.response(interceptorResult));
            }
        }

        return result;
    }

    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        ExecutionInterceptor[] targets = afterExecution;
        for (int i = targets.length - 1; i >= 0; i--) {
            targets[i].afterExecution(context, executionAttributes);
        }
    }

    public DefaultFailedExecutionContext modifyException(DefaultFailedExecutionContext context,
                                                         ExecutionAttributes executionAttributes) {
        DefaultFailedExecutionContext result = context;
        ExecutionInterceptor[] targets = modifyException;
        for (int i = targets.length - 1; i >= 0; i--) {
            ExecutionInterceptor interceptor = targets[i];
            Throwable interceptorResult = interceptor.modifyException(result, executionAttributes);

            if (interceptorResult != result.exception()) {
                validateInterceptorResult(result.exception(), interceptorResult, interceptor, "modifyException");
                result = result.copy(b -> b.exception(interceptorResult));
            }
        }

        return result;
    }

    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        ExecutionInterceptor[] targets = onExecutionFailure;
        for (int i = 0; i < targets.length; i++) {
            targets[i].onExecutionFailure(context, executionAttributes);
        }
    }

    /**
     * Validate the result of calling an interceptor method that is attempting to modify the message to make sure its result is
     * valid.
     */
    private void validateInterceptorResult(Object originalMessage, Object newMessage,
                                           ExecutionInterceptor interceptor, String methodName) {
        if (!Objects.equals(originalMessage, newMessage)) {
            LOG.debug(() -> "Interceptor '" + interceptor + "' modified the message with its " + methodName + " method.");
            LOG.trace(() -> "Old: " + originalMessage + "\nNew: " + newMessage);
        }
        Validate.validState(newMessage != null,
                            "Request interceptor '%s' returned null from its %s interceptor.",
                            interceptor, methodName);
        Validate.isInstanceOf(originalMessage.getClass(), newMessage,
                              "Request interceptor '%s' returned '%s' from its %s method, but '%s' was expected.",
                              interceptor, newMessage.getClass(), methodName, originalMessage.getClass());
    }

    /**
     * The interceptors, in chain order, that override at least one of the given hooks. {@code null} in the overrides list
     * means "could not inspect; assume everything".
     */
    private ExecutionInterceptor[] select(List<Set<String>> overrides, String... hooks) {
        List<ExecutionInterceptor> selected = null;
        for (int i = 0; i < interceptors.size(); i++) {
            Set<String> overridden = overrides.get(i);
            if (overridden == null || overridesAny(overridden, hooks)) {
                if (selected == null) {
                    selected = new ArrayList<>();
                }
                selected.add(interceptors.get(i));
            }
        }
        return selected == null ? NONE : selected.toArray(new ExecutionInterceptor[0]);
    }

    private static boolean overridesAny(Set<String> overridden, String[] hooks) {
        for (String hook : hooks) {
            if (overridden.contains(hook)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The names of the {@link ExecutionInterceptor} hook methods this interceptor declares somewhere in its class hierarchy,
     * or {@code null} if the hierarchy could not be inspected. Declaring a hook (in the class, a superclass, or an
     * interface other than {@link ExecutionInterceptor} itself) is what makes the default no-op implementation
     * unreachable, so it is the right conservative notion of "overrides": a match may be an unrelated overload with the same
     * name, in which case the interceptor is simply visited as it always was.
     */
    private static Set<String> overriddenHooks(ExecutionInterceptor interceptor) {
        try {
            Set<String> hooks = new HashSet<>();
            Set<Class<?>> visited = new HashSet<>();
            collectDeclaredHooks(interceptor.getClass(), hooks, visited);
            return hooks;
        } catch (Throwable t) {
            LOG.debug(() -> "Could not inspect interceptor " + interceptor + "; it will be invoked for every hook.", t);
            return null;
        }
    }

    private static void collectDeclaredHooks(Class<?> type, Set<String> hooks, Set<Class<?>> visited) {
        if (type == null || type == Object.class || type == ExecutionInterceptor.class || !visited.add(type)) {
            return;
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getParameterCount() == 2 && !method.isSynthetic() && !method.isBridge()) {
                hooks.add(method.getName());
            }
        }
        collectDeclaredHooks(type.getSuperclass(), hooks, visited);
        for (Class<?> iface : type.getInterfaces()) {
            collectDeclaredHooks(iface, hooks, visited);
        }
    }
}
