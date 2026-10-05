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

import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.tools.servicefleet.CanonicalModel.ModelFile;

/**
 * One generated V3 service of the fleet: a V2 baseline service and the canonical model it is generated from.
 *
 * @param module the service module directory under {@code v3/services/} and its artifact ID (the V2 module name)
 * @param name the baseline service name ({@code module} or {@code module.subPackage})
 * @param packageName the generated Java package (the V2 client package)
 * @param service the absolute shape ID of the service in its canonical model
 * @param sdkId the canonical model's SDK ID, equal to the V2 {@code serviceId}
 * @param canonicalDirectory the canonical model's directory in the aws-models repository
 * @param modelDirectory the directory of the vendored model files, relative to the module directory
 * @param protocolModules the V3 protocol modules of the modeled protocols, sorted
 * @param requiredTypes the V2 client types the module must generate, sorted
 * @param modelFiles the vendored model files and their digests, sorted by path
 * @param modelDigest the location-independent digest of the vendored model files that generation records
 */
public record FleetService(String module, String name, String packageName, String service, String sdkId,
                           String canonicalDirectory, String modelDirectory, List<String> protocolModules,
                           List<String> requiredTypes, List<ModelFile> modelFiles, String modelDigest) {

    /** Vendored model directory of a module's own service, relative to the module directory. */
    public static final String PRIMARY_MODEL_DIRECTORY = "src/main/smithy";

    public FleetService {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(packageName, "packageName");
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(canonicalDirectory, "canonicalDirectory");
        Objects.requireNonNull(modelDirectory, "modelDirectory");
        Objects.requireNonNull(modelDigest, "modelDigest");
        protocolModules = List.copyOf(protocolModules);
        requiredTypes = List.copyOf(requiredTypes);
        modelFiles = List.copyOf(modelFiles);
    }

    /** Whether this is its module's own service rather than a service the V2 module bundles in a sub-package. */
    public boolean primary() {
        return name.equals(module);
    }

    /** The sub-package of a bundled service ({@code streams} for {@code dynamodb.streams}), or {@code null}. */
    public String subPackage() {
        return primary() ? null : name.substring(module.length() + 1);
    }
}
