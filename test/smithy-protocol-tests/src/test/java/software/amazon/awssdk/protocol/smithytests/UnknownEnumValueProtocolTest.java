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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sdkec2protocol.SdkEc2ProtocolClient;
import software.amazon.awssdk.services.sdkjsonprotocol.SdkJsonProtocolClient;
import software.amazon.awssdk.services.sdkjsonrpc10.SdkJsonRpc10Client;
import software.amazon.awssdk.services.sdkqueryprotocol.SdkQueryProtocolClient;
import software.amazon.awssdk.services.sdkrestjsonprotocol.SdkRestJsonProtocolClient;
import software.amazon.awssdk.services.sdkrestxmlprotocol.SdkRestXmlProtocolClient;
import software.amazon.awssdk.services.sdkrpcv2protocol.SdkRpcV2ProtocolClient;

/**
 * Enum values the model does not define are kept as the raw wire string, in a member, a list element, a map key and a
 * map value (legacy-disposition.csv rows json-core-output.json#5, #7, #9 and #10, retired
 * {@code [moved-to-handwritten]}: Smithy rejects undefined enum values in test params, so these cannot be Smithy
 * cases).
 *
 * <p>Each nested class covers one Supported_Protocol through an SDK-owned client and is tagged with that protocol, so
 * its tests run only when {@code protocolTests.enabledProtocols} lists the protocol and are counted under it. Task 3.34
 * covers rpcv2Cbor; task 7.21 adds the other Supported_Protocols.
 */
class UnknownEnumValueProtocolTest {
    private static final String NAMESPACE = "https://example.com/";

    /** The legacy cases, each an unknown value {@code NewValue} next to a known one. */
    enum Case {
        /** json-core-output.json#5: {@code {"enumMember": "NewValue"}}. */
        MEMBER,
        /** json-core-output.json#7: {@code {"enumList": ["EnumValue1", "NewValue"]}}. */
        LIST,
        /** json-core-output.json#9: {@code {"enumKeyMap": {"NewValue": "v", "EnumValue2": "w"}}}. */
        KEY_MAP,
        /** json-core-output.json#10: {@code {"enumValueMap": {"k": "NewValue", "l": "EnumValue1"}}}. */
        VALUE_MAP
    }

    private static final Map<Case, String> JSON_BODIES = new EnumMap<>(Case.class);
    private static final Map<Case, String> CBOR_BODIES = new EnumMap<>(Case.class);
    private static final Map<Case, String> XML_MEMBERS = new EnumMap<>(Case.class);

    static {
        JSON_BODIES.put(Case.MEMBER, "{\"enumMember\": \"NewValue\"}");
        JSON_BODIES.put(Case.LIST, "{\"enumList\": [\"EnumValue1\", \"NewValue\"]}");
        JSON_BODIES.put(Case.KEY_MAP, "{\"enumKeyMap\": {\"NewValue\": \"v\", \"EnumValue2\": \"w\"}}");
        JSON_BODIES.put(Case.VALUE_MAP, "{\"enumValueMap\": {\"k\": \"NewValue\", \"l\": \"EnumValue1\"}}");

        // The JSON bodies above, CBOR-encoded and base64-encoded.
        CBOR_BODIES.put(Case.MEMBER, "oWplbnVtTWVtYmVyaE5ld1ZhbHVl");
        CBOR_BODIES.put(Case.LIST, "oWhlbnVtTGlzdIJqRW51bVZhbHVlMWhOZXdWYWx1ZQ==");
        CBOR_BODIES.put(Case.KEY_MAP, "oWplbnVtS2V5TWFwomhOZXdWYWx1ZWF2akVudW1WYWx1ZTJhdw==");
        CBOR_BODIES.put(Case.VALUE_MAP, "oWxlbnVtVmFsdWVNYXCiYWtoTmV3VmFsdWVhbGpFbnVtVmFsdWUx");

        XML_MEMBERS.put(Case.MEMBER, "<enumMember>NewValue</enumMember>");
        XML_MEMBERS.put(Case.LIST, "<enumList><member>EnumValue1</member><member>NewValue</member></enumList>");
        XML_MEMBERS.put(Case.KEY_MAP, "<enumKeyMap>"
                                      + "<entry><key>NewValue</key><value>v</value></entry>"
                                      + "<entry><key>EnumValue2</key><value>w</value></entry>"
                                      + "</enumKeyMap>");
        XML_MEMBERS.put(Case.VALUE_MAP, "<enumValueMap>"
                                        + "<entry><key>k</key><value>NewValue</value></entry>"
                                        + "<entry><key>l</key><value>EnumValue1</value></entry>"
                                        + "</enumValueMap>");
    }

    /**
     * The four tests, run once for each protocol by the nested classes.
     */
    abstract static class UnknownEnumCases {
        /**
         * @param testCase a legacy case
         * @return the response of the protocol's SDK-owned {@code SdkEnums} operation for the case's body
         */
        abstract EnumValues respondWith(Case testCase);

        @Test
        void unknownEnumMember_keepsTheWireString() {
            EnumValues response = respondWith(Case.MEMBER);
            assertThat(response.member).isEqualTo("UNKNOWN_TO_SDK_VERSION");
            assertThat(response.memberAsString).isEqualTo("NewValue");
        }

        @Test
        void unknownEnumListElement_keepsTheWireString() {
            EnumValues response = respondWith(Case.LIST);
            assertThat(response.list).containsExactly("ENUM_VALUE1", "UNKNOWN_TO_SDK_VERSION");
            assertThat(response.listAsStrings).containsExactly("EnumValue1", "NewValue");
        }

        @Test
        void unknownEnumMapKey_keepsTheWireString() {
            EnumValues response = respondWith(Case.KEY_MAP);
            assertThat(response.keyMapAsStrings).containsExactly(entry("NewValue", "v"), entry("EnumValue2", "w"));
            // The enum-keyed view leaves unknown keys out, as every generated SDK model class does (the map copier
            // skips keys that convert to UNKNOWN_TO_SDK_VERSION, which would otherwise collide).
            assertThat(response.keyMap).containsExactly(entry("ENUM_VALUE2", "w"));
        }

        @Test
        void unknownEnumMapValue_keepsTheWireString() {
            EnumValues response = respondWith(Case.VALUE_MAP);
            assertThat(response.valueMapAsStrings).containsExactly(entry("k", "NewValue"), entry("l", "EnumValue1"));
            assertThat(response.valueMap).containsExactly(entry("k", "UNKNOWN_TO_SDK_VERSION"),
                                                          entry("l", "ENUM_VALUE1"));
        }
    }

    @Nested
    @Tag("protocol:smithy.protocols#rpcv2Cbor")
    class RpcV2Cbor extends UnknownEnumCases {
        private final ProtocolTestHarness<SdkRpcV2ProtocolClient> harness =
            harness("smithy.protocols#rpcv2Cbor", SdkRpcV2ProtocolClient::builder);

        @Override
        EnumValues respondWith(Case testCase) {
            StubbedResponse response = StubbedResponse.fromTestCase(
                200,
                ProtocolTestValues.<String>mapOf("Content-Type", "application/cbor", "smithy-protocol", "rpc-v2-cbor"),
                CBOR_BODIES.get(testCase),
                "application/cbor");
            return values(harness.respondWith(response, c -> c.sdkEnums(r -> { })),
                          r -> new EnumValues(r.enumMember(), r.enumMemberAsString(),
                                              r.enumList(), r.enumListAsStrings(),
                                              r.enumKeyMap(), r.enumKeyMapAsStrings(),
                                              r.enumValueMap(), r.enumValueMapAsStrings()));
        }
    }

    @Nested
    @Tag("protocol:aws.protocols#awsJson1_0")
    class AwsJson10 extends UnknownEnumCases {
        private final ProtocolTestHarness<SdkJsonRpc10Client> harness =
            harness("aws.protocols#awsJson1_0", SdkJsonRpc10Client::builder);

        @Override
        EnumValues respondWith(Case testCase) {
            StubbedResponse response = response("application/x-amz-json-1.0", JSON_BODIES.get(testCase));
            return values(harness.respondWith(response, c -> c.sdkEnums(r -> { })),
                          r -> new EnumValues(r.enumMember(), r.enumMemberAsString(),
                                              r.enumList(), r.enumListAsStrings(),
                                              r.enumKeyMap(), r.enumKeyMapAsStrings(),
                                              r.enumValueMap(), r.enumValueMapAsStrings()));
        }
    }

    @Nested
    @Tag("protocol:aws.protocols#awsJson1_1")
    class AwsJson11 extends UnknownEnumCases {
        private final ProtocolTestHarness<SdkJsonProtocolClient> harness =
            harness("aws.protocols#awsJson1_1", SdkJsonProtocolClient::builder);

        @Override
        EnumValues respondWith(Case testCase) {
            StubbedResponse response = response("application/x-amz-json-1.1", JSON_BODIES.get(testCase));
            return values(harness.respondWith(response, c -> c.sdkEnums(r -> { })),
                          r -> new EnumValues(r.enumMember(), r.enumMemberAsString(),
                                              r.enumList(), r.enumListAsStrings(),
                                              r.enumKeyMap(), r.enumKeyMapAsStrings(),
                                              r.enumValueMap(), r.enumValueMapAsStrings()));
        }
    }

    @Nested
    @Tag("protocol:aws.protocols#restJson1")
    class RestJson1 extends UnknownEnumCases {
        private final ProtocolTestHarness<SdkRestJsonProtocolClient> harness =
            harness("aws.protocols#restJson1", SdkRestJsonProtocolClient::builder);

        @Override
        EnumValues respondWith(Case testCase) {
            StubbedResponse response = response("application/json", JSON_BODIES.get(testCase));
            return values(harness.respondWith(response, c -> c.sdkEnums(r -> { })),
                          r -> new EnumValues(r.enumMember(), r.enumMemberAsString(),
                                              r.enumList(), r.enumListAsStrings(),
                                              r.enumKeyMap(), r.enumKeyMapAsStrings(),
                                              r.enumValueMap(), r.enumValueMapAsStrings()));
        }
    }

    @Nested
    @Tag("protocol:aws.protocols#restXml")
    class RestXml extends UnknownEnumCases {
        private final ProtocolTestHarness<SdkRestXmlProtocolClient> harness =
            harness("aws.protocols#restXml", SdkRestXmlProtocolClient::builder);

        @Override
        EnumValues respondWith(Case testCase) {
            StubbedResponse response = response("application/xml",
                                                "<SdkEnumsOutput>" + XML_MEMBERS.get(testCase) + "</SdkEnumsOutput>");
            return values(harness.respondWith(response, c -> c.sdkEnums(r -> { })),
                          r -> new EnumValues(r.enumMember(), r.enumMemberAsString(),
                                              r.enumList(), r.enumListAsStrings(),
                                              r.enumKeyMap(), r.enumKeyMapAsStrings(),
                                              r.enumValueMap(), r.enumValueMapAsStrings()));
        }
    }

    @Nested
    @Tag("protocol:aws.protocols#awsQuery")
    class AwsQuery extends UnknownEnumCases {
        private final ProtocolTestHarness<SdkQueryProtocolClient> harness =
            harness("aws.protocols#awsQuery", SdkQueryProtocolClient::builder);

        @Override
        EnumValues respondWith(Case testCase) {
            StubbedResponse response = response("text/xml",
                                                "<SdkEnumsResponse xmlns=\"" + NAMESPACE + "\"><SdkEnumsResult>"
                                                + XML_MEMBERS.get(testCase)
                                                + "</SdkEnumsResult></SdkEnumsResponse>");
            return values(harness.respondWith(response, c -> c.sdkEnums(r -> { })),
                          r -> new EnumValues(r.enumMember(), r.enumMemberAsString(),
                                              r.enumList(), r.enumListAsStrings(),
                                              r.enumKeyMap(), r.enumKeyMapAsStrings(),
                                              r.enumValueMap(), r.enumValueMapAsStrings()));
        }
    }

    @Nested
    @Tag("protocol:aws.protocols#ec2Query")
    class Ec2Query extends UnknownEnumCases {
        private final ProtocolTestHarness<SdkEc2ProtocolClient> harness =
            harness("aws.protocols#ec2Query", SdkEc2ProtocolClient::builder);

        @Override
        EnumValues respondWith(Case testCase) {
            StubbedResponse response = response("text/xml",
                                                "<SdkEnumsResponse xmlns=\"" + NAMESPACE + "\">"
                                                + XML_MEMBERS.get(testCase)
                                                + "<requestId>requestid</requestId></SdkEnumsResponse>");
            return values(harness.respondWith(response, c -> c.sdkEnums(r -> { })),
                          r -> new EnumValues(r.enumMember(), r.enumMemberAsString(),
                                              r.enumList(), r.enumListAsStrings(),
                                              r.enumKeyMap(), r.enumKeyMapAsStrings(),
                                              r.enumValueMap(), r.enumValueMapAsStrings()));
        }
    }

    private static <C, B extends AwsClientBuilder<B, C>> ProtocolTestHarness<C> harness(String protocol,
                                                                                      Supplier<B> clientBuilder) {
        return ProtocolTestHarnesses.forClient(
            protocol,
            clientBuilder,
            b -> b.region(Region.US_EAST_1)
                  .credentialsProvider(AnonymousCredentialsProvider.create())
                  .overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.doNotRetry())));
    }

    private static StubbedResponse response(String contentType, String body) {
        return StubbedResponse.fromTestCase(200, ProtocolTestValues.<String>mapOf("Content-Type", contentType), body,
                                            contentType);
    }

    private static <T> EnumValues values(T response, Function<T, EnumValues> views) {
        return views.apply(response);
    }

    /**
     * The enum and string views of an {@code SdkEnums} response, with enum constants by name, so one assertion serves
     * the enum classes of every protocol's client.
     */
    static final class EnumValues {
        private final String member;
        private final String memberAsString;
        private final List<String> list;
        private final List<String> listAsStrings;
        private final Map<String, String> keyMap;
        private final Map<String, String> keyMapAsStrings;
        private final Map<String, String> valueMap;
        private final Map<String, String> valueMapAsStrings;

        EnumValues(Enum<?> member, String memberAsString, List<? extends Enum<?>> list, List<String> listAsStrings,
                   Map<? extends Enum<?>, String> keyMap, Map<String, String> keyMapAsStrings,
                   Map<String, ? extends Enum<?>> valueMap, Map<String, String> valueMapAsStrings) {
            this.member = member == null ? null : member.name();
            this.memberAsString = memberAsString;
            this.list = new ArrayList<>();
            list.forEach(e -> this.list.add(e.name()));
            this.listAsStrings = listAsStrings;
            this.keyMap = new LinkedHashMap<>();
            keyMap.forEach((k, v) -> this.keyMap.put(k.name(), v));
            this.keyMapAsStrings = keyMapAsStrings;
            this.valueMap = new LinkedHashMap<>();
            valueMap.forEach((k, v) -> this.valueMap.put(k, v.name()));
            this.valueMapAsStrings = valueMapAsStrings;
        }
    }
}
