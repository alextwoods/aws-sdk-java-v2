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

package software.amazon.awssdk.v2migration.manifest;

/**
 * The Compatibility Classification of a Parity Manifest entry: the {@code classification} member of
 * {@code parity-manifest-1.schema.json}, exactly one per entry (Requirement 16.3).
 */
public enum Classification {
    PRESERVED,
    ADAPTED,
    EXCLUDED
}
