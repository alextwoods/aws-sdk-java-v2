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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.dynamicclient.CallTestClients.map;

import java.util.Collections;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * {@link DynamicRequest} and {@link DynamicResponse}: builders, the {@code SerializableStruct} view of the document,
 * equality and {@code toString}.
 */
class DynamicRequestResponseTest {
    private static DynamicClient client;
    private static StructDocument input;
    private static StructDocument otherInput;
    private static StructDocument output;

    @BeforeAll
    static void createDocuments() {
        client = CallTestClients.client(DynamicTestModels.json10(), new RecordingHttpClients.Sync(),
                                        new RecordingHttpClients.Async());
        input = (StructDocument) client.createStruct(ShapeId.from("test.json10#PutItemInput"), map("name", "a"));
        otherInput = (StructDocument) client.createStruct(ShapeId.from("test.json10#PutItemInput"), map("name", "b"));
        output = (StructDocument) client.createStruct(ShapeId.from("test.json10#PutItemOutput"), map("id", "i-1"));
    }

    @AfterAll
    static void closeClient() {
        client.close();
    }

    @Test
    void request_builder_holdsTheDocumentAndTheOverrideConfiguration() {
        AwsRequestOverrideConfiguration override = AwsRequestOverrideConfiguration.builder().putHeader("a", "b").build();

        DynamicRequest request = DynamicRequest.builder().document(input).overrideConfiguration(override).build();

        assertThat(request.document()).isSameAs(input);
        assertThat(request.sdkSchema()).isSameAs(input.sdkSchema());
        assertThat(request.overrideConfiguration()).hasValue(override);
        assertThat(request.getMemberValue(input.sdkSchema().member("name")).toString()).isEqualTo("a");
        assertThat(request.sdkFields()).isEmpty();
        assertThat(request.toBuilder().build()).isEqualTo(request);
        assertThat(request.toBuilder().document(otherInput).build().document()).isSameAs(otherInput);
        assertThat(request).hasToString("DynamicRequest(Input=test.json10#PutItemInput, Document=Document(structure: "
                                        + "{name=Document(string: a)}))");
    }

    @Test
    void request_withoutDocument_fails() {
        assertThatThrownBy(() -> DynamicRequest.builder().build()).isInstanceOf(NullPointerException.class)
                                                                   .hasMessageContaining("document");
    }

    @Test
    void response_builder_holdsTheDocumentAndTheResponseMetadata() {
        SdkHttpResponse http = SdkHttpResponse.builder().statusCode(200).build();
        AwsResponseMetadata metadata =
            DefaultAwsResponseMetadata.create(Collections.singletonMap("AWS_REQUEST_ID", "r-1"));

        DynamicResponse response = DynamicResponse.builder().document(output).sdkHttpResponse(http)
                                                  .responseMetadata(metadata).build();

        assertThat(response.document()).isSameAs(output);
        assertThat(response.sdkSchema()).isSameAs(output.sdkSchema());
        assertThat(response.sdkHttpResponse()).isSameAs(http);
        assertThat(response.responseMetadata().requestId()).isEqualTo("r-1");
        assertThat(response.toBuilder().build()).isEqualTo(response);
        assertThat(response.sdkFields()).isEmpty();
    }

    @Test
    void response_publicBuilderWithoutDocument_cannotDeserialize() {
        ShapeBuilder<?> builder = (ShapeBuilder<?>) DynamicResponse.builder();

        assertThatThrownBy(builder::sdkSchema).isInstanceOf(IllegalStateException.class)
                                               .hasMessage("This DynamicResponse builder has no schema to deserialize "
                                                           + "with; set the output with document(StructDocument)");
        assertThatThrownBy(() -> DynamicResponse.builder().build()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void request_equalsAndHashCode_coverEveryField() {
        EqualsVerifier.forClass(DynamicRequest.class)
                      .withRedefinedSuperclass()
                      .withPrefabValues(StructDocument.class, input, otherInput)
                      .withPrefabValues(Document.class, input, otherInput)
                      .withNonnullFields("document")
                      .verify();
    }

    @Test
    void response_equalsAndHashCode_coverEveryField() {
        EqualsVerifier.forClass(DynamicResponse.class)
                      .withRedefinedSuperclass()
                      .withPrefabValues(StructDocument.class, input, otherInput)
                      .withPrefabValues(Document.class, input, otherInput)
                      .withNonnullFields("document")
                      .suppress(Warning.NONFINAL_FIELDS)
                      .verify();
    }
}
