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

package software.amazon.awssdk.smithy.runtime.fixture;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The generated-style service descriptor, static operation descriptor, and primary type registry for the fixture
 * service. Every reference is a static constant; nothing is resolved by name or loaded from a model at runtime.
 */
public final class WidgetService implements ApiService {

    public static final WidgetService INSTANCE = new WidgetService();

    public static final Schema SCHEMA = Schema.createService(ShapeId.from("software.amazon.awssdk.fixture#WidgetService"));

    /** The primary type registry of the fixture service package. */
    public static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(Widget.ID, Widget.class, Widget::builder)
        .build();

    /** The static operation descriptor for {@code PutWidget}. */
    public static final ApiOperation<Widget, Widget> PUT_WIDGET = new PutWidget();

    private WidgetService() {
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    private static final class PutWidget implements ApiOperation<Widget, Widget> {
        private static final Schema SCHEMA = Schema.createOperation(
            ShapeId.from("software.amazon.awssdk.fixture#PutWidget"));

        @Override
        public ShapeBuilder<Widget> inputBuilder() {
            return Widget.builder();
        }

        @Override
        public ShapeBuilder<Widget> outputBuilder() {
            return Widget.builder();
        }

        @Override
        public Schema schema() {
            return SCHEMA;
        }

        @Override
        public Schema inputSchema() {
            return Widget.SCHEMA;
        }

        @Override
        public Schema outputSchema() {
            return Widget.SCHEMA;
        }

        @Override
        public TypeRegistry errorRegistry() {
            return TypeRegistry.empty();
        }

        @Override
        public List<ShapeId> effectiveAuthSchemes() {
            return List.of();
        }

        @Override
        public List<Schema> errorSchemas() {
            return List.of();
        }

        @Override
        public ApiService service() {
            return INSTANCE;
        }
    }
}
