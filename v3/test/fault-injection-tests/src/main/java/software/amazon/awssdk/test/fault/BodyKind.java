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

/** The streaming request body of a call (see {@link FaultBodies}). */
public enum BodyKind {
    /** The operation has no streaming input body. */
    NONE,
    /** Replayable: every attempt opens a fresh stream (subscription) over the same bytes. */
    REPLAYABLE,
    /** Readable once: the pipeline must not retry a call whose body the transport began to consume. */
    ONE_SHOT,
    /** Replayable, but the first stream (subscription) fails midway with an {@link java.io.IOException}. */
    FAILING_ONCE
}
