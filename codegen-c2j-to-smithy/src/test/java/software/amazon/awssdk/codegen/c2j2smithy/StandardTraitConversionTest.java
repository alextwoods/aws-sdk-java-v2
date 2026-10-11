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

package software.amazon.awssdk.codegen.c2j2smithy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.REST_JSON_NS;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.expectOnlyService;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.gapOccurrences;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.marker;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.member;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.restJsonOperation;
import static software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverterTest.restJsonShape;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.traits.HttpChecksumTrait;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.aws.traits.auth.SigV4ATrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientDiscoveredEndpointTrait;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientEndpointDiscoveryIdTrait;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientEndpointDiscoveryTrait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryCompatibleTrait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryNameTrait;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpBearerAuthTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;
import software.amazon.smithy.protocol.traits.Rpcv2CborTrait;
import software.amazon.smithy.rulesengine.traits.ClientContextParamsTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;
import software.amazon.smithy.rulesengine.traits.OperationContextParamsTrait;
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;

/**
 * One test (or more) per row of the design's "Ported converter adds" table (A.14), plus the OD-19 markers. Each C2J
 * property that has a standard trait is emitted as that trait and no longer as a marker.
 */
class StandardTraitConversionTest {
    private static final String JSON10_NS = "com.amazonaws.fixturejson10";

    // ----- protocols (Requirement 12.6) -----

    @Test
    void protocols_multipleEntries_emitOneProtocolTraitEachInC2jOrder() {
        ServiceShape service = expectOnlyService(TestModels.convertFixture("cbor").model());

        List<ShapeId> protocols = new ArrayList<>();
        for (Trait t : service.getAllTraits().values()) {
            if (t instanceof Rpcv2CborTrait || t instanceof AwsJson1_0Trait || t instanceof AwsQueryTrait) {
                protocols.add(t.toShapeId());
            }
        }
        assertThat(protocols).containsExactlyInAnyOrder(Rpcv2CborTrait.ID, AwsJson1_0Trait.ID, AwsQueryTrait.ID);
        assertThat(service.getId().getName()).isEqualTo("FixtureCborService20100801");
    }

    @Test
    void protocols_ec2_emitsEc2QueryTraitAndDerivedXmlNamespace() {
        ServiceShape service = expectOnlyService(TestModels.convertFixture("ec2").model());

        assertThat(service.hasTrait(Ec2QueryTrait.class)).isTrue();
        assertThat(service.hasTrait(XmlNamespaceTrait.class)).isTrue();
    }

    @Test
    void protocols_unmapped_emitsNoProtocolTraitAndRecordsGap() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocols': "
                                                              + "['json', 'smithy-rpc-v3']}}");

        assertThat(expectOnlyService(result.model()).hasTrait(AwsJson1_0Trait.class)).isTrue();
        assertThat(gapOccurrences(result)).containsEntry("metadata.protocols=smithy-rpc-v3", 1);
    }

    @Test
    void protocols_h2EventStreamSetting_configuresHttpVersions() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': "
                                                              + "'rest-json', 'protocolSettings': {'h2': "
                                                              + "'eventstream'}}}");

        Node config = expectOnlyService(result.model())
            .findTrait(ShapeId.from("aws.protocols#restJson1")).get().toNode();
        assertThat(config.expectObjectNode().expectArrayMember("eventStreamHttp").getElementsAs(n -> n.expectStringNode()
                                                                                              .getValue()))
            .containsExactly("h2");
        assertThat(expectOnlyService(result.model()).findTrait(C2jMarkerTrait.METADATA.traitId())).isEmpty();
    }

    @Test
    void targetPrefix_jsonService_namesServiceShape() {
        assertThat(expectOnlyService(TestModels.convertFixture("json10").model()).getId())
            .isEqualTo(ShapeId.from(JSON10_NS + "#FixtureJson"));
    }

    // ----- aws.api#service, @title, leftover metadata -----

    @Test
    void serviceTrait_fromServiceIdEndpointPrefixSigningNameAndUid() {
        ServiceShape service = expectOnlyService(TestModels.convertFixture("restjson").model());

        ServiceTrait trait = service.expectTrait(ServiceTrait.class);
        assertThat(trait.getSdkId()).isEqualTo("Fixture RestJson");
        assertThat(trait.getEndpointPrefix()).isEqualTo("fixture-restjson");
        assertThat(trait.getArnNamespace()).isEqualTo("fixture");
        assertThat(trait.getCloudFormationName()).isEqualTo("FixtureRestJson");
        assertThat(trait.getCloudTrailEventSource()).isEqualTo("fixture.amazonaws.com");
        assertThat(trait.resolveDocId(service)).isEqualTo("fixture-restjson-2024-01-01");
        assertThat(service.expectTrait(TitleTrait.class).getValue()).isEqualTo("Fixture Rest JSON");
    }

    @Test
    void metadataMarker_keepsOnlyPropertiesWithoutStandardTraits() {
        Node restJson = marker(expectOnlyService(TestModels.convertFixture("restjson").model()),
                               C2jMarkerTrait.METADATA);
        Node cbor = marker(expectOnlyService(TestModels.convertFixture("cbor").model()), C2jMarkerTrait.METADATA);

        assertThat(restJson.expectObjectNode().getStringMap().keySet()).containsExactly("serviceAbbreviation");
        assertThat(cbor.expectObjectNode().getStringMap().keySet()).containsExactly("globalEndpoint");
        assertThat(expectOnlyService(TestModels.convertFixture("json10").model())
                       .findTrait(C2jMarkerTrait.METADATA.traitId())).isEmpty();
    }

    // ----- sigv4, sigv4a, @auth -----

    @Test
    void auth_sigv4_namedBySigningNameElseEndpointPrefix() {
        assertThat(expectOnlyService(TestModels.convertFixture("restjson").model()).expectTrait(SigV4Trait.class)
                                                                                    .getName()).isEqualTo("fixture");
        assertThat(expectOnlyService(TestModels.convertFixture("json10").model()).expectTrait(SigV4Trait.class)
                                                                                  .getName()).isEqualTo("fixturejson");
    }

    @Test
    void auth_operationOnlyScheme_isAppliedToServiceWithServiceAuthOrder() {
        ServiceShape service = expectOnlyService(TestModels.convertFixture("restjson").model());

        assertThat(service.hasTrait(HttpBearerAuthTrait.class)).isTrue();
        assertThat(service.expectTrait(AuthTrait.class).getValues()).containsExactly(SigV4Trait.ID);
        assertThat(restJsonOperation("WatchThings").expectTrait(AuthTrait.class).getValues())
            .containsExactly(HttpBearerAuthTrait.ID);
    }

    @Test
    void auth_sigv4a_alsoAppliesSigv4() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json', "
                                                              + "'signingName': 'x', 'auth': ['aws.auth#sigv4a', "
                                                              + "'aws.auth#sigv4']}}");

        ServiceShape service = expectOnlyService(result.model());
        assertThat(service.expectTrait(SigV4ATrait.class).getName()).isEqualTo("x");
        assertThat(service.hasTrait(SigV4Trait.class)).isTrue();
        assertThat(service.expectTrait(AuthTrait.class).getValues()).containsExactly(SigV4ATrait.ID, SigV4Trait.ID);
    }

    @Test
    void auth_perOperation_mapsNoneUnsignedBodyAndBearer() {
        OperationShape put = restJsonOperation("PutThing");
        assertThat(put.hasTrait(UnsignedPayloadTrait.class)).isTrue();
        assertThat(put.hasTrait(AuthTrait.class)).isFalse();
        assertThat(put.findTrait(C2jMarkerTrait.AUTHTYPE.traitId())).isEmpty();

        assertThat(restJsonOperation("ThingC2jOperation").expectTrait(AuthTrait.class).getValues()).isEmpty();
        Model json10 = TestModels.convertFixture("json10").model();
        assertThat(json10.expectShape(ShapeId.from(JSON10_NS + "#Ping")).expectTrait(AuthTrait.class).getValues())
            .isEmpty();
    }

    @Test
    void auth_operationAuthListWithNoAuth_isOptionalAuth() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json', "
                                                              + "'signatureVersion': 'v4'}, 'operations': {'Op': "
                                                              + "{'auth': ['aws.auth#sigv4', 'smithy.api#noAuth']}}}");

        OperationShape op = result.model().expectShape(ShapeId.from("com.amazonaws.x#Op"), OperationShape.class);
        assertThat(op.hasTrait(OptionalAuthTrait.class)).isTrue();
        assertThat(op.hasTrait(AuthTrait.class)).isFalse();
    }

    @Test
    void auth_customAuthtype_keepsMarkerAndRecordsGap() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json', "
                                                              + "'signatureVersion': 'v4'}, 'operations': {'Op': "
                                                              + "{'authtype': 'custom'}}}");

        OperationShape op = result.model().expectShape(ShapeId.from("com.amazonaws.x#Op"), OperationShape.class);
        assertThat(marker(op, C2jMarkerTrait.AUTHTYPE)).isEqualTo(Node.from("custom"));
        assertThat(op.hasTrait(AuthTrait.class)).isFalse();
    }

    // ----- awsQueryCompatible -----

    @Test
    void awsQueryCompatible_onAwsJson10AndRpcv2Cbor() {
        assertThat(expectOnlyService(TestModels.convertFixture("json10").model())
                       .hasTrait(AwsQueryCompatibleTrait.class)).isTrue();
        assertThat(expectOnlyService(TestModels.convertFixture("cbor").model())
                       .hasTrait(AwsQueryCompatibleTrait.class)).isTrue();
    }

    @Test
    void awsQueryCompatible_onOtherProtocol_isDroppedWithGap() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json', "
                                                              + "'jsonVersion': '1.1', 'awsQueryCompatible': {}}}");

        assertThat(expectOnlyService(result.model()).hasTrait(AwsQueryCompatibleTrait.class)).isFalse();
        assertThat(gapOccurrences(result)).containsKey("metadata.awsQueryCompatible(protocol)");
    }

    // ----- @httpError and awsQueryError -----

    @Test
    void errors_mapToErrorHttpErrorAndAwsQueryError() {
        Model restJson = TestModels.convertFixture("restjson").model();
        Shape notFound = restJsonShape(restJson, "ThingNotFound");
        assertThat(notFound.expectTrait(ErrorTrait.class).getValue()).isEqualTo("client");
        assertThat(notFound.expectTrait(HttpErrorTrait.class).getCode()).isEqualTo(404);
        assertThat(notFound.hasTrait(AwsQueryErrorTrait.class)).isFalse();
        assertThat(restJsonShape(restJson, "InternalFailure").expectTrait(ErrorTrait.class).getValue())
            .isEqualTo("server");

        Shape queueMissing = TestModels.convertFixture("json10").model()
                                       .expectShape(ShapeId.from(JSON10_NS + "#QueueMissing"));
        AwsQueryErrorTrait queryError = queueMissing.expectTrait(AwsQueryErrorTrait.class);
        assertThat(queryError.getCode()).isEqualTo("AWS.SimpleQueueService.NonExistentQueue");
        assertThat(queryError.getHttpResponseCode()).isEqualTo(400);
        assertThat(queueMissing.findTrait(ShapeId.from("com.amazonaws.c2j#error"))).isEmpty();
    }

    @Test
    void errors_faultWithoutSenderFault_followsStatusCode() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json'}, "
                                                              + "'shapes': {'Boom': {'type': 'structure', 'members': {},"
                                                              + " 'exception': true, 'error': {'httpStatusCode': "
                                                              + "503}}}}");

        assertThat(result.model().expectShape(ShapeId.from("com.amazonaws.x#Boom")).expectTrait(ErrorTrait.class)
                         .getValue()).isEqualTo("server");
    }

    @Test
    void errors_duplicateQueryErrorCodes_suppressQueryErrorCodeWithGap() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'query'}, "
                                                              + "'operations': {'Op': {'errors': [{'shape': 'A'}, "
                                                              + "{'shape': 'B'}]}}, 'shapes': {"
                                                              + "'A': {'type': 'structure', 'members': {}, 'exception': "
                                                              + "true, 'error': {'code': 'Same'}}, "
                                                              + "'B': {'type': 'structure', 'members': {}, 'exception': "
                                                              + "true, 'error': {'code': 'Same'}}}}");

        assertThat(expectOnlyService(result.model()).expectTrait(SuppressTrait.class).getValues())
            .contains("QueryErrorCode");
        assertThat(gapOccurrences(result)).containsKey("error.code(duplicate)");
    }

    // ----- URI label normalization -----

    @Test
    void uriLabels_renamedToBoundMemberNames() {
        HttpTrait put = restJsonOperation("PutThing").expectTrait(HttpTrait.class);
        assertThat(put.getUri().toString()).isEqualTo("/things/{ThingId}/data");
        MemberShape thingId = member(TestModels.convertFixture("restjson").model(), "PutThingRequest", "ThingId");
        assertThat(marker(thingId, C2jMarkerTrait.LOCATION_NAME)).isEqualTo(Node.from("thing-id"));

        OperationShape changeRecords = TestModels.convertFixture("restxml").model().expectShape(
            ShapeId.from("com.amazonaws.fixturerestxml#ChangeRecords"), OperationShape.class);
        assertThat(changeRecords.expectTrait(HttpTrait.class).getUri().toString())
            .isEqualTo("/2013-04-01/zone/{ZoneId}/rrset/");
    }

    @Test
    void uriLabels_greedyLabelAndQueryLiteral_arePreserved() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'rest-xml'}, "
                                                              + "'operations': {'Get': {'http': {'method': 'GET', "
                                                              + "'requestUri': '/{Bucket}/{Key+}?acl'}, 'input': "
                                                              + "{'shape': 'In'}}}, 'shapes': {'In': {'type': "
                                                              + "'structure', 'required': ['Bucket', 'Key'], "
                                                              + "'members': {'Bucket': {'shape': 'S', 'location': 'uri',"
                                                              + " 'locationName': 'Bucket'}, 'ObjectKey': {'shape': "
                                                              + "'S', 'location': 'uri', 'locationName': 'Key'}}}, "
                                                              + "'S': {'type': 'string'}}}");

        OperationShape op = result.model().expectShape(ShapeId.from("com.amazonaws.x#Get"), OperationShape.class);
        assertThat(op.expectTrait(HttpTrait.class).getUri().toString()).isEqualTo("/{Bucket}/{ObjectKey+}?acl");
    }

    @Test
    void uriLabels_unboundLabel_failsConversion() {
        assertThatThrownBy(() -> TestModels.convertJson("arn", "{'metadata': {'serviceId': 'Arn', 'protocol': "
                                                               + "'rest-json'}, 'operations': {'Tag': {'http': "
                                                               + "{'method': 'POST', 'requestUri': "
                                                               + "'/tags/{resource-arn}'}}}}"))
            .isInstanceOf(C2jConversionException.class)
            .hasMessageContaining("Tag")
            .hasMessageContaining("{resource-arn}");
    }

    // ----- httpChecksum, requestCompression -----

    @Test
    void httpChecksum_mapsToAwsHttpChecksumTrait() {
        HttpChecksumTrait checksum = restJsonOperation("PutThing").expectTrait(HttpChecksumTrait.class);

        assertThat(checksum.getRequestAlgorithmMember()).contains("ChecksumAlgorithm");
        assertThat(checksum.isRequestChecksumRequired()).isTrue();
        assertThat(restJsonOperation("PutThing").findTrait(ShapeId.from("com.amazonaws.c2j#httpChecksum"))).isEmpty();
    }

    @Test
    void requestCompression_mapsToRequestCompressionTrait() {
        assertThat(restJsonOperation("SendMetrics").expectTrait(RequestCompressionTrait.class).getEncodings())
            .containsExactly("gzip");
    }

    // ----- @endpoint and @hostLabel -----

    @Test
    void hostPrefix_mapsToEndpointAndHostLabel() {
        assertThat(restJsonOperation("GetThing").expectTrait(EndpointTrait.class).getHostPrefix().toString())
            .isEqualTo("data-");
        assertThat(restJsonOperation("SendMetrics").expectTrait(EndpointTrait.class).getHostPrefix().toString())
            .isEqualTo("metrics-{AccountId}.");
        assertThat(member(TestModels.convertFixture("restjson").model(), "SendMetricsRequest", "AccountId")
                       .hasTrait(HostLabelTrait.class)).isTrue();
    }

    @Test
    void hostPrefix_labelNotRequired_keepsMarkerWithoutEndpoint() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json'}, "
                                                              + "'operations': {'Op': {'input': {'shape': 'In'}, "
                                                              + "'endpoint': {'hostPrefix': '{Id}.'}}}, 'shapes': "
                                                              + "{'In': {'type': 'structure', 'members': {'Id': "
                                                              + "{'shape': 'S'}}}, 'S': {'type': 'string'}}}");

        OperationShape op = result.model().expectShape(ShapeId.from("com.amazonaws.x#Op"), OperationShape.class);
        assertThat(op.hasTrait(EndpointTrait.class)).isFalse();
        assertThat(marker(op, C2jMarkerTrait.HOST_PREFIX)).isEqualTo(Node.from("{Id}."));
    }

    // ----- rules engine context parameters, endpointRuleSet, endpointTests -----

    @Test
    void contextParams_mapToRulesEngineTraits() {
        Model model = TestModels.convertFixture("restjson").model();

        assertThat(restJsonOperation("GetThing").expectTrait(StaticContextParamsTrait.class).getParameters())
            .containsKey("UseThing");
        assertThat(member(model, "GetThingRequest", "ThingId").expectTrait(ContextParamTrait.class).getName())
            .isEqualTo("ThingId");
        assertThat(expectOnlyService(model).expectTrait(ClientContextParamsTrait.class).getParameters())
            .containsKey("UseThing");
        OperationShape send = TestModels.convertFixture("json10").model()
                                        .expectShape(ShapeId.from(JSON10_NS + "#SendThing"), OperationShape.class);
        assertThat(send.expectTrait(OperationContextParamsTrait.class).getParameters()).containsKey("QueueName");
    }

    @Test
    void endpointRuleSetAndTests_mapToServiceTraits() {
        ServiceShape service = expectOnlyService(TestModels.convertFixture("restjson").model());

        assertThat(service.expectTrait(EndpointRuleSetTrait.class).getRuleSet().expectObjectNode()
                          .expectObjectMember("parameters").getMember("ThingId")).isPresent();
        assertThat(service.expectTrait(EndpointTestsTrait.class).getTestCases()).hasSize(1);
    }

    @Test
    void endpointRuleSet_reviewedAwsBuiltIns_areSuppressed() {
        ConversionResult result = TestModels.convertJson(
            "x", "{'metadata': {'serviceId': 'X', 'protocol': 'json'}}",
            "{'version': '1.0', 'parameters': {'AccountId': {'builtIn': 'AWS::Auth::AccountId', 'required': false, "
            + "'documentation': 'The account.', 'type': 'String'}}, 'rules': []}");

        assertThat(expectOnlyService(result.model()).expectTrait(SuppressTrait.class).getValues())
            .containsExactly("RuleSetAwsBuiltIn.AWS::Auth::AccountId");
    }

    // ----- endpoint discovery -----

    @Test
    void endpointDiscovery_mapsToClientEndpointDiscoveryTraits() {
        Model model = TestModels.convertFixture("json10").model();

        ClientEndpointDiscoveryTrait discovery = expectOnlyService(model).expectTrait(ClientEndpointDiscoveryTrait.class);
        assertThat(discovery.getOperation()).isEqualTo(ShapeId.from(JSON10_NS + "#DescribeEndpoints"));
        assertThat(discovery.getOptionalError()).contains(ShapeId.from(JSON10_NS + "#InvalidEndpointException"));
        OperationShape send = model.expectShape(ShapeId.from(JSON10_NS + "#SendThing"), OperationShape.class);
        assertThat(send.expectTrait(ClientDiscoveredEndpointTrait.class).isRequired()).isFalse();
        assertThat(member(model, JSON10_NS, "SendThingRequest", "QueueName").hasTrait(ClientEndpointDiscoveryIdTrait.class))
            .isTrue();
    }

    @Test
    void endpointDiscovery_withoutInvalidEndpointError_suppressesMissingErrorWithGap() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json'}, "
                                                              + "'operations': {'Describe': {'endpointoperation': true},"
                                                              + " 'Op': {'endpointdiscovery': {}}}}");

        ServiceShape service = expectOnlyService(result.model());
        assertThat(service.expectTrait(ClientEndpointDiscoveryTrait.class).getOptionalError()).isEmpty();
        assertThat(service.expectTrait(SuppressTrait.class).getValues())
            .containsExactly("ClientEndpointDiscovery.MissingErrorDefinition");
        assertThat(gapOccurrences(result)).containsKey("endpointdiscovery.error");
    }

    // ----- event streams -----

    @Test
    void eventStream_becomesStreamingUnionWithoutMarkers() {
        Model model = TestModels.convertFixture("restjson").model();

        Shape events = restJsonShape(model, "ThingEvents");
        assertThat(events.getType()).isEqualTo(ShapeType.UNION);
        assertThat(events.hasTrait(StreamingTrait.class)).isTrue();
        assertThat(events.getMemberNames()).containsExactly("Changed", "Failure");
        assertThat(restJsonShape(model, "ThingChanged").getAllTraits().keySet())
            .noneMatch(id -> id.getNamespace().equals(C2jMarkerTrait.NAMESPACE));
    }

    // ----- ec2QueryName -----

    @Test
    void queryName_mapsToEc2QueryName() {
        MemberShape ids = member(TestModels.convertFixture("ec2").model(), "com.amazonaws.fixtureec2",
                                 "DescribeThingsRequest", "ThingIds");

        assertThat(ids.expectTrait(Ec2QueryNameTrait.class).getValue()).isEqualTo("ThingId");
    }

    // ----- IDL 2 enums -----

    @Test
    void enums_becomeEnumShapesWithSynthesizedMemberNames() {
        Shape state = restJsonShape(TestModels.convertFixture("restjson").model(), "ThingState");

        assertThat(state).isInstanceOf(EnumShape.class);
        assertThat(((EnumShape) state).getEnumValues())
            .containsExactly(entry("ACTIVE", "ACTIVE"), entry("DELETED", "DELETED"), entry("in_progress", "in-progress"),
                             entry("_1st", "1st"), entry("VALUE_4", "*"), entry("Active_2", "Active"));
        assertThat(state.getType()).isEqualTo(ShapeType.ENUM);
    }

    @Test
    void enums_emptyValue_keepsLegacyEnumTraitWithGap() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json'}, "
                                                              + "'shapes': {'E': {'type': 'string', 'enum': ['', "
                                                              + "'a']}}}");

        Shape e = result.model().expectShape(ShapeId.from("com.amazonaws.x#E"));
        assertThat(e.getType()).isEqualTo(ShapeType.STRING);
        assertThat(e.hasTrait(EnumTrait.class)).isTrue();
        assertThat(gapOccurrences(result)).containsKey("shape.enum(empty value)");
    }

    // ----- @clientOptional -----

    @Test
    void clientOptional_onEveryRequiredStructureMember() {
        Model model = TestModels.convertFixture("restjson").model();

        MemberShape thingId = member(model, "PutThingRequest", "ThingId");
        assertThat(thingId.hasTrait(RequiredTrait.class)).isTrue();
        assertThat(thingId.hasTrait(ClientOptionalTrait.class)).isTrue();
        assertThat(member(model, "PutThingRequest", "ChecksumAlgorithm").hasTrait(ClientOptionalTrait.class)).isFalse();
    }

    // ----- @pattern -----

    @Test
    void pattern_mapsToPatternTrait() {
        assertThat(restJsonShape(TestModels.convertFixture("restjson").model(), "ThingName")
                       .expectTrait(PatternTrait.class).getValue()).isEqualTo("^[a-z0-9-]+$");
    }

    @Test
    void pattern_invalidRegex_isDroppedWithGap() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json'}, "
                                                              + "'shapes': {'S': {'type': 'string', 'pattern': "
                                                              + "'[a-'}}}");

        assertThat(result.model().expectShape(ShapeId.from("com.amazonaws.x#S")).hasTrait(PatternTrait.class)).isFalse();
        assertThat(gapOccurrences(result)).containsKey("shape.pattern(invalid)");
    }

    // ----- @mediaType from jsonvalue -----

    @Test
    void jsonValue_exclusiveTarget_getsMediaType() {
        Model model = TestModels.convertFixture("restjson").model();

        assertThat(restJsonShape(model, "PolicyText").expectTrait(MediaTypeTrait.class).getValue())
            .isEqualTo("application/json");
        assertThat(member(model, "GetThingResponse", "Policy").getTarget())
            .isEqualTo(ShapeId.from(REST_JSON_NS + "#PolicyText"));
    }

    @Test
    void jsonValue_sharedTarget_isCopiedForJsonValueMembers() {
        Model model = TestModels.convertFixture("json10").model();

        assertThat(model.expectShape(ShapeId.from(JSON10_NS + "#String")).hasTrait(MediaTypeTrait.class)).isFalse();
        Shape copy = model.expectShape(ShapeId.from(JSON10_NS + "#StringJsonValue"));
        assertThat(copy.expectTrait(MediaTypeTrait.class).getValue()).isEqualTo("application/json");
        assertThat(marker(copy, C2jMarkerTrait.SHAPE_NAME)).isEqualTo(Node.from("String"));
        assertThat(member(model, JSON10_NS, "SendThingRequest", "Attributes").getTarget()).isEqualTo(copy.getId());
        assertThat(member(model, JSON10_NS, "SendThingRequest", "Body").getTarget())
            .isEqualTo(ShapeId.from(JSON10_NS + "#String"));
        assertThat(model.expectShape(ShapeId.from(JSON10_NS + "#PolicyDocument")).hasTrait(MediaTypeTrait.class))
            .isTrue();
    }

    // ----- reserved headers -----

    @Test
    void reservedHeader_isUnboundWithMarker() {
        MemberShape agent = member(TestModels.convertFixture("restjson").model(), "SendMetricsRequest", "Agent");

        assertThat(agent.hasTrait(HttpHeaderTrait.class)).isFalse();
        assertThat(marker(agent, C2jMarkerTrait.RESERVED_HEADER)).isEqualTo(Node.from("User-Agent"));
    }

    // ----- case-insensitive shape name collisions -----

    @Test
    void shapeNamesDifferingInCase_keepStructureOrFirstNameAndRenameOthers() {
        Model model = TestModels.convertFixture("query").model();
        String ns = "com.amazonaws.fixturequery";

        assertThat(model.expectShape(ShapeId.from(ns + "#Endpoint")).getType()).isEqualTo(ShapeType.STRUCTURE);
        Shape endpoint = model.expectShape(ShapeId.from(ns + "#endpointC2jShape"));
        assertThat(marker(endpoint, C2jMarkerTrait.SHAPE_NAME)).isEqualTo(Node.from("endpoint"));
        assertThat(model.getShape(ShapeId.from(ns + "#String"))).isPresent();
        assertThat(marker(model.expectShape(ShapeId.from(ns + "#stringC2jShape")), C2jMarkerTrait.SHAPE_NAME))
            .isEqualTo(Node.from("string"));
        assertThat(member(model, ns, "PublishInput", "Target").getTarget()).isEqualTo(endpoint.getId());
        assertThat(member(model, ns, "Endpoint", "Arn").getTarget()).isEqualTo(endpoint.getId());
        assertThat(member(model, ns, "PublishInput", "Note").getTarget())
            .isEqualTo(ShapeId.from(ns + "#stringC2jShape"));
    }

    // ----- OD-19 markers -----

    @Test
    void resultWrapper_onlyNonstandardKeepsMarker() {
        Model model = TestModels.convertFixture("query").model();
        String ns = "com.amazonaws.fixturequery";

        assertThat(model.expectShape(ShapeId.from(ns + "#Publish")).findTrait(C2jMarkerTrait.RESULT_WRAPPER.traitId()))
            .isEmpty();
        assertThat(marker(model.expectShape(ShapeId.from(ns + "#Subscribe")), C2jMarkerTrait.RESULT_WRAPPER))
            .isEqualTo(Node.from("SubscribeOutcome"));
    }

    @Test
    void od19Properties_keepMarkers() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json'}, "
                                                              + "'authorizers': {'Auth': {'name': 'Auth'}}, "
                                                              + "'operations': {'Op': {'input': {'shape': 'In'}, "
                                                              + "'authorizer': 'Auth'}}, 'shapes': {'In': {'type': "
                                                              + "'structure', 'wrapper': true, 'synthetic': true, "
                                                              + "'members': {'A': {'shape': 'S', 'deprecatedName': "
                                                              + "'Old', 'alternateBeanPropertyName': 'Alt'}}}, 'S': "
                                                              + "{'type': 'string'}}}");
        Model model = result.model();

        Shape in = model.expectShape(ShapeId.from("com.amazonaws.x#In"));
        assertThat(marker(in, C2jMarkerTrait.WRAPPER)).isEqualTo(Node.from(true));
        assertThat(marker(in, C2jMarkerTrait.SYNTHETIC)).isEqualTo(Node.from(true));
        MemberShape a = model.expectShape(ShapeId.from("com.amazonaws.x#In$A"), MemberShape.class);
        assertThat(marker(a, C2jMarkerTrait.DEPRECATED_NAME)).isEqualTo(Node.from("Old"));
        assertThat(marker(a, C2jMarkerTrait.ALTERNATE_BEAN_PROPERTY_NAME)).isEqualTo(Node.from("Alt"));
        assertThat(marker(expectOnlyService(model), C2jMarkerTrait.AUTHORIZERS).expectObjectNode().getMember("Auth"))
            .isPresent();
        assertThat(marker(model.expectShape(ShapeId.from("com.amazonaws.x#Op")), C2jMarkerTrait.AUTHORIZER))
            .isEqualTo(Node.from("Auth"));
        assertThat(gapOccurrences(result)).containsKeys("shape.wrapper", "shape.synthetic", "member.deprecatedName",
                                                        "member.alternateBeanPropertyName", "authorizers",
                                                        "operation.authorizer");
    }

    @Test
    void labelMember_keepsHttpLabelOnRenamedLabel() {
        MemberShape thingId = member(TestModels.convertFixture("restjson").model(), "PutThingRequest", "ThingId");

        assertThat(thingId.hasTrait(HttpLabelTrait.class)).isTrue();
    }
}
