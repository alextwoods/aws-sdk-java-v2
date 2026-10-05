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

package software.amazon.awssdk.endpoints.discovery;

import java.util.Map;
import java.util.Objects;

/**
 * The key of a discovered endpoint: the caller identity (for example the access key ID, since discovered endpoints
 * are account-specific) and, for operations with {@code aws.api#clientEndpointDiscoveryId} members, the operation and
 * the identifier values, because the service may direct different resources to different endpoints.
 *
 * @param identity the caller identity key
 * @param operation the operation name, or an empty string if the endpoint is not operation-specific
 * @param ids discovery identifier values by member name; immutable
 */
public record EndpointDiscoveryKey(String identity, String operation, Map<String, String> ids) {
    public EndpointDiscoveryKey {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(operation, "operation");
        ids = Map.copyOf(ids);
    }

    /**
     * The key of a call of {@code operation} by {@code identity} with discovery identifiers {@code ids}: the key is
     * operation-specific only if the call has identifiers, as the service-wide endpoint otherwise applies.
     */
    public static EndpointDiscoveryKey of(String identity, String operation, Map<String, String> ids) {
        return ids.isEmpty() ? new EndpointDiscoveryKey(identity, "", Map.of())
                             : new EndpointDiscoveryKey(identity, operation, ids);
    }
}
