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

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Every {@link RenderPlan}, and every plan of the schema package ({@code schema.SchemaRenderPlan}), is a file-local
 * value that cannot restate the model (Requirements 3.3, 3.4): it is a record whose components are symbols, shape IDs,
 * names, primitives, or lists/nested records of those. A plan that held a {@code Model}, {@code Shape}, trait,
 * {@code Node} or index would fail here.
 */
class RenderPlanRulesTest {

    private static final Set<Class<?>> ALLOWED_LEAVES = Set.of(Symbol.class, ShapeId.class, String.class,
                                                               boolean.class, int.class, long.class);

    @Test
    void everyRenderPlanIsARecordOfFileLocalValues() {
        Class<?>[] plans = RenderPlan.class.getPermittedSubclasses();
        assertEquals(Set.of(OperationDescriptorPlan.class, OperationTablePlan.class, ServiceMetadataPlan.class,
                            TypeRegistryPlan.class, EndpointParamsPlan.class, EndpointProviderPlan.class,
                            EndpointRulesPlan.class), Set.of(plans));
        List<String> violations = new ArrayList<>();
        Arrays.stream(plans).forEach(plan -> check(RenderPlan.class, plan, violations));
        assertEquals(List.of(), violations);
    }

    @Test
    void everySchemaRenderPlanIsARecordOfFileLocalValues() throws ClassNotFoundException {
        Class<?> root = Class.forName("software.amazon.awssdk.awscodegen.schema.SchemaRenderPlan");
        Class<?>[] plans = root.getPermittedSubclasses();
        assertEquals(Set.of("SchemaFilePlan"), Arrays.stream(plans).map(Class::getSimpleName).collect(toSet()));
        List<String> violations = new ArrayList<>();
        Arrays.stream(plans).forEach(plan -> check(root, plan, violations));
        assertEquals(List.of(), violations);
    }

    @Test
    void everyModelRenderPlanIsARecordOfFileLocalValues() throws ClassNotFoundException {
        Class<?> root = Class.forName("software.amazon.awssdk.awscodegen.model.ModelRenderPlan");
        Class<?>[] plans = root.getPermittedSubclasses();
        assertEquals(Set.of("ModelTypePlan", "EnumTypePlan", "ServiceExceptionPlan"),
                     Arrays.stream(plans).map(Class::getSimpleName).collect(toSet()));
        List<String> violations = new ArrayList<>();
        Arrays.stream(plans).forEach(plan -> check(root, plan, violations));
        assertEquals(List.of(), violations);
    }

    private static void check(Class<?> root, Class<?> type, List<String> violations) {
        if (!type.isRecord()) {
            violations.add(type.getName() + " is not a record");
            return;
        }
        for (RecordComponent component : type.getRecordComponents()) {
            checkType(root, type.getSimpleName() + "." + component.getName(), component.getGenericType(), violations);
        }
    }

    private static void checkType(Class<?> root, String where, Type type, List<String> violations) {
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() == List.class) {
            checkType(root, where + "[]", parameterized.getActualTypeArguments()[0], violations);
        } else if (type instanceof Class<?> c && ALLOWED_LEAVES.contains(c)) {
            return;
        } else if (type instanceof Class<?> c && c.isRecord() && c.getEnclosingClass() != null
                   && root.isAssignableFrom(c.getEnclosingClass())) {
            check(root, c, violations);
        } else {
            violations.add(where + " has type " + type.getTypeName());
        }
    }

    @Test
    void nestedPlanRecordsAreChecked() {
        List<String> violations = new ArrayList<>();
        check(RenderPlan.class, OperationTablePlan.Entry.class, violations);
        check(RenderPlan.class, OperationDescriptorPlan.SchemaLink.class, violations);
        check(RenderPlan.class, TypeRegistryPlan.Entry.class, violations);
        assertEquals(List.of(), violations);

        List<String> rejected = new ArrayList<>();
        checkType(RenderPlan.class, "x", software.amazon.smithy.model.Model.class, rejected);
        assertTrue(rejected.get(0).contains("software.amazon.smithy.model.Model"), rejected.toString());
    }
}
