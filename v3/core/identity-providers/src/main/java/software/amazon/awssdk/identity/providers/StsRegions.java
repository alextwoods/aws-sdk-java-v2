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

package software.amazon.awssdk.identity.providers;

import software.amazon.awssdk.profiles.SystemSettings;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

/** The region STS calls of the role providers use when none is configured. */
final class StsRegions {
    private StsRegions() {
    }

    /** The default region chain's region (without instance metadata), else {@code us-east-1}. */
    static String defaultRegion(SystemSettings settings) {
        try {
            return DefaultAwsRegionProviderChain.builder().settings(settings).build().getRegion().id();
        } catch (RuntimeException e) {
            return "us-east-1";
        }
    }
}
