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

/**
 * The Maven side of the migration inventory: the effective dependency and BOM inventory of a project
 * ({@link software.amazon.awssdk.v2migration.pom.ProjectDependencies}) and a lossless, offset-exact view of its
 * {@code pom.xml} ({@link software.amazon.awssdk.v2migration.pom.PomDocument}) that recipes edit in place.
 */
package software.amazon.awssdk.v2migration.pom;
