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

package software.amazon.awssdk.regions.providers;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;

/**
 * Supplies the region a client uses when none is configured explicitly, with the V2 name and member. Implementations
 * are thread-safe; they may read files or the network, so clients call them at construction, never on an event loop.
 */
@FunctionalInterface
public interface AwsRegionProvider {
    /**
     * The region.
     *
     * @throws SdkClientException if this provider cannot determine a region
     */
    Region getRegion();
}
