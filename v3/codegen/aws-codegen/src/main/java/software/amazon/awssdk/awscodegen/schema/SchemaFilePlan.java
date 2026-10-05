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

package software.amazon.awssdk.awscodegen.schema;

import java.util.List;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Render plan of one schema file: the service's {@code <Service>Schemas} holder, or one of its partitions when the
 * service's schemas do not fit one class.
 *
 * <p>Like every render plan it holds only symbols, shape IDs, names and pre-rendered Java code, never a model, shape,
 * trait or node (see {@code RenderPlan} in the generator package); {@link SchemaPlanner} computes it from the model
 * for the one file and it is discarded after rendering.
 *
 * @param type symbol of the generated class
 * @param holder whether this is the public holder ({@code false} for a package-private partition)
 * @param service absolute shape ID of the generated service
 * @param fields the schema field declarations and static initializer blocks, in initialization order
 * @param registrations statements that register this file's schemas with the {@code ServiceSchemaIndex.Builder}
 *        named {@code builder}; for a holder with partitions, calls to each partition's {@code register}
 * @param serviceField reference, from the holder, to the service schema field; empty for a partition
 */
record SchemaFilePlan(Symbol type, boolean holder, ShapeId service, List<Code> fields, List<Code> registrations,
                      String serviceField) implements SchemaRenderPlan {

    SchemaFilePlan {
        fields = List.copyOf(fields);
        registrations = List.copyOf(registrations);
    }

    /**
     * A Java source fragment: {@code template} is written through {@code AwsJavaWriter}, with one {@code $T}
     * placeholder per element of {@code types} (which the writer imports) and {@code $$} for a literal {@code $}.
     */
    record Code(String template, List<Symbol> types) {
        Code {
            types = List.copyOf(types);
        }
    }
}
