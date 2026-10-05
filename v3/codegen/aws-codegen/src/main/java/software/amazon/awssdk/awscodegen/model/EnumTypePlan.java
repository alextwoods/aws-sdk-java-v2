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

package software.amazon.awssdk.awscodegen.model;

import java.util.List;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Render plan of one generated enum or intEnum type.
 *
 * @param type symbol of the generated Java enum
 * @param shape absolute ID of the enum shape
 * @param intEnum whether the shape is an intEnum (values are {@code Integer}) rather than an enum ({@code String})
 * @param schemaHolder the service's schema holder
 * @param schemaConstant name of the enum's exported schema constant in {@code schemaHolder}
 * @param constants the modeled values in modeled order
 */
record EnumTypePlan(Symbol type, ShapeId shape, boolean intEnum, Symbol schemaHolder, String schemaConstant,
                    List<Constant> constants) implements ModelRenderPlan {

    EnumTypePlan {
        constants = List.copyOf(constants);
    }

    /**
     * One modeled value.
     *
     * @param name Java constant name
     * @param value Java literal of the modeled value
     */
    record Constant(String name, String value) {
    }
}
