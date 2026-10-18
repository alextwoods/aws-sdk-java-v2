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

import static software.amazon.awssdk.http.Header.CHUNKED;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;
import static software.amazon.awssdk.http.Header.TRANSFER_ENCODING;

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.AwsProtocolTrait;
import software.amazon.awssdk.schema.traits.MediaTypeTrait;
import software.amazon.awssdk.schema.traits.Rpcv2ProtocolTrait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Binds the caller's streaming request body to the serialized request of an operation whose input has a streaming blob
 * member (Requirement 14.1, design A.13), as the legacy {@code StreamingRequestMarshaller} and
 * {@code AsyncStreamingRequestMarshaller} wrap a generated marshaller. Generated request classes do not hold the
 * streaming member: the client method takes it as a {@link RequestBody} or {@link AsyncRequestBody}, which the client
 * handler's context factory puts on the interceptor context, so the protocol serializes the other members and this step
 * attaches the body. Nothing is read: the sync body's {@code ContentStreamProvider} becomes the request content, and the
 * async body is sent by the transport from the interceptor context, as on the legacy pipeline.
 *
 * <p>Headers, as the legacy marshallers write them, with the decisions the generated code made at build time read from
 * the schemas:
 * <ul>
 *     <li>{@code Content-Type}: kept when the protocol already set it (a bound header member); otherwise the member's
 *     {@code @mediaType}, and otherwise the body's content type ({@code application/octet-stream} for bytes and
 *     streams, the probed type for a file). {@code @mediaType} is an addition: legacy marshallers ignore it, and no C2J
 *     model carries it.</li>
 *     <li>{@code Content-Length}: kept when already set; otherwise the body's length when it declares one. Without a
 *     length, a {@code @requiresLength} member fails the call with the legacy {@link SdkClientException}, and an
 *     operation with {@code @unsignedPayload} (the C2J {@code v4-unsigned-body} auth type or {@code unsignedPayload})
 *     gets {@code Transfer-Encoding: chunked}, unless the service supports HTTP/2 (a protocol trait lists {@code h2} in
 *     {@code http} or {@code eventStreamHttp}, which is the codegen's {@code supportsH2}).</li>
 * </ul>
 * Signing, checksums and compression then see the request exactly as a legacy marshaller produced it, because the new
 * path runs the same legacy stages over it ({@link RequestMutationStages}, {@link RequestSigning}).
 */
@SdkInternalApi
final class StreamingRequestBodies {
    private static final String H2 = "h2";

    private StreamingRequestBodies() {
    }

    /**
     * Returns the request with the streaming body bound, or the request unchanged when the operation's input has no
     * streaming blob member or the call has no streaming body.
     *
     * @param operation the operation being called
     * @param request the request the protocol serialized
     * @param context the interceptor context before marshalling, holding the call's request body or async request body
     * @return the request to send
     * @throws SdkClientException if the member requires a length and the body does not declare one
     */
    static SdkHttpFullRequest bind(ApiOperation<?, ?> operation, SdkHttpFullRequest request, InterceptorContext context) {
        Schema member = operation.inputStreamMember();
        if (member == null || member.type() != ShapeType.BLOB) {
            return request;
        }
        Optional<AsyncRequestBody> asyncBody = context.asyncRequestBody();
        Optional<RequestBody> syncBody = context.requestBody();
        if (!asyncBody.isPresent() && !syncBody.isPresent()) {
            return request;
        }

        SdkHttpFullRequest.Builder builder = request.toBuilder();
        String bodyContentType;
        Optional<Long> contentLength;
        if (asyncBody.isPresent()) {
            bodyContentType = asyncBody.get().contentType();
            contentLength = asyncBody.get().contentLength();
        } else {
            RequestBody body = syncBody.get();
            builder.contentStreamProvider(body.contentStreamProvider());
            bodyContentType = body.contentType();
            contentLength = body.optionalContentLength();
        }

        if (StringUtils.isEmpty(builder.firstMatchingHeader(CONTENT_TYPE).orElse(null))) {
            MediaTypeTrait mediaType = member.getTrait(TraitKey.MEDIA_TYPE_TRAIT);
            String contentType = mediaType != null ? mediaType.getValue() : bodyContentType;
            if (contentType != null) {
                builder.putHeader(CONTENT_TYPE, contentType);
            }
        }
        addLengthHeaders(builder, operation, member, contentLength);
        return builder.build();
    }

    /**
     * Same as the legacy {@code AbstractStreamingRequestMarshaller.addHeaders}.
     */
    private static void addLengthHeaders(SdkHttpFullRequest.Builder builder,
                                         ApiOperation<?, ?> operation,
                                         Schema member,
                                         Optional<Long> contentLength) {
        if (builder.firstMatchingHeader(CONTENT_LENGTH).isPresent()) {
            return;
        }
        if (contentLength.isPresent()) {
            builder.putHeader(CONTENT_LENGTH, Long.toString(contentLength.get()));
            return;
        }
        if (member.hasTrait(TraitKey.REQUIRES_LENGTH_TRAIT)) {
            throw SdkClientException.create("This API requires Content-Length header to be set. "
                                            + "Please set the content length on the RequestBody.");
        }
        if (operation.schema().hasTrait(TraitKey.UNSIGNED_PAYLOAD_TRAIT) && !supportsHttp2(operation)) {
            builder.putHeader(TRANSFER_ENCODING, CHUNKED);
        }
    }

    private static boolean supportsHttp2(ApiOperation<?, ?> operation) {
        for (Trait trait : operation.service().schema().traits()) {
            if (trait instanceof AwsProtocolTrait) {
                AwsProtocolTrait protocol = (AwsProtocolTrait) trait;
                if (listsH2(protocol.getHttp(), protocol.getEventStreamHttp())) {
                    return true;
                }
            } else if (trait instanceof Rpcv2ProtocolTrait) {
                Rpcv2ProtocolTrait protocol = (Rpcv2ProtocolTrait) trait;
                if (listsH2(protocol.getHttp(), protocol.getEventStreamHttp())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean listsH2(List<String> http, List<String> eventStreamHttp) {
        return http.contains(H2) || eventStreamHttp.contains(H2);
    }
}
