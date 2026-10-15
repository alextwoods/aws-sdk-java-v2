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

package software.amazon.awssdk.codegen.c2j2smithy;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * The C2J model files of one service, as JSON trees: the converter's input.
 *
 * <p>This is the converter-side counterpart of codegen's {@code software.amazon.awssdk.codegen.C2jModels}. The codegen
 * module depends on this module, so this module cannot use codegen's POJO model types; it reads the same files as
 * Jackson trees instead. The service model is the C2J {@code service-2.json} after C2J customizations are applied.
 */
public final class C2jInput {
    /** The C2J service model file name. */
    public static final String SERVICE_MODEL_FILE = "service-2.json";
    /** The C2J paginators file name. */
    public static final String PAGINATORS_FILE = "paginators-1.json";
    /** The C2J waiters file name. */
    public static final String WAITERS_FILE = "waiters-2.json";
    /** The endpoint rule set file name. */
    public static final String ENDPOINT_RULE_SET_FILE = "endpoint-rule-set.json";
    /** The endpoint tests file name. */
    public static final String ENDPOINT_TESTS_FILE = "endpoint-tests.json";

    // Comments are allowed, as in codegen's C2J loader (software.amazon.awssdk.codegen.internal.Jackson): several C2J
    // test models annotate shapes with // comments.
    private static final ObjectMapper MAPPER = JsonMapper.builder().enable(JsonReadFeature.ALLOW_JAVA_COMMENTS).build();

    private final String serviceName;
    private final JsonNode serviceModel;
    private final JsonNode paginatorsModel;
    private final JsonNode waitersModel;
    private final JsonNode endpointRuleSetModel;
    private final JsonNode endpointTestsModel;

    private C2jInput(Builder builder) {
        this.serviceName = Objects.requireNonNull(builder.serviceName, "serviceName");
        this.serviceModel = Objects.requireNonNull(builder.serviceModel, "serviceModel");
        if (!serviceModel.isObject()) {
            throw new IllegalArgumentException("The C2J service model of " + serviceName + " must be a JSON object");
        }
        this.paginatorsModel = builder.paginatorsModel;
        this.waitersModel = builder.waitersModel;
        this.endpointRuleSetModel = builder.endpointRuleSetModel;
        this.endpointTestsModel = builder.endpointTestsModel;
    }

    /**
     * Create a builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Load the C2J files of a service from a {@code codegen-resources} style directory. {@code service-2.json} is
     * required; the paginators, waiters, endpoint rule set and endpoint tests files are read when present.
     *
     * @param serviceName the service name used in the gap report and in errors, for example {@code sqs}
     * @param directory the directory that holds the C2J files
     */
    public static C2jInput fromDirectory(String serviceName, Path directory) {
        Path serviceFile = directory.resolve(SERVICE_MODEL_FILE);
        if (!Files.isRegularFile(serviceFile)) {
            throw new IllegalArgumentException("No " + SERVICE_MODEL_FILE + " in " + directory);
        }
        return builder().serviceName(serviceName)
                        .serviceModel(read(serviceFile))
                        .paginatorsModel(readIfPresent(directory.resolve(PAGINATORS_FILE)))
                        .waitersModel(readIfPresent(directory.resolve(WAITERS_FILE)))
                        .endpointRuleSetModel(readIfPresent(directory.resolve(ENDPOINT_RULE_SET_FILE)))
                        .endpointTestsModel(readIfPresent(directory.resolve(ENDPOINT_TESTS_FILE)))
                        .build();
    }

    /**
     * Parse one C2J JSON file. Java-style comments are allowed, as in codegen's C2J loader.
     */
    public static JsonNode read(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            return MAPPER.readTree(is);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read C2J model file " + file, e);
        }
    }

    private static JsonNode readIfPresent(Path file) {
        return Files.isRegularFile(file) ? read(file) : null;
    }

    /**
     * The service name used in the gap report and in errors.
     */
    public String serviceName() {
        return serviceName;
    }

    /**
     * The C2J service model ({@code service-2.json}), after C2J customizations. Callers must not modify it.
     */
    public JsonNode serviceModel() {
        return serviceModel;
    }

    /**
     * The C2J paginators model, or null if the service has none.
     */
    public JsonNode paginatorsModel() {
        return paginatorsModel;
    }

    /**
     * The C2J waiters model, or null if the service has none.
     */
    public JsonNode waitersModel() {
        return waitersModel;
    }

    /**
     * The endpoint rule set, or null if the service has none.
     */
    public JsonNode endpointRuleSetModel() {
        return endpointRuleSetModel;
    }

    /**
     * The endpoint tests, or null if the service has none.
     */
    public JsonNode endpointTestsModel() {
        return endpointTestsModel;
    }

    /**
     * Builder for {@link C2jInput}.
     */
    public static final class Builder {
        private String serviceName;
        private JsonNode serviceModel;
        private JsonNode paginatorsModel;
        private JsonNode waitersModel;
        private JsonNode endpointRuleSetModel;
        private JsonNode endpointTestsModel;

        private Builder() {
        }

        /**
         * The service name used in the gap report and in errors. Required.
         */
        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        /**
         * The C2J service model, after C2J customizations. Required.
         */
        public Builder serviceModel(JsonNode serviceModel) {
            this.serviceModel = serviceModel;
            return this;
        }

        /**
         * The C2J paginators model. Optional.
         */
        public Builder paginatorsModel(JsonNode paginatorsModel) {
            this.paginatorsModel = paginatorsModel;
            return this;
        }

        /**
         * The C2J waiters model. Optional.
         */
        public Builder waitersModel(JsonNode waitersModel) {
            this.waitersModel = waitersModel;
            return this;
        }

        /**
         * The endpoint rule set. Optional.
         */
        public Builder endpointRuleSetModel(JsonNode endpointRuleSetModel) {
            this.endpointRuleSetModel = endpointRuleSetModel;
            return this;
        }

        /**
         * The endpoint tests. Optional.
         */
        public Builder endpointTestsModel(JsonNode endpointTestsModel) {
            this.endpointTestsModel = endpointTestsModel;
            return this;
        }

        /**
         * Build the input models.
         *
         * @throws NullPointerException if the service name or service model is missing
         */
        public C2jInput build() {
            return new C2jInput(this);
        }
    }
}
