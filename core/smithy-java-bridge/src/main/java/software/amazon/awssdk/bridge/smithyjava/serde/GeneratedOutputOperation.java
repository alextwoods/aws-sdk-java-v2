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

package software.amazon.awssdk.bridge.smithyjava.serde;

import java.util.List;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Like {@link BridgeOutputOperation}, but its {@link #outputBuilder()} returns a <b>generated</b>
 * v2 builder directly (which already implements {@link ShapeBuilder}) — no {@code BridgeOutputBuilder}
 * wrapper. This is the deserialize counterpart of running the generated POJO as the
 * {@code SerializableStruct} on the serialize side: the response is deserialized straight into the
 * generated builder by the smithy-java HTTP-binding deserializer (full header/payload binding).
 *
 * <p>Header/status binding still uses the real (DynamicClient-resolved) {@code delegate}'s
 * {@code outputSchema()}; the body is deserialized by the generated builder's own {@code $SCHEMA}.
 */
@SdkProtectedApi
public final class GeneratedOutputOperation implements ApiOperation<SerializableStruct, SerializableStruct> {

    private final ApiOperation<?, ?> delegate;
    private final Supplier<ShapeBuilder<? extends SerializableStruct>> generatedOutputBuilderFactory;

    public GeneratedOutputOperation(ApiOperation<?, ?> delegate,
                                    Supplier<ShapeBuilder<? extends SerializableStruct>> generatedOutputBuilderFactory) {
        this.delegate = delegate;
        this.generatedOutputBuilderFactory = generatedOutputBuilderFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ShapeBuilder<SerializableStruct> inputBuilder() {
        return (ShapeBuilder<SerializableStruct>) delegate.inputBuilder();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ShapeBuilder<SerializableStruct> outputBuilder() {
        return (ShapeBuilder<SerializableStruct>) generatedOutputBuilderFactory.get();
    }

    @Override
    public Schema schema() {
        return delegate.schema();
    }

    @Override
    public Schema inputSchema() {
        return delegate.inputSchema();
    }

    @Override
    public Schema outputSchema() {
        return delegate.outputSchema();
    }

    @Override
    public TypeRegistry errorRegistry() {
        return delegate.errorRegistry();
    }

    @Override
    public List<ShapeId> effectiveAuthSchemes() {
        return delegate.effectiveAuthSchemes();
    }

    @Override
    public List<Schema> errorSchemas() {
        return delegate.errorSchemas();
    }

    @Override
    public ApiService service() {
        return delegate.service();
    }
}
