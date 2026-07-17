package com.example.demo.interceptors;

/**
 * A miniature of the AWS SDK for Java <b>v2</b> {@code ExecutionInterceptor} SPI
 * (software.amazon.awssdk.core.interceptor.ExecutionInterceptor).
 *
 * <p>The real interface has 13 hooks taking {@code Context.*} + {@code ExecutionAttributes}.
 * We model the representative subset the demo exercises, keeping the v2 names and the
 * "read vs. modify" split so the shape is recognizably v2. {@link V2InterceptorBridge}
 * adapts implementations of this onto smithy-java's {@code ClientInterceptor}.
 */
public interface V2ExecutionInterceptor {

    /** Mirrors v2 {@code beforeExecution(Context.BeforeExecution, ExecutionAttributes)}. */
    default void beforeExecution(String operationName) {}

    /** Mirrors v2 {@code beforeTransmission(...)} — observe the HTTP request before it's sent. */
    default void beforeTransmission(V2HttpRequest httpRequest) {}

    /**
     * Mirrors v2 {@code modifyHttpRequest(Context.ModifyHttpRequest, ExecutionAttributes)} —
     * return a (possibly mutated) HTTP request. The default is identity.
     */
    default V2HttpRequest modifyHttpRequest(V2HttpRequest httpRequest) {
        return httpRequest;
    }

    /** Mirrors v2 {@code afterExecution(...)} / {@code onExecutionFailure(...)}. */
    default void afterExecution(String operationName, Throwable error) {}

    /**
     * A tiny stand-in for v2's {@code SdkHttpRequest} as seen by an interceptor: just enough
     * surface (add a header) to prove a mutation made through the v2 API reaches the wire.
     * The bridge backs this by a live smithy-java {@code ModifiableHttpRequest}.
     */
    interface V2HttpRequest {
        String method();

        String uri();

        /** v2-style: returns a new request with the header added (here: mutates the backing request). */
        V2HttpRequest addHeader(String name, String value);
    }
}
