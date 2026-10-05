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

package software.amazon.awssdk.awscodegen;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * The orders every emitter and integration uses when rendering collections, so that generated output depends only on
 * the model's content and never on model insertion order, integration enumeration order or hash iteration order
 * (Requirement 3.6):
 * <ul>
 *   <li>shapes and shape IDs by absolute shape ID;</li>
 *   <li>members in modeled order;</li>
 *   <li>traits by absolute trait ID;</li>
 *   <li>integrations by {@code (order(), identifier())};</li>
 *   <li>generated maps and sets by their natural key or element order.</li>
 * </ul>
 * {@link AwsJavaWriter} rejects unordered collections passed to {@code $L}/{@code $S}, and the staged tree is written
 * sorted by relative path.
 */
public final class CodegenOrdering {

    /** Integration execution order: ascending {@code order()}, then ascending {@code identifier()}. */
    public static final Comparator<AwsCodegenIntegration> INTEGRATIONS = AwsCodegenIntegrations.EXECUTION_ORDER;

    private CodegenOrdering() {
    }

    /** {@code shapes} sorted by absolute shape ID. */
    public static <S extends ToShapeId> List<S> byShapeId(Collection<S> shapes) {
        return shapes.stream().sorted(Comparator.comparing(ToShapeId::toShapeId)).toList();
    }

    /** The absolute IDs of {@code shapes}, sorted. */
    public static List<ShapeId> shapeIds(Collection<? extends ToShapeId> shapes) {
        return shapes.stream().map(ToShapeId::toShapeId).sorted().toList();
    }

    /** The members of {@code shape} in modeled order. */
    public static List<MemberShape> members(Shape shape) {
        return List.copyOf(shape.members());
    }

    /** Every trait applied to {@code shape}, sorted by absolute trait ID. */
    public static List<Trait> traits(Shape shape) {
        return List.copyOf(new TreeMap<>(shape.getAllTraits()).values());
    }

    /** An unmodifiable copy of {@code map} ordered by key. */
    public static <K extends Comparable<? super K>, V> SortedMap<K, V> sorted(Map<K, V> map) {
        return Collections.unmodifiableSortedMap(new TreeMap<>(map));
    }

    /** An unmodifiable copy of {@code elements} in natural order, without duplicates. */
    public static <E extends Comparable<? super E>> SortedSet<E> sorted(Collection<E> elements) {
        return Collections.unmodifiableSortedSet(new TreeSet<>(elements));
    }
}
