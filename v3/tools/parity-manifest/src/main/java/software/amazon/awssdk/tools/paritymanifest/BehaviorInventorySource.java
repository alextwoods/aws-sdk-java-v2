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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * {@code behavior:<id>} for every documented V2 customer behavior in the curated, checked-in
 * {@link #DEFAULT_RESOURCE} inventory (Requirements 16.2 and 16.10 name the categories). Each behavior has a stable
 * {@code id}, a {@code surface} description, the V2 {@code artifact} that owns it, a {@code reference} to the V2
 * documentation or source that defines it, and an optional {@code deprecated} flag.
 */
public final class BehaviorInventorySource implements InventorySource {

    public static final String DEFAULT_RESOURCE = "/software/amazon/awssdk/tools/paritymanifest/v2-behaviors.json";

    @Override
    public String name() {
        return "v2-behaviors";
    }

    @Override
    public List<InventoryItem> scan(Context context) throws IOException {
        try (InputStream in = BehaviorInventorySource.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IOException("Missing " + DEFAULT_RESOURCE);
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /** Parses a behavior inventory document. */
    static List<InventoryItem> parse(String json) {
        ObjectNode root = Node.parse(json).expectObjectNode();
        List<InventoryItem> items = new ArrayList<>();
        for (Node node : root.expectArrayMember("behaviors").getElements()) {
            ObjectNode b = node.expectObjectNode();
            String id = "behavior:" + b.expectStringMember("id").getValue();
            items.add(new InventoryItem(id, EntryKind.BEHAVIOR, b.expectStringMember("surface").getValue(),
                                        new SourceLocation(b.expectStringMember("artifact").getValue(),
                                                           b.expectStringMember("reference").getValue()),
                                        b.getBooleanMemberOrDefault("deprecated", false)));
        }
        items.sort(Comparator.comparing(InventoryItem::id));
        return List.copyOf(items);
    }
}
