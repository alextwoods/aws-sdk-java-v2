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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.traits.GenericTrait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.utils.Validate;

/**
 * The immutable trait storage of a schema.
 *
 * <p>Typed traits live in an array indexed by {@link TraitKey#id}, so {@link #get(TraitKey)} is a bounds check and an
 * array load. {@link GenericTrait}s, which have no key, live in a separate small array of distinct trait IDs and are
 * found by {@link #getGeneric(ShapeId)}. When two traits of one class, or two generic traits with one trait ID, are
 * given, the later one wins.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.TraitMap}, plus the generic-trait storage and
 * the {@link #traits()} view.
 */
@SdkInternalApi
final class TraitMap {
    private static final Trait[] NO_TRAITS = new Trait[0];
    private static final GenericTrait[] NO_GENERIC_TRAITS = new GenericTrait[0];

    static final TraitMap EMPTY = new TraitMap(NO_TRAITS, NO_GENERIC_TRAITS);

    private final Trait[] values;
    private final GenericTrait[] generics;

    /**
     * Cached {@link #traits()} view. Computed on first use; a race computes equal views, and the view is safely
     * published through its final fields, so the field needs no synchronization.
     */
    private Collection<Trait> view;

    private TraitMap(Trait[] values, GenericTrait[] generics) {
        this.values = values;
        this.generics = generics;
    }

    /**
     * Creates trait storage holding the given traits.
     *
     * @throws NullPointerException if the array or any of its elements is null
     */
    static TraitMap create(Trait... traits) {
        Validate.paramNotNull(traits, "traits");
        int largestId = -1;
        int genericCount = 0;
        for (int i = 0; i < traits.length; i++) {
            Trait trait = traits[i];
            if (trait == null) {
                throw new NullPointerException("traits[" + i + "] must not be null");
            }
            if (trait instanceof GenericTrait) {
                genericCount++;
            } else {
                largestId = Math.max(largestId, TraitKey.get(trait.getClass()).id);
            }
        }
        if (largestId < 0 && genericCount == 0) {
            return EMPTY;
        }

        // Exact-sized storage: one slot per key index up to the largest one present.
        Trait[] values = largestId < 0 ? NO_TRAITS : new Trait[largestId + 1];
        GenericTrait[] generics = genericCount == 0 ? NO_GENERIC_TRAITS : new GenericTrait[genericCount];
        int genericSize = 0;
        for (Trait trait : traits) {
            if (trait instanceof GenericTrait) {
                genericSize = putGeneric(generics, genericSize, (GenericTrait) trait);
            } else {
                values[TraitKey.get(trait.getClass()).id] = trait;
            }
        }
        return new TraitMap(values, trim(generics, genericSize));
    }

    /**
     * Returns the typed trait for a key, or null if it is absent. Never throws for a non-null key and never allocates.
     */
    @SuppressWarnings("unchecked")
    <T extends Trait> T get(TraitKey<T> key) {
        int idx = key.id;
        return idx < values.length ? (T) values[idx] : null;
    }

    boolean contains(TraitKey<? extends Trait> key) {
        return get(key) != null;
    }

    /**
     * Returns the generic trait with a trait ID, or null if it is absent. Typed traits are not returned.
     */
    GenericTrait getGeneric(ShapeId traitId) {
        for (GenericTrait trait : generics) {
            if (trait.traitId().equals(traitId)) {
                return trait;
            }
        }
        return null;
    }

    boolean isEmpty() {
        return values.length == 0 && generics.length == 0;
    }

    /**
     * Returns an unmodifiable view of every trait, each once: the typed traits in key index order, then the generic
     * traits. Repeated calls return the same instance.
     */
    Collection<Trait> traits() {
        Collection<Trait> result = view;
        if (result == null) {
            result = computeView();
            view = result;
        }
        return result;
    }

    /**
     * Returns storage holding this map's traits overridden by the member's traits: the target traits of a member
     * schema merged with the member's own traits. Neither map is modified.
     */
    TraitMap withMemberTraits(TraitMap memberTraits) {
        if (memberTraits == this || memberTraits.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return memberTraits;
        }

        Trait[] mergedValues;
        if (memberTraits.values.length == 0) {
            mergedValues = values;
        } else {
            mergedValues = Arrays.copyOf(values, Math.max(values.length, memberTraits.values.length));
            for (int i = 0; i < memberTraits.values.length; i++) {
                Trait value = memberTraits.values[i];
                if (value != null) {
                    mergedValues[i] = value;
                }
            }
        }

        GenericTrait[] mergedGenerics;
        if (memberTraits.generics.length == 0) {
            mergedGenerics = generics;
        } else {
            mergedGenerics = Arrays.copyOf(generics, generics.length + memberTraits.generics.length);
            int size = generics.length;
            for (GenericTrait trait : memberTraits.generics) {
                size = putGeneric(mergedGenerics, size, trait);
            }
            mergedGenerics = trim(mergedGenerics, size);
        }
        return new TraitMap(mergedValues, mergedGenerics);
    }

    private Collection<Trait> computeView() {
        int count = generics.length;
        for (Trait value : values) {
            if (value != null) {
                count++;
            }
        }
        if (count == 0) {
            return Collections.emptyList();
        }
        Trait[] all = new Trait[count];
        int i = 0;
        for (Trait value : values) {
            if (value != null) {
                all[i++] = value;
            }
        }
        System.arraycopy(generics, 0, all, i, generics.length);
        return Collections.unmodifiableList(Arrays.asList(all));
    }

    /**
     * Adds a generic trait to the first {@code size} slots of {@code target}, replacing a trait with the same ID.
     * Returns the new size.
     */
    private static int putGeneric(GenericTrait[] target, int size, GenericTrait trait) {
        for (int i = 0; i < size; i++) {
            if (target[i].traitId().equals(trait.traitId())) {
                target[i] = trait;
                return size;
            }
        }
        target[size] = trait;
        return size + 1;
    }

    private static GenericTrait[] trim(GenericTrait[] generics, int size) {
        if (size == 0) {
            return NO_GENERIC_TRAITS;
        }
        return size == generics.length ? generics : Arrays.copyOf(generics, size);
    }
}
