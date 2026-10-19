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

package software.amazon.awssdk.services.route53;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * The request's query parameters and URI labels with reserved characters. Task 9.4 sends the requests through
 * {@link Route53Client} and reads them at {@code beforeTransmission}, because the schema path generates no
 * {@code transform} marshallers; the assertions are the ones the test made on the marshallers' output.
 */
public class QueryParamBindingTest {
    private static final String NAMESPACE = " xmlns=\"https://route53.amazonaws.com/doc/2013-04-01/\"";

    /**
     * Make sure the marshaller is able to handle @UriLabel parameter values
     * containing special characters.
     */
    @Test
    public void testReservedCharInParamValue() {
        final String VALUE_WITH_SEMICOLON = ";foo";
        final String VALUE_WITH_AMPERSAND = "&bar";
        final String VALUE_WITH_QUESTION_MARK = "?charlie";

        SdkHttpRequest httpReq_List = send(
            "<ListHealthChecksResponse" + NAMESPACE + "><HealthChecks/><IsTruncated>false</IsTruncated>"
            + "<MaxItems>1</MaxItems></ListHealthChecksResponse>",
            c -> c.listHealthChecks(r -> r.marker(VALUE_WITH_SEMICOLON).maxItems(VALUE_WITH_AMPERSAND)));

        assertEquals("/2013-04-01/healthcheck", httpReq_List.encodedPath());

        Map<String, List<String>> queryParams = httpReq_List.rawQueryParameters();
        assertEquals(2, queryParams.size());
        assertEquals(VALUE_WITH_SEMICOLON, queryParams.get("marker").get(0));
        assertEquals(VALUE_WITH_AMPERSAND, queryParams.get("maxitems").get(0));

        SdkHttpRequest httpReq_GetFailure = send(
            "<GetHealthCheckLastFailureReasonResponse" + NAMESPACE + "><HealthCheckObservations/>"
            + "</GetHealthCheckLastFailureReasonResponse>",
            c -> c.getHealthCheckLastFailureReason(r -> r.healthCheckId(VALUE_WITH_QUESTION_MARK)));
        // parameter value should be URL encoded
        assertEquals(
                "/2013-04-01/healthcheck/%3Fcharlie/lastfailurereason",
                httpReq_GetFailure.encodedPath());

        queryParams = httpReq_GetFailure.rawQueryParameters();
        assertEquals(0, queryParams.size());
    }

    private static SdkHttpRequest send(String responseBody, Consumer<Route53Client> call) {
        AtomicReference<SdkHttpRequest> sent = new AtomicReference<>();
        MockSyncHttpClient http = new MockSyncHttpClient();
        http.stubResponses(HttpExecuteResponse.builder()
                                              .response(SdkHttpResponse.builder().statusCode(200).build())
                                              .responseBody(AbortableInputStream.create(
                                                  new StringInputStream(responseBody)))
                                              .build());
        ExecutionInterceptor recorder = new ExecutionInterceptor() {
            @Override
            public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attributes) {
                sent.set(context.httpRequest());
            }
        };
        try (Route53Client client =
                 Route53Client.builder()
                              .region(Region.AWS_GLOBAL)
                              .endpointOverride(URI.create("http://localhost"))
                              .credentialsProvider(StaticCredentialsProvider.create(
                                  AwsBasicCredentials.create("ak", "sk")))
                              .httpClient(http)
                              .overrideConfiguration(o -> o.addExecutionInterceptor(recorder))
                              .build()) {
            call.accept(client);
        }
        return sent.get();
    }
}
