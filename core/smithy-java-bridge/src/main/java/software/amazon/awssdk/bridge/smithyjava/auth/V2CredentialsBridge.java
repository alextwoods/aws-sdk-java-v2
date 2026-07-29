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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.java.auth.api.identity.IdentityResult;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver;
import software.amazon.smithy.java.context.Context;

/**
 * Bridges a v2 {@link AwsCredentialsProvider} to a smithy-java {@link AwsCredentialsResolver},
 * enabling the smithy-java pipeline to resolve credentials through the v2 credential chain.
 *
 * <p>The bridge maps v2 credential types to smithy-java identity types:
 * <ul>
 *   <li>{@link AwsCredentials} → {@link AwsCredentialsIdentity} (accessKeyId + secretAccessKey)</li>
 *   <li>{@link AwsSessionCredentials} → {@link AwsCredentialsIdentity} (accessKeyId + secretAccessKey + sessionToken)</li>
 * </ul>
 *
 * <p>Any exception thrown by the underlying v2 provider is wrapped in {@link SdkClientException}
 * with the original exception preserved as the cause.
 */
@SdkInternalApi
public final class V2CredentialsBridge implements AwsCredentialsResolver {

    private final AwsCredentialsProvider v2Provider;

    /**
     * Creates a new bridge wrapping the given v2 credentials provider.
     *
     * @param v2Provider the v2 credentials provider to delegate to
     * @throws SdkClientException if {@code v2Provider} is null
     */
    public V2CredentialsBridge(AwsCredentialsProvider v2Provider) {
        if (v2Provider == null) {
            throw SdkClientException.builder()
                .message("No credential provider was configured. An AwsCredentialsProvider is required "
                         + "for the smithy-java pipeline to sign requests.")
                .build();
        }
        this.v2Provider = v2Provider;
    }

    @Override
    public Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    public IdentityResult<AwsCredentialsIdentity> resolveIdentity(Context properties) {
        try {
            AwsCredentials v2Creds = v2Provider.resolveCredentials();
            AwsCredentialsIdentity identity = toSmithyIdentity(v2Creds);
            return IdentityResult.of(identity);
        } catch (Exception e) {
            throw SdkClientException.builder()
                .message("Failed to resolve credentials via v2 provider: " + e.getMessage())
                .cause(e)
                .build();
        }
    }

    private static AwsCredentialsIdentity toSmithyIdentity(AwsCredentials v2Creds) {
        if (v2Creds instanceof AwsSessionCredentials) {
            AwsSessionCredentials session = (AwsSessionCredentials) v2Creds;
            return AwsCredentialsIdentity.create(
                session.accessKeyId(),
                session.secretAccessKey(),
                session.sessionToken());
        }
        return AwsCredentialsIdentity.create(
            v2Creds.accessKeyId(),
            v2Creds.secretAccessKey());
    }
}
