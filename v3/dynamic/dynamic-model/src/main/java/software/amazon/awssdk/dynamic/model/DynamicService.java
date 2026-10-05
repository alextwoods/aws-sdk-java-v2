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

package software.amazon.awssdk.dynamic.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.PaginatedIndex;
import software.amazon.smithy.model.knowledge.PaginationInfo;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.waiters.WaitableTrait;
import software.amazon.smithy.waiters.Waiter;

/**
 * One service of a runtime model with its runtime schemas and dynamic operations (Requirements 14.1, 14.6, 14.7,
 * 14.17).
 *
 * <p>The schemas are built from the service's codegen view of the runtime model, the view {@code aws-codegen} generates
 * from: service errors copied to every operation, and mixins flattened into the shapes that use them and removed.
 * {@link #schemaIndex()} exposes them exactly as a generated service's {@code ServiceSchemaIndex} does, including
 * enum and intEnum member schemas and every unknown trait by exact trait ID. Each operation of the service closure
 * has one {@link DynamicAwsOperation}, found by its exact modeled name. Paginated operations have a
 * {@link DynamicPagination} and modeled waiters a {@link DynamicWaiterDefinition}, read from the same codegen view
 * the generator reads them from.
 *
 * <p>Instances are created once per service by {@link DynamicModel}, are immutable and safe for concurrent use.
 */
public final class DynamicService {

    private final ShapeId id;
    private final Model model;
    private final ServiceSchemaIndex schemaIndex;
    private final SortedMap<String, DynamicAwsOperation> operations;
    private final List<String> operationNames;
    private final SortedMap<String, DynamicPagination> paginations;
    private final List<String> paginatedOperationNames;
    private final SortedMap<String, DynamicWaiterDefinition> waiters;
    private final List<String> waiterNames;

    private DynamicService(ShapeId id, Model model, ServiceSchemaIndex schemaIndex,
                           SortedMap<String, DynamicAwsOperation> operations,
                           SortedMap<String, DynamicPagination> paginations,
                           SortedMap<String, DynamicWaiterDefinition> waiters) {
        this.id = id;
        this.model = model;
        this.schemaIndex = schemaIndex;
        this.operations = Collections.unmodifiableSortedMap(operations);
        this.operationNames = List.copyOf(operations.keySet());
        this.paginations = Collections.unmodifiableSortedMap(paginations);
        this.paginatedOperationNames = List.copyOf(paginations.keySet());
        this.waiters = Collections.unmodifiableSortedMap(waiters);
        this.waiterNames = List.copyOf(waiters.keySet());
    }

    /** Converts the service {@code id} of the assembled runtime model {@code runtimeModel}. */
    static DynamicService of(Model runtimeModel, ShapeId id) {
        ModelTransformer transformer = ModelTransformer.create();
        ServiceShape original = runtimeModel.expectShape(id, ServiceShape.class);
        Model model = transformer.flattenAndRemoveMixins(transformer.copyServiceErrorsToOperations(runtimeModel,
                                                                                                    original));
        ServiceShape service = model.expectShape(id, ServiceShape.class);
        ServiceSchemaIndex index = ServiceSchemaConverter.convert(model, service);

        OperationIndex operationIndex = OperationIndex.of(model);
        PaginatedIndex paginatedIndex = PaginatedIndex.of(model);
        SortedMap<String, DynamicAwsOperation> operations = new TreeMap<>();
        SortedMap<String, DynamicPagination> paginations = new TreeMap<>();
        SortedMap<String, DynamicWaiterDefinition> waiters = new TreeMap<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            String name = operation.getId().getName(service);
            paginatedIndex.getPaginationInfo(service, operation)
                          .filter(info -> info.getInputTokenMember() != null)
                          .ifPresent(info -> paginations.put(name, pagination(name, info)));
            for (Map.Entry<String, Waiter> waiter : operation.getTrait(WaitableTrait.class)
                                                             .map(WaitableTrait::getWaiters)
                                                             .orElse(Map.of()).entrySet()) {
                DynamicWaiterDefinition definition = new DynamicWaiterDefinition(waiter.getKey(), name,
                                                                                 waiter.getValue());
                if (waiters.putIfAbsent(waiter.getKey(), definition) != null) {
                    throw new IllegalStateException("Service " + id + " has two waiters named " + waiter.getKey());
                }
            }
            List<ShapeId> errors = operationIndex.getErrors(service, operation).stream()
                                                 .map(StructureShape::getId).sorted().toList();
            OperationSchemas.Builder schemas = OperationSchemas.builder(
                    index.schema(operation.getId()).orElseThrow())
                .input(ServiceSchemaConverter.target(index, operation.getInputShape()))
                .output(ServiceSchemaConverter.target(index, operation.getOutputShape()));
            List<Schema> errorSchemas = new ArrayList<>();
            errors.forEach(error -> errorSchemas.add(ServiceSchemaConverter.target(index, error)));
            schemas.errors(errorSchemas.toArray(Schema[]::new));
            streamMember(model, operation.getInputShape()).ifPresent(schemas::inputStreamMember);
            streamMember(model, operation.getOutputShape()).ifPresent(schemas::outputStreamMember);

            AwsOperationDescriptor descriptor = AwsOperationDescriptor.builder()
                                                                      .id(operation.getId())
                                                                      .service(id)
                                                                      .input(operation.getInputShape())
                                                                      .output(operation.getOutputShape())
                                                                      .errors(errors.toArray(ShapeId[]::new))
                                                                      .schemas(schemas.build())
                                                                      .build();
            DynamicAwsOperation dynamic = new DynamicAwsOperation(name, descriptor,
                                                                  index.unknownTraits(operation.getId()));
            if (operations.putIfAbsent(name, dynamic) != null) {
                throw new IllegalStateException("Service " + id + " has two operations named " + name);
            }
        }
        return new DynamicService(id, model, index, operations, paginations, waiters);
    }

    /** The pagination of {@code operation}, as {@code aws-codegen}'s paginator plan reads it. */
    private static DynamicPagination pagination(String operation, PaginationInfo info) {
        return new DynamicPagination(operation, info.getInputTokenMember().getMemberName(),
                                     memberNames(info.getOutputTokenMemberPath()),
                                     memberNames(info.getItemsMemberPath()),
                                     info.getPageSizeMember().map(MemberShape::getMemberName).orElse(null));
    }

    private static List<String> memberNames(List<MemberShape> path) {
        return path == null ? List.of() : path.stream().map(MemberShape::getMemberName).toList();
    }

    /** Absolute shape ID of the service. */
    public ShapeId id() {
        return id;
    }

    /** The service schema. */
    public Schema schema() {
        return schemaIndex.service();
    }

    /**
     * The codegen view of the runtime model the schemas were built from (service errors copied to operations, mixins
     * flattened). The runtime model as loaded is {@link DynamicModel#model()}.
     */
    public Model model() {
        return model;
    }

    /**
     * The exact-ID view of every schema of the service closure: the service, operations, types, other shapes,
     * structure/union/list/map and enum/intEnum member schemas, and every unknown trait by exact trait ID.
     */
    public ServiceSchemaIndex schemaIndex() {
        return schemaIndex;
    }

    /** The modeled operation names of the service, in lexical order. */
    public List<String> operationNames() {
        return operationNames;
    }

    /** Every operation of the service by exact modeled name, in lexical order; unmodifiable. */
    public SortedMap<String, DynamicAwsOperation> operations() {
        return operations;
    }

    /**
     * The operation with exactly the modeled name {@code name} (case-sensitive; Requirement 14.6).
     *
     * @throws DynamicModelException ({@link DynamicModelException.Reason#UNKNOWN_OPERATION}) naming this service, the
     *         unknown name and every operation name in lexical order, if the service has no such operation
     *         (Requirement 14.7)
     */
    public DynamicAwsOperation operation(String name) {
        Objects.requireNonNull(name, "name");
        DynamicAwsOperation operation = operations.get(name);
        if (operation == null) {
            throw DynamicModelException.unknownOperation(id, name, operationNames);
        }
        return operation;
    }

    /** The names of the service's paginated operations (input and output token modeled), in lexical order. */
    public List<String> paginatedOperationNames() {
        return paginatedOperationNames;
    }

    /**
     * The pagination of the operation with exactly the modeled name {@code operationName}.
     *
     * @throws DynamicModelException ({@link DynamicModelException.Reason#UNKNOWN_OPERATION}) if the service has no
     *         such operation, or ({@link DynamicModelException.Reason#NOT_PAGINATED}) naming the operation and every
     *         paginated operation name in lexical order if it is not paginated
     */
    public DynamicPagination pagination(String operationName) {
        operation(operationName);
        DynamicPagination pagination = paginations.get(operationName);
        if (pagination == null) {
            throw DynamicModelException.notPaginated(id, operationName, paginatedOperationNames);
        }
        return pagination;
    }

    /** The names of every modeled waiter of the service's operations, in lexical order. */
    public List<String> waiterNames() {
        return waiterNames;
    }

    /**
     * The modeled waiter with exactly the name {@code name} (case-sensitive).
     *
     * @throws DynamicModelException ({@link DynamicModelException.Reason#UNKNOWN_WAITER}) naming this service, the
     *         unknown name and every waiter name in lexical order, if the service has no such waiter
     */
    public DynamicWaiterDefinition waiter(String name) {
        Objects.requireNonNull(name, "name");
        DynamicWaiterDefinition waiter = waiters.get(name);
        if (waiter == null) {
            throw DynamicModelException.unknownWaiter(id, name, waiterNames);
        }
        return waiter;
    }

    @Override
    public String toString() {
        return "DynamicService[" + id + "]";
    }

    /** The member of {@code structure} whose target has the {@code @streaming} trait (Smithy allows at most one). */
    private static Optional<String> streamMember(Model model, ShapeId structure) {
        for (Map.Entry<String, MemberShape> member : model.expectShape(structure).getAllMembers().entrySet()) {
            if (model.expectShape(member.getValue().getTarget()).hasTrait(StreamingTrait.class)) {
                return Optional.of(member.getKey());
            }
        }
        return Optional.empty();
    }
}
