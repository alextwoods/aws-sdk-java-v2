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

package software.amazon.awssdk.identity.spi;

/**
 * An identity provider whose contract permits refresh (Requirements 13.16, 13.17, 13.19).
 *
 * <p>A provider that caches temporary identity refreshes it on its own when it expires. When a service rejects an
 * identity before then (for example with {@code ExpiredToken}), the pipeline reports it with {@link #invalidate}; the
 * provider then never returns that identity again, and its next {@link #resolveIdentity(ResolveIdentityRequest)}
 * loads a fresh one, which the next attempt or call signs with. If that load fails, resolution fails with an
 * {@link IdentityRefreshException} whose cause is the load failure: a rejected or expired identity is never reused.
 *
 * <p>Providers that return fixed identities (static credentials) do not implement this interface, so a rejected fixed
 * identity is not retried with.
 *
 * @param <IdentityT> the identity type this provider resolves
 */
public interface RefreshableIdentityProvider<IdentityT extends Identity> extends IdentityProvider<IdentityT> {
    /**
     * Reports that a service rejected {@code identity}. If it is the identity this provider currently returns, the
     * provider discards it; otherwise (it already refreshed, or {@code identity} came from elsewhere) nothing changes.
     */
    void invalidate(Identity identity);
}
