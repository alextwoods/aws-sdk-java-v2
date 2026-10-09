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

package software.amazon.awssdk.benchmark.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.benchmark.harness.BenchmarkStatistics.Ratio;
import software.amazon.awssdk.benchmark.harness.BenchmarkStatistics.Summary;
import software.amazon.smithy.model.node.Node;

/** Each statistic on inputs with known (hand- or table-derived) results. */
class BenchmarkStatisticsTest {
    private static final double LN2 = Math.log(2);

    @Test
    void studentTQuantilesMatchTables() {
        assertEquals(12.7062047362, BenchmarkStatistics.tQuantile(0.975, 1), 1e-6);
        assertEquals(2.7764451052, BenchmarkStatistics.tQuantile(0.975, 4), 1e-8);
        assertEquals(2.2621571628, BenchmarkStatistics.tQuantile(0.975, 9), 1e-8);
        assertEquals(2.0422724563, BenchmarkStatistics.tQuantile(0.975, 30), 1e-8);
        assertEquals(1.959963985, BenchmarkStatistics.tQuantile(0.975, Double.POSITIVE_INFINITY), 1e-6);
        assertEquals(-2.7764451052, BenchmarkStatistics.tQuantile(0.025, 4), 1e-8);
        assertEquals(0.5, BenchmarkStatistics.regularizedBeta(0.5, 3.5, 3.5), 1e-12);
    }

    @Test
    void summaryIsMeanSampleSdAndStudentInterval() {
        Summary s = BenchmarkStatistics.summarize(new double[] {1, 2, 3, 4, 5}).orElseThrow();
        assertEquals(5, s.n());
        assertEquals(3, s.mean(), 1e-12);
        assertEquals(Math.sqrt(2.5), s.sd(), 1e-12);
        double h = 2.7764451052 * Math.sqrt(2.5) / Math.sqrt(5);
        assertEquals(h, s.halfWidth(), 1e-8);
        assertEquals(3 - h, s.ciLow(), 1e-8);
        assertEquals(3 + h, s.ciHigh(), 1e-8);
        assertEquals(h / 3, s.relativeHalfWidth(), 1e-8);
        assertTrue(BenchmarkStatistics.summarize(new double[] {7}).isEmpty());
        assertEquals(0, BenchmarkStatistics.summarize(new double[] {7, 7, 7}).orElseThrow().relativeHalfWidth());
    }

    @Test
    void ratioIsRatioOfGeometricMeansWithWelchInterval() {
        // Logs {0, ln2, 2 ln2} and {ln2, 2 ln2, 3 ln2}: d = ln2, each var/n = ln2^2/3, Welch df = 4.
        Ratio r = BenchmarkStatistics.ratio(new double[] {2, 4, 8}, new double[] {1, 2, 4}).orElseThrow();
        assertEquals(2, r.ratio(), 1e-12);
        assertEquals(LN2 * Math.sqrt(2.0 / 3), r.standardError(), 1e-12);
        assertEquals(4, r.df(), 1e-9);
        double h = 2.7764451052 * r.standardError();
        assertEquals(Math.exp(LN2 - h), r.ciLow(), 1e-8);
        assertEquals(Math.exp(LN2 + h), r.ciHigh(), 1e-8);
        assertEquals((r.ciHigh() - r.ciLow()) / 4, r.relativeHalfWidth(), 1e-12);
        assertEquals(1, r.cells());

        Ratio exact = BenchmarkStatistics.ratio(new double[] {3, 3}, new double[] {2, 2}).orElseThrow();
        assertEquals(1.5, exact.ratio(), 1e-12);
        assertEquals(Double.POSITIVE_INFINITY, exact.df());
        assertEquals(1.5, exact.ciLow(), 1e-12);
        assertEquals(1.5, exact.ciHigh(), 1e-12);
        assertEquals(0, exact.relativeHalfWidth(), 1e-12);
        assertTrue(exact.upperBoundAtMost(1.5));

        assertTrue(BenchmarkStatistics.ratio(new double[] {1}, new double[] {1, 2}).isEmpty());
        assertTrue(BenchmarkStatistics.ratio(new double[] {0, 1}, new double[] {1, 2}).isEmpty());
    }

    @Test
    void equalWeightGeometricMeans() {
        assertEquals(4, BenchmarkStatistics.geometricMean(new double[] {1, 4, 16}).orElseThrow(), 1e-12);
        assertTrue(BenchmarkStatistics.geometricMean(new double[] {1, -1}).isEmpty());

        Ratio twice = BenchmarkStatistics.ratio(new double[] {2, 2}, new double[] {1, 1}).orElseThrow();
        Ratio half = BenchmarkStatistics.ratio(new double[] {1, 1}, new double[] {2, 2}).orElseThrow();
        Ratio g = BenchmarkStatistics.geometricMean(List.of(twice, half, twice, half)).orElseThrow();
        assertEquals(1, g.ratio(), 1e-12);
        assertEquals(4, g.cells());

        // Two identical cells of df 4: se = sqrt(2 se^2) / 2, df = (2 se^2)^2 / (2 se^4 / 4) = 8.
        Ratio r = BenchmarkStatistics.ratio(new double[] {2, 4, 8}, new double[] {1, 2, 4}).orElseThrow();
        Ratio both = BenchmarkStatistics.geometricMean(List.of(r, r)).orElseThrow();
        assertEquals(2, both.ratio(), 1e-12);
        assertEquals(r.standardError() / Math.sqrt(2), both.standardError(), 1e-12);
        assertEquals(8, both.df(), 1e-9);
        assertEquals(Math.exp(LN2 + BenchmarkStatistics.tQuantile(0.975, 8) * both.standardError()), both.ciHigh(),
                     1e-12);
        assertTrue(BenchmarkStatistics.geometricMean(List.of()).isEmpty());
    }

    @Test
    void summariesAndRatiosRoundTripThroughJson() {
        Summary s = BenchmarkStatistics.summarize(new double[] {1, 2, 3, 4, 5}).orElseThrow();
        assertEquals(s, Summary.fromNode(Node.parse(Node.printJson(s.toNode())).expectObjectNode()));
        Ratio r = BenchmarkStatistics.ratio(new double[] {2, 4, 8}, new double[] {1, 2, 4}).orElseThrow();
        assertEquals(r, Ratio.fromNode(Node.parse(Node.printJson(r.toNode())).expectObjectNode()));
        Ratio exact = BenchmarkStatistics.ratio(new double[] {3, 3}, new double[] {2, 2}).orElseThrow();
        assertEquals(exact, Ratio.fromNode(Node.parse(Node.printJson(exact.toNode())).expectObjectNode()));
    }
}
