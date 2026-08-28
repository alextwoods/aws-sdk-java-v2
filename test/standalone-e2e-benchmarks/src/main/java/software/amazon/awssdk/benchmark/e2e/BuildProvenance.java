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

package software.amazon.awssdk.benchmark.e2e;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Identifies the build a benchmark jar was produced from.
 *
 * <p>Values are baked into {@code benchmark-provenance.properties} at build time by the
 * {@code git-commit-id-plugin} / resource filtering, so a jar is self-describing: an archived
 * artifact can always be traced back to a commit even after it has been renamed or copied to
 * another host. Deliberately does not read anything from the filesystem or environment at runtime,
 * since the point is to describe the <em>artifact</em> rather than wherever it happens to be
 * running.
 */
public final class BuildProvenance {

    private static final String RESOURCE = "/benchmark-provenance.properties";
    private static final BuildProvenance INSTANCE = load();

    private final String phase;
    private final String commit;
    private final String sdkCommit;
    private final String branch;
    private final String dirtyFiles;
    private final String buildTime;
    private final String sdkV2Version;
    private final String sdkV1Version;
    private final String smithyVersion;

    private BuildProvenance(Properties p) {
        this.phase = p.getProperty("phase", "unspecified");
        this.commit = p.getProperty("git.commit", "unknown");
        this.sdkCommit = p.getProperty("sdk.commit", "unrecorded");
        this.branch = p.getProperty("git.branch", "unknown");
        this.dirtyFiles = p.getProperty("git.dirty.files", "unknown");
        this.buildTime = p.getProperty("build.time", "unknown");
        this.sdkV2Version = p.getProperty("sdk.v2.version", "unknown");
        this.sdkV1Version = p.getProperty("sdk.v1.version", "unknown");
        this.smithyVersion = p.getProperty("smithy.java.version", "unknown");
    }

    private static BuildProvenance load() {
        Properties p = new Properties();
        try (InputStream in = BuildProvenance.class.getResourceAsStream(RESOURCE)) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            // Fall through to the "unknown" defaults: missing provenance must not stop a run.
        }
        return new BuildProvenance(p);
    }

    public static BuildProvenance get() {
        return INSTANCE;
    }

    public String phase() {
        return phase;
    }

    public String commit() {
        return commit;
    }

    public String shortCommit() {
        return abbreviate(commit);
    }

    /**
     * Commit the SDK inside this jar was built from, which differs from {@link #commit()} whenever a
     * jar deliberately pairs the current harness with an SDK from another revision. {@code
     * "unrecorded"} when the build did not verify it.
     */
    public String sdkCommit() {
        return sdkCommit;
    }

    public String shortSdkCommit() {
        return abbreviate(sdkCommit);
    }

    private static String abbreviate(String sha) {
        return sha.length() >= 11 ? sha.substring(0, 11) : sha;
    }

    public String branch() {
        return branch;
    }

    public String dirtyFiles() {
        return dirtyFiles;
    }

    public String sdkV2Version() {
        return sdkV2Version;
    }

    /**
     * Single-line summary for the run header.
     */
    public String summary() {
        return String.format("phase=%s commit=%s sdkCommit=%s branch=%s dirty=%s built=%s"
                             + " sdkV2=%s sdkV1=%s smithy=%s",
                             phase, shortCommit(), shortSdkCommit(), branch, dirtyFiles, buildTime,
                             sdkV2Version, sdkV1Version, smithyVersion);
    }
}
