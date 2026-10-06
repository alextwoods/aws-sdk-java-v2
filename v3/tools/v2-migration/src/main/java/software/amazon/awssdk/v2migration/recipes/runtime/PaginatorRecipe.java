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
 * {@code pagination.replacements}: converts uses of the V2 pagination runtime ({@code core.pagination}, such as
 * {@code SdkIterable} and the async paginator support types) to the V3 replacements the Parity Manifest names. Each
 * service's generated {@code paginators} types are service API, converted by the client recipes; the publisher
 * operators that V3 does not ship are known gaps ({@link KnownGapRecipe}) and stay unresolved.
 */
public final class PaginatorRecipe extends ReplacementRecipe {

    public PaginatorRecipe() {
        super("pagination.replacements",
              "Converts V2 paginator types and members to the V3 replacements the Parity Manifest names.",
              Family.PAGINATION);
    }
}
