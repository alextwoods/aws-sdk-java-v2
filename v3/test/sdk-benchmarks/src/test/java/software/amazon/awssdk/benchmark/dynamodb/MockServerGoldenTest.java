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

package software.amazon.awssdk.benchmark.dynamodb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

/**
 * The V3 mock server serves racecar's canned responses byte for byte, and verification accepts a correct arm and
 * rejects a wrong transport, a wrong request body and a wrong response.
 *
 * <p>The golden lengths and SHA-256 values were taken from racecar's own {@code BenchmarkItems} JSON writers
 * ({@code feature/poc/racecar:test/standalone-e2e-benchmarks/src/main/java/.../BenchmarkItems.java}, run standalone).
 */
class MockServerGoldenTest {
    private static MockDdbServer server;
    private static URI endpoint;

    @BeforeAll
    static void start() throws Exception {
        server = MockDdbServer.start(0, 8, MockDdbServer.cannedResponses());
        endpoint = URI.create("http://127.0.0.1:" + server.port());
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    @ParameterizedTest
    @CsvSource({
        "GetItem, 470, 8cd477e9863962306c32d414753a634c4f693630473992c4c62a0267c86b2695",
        "PutItem, 2, 44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
        "BatchGetItem, 37808, 5e737ecbecc37a6c4041df7d73c075942f1a98b7bc7e9e654e50a7cb0a5fef3b",
        "BatchWriteItem, 23, 148072d9c6ab0b5b8d4baed5c5c670dd58a790235beafd9bd6a1859351e394b2"
    })
    void servesRacecarBytes(String operation, int length, String sha256) throws Exception {
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(endpoint.resolve("/")).header("X-Amz-Target", "DynamoDB_20120810." + operation)
                       .header("Content-Type", MockDdbServer.CONTENT_TYPE)
                       .POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
            HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode());
        assertEquals(MockDdbServer.CONTENT_TYPE, response.headers().firstValue("Content-Type").orElseThrow());
        assertEquals(Integer.toString(length), response.headers().firstValue("Content-Length").orElseThrow());
        assertEquals(length, response.body().length);
        assertEquals(sha256, ResponseDigest.sha256Bytes(response.body()));
        assertArrayEquals(MockDdbServer.cannedResponses().get("DynamoDB_20120810." + operation), response.body());
    }

    @Test
    void unknownTargetIs400() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(endpoint.resolve("/")).header("X-Amz-Target", "DynamoDB_20120810.Nope")
                       .POST(HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    void verifiesCorrectSyncAndAsyncArms() throws Exception {
        for (Arm arm : List.of(Arm.V3_SYNC_APACHE5, Arm.V3_ASYNC_NETTY)) {
            try (V3Workload w = new V3Workload(arm, endpoint, 1)) {
                Map<Scenario, String> digests = TransportCheck.verify(w, List.of(Scenario.values()));
                assertEquals(VerifyAll.expectedDigests(), digests);
            }
        }
    }

    @Test
    void rejectsWrongTransport() throws Exception {
        try (V3Workload w = new V3Workload(Arm.V3_SYNC_APACHE5, endpoint, UrlConnectionHttpClient.create())) {
            assertThrows(TransportCheck.VerificationException.class, () -> TransportCheck.requireTransport(w));
        }
    }

    @Test
    void rejectsWrongRequestBody() throws Exception {
        try (V3Workload w = new V3Workload(Arm.V3_SYNC_APACHE5, endpoint, 1)) {
            w.call(Scenario.SMALL_PUT);
            assertThrows(TransportCheck.VerificationException.class,
                         () -> TransportCheck.requireWire(w, Scenario.SMALL_PUT,
                                                          DynamoDbItems.utf8(Scenario.SMALL_GET.expectedRequestJson())));
        }
    }

    @Test
    void rejectsWrongResponse() throws Exception {
        Map<String, byte[]> tampered = MockDdbServer.cannedResponses();
        tampered.put("DynamoDB_20120810.GetItem",
                     DynamoDbItems.utf8(DynamoDbItems.getItemResponseJson().replace("123.456", "123.457")));
        MockDdbServer bad = MockDdbServer.start(0, 4, tampered);
        try (V3Workload w = new V3Workload(Arm.V3_SYNC_APACHE5, URI.create("http://127.0.0.1:" + bad.port()), 1)) {
            assertThrows(TransportCheck.VerificationException.class,
                         () -> TransportCheck.verify(w, List.of(Scenario.SMALL_GET)));
        } finally {
            bad.stop();
        }
    }
}
