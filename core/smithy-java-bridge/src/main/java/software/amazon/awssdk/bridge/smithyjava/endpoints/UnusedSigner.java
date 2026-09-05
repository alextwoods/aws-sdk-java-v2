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

package software.amazon.awssdk.bridge.smithyjava.endpoints;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * A signer that is never invoked.
 *
 * <p>{@link software.amazon.awssdk.core.SelectedAuthScheme} requires a non-null signer, but the v2
 * endpoint rules engine only ever reads the {@code identity()} off it. Signing itself is done by
 * smithy-java's {@code SigV4Signer}, so nothing in the bridge ever calls this.
 */
@SdkInternalApi
final class UnusedSigner implements HttpSigner<AwsCredentialsIdentity> {

    static final UnusedSigner INSTANCE = new UnusedSigner();

    private UnusedSigner() {
    }

    @Override
    public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
        throw new UnsupportedOperationException("Signing is performed by smithy-java, not by the v2 signer");
    }

    @Override
    public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        throw new UnsupportedOperationException("Signing is performed by smithy-java, not by the v2 signer");
    }
}
