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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.awscodegen.schema.SchemaSymbols;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Render plan of one file of the service's primary type registry: the public {@code <Service>TypeRegistry} holder,
 * or one of its package-private partitions {@code <Service>TypeRegistry1..n} when the service has more than
 * {@code partitionSize} registered types.
 *
 * <p>Registered types are every structure (including requests, responses and modeled errors) and union of the service
 * closure outside the Smithy prelude, sorted by absolute shape ID (Requirement 6.6). Enums and intEnums are not
 * aggregates and have no builder, so they are not registered.
 *
 * @param type symbol of the generated class
 * @param holder whether this is the public holder ({@code false} for a partition)
 * @param service absolute shape ID of the generated service
 * @param entries the types this file registers itself: all of them for a single-file registry, none for a holder
 *        with partitions, and one partition's for a partition
 * @param partitions the holder's partitions in order (empty for a single-file registry and for a partition)
 */
record TypeRegistryPlan(Symbol type, boolean holder, ShapeId service, List<Entry> entries, List<Symbol> partitions)
    implements RenderPlan {

    /** Default number of types per partition; far below the JVM limits on method size and constants. */
    static final int DEFAULT_PARTITION_SIZE = 200;

    TypeRegistryPlan {
        entries = List.copyOf(entries);
        partitions = List.copyOf(partitions);
    }

    /**
     * One registered type.
     *
     * @param shape absolute shape ID of the type
     * @param javaType symbol of the generated Java class
     * @param schemaOwner class declaring the type's exported schema (the service's schema holder)
     * @param schemaField name of the exported schema constant
     */
    record Entry(ShapeId shape, Symbol javaType, Symbol schemaOwner, String schemaField) {
    }

    /** The holder's plan first, then the plan of each partition in order. */
    static List<TypeRegistryPlan> of(AwsCodegenContext context, int partitionSize) {
        if (partitionSize < 1) {
            throw new IllegalArgumentException("partitionSize must be positive");
        }
        Symbol schemaHolder = AwsSymbolProvider.schemaHolder(context.service(), context.settings());
        List<Entry> entries = new ArrayList<>();
        for (Shape shape : CodegenOrdering.byShapeId(new Walker(context.model()).walkShapes(context.service()))) {
            if (!(shape.isStructureShape() || shape.isUnionShape())
                || shape.getId().getNamespace().equals("smithy.api")) {
                continue;
            }
            SchemaSymbols.Reference schema = SchemaSymbols.exported(shape.getId(), context.service(), schemaHolder);
            entries.add(new Entry(shape.getId(), context.symbolProvider().toSymbol(shape), schema.owner(),
                                  schema.field()));
        }
        Symbol holder = AwsSymbolProvider.typeRegistry(context.service(), context.settings());
        ShapeId service = context.service().getId();
        if (entries.size() <= partitionSize) {
            return List.of(new TypeRegistryPlan(holder, true, service, entries, List.of()));
        }
        List<TypeRegistryPlan> parts = new ArrayList<>();
        List<Symbol> symbols = new ArrayList<>();
        for (int from = 0, number = 1; from < entries.size(); from += partitionSize, number++) {
            Symbol partition = AwsSymbolProvider.typeRegistryPartition(holder, number);
            symbols.add(partition);
            parts.add(new TypeRegistryPlan(partition, false, service,
                                           entries.subList(from, Math.min(entries.size(), from + partitionSize)),
                                           List.of()));
        }
        List<TypeRegistryPlan> plans = new ArrayList<>();
        plans.add(new TypeRegistryPlan(holder, true, service, List.of(), symbols));
        plans.addAll(parts);
        return plans;
    }
}
