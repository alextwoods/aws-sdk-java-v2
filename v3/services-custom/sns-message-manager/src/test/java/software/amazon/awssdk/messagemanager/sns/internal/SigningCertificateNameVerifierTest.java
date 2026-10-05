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

package software.amazon.awssdk.messagemanager.sns.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The signing-name rules of the Apache HttpClient 5 {@code DefaultHostnameVerifier} that V2 used, over the fixture
 * certificates and literal identities.
 */
class SigningCertificateNameVerifierTest {

    @ParameterizedTest(name = "{0} issued to {1}: {2}")
    @CsvSource({
        // SAN dNSName match, case-insensitive.
        "sns-global, sns.amazonaws.com, true",
        "sns-global, SNS.AMAZONAWS.COM, true",
        "sns-global, sns.us-east-1.amazonaws.com, false",
        "sns-signing-ap-east-1, sns-signing.ap-east-1.amazonaws.com, true",
        "sns-signing-ap-east-1, sns.amazonaws.com, false",
        // SANs win over the subject CN when present.
        "wrong-name, sns.amazonaws.com, false",
        "wrong-name, sns.example.com, true",
        // The CN is the fallback when there are no DNS SANs.
        "cn-only, sns-cn-north-1.amazonaws.com.cn, true",
        "cn-only, sns.amazonaws.com, false",
        // A wildcard matches within one label only.
        "wildcard, sns.amazonaws.com, true",
        "wildcard, sns-signing.ap-east-1.amazonaws.com, false",
        "wildcard, amazonaws.com, false"
    })
    void matches_fixtureCertificates(String certificate, String expectedName, boolean matches) throws Exception {
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
            new ByteArrayInputStream(SnsTestFixtures.certificate(certificate)));

        assertThat(SigningCertificateNameVerifier.matches(expectedName, cert)).isEqualTo(matches);
    }

    @ParameterizedTest(name = "{0} ~ {1}: {2}")
    @CsvSource({
        "sns.amazonaws.com, sns.amazonaws.com, true",
        "sns.amazonaws.com, *.amazonaws.com, true",
        "sns.amazonaws.com, s*.amazonaws.com, true",
        "sns.amazonaws.com, *s.amazonaws.com, true",
        "sns.amazonaws.com, x*.amazonaws.com, false",
        "sns.amazonaws.com, *, false",
        "a.b.amazonaws.com, *.amazonaws.com, false",
        "ab.com, ab*b.com, false",
        "sns.amazonaws.com, sns.amazonaws.com.evil, false"
    })
    void matchesIdentity_strictWildcards(String name, String identity, boolean matches) {
        assertThat(SigningCertificateNameVerifier.matchesIdentity(name, identity)).isEqualTo(matches);
    }
}
