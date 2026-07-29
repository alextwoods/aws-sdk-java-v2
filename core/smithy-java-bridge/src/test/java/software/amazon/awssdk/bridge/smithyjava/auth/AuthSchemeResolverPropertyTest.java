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

package software.amazon.awssdk.bridge.smithyjava.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property-based tests for {@link AuthSchemeResolver} verifying auth scheme selection order.
 *
 * <p><b>Property 4: Auth Scheme Selection Order</b> — For any ordered list of auth scheme
 * {@code ShapeId}s returned by {@code effectiveAuthSchemes()} where at least one has a
 * registered signer, the {@code AuthSchemeResolver} SHALL select the <b>first</b> scheme
 * in the list that has a registered signer, ignoring subsequent schemes.
 *
 * <p><b>Validates: Requirements 3.4</b>
 */
class AuthSchemeResolverPropertyTest {

    /**
     * Property: Given an ordered list of auth scheme ShapeIds where at least one has a
     * registered signer, the resolver always selects the FIRST scheme with a registered signer.
     *
     * <p><b>Validates: Requirements 3.4</b>
     */
    @Property(tries = 100)
    void firstSchemeWithRegisteredSignerIsAlwaysSelected(
            @ForAll("authSchemeScenarios") AuthSchemeScenario scenario) {

        ApiOperation<?, ?> operation = mockOperationWithSchemes(scenario.schemes);

        AuthSchemeResolver.AuthSchemeConfig result =
            AuthSchemeResolver.resolve(operation, scenario.registeredSigners);

        assertThat(result.schemeId()).isEqualTo(scenario.expectedSchemeId);
        assertThat(result.signerConfig()).isEqualTo(scenario.registeredSigners.get(scenario.expectedSchemeId));
    }

    /**
     * Property: When no scheme in the list has a registered signer (and none is noAuth),
     * the resolver throws SdkClientException.
     *
     * <p><b>Validates: Requirements 3.4</b> (complementary — verifies failure case)
     */
    @Property(tries = 100)
    void noSupportedSchemeThrowsSdkClientException(
            @ForAll("unsupportedSchemeScenarios") List<ShapeId> schemes) {

        ApiOperation<?, ?> operation = mockOperationWithSchemes(schemes);
        Map<ShapeId, Object> emptySigners = new HashMap<>();

        assertThatThrownBy(() -> AuthSchemeResolver.resolve(operation, emptySigners))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("No supported auth scheme found");
    }

    /**
     * Property: Regardless of how many schemes follow the first matching one,
     * the result is always the first match. This verifies ordering is respected
     * even when multiple schemes have registered signers.
     *
     * <p><b>Validates: Requirements 3.4</b>
     */
    @Property(tries = 100)
    void multipleRegisteredSignersStillSelectsFirst(
            @ForAll("multiMatchScenarios") AuthSchemeScenario scenario) {

        ApiOperation<?, ?> operation = mockOperationWithSchemes(scenario.schemes);

        AuthSchemeResolver.AuthSchemeConfig result =
            AuthSchemeResolver.resolve(operation, scenario.registeredSigners);

        assertThat(result.schemeId()).isEqualTo(scenario.expectedSchemeId);
    }

    @Provide
    Arbitrary<AuthSchemeScenario> authSchemeScenarios() {
        // Generate a list of 1–10 scheme ShapeIds
        Arbitrary<List<ShapeId>> schemeLists = schemeIds()
            .list()
            .ofMinSize(1)
            .ofMaxSize(10);

        return schemeLists.flatMap(schemes -> {
            // Pick a random index to be the first registered signer
            return Arbitraries.integers().between(0, schemes.size() - 1).map(firstMatchIndex -> {
                Map<ShapeId, Object> registeredSigners = new HashMap<>();
                // Register only the signer at firstMatchIndex (none before it)
                Object signerConfig = "signer-config-for-" + schemes.get(firstMatchIndex).toString();
                registeredSigners.put(schemes.get(firstMatchIndex), signerConfig);

                return new AuthSchemeScenario(
                    schemes,
                    registeredSigners,
                    schemes.get(firstMatchIndex)
                );
            });
        });
    }

    @Provide
    Arbitrary<AuthSchemeScenario> multiMatchScenarios() {
        // Generate a list of 2–10 unique scheme ShapeIds with multiple registered signers
        Arbitrary<List<ShapeId>> schemeLists = schemeIds()
            .list()
            .ofMinSize(2)
            .ofMaxSize(10)
            .uniqueElements();

        return schemeLists.flatMap(schemes -> {
            // Pick the first match index (at least one must match)
            return Arbitraries.integers().between(0, schemes.size() - 1).flatMap(firstMatchIndex -> {
                // Register signers for firstMatchIndex and some subset of the remaining schemes
                return Arbitraries.integers().between(1, schemes.size() - firstMatchIndex).map(extraCount -> {
                    Map<ShapeId, Object> registeredSigners = new HashMap<>();
                    // Always register the first match
                    registeredSigners.put(schemes.get(firstMatchIndex),
                        "signer-" + schemes.get(firstMatchIndex).toString());
                    // Register additional signers after the first match
                    for (int i = firstMatchIndex + 1; i < schemes.size() && registeredSigners.size() <= extraCount; i++) {
                        registeredSigners.put(schemes.get(i), "signer-" + schemes.get(i).toString());
                    }

                    return new AuthSchemeScenario(
                        schemes,
                        registeredSigners,
                        schemes.get(firstMatchIndex)
                    );
                });
            });
        });
    }

    @Provide
    Arbitrary<List<ShapeId>> unsupportedSchemeScenarios() {
        // Generate lists of ShapeIds that do NOT include noAuth, and where no signer is registered
        return schemeIds()
            .filter(id -> !id.equals(ShapeId.from("smithy.api#noAuth")))
            .list()
            .ofMinSize(1)
            .ofMaxSize(10);
    }

    private Arbitrary<ShapeId> schemeIds() {
        // Generate auth scheme ShapeIds from a realistic set of namespaces and names
        Arbitrary<String> namespaces = Arbitraries.of(
            "aws.auth", "smithy.api", "custom.auth", "my.service.auth"
        );
        Arbitrary<String> names = Arbitraries.of(
            "sigv4", "sigv4a", "httpBasic", "httpDigest", "httpBearer",
            "customScheme1", "customScheme2", "customScheme3", "tokenAuth", "apiKey"
        );
        return Combinators.combine(namespaces, names)
                          .as((ns, name) -> ShapeId.from(ns + "#" + name));
    }

    @SuppressWarnings("unchecked")
    private ApiOperation<?, ?> mockOperationWithSchemes(List<ShapeId> schemes) {
        ApiOperation<?, ?> operation = mock(ApiOperation.class);
        when(operation.effectiveAuthSchemes()).thenReturn(schemes);
        return operation;
    }

    /**
     * Data holder for a test scenario: the ordered list of schemes, the registered signers map,
     * and the expected selected scheme.
     */
    static final class AuthSchemeScenario {
        final List<ShapeId> schemes;
        final Map<ShapeId, Object> registeredSigners;
        final ShapeId expectedSchemeId;

        AuthSchemeScenario(List<ShapeId> schemes, Map<ShapeId, Object> registeredSigners, ShapeId expectedSchemeId) {
            this.schemes = schemes;
            this.registeredSigners = registeredSigners;
            this.expectedSchemeId = expectedSchemeId;
        }

        @Override
        public String toString() {
            return "AuthSchemeScenario{schemes=" + schemes
                   + ", registeredSigners=" + registeredSigners.keySet()
                   + ", expectedSchemeId=" + expectedSchemeId + "}";
        }
    }
}
