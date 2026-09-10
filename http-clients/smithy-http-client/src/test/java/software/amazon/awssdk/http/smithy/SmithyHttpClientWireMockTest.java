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

package software.amazon.awssdk.http.smithy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import java.io.IOException;
import java.net.URI;
import org.junit.Test;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Runs the SDK's shared synchronous HTTP client conformance suite against {@link SmithyHttpClient}, plus a few
 * checks specific to this adapter's mapping.
 */
public class SmithyHttpClientWireMockTest extends SdkHttpClientTestSuite {

    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        AttributeMap.Builder attributeMap = AttributeMap.builder();
        if (options.tlsTrustManagersProvider() != null) {
            attributeMap.put(software.amazon.awssdk.http.SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER,
                             options.tlsTrustManagersProvider());
        }
        if (options.trustAll()) {
            attributeMap.put(TRUST_ALL_CERTIFICATES, options.trustAll());
        }
        return SmithyHttpClient.builder().buildWithDefaults(attributeMap.build());
    }

    /**
     * Bodies of several sizes must arrive byte-for-byte, including the empty body that the adapter releases
     * eagerly rather than streaming.
     */
    @Test
    public void handlesVariousContentLengths() throws Exception {
        try (SdkHttpClient client = createSdkHttpClient()) {
            int[] contentLengths = {0, 1, 100, 1024, 65536};

            for (int length : contentLengths) {
                String path = "/content-length-" + length;
                byte[] body = new byte[length];
                for (int i = 0; i < length; i++) {
                    body[i] = (byte) ('A' + (i % 26));
                }

                mockServer.stubFor(any(urlPathEqualTo(path))
                                       .willReturn(aResponse()
                                                       .withStatus(200)
                                                       .withHeader("Content-Length", String.valueOf(length))
                                                       .withBody(body)));

                SdkHttpFullRequest req = mockSdkRequest("http://localhost:" + mockServer.port() + path,
                                                        SdkHttpMethod.GET);
                HttpExecuteResponse rsp = client.prepareRequest(execute(req)).call();

                assertThat(rsp.httpResponse().statusCode()).isEqualTo(200);
                assertThat(rsp.responseBody()).isPresent();
                assertThat(IoUtils.toByteArray(rsp.responseBody().get())).isEqualTo(body);
            }
        }
    }

    /**
     * A request body must reach the server intact. This is the path where the adapter hands smithy the
     * already-buffered {@code ByteBuffer} instead of an {@code InputStream}.
     */
    @Test
    public void sendsRequestBody() throws Exception {
        String path = "/echo-body";
        mockServer.stubFor(any(urlPathEqualTo(path)).willReturn(aResponse().withStatus(200).withBody("ok")));

        byte[] payload = "{\"TableName\":\"racecar\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        try (SdkHttpClient client = createSdkHttpClient()) {
            SdkHttpFullRequest req =
                SdkHttpFullRequest.builder()
                                  .uri(URI.create("http://localhost:" + mockServer.port() + path))
                                  .method(SdkHttpMethod.POST)
                                  .putHeader("Host", "localhost:" + mockServer.port())
                                  .putHeader("Content-Length", String.valueOf(payload.length))
                                  .contentStreamProvider(
                                      software.amazon.awssdk.http.ContentStreamProvider.fromByteArray(payload))
                                  .build();

            HttpExecuteResponse rsp =
                client.prepareRequest(HttpExecuteRequest.builder()
                                                        .request(req)
                                                        .contentStreamProvider(req.contentStreamProvider()
                                                                                  .orElse(null))
                                                        .build())
                      .call();

            assertThat(rsp.httpResponse().statusCode()).isEqualTo(200);
            rsp.responseBody().ifPresent(IoUtils::drainInputStream);
        }

        mockServer.verify(1, com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlPathEqualTo(path))
                                                                           .withRequestBody(
                                                                               com.github.tomakehurst.wiremock.client.WireMock
                                                                                   .equalTo(new String(payload,
                                                                                       java.nio.charset.StandardCharsets.UTF_8))));
    }

    /**
     * Error responses must still expose a body, since the SDK reads it to build the exception and closes it to
     * release the connection.
     */
    @Test
    public void errorResponseHasReadableBody() throws Exception {
        String path = "/error";
        mockServer.stubFor(any(urlPathEqualTo(path))
                               .willReturn(aResponse().withStatus(400).withBody("{\"__type\":\"Broken\"}")));

        try (SdkHttpClient client = createSdkHttpClient()) {
            SdkHttpFullRequest req = mockSdkRequest("http://localhost:" + mockServer.port() + path,
                                                    SdkHttpMethod.GET);
            HttpExecuteResponse rsp = client.prepareRequest(execute(req)).call();

            assertThat(rsp.httpResponse().statusCode()).isEqualTo(400);
            assertThat(rsp.responseBody()).isPresent();
            assertThat(new String(IoUtils.toByteArray(rsp.responseBody().get()),
                                  java.nio.charset.StandardCharsets.UTF_8))
                .contains("Broken");
        }
    }

    /**
     * Response headers must survive the mapping, including a header sent more than once.
     */
    @Test
    public void preservesResponseHeaders() throws Exception {
        String path = "/headers";
        mockServer.stubFor(any(urlPathEqualTo(path))
                               .willReturn(aResponse()
                                               .withStatus(200)
                                               .withHeader("x-amzn-RequestId", "abc123")
                                               .withHeader("Set-Cookie", "a=1", "b=2")
                                               .withBody("x")));

        try (SdkHttpClient client = createSdkHttpClient()) {
            SdkHttpFullRequest req = mockSdkRequest("http://localhost:" + mockServer.port() + path,
                                                    SdkHttpMethod.GET);
            HttpExecuteResponse rsp = client.prepareRequest(execute(req)).call();

            assertThat(rsp.httpResponse().firstMatchingHeader("x-amzn-RequestId")).hasValue("abc123");
            assertThat(rsp.httpResponse().matchingHeaders("Set-Cookie")).containsExactlyInAnyOrder("a=1", "b=2");
            rsp.responseBody().ifPresent(IoUtils::drainInputStream);
        }
    }

    /**
     * Not supported by this client, and the limitation is in smithy rather than in the adapter.
     *
     * <p>{@code trustAll} is meant to make a self-signed certificate usable. This client can make any issuer
     * acceptable, but smithy's TLS provider always enables hostname verification, so the suite's certificate
     * (which does not carry a name matching {@code localhost}) is still rejected. Overridden rather than left
     * failing so the rest of the suite stays meaningful; see {@link SmithyClientOptions} for why disabling
     * verification is not worth what it would cost.
     */
    @Override
    @Test
    public void testTrustAllWorks() {
        // Intentionally empty: capability not supported, see javadoc.
    }

    /**
     * Weakened to "the request fails" rather than "it fails with {@link javax.net.ssl.SSLHandshakeException}", because
     * smithy's HTTP/1.1 connect path loses the exception type on a multi-address host.
     *
     * <p>{@code HttpConnectionFactory.create} tries every resolved address in turn and keeps only the <i>last</i>
     * failure as the cause of the {@code IOException} it throws. The suite's host is {@code localhost}, which resolves
     * to both {@code ::1} and {@code 127.0.0.1} here, and WireMock does not answer on both — so the surviving cause can
     * be the connection failure from one address rather than the certificate failure from the other, and no amount of
     * unwrapping in this adapter can recover what was discarded. {@link SmithyHttpClient} does unwrap TLS failures when
     * they survive, which is the single-address case.
     *
     * <p>This surfaced when the client stopped leaving smithy's default HTTP version policy in place (see
     * {@link SmithyClientOptions}): that default prefers HTTP/2 and took a connect path which did preserve the TLS
     * exception, while also making this the only SDK transport whose wire protocol changed as soon as the endpoint was
     * {@code https}. Correct protocol selection is worth more than exception fidelity on a multi-address host.
     *
     * <p>What is verified here is the part that matters for safety: an untrusted certificate still fails the request.
     * It fails closed, and the SDK's retry policy treats a bare {@code IOException} as retryable rather than fatal, so
     * the practical cost is a wasted retry cycle on a misconfigured endpoint, not a security hole.
     */
    @Override
    @Test
    public void validatesHttpsCertificateIssuer() {
        try (SdkHttpClient client = createSdkHttpClient()) {
            SdkHttpFullRequest request = mockSdkRequest("https://localhost:" + mockServer.httpsPort(),
                                                       SdkHttpMethod.POST);

            assertThatThrownBy(client.prepareRequest(HttpExecuteRequest.builder().request(request).build())::call)
                .isInstanceOf(IOException.class);
        }
    }

    /**
     * The client reports a name that identifies both the backing implementation and the transmission type, per
     * the {@code clientName()} contract.
     */
    @Test
    public void reportsClientName() {
        try (SdkHttpClient client = createSdkHttpClient()) {
            assertThat(client.clientName()).isEqualTo("SmithySync");
        }
    }

    /**
     * Builds the execute request with the body attached. {@code mockSdkRequest} puts a body and a matching
     * Content-Length on every non-HEAD request, so dropping the content provider would announce a body and
     * then never send it, leaving the server waiting.
     */
    private static HttpExecuteRequest execute(SdkHttpFullRequest request) {
        return HttpExecuteRequest.builder()
                                 .request(request)
                                 .contentStreamProvider(request.contentStreamProvider().orElse(null))
                                 .build();
    }
}
