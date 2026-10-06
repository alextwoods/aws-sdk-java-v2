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

import java.util.Comparator;
import java.util.Objects;

/**
 * One encountered V2 construct: a dependency, BOM, import, type, member use, hook or configuration key at one source
 * location. Every item receives exactly one {@link Disposition} (Requirement 17.10).
 *
 * @param kind      the syntactic form, which defines what {@code location} spans
 * @param id        the Parity Manifest entry ID the construct joins by (for example
 *                  {@code api:software.amazon.awssdk.regions.Region#of(Ljava/lang/String;)Lsoftware/amazon/awssdk/regions/Region;}),
 *                  or {@code null} when the construct could not be identified
 * @param category  the migration concern
 * @param construct the affected construct as it appears in source, whitespace-normalized and abbreviated
 * @param location  where the construct is
 * @param detail    why the item could not be joined exactly (for example an unattributed type), or {@code null}
 */
public record MigrationItem(ItemKind kind, String id, MigrationCategory category, String construct,
                            SourceLocation location, String detail) implements Comparable<MigrationItem> {

    private static final Comparator<MigrationItem> ORDER =
        Comparator.comparing(MigrationItem::location)
                  .thenComparing(MigrationItem::kind)
                  .thenComparing(i -> i.id() == null ? "" : i.id())
                  .thenComparing(MigrationItem::construct);

    public MigrationItem {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(construct, "construct");
        Objects.requireNonNull(location, "location");
    }

    /** An item whose category is derived from its kind and ID. */
    public static MigrationItem of(ItemKind kind, String id, String construct, SourceLocation location,
                                   String detail) {
        return new MigrationItem(kind, id, MigrationCategory.of(kind, id), construct, location, detail);
    }

    /** Items sort by location, then kind, ID and construct: the deterministic processing and report order. */
    @Override
    public int compareTo(MigrationItem other) {
        return ORDER.compare(this, other);
    }
}
