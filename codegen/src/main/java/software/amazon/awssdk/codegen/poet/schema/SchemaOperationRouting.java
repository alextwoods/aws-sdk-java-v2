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

package software.amazon.awssdk.codegen.poet.schema;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Decides which generated client methods call {@code clientHandler.execute(new ClientExecutionParams<>()
 * .withOperation(<Svc>Operations.OP)...)}, the schema-based pipeline path, instead of passing a marshaller and response
 * handlers (task 3.32, design A.18 "Generated client methods", Requirements 23.6, 30.3, 37.2).
 *
 * <p>A client is routed when it has runtime protocol selection ({@link ClientProtocolSelection}): its default protocol
 * has a {@code ClientProtocolFactory}, so the configuration's {@code CLIENT_PROTOCOL} can serialize every call. In
 * phase 2 that is CloudWatch (rpcv2Cbor) and the rpcv2Cbor Protocol_Test_Services. Within such a client, an operation
 * is routed unless it needs something the new path or the generated classes do not have yet
 * ({@link #legacyReason(OperationModel)}); those operations keep their marshaller and response handlers.
 */
@SdkInternalApi
public final class SchemaOperationRouting {
    private final ClassName operationsClass;
    private final Map<ShapeId, String> operationFields;
    private final SchemaSerdeModel serde;
    private final Set<ShapeId> errorsWithoutSerde;

    private SchemaOperationRouting(OperationsClassSpec operations, SchemaSerdeModel serde) {
        this.operationsClass = operations.className();
        this.operationFields = operations.operationFields();
        this.serde = serde;
        Set<ShapeId> withSerde = new HashSet<>();
        for (SchemaSerdeModel.Entry entry : serde.entries().values()) {
            if (entry.shapeId() != null) {
                withSerde.add(entry.shapeId());
            }
        }
        Set<ShapeId> missing = new TreeSet<>(operations.registeredErrors());
        missing.removeAll(withSerde);
        this.errorsWithoutSerde = missing;
    }

    /**
     * @param model the intermediate model
     * @param schemaCodegen the model's prepared Schema_Generator, or null if the model gets no schemas
     * @return the routing of the model's client, or empty if no operation of the client is routed (the client has no
     *     runtime protocol selection or no schemas)
     */
    public static Optional<SchemaOperationRouting> forClient(IntermediateModel model, SchemaCodegen schemaCodegen) {
        if (schemaCodegen == null || !ClientProtocolSelection.forModel(model).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new SchemaOperationRouting(schemaCodegen.operations(), schemaCodegen.serde()));
    }

    /**
     * Whether the model is a schema-based service: its client is routed and every operation of the model, including the
     * event-stream operations that only the async client has methods for, runs on the schema-based pipeline path. Such
     * a service generates no per-operation serde components (task 5.1, Requirements 17.12, 23.6, 36.4): no
     * {@code transform} marshallers, and no legacy protocol factory configuration in its client builder.
     *
     * @param model the intermediate model
     * @param schemaCodegen the model's prepared Schema_Generator, or null if the model gets no schemas
     * @return whether no operation of the model keeps a marshaller and response handlers
     */
    public static boolean routesEveryOperation(IntermediateModel model, SchemaCodegen schemaCodegen) {
        return forClient(model, schemaCodegen).map(r -> !r.hasLegacyOperations(model.getOperations().values()))
                                              .orElse(false);
    }

    /**
     * @param operation an operation of the client
     * @return whether its client methods run on the schema-based pipeline path
     */
    public boolean routes(OperationModel operation) {
        return legacyReason(operation) == null;
    }

    /**
     * @param operations the operations the client class generates methods for
     * @return whether any of them keeps the marshaller and response handlers
     */
    public boolean hasLegacyOperations(Collection<OperationModel> operations) {
        return operations.stream().anyMatch(o -> !routes(o));
    }

    /**
     * Why an operation stays on the legacy path, or null if it is routed.
     *
     * <ul>
     *     <li>Event-stream input or output: the client protocols reject event streams (task 7.20).</li>
     *     <li>Streaming input: the new path binds no streaming request body yet (task 7.19).</li>
     *     <li>Streaming output: the client protocols cannot yet deserialize only the bound members of a streaming
     *     output, and {@code AsyncApiCallPipeline} rejects {@code AsyncResponseTransformer} (task 7.19).</li>
     *     <li>The input or output class has no schema serde members ({@link SchemaSerdeModel}, task 3.7), so the
     *     protocol cannot serialize or build it.</li>
     *     <li>An error class of the service has no schema serde members: the service-wide Error_Registry can resolve
     *     any of its errors for any operation (Requirement 23.7), and it cannot build that class.</li>
     * </ul>
     *
     * @param operation an operation of the client
     * @return the reason, or null
     */
    public String legacyReason(OperationModel operation) {
        if (operation.hasEventStreamInput() || operation.hasEventStreamOutput()) {
            return "event streams are not supported by the client protocols yet (task 7.20)";
        }
        if (operation.hasStreamingInput()) {
            return "streaming request bodies are not bound by the new pipeline path yet (task 7.19)";
        }
        if (operation.hasStreamingOutput()) {
            return "streaming outputs are not deserialized by the client protocols yet (task 7.19)";
        }
        if (operation.operationId() == null || !operationFields.containsKey(operation.operationId())) {
            return "the operation has no field in " + operationsClass.simpleName();
        }
        if (serde.operation(operation.operationId()) == null) {
            return "the input or output class has no schema serde members";
        }
        if (!errorsWithoutSerde.isEmpty()) {
            return "the error classes of " + errorsWithoutSerde + " have no schema serde members";
        }
        return null;
    }

    /**
     * @param operation a routed operation
     * @return the expression of its {@code ApiOperation}, such as {@code CloudWatchOperations.GET_DASHBOARD}
     * @throws IllegalArgumentException if the operation is not routed
     */
    public CodeBlock operationReference(OperationModel operation) {
        String reason = legacyReason(operation);
        if (reason != null) {
            throw new IllegalArgumentException("Operation " + operation.getOperationName() + " is not routed through "
                                               + operationsClass.simpleName() + ": " + reason);
        }
        return CodeBlock.of("$T.$L", operationsClass, operationFields.get(operation.operationId()));
    }

    /**
     * @param operation a routed operation
     * @return whether {@code ApiOperation.name()}, which {@code withOperation} sets as the operation name, differs from
     *     the client's operation name, so the generated call must set the client's name after it
     */
    public boolean renamesOperation(OperationModel operation) {
        return operation.operationId() != null
               && !operation.operationId().getName().equals(operation.getOperationName());
    }

    /**
     * @return the {@code <Svc>Operations} class
     */
    public ClassName operationsClass() {
        return operationsClass;
    }
}
