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

package software.amazon.awssdk.pipeline;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Tuple;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;

/** jqwik generators shared by the pipeline property tests. */
final class PipelineArbitraries {
    /** Every {@link PipelineStage.Kind#HOOK} stage. */
    static final Set<PipelineStage> HOOK_STAGES = Collections.unmodifiableSet(hookStages());

    private PipelineArbitraries() {
    }

    /** Either driver. */
    static Arbitrary<Driver> drivers() {
        return Arbitraries.of(Driver.class);
    }

    /**
     * A set of hook stages, for example the hooks an interceptor implements: none and all of them each a tenth of the
     * time, otherwise any subset.
     */
    static Arbitrary<Set<PipelineStage>> hookSubsets() {
        Arbitrary<Set<PipelineStage>> none = Arbitraries.just(Set.of());
        Arbitrary<Set<PipelineStage>> all = Arbitraries.just(HOOK_STAGES);
        Arbitrary<Set<PipelineStage>> some = Arbitraries.subsetOf(HOOK_STAGES);
        return Arbitraries.frequencyOf(Tuple.of(1, none), Tuple.of(1, all), Tuple.of(8, some))
                          .map(PipelineArbitraries::enumSet);
    }

    /** An immutable, declaration-ordered copy of {@code stages}, so generated sets print and iterate stably. */
    static Set<PipelineStage> enumSet(Set<PipelineStage> stages) {
        EnumSet<PipelineStage> copy = EnumSet.noneOf(PipelineStage.class);
        copy.addAll(stages);
        return Collections.unmodifiableSet(copy);
    }

    private static EnumSet<PipelineStage> hookStages() {
        EnumSet<PipelineStage> hooks = EnumSet.noneOf(PipelineStage.class);
        for (PipelineStage stage : PipelineStage.values()) {
            if (stage.kind() == PipelineStage.Kind.HOOK) {
                hooks.add(stage);
            }
        }
        return hooks;
    }
}
