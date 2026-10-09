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

import static software.amazon.awssdk.benchmark.harness.PaddedFixture.response;
import static software.amazon.awssdk.benchmark.harness.PaddedFixture.utf8;

import java.util.Map;

/**
 * The SDK-free wire half of the restJson1 fixture (Lambda {@code UpdateFunctionConfiguration}, a {@code PUT} with an
 * HTTP label and a JSON body): the function {@code Description} is the pad; the success response, the function
 * configuration, echoes it. Error cells get the modeled {@code ResourceNotFoundException} (404); retry cells the
 * modeled, retryable {@code ServiceException} (500).
 */
public final class LambdaUpdateFunctionConfigurationFixture {
    public static final String NAME = "lambda-updatefunctionconfiguration";
    public static final String FUNCTION = "benchmark-function";
    public static final String CONTENT_TYPE = "application/json";

    private LambdaUpdateFunctionConfigurationFixture() {
    }

    public static PaddedFixture create(PayloadClass payload) {
        return new PaddedFixture(
            payload, PaddedFixture.Route.path("PUT", "/2015-03-31/functions/" + FUNCTION + "/configuration"), 1,
            pad -> utf8("{\"Description\":\"" + pad + "\"}"),
            pad -> response(200, CONTENT_TYPE, "{\"FunctionName\":\"" + FUNCTION + "\",\"Description\":\"" + pad + "\"}"),
            response(404, CONTENT_TYPE, utf8("{\"Type\":\"User\",\"message\":\"Function not found\"}"),
                     Map.of("X-Amzn-ErrorType", "ResourceNotFoundException")),
            response(500, CONTENT_TYPE, utf8("{\"Type\":\"Service\",\"message\":\"injected retry\"}"),
                     Map.of("X-Amzn-ErrorType", "ServiceException")));
    }
}
