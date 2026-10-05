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

package software.amazon.awssdk.pipeline.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.ModelConflictException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

class IdempotencyTokenFeatureTest {
    private static final ShapeId OPERATION = ShapeId.from("com.example#CreateThing");
    private static final Schema STRING = Schema.createString(ShapeId.from("smithy.api#String"));
    private static final Schema INTEGER = Schema.createInteger(ShapeId.from("smithy.api#Integer"));
    private static final Schema STREAM = Schema.createBlob(ShapeId.from("com.example#Stream"), new StreamingTrait());
    private static final Schema INPUT = Schema.structureBuilder(ShapeId.from("com.example#CreateThingInput"))
                                              .putMember("Name", STRING)
                                              .putMember("Count", INTEGER)
                                              .putMember("ClientToken", STRING, new IdempotencyTokenTrait())
                                              .putMember("Body", STREAM)
                                              .build();

    @Test
    void forOperation_whenNoTokenMember_isEmpty() {
        Schema input = Schema.structureBuilder(ShapeId.from("com.example#Plain")).putMember("Name", STRING).build();
        assertThat(IdempotencyTokenFeature.forOperation(OPERATION, input, () -> new Builder(input))).isEmpty();
    }

    @Test
    void forOperation_whenTwoTokenMembers_rejects() {
        Schema input = Schema.structureBuilder(ShapeId.from("com.example#Twice"))
                             .putMember("A", STRING, new IdempotencyTokenTrait())
                             .putMember("B", STRING, new IdempotencyTokenTrait())
                             .build();
        assertThatThrownBy(() -> IdempotencyTokenFeature.forOperation(OPERATION, input, () -> new Builder(input)))
            .isInstanceOfSatisfying(ModelConflictException.class, e -> {
                assertThat(e.operation()).isEqualTo(OPERATION);
                assertThat(e.setting()).isEqualTo("idempotencyToken");
            });
    }

    @Test
    void apply_whenTokenOmitted_injectsExactlyOneGeneratedValueAndCopiesMembers() {
        AtomicInteger generated = new AtomicInteger();
        IdempotencyTokenFeature<Struct> feature =
            IdempotencyTokenFeature.forOperation(OPERATION, INPUT, () -> new Builder(INPUT),
                                                 () -> "token-" + generated.incrementAndGet())
                                   .orElseThrow();
        Struct request = new Struct(INPUT, Map.of("Name", "widget", "Count", 3));

        IdempotencyTokenFeature.Result<Struct> result = feature.apply(request);

        assertThat(generated).hasValue(1);
        assertThat(result.generatedToken()).contains("token-1");
        assertThat(result.request().values).containsExactlyInAnyOrderEntriesOf(
            Map.of("Name", "widget", "Count", 3, "ClientToken", "token-1"));
        // Every attempt serializes result.request(); applying again (as a retry must not) would not change it.
        assertThat(feature.apply(result.request()).request()).isSameAs(result.request());
        assertThat(generated).hasValue(1);
    }

    @Test
    void apply_whenTokenSupplied_keepsRequestUnchanged() {
        IdempotencyTokenFeature<Struct> feature =
            IdempotencyTokenFeature.forOperation(OPERATION, INPUT, () -> new Builder(INPUT)).orElseThrow();
        Struct request = new Struct(INPUT, Map.of("ClientToken", ""));

        IdempotencyTokenFeature.Result<Struct> result = feature.apply(request);

        assertThat(result.request()).isSameAs(request);
        assertThat(result.generatedToken()).isEmpty();
    }

    @Test
    void apply_defaultGenerator_producesUuid() {
        IdempotencyTokenFeature<Struct> feature =
            IdempotencyTokenFeature.forOperation(OPERATION, INPUT, () -> new Builder(INPUT)).orElseThrow();

        String token = feature.apply(new Struct(INPUT, Map.of())).generatedToken().orElseThrow();

        assertThat(UUID.fromString(token).version()).isEqualTo(4);
        assertThat(feature.tokenMember()).isEqualTo("ClientToken");
    }

    /** A map-backed modeled request; the streaming member is bound to the call, as in generated types. */
    static final class Struct implements SerializableStruct {
        private final Schema schema;
        private final Map<String, Object> values;

        Struct(Schema schema, Map<String, Object> values) {
            this.schema = schema;
            this.values = Map.copyOf(values);
        }

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) values.get(member.memberName());
        }
    }

    private static final class Builder implements ShapeBuilder<Struct> {
        private final Schema schema;
        private final Map<String, Object> values = new HashMap<>();

        Builder(Schema schema) {
            this.schema = schema;
        }

        @Override
        public Struct build() {
            return new Struct(schema, values);
        }

        @Override
        public ShapeBuilder<Struct> deserialize(ShapeDeserializer decoder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            if (member.hasTrait(software.amazon.smithy.java.core.schema.TraitKey.STREAMING_TRAIT)) {
                throw new UnsupportedOperationException("streaming member");
            }
            values.put(member.memberName(), value);
        }

        @Override
        public Schema schema() {
            return schema;
        }
    }
}
