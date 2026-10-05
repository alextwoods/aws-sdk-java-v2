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

import java.util.Objects;

/**
 * One raw, unclassified V2 baseline inventory item produced by an {@link InventorySource}.
 *
 * @param id         the stable entry ID, following {@code kind}'s scheme (see {@link EntryKind})
 * @param kind       the entry kind
 * @param v2Surface  a human-readable rendering of the V2 surface, e.g. {@code RetryPolicy.builder()}
 * @param source     where the item was found
 * @param deprecated whether the V2 baseline marks the item deprecated
 */
public record InventoryItem(String id, EntryKind kind, String v2Surface, SourceLocation source, boolean deprecated) {
    public InventoryItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(v2Surface, "v2Surface");
        Objects.requireNonNull(source, "source");
    }
}
