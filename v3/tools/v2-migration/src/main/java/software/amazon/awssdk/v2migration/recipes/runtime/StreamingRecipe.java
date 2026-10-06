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
 * {@code streaming.replacements}: converts uses of the V2 sync and async body and response types
 * ({@code RequestBody}, {@code ResponseTransformer}, {@code AsyncRequestBody}, {@code AsyncResponseTransformer},
 * {@code SdkPublisher}, {@code ResponseInputStream}, {@code ResponseBytes}) to the V3 replacements the Parity Manifest
 * names, including custom body implementations that override a recipe-replaced method. Changed body semantics that
 * need a decision (such as whether a custom {@code AsyncRequestBody} is replayable) are manifest diagnostics, and
 * operators V3 does not ship are known gaps; both stay unresolved.
 */
public final class StreamingRecipe extends ReplacementRecipe {

    public StreamingRecipe() {
        super("streaming.replacements",
              "Converts V2 sync and async body, response transformer and publisher types to the V3 replacements the "
              + "Parity Manifest names.",
              Family.STREAMING);
    }
}
