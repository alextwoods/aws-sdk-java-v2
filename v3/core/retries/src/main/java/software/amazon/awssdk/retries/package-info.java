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
 * Root package of the {@code software.amazon.awssdk:retries} V3 artifact: the Modern Retry strategies.
 *
 * <p>Owns: Retry strategies, attempt/quota/token accounting, and retry classification.
 *
 * <p>The AWS pipeline's retry owner drives a {@link software.amazon.awssdk.retries.RetryStrategy} through one token
 * per attempt (Requirements 10.6-10.8):
 * <ol>
 *   <li>{@link software.amazon.awssdk.retries.RetryStrategy#acquireInitialToken} before attempt 1, waiting the
 *       returned delay (non-zero only for the adaptive rate limiter);</li>
 *   <li>after a failed attempt, {@link software.amazon.awssdk.retries.RetryStrategy#refreshRetryToken} with an
 *       {@link software.amazon.awssdk.retries.AttemptFailure}; a
 *       {@link software.amazon.awssdk.retries.RetryDecision.Retry} authorizes exactly one next attempt after its
 *       delay, and a {@link software.amazon.awssdk.retries.RetryDecision.Stop} authorizes none;</li>
 *   <li>after a successful attempt, {@link software.amazon.awssdk.retries.RetryStrategy#recordSuccess}; after a
 *       terminal failure that was not refused by a refresh (cancellation, whole-call timeout),
 *       {@link software.amazon.awssdk.retries.RetryStrategy#recordFailure}.</li>
 * </ol>
 * Delays are values, never sleeps inside the strategy: a sync driver sleeps, and an async driver schedules the next
 * attempt through a {@link software.amazon.awssdk.retries.RetryScheduler} without blocking an event loop.
 *
 * <p>Allowed V3 module dependencies: {@code sdk-core}, {@code smithy-runtime} (error Schema retry traits) and
 * {@code http-spi} (transport failure categories and response hints). Forbidden dependencies: generated service
 * artifacts.
 */
package software.amazon.awssdk.retries;
