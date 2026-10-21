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

package software.amazon.awssdk.dynamicclient.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.dynamicclient.schema.SchemaConverter;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson11Protocol;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson1Protocol;
import software.amazon.awssdk.protocols.client.awsquery.AwsQueryClientProtocol;
import software.amazon.awssdk.protocols.client.awsquery.Ec2QueryClientProtocol;
import software.amazon.awssdk.protocols.client.restjson.RestJsonClientProtocol;
import software.amazon.awssdk.protocols.client.restxml.RestXmlClientProtocol;
import software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

class DynamicProtocolSelectionTest {
    private static final MessageExchange<Object, Object> OTHER_EXCHANGE = new MessageExchange<Object, Object>() {
    };

    static Stream<Arguments> supportedProtocols() {
        return Stream.of(Arguments.of("smithy.protocols#rpcv2Cbor", RpcV2CborProtocol.class),
                         Arguments.of("aws.protocols#awsJson1_0", AwsJson1Protocol.class),
                         Arguments.of("aws.protocols#awsJson1_1", AwsJson11Protocol.class),
                         Arguments.of("aws.protocols#restJson1", RestJsonClientProtocol.class),
                         Arguments.of("aws.protocols#restXml", RestXmlClientProtocol.class),
                         Arguments.of("aws.protocols#awsQuery", AwsQueryClientProtocol.class),
                         Arguments.of("aws.protocols#ec2Query", Ec2QueryClientProtocol.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("supportedProtocols")
    void select_withOneProtocolTrait_createsThatProtocol(String trait, Class<?> protocolClass) {
        Model model = serviceWith(trait);

        ClientProtocol<?, ?> protocol = select(model, HttpMessageExchange.INSTANCE);

        assertThat(protocol).isInstanceOf(protocolClass);
        assertThat(protocol.id().toString()).isEqualTo(trait);
    }

    @Test
    void select_withSeveralProtocolTraits_usesThePriorityListWhateverTheTraitOrder() {
        Model cloudWatchOrder = serviceWith("smithy.protocols#rpcv2Cbor", "aws.protocols#awsJson1_0",
                                            "aws.protocols#awsQuery");
        Model reversed = serviceWith("aws.protocols#awsQuery", "aws.protocols#awsJson1_0",
                                     "smithy.protocols#rpcv2Cbor");
        Model restFirst = serviceWith("aws.protocols#restXml", "aws.protocols#awsJson1_1");
        Model restLast = serviceWith("aws.protocols#awsJson1_1", "aws.protocols#restXml");

        assertThat(select(cloudWatchOrder, HttpMessageExchange.INSTANCE)).isInstanceOf(RpcV2CborProtocol.class);
        assertThat(select(reversed, HttpMessageExchange.INSTANCE)).isInstanceOf(RpcV2CborProtocol.class);
        assertThat(select(restFirst, HttpMessageExchange.INSTANCE)).isInstanceOf(AwsJson11Protocol.class);
        assertThat(select(restLast, HttpMessageExchange.INSTANCE)).isInstanceOf(AwsJson11Protocol.class);
        assertThat(DynamicProtocolSelection.forService(reversed, InternalTestModels.onlyService(reversed)).candidates())
            .containsExactly(RpcV2CborProtocol.Factory.INSTANCE, AwsJson1Protocol.Factory.INSTANCE,
                             AwsQueryClientProtocol.Factory.INSTANCE);
    }

    @Test
    void select_whenNoCandidateMatchesTheTransport_returnsTheFirstCandidateForTheMismatchCheck() {
        Model model = serviceWith("aws.protocols#restJson1", "aws.protocols#awsJson1_0");

        ClientProtocol<?, ?> protocol = select(model, OTHER_EXCHANGE);

        assertThat(protocol).isInstanceOf(AwsJson1Protocol.class);
    }

    @Test
    void noSupportedProtocol_withoutProtocolTraits_saysNoneWereFound() {
        Model model = serviceWith();
        DynamicProtocolSelection selection = DynamicProtocolSelection.forService(model, InternalTestModels.onlyService(model));

        assertThat(selection.candidates()).isEmpty();
        assertThat(selection.select(settings(model), HttpMessageExchange.INSTANCE)).isNull();
        assertThat(selection.noSupportedProtocol())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Service test.protocols#Service declares no protocol that the dynamic client supports. Protocol "
                        + "traits on the service: none found. Supported protocols: [smithy.protocols#rpcv2Cbor, "
                        + "aws.protocols#awsJson1_0, aws.protocols#awsJson1_1, aws.protocols#restJson1, "
                        + "aws.protocols#restXml, aws.protocols#awsQuery, aws.protocols#ec2Query]. Set a protocol with "
                        + "DynamicClient.Builder.protocol(...).");
    }

    @Test
    void noSupportedProtocol_withUnsupportedProtocolTraits_listsThem() {
        Model model = InternalTestModels.assembleIdl(
            "$version: \"2.0\"\nnamespace test.protocols\n"
            + "@protocolDefinition\n@trait(selector: \"service\")\nstructure zProtocol {}\n"
            + "@protocolDefinition\n@trait(selector: \"service\")\nstructure aProtocol {}\n"
            + "@zProtocol\n@aProtocol\nservice Service { version: \"2024-01-01\" }\n");
        DynamicProtocolSelection selection = DynamicProtocolSelection.forService(model, InternalTestModels.onlyService(model));

        assertThat(selection.candidates()).isEmpty();
        assertThat(selection.noSupportedProtocol())
            .hasMessageContaining("Protocol traits on the service: [test.protocols#aProtocol, test.protocols#zProtocol].");
    }

    @Test
    void priority_isTheSdkProtocolPriorityList() {
        assertThat(DynamicProtocolSelection.priority())
            .containsExactly(ShapeId.from("smithy.protocols#rpcv2Cbor"), ShapeId.from("aws.protocols#awsJson1_0"),
                             ShapeId.from("aws.protocols#awsJson1_1"), ShapeId.from("aws.protocols#restJson1"),
                             ShapeId.from("aws.protocols#restXml"), ShapeId.from("aws.protocols#awsQuery"),
                             ShapeId.from("aws.protocols#ec2Query"));
    }

    private static ClientProtocol<?, ?> select(Model model, MessageExchange<?, ?> exchange) {
        DynamicProtocolSelection selection = DynamicProtocolSelection.forService(model, InternalTestModels.onlyService(model));
        return selection.select(settings(model), exchange);
    }

    private static ProtocolSettings settings(Model model) {
        ServiceShape service = InternalTestModels.onlyService(model);
        return ProtocolSettings.builder()
                               .serviceSchema(new SchemaConverter(model, service).getSchema(service))
                               .serviceVersion(service.getVersion())
                               .baseExceptionBuilder(AwsServiceException::builder)
                               .build();
    }

    /**
     * A service with the protocol traits in the given order.
     */
    private static Model serviceWith(String... protocolTraits) {
        StringBuilder idl = new StringBuilder("$version: \"2.0\"\nnamespace test.protocols\n");
        Arrays.stream(protocolTraits).forEach(trait -> idl.append('@').append(trait).append('\n'));
        // awsQuery and ec2Query may only be applied to a service with an XML namespace.
        idl.append("@xmlNamespace(uri: \"https://example.com/doc/2024-01-01/\")\n");
        idl.append("service Service { version: \"2024-01-01\" }\n");
        return InternalTestModels.assembleIdl(idl.toString());
    }
}
