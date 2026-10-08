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

package software.amazon.awssdk.test.fault;

import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An auth scheme under the SigV4 scheme ID that signs with a {@link ScriptedSigner}, so a service whose operations
 * model {@code aws.auth#sigv4} selects it in place of the real signer: identity resolution and signing then run every
 * attempt exactly as for SigV4, with both steps scripted.
 */
public final class FaultAuthScheme implements AuthScheme<AwsCredentialsIdentity> {
    /** The modeled SigV4 scheme ID. */
    public static final String SIGV4 = "aws.auth#sigv4";

    private final ScriptedSigner signer;

    public FaultAuthScheme(ScriptedSigner signer) {
        this.signer = Objects.requireNonNull(signer, "signer");
    }

    /** A selector offering only this scheme. */
    public AuthSchemeSelector selector() {
        return AuthSchemeSelector.create(List.of(this));
    }

    @Override
    public String schemeId() {
        return SIGV4;
    }

    @Override
    public Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    public HttpSigner<AwsCredentialsIdentity> signer() {
        return signer;
    }
}
