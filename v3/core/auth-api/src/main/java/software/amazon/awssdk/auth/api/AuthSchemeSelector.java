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

package software.amazon.awssdk.auth.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;

/**
 * Chooses the auth scheme of a call from the operation's ordered {@link AuthSchemeOption} candidates (Requirement
 * 13.1), with the V2 selection rules:
 *
 * <ol>
 *   <li>The candidates are reordered by the configured auth scheme preference (V2 {@code auth_scheme_preference} /
 *       {@code AWS_AUTH_SCHEME_PREFERENCE}): preferred scheme names (the part after {@code #}) first, in preference
 *       order, then the remaining candidates in modeled order. Unknown names are ignored.</li>
 *   <li>The first candidate whose scheme the client has and for whose identity type an identity provider is available
 *       is selected.</li>
 *   <li>If none qualifies, selection fails before anything is transmitted, naming the operation and why each candidate
 *       was rejected.</li>
 * </ol>
 *
 * <p>Immutable; built once per client.
 */
public final class AuthSchemeSelector {
    private final Map<String, AuthScheme<?>> schemes;
    private final List<String> preference;

    private AuthSchemeSelector(Map<String, AuthScheme<?>> schemes, List<String> preference) {
        this.schemes = schemes;
        this.preference = preference;
    }

    /** A selector over {@code schemes}, which must have distinct scheme IDs, with no preference. */
    public static AuthSchemeSelector create(List<? extends AuthScheme<?>> schemes) {
        return create(schemes, List.of());
    }

    /**
     * A selector over {@code schemes} with the scheme-name {@code preference}.
     *
     * @throws IllegalArgumentException if two schemes share an ID
     */
    public static AuthSchemeSelector create(List<? extends AuthScheme<?>> schemes, List<String> preference) {
        Map<String, AuthScheme<?>> byId = new LinkedHashMap<>();
        for (AuthScheme<?> scheme : schemes) {
            if (byId.put(scheme.schemeId(), scheme) != null) {
                throw new IllegalArgumentException("Two auth schemes have the ID " + scheme.schemeId());
            }
        }
        return new AuthSchemeSelector(Map.copyOf(byId), List.copyOf(preference));
    }

    /**
     * Parses a V2 preference list setting: comma-separated scheme names, whitespace ignored.
     */
    public static List<String> parsePreference(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.replaceAll("\\s+", "").split(",")).filter(s -> !s.isEmpty()).toList();
    }

    /** The candidates of {@code options} in preference order, per the class description. */
    public List<AuthSchemeOption> order(List<AuthSchemeOption> options) {
        if (preference.isEmpty()) {
            return options;
        }
        List<AuthSchemeOption> ordered = new ArrayList<>();
        for (String preferred : preference) {
            for (AuthSchemeOption option : options) {
                String id = option.schemeId();
                String name = id.contains("#") ? id.substring(id.indexOf('#') + 1) : id;
                if (name.equals(preferred) && !ordered.contains(option)) {
                    ordered.add(option);
                }
            }
        }
        options.stream().filter(o -> !ordered.contains(o)).forEach(ordered::add);
        return ordered;
    }

    /**
     * Selects the auth scheme for operation {@code operationId}.
     *
     * @throws SdkClientException if no candidate is usable, naming the operation and every rejection reason
     */
    public SelectedAuthScheme<?> select(String operationId, List<AuthSchemeOption> options,
                                        IdentityProviders identityProviders) {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(identityProviders, "identityProviders");
        List<String> rejected = new ArrayList<>();
        for (AuthSchemeOption option : order(options)) {
            AuthScheme<?> scheme = schemes.get(option.schemeId());
            if (scheme == null) {
                rejected.add(option.schemeId() + " is not supported by this client");
                continue;
            }
            Optional<SelectedAuthScheme<?>> selected = selected(scheme, option, identityProviders);
            if (selected.isPresent()) {
                return selected.get();
            }
            rejected.add(option.schemeId() + " has no identity provider for " + scheme.identityType().getSimpleName());
        }
        throw SdkClientException.create("Failed to select an auth scheme for operation " + operationId
                                        + ". None of the modeled auth schemes can be used: " + rejected);
    }

    private static <T extends Identity> Optional<SelectedAuthScheme<?>> selected(AuthScheme<T> scheme,
                                                                                 AuthSchemeOption option,
                                                                                 IdentityProviders providers) {
        Optional<IdentityProvider<? extends T>> provider = scheme.identityProvider(providers);
        return provider.map(p -> new SelectedAuthScheme<>(scheme, option, p));
    }

    @Override
    public String toString() {
        return "AuthSchemeSelector" + schemes.keySet();
    }
}
