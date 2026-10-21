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

package software.amazon.awssdk.benchmark.schema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * The valid JSON samples of the S3 Event Notifications test suite ({@code services-custom/s3-event-notifications},
 * {@code src/test/resources/samples/valid}), which the module build copies into
 * {@value #DIRECTORY} on the classpath.
 */
public enum S3EventNotificationSampleSet {
    /** Every valid sample of the test suite, in file-name order. */
    ALL_SAMPLES,
    /** One typical notification: the S3 User Guide's {@code ObjectCreated:Put} event. */
    OBJECT_CREATED_PUT;

    static final String DIRECTORY = "s3-event-notifications/samples/valid";
    private static final String OBJECT_CREATED_PUT_FILE = "docs-object-created-put.json";

    /**
     * The samples of this set: file name to JSON text (decoded as UTF-8), in file-name order.
     */
    public Map<String, String> load() {
        Map<String, String> all = loadAll();
        if (this == ALL_SAMPLES) {
            return all;
        }
        String json = all.get(OBJECT_CREATED_PUT_FILE);
        if (json == null) {
            throw new IllegalStateException("No sample " + OBJECT_CREATED_PUT_FILE + " in " + DIRECTORY);
        }
        return Collections.singletonMap(OBJECT_CREATED_PUT_FILE, json);
    }

    private static Map<String, String> loadAll() {
        Map<String, String> samples = new TreeMap<>();
        try {
            Path location = Paths.get(S3EventNotificationSampleSet.class.getProtectionDomain().getCodeSource()
                                                                      .getLocation().toURI());
            if (Files.isDirectory(location)) {
                readDirectory(location.resolve(DIRECTORY), samples);
            } else {
                readJar(location, samples);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        if (samples.isEmpty()) {
            throw new IllegalStateException("No JSON samples in " + DIRECTORY + "; build the module with Maven, which "
                                            + "copies them from services-custom/s3-event-notifications");
        }
        return Collections.unmodifiableMap(samples);
    }

    private static void readDirectory(Path directory, Map<String, String> samples) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String name = file.getFileName().toString();
                if (name.endsWith(".json")) {
                    samples.put(name, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
                }
            }
        }
    }

    private static void readJar(Path jar, Map<String, String> samples) throws IOException {
        String prefix = DIRECTORY + "/";
        try (JarFile file = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(prefix) || !name.endsWith(".json")
                    || name.indexOf('/', prefix.length()) >= 0) {
                    continue;
                }
                try (InputStream in = file.getInputStream(entry)) {
                    samples.put(name.substring(prefix.length()), new String(readAll(in), StandardCharsets.UTF_8));
                }
            }
        }
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
