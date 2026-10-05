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

package software.amazon.awssdk.tools.servicefleet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The service fleet pinned to one canonical model revision: the content of {@code v3/services/service-fleet.json}.
 *
 * @param revision the full Git commit object ID of the canonical aws-models revision every model comes from
 * @param services the generated services, sorted by name
 * @param missing baseline services without a generated service, each with the reason, sorted
 * @param unmappedCanonicalModels canonical models that no baseline service maps to (informational), sorted
 */
public record FleetPlan(String revision, List<FleetService> services, List<String> missing,
                        List<String> unmappedCanonicalModels) {

    /** The repository the canonical models come from. */
    public static final String CANONICAL_REPOSITORY = "https://github.com/aws/aws-models";

    private static final Pattern REVISION = Pattern.compile("[0-9a-f]{40}|[0-9a-f]{64}");

    public FleetPlan {
        Objects.requireNonNull(revision, "revision");
        if (!REVISION.matcher(revision).matches()) {
            throw new IllegalArgumentException("The canonical model revision '" + revision + "' is not a full Git "
                                               + "commit object ID (40 or 64 lower-case hexadecimal digits)");
        }
        services = List.copyOf(services);
        missing = List.copyOf(missing);
        unmappedCanonicalModels = List.copyOf(unmappedCanonicalModels);
    }

    /** The services of each module, by module, in module order; a module's own service comes first. */
    public Map<String, List<FleetService>> modules() {
        Map<String, List<FleetService>> modules = new LinkedHashMap<>();
        services.stream()
                .sorted((a, b) -> a.module().equals(b.module())
                                  ? Boolean.compare(b.primary(), a.primary()) != 0
                                    ? Boolean.compare(b.primary(), a.primary()) : a.name().compareTo(b.name())
                                  : a.module().compareTo(b.module()))
                .forEach(s -> modules.computeIfAbsent(s.module(), m -> new ArrayList<>()).add(s));
        return modules;
    }
}
