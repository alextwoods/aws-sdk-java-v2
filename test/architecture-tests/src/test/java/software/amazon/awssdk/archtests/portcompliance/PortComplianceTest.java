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

package software.amazon.awssdk.archtests.portcompliance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Java 8 port-compliance check for the modules that port smithy-java (task 1.19; Requirements 1.3, 1.4, 1.5, 1.8).
 * See {@link PortComplianceChecker} for the rules.
 *
 * <p>To cover a new ported module, add its artifactId to {@link #PORTED_MODULES} and a test-scoped dependency on it
 * to {@code test/architecture-tests/pom.xml}. The test fails if a listed module is not on the test classpath.
 */
class PortComplianceTest {

    /**
     * The ported modules, by artifactId. Modules that are still skeletons (no classes yet) are listed so that they are
     * checked as soon as code lands.
     */
    static final List<String> PORTED_MODULES = Collections.unmodifiableList(Arrays.asList(
        "schema",
        "codec-commons",
        "json-codec",
        "cbor-codec",
        "xml-codec",
        "http-binding",
        "http-client-protocol",
        "rpcv2-cbor-client-protocol",
        "aws-json-client-protocol",
        "rest-json-client-protocol",
        "rest-xml-client-protocol",
        "aws-query-client-protocol",
        "dynamic-client",
        "client-mock-plugin",
        "message-payloads"));

    static final String SIGNATURE_PROPERTY = "port.compliance.java18.signature";

    private static PortComplianceChecker checker;

    @BeforeAll
    static void loadSignature() throws IOException {
        try (InputStream in = Files.newInputStream(java18Signature())) {
            checker = PortComplianceChecker.fromSignature(in);
        }
    }

    static List<String> portedModules() {
        return PORTED_MODULES;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("portedModules")
    void portedModuleCompliesWithJava8PortingRules(String artifactId) throws IOException {
        Path location = PortComplianceChecker.locateArtifact(artifactId);
        assertThat(location)
            .as("%s is listed in PORTED_MODULES but is not on the test classpath; add it to architecture-tests/pom.xml",
                artifactId)
            .isNotNull();

        assertThat(checker.check(location))
            .as("Java 8 port-compliance violations in %s (%s)", artifactId, location)
            .isEmpty();
    }

    /**
     * The animal-sniffer {@code java18} signature. Maven passes its location (see the {@code dependency:properties}
     * execution in the pom); IDE runs fall back to the local repository.
     */
    static Path java18Signature() {
        String configured = System.getProperty(SIGNATURE_PROPERTY);
        Path path = configured != null && !configured.isEmpty()
                    ? Paths.get(configured)
                    : Paths.get(System.getProperty("user.home"), ".m2", "repository", "org", "codehaus", "mojo",
                                "signature", "java18", "1.0", "java18-1.0.signature");
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Java 8 API signature not found at " + path + "; set -D" + SIGNATURE_PROPERTY
                                            + " or resolve org.codehaus.mojo.signature:java18:1.0:signature");
        }
        return path;
    }
}
