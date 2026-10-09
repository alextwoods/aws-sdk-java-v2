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

import java.net.URI;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.benchmark.harness.CellId;
import software.amazon.awssdk.benchmark.harness.DynamoDbPutItemFixture;
import software.amazon.awssdk.benchmark.harness.PaddedFixture;
import software.amazon.awssdk.benchmark.harness.PayloadClass;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * The V2 reference against the shared fixtures, without a mock-service process: for every protocol it serves and every
 * payload class, V2 {@value V2ReferenceMain#EXPECTED_VERSION} sends exactly the fixture's request bytes, method, path
 * and routing header (or routing body prefix), decodes the fixture's success response (the echoed pad) and maps its
 * modeled-error response to the modeled exception; and the class path is V2 only.
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
            CapturingHttpClient http = new CapturingHttpClient(fixture.success());
            try (DynamoDbClient client = client(http)) {
                PutItemResponse response = client.putItem(V2DynamoDbWorkloads.request(fixture));
                assertEquals(fixture.pad(), response.attributes().get("pad").s());
            }
            assertEquals(1, http.requests().size());
            HttpExecuteRequest sent = http.requests().get(0);
            assertTrue(fixture.matches(sent.httpRequest().method().name(), sent.httpRequest().encodedPath(),
                                       sent.httpRequest().firstMatchingHeader(fixture.routingHeaderName())
                                           .orElse(null)), payload.id());
            assertArrayEquals(fixture.requestJson(), http.bodies().get(0), payload.id());
        }
    }

    @Test
    void v2MapsTheModeledErrorToResourceNotFound() {
        DynamoDbPutItemFixture fixture = new DynamoDbPutItemFixture(PayloadClass.SMALL);
        try (DynamoDbClient client = client(new CapturingHttpClient(fixture.modeledError()))) {
            assertThrows(ResourceNotFoundException.class, () -> client.putItem(V2DynamoDbWorkloads.request(fixture)));
        }
    }

    @Test
    void v2SendsEachProtocolsFixtureBytesAndDecodesItsResponses() {
        for (String protocol : V2ReferenceMain.PROTOCOLS) {
            if (protocol.equals("awsJson1_0")) {
                continue;
            }
            for (PayloadClass payload : PayloadClass.values()) {
                String label = protocol + "/" + payload.id();
                PaddedFixture fixture = V2FiniteWorkloads.fixture(cell(protocol, payload, "success"));
                CapturingHttpClient http = new CapturingHttpClient(fixture.success());
                V2FiniteWorkloads.Invoker invoker = V2FiniteWorkloads.invoker(protocol, fixture, ENDPOINT, http);
                try (AutoCloseable client = invoker.client()) {
                    assertEquals(fixture.pad(), invoker.echo().apply(invoker.sync().get()), label);
                } catch (Exception e) {
                    throw new AssertionError(label, e);
                }
                assertEquals(1, http.requests().size(), label);
                HttpExecuteRequest sent = http.requests().get(0);
                String header = fixture.routingHeaderName() == null ? null
                    : sent.httpRequest().firstMatchingHeader(fixture.routingHeaderName()).orElse(null);
                // V2's query marshallers leave the path empty; every transport sends an empty path as "/" (the
                // origin-form of RFC 9112 3.2.1), which is what the mock service sees (the forked smoke checks it).
                String encodedPath = sent.httpRequest().encodedPath();
                encodedPath = encodedPath.isEmpty() ? "/" : encodedPath;
                assertTrue(fixture.matches(sent.httpRequest().method().name(), encodedPath, header),
                           label + " " + sent.httpRequest());
                assertTrue(fixture.matchesBody(http.bodies().get(0)), label);
                byte[] expected = V2FiniteWorkloads.expectedRequest(protocol, fixture);
                assertArrayEquals(expected, http.bodies().get(0), label);
                long encoded = V2FiniteWorkloads.encodedBytes(cell(protocol, payload, "success"));
                assertEquals(http.bodies().get(0).length + (long) fixture.success().body().length, encoded, label);
                // Only rpcv2Cbor's long text strings deviate from the shared fixture's bytes.
                boolean shared = !protocol.equals("rpcv2Cbor") || payload == PayloadClass.SMALL;
                assertEquals(shared, Arrays.equals(fixture.request(), expected), label);
            }
        }
    }

    @Test
    void v2MapsEachProtocolsModeledErrorToItsModeledException() {
        for (String protocol : V2ReferenceMain.PROTOCOLS) {
            PaddedFixture fixture = protocol.equals("awsJson1_0") ? null
                : V2FiniteWorkloads.fixture(cell(protocol, PayloadClass.SMALL, "error"));
            if (fixture == null || !fixture.hasModeledError()) {
                continue;
            }
            V2FiniteWorkloads.Invoker invoker =
                V2FiniteWorkloads.invoker(protocol, fixture, ENDPOINT, new CapturingHttpClient(fixture.modeledError()));
            try (AutoCloseable client = invoker.client()) {
                RuntimeException e = assertThrows(RuntimeException.class, () -> invoker.sync().get(), protocol);
                assertTrue(invoker.modeledError().test(e), protocol + ": " + e);
            } catch (Exception e) {
                throw new AssertionError(protocol, e);
            }
        }
    }

    @Test
    void ec2QueryHasNoModeledErrorCell() {
        CellId cell = cell("ec2Query", PayloadClass.SMALL, "error");
        assertFalse(V2FiniteWorkloads.fixture(cell).hasModeledError());
        assertThrows(IllegalArgumentException.class, () -> V2FiniteWorkloads.create(cell, ENDPOINT));
    }

    private static CellId cell(String protocol, PayloadClass payload, String outcome) {
        return CellId.parse(protocol + "/generated-sync/apache5-sync/finite/" + payload.id() + "/" + outcome + "/c1");
    }

    @Test
    void applicabilityIsTheGeneratedFiniteCellsOfTheServedProtocolsOnV2Transports() {
        assertTrue(V2ReferenceMain.appliesTo(CellId.parse("awsJson1_0/generated-sync/apache5-sync/finite/small/success/c1")));
        assertTrue(V2ReferenceMain.appliesTo(CellId.parse("awsJson1_0/generated-async/netty-async/finite/large/retry/c32")));
        assertFalse(V2ReferenceMain.appliesTo(CellId.parse("awsJson1_0/generated-async/apache5-async/finite/small/success/c1")));
        assertFalse(V2ReferenceMain.appliesTo(CellId.parse("awsJson1_0/dynamic-sync/apache5-sync/finite/small/success/c1")));
        assertTrue(V2ReferenceMain.appliesTo(CellId.parse("restJson1/generated-sync/apache5-sync/finite/small/success/c1")));
        assertTrue(V2ReferenceMain.appliesTo(CellId.parse("rpcv2Cbor/generated-async/crt-async/finite/medium/error/c1")));
        assertFalse(V2ReferenceMain.appliesTo(CellId.parse("restXml/generated-sync/apache5-sync/finite/small/success/c1")));
        assertFalse(V2ReferenceMain.appliesTo(CellId.parse("restJson1/generated-sync/apache5-sync/streaming/small/success/c1")));
        assertThrows(IllegalStateException.class, () -> new V2ReferenceMain.Source().fixtureName(
            CellId.parse("awsJson1_0/dynamic-sync/apache5-sync/finite/small/success/c1")));
    }

    private static final URI ENDPOINT = URI.create("http://127.0.0.1:1");

    private static DynamoDbClient client(SdkHttpClient http) {
        return DynamoDbClient.builder().region(Region.US_EAST_1).credentialsProvider(V2DynamoDbWorkloads.credentials())
                             .endpointOverride(URI.create("http://127.0.0.1:1")).httpClient(http)
                             .overrideConfiguration(o -> o.retryStrategy(V2DynamoDbWorkloads.retryStrategy())).build();
    }
}
