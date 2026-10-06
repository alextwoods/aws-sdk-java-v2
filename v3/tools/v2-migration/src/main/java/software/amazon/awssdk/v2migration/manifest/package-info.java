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
 * The Parity Manifest reader of the Migration Tooling: reads the published {@code parity-manifest-1.schema.json}
 * document form ({@link software.amazon.awssdk.v2migration.manifest.MigrationManifest}) that the
 * {@code parity-manifest} release tooling writes. This artifact is published and that one is not, so the format is read
 * here rather than linked; the member lists, ID schemes and validation rules mirror the publisher's exactly, and a test
 * checks them against the schema in the checkout.
 */
package software.amazon.awssdk.v2migration.manifest;
