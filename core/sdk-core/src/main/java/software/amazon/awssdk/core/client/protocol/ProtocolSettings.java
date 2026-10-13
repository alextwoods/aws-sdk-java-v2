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

package software.amazon.awssdk.core.client.protocol;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The service-level settings a {@link ClientProtocolFactory} creates a {@link ClientProtocol} with.
 *
 * <p>Instances are immutable and safe to use from multiple threads.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.core.ProtocolSettings}, with
 * {@link #baseExceptionBuilder()} added: protocols throw the service's base exception for errors the error registry
 * cannot resolve.
 */
@SdkPublicApi
public final class ProtocolSettings implements ToCopyableBuilder<ProtocolSettings.Builder, ProtocolSettings> {
    private static final Supplier<SdkServiceException.Builder> DEFAULT_BASE_EXCEPTION_BUILDER = SdkServiceException::builder;

    private final ShapeId service;
    private final String serviceVersion;
    private final Schema serviceSchema;
    private final Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder;

    private ProtocolSettings(BuilderImpl builder) {
        ShapeId resolvedService = builder.service;
        Schema schema = builder.serviceSchema;
        if (schema != null) {
            Validate.isTrue(schema.type() == ShapeType.SERVICE,
                            "serviceSchema must have shape type service, but %s has shape type %s", schema.id(),
                            schema.type());
            if (resolvedService == null) {
                resolvedService = schema.id();
            } else {
                Validate.isTrue(resolvedService.equals(schema.id()),
                                "service %s does not match the ID of serviceSchema %s", resolvedService, schema.id());
            }
        }
        this.service = Validate.notNull(resolvedService, "service or serviceSchema must be set");
        this.serviceVersion = builder.serviceVersion;
        this.serviceSchema = schema;
        this.baseExceptionBuilder = builder.baseExceptionBuilder != null ? builder.baseExceptionBuilder
                                                                         : DEFAULT_BASE_EXCEPTION_BUILDER;
    }

    /**
     * Creates a builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Returns the ID of the service.
     *
     * @return the service shape ID; never null
     */
    public ShapeId service() {
        return service;
    }

    /**
     * Returns the version of the service API. Some protocols, such as {@code aws.protocols#awsQuery}, send it with
     * every request.
     *
     * @return the service version, or null if not set
     */
    public String serviceVersion() {
        return serviceVersion;
    }

    /**
     * Returns the schema of the service. It carries the service-level traits that protocols read at runtime, such as
     * {@code aws.protocols#awsQueryCompatible} and the protocol definition traits.
     *
     * @return the service schema, or null if not set
     */
    public Schema serviceSchema() {
        return serviceSchema;
    }

    /**
     * Returns the supplier of builders for the service's base exception, for example {@code SqsException::builder}.
     * Protocols use it for error responses whose error type is not in the error registry or cannot be determined.
     *
     * @return the supplier; {@code SdkServiceException::builder} if none was set
     */
    public Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder() {
        return baseExceptionBuilder;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public String toString() {
        return ToString.builder("ProtocolSettings")
                       .add("service", service)
                       .add("serviceVersion", serviceVersion)
                       .build();
    }

    /**
     * A builder for {@link ProtocolSettings}.
     */
    public interface Builder extends CopyableBuilder<Builder, ProtocolSettings> {
        /**
         * Sets the ID of the service. Required unless {@link #serviceSchema(Schema)} is set, whose ID is then used.
         *
         * @param service the service shape ID
         * @return this builder
         */
        Builder service(ShapeId service);

        /**
         * Sets the version of the service API.
         *
         * @param serviceVersion the service version
         * @return this builder
         */
        Builder serviceVersion(String serviceVersion);

        /**
         * Sets the schema of the service. It must be a service schema, and its ID must equal {@link #service(ShapeId)}
         * when that is set too.
         *
         * @param serviceSchema the service schema
         * @return this builder
         */
        Builder serviceSchema(Schema serviceSchema);

        /**
         * Sets the supplier of builders for the service's base exception. Defaults to
         * {@code SdkServiceException::builder}.
         *
         * @param baseExceptionBuilder the supplier, for example {@code SqsException::builder}
         * @return this builder
         */
        Builder baseExceptionBuilder(Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder);

        /**
         * Builds the settings.
         *
         * @return the settings
         * @throws NullPointerException if neither the service nor the service schema is set
         * @throws IllegalArgumentException if the service schema is not a service schema, or its ID differs from the
         *     service
         */
        @Override
        ProtocolSettings build();
    }

    private static final class BuilderImpl implements Builder {
        private ShapeId service;
        private String serviceVersion;
        private Schema serviceSchema;
        private Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder;

        private BuilderImpl() {
        }

        private BuilderImpl(ProtocolSettings settings) {
            this.service = settings.service;
            this.serviceVersion = settings.serviceVersion;
            this.serviceSchema = settings.serviceSchema;
            this.baseExceptionBuilder = settings.baseExceptionBuilder;
        }

        @Override
        public Builder service(ShapeId service) {
            this.service = service;
            return this;
        }

        @Override
        public Builder serviceVersion(String serviceVersion) {
            this.serviceVersion = serviceVersion;
            return this;
        }

        @Override
        public Builder serviceSchema(Schema serviceSchema) {
            this.serviceSchema = serviceSchema;
            return this;
        }

        @Override
        public Builder baseExceptionBuilder(Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder) {
            this.baseExceptionBuilder = baseExceptionBuilder;
            return this;
        }

        @Override
        public ProtocolSettings build() {
            return new ProtocolSettings(this);
        }
    }
}
