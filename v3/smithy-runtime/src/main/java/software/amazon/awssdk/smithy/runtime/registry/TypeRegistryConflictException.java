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

package software.amazon.awssdk.smithy.runtime.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Type registries could not be composed because two of them register different entries for one shape ID
 * (Requirement 6.13).
 *
 * <p>The exception lists every conflict, sorted by shape ID and then by registry position, so the same registries
 * always produce the same exception and message. Each conflict names the shape ID, both registries (by position in
 * the composition and by the namespaces they register) and both registered Java types.
 */
public final class TypeRegistryConflictException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final transient List<Conflict> conflicts;

    TypeRegistryConflictException(List<Conflict> conflicts) {
        super(message(conflicts));
        this.conflicts = List.copyOf(conflicts);
    }

    private static String message(List<Conflict> conflicts) {
        // One shape ID can conflict in several pairs of registries; the count is of shape IDs, not of conflicts.
        long shapeIds = conflicts.stream().map(Conflict::shapeId).distinct().count();
        StringBuilder message = new StringBuilder("Cannot compose type registries: ")
            .append(shapeIds).append(shapeIds == 1 ? " shape ID is" : " shape IDs are")
            .append(" registered with conflicting entries");
        for (Conflict conflict : conflicts) {
            message.append("; ").append(conflict.describe());
        }
        return message.toString();
    }

    /** Every conflict, sorted by shape ID and then by registry position. */
    public List<Conflict> conflicts() {
        return conflicts;
    }

    /** Two different entries registered for one shape ID by two composed registries. */
    public static final class Conflict {
        private final ShapeId shapeId;
        private final int firstRegistry;
        private final String firstSource;
        private final RegisteredType<?> first;
        private final int secondRegistry;
        private final String secondSource;
        private final RegisteredType<?> second;

        Conflict(ShapeId shapeId, int firstRegistry, String firstSource, RegisteredType<?> first,
                 int secondRegistry, String secondSource, RegisteredType<?> second) {
            this.shapeId = Objects.requireNonNull(shapeId, "shapeId");
            this.firstRegistry = firstRegistry;
            this.firstSource = firstSource;
            this.first = first;
            this.secondRegistry = secondRegistry;
            this.secondSource = secondSource;
            this.second = second;
        }

        /** The shape ID both registries register. */
        public ShapeId shapeId() {
            return shapeId;
        }

        /** The position, in the composition, of the earlier registry. */
        public int firstRegistry() {
            return firstRegistry;
        }

        /** A description of the earlier registry: its position and registered namespaces. */
        public String firstSource() {
            return firstSource;
        }

        /** The earlier registry's entry. */
        public RegisteredType<?> first() {
            return first;
        }

        /** The position, in the composition, of the later registry. */
        public int secondRegistry() {
            return secondRegistry;
        }

        /** A description of the later registry: its position and registered namespaces. */
        public String secondSource() {
            return secondSource;
        }

        /** The later registry's entry. */
        public RegisteredType<?> second() {
            return second;
        }

        /** Which components of the two entries differ: {@code schema}, {@code type} and/or {@code builder factory}. */
        public List<String> differences() {
            List<String> result = new ArrayList<>(3);
            if (first.schema() != second.schema()) {
                result.add("schema");
            }
            if (first.type() != second.type()) {
                result.add("type");
            }
            if (first.builderFactory() != second.builderFactory()) {
                result.add("builder factory");
            }
            return List.copyOf(result);
        }

        String describe() {
            return shapeId + " is registered as " + first.type().getName() + " by " + firstSource + " and as "
                   + second.type().getName() + " by " + secondSource + " (differing " + String.join(", ", differences())
                   + ")";
        }

        @Override
        public String toString() {
            return "Conflict[" + describe() + "]";
        }
    }
}
