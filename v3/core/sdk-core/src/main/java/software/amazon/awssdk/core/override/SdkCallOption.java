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

package software.amazon.awssdk.core.override;

import java.util.List;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.TimeoutConfiguration;

/** The protocol-agnostic {@link CallOption}s. */
public final class SdkCallOption {
    /**
     * Per-call timeouts over {@link SdkClientOption#TIMEOUT_CONFIGURATION}, merged field by field: overriding only
     * {@code apiCallAttemptTimeout} keeps the client's {@code apiCallTimeout}.
     */
    public static final CallOption<TimeoutConfiguration> TIMEOUT_CONFIGURATION =
        CallOption.merging(SdkClientOption.TIMEOUT_CONFIGURATION, SdkCallOption::mergeTimeouts);

    /**
     * The API names a call adds to its user agent ({@code RequestOverrideConfiguration.Builder#addApiName}), appended
     * after the request-embedded override's when an explicit override also adds some.
     */
    public static final CallOption<List<ApiName>> API_NAMES =
        CallOption.appending(SdkClientOption.API_NAMES, ApiName.class);

    private SdkCallOption() {
    }

    private static TimeoutConfiguration mergeTimeouts(TimeoutConfiguration client, TimeoutConfiguration call) {
        if (client == null || client.equals(TimeoutConfiguration.none())) {
            return call;
        }
        return TimeoutConfiguration.builder()
                                   .apiCallTimeout(call.apiCallTimeout().or(client::apiCallTimeout).orElse(null))
                                   .apiCallAttemptTimeout(call.apiCallAttemptTimeout()
                                                              .or(client::apiCallAttemptTimeout).orElse(null))
                                   .build();
    }
}
