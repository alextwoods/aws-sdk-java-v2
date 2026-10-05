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

package software.amazon.awssdk.auth.aws.internal;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.util.Map;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.auth.aws.RegionSet;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/** The {@link AwsV4aHttpSigner} implementation. Not a public API. */
public final class DefaultAwsV4aHttpSigner implements AwsV4aHttpSigner {
    public static final DefaultAwsV4aHttpSigner INSTANCE = new DefaultAwsV4aHttpSigner();
    public static final String REGION_SET_NAME = "X-Amz-Region-Set";

    private DefaultAwsV4aHttpSigner() {
    }

    @Override
    public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
        if (V4RequestSigning.isEventStream(request.request())) {
            // V2 parity: SigV4a has no event stream frame signature algorithm.
            throw SdkClientException.create("SigV4a cannot sign an event stream request; the operation's event "
                                            + "stream needs SigV4.");
        }
        AwsCredentialsIdentity credentials = request.identity();
        RegionSet regionSet = request.requireProperty(REGION_SET);
        String token = credentials instanceof AwsSessionCredentialsIdentity session ? session.sessionToken() : null;
        ECPrivateKey key = V4aKeyDerivation.privateKey(credentials.accessKeyId(), credentials.secretAccessKey());
        V4RequestSigning.Spec spec = new V4RequestSigning.Spec(
            V4aKeyDerivation.ALGORITHM, null, "X-Amz-Security-Token", token,
            Map.of(REGION_SET_NAME, regionSet.asString()),
            (stringToSign, date, service, scopeRegion) -> SigningCrypto.hex(ecdsa(key, stringToSign)));
        V4RequestSigning.Result result = V4RequestSigning.sign(request, spec);
        return new SignedRequest(result.request(), result.signature());
    }

    private static byte[] ecdsa(ECPrivateKey key, String stringToSign) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(key);
            signature.update(stringToSign.getBytes(StandardCharsets.UTF_8));
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw SdkClientException.create("Unable to compute the SigV4a signature", e);
        }
    }

    @Override
    public String toString() {
        return "AwsV4aHttpSigner";
    }
}
