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

import java.util.List;
import software.amazon.awssdk.awscodegen.schema.SchemaSymbols;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Render plan of one static operation descriptor file, {@code <Name>Operation.java}.
 *
 * @param descriptor symbol of the generated descriptor class
 * @param operation absolute shape ID of the operation
 * @param service absolute shape ID of the generated service
 * @param input input structure, {@code smithy.api#Unit} when the operation has none
 * @param output output structure, {@code smithy.api#Unit} when the operation has none
 * @param errors every modeled error the operation can return in the service (operation and service errors), sorted by
 *        absolute shape ID so the order does not depend on model transforms or declaration order
 * @param operationSchema the exported operation schema
 * @param inputSchema the exported input schema ({@code Unit.SCHEMA} for {@code smithy.api#Unit})
 * @param outputSchema the exported output schema ({@code Unit.SCHEMA} for {@code smithy.api#Unit})
 * @param errorSchemas the exported schema of each error, in the order of {@code errors}
 * @param inputStreamMember the input member targeting a streaming blob or event stream, or empty
 * @param outputStreamMember the output member targeting a streaming blob or event stream, or empty
 */
record OperationDescriptorPlan(Symbol descriptor, ShapeId operation, ShapeId service, ShapeId input, ShapeId output,
                               List<ShapeId> errors, SchemaLink operationSchema, SchemaLink inputSchema,
                               SchemaLink outputSchema, List<SchemaLink> errorSchemas, String inputStreamMember,
                               String outputStreamMember) implements RenderPlan {

    OperationDescriptorPlan {
        errors = List.copyOf(errors);
        errorSchemas = List.copyOf(errorSchemas);
    }

    /**
     * A static schema field.
     *
     * @param owner the class declaring the field
     * @param field the field name
     */
    record SchemaLink(Symbol owner, String field) {
    }

    /** Plans the descriptor of {@code operation} directly from the context's model, operation index and symbols. */
    static OperationDescriptorPlan of(AwsCodegenContext context, OperationShape operation) {
        Model model = context.model();
        ServiceShape service = context.service();
        Symbol holder = AwsSymbolProvider.schemaHolder(service, context.settings());
        List<ShapeId> errors = CodegenOrdering.shapeIds(OperationIndex.of(model).getErrors(service, operation));
        return new OperationDescriptorPlan(context.symbolProvider().toSymbol(operation),
                                           operation.getId(),
                                           service.getId(),
                                           operation.getInputShape(),
                                           operation.getOutputShape(),
                                           errors,
                                           link(operation.getId(), service, holder),
                                           link(operation.getInputShape(), service, holder),
                                           link(operation.getOutputShape(), service, holder),
                                           errors.stream().map(error -> link(error, service, holder)).toList(),
                                           streamMember(model, operation.getInputShape()),
                                           streamMember(model, operation.getOutputShape()));
    }

    private static SchemaLink link(ShapeId shape, ServiceShape service, Symbol holder) {
        SchemaSymbols.Reference reference = SchemaSymbols.exported(shape, service, holder);
        return new SchemaLink(reference.owner(), reference.field());
    }

    /** The member of {@code structure} whose target has the {@code @streaming} trait (Smithy allows at most one). */
    private static String streamMember(Model model, ShapeId structure) {
        Shape shape = model.expectShape(structure);
        for (MemberShape member : CodegenOrdering.members(shape)) {
            if (model.expectShape(member.getTarget()).hasTrait(StreamingTrait.class)) {
                return member.getMemberName();
            }
        }
        return "";
    }
}
