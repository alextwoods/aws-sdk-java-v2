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
 * Migration recipes for the V2 runtime extension surfaces and high-level libraries (Requirements 11.12, 17.4-17.7,
 * 17.9, 17.11; design "Migration tooling flow" and "Interceptor migration rules"): interceptor hooks and typed
 * contexts ({@code interceptors.hooks}), HTTP SPI types and options ({@code http.replacements}), sync and async body,
 * response and publisher types ({@code streaming.replacements}), paginators ({@code pagination.replacements}),
 * waiters ({@code waiters.replacements}), the DynamoDB Enhanced Client, S3 Transfer Manager, IAM Policy Builder, S3
 * Event Notifications and SNS Message Manager ({@code hll.replacements}), the known V3 launch gaps of the
 * high-level-library parity matrix ({@code hll.known-gaps}), and the imports those conversions leave unused
 * ({@code conversions.unused-imports}).
 *
 * <p>Every conversion follows the Parity Manifest: only {@code ADAPTED} entries with an automated recipe migration
 * and a named replacement are converted, to exactly that replacement; {@code PRESERVED}, diagnostic and
 * {@code EXCLUDED} entries keep the engine's manifest disposition, and known gaps are always unresolved. Recipes edit
 * only within their own items' ranges; of the imports they rewrite only the single-type imports of their own
 * families' types, so a converted name is written unqualified only when the unit's imports resolve it after that.
 */
package software.amazon.awssdk.v2migration.recipes.runtime;
