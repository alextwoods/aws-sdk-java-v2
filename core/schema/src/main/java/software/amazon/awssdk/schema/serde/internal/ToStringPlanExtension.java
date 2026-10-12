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

package software.amazon.awssdk.schema.serde.internal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;

/**
 * Computes, once per schema, how {@code software.amazon.awssdk.schema.serde.ToStringSerializer} writes a value of
 * that schema.
 *
 * <p>A value is redacted when its schema, or its target for a member schema, has the {@code sensitive} trait, or
 * when it is a list or map whose elements, keys or values are sensitive or are themselves such lists or maps. This is
 * the rule the SDK's previous code generator used for generated {@code toString} methods, so a container holding
 * sensitive data is replaced by the placeholder as a whole. A root structure or union schema is never redacted:
 * its members carry the redaction. A member schema whose target is a streaming blob or event stream (the
 * {@code streaming} trait) is omitted from the output.
 *
 * <p>SDK addition with no smithy-java counterpart. Registered as a {@link SchemaExtensionProvider} through
 * {@code META-INF/services}.
 */
@SdkInternalApi
public final class ToStringPlanExtension implements SchemaExtensionProvider<ToStringPlanExtension.Plan> {

    public static final SchemaExtensionKey<Plan> KEY = new SchemaExtensionKey<>();

    @Override
    public SchemaExtensionKey<Plan> key() {
        return KEY;
    }

    @Override
    public Plan provide(Schema schema) {
        if (schema.isMember() && schema.hasTrait(TraitKey.STREAMING_TRAIT)) {
            return Plan.OMIT;
        }
        if (!schema.isMember() && (schema.type() == ShapeType.STRUCTURE || schema.type() == ShapeType.UNION)) {
            return Plan.WRITE;
        }
        Schema target = schema.isMember() ? schema.memberTarget() : schema;
        boolean redact = schema.hasTrait(TraitKey.SENSITIVE_TRAIT)
                         || holdsSensitive(target, Collections.newSetFromMap(new IdentityHashMap<>()));
        return redact ? Plan.REDACT : Plan.WRITE;
    }

    /**
     * Returns the cached plan for a schema.
     */
    public static Plan plan(Schema schema) {
        Plan plan = schema.getExtension(KEY);
        // Null only when the provider is not registered, for example in a class loader without the services file.
        return plan != null ? plan : new ToStringPlanExtension().provide(schema);
    }

    /**
     * Returns whether a list or map holds sensitive values at any list or map nesting depth.
     */
    private static boolean holdsSensitive(Schema container, Set<Schema> visited) {
        if (!visited.add(container)) {
            return false;
        }
        switch (container.type()) {
            case LIST:
                return isSensitiveMember(container.listMember(), visited);
            case MAP:
                return isSensitiveMember(container.mapKeyMember(), visited)
                       || isSensitiveMember(container.mapValueMember(), visited);
            default:
                return false;
        }
    }

    private static boolean isSensitiveMember(Schema member, Set<Schema> visited) {
        return member.hasTrait(TraitKey.SENSITIVE_TRAIT) || holdsSensitive(member.memberTarget(), visited);
    }

    /**
     * How a value is written.
     */
    public static final class Plan {
        static final Plan WRITE = new Plan(false, false);
        static final Plan REDACT = new Plan(true, false);
        static final Plan OMIT = new Plan(false, true);

        private final boolean redact;
        private final boolean omit;

        private Plan(boolean redact, boolean omit) {
            this.redact = redact;
            this.omit = omit;
        }

        /**
         * Whether the value is replaced by the redaction placeholder.
         */
        public boolean redact() {
            return redact;
        }

        /**
         * Whether a structure member is left out of the output without reading its value.
         */
        public boolean omit() {
            return omit;
        }
    }
}
