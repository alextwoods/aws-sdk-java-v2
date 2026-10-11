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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class SchemaIndexTest {
    static final Schema DISCOVERED = Schema.createString(ShapeId.fromParts("com.example.index", "Discovered"));

    @Test
    void getSchema_unknownId_returnsNull() {
        SchemaIndex index = new MapSchemaIndex(PreludeSchemas.STRING);

        assertThat(index.getSchema(PreludeSchemas.STRING.id())).isSameAs(PreludeSchemas.STRING);
        assertThat(index.getSchema(ShapeId.from("com.example#Unknown"))).isNull();
    }

    @Test
    void compose_returnsFirstMatch_andNullWhenNoneMatch() {
        Schema shadow = Schema.createInteger(PreludeSchemas.STRING.id());
        SchemaIndex combined = SchemaIndex.compose(new MapSchemaIndex(PreludeSchemas.STRING),
                                                   new MapSchemaIndex(shadow, PreludeSchemas.BLOB));

        assertThat(combined.getSchema(PreludeSchemas.STRING.id())).isSameAs(PreludeSchemas.STRING);
        assertThat(combined.getSchema(PreludeSchemas.BLOB.id())).isSameAs(PreludeSchemas.BLOB);
        assertThat(combined.getSchema(ShapeId.from("com.example#Unknown"))).isNull();

        List<Schema> visited = new ArrayList<>();
        combined.visit(visited::add);
        assertThat(visited).containsExactly(PreludeSchemas.STRING, shadow, PreludeSchemas.BLOB);
    }

    @Test
    void compose_nullIndex_throws() {
        assertThatThrownBy(() -> SchemaIndex.compose(new MapSchemaIndex(), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("indexes[1]");
    }

    @Test
    void combinedSchemaIndex_includesServiceLoadedIndexes() {
        SchemaIndex combined = SchemaIndex.getCombinedSchemaIndex();

        assertThat(combined).isSameAs(SchemaIndex.getCombinedSchemaIndex());
        assertThat(combined.getSchema(DISCOVERED.id())).isSameAs(DISCOVERED);
        assertThat(combined.getSchema(ShapeId.from("com.example#Unknown"))).isNull();
    }

    static class MapSchemaIndex extends SchemaIndex {
        private final Map<ShapeId, Schema> schemas = new LinkedHashMap<>();

        MapSchemaIndex(Schema... schemas) {
            for (Schema schema : schemas) {
                this.schemas.put(schema.id(), schema);
            }
        }

        @Override
        public Schema getSchema(ShapeId id) {
            return schemas.get(id);
        }

        @Override
        public void visit(Consumer<Schema> visitor) {
            schemas.values().forEach(visitor);
        }
    }

    /**
     * Registered in {@code META-INF/services/software.amazon.awssdk.schema.SchemaIndex} under test resources.
     */
    public static final class DiscoveredSchemaIndex extends MapSchemaIndex {
        public DiscoveredSchemaIndex() {
            super(DISCOVERED);
        }
    }
}
