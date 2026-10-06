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

package software.amazon.awssdk.v2migration;

/** The single outcome recorded for every encountered migration item (Requirement 17.10). */
public enum Disposition {
    /** The construct is kept unchanged because V3 preserves it. */
    PRESERVED,
    /** A deterministic recipe rewrote the construct. */
    TRANSFORMED,
    /** The construct is left byte-for-byte unchanged and a blocking, source-located diagnostic is emitted. */
    UNRESOLVED
}
