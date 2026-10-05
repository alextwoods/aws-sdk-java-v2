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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@code artifact:<groupId>:<artifactId>} for every published module of the V2 aggregator tree ({@link V2PomTree}
 * documents the published criterion). No V2 POM declares deprecation, so artifacts are reported non-deprecated;
 * deprecated APIs inside an artifact are inventoried by {@link PublicApiInventorySource}.
 */
public final class ArtifactInventorySource implements InventorySource {

    @Override
    public String name() {
        return "v2-artifacts";
    }

    @Override
    public List<InventoryItem> scan(Context context) throws IOException {
        List<InventoryItem> items = new ArrayList<>();
        for (V2PomTree.Module m : V2PomTree.modules(context.repositoryRoot())) {
            if (!m.published()) {
                continue;
            }
            String pom = m.path().isEmpty() ? "pom.xml" : m.path() + "/pom.xml";
            items.add(new InventoryItem(EntryKind.artifactId(m.groupId(), m.artifactId()), EntryKind.ARTIFACT,
                                        m.packaging() + " artifact " + m.groupId() + ":" + m.artifactId(),
                                        new SourceLocation(m.artifactId(), pom), false));
        }
        items.sort(Comparator.comparing(InventoryItem::id));
        return List.copyOf(items);
    }
}
