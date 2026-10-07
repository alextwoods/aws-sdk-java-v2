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

package software.amazon.awssdk.codegen.poet.client.specs;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.config.customization.MetadataConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

/**
 * Codegen for the smithy-java serde option ({@code generateSmithyJavaSerde} in {@code customization.config}).
 *
 * <p>The generated client keeps the stock v2 pipeline. All this changes is where the marshaller and the
 * success response handler handed to {@code ClientExecutionParams} come from: a per-client
 * {@code smithySerde} object that wraps smithy-java's codecs, instead of the generated
 * {@code *RequestMarshaller} and v2's protocol factory. The v2 versions are still generated and still
 * reachable — they are what runs when the {@code aws.smithyJavaSerde} system property is {@code false} at
 * client construction, which is also what lets a benchmark compare the two from one build.
 *
 * <p>Operations that stay on v2's serde regardless: event streams (input or output), and for awsJson any
 * streaming operation. Error unmarshalling stays v2's for every operation.
 */
public final class SmithyJavaSerdeSpec {

    public static final String FIELD_NAME = "smithySerde";

    private static final String SERDE_PACKAGE = "software.amazon.awssdk.protocols.smithyjava";
    private static final ClassName SMITHY_JAVA_SERDE = ClassName.get(SERDE_PACKAGE, "SmithyJavaSerde");
    private static final ClassName AWS_JSON_PROTOCOL = ClassName.get(SERDE_PACKAGE, "SmithyAwsJsonProtocol");
    private static final ClassName REST_XML_PROTOCOL = ClassName.get(SERDE_PACKAGE, "SmithyRestXmlProtocol");

    private SmithyJavaSerdeSpec() {
    }

    /** Whether the client for {@code model} carries a {@code smithySerde} field at all. */
    public static boolean isEnabled(IntermediateModel model) {
        if (model.getCustomizationConfig() == null || !model.getCustomizationConfig().isGenerateSmithyJavaSerde()) {
            return false;
        }
        Protocol protocol = model.getMetadata().getProtocol();
        return protocol == Protocol.AWS_JSON || protocol == Protocol.REST_XML;
    }

    /** Whether {@code opModel}'s marshaller and response handler come from {@code smithySerde}. */
    public static boolean appliesTo(IntermediateModel model, OperationModel opModel) {
        if (!isEnabled(model) || opModel.getInputShape() == null) {
            return false;
        }
        if (opModel.hasEventStreamInput() || opModel.hasEventStreamOutput()) {
            return false;
        }
        if (model.getMetadata().getProtocol() == Protocol.AWS_JSON
            && (opModel.hasStreamingInput() || opModel.hasStreamingOutput())) {
            return false;
        }
        return true;
    }

    public static FieldSpec field(IntermediateModel model) {
        return FieldSpec.builder(protocolClass(model), FIELD_NAME, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    /**
     * The constructor statement. Must run after {@code clientConfiguration} is assigned; reads the switch
     * once, so a client's choice is fixed for its lifetime.
     */
    public static CodeBlock initialization(IntermediateModel model) {
        Metadata metadata = model.getMetadata();
        CodeBlock.Builder builder = CodeBlock.builder()
            .add("this.$N = $T.isEnabled()\n", FIELD_NAME, SMITHY_JAVA_SERDE)
            .add("    ? $T.builder().clientConfiguration(clientConfiguration)", protocolClass(model));
        if (metadata.getProtocol() == Protocol.AWS_JSON) {
            builder.add(".contentType($S)", jsonContentType(model))
                   .add(".targetPrefix($S)", metadata.getTargetPrefix())
                   .add(".serviceNamespace($S)", serviceNamespace(model))
                   .add(".hasAwsQueryCompatible($L)", metadata.getAwsQueryCompatible() != null);
        }
        return builder.add(".build()\n")
                      .add("    : null;\n")
                      .build();
    }

    /**
     * The marshaller expression for {@code withMarshaller}/{@code delegateMarshaller}: smithy's when the
     * client has it, otherwise {@code v2Marshaller}, unchanged.
     */
    public static CodeBlock marshaller(IntermediateModel model, OperationModel opModel, CodeBlock v2Marshaller) {
        if (!appliesTo(model, opModel)) {
            return v2Marshaller;
        }
        if (model.getMetadata().getProtocol() == Protocol.REST_XML) {
            return CodeBlock.of("($N != null ? $N.marshaller($T.instance(), $S, $S) : $L)",
                                FIELD_NAME, FIELD_NAME, operationClass(model, opModel),
                                xmlNamespace(opModel.getInputShape()), requestUri(opModel.getInputShape()),
                                v2Marshaller);
        }
        return CodeBlock.of("($N != null ? $N.marshaller($T.instance()) : $L)",
                            FIELD_NAME, FIELD_NAME, operationClass(model, opModel), v2Marshaller);
    }

    // The same request URI v2's generated marshaller puts in OperationInfo (QueryMarshallerSpec).
    private static String requestUri(ShapeModel inputShape) {
        return inputShape.getMarshaller() == null ? null : inputShape.getMarshaller().getRequestUri();
    }

    // The same namespace v2's generated marshaller puts in XML_NAMESPACE_ATTRIBUTE (QueryMarshallerSpec).
    private static String xmlNamespace(ShapeModel inputShape) {
        if (inputShape.getMarshaller() != null && inputShape.getMarshaller().getXmlNameSpaceUri() != null) {
            return inputShape.getMarshaller().getXmlNameSpaceUri();
        }
        Set<String> uris = inputShape.getMembers().stream()
                                     .map(MemberModel::getXmlNameSpaceUri)
                                     .filter(Objects::nonNull)
                                     .collect(Collectors.toSet());
        if (uris.size() > 1) {
            throw new IllegalStateException("Request " + inputShape.getShapeName() + " has more than one XML namespace");
        }
        return uris.isEmpty() ? null : uris.iterator().next();
    }

    /** The success response handler expression: smithy's when the client has it, otherwise {@code v2Handler}. */
    public static CodeBlock responseHandler(IntermediateModel model, OperationModel opModel, CodeBlock v2Handler) {
        if (!appliesTo(model, opModel)) {
            return v2Handler;
        }
        return CodeBlock.of("($N != null ? $N.$L($T.instance()) : $L)",
                            FIELD_NAME, FIELD_NAME,
                            opModel.hasStreamingOutput() ? "streamingResponseHandler" : "responseHandler",
                            operationClass(model, opModel), v2Handler);
    }

    /**
     * The combined (success-or-error) handler expression for rest-xml. {@code v2Factory} is a non-capturing
     * lambda from the protocol factory to v2's combined handler: smithy's handler builds it once, lazily,
     * for the responses it hands back to v2 (errors, and S3's 200-with-an-error-body).
     */
    public static CodeBlock combinedResponseHandler(IntermediateModel model, OperationModel opModel,
                                                    CodeBlock v2Handler, CodeBlock v2Factory) {
        if (!appliesTo(model, opModel)) {
            return v2Handler;
        }
        return CodeBlock.of("($N != null ? $N.combinedResponseHandler($T.instance(), protocolFactory, $L) : $L)",
                            FIELD_NAME, FIELD_NAME, operationClass(model, opModel), v2Factory, v2Handler);
    }

    public static ClassName operationClass(IntermediateModel model, OperationModel opModel) {
        String operationsPackage = model.getMetadata().getFullModelPackageName().replace(".model", ".operations");
        return ClassName.get(operationsPackage, opModel.getOperationName() + "Operation");
    }

    private static ClassName protocolClass(IntermediateModel model) {
        return model.getMetadata().getProtocol() == Protocol.AWS_JSON ? AWS_JSON_PROTOCOL : REST_XML_PROTOCOL;
    }

    // Mirrors BaseAwsJsonProtocolFactory#getContentType: the customization's content type, then the model's,
    // then the awsJson default for the protocol version.
    private static String jsonContentType(IntermediateModel model) {
        MetadataConfig custom = model.getCustomizationConfig().getCustomServiceMetadata();
        if (custom != null && custom.getContentType() != null) {
            return custom.getContentType();
        }
        if (model.getMetadata().getContentType() != null) {
            return model.getMetadata().getContentType();
        }
        return "application/x-amz-json-" + model.getMetadata().getJsonVersion();
    }

    // Same namespace AwsServiceModel gives the generated shape ids.
    private static String serviceNamespace(IntermediateModel model) {
        return "com.amazonaws." + model.getMetadata().getServiceName().toLowerCase(java.util.Locale.US);
    }
}
