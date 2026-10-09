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

package software.amazon.awssdk.benchmark.reference.v2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.benchmark.harness.ServiceFixture;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * A wire-test transport: records each request and its body and answers with one fixed fixture response. Shared by the
 * V2 and Racecar reference wire tests (the Racecar module compiles it as a shared test source), which check that a
 * reference SDK sends a fixture's exact bytes without a mock-service process.
 */
public final class CapturingHttpClient implements SdkHttpClient {
    private final List<HttpExecuteRequest> requests = new ArrayList<>();
    private final List<byte[]> bodies = new ArrayList<>();
    private final ServiceFixture.MockResponse response;

    public CapturingHttpClient(ServiceFixture.MockResponse response) {
        this.response = response;
    }

    /** The requests sent, in order. */
    public List<HttpExecuteRequest> requests() {
        return requests;
    }

    /** The request bodies sent, in order (empty for a request without a body). */
    public List<byte[]> bodies() {
        return bodies;
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() {
                requests.add(request);
                bodies.add(request.contentStreamProvider().map(p -> {
                    try (InputStream in = p.newStream()) {
                        return in.readAllBytes();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }).orElse(new byte[0]));
                SdkHttpResponse.Builder headers = SdkHttpResponse.builder().statusCode(response.status())
                                                                 .putHeader("Content-Type", response.contentType());
                response.headers().forEach(headers::putHeader);
                return HttpExecuteResponse.builder()
                    .response(headers.build())
                    .responseBody(AbortableInputStream.create(new ByteArrayInputStream(response.body())))
                    .build();
            }

            @Override
            public void abort() {
            }
        };
    }

    @Override
    public void close() {
    }
}
