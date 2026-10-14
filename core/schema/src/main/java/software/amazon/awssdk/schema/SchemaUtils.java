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

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Member checks used by generated {@code getMemberValue} and {@code setMemberValue} methods, which select a member by
 * {@link Schema#memberIndex()} and then confirm that the schema they were given is that member.
 *
 * <p>Port of the member-validation part of smithy-java {@code software.amazon.smithy.java.core.schema.SchemaUtils}.
 */
@SdkProtectedApi
public final class SchemaUtils {
    private SchemaUtils() {
    }

    /**
     * Returns {@code value} if {@code actual} is the member {@code expected}: the same instance, or a member with the
     * same ShapeId and member index (for example the member of a schema built by another copy of the same class).
     *
     * @param expected the member a generated switch case selected by member index
     * @param actual the member schema the caller passed
     * @param value the value to return
     * @param <T> the value type
     * @return {@code value}
     * @throws IllegalArgumentException if {@code actual} is a different member
     */
    public static <T> T validateSameMember(Schema expected, Schema actual, T value) {
        if (expected == actual || isSameMember(expected, actual)) {
            return value;
        }
        throw illegalMember(expected, actual);
    }

    /**
     * Returns the exception generated code throws when a schema is not a member of a shape.
     *
     * @param container the shape's schema
     * @param member the schema that is not one of its members
     * @return the exception to throw
     */
    public static IllegalArgumentException notAMember(Schema container, Schema member) {
        return new IllegalArgumentException("Schema " + describe(member) + " is not a member of " + container.id());
    }

    private static boolean isSameMember(Schema expected, Schema actual) {
        return actual != null
               && actual.isMember()
               && actual.memberIndex() == expected.memberIndex()
               && actual.id().equals(expected.id());
    }

    private static IllegalArgumentException illegalMember(Schema expected, Schema actual) {
        return new IllegalArgumentException("Expected member " + expected.id() + " at member index "
                                            + expected.memberIndex() + ", found " + describe(actual));
    }

    private static String describe(Schema schema) {
        if (schema == null) {
            return "null";
        }
        return schema.isMember() ? schema.id() + " (member index " + schema.memberIndex() + ")" : schema.id().toString();
    }
}
