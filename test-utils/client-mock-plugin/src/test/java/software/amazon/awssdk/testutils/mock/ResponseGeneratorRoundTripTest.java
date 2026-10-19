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

package software.amazon.awssdk.testutils.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson11Protocol;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson1Protocol;
import software.amazon.awssdk.protocols.client.awsquery.AwsQueryClientProtocol;
import software.amazon.awssdk.protocols.client.awsquery.Ec2QueryClientProtocol;
import software.amazon.awssdk.protocols.client.restjson.RestJsonClientProtocol;
import software.amazon.awssdk.protocols.client.restxml.RestXmlClientProtocol;
import software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.services.account.model.AccountException;
import software.amazon.awssdk.services.account.model.AccountOperations;
import software.amazon.awssdk.services.account.model.AlternateContact;
import software.amazon.awssdk.services.account.model.AlternateContactType;
import software.amazon.awssdk.services.account.model.GetAlternateContactResponse;
import software.amazon.awssdk.services.account.model.InternalServerException;
import software.amazon.awssdk.services.account.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchOperations;
import software.amazon.awssdk.services.cloudwatch.model.DashboardNotFoundErrorException;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.InternalServiceException;
import software.amazon.awssdk.services.cloudwatch.model.KmsKeyNotFoundException;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.route53.model.ChangeInfo;
import software.amazon.awssdk.services.route53.model.ChangeStatus;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.DelegationSet;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.HostedZoneConfig;
import software.amazon.awssdk.services.route53.model.NoSuchHostedZoneException;
import software.amazon.awssdk.services.route53.model.Route53Exception;
import software.amazon.awssdk.services.route53.model.Route53Operations;

/**
 * Generates responses with each {@link ResponseGenerator} and reads them back with the matching client protocol, using
 * the generated classes of CloudWatch (awsJson1_0, awsQuery, rpcv2Cbor, awsQueryCompatible; also driven through
 * awsJson1_1 and ec2Query), Route 53 (restXml) and Account (restJson1).
 */
class ResponseGeneratorRoundTripTest {
    private static final Instant TIME = Instant.ofEpochSecond(1_700_000_000L);
    private static final ProtocolSettings CLOUDWATCH = ProtocolSettings.builder()
                                                                       .service(CloudWatchOperations.SERVICE.schema().id())
                                                                       .serviceSchema(CloudWatchOperations.SERVICE.schema())
                                                                       .serviceVersion("2010-08-01")
                                                                       .baseExceptionBuilder(CloudWatchException::builder)
                                                                       .build();
    private static final ProtocolSettings ROUTE53 = ProtocolSettings.builder()
                                                                    .service(Route53Operations.SERVICE.schema().id())
                                                                    .serviceSchema(Route53Operations.SERVICE.schema())
                                                                    .serviceVersion("2013-04-01")
                                                                    .baseExceptionBuilder(Route53Exception::builder)
                                                                    .build();
    private static final ProtocolSettings ACCOUNT = ProtocolSettings.builder()
                                                                    .service(AccountOperations.SERVICE.schema().id())
                                                                    .serviceSchema(AccountOperations.SERVICE.schema())
                                                                    .serviceVersion("2021-02-01")
                                                                    .baseExceptionBuilder(AccountException::builder)
                                                                    .build();

    static Stream<ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse>> cloudWatchProtocols() {
        return Stream.of(new AwsJson1Protocol(CLOUDWATCH), new AwsJson11Protocol(CLOUDWATCH),
                         new RpcV2CborProtocol(CLOUDWATCH), new AwsQueryClientProtocol(CLOUDWATCH),
                         new Ec2QueryClientProtocol(CLOUDWATCH));
    }

    static Stream<Arguments> cloudWatchErrors() {
        return cloudWatchProtocols().flatMap(protocol -> Stream.of(
            // httpError 404, awsQueryError code ResourceNotFound, a modeled "message" member.
            Arguments.of(protocol, DashboardNotFoundErrorException.builder().message("no dashboard").requestId("r-1")
                                                                  .build(), 404),
            // httpError 500 on a server fault, modeled "Message" member.
            Arguments.of(protocol, InternalServiceException.builder().message("boom").build(), 500),
            // No httpError: the client fault default.
            Arguments.of(protocol, KmsKeyNotFoundException.builder().message("no key").requestId("r-2").build(), 400)));
    }

    @Test
    void forProtocol_whenEachSupportedProtocol_returnsItsGenerator() {
        Stream.of("aws.protocols#restJson1", "aws.protocols#restXml", "aws.protocols#awsJson1_0",
                  "aws.protocols#awsJson1_1", "smithy.protocols#rpcv2Cbor", "aws.protocols#awsQuery",
                  "aws.protocols#ec2Query")
              .map(ShapeId::from)
              .forEach(id -> assertThat(ResponseGenerator.forProtocol(id).protocolId()).isEqualTo(id));
    }

    @Test
    void forProtocol_whenUnknownProtocol_throwsNamingIt() {
        assertThatThrownBy(() -> ResponseGenerator.forProtocol(ShapeId.from("example#unknown")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("example#unknown");
    }

    @Test
    void generateError_whenNotAnError_throws() {
        ResponseGenerator generator = ResponseGenerator.forProtocol(AwsJson1Protocol.Factory.INSTANCE.id());
        assertThatThrownBy(() -> generator.generateError(CloudWatchOperations.DESCRIBE_ALARMS, describeAlarms()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has no error trait");
    }

    @ParameterizedTest
    @MethodSource("cloudWatchProtocols")
    void generateOutput_whenNestedListsAndTimestamps_roundTrips(
        ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol) {
        DescribeAlarmsResponse output = describeAlarms();

        SdkHttpFullResponse response = generator(protocol).generateOutput(CloudWatchOperations.DESCRIBE_ALARMS, output);

        assertThat(response.statusCode()).isEqualTo(200);
        assertSameFields(output, read(protocol, CloudWatchOperations.DESCRIBE_ALARMS, response));
    }

    @ParameterizedTest
    @MethodSource("cloudWatchErrors")
    void generateError_whenCloudWatchError_roundTripsTypeMembersAndStatus(
        ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol, AwsServiceException error, int status) {
        SdkHttpFullResponse response = generator(protocol).generateError(CloudWatchOperations.GET_DASHBOARD,
                                                                         (SerializableStruct) error);

        assertThat(response.statusCode()).isEqualTo(status);
        assertRoundTrip(error, readError(protocol, CloudWatchOperations.GET_DASHBOARD, response), status);
    }

    @Test
    void generateError_whenAwsQueryCompatible_addsQueryErrorHeader() {
        ResponseGenerator generator = ResponseGenerator.forProtocol(AwsJson1Protocol.Factory.INSTANCE.id());

        SdkHttpFullResponse notFound = generator.generateError(
            CloudWatchOperations.GET_DASHBOARD, DashboardNotFoundErrorException.builder().message("m").build());
        SdkHttpFullResponse fault = generator.generateError(
            CloudWatchOperations.GET_DASHBOARD, InternalServiceException.builder().message("m").build());

        assertThat(notFound.firstMatchingHeader("x-amzn-query-error")).hasValue("ResourceNotFound;Sender");
        assertThat(notFound.firstMatchingHeader("X-Amzn-Errortype"))
            .hasValue("com.amazonaws.cloudwatch#DashboardNotFoundError");
        assertThat(fault.firstMatchingHeader("x-amzn-query-error")).hasValue("InternalServiceError;Receiver");
        assertThat(readError(new AwsJson1Protocol(CLOUDWATCH), CloudWatchOperations.GET_DASHBOARD, notFound)
                       .awsErrorDetails().errorCode()).isEqualTo("ResourceNotFound");
    }

    @Test
    void generateOutput_whenRestXmlWithHeaderAndNestedBody_roundTrips() {
        CreateHostedZoneResponse output =
            CreateHostedZoneResponse.builder()
                                    .location("https://route53.amazonaws.com/2013-04-01/hostedzone/Z1")
                                    .hostedZone(HostedZone.builder().id("/hostedzone/Z1").name("example.com.")
                                                          .callerReference("ref")
                                                          .config(HostedZoneConfig.builder().comment("c")
                                                                                  .privateZone(false).build())
                                                          .resourceRecordSetCount(2L).build())
                                    .changeInfo(ChangeInfo.builder().id("C1").status(ChangeStatus.PENDING)
                                                          .submittedAt(TIME).build())
                                    .delegationSet(DelegationSet.builder().nameServers("ns-1", "ns-2").build())
                                    .build();
        RestXmlClientProtocol protocol = new RestXmlClientProtocol(ROUTE53);

        SdkHttpFullResponse response = generator(protocol).generateOutput(Route53Operations.CREATE_HOSTED_ZONE, output);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.firstMatchingHeader("Location")).hasValue(output.location());
        assertSameFields(output, read(protocol, Route53Operations.CREATE_HOSTED_ZONE, response));
    }

    @Test
    void generateError_whenRestXmlError_roundTripsInErrorResponseWrapper() {
        RestXmlClientProtocol protocol = new RestXmlClientProtocol(ROUTE53);
        NoSuchHostedZoneException error = NoSuchHostedZoneException.builder().message("no zone").requestId("r-3")
                                                                   .build();

        SdkHttpFullResponse response = generator(protocol).generateError(Route53Operations.CREATE_HOSTED_ZONE, error);

        assertThat(response.statusCode()).isEqualTo(404);
        assertRoundTrip(error, readError(protocol, Route53Operations.CREATE_HOSTED_ZONE, response), 404);
    }

    @Test
    void generateOutput_whenRestJsonNestedBody_roundTrips() {
        GetAlternateContactResponse output =
            GetAlternateContactResponse.builder()
                                       .alternateContact(AlternateContact.builder().name("n").title("t")
                                                                         .emailAddress("e@example.com")
                                                                         .phoneNumber("+1 555")
                                                                         .alternateContactType(AlternateContactType.BILLING)
                                                                         .build())
                                       .build();
        RestJsonClientProtocol protocol = new RestJsonClientProtocol(ACCOUNT);

        SdkHttpFullResponse response = generator(protocol).generateOutput(AccountOperations.GET_ALTERNATE_CONTACT, output);

        assertThat(response.statusCode()).isEqualTo(200);
        assertSameFields(output, read(protocol, AccountOperations.GET_ALTERNATE_CONTACT, response));
    }

    static Stream<Arguments> accountErrors() {
        return Stream.of(Arguments.of(ResourceNotFoundException.builder().message("gone").requestId("r-4")
                                                               .errorType("com.amazonaws.account#ResourceNotFoundException")
                                                               .build(), 404),
                         Arguments.of(InternalServerException.builder().message("oops")
                                                             .errorType("com.amazonaws.account#InternalServerException")
                                                             .build(), 500));
    }

    @ParameterizedTest
    @MethodSource("accountErrors")
    void generateError_whenRestJsonError_roundTripsWithErrorTypeHeader(AwsServiceException error, int status) {
        RestJsonClientProtocol protocol = new RestJsonClientProtocol(ACCOUNT);

        SdkHttpFullResponse response = generator(protocol).generateError(AccountOperations.GET_ALTERNATE_CONTACT,
                                                                         (SerializableStruct) error);

        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.firstMatchingHeader("X-Amzn-Errortype")).hasValue(((SerializableStruct) error).sdkSchema()
                                                                                                         .id().toString());
        assertRoundTrip(error, readError(protocol, AccountOperations.GET_ALTERNATE_CONTACT, response), status);
    }

    private static DescribeAlarmsResponse describeAlarms() {
        MetricAlarm alarm = MetricAlarm.builder()
                                       .alarmName("high-cpu")
                                       .dimensions(Dimension.builder().name("InstanceId").value("i-1").build(),
                                                   Dimension.builder().name("AutoScalingGroupName").value("g").build())
                                       .alarmActions("arn:a", "arn:b")
                                       .stateUpdatedTimestamp(TIME)
                                       .threshold(80.5)
                                       .evaluationPeriods(3)
                                       .build();
        return DescribeAlarmsResponse.builder().metricAlarms(alarm, alarm.toBuilder().alarmName("low").build())
                                     .nextToken("next").build();
    }

    private static void assertSameFields(SdkPojo expected, Object actual) {
        // equals also compares the response metadata, which only the deserialized value has.
        assertThat(expected.equalsBySdkFields(actual)).as("%s", actual).isTrue();
    }

    private static ResponseGenerator generator(ClientProtocol<?, ?> protocol) {
        return ResponseGenerator.forProtocol(protocol.id());
    }

    private static <O extends SerializableStruct> O read(ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol,
                                                         ApiOperation<?, O> operation, SdkHttpFullResponse response) {
        return deserialize(protocol, operation, response);
    }

    @SuppressWarnings("unchecked")
    private static <I extends SerializableStruct, O extends SerializableStruct> O deserialize(
        ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol, ApiOperation<?, O> operation,
        SdkHttpFullResponse response) {
        ApiOperation<I, O> typed = (ApiOperation<I, O>) operation;
        return protocol.deserializeResponse(typed, new ExecutionAttributes(), typed.errorRegistry(), null, response);
    }

    private static AwsServiceException readError(ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol,
                                                 ApiOperation<?, ?> operation, SdkHttpFullResponse response) {
        Throwable thrown = catchThrowable(() -> deserialize(protocol, operation, response));
        assertThat(thrown).isInstanceOf(AwsServiceException.class);
        return (AwsServiceException) thrown;
    }

    private static void assertRoundTrip(AwsServiceException expected, AwsServiceException actual, int status) {
        assertThat(actual).isExactlyInstanceOf(expected.getClass());
        Schema schema = ((SerializableStruct) expected).sdkSchema();
        for (Schema member : schema.members()) {
            Object want = ((SerializableStruct) expected).getMemberValue(member);
            Object got = ((SerializableStruct) actual).getMemberValue(member);
            assertThat(got).as(member.memberName()).isEqualTo(want);
        }
        assertThat(actual.rawMessage()).isEqualTo(expected.rawMessage());
        assertThat(actual.statusCode()).isEqualTo(status);
        if (expected.requestId() != null) {
            assertThat(actual.requestId()).isEqualTo(expected.requestId());
        }
        assertThat(((SdkServiceException) actual).statusCode()).isEqualTo(status);
    }
}
