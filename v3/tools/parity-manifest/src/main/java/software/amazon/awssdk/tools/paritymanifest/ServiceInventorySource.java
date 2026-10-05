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
 * {@code service:<module>} for every V2 service client module: each {@code services/<module>} directory module of the
 * aggregator tree (Requirement 16.4).
 *
 * <p>Deprecation: V2 has no service-level deprecation marker. A service's
 * {@code src/main/resources/codegen-resources/customization.config} only deprecates individual operations and shapes
 * ({@code deprecatedOperations}, {@code deprecatedShapes}), and those surface as {@code @Deprecated} generated APIs
 * that {@link PublicApiInventorySource} records. Services are therefore reported non-deprecated.
 */
public final class ServiceInventorySource implements InventorySource {

    @Override
    public String name() {
        return "v2-services";
    }

    @Override
    public List<InventoryItem> scan(Context context) throws IOException {
        List<InventoryItem> items = new ArrayList<>();
        for (V2PomTree.Module m : V2PomTree.modules(context.repositoryRoot())) {
            String path = m.path();
            if (!path.startsWith("services/") || path.indexOf('/', "services/".length()) >= 0) {
                continue;
            }
            String module = path.substring("services/".length());
            items.add(new InventoryItem("service:" + module, EntryKind.SERVICE,
                                        "AWS service client " + m.groupId() + ":" + m.artifactId(),
                                        new SourceLocation(m.artifactId(), path + "/pom.xml"), false));
        }
        items.sort(Comparator.comparing(InventoryItem::id));
        return List.copyOf(items);
    }
}
