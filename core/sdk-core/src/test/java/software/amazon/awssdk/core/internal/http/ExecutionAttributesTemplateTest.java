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

package software.amazon.awssdk.core.internal.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.AttributeMap;

class ExecutionAttributesTemplateTest {
    private static final ExecutionAttribute<String> EXTRA = new ExecutionAttribute<>("ExecutionAttributesTemplateTest.Extra");

    private final ClientProtocol<?, ?> protocol = new ApiCallStagesTestSupport.HttpStubProtocol();
    private final ClientEndpointProvider endpointProvider =
        ClientEndpointProvider.create(ApiCallStagesTestSupport.CLIENT_ENDPOINT, false);
    private final Map<String, AuthScheme<?>> authSchemes = Collections.emptyMap();
    private final SdkClient sdkClient = mock(SdkClient.class);
    private final ServiceConfiguration serviceConfiguration = mock(ServiceConfiguration.class);
    private final AttributeMap contextParams = AttributeMap.empty();
    private final java.util.function.Supplier<ProfileFile> profileFile = ProfileFile::defaultProfileFile;

    private SdkClientConfiguration config() {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.SERVICE_CONFIGURATION, serviceConfiguration)
                                     .option(SdkClientOption.SERVICE_NAME, "Thing")
                                     .option(SdkClientOption.CLIENT_TYPE, ClientType.SYNC)
                                     .option(SdkClientOption.PROFILE_FILE_SUPPLIER, profileFile)
                                     .option(SdkClientOption.PROFILE_NAME, "default")
                                     .option(SdkClientOption.NEW_RETRIES_2026_ENABLED, true)
                                     .option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER, endpointProvider)
                                     .option(SdkClientOption.CLIENT_CONTEXT_PARAMS, contextParams)
                                     .option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true)
                                     .option(SdkClientOption.SDK_CLIENT, sdkClient)
                                     .option(SdkClientOption.SIGNER_OVERRIDDEN, false)
                                     .option(SdkClientOption.REQUEST_CHECKSUM_CALCULATION,
                                             RequestChecksumCalculation.WHEN_REQUIRED)
                                     .option(SdkClientOption.RESPONSE_CHECKSUM_VALIDATION,
                                             ResponseChecksumValidation.WHEN_SUPPORTED)
                                     .option(SdkClientOption.AUTH_SCHEMES, authSchemes)
                                     .option(SdkClientOption.CLIENT_PROTOCOL, protocol)
                                     .build();
    }

    @Test
    void newExecutionAttributes_holdTheClientLevelValues() {
        TypeRegistry errors = TypeRegistry.empty();
        ExecutionAttributesTemplate template = ExecutionAttributesTemplate.builder(config()).errorRegistry(errors).build();

        ExecutionAttributes attributes = template.newExecutionAttributes();

        assertThat(attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL)).isSameAs(protocol);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.ERROR_REGISTRY)).isSameAs(errors);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.SERVICE_CONFIG)).isSameAs(serviceConfiguration);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME)).isEqualTo("Thing");
        assertThat(attributes.getAttribute(SdkExecutionAttribute.CLIENT_TYPE)).isEqualTo(ClientType.SYNC);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.PROFILE_FILE_SUPPLIER)).isSameAs(profileFile);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.PROFILE_NAME)).isEqualTo("default");
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.NEW_RETRIES_2026_ENABLED)).isTrue();
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER)).isSameAs(endpointProvider);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT))
            .isEqualTo(ApiCallStagesTestSupport.CLIENT_ENDPOINT);
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS)).isSameAs(contextParams);
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION)).isTrue();
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.SDK_CLIENT)).isSameAs(sdkClient);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.SIGNER_OVERRIDDEN)).isFalse();
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.REQUEST_CHECKSUM_CALCULATION))
            .isEqualTo(RequestChecksumCalculation.WHEN_REQUIRED);
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION))
            .isEqualTo(ResponseChecksumValidation.WHEN_SUPPORTED);
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES)).isSameAs(authSchemes);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.PROFILE_FILE)).isNull();
        assertThat(attributes.getAttribute(SdkExecutionAttribute.API_OPERATION)).isNull();
    }

    @Test
    void newExecutionAttributes_areIndependentMutableCopies() {
        ExecutionAttributesTemplate template = ExecutionAttributesTemplate.builder(config()).build();

        ExecutionAttributes first = template.newExecutionAttributes();
        first.putAttribute(SdkExecutionAttribute.SERVICE_NAME, "changed");
        ExecutionAttributes second = template.newExecutionAttributes();

        assertThat(first).isNotSameAs(second);
        assertThat(second.getAttribute(SdkExecutionAttribute.SERVICE_NAME)).isEqualTo("Thing");
    }

    @Test
    void applyTo_overwritesOverrideValuesAsTheLegacyHandler() {
        ExecutionAttributesTemplate template = ExecutionAttributesTemplate.builder(config()).build();
        ExecutionAttributes call = new ExecutionAttributes();
        call.putAttribute(SdkExecutionAttribute.SERVICE_NAME, "from-override");
        call.putAttribute(EXTRA, "kept");

        template.applyTo(call);

        assertThat(call.getAttribute(SdkExecutionAttribute.SERVICE_NAME)).isEqualTo("Thing");
        assertThat(call.getAttribute(EXTRA)).isEqualTo("kept");
    }

    @Test
    void builder_putAddsAndReplacesValues() {
        ExecutionAttributesTemplate template = ExecutionAttributesTemplate.builder(config())
                                                                          .put(EXTRA, "first")
                                                                          .put(EXTRA, "second")
                                                                          .put(SdkExecutionAttribute.SERVICE_NAME, "Other")
                                                                          .build();

        ExecutionAttributes attributes = template.newExecutionAttributes();

        assertThat(attributes.getAttribute(EXTRA)).isEqualTo("second");
        assertThat(attributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME)).isEqualTo("Other");
        assertThat(template.attributes()).containsOnlyOnce(EXTRA, SdkExecutionAttribute.SERVICE_NAME);
    }

    @Test
    void errorRegistry_nullLeavesTheAttributeToTheCall() {
        ExecutionAttributesTemplate template = ExecutionAttributesTemplate.builder(config()).errorRegistry(null).build();

        assertThat(template.attributes()).doesNotContain(SdkExecutionAttribute.ERROR_REGISTRY);
    }

    @Test
    void template_readsTheConfigurationOnlyWhenBuilt() {
        SdkClientConfiguration.Builder config = config().toBuilder();
        ExecutionAttributesTemplate template = ExecutionAttributesTemplate.builder(config.build()).build();
        config.option(SdkClientOption.SERVICE_NAME, "Later");

        assertThat(template.newExecutionAttributes().getAttribute(SdkExecutionAttribute.SERVICE_NAME)).isEqualTo("Thing");
    }
}
