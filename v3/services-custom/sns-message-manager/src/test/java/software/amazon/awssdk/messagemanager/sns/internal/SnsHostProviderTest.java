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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.endpoints.SnsEndpointParams;
import software.amazon.awssdk.services.sns.endpoints.SnsEndpointProvider;

class SnsHostProviderTest {

    @ParameterizedTest(name = "{0} - {1}")
    @CsvSource({
        // gov regions
        "us-gov-west-1, sns-us-gov-west-1.amazonaws.com",
        "us-gov-east-1, sns-us-gov-west-1.amazonaws.com",
        // cn regions
        "cn-north-1, sns-cn-north-1.amazonaws.com.cn",
        "cn-northwest-1, sns-cn-northwest-1.amazonaws.com.cn",
        // opt-in regions
        "me-south-1, sns-signing.me-south-1.amazonaws.com",
        "ap-east-1, sns-signing.ap-east-1.amazonaws.com",
        "ap-east-2, sns-signing.ap-east-2.amazonaws.com",
        "ap-southeast-5, sns-signing.ap-southeast-5.amazonaws.com",
        "ap-southeast-6, sns-signing.ap-southeast-6.amazonaws.com",
        "ap-southeast-7, sns-signing.ap-southeast-7.amazonaws.com",
        "mx-central-1, sns-signing.mx-central-1.amazonaws.com",
        // iso regions
        "us-iso-east-1, sns-us-iso-east-1.c2s.ic.gov",
        "us-isob-east-1, sns-us-isob-east-1.sc2s.sgov.gov",
        "us-isof-east-1, sns-signing.us-isof-east-1.csp.hci.ic.gov",
        "us-isof-south-1, sns-signing.us-isof-south-1.csp.hci.ic.gov",
        "eu-isoe-west-1, sns-signing.eu-isoe-west-1.cloud.adc-e.uk",
        // eusc
        "eusc-de-east-1, sns-signing.eusc-de-east-1.amazonaws.eu",
        // other regions
        "us-east-1, sns.amazonaws.com",
        "us-west-1, sns.amazonaws.com",
        // unknown regions: the partition's DNS suffix
        "us-east-9, sns.amazonaws.com",
        "foo-bar-1, sns.amazonaws.com",
        "cn-northwest-9, sns.amazonaws.com.cn"
    })
    void signingCertCommonName_returnsCorrectNameForRegion(String region, String expectedCommonName) {
        assertThat(new SnsHostProvider(Region.of(region)).signingCertCommonName()).isEqualTo(expectedCommonName);
    }

    static Stream<Region> regions() {
        return Stream.concat(Region.regions().stream(),
                             Stream.of("us-east-9", "us-gov-west-3", "cn-northwest-9", "us-iso-west-7", "foo-bar-1")
                                   .map(Region::of));
    }

    /**
     * The certificate host is exactly the endpoint the canonical SNS rule set resolves (FIPS and dual-stack off, no
     * custom endpoint), as V2's use of the generated endpoint provider produced, for every region constant and
     * unknown regions of each partition pattern.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("regions")
    void regionalEndpoint_isTheSnsEndpointRuleSetResult(Region region) {
        URI expected = SnsEndpointProvider.defaultProvider()
                                          .resolveEndpoint(SnsEndpointParams.builder().region(region).useFips(false)
                                                                            .useDualStack(false).build())
                                          .join()
                                          .url();

        assertThat(new SnsHostProvider(region).regionalEndpoint()).isEqualTo(expected);
    }

    @Test
    void ctor_nullArguments_throw() {
        assertThatThrownBy(() -> new SnsHostProvider(null)).hasMessage("region must not be null");
        assertThatThrownBy(() -> new SnsHostProvider(Region.US_WEST_2, null)).hasMessage("partitions must not be null");
    }
}
