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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.smithy.model.Model;

/**
 * Requirement 26.1: a client built from the single-file Smithy JSON AST of a Prototype_Service_Set service, Account,
 * which is generated natively from the same file.
 */
class DynamicClientAccountModelTest {
    private static Model model;

    @BeforeAll
    static void loadModel() {
        Path path = DynamicTestModels.accountModel();
        Assumptions.assumeTrue(Files.isRegularFile(path), "Account model not found at " + path.toAbsolutePath());
        model = Model.assembler().discoverModels(Model.class.getClassLoader()).addImport(path).assemble().unwrap();
    }

    @Test
    void build_fromTheJsonAst_resolvesTheAccountService() {
        try (DynamicClient client = builder().build()) {
            Schema service = client.service().schema();

            assertThat(service.id().toString()).isEqualTo("com.amazonaws.account#Account");
            assertThat(service.type()).isEqualTo(ShapeType.SERVICE);
            assertThat(client.service().name()).isEqualTo("Account");
            assertThat(service.traits().stream().map(Trait::traitId).map(Object::toString).collect(Collectors.toList()))
                .contains("aws.protocols#restJson1", "aws.auth#sigv4");
        }
    }

    @Test
    void build_fromTheJsonAst_derivesTheGeneratedClientsNames() {
        try (DynamicClient client = builder().build()) {
            SdkClientConfiguration config = client.syncClientConfiguration();

            // The generated Account client: serviceName() "Account", signingName() "account", serviceEndpointPrefix()
            // "account", SERVICE_NAME "account".
            assertThat(config.option(SdkClientOption.SERVICE_NAME)).isEqualTo("Account");
            assertThat(config.option(AwsClientOption.SERVICE_SIGNING_NAME)).isEqualTo("account");
            assertThat(config.option(AwsClientOption.ENDPOINT_PREFIX)).isEqualTo("account");
            assertThat(client.serviceName()).isEqualTo("account");
            assertThat(config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).clientEndpoint())
                .isEqualTo(URI.create("https://account.us-east-1.amazonaws.com"));
        }
    }

    @Test
    void build_withTheServiceId_resolvesTheSameService() {
        try (DynamicClient client = builder().serviceId(DynamicTestModels.ACCOUNT).build()) {
            assertThat(client.serviceShape()).isSameAs(model.expectShape(DynamicTestModels.ACCOUNT));
        }
    }

    private static DynamicClient.Builder builder() {
        return DynamicClient.builder()
                            .model(model)
                            .region(Region.US_EAST_1)
                            .credentialsProvider(TestCredentials.PROVIDER)
                            .httpClient(new StubHttpClients.Sync())
                            .httpClient(new StubHttpClients.Async());
    }
}
