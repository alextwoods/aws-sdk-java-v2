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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.core.override.CallOptions;

/**
 * Derives the ordered auth scheme candidates of one call from the operation's {@link ModeledAuth} traits, for the
 * {@link PipelineStage#SELECT_AUTH_SCHEME} stage. AWS clients supply a resolver that adds the signer properties of
 * each modeled scheme (signing name, region, payload signing), for example over {@code AwsAuthSchemeOptions}.
 *
 * <p>Implementations are immutable, thread safe and non-blocking: the stage runs on the call's driver, which may be
 * an async continuation.
 */
@FunctionalInterface
public interface AuthOptionsResolver {
    /** The candidates of a call of an operation with {@code auth}, in modeled priority order. */
    List<AuthSchemeOption> options(ModeledAuth auth, CallOptions options);

    /** The modeled effective schemes, in order, without signer or identity properties. */
    static AuthOptionsResolver modeled() {
        return (auth, options) -> {
            List<String> ids = auth.effectiveSchemeIds();
            List<AuthSchemeOption> result = new ArrayList<>(ids.size());
            for (String id : ids) {
                result.add(AuthSchemeOption.builder().schemeId(id).build());
            }
            return result;
        };
    }
}
