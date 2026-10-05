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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;

class CertificateUrlValidatorTest {
    private static final String CERT_HOST = "sns.us-west-2.amazonaws.com";
    private static final CertificateUrlValidator VALIDATOR = new CertificateUrlValidator(CERT_HOST);

    @Test
    void validate_urlValid_succeeds() {
        assertThatCode(() -> VALIDATOR.validate(URI.create("https://" + CERT_HOST + "/SimpleNotificationService-1.pem")))
            .doesNotThrowAnyException();
    }

    @Test
    void validate_urlNull_throws() {
        assertThatThrownBy(() -> VALIDATOR.validate(null))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Certificate URL cannot be null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://sns.us-west-2.amazonaws.com/cert.pem", "ftp://sns.us-west-2.amazonaws.com/cert.pem",
                            "HTTPS://sns.us-west-2.amazonaws.com/cert.pem", "/cert.pem"})
    void validate_schemeNotHttps_throws(String url) {
        assertThatThrownBy(() -> VALIDATOR.validate(URI.create(url)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Certificate URL must use HTTPS");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://sns.us-east-1.amazonaws.com/cert.pem",
        "https://evil.example/cert.pem",
        "https://sns.us-west-2.amazonaws.com.evil.example/cert.pem",
        "https://evil.sns.us-west-2.amazonaws.com/cert.pem",
        "https://sns.us-west-2.amazonaws.com@evil.example/cert.pem",
        "https://evil.example/sns.us-west-2.amazonaws.com/cert.pem",
        "https://evil.example?host=sns.us-west-2.amazonaws.com",
        "https://SNS.US-WEST-2.AMAZONAWS.COM/cert.pem",
        "https:sns.us-west-2.amazonaws.com/cert.pem"})
    void validate_urlHostIsNotTheExpectedSnsHost_throws(String url) {
        assertThatThrownBy(() -> VALIDATOR.validate(URI.create(url)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Certificate URL does not match expected host: " + CERT_HOST);
    }

    @Test
    void constructor_hostNullOrBlank_throws() {
        assertThatThrownBy(() -> new CertificateUrlValidator(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Expected certificate host cannot be null or empty");
        assertThatThrownBy(() -> new CertificateUrlValidator(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Expected certificate host cannot be null or empty");
    }
}
