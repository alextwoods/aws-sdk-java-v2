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

package software.amazon.awssdk.test.reactor;

import java.nio.file.Path;
import java.util.Set;

/**
 * One module of the V3 reactor, read directly from its POM.
 *
 * @param path directory relative to the V3 root, with {@code /} separators ({@code .} for the aggregator)
 * @param directory absolute module directory
 * @param artifactId the module's artifactId
 * @param version the module's version
 * @param packaging the module's packaging ({@code jar} when not declared)
 * @param parentArtifactId artifactId of the declared parent, or {@code null}
 * @param v3References artifactIds of V3 dependencies and build plugins declared by the module or inherited from its
 *                     parent chain
 */
public record ReactorModule(String path, Path directory, String artifactId, String version, String packaging,
                            String parentArtifactId, Set<String> v3References) {

    public ReactorModule {
        v3References = Set.copyOf(v3References);
    }

    /** True for modules that produce a jar (including {@code maven-plugin} packaging). */
    public boolean producesJar() {
        return "jar".equals(packaging) || "maven-plugin".equals(packaging);
    }

    /** The packaged archive this module produces under {@code target/}. */
    public Path jar() {
        return directory.resolve("target").resolve(artifactId + "-" + version + ".jar");
    }
}
