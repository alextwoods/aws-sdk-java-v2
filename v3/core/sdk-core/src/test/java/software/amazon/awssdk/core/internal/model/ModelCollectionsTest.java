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

package software.amazon.awssdk.core.internal.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

class ModelCollectionsTest {

    @Test
    void absentMembersSnapshotToNullAndReadAsAutoConstruct() {
        assertNull(ModelCollections.copyList(null));
        assertNull(ModelCollections.copyList(DefaultSdkAutoConstructList.getInstance()));
        assertNull(ModelCollections.copyMap(null));
        assertNull(ModelCollections.copyMap(DefaultSdkAutoConstructMap.getInstance()));
        List<String> absent = ModelCollections.orAutoConstruct((List<String>) null);
        assertInstanceOf(SdkAutoConstructList.class, absent);
        assertEquals(List.of(), absent);
        assertEquals(Map.of(), ModelCollections.orAutoConstruct((Map<String, String>) null));
    }

    @Test
    void snapshotsAreDeepAndShareNoMutableState() {
        List<String> inner = new ArrayList<>(List.of("a", "b"));
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("k", inner);
        List<Map<String, List<String>>> outer = new ArrayList<>();
        outer.add(map);
        outer.add(null);

        List<Map<String, List<String>>> copy = ModelCollections.copyList(outer);
        inner.add("c");
        map.put("other", List.of());
        outer.clear();

        assertEquals(Arrays.asList(Map.of("k", List.of("a", "b")), null), copy);
        assertThrows(UnsupportedOperationException.class, () -> copy.add(null));
        assertThrows(UnsupportedOperationException.class, () -> copy.get(0).put("x", List.of()));
        assertThrows(UnsupportedOperationException.class, () -> copy.get(0).get("k").add("z"));
        assertThrows(UnsupportedOperationException.class, () -> copy.get(0).entrySet().iterator().next()
                                                                     .setValue(List.of()));
        Iterator<String> iterator = copy.get(0).get("k").iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertThrows(UnsupportedOperationException.class, () -> copy.subList(0, 1).clear());
        assertThrows(UnsupportedOperationException.class, () -> copy.get(0).keySet().clear());
        assertThrows(UnsupportedOperationException.class, () -> copy.get(0).values().clear());
    }

    @Test
    void mutatorsThrowEvenWhenTheyWouldNotChangeTheCollection() {
        List<String> empty = ModelCollections.copyList(List.of());
        List<String> list = ModelCollections.copyList(List.of("a"));
        Map<String, String> emptyMap = ModelCollections.copyMap(Map.of());
        Map<String, String> map = ModelCollections.copyMap(Map.of("k", "v"));

        assertThrows(UnsupportedOperationException.class, empty::clear);
        assertThrows(UnsupportedOperationException.class, () -> list.addAll(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> list.remove("absent"));
        assertThrows(UnsupportedOperationException.class, () -> list.removeIf(e -> false));
        assertThrows(UnsupportedOperationException.class, () -> list.retainAll(List.of("a")));
        assertThrows(UnsupportedOperationException.class, () -> empty.sort(null));
        assertThrows(UnsupportedOperationException.class, () -> list.subList(0, 0).addAll(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> list.reversed().addAll(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> emptyMap.putAll(Map.of()));
        assertThrows(UnsupportedOperationException.class, () -> map.remove("absent"));
        assertThrows(UnsupportedOperationException.class, () -> map.putIfAbsent("k", "x"));
        assertThrows(UnsupportedOperationException.class, () -> map.computeIfAbsent("absent", k -> null));
        assertThrows(UnsupportedOperationException.class, () -> map.replace("absent", "x"));
        assertEquals(List.of("a"), list.reversed());
        assertEquals(Map.of("k", "v"), map);
    }

    @Test
    void deepSnapshotsAreReusedAndShallowOnesAreNot() {
        List<List<String>> copy = ModelCollections.copyList(List.of(new ArrayList<>(List.of("a"))));
        assertSame(copy, ModelCollections.copyList(copy));

        // A converted list holding a mutable list is not deep, so it is copied again.
        List<Object> mutable = new ArrayList<>(List.of("a"));
        List<Object> converted = ModelCollections.convertList(List.of(1), e -> mutable);
        List<Object> snapshot = ModelCollections.copyList(converted);
        assertNotSame(converted, snapshot);
        mutable.add("b");
        assertEquals(List.of(List.of("a")), snapshot);
    }

    @Test
    void nestedCollectionsBecomeListsAndMapsKeepInsertionOrder() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("z", 1);
        source.put("a", 2);
        source.put("m", null);
        Map<String, Integer> copy = ModelCollections.copyMap(source);
        assertEquals(List.of("z", "a", "m"), List.copyOf(copy.keySet()));
        assertEquals(Arrays.asList(1, 2, null), new ArrayList<>(copy.values()));

        List<List<String>> fromSet = ModelCollections.copyList(List.of(new HashSet<>(List.of("x"))));
        assertInstanceOf(List.class, fromSet.get(0));
        List<List<String>> nestedAbsent = ModelCollections.copyList(List.of(DefaultSdkAutoConstructList.getInstance()));
        assertEquals(List.of(List.of()), nestedAbsent);
    }

    @Test
    void adoptedCollectionsAreViewsThatLaterSnapshotsReuse() {
        List<String> fresh = new ArrayList<>(List.of("a"));
        List<String> adopted = ModelCollections.adoptList(fresh);
        assertThrows(UnsupportedOperationException.class, () -> adopted.add("b"));
        assertSame(adopted, ModelCollections.copyList(adopted));
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("k", adopted);
        Map<String, List<String>> adoptedMap = ModelCollections.adoptMap(map);
        assertSame(adoptedMap, ModelCollections.copyMap(adoptedMap));
    }

    @Test
    void convertedCollectionsAreUnmodifiableAndAbsentStaysAbsent() {
        assertNull(ModelCollections.convertList(null, e -> e));
        assertNull(ModelCollections.convertMap(DefaultSdkAutoConstructMap.getInstance(), k -> k, v -> v));
        List<String> strings = ModelCollections.convertList(List.of(1, 2), String::valueOf);
        assertEquals(List.of("1", "2"), strings);
        assertThrows(UnsupportedOperationException.class, () -> strings.set(0, "x"));
        Map<String, String> map = ModelCollections.convertMap(Map.of(1, 2), String::valueOf, String::valueOf);
        assertEquals(Map.of("1", "2"), map);
    }
}
