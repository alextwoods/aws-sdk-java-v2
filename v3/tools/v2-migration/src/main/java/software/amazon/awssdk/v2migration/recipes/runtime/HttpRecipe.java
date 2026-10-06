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
 * {@code http.replacements}: converts uses of the V2 HTTP SPI, HTTP client builders and HTTP options to the V3
 * replacements the Parity Manifest names, for example the V2 sync {@code SdkHttpClient} SPI type to the separate V3
 * {@code SdkSyncHttpClient} SPI, or a renamed builder option. Options whose V3 type changes (such as a builder method
 * that now takes a V3 sync client) are manifest diagnostics and stay unresolved.
 */
public final class HttpRecipe extends ReplacementRecipe {

    public HttpRecipe() {
        super("http.replacements",
              "Converts V2 HTTP SPI types, HTTP client builders and options to the V3 replacements the Parity "
              + "Manifest names.",
              Family.HTTP);
    }
}
