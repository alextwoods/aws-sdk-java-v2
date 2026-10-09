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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.benchmark.harness.CellId;
import software.amazon.awssdk.benchmark.harness.DynamoDbPutItemFixture;
import software.amazon.awssdk.benchmark.harness.PayloadClass;
import software.amazon.awssdk.benchmark.harness.ServiceFixture;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * The V2 reference against the shared fixture, without a mock-service process: V2 {@value V2ReferenceMain#EXPECTED_VERSION}
 * sends exactly the fixture's request bytes and routing header for every payload class, decodes the fixture's success
 * and modeled-error responses, and the class path is V2 only.
 */
class V2ReferenceWireTest {

    @Test
    void classPathIsExactlyTheV2Baseline() {
        assertEquals(V2ReferenceMain.EXPECTED_VERSION, VersionInfo.SDK_VERSION);
        V2ReferenceMain.checkIsolation();
    }

    @Test
    void v2SendsTheFixtureBytesAndDecodesItsResponses() {
        for (PayloadClass payload : PayloadClass.values()) {
            DynamoDbPutItemFixture fixture = new DynamoDbPutItemFixture(payload);
            Capturing http = new Capturing(fixture.success());
            try (DynamoDbClient client = client(http)) {
                PutItemResponse response = client.putItem(V2DynamoDbWorkloads.request(fixture));
                assertEquals(fixture.pad(), response.attributes().get("pad").s());
            }
            assertEquals(1, http.requests.size());
            HttpExecuteRequest sent = http.requests.get(0);
            assertTrue(fixture.matches(sent.httpRequest().method().name(), sent.httpRequest().encodedPath(),
                                       sent.httpRequest().firstMatchingHeader(fixture.routingHeaderName())
                                           .orElse(null)), payload.id());
            assertArrayEquals(fixture.requestJson(), http.bodies.get(0), payload.id());
        }
    }

    @Test
    void v2MapsTheModeledErrorToResourceNotFound() {
        DynamoDbPutItemFixture fixture = new DynamoDbPutItemFixture(PayloadClass.SMALL);
        try (DynamoDbClient client = client(new Capturing(fixture.modeledError()))) {
            assertThrows(ResourceNotFoundException.class, () -> client.putItem(V2DynamoDbWorkloads.request(fixture)));
        }
    }

    @Test
    void applicabilityIsTheAwsJson10GeneratedFiniteCellsOnV2Transports() {
        assertTrue(V2ReferenceMain.appliesTo(CellId.parse("awsJson1_0/generated-sync/apache5-sync/finite/small/success/c1")));
        assertTrue(V2ReferenceMain.appliesTo(CellId.parse("awsJson1_0/generated-async/netty-async/finite/large/retry/c32")));
        assertFalse(V2ReferenceMain.appliesTo(CellId.parse("awsJson1_0/generated-async/apache5-async/finite/small/success/c1")));
        assertFalse(V2ReferenceMain.appliesTo(CellId.parse("awsJson1_0/dynamic-sync/apache5-sync/finite/small/success/c1")));
        assertFalse(V2ReferenceMain.appliesTo(CellId.parse("restJson1/generated-sync/apache5-sync/finite/small/success/c1")));
        assertThrows(IllegalStateException.class, () -> new V2ReferenceMain.Source().fixtureName(
            CellId.parse("awsJson1_0/dynamic-sync/apache5-sync/finite/small/success/c1")));
    }

    private static DynamoDbClient client(SdkHttpClient http) {
        return DynamoDbClient.builder().region(Region.US_EAST_1).credentialsProvider(V2DynamoDbWorkloads.credentials())
                             .endpointOverride(URI.create("http://127.0.0.1:1")).httpClient(http)
                             .overrideConfiguration(o -> o.retryStrategy(V2DynamoDbWorkloads.retryStrategy())).build();
    }

    /** Records each request and its body and answers with one fixed response. */
    private static final class Capturing implements SdkHttpClient {
        final List<HttpExecuteRequest> requests = new ArrayList<>();
        final List<byte[]> bodies = new ArrayList<>();
        private final ServiceFixture.MockResponse response;

        Capturing(ServiceFixture.MockResponse response) {
            this.response = response;
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    requests.add(request);
                    bodies.add(request.contentStreamProvider().map(p -> {
                        try (InputStream in = p.newStream()) {
                            return in.readAllBytes();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }).orElse(new byte[0]));
                    return HttpExecuteResponse.builder()
                        .response(SdkHttpResponse.builder().statusCode(response.status())
                                                 .putHeader("Content-Type", response.contentType()).build())
                        .responseBody(AbortableInputStream.create(new ByteArrayInputStream(response.body())))
                        .build();
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public void close() {
        }
    }
}
