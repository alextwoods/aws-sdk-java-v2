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

package software.amazon.awssdk.archtests.serde;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.archtests.ClassFiles;

/**
 * The schema-based clients and the new request pipeline path have no dependency on generated or legacy marshaller
 * and unmarshaller types (task 5.3; Requirements 37.2, 36.4). See {@link MarshallerDependencyScanner} for the rules.
 *
 * <p>It scans every class of the {@link #SCHEMA_BASED_SERVICES} JARs, and the {@link #NEW_PATH_CLASSES} of
 * {@code sdk-core} with their nested, local and anonymous classes. Tasks 9.2–9.4 add the services they move to
 * schema-based serde (with a test dependency in {@code architecture-tests/pom.xml}).
 */
class NoMarshallerDependencyTest {

    /**
     * The services whose clients are fully routed through the new path (the {@code SCHEMA_BASED_MODULES} of
     * {@code NoTransformPackageTest}), by artifactId.
     */
    static final List<String> SCHEMA_BASED_SERVICES =
        Collections.unmodifiableList(Arrays.asList("cloudwatch", "sqs", "secretsmanager", "account"));

    /**
     * The new-path pipeline classes of {@code sdk-core} (design D.6), by internal name.
     */
    static final List<String> NEW_PATH_CLASSES = Collections.unmodifiableList(Arrays.asList(
        "software/amazon/awssdk/core/internal/http/SyncApiCallPipeline",
        "software/amazon/awssdk/core/internal/http/AsyncApiCallPipeline",
        "software/amazon/awssdk/core/internal/http/RequestMutationStages",
        "software/amazon/awssdk/core/internal/http/ProtocolMarshalling",
        "software/amazon/awssdk/core/internal/http/ProtocolResponseHandling",
        "software/amazon/awssdk/core/internal/http/PipelineCache"));

    @Test
    void scan_schemaBasedServiceJars_referenceNoMarshallerTypes() throws IOException {
        for (String artifactId : SCHEMA_BASED_SERVICES) {
            Path location = locate(artifactId);
            Map<String, byte[]> classes = ClassFiles.read(location);
            assertThat(classes).as("classes of %s (%s)", artifactId, location).isNotEmpty();

            assertThat(MarshallerDependencyScanner.create().scan(classes))
                .as("legacy serde references in %s (%s)", artifactId, location)
                .isEmpty();
        }
    }

    @Test
    void scan_newPathPipelineClasses_referenceNoMarshallerTypes() throws IOException {
        Path location = locate("sdk-core");
        Map<String, byte[]> sdkCore = ClassFiles.read(location);
        Map<String, byte[]> newPath = new TreeMap<>();
        for (String name : NEW_PATH_CLASSES) {
            assertThat(sdkCore).as("%s in %s", name, location).containsKey(name);
            sdkCore.forEach((className, bytes) -> {
                if (className.equals(name) || className.startsWith(name + "$")) {
                    newPath.put(className, bytes);
                }
            });
        }
        // The pipelines' per-call state lives in nested classes (for example AsyncApiCallPipeline$AsyncCall).
        assertThat(newPath.keySet()).anyMatch(n -> n.startsWith(NEW_PATH_CLASSES.get(1) + "$"));

        assertThat(MarshallerDependencyScanner.create().scan(newPath))
            .as("legacy serde references in the new-path classes of sdk-core (%s)", location)
            .isEmpty();
    }

    static Path locate(String artifactId) throws IOException {
        Path location = ClassFiles.locateArtifact(artifactId);
        assertThat(location)
            .as("%s is not on the test classpath; add it to architecture-tests/pom.xml", artifactId)
            .isNotNull();
        return location;
    }
}
