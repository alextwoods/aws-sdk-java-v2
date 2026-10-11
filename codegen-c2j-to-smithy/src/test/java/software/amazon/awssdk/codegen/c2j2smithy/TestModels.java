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

package software.amazon.awssdk.codegen.c2j2smithy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test helpers for loading C2J fixtures.
 */
final class TestModels {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestModels() {
    }

    static Path fixtureDirectory(String fixture) {
        URL url = TestModels.class.getResource("/fixtures/" + fixture + "/" + C2jInput.SERVICE_MODEL_FILE);
        if (url == null) {
            throw new IllegalArgumentException("No fixture " + fixture);
        }
        try {
            return Paths.get(url.toURI()).getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    static ConversionResult convertFixture(String fixture) {
        return C2jToSmithyConverter.convert(C2jInput.fromDirectory(fixture, fixtureDirectory(fixture)));
    }

    /**
     * Convert an inline C2J service model. Single quotes in {@code json} are replaced by double quotes.
     */
    static ConversionResult convertJson(String serviceName, String json) {
        return C2jToSmithyConverter.convert(C2jInput.builder()
                                                          .serviceName(serviceName)
                                                          .serviceModel(parse(json))
                                                          .build());
    }

    /**
     * Convert an inline C2J service model with an endpoint rule set. Single quotes are replaced by double quotes.
     */
    static ConversionResult convertJson(String serviceName, String json, String endpointRuleSetJson) {
        return C2jToSmithyConverter.convert(C2jInput.builder()
                                                    .serviceName(serviceName)
                                                    .serviceModel(parse(json))
                                                    .endpointRuleSetModel(parse(endpointRuleSetJson))
                                                    .build());
    }

    static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json.replace('\'', '"'));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
