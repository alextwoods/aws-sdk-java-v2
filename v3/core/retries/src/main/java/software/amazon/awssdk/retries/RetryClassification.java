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

package software.amazon.awssdk.retries;

/** Retryability of one failed attempt, as decided by a {@link RetryClassifier}. */
public enum RetryClassification {
    /** The service is throttling: retry with the throttling backoff and quota cost. */
    THROTTLING,
    /** A transient service or transport failure: retry with the standard backoff and quota cost. */
    TRANSIENT,
    /** Another attempt cannot be expected to succeed, or must not be sent. */
    NON_RETRYABLE;

    /** Whether another attempt may be made for a failure of this classification. */
    public boolean isRetryable() {
        return this != NON_RETRYABLE;
    }
}
