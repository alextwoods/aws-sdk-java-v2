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

package software.amazon.awssdk.core.exception;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A generated builder was built without one or more of its required members (Requirement 4.11).
 *
 * <p>One exception reports every missing member: {@link #missingMembers()} holds the exact absolute member IDs (for
 * example {@code com.example#Widget$name}) in modeled member order. A member is required at build time when it has the
 * Smithy {@code @required} trait and is not client optional (it is not in an {@code @input} structure and has no
 * {@code @clientOptional} trait); a required member with a {@code @default} value is missing only if it was explicitly
 * set to {@code null}. The message names members only, never member values, and the error is not retryable.
 */
public final class MissingRequiredMembersException extends SdkClientException {

    private static final long serialVersionUID = 1L;

    private final ShapeId shape;
    private final List<ShapeId> missingMembers;

    private MissingRequiredMembersException(BuilderImpl builder) {
        super(builder);
        this.shape = builder.shape;
        this.missingMembers = builder.missingMembers;
    }

    /**
     * The exception for {@code shape} missing {@code missingMembers}.
     *
     * @param shape absolute ID of the structure being built
     * @param missingMembers absolute IDs of every missing required member, in modeled member order; not empty
     */
    public static MissingRequiredMembersException create(ShapeId shape, List<ShapeId> missingMembers) {
        return builder().shape(shape).missingMembers(missingMembers).build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** Absolute ID of the structure that was being built. */
    public ShapeId shape() {
        return shape;
    }

    /** Absolute IDs of every missing required member, in modeled member order. */
    public List<ShapeId> missingMembers() {
        return missingMembers;
    }

    private static String describe(ShapeId shape, List<ShapeId> missing) {
        return "Missing required members of " + shape + ": "
               + missing.stream().map(ShapeId::toString).collect(Collectors.joining(", "));
    }

    /** Builder of a {@link MissingRequiredMembersException}; the message is derived from the shape and members. */
    public interface Builder extends SdkClientException.Builder {

        Builder shape(ShapeId shape);

        Builder missingMembers(List<ShapeId> missingMembers);

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        /**
         * @throws NullPointerException if the shape or the missing members are not set
         * @throws IllegalArgumentException if no missing member is set
         */
        @Override
        MissingRequiredMembersException build();
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {

        private ShapeId shape;
        private List<ShapeId> missingMembers;

        private BuilderImpl() {
        }

        private BuilderImpl(MissingRequiredMembersException ex) {
            super(ex);
            this.shape = ex.shape;
            this.missingMembers = ex.missingMembers;
        }

        @Override
        public Builder shape(ShapeId shape) {
            this.shape = shape;
            return this;
        }

        @Override
        public Builder missingMembers(List<ShapeId> missingMembers) {
            this.missingMembers = missingMembers == null ? null : List.copyOf(missingMembers);
            return this;
        }

        @Override
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public Builder writableStackTrace(Boolean writableStackTrace) {
            this.writableStackTrace = writableStackTrace;
            return this;
        }

        @Override
        public Builder numAttempts(Integer numAttempts) {
            this.numAttempts = numAttempts;
            return this;
        }

        @Override
        public MissingRequiredMembersException build() {
            Objects.requireNonNull(shape, "shape");
            Objects.requireNonNull(missingMembers, "missingMembers");
            if (missingMembers.isEmpty()) {
                throw new IllegalArgumentException("At least one missing member is required.");
            }
            if (message == null) {
                message = describe(shape, missingMembers);
            }
            return new MissingRequiredMembersException(this);
        }
    }
}
