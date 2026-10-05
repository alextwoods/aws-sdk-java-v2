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

package software.amazon.awssdk.http.urlconnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/** Request builders and response readers shared by the transport tests. */
final class TestRequests {
    private TestRequests() {
    }

    static SdkHttpRequest.Builder get(int port, String path) {
        return SdkHttpRequest.builder().method("GET").scheme("http").host("127.0.0.1").port(port).encodedPath(path);
    }

    static SdkHttpRequest.Builder request(String method, String scheme, int port, String path) {
        return SdkHttpRequest.builder().method(method).scheme(scheme).host("127.0.0.1").port(port).encodedPath(path);
    }

    /** Opens the response body stream (EMPTY bodies give an empty stream). */
    static InputStream open(SdkHttpResponse response) {
        HttpBody body = response.body();
        return body.kind() == HttpBody.Kind.STREAMING ? body.streamingBody().openStream()
                                                      : InputStream.nullInputStream();
    }

    /** Reads the whole body and closes the stream. */
    static String readAll(SdkHttpResponse response) {
        try (InputStream in = open(response)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
