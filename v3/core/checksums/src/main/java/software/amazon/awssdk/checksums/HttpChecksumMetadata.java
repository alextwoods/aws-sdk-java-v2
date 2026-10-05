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

package software.amazon.awssdk.checksums;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The exact value of an operation's {@code aws.protocols#httpChecksum} trait, as runtime metadata.
 *
 * <p>The trait's class lives in {@code smithy-aws-traits}, a build-time artifact, so generated schemas do not carry it
 * (see the generator's runtime trait set). Generated services declare it with {@link #builder()} in their operation
 * bindings, and the dynamic client reads it from the trait's node value with {@link #fromNode(Node)}. Both keep the
 * modeled values exactly, including {@link #responseAlgorithms()} order and algorithm IDs this runtime does not
 * implement, so that feature plans can tell unsupported algorithms from invalid ones.
 */
public final class HttpChecksumMetadata {
    /** Shape ID of the {@code httpChecksum} trait. */
    public static final ShapeId TRAIT_ID = ShapeId.from("aws.protocols#httpChecksum");

    private final boolean requestChecksumRequired;
    private final String requestAlgorithmMember;
    private final String requestValidationModeMember;
    private final List<String> responseAlgorithms;

    private HttpChecksumMetadata(Builder builder) {
        this.requestChecksumRequired = builder.requestChecksumRequired;
        this.requestAlgorithmMember = builder.requestAlgorithmMember;
        this.requestValidationModeMember = builder.requestValidationModeMember;
        this.responseAlgorithms = List.copyOf(builder.responseAlgorithms);
        if (!requestChecksumRequired && requestAlgorithmMember == null
            && (requestValidationModeMember == null || responseAlgorithms.isEmpty())) {
            throw new IllegalArgumentException("The httpChecksum trait must define request or response checksum "
                                               + "behavior");
        }
        if ((requestValidationModeMember == null) != responseAlgorithms.isEmpty()) {
            throw new IllegalArgumentException("The httpChecksum trait must define both requestValidationModeMember "
                                               + "and responseAlgorithms to define response checksum behavior");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The metadata of an {@code httpChecksum} trait node value.
     *
     * @throws IllegalArgumentException if {@code node} is not a valid trait value
     */
    public static HttpChecksumMetadata fromNode(Node node) {
        ObjectNode object = node.asObjectNode()
                                .orElseThrow(() -> new IllegalArgumentException("The httpChecksum trait value must "
                                                                                + "be an object"));
        Builder builder = builder();
        object.getBooleanMember("requestChecksumRequired")
              .ifPresent(value -> builder.requestChecksumRequired(value.getValue()));
        object.getStringMember("requestAlgorithmMember").map(StringNode::getValue)
              .ifPresent(builder::requestAlgorithmMember);
        object.getStringMember("requestValidationModeMember").map(StringNode::getValue)
              .ifPresent(builder::requestValidationModeMember);
        object.getArrayMember("responseAlgorithms").map(ArrayNode::getElements).ifPresent(elements -> {
            for (Node element : elements) {
                builder.addResponseAlgorithm(element.expectStringNode().getValue());
            }
        });
        return builder.build();
    }

    /** True if the operation requires a request checksum ({@code requestChecksumRequired}). */
    public boolean requestChecksumRequired() {
        return requestChecksumRequired;
    }

    /** Name of the top-level input member that selects the request checksum algorithm. */
    public Optional<String> requestAlgorithmMember() {
        return Optional.ofNullable(requestAlgorithmMember);
    }

    /** Name of the top-level input member whose value {@code ENABLED} opts in to response validation. */
    public Optional<String> requestValidationModeMember() {
        return Optional.ofNullable(requestValidationModeMember);
    }

    /** The modeled response algorithm IDs, in modeled order; empty without response checksum behavior. */
    public List<String> responseAlgorithms() {
        return responseAlgorithms;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HttpChecksumMetadata other
                            && requestChecksumRequired == other.requestChecksumRequired
                            && Objects.equals(requestAlgorithmMember, other.requestAlgorithmMember)
                            && Objects.equals(requestValidationModeMember, other.requestValidationModeMember)
                            && responseAlgorithms.equals(other.responseAlgorithms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestChecksumRequired, requestAlgorithmMember, requestValidationModeMember,
                            responseAlgorithms);
    }

    @Override
    public String toString() {
        return "HttpChecksumMetadata[requestChecksumRequired=" + requestChecksumRequired
               + ", requestAlgorithmMember=" + requestAlgorithmMember
               + ", requestValidationModeMember=" + requestValidationModeMember
               + ", responseAlgorithms=" + responseAlgorithms + "]";
    }

    /** Builder of {@link HttpChecksumMetadata}. */
    public static final class Builder {
        private boolean requestChecksumRequired;
        private String requestAlgorithmMember;
        private String requestValidationModeMember;
        private final List<String> responseAlgorithms = new ArrayList<>();

        private Builder() {
        }

        public Builder requestChecksumRequired(boolean requestChecksumRequired) {
            this.requestChecksumRequired = requestChecksumRequired;
            return this;
        }

        public Builder requestAlgorithmMember(String requestAlgorithmMember) {
            this.requestAlgorithmMember = requestAlgorithmMember;
            return this;
        }

        public Builder requestValidationModeMember(String requestValidationModeMember) {
            this.requestValidationModeMember = requestValidationModeMember;
            return this;
        }

        /**
         * Appends a modeled response algorithm ID.
         *
         * @throws IllegalArgumentException if the ID was already added (the modeled list is a set)
         */
        public Builder addResponseAlgorithm(String algorithmId) {
            Objects.requireNonNull(algorithmId, "algorithmId");
            if (responseAlgorithms.contains(algorithmId)) {
                throw new IllegalArgumentException("Response algorithm " + algorithmId + " is listed more than once");
            }
            responseAlgorithms.add(algorithmId);
            return this;
        }

        /**
         * Builds the metadata.
         *
         * @throws IllegalArgumentException if the value defines neither request nor response behavior, or only half
         *     of the response behavior
         */
        public HttpChecksumMetadata build() {
            return new HttpChecksumMetadata(this);
        }
    }
}
