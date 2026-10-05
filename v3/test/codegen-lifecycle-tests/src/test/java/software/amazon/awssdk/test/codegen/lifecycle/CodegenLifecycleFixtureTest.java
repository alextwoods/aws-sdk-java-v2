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

package software.amazon.awssdk.test.codegen.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class CodegenLifecycleFixtureTest {
    private static final Path GENERATED_ROOT = Path.of(System.getProperty("v3.fixture.generatedSourceRoot"));
    private static final Path MODEL_DIRECTORY = Path.of(System.getProperty("v3.fixture.modelDirectory"));

    @Test
    void generatedSourcesWereCompiledIntoTheModule() throws URISyntaxException {
        assertEquals("software.amazon.awssdk.test.codegen.lifecycle#CodegenLifecycleFixture",
                     CodegenLifecycleFixture.serviceShapeId());

        // The generated type is loaded from this module's main output, i.e. it was compiled with the module sources.
        Path generatedClass = Path.of(GeneratedServiceInfo.class.getProtectionDomain().getCodeSource()
                                                                .getLocation().toURI());
        Path handWrittenClass = Path.of(CodegenLifecycleFixture.class.getProtectionDomain().getCodeSource()
                                                                     .getLocation().toURI());
        assertEquals(handWrittenClass, generatedClass);
    }

    @Test
    void generatedSourceIsPublishedUnderTheGeneratedSourceRoot() {
        Path source = GENERATED_ROOT.resolve("software/amazon/awssdk/test/codegen/lifecycle/GeneratedServiceInfo.java");
        assertTrue(Files.isRegularFile(source), source + " should exist");
    }

    @Test
    void modelDigestMatchesTheSingleFixtureModel() throws IOException, NoSuchAlgorithmException {
        // Independent recomputation for the single-file fixture: length-prefixed relative path, then length-prefixed
        // content, so the digest does not depend on where the checkout lives.
        byte[] name = "codegen-lifecycle-fixture.smithy".getBytes(StandardCharsets.UTF_8);
        byte[] content = Files.readAllBytes(MODEL_DIRECTORY.resolve("codegen-lifecycle-fixture.smithy"));
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(ByteBuffer.allocate(Integer.BYTES).putInt(name.length).array());
        sha256.update(name);
        sha256.update(ByteBuffer.allocate(Long.BYTES).putLong(content.length).array());
        sha256.update(content);

        assertEquals("sha256:" + HexFormat.of().formatHex(sha256.digest()), CodegenLifecycleFixture.modelDigest());
    }
}
