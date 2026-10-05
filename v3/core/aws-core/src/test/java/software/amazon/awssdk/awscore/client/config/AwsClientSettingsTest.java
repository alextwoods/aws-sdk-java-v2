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

package software.amazon.awssdk.awscore.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.ClientConfigurationResolver;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.regions.Region;

/** The region setting resolves from the builder, JVM property, environment and profile, in that order. */
class AwsClientSettingsTest {
    @Test
    void regionResolvesFromEachSourceInPrecedenceOrder() {
        SettingSource jvm = source(Map.of("aws.region", "us-west-1"));
        SettingSource environment = source(Map.of("AWS_REGION", "eu-west-1"));
        SettingSource profile = source(Map.of("region", "ap-south-1"));
        SettingSource none = source(Map.of());

        assertEquals(Region.of("ap-south-1"), region(none, none, profile, null));
        assertEquals(Region.of("eu-west-1"), region(none, environment, profile, null));
        assertEquals(Region.of("us-west-1"), region(jvm, environment, profile, null));
        assertEquals(Region.of("sa-east-1"), region(jvm, environment, profile, Region.of("sa-east-1")));
    }

    @Test
    void noSourceLeavesTheRegionUnset() {
        SettingSource none = source(Map.of());
        SdkClientConfiguration resolved = resolver(none, none, none).resolve(SdkClientConfiguration.builder().build());
        assertTrue(resolved.get(AwsClientOption.AWS_REGION).isEmpty());
    }

    private static Region region(SettingSource jvm, SettingSource environment, SettingSource profile,
                                 Region builderValue) {
        SdkClientConfiguration.Builder values = SdkClientConfiguration.builder();
        if (builderValue != null) {
            values.option(AwsClientOption.AWS_REGION, builderValue);
        }
        return resolver(jvm, environment, profile).resolve(values.build()).get(AwsClientOption.AWS_REGION)
                                                  .orElseThrow();
    }

    private static ClientConfigurationResolver resolver(SettingSource jvm, SettingSource environment,
                                                        SettingSource profile) {
        ConfigurationSources sources = ConfigurationSources.builder().jvmProperties(jvm).environment(environment)
                                                           .profile(profile).build();
        return ClientConfigurationResolver.builder().sources(sources).addSetting(AwsClientSettings.REGION).build();
    }

    private static SettingSource source(Map<String, String> settings) {
        return name -> Optional.ofNullable(settings.get(name));
    }
}
