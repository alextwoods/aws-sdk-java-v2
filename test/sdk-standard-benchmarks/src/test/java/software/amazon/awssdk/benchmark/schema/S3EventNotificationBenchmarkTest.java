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

package software.amazon.awssdk.benchmark.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.benchmark.schema.S3EventNotificationBenchmark.Implementation;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;

/**
 * Checks the {@link S3EventNotificationBenchmark} fixture: the copied samples are all there, and both implementations
 * read and write each of them the same way, so the benchmark compares equal work.
 */
class S3EventNotificationBenchmarkTest {
    /** The valid samples of {@code S3EventNotificationParityTest} (task 11.19). */
    private static final int VALID_SAMPLES = 50;

    @Test
    void load_allSamples_hasEveryValidSampleOfTheLibraryTestSuite() {
        Map<String, String> samples = S3EventNotificationSampleSet.ALL_SAMPLES.load();

        assertThat(samples).hasSize(VALID_SAMPLES).containsKey("docs-object-created-put.json");
        assertThat(S3EventNotificationSampleSet.OBJECT_CREATED_PUT.load())
            .containsOnlyKeys("docs-object-created-put.json");
    }

    @Test
    void implementations_everySample_readAndWriteTheSame() {
        for (Map.Entry<String, String> sample : S3EventNotificationSampleSet.ALL_SAMPLES.load().entrySet()) {
            S3EventNotification legacy = Implementation.LEGACY.read(sample.getValue());
            S3EventNotification schema = Implementation.SCHEMA.read(sample.getValue());

            assertThat(schema).as(sample.getKey()).isEqualTo(legacy);
            assertThat(Implementation.SCHEMA.write(schema)).as(sample.getKey())
                                                           .isEqualTo(Implementation.LEGACY.write(legacy));
        }
    }

    @ParameterizedTest
    @EnumSource(Implementation.class)
    void benchmarkMethods_allSamples_run(Implementation implementation) {
        S3EventNotificationBenchmark benchmark = new S3EventNotificationBenchmark();
        benchmark.setup(implementation, S3EventNotificationSampleSet.ALL_SAMPLES);
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes "
                                            + "directly is dangerous.");

        benchmark.read(blackhole);
        benchmark.write(blackhole);

        assertThat(benchmark.sampleCount()).isEqualTo(VALID_SAMPLES);
    }
}
