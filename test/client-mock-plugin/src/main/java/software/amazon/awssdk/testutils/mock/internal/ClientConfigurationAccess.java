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

package software.amazon.awssdk.testutils.mock.internal;

import java.lang.reflect.Field;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.utils.Validate;

/**
 * Finds the {@link SdkClientConfiguration.Builder} behind the service configuration builder a plugin receives.
 *
 * <p>{@link SdkServiceClientConfiguration.Builder} has no setter for the client transport. Every generated
 * {@code <Service>ServiceClientConfigurationBuilder} wraps the client's {@link SdkClientConfiguration.Builder} in a
 * private field, so the mock plugin reads that field, found by its type, to set {@code SdkClientOption.CLIENT_TRANSPORT}
 * and {@code ASYNC_CLIENT_TRANSPORT}.
 */
@SdkInternalApi
public final class ClientConfigurationAccess {
    private ClientConfigurationAccess() {
    }

    /**
     * Returns the client configuration builder wrapped by a generated service configuration builder.
     *
     * @param config the builder passed to {@code SdkPlugin.configureClient}
     * @return the wrapped builder; options set on it apply to the client being built
     * @throws IllegalStateException if {@code config} wraps no {@link SdkClientConfiguration.Builder}, for example
     *     because it is not a generated service configuration builder
     */
    public static SdkClientConfiguration.Builder clientConfiguration(SdkServiceClientConfiguration.Builder config) {
        Validate.paramNotNull(config, "config");
        for (Class<?> type = config.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() == SdkClientConfiguration.Builder.class) {
                    return read(field, config);
                }
            }
        }
        throw new IllegalStateException("MockPlugin needs the configuration builder of a generated client, but "
                                        + config.getClass().getName() + " wraps no SdkClientConfiguration.Builder");
    }

    private static SdkClientConfiguration.Builder read(Field field, Object owner) {
        try {
            field.setAccessible(true);
            SdkClientConfiguration.Builder builder = (SdkClientConfiguration.Builder) field.get(owner);
            if (builder == null) {
                throw new IllegalStateException(field + " of " + owner.getClass().getName() + " is null");
            }
            return builder;
        } catch (IllegalAccessException | RuntimeException e) {
            throw new IllegalStateException("Unable to read the client configuration from " + field, e);
        }
    }
}
