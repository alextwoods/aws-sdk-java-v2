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

package software.amazon.awssdk.http;

/** Why an exchange is cancelled ({@link AsyncHttpExchange#cancel}, {@link SyncHttpExchange#abort}). */
public enum CancellationReason {
    /** The caller cancelled the call, for example through the returned future. */
    CALLER_CANCELLED,
    /** The whole-call {@code apiCallTimeout} elapsed. */
    API_CALL_TIMEOUT,
    /** The attempt's {@code apiCallAttemptTimeout} elapsed. */
    API_CALL_ATTEMPT_TIMEOUT,
    /** The response was abandoned before its body was consumed, for example after an error was decided. */
    RESPONSE_ABANDONED,
    /** The client was closed while the exchange was in flight. */
    CLIENT_CLOSED
}
