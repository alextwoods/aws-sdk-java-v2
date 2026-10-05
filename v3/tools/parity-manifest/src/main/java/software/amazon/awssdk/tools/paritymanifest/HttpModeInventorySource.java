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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@code http-mode:<module>:<sync|async>} for every V2 HTTP Implementation mode (Requirement 12.10): each
 * {@code http-clients/<module>} that registers a {@code software.amazon.awssdk.http.SdkHttpService} (sync) or
 * {@code software.amazon.awssdk.http.async.SdkAsyncHttpService} (async) provider in
 * {@code src/main/resources/META-INF/services}. The surface names the registered provider classes.
 */
public final class HttpModeInventorySource implements InventorySource {

    static final String SYNC_SERVICE = "software.amazon.awssdk.http.SdkHttpService";
    static final String ASYNC_SERVICE = "software.amazon.awssdk.http.async.SdkAsyncHttpService";

    @Override
    public String name() {
        return "v2-http-modes";
    }

    @Override
    public List<InventoryItem> scan(Context context) throws IOException {
        Path clients = context.repositoryRoot().resolve("http-clients");
        List<InventoryItem> items = new ArrayList<>();
        if (!Files.isDirectory(clients)) {
            throw new IOException("No V2 http-clients directory at " + clients);
        }
        List<Path> modules;
        try (Stream<Path> s = Files.list(clients)) {
            modules = s.filter(p -> Files.isRegularFile(p.resolve("pom.xml"))).sorted().toList();
        }
        for (Path module : modules) {
            String name = module.getFileName().toString();
            addMode(items, module, name, SYNC_SERVICE, false);
            addMode(items, module, name, ASYNC_SERVICE, true);
        }
        items.sort(Comparator.comparing(InventoryItem::id));
        return List.copyOf(items);
    }

    private static void addMode(List<InventoryItem> items, Path module, String name, String service, boolean async)
        throws IOException {
        String relative = "src/main/resources/META-INF/services/" + service;
        Path file = module.resolve(relative);
        if (!Files.isRegularFile(file)) {
            return;
        }
        List<String> providers = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                                      .map(l -> l.replaceAll("#.*", "").strip())
                                      .filter(l -> !l.isEmpty()).sorted().toList();
        if (providers.isEmpty()) {
            return;
        }
        items.add(new InventoryItem(EntryKind.httpModeId(name, async), EntryKind.HTTP_MODE,
                                    (async ? "async" : "sync") + " HTTP Implementation " + String.join(", ", providers)
                                    + " (" + service + ")",
                                    new SourceLocation(name, "http-clients/" + name + "/" + relative), false));
    }
}
