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

package software.amazon.awssdk.codecs.json;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.codecs.json.internal.SmithyJsonSerdeProvider;

/**
 * Base class for JSON codec tests that run once per JSON serde provider.
 *
 * <p>Port of the smithy-java test class of the same name. smithy-java runs each test against its Jackson and native
 * providers (and every pairing of them); the SDK ships only the native provider (design K5), so each list has one
 * entry and the tests keep their {@code @PerProvider} and {@code @MethodSource("crossProviders")} shape for a future
 * provider (OD-5).
 */
abstract class ProviderTestBase {

    static final SmithyJsonSerdeProvider SMITHY = new SmithyJsonSerdeProvider();

    static List<JsonSerdeProvider> providerInstances() {
        return Collections.singletonList(SMITHY);
    }

    static List<Arguments> providers() {
        return Collections.singletonList(Arguments.of(Named.of(SMITHY.getName(), SMITHY)));
    }

    static List<Arguments> crossProviders() {
        return Collections.singletonList(Arguments.of(Named.of("smithy->smithy", SMITHY), SMITHY));
    }

    /**
     * Creates a codec with default settings using the given provider.
     */
    static JsonCodec codec(JsonSerdeProvider provider) {
        return codecBuilder(provider).build();
    }

    /**
     * Creates a codec builder using the given provider.
     */
    static JsonCodec.Builder codecBuilder(JsonSerdeProvider provider) {
        return JsonCodec.builder().overrideSerdeProvider(provider);
    }
}
