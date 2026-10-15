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

package software.amazon.awssdk.protocol.smithytests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sdkjsonprotocol.SdkJsonProtocolClient;
import software.amazon.awssdk.services.sdkjsonprotocol.SdkJsonProtocolClientBuilder;
import software.amazon.awssdk.services.sdkjsonprotocol.model.SdkDocumentsResponse;
import software.amazon.awssdk.services.sdkrestjsonprotocol.SdkRestJsonProtocolClient;
import software.amazon.awssdk.services.sdkrestjsonprotocol.SdkRestJsonProtocolClientBuilder;

/**
 * A JSON {@code null} document member reads as {@code Document.fromNull()}, not as an unset member
 * (legacy-disposition.csv row json-core-output.json#35, retired {@code [moved-to-handwritten]}: the protocol test
 * generator treats a null params value as unset, so no Smithy case can assert this).
 *
 * <p>Each test is tagged with the protocol it covers, so it runs only when {@code protocolTests.enabledProtocols} lists
 * that protocol and is counted under it. rpcv2Cbor has no document types ({@code rpcv2Cbor.NoDocuments}), so this class
 * has no rpcv2Cbor case; the JSON protocol cases run once task 7.21 enables those protocols.
 */
class NullDocumentProtocolTest {
    private static final String AWS_JSON_1_1 = "aws.protocols#awsJson1_1";
    private static final String REST_JSON_1 = "aws.protocols#restJson1";

    private static final ProtocolTestHarness<SdkJsonProtocolClient> AWS_JSON_1_1_HARNESS =
        ProtocolTestHarnesses.<SdkJsonProtocolClient, SdkJsonProtocolClientBuilder>forClient(
            AWS_JSON_1_1,
            SdkJsonProtocolClient::builder,
            b -> b.region(Region.US_EAST_1)
                  .credentialsProvider(AnonymousCredentialsProvider.create())
                  .overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.doNotRetry())));

    private static final ProtocolTestHarness<SdkRestJsonProtocolClient> REST_JSON_1_HARNESS =
        ProtocolTestHarnesses.<SdkRestJsonProtocolClient, SdkRestJsonProtocolClientBuilder>forClient(
            REST_JSON_1,
            SdkRestJsonProtocolClient::builder,
            b -> b.region(Region.US_EAST_1)
                  .credentialsProvider(AnonymousCredentialsProvider.create())
                  .overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.doNotRetry())));

    @Test
    @Tag("protocol:" + AWS_JSON_1_1)
    void awsJson11_nullDocumentMember_readsAsNullDocument() {
        StubbedResponse response = StubbedResponse.fromTestCase(
            200, ProtocolTestValues.<String>mapOf("Content-Type", "application/x-amz-json-1.1"),
            "{\"documentValue\": null}", "application/json");

        SdkDocumentsResponse output = AWS_JSON_1_1_HARNESS.respondWith(response, c -> c.sdkDocuments(r -> { }));

        assertThat(output.documentValue()).isEqualTo(Document.fromNull());
    }

    @Test
    @Tag("protocol:" + REST_JSON_1)
    void restJson1_nullDocumentMember_readsAsNullDocument() {
        StubbedResponse response = StubbedResponse.fromTestCase(
            200, ProtocolTestValues.<String>mapOf("Content-Type", "application/json"),
            "{\"documentValue\": null}", "application/json");

        software.amazon.awssdk.services.sdkrestjsonprotocol.model.SdkDocumentsResponse output =
            REST_JSON_1_HARNESS.respondWith(response, c -> c.sdkDocuments(r -> { }));

        assertThat(output.documentValue()).isEqualTo(Document.fromNull());
    }
}
