package software.amazon.awssdk.services.demo;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.services.demo.auth.scheme.DemoAuthSchemeProvider;
import software.amazon.awssdk.services.demo.endpoints.DemoEndpointProvider;

/**
 * This includes configuration specific to null that is supported by both {@link DemoClientBuilder} and
 * {@link DemoAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface DemoBaseClientBuilder<B extends DemoBaseClientBuilder<B, C>, C> extends AwsClientBuilder<B, C> {
    /**
     * Set the {@link DemoEndpointProvider} implementation that will be used by the client to determine the endpoint for
     * each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B endpointProvider(DemoEndpointProvider endpointProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the {@link DemoAuthSchemeProvider} implementation that will be used by the client to resolve the auth scheme
     * for each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B authSchemeProvider(DemoAuthSchemeProvider authSchemeProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the {@link ClientProtocol} the client serializes requests and deserializes responses and errors with. This is
     * optional; if none is provided, or it is set to null, the client uses the service's default protocol.
     * <p>
     * The protocol's message exchange must match the client transport's; otherwise building the client fails with an
     * {@link java.lang.IllegalStateException}.
     */
    default B protocol(ClientProtocol<?, ?> protocol) {
        throw new UnsupportedOperationException();
    }
}
