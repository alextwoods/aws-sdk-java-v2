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

package software.amazon.awssdk.v2migration.recipes.runtime;

/**
 * {@code hll.replacements}: converts uses of the high-level libraries (DynamoDB Enhanced Client, S3 Transfer Manager,
 * IAM Policy Builder, S3 Event Notifications and SNS Message Manager) to the V3 replacements the Parity Manifest names.
 * The V3 libraries keep their V2 names, so most uses are preserved by the manifest; the recorded adaptations (such as
 * the SNS {@code httpClient} option or the Transfer Manager's {@code CompletedObjectTransfer#response} type) are
 * manifest diagnostics, and APIs V3 does not ship are known gaps ({@link KnownGapRecipe}); both stay unresolved.
 */
public final class HighLevelLibraryRecipe extends ReplacementRecipe {

    public HighLevelLibraryRecipe() {
        super("hll.replacements",
              "Converts uses of the DynamoDB Enhanced Client, S3 Transfer Manager, IAM Policy Builder, S3 Event "
              + "Notifications and SNS Message Manager to the V3 replacements the Parity Manifest names.",
              Family.HIGH_LEVEL_LIBRARIES);
    }
}
