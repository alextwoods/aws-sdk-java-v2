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

package software.amazon.awssdk.benchmark.reference.v2;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.benchmark.harness.CellId;
import software.amazon.awssdk.benchmark.harness.CellWorkload;
import software.amazon.awssdk.benchmark.harness.CloudWatchPutDashboardFixture;
import software.amazon.awssdk.benchmark.harness.Ec2ImportKeyPairFixture;
import software.amazon.awssdk.benchmark.harness.KinesisPutRecordFixture;
import software.amazon.awssdk.benchmark.harness.LambdaUpdateFunctionConfigurationFixture;
import software.amazon.awssdk.benchmark.harness.Outcome;
import software.amazon.awssdk.benchmark.harness.PaddedFixture;
import software.amazon.awssdk.benchmark.harness.PayloadClass;
import software.amazon.awssdk.benchmark.harness.SnsPublishFixture;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.DashboardInvalidInputErrorException;
import software.amazon.awssdk.services.cloudwatch.model.PutDashboardRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutDashboardResponse;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairResponse;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationRequest;
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationResponse;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * The finite fixtures of every protocol but awsJson1.0 ({@link V2DynamoDbWorkloads}) on the released V2 clients of
 * the V3 harness's own services and operations, over the shared SDK-free {@link PaddedFixture} wire bytes, so V2
 * sends each fixture's exact request bytes and the validity rules apply to this arm unchanged
 * ({@link V2ReferenceWireTest}). Client configuration and outcome checks are {@link V2DynamoDbWorkloads}'s.
 *
 * <ul>
 *     <li>awsJson1.1: Kinesis {@code PutRecord}. V2's Kinesis client encodes CBOR by default; the awsJson1.1 cell
 *     runs it on its JSON protocol ({@code aws.cborEnabled=false}, read when the client is built), which V3's
 *     Kinesis client speaks.</li>
 *     <li>restJson1: Lambda {@code UpdateFunctionConfiguration}.</li>
 *     <li>awsQuery: SNS {@code Publish}.</li>
 *     <li>ec2Query: EC2 {@code ImportKeyPair} (no modeled error, as for V3).</li>
 *     <li>rpcv2Cbor: CloudWatch {@code PutDashboard}; V2 {@value V2ReferenceMain#EXPECTED_VERSION}'s CloudWatch client
 *     speaks rpcv2Cbor. Its CBOR generator chunks text strings longer than
 *     {@value CloudWatchPutDashboardFixture#V2_BASELINE_TEXT_CHUNK} characters, so on the medium and large payload
 *     classes V2's request is the fixture's reference-specific {@link CloudWatchPutDashboardFixture#v2BaselineRequest}
 *     and this arm declares those exact bytes ({@link #encodedBytes}).</li>
 * </ul>
 *
 * <p>No restXml client: the V3 restXml fixture is Route 53 {@code UpdateHostedZoneComment}, and route53
 * {@value V2ReferenceMain#EXPECTED_VERSION} is not available to this build.
 */
final class V2FiniteWorkloads {

    private V2FiniteWorkloads() {
    }

    /** The fixture name of a protocol id this class serves (the V3 provider's name), or {@code null}. */
    static String fixtureName(String protocol) {
        return switch (protocol) {
            case "awsJson1_1" -> KinesisPutRecordFixture.NAME;
            case "restJson1" -> LambdaUpdateFunctionConfigurationFixture.NAME;
            case "awsQuery" -> SnsPublishFixture.NAME;
            case "ec2Query" -> Ec2ImportKeyPairFixture.NAME;
            case "rpcv2Cbor" -> CloudWatchPutDashboardFixture.NAME;
            default -> null;
        };
    }

    static PaddedFixture fixture(CellId cell) {
        PayloadClass payload = PayloadClass.fromId(cell.payload());
        return switch (cell.protocol()) {
            case "awsJson1_1" -> KinesisPutRecordFixture.create(payload);
            case "restJson1" -> LambdaUpdateFunctionConfigurationFixture.create(payload);
            case "awsQuery" -> SnsPublishFixture.create(payload);
            case "ec2Query" -> Ec2ImportKeyPairFixture.create(payload);
            case "rpcv2Cbor" -> CloudWatchPutDashboardFixture.create(payload);
            default -> throw new IllegalArgumentException("no V2 finite fixture for " + cell);
        };
    }

    /**
     * The exact request body V2 sends for {@code fixture}: the shared fixture's, except on rpcv2Cbor, where V2's CBOR
     * generator chunks long text strings ({@link CloudWatchPutDashboardFixture#v2BaselineRequest}).
     */
    static byte[] expectedRequest(String protocol, PaddedFixture fixture) {
        return "rpcv2Cbor".equals(protocol) ? CloudWatchPutDashboardFixture.v2BaselineRequest(fixture)
                                            : fixture.request();
    }

    /** The encoded bytes of one logical operation V2 moves (its exact request plus the shared success response). */
    static long encodedBytes(CellId cell) {
        PaddedFixture fixture = fixture(cell);
        return "rpcv2Cbor".equals(cell.protocol())
               ? CloudWatchPutDashboardFixture.v2BaselineEncodedBytes(fixture, PayloadClass.fromId(cell.payload()))
               : fixture.encodedBytes();
    }

    static CellWorkload create(CellId cell, URI endpoint) {
        PaddedFixture fixture = fixture(cell);
        if (Outcome.fromId(cell.outcome()) == Outcome.MODELED_ERROR && !fixture.hasModeledError()) {
            throw new IllegalArgumentException(cell + ": the operation models no error");
        }
        boolean async = V2Transports.isAsync(cell.transport());
        AutoCloseable http = async ? V2Transports.async(cell.transport(), cell.concurrency())
                                   : V2Transports.sync(cell.transport(), cell.concurrency());
        try {
            return new PathWorkload(cell, fixture.pad(), http, invoker(cell.protocol(), fixture, endpoint, http));
        } catch (RuntimeException e) {
            try {
                http.close();
            } catch (Exception ignored) {
                // best effort while failing construction
            }
            throw e;
        }
    }

    /** One client's invocation, as the V3 harness's {@code FiniteWorkloads.Invoker}. */
    record Invoker(AutoCloseable client, Supplier<Object> sync, Supplier<CompletableFuture<?>> async,
                   Predicate<Throwable> modeledError, Function<Object, String> echo) {
    }

    /** The benchmark configuration of every V2 client: credentials, region, endpoint, transport, retry strategy. */
    private static <B extends AwsClientBuilder<B, ?>> B configure(B builder, URI endpoint) {
        return builder.region(Region.US_EAST_1).credentialsProvider(V2DynamoDbWorkloads.credentials())
                      .endpointOverride(endpoint)
                      .overrideConfiguration(o -> o.retryStrategy(V2DynamoDbWorkloads.retryStrategy()));
    }

    private static <B extends AwsClientBuilder<B, C> & AwsSyncClientBuilder<B, C>, C> C sync(B builder, URI endpoint,
                                                                                           AutoCloseable http) {
        return configure(builder, endpoint).httpClient((SdkHttpClient) http).build();
    }

    private static <B extends AwsClientBuilder<B, C> & AwsAsyncClientBuilder<B, C>, C> C async(B builder, URI endpoint,
                                                                                            AutoCloseable http) {
        return configure(builder, endpoint).httpClient((SdkAsyncHttpClient) http).build();
    }

    static Invoker invoker(String protocol, PaddedFixture fixture, URI endpoint, AutoCloseable http) {
        boolean async = http instanceof SdkAsyncHttpClient;
        String pad = fixture.pad();
        switch (protocol) {
            case "awsJson1_1": {
                // V2's Kinesis client is CBOR unless disabled; the awsJson1.1 cell runs its JSON protocol.
                System.setProperty(SdkSystemSetting.CBOR_ENABLED.property(), "false");
                PutRecordRequest request = PutRecordRequest.builder().streamName(KinesisPutRecordFixture.STREAM)
                    .data(SdkBytes.fromUtf8String(pad)).partitionKey(KinesisPutRecordFixture.KEY).build();
                Predicate<Throwable> error =
                    t -> t instanceof software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;
                Function<Object, String> echo = r -> ((PutRecordResponse) r).sequenceNumber();
                if (async) {
                    KinesisAsyncClient c = async(KinesisAsyncClient.builder(), endpoint, http);
                    return new Invoker(c, null, () -> c.putRecord(request), error, echo);
                }
                KinesisClient c = sync(KinesisClient.builder(), endpoint, http);
                return new Invoker(c, () -> c.putRecord(request), null, error, echo);
            }
            case "restJson1": {
                UpdateFunctionConfigurationRequest request = UpdateFunctionConfigurationRequest.builder()
                    .functionName(LambdaUpdateFunctionConfigurationFixture.FUNCTION).description(pad).build();
                Predicate<Throwable> error =
                    t -> t instanceof software.amazon.awssdk.services.lambda.model.ResourceNotFoundException;
                Function<Object, String> echo = r -> ((UpdateFunctionConfigurationResponse) r).description();
                if (async) {
                    LambdaAsyncClient c = async(LambdaAsyncClient.builder(), endpoint, http);
                    return new Invoker(c, null, () -> c.updateFunctionConfiguration(request), error, echo);
                }
                LambdaClient c = sync(LambdaClient.builder(), endpoint, http);
                return new Invoker(c, () -> c.updateFunctionConfiguration(request), null, error, echo);
            }
            case "awsQuery": {
                PublishRequest request = PublishRequest.builder().topicArn(SnsPublishFixture.TOPIC).message(pad)
                                                       .build();
                Predicate<Throwable> error = t -> t instanceof NotFoundException;
                Function<Object, String> echo = r -> ((PublishResponse) r).messageId();
                if (async) {
                    SnsAsyncClient c = async(SnsAsyncClient.builder(), endpoint, http);
                    return new Invoker(c, null, () -> c.publish(request), error, echo);
                }
                SnsClient c = sync(SnsClient.builder(), endpoint, http);
                return new Invoker(c, () -> c.publish(request), null, error, echo);
            }
            case "ec2Query": {
                ImportKeyPairRequest request = ImportKeyPairRequest.builder().keyName(Ec2ImportKeyPairFixture.KEY_NAME)
                    .publicKeyMaterial(SdkBytes.fromUtf8String(pad)).build();
                Function<Object, String> echo = r -> ((ImportKeyPairResponse) r).keyFingerprint();
                if (async) {
                    Ec2AsyncClient c = async(Ec2AsyncClient.builder(), endpoint, http);
                    return new Invoker(c, null, () -> c.importKeyPair(request), t -> false, echo);
                }
                Ec2Client c = sync(Ec2Client.builder(), endpoint, http);
                return new Invoker(c, () -> c.importKeyPair(request), null, t -> false, echo);
            }
            case "rpcv2Cbor": {
                PutDashboardRequest request = PutDashboardRequest.builder()
                    .dashboardName(CloudWatchPutDashboardFixture.DASHBOARD).dashboardBody(pad).build();
                Predicate<Throwable> error = t -> t instanceof DashboardInvalidInputErrorException;
                Function<Object, String> echo =
                    r -> ((PutDashboardResponse) r).dashboardValidationMessages().get(0).message();
                if (async) {
                    CloudWatchAsyncClient c = async(CloudWatchAsyncClient.builder(), endpoint, http);
                    return new Invoker(c, null, () -> c.putDashboard(request), error, echo);
                }
                CloudWatchClient c = sync(CloudWatchClient.builder(), endpoint, http);
                return new Invoker(c, () -> c.putDashboard(request), null, error, echo);
            }
            default:
                throw new IllegalArgumentException("no V2 finite client for protocol " + protocol);
        }
    }

    /** A client driven through an {@link Invoker}, with {@link V2DynamoDbWorkloads}' outcome checks. */
    static final class PathWorkload implements CellWorkload {
        private final CellId cell;
        private final String pad;
        private final AutoCloseable transport;
        private final Invoker invoker;
        private final boolean expectError;

        PathWorkload(CellId cell, String pad, AutoCloseable transport, Invoker invoker) {
            this.cell = cell;
            this.pad = pad;
            this.transport = transport;
            this.invoker = invoker;
            this.expectError = Outcome.fromId(cell.outcome()) == Outcome.MODELED_ERROR;
        }

        @Override
        public boolean isAsync() {
            return invoker.async() != null;
        }

        @Override
        public String transportClass() {
            return transport.getClass().getName();
        }

        private void checkError(Throwable t) {
            Throwable cause = t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
            if (expectError && invoker.modeledError().test(cause)) {
                return;
            }
            throw new IllegalStateException(cell + ": unexpected outcome " + cause, cause);
        }

        private Object invoke() {
            Object response;
            try {
                response = isAsync() ? invoker.async().get().join() : invoker.sync().get();
            } catch (RuntimeException e) {
                checkError(e);
                return null;
            }
            if (expectError) {
                throw new IllegalStateException(cell + ": unexpected outcome: success in an error cell");
            }
            return response;
        }

        @Override
        public void call() {
            invoke();
        }

        @Override
        public CompletableFuture<Void> callAsync() {
            if (!isAsync()) {
                throw new UnsupportedOperationException("sync path");
            }
            return invoker.async().get().handle((r, t) -> {
                if (t != null) {
                    checkError(t);
                } else if (expectError) {
                    throw new IllegalStateException(cell + ": unexpected outcome: success in an error cell");
                }
                return null;
            });
        }

        @Override
        public void verify() {
            Object response = invoke();
            if (response != null && !pad.equals(invoker.echo().apply(response))) {
                throw new IllegalStateException(cell + ": decoded response does not match the fixture");
            }
        }

        @Override
        public void close() throws Exception {
            try {
                invoker.client().close();
            } finally {
                transport.close();
            }
        }
    }
}
