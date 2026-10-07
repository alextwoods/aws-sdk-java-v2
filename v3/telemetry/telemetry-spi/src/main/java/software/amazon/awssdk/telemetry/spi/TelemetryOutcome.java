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

package software.amazon.awssdk.telemetry.spi;

/** The terminal outcome of a call or an attempt. */
public enum TelemetryOutcome {
    /**
     * The call returned its result (a presign call: its presigned request), or the attempt produced the output the
     * call returned or a later hook replaced.
     */
    SUCCEEDED,
    /** The call surfaced a failure, or the attempt failed (and the call retried it or surfaced a failure). */
    FAILED,
    /** The call was cancelled (by its caller or by an interrupt), or the attempt was running when it was. */
    CANCELLED
}
