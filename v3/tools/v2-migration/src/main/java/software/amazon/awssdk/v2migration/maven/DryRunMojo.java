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
 * Computes the same migration as {@code migrate} and writes the migration report, but changes no project file. With
 * {@code -Dv2migration.failOnUnresolved=true} it gates a build on a complete migration.
 */
@Mojo(name = "dry-run", requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true,
      threadSafe = true)
public final class DryRunMojo extends AbstractMigrationMojo {

    @Override
    boolean applies() {
        return false;
    }
}
