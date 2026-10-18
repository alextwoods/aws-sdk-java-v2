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

package software.amazon.awssdk.schema.serde.event;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A frame wraps the data of one event stream message, with the metadata needed to sign and validate it.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.event.Frame}.
 *
 * @param <T> the type of the wrapped message
 */
@SdkPublicApi
public interface Frame<T> {
    /**
     * Returns the wrapped message.
     *
     * @return the wrapped message
     */
    T unwrap();
}
