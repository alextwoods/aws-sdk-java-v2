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

package software.amazon.awssdk.sizereport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The parts of a JAR file that the size report measures: its compressed size, its {@code .class} entries, and a
 * content fingerprint of every entry.
 */
@SdkInternalApi
final class JarContents {
    private static final String CLASS_SUFFIX = ".class";
    private static final String MANIFEST = "META-INF/MANIFEST.MF";

    private final long compressedBytes;
    private final Map<String, byte[]> classes;
    private final Map<String, Long> entryCrcs;
    private final String buildJdkSpec;

    JarContents(long compressedBytes, Map<String, byte[]> classes, Map<String, Long> entryCrcs, String buildJdkSpec) {
        this.compressedBytes = compressedBytes;
        this.classes = Collections.unmodifiableMap(new TreeMap<>(classes));
        this.entryCrcs = Collections.unmodifiableMap(new TreeMap<>(entryCrcs));
        this.buildJdkSpec = buildJdkSpec;
    }

    /**
     * Read a JAR file.
     */
    public static JarContents read(Path jar) throws IOException {
        Map<String, byte[]> classes = new TreeMap<>();
        Map<String, Long> crcs = new TreeMap<>();
        String buildJdkSpec = null;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (MANIFEST.equals(name)) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        Attributes attributes = new Manifest(in).getMainAttributes();
                        buildJdkSpec = attributes.getValue(new Attributes.Name("Build-Jdk-Spec"));
                    }
                    continue;
                }
                if (name.startsWith("META-INF/maven/")) {
                    // Maven metadata (pom.xml, pom.properties) is not part of the module's content.
                    continue;
                }
                crcs.put(name, entry.getCrc());
                if (name.endsWith(CLASS_SUFFIX)) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        classes.put(name.substring(0, name.length() - CLASS_SUFFIX.length()), readAll(in));
                    }
                }
            }
        }
        return new JarContents(Files.size(jar), classes, crcs, buildJdkSpec);
    }

    /**
     * The size of the JAR file in bytes.
     */
    public long compressedBytes() {
        return compressedBytes;
    }

    /**
     * Every {@code .class} entry, nested, anonymous and synthetic classes included, keyed by internal name
     * ({@code a/b/C$D}), sorted by name.
     */
    public Map<String, byte[]> classes() {
        return classes;
    }

    /**
     * The CRC-32 of every file entry except the manifest and {@code META-INF/maven/}, keyed by entry name. Two JARs
     * with equal maps have the same content.
     */
    public Map<String, Long> entryCrcs() {
        return entryCrcs;
    }

    /**
     * The manifest's {@code Build-Jdk-Spec} (the major version of the JDK that built the JAR), or {@code null}.
     */
    public String buildJdkSpec() {
        return buildJdkSpec;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
