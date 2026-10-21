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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.dynamicclient.schema.SchemaConverter;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * {@link DocumentException} and its builder, without a call (Requirements 29.1 and 29.2).
 */
class DocumentExceptionTest {
    private static final SchemaConverter CONVERTER = converter();
    private static final Schema RECORD_INVALID = schema("RecordInvalid");
    private static final Schema SERVICE_FAULT = schema("ServiceFault");
    private static final Schema TRY_AGAIN = schema("TryAgain");
    private static final Schema THROTTLED = schema("Throttled");

    @Test
    void build_membersAndResponseValues_areKept() {
        DocumentException e = DocumentException.builder(RECORD_INVALID)
                                               .statusCode(400)
                                               .requestId("req")
                                               .extendedRequestId("ext")
                                               .awsErrorDetails(AwsErrorDetails.builder()
                                                                               .errorCode("RecordInvalid")
                                                                               .serviceName("ErrorJson")
                                                                               .build())
                                               .message("bad")
                                               .build();

        assertThat(e).isInstanceOf(AwsServiceException.class);
        assertThat(e.getContents().getDiscriminator()).isEqualTo(ShapeId.from("test.errors.json#RecordInvalid"));
        assertThat(e.getContents().getMemberNames()).isEmpty();
        assertThat(e.sdkSchema()).isSameAs(RECORD_INVALID);
        assertThat(e.statusCode()).isEqualTo(400);
        assertThat(e.requestId()).isEqualTo("req");
        assertThat(e.extendedRequestId()).isEqualTo("ext");
        assertThat(e.awsErrorDetails().errorCode()).isEqualTo("RecordInvalid");
        assertThat(e.getMessage()).isEqualTo("bad (Service: ErrorJson, Status Code: 400, Request ID: req, "
                                             + "Extended Request ID: ext)");
    }

    @Test
    void message_withoutMessage_isTheMessageMemberInAnyCase() {
        DocumentException.Builder lower = DocumentException.builder(RECORD_INVALID);
        lower.setMemberValue(RECORD_INVALID.member("message"), "from member");
        DocumentException.Builder upper = DocumentException.builder(SERVICE_FAULT);
        upper.setMemberValue(SERVICE_FAULT.member("Message"), "upper");

        assertThat(lower.message()).isEqualTo("from member");
        assertThat(upper.message()).isEqualTo("upper");
        assertThat(lower.message("explicit").message()).isEqualTo("explicit");
        assertThat(DocumentException.builder(RECORD_INVALID).message()).isNull();
        assertThat(lower.build().getContents().getMember("message").asString()).isEqualTo("from member");
    }

    @Test
    void toBuilder_keepsTheTypeContentsAndValues() {
        DocumentException.Builder builder = DocumentException.builder(RECORD_INVALID).statusCode(400).requestId("req");
        builder.setMemberValue(RECORD_INVALID.member("reason"), "too long");
        builder.setMemberValue(RECORD_INVALID.member("fieldCount"), 2);
        DocumentException original = builder.message("bad").build();

        DocumentException copy = original.toBuilder().numAttempts(3).build();

        assertThat(copy).isNotSameAs(original);
        assertThat(copy.getContents()).isSameAs(original.getContents());
        assertThat(copy.sdkSchema()).isSameAs(RECORD_INVALID);
        assertThat(copy.statusCode()).isEqualTo(400);
        assertThat(copy.requestId()).isEqualTo("req");
        assertThat(copy.numAttempts()).isEqualTo(3);
        assertThat(copy.getMessage()).startsWith("bad");
    }

    @Test
    void toBuilder_thenSetMember_keepsTheOtherMembers() {
        DocumentException.Builder builder = DocumentException.builder(RECORD_INVALID);
        builder.setMemberValue(RECORD_INVALID.member("reason"), "too long");
        builder.setMemberValue(RECORD_INVALID.member("fieldCount"), 2);
        DocumentException original = builder.build();

        DocumentException.Builder copy = original.toBuilder();
        copy.setMemberValue(RECORD_INVALID.member("reason"), "changed");
        Document contents = copy.build().getContents();

        assertThat(contents.getMember("reason").asString()).isEqualTo("changed");
        assertThat(contents.getMember("fieldCount").asInteger()).isEqualTo(2);
        assertThat(original.getContents().getMember("reason").asString()).isEqualTo("too long");
    }

    @Test
    void retryableAndThrottling_followTheRetryableTrait() {
        DocumentException plain = DocumentException.builder(RECORD_INVALID).statusCode(400).build();
        DocumentException retryable = DocumentException.builder(TRY_AGAIN).statusCode(400).build();
        DocumentException throttling = DocumentException.builder(THROTTLED).statusCode(400).build();
        DocumentException tooManyRequests = DocumentException.builder(RECORD_INVALID).statusCode(429).build();

        assertThat(plain.isRetryableException()).isFalse();
        assertThat(plain.isThrottlingException()).isFalse();
        assertThat(retryable.isRetryableException()).isTrue();
        assertThat(retryable.isThrottlingException()).isFalse();
        assertThat(throttling.isRetryableException()).isTrue();
        assertThat(throttling.isThrottlingException()).isTrue();
        assertThat(tooManyRequests.isThrottlingException()).isTrue();
    }

    @Test
    void serializableStruct_delegatesToTheContents() {
        DocumentException.Builder builder = DocumentException.builder(RECORD_INVALID);
        builder.setMemberValue(RECORD_INVALID.member("reason"), "too long");
        DocumentException e = builder.build();

        Document document = Document.of(e);

        assertThat(document.getDiscriminator()).isEqualTo(ShapeId.from("test.errors.json#RecordInvalid"));
        assertThat(document.getMemberNames()).containsExactly("reason");
        assertThat(document.getMember("reason").asString()).isEqualTo("too long");
        assertThat((String) e.getMemberValue(RECORD_INVALID.member("reason"))).isEqualTo("too long");
    }

    @Test
    void builder_schemaThatIsNotAnErrorStructure_fails() {
        assertThatThrownBy(() -> DocumentException.builder(schema("Details")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A DocumentException needs the schema of a structure with the smithy.api#error trait, but "
                        + "test.errors.json#Details is a structure without it");
        assertThatThrownBy(() -> DocumentException.builder(schema("Tags")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageEndingWith("test.errors.json#Tags is a list");
        assertThatThrownBy(() -> DocumentException.builder(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void javaSerialization_keepsTheExceptionValuesButNotTheContents() throws Exception {
        DocumentException.Builder builder = DocumentException.builder(RECORD_INVALID).statusCode(400).message("bad");
        builder.setMemberValue(RECORD_INVALID.member("reason"), "too long");
        DocumentException e = builder.build();

        DocumentException copy = roundTrip(e);

        assertThat(copy.statusCode()).isEqualTo(400);
        assertThat(copy.getMessage()).isEqualTo("bad");
        assertThat(copy.getContents()).isNull();
        assertThat(copy.sdkSchema()).isNull();
        assertThat(copy.isRetryableException()).isFalse();
        assertThatThrownBy(copy::toBuilder)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("deserialized DocumentException cannot be copied");
    }

    private static SchemaConverter converter() {
        Model model = DynamicTestModels.errorsJson10();
        return new SchemaConverter(model, model.expectShape(
            software.amazon.smithy.model.shapes.ShapeId.from("test.errors.json#ErrorJson"), ServiceShape.class));
    }

    private static Schema schema(String name) {
        return CONVERTER.getSchema(software.amazon.smithy.model.shapes.ShapeId.fromParts("test.errors.json", name));
    }

    private static DocumentException roundTrip(DocumentException e) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(e);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (DocumentException) in.readObject();
        }
    }
}
