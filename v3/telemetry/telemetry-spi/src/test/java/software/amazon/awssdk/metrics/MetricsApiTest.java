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

package software.amazon.awssdk.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.metrics.CoreMetric;

/** The V2 behavior of the ported metrics API: metric identity, collection, categories and levels. */
class MetricsApiTest {
    private static final SdkMetric<Integer> COUNT =
        SdkMetric.create("MetricsApiTest.Count", Integer.class, MetricLevel.INFO, MetricCategory.CUSTOM);
    private static final SdkMetric<String> LABEL =
        SdkMetric.create("MetricsApiTest.Label", String.class, MetricLevel.TRACE,
                         EnumSet.of(MetricCategory.CUSTOM, MetricCategory.HTTP_CLIENT));

    @Test
    void metricNamesAreUniqueAndDefineEquality() {
        assertThatThrownBy(() -> SdkMetric.create("MetricsApiTest.Count", Long.class, MetricLevel.ERROR,
                                                  MetricCategory.CORE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MetricsApiTest.Count");
        assertThat(COUNT.name()).isEqualTo("MetricsApiTest.Count");
        assertThat(COUNT.valueClass()).isEqualTo(Integer.class);
        assertThat(COUNT.level()).isEqualTo(MetricLevel.INFO);
        assertThat(LABEL.categories()).containsExactlyInAnyOrder(MetricCategory.CUSTOM, MetricCategory.HTTP_CLIENT);
        assertThatThrownBy(() -> LABEL.categories().add(MetricCategory.ALL))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(CoreMetric.SERVICE_ID.categories()).containsExactly(MetricCategory.CORE);
        assertThat(CoreMetric.API_CALL_DURATION.level()).isEqualTo(MetricLevel.INFO);
    }

    @Test
    void invalidMetricsAreRejected() {
        assertThatThrownBy(() -> SdkMetric.create(" ", String.class, MetricLevel.INFO, MetricCategory.CORE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SdkMetric.create("MetricsApiTest.NoCategory", String.class, MetricLevel.INFO,
                                                  Set.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SdkMetric.create("MetricsApiTest.NullLevel", String.class, null,
                                                  MetricCategory.CORE))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void collectorsCollectValuesInOrderWithChildren() {
        MetricCollector collector = MetricCollector.create("Parent");
        collector.reportMetric(COUNT, 1);
        collector.reportMetric(LABEL, "a");
        collector.reportMetric(COUNT, 2);
        MetricCollector child = collector.createChild("Child");
        child.reportMetric(LABEL, "b");

        MetricCollection collection = collector.collect();
        assertThat(collection.name()).isEqualTo("Parent");
        assertThat(collection.metricValues(COUNT)).containsExactly(1, 2);
        assertThat(collection.metricValues(CoreMetric.RETRY_COUNT)).isEmpty();
        assertThat(collection.stream().map(r -> r.metric().name() + "=" + r.value()).collect(Collectors.toList()))
            .containsExactly("MetricsApiTest.Count=1", "MetricsApiTest.Count=2", "MetricsApiTest.Label=a");
        assertThat(collection.children()).hasSize(1);
        assertThat(collection.childrenWithName("Child").findFirst().orElseThrow().metricValues(LABEL))
            .containsExactly("b");
        assertThat(collection.childrenWithName("Other")).isEmpty();
        assertThat(collection.creationTime()).isNotNull();
        assertThatThrownBy(() -> collection.metricValues(COUNT).add(3))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> MetricCollector.create("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noOpCollectorCollectsNothing() {
        NoOpMetricCollector noOp = NoOpMetricCollector.create();
        noOp.reportMetric(COUNT, 5);
        assertThat(noOp.createChild("x")).isSameAs(noOp);
        MetricCollection collection = noOp.collect();
        assertThat(collection.name()).isEqualTo("NoOp");
        assertThat(collection.metricValues(COUNT)).isEmpty();
        assertThat(collection.children()).isEmpty();
        assertThat(collection.iterator().hasNext()).isFalse();
    }

    @Test
    void categoriesAndLevelsBehaveAsInV2() {
        assertThat(MetricCategory.fromString("httpclient")).isEqualTo(MetricCategory.HTTP_CLIENT);
        assertThat(MetricCategory.CORE.getValue()).isEqualTo("Core");
        assertThatThrownBy(() -> MetricCategory.fromString("nope")).isInstanceOf(IllegalArgumentException.class);
        assertThat(MetricLevel.TRACE.includesLevel(MetricLevel.ERROR)).isTrue();
        assertThat(MetricLevel.INFO.includesLevel(MetricLevel.INFO)).isTrue();
        assertThat(MetricLevel.ERROR.includesLevel(MetricLevel.INFO)).isFalse();
        assertThat(List.of(MetricLevel.values())).containsExactly(MetricLevel.TRACE, MetricLevel.INFO,
                                                                  MetricLevel.ERROR);
    }
}
