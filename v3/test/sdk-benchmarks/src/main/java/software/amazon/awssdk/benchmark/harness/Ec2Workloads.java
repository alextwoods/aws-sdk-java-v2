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

package software.amazon.awssdk.benchmark.harness;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.benchmark.harness.FiniteWorkloads.Invoker;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairResponse;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The ec2Query fixture: EC2 {@code ImportKeyPair} (a form-encoded {@code Action=ImportKeyPair} body, routed by that
 * body). The {@code PublicKeyMaterial} blob (base64 on the wire) is the pad; the success response echoes it as the
 * {@code keyFingerprint}. Retry cells get a 500 {@code InternalError}, retryable by status.
 *
 * <p>No error cells: the EC2 model declares no error shapes at all (every EC2 error is an unmodeled code), so an
 * ec2Query modeled-error workload does not exist; those cells stay without a fixture, and release mode refuses them.
 */
final class Ec2Workloads extends FiniteWorkloads.Provider {
    static final String KEY_NAME = Ec2ImportKeyPairFixture.KEY_NAME;
    static final String NS = Ec2ImportKeyPairFixture.NS;
    static final String CONTENT_TYPE = Ec2ImportKeyPairFixture.CONTENT_TYPE;

    Ec2Workloads() {
        super("ec2-importkeypair", BenchProtocol.EC2_QUERY, "ec2", "com.amazonaws.ec2#AmazonEC2", "ImportKeyPair",
              null);
    }

    @Override
    public PaddedFixture fixture(BenchmarkCell cell) {
        return Ec2ImportKeyPairFixture.create(cell.payload());
    }

    @Override
    Invoker generated(boolean async, PaddedFixture fixture, URI endpoint, AutoCloseable http) {
        ImportKeyPairRequest request = ImportKeyPairRequest.builder().keyName(KEY_NAME)
                                                           .publicKeyMaterial(SdkBytes.fromUtf8String(fixture.pad()))
                                                           .build();
        Function<Object, String> echo = r -> ((ImportKeyPairResponse) r).keyFingerprint();
        if (async) {
            Ec2AsyncClient client = Ec2AsyncClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkAsyncHttpClient) http).build();
            return Invoker.async(client, () -> client.importKeyPair(request), t -> false, echo);
        }
        Ec2Client client = Ec2Client.builder().region(Region.US_EAST_1)
            .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
            .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkSyncHttpClient) http).build();
        return Invoker.sync(client, () -> client.importKeyPair(request), t -> false, echo);
    }

    @Override
    Document dynamicInput(PaddedFixture fixture) {
        Map<String, Document> input = new LinkedHashMap<>();
        input.put("KeyName", Document.of(KEY_NAME));
        input.put("PublicKeyMaterial", Document.of(ByteBuffer.wrap(fixture.pad().getBytes(StandardCharsets.UTF_8))));
        return Document.of(input);
    }

    @Override
    String dynamicEcho(Document output) {
        return output.getMember("KeyFingerprint").asString();
    }
}
