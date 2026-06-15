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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaExtensionKey;
import software.amazon.smithy.java.core.schema.SchemaExtensionProvider;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Pre-computes the v2-bridge serialization plan on {@link Schema} objects, the same way the
 * JSON/XML/CBOR codecs pre-compute their per-schema state via {@link SchemaExtensionProvider}.
 *
 * <p>For each structure/union schema this stores, keyed on the Schema itself, a
 * {@code member-name -> member Schema} lookup so the bridge can bind a v2 {@code SdkField} to
 * its smithy member with a single map get instead of {@code Schema.member(name)} (a linear
 * scan) on every call. Discovered via {@link java.util.ServiceLoader}; the extension is
 * computed once per Schema on first access and thereafter read as a plain array slot — i.e.
 * effectively free per call, with zero reflection.
 *
 * <p>It also hosts the per-pojo-class compiled serialize/deserialize <b>plans</b>, cached in a
 * {@link ClassValue} on the Schema's {@code BridgePlan}. This replaces the old
 * {@code ConcurrentHashMap} keyed by {@code schema.id()+"|"+class}: profiling showed that
 * building + hashing that string key per nested element cost ~9% of serialize and ~16% of
 * deserialize CPU (every {@code AttributeValue} in a DynamoDB map went through it). The
 * {@code BridgePlan} is read from the Schema's extension array (effectively free), and the
 * plan is looked up by {@code Class} identity via {@code ClassValue} — no string build, no
 * string hash on the hot path.
 */
@SdkProtectedApi
public final class BridgeSchemaExtension
        implements SchemaExtensionProvider<BridgeSchemaExtension.BridgePlan> {

    /** Extension key for the bridge's per-schema data. */
    public static final SchemaExtensionKey<BridgePlan> KEY = new SchemaExtensionKey<>();

    /**
     * Pre-computed bridge data stored on a struct/union Schema: fast member-name lookup plus a
     * per-pojo-class plan cache (one for serialize writers, one for deserialize setters).
     */
    public static final class BridgePlan {
        private final Map<String, Schema> memberByName;

        // ClassValue can't take a per-call supplier, so back the cache with a tiny
        // ClassValue<Holder> whose Holder lazily computes via the supplied compiler.
        private final ClassValue<PlanHolder> serializeHolders = new ClassValue<>() {
            @Override
            protected PlanHolder computeValue(Class<?> type) {
                return new PlanHolder();
            }
        };
        private final ClassValue<PlanHolder> deserializeHolders = new ClassValue<>() {
            @Override
            protected PlanHolder computeValue(Class<?> type) {
                return new PlanHolder();
            }
        };

        BridgePlan(Map<String, Schema> memberByName) {
            this.memberByName = memberByName;
        }

        public Schema member(String name) {
            return memberByName.get(name);
        }

        /** Compiled serialize plan for the given pojo class, computed once and cached by identity. */
        @SuppressWarnings("unchecked")
        public <T> T serializePlan(Class<?> pojoClass, Function<Class<?>, T> compiler) {
            return (T) PlanHolder.get(serializeHolders, pojoClass, compiler);
        }

        /** Compiled deserialize plan for the given builder class, computed once and cached by identity. */
        @SuppressWarnings("unchecked")
        public <T> T deserializePlan(Class<?> builderClass, Function<Class<?>, T> compiler) {
            return (T) PlanHolder.get(deserializeHolders, builderClass, compiler);
        }
    }

    private static final class PlanHolder {
        private volatile Object plan;

        static Object get(ClassValue<PlanHolder> holders, Class<?> key, Function<Class<?>, ?> compiler) {
            PlanHolder h = holders.get(key);
            Object p = h.plan;
            if (p == null) {
                synchronized (h) {
                    p = h.plan;
                    if (p == null) {
                        p = compiler.apply(key);
                        h.plan = p;
                    }
                }
            }
            return p;
        }
    }

    @Override
    public SchemaExtensionKey<BridgePlan> key() {
        return KEY;
    }

    @Override
    public BridgePlan provide(Schema schema) {
        var type = schema.type();
        if (type != ShapeType.STRUCTURE && type != ShapeType.UNION) {
            return null;
        }
        var members = schema.members();
        Map<String, Schema> byName = HashMap.newHashMap(members.size());
        for (Schema m : members) {
            byName.put(m.memberName(), m);
        }
        return new BridgePlan(byName);
    }
}
