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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Builds small compiled V3 candidate artifacts (class directories and jars) for scanner and gate tests. */
final class CandidateArtifacts {
    /** The demo service package of {@link #service}. */
    static final String DEMO = "software.amazon.awssdk.services.demo";

    private CandidateArtifacts() {
    }

    /**
     * A compiled class directory of artifact {@code software.amazon.awssdk:demo}: a generated service (client with
     * {@code getThing(String)}, an internal class, generation manifest), a sync HTTP provider registration, declared
     * capabilities and an {@code Automatic-Module-Name}.
     */
    static Path service(Path directory) throws IOException {
        compile(directory, Map.of(
            "software/amazon/awssdk/services/demo/DemoClient.java",
            "package " + DEMO + "; public interface DemoClient { String getThing(String name); }",
            "software/amazon/awssdk/services/demo/internal/Hidden.java",
            "package " + DEMO + ".internal; public class Hidden {}"));
        write(directory, CandidateInventoryScanner.MODULE_PROPERTIES + "demo.properties",
              "groupId=software.amazon.awssdk\nartifactId=demo\nversion=3.0.0\n");
        write(directory, "META-INF/aws-sdk-java-v3/services/" + DEMO + "/generation-manifest.json", "{}");
        write(directory, "META-INF/services/" + CandidateInventoryScanner.SYNC_HTTP_SERVICE,
              "# provider\nsoftware.amazon.awssdk.services.demo.DemoProvider\n");
        write(directory, "META-INF/services/" + CandidateInventoryScanner.ASYNC_HTTP_SERVICE, "# none\n");
        write(directory, CandidateInventoryScanner.CAPABILITIES_RESOURCE, """
            # Behaviors and settings this artifact implements.
            behavior:checksum.request-calculation
            config:env:AWS_REGION   # trailing comment
            api:software.amazon.awssdk.services.demo.Declared#member()V
            """);
        write(directory, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nAutomatic-Module-Name: demo.module\n\n");
        write(directory, "META-INF/native-image/demo/reflect-config.json", "[]");
        return directory;
    }

    static void compile(Path classes, Map<String, String> sources) throws IOException {
        Path src = Files.createDirectories(classes.resolveSibling(classes.getFileName() + "-src"));
        List<Path> files = new ArrayList<>();
        for (Map.Entry<String, String> s : sources.entrySet()) {
            Path file = src.resolve(s.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, s.getValue(), StandardCharsets.UTF_8);
            files.add(file);
        }
        Files.createDirectories(classes);
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager manager = javac.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            assertTrue(javac.getTask(null, manager, null, List.of("-proc:none", "-d", classes.toString()), null,
                                     manager.getJavaFileObjectsFromPaths(files)).call(), "fixture compiles");
        }
    }

    static void write(Path root, String name, String content) throws IOException {
        Path file = root.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    /** {@code directory} as a jar at {@code jar}. */
    static Path jar(Path directory, Path jar) throws IOException {
        try (OutputStream out = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(out);
             Stream<Path> files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                zip.putNextEntry(new ZipEntry(directory.relativize(file).toString().replace('\\', '/')));
                zip.write(Files.readAllBytes(file));
                zip.closeEntry();
            }
        }
        return jar;
    }
}
