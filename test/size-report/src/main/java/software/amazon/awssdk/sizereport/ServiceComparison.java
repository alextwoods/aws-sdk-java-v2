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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The Baseline_Build and feature build measurements of one service, and the change between them (Requirements 36.2,
 * 36.3 and 36.8).
 */
@SdkInternalApi
final class ServiceComparison {
    private final String artifactId;
    private final String displayName;
    private final ServiceSizes baseline;
    private final ServiceSizes feature;
    private final Map<SizeCategory, Long> categoryChanges;

    ServiceComparison(String artifactId, String displayName, ServiceSizes baseline, ServiceSizes feature) {
        this.artifactId = artifactId;
        this.displayName = displayName;
        this.baseline = baseline;
        this.feature = feature;
        Map<SizeCategory, Long> changes = new EnumMap<>(SizeCategory.class);
        long sum = 0;
        for (SizeCategory category : SizeCategory.values()) {
            long change = feature.categoryBytes().get(category) - baseline.categoryBytes().get(category);
            changes.put(category, change);
            sum += change;
        }
        if (sum != classBytesChange()) {
            throw new IllegalStateException("The category changes of " + artifactId + " add up to " + sum
                                            + " bytes, not to its change in class-file bytes, " + classBytesChange());
        }
        this.categoryChanges = Collections.unmodifiableMap(changes);
    }

    String artifactId() {
        return artifactId;
    }

    String displayName() {
        return displayName;
    }

    ServiceSizes baseline() {
        return baseline;
    }

    ServiceSizes feature() {
        return feature;
    }

    long compressedChange() {
        return feature.compressedBytes() - baseline.compressedBytes();
    }

    long classBytesChange() {
        return feature.classBytes() - baseline.classBytes();
    }

    /**
     * The change in uncompressed class-file bytes by category. The values add up to {@link #classBytesChange()}.
     */
    Map<SizeCategory, Long> categoryChanges() {
        return categoryChanges;
    }

    /**
     * Requirement 36.3: the feature build's compressed JAR is no larger than the Baseline_Build's.
     */
    boolean meetsJarSizeRequirement() {
        return compressedChange() <= 0;
    }

    /**
     * The categories with a net byte increase (Requirement 36.8), in category order.
     */
    List<SizeCategory> grownCategories() {
        List<SizeCategory> grown = new ArrayList<>();
        categoryChanges.forEach((category, change) -> {
            if (change > 0) {
                grown.add(category);
            }
        });
        return grown;
    }

    /**
     * The top-level classes (each with its nested classes) whose uncompressed bytes changed most, largest absolute
     * change first.
     */
    List<ClassGroupChange> largestChanges(int limit) {
        Map<String, ClassGroupChange> groups = new TreeMap<>();
        for (ClassAttribution attribution : baseline.classes()) {
            groups.computeIfAbsent(outerClass(attribution.className()), ClassGroupChange::new).addBaseline(attribution);
        }
        for (ClassAttribution attribution : feature.classes()) {
            groups.computeIfAbsent(outerClass(attribution.className()), ClassGroupChange::new).addFeature(attribution);
        }
        List<ClassGroupChange> result = new ArrayList<>();
        for (ClassGroupChange group : groups.values()) {
            if (group.change() != 0) {
                result.add(group);
            }
        }
        result.sort(Comparator.comparingLong((ClassGroupChange g) -> -Math.abs(g.change()))
                              .thenComparing(ClassGroupChange::name));
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    private static String outerClass(String internalName) {
        int dollar = internalName.indexOf('$', internalName.lastIndexOf('/') + 1);
        return dollar < 0 ? internalName : internalName.substring(0, dollar);
    }

    /**
     * A top-level class and its nested classes in both builds.
     */
    static final class ClassGroupChange {
        private final String name;
        private SizeCategory category;
        private int baselineClasses;
        private long baselineBytes;
        private int featureClasses;
        private long featureBytes;

        ClassGroupChange(String name) {
            this.name = name;
        }

        void addBaseline(ClassAttribution attribution) {
            baselineClasses++;
            baselineBytes += attribution.bytes();
            if (category == null || attribution.className().equals(name)) {
                category = attribution.category();
            }
        }

        void addFeature(ClassAttribution attribution) {
            featureClasses++;
            featureBytes += attribution.bytes();
            if (attribution.className().equals(name) || category == null) {
                category = attribution.category();
            }
        }

        String name() {
            return name;
        }

        SizeCategory category() {
            return category;
        }

        int baselineClasses() {
            return baselineClasses;
        }

        long baselineBytes() {
            return baselineBytes;
        }

        int featureClasses() {
            return featureClasses;
        }

        long featureBytes() {
            return featureBytes;
        }

        long change() {
            return featureBytes - baselineBytes;
        }
    }
}
