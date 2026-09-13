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

package software.amazon.awssdk.core.internal.http.auth;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.http.auth.AuthSchemeResolver;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProviders;

/**
 * A single-entry, per-client cache of the call-independent half of auth scheme resolution: which scheme was selected for
 * a given options list, its signer, its identity provider, its identity request, and the option after the pre-existing
 * scheme's properties were merged in. Only resolving the identity is left for each call.
 *
 * <p>The key is the <em>identity</em> of the four inputs the result is a pure function of: the auth scheme options list,
 * the client's auth scheme map, the identity providers in effect, and the option of the scheme that was already on the
 * execution attributes before resolution (the legacy signing-name/region placeholder, normally). Identity rather than
 * equality is deliberate. The generated clients hand back the same options list instance for the same parameters when
 * the default provider is in use, and the other three are the same instances on every call of a client; anything that
 * legitimately changes the answer — a request-level auth scheme provider or credentials override, a plugin that alters
 * the auth schemes, an interceptor that edited the selected scheme — arrives as a different instance and misses into the
 * ordinary path. A miss is never wrong, only slower.
 *
 * <p>An endpoint-based auth provider (S3) builds a fresh options list on every call, so such a client would miss on
 * every call and, if the cache were written on every miss, every call would write a shared field. After
 * {@value #MAX_CONSECUTIVE_MISSES} consecutive misses the cache stops storing entries for the client; the per-call cost
 * is then one volatile read and one reference comparison. A hit resets the streak, so a client whose first calls used a
 * request-level override and then settled still gets cached.
 *
 * <p>Racing threads compute equivalent entries for identical inputs, so a lost write costs one re-resolution; races on
 * the miss counter are equally benign (a few extra or fewer stores).
 */
@SdkInternalApi
public final class AuthSchemeResolutionCache {
    static final int MAX_CONSECUTIVE_MISSES = 8;

    private volatile Entry<?> entry;
    /**
     * Written only on a miss (at most {@link #MAX_CONSECUTIVE_MISSES} times in a row) and on the first hit after a
     * miss, so the steady-state hit path does one volatile read of it and never writes.
     */
    private volatile int consecutiveMisses;

    /**
     * The cached resolution for these inputs, or {@code null} if it has to be computed.
     */
    public Entry<? extends Identity> lookup(List<AuthSchemeOption> authOptions,
                                            Map<String, AuthScheme<?>> authSchemes,
                                            IdentityProviders identityProviders,
                                            AuthSchemeOption existingOption) {
        Entry<?> cached = entry;
        if (cached != null
            && cached.authOptions == authOptions
            && cached.authSchemes == authSchemes
            && cached.identityProviders == identityProviders
            && cached.existingOption == existingOption) {
            if (consecutiveMisses != 0) {
                consecutiveMisses = 0;
            }
            return cached;
        }
        return null;
    }

    /**
     * Remember a resolution computed for these inputs, unless this client has been missing consistently (see the class
     * documentation).
     */
    public void store(List<AuthSchemeOption> authOptions,
                      Map<String, AuthScheme<?>> authSchemes,
                      IdentityProviders identityProviders,
                      AuthSchemeOption existingOption,
                      AuthSchemeResolver.Resolution<? extends Identity> resolution) {
        int misses = consecutiveMisses;
        if (misses >= MAX_CONSECUTIVE_MISSES) {
            return;
        }
        consecutiveMisses = misses + 1;
        entry = new Entry<>(authOptions, authSchemes, identityProviders, existingOption, resolution);
    }

    /**
     * Test hook: whether the cache currently holds an entry.
     */
    boolean hasEntry() {
        return entry != null;
    }

    /**
     * A cached resolution and the inputs it was computed from.
     */
    public static final class Entry<T extends Identity> {
        private final List<AuthSchemeOption> authOptions;
        private final Map<String, AuthScheme<?>> authSchemes;
        private final IdentityProviders identityProviders;
        private final AuthSchemeOption existingOption;
        private final AuthSchemeResolver.Resolution<T> resolution;

        private Entry(List<AuthSchemeOption> authOptions,
                      Map<String, AuthScheme<?>> authSchemes,
                      IdentityProviders identityProviders,
                      AuthSchemeOption existingOption,
                      AuthSchemeResolver.Resolution<T> resolution) {
            this.authOptions = authOptions;
            this.authSchemes = authSchemes;
            this.identityProviders = identityProviders;
            this.existingOption = existingOption;
            this.resolution = resolution;
        }

        /**
         * The resolution with the merged option baked in; {@link AuthSchemeResolver.Resolution#select} it for this call.
         */
        public AuthSchemeResolver.Resolution<T> resolution() {
            return resolution;
        }
    }
}
