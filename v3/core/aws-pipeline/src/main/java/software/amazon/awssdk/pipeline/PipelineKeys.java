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

package software.amazon.awssdk.pipeline;

import java.util.List;
import software.amazon.awssdk.auth.api.SelectedAuthScheme;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.identity.spi.Identity;

/**
 * The attempt-scoped context values the pipeline itself produces. Every {@link ContextLayout} holds them in fixed
 * slots, and only the stage that produces a value writes it: {@link AttemptContext#put} rejects them, so extension code
 * can read but never replace them (Requirements 10.8, 10.9).
 */
public final class PipelineKeys {
    /** The one-based number of the attempt, set when the attempt starts. */
    public static final ContextKey<Integer> ATTEMPT_NUMBER = ContextKey.attempt("AttemptNumber", Integer.class);

    /** The endpoint the attempt transmits to, visible once the endpoint stage has resolved it. */
    public static final ContextKey<Endpoint> ENDPOINT = ContextKey.attempt("ResolvedEndpoint", Endpoint.class);

    /** The auth scheme selected for the attempt, visible once the auth-scheme stage has selected it. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final ContextKey<SelectedAuthScheme<?>> AUTH_SCHEME =
        (ContextKey) ContextKey.attempt("SelectedAuthScheme", SelectedAuthScheme.class);

    /** The identity the attempt signs with, visible once the identity stage has resolved it. */
    public static final ContextKey<Identity> IDENTITY = ContextKey.attempt("ResolvedIdentity", Identity.class);

    /** The keys above in slot order: {@code BUILT_IN.get(i)} occupies attempt slot {@code i} of every layout. */
    static final List<ContextKey<?>> BUILT_IN = List.of(ATTEMPT_NUMBER, ENDPOINT, AUTH_SCHEME, IDENTITY);

    static final int ATTEMPT_NUMBER_SLOT = 0;
    static final int ENDPOINT_SLOT = 1;
    static final int AUTH_SCHEME_SLOT = 2;
    static final int IDENTITY_SLOT = 3;

    private PipelineKeys() {
    }

    /** Whether {@code key} is one of the pipeline-produced keys. */
    static boolean isBuiltIn(ContextKey<?> key) {
        return key == ATTEMPT_NUMBER || key == ENDPOINT || key == AUTH_SCHEME || key == IDENTITY;
    }
}
