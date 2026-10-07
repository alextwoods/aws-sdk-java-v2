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
 * Root package of the {@code software.amazon.awssdk:cloudwatch-metric-publisher} V3 artifact.
 *
 * <p>Owns: the opt-in CloudWatch telemetry exporter
 * ({@link software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher}), a V2-compatible
 * {@code MetricPublisher} over the generated V3 CloudWatch client (Requirement 15.7).
 *
 * <p>Allowed V3 module dependencies: {@code telemetry-spi} and the generated {@code cloudwatch} service. No core,
 * service or high-level-library module may depend on it.
 */
package software.amazon.awssdk.metrics.publishers.cloudwatch;
