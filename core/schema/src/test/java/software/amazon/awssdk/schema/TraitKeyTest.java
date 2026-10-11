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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.traits.GenericTrait;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.RequiredTrait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.schema.traits.TraitFactory;

class TraitKeyTest {
    private static final int THREADS = 64;

    @Test
    void get_sameClass_returnsSameInstance() {
        assertThat(TraitKey.get(JsonNameTrait.class)).isSameAs(TraitKey.get(JsonNameTrait.class))
                                                     .isSameAs(TraitKey.JSON_NAME_TRAIT);
        assertThat(TraitKey.get(RequiredTrait.class)).isSameAs(TraitKey.REQUIRED_TRAIT);
    }

    @Test
    void traitClass_returnsTheKeyedClass() {
        assertThat(TraitKey.REQUIRED_TRAIT.traitClass()).isEqualTo(RequiredTrait.class);
        assertThat(TraitKey.get(TestTraits.ThirdParty.class).traitClass()).isEqualTo(TestTraits.ThirdParty.class);
    }

    @Test
    void constants_haveContiguousIdsFromZeroAndDistinctClasses() throws Exception {
        List<TraitKey<?>> constants = constants();
        Set<Integer> ids = new HashSet<>();
        Set<Class<?>> classes = new HashSet<>();
        for (TraitKey<?> key : constants) {
            ids.add(key.id);
            classes.add(key.traitClass());
        }
        Set<Integer> expected = new HashSet<>();
        for (int i = 0; i < constants.size(); i++) {
            expected.add(i);
        }
        assertThat(ids).isEqualTo(expected);
        assertThat(classes).hasSize(constants.size());
        // Declaration order is creation order, so the first constant has index 0.
        assertThat(TraitKey.REQUIRED_TRAIT.id).isZero();
    }

    @Test
    void constants_coverEveryBuiltInTypedTrait() throws Exception {
        Set<Class<?>> constantClasses = new HashSet<>();
        for (TraitKey<?> key : constants()) {
            constantClasses.add(key.traitClass());
        }
        Set<Class<?>> builtInClasses = new HashSet<>();
        for (TraitFactory<?> factory : builtInFactories()) {
            builtInClasses.add(factory.traitClass());
        }
        assertThat(builtInClasses).hasSize(48);
        assertThat(constantClasses).isEqualTo(builtInClasses);
    }

    @Test
    void get_newClasses_getConsecutiveIds() {
        TraitKey<TestTraits.SequentialA> first = TraitKey.get(TestTraits.SequentialA.class);
        TraitKey<TestTraits.SequentialB> second = TraitKey.get(TestTraits.SequentialB.class);
        assertThat(first.id).isGreaterThanOrEqualTo(48);
        assertThat(second.id).isEqualTo(first.id + 1);
    }

    @Test
    void get_concurrentFirstCalls_returnOneKeyPerClassWithDenseIds() throws Exception {
        List<Class<? extends Trait>> raced = Arrays.asList(TestTraits.RacedA.class, TestTraits.RacedB.class,
                                                           TestTraits.RacedC.class, TestTraits.RacedD.class);
        CyclicBarrier start = new CyclicBarrier(THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<List<TraitKey<?>>>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                int offset = t;
                futures.add(executor.submit(() -> {
                    start.await();
                    // Each thread asks for the classes in a different order, so first calls race on every class.
                    TraitKey<?>[] keys = new TraitKey<?>[raced.size()];
                    for (int i = 0; i < raced.size(); i++) {
                        int classIndex = (i + offset) % raced.size();
                        keys[classIndex] = TraitKey.get(raced.get(classIndex));
                    }
                    return Arrays.asList(keys);
                }));
            }

            List<TraitKey<?>> reference = futures.get(0).get(30, TimeUnit.SECONDS);
            for (Future<List<TraitKey<?>>> future : futures) {
                List<TraitKey<?>> keys = future.get(30, TimeUnit.SECONDS);
                for (int i = 0; i < raced.size(); i++) {
                    assertThat(keys.get(i)).isSameAs(reference.get(i));
                    assertThat(keys.get(i).traitClass()).isEqualTo(raced.get(i));
                }
            }

            // The four keys hold four consecutive ids, and the next key created gets the following one: no id is lost
            // to a discarded race result.
            Set<Integer> ids = new HashSet<>();
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (TraitKey<?> key : reference) {
                ids.add(key.id);
                min = Math.min(min, key.id);
                max = Math.max(max, key.id);
            }
            assertThat(ids).hasSize(raced.size());
            assertThat(max - min).isEqualTo(raced.size() - 1);
            assertThat(TraitKey.get(TestTraits.AfterRace.class).id).isEqualTo(max + 1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void get_genericTrait_isRejected() {
        assertThatThrownBy(() -> TraitKey.get(GenericTrait.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("GenericTrait")
            .hasMessageContaining("shape ID");
    }

    @Test
    void get_nullClass_throwsNullPointerException() {
        assertThatThrownBy(() -> TraitKey.get(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("traitClass");
    }

    @Test
    void toString_namesClassAndId() {
        assertThat(TraitKey.REQUIRED_TRAIT.toString())
            .isEqualTo("TraitKey(software.amazon.awssdk.schema.traits.RequiredTrait, id=0)");
    }

    private static List<TraitKey<?>> constants() throws IllegalAccessException {
        List<TraitKey<?>> result = new ArrayList<>();
        for (Field field : TraitKey.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (field.getType() == TraitKey.class && Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
                && Modifier.isFinal(modifiers)) {
                result.add((TraitKey<?>) field.get(null));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<TraitFactory<?>> builtInFactories() throws Exception {
        Class<?> builtIns = Class.forName("software.amazon.awssdk.schema.traits.BuiltInTraitFactories");
        Method all = builtIns.getDeclaredMethod("all");
        all.setAccessible(true);
        return (List<TraitFactory<?>>) all.invoke(null);
    }
}
