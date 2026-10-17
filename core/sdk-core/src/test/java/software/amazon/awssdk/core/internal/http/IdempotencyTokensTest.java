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

package software.amazon.awssdk.core.internal.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.StubProtocol;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaBuilder;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.traits.IdempotencyTokenTrait;

class IdempotencyTokensTest {
    private static final String NS = "com.example";

    private Supplier<String> previousGenerator;

    @BeforeEach
    void installGenerator() {
        previousGenerator = IdempotentUtils.getGenerator();
        AtomicInteger counter = new AtomicInteger();
        IdempotentUtils.setGenerator(() -> "token-" + counter.incrementAndGet());
    }

    @AfterEach
    void restoreGenerator() {
        IdempotentUtils.setGenerator(previousGenerator);
    }

    @Test
    void fill_withoutTokenMember_returnsTheInput() {
        TokenRequest input = new TokenRequest(schema(-1), "a", null, "c");

        assertThat(IdempotencyTokens.fill(operation(input.schema), input)).isSameAs(input);
    }

    @Test
    void fill_withTokenSet_returnsTheInput() {
        TokenRequest input = new TokenRequest(schema(1), "a", "caller-token", "c");

        SerializableStruct filled = IdempotencyTokens.fill(operation(input.schema), input);

        assertThat(filled).isSameAs(input);
        assertThat(written(filled)).containsExactly(entry("first", "a"), entry("token", "caller-token"),
                                                    entry("last", "c"));
    }

    @Test
    void fill_withTokenSetToEmptyString_keepsTheEmptyString() {
        TokenRequest input = new TokenRequest(schema(1), "a", "", "c");

        assertThat(IdempotencyTokens.fill(operation(input.schema), input)).isSameAs(input);
    }

    @ParameterizedTest(name = "token member {0}, members set: {1}")
    @CsvSource({
        // tokenIndex, firstSet, lastSet, expected member order
        "0, true, true, token first last",
        "1, true, true, first token last",
        "2, true, true, first last token",
        "1, false, true, token last",
        "1, true, false, first token",
        "1, false, false, token",
    })
    void fill_withTokenUnset_writesAGeneratedTokenInMemberOrder(int tokenIndex, boolean firstSet, boolean lastSet,
                                                                String expectedOrder) {
        TokenRequest input = new TokenRequest(schema(tokenIndex), firstSet ? "a" : null, null, lastSet ? "c" : null);

        SerializableStruct filled = IdempotencyTokens.fill(operation(input.schema), input);

        Map<String, String> written = written(filled);
        assertThat(String.join(" ", written.keySet())).isEqualTo(expectedOrder);
        assertThat(written.get("token")).isEqualTo("token-1");
    }

    @Test
    void fill_withTokenUnset_exposesTheTokenAndLeavesTheInputUnchanged() {
        TokenRequest input = new TokenRequest(schema(1), "a", null, "c");

        SerializableStruct filled = IdempotencyTokens.fill(operation(input.schema), input);

        Schema tokenMember = input.schema.member("token");
        assertThat(filled).isNotSameAs(input);
        assertThat(filled.sdkSchema()).isSameAs(input.schema);
        assertThat((String) filled.getMemberValue(tokenMember)).isEqualTo("token-1");
        assertThat((String) filled.getMemberValue(input.schema.member("first"))).isEqualTo("a");
        assertThat((String) input.getMemberValue(tokenMember)).isNull();
        assertThat(written(input)).doesNotContainKey("token");
        assertThat(filled).hasToString(input.toString());
    }

    @Test
    void fill_eachCall_takesANewTokenFromTheCurrentGenerator() {
        TokenRequest input = new TokenRequest(schema(1), null, null, null);
        ApiOperation<?, ?> operation = operation(input.schema);

        assertThat(written(IdempotencyTokens.fill(operation, input))).containsEntry("token", "token-1");
        assertThat(written(IdempotencyTokens.fill(operation, input))).containsEntry("token", "token-2");

        IdempotentUtils.setGenerator(() -> "fixed");
        assertThat(written(IdempotencyTokens.fill(operation, input))).containsEntry("token", "fixed");
    }

    @Test
    void marshall_withTokenUnset_serializesTheFilledInputAndKeepsTheRequest() {
        TokenRequest input = new TokenRequest(schema(1), "a", null, null);
        StubProtocol protocol = new StubProtocol();
        ExecutionContext context = ProtocolStepsTestSupport.executionContext(protocol, input, null);
        context.executionAttributes().putAttribute(SdkExecutionAttribute.API_OPERATION, operation(input.schema));

        ProtocolMarshalling.marshall(context);

        assertThat(protocol.input).isNotSameAs(input);
        assertThat(written((SerializableStruct) protocol.input)).containsEntry("token", "token-1");
        assertThat(context.interceptorContext().request()).isSameAs(input);
        assertThat((String) input.getMemberValue(input.schema.member("token"))).isNull();
    }

    private static Map.Entry<String, String> entry(String key, String value) {
        return new java.util.AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    /**
     * A structure {@code first, token, last} in that order, with the member at {@code tokenIndex} named {@code token}
     * and carrying {@code @idempotencyToken}; -1 gives no idempotency token trait.
     */
    private static Schema schema(int tokenIndex) {
        List<String> names = new ArrayList<>();
        Collections.addAll(names, "first", "last");
        names.add(tokenIndex < 0 ? 1 : tokenIndex, "token");
        SchemaBuilder builder = Schema.structureBuilder(ShapeId.fromParts(NS, "TokenRequest" + (tokenIndex + 1)));
        for (String name : names) {
            if (name.equals("token") && tokenIndex >= 0) {
                builder.putMember(name, PreludeSchemas.STRING, IdempotencyTokenTrait.INSTANCE);
            } else {
                builder.putMember(name, PreludeSchemas.STRING);
            }
        }
        return builder.build();
    }

    private static ApiOperation<TokenRequest, ProtocolStepsTestSupport.TestResponse> operation(Schema inputSchema) {
        return SimpleApiOperation.<TokenRequest, ProtocolStepsTestSupport.TestResponse>builder()
                                 .schema(Schema.createOperation(ShapeId.fromParts(NS, "PutThing")))
                                 .inputSchema(() -> inputSchema)
                                 .outputSchema(() -> ProtocolStepsTestSupport.TestResponse.SCHEMA)
                                 .errorRegistry(TypeRegistry.empty())
                                 .service(() -> ProtocolStepsTestSupport.SERVICE_SCHEMA)
                                 .build();
    }

    /**
     * Returns the members {@code serializeMembers} writes, in write order.
     */
    private static Map<String, String> written(SerializableStruct struct) {
        Map<String, String> written = new LinkedHashMap<>();
        struct.serializeMembers(new SpecificShapeSerializer() {
            @Override
            public void writeString(Schema schema, String value) {
                written.put(schema.memberName(), value);
            }
        });
        return written;
    }

    /**
     * A request shaped like a generated class: string members {@code first}, {@code token} and {@code last}, written in
     * member index order when set.
     */
    private static final class TokenRequest extends SdkRequest implements SerializableStruct {
        private final Schema schema;
        private final Map<String, String> values = new LinkedHashMap<>();

        private TokenRequest(Schema schema, String first, String token, String last) {
            this.schema = schema;
            values.put("first", first);
            values.put("token", token);
            values.put("last", last);
        }

        @Override
        public Optional<? extends RequestOverrideConfiguration> overrideConfiguration() {
            return Optional.empty();
        }

        @Override
        public Builder toBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            for (Schema member : schema.members()) {
                String value = values.get(member.memberName());
                if (value != null) {
                    serializer.writeString(member, value);
                }
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) values.get(member.memberName());
        }

        @Override
        public String toString() {
            return "TokenRequest(first=" + values.get("first") + ")";
        }
    }
}
