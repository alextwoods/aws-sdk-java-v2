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

package software.amazon.awssdk.pipeline.client;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.endpoints.rules.EndpointRuleSet;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The static, client-independent facts a generated client builder hands to {@link AwsClientBuildState}: the
 * service's identity, its declared protocols, its type registry, if modeled its endpoint rule set and, if its modeled
 * auth needs AWS credentials, the factory of its default credentials provider. Generated builders create one from
 * direct references to the service's generated metadata; nothing is discovered by name.
 *
 * <p>The default credentials factory is a plain {@link Function} of the client's {@link ConfigurationSources}: it is
 * called once per build, only when no credentials provider was set, with the same external sources the rest of the
 * client configuration resolves from. This keeps aws-pipeline independent of the credential-provider implementations
 * (generated service modules supply the default chain). An {@link AutoCloseable} result is owned by the client.
 *
 * @param serviceId absolute shape ID of the service
 * @param serviceName the V2-compatible service name ({@code serviceName()}) and retry-quota scope
 * @param protocols the service's declared protocols
 * @param typeRegistry the service's primary type registry
 * @param endpointRules the service's endpoint rule set, or {@code null} when it has none
 * @param defaultCredentials the factory of the default credentials provider, or {@code null} when there is none
 */
public record AwsServiceClientDefinition(
    ShapeId serviceId, String serviceName, ServiceProtocols protocols, SchemaTypeRegistry typeRegistry,
    EndpointRuleSet endpointRules,
    Function<ConfigurationSources, ? extends IdentityProvider<? extends AwsCredentialsIdentity>> defaultCredentials) {

    public AwsServiceClientDefinition {
        Objects.requireNonNull(serviceId, "serviceId");
        Objects.requireNonNull(serviceName, "serviceName");
        Objects.requireNonNull(protocols, "protocols");
        Objects.requireNonNull(typeRegistry, "typeRegistry");
    }

    /** A definition without a default credentials provider. */
    public AwsServiceClientDefinition(ShapeId serviceId, String serviceName, ServiceProtocols protocols,
                                      SchemaTypeRegistry typeRegistry, EndpointRuleSet endpointRules) {
        this(serviceId, serviceName, protocols, typeRegistry, endpointRules, null);
    }

    /** This definition with {@code factory} as its default credentials provider factory. */
    public AwsServiceClientDefinition withDefaultCredentials(
        Function<ConfigurationSources, ? extends IdentityProvider<? extends AwsCredentialsIdentity>> factory) {
        return new AwsServiceClientDefinition(serviceId, serviceName, protocols, typeRegistry, endpointRules,
                                              Objects.requireNonNull(factory, "factory"));
    }

    /** The service's endpoint rule set, if it has one. */
    public Optional<EndpointRuleSet> rules() {
        return Optional.ofNullable(endpointRules);
    }

    /** The factory of the service's default credentials provider, if it has one. */
    public Optional<Function<ConfigurationSources, ? extends IdentityProvider<? extends AwsCredentialsIdentity>>>
        defaultCredentialsFactory() {
        return Optional.ofNullable(defaultCredentials);
    }
}
