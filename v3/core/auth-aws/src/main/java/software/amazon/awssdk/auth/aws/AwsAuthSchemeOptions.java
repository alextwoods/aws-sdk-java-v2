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

package software.amazon.awssdk.auth.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeIds;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.NoAuthAuthScheme;
import software.amazon.awssdk.auth.api.SignerProperty;
import software.amazon.awssdk.auth.aws.s3express.S3ExpressAuthScheme;

/**
 * The AWS auth scheme options of an operation, derived from its {@link ModeledAuth} traits (Requirement 13.1), and the
 * launch baseline's AWS auth schemes (Requirement 13.2).
 *
 * <p>Each effective modeled scheme becomes one option, in modeled order, carrying the signer properties its traits and
 * the client determine:
 *
 * <ul>
 *   <li>{@code aws.auth#sigv4}: the trait {@code name} as signing name, the client region, and payload signing disabled
 *       for {@code aws.auth#unsignedPayload} operations;</li>
 *   <li>{@code aws.auth#sigv4a}: as SigV4, with the configured region set, else the client region;</li>
 *   <li>{@code aws.auth#sigv4-s3express}: the trait {@code name} (default {@code s3express}) and the client region;</li>
 *   <li>{@code smithy.api#httpBearerAuth} and {@code smithy.api#noAuth}: no properties.</li>
 * </ul>
 *
 * <p>Service-wide signer defaults (for example S3's disabled double URL encoding and path normalization) apply to every
 * option unless the option sets the property itself. Endpoint auth properties refine these options later in the
 * pipeline.
 */
public final class AwsAuthSchemeOptions {
    private static final Map<SignerProperty<?>, Object> S3_SIGNER_DEFAULTS = Map.of(
        AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false,
        AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE, false,
        AwsV4FamilyHttpSigner.NORMALIZE_PATH, false);

    private AwsAuthSchemeOptions() {
    }

    /** SigV4, SigV4a, bearer and anonymous: the schemes every AWS client has. */
    public static List<AuthScheme<?>> defaultSchemes() {
        return List.of(AwsV4AuthScheme.create(), AwsV4aAuthScheme.create(), BearerAuthScheme.create(),
                       NoAuthAuthScheme.create());
    }

    /** {@link #defaultSchemes()} plus S3 Express, for S3. */
    public static List<AuthScheme<?>> s3Schemes() {
        List<AuthScheme<?>> schemes = new ArrayList<>(defaultSchemes());
        schemes.add(S3ExpressAuthScheme.create());
        return List.copyOf(schemes);
    }

    /**
     * S3's service-wide signer defaults, as V2's {@code s3} auth type: the payload is not hashed into the signature
     * ({@code UNSIGNED-PAYLOAD} over HTTPS, so a streaming upload is never read before it is sent and its checksum
     * travels in an unsigned trailer; plain HTTP still signs the payload), and the canonical URI keeps the encoded
     * key as is (no second encoding, no dot-segment normalization).
     */
    public static Map<SignerProperty<?>, Object> s3SignerDefaults() {
        return S3_SIGNER_DEFAULTS;
    }

    /** The options of {@code auth} for a client in {@code region}, with no region set or service defaults. */
    public static List<AuthSchemeOption> modeled(ModeledAuth auth, String region) {
        return modeled(auth, region, null, Map.of());
    }

    /**
     * The options of {@code auth} for a client in {@code region}.
     *
     * @param regionSet the configured SigV4a region set, or null to sign for {@code region}
     * @param serviceDefaults signer properties every option gets unless it sets them itself
     */
    public static List<AuthSchemeOption> modeled(ModeledAuth auth, String region, RegionSet regionSet,
                                                 Map<SignerProperty<?>, Object> serviceDefaults) {
        Objects.requireNonNull(auth, "auth");
        Objects.requireNonNull(region, "region");
        List<AuthSchemeOption> options = new ArrayList<>();
        for (String schemeId : auth.effectiveSchemeIds()) {
            Map<String, String> trait = auth.schemeTraitValues(schemeId).orElse(Map.of());
            AuthSchemeOption.Builder option = AuthSchemeOption.builder().schemeId(schemeId);
            switch (schemeId) {
                case AuthSchemeIds.SIGV4 -> {
                    putSigningName(option, trait, null, auth);
                    option.putSignerProperty(AwsV4HttpSigner.REGION_NAME, region);
                    payload(option, auth);
                }
                case AuthSchemeIds.SIGV4A -> {
                    putSigningName(option, trait, null, auth);
                    option.putSignerProperty(AwsV4aHttpSigner.REGION_SET,
                                             regionSet != null ? regionSet : RegionSet.create(List.of(region)));
                    payload(option, auth);
                }
                case AuthSchemeIds.SIGV4_S3EXPRESS -> {
                    putSigningName(option, trait, "s3express", auth);
                    option.putSignerProperty(AwsV4HttpSigner.REGION_NAME, region);
                    payload(option, auth);
                }
                default -> {
                    // Bearer, noAuth and schemes without AWS signer properties.
                }
            }
            if (!schemeId.equals(AuthSchemeIds.NO_AUTH) && !schemeId.equals(AuthSchemeIds.HTTP_BEARER)) {
                serviceDefaults.forEach((property, value) -> putIfAbsent(option, property, value));
            }
            options.add(option.build());
        }
        return List.copyOf(options);
    }

    private static void putSigningName(AuthSchemeOption.Builder option, Map<String, String> trait, String fallback,
                                       ModeledAuth auth) {
        String name = trait.getOrDefault("name", fallback);
        if (name == null) {
            throw new IllegalArgumentException("The auth trait of operation " + auth.operationId()
                                               + " has no signing name.");
        }
        option.putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, name);
    }

    private static void payload(AuthSchemeOption.Builder option, ModeledAuth auth) {
        if (auth.unsignedPayload()) {
            option.putSignerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void putIfAbsent(AuthSchemeOption.Builder option, SignerProperty<T> property, Object value) {
        option.putSignerPropertyIfAbsent(property, (T) value);
    }
}
