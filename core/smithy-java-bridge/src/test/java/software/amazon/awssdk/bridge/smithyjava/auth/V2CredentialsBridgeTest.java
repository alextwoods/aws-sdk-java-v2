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

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.java.auth.api.identity.IdentityResult;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity;
import software.amazon.smithy.java.context.Context;
import org.junit.jupiter.api.Test;

class V2CredentialsBridgeTest {

    @Test
    void identityType_returnsAwsCredentialsIdentityClass() {
        AwsCredentialsProvider provider = () -> AwsBasicCredentials.create("akid", "skid");
        V2CredentialsBridge bridge = new V2CredentialsBridge(provider);

        assertThat(bridge.identityType()).isEqualTo(AwsCredentialsIdentity.class);
    }

    @Test
    void resolveIdentity_basicCredentials_mapsCorrectly() {
        AwsCredentialsProvider provider = () -> AwsBasicCredentials.create("myAccessKey", "mySecretKey");
        V2CredentialsBridge bridge = new V2CredentialsBridge(provider);

        IdentityResult<AwsCredentialsIdentity> result = bridge.resolveIdentity(Context.create());
        AwsCredentialsIdentity identity = result.identity();

        assertThat(identity.accessKeyId()).isEqualTo("myAccessKey");
        assertThat(identity.secretAccessKey()).isEqualTo("mySecretKey");
        assertThat(identity.sessionToken()).isNull();
    }

    @Test
    void resolveIdentity_sessionCredentials_includesSessionToken() {
        AwsCredentialsProvider provider = () -> AwsSessionCredentials.create("akid", "skid", "token123");
        V2CredentialsBridge bridge = new V2CredentialsBridge(provider);

        IdentityResult<AwsCredentialsIdentity> result = bridge.resolveIdentity(Context.create());
        AwsCredentialsIdentity identity = result.identity();

        assertThat(identity.accessKeyId()).isEqualTo("akid");
        assertThat(identity.secretAccessKey()).isEqualTo("skid");
        assertThat(identity.sessionToken()).isEqualTo("token123");
    }

    @Test
    void constructor_nullProvider_throwsSdkClientException() {
        assertThatThrownBy(() -> new V2CredentialsBridge(null))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("No credential provider was configured");
    }

    @Test
    void resolveIdentity_providerThrowsException_wrapsInSdkClientException() {
        RuntimeException originalException = new RuntimeException("credentials expired");
        AwsCredentialsProvider provider = () -> {
            throw originalException;
        };
        V2CredentialsBridge bridge = new V2CredentialsBridge(provider);

        assertThatThrownBy(() -> bridge.resolveIdentity(Context.create()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Failed to resolve credentials via v2 provider")
            .hasMessageContaining("credentials expired")
            .hasCause(originalException);
    }

    @Test
    void resolveIdentity_providerThrowsCheckedExceptionWrapped_preservesCause() {
        IllegalStateException cause = new IllegalStateException("provider not initialized");
        AwsCredentialsProvider provider = () -> {
            throw cause;
        };
        V2CredentialsBridge bridge = new V2CredentialsBridge(provider);

        assertThatThrownBy(() -> bridge.resolveIdentity(Context.create()))
            .isInstanceOf(SdkClientException.class)
            .hasCause(cause);
    }
}
