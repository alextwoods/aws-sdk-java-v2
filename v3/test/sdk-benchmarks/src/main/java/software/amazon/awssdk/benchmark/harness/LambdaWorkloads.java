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

package software.amazon.awssdk.benchmark.harness;

import static software.amazon.awssdk.benchmark.harness.FiniteWorkloads.response;
import static software.amazon.awssdk.benchmark.harness.FiniteWorkloads.utf8;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import software.amazon.awssdk.benchmark.harness.FiniteWorkloads.Invoker;
import software.amazon.awssdk.benchmark.harness.FiniteWorkloads.PaddedFixture;
import software.amazon.awssdk.benchmark.harness.FiniteWorkloads.Route;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException;
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationRequest;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The restJson1 fixture: Lambda {@code UpdateFunctionConfiguration} ({@code PUT} with an HTTP label and a JSON body).
 * The function {@code Description} is the pad; the success response, the function configuration, echoes it. Error
 * cells get the modeled {@code ResourceNotFoundException} (404); retry cells the modeled, retryable
 * {@code ServiceException} (500).
 */
final class LambdaWorkloads extends FiniteWorkloads.Provider {
    static final String FUNCTION = "benchmark-function";
    static final String CONTENT_TYPE = "application/json";

    LambdaWorkloads() {
        super("lambda-updatefunctionconfiguration", BenchProtocol.REST_JSON_1, "lambda",
              "com.amazonaws.lambda#AWSGirApiService", "UpdateFunctionConfiguration",
              "com.amazonaws.lambda#ResourceNotFoundException");
    }

    @Override
    public PaddedFixture fixture(BenchmarkCell cell) {
        return new PaddedFixture(
            cell.payload(), Route.path("PUT", "/2015-03-31/functions/" + FUNCTION + "/configuration"), 1,
            pad -> utf8("{\"Description\":\"" + pad + "\"}"),
            pad -> response(200, CONTENT_TYPE, "{\"FunctionName\":\"" + FUNCTION + "\",\"Description\":\"" + pad + "\"}"),
            response(404, CONTENT_TYPE, utf8("{\"Type\":\"User\",\"message\":\"Function not found\"}"),
                     Map.of("X-Amzn-ErrorType", "ResourceNotFoundException")),
            response(500, CONTENT_TYPE, utf8("{\"Type\":\"Service\",\"message\":\"injected retry\"}"),
                     Map.of("X-Amzn-ErrorType", "ServiceException")));
    }

    @Override
    Invoker generated(boolean async, PaddedFixture fixture, URI endpoint, AutoCloseable http) {
        UpdateFunctionConfigurationRequest request = UpdateFunctionConfigurationRequest.builder().functionName(FUNCTION)
                                                                                       .description(fixture.pad())
                                                                                       .build();
        Predicate<Throwable> error = t -> t instanceof ResourceNotFoundException;
        Function<Object, String> echo = r -> ((FunctionConfiguration) r).description();
        if (async) {
            LambdaAsyncClient client = LambdaAsyncClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkAsyncHttpClient) http).build();
            return Invoker.async(client, () -> client.updateFunctionConfiguration(request), error, echo);
        }
        LambdaClient client = LambdaClient.builder().region(Region.US_EAST_1)
            .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
            .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkSyncHttpClient) http).build();
        return Invoker.sync(client, () -> client.updateFunctionConfiguration(request), error, echo);
    }

    @Override
    Document dynamicInput(PaddedFixture fixture) {
        Map<String, Document> input = new LinkedHashMap<>();
        input.put("FunctionName", Document.of(FUNCTION));
        input.put("Description", Document.of(fixture.pad()));
        return Document.of(input);
    }

    @Override
    String dynamicEcho(Document output) {
        return output.getMember("Description").asString();
    }
}
