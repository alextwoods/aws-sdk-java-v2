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
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.java.context.Context;

/**
 * Property-based test for credential resolution error wrapping in {@link V2CredentialsBridge}.
 *
 * <p><b>Validates: Requirements 1.4</b>
 *
 * <p>Property 2: Credential Resolution Error Wrapping — For any exception thrown by a v2
 * {@code AwsCredentialsProvider.resolveCredentials()}, the {@code V2CredentialsBridge} SHALL throw
 * an {@code SdkClientException} whose {@code cause()} is the original exception and whose
 * {@code message()} is non-null and non-empty.
 */
class V2CredentialsBridgeErrorPropertyTest {

    /**
     * Provides arbitrary exceptions of various types that a credentials provider might throw.
     * Generates RuntimeExceptions, IOExceptions (wrapped in RuntimeException by providers),
     * NullPointerExceptions, IllegalStateExceptions, and TimeoutExceptions with random messages.
     */
    @Provide
    Arbitrary<Exception> arbitraryExceptions() {
        Arbitrary<String> messages = Arbitraries.strings()
            .ofMinLength(0)
            .ofMaxLength(200)
            .alpha().numeric().withChars(' ', '-', '_', ':', '.');

        return Arbitraries.oneOf(
            messages.map(RuntimeException::new),
            messages.map(IOException::new),
            messages.map(NullPointerException::new),
            messages.map(IllegalStateException::new),
            messages.map(IllegalArgumentException::new),
            messages.map(TimeoutException::new),
            messages.map(UnsupportedOperationException::new),
            // Exception with null message
            Arbitraries.just(new RuntimeException((String) null)),
            // Exception with empty message
            Arbitraries.just(new RuntimeException("")),
            // Nested cause chain
            messages.map(msg -> new RuntimeException(msg, new IOException("nested cause")))
        );
    }

    /**
     * <b>Property 2: Credential Resolution Error Wrapping</b>
     *
     * <p>For any exception thrown by a v2 AwsCredentialsProvider.resolveCredentials(),
     * the V2CredentialsBridge SHALL throw an SdkClientException whose cause() is the
     * original exception and whose message() is non-null and non-empty.
     *
     * <p><b>Validates: Requirements 1.4</b>
     */
    @Property(tries = 100)
    void resolveIdentity_alwaysWrapsThrownExceptionInSdkClientException(
            @ForAll("arbitraryExceptions") Exception originalException) {

        AwsCredentialsProvider throwingProvider = () -> {
            if (originalException instanceof RuntimeException) {
                throw (RuntimeException) originalException;
            }
            // Checked exceptions must be wrapped - providers typically wrap them
            throw new RuntimeException(originalException);
        };

        V2CredentialsBridge bridge = new V2CredentialsBridge(throwingProvider);

        Throwable thrown = catchThrowable(() -> bridge.resolveIdentity(Context.create()));

        // Must be SdkClientException
        assertThat(thrown)
            .isInstanceOf(SdkClientException.class);

        SdkClientException sdkEx = (SdkClientException) thrown;

        // Must have non-null, non-empty message
        assertThat(sdkEx.getMessage())
            .isNotNull()
            .isNotEmpty();

        // Must preserve the original exception as the cause
        if (originalException instanceof RuntimeException) {
            assertThat(sdkEx.getCause()).isSameAs(originalException);
        } else {
            // Checked exceptions are wrapped in RuntimeException by the provider lambda,
            // so the direct cause is the RuntimeException wrapper
            assertThat(sdkEx.getCause())
                .isInstanceOf(RuntimeException.class)
                .hasCause(originalException);
        }
    }

    /**
     * Verifies that the wrapping message always contains contextual information
     * indicating the failure was during credential resolution.
     *
     * <p><b>Validates: Requirements 1.4</b>
     */
    @Property(tries = 100)
    void resolveIdentity_wrappedExceptionMessageContainsCredentialContext(
            @ForAll("arbitraryExceptions") Exception originalException) {

        AwsCredentialsProvider throwingProvider = () -> {
            if (originalException instanceof RuntimeException) {
                throw (RuntimeException) originalException;
            }
            throw new RuntimeException(originalException);
        };

        V2CredentialsBridge bridge = new V2CredentialsBridge(throwingProvider);

        Throwable thrown = catchThrowable(() -> bridge.resolveIdentity(Context.create()));

        assertThat(thrown)
            .isInstanceOf(SdkClientException.class);

        // The message should indicate credential resolution failed
        assertThat(thrown.getMessage())
            .contains("Failed to resolve credentials via v2 provider");
    }
}
