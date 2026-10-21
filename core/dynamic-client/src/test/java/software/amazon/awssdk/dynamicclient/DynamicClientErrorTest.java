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
import static org.assertj.core.api.Assertions.catchThrowable;
import static software.amazon.awssdk.dynamicclient.CallTestClients.map;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.dynamicclient.RecordingHttpClients.Stub;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.smithy.model.Model;

/**
 * Error responses of {@link DynamicClient} calls, end to end through the SDK pipeline to a recording HTTP client, for
 * awsJson1_0, restJson1 and awsQuery (Requirements 29.1 to 29.6).
 */
@Timeout(60)
class DynamicClientErrorTest {
    private static final String RECORD_INVALID_BODY =
        "{\"__type\":\"test.errors.json#RecordInvalid\",\"message\":\"bad record\",\"reason\":\"too long\","
        + "\"fieldCount\":2,\"details\":{\"field\":\"name\",\"at\":1700000000},\"tags\":[\"a\",\"b\"]}";

    private final RecordingHttpClients.Sync sync = new RecordingHttpClients.Sync();
    private final RecordingHttpClients.Async async = new RecordingHttpClients.Async();
    private DynamicClient client;

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void call_awsJson10_operationError_throwsDocumentExceptionWithContentsAndResponseValues() {
        client = client(DynamicTestModels.errorsJson10());
        sync.stub(Stub.of(400, RECORD_INVALID_BODY, "x-amzn-RequestId", "req-1", "x-amz-id-2", "ext-1"));

        DocumentException e = documentException(() -> client.call("PutRecord", map("name", "x")));

        Document contents = e.getContents();
        assertThat(contents).isInstanceOf(StructDocument.class);
        assertThat(contents.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(contents.getDiscriminator()).isEqualTo(ShapeId.from("test.errors.json#RecordInvalid"));
        assertThat(contents.getMemberNames()).containsExactly("message", "reason", "fieldCount", "details", "tags");
        assertThat(contents.getMember("message").asString()).isEqualTo("bad record");
        assertThat(contents.getMember("reason").asString()).isEqualTo("too long");
        assertThat(contents.getMember("fieldCount").type()).isEqualTo(ShapeType.INTEGER);
        assertThat(contents.getMember("fieldCount").asInteger()).isEqualTo(2);
        Document details = contents.getMember("details");
        assertThat(details.getDiscriminator()).isEqualTo(ShapeId.from("test.errors.json#Details"));
        assertThat(details.getMember("at").asTimestamp()).isEqualTo(Instant.ofEpochSecond(1700000000));
        assertThat(contents.getMember("tags").asList()).extracting(Document::asString).containsExactly("a", "b");
        assertThat(e.sdkSchema()).isSameAs(client.getOperation("PutRecord").errorSchemas().get(0));

        assertThat(e.statusCode()).isEqualTo(400);
        assertThat(e.requestId()).isEqualTo("req-1");
        assertThat(e.extendedRequestId()).isEqualTo("ext-1");
        AwsErrorDetails errorDetails = e.awsErrorDetails();
        assertThat(errorDetails.errorCode()).isEqualTo("RecordInvalid");
        assertThat(errorDetails.errorMessage()).isEqualTo("bad record");
        assertThat(errorDetails.serviceName()).isEqualTo("ErrorJson");
        assertThat(errorDetails.sdkHttpResponse().statusCode()).isEqualTo(400);
        assertThat(errorDetails.rawResponse().asUtf8String()).isEqualTo(RECORD_INVALID_BODY);
        assertThat(e.getMessage()).isEqualTo("bad record (Service: ErrorJson, Status Code: 400, Request ID: req-1, "
                                             + "Extended Request ID: ext-1) (SDK Attempt Count: 1)");
        assertThat(e.isRetryableException()).isFalse();
        assertThat(sync.requests()).hasSize(1);
    }

    @Test
    void call_awsJson10_serviceErrorWithRelativeNameInTheHeader_throwsDocumentException() {
        client = client(DynamicTestModels.errorsJson10());
        sync.stub(Stub.of(400, "{\"Message\":\"down\",\"code\":7}",
                          "X-Amzn-Errortype", "ServiceFault:http://internal.amazon.com/coral/com.amazon.coral.service/",
                          "x-amzn-RequestId", "req-2"));

        DocumentException e = documentException(() -> client.call("PutRecord", map("name", "x")));

        assertThat(e.getContents().getDiscriminator()).isEqualTo(ShapeId.from("test.errors.json#ServiceFault"));
        assertThat(e.getContents().getMember("Message").asString()).isEqualTo("down");
        assertThat(e.getContents().getMember("code").asInteger()).isEqualTo(7);
        assertThat(e.awsErrorDetails().errorCode()).isEqualTo("ServiceFault");
        assertThat(e.awsErrorDetails().errorMessage()).isEqualTo("down");
        assertThat(e.statusCode()).isEqualTo(400);
        assertThat(e.requestId()).isEqualTo("req-2");
    }

    @Test
    void call_awsJson10_serviceErrorWithRelativeNameInTheBody_resolvesForEveryOperation() {
        client = client(DynamicTestModels.errorsJson10());
        sync.stub(Stub.of(400, "{\"__type\":\"ServiceFault\",\"Message\":\"down\"}"));

        DocumentException e = documentException(() -> client.call("GetRecord", map("id", "1")));

        assertThat(e.getContents().getDiscriminator()).isEqualTo(ShapeId.from("test.errors.json#ServiceFault"));
        assertThat(e.getMessage()).startsWith("down (Service: ErrorJson, Status Code: 400");
    }

    @Test
    void call_errorListedByTheOperationAndTheService_resolvesOnceToTheOperationEntry() {
        client = client(DynamicTestModels.errorsJson10());
        sync.stub(Stub.of(400, "{\"__type\":\"test.errors.json#SharedFault\",\"message\":\"shared\"}"));

        DocumentException fromOperation = documentException(() -> client.call("PutRecord", map("name", "x")));
        DocumentException fromService = documentException(() -> client.call("GetRecord", map("id", "1")));

        DynamicOperation putRecord = client.getOperation("PutRecord");
        assertThat(putRecord.errorSchemas()).extracting(Schema::id)
                                            .containsExactly(ShapeId.from("test.errors.json#RecordInvalid"),
                                                             ShapeId.from("test.errors.json#SharedFault"),
                                                             ShapeId.from("test.errors.json#TryAgain"),
                                                             ShapeId.from("test.errors.json#ServiceFault"));
        assertThat(fromOperation.sdkSchema()).isSameAs(putRecord.errorSchemas().get(1));
        assertThat(fromService.sdkSchema()).isSameAs(fromOperation.sdkSchema());
        assertThat(fromOperation.getContents().getMember("message").asString()).isEqualTo("shared");
    }

    @Test
    void call_errorOfAnotherOperation_throwsAPlainServiceException() {
        client = client(DynamicTestModels.errorsJson10());
        sync.stub(Stub.of(400, RECORD_INVALID_BODY, "x-amzn-RequestId", "req-3"));

        Throwable thrown = catchThrowable(() -> client.call("GetRecord", map("id", "1")));

        assertPlainServiceException(thrown, 400, "req-3", "RecordInvalid", "bad record");
    }

    @Test
    void call_unknownErrorCode_throwsAPlainServiceException() {
        client = client(DynamicTestModels.errorsJson10());
        sync.stub(Stub.of(400, "{\"__type\":\"test.errors.json#Mystery\",\"message\":\"what\"}",
                          "x-amzn-RequestId", "req-4"));

        Throwable thrown = catchThrowable(() -> client.call("PutRecord", map("name", "x")));

        assertPlainServiceException(thrown, 400, "req-4", "Mystery", "what");
    }

    @Test
    void call_noDiscriminator_throwsAPlainServiceException() {
        client = client(DynamicTestModels.errorsJson10());
        sync.stub(Stub.of(400, "{\"message\":\"no type\"}", "x-amzn-RequestId", "req-5"));

        Throwable thrown = catchThrowable(() -> client.call("PutRecord", map("name", "x")));

        assertPlainServiceException(thrown, 400, "req-5", null, "no type");
    }

    @Test
    void call_retryableError_isRetriedAndStaysADocumentException() {
        client = client(DynamicTestModels.errorsJson10());
        sync.stub(Stub.of(400, "{\"__type\":\"TryAgain\",\"message\":\"later\"}"));

        DocumentException e = documentException(() -> client.call("PutRecord", map("name", "x")));

        assertThat(e.isRetryableException()).isTrue();
        assertThat(e.isThrottlingException()).isFalse();
        assertThat(sync.requests()).hasSizeGreaterThan(1);
        assertThat(e.numAttempts()).isEqualTo(sync.requests().size());
        assertThat(e.getContents().getDiscriminator()).isEqualTo(ShapeId.from("test.errors.json#TryAgain"));
        assertThat(e.getContents().getMember("message").asString()).isEqualTo("later");
        assertThat(e.getMessage()).startsWith("later (Service: ErrorJson, Status Code: 400")
                                  .endsWith("(SDK Attempt Count: " + sync.requests().size() + ")");
    }

    @Test
    void callAsync_operationError_completesExceptionallyWithTheDocumentException() throws Exception {
        client = client(DynamicTestModels.errorsJson10());
        async.stub(Stub.of(400, RECORD_INVALID_BODY, "x-amzn-RequestId", "req-6"));

        CompletableFuture<Document> future = client.callAsync("PutRecord", map("name", "x"));

        Throwable thrown = catchThrowable(() -> future.get(30, TimeUnit.SECONDS));
        assertThat(thrown).isInstanceOf(ExecutionException.class).hasCauseInstanceOf(DocumentException.class);
        DocumentException e = (DocumentException) thrown.getCause();
        assertThat(e.getContents().getDiscriminator()).isEqualTo(ShapeId.from("test.errors.json#RecordInvalid"));
        assertThat(e.getContents().getMember("fieldCount").asInteger()).isEqualTo(2);
        assertThat(e.statusCode()).isEqualTo(400);
        assertThat(e.requestId()).isEqualTo("req-6");
        assertThat(e.awsErrorDetails().errorCode()).isEqualTo("RecordInvalid");
        assertThat(sync.requests()).isEmpty();
    }

    @Test
    void callAsync_unknownErrorCode_completesExceptionallyWithAPlainServiceException() throws Exception {
        client = client(DynamicTestModels.errorsJson10());
        async.stub(Stub.of(400, "{\"__type\":\"Mystery\"}", "x-amzn-RequestId", "req-7"));

        CompletableFuture<Document> future = client.callAsync("PutRecord", map("name", "x"));

        Throwable thrown = catchThrowable(() -> future.get(30, TimeUnit.SECONDS));
        assertThat(thrown).isInstanceOf(ExecutionException.class);
        assertPlainServiceException(thrown.getCause(), 400, "req-7", "Mystery", null);
    }

    @Test
    void call_restJson1_operationError_readsHeaderAndJsonNameMembers() {
        client = client(DynamicTestModels.errorsRestJson());
        sync.stub(Stub.of(404, "{\"message\":\"no widget\",\"retry_after\":5}",
                          "X-Amzn-Errortype", "WidgetNotFound:http://internal.amazon.com/coral/com.amazon.coral/",
                          "X-Widget-Id", "w-9", "x-amzn-RequestId", "req-8"));

        DocumentException e = documentException(() -> client.call("GetWidget", map("id", "w-9")));

        Document contents = e.getContents();
        assertThat(contents.getDiscriminator()).isEqualTo(ShapeId.from("test.errors.rest#WidgetNotFound"));
        assertThat(contents.getMemberNames()).containsExactlyInAnyOrder("message", "widgetId", "retryAfter");
        assertThat(contents.getMember("widgetId").asString()).isEqualTo("w-9");
        assertThat(contents.getMember("retryAfter").asInteger()).isEqualTo(5);
        assertThat(e.statusCode()).isEqualTo(404);
        assertThat(e.requestId()).isEqualTo("req-8");
        assertThat(e.awsErrorDetails().errorCode()).isEqualTo("WidgetNotFound");
        assertThat(e.awsErrorDetails().errorMessage()).isEqualTo("no widget");
        assertThat(e.awsErrorDetails().serviceName()).isEqualTo("ErrorRest");
    }

    @Test
    void callAsync_restJson1_serviceErrorWithAbsoluteIdInTheBody_completesWithTheDocumentException()
        throws Exception {
        client = client(DynamicTestModels.errorsRestJson());
        async.stub(Stub.of(403, "{\"__type\":\"test.errors.rest#AccessDenied\",\"Message\":\"denied\"}",
                           "x-amzn-RequestId", "req-9"));

        CompletableFuture<Document> future = client.callAsync("GetWidget", map("id", "w"));

        Throwable thrown = catchThrowable(() -> future.get(30, TimeUnit.SECONDS));
        assertThat(thrown).hasCauseInstanceOf(DocumentException.class);
        DocumentException e = (DocumentException) thrown.getCause();
        assertThat(e.getContents().getDiscriminator()).isEqualTo(ShapeId.from("test.errors.rest#AccessDenied"));
        assertThat(e.getContents().getMember("Message").asString()).isEqualTo("denied");
        assertThat(e.statusCode()).isEqualTo(403);
        assertThat(e.awsErrorDetails().errorCode()).isEqualTo("AccessDenied");
        assertThat(e.awsErrorDetails().errorMessage()).isEqualTo("denied");
    }

    @Test
    void call_restJson1_unknownErrorCode_throwsAPlainServiceException() {
        client = client(DynamicTestModels.errorsRestJson());
        sync.stub(Stub.of(400, "{\"code\":\"Mystery\",\"message\":\"what\"}", "x-amzn-RequestId", "req-10"));

        Throwable thrown = catchThrowable(() -> client.call("GetWidget", map("id", "w")));

        assertPlainServiceException(thrown, 400, "req-10", "Mystery", "what");
    }

    @Test
    void call_awsQuery_customCodeOfOperationAndServiceErrors_resolvesToTheOperationErrorFirst() {
        client = client(DynamicTestModels.errorsQuery());
        sync.stub(Stub.of(400, queryError("LimitExceeded", "slow down", "<limit>10</limit>", "req-11")));

        DocumentException fromPublish = documentException(() -> client.call("Publish", map("topic", "t")));
        DocumentException fromDescribe = documentException(() -> client.call("Describe"));

        assertThat(fromPublish.getContents().getDiscriminator())
            .isEqualTo(ShapeId.from("test.errors.query#PublishLimit"));
        // XML member names are matched exactly: <Message> is the error message, not the "message" member.
        assertThat(fromPublish.getContents().getMemberNames()).containsExactly("limit");
        assertThat(fromPublish.getContents().getMember("limit").asInteger()).isEqualTo(10);
        assertThat(fromPublish.getMessage()).startsWith("slow down (Service: ErrorQuery, Status Code: 400");
        assertThat(fromPublish.statusCode()).isEqualTo(400);
        assertThat(fromPublish.requestId()).isEqualTo("req-11");
        assertThat(fromPublish.awsErrorDetails().errorCode()).isEqualTo("LimitExceeded");
        assertThat(fromPublish.awsErrorDetails().errorMessage()).isEqualTo("slow down");
        assertThat(fromDescribe.getContents().getDiscriminator())
            .isEqualTo(ShapeId.from("test.errors.query#ServiceLimit"));
        assertThat(fromDescribe.getContents().getMember("Message").asString()).isEqualTo("slow down");
        assertThat(fromDescribe.awsErrorDetails().errorCode()).isEqualTo("LimitExceeded");
    }

    @Test
    void call_awsQuery_unknownCode_throwsAPlainServiceException() {
        client = client(DynamicTestModels.errorsQuery());
        sync.stub(Stub.of(400, queryError("Mystery", "what", "", "req-12")));

        Throwable thrown = catchThrowable(() -> client.call("Publish", map("topic", "t")));

        assertPlainServiceException(thrown, 400, "req-12", "Mystery", "what");
    }

    private DynamicClient client(Model model) {
        return CallTestClients.client(model, sync, async);
    }

    private static String queryError(String code, String message, String members, String requestId) {
        return "<ErrorResponse><Error><Type>Sender</Type><Code>" + code + "</Code><Message>" + message
               + "</Message>" + members + "</Error><RequestId>" + requestId + "</RequestId></ErrorResponse>";
    }

    private static DocumentException documentException(Runnable call) {
        Throwable thrown = catchThrowable(call::run);
        assertThat(thrown).isInstanceOf(DocumentException.class).isInstanceOf(AwsServiceException.class);
        return (DocumentException) thrown;
    }

    private static void assertPlainServiceException(Throwable thrown, int statusCode, String requestId,
                                                    String errorCode, String message) {
        assertThat(thrown).isExactlyInstanceOf(AwsServiceException.class);
        AwsServiceException e = (AwsServiceException) thrown;
        assertThat(e.statusCode()).isEqualTo(statusCode);
        assertThat(e.requestId()).isEqualTo(requestId);
        assertThat(e.awsErrorDetails().errorCode()).isEqualTo(errorCode);
        assertThat(e.awsErrorDetails().errorMessage()).isEqualTo(message);
    }
}
