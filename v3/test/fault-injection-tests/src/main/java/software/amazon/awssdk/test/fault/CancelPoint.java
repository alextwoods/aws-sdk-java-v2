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

package software.amazon.awssdk.test.fault;

/**
 * Where the caller cancels a call: the future of an async call, or (sync driver) an interrupt of the calling thread.
 * Each point is reached deterministically: the fault that parks the call there is part of the same trace.
 */
public enum CancelPoint {
    /** Not cancelled. */
    NONE,
    /** While the call waits for an identity ({@link IdentityStep#HANG}). Both drivers. */
    IDENTITY,
    /** While the transport holds the exchange without a response ({@link TransportFault.Hang}). Async driver. */
    TRANSMISSION,
    /** While the response body is being received ({@link TransportFault.HangBody}). Async driver. */
    RESPONSE_BODY,
    /** While the call waits out a retry delay. Both drivers (sync: the interrupted sleep). */
    RETRY_DELAY
}
