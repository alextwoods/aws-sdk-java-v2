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

import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The harness's statistics (Req 20.11, 20.14-20.19, 20.23): pure functions of the valid repetitions' values, with no
 * state, randomness or I/O, so an independent oracle can recompute every number (Property 65).
 *
 * <ul>
 *     <li>{@link #summarize}: one arm's metric over its valid repetitions: arithmetic mean, sample standard deviation
 *     ({@code n - 1}), and the two-sided 95% Student-t interval {@code mean +- t(0.975, n - 1) * sd / sqrt(n)}; the
 *     relative half-width is {@code half-width / mean}.</li>
 *     <li>{@link #ratio}: a candidate arm over a reference arm on one cell, on the log scale: with {@code d} the
 *     difference of the two arms' mean log values and {@code se = sqrt(var_c / n_c + var_r / n_r)} (sample variances
 *     of the logs), the ratio is {@code exp(d)} (the ratio of geometric means), its 95% interval
 *     {@code exp(d +- t(0.975, df) * se)} with Welch-Satterthwaite {@code df}, and its relative half-width
 *     {@code (high - low) / (2 * ratio)}.</li>
 *     <li>{@link #geometricMean(List)}: the equal-weight geometric mean of per-cell ratios: {@code exp(mean d_c)},
 *     {@code se = sqrt(sum se_c^2) / C}, Welch-Satterthwaite {@code df = (sum se_c^2)^2 / sum(se_c^4 / df_c)}, and the
 *     same interval and half-width definitions.</li>
 * </ul>
 * Intervals need at least two values per arm; with fewer, or with a non-positive value where logs are taken, the
 * result is absent rather than a made-up interval.
 */
public final class BenchmarkStatistics {

    /** The two-sided confidence level of every interval (Req 20.23). */
    public static final double CONFIDENCE = 0.95;

    private BenchmarkStatistics() {
    }

    /** One arm's metric over its valid repetitions. */
    public record Summary(int n, double mean, double sd, double ciLow, double ciHigh, double halfWidth,
                          double relativeHalfWidth) {

        public ObjectNode toNode() {
            return ObjectNode.builder().withMember("n", n).withMember("mean", number(mean))
                             .withMember("sd", number(sd)).withMember("ci_low", number(ciLow))
                             .withMember("ci_high", number(ciHigh)).withMember("half_width", number(halfWidth))
                             .withMember("relative_half_width", number(relativeHalfWidth)).build();
        }

        public static Summary fromNode(ObjectNode n) {
            return new Summary(n.expectNumberMember("n").getValue().intValue(), d(n, "mean"), d(n, "sd"),
                               d(n, "ci_low"), d(n, "ci_high"), d(n, "half_width"), d(n, "relative_half_width"));
        }
    }

    /**
     * A ratio of geometric means with its 95% interval.
     *
     * @param logDifference {@code d}, the log of the ratio
     * @param standardError the standard error of {@code d}
     * @param df the Welch-Satterthwaite degrees of freedom ({@code Infinity} when {@code se} is 0)
     * @param cells how many per-cell ratios an aggregate combines (1 for a single cell)
     */
    public record Ratio(double ratio, double logDifference, double standardError, double df, double ciLow,
                        double ciHigh, double relativeHalfWidth, int cells) {

        /** True when the whole interval is at or below {@code bound} (a boundary met as a valid result). */
        public boolean upperBoundAtMost(double bound) {
            return ciHigh <= bound;
        }

        public ObjectNode toNode() {
            return ObjectNode.builder().withMember("ratio", number(ratio))
                             .withMember("log_difference", number(logDifference))
                             .withMember("standard_error", number(standardError)).withMember("df", number(df))
                             .withMember("ci_low", number(ciLow)).withMember("ci_high", number(ciHigh))
                             .withMember("relative_half_width", number(relativeHalfWidth))
                             .withMember("cells", cells).build();
        }

        public static Ratio fromNode(ObjectNode n) {
            return new Ratio(d(n, "ratio"), d(n, "log_difference"), d(n, "standard_error"), d(n, "df"), d(n, "ci_low"),
                             d(n, "ci_high"), d(n, "relative_half_width"),
                             n.expectNumberMember("cells").getValue().intValue());
        }
    }

    public static double mean(double[] x) {
        double s = 0;
        for (double v : x) {
            s += v;
        }
        return s / x.length;
    }

    /** Sample variance ({@code n - 1} denominator). */
    public static double variance(double[] x) {
        double m = mean(x);
        double s = 0;
        for (double v : x) {
            s += (v - m) * (v - m);
        }
        return s / (x.length - 1);
    }

    /** {@code exp(mean(log x))}; empty when {@code x} is empty or has a non-positive value. */
    public static Optional<Double> geometricMean(double[] x) {
        Optional<double[]> logs = logs(x);
        return logs.filter(l -> l.length > 0).map(l -> Math.exp(mean(l)));
    }

    /** {@link Summary} of {@code x}; empty for fewer than two values. */
    public static Optional<Summary> summarize(double[] x) {
        if (x.length < 2) {
            return Optional.empty();
        }
        double m = mean(x);
        double sd = Math.sqrt(variance(x));
        double h = tQuantile(0.5 + CONFIDENCE / 2, x.length - 1) * sd / Math.sqrt(x.length);
        double relative = h == 0 ? 0 : m == 0 ? Double.POSITIVE_INFINITY : h / Math.abs(m);
        return Optional.of(new Summary(x.length, m, sd, m - h, m + h, h, relative));
    }

    /** {@code candidate / reference} on one cell; empty unless both have two or more values, all positive. */
    public static Optional<Ratio> ratio(double[] candidate, double[] reference) {
        Optional<double[]> c = logs(candidate);
        Optional<double[]> r = logs(reference);
        if (c.isEmpty() || r.isEmpty() || c.get().length < 2 || r.get().length < 2) {
            return Optional.empty();
        }
        double vc = variance(c.get()) / c.get().length;
        double vr = variance(r.get()) / r.get().length;
        double se2 = vc + vr;
        double df = se2 == 0 ? Double.POSITIVE_INFINITY
                             : se2 * se2 / (vc * vc / (c.get().length - 1) + vr * vr / (r.get().length - 1));
        return Optional.of(interval(mean(c.get()) - mean(r.get()), se2, df, 1));
    }

    /** The equal-weight geometric mean of per-cell ratios; empty for none. */
    public static Optional<Ratio> geometricMean(List<Ratio> cells) {
        if (cells.isEmpty()) {
            return Optional.empty();
        }
        int k = cells.size();
        double d = 0;
        double se2 = 0;
        double denominator = 0;
        for (Ratio r : cells) {
            d += r.logDifference();
            double v = r.standardError() * r.standardError();
            se2 += v;
            if (v > 0) {
                denominator += v * v / r.df();
            }
        }
        d /= k;
        double df = se2 == 0 ? Double.POSITIVE_INFINITY : se2 * se2 / denominator;
        return Optional.of(interval(d, se2 / ((double) k * k), df, k));
    }

    private static Ratio interval(double d, double se2, double df, int cells) {
        double se = Math.sqrt(se2);
        double h = tQuantile(0.5 + CONFIDENCE / 2, df) * se;
        double ratio = Math.exp(d);
        double low = Math.exp(d - h);
        double high = Math.exp(d + h);
        return new Ratio(ratio, d, se, df, low, high, (high - low) / (2 * ratio), cells);
    }

    private static Optional<double[]> logs(double[] x) {
        double[] l = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            if (!(x[i] > 0) || !Double.isFinite(x[i])) {
                return Optional.empty();
            }
            l[i] = Math.log(x[i]);
        }
        return Optional.of(l);
    }

    /**
     * The {@code p} quantile of Student's t with {@code df} degrees of freedom ({@code df} may be fractional; for an
     * infinite {@code df}, the standard normal). Bisection on {@link #tCdf} to 1e-12.
     */
    public static double tQuantile(double p, double df) {
        if (!(p > 0 && p < 1) || !(df > 0)) {
            throw new IllegalArgumentException("p in (0,1) and df > 0 required: p=" + p + " df=" + df);
        }
        if (p == 0.5) {
            return 0;
        }
        if (p < 0.5) {
            return -tQuantile(1 - p, df);
        }
        double lo = 0;
        double hi = 1;
        while (tCdf(hi, df) < p) {
            hi *= 2;
        }
        for (int i = 0; i < 200 && hi - lo > 1e-12 * Math.max(1, hi); i++) {
            double mid = (lo + hi) / 2;
            if (tCdf(mid, df) < p) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2;
    }

    /** Student's t cumulative distribution function. */
    public static double tCdf(double t, double df) {
        if (Double.isInfinite(df)) {
            return normalCdf(t);
        }
        double x = df / (df + t * t);
        double tail = 0.5 * regularizedBeta(x, df / 2, 0.5);
        return t >= 0 ? 1 - tail : tail;
    }

    private static double normalCdf(double z) {
        return 0.5 * erfc(-z / Math.sqrt(2));
    }

    /** Complementary error function (Numerical Recipes erfcc, relative error below 1.2e-7) refined by Newton. */
    private static double erfc(double x) {
        double z = Math.abs(x);
        double t = 1 / (1 + 0.5 * z);
        double r = t * Math.exp(-z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418
                   + t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587
                   + t * (-0.82215223 + t * 0.17087277)))))))));
        return x >= 0 ? r : 2 - r;
    }

    /** The regularized incomplete beta function I_x(a, b) (Lentz continued fraction). */
    static double regularizedBeta(double x, double a, double b) {
        if (x <= 0) {
            return 0;
        }
        if (x >= 1) {
            return 1;
        }
        double front = Math.exp(lnGamma(a + b) - lnGamma(a) - lnGamma(b) + a * Math.log(x) + b * Math.log(1 - x));
        if (x < (a + 1) / (a + b + 2)) {
            return front * betaContinuedFraction(x, a, b) / a;
        }
        return 1 - front * betaContinuedFraction(1 - x, b, a) / b;
    }

    private static double betaContinuedFraction(double x, double a, double b) {
        double tiny = 1e-300;
        double qab = a + b;
        double qap = a + 1;
        double qam = a - 1;
        double c = 1;
        double d = 1 - qab * x / qap;
        d = Math.abs(d) < tiny ? tiny : d;
        d = 1 / d;
        double h = d;
        for (int m = 1; m <= 10_000; m++) {
            int m2 = 2 * m;
            double aa = m * (b - m) * x / ((qam + m2) * (a + m2));
            d = 1 + aa * d;
            d = Math.abs(d) < tiny ? tiny : d;
            c = 1 + aa / c;
            c = Math.abs(c) < tiny ? tiny : c;
            d = 1 / d;
            h *= d * c;
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
            d = 1 + aa * d;
            d = Math.abs(d) < tiny ? tiny : d;
            c = 1 + aa / c;
            c = Math.abs(c) < tiny ? tiny : c;
            d = 1 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < 1e-15) {
                break;
            }
        }
        return h;
    }

    /** Lanczos approximation (g = 7, n = 9) of ln Gamma(x), x > 0. */
    static double lnGamma(double x) {
        double[] g = {0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
                      -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6,
                      1.5056327351493116e-7};
        if (x < 0.5) {
            return Math.log(Math.PI / Math.abs(Math.sin(Math.PI * x))) - lnGamma(1 - x);
        }
        double y = x - 1;
        double a = g[0];
        double t = y + 7.5;
        for (int i = 1; i < 9; i++) {
            a += g[i] / (y + i);
        }
        return 0.5 * Math.log(2 * Math.PI) + (y + 0.5) * Math.log(t) - t + Math.log(a);
    }

    /** A number member, or a non-finite value written as its string ({@code "Infinity"}): JSON has no infinity. */
    static double d(ObjectNode n, String member) {
        Node v = n.expectMember(member);
        return v.isStringNode() ? Double.parseDouble(v.expectStringNode().getValue())
                                : v.expectNumberNode().getValue().doubleValue();
    }

    static Node number(double v) {
        return Double.isFinite(v) ? Node.from(v) : Node.from(Double.toString(v));
    }
}
