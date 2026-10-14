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

package software.amazon.awssdk.protocols.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocols.client.http.TestShapes.CODEC;
import static software.amazon.awssdk.protocols.client.http.TestShapes.ERRORS;
import static software.amazon.awssdk.protocols.client.http.TestShapes.GREET;
import static software.amazon.awssdk.protocols.client.http.TestShapes.SERVICE_ID;
import static software.amazon.awssdk.protocols.client.http.TestShapes.cbor;
import static software.amazon.awssdk.protocols.client.http.TestShapes.response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.client.http.TestShapes.ExampleServiceException;
import software.amazon.awssdk.protocols.client.http.TestShapes.FooError;
import software.amazon.awssdk.protocols.client.http.TestShapes.QueueMissing;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.traits.AwsQueryCompatibleTrait;

class HttpErrorDeserializerTest {
    private static final HttpErrorDeserializer JSON_LIKE = deserializer().headerErrorExtractor(AmznErrorHeaderExtractor.INSTANCE)
                                                                         .build();

    private static HttpErrorDeserializer.Builder deserializer() {
        return HttpErrorDeserializer.builder()
                                    .codec(CODEC)
                                    .serviceId(SERVICE_ID)
                                    .baseExceptionBuilder(ExampleServiceException::builder);
    }

    private static ExecutionAttributes context() {
        return new ExecutionAttributes().putAttribute(SdkExecutionAttribute.SERVICE_NAME, "Example")
                                        .putAttribute(SdkExecutionAttribute.TIME_OFFSET, 5);
    }

    @Test
    void modeledErrorFromBodyTypeCarriesMembersAndResponseFields() {
        byte[] body = cbor("__type", "smithy.example#FooError:http://abc.com", "message", "Bad foo", "reason", "because");
        SdkHttpFullResponse response = response(400, body).putHeader("x-amzn-RequestId", "req-1")
                                                          .putHeader("x-amz-id-2", "ext-1")
                                                          .build();

        SdkServiceException error = JSON_LIKE.createError(GREET, context(), ERRORS, response);

        assertThat(error).isExactlyInstanceOf(FooError.class);
        FooError foo = (FooError) error;
        assertThat(foo.reason()).isEqualTo("because");
        assertThat(foo.statusCode()).isEqualTo(400);
        assertThat(foo.requestId()).isEqualTo("req-1");
        assertThat(foo.extendedRequestId()).isEqualTo("ext-1");
        assertThat(foo.awsErrorDetails().errorCode()).isEqualTo("FooError");
        assertThat(foo.awsErrorDetails().errorMessage()).isEqualTo("Bad foo");
        assertThat(foo.awsErrorDetails().serviceName()).isEqualTo("Example");
        assertThat(foo.awsErrorDetails().sdkHttpResponse()).isSameAs(response);
        assertThat(foo.awsErrorDetails().rawResponse().asByteArray()).isEqualTo(body);
        assertThat(foo.getMessage())
            .isEqualTo("Bad foo (Service: Example, Status Code: 400, Request ID: req-1, Extended Request ID: ext-1)");
    }

    @Test
    void codeMemberIsUsedOnlyByTheDefaultParser() {
        byte[] body = cbor("code", "FooError", "Message", "from code");

        SdkServiceException byCode = JSON_LIKE.createError(GREET, context(), ERRORS, response(400, body).build());
        assertThat(byCode).isExactlyInstanceOf(FooError.class);
        assertThat(((AwsServiceException) byCode).awsErrorDetails().errorMessage()).isEqualTo("from code");

        HttpErrorDeserializer typeOnly = deserializer().errorPayloadParser(HttpErrorDeserializer.TYPE_ONLY_ERROR_PAYLOAD_PARSER)
                                                       .build();
        SdkServiceException byType = typeOnly.createError(GREET, context(), ERRORS, response(400, body).build());
        assertThat(byType).isExactlyInstanceOf(ExampleServiceException.class);
        assertThat(((AwsServiceException) byType).awsErrorDetails().errorCode()).isNull();
        assertThat(((AwsServiceException) byType).awsErrorDetails().errorMessage()).isEqualTo("from code");
    }

    @Test
    void errorTypeAndMessageHeadersTakePrecedenceOverTheBody() {
        byte[] body = cbor("__type", "Unknown", "message", "body message", "reason", "r");
        SdkHttpFullResponse response = response(400, body)
            .putHeader("X-Amzn-Errortype", "FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/")
            .putHeader("X-Amzn-Error-Message", "header message")
            .build();

        SdkServiceException error = JSON_LIKE.createError(GREET, context(), ERRORS, response);

        assertThat(error).isExactlyInstanceOf(FooError.class);
        assertThat(((FooError) error).reason()).isEqualTo("r");
        assertThat(((AwsServiceException) error).awsErrorDetails().errorCode()).isEqualTo("FooError");
        assertThat(((AwsServiceException) error).awsErrorDetails().errorMessage()).isEqualTo("header message");
    }

    @Test
    void unknownErrorTypeFallsBackToTheBaseExceptionWithTheRawCode() {
        byte[] body = cbor("__type", "aws.other#NotModeled", "message", "nope");
        SdkHttpFullResponse response = response(503, body).putHeader("x-amz-request-id", "req-2").build();

        SdkServiceException error = JSON_LIKE.createError(GREET, context(), ERRORS, response);

        assertThat(error).isExactlyInstanceOf(ExampleServiceException.class);
        AwsServiceException aws = (AwsServiceException) error;
        assertThat(aws.statusCode()).isEqualTo(503);
        assertThat(aws.requestId()).isEqualTo("req-2");
        assertThat(aws.awsErrorDetails().errorCode()).isEqualTo("NotModeled");
        assertThat(aws.awsErrorDetails().errorMessage()).isEqualTo("nope");
    }

    @Test
    void noDiscriminatorAndNoBodyFallsBackToTheBaseException() {
        SdkServiceException error = JSON_LIKE.createError(GREET, context(), ERRORS, response(500, null).build());

        assertThat(error).isExactlyInstanceOf(ExampleServiceException.class);
        AwsServiceException aws = (AwsServiceException) error;
        assertThat(aws.statusCode()).isEqualTo(500);
        assertThat(aws.requestId()).isNull();
        assertThat(aws.awsErrorDetails().errorCode()).isNull();
        assertThat(aws.awsErrorDetails().errorMessage()).isNull();
        assertThat(aws.awsErrorDetails().rawResponse()).isNull();
    }

    @Test
    void malformedBodyNeverThrowsADeserializationException() {
        byte[] garbage = {(byte) 0xbf, 0x63, 'a'};
        SdkHttpFullResponse withHeader = response(400, garbage).putHeader("X-Amzn-Errortype", "FooError")
                                                               .putHeader("x-amzn-RequestId", "req-3")
                                                               .build();

        SdkServiceException error = JSON_LIKE.createError(GREET, context(), ERRORS, withHeader);

        assertThat(error).isExactlyInstanceOf(ExampleServiceException.class);
        AwsServiceException aws = (AwsServiceException) error;
        assertThat(aws.statusCode()).isEqualTo(400);
        assertThat(aws.requestId()).isEqualTo("req-3");
        assertThat(aws.awsErrorDetails().errorCode()).isEqualTo("FooError");
        assertThat(aws.awsErrorDetails().rawResponse().asByteArray()).isEqualTo(garbage);

        byte[] text = "<html>Service Unavailable</html>".getBytes(StandardCharsets.UTF_8);
        SdkServiceException fromText = JSON_LIKE.createError(GREET, context(), ERRORS, response(503, text).build());
        assertThat(fromText).isExactlyInstanceOf(ExampleServiceException.class);
        assertThat(fromText.statusCode()).isEqualTo(503);
    }

    @Test
    void modeledMemberThatDoesNotDeserializeFallsBackToTheBaseException() {
        byte[] body = cbor("__type", "FooError", "message", "wrong member type", "reason", 42);

        SdkServiceException error = JSON_LIKE.createError(GREET, context(), ERRORS, response(400, body).build());

        assertThat(error).isExactlyInstanceOf(ExampleServiceException.class);
        assertThat(((AwsServiceException) error).awsErrorDetails().errorCode()).isEqualTo("FooError");
        assertThat(((AwsServiceException) error).awsErrorDetails().errorMessage()).isEqualTo("wrong member type");
    }

    @Test
    void unreadableBodyFallsBackToHeaders() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("connection reset");
            }
        };
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                                                          .statusCode(500)
                                                          .putHeader("X-Amzn-Errortype", "FooError")
                                                          .content(AbortableInputStream.create(failing))
                                                          .build();

        SdkServiceException error = JSON_LIKE.createError(GREET, context(), ERRORS, response);

        // The modeled error is still created from the header, with no members.
        assertThat(error).isExactlyInstanceOf(FooError.class);
        assertThat(((FooError) error).reason()).isNull();
        assertThat(((AwsServiceException) error).awsErrorDetails().rawResponse()).isNull();
    }

    @Test
    void registryEntryThatIsNotAnExceptionFallsBackToTheBaseException() {
        byte[] body = cbor("__type", "Greeting", "message", "hi");

        SdkServiceException error = JSON_LIKE.createError(GREET, context(), ERRORS, response(400, body).build());

        assertThat(error).isExactlyInstanceOf(ExampleServiceException.class);
        assertThat(((AwsServiceException) error).awsErrorDetails().errorCode()).isEqualTo("Greeting");
    }

    @Test
    void nullRegistryAlwaysUsesTheBaseException() {
        byte[] body = cbor("__type", "FooError");
        assertThat(JSON_LIKE.createError(GREET, context(), null, response(400, body).build()))
            .isExactlyInstanceOf(ExampleServiceException.class);
    }

    @Test
    void errorOutsideTheOperationsDeclaredErrorsStillResolves() {
        // GREET declares no errors; the service-wide registry still resolves FooError (Requirement 23.7).
        assertThat(GREET.errorSchemas()).isEmpty();
        byte[] body = cbor("__type", "FooError");
        assertThat(JSON_LIKE.createError(GREET, context(), ERRORS, response(400, body).build()))
            .isExactlyInstanceOf(FooError.class);
    }

    @Test
    void customErrorCodesResolveWithTheCodeIndex() {
        byte[] body = cbor("__type", QueueMissing.CODE);
        HttpErrorDeserializer query = deserializer().useErrorCodeIndex(true).build();

        SdkServiceException error = query.createError(GREET, context(), ERRORS, response(400, body).build());

        assertThat(error).isExactlyInstanceOf(QueueMissing.class);
        assertThat(((AwsServiceException) error).awsErrorDetails().errorCode()).isEqualTo(QueueMissing.CODE);
        assertThat(deserializer().build().createError(GREET, context(), ERRORS, response(400, body).build()))
            .isExactlyInstanceOf(ExampleServiceException.class);
    }

    @Test
    void awsQueryCompatibleReadsTheQueryErrorHeaderFirst() {
        HttpErrorDeserializer compatible = deserializer().headerErrorExtractor(AmznErrorHeaderExtractor.INSTANCE)
                                                         .awsQueryCompatible(true)
                                                         .build();

        SdkHttpFullResponse custom = response(400, cbor("__type", "FooError"))
            .putHeader("x-amzn-query-error", QueueMissing.CODE + ";Sender")
            .build();
        SdkServiceException error = compatible.createError(GREET, context(), ERRORS, custom);
        assertThat(error).isExactlyInstanceOf(QueueMissing.class);
        assertThat(((AwsServiceException) error).awsErrorDetails().errorCode()).isEqualTo(QueueMissing.CODE);

        // An unknown query code still reports the query code, but the protocol's own discriminator resolves the type.
        SdkHttpFullResponse unknownCode = response(400, cbor("__type", "FooError", "reason", "r"))
            .putHeader("x-amzn-query-error", "AWS.Unknown;Sender")
            .build();
        SdkServiceException fallback = compatible.createError(GREET, context(), ERRORS, unknownCode);
        assertThat(fallback).isExactlyInstanceOf(FooError.class);
        assertThat(((AwsServiceException) fallback).awsErrorDetails().errorCode()).isEqualTo("AWS.Unknown");

        // Without the trait the header is ignored.
        SdkHttpFullResponse notCompatible = response(400, cbor("__type", "FooError"))
            .putHeader("x-amzn-query-error", QueueMissing.CODE + ";Sender")
            .build();
        assertThat(JSON_LIKE.createError(GREET, context(), ERRORS, notCompatible)).isExactlyInstanceOf(FooError.class);
    }

    @Test
    void protocolSettingsConfigureServiceBaseExceptionAndQueryCompatibility() {
        Schema compatibleService = Schema.createService(SERVICE_ID, AwsQueryCompatibleTrait.INSTANCE);
        ProtocolSettings settings = ProtocolSettings.builder()
                                                    .serviceSchema(compatibleService)
                                                    .baseExceptionBuilder(ExampleServiceException::builder)
                                                    .build();
        HttpErrorDeserializer fromSettings = HttpErrorDeserializer.builder().codec(CODEC).protocolSettings(settings).build();

        SdkHttpFullResponse response = response(400, null).putHeader("x-amzn-query-error", QueueMissing.CODE + ";Sender")
                                                          .build();
        assertThat(fromSettings.createError(GREET, context(), ERRORS, response)).isExactlyInstanceOf(QueueMissing.class);
        assertThat(fromSettings.toString()).contains("awsQueryCompatible=true");

        HttpErrorDeserializer notCompatible =
            HttpErrorDeserializer.builder().codec(CODEC).protocolSettings(ProtocolSettings.builder()
                                                                                          .service(SERVICE_ID)
                                                                                          .build()).build();
        assertThat(notCompatible.createError(GREET, context(), ERRORS, response))
            .isExactlyInstanceOf(SdkServiceException.class);
    }

    @Test
    void plainSdkServiceExceptionBaseKeepsTheCodeInTheMessage() {
        HttpErrorDeserializer plain = HttpErrorDeserializer.builder()
                                                           .codec(CODEC)
                                                           .serviceId(SERVICE_ID)
                                                           .baseExceptionBuilder(SdkServiceException::builder)
                                                           .build();

        SdkServiceException error = plain.createError(GREET, context(), ERRORS,
                                                      response(404, cbor("__type", "NotModeled")).build());

        assertThat(error).isExactlyInstanceOf(SdkServiceException.class);
        assertThat(error.statusCode()).isEqualTo(404);
        assertThat(error.getMessage()).isEqualTo("Service returned error code NotModeled");
    }

    @Test
    void payloadRequestIdTakesPrecedenceOverHeaders() {
        HttpErrorDeserializer xmlLike = deserializer()
            .errorPayloadParser((codec, response, body) -> HttpErrorDeserializer.ErrorPayload.create("FooError", "m", "body-id"))
            .build();
        SdkHttpFullResponse response = response(400, new byte[] {1}).putHeader("x-amzn-RequestId", "header-id").build();

        SdkServiceException error = xmlLike.createError(GREET, context(), ERRORS, response);

        // The body byte is not a CBOR map, so the modeled error falls back; the parsed values are kept.
        assertThat(error.requestId()).isEqualTo("body-id");
        assertThat(((AwsServiceException) error).awsErrorDetails().errorCode()).isEqualTo("FooError");
        assertThat(((AwsServiceException) error).awsErrorDetails().errorMessage()).isEqualTo("m");
    }

    @Test
    void customKnownErrorFactoryPopulatesTheBuilder() {
        HttpErrorDeserializer rest = deserializer()
            .headerErrorExtractor(AmznErrorHeaderExtractor.INSTANCE)
            .knownErrorFactory((context, codec, response, body, builder) ->
                                   builder.setMemberValue(FooError.SCHEMA.member("reason"),
                                                          response.firstMatchingHeader("x-reason").orElse(null)))
            .build();
        SdkHttpFullResponse response = response(400, null).putHeader("X-Amzn-Errortype", "FooError")
                                                          .putHeader("x-reason", "from header")
                                                          .build();

        SdkServiceException error = rest.createError(GREET, context(), ERRORS, response);
        assertThat(error).isExactlyInstanceOf(FooError.class);
        assertThat(((FooError) error).reason()).isEqualTo("from header");
    }

    @Test
    void builderRequiresCodecAndServiceId() {
        assertThatThrownBy(() -> HttpErrorDeserializer.builder().serviceId(SERVICE_ID).build())
            .isInstanceOf(NullPointerException.class).hasMessageContaining("codec");
        assertThatThrownBy(() -> HttpErrorDeserializer.builder().codec(CODEC).build())
            .isInstanceOf(NullPointerException.class).hasMessageContaining("serviceId");
    }

    @Test
    void amznHeaderExtractorReadsHeadersCaseInsensitively() {
        SdkHttpFullResponse response = response(400, null).putHeader("x-amzn-errortype", "Foo")
                                                          .putHeader("X-AMZN-ERROR-MESSAGE", "msg")
                                                          .build();
        assertThat(AmznErrorHeaderExtractor.INSTANCE.errorType(response)).isEqualTo("Foo");
        assertThat(AmznErrorHeaderExtractor.INSTANCE.errorMessage(response)).isEqualTo("msg");
        assertThat(AmznErrorHeaderExtractor.INSTANCE.errorType(response(400, null).build())).isNull();
    }
}
