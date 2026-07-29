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

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Resolves which auth scheme and signer configuration to use for a given operation.
 *
 * <p>Iterates the operation's {@code effectiveAuthSchemes()} list in order and selects
 * the first scheme that has a registered signer. If the {@code smithy.api#noAuth} scheme
 * is encountered, signing is skipped entirely (anonymous request).
 *
 * <p>This class is primarily used at client construction time to validate that the
 * configured signers can cover the auth schemes required by the service's operations.
 * When delegating to smithy-java's {@code Client.call()}, the runtime handles auth
 * scheme resolution internally.
 */
@SdkInternalApi
public final class AuthSchemeResolver {

    private static final ShapeId NO_AUTH = ShapeId.from("smithy.api#noAuth");

    private AuthSchemeResolver() {
    }

    /**
     * Resolves the effective auth scheme for an operation by iterating the operation's
     * {@code effectiveAuthSchemes()} list and selecting the first scheme with a registered signer.
     *
     * <p>Special cases:
     * <ul>
     *   <li>{@code smithy.api#noAuth} — returns {@link AuthSchemeConfig#NO_AUTH} immediately,
     *       indicating the request should be sent unsigned.</li>
     * </ul>
     *
     * @param operation the operation to resolve auth for
     * @param registeredSigners map of scheme IDs to their signer configuration
     * @return the selected {@link AuthSchemeConfig}
     * @throws SdkClientException if no supported auth scheme is found
     */
    public static AuthSchemeConfig resolve(
            ApiOperation<?, ?> operation,
            Map<ShapeId, Object> registeredSigners) {

        List<ShapeId> schemes = operation.effectiveAuthSchemes();
        for (ShapeId scheme : schemes) {
            if (NO_AUTH.equals(scheme)) {
                return AuthSchemeConfig.NO_AUTH;
            }
            if (registeredSigners.containsKey(scheme)) {
                return new AuthSchemeConfig(scheme, registeredSigners.get(scheme));
            }
        }
        throw SdkClientException.builder()
            .message("No supported auth scheme found for operation. "
                     + "Available schemes: " + schemes)
            .build();
    }

    /**
     * Holds the resolved auth scheme selection: the scheme ID and the associated signer configuration.
     */
    public static final class AuthSchemeConfig {

        /**
         * Sentinel instance indicating anonymous (unsigned) requests.
         */
        public static final AuthSchemeConfig NO_AUTH = new AuthSchemeConfig(ShapeId.from("smithy.api#noAuth"), null);

        private final ShapeId schemeId;
        private final Object signerConfig;

        public AuthSchemeConfig(ShapeId schemeId, Object signerConfig) {
            this.schemeId = schemeId;
            this.signerConfig = signerConfig;
        }

        /**
         * Returns the selected auth scheme identifier (e.g., {@code aws.auth#sigv4}).
         */
        public ShapeId schemeId() {
            return schemeId;
        }

        /**
         * Returns the signer configuration associated with this scheme, or {@code null}
         * for anonymous (noAuth) requests.
         */
        public Object signerConfig() {
            return signerConfig;
        }

        @Override
        public String toString() {
            return "AuthSchemeConfig{schemeId=" + schemeId + ", signerConfig=" + signerConfig + "}";
        }
    }
}
