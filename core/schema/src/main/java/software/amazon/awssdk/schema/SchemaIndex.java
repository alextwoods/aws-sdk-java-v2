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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * An index of schemas by shape ID. Each generated service has one, holding the schema of every shape in its closure,
 * and registers it in {@code META-INF/services/software.amazon.awssdk.schema.SchemaIndex}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.SchemaIndex}.
 */
@SdkPublicApi
public abstract class SchemaIndex {
    protected SchemaIndex() {
    }

    /**
     * Returns an index over every {@link SchemaIndex} discovered through {@link ServiceLoader} with the class loader
     * of this class. Discovery happens once, on the first call.
     *
     * @return the combined index
     */
    public static SchemaIndex getCombinedSchemaIndex() {
        return CombinedHolder.COMBINED;
    }

    /**
     * Returns an index that looks a shape ID up in each of the given indexes in order and returns the first schema
     * found.
     *
     * @param indexes the indexes to combine
     * @return the combined index
     * @throws NullPointerException if the array or any element is null
     */
    public static SchemaIndex compose(SchemaIndex... indexes) {
        Validate.paramNotNull(indexes, "indexes");
        for (int i = 0; i < indexes.length; i++) {
            if (indexes[i] == null) {
                throw new NullPointerException("indexes[" + i + "] must not be null");
            }
        }
        return new CombinedSchemaIndex(Collections.unmodifiableList(new ArrayList<>(Arrays.asList(indexes))));
    }

    /**
     * Returns the schema with a shape ID.
     *
     * @param id the shape ID
     * @return the schema, or null if the index has no schema with that ID
     */
    public abstract Schema getSchema(ShapeId id);

    /**
     * Calls a visitor with every schema in the index.
     *
     * @param visitor the visitor
     */
    public abstract void visit(Consumer<Schema> visitor);

    private static final class CombinedHolder {
        private static final SchemaIndex COMBINED = discover();

        private CombinedHolder() {
        }

        private static SchemaIndex discover() {
            List<SchemaIndex> indexes = new ArrayList<>();
            for (SchemaIndex index : ServiceLoader.load(SchemaIndex.class, SchemaIndex.class.getClassLoader())) {
                indexes.add(index);
            }
            return new CombinedSchemaIndex(Collections.unmodifiableList(indexes));
        }
    }

    private static final class CombinedSchemaIndex extends SchemaIndex {
        private final List<SchemaIndex> indexes;

        private CombinedSchemaIndex(List<SchemaIndex> indexes) {
            this.indexes = indexes;
        }

        @Override
        public Schema getSchema(ShapeId id) {
            for (SchemaIndex index : indexes) {
                Schema schema = index.getSchema(id);
                if (schema != null) {
                    return schema;
                }
            }
            return null;
        }

        @Override
        public void visit(Consumer<Schema> visitor) {
            for (SchemaIndex index : indexes) {
                index.visit(visitor);
            }
        }
    }
}
