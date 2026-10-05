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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.core.exception.ModelConflictException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * The idempotency token feature plugin of one operation (Requirement 13.9): the Smithy {@code idempotencyToken}
 * trait.
 *
 * <p>{@link #forOperation} builds the plan once per client and operation from the input schema: the one top-level
 * string member with {@code @idempotencyToken}. The pipeline calls {@link #apply} exactly once per execution, after
 * {@code modifyRequest} and before the first attempt, and serializes every attempt from the returned request, so a
 * generated token is created once per call and reused unchanged by every retry. A caller-supplied token (including
 * an empty string, which is an explicit value) is never replaced; the request is then returned as is. Otherwise the
 * request is rebuilt through the operation's generated builder with every set member copied and the token member set
 * to one value of the token generator (a random UUID by default, as in V2). The generated token is also available in
 * the execution-scoped {@link #GENERATED_TOKEN} slot the pipeline records it in.
 *
 * <p>The feature is stateless and thread-safe when its token generator is.
 */
public final class IdempotencyTokenFeature<I extends SerializableStruct> {
    /** The execution-scoped context slot of a token this feature generated for the call. */
    public static final ContextKey<String> GENERATED_TOKEN = ContextKey.execution("IdempotencyToken", String.class);

    /** The default token generator: a random (version 4) UUID string. */
    public static final Supplier<String> UUID_TOKENS = () -> UUID.randomUUID().toString();

    private final ShapeId operation;
    private final Schema tokenMember;
    private final List<Schema> copiedMembers;
    private final Supplier<? extends ShapeBuilder<I>> builders;
    private final Supplier<String> tokens;

    private IdempotencyTokenFeature(ShapeId operation, Schema tokenMember, List<Schema> copiedMembers,
                                    Supplier<? extends ShapeBuilder<I>> builders, Supplier<String> tokens) {
        this.operation = operation;
        this.tokenMember = tokenMember;
        this.copiedMembers = List.copyOf(copiedMembers);
        this.builders = builders;
        this.tokens = tokens;
    }

    /** {@link #forOperation(ShapeId, Schema, Supplier, Supplier)} with {@link #UUID_TOKENS}. */
    public static <I extends SerializableStruct> Optional<IdempotencyTokenFeature<I>> forOperation(
        ShapeId operation, Schema inputSchema, Supplier<? extends ShapeBuilder<I>> inputBuilders) {
        return forOperation(operation, inputSchema, inputBuilders, UUID_TOKENS);
    }

    /**
     * The idempotency plan of an operation, or empty if its input has no {@code @idempotencyToken} member.
     *
     * @param operation absolute shape ID of the operation, for diagnostics
     * @param inputSchema the operation's input structure schema
     * @param inputBuilders the generated builder supplier of the input type
     * @param tokens the token generator; must return a new non-null value on each call
     * @throws ModelConflictException if more than one input member, or a non-string member, has the trait
     */
    public static <I extends SerializableStruct> Optional<IdempotencyTokenFeature<I>> forOperation(
        ShapeId operation, Schema inputSchema, Supplier<? extends ShapeBuilder<I>> inputBuilders,
        Supplier<String> tokens) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(inputSchema, "inputSchema");
        Objects.requireNonNull(inputBuilders, "inputBuilders");
        Objects.requireNonNull(tokens, "tokens");
        Schema token = null;
        List<Schema> copied = new ArrayList<>();
        for (Schema member : inputSchema.members()) {
            if (member.hasTrait(TraitKey.IDEMPOTENCY_TOKEN)) {
                if (token != null) {
                    throw ModelConflictException.create(operation, "idempotencyToken",
                                                        "the input " + inputSchema.id() + " has more than one "
                                                        + "idempotency token member: " + token.memberName() + ", "
                                                        + member.memberName());
                }
                if (member.type() != ShapeType.STRING) {
                    throw ModelConflictException.create(operation, member.memberName(),
                                                        "an idempotency token member must target a string, not "
                                                        + member.type());
                }
                token = member;
            }
            // Streaming members are bound to the call, not held by the modeled request.
            if (!member.hasTrait(TraitKey.STREAMING_TRAIT)) {
                copied.add(member);
            }
        }
        if (token == null) {
            return Optional.empty();
        }
        return Optional.of(new IdempotencyTokenFeature<>(operation, token, copied, inputBuilders, tokens));
    }

    /** Absolute shape ID of the operation. */
    public ShapeId operation() {
        return operation;
    }

    /** Name of the token member. */
    public String tokenMember() {
        return tokenMember.memberName();
    }

    /**
     * The request every attempt of this execution serializes: {@code request} itself if it has a token, else a copy
     * with one newly generated token.
     */
    public Result<I> apply(I request) {
        Objects.requireNonNull(request, "request");
        if (request.getMemberValue(tokenMember) != null) {
            return new Result<>(request, Optional.empty());
        }
        String token = Objects.requireNonNull(tokens.get(), "The idempotency token generator returned null");
        ShapeBuilder<I> builder = builders.get();
        for (Schema member : copiedMembers) {
            Object value = request.getMemberValue(member);
            if (value != null) {
                builder.setMemberValue(member, value);
            }
        }
        builder.setMemberValue(tokenMember, token);
        return new Result<>(builder.build(), Optional.of(token));
    }

    @Override
    public String toString() {
        return "IdempotencyTokenFeature[" + operation + ", " + tokenMember.memberName() + "]";
    }

    /**
     * The request of an execution and the token generated for it, if any.
     *
     * @param request the request every attempt serializes
     * @param generatedToken the token this feature generated; empty if the caller supplied one
     */
    public record Result<I extends SerializableStruct>(I request, Optional<String> generatedToken) {
        public Result {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(generatedToken, "generatedToken");
        }
    }
}
