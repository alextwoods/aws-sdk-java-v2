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

package software.amazon.awssdk.archtests.portcompliance.fixtures;

/**
 * Negative fixture for {@code PortComplianceCheckerTest}: a javac-compiled public top-level type with no SDK visibility
 * annotation. It is test-only and never part of a ported module.
 */
public final class UnannotatedPublicPortFixture {

    /**
     * Nested public types inherit the enclosing type's visibility, so this one needs no annotation of its own.
     */
    public static final class Nested {
    }
}
