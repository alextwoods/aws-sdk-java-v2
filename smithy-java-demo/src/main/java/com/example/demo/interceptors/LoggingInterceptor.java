package com.example.demo.interceptors;

import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.InputHook;
import software.amazon.smithy.java.client.core.interceptors.OutputHook;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.client.core.interceptors.ResponseHook;
import software.amazon.smithy.java.http.api.HttpRequest;

/**
 * A <b>native</b> smithy-java {@link ClientInterceptor}. This is the baseline: it shows the
 * generated DynamoDB client (which {@code extends Client}) accepts interceptors directly via
 * the inherited {@code Builder.addInterceptor(...)} — no bridge needed.
 *
 * <p>It just narrates the request lifecycle so you can see the smithy-java pipeline firing.
 */
public final class LoggingInterceptor implements ClientInterceptor {

    @Override
    public void readBeforeExecution(InputHook<?, ?> hook) {
        System.out.println("  [native] readBeforeExecution   op=" + hook.operation().schema().id().getName());
    }

    @Override
    public void readAfterSerialization(RequestHook<?, ?, ?> hook) {
        System.out.println("  [native] readAfterSerialization  request=" + hook.request().getClass().getSimpleName());
    }

    @Override
    public void readBeforeTransmit(RequestHook<?, ?, ?> hook) {
        // This hook runs AFTER signing. If the header the v2-bridge added in
        // modifyBeforeSigning is visible here, it survived onto the signed, transmitted
        // request -> proof the v2 mutation reached the wire (and was signed).
        String injected = hook.request() instanceof HttpRequest req
                ? req.headers().firstValue("X-Demo-Interceptor")
                : null;
        System.out.println("  [native] readBeforeTransmit    (post-sign) X-Demo-Interceptor="
                + injected);
    }

    @Override
    public void readAfterTransmit(ResponseHook<?, ?, ?, ?> hook) {
        System.out.println("  [native] readAfterTransmit     response=" + hook.response().getClass().getSimpleName());
    }

    @Override
    public void readAfterExecution(OutputHook<?, ?, ?, ?> hook, RuntimeException error) {
        System.out.println("  [native] readAfterExecution    error=" + (error == null ? "none" : error));
    }
}
