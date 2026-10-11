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
import software.amazon.awssdk.schema.traits.DefaultTrait;
import software.amazon.awssdk.schema.traits.Trait;

/**
 * The definition of one member added to a {@link SchemaBuilder}, from which its member schema is built.
 *
 * <p>The member index is the member's position in its container's declaration order, fixed when the member is added.
 * Member and target traits are merged here, once, so that member schemas look traits up in a single map.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.MemberSchemaBuilder}.
 */
@SdkInternalApi
final class MemberSchemaBuilder {
    final ShapeType type;
    final ShapeId id;
    final String memberName;
    final int memberIndex;
    final TraitMap traits;
    final TraitMap directTraits;
    final Schema target;
    final SchemaBuilder targetBuilder;
    final boolean isRequiredByValidation;

    /**
     * Creates a member of {@code container}. Exactly one of {@code target} and {@code targetBuilder} is non-null.
     */
    MemberSchemaBuilder(SchemaBuilder container, String memberName, Schema target, SchemaBuilder targetBuilder,
                        Trait[] traits) {
        this.id = container.id.withMember(memberName);
        this.memberName = memberName;
        this.memberIndex = container.memberCount();
        this.target = target;
        this.targetBuilder = targetBuilder;
        this.type = target != null ? target.type() : targetBuilder.type;
        this.directTraits = TraitMap.create(traits);
        TraitMap targetTraits = target != null ? target.traits : targetBuilder.traits;
        this.traits = targetTraits.withMemberTraits(directTraits);
        this.isRequiredByValidation = computeIsRequired(this.traits);
    }

    private static boolean computeIsRequired(TraitMap traits) {
        if (!traits.contains(TraitKey.REQUIRED_TRAIT)) {
            return false;
        }
        DefaultTrait defaultTrait = traits.get(TraitKey.DEFAULT_TRAIT);
        return defaultTrait == null || defaultTrait.getValue() == null;
    }

    Schema build() {
        return targetBuilder != null ? new DeferredMemberSchema(this) : new MemberSchema(this);
    }
}
