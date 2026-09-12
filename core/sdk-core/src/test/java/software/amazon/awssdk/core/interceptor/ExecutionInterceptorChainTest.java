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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.internal.interceptor.DefaultFailedExecutionContext;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Pins the two things a caller can observe about the chain: the order interceptors are visited in for each hook, and that an
 * interceptor is visited for every hook it implements, however it came to implement it.
 */
public class ExecutionInterceptorChainTest {
    private final List<String> calls = new ArrayList<>();
    private final ExecutionAttributes attributes = new ExecutionAttributes();

    @Test
    public void allHooks_twoInterceptors_forwardOnRequestPathReverseOnResponsePath() {
        ExecutionInterceptorChain chain = new ExecutionInterceptorChain(Arrays.asList(new Recording("a"), new Recording("b")));
        InterceptorContext context = context();

        chain.beforeExecution(context, attributes);
        chain.modifyRequest(context, attributes);
        chain.beforeMarshalling(context, attributes);
        chain.afterMarshalling(context, attributes);
        chain.modifyHttpRequestAndHttpContent(context, attributes);
        chain.beforeTransmission(context, attributes);
        chain.afterTransmission(context, attributes);
        chain.modifyHttpResponse(context, attributes);
        chain.modifyAsyncHttpResponse(context, attributes);
        chain.beforeUnmarshalling(context, attributes);
        chain.afterUnmarshalling(context, attributes);
        chain.modifyResponse(context, attributes);
        chain.afterExecution(context, attributes);
        chain.modifyException(failedContext(context), attributes);
        chain.onExecutionFailure(failedContext(context), attributes);

        assertThat(calls).containsExactly(
            "a.beforeExecution", "b.beforeExecution",
            "a.modifyRequest", "b.modifyRequest",
            "a.beforeMarshalling", "b.beforeMarshalling",
            "a.afterMarshalling", "b.afterMarshalling",
            "a.modifyAsyncHttpContent", "a.modifyHttpContent", "a.modifyHttpRequest",
            "b.modifyAsyncHttpContent", "b.modifyHttpContent", "b.modifyHttpRequest",
            "a.beforeTransmission", "b.beforeTransmission",
            "b.afterTransmission", "a.afterTransmission",
            "b.modifyHttpResponse", "b.modifyHttpResponseContent", "a.modifyHttpResponse", "a.modifyHttpResponseContent",
            "b.modifyAsyncHttpResponseContent", "a.modifyAsyncHttpResponseContent",
            "b.beforeUnmarshalling", "a.beforeUnmarshalling",
            "b.afterUnmarshalling", "a.afterUnmarshalling",
            "b.modifyResponse", "a.modifyResponse",
            "b.afterExecution", "a.afterExecution",
            "b.modifyException", "a.modifyException",
            "a.onExecutionFailure", "b.onExecutionFailure");
    }

    @Test
    public void hooks_interceptorsImplementDifferentHooks_relativeOrderPreserved() {
        // "only" implements a single hook; it must still slot in between "a" and "b" on that hook.
        ExecutionInterceptorChain chain = new ExecutionInterceptorChain(
            Arrays.asList(new Recording("a"), new OnlyBeforeTransmission("only"), new Recording("b")));
        InterceptorContext context = context();

        chain.beforeExecution(context, attributes);
        chain.beforeTransmission(context, attributes);
        chain.afterTransmission(context, attributes);

        assertThat(calls).containsExactly("a.beforeExecution", "b.beforeExecution",
                                          "a.beforeTransmission", "only.beforeTransmission", "b.beforeTransmission",
                                          "b.afterTransmission", "a.afterTransmission");
    }

    @Test
    public void beforeExecution_hookInheritedFromSuperclass_isInvoked() {
        ExecutionInterceptorChain chain = new ExecutionInterceptorChain(Arrays.asList(new SubclassOfRecording("sub")));
        chain.beforeExecution(context(), attributes);
        assertThat(calls).containsExactly("sub.beforeExecution");
    }

    @Test
    public void beforeExecution_hookFromInterfaceDefault_isInvoked() {
        ExecutionInterceptorChain chain = new ExecutionInterceptorChain(Arrays.asList(new ViaInterfaceDefault()));
        chain.beforeExecution(context(), attributes);
        assertThat(calls).containsExactly("default.beforeExecution");
    }

    @Test
    public void beforeExecution_hookFromInterfaceDefaultWithBaseInterfaceAlsoListed_isInvoked() {
        ExecutionInterceptorChain chain = new ExecutionInterceptorChain(Arrays.asList(new ViaInterfaceDefaultAndBase()));
        chain.beforeExecution(context(), attributes);
        assertThat(calls).containsExactly("default.beforeExecution");
    }

    @Test
    public void hooks_proxyInterceptor_isInvokedForEveryHook() {
        InvocationHandler handler = (proxy, method, args) -> {
            calls.add("proxy." + method.getName());
            return method.isDefault() ? defaultResult(method.getName(), (InterceptorContext) args[0]) : null;
        };
        ExecutionInterceptor proxy = (ExecutionInterceptor) Proxy.newProxyInstance(getClass().getClassLoader(),
                                                                                    new Class<?>[] {ExecutionInterceptor.class},
                                                                                    handler);
        ExecutionInterceptorChain chain = new ExecutionInterceptorChain(Arrays.asList(proxy));
        InterceptorContext context = context();

        chain.beforeExecution(context, attributes);
        chain.modifyRequest(context, attributes);
        chain.afterExecution(context, attributes);

        assertThat(calls).containsExactly("proxy.beforeExecution", "proxy.modifyRequest", "proxy.afterExecution");
    }

    @Test
    public void beforeExecution_mockInterceptor_isInvoked() {
        ExecutionInterceptor mock = mock(ExecutionInterceptor.class);
        new ExecutionInterceptorChain(Arrays.asList(mock)).beforeExecution(context(), attributes);
        verify(mock).beforeExecution(any(), any());
    }

    @Test
    public void modifyHooks_emptyChain_returnSameContext() {
        ExecutionInterceptorChain chain = new ExecutionInterceptorChain(new ArrayList<>());
        InterceptorContext context = context();
        assertThat(chain.modifyRequest(context, attributes)).isSameAs(context);
        assertThat(chain.modifyHttpRequestAndHttpContent(context, attributes)).isSameAs(context);
        assertThat(chain.modifyHttpResponse(context, attributes)).isSameAs(context);
        assertThat(chain.modifyResponse(context, attributes)).isSameAs(context);
    }

    @Test
    public void modifyHttpRequestAndHttpContent_twoModifyingInterceptors_appliedInOrder() {
        ExecutionInterceptor first = new ExecutionInterceptor() {
            @Override
            public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes attrs) {
                return context.httpRequest().copy(r -> r.putHeader("x-first", "1"));
            }
        };
        ExecutionInterceptor second = new ExecutionInterceptor() {
            @Override
            public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes attrs) {
                assertThat(context.httpRequest().firstMatchingHeader("x-first")).hasValue("1");
                return context.httpRequest().copy(r -> r.putHeader("x-second", "2"));
            }
        };
        InterceptorContext result = new ExecutionInterceptorChain(Arrays.asList(first, second))
            .modifyHttpRequestAndHttpContent(context(), attributes);
        assertThat(result.httpRequest().firstMatchingHeader("x-first")).hasValue("1");
        assertThat(result.httpRequest().firstMatchingHeader("x-second")).hasValue("2");
    }

    private static Object defaultResult(String hook, InterceptorContext context) {
        switch (hook) {
            case "modifyRequest":
                return context.request();
            case "modifyHttpRequest":
                return context.httpRequest();
            case "modifyHttpResponse":
                return context.httpResponse();
            case "modifyResponse":
                return context.response();
            case "modifyHttpContent":
                return context.requestBody();
            case "modifyAsyncHttpContent":
                return context.asyncRequestBody();
            case "modifyHttpResponseContent":
                return context.responseBody();
            case "modifyAsyncHttpResponseContent":
                return context.responsePublisher();
            default:
                return null;
        }
    }

    private InterceptorContext context() {
        return InterceptorContext.builder()
                                 .request(mock(SdkRequest.class))
                                 .httpRequest(SdkHttpFullRequest.builder()
                                                                .method(SdkHttpMethod.GET)
                                                                .protocol("https")
                                                                .host("localhost")
                                                                .build())
                                 .httpResponse(SdkHttpFullResponse.builder().statusCode(200).build())
                                 .response(mock(SdkResponse.class))
                                 .build();
    }

    private DefaultFailedExecutionContext failedContext(InterceptorContext context) {
        return DefaultFailedExecutionContext.builder()
                                            .interceptorContext(context)
                                            .exception(new RuntimeException("boom"))
                                            .build();
    }

    /** Overrides every hook and records the visits. */
    private class Recording implements ExecutionInterceptor {
        private final String name;

        Recording(String name) {
            this.name = name;
        }

        void record(String hook) {
            calls.add(name + "." + hook);
        }

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes attrs) {
            record("beforeExecution");
        }

        @Override
        public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes attrs) {
            record("modifyRequest");
            return context.request();
        }

        @Override
        public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes attrs) {
            record("beforeMarshalling");
        }

        @Override
        public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes attrs) {
            record("afterMarshalling");
        }

        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes attrs) {
            record("modifyHttpRequest");
            return context.httpRequest();
        }

        @Override
        public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context, ExecutionAttributes attrs) {
            record("modifyHttpContent");
            return context.requestBody();
        }

        @Override
        public Optional<software.amazon.awssdk.core.async.AsyncRequestBody> modifyAsyncHttpContent(
            Context.ModifyHttpRequest context, ExecutionAttributes attrs) {
            record("modifyAsyncHttpContent");
            return context.asyncRequestBody();
        }

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attrs) {
            record("beforeTransmission");
        }

        @Override
        public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes attrs) {
            record("afterTransmission");
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context, ExecutionAttributes attrs) {
            record("modifyHttpResponse");
            return context.httpResponse();
        }

        @Override
        public Optional<java.io.InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context,
                                                                       ExecutionAttributes attrs) {
            record("modifyHttpResponseContent");
            return context.responseBody();
        }

        @Override
        public Optional<org.reactivestreams.Publisher<java.nio.ByteBuffer>> modifyAsyncHttpResponseContent(
            Context.ModifyHttpResponse context, ExecutionAttributes attrs) {
            record("modifyAsyncHttpResponseContent");
            return context.responsePublisher();
        }

        @Override
        public void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes attrs) {
            record("beforeUnmarshalling");
        }

        @Override
        public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes attrs) {
            record("afterUnmarshalling");
        }

        @Override
        public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes attrs) {
            record("modifyResponse");
            return context.response();
        }

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes attrs) {
            record("afterExecution");
        }

        @Override
        public Throwable modifyException(Context.FailedExecution context, ExecutionAttributes attrs) {
            record("modifyException");
            return context.exception();
        }

        @Override
        public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes attrs) {
            record("onExecutionFailure");
        }
    }

    /** Declares nothing itself; every hook comes from the superclass. */
    private class SubclassOfRecording extends Recording {
        SubclassOfRecording(String name) {
            super(name);
        }
    }

    private class OnlyBeforeTransmission implements ExecutionInterceptor {
        private final String name;

        OnlyBeforeTransmission(String name) {
            this.name = name;
        }

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attrs) {
            calls.add(name + ".beforeTransmission");
        }
    }

    private interface DefaultBeforeExecution extends ExecutionInterceptor {
        List<String> calls();

        @Override
        default void beforeExecution(Context.BeforeExecution context, ExecutionAttributes attrs) {
            calls().add("default.beforeExecution");
        }
    }

    private class ViaInterfaceDefault implements DefaultBeforeExecution {
        @Override
        public List<String> calls() {
            return calls;
        }
    }

    private class ViaInterfaceDefaultAndBase implements ExecutionInterceptor, DefaultBeforeExecution {
        @Override
        public List<String> calls() {
            return calls;
        }
    }
}
