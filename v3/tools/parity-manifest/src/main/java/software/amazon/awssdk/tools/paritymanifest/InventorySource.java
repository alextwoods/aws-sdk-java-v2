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

package software.amazon.awssdk.tools.paritymanifest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A scanner that inventories one part of the V2 baseline (artifacts, public APIs, configuration, HTTP modes,
 * behaviors). Implementations are discovered with {@link java.util.ServiceLoader} and must be deterministic: the same
 * baseline yields the same items. Items carry no classification; {@link ClassificationRules} assigns it.
 */
public interface InventorySource {

    /** A stable, unique source name, e.g. {@code v2-public-api}; used in problems and by {@code --source}. */
    String name();

    /** Scans the baseline described by {@code context}. */
    List<InventoryItem> scan(Context context) throws IOException;

    /**
     * What a scan runs against.
     *
     * @param repositoryRoot the V2 source repository root (the directory holding the V2 root {@code pom.xml})
     * @param baseline       the baseline identity being generated
     * @param options        scanner options by name (for example {@link PublicApiInventorySource#MODULES_OPTION});
     *                       scanners ignore options they do not know
     */
    record Context(Path repositoryRoot, BaselineIdentity baseline, Map<String, String> options) {

        public Context {
            options = Map.copyOf(Objects.requireNonNull(options, "options"));
        }

        /** A context without scanner options. */
        public Context(Path repositoryRoot, BaselineIdentity baseline) {
            this(repositoryRoot, baseline, Map.of());
        }

        /** The value of {@code option}, or {@code null}. */
        public String option(String option) {
            return options.get(option);
        }
    }
}
