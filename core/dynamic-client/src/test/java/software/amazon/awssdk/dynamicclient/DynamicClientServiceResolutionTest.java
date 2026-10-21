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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Requirements 26.2 to 26.4 and 26.6 to 26.9: which service a builder resolves, the errors when it cannot, and the
 * {@link ApiService} of the client.
 */
class DynamicClientServiceResolutionTest {

    @Test
    void build_withoutModel_throwsModelRequired() {
        DynamicClient.Builder builder = builder();

        assertThatThrownBy(builder::build)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("A Smithy model is required");
    }

    @Test
    void build_withModelUnset_throwsModelRequired() {
        DynamicClient.Builder builder = builder().model(DynamicTestModels.weather()).model(null);

        assertThatThrownBy(builder::build)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("A Smithy model is required");
    }

    @Test
    void build_withOneServiceAndNoServiceId_resolvesThatService() {
        try (DynamicClient client = builder().model(DynamicTestModels.weather()).build()) {
            assertThat(client.serviceShape().getId()).isEqualTo(DynamicTestModels.WEATHER);
        }
    }

    @Test
    void build_withNoServices_throwsNoService() {
        DynamicClient.Builder builder = builder().model(DynamicTestModels.noService());

        assertThatThrownBy(builder::build)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("No serviceId is set, and the model contains no services");
    }

    @Test
    void build_withSeveralServicesAndNoServiceId_throwsListingEveryService() {
        DynamicClient.Builder builder = builder().model(DynamicTestModels.twoServices());

        assertThatThrownBy(builder::build)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("No serviceId is set, and the model contains multiple services: "
                        + "[test.multi#AlphaService, test.multi#BetaService]. Set serviceId to one of them.");
    }

    @Test
    void build_withServiceIdAmongSeveralServices_resolvesThatService() {
        try (DynamicClient alpha = builder().model(DynamicTestModels.twoServices())
                                            .serviceId(DynamicTestModels.ALPHA).build();
             DynamicClient beta = builder().model(DynamicTestModels.twoServices())
                                           .serviceId(DynamicTestModels.BETA).build()) {
            assertThat(alpha.serviceShape().getId()).isEqualTo(DynamicTestModels.ALPHA);
            assertThat(beta.serviceShape().getId()).isEqualTo(DynamicTestModels.BETA);
        }
    }

    @Test
    void build_withServiceIdOfTheOnlyService_resolvesThatService() {
        try (DynamicClient client = builder().model(DynamicTestModels.weather())
                                             .serviceId(DynamicTestModels.WEATHER).build()) {
            assertThat(client.serviceShape().getId()).isEqualTo(DynamicTestModels.WEATHER);
        }
    }

    @Test
    void build_withUnknownServiceId_throwsNamingTheId() {
        DynamicClient.Builder builder = builder().model(DynamicTestModels.twoServices())
                                                 .serviceId(ShapeId.from("test.multi#GammaService"));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Service test.multi#GammaService is not in the model. "
                        + "The model contains the services [test.multi#AlphaService, test.multi#BetaService].");
    }

    @Test
    void build_withServiceIdOfANonServiceShape_throwsNamingTheIdAndType() {
        DynamicClient.Builder builder = builder().model(DynamicTestModels.twoServices())
                                                 .serviceId(ShapeId.from("test.multi#NotAService"));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Shape test.multi#NotAService is a structure, not a service. "
                        + "The model contains the services [test.multi#AlphaService, test.multi#BetaService].");
    }

    @Test
    void build_withServiceIdAndNoServices_throwsNamingTheId() {
        DynamicClient.Builder builder = builder().model(DynamicTestModels.noService())
                                                 .serviceId(ShapeId.from("test.none#Lonely"));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Shape test.none#Lonely is a structure, not a service. The model contains no services.");
    }

    @Test
    void service_returnsTheConvertedServiceSchema() {
        try (DynamicClient client = builder().model(DynamicTestModels.weather()).build()) {
            ApiService service = client.service();

            assertThat(service.schema().type()).isEqualTo(ShapeType.SERVICE);
            assertThat(service.schema().id().toString()).isEqualTo("test.weather#WeatherService");
            assertThat(service.schema().isMember()).isFalse();
            assertThat(service.name()).isEqualTo("WeatherService");
            assertThat(service.schema()).isSameAs(client.schemaConverter().getSchema(client.serviceShape()));
            assertThat(client.service()).isSameAs(service);
        }
    }

    @Test
    void service_ofTheSelectedService_hasThatServiceId() {
        try (DynamicClient client = builder().model(DynamicTestModels.twoServices())
                                             .serviceId(DynamicTestModels.BETA).build()) {
            assertThat(client.service().schema().id().toString()).isEqualTo("test.multi#BetaService");
            assertThat(client.service().name()).isEqualTo("BetaService");
        }
    }

    @Test
    void schemaConverter_usesTheModelOfTheBuilder() {
        Model model = DynamicTestModels.weather();
        try (DynamicClient client = builder().model(model).build()) {
            assertThat(client.model()).isSameAs(model);
            assertThat(client.serviceShape()).isSameAs(model.expectShape(DynamicTestModels.WEATHER));
        }
    }

    private static DynamicClient.Builder builder() {
        return DynamicClient.builder()
                            .region(Region.US_WEST_2)
                            .credentialsProvider(TestCredentials.PROVIDER)
                            .httpClient(new StubHttpClients.Sync())
                            .httpClient(new StubHttpClients.Async());
    }
}
