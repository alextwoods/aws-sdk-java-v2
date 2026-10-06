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

package software.amazon.awssdk.v2migration.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Migrates the project from V2 to V3 in place: inventories its effective V2 dependencies, BOMs and Java sources,
 * applies every deterministic recipe edit, leaves every unresolved construct unchanged, and writes the migration
 * report. Run it from the project to migrate, for example
 * {@code mvn software.amazon.awssdk:v2-migration:3.x.y:migrate -Dv2migration.manifest=parity-manifest.json}.
 */
@Mojo(name = "migrate", requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true, threadSafe = true)
public final class MigrateMojo extends AbstractMigrationMojo {

    @Override
    boolean applies() {
        return true;
    }
}
