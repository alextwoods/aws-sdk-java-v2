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

import java.util.List;
import java.util.Objects;

/** An {@link InventorySource} over a fixed item list, for hand-curated inventories and tests. */
public final class StaticInventorySource implements InventorySource {
    private final String name;
    private final List<InventoryItem> items;

    public StaticInventorySource(String name, List<InventoryItem> items) {
        this.name = Objects.requireNonNull(name, "name");
        this.items = List.copyOf(items);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<InventoryItem> scan(Context context) {
        return items;
    }
}
