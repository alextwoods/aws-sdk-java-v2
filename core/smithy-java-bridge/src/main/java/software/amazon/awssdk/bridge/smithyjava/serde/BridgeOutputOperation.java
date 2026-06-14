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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * An {@link ApiOperation} that delegates to a real (e.g. DynamicClient-resolved) operation but
 * substitutes {@link #outputBuilder()} with a {@link BridgeOutputBuilder} that deserializes the
 * response into an AWS SDK v2 {@link SdkPojo} builder.
 *
 * <p>This is how the deserialize bridge plugs into smithy-java: the protocol's
 * {@code deserializeResponse} obtains its output builder from {@code operation.outputBuilder()},
 * so returning a {@code BridgeOutputBuilder} here routes wire-decoding straight into a v2 pojo.
 */
@SdkPublicApi
public final class BridgeOutputOperation implements ApiOperation<SerializableStruct, BridgeOutputBuilder.Built> {

    private final ApiOperation<?, ?> delegate;
    private final Supplier<SdkPojo> v2OutputBuilderFactory;

    /**
     * @param delegate               the real operation (provides schemas, error registry, auth, etc.)
     * @param v2OutputBuilderFactory supplies a fresh v2 {@code *.Builder} (an SdkPojo) per call
     */
    public BridgeOutputOperation(ApiOperation<?, ?> delegate, Supplier<SdkPojo> v2OutputBuilderFactory) {
        this.delegate = delegate;
        this.v2OutputBuilderFactory = v2OutputBuilderFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ShapeBuilder<SerializableStruct> inputBuilder() {
        return (ShapeBuilder<SerializableStruct>) delegate.inputBuilder();
    }

    @Override
    public ShapeBuilder<BridgeOutputBuilder.Built> outputBuilder() {
        return BridgeOutputBuilder.of(delegate.outputSchema(), v2OutputBuilderFactory.get());
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
