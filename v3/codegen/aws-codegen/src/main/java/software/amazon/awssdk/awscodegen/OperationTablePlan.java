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

package software.amazon.awssdk.awscodegen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.awscodegen.schema.SchemaSymbols;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Render plan of the service's static operation table, {@code <Service>Operations.java}: one constant per operation
 * in the service closure, each initialized with a direct reference to that operation's descriptor {@code INSTANCE}.
 *
 * @param table symbol of the generated table class
 * @param service absolute shape ID of the generated service
 * @param entries one entry per operation, sorted by absolute operation shape ID
 */
record OperationTablePlan(Symbol table, ShapeId service, List<Entry> entries) implements RenderPlan {

    OperationTablePlan {
        entries = List.copyOf(entries);
    }

    /**
     * One table constant.
     *
     * @param constant Java constant name
     * @param descriptor symbol of the operation's descriptor class
     * @param operation absolute shape ID of the operation
     */
    record Entry(String constant, Symbol descriptor, ShapeId operation) {
    }

    /**
     * Plans the table from the operations {@link TopDownIndex} finds in the service closure, including operations
     * bound to resources.
     *
     * @throws AwsCodegenException if two operations map to the same Java constant name
     */
    static OperationTablePlan of(AwsCodegenContext context) {
        List<OperationShape> operations = CodegenOrdering.byShapeId(
            TopDownIndex.of(context.model()).getContainedOperations(context.service()));
        Map<String, ShapeId> constants = new HashMap<>();
        List<Entry> entries = operations.stream().map(operation -> {
            String constant = constantName(operation.getId().getName(context.service()));
            ShapeId previous = constants.putIfAbsent(constant, operation.getId());
            if (previous != null) {
                throw new AwsCodegenException("Operations " + previous + " and " + operation.getId() + " of service "
                                              + context.service().getId() + " both map to the operation table "
                                              + "constant " + constant + ".");
            }
            return new Entry(constant, context.symbolProvider().toSymbol(operation), operation.getId());
        }).toList();
        return new OperationTablePlan(AwsSymbolProvider.operationTable(context.service(), context.settings()),
                                      context.service().getId(), entries);
    }

    /**
     * The constant for an operation's contextual (service-renamed) name: {@code GetObject} becomes {@code GET_OBJECT}
     * and {@code ListObjectsV2} becomes {@code LIST_OBJECTS_V2}.
     */
    static String constantName(String operationName) {
        return SchemaSymbols.constantName(operationName);
    }
}
