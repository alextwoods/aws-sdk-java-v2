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

package software.amazon.awssdk.protocols.smithyjava.internal;

import static software.amazon.awssdk.awscore.util.AwsHeader.AWS_REQUEST_ID;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Small pieces shared by the protocol implementations, each copying what v2's own protocol layer does so
 * that only the codec differs.
 */
@SdkInternalApi
public final class SerdeSupport {

    private static final byte[] EMPTY = new byte[0];

    private SerdeSupport() {
    }

    /**
     * The endpoint a marshalled request starts from, resolved the way v2's protocol factories resolve it.
     * The endpoint interceptors replace it later in the pipeline, exactly as they do for v2's marshallers.
     */
    public static URI clientEndpoint(SdkClientConfiguration clientConfiguration) {
        ClientEndpointProvider endpointProvider = clientConfiguration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER);
        if (endpointProvider != null) {
            return endpointProvider.clientEndpoint();
        }
        return clientConfiguration.option(SdkClientOption.ENDPOINT);
    }

    /**
     * The same {@link AwsResponseMetadata} v2's {@code AwsJsonResponseHandler} builds: the request id under
     * {@code AWS_REQUEST_ID}, taken from any of v2's request-id header names, then the first value of every
     * response header.
     */
    public static AwsResponseMetadata responseMetadata(SdkHttpResponse response) {
        return responseMetadata(response, response.firstMatchingHeader(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADERS)
                                                  .orElse(null));
    }

    /**
     * The same {@link AwsResponseMetadata} v2's XML response handlers build, which differ from the JSON one in
     * looking at only the {@code x-amzn-RequestId} spelling. The difference is visible: the map is part of
     * {@code AwsResponse.equals}, and S3 answers with {@code x-amz-request-id}, so taking the broader set here
     * would make every S3 response unequal to the one stock v2 returns.
     */
    public static AwsResponseMetadata xmlResponseMetadata(SdkHttpResponse response) {
        return responseMetadata(response, response.firstMatchingHeader("x-amzn-RequestId").orElse(null));
    }

    private static AwsResponseMetadata responseMetadata(SdkHttpResponse response, String requestId) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(AWS_REQUEST_ID, requestId);
        response.forEachHeader((key, value) -> metadata.put(key, value.get(0)));
        return DefaultAwsResponseMetadata.create(metadata);
    }

    /**
     * Reads a response body to the end. Reading to EOF is load-bearing, not just convenient: v2 wraps the
     * DynamoDB response stream in a CRC32-checking stream that validates only once it sees the end.
     */
    public static byte[] readAll(InputStream in) throws IOException {
        if (in == null) {
            return EMPTY;
        }
        return in.readAllBytes();
    }

    /**
     * {@link #readAll(InputStream)}, sized from the response's {@code Content-Length} when it has one: one
     * array of exactly the body's size instead of a growing buffer and a final copy. Still reads to EOF, and
     * falls back to a plain read when the header is absent, unparseable or wrong.
     */
    public static byte[] readAll(InputStream in, SdkHttpResponse response) throws IOException {
        if (in == null) {
            return EMPTY;
        }
        long length = response.firstMatchingHeader("Content-Length").map(SerdeSupport::parseLength).orElse(-1L);
        if (length < 0 || length > Integer.MAX_VALUE - 8) {
            return in.readAllBytes();
        }
        byte[] body = in.readNBytes((int) length);
        int next = in.read();
        if (body.length == length && next == -1) {
            return body;
        }
        // Shorter or longer than advertised: return what is actually there, still read to the end.
        byte[] rest = next == -1 ? EMPTY : in.readAllBytes();
        byte[] all = new byte[body.length + (next == -1 ? 0 : 1) + rest.length];
        System.arraycopy(body, 0, all, 0, body.length);
        if (next != -1) {
            all[body.length] = (byte) next;
            System.arraycopy(rest, 0, all, body.length + 1, rest.length);
        }
        return all;
    }

    private static long parseLength(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    /** The bytes of a codec's output, without a copy when the buffer is exactly its backing array. */
    public static byte[] bytes(ByteBuffer buffer) {
        if (buffer.hasArray() && buffer.arrayOffset() == 0 && buffer.position() == 0
            && buffer.remaining() == buffer.array().length) {
            return buffer.array();
        }
        byte[] out = new byte[buffer.remaining()];
        buffer.duplicate().get(out);
        return out;
    }
}
