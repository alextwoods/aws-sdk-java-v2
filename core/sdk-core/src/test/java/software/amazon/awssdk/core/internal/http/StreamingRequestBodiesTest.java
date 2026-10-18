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

package software.amazon.awssdk.core.internal.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestRequest;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestResponse;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.traits.MediaTypeTrait;
import software.amazon.awssdk.schema.traits.RequiresLengthTrait;
import software.amazon.awssdk.schema.traits.RestJson1Trait;
import software.amazon.awssdk.schema.traits.StreamingTrait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.schema.traits.UnsignedPayloadTrait;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Tests for {@link StreamingRequestBodies}: the headers of the legacy {@code StreamingRequestMarshaller} and
 * {@code AsyncStreamingRequestMarshaller}, with the build-time flags read from the schemas (Requirement 14.1).
 */
class StreamingRequestBodiesTest {
    private static final String NS = "com.example";
    private static final Schema BLOB_STREAM = Schema.createBlob(ShapeId.fromParts(NS, "Stream"), StreamingTrait.INSTANCE);
    private static final TestRequest REQUEST = new TestRequest("name");

    @Test
    void bind_syncBody_isTheRequestContentWithItsLengthAndContentType() throws Exception {
        RequestBody body = RequestBody.fromString("payload");

        SdkHttpFullRequest bound = StreamingRequestBodies.bind(operation(BLOB_STREAM), serialized(), context(body));

        assertThat(bound.contentStreamProvider()).hasValue(body.contentStreamProvider());
        assertThat(IoUtils.toUtf8String(bound.contentStreamProvider().get().newStream())).isEqualTo("payload");
        assertThat(bound.firstMatchingHeader("Content-Length")).hasValue("7");
        assertThat(bound.firstMatchingHeader("Content-Type")).hasValue(body.contentType());
        assertThat(bound.firstMatchingHeader("x-amz-meta")).as("protocol headers are kept").hasValue("kept");
    }

    @Test
    void bind_asyncBody_setsOnlyTheHeaders() {
        AsyncRequestBody body = AsyncRequestBody.fromString("payload");

        SdkHttpFullRequest bound = StreamingRequestBodies.bind(operation(BLOB_STREAM), serialized(), context(body));

        assertThat(bound.contentStreamProvider()).isEmpty();
        assertThat(bound.firstMatchingHeader("Content-Length")).hasValue("7");
        assertThat(bound.firstMatchingHeader("Content-Type")).hasValue(body.contentType());
    }

    @Test
    void bind_contentTypeFromAHeaderMember_isKept() {
        SdkHttpFullRequest serialized = serialized().toBuilder().putHeader("Content-Type", "image/png").build();
        Schema stream = Schema.createBlob(ShapeId.fromParts(NS, "Stream"), StreamingTrait.INSTANCE,
                                          new MediaTypeTrait("text/plain"));

        SdkHttpFullRequest bound = StreamingRequestBodies.bind(operation(stream), serialized,
                                                               context(RequestBody.fromBytes(new byte[1])));

        assertThat(bound.firstMatchingHeader("Content-Type")).hasValue("image/png");
    }

    @Test
    void bind_mediaTypeMember_isTheContentTypeOfABody() {
        Schema stream = Schema.createBlob(ShapeId.fromParts(NS, "Stream"), StreamingTrait.INSTANCE,
                                          new MediaTypeTrait("text/plain"));

        SdkHttpFullRequest bound = StreamingRequestBodies.bind(operation(stream), serialized(),
                                                               context(RequestBody.fromBytes(new byte[1])));

        assertThat(bound.firstMatchingHeader("Content-Type")).hasValue("text/plain");
    }

    @Test
    void bind_bodyWithoutLengthAndRequiresLength_failsAsTheLegacyMarshaller() {
        Schema stream = Schema.createBlob(ShapeId.fromParts(NS, "Stream"), StreamingTrait.INSTANCE,
                                          RequiresLengthTrait.INSTANCE);

        assertThatThrownBy(() -> StreamingRequestBodies.bind(operation(stream), serialized(), context(unknownLength())))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("This API requires Content-Length header to be set. "
                        + "Please set the content length on the RequestBody.");
    }

    @Test
    void bind_bodyWithoutLength_unsignedPayloadIsChunkedUnlessTheServiceSupportsHttp2() {
        SdkHttpFullRequest signed = StreamingRequestBodies.bind(operation(BLOB_STREAM), serialized(),
                                                                context(unknownLength()));
        SdkHttpFullRequest unsigned = StreamingRequestBodies.bind(
            operation(BLOB_STREAM, service(), UnsignedPayloadTrait.INSTANCE), serialized(), context(unknownLength()));
        RestJson1Trait h2 = new RestJson1Trait(Arrays.asList("http/1.1", "h2"), Collections.singletonList("h2"));
        SdkHttpFullRequest unsignedH2 = StreamingRequestBodies.bind(
            operation(BLOB_STREAM, service(h2), UnsignedPayloadTrait.INSTANCE), serialized(), context(unknownLength()));

        assertThat(signed.firstMatchingHeader("Transfer-Encoding")).isEmpty();
        assertThat(signed.firstMatchingHeader("Content-Length")).isEmpty();
        assertThat(unsigned.firstMatchingHeader("Transfer-Encoding")).hasValue("chunked");
        assertThat(unsignedH2.firstMatchingHeader("Transfer-Encoding")).isEmpty();
    }

    @Test
    void bind_withoutStreamingBlobMemberOrBody_returnsTheRequestUnchanged() {
        SdkHttpFullRequest serialized = serialized();
        ApiOperation<TestRequest, TestResponse> noStream = operation(PreludeSchemas.BLOB);

        assertThat(StreamingRequestBodies.bind(noStream, serialized, context(RequestBody.fromString("x"))))
            .isSameAs(serialized);
        assertThat(StreamingRequestBodies.bind(operation(BLOB_STREAM), serialized, InterceptorContext.builder().request(REQUEST).build()))
            .isSameAs(serialized);
    }

    private static RequestBody unknownLength() {
        ContentStreamProvider provider = () -> new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8));
        return RequestBody.fromContentProvider(provider, "application/octet-stream");
    }

    private static SdkHttpFullRequest serialized() {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.PUT)
                                 .uri(URI.create("https://example.com/things"))
                                 .putHeader("x-amz-meta", "kept")
                                 .build();
    }

    private static InterceptorContext context(RequestBody body) {
        return InterceptorContext.builder().request(REQUEST).requestBody(body).build();
    }

    private static InterceptorContext context(AsyncRequestBody body) {
        return InterceptorContext.builder().request(REQUEST).asyncRequestBody(body).build();
    }

    private static Schema service(Trait... traits) {
        return Schema.createService(ShapeId.fromParts(NS, "ThingService"), traits);
    }

    private static ApiOperation<TestRequest, TestResponse> operation(Schema streamTarget) {
        return operation(streamTarget, service());
    }

    private static ApiOperation<TestRequest, TestResponse> operation(Schema streamTarget, Schema service,
                                                                     Trait... operationTraits) {
        Schema input = Schema.structureBuilder(ShapeId.fromParts(NS, "PutThingRequest"))
                             .putMember("name", PreludeSchemas.STRING)
                             .putMember("body", streamTarget)
                             .build();
        List<Trait> traits = Arrays.asList(operationTraits);
        return SimpleApiOperation.<TestRequest, TestResponse>builder()
                                 .schema(Schema.createOperation(ShapeId.fromParts(NS, "PutThing"),
                                                                traits.toArray(new Trait[0])))
                                 .inputSchema(() -> input)
                                 .outputSchema(() -> TestResponse.SCHEMA)
                                 .errorRegistry(TypeRegistry.empty())
                                 .service(() -> service)
                                 .build();
    }
}
