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

package software.amazon.awssdk.protocols.core;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * The protocols a service's canonical model declares, and the protocol data of its model that is not in runtime
 * schemas because it lives in build-time-only AWS protocol traits. Generated services create one instance as
 * {@code <Service>ServiceMetadata.serviceProtocols()}; the dynamic client creates one from a runtime model.
 *
 * <p>It holds:
 * <ul>
 *   <li>the service schema, whose {@code smithy.api#xmlNamespace} trait the XML-based protocols read
 *       ({@link #xmlNamespace()});</li>
 *   <li>every protocol trait applied to the service, with its options, sorted by trait shape ID and unique;</li>
 *   <li>whether the service has {@code aws.protocols#awsQueryCompatible};</li>
 *   <li>the {@code aws.protocols#awsQueryError} code of each modeled error that has one, by error shape ID;</li>
 *   <li>the modeled service version, which the query protocols send as {@code Version} ({@link #serviceVersion()});
 *       generated services record it when they declare {@code awsQuery} or {@code ec2Query};</li>
 *   <li>the {@code aws.protocols#ec2QueryName} of each member that has one, by member shape ID
 *       ({@link #ec2QueryNames()}); generated services record them when they declare {@code ec2Query}.</li>
 * </ul>
 * Instances are immutable. The declared set may include protocols that no launch runtime supports; selection
 * ({@link ProtocolSelector}) only ever picks a declared protocol that is also installed.
 */
public final class ServiceProtocols {
    private final Schema service;
    private final Map<ShapeId, ModeledProtocol> protocols;
    private final List<ShapeId> protocolIds;
    private final boolean awsQueryCompatible;
    private final Map<ShapeId, AwsQueryErrorCode> awsQueryErrors;
    private final String serviceVersion;
    private final Map<ShapeId, String> ec2QueryNames;

    private ServiceProtocols(Builder builder) {
        this.service = builder.service;
        this.protocols = Collections.unmodifiableMap(new TreeMap<>(builder.protocols));
        this.protocolIds = List.copyOf(protocols.keySet());
        this.awsQueryCompatible = builder.awsQueryCompatible;
        this.awsQueryErrors = Collections.unmodifiableMap(new TreeMap<>(builder.awsQueryErrors));
        this.serviceVersion = builder.serviceVersion;
        this.ec2QueryNames = Collections.unmodifiableMap(new TreeMap<>(builder.ec2QueryNames));
    }

    /**
     * A builder for the protocols of the service described by {@code serviceSchema}.
     *
     * @throws IllegalArgumentException if {@code serviceSchema} is not a service schema
     */
    public static Builder builder(Schema serviceSchema) {
        return new Builder(serviceSchema);
    }

    /** Absolute shape ID of the service. */
    public ShapeId serviceId() {
        return service.id();
    }

    /** The service schema. */
    public Schema serviceSchema() {
        return service;
    }

    /** Every declared protocol, sorted by trait shape ID. */
    public Collection<ModeledProtocol> protocols() {
        return protocols.values();
    }

    /** The trait shape IDs of every declared protocol, sorted. */
    public List<ShapeId> protocolIds() {
        return protocolIds;
    }

    /** The declared protocol with exactly the trait shape ID {@code protocolId}, if the service declares it. */
    public Optional<ModeledProtocol> protocol(ShapeId protocolId) {
        return Optional.ofNullable(protocols.get(protocolId));
    }

    /** True if the service declares the protocol with trait shape ID {@code protocolId}. */
    public boolean declares(ShapeId protocolId) {
        return protocols.containsKey(protocolId);
    }

    /** The service's {@code smithy.api#xmlNamespace} trait, if its schema has one. */
    public Optional<XmlNamespaceTrait> xmlNamespace() {
        return Optional.ofNullable(service.getTrait(TraitKey.XML_NAMESPACE_TRAIT));
    }

    /** True if the service has the {@code aws.protocols#awsQueryCompatible} trait. */
    public boolean awsQueryCompatible() {
        return awsQueryCompatible;
    }

    /** The {@code awsQueryError} codes of the service's errors, by error shape ID, sorted. */
    public Map<ShapeId, AwsQueryErrorCode> awsQueryErrors() {
        return awsQueryErrors;
    }

    /** The modeled service version; empty if it was not recorded or the service has none. */
    public String serviceVersion() {
        return serviceVersion;
    }

    /** The {@code aws.protocols#ec2QueryName} values of the service's members, by member shape ID, sorted. */
    public Map<ShapeId, String> ec2QueryNames() {
        return ec2QueryNames;
    }

    @Override
    public String toString() {
        return "ServiceProtocols[" + service.id() + ": " + protocols.keySet() + "]";
    }

    /** Builder of {@link ServiceProtocols}. */
    public static final class Builder {
        private final Schema service;
        private final Map<ShapeId, ModeledProtocol> protocols = new TreeMap<>();
        private boolean awsQueryCompatible;
        private final Map<ShapeId, AwsQueryErrorCode> awsQueryErrors = new TreeMap<>();
        private String serviceVersion = "";
        private final Map<ShapeId, String> ec2QueryNames = new TreeMap<>();

        private Builder(Schema service) {
            Objects.requireNonNull(service, "serviceSchema");
            if (service.type() != ShapeType.SERVICE) {
                throw new IllegalArgumentException("Not a service schema: " + service.type() + " " + service.id());
            }
            this.service = service;
        }

        /**
         * Adds a declared protocol.
         *
         * @throws IllegalArgumentException if a protocol with the same trait shape ID was already added
         */
        public Builder protocol(ModeledProtocol protocol) {
            Objects.requireNonNull(protocol, "protocol");
            if (protocols.putIfAbsent(protocol.id(), protocol) != null) {
                throw new IllegalArgumentException("Protocol " + protocol.id() + " of " + service.id()
                                                   + " is declared more than once");
            }
            return this;
        }

        /** Records the {@code aws.protocols#awsQueryCompatible} trait. */
        public Builder awsQueryCompatible(boolean awsQueryCompatible) {
            this.awsQueryCompatible = awsQueryCompatible;
            return this;
        }

        /**
         * Adds the {@code awsQueryError} code of one error.
         *
         * @throws IllegalArgumentException if the error already has one
         */
        public Builder awsQueryError(ShapeId error, String code, int httpResponseCode) {
            AwsQueryErrorCode value = new AwsQueryErrorCode(error, code, httpResponseCode);
            if (awsQueryErrors.putIfAbsent(error, value) != null) {
                throw new IllegalArgumentException("Error " + error + " has more than one awsQueryError code");
            }
            return this;
        }

        /** Records the modeled service version. */
        public Builder serviceVersion(String serviceVersion) {
            this.serviceVersion = Objects.requireNonNull(serviceVersion, "serviceVersion");
            return this;
        }

        /**
         * Adds the {@code aws.protocols#ec2QueryName} of one structure or union member.
         *
         * @throws IllegalArgumentException if {@code member} is not a member shape ID, {@code name} is empty, or the
         *         member already has one
         */
        public Builder ec2QueryName(ShapeId member, String name) {
            Objects.requireNonNull(member, "member");
            Objects.requireNonNull(name, "name");
            if (!member.hasMember()) {
                throw new IllegalArgumentException("An ec2QueryName belongs to a member, not " + member);
            }
            if (name.isEmpty()) {
                throw new IllegalArgumentException("The ec2QueryName of " + member + " is empty");
            }
            if (ec2QueryNames.putIfAbsent(member, name) != null) {
                throw new IllegalArgumentException("Member " + member + " has more than one ec2QueryName");
            }
            return this;
        }

        public ServiceProtocols build() {
            return new ServiceProtocols(this);
        }
    }
}
