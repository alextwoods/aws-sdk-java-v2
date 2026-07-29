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

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.NotBlank;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.smithy.java.auth.api.identity.IdentityResult;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity;
import software.amazon.smithy.java.context.Context;

/**
 * Property-based tests for {@link V2CredentialsBridge} verifying credential round-trip preservation.
 *
 * <p><b>Validates: Requirements 1.2, 1.3</b>
 *
 * <p><b>Property 1: Credential Round-Trip Preservation</b> — For any v2 AwsCredentials (basic or session)
 * with arbitrary accessKeyId, secretAccessKey, and optional sessionToken values, the
 * V2CredentialsBridge.resolveIdentity() SHALL produce an AwsCredentialsIdentity where accessKeyId(),
 * secretAccessKey(), and (when present) sessionToken() are equal to the original v2 credential values.
 */
class V2CredentialsBridgePropertyTest {

    /**
     * Verifies that basic credentials (accessKeyId + secretAccessKey, no session token)
     * are preserved exactly through the V2CredentialsBridge round-trip.
     *
     * <p><b>Validates: Requirements 1.2</b>
     */
    @Property(tries = 100)
    void basicCredentials_roundTripPreservesAllValues(
            @ForAll("credentialStrings") String accessKeyId,
            @ForAll("credentialStrings") String secretAccessKey) {

        AwsCredentialsProvider provider = () -> AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        V2CredentialsBridge bridge = new V2CredentialsBridge(provider);

        IdentityResult<AwsCredentialsIdentity> result = bridge.resolveIdentity(Context.create());
        AwsCredentialsIdentity identity = result.identity();

        assertThat(identity.accessKeyId()).isEqualTo(accessKeyId);
        assertThat(identity.secretAccessKey()).isEqualTo(secretAccessKey);
        assertThat(identity.sessionToken()).isNull();
    }

    /**
     * Verifies that session credentials (accessKeyId + secretAccessKey + sessionToken)
     * are preserved exactly through the V2CredentialsBridge round-trip.
     *
     * <p><b>Validates: Requirements 1.3</b>
     */
    @Property(tries = 100)
    void sessionCredentials_roundTripPreservesAllValues(
            @ForAll("credentialStrings") String accessKeyId,
            @ForAll("credentialStrings") String secretAccessKey,
            @ForAll("credentialStrings") String sessionToken) {

        AwsCredentialsProvider provider = () -> AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken);
        V2CredentialsBridge bridge = new V2CredentialsBridge(provider);

        IdentityResult<AwsCredentialsIdentity> result = bridge.resolveIdentity(Context.create());
        AwsCredentialsIdentity identity = result.identity();

        assertThat(identity.accessKeyId()).isEqualTo(accessKeyId);
        assertThat(identity.secretAccessKey()).isEqualTo(secretAccessKey);
        assertThat(identity.sessionToken()).isEqualTo(sessionToken);
    }

    /**
     * Verifies that for any credential tuple (basic or session), the bridge always
     * preserves the exact values. This combines both basic and session cases into a
     * single property covering the full input space.
     *
     * <p><b>Validates: Requirements 1.2, 1.3</b>
     */
    @Property(tries = 100)
    void anyCredentials_roundTripPreservesAllValues(@ForAll("credentialTuples") CredentialTuple tuple) {

        AwsCredentialsProvider provider;
        if (tuple.sessionToken != null) {
            provider = () -> AwsSessionCredentials.create(tuple.accessKeyId, tuple.secretAccessKey, tuple.sessionToken);
        } else {
            provider = () -> AwsBasicCredentials.create(tuple.accessKeyId, tuple.secretAccessKey);
        }

        V2CredentialsBridge bridge = new V2CredentialsBridge(provider);
        IdentityResult<AwsCredentialsIdentity> result = bridge.resolveIdentity(Context.create());
        AwsCredentialsIdentity identity = result.identity();

        assertThat(identity.accessKeyId()).isEqualTo(tuple.accessKeyId);
        assertThat(identity.secretAccessKey()).isEqualTo(tuple.secretAccessKey);
        assertThat(identity.sessionToken()).isEqualTo(tuple.sessionToken);
    }

    @Provide
    Arbitrary<String> credentialStrings() {
        // Generate non-empty strings typical of credential values:
        // alphanumeric with some special characters commonly found in AWS credentials
        return Arbitraries.strings()
                          .withCharRange('A', 'Z')
                          .withCharRange('a', 'z')
                          .withCharRange('0', '9')
                          .withChars('+', '/', '=')
                          .ofMinLength(1)
                          .ofMaxLength(128);
    }

    @Provide
    Arbitrary<CredentialTuple> credentialTuples() {
        Arbitrary<String> accessKeyId = credentialStrings();
        Arbitrary<String> secretAccessKey = credentialStrings();
        // sessionToken is optional — null for basic credentials, non-null for session credentials
        Arbitrary<String> sessionToken = Arbitraries.oneOf(
            Arbitraries.just(null),
            credentialStrings()
        );

        return Combinators.combine(accessKeyId, secretAccessKey, sessionToken)
                          .as(CredentialTuple::new);
    }

    /**
     * Simple data holder for a credential tuple (accessKeyId, secretAccessKey, optional sessionToken).
     */
    static final class CredentialTuple {
        final String accessKeyId;
        final String secretAccessKey;
        final String sessionToken;

        CredentialTuple(String accessKeyId, String secretAccessKey, String sessionToken) {
            this.accessKeyId = accessKeyId;
            this.secretAccessKey = secretAccessKey;
            this.sessionToken = sessionToken;
        }

        @Override
        public String toString() {
            return "CredentialTuple{accessKeyId='" + accessKeyId + "', secretAccessKey='" + secretAccessKey
                   + "', sessionToken=" + (sessionToken == null ? "null" : "'" + sessionToken + "'") + "}";
        }
    }
}
