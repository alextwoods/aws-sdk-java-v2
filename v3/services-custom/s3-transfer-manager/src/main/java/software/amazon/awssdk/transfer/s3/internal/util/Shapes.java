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

package software.amazon.awssdk.transfer.s3.internal.util;

import java.util.Set;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;

/**
 * Schema-driven member copies between related S3 shapes, the V3 counterpart of V2's {@code SdkPojoConversionUtils}:
 * a member is copied when the target shape has a member with the same modeled name and shape type. Only the public
 * Smithy Java schema API ({@link SerializableStruct#getMemberValue}, {@link ShapeBuilder#setMemberValue}) is used.
 * Not a public API.
 */
public final class Shapes {
    private Shapes() {
    }

    /**
     * Copies every set member of {@code source} that {@code target} also models, except the members named in
     * {@code exclude}, and returns {@code target}.
     */
    public static <B extends ShapeBuilder<?>> B copyMembers(SerializableStruct source, B target, Set<String> exclude) {
        Schema targetSchema = target.schema();
        for (Schema member : source.schema().members()) {
            String name = member.memberName();
            if (exclude.contains(name)) {
                continue;
            }
            Object value = source.getMemberValue(member);
            if (value == null) {
                continue;
            }
            Schema targetMember = memberOrNull(targetSchema, name);
            if (targetMember != null && targetMember.type() == member.type()) {
                target.setMemberValue(targetMember, value);
            }
        }
        return target;
    }

    /**
     * Fails if {@code source} has a set member outside {@code allowed}: such a member has no defined meaning when the
     * request is split into multipart requests, so it is rejected rather than silently dropped (as in V2).
     *
     * @throws IllegalArgumentException naming the unsupported member
     */
    public static void validateMembers(SerializableStruct source, Set<String> allowed, String operation) {
        for (Schema member : source.schema().members()) {
            if (!allowed.contains(member.memberName()) && source.getMemberValue(member) != null) {
                throw new IllegalArgumentException(String.format(
                    "%s does not support the %s member %s", operation, source.schema().id().getName(),
                    member.memberName()));
            }
        }
    }

    /** The member of {@code schema} named {@code name}, or {@code null}. */
    public static Schema memberOrNull(Schema schema, String name) {
        for (Schema member : schema.members()) {
            if (member.memberName().equals(name)) {
                return member;
            }
        }
        return null;
    }
}
