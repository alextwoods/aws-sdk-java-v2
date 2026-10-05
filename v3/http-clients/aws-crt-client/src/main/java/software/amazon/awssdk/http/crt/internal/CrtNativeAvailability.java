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

package software.amazon.awssdk.http.crt.internal;

import java.util.Objects;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.crt.CRT;

/**
 * Whether the AWS CRT native library loads on this platform. Checked when a client is built (never while reading
 * provider metadata), so a missing native library fails that one construction with a
 * {@link SdkClientConfigurationException} naming the implementation and platform, instead of a raw
 * {@link LinkageError}, and never affects the discovery of other HTTP implementations.
 */
public final class CrtNativeAvailability {
    /** Loads the native library through {@link CRT}'s static initializer. */
    public static final Probe DEFAULT_PROBE = CRT::checkLoaded;

    private CrtNativeAvailability() {
    }

    /** Checks the native library; throws when it is unavailable. Replaceable in tests. */
    @FunctionalInterface
    public interface Probe {
        void check() throws Throwable;
    }

    /**
     * Runs {@code probe}.
     *
     * @throws SdkClientConfigurationException if the native library cannot be loaded
     */
    public static void require(Probe probe, String implementationName) {
        Objects.requireNonNull(probe, "probe");
        Throwable failure;
        try {
            probe.check();
            return;
        } catch (LinkageError | RuntimeException e) {
            failure = e;
        } catch (Throwable e) {
            if (e instanceof Error error && !(e instanceof LinkageError)) {
                throw error;
            }
            failure = e;
        }
        throw SdkClientConfigurationException
            .builder()
            .message("The AWS Common Runtime native library could not be loaded on " + platform() + ", so "
                     + implementationName + " cannot be used. Add an aws-crt artifact that includes this platform, "
                     + "or configure another HTTP implementation. Cause: " + failure)
            .cause(failure)
            .build();
    }

    /** True if {@code probe} succeeds. */
    public static boolean isAvailable(Probe probe) {
        try {
            require(probe, "aws-crt-client");
            return true;
        } catch (SdkClientConfigurationException e) {
            return false;
        }
    }

    private static String platform() {
        return System.getProperty("os.name") + "/" + System.getProperty("os.arch");
    }
}
