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

package software.amazon.awssdk.schema;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The resolved form of a {@link DeferredRootSchema}: the same shape ID, traits, builder supplier, class and member
 * schemas, with the members held directly instead of built on first access.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.ResolvedRootSchema}.
 */
@SdkInternalApi
final class ResolvedRootSchema extends Schema {
    private final SchemaBuilder.Members members;

    ResolvedRootSchema(DeferredRootSchema deferred, SchemaBuilder.Members members) {
        super(deferred.type(), deferred.id(), deferred.traits, deferred.builderSupplier, deferred.shapeClass);
        this.members = members;
    }

    @Override
    SchemaBuilder.Members memberData() {
        return members;
    }
}
