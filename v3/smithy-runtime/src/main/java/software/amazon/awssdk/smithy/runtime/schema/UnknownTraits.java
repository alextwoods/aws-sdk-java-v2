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

package software.amazon.awssdk.smithy.runtime.schema;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;

/**
 * The unknown traits of one shape or member, keyed by exact absolute trait shape ID (Requirements 2.11, 14.17).
 *
 * <p>An unknown trait is a trait applied in the canonical model whose definition has no trait implementation available
 * to the generator; it is retained with its exact trait ID and Smithy node value as a {@link DynamicTrait}. Smithy
 * Java's {@code Schema} stores traits by Java class, so it can hold at most one {@code DynamicTrait} per schema and
 * cannot look one up by trait ID. Generated schemas therefore keep unknown traits out of the Smithy Java schema and
 * expose them here instead, any number per shape or member, through {@link ServiceSchemaIndex#unknownTraits}.
 *
 * <p>Instances are immutable; iteration is in ascending trait ID order. Equality compares trait IDs and node values
 * structurally.
 */
public final class UnknownTraits {

    /** No unknown traits. */
    public static final UnknownTraits EMPTY = new UnknownTraits(new TreeMap<>());

    private final SortedMap<ShapeId, DynamicTrait> traits;

    private UnknownTraits(TreeMap<ShapeId, DynamicTrait> traits) {
        this.traits = Collections.unmodifiableSortedMap(traits);
    }

    /**
     * The given unknown traits.
     *
     * @throws IllegalArgumentException if two traits have the same trait ID
     */
    public static UnknownTraits of(DynamicTrait... traits) {
        return of(List.of(traits));
    }

    /**
     * The given unknown traits.
     *
     * @throws IllegalArgumentException if two traits have the same trait ID
     */
    public static UnknownTraits of(List<DynamicTrait> traits) {
        if (traits.isEmpty()) {
            return EMPTY;
        }
        TreeMap<ShapeId, DynamicTrait> byId = new TreeMap<>();
        for (DynamicTrait trait : traits) {
            Objects.requireNonNull(trait, "trait");
            if (byId.putIfAbsent(trait.toShapeId(), trait) != null) {
                throw new IllegalArgumentException("Unknown trait " + trait.toShapeId() + " is listed more than once");
            }
        }
        return new UnknownTraits(byId);
    }

    /** The unknown trait with exactly the trait ID {@code traitId}. */
    public Optional<DynamicTrait> get(ShapeId traitId) {
        return Optional.ofNullable(traits.get(traitId));
    }

    /** The Smithy node value of the unknown trait with exactly the trait ID {@code traitId}. */
    public Optional<Node> value(ShapeId traitId) {
        return get(traitId).map(DynamicTrait::toNode);
    }

    /** Whether an unknown trait with exactly the trait ID {@code traitId} is present. */
    public boolean contains(ShapeId traitId) {
        return traits.containsKey(traitId);
    }

    /** The trait IDs, in ascending order. */
    public List<ShapeId> ids() {
        return List.copyOf(traits.keySet());
    }

    /** Every unknown trait by trait ID, in ascending trait ID order; unmodifiable. */
    public SortedMap<ShapeId, DynamicTrait> asMap() {
        return traits;
    }

    /** The number of unknown traits. */
    public int size() {
        return traits.size();
    }

    /** Whether there is no unknown trait. */
    public boolean isEmpty() {
        return traits.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UnknownTraits that) || that.traits.size() != traits.size()) {
            return false;
        }
        for (DynamicTrait trait : traits.values()) {
            DynamicTrait match = that.traits.get(trait.toShapeId());
            if (match == null || !match.toNode().equals(trait.toNode())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (DynamicTrait trait : traits.values()) {
            hash = 31 * hash + trait.toShapeId().hashCode();
            hash = 31 * hash + trait.toNode().hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("UnknownTraits[");
        String separator = "";
        for (DynamicTrait trait : traits.values()) {
            result.append(separator).append('@').append(trait.toShapeId()).append('(')
                  .append(Node.printJson(trait.toNode())).append(')');
            separator = ", ";
        }
        return result.append(']').toString();
    }
}
