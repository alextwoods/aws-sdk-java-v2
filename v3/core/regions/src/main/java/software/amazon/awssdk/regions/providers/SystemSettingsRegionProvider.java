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

package software.amazon.awssdk.regions.providers;

import java.util.Objects;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.SystemSettings;
import software.amazon.awssdk.regions.Region;

/**
 * The region of the {@code aws.region} JVM system property, else the {@code AWS_REGION} environment variable, with the V2
 * name and precedence.
 */
public final class SystemSettingsRegionProvider implements AwsRegionProvider {
    /** The JVM system property. */
    public static final String REGION_PROPERTY = "aws.region";
    /** The environment variable. */
    public static final String REGION_ENVIRONMENT = "AWS_REGION";

    private final SystemSettings settings;

    private SystemSettingsRegionProvider(SystemSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /** The provider of the process's settings. */
    public static SystemSettingsRegionProvider create() {
        return new SystemSettingsRegionProvider(SystemSettings.system());
    }

    /** The provider of {@code settings}. */
    public static SystemSettingsRegionProvider create(SystemSettings settings) {
        return new SystemSettingsRegionProvider(settings);
    }

    @Override
    public Region getRegion() {
        return settings.value(REGION_PROPERTY, REGION_ENVIRONMENT)
                       .map(value -> Region.of(value.strip()))
                       .orElseThrow(() -> SdkClientException.create(
                           "Unable to load region from system settings. Region must be specified either via environment "
                           + "variable (" + REGION_ENVIRONMENT + ") or system property (" + REGION_PROPERTY + ")."));
    }
}
