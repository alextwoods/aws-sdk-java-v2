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

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * One endpoint returned by a service's endpoint discovery operation ({@code Address} and
 * {@code CachePeriodInMinutes} of the modeled {@code Endpoint} structure).
 *
 * @param address the host (optionally with a port or path) the service directs calls to
 * @param cachePeriod how long the endpoint may be used
 */
public record DiscoveredEndpoint(String address, Duration cachePeriod) {
    public DiscoveredEndpoint {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(cachePeriod, "cachePeriod");
        if (address.isBlank()) {
            throw new IllegalArgumentException("A discovered endpoint address must not be blank.");
        }
        if (cachePeriod.isNegative()) {
            throw new IllegalArgumentException("A discovered endpoint cache period must not be negative.");
        }
    }

    /** An endpoint from the modeled {@code Address} and {@code CachePeriodInMinutes}. */
    public static DiscoveredEndpoint of(String address, long cachePeriodInMinutes) {
        return new DiscoveredEndpoint(address, Duration.ofMinutes(cachePeriodInMinutes));
    }

    /** The endpoint URI: {@code https://} and the address, unless the address already has a scheme. */
    public URI uri() {
        return URI.create(address.contains("://") ? address : "https://" + address);
    }
}
