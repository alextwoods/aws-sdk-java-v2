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

package software.amazon.awssdk.identity.spi;

import software.amazon.awssdk.identity.spi.internal.DefaultResolveIdentityRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The input of {@link IdentityProvider#resolveIdentity(ResolveIdentityRequest)}: a set of typed
 * {@link IdentityProperty} values, with the V2 name and members.
 *
 * <p>Immutable; equal when the property values are equal.
 */
public interface ResolveIdentityRequest extends ToCopyableBuilder<ResolveIdentityRequest.Builder,
                                                                  ResolveIdentityRequest> {
    static Builder builder() {
        return DefaultResolveIdentityRequest.builder();
    }

    /** The value of {@code property}, or {@code null} if the request does not set it. */
    <T> T property(IdentityProperty<T> property);

    /** Builder of a {@link ResolveIdentityRequest}. */
    interface Builder extends CopyableBuilder<Builder, ResolveIdentityRequest> {
        /** Sets {@code key} to {@code value}; {@code null} removes it. */
        <T> Builder putProperty(IdentityProperty<T> key, T value);
    }
}
