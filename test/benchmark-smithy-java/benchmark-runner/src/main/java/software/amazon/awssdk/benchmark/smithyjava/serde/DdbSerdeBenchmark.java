package software.amazon.awssdk.benchmark.smithyjava.serde;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.io.uri.SmithyUri;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * SERDE-only comparison: AWS SDK V2 JSON protocol marshalling/unmarshalling vs Smithy-Java
 * schema-driven serialization/deserialization, on DynamoDB-shaped payloads of increasing size.
 *
 * <p>Marshal side: both produce a full protocol HTTP request object (headers + serialized body)
 * exactly the way the respective clients do in their pipelines:
 * <ul>
 *   <li>V2: {@code JsonProtocolMarshallerBuilder} with the same {@code OperationInfo} the
 *       generated {@code PutItemRequestMarshaller} uses.</li>
 *   <li>Smithy-Java: {@code AwsJson1Protocol.createRequest} (which calls
 *       {@code JsonCodec.serialize}).</li>
 * </ul>
 *
 * <p>Unmarshal side: both parse identical DynamoDB wire-format GetItem response bytes into
 * their typed output object. V2 is measured both with the default DOM-based unmarshaller
 * (what real clients use out of the box: {@code ENABLE_FAST_UNMARSHALLER} defaults to false)
 * and the opt-in fast (streaming) unmarshaller.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class DdbSerdeBenchmark {

    private static final URI ENDPOINT = URI.create("http://localhost/");
    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";
    private static final OperationInfo PUT_ITEM_OPERATION_INFO = OperationInfo.builder()
        .requestUri("/")
        .httpMethod(SdkHttpMethod.POST)
        .hasExplicitPayloadMember(false)
        .hasImplicitPayloadMembers(true)
        .hasPayloadMembers(true)
        .operationIdentifier("DynamoDB_20120810.PutItem")
        .build();

    @Param({"SMALL", "MEDIUM", "LARGE", "XL"})
    private String size;

    // V2 state
    private PutItemRequest v2PutRequest;
    private AwsJsonProtocolMetadata v2ProtocolMetadata;
    private JsonProtocolUnmarshaller v2DomUnmarshaller;
    private JsonProtocolUnmarshaller v2FastUnmarshaller;

    // Smithy-Java state
    private AwsJson1Protocol sjProtocol;
    private software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemInput sjPutInput;
    private Context sjContext;
    private SmithyUri sjUri;

    // Shared response bytes (identical for both SDKs)
    private byte[] getItemResponseBytes;

    @Setup(Level.Trial)
    public void setup() {
        Map<String, DdbItems.Attr> item = DdbItems.forSize(size);

        // --- V2 ---
        v2PutRequest = PutItemRequest.builder()
                                     .tableName("benchmark-table")
                                     .item(DdbItems.toV2(item))
                                     .build();
        v2ProtocolMetadata = AwsJsonProtocolMetadata.builder()
                                                    .protocol(AwsJsonProtocol.AWS_JSON)
                                                    .contentType(CONTENT_TYPE)
                                                    .build();
        v2DomUnmarshaller = JsonProtocolUnmarshaller.builder()
                                                    .enableFastUnmarshalling(false)
                                                    .protocolUnmarshallDependencies(
                                                        JsonProtocolUnmarshaller.defaultProtocolUnmarshallDependencies())
                                                    .build();
        v2FastUnmarshaller = JsonProtocolUnmarshaller.builder()
                                                     .enableFastUnmarshalling(true)
                                                     .protocolUnmarshallDependencies(
                                                         JsonProtocolUnmarshaller.defaultProtocolUnmarshallDependencies())
                                                     .build();

        // --- Smithy-Java ---
        sjProtocol = new AwsJson1Protocol(ShapeId.from("com.amazonaws.dynamodb#DynamoDB_20120810"));
        sjPutInput = software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemInput.builder()
                                                                                            .tableName("benchmark-table")
                                                                                            .item(DdbItems.toSj(item))
                                                                                            .build();
        sjContext = Context.create();
        sjUri = SmithyUri.of("http://localhost/");

        // --- Shared response ---
        getItemResponseBytes = DdbItems.toGetItemResponseJson(item).getBytes(StandardCharsets.UTF_8);

        // Sanity: report body sizes once so runs document payload scale.
        SdkHttpFullRequest v2Marshalled = v2Marshaller().marshall(v2PutRequest);
        int v2Len = v2Marshalled.contentStreamProvider()
                                .map(p -> {
                                    try {
                                        return p.newStream().readAllBytes().length;
                                    } catch (Exception e) {
                                        return -1;
                                    }
                                })
                                .orElse(0);
        var sjReq = sjProtocol.createRequest(
            software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItem.instance(),
            sjPutInput, sjContext, sjUri);
        long sjLen = sjReq.body().contentLength();
        System.out.printf("%n[setup %s] v2 marshalled body=%d bytes, sj marshalled body=%d bytes, "
                          + "getItem response=%d bytes%n", size, v2Len, sjLen, getItemResponseBytes.length);
    }

    private ProtocolMarshaller<SdkHttpFullRequest> v2Marshaller() {
        // A fresh marshaller per request is exactly what the generated marshaller does.
        return JsonProtocolMarshallerBuilder.create()
                                            .endpoint(ENDPOINT)
                                            .jsonGenerator(AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY
                                                               .createWriter(CONTENT_TYPE))
                                            .contentType(CONTENT_TYPE)
                                            .operationInfo(PUT_ITEM_OPERATION_INFO)
                                            .sendExplicitNullForPayload(false)
                                            .protocolMetadata(v2ProtocolMetadata)
                                            .build();
    }

    // ==================== Marshal (PutItem request) ====================

    @Benchmark
    public void v2Marshall(Blackhole bh) {
        bh.consume(v2Marshaller().marshall(v2PutRequest));
    }

    @Benchmark
    public void sjMarshall(Blackhole bh) {
        bh.consume(sjProtocol.createRequest(
            software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItem.instance(),
            sjPutInput, sjContext, sjUri));
    }

    // ==================== Unmarshal (GetItem response) ====================

    @Benchmark
    public void v2UnmarshallDom(Blackhole bh) throws Exception {
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
            .statusCode(200)
            .putHeader("Content-Type", CONTENT_TYPE)
            .content(AbortableInputStream.create(new ByteArrayInputStream(getItemResponseBytes)))
            .build();
        bh.consume(v2DomUnmarshaller.unmarshall((SdkPojo) GetItemResponse.builder(), response));
    }

    @Benchmark
    public void v2UnmarshallFast(Blackhole bh) throws Exception {
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
            .statusCode(200)
            .putHeader("Content-Type", CONTENT_TYPE)
            .content(AbortableInputStream.create(new ByteArrayInputStream(getItemResponseBytes)))
            .build();
        bh.consume(v2FastUnmarshaller.unmarshall((SdkPojo) GetItemResponse.builder(), response));
    }

    @Benchmark
    public void sjUnmarshall(Blackhole bh) {
        bh.consume(sjProtocol.payloadCodec().deserializeShape(
            getItemResponseBytes,
            software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItemOutput.builder()));
    }
}
