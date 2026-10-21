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

package software.amazon.awssdk.dynamicclient;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The Smithy models of the dynamic client tests, assembled with the trait definitions on the class path.
 */
final class DynamicTestModels {
    static final ShapeId WEATHER = ShapeId.from("test.weather#WeatherService");
    static final ShapeId ALPHA = ShapeId.from("test.multi#AlphaService");
    static final ShapeId BETA = ShapeId.from("test.multi#BetaService");
    static final ShapeId ACCOUNT = ShapeId.from("com.amazonaws.account#Account");

    private static final Model WEATHER_MODEL = assemble("weather.smithy");
    private static final Model TWO_SERVICES_MODEL = assemble("two-services.smithy");
    private static final Model NO_SERVICE_MODEL = assemble("no-service.smithy");
    private static final Model JSON10_MODEL = assemble("json10.smithy");
    private static final Model REST_JSON_MODEL = assemble("rest-json.smithy");

    private DynamicTestModels() {
    }

    /**
     * One restJson1 and sigv4 service with the {@code aws.api#service} trait.
     */
    static Model weather() {
        return WEATHER_MODEL;
    }

    /**
     * Two awsJson1_0 services without the {@code aws.api#service} trait, and a structure.
     */
    static Model twoServices() {
        return TWO_SERVICES_MODEL;
    }

    /**
     * A structure and no service.
     */
    static Model noService() {
        return NO_SERVICE_MODEL;
    }

    /**
     * An awsJson1_0 and sigv4 service ({@code test.json10#JsonTen}) whose {@code PutItem} input has a member of every
     * coercion rule, with a resource operation, an operation without input, and operation and service errors.
     */
    static Model json10() {
        return JSON10_MODEL;
    }

    /**
     * A restJson1 and sigv4 service ({@code test.restjson#RestWidgets}) with label, query, header and body members.
     */
    static Model restJson() {
        return REST_JSON_MODEL;
    }

    /**
     * The Smithy JSON AST of the Account prototype service, which is generated from it natively (design A.14).
     */
    static Path accountModel() {
        return Paths.get("..", "..", "services", "account", "model", "model.json");
    }

    private static Model assemble(String file) {
        URL url = DynamicTestModels.class.getResource(file);
        if (url == null) {
            throw new IllegalStateException("Test model " + file + " not found");
        }
        return Model.assembler().discoverModels(Model.class.getClassLoader()).addImport(url).assemble().unwrap();
    }
}
