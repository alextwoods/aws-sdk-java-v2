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

package software.amazon.awssdk.codegen.pipeline;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.loader.ModelDiscovery;

/**
 * {@link ModelAssembler#discoverModels(ClassLoader)} without the Smithy protocol test models.
 *
 * <p>The codegen class path holds {@code smithy-aws-protocol-tests} and {@code smithy-protocol-tests} so that protocol
 * tests can be generated from them (Requirement 32). Their {@code META-INF/smithy} manifests declare dozens of test
 * services; discovering them would add those services to every model codegen assembles. This discovery imports the
 * model files of every other manifest on the class path, which keeps trait definitions such as {@code aws.api},
 * {@code smithy.test} and the {@code com.amazonaws.c2j} markers. A build that generates the protocol tests imports the
 * test models explicitly.
 */
@SdkInternalApi
public final class SmithyModelDiscovery {

    private static final String MANIFEST = "META-INF/smithy/manifest";

    /** Matches a manifest inside a protocol test model JAR, for example {@code .../smithy-aws-protocol-tests-1.71.0.jar!}. */
    private static final Pattern PROTOCOL_TEST_MODELS =
        Pattern.compile(".*/(smithy-aws-protocol-tests|smithy-protocol-tests)-[^/]*\\.jar!/.*");

    private SmithyModelDiscovery() {
    }

    /**
     * Add the discoverable models of {@code classLoader}, except the protocol test models, to {@code assembler}.
     */
    public static ModelAssembler discoverModels(ModelAssembler assembler, ClassLoader classLoader) {
        Enumeration<URL> manifests;
        try {
            manifests = classLoader.getResources(MANIFEST);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to find Smithy model manifests", e);
        }
        while (manifests.hasMoreElements()) {
            URL manifest = manifests.nextElement();
            if (!isProtocolTestModels(manifest)) {
                for (URL model : ModelDiscovery.findModels(manifest)) {
                    assembler.addImport(model);
                }
            }
        }
        return assembler;
    }

    static boolean isProtocolTestModels(URL manifest) {
        return PROTOCOL_TEST_MODELS.matcher(manifest.toString()).matches();
    }
}
