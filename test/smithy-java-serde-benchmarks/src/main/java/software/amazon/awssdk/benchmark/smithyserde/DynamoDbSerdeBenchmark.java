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

package software.amazon.awssdk.benchmark.smithyserde;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.protocols.smithyjava.SmithyAwsJsonProtocol;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.operations.BatchGetItemOperation;
import software.amazon.awssdk.services.dynamodb.operations.BatchWriteItemOperation;
import software.amazon.awssdk.services.dynamodb.operations.GetItemOperation;
import software.amazon.awssdk.services.dynamodb.operations.PutItemOperation;
import software.amazon.awssdk.services.dynamodb.transform.BatchGetItemRequestMarshaller;
import software.amazon.awssdk.services.dynamodb.transform.BatchWriteItemRequestMarshaller;
import software.amazon.awssdk.services.dynamodb.transform.GetItemRequestMarshaller;
import software.amazon.awssdk.services.dynamodb.transform.PutItemRequestMarshaller;

/**
 * Serde alone, outside the pipeline: the marshaller a call hands to {@code withMarshaller}, and the success
 * response handler, built exactly the way the generated {@code DefaultDynamoDbClient} builds each.
 *
 * <p>{@code marshall*} produces the {@link SdkHttpFullRequest} and reads its body once, as signing and the
 * transport would. {@code unmarshall*} parses the canned response the end-to-end harness's mock server
 * sends into the response POJO, response metadata included. The difference between the arms here bounds
 * the difference {@link DynamoDbClientBenchmark} can show; what is left over in that benchmark is pipeline
 * cost common to both.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 8, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(3)
public class DynamoDbSerdeBenchmark {

    @Param({"v2", "smithy", "smithy-native"})
    public String serde;

    private final ExecutionAttributes executionAttributes = new ExecutionAttributes();
    private final byte[] drain = new byte[16 * 1024];

    private GetItemRequest smallGet;
    private PutItemRequest smallPut;
    private BatchGetItemRequest batchGet;
    private BatchWriteItemRequest batchPut;

    private Supplier<Marshaller<GetItemRequest>> smallGetMarshallerPerCall;
    private Supplier<Marshaller<PutItemRequest>> smallPutMarshallerPerCall;
    private Supplier<Marshaller<BatchGetItemRequest>> batchGetMarshallerPerCall;
    private Supplier<Marshaller<BatchWriteItemRequest>> batchPutMarshallerPerCall;

    private HttpResponseHandler<GetItemResponse> smallGetHandler;
    private HttpResponseHandler<PutItemResponse> smallPutHandler;
    private HttpResponseHandler<BatchGetItemResponse> batchGetHandler;
    private HttpResponseHandler<BatchWriteItemResponse> batchPutHandler;

    private byte[] smallGetBody;
    private byte[] smallPutBody;
    private byte[] batchGetBody;
    private byte[] batchPutBody;

    @Setup
    public void setup() {
        SerdeSelection.select(serde);
        SdkClientConfiguration config =
            SdkClientConfiguration.builder()
                                  .option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
                                          ClientEndpointProvider.forEndpointOverride(URI.create("http://localhost:19080")))
                                  .build();

        smallGet = DynamoDbRequests.smallGet();
        smallPut = DynamoDbRequests.smallPut();
        batchGet = DynamoDbRequests.batchGet();
        batchPut = DynamoDbRequests.batchPut();

        smallGetBody = utf8(BenchmarkItems.getItemResponseJson());
        smallPutBody = utf8(BenchmarkItems.putItemResponseJson());
        batchGetBody = utf8(BenchmarkItems.batchGetItemResponseJson());
        batchPutBody = utf8(BenchmarkItems.batchWriteItemResponseJson());

        if ("v2".equals(serde)) {
            // As DefaultDynamoDbClient#init builds it; the generated client makes a new marshaller and a new
            // response handler on every call, so the per-call suppliers below do the same.
            AwsJsonProtocolFactory factory = AwsJsonProtocolFactory.builder()
                                                                   .clientConfiguration(config)
                                                                   .defaultServiceExceptionSupplier(DynamoDbException::builder)
                                                                   .protocol(AwsJsonProtocol.AWS_JSON)
                                                                   .protocolVersion("1.0")
                                                                   .build();
            JsonOperationMetadata metadata = JsonOperationMetadata.builder()
                                                                  .hasStreamingSuccessResponse(false)
                                                                  .isPayloadJson(true)
                                                                  .build();
            smallGetMarshallerPerCall = () -> new GetItemRequestMarshaller(factory);
            smallPutMarshallerPerCall = () -> new PutItemRequestMarshaller(factory);
            batchGetMarshallerPerCall = () -> new BatchGetItemRequestMarshaller(factory);
            batchPutMarshallerPerCall = () -> new BatchWriteItemRequestMarshaller(factory);
            smallGetHandler = factory.createResponseHandler(metadata, GetItemResponse::builder);
            smallPutHandler = factory.createResponseHandler(metadata, PutItemResponse::builder);
            batchGetHandler = factory.createResponseHandler(metadata, BatchGetItemResponse::builder);
            batchPutHandler = factory.createResponseHandler(metadata, BatchWriteItemResponse::builder);
        } else {
            SmithyAwsJsonProtocol protocol = SmithyAwsJsonProtocol.builder()
                                                                  .clientConfiguration(config)
                                                                  .contentType("application/x-amz-json-1.0")
                                                                  .targetPrefix("DynamoDB_20120810")
                                                                  .serviceNamespace("com.amazonaws.dynamodb")
                                                                  .build();
            smallGetMarshallerPerCall = () -> protocol.marshaller(GetItemOperation.instance());
            smallPutMarshallerPerCall = () -> protocol.marshaller(PutItemOperation.instance());
            batchGetMarshallerPerCall = () -> protocol.marshaller(BatchGetItemOperation.instance());
            batchPutMarshallerPerCall = () -> protocol.marshaller(BatchWriteItemOperation.instance());
            smallGetHandler = protocol.responseHandler(GetItemOperation.instance());
            smallPutHandler = protocol.responseHandler(PutItemOperation.instance());
            batchGetHandler = protocol.responseHandler(BatchGetItemOperation.instance());
            batchPutHandler = protocol.responseHandler(BatchWriteItemOperation.instance());
        }
    }

    @Benchmark
    public int marshallSmallGet() {
        return consume(smallGetMarshallerPerCall.get().marshall(smallGet));
    }

    @Benchmark
    public int marshallSmallPut() {
        return consume(smallPutMarshallerPerCall.get().marshall(smallPut));
    }

    @Benchmark
    public int marshallBatchGet() {
        return consume(batchGetMarshallerPerCall.get().marshall(batchGet));
    }

    @Benchmark
    public int marshallBatchPut() {
        return consume(batchPutMarshallerPerCall.get().marshall(batchPut));
    }

    @Benchmark
    public GetItemResponse unmarshallSmallGet() throws Exception {
        return smallGetHandler.handle(response(smallGetBody), executionAttributes);
    }

    @Benchmark
    public PutItemResponse unmarshallSmallPut() throws Exception {
        return smallPutHandler.handle(response(smallPutBody), executionAttributes);
    }

    @Benchmark
    public BatchGetItemResponse unmarshallBatchGet() throws Exception {
        return batchGetHandler.handle(response(batchGetBody), executionAttributes);
    }

    @Benchmark
    public BatchWriteItemResponse unmarshallBatchPut() throws Exception {
        return batchPutHandler.handle(response(batchPutBody), executionAttributes);
    }

    private int consume(SdkHttpFullRequest request) {
        int total = request.headers().size();
        try (InputStream in = request.contentStreamProvider().get().newStream()) {
            int n;
            while ((n = in.read(drain)) != -1) {
                total += n;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return total;
    }

    private static SdkHttpFullResponse response(byte[] body) {
        return SdkHttpFullResponse.builder()
                                  .statusCode(200)
                                  .putHeader("x-amzn-RequestId", "BENCHMARKREQUESTID0000000000000000000000000000")
                                  .putHeader("Content-Type", "application/x-amz-json-1.0")
                                  .putHeader("Content-Length", Integer.toString(body.length))
                                  .content(AbortableInputStream.create(new ByteArrayInputStream(body)))
                                  .build();
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
