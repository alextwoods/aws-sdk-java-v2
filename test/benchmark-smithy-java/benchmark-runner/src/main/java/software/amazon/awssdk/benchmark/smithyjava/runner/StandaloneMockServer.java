package software.amazon.awssdk.benchmark.smithyjava.runner;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Random;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import java.nio.charset.StandardCharsets;

import software.amazon.awssdk.benchmark.smithyjava.serde.DdbItems;
import software.amazon.awssdk.benchmark.smithyjava.utils.MockHttpServer;

/**
 * Standalone mock HTTP server process for the pipeline benchmarks.
 *
 * <p>Runs six fixed-response Jetty endpoints (one port per service/case) in its own JVM so
 * client-side profiles are not polluted by server work. The responses are identical for
 * every SDK; only the service/case determines the fixture.
 *
 * <pre>
 *   base+0  DynamoDB PutItem    fixtures/dynamodb/putitem-response.json    (application/x-amz-json-1.0)
 *   base+1  DynamoDB GetItem    fixtures/dynamodb/getitem-response.json    (application/x-amz-json-1.0)
 *   base+2  S3 PutObject        empty body + ETag = MD5(payload)           (application/xml)
 *   base+3  S3 GetObject        1 MiB deterministic payload + ETag/Length  (application/octet-stream)
 *   base+4  CloudWatch PutMetricData  fixtures/cloudwatch/putmetricdata-response.xml (text/xml)
 *   base+5  CloudWatch GetMetricData  fixtures/cloudwatch/getmetricdata-response.xml (text/xml)
 * </pre>
 *
 * <p>The S3 ETag is the real MD5 of the payload so SDK V1's client-side GetObject/PutObject
 * MD5 validation passes (V2 and smithy-java ignore it).
 *
 * Usage: java -cp pipeline-benchmarks.jar ...StandaloneMockServer [basePort]
 * Prints "READY base=<port>" when all endpoints are up; runs until killed.
 */
public final class StandaloneMockServer {

    public static final int DEFAULT_BASE_PORT = 18100;
    public static final int OBJECT_SIZE = 1024 * 1024;
    public static final long PAYLOAD_SEED = 0xBEEF;

    public static void main(String[] args) throws Exception {
        int basePort = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_BASE_PORT;

        byte[] ddbPut = MockHttpServer.loadFixture("dynamodb/putitem-response.json");
        byte[] ddbGet = MockHttpServer.loadFixture("dynamodb/getitem-response.json");
        byte[] cwPut = MockHttpServer.loadFixture("cloudwatch/putmetricdata-response.xml");
        byte[] cwGet = MockHttpServer.loadFixture("cloudwatch/getmetricdata-response.xml");

        byte[] s3Payload = payload();
        String etag = "\"" + hex(MessageDigest.getInstance("MD5").digest(s3Payload)) + "\"";
        String[][] s3PutHeaders = {{"ETag", etag}};
        String[][] s3GetHeaders = {{"ETag", etag}, {"Content-Length", String.valueOf(OBJECT_SIZE)}};
        String[][] none = new String[0][];

        // AWS SDK V1 1.12.797 speaks RPCv2-CBOR to CloudWatch (V2 still uses awsQuery/XML), so the
        // V1 CloudWatch endpoints serve CBOR bodies with content mirroring the XML fixtures.
        String[][] cborHeaders = {{"smithy-protocol", "rpc-v2-cbor"}};

        MockHttpServer[] servers = {
            new MockHttpServer(ddbPut, "application/x-amz-json-1.0", none, basePort),
            new MockHttpServer(ddbGet, "application/x-amz-json-1.0", none, basePort + 1),
            new MockHttpServer(new byte[0], "application/xml", s3PutHeaders, basePort + 2),
            new MockHttpServer(s3Payload, "application/octet-stream", s3GetHeaders, basePort + 3),
            new MockHttpServer(cwPut, "text/xml", none, basePort + 4),
            new MockHttpServer(cwGet, "text/xml", none, basePort + 5),
            new MockHttpServer(cwPutCbor(), "application/cbor", cborHeaders, basePort + 6),
            new MockHttpServer(cwGetCbor(), "application/cbor", cborHeaders, basePort + 7),
            // Size-parameterized DynamoDB GetItem responses (round 5: larger, nested, mixed-type
            // items generated from the shared DdbItems spec). PutItem reuses the base+0 endpoint.
            new MockHttpServer(ddbGetJson("MEDIUM"), "application/x-amz-json-1.0", none, basePort + 8),
            new MockHttpServer(ddbGetJson("LARGE"), "application/x-amz-json-1.0", none, basePort + 9),
        };
        for (MockHttpServer server : servers) {
            server.start();
        }

        System.out.printf("READY base=%d (ddb-put,%d ddb-get,%d s3-put,%d s3-get,%d cw-put,%d cw-get,%d"
                          + " cw-put-cbor,%d cw-get-cbor,%d)%n",
                          basePort, basePort, basePort + 1, basePort + 2, basePort + 3, basePort + 4,
                          basePort + 5, basePort + 6, basePort + 7);
        System.out.flush();
        Thread.currentThread().join();
    }

    /** Deterministic 1 MiB payload, shared with the client-side runner. */
    public static byte[] payload() {
        byte[] payload = new byte[OBJECT_SIZE];
        new Random(PAYLOAD_SEED).nextBytes(payload);
        return payload;
    }

    /** DynamoDB GetItem response body for a DdbItems size (identical bytes for every SDK). */
    private static byte[] ddbGetJson(String size) {
        return DdbItems.toGetItemResponseJson(DdbItems.forSize(size)).getBytes(StandardCharsets.UTF_8);
    }

    /** Empty CBOR map — PutMetricData has no output members. */
    private static byte[] cwPutCbor() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CBORGenerator gen = new CBORFactory().createGenerator(out)) {
            gen.writeStartObject();
            gen.writeEndObject();
        }
        return out.toByteArray();
    }

    /** CBOR GetMetricData response with the same content as fixtures/cloudwatch/getmetricdata-response.xml. */
    private static byte[] cwGetCbor() throws Exception {
        long base = 1704067200000L; // 2024-01-01T00:00:00Z
        String[][] results = {
            {"cpu_utilization", "CPUUtilization", "45.2", "52.1", "48.7", "55.3", "41.9"},
            {"memory_utilization", "MemoryUtilization", "72.5", "73.1", "74.2", "71.8", "73.9"},
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CBORGenerator gen = new CBORFactory().createGenerator(out)) {
            gen.writeStartObject();
            gen.writeFieldName("MetricDataResults");
            gen.writeStartArray();
            for (String[] result : results) {
                gen.writeStartObject();
                gen.writeStringField("Id", result[0]);
                gen.writeStringField("Label", result[1]);
                gen.writeFieldName("Timestamps");
                gen.writeStartArray();
                for (int i = 0; i < 5; i++) {
                    gen.writeNumber(base + i * 300_000L); // epoch millis, 5-minute steps
                }
                gen.writeEndArray();
                gen.writeFieldName("Values");
                gen.writeStartArray();
                for (int i = 2; i < 7; i++) {
                    gen.writeNumber(Double.parseDouble(result[i]));
                }
                gen.writeEndArray();
                gen.writeStringField("StatusCode", "Complete");
                gen.writeEndObject();
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
        return out.toByteArray();
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
