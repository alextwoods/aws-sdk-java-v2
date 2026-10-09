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

package software.amazon.awssdk.benchmark.harness;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * One compared arm of a cell: the artifact a client process runs. The default {@link #V3} arm is this module's
 * {@link ClientMain} on the harness classpath; reference arms (19.3) name their own classpath and a main class that
 * honours {@link ClientMain}'s command line and result record.
 *
 * <p>A same-artifact null arm ({@link #nullTwin()}, Req 20.8) runs the identical artifact as its source arm (same
 * classpath, main class, JDK and JVM flags) under a distinct id, so the source/null ratio measures harness noise
 * only; the validity rules check the artifacts are the same from the launch digests.
 *
 * @param id stable arm id, e.g. {@code v3}; a null twin is {@code <source>#null}
 * @param classpath the client classpath, or {@code null} for the harness classpath
 * @param mainClass the client main class
 * @param nullOf the source arm id when this is a same-artifact null arm, else {@code null}
 */
public record BenchmarkArm(String id, String classpath, String mainClass, String nullOf) {

    public static final String NULL_SUFFIX = "#null";

    /** V3 itself: {@link ClientMain} on the harness classpath. */
    public static final BenchmarkArm V3 = new BenchmarkArm("v3", null, ClientMain.class.getName(), null);

    public BenchmarkArm {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(mainClass, "mainClass");
        if (id.isBlank() || id.contains("/") || id.contains(" ")) {
            throw new IllegalArgumentException("arm id must be non-blank without '/' or spaces: " + id);
        }
        if (nullOf != null && !id.equals(nullOf + NULL_SUFFIX)) {
            throw new IllegalArgumentException("null arm of " + nullOf + " must be named " + nullOf + NULL_SUFFIX);
        }
        if (nullOf == null && id.endsWith(NULL_SUFFIX)) {
            throw new IllegalArgumentException("only null arms may end in " + NULL_SUFFIX + ": " + id);
        }
    }

    public boolean isNull() {
        return nullOf != null;
    }

    /** The same-artifact null arm of this arm. */
    public BenchmarkArm nullTwin() {
        if (isNull()) {
            throw new IllegalStateException(id + " is already a null arm");
        }
        return new BenchmarkArm(id + NULL_SUFFIX, classpath, mainClass, id);
    }

    /** True when {@code other} runs the same artifact (classpath and main class) as this arm. */
    public boolean sameArtifact(BenchmarkArm other) {
        return Objects.equals(classpath, other.classpath) && mainClass.equals(other.mainClass);
    }

    public Optional<String> classpathOverride() {
        return Optional.ofNullable(classpath);
    }

    ObjectNode toNode() {
        ObjectNode.Builder b = ObjectNode.builder().withMember("id", id).withMember("main_class", mainClass)
                                         .withMember("harness_classpath", classpath == null);
        return nullOf == null ? b.build() : b.withMember("null_of", nullOf).build();
    }
}
