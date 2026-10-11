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

package software.amazon.awssdk.codegen.schema;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * A Codegen plugin for schema generation, discovered through {@link java.util.ServiceLoader} (the SDK counterpart of
 * smithy-java's {@code JavaCodegenIntegration} for trait initializers). Register an implementation in
 * {@code META-INF/services/software.amazon.awssdk.codegen.schema.SchemaCodegenExtension}.
 */
@SdkProtectedApi
public interface SchemaCodegenExtension {
    /**
     * Returns trait initializers to register. Each trait ShapeId may have one initializer across all extensions,
     * including the SDK's built-in ones; a second one fails generation (Requirement 9.8).
     *
     * @return the initializers
     */
    default List<TraitInitializer<?>> traitInitializers() {
        return Collections.emptyList();
    }
}
