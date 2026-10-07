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
 * The V2-compatible metrics API ({@code MetricPublisher}, {@code MetricCollection}, {@code SdkMetric} and friends),
 * shipped by {@code software.amazon.awssdk:telemetry-spi} in place of V2's {@code metrics-spi}. The V3 pipeline
 * delivers its terminal call and attempt records ({@code software.amazon.awssdk.telemetry.spi}) to a configured
 * {@link software.amazon.awssdk.metrics.MetricPublisher} as one V2-shaped {@code ApiCall} collection per call.
 */
package software.amazon.awssdk.metrics;
