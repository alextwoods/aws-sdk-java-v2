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

package software.amazon.awssdk.core.resource;

/**
 * Who closes a configured resource (Requirement 9.6).
 *
 * <p>A built resource passed to a client builder (an HTTP client, identity provider or executor) is
 * {@link #CALLER_OWNED}; a resource the SDK creates, from a builder, factory or default, is {@link #SDK_OWNED}.
 */
public enum ResourceOwnership {
    /** The SDK created the resource and closes it exactly once when the client closes. */
    SDK_OWNED,
    /** The caller supplied the resource and closes it; closing the client leaves it open. */
    CALLER_OWNED;

    /** Whether the SDK closes resources of this ownership. */
    public boolean closedBySdk() {
        return this == SDK_OWNED;
    }
}
