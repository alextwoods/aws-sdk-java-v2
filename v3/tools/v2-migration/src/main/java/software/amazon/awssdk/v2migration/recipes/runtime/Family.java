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

package software.amazon.awssdk.v2migration.recipes.runtime;

import java.util.Arrays;
import java.util.Optional;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.MigrationCategory;
import software.amazon.awssdk.v2migration.manifest.EntryKind;

/**
 * The V2 surfaces these recipes own, by the binary name of the owning V2 type: the engine's migration categories for
 * interceptors, HTTP, streaming and high-level libraries, and the pagination and waiter runtime packages. The
 * generated service packages (including their {@code paginators} and {@code waiters}) belong to the client recipes.
 */
enum Family {
    INTERCEPTORS,
    HTTP,
    STREAMING,
    HIGH_LEVEL_LIBRARIES,
    PAGINATION,
    WAITERS;

    /** True if this family converts constructs declared by the V2 type {@code ownerBinaryName}. */
    boolean owns(String ownerBinaryName) {
        switch (this) {
            case INTERCEPTORS:
                return category(ownerBinaryName) == MigrationCategory.INTERCEPTOR;
            case HTTP:
                return category(ownerBinaryName) == MigrationCategory.HTTP;
            case STREAMING:
                return category(ownerBinaryName) == MigrationCategory.STREAMING;
            case HIGH_LEVEL_LIBRARIES:
                return category(ownerBinaryName) == MigrationCategory.HIGH_LEVEL_LIBRARY;
            case PAGINATION:
                return ownerBinaryName.startsWith(Symbols.V2_PACKAGE + "core.pagination.");
            case WAITERS:
                return ownerBinaryName.startsWith(Symbols.V2_PACKAGE + "core.waiters.");
            default:
                throw new IllegalStateException(name());
        }
    }

    /** True if some family owns {@code ownerBinaryName}, and so rewrites its single-type imports. */
    static boolean anyOwns(String ownerBinaryName) {
        return of(ownerBinaryName).isPresent();
    }

    /** The family that owns {@code ownerBinaryName}, if any. */
    static Optional<Family> of(String ownerBinaryName) {
        return Arrays.stream(values()).filter(f -> f.owns(ownerBinaryName)).findFirst();
    }

    /**
     * How this family converts overrides of its V2 methods. Interceptor hooks are lifecycle callbacks: a hook that
     * V3 renames runs at another point or frequency, so its body must be free of side effects, calling only the
     * interceptor and HTTP types (and JDK value types).
     */
    OverrideConversion conversion() {
        return this == INTERCEPTORS ? Conversions.HOOKS : Conversions.PLAIN;
    }

    private static final class Conversions {
        static final OverrideConversion HOOKS =
            new OverrideConversion(true, owner -> INTERCEPTORS.owns(owner) || HTTP.owns(owner));
        static final OverrideConversion PLAIN = new OverrideConversion(false, owner -> false);
    }

    /** The engine's migration category of constructs declared by {@code ownerBinaryName}. */
    static MigrationCategory category(String ownerBinaryName) {
        return MigrationCategory.of(ItemKind.TYPE_REFERENCE, EntryKind.typeId(ownerBinaryName));
    }
}
