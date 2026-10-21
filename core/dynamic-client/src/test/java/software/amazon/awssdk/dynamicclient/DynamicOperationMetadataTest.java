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

package software.amazon.awssdk.dynamicclient;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.dynamicclient.CallTestClients.map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.internal.interceptor.trait.RequestCompression;
import software.amazon.awssdk.dynamicclient.internal.DynamicOperationMetadata;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.smithy.model.Model;

/**
 * The per-operation call settings that {@link DynamicOperationMetadata} derives from the schemas of
 * {@code pipeline-features.smithy}, and puts on a call's parameters as a generated operation method does.
 */
class DynamicOperationMetadataTest {
    private static final String BEARER_MODEL =
        "$version: \"2.0\"\n"
        + "namespace test.bearer\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#awsJson1_0\n"
        + "@awsJson1_0\n"
        + "@sigv4(name: \"bearer\")\n"
        + "@httpBearerAuth\n"
        + "@auth([httpBearerAuth, sigv4])\n"
        + "service BearerService { version: \"2024-01-01\", operations: [TokenFirst, Sigv4First] }\n"
        + "operation TokenFirst {}\n"
        + "@auth([sigv4, httpBearerAuth])\n"
        + "operation Sigv4First {}\n";

    private static DynamicClient client;

    @BeforeAll
    static void setUp() {
        client = CallTestClients.client(DynamicTestModels.pipelineFeatures(), new RecordingHttpClients.Sync(),
                                        new RecordingHttpClients.Async());
    }

    @AfterAll
    static void tearDown() {
        client.close();
    }

    @Test
    void features_requestCompression_comesFromTheTrait() {
        assertThat(features("UploadLogs").requestCompression())
            .isEqualTo(RequestCompression.builder().encodings("gzip").isStreaming(false).build());
        assertThat(features("PutStatic").requestCompression()).isNull();
    }

    @Test
    void features_httpChecksum_readsTheTraitAndTheInputMembers() {
        DynamicOperationMetadata features = features("PutChecksummed");

        HttpChecksum withMembers = features.httpChecksum(input("PutChecksummed", map("checksumAlgorithm", "CRC32C",
                                                                                     "checksumMode", "ENABLED")));
        assertThat(withMembers.isRequestChecksumRequired()).isTrue();
        assertThat(withMembers.isRequestStreaming()).isFalse();
        assertThat(withMembers.requestAlgorithm()).isEqualTo("CRC32C");
        assertThat(withMembers.requestAlgorithmHeader()).isEqualTo("x-amz-sdk-checksum-algorithm");
        assertThat(withMembers.requestValidationMode()).isEqualTo("ENABLED");
        // Sorted fastest first, as codegen sorts them.
        assertThat(withMembers.responseAlgorithmsV2()).containsExactly(DefaultChecksumAlgorithm.CRC32C,
                                                                       DefaultChecksumAlgorithm.CRC32,
                                                                       DefaultChecksumAlgorithm.SHA256);

        HttpChecksum withoutMembers = features.httpChecksum(input("PutChecksummed", Document.EMPTY_MAP));
        assertThat(withoutMembers.requestAlgorithm()).isNull();
        assertThat(withoutMembers.requestAlgorithmHeader()).isEqualTo("x-amz-sdk-checksum-algorithm");
        assertThat(withoutMembers.requestValidationMode()).isNull();
        // As in generated clients, the response algorithms are set whenever the trait names a validation mode member;
        // validation itself needs the member's value.
        assertThat(withoutMembers.responseAlgorithmsV2()).isEqualTo(withMembers.responseAlgorithmsV2());

        assertThat(features("PutStatic").httpChecksum(input("PutStatic", Document.EMPTY_MAP))).isNull();
    }

    @Test
    void features_httpChecksumRequired_comesFromTheTrait() {
        assertThat(features("PutChecksumRequired").httpChecksumRequired()).isTrue();
        assertThat(features("PutChecksummed").httpChecksumRequired()).isFalse();
    }

    @Test
    void features_hostPrefix_fillsTheLabels() {
        assertThat(features("PutLabeled").hostPrefix()).isEqualTo("{tenant}-data.");
        assertThat(features("PutLabeled").resolvedHostPrefix(input("PutLabeled", map("tenant", "a-1"))))
            .isEqualTo("a-1-data.");
        assertThat(features("PutStatic").resolvedHostPrefix(input("PutStatic", Document.EMPTY_MAP)))
            .isEqualTo("static.");
        assertThat(features("Ping").hostPrefix()).isNull();
        assertThat(features("Ping").resolvedHostPrefix(input("Ping", Document.EMPTY_MAP))).isNull();
    }

    @Test
    void apply_putsTheGeneratedClientsParameters() {
        DynamicRequest request = input("PutLabeled", map("tenant", "acme"));
        ClientExecutionParams<DynamicRequest, DynamicResponse> params = new ClientExecutionParams<>();

        features("PutLabeled").apply(params, request);

        assertThat(params.hostPrefixExpression()).isEqualTo("acme-data.");
        assertThat(params.credentialType()).isNull();
        assertThat(params.executionAttributes().getAttribute(DynamicOperationMetadata.METADATA))
            .isSameAs(features("PutLabeled"));
        assertThat(params.executionAttributes().getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION)).isNull();
        assertThat(params.executionAttributes().getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM)).isNull();
        assertThat(params.executionAttributes().getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED))
            .isNull();

        ClientExecutionParams<DynamicRequest, DynamicResponse> required = new ClientExecutionParams<>();
        features("PutChecksumRequired").apply(required, input("PutChecksumRequired", Document.EMPTY_MAP));
        assertThat(required.hostPrefixExpression()).isNull();
        assertThat(required.executionAttributes().getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED))
            .isNotNull();

        ClientExecutionParams<DynamicRequest, DynamicResponse> compressed = new ClientExecutionParams<>();
        features("UploadLogs").apply(compressed, input("UploadLogs", Document.EMPTY_MAP));
        assertThat(compressed.executionAttributes().getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION))
            .isEqualTo(features("UploadLogs").requestCompression());
    }

    @Test
    void features_firstAuthSchemeBearer_usesTheTokenCredentialType() {
        Model model = Model.assembler()
                           .discoverModels(Model.class.getClassLoader())
                           .addUnparsedModel("bearer.smithy", BEARER_MODEL)
                           .assemble()
                           .unwrap();
        try (DynamicClient bearer = CallTestClients.client(model, new RecordingHttpClients.Sync(),
                                                           new RecordingHttpClients.Async())) {
            DynamicOperation tokenFirst = bearer.getOperation("TokenFirst");
            DynamicOperation sigv4First = bearer.getOperation("Sigv4First");
            assertThat(tokenFirst.effectiveAuthSchemes()).containsExactly(ShapeId.from("smithy.api#httpBearerAuth"),
                                                                          ShapeId.from("aws.auth#sigv4"));
            assertThat(tokenFirst.metadata().bearerAuthPreferred()).isTrue();
            assertThat(sigv4First.metadata().bearerAuthPreferred()).isFalse();

            ClientExecutionParams<DynamicRequest, DynamicResponse> params = new ClientExecutionParams<>();
            StructDocument empty = StructDocument.coerce(tokenFirst.inputSchema(), Document.EMPTY_MAP,
                                                         bearer.service().schema().id());
            tokenFirst.metadata().apply(params, DynamicRequest.builder().document(empty).build());
            assertThat(params.credentialType()).isEqualTo(CredentialType.TOKEN);
        }
        assertThat(features("PutStatic").bearerAuthPreferred()).isFalse();
    }

    @Test
    void features_isCreatedOncePerOperation() {
        assertThat(features("UploadLogs")).isSameAs(features("UploadLogs"));
    }

    private static DynamicOperationMetadata features(String operation) {
        return client.getOperation(operation).metadata();
    }

    private static DynamicRequest input(String operation, Document value) {
        DynamicOperation op = client.getOperation(operation);
        StructDocument document = StructDocument.coerce(op.inputSchema(), value, client.service().schema().id());
        return DynamicRequest.builder().document(document).build();
    }
}
