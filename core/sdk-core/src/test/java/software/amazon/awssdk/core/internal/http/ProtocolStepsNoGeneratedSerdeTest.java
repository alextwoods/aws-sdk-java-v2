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

package software.amazon.awssdk.core.internal.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Requirement 37.2: the new path's protocol steps have no compile-time dependency on generated marshaller or
 * unmarshaller types. Every type a class refers to is named in its constant pool, so the check reads the compiled
 * class files (the class and its nested classes) and looks for the legacy serde type names.
 */
class ProtocolStepsNoGeneratedSerdeTest {
    private static final List<String> FORBIDDEN = Arrays.asList(
        // Generated <Op>RequestMarshaller types implement this; protocol marshallers live under it.
        "software/amazon/awssdk/core/runtime/transform/",
        // Generated response handlers and the protocol factories' unmarshallers.
        "software/amazon/awssdk/core/http/HttpResponseHandler",
        "software/amazon/awssdk/protocols/",
        "Marshaller",
        "Unmarshaller");

    static List<Class<?>> protocolStepClasses() {
        List<Class<?>> classes = new ArrayList<>();
        // The protocol steps (task 3.24), the shared stages (task 3.25) and the sync and async pipelines (tasks 3.26,
        // 3.27).
        for (Class<?> type : Arrays.asList(ProtocolMarshalling.class, ProtocolResponseHandling.class,
                                           RequestMutationStages.class, EndpointResolution.class,
                                           AuthSchemeResolutionCache.class, UserAgentHeaderCache.class,
                                           RequestSigning.class, RetryDecisions.class,
                                           ExecutionAttributesTemplate.class, SyncApiCallPipeline.class,
                                           AsyncApiCallPipeline.class)) {
            classes.add(type);
            classes.addAll(Arrays.asList(type.getDeclaredClasses()));
        }
        return classes;
    }

    @ParameterizedTest
    @MethodSource("protocolStepClasses")
    void classFile_referencesNoGeneratedMarshallerOrUnmarshallerType(Class<?> type) throws IOException {
        String constantPool = classFileText(type);

        // The constant pool names the class itself, so this confirms the bytes were read as expected.
        assertThat(constantPool).contains(type.getName().replace('.', '/'));
        for (String forbidden : FORBIDDEN) {
            assertThat(constantPool).as("%s must not reference %s", type.getName(), forbidden).doesNotContain(forbidden);
        }
    }

    private static String classFileText(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream in = type.getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            return new String(IoUtils.toByteArray(in), StandardCharsets.ISO_8859_1);
        }
    }
}
