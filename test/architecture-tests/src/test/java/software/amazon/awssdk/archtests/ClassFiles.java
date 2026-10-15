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

package software.amazon.awssdk.archtests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads compiled classes for the ASM-based architecture checks ({@code PortComplianceChecker},
 * {@code MarshallerDependencyScanner}).
 */
public final class ClassFiles {

    private static final String CLASS_SUFFIX = ".class";

    private ClassFiles() {
    }

    /**
     * Read every class file under {@code root}, a directory of class files or a jar. Entries under {@code META-INF/}
     * (multi-release variants, {@code module-info}) are skipped.
     *
     * @return the class file bytes keyed by internal class name (for example {@code a/b/C$D}), sorted by name
     */
    public static Map<String, byte[]> read(Path root) throws IOException {
        Map<String, byte[]> result = new TreeMap<>();
        if (Files.isDirectory(root)) {
            List<Path> classFiles;
            try (Stream<Path> paths = Files.walk(root)) {
                classFiles = paths.filter(f -> f.toString().endsWith(CLASS_SUFFIX)).collect(Collectors.toList());
            }
            for (Path p : classFiles) {
                String entry = root.relativize(p).toString().replace(File.separatorChar, '/');
                if (!entry.startsWith("META-INF/")) {
                    result.put(internalName(entry), Files.readAllBytes(p));
                }
            }
        } else {
            try (ZipFile zip = new ZipFile(root.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(CLASS_SUFFIX)
                        && !entry.getName().startsWith("META-INF/")) {
                        try (InputStream in = zip.getInputStream(entry)) {
                            result.put(internalName(entry.getName()), readAll(in));
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Locate the jar or class directory of an SDK artifact on the test classpath.
     *
     * @return the location, or {@code null} if it is not on the classpath
     */
    public static Path locateArtifact(String artifactId) throws IOException {
        // Packaged jars (local repository or reactor "package" phase) carry Maven metadata.
        String pomProperties = "META-INF/maven/software.amazon.awssdk/" + artifactId + "/pom.properties";
        URL url = ClassFiles.class.getClassLoader().getResource(pomProperties);
        if (url != null && "jar".equals(url.getProtocol())) {
            String spec = url.getPath();
            String jarUri = spec.substring(0, spec.indexOf("!/"));
            try {
                return new File(new URI(jarUri)).toPath();
            } catch (URISyntaxException e) {
                throw new IOException("Cannot resolve " + url, e);
            }
        }
        // Reactor builds that stop before "package" resolve to <module>/target/classes.
        String suffix = File.separator + artifactId + File.separator + "target" + File.separator + "classes";
        for (String entry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
            if (entry.endsWith(suffix)) {
                return new File(entry).toPath();
            }
        }
        return null;
    }

    /**
     * Read an input stream fully.
     */
    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static String internalName(String classFileEntry) {
        return classFileEntry.substring(0, classFileEntry.length() - CLASS_SUFFIX.length());
    }
}
