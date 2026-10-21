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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * One higher-level library of the Library_Integration_Analysis (design D.7, Requirement 38.2) in both builds: its own
 * JAR's compressed size, uncompressed class-file bytes and class count. A library that does not exist in the
 * Baseline_Build commit is recorded as 0 there.
 */
@SdkInternalApi
final class LibraryComparison {

    enum Status {
        /** The library has no module in the Baseline_Build commit (recorded as 0 bytes there). */
        NEW,
        /** In both builds, with different content. */
        CHANGED,
        /** In both builds with the same content. */
        UNCHANGED
    }

    private final String artifactId;
    private final String displayName;
    private final Status status;
    private final Measures baseline;
    private final Measures feature;

    private LibraryComparison(String artifactId, String displayName, Status status, Measures baseline,
                              Measures feature) {
        this.artifactId = artifactId;
        this.displayName = displayName;
        this.status = status;
        this.baseline = baseline;
        this.feature = feature;
    }

    /**
     * Compare a library's JARs.
     *
     * @param baseline the Baseline_Build JAR, or {@code null} if the library is not in the Baseline_Build commit
     * @param feature the feature build JAR
     */
    static LibraryComparison compare(String artifactId, String displayName, JarContents baseline,
                                     JarContents feature) {
        Objects.requireNonNull(feature, "feature");
        Status status;
        if (baseline == null) {
            status = Status.NEW;
        } else if (baseline.entryCrcs().equals(feature.entryCrcs())) {
            status = Status.UNCHANGED;
        } else {
            status = Status.CHANGED;
        }
        return new LibraryComparison(artifactId, displayName, status,
                                     baseline == null ? Measures.ABSENT : Measures.of(baseline), Measures.of(feature));
    }

    String artifactId() {
        return artifactId;
    }

    String displayName() {
        return displayName;
    }

    Status status() {
        return status;
    }

    Measures baseline() {
        return baseline;
    }

    Measures feature() {
        return feature;
    }

    /**
     * The sizes of one build's JAR.
     */
    static final class Measures {
        static final Measures ABSENT = new Measures(0, 0, 0);

        private final long compressedBytes;
        private final long classBytes;
        private final int classCount;

        Measures(long compressedBytes, long classBytes, int classCount) {
            this.compressedBytes = compressedBytes;
            this.classBytes = classBytes;
            this.classCount = classCount;
        }

        static Measures of(JarContents jar) {
            long classBytes = 0;
            for (byte[] bytes : jar.classes().values()) {
                classBytes += bytes.length;
            }
            return new Measures(jar.compressedBytes(), classBytes, jar.classes().size());
        }

        /**
         * The JAR file's size.
         */
        long compressedBytes() {
            return compressedBytes;
        }

        /**
         * The uncompressed bytes of every {@code .class} entry.
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
    }
}
