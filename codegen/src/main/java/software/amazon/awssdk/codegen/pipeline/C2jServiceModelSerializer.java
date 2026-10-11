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

package software.amazon.awssdk.codegen.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.stree.JrSimpleTreeExtension;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.Authorizer;
import software.amazon.awssdk.codegen.model.service.ErrorTrait;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.RetryableTrait;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;

/**
 * Writes codegen's C2J {@link ServiceModel} POJO back to the C2J JSON form ({@code service-2.json}), so that a model
 * changed by C2J customizations (Codegen_Pipeline stage 2) can be handed to the C2J_Converter, which reads JSON trees.
 *
 * <p>The POJO is the source of truth: a C2J property that the POJO does not hold is not written. Writing the POJO of an
 * unmodified {@code service-2.json} converts to the same Smithy model as the file itself;
 * {@code C2jServiceModelRoundTripTest} checks this for the Prototype_Service_Set.
 *
 * <p>Property names follow the C2J file, not the POJO accessors. The mix-ins below rename the accessors whose names
 * differ from the C2J keys ({@code enumValues} is {@code enum}, {@code listMember} is {@code member}, and so on), hide
 * derived accessors, and expose fields that have no getter. {@code false}, {@code 0}, null and empty values are left out,
 * as C2J files leave them out; {@code min} and {@code max} keep an explicit {@code 0}, and
 * {@code awsQueryCompatible} keeps an empty object.
 */
@SdkInternalApi
public final class C2jServiceModelSerializer {

    private static final ObjectMapper MAPPER = createMapper();

    /** Reads the {@link TreeNode} values the POJO holds (context param values and paths), which are jackson-jr trees. */
    private static final JSON JR = JSON.builder().register(new JrSimpleTreeExtension()).build();

    private C2jServiceModelSerializer() {
    }

    /**
     * Write the service model as a C2J JSON tree.
     */
    public static JsonNode toTree(ServiceModel serviceModel) {
        try {
            // A string round trip rather than valueToTree: the jackson-jr tree values are written as raw JSON.
            return MAPPER.readTree(MAPPER.writeValueAsString(serviceModel));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write the C2J service model", e);
        }
    }

    /**
     * Read a C2J JSON tree into a service model POJO, with the same reader as {@code service-2.json} files.
     */
    public static ServiceModel fromTree(JsonNode tree) {
        try {
            return Jackson.load(ServiceModel.class, tree.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the C2J service model", e);
        }
    }

    private static ObjectMapper createMapper() {
        SimpleModule module = new SimpleModule("c2j");
        module.addSerializer(AuthType.class, new AuthTypeSerializer());
        module.addSerializer(TreeNode.class, new JrTreeSerializer());
        module.setMixInAnnotation(ServiceMetadata.class, ServiceMetadataMixIn.class);
        module.setMixInAnnotation(Shape.class, ShapeMixIn.class);
        module.setMixInAnnotation(Member.class, MemberMixIn.class);
        module.setMixInAnnotation(ErrorTrait.class, ErrorTraitMixIn.class);
        module.setMixInAnnotation(RetryableTrait.class, RetryableTraitMixIn.class);
        module.setMixInAnnotation(Authorizer.class, AuthorizerMixIn.class);

        return new ObjectMapper().registerModule(module)
                                 .setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT)
                                 .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /** C2J writes auth types in their file form ({@code v4}), not the enum constant name. */
    private static final class AuthTypeSerializer extends JsonSerializer<AuthType> {
        @Override
        public void serialize(AuthType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.value());
        }
    }

    private static final class JrTreeSerializer extends JsonSerializer<TreeNode> {
        @Override
        public void serialize(TreeNode value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value instanceof JsonNode) {
                gen.writeTree(value);
                return;
            }
            gen.writeRawValue(JR.asString(value));
        }
    }

    @SuppressWarnings("unused")
    private abstract static class ServiceMetadataMixIn {
        /** An empty {@code awsQueryCompatible} object still marks the service as query compatible. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        abstract Map<String, String> getAwsQueryCompatible();
    }

    @SuppressWarnings("unused")
    private abstract static class ShapeMixIn {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("min")
        private Long min;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("max")
        private Long max;

        @JsonProperty("retryable")
        private RetryableTrait retryable;

        @JsonProperty("enum")
        abstract List<String> getEnumValues();

        @JsonProperty("member")
        abstract Member getListMember();

        @JsonProperty("key")
        abstract Member getMapKeyType();

        @JsonProperty("value")
        abstract Member getMapValueType();

        @JsonIgnore
        abstract long getMin();

        @JsonIgnore
        abstract long getMax();

        @JsonIgnore
        abstract boolean isRetryable();

        @JsonIgnore
        abstract boolean isThrottling();
    }

    @SuppressWarnings("unused")
    private abstract static class MemberMixIn {
        @JsonProperty("jsonvalue")
        abstract boolean getJsonvalue();
    }

    @SuppressWarnings("unused")
    private abstract static class ErrorTraitMixIn {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("senderFault")
        abstract Boolean getSenderFault();
    }

    @SuppressWarnings("unused")
    private abstract static class RetryableTraitMixIn {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("throttling")
        abstract Boolean getThrottling();

        @JsonIgnore
        abstract boolean isThrottling();
    }

    @SuppressWarnings("unused")
    private abstract static class AuthorizerMixIn {
        @JsonProperty("placement")
        private Authorizer.Placement placement;

        @JsonIgnore
        abstract Object getTokenLocation();

        @JsonIgnore
        abstract String getTokenName();
    }
}
