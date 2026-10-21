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

package software.amazon.awssdk.dynamicclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.dynamicclient.schema.SchemaConverter;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.traits.AwsQueryErrorTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * An operation of the service a {@link DynamicClient} calls, created from the model: its schemas, its error registry,
 * its effective auth schemes and the service it belongs to. Inputs and outputs are {@link DynamicRequest}s and
 * {@link DynamicResponse}s, whose documents have the operation's input and output schemas.
 *
 * <p>A client creates each operation once, on the first call or {@link DynamicClient#getOperation(String)} that names
 * it, and returns the same instance afterwards. The request pipeline exposes that instance to interceptors as
 * {@code SdkExecutionAttribute.API_OPERATION}, and the client protocol serializes the call with it.
 *
 * <p>Instances are immutable and safe to use from multiple threads.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.dynamicclient.DynamicOperation}.
 */
@SdkPublicApi
@ThreadSafe
public final class DynamicOperation implements ApiOperation<DynamicRequest, DynamicResponse> {
    private final Schema schema;
    private final Schema inputSchema;
    private final Schema outputSchema;
    private final List<Schema> errorSchemas;
    private final ErrorRegistry errorRegistry;
    private final List<ShapeId> effectiveAuthSchemes;
    private final ApiService service;
    private final ShapeId serviceId;

    private DynamicOperation(Schema schema, Schema inputSchema, Schema outputSchema, List<Schema> errorSchemas,
                             List<ShapeId> effectiveAuthSchemes, ApiService service) {
        this.schema = schema;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.errorSchemas = errorSchemas;
        this.effectiveAuthSchemes = effectiveAuthSchemes;
        this.service = service;
        this.serviceId = service.schema().id();
        this.errorRegistry = errorRegistry(errorSchemas, serviceId);
    }

    /**
     * Creates the operation of a model operation shape.
     *
     * @param operation the operation shape, in the closure of the client's service
     * @param serviceShape the client's service shape, whose errors every operation can return
     * @param converter the client's schema converter
     * @param effectiveAuthSchemes the operation's effective auth scheme IDs, in priority order
     * @param service the client's service
     */
    static DynamicOperation create(OperationShape operation, ServiceShape serviceShape, SchemaConverter converter,
                                   List<ShapeId> effectiveAuthSchemes, ApiService service) {
        Set<software.amazon.smithy.model.shapes.ShapeId> errorIds = new LinkedHashSet<>(operation.getErrors());
        errorIds.addAll(serviceShape.getErrors());
        List<Schema> errors = new ArrayList<>(errorIds.size());
        for (software.amazon.smithy.model.shapes.ShapeId errorId : errorIds) {
            errors.add(converter.getSchema(errorId));
        }
        return new DynamicOperation(converter.getSchema(operation),
                                    converter.getSchema(operation.getInputShape()),
                                    converter.getSchema(operation.getOutputShape()),
                                    Collections.unmodifiableList(errors),
                                    Collections.unmodifiableList(new ArrayList<>(effectiveAuthSchemes)),
                                    service);
    }

    /**
     * Creates a builder of the operation's input, whose {@code build()} returns a request with the input document.
     */
    @Override
    public ShapeBuilder<DynamicRequest> inputBuilder() {
        return new InputBuilder(SchemaConverter.createDocumentBuilder(inputSchema, serviceId));
    }

    /**
     * Creates a builder of the operation's output, whose {@code build()} returns a response with the output document.
     * It is also a {@link DynamicResponse.Builder}, so a protocol attaches the HTTP response and response metadata.
     */
    @Override
    public ShapeBuilder<DynamicResponse> outputBuilder() {
        return DynamicResponse.deserializingBuilder(SchemaConverter.createDocumentBuilder(outputSchema, serviceId));
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public Schema inputSchema() {
        return inputSchema;
    }

    @Override
    public Schema outputSchema() {
        return outputSchema;
    }

    /**
     * Returns the registry of the errors the operation can return: the operation's errors, then the service's, as in
     * {@link #errorSchemas()}. Each entry creates a {@link DocumentException.Builder} that reads the error's members
     * into a document. The registry is an {@link ErrorRegistry} whose code index holds the custom codes of the errors'
     * {@code aws.protocols#awsQueryError} traits.
     *
     * <p>When an error is resolved, the operation's errors take precedence over the service's: a discriminator or
     * custom code that matches both resolves to the operation's error, as {@code TypeRegistry.compose(operationErrors,
     * serviceErrors)} would. An error response that resolves to no entry, for example the error of another operation,
     * becomes a plain {@code AwsServiceException}.
     */
    @Override
    public TypeRegistry errorRegistry() {
        return errorRegistry;
    }

    /**
     * Returns the schemas of the errors the operation can return, in model order: the operation's errors, then the
     * service's, as in a generated client's operation.
     */
    @Override
    public List<Schema> errorSchemas() {
        return errorSchemas;
    }

    @Override
    public List<ShapeId> effectiveAuthSchemes() {
        return effectiveAuthSchemes;
    }

    @Override
    public ApiService service() {
        return service;
    }

    @Override
    public String toString() {
        return ToString.builder("DynamicOperation").add("schema", schema.id()).build();
    }

    /**
     * Registers the errors in order; {@code errorSchemas} has no duplicate ShapeIds, so an error that both the
     * operation and the service list has one entry. The first error with a custom code wins, so the operation's errors
     * take precedence over the service's (Requirement 29.4). One registry, rather than
     * {@code TypeRegistry.compose}, because a composed registry is not an {@link ErrorRegistry} and would lose the
     * custom code index that the query protocols resolve first.
     */
    private static ErrorRegistry errorRegistry(List<Schema> errorSchemas, ShapeId serviceId) {
        ErrorRegistry.Builder registry = ErrorRegistry.builder();
        for (Schema error : errorSchemas) {
            registry.putType(error.id(), DocumentException.class,
                             () -> DocumentException.schemaGuidedBuilder(error, serviceId));
            AwsQueryErrorTrait queryError = error.getTrait(TraitKey.AWS_QUERY_ERROR_TRAIT);
            if (queryError != null && queryError.getCode() != null && !queryError.getCode().trim().isEmpty()) {
                registry.errorCode(queryError.getCode(), error.id());
            }
        }
        return registry.build();
    }

    /**
     * Builds a {@link DynamicRequest} from a document builder of the input schema.
     */
    private static final class InputBuilder implements ShapeBuilder<DynamicRequest> {
        private final ShapeBuilder<StructDocument> documentBuilder;

        private InputBuilder(ShapeBuilder<StructDocument> documentBuilder) {
            this.documentBuilder = documentBuilder;
        }

        @Override
        public DynamicRequest build() {
            return DynamicRequest.builder().document(documentBuilder.build()).build();
        }

        @Override
        public ShapeBuilder<DynamicRequest> deserialize(ShapeDeserializer decoder) {
            documentBuilder.deserialize(decoder);
            return this;
        }

        @Override
        public ShapeBuilder<DynamicRequest> deserializeMember(ShapeDeserializer decoder, Schema member) {
            documentBuilder.deserializeMember(decoder, member);
            return this;
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            documentBuilder.setMemberValue(member, value);
        }

        @Override
        public ShapeBuilder<DynamicRequest> errorCorrection() {
            documentBuilder.errorCorrection();
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return documentBuilder.sdkSchema();
        }
    }
}
