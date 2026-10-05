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

package software.amazon.awssdk.checksums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

class HttpChecksumMetadataTest {
    @Test
    void fromNode_keepsExactTraitValue() {
        Node node = Node.parse("{\"requestChecksumRequired\": true, \"requestAlgorithmMember\": \"ChecksumAlgorithm\","
                               + "\"requestValidationModeMember\": \"ChecksumMode\","
                               + "\"responseAlgorithms\": [\"CRC64NVME\", \"CRC32\", \"XXHASH3\"]}");

        HttpChecksumMetadata metadata = HttpChecksumMetadata.fromNode(node);

        assertThat(metadata).isEqualTo(HttpChecksumMetadata.builder()
                                                           .requestChecksumRequired(true)
                                                           .requestAlgorithmMember("ChecksumAlgorithm")
                                                           .requestValidationModeMember("ChecksumMode")
                                                           .addResponseAlgorithm("CRC64NVME")
                                                           .addResponseAlgorithm("CRC32")
                                                           .addResponseAlgorithm("XXHASH3")
                                                           .build());
        assertThat(metadata.responseAlgorithms()).containsExactly("CRC64NVME", "CRC32", "XXHASH3");
    }

    @Test
    void build_whenNoBehaviorOrHalfResponseBehavior_rejects() {
        assertThatThrownBy(() -> HttpChecksumMetadata.builder().build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HttpChecksumMetadata.builder().requestChecksumRequired(true)
                                                     .requestValidationModeMember("Mode").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("responseAlgorithms");
        assertThatThrownBy(() -> HttpChecksumMetadata.builder().addResponseAlgorithm("CRC32")
                                                     .addResponseAlgorithm("CRC32"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checksumOptions_parseSettingTextAndDefault() {
        assertThat(RequestChecksumCalculation.fromValue(" when_required ")).isEqualTo(
            RequestChecksumCalculation.WHEN_REQUIRED);
        assertThat(ResponseChecksumValidation.fromValue("WHEN_SUPPORTED")).isEqualTo(
            ResponseChecksumValidation.WHEN_SUPPORTED);
        assertThatThrownBy(() -> RequestChecksumCalculation.fromValue("always"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(ChecksumOptions.REQUEST_CHECKSUM_CALCULATION_SETTING.environmentVariable())
            .contains("AWS_REQUEST_CHECKSUM_CALCULATION");
        assertThat(ChecksumOptions.RESPONSE_CHECKSUM_VALIDATION_SETTING.profileProperty())
            .contains("response_checksum_validation");
    }
}
