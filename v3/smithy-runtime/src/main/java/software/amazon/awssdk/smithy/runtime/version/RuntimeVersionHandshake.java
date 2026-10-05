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

package software.amazon.awssdk.smithy.runtime.version;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Runtime version handshake between the V3 modules that participate in one client (Requirement 15.11).
 *
 * <p>Every non-POM V3 module publishes {@code META-INF/aws-sdk-java-v3/modules/<artifactId>.properties} with
 * {@code groupId}, {@code artifactId} and {@code version} keys; the V3 build generates it. Client configuration calls
 * {@link #verify(String, Collection)} once per client construction with the client's own release version and the
 * artifact IDs of every participating module (the client's own artifact included). Construction fails with a
 * {@link RuntimeVersionMismatchException} that lists every module that is missing, is at another version, or is present
 * more than once with differing versions.
 *
 * <p>Instances are immutable and thread-safe.
 */
public final class RuntimeVersionHandshake {

    /**
     * Class-path directory that contains one version resource per V3 module.
     */
    public static final String MODULE_RESOURCE_DIRECTORY = "META-INF/aws-sdk-java-v3/modules/";

    private final ClassLoader classLoader;

    private RuntimeVersionHandshake(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Creates a handshake that reads module version resources through {@code classLoader}, normally the class loader of
     * the generated client.
     */
    public static RuntimeVersionHandshake create(ClassLoader classLoader) {
        return new RuntimeVersionHandshake(Objects.requireNonNull(classLoader, "classLoader"));
    }

    /**
     * The class-path resource name of the version resource for {@code artifactId}.
     */
    public static String resourceName(String artifactId) {
        return MODULE_RESOURCE_DIRECTORY + Objects.requireNonNull(artifactId, "artifactId") + ".properties";
    }

    /**
     * Verifies that every participating module is present at exactly {@code expectedVersion}.
     *
     * @param expectedVersion the exact V3 release version of the client being constructed
     * @param participatingModules artifact IDs of every V3 module the client depends on at runtime
     * @throws RuntimeVersionMismatchException identifying every conflicting module and version
     */
    public void verify(String expectedVersion, Collection<String> participatingModules) {
        requireVersion(expectedVersion);
        List<RuntimeVersionConflict> conflicts = findConflicts(expectedVersion, participatingModules,
                                                               discover(participatingModules));
        if (!conflicts.isEmpty()) {
            throw new RuntimeVersionMismatchException(expectedVersion, conflicts);
        }
    }

    /**
     * Returns every copy of the version resource of each requested module, keyed and sorted by artifact ID. A module
     * without a resource maps to an empty list. Copies appear in class-path order.
     */
    public Map<String, List<RuntimeModuleVersion>> discover(Collection<String> artifactIds) {
        Map<String, List<RuntimeModuleVersion>> result = new TreeMap<>();
        for (String artifactId : sortedUnique(artifactIds)) {
            result.put(artifactId, load(artifactId));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Computes the conflicts between {@code expectedVersion} and the observed module versions without touching the class
     * path. A participating module conflicts when it has no observed copy, or when any observed copy does not declare
     * exactly its artifact ID and {@code expectedVersion}. Observed entries for non-participating modules are ignored.
     *
     * @return conflicts sorted by artifact ID; empty when every participating module is aligned
     */
    public static List<RuntimeVersionConflict> findConflicts(
            String expectedVersion,
            Collection<String> participatingModules,
            Map<String, ? extends Collection<RuntimeModuleVersion>> observed) {
        requireVersion(expectedVersion);
        Objects.requireNonNull(observed, "observed");
        List<RuntimeVersionConflict> conflicts = new ArrayList<>();
        for (String artifactId : sortedUnique(participatingModules)) {
            Collection<RuntimeModuleVersion> copies = observed.get(artifactId);
            List<RuntimeModuleVersion> versions = copies == null ? List.of() : List.copyOf(copies);
            boolean aligned = !versions.isEmpty()
                              && versions.stream().allMatch(v -> v.matches(artifactId, expectedVersion));
            if (!aligned) {
                conflicts.add(new RuntimeVersionConflict(artifactId, expectedVersion, versions));
            }
        }
        return List.copyOf(conflicts);
    }

    static String describe(String expectedVersion, List<RuntimeVersionConflict> conflicts) {
        StringBuilder message = new StringBuilder()
            .append("Incompatible AWS SDK for Java V3 runtime module versions: every participating module must be at ")
            .append("exactly ").append(expectedVersion).append(", but ").append(conflicts.size())
            .append(conflicts.size() == 1 ? " module conflicts:" : " modules conflict:");
        for (RuntimeVersionConflict conflict : conflicts) {
            message.append(System.lineSeparator()).append("  - ").append(conflict.describe());
        }
        return message.append(System.lineSeparator())
                      .append("Use one version for every AWS SDK for Java V3 artifact, for example by importing ")
                      .append("software.amazon.awssdk:bom:").append(expectedVersion).append('.')
                      .toString();
    }

    private List<RuntimeModuleVersion> load(String artifactId) {
        String resource = resourceName(artifactId);
        Enumeration<URL> urls;
        try {
            urls = classLoader.getResources(resource);
        } catch (IOException e) {
            return List.of(new RuntimeModuleVersion("", "", resource + " (unreadable: " + e.getMessage() + ")"));
        }
        List<RuntimeModuleVersion> copies = new ArrayList<>();
        while (urls.hasMoreElements()) {
            copies.add(read(urls.nextElement()));
        }
        return List.copyOf(copies);
    }

    private static RuntimeModuleVersion read(URL url) {
        Properties properties = new Properties();
        try (InputStream in = url.openStream()) {
            properties.load(in);
        } catch (IOException | IllegalArgumentException e) {
            return new RuntimeModuleVersion("", "", url + " (unreadable: " + e.getMessage() + ")");
        }
        return new RuntimeModuleVersion(properties.getProperty("artifactId", "").trim(),
                                        properties.getProperty("version", "").trim(),
                                        url.toString());
    }

    private static TreeSet<String> sortedUnique(Collection<String> artifactIds) {
        Objects.requireNonNull(artifactIds, "participatingModules");
        TreeSet<String> sorted = new TreeSet<>();
        for (String artifactId : artifactIds) {
            if (artifactId == null || artifactId.isBlank()) {
                throw new IllegalArgumentException("Participating module artifact IDs must be non-blank: " + artifactIds);
            }
            sorted.add(artifactId);
        }
        return sorted;
    }

    private static void requireVersion(String expectedVersion) {
        if (expectedVersion == null || expectedVersion.isBlank()) {
            throw new IllegalArgumentException("expectedVersion must be a non-blank release version");
        }
    }
}
