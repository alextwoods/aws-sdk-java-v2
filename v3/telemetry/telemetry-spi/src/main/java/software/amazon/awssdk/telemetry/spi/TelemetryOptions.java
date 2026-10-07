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

package software.amazon.awssdk.telemetry.spi;

import software.amazon.awssdk.core.client.config.ClientOption;

/** The client configuration keys of the telemetry concern. */
public final class TelemetryOptions {
    /**
     * The client's telemetry; absent (the default) when the client has none. Read once when the client runtime is
     * built.
     */
    public static final ClientOption<TelemetryConfiguration> TELEMETRY =
        ClientOption.create("Telemetry", TelemetryConfiguration.class);

    private TelemetryOptions() {
    }
}
