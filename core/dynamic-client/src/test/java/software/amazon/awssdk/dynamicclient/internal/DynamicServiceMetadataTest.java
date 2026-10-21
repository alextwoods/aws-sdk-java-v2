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

package software.amazon.awssdk.dynamicclient.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;

/**
 * The names a dynamic client derives from its service, compared with the generated clients of the prototype services
 * (Secrets Manager, Account, SQS), and service resolution of the model's only service.
 */
class DynamicServiceMetadataTest {

    @Test
    void resolve_withAServiceTraitWithSpaces_derivesTheGeneratedNames() {
        DynamicServiceMetadata metadata = resolve("@aws.api#service(sdkId: \"Secrets Manager\", "
                                                  + "endpointPrefix: \"secretsmanager\", arnNamespace: \"secretsmanager\")\n"
                                                  + "@aws.auth#sigv4(name: \"secretsmanager\")\n"
                                                  + "service SecretsManager {}");

        assertThat(metadata.sdkId()).isEqualTo("Secrets Manager");
        assertThat(metadata.serviceName()).isEqualTo("SecretsManager");
        assertThat(metadata.endpointPrefix()).isEqualTo("secretsmanager");
        assertThat(metadata.signingName()).isEqualTo("secretsmanager");
        assertThat(metadata.serviceNameForEnvironmentVariables()).isEqualTo("SECRETS_MANAGER");
        assertThat(metadata.serviceNameForProfileFile()).isEqualTo("secrets_manager");
    }

    @Test
    void resolve_withAnAcronymSdkId_derivesTheGeneratedNames() {
        DynamicServiceMetadata metadata = resolve("@aws.api#service(sdkId: \"SQS\", endpointPrefix: \"sqs\", "
                                                  + "arnNamespace: \"sqs\")\n"
                                                  + "@aws.auth#sigv4(name: \"sqs\")\n"
                                                  + "service AmazonSQS {}");

        assertThat(metadata.serviceName()).isEqualTo("Sqs");
        assertThat(metadata.serviceNameForEnvironmentVariables()).isEqualTo("SQS");
        assertThat(metadata.serviceNameForProfileFile()).isEqualTo("sqs");
    }

    @Test
    void resolve_withRedundantPrefixAndSuffix_removesThemFromTheServiceName() {
        // Smithy rejects such SDK IDs, so the shape name of a service without the trait is the only way in.
        DynamicServiceMetadata metadata = resolve("service AmazonWidgetService {}");

        assertThat(metadata.sdkId()).isEqualTo("AmazonWidgetService");
        assertThat(metadata.serviceName()).isEqualTo("Widget");
    }

    @Test
    void resolve_withoutASigV4Trait_usesTheArnNamespaceAsSigningName() {
        DynamicServiceMetadata metadata = resolve("@aws.api#service(sdkId: \"Thing\", arnNamespace: \"things\")\n"
                                                  + "service Things {}");

        assertThat(metadata.signingName()).isEqualTo("things");
    }

    @Test
    void resolve_withoutAServiceTrait_usesTheShapeName() {
        DynamicServiceMetadata metadata = resolve("service PlainService {}");

        assertThat(metadata.sdkId()).isEqualTo("PlainService");
        assertThat(metadata.serviceName()).isEqualTo("Plain");
        assertThat(metadata.endpointPrefix()).isEqualTo("plainservice");
        assertThat(metadata.signingName()).isEmpty();
        assertThat(metadata.serviceNameForEnvironmentVariables()).isEqualTo("PLAINSERVICE");
    }

    @Test
    void resolve_withoutAModel_throws() {
        assertThatThrownBy(() -> DynamicServiceMetadata.resolve(null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("model");
    }

    private static DynamicServiceMetadata resolve(String service) {
        Model model = Model.assembler()
                           .discoverModels(Model.class.getClassLoader())
                           .addUnparsedModel("test.smithy", "$version: \"2.0\"\nnamespace test.meta\n" + service + "\n")
                           .assemble()
                           .unwrap();
        DynamicServiceMetadata metadata = DynamicServiceMetadata.resolve(model, null);
        assertThat(metadata.service().getId().getNamespace()).isEqualTo("test.meta");
        assertThat(DynamicServiceMetadata.resolve(model, metadata.service().getId()).service())
            .isSameAs(metadata.service());
        return metadata;
    }
}
