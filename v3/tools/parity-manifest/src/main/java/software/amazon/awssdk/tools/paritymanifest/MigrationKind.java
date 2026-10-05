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

package software.amazon.awssdk.tools.paritymanifest;

/**
 * How a migration action is delivered. {@code ADAPTED} entries need a {@link #RECIPE} or a source-located
 * {@link #DIAGNOSTIC} (Requirement 17.4); {@code EXCLUDED} entries need a {@link #COMPILE_TIME} or Migration Tooling
 * {@link #DIAGNOSTIC} (Requirement 16.13).
 */
public enum MigrationKind {
    /** An automated Migration Tooling recipe. */
    RECIPE,
    /** A source-located Migration Tooling diagnostic describing the manual change. */
    DIAGNOSTIC,
    /** A compile-time failure against V3 (the V2 symbol is absent) with a manifest-linked message. */
    COMPILE_TIME
}
