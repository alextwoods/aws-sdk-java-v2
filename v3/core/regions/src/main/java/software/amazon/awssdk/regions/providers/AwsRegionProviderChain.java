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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;

/**
 * Asks each provider in order and returns the first region found, with the V2 name. If every provider fails, the
 * exception lists each provider's failure message in order.
 */
public class AwsRegionProviderChain implements AwsRegionProvider {
    private final List<AwsRegionProvider> providers;

    public AwsRegionProviderChain(AwsRegionProvider... providers) {
        this(List.of(providers));
    }

    public AwsRegionProviderChain(List<? extends AwsRegionProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
        if (this.providers.isEmpty()) {
            throw new IllegalArgumentException("A region provider chain needs at least one provider.");
        }
    }

    /** The providers, in the order they are asked. */
    public List<AwsRegionProvider> providers() {
        return providers;
    }

    @Override
    public Region getRegion() {
        List<String> failures = new ArrayList<>();
        for (AwsRegionProvider provider : providers) {
            try {
                Region region = provider.getRegion();
                if (region != null) {
                    return region;
                }
                failures.add(provider.getClass().getSimpleName() + ": returned no region");
            } catch (RuntimeException e) {
                failures.add(provider.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        throw SdkClientException.create("Unable to load region from any of the providers in the chain " + this + ": "
                                        + failures);
    }

    @Override
    public String toString() {
        List<String> names = new ArrayList<>();
        providers.forEach(p -> names.add(p.getClass().getSimpleName()));
        return getClass().getSimpleName() + names;
    }
}
