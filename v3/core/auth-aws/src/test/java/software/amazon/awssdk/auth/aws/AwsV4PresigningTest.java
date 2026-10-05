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

package software.amazon.awssdk.auth.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.api.AuthSchemeIds;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.auth.aws.AwsV4FamilyHttpSigner.AuthLocation;
import software.amazon.awssdk.core.exception.SdkClientException;

class AwsV4PresigningTest {
    @Test
    void queryString_keepsTheSelectedOptionAndPlacesTheSignatureInTheQuery() {
        AuthSchemeOption selected = AuthSchemeOption.builder().schemeId(AuthSchemeIds.SIGV4)
                                                    .putSignerProperty(AwsV4HttpSigner.REGION_NAME, "us-west-2")
                                                    .build();

        AuthSchemeOption presign = AwsV4Presigning.queryString(Duration.ofMinutes(5), false).apply(selected);

        assertEquals(AuthSchemeIds.SIGV4, presign.schemeId());
        assertEquals("us-west-2", presign.signerProperty(AwsV4HttpSigner.REGION_NAME));
        assertEquals(AuthLocation.QUERY_STRING, presign.signerProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION));
        assertEquals(Duration.ofMinutes(5), presign.signerProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION));
        assertEquals(false, presign.signerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED));
    }

    @Test
    void s3QueryString_signsAnUnsignedPayloadWithS3CanonicalUriRules() {
        AuthSchemeOption presign = AwsV4Presigning.s3QueryString(Duration.ofMinutes(5))
                                                  .apply(AuthSchemeOption.builder().schemeId(AuthSchemeIds.SIGV4)
                                                                         .build());

        assertEquals(AuthLocation.QUERY_STRING, presign.signerProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION));
        assertEquals(false, presign.signerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED));
        assertEquals(false, presign.signerProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE));
        assertEquals(false, presign.signerProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH));
    }

    @Test
    void queryString_rejectsExpirationsOutsideTheSigV4Bound() {
        assertThrows(SdkClientException.class, () -> AwsV4Presigning.queryString(Duration.ZERO, false));
        assertThrows(SdkClientException.class,
                     () -> AwsV4Presigning.queryString(Duration.ofDays(7).plusSeconds(1), false));
    }
}
