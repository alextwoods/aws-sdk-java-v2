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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.model.shapes.ShapeId;

class AuthSchemeResolverTest {

    private static final ShapeId SIGV4 = ShapeId.from("aws.auth#sigv4");
    private static final ShapeId NO_AUTH = ShapeId.from("smithy.api#noAuth");
    private static final ShapeId CUSTOM_SCHEME = ShapeId.from("com.example#customAuth");

    @Test
    void resolve_singleSigv4Scheme_selectsSigv4() {
        ApiOperation<?, ?> operation = operationWithSchemes(SIGV4);
        Map<ShapeId, Object> signers = signerMapWith(SIGV4);

        AuthSchemeResolver.AuthSchemeConfig result = AuthSchemeResolver.resolve(operation, signers);

        assertThat(result.schemeId()).isEqualTo(SIGV4);
        assertThat(result.signerConfig()).isNotNull();
    }

    @Test
    void resolve_noAuthScheme_returnsNoAuthConfig() {
        ApiOperation<?, ?> operation = operationWithSchemes(NO_AUTH);
        Map<ShapeId, Object> signers = signerMapWith(SIGV4);

        AuthSchemeResolver.AuthSchemeConfig result = AuthSchemeResolver.resolve(operation, signers);

        assertThat(result).isSameAs(AuthSchemeResolver.AuthSchemeConfig.NO_AUTH);
        assertThat(result.schemeId()).isEqualTo(NO_AUTH);
        assertThat(result.signerConfig()).isNull();
    }

    @Test
    void resolve_multipleSchemes_selectsFirstWithRegisteredSigner() {
        // CUSTOM_SCHEME has no registered signer, SIGV4 does
        ApiOperation<?, ?> operation = operationWithSchemes(CUSTOM_SCHEME, SIGV4);
        Map<ShapeId, Object> signers = signerMapWith(SIGV4);

        AuthSchemeResolver.AuthSchemeConfig result = AuthSchemeResolver.resolve(operation, signers);

        assertThat(result.schemeId()).isEqualTo(SIGV4);
    }

    @Test
    void resolve_multipleSchemes_firstMatchWins() {
        // Both CUSTOM_SCHEME and SIGV4 have registered signers, CUSTOM_SCHEME should win
        ApiOperation<?, ?> operation = operationWithSchemes(CUSTOM_SCHEME, SIGV4);
        Map<ShapeId, Object> signers = signerMapWith(CUSTOM_SCHEME, SIGV4);

        AuthSchemeResolver.AuthSchemeConfig result = AuthSchemeResolver.resolve(operation, signers);

        assertThat(result.schemeId()).isEqualTo(CUSTOM_SCHEME);
    }

    @Test
    void resolve_noSupportedScheme_throwsSdkClientException() {
        ShapeId unsupported = ShapeId.from("com.example#unsupported");
        ApiOperation<?, ?> operation = operationWithSchemes(unsupported);
        Map<ShapeId, Object> signers = signerMapWith(SIGV4);

        assertThatThrownBy(() -> AuthSchemeResolver.resolve(operation, signers))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("No supported auth scheme found for operation")
            .hasMessageContaining(unsupported.toString());
    }

    @Test
    void resolve_noAuthBeforeSigv4_selectsNoAuth() {
        // noAuth appears before sigv4, so noAuth wins immediately
        ApiOperation<?, ?> operation = operationWithSchemes(NO_AUTH, SIGV4);
        Map<ShapeId, Object> signers = signerMapWith(SIGV4);

        AuthSchemeResolver.AuthSchemeConfig result = AuthSchemeResolver.resolve(operation, signers);

        assertThat(result).isSameAs(AuthSchemeResolver.AuthSchemeConfig.NO_AUTH);
    }

    @Test
    void resolve_emptySchemeList_throwsSdkClientException() {
        ApiOperation<?, ?> operation = mock(ApiOperation.class);
        when(operation.effectiveAuthSchemes()).thenReturn(Collections.emptyList());
        Map<ShapeId, Object> signers = signerMapWith(SIGV4);

        assertThatThrownBy(() -> AuthSchemeResolver.resolve(operation, signers))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("No supported auth scheme found for operation");
    }

    /**
     * Helper that creates a mock ApiOperation returning the given auth schemes.
     */
    @SuppressWarnings("unchecked")
    private static ApiOperation<?, ?> operationWithSchemes(ShapeId... schemes) {
        ApiOperation<?, ?> operation = mock(ApiOperation.class);
        when(operation.effectiveAuthSchemes()).thenReturn(Arrays.asList(schemes));
        return operation;
    }

    /**
     * Helper that builds a signer map with stub entries for the given scheme IDs.
     */
    private static Map<ShapeId, Object> signerMapWith(ShapeId... schemes) {
        Map<ShapeId, Object> map = new HashMap<>();
        for (ShapeId scheme : schemes) {
            map.put(scheme, new Object()); // stub signer config
        }
        return map;
    }
}
