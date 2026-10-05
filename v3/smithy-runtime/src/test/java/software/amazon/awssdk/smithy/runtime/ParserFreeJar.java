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

package software.amazon.awssdk.smithy.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringJoiner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary;

/**
 * Writes a copy of a jar with every class outside the {@link SmithyRuntimeBoundary} removed, together with service
 * registrations for removed service interfaces or providers. Applied to {@code smithy-model}, the result is the
 * lightweight shape-identity/runtime-trait artifact the boundary requires: it keeps {@code ShapeId}, {@code ShapeType},
 * traits and nodes and drops the parser, assembler, validators, selectors, knowledge indexes and transforms.
 */
final class ParserFreeJar {

    private static final String SERVICES = "META-INF/services/";
    private static final String VERSIONS = "META-INF/versions/";

    private ParserFreeJar() {
    }

    /**
     * Copies {@code source} to {@code target} without boundary-forbidden content.
     *
     * @return the binary names of the removed classes.
     */
    static List<String> write(Path source, Path target) {
        List<String> removed = new ArrayList<>();
        try (ZipFile zip = new ZipFile(source.toFile());
             OutputStream out = Files.newOutputStream(target);
             ZipOutputStream zipOut = new ZipOutputStream(out)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                byte[] content = read(zip, entry);
                if (name.endsWith(".class")) {
                    String binaryName = binaryName(name);
                    if (SmithyRuntimeBoundary.isForbiddenRuntimeClass(binaryName)) {
                        removed.add(binaryName);
                        continue;
                    }
                } else if (name.startsWith(SERVICES) && !entry.isDirectory()) {
                    if (SmithyRuntimeBoundary.isForbiddenRuntimeClass(name.substring(SERVICES.length()))) {
                        continue;
                    }
                    content = withoutForbiddenProviders(content);
                }
                zipOut.putNextEntry(new ZipEntry(name));
                zipOut.write(content);
                zipOut.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return removed;
    }

    private static String binaryName(String entryName) {
        String path = entryName;
        if (path.startsWith(VERSIONS)) {
            int classPathStart = path.indexOf('/', VERSIONS.length());
            path = path.substring(classPathStart + 1);
        }
        return path.substring(0, path.length() - ".class".length()).replace('/', '.');
    }

    private static byte[] withoutForbiddenProviders(byte[] content) {
        StringJoiner kept = new StringJoiner("\n", "", "\n");
        for (String line : new String(content, StandardCharsets.UTF_8).split("\n")) {
            String provider = line.replaceFirst("#.*", "").trim();
            if (provider.isEmpty() || !SmithyRuntimeBoundary.isForbiddenRuntimeClass(provider)) {
                kept.add(line);
            }
        }
        return kept.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] read(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }
}
