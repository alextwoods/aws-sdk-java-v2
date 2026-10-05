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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Maps the V2 Launch Parity Baseline to the canonical models (Requirements 2.1, 16.4).
 *
 * <p>Each baseline service maps to the canonical model whose service shape's {@code aws.api#service} {@code sdkId}
 * equals the service's C2J {@code metadata.serviceId}: V2 derives the service name, and therefore every generated
 * type name, from that ID, so the mapping also preserves the V2 client names. A baseline service with no such model,
 * or with more than one, or two baseline services with the same ID, is reported as missing; it gets no generated
 * service and fails the fleet gate. No other key (directory names, endpoint prefixes) is trusted: aws-models stores
 * CloudWatch under {@code monitoring/} and SES under {@code email/}.
 */
public final class ServiceFleetPlanner {

    private ServiceFleetPlanner() {
    }

    /** The fleet of {@code baseline} generated from {@code models} at {@code revision}. */
    public static FleetPlan plan(List<V2Service> baseline, List<CanonicalModel> models, String revision) {
        Map<String, List<CanonicalModel>> bySdkId = models.stream()
                                                          .filter(m -> m.sdkId() != null)
                                                          .collect(Collectors.groupingBy(CanonicalModel::sdkId,
                                                                                         TreeMap::new,
                                                                                         Collectors.toList()));
        Map<String, List<V2Service>> byServiceId = baseline.stream()
                                                           .filter(s -> s.serviceId() != null)
                                                           .collect(Collectors.groupingBy(V2Service::serviceId,
                                                                                          TreeMap::new,
                                                                                          Collectors.toList()));
        List<FleetService> services = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (V2Service v2 : baseline) {
            if (v2.serviceId() == null) {
                missing.add(v2.name() + ": the V2 module has no C2J service model to identify its service");
                continue;
            }
            List<CanonicalModel> candidates = bySdkId.getOrDefault(v2.serviceId(), List.of());
            if (candidates.isEmpty()) {
                missing.add(v2.name() + ": no canonical model has sdkId \"" + v2.serviceId() + "\" at revision "
                            + revision);
            } else if (candidates.size() > 1 || byServiceId.get(v2.serviceId()).size() > 1) {
                missing.add(v2.name() + ": sdkId \"" + v2.serviceId() + "\" is ambiguous: canonical models "
                            + candidates.stream().map(CanonicalModel::directory).toList() + ", baseline services "
                            + byServiceId.get(v2.serviceId()).stream().map(V2Service::name).toList());
            } else {
                CanonicalModel model = candidates.get(0);
                if (model.protocolModules().isEmpty()) {
                    missing.add(v2.name() + ": canonical model " + model.directory() + " declares no launch protocol"
                                + (model.unknownProtocols().isEmpty() ? "" : " (only " + model.unknownProtocols()
                                                                            + ")"));
                    continue;
                }
                String modelDirectory = v2.subPackage() == null
                                        ? FleetService.PRIMARY_MODEL_DIRECTORY
                                        : FleetService.PRIMARY_MODEL_DIRECTORY + "-" + v2.subPackage();
                services.add(new FleetService(v2.module(), v2.name(), v2.packageName(), model.serviceShapeId(),
                                              model.sdkId(), model.directory(), modelDirectory,
                                              model.protocolModules(), v2.requiredTypes(), model.files(),
                                              model.modelDigest()));
            }
        }
        List<String> mappedDirectories = services.stream().map(FleetService::canonicalDirectory).toList();
        List<String> unmapped = models.stream()
                                      .filter(m -> !mappedDirectories.contains(m.directory()))
                                      .map(m -> m.directory() + " (sdkId " + m.sdkId() + ", service "
                                                + m.serviceShapeId() + ")")
                                      .sorted()
                                      .toList();
        services.sort(Comparator.comparing(FleetService::name));
        missing.sort(Comparator.naturalOrder());
        return new FleetPlan(revision, services, missing, unmapped);
    }
}
