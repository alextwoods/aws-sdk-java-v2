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

import java.util.Locale;

/**
 * A dimension in which an entry is compatible with the V2 baseline (design "Parity Manifest schema": source | JVM
 * descriptor | behavior | configuration). An entry lists the dimensions it claims; the others are not claimed.
 */
public enum CompatibilityDimension {
    /** V2 source using the entry compiles unchanged (Requirements 17.1, 17.3). */
    SOURCE,
    /** Compiled V2 caller bytecode links and runs: the JVM descriptor is preserved (Requirement 17.2). */
    BINARY,
    /** The documented runtime behavior is preserved. */
    BEHAVIOR,
    /** The configuration names, sources and value semantics are preserved. */
    CONFIGURATION;

    /** The JSON name: the lower-case constant name. */
    public String jsonName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses a {@linkplain #jsonName() JSON name}. */
    public static CompatibilityDimension fromJsonName(String name) {
        for (CompatibilityDimension d : values()) {
            if (d.jsonName().equals(name)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown compatibility dimension: " + name);
    }
}
