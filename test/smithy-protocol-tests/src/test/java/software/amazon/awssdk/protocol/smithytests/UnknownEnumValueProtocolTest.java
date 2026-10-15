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

package software.amazon.awssdk.protocol.smithytests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sdkrpcv2protocol.SdkRpcV2ProtocolClient;
import software.amazon.awssdk.services.sdkrpcv2protocol.SdkRpcV2ProtocolClientBuilder;
import software.amazon.awssdk.services.sdkrpcv2protocol.model.SdkEnum;
import software.amazon.awssdk.services.sdkrpcv2protocol.model.SdkEnumsResponse;

/**
 * Enum values the model does not define are kept as the raw wire string, in a member, a list element, a map key and a
 * map value (legacy-disposition.csv rows json-core-output.json#5, #7, #9 and #10, retired {@code [moved-to-handwritten]}:
 * Smithy rejects undefined enum values in test params, so these cannot be Smithy cases).
 *
 * <p>Each test is tagged with the protocol it covers, so it runs only when {@code protocolTests.enabledProtocols} lists
 * that protocol and is counted under it. Task 3.34 covers rpcv2Cbor; task 7.21 adds the other Supported_Protocols.
 */
class UnknownEnumValueProtocolTest {
    private static final String RPCV2_CBOR = "smithy.protocols#rpcv2Cbor";
    private static final String RPCV2_CBOR_TAG = "protocol:smithy.protocols#rpcv2Cbor";

    private static final ProtocolTestHarness<SdkRpcV2ProtocolClient> RPCV2_CBOR_HARNESS =
        ProtocolTestHarnesses.<SdkRpcV2ProtocolClient, SdkRpcV2ProtocolClientBuilder>forClient(
            RPCV2_CBOR,
            SdkRpcV2ProtocolClient::builder,
            b -> b.region(Region.US_EAST_1)
                  .credentialsProvider(AnonymousCredentialsProvider.create())
                  .overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.doNotRetry())));

    /**
     * json-core-output.json#5: {@code {"enumMember": "NewValue"}}.
     */
    @Test
    @Tag(RPCV2_CBOR_TAG)
    void rpcv2Cbor_unknownEnumMember_keepsTheWireString() {
        SdkEnumsResponse response = rpcv2CborRespondWith("oWplbnVtTWVtYmVyaE5ld1ZhbHVl");

        assertThat(response.enumMember()).isEqualTo(SdkEnum.UNKNOWN_TO_SDK_VERSION);
        assertThat(response.enumMemberAsString()).isEqualTo("NewValue");
    }

    /**
     * json-core-output.json#7: {@code {"enumList": ["EnumValue1", "NewValue"]}}.
     */
    @Test
    @Tag(RPCV2_CBOR_TAG)
    void rpcv2Cbor_unknownEnumListElement_keepsTheWireString() {
        SdkEnumsResponse response = rpcv2CborRespondWith("oWhlbnVtTGlzdIJqRW51bVZhbHVlMWhOZXdWYWx1ZQ==");

        assertThat(response.enumList()).containsExactly(SdkEnum.ENUM_VALUE1, SdkEnum.UNKNOWN_TO_SDK_VERSION);
        assertThat(response.enumListAsStrings()).containsExactly("EnumValue1", "NewValue");
    }

    /**
     * json-core-output.json#9: {@code {"enumKeyMap": {"NewValue": "v", "EnumValue2": "w"}}}.
     */
    @Test
    @Tag(RPCV2_CBOR_TAG)
    void rpcv2Cbor_unknownEnumMapKey_keepsTheWireString() {
        SdkEnumsResponse response = rpcv2CborRespondWith("oWplbnVtS2V5TWFwomhOZXdWYWx1ZWF2akVudW1WYWx1ZTJhdw==");

        assertThat(response.enumKeyMapAsStrings()).containsExactly(entry("NewValue", "v"), entry("EnumValue2", "w"));
        // The enum-keyed view leaves unknown keys out, as every generated SDK model class does (the map copier skips
        // keys that convert to UNKNOWN_TO_SDK_VERSION, which would otherwise collide).
        assertThat(response.enumKeyMap()).containsExactly(entry(SdkEnum.ENUM_VALUE2, "w"));
    }

    /**
     * json-core-output.json#10: {@code {"enumValueMap": {"k": "NewValue", "l": "EnumValue1"}}}.
     */
    @Test
    @Tag(RPCV2_CBOR_TAG)
    void rpcv2Cbor_unknownEnumMapValue_keepsTheWireString() {
        SdkEnumsResponse response = rpcv2CborRespondWith("oWxlbnVtVmFsdWVNYXCiYWtoTmV3VmFsdWVhbGpFbnVtVmFsdWUx");

        assertThat(response.enumValueMapAsStrings()).containsExactly(entry("k", "NewValue"), entry("l", "EnumValue1"));
        assertThat(response.enumValueMap()).containsExactly(entry("k", SdkEnum.UNKNOWN_TO_SDK_VERSION),
                                                            entry("l", SdkEnum.ENUM_VALUE1));
    }

    private static SdkEnumsResponse rpcv2CborRespondWith(String base64Body) {
        StubbedResponse response = StubbedResponse.fromTestCase(
            200,
            ProtocolTestValues.<String>mapOf("Content-Type", "application/cbor", "smithy-protocol", "rpc-v2-cbor"),
            base64Body,
            "application/cbor");
        return RPCV2_CBOR_HARNESS.respondWith(response, c -> c.sdkEnums(r -> { }));
    }
}
