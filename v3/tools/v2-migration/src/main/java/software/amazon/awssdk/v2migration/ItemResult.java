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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;

/**
 * The single disposition of one item.
 *
 * <ul>
 *   <li>{@link Disposition#PRESERVED}: no edits and no diagnostic; {@code recipe} names the recipe that judged the
 *       construct preserved, or is {@code null} when the manifest classification ({@code PRESERVED}) decided.</li>
 *   <li>{@link Disposition#TRANSFORMED}: the recipe and its applied edits; no diagnostic.</li>
 *   <li>{@link Disposition#UNRESOLVED}: no edits and a diagnostic; {@code recipe} names the recipe that declined, if
 *       one did.</li>
 * </ul>
 *
 * @param item        the encountered item
 * @param disposition the single disposition
 * @param entry       the item's Parity Manifest entry, or {@code null} when it has none
 * @param recipe      the deciding recipe ID, or {@code null}
 * @param reason      the decision's explanation (preservation rationale, transformation summary or unresolved reason)
 * @param edits       the applied edits ({@code TRANSFORMED} only)
 * @param diagnostic  the blocking diagnostic ({@code UNRESOLVED} only)
 */
public record ItemResult(MigrationItem item,
                         Disposition disposition,
                         ManifestEntry entry,
                         String recipe,
                         String reason,
                         List<TextEdit> edits,
                         MigrationDiagnostic diagnostic) {

    public ItemResult {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(disposition, "disposition");
        edits = edits == null ? List.of() : List.copyOf(edits);
        if ((disposition == Disposition.TRANSFORMED) == edits.isEmpty()) {
            throw new IllegalArgumentException("Exactly the TRANSFORMED disposition carries edits: " + item);
        }
        if ((disposition == Disposition.UNRESOLVED) != (diagnostic != null)) {
            throw new IllegalArgumentException("Exactly the UNRESOLVED disposition carries a diagnostic: " + item);
        }
    }

    /** The item's manifest entry, if it has one. */
    public Optional<ManifestEntry> manifestEntry() {
        return Optional.ofNullable(entry);
    }
}
