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

package software.amazon.awssdk.core.pagination.async;

import java.util.concurrent.CompletableFuture;

/**
 * Fetches the pages of a paginated operation one non-blocking call at a time.
 *
 * @param <T> the page (response) type
 */
public interface AsyncPageFetcher<T> {

    /** Whether a page follows {@code previousPage}, by the modeled continuation token. */
    boolean hasNextPage(T previousPage);

    /** Calls the operation for the first page when {@code previousPage} is {@code null}, else for the next page. */
    CompletableFuture<T> nextPage(T previousPage);
}
