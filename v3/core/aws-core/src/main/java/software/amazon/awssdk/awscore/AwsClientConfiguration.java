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

package software.amazon.awssdk.awscore;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.internal.DefaultAwsClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.TimeoutConfiguration;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The resolved, immutable configuration of an AWS client (Requirements 7.11, 9.1): the exact object every call and
 * waiter of that client reads, not a reconstructed summary.
 *
 * <p>Typed accessors cover the settings whose types {@code aws-core} can see. Settings owned by other modules (retry
 * strategy, HTTP client, interceptors, telemetry) and service-specific options are read through
 * {@link #get(ClientOption)} with the keys those modules declare; a generated {@code <Service>ClientConfiguration}
 * extends this view with typed service options.
 *
 * <p>Implementations are immutable and safe to publish, and never render option values that may be secret.
 */
public interface AwsClientConfiguration {
    /** The region the client sends requests to. */
    Region region();

    /** The endpoint every call starts from: the endpoint override, or the resolved default endpoint. */
    URI clientEndpoint();

    /** The provider of the AWS credentials the client signs with; it may refresh the credentials it returns. */
    IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider();

    /** The client-level API-call and API-call-attempt timeouts; {@link TimeoutConfiguration#none()} if neither. */
    TimeoutConfiguration timeouts();

    /** Shape ID of the protocol the client was built with. */
    ShapeId protocolId();

    /** Every resource the client was configured with and who owns it. Immutable. */
    List<ManagedResource<?>> resources();

    /** The value of {@code option}, or empty if it is not configured. */
    <T> Optional<T> get(ClientOption<T> option);

    /**
     * The view of {@code configuration}.
     *
     * @throws SdkClientConfigurationException naming the option, if {@link AwsClientOption#AWS_REGION},
     *     {@link SdkClientOption#CLIENT_ENDPOINT}, {@link AwsClientOption#CREDENTIALS_IDENTITY_PROVIDER} or
     *     {@link SdkClientOption#PROTOCOL_ID} is missing, or the credentials provider does not resolve
     *     {@link AwsCredentialsIdentity} instances
     */
    static AwsClientConfiguration create(SdkClientConfiguration configuration) {
        return DefaultAwsClientConfiguration.create(configuration);
    }
}
