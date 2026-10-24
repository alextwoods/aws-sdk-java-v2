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

import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A member schema that targets a {@link SchemaBuilder}, used for recursive shapes. The target is the builder's built
 * schema, read until the target is resolved, then cached, so the member sees the resolved schema once the builder's
 * schema is resolved.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.DeferredMemberSchema}.
 */
@SdkInternalApi
final class DeferredMemberSchema extends Schema {
    private final SchemaBuilder targetBuilder;

    // The target once it is no longer a DeferredRootSchema (SchemaBuilder.resolve replaces that one). A race stores the
    // same instance, which is safely published through its final fields, so the field needs no synchronization.
    private Schema resolvedTarget;

    DeferredMemberSchema(MemberSchemaBuilder builder) {
        super(builder);
        this.targetBuilder = builder.targetBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException naming the target shape ID if the target's builder has not been built yet
     */
    @Override
    public Schema memberTarget() {
        Schema target = resolvedTarget;
        if (target != null) {
            return target;
        }
        return resolveTarget();
    }

    private Schema resolveTarget() {
        Schema target = targetBuilder.builtShape();
        if (target == null) {
            throw unresolvedTarget();
        }
        if (!(target instanceof DeferredRootSchema)) {
            resolvedTarget = target;
        }
        return target;
    }

    private IllegalStateException unresolvedTarget() {
        return new IllegalStateException("Target " + targetBuilder.id + " of member " + id()
                                         + " is not resolved: its SchemaBuilder has not been built");
    }

    @Override
    SchemaBuilder.Members memberData() {
        return memberTarget().memberData();
    }

    @Override
    public Set<String> stringEnumValues() {
        return memberTarget().stringEnumValues();
    }

    @Override
    public Set<Integer> intEnumValues() {
        return memberTarget().intEnumValues();
    }
}
