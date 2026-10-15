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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The runtime modules of the reported services in both builds (Requirement 36.6): each module JAR that the services
 * depend on in either build, other than the service JARs.
 */
@SdkInternalApi
final class ModuleComparison {

    enum Status {
        /** Absent from the Baseline_Build's dependencies (recorded as 0 bytes there). */
        ADDED,
        /** In both builds, with different content. */
        CHANGED,
        /** No longer a dependency of the reported services in the feature build (0 bytes there). */
        DROPPED,
        /** In both builds with the same content. */
        UNCHANGED
    }

    private final List<Row> rows;

    private ModuleComparison(List<Row> rows) {
        this.rows = Collections.unmodifiableList(rows);
    }

    /**
     * Compare the module JARs of the two builds.
     *
     * @param baseline the Baseline_Build JARs by artifactId
     * @param feature the feature build JARs by artifactId
     * @param services the artifactIds of the reported services, which are not modules here
     */
    static ModuleComparison compare(Map<String, JarContents> baseline, Map<String, JarContents> feature,
                                    Set<String> services) {
        Set<String> names = new TreeSet<>(baseline.keySet());
        names.addAll(feature.keySet());
        names.removeAll(services);
        List<Row> rows = new ArrayList<>();
        for (String name : names) {
            JarContents before = baseline.get(name);
            JarContents after = feature.get(name);
            Status status;
            if (before == null) {
                status = Status.ADDED;
            } else if (after == null) {
                status = Status.DROPPED;
            } else if (before.entryCrcs().equals(after.entryCrcs())) {
                status = Status.UNCHANGED;
            } else {
                status = Status.CHANGED;
            }
            rows.add(new Row(name, status, before == null ? 0 : before.compressedBytes(),
                             after == null ? 0 : after.compressedBytes()));
        }
        return new ModuleComparison(rows);
    }

    /**
     * Every module, sorted by artifactId.
     */
    List<Row> rows() {
        return rows;
    }

    /**
     * One module.
     */
    static final class Row {
        private final String artifactId;
        private final Status status;
        private final long baselineBytes;
        private final long featureBytes;

        Row(String artifactId, Status status, long baselineBytes, long featureBytes) {
            this.artifactId = artifactId;
            this.status = status;
            this.baselineBytes = baselineBytes;
            this.featureBytes = featureBytes;
        }

        String artifactId() {
            return artifactId;
        }

        Status status() {
            return status;
        }

        /**
         * The compressed JAR bytes in the Baseline_Build, 0 if absent.
         */
        long baselineBytes() {
            return baselineBytes;
        }

        /**
         * The compressed JAR bytes in the feature build, 0 if absent.
         */
        long featureBytes() {
            return featureBytes;
        }
    }
}
