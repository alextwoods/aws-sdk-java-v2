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

import software.amazon.awssdk.core.client.config.ClientSetting;
import software.amazon.awssdk.regions.Region;

/**
 * The externally configurable settings of every AWS client, resolved once at client construction by
 * {@link software.amazon.awssdk.core.client.config.ClientConfigurationResolver}: the builder value, then the JVM
 * property, the environment variable and the active profile, as V2 names them.
 */
public final class AwsClientSettings {
    /** {@link AwsClientOption#AWS_REGION}: {@code aws.region}, {@code AWS_REGION}, profile {@code region}. */
    public static final ClientSetting<Region> REGION = ClientSetting.builder(AwsClientOption.AWS_REGION)
                                                                    .jvmProperty("aws.region")
                                                                    .environmentVariable("AWS_REGION")
                                                                    .profileProperty("region")
                                                                    .parser(Region::of)
                                                                    .build();

    private AwsClientSettings() {
    }
}
