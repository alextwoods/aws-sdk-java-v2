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

/**
 * Root package of the {@code software.amazon.awssdk:telemetry-spi} V3 artifact.
 *
 * <p>Owns: The optional telemetry contract (Requirements 15.7, 21.2-21.5, 21.14, 21.15): the
 * {@link software.amazon.awssdk.telemetry.spi.TelemetryPublisher} SPI with its terminal
 * {@link software.amazon.awssdk.telemetry.spi.CallRecord} and {@link software.amazon.awssdk.telemetry.spi.AttemptRecord},
 * a client's {@link software.amazon.awssdk.telemetry.spi.TelemetryConfiguration} and its telemetry failure channel
 * ({@link software.amazon.awssdk.telemetry.spi.TelemetryFailureListener}). The same artifact ships the V2-compatible
 * metrics API ({@code software.amazon.awssdk.metrics}, {@code software.amazon.awssdk.core.metrics.CoreMetric}), and
 * {@link software.amazon.awssdk.telemetry.spi.TelemetryPublisher#fromMetricPublisher} bridges a V2 {@code MetricPublisher}.
 * The AWS pipeline emits the records only when a client configures a publisher.
 *
 * <p>Allowed V3 module dependencies: {@code sdk-core}. Forbidden dependencies: telemetry exporters, which are
 * separate opt-in artifacts (for example {@code cloudwatch-metric-publisher}).
 */
package software.amazon.awssdk.telemetry.spi;
