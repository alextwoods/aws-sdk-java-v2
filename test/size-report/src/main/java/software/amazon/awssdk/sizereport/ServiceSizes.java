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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The Requirement 36.1 measurements of one service JAR from one build, with the attribution of every class.
 */
@SdkInternalApi
final class ServiceSizes {
    private final long compressedBytes;
    private final long classBytes;
    private final int classCount;
    private final Map<String, PackageSize> packages;
    private final Map<SizeCategory, Long> categoryBytes;
    private final List<ClassAttribution> classes;
    private final List<String> servicePackages;

    private ServiceSizes(long compressedBytes, long classBytes, int classCount, Map<String, PackageSize> packages,
                         Map<SizeCategory, Long> categoryBytes, List<ClassAttribution> classes,
                         List<String> servicePackages) {
        this.compressedBytes = compressedBytes;
        this.classBytes = classBytes;
        this.classCount = classCount;
        this.packages = Collections.unmodifiableMap(packages);
        this.categoryBytes = Collections.unmodifiableMap(categoryBytes);
        this.classes = Collections.unmodifiableList(classes);
        this.servicePackages = Collections.unmodifiableList(servicePackages);
    }

    /**
     * Measure a service JAR.
     */
    static ServiceSizes measure(JarContents jar) {
        ClassAttributor attributor = ClassAttributor.forClasses(jar.classes().keySet());
        Map<String, PackageSize> packages = new TreeMap<>();
        Map<SizeCategory, Long> categoryBytes = new EnumMap<>(SizeCategory.class);
        for (SizeCategory category : SizeCategory.values()) {
            categoryBytes.put(category, 0L);
        }
        List<ClassAttribution> classes = new ArrayList<>();
        long total = 0;
        for (Map.Entry<String, byte[]> entry : jar.classes().entrySet()) {
            ClassAttribution attribution = attributor.attribute(entry.getKey(), entry.getValue());
            classes.add(attribution);
            total += attribution.bytes();
            packages.computeIfAbsent(packageName(entry.getKey()), p -> new PackageSize()).add(attribution.bytes());
            attribution.categoryBytes().forEach((category, bytes) -> categoryBytes.merge(category, bytes, Long::sum));
        }
        return new ServiceSizes(jar.compressedBytes(), total, classes.size(), packages, categoryBytes, classes,
                                attributor.servicePackages());
    }

    /**
     * The JAR file size in bytes.
     */
    long compressedBytes() {
        return compressedBytes;
    }

    /**
     * The total uncompressed class-file bytes.
     */
    long classBytes() {
        return classBytes;
    }

    /**
     * The number of {@code .class} entries.
     */
    int classCount() {
        return classCount;
    }

    /**
     * Class count and uncompressed class-file bytes by Java package (dotted name), sorted.
     */
    Map<String, PackageSize> packages() {
        return packages;
    }

    /**
     * Uncompressed class-file bytes by category, every category present; the values add up to {@link #classBytes()}.
     */
    Map<SizeCategory, Long> categoryBytes() {
        return categoryBytes;
    }

    /**
     * The attribution of every class, sorted by class name.
     */
    List<ClassAttribution> classes() {
        return classes;
    }

    /**
     * The service packages the attribution found, in internal form.
     */
    List<String> servicePackages() {
        return servicePackages;
    }

    static String packageName(String internalClassName) {
        int slash = internalClassName.lastIndexOf('/');
        return slash < 0 ? "(default)" : internalClassName.substring(0, slash).replace('/', '.');
    }

    /**
     * The classes of one package.
     */
    static final class PackageSize {
        private int classes;
        private long bytes;

        void add(long classFileBytes) {
            classes++;
            bytes += classFileBytes;
        }

        int classes() {
            return classes;
        }

        long bytes() {
            return bytes;
        }
    }
}
