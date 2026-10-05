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
package software.amazon.awssdk.enhanced.dynamodb.internal.async;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.PaginationTokens;
import software.amazon.awssdk.core.pagination.async.AsyncPageFetcher;
import software.amazon.awssdk.core.pagination.async.PagePublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.pagination.sync.SyncPageFetcher;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

/**
 * BatchGetItem pagination over unprocessed keys: the first page is the response to the original request, and while a
 * page has unprocessed keys the next page is the response to the original request with its request items replaced by
 * those keys. This is the V2 {@code batchGetItemPaginator} customization (paginator input token {@code RequestItems},
 * output token {@code UnprocessedKeys}), which the canonical DynamoDB model does not declare, so the V3 generated
 * clients have no such paginator; it is implemented here over the public {@code batchGetItem} operation and the V3
 * core page fetchers. Each iteration or subscription starts again from the original request.
 */
public final class BatchGetItemPages {
    private BatchGetItemPages() {
    }

    public static SdkIterable<BatchGetItemResponse> iterable(DynamoDbClient client, BatchGetItemRequest firstRequest) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(firstRequest, "firstRequest");
        return () -> SdkIterable.pages(new SyncPageFetcher<BatchGetItemResponse>() {
            @Override
            public boolean hasNextPage(BatchGetItemResponse previousPage) {
                return hasUnprocessedKeys(previousPage);
            }

            @Override
            public BatchGetItemResponse nextPage(BatchGetItemResponse previousPage) {
                return client.batchGetItem(nextRequest(firstRequest, previousPage));
            }
        }).iterator();
    }

    public static SdkPublisher<BatchGetItemResponse> publisher(DynamoDbAsyncClient client,
                                                               BatchGetItemRequest firstRequest) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(firstRequest, "firstRequest");
        return subscriber -> PagePublisher.create(new AsyncPageFetcher<BatchGetItemResponse>() {
            @Override
            public boolean hasNextPage(BatchGetItemResponse previousPage) {
                return hasUnprocessedKeys(previousPage);
            }

            @Override
            public CompletableFuture<BatchGetItemResponse> nextPage(BatchGetItemResponse previousPage) {
                return client.batchGetItem(nextRequest(firstRequest, previousPage));
            }
        }).subscribe(subscriber);
    }

    static boolean hasUnprocessedKeys(BatchGetItemResponse page) {
        return page != null && page.hasUnprocessedKeys() && PaginationTokens.hasToken(page.unprocessedKeys());
    }

    private static BatchGetItemRequest nextRequest(BatchGetItemRequest firstRequest, BatchGetItemResponse previous) {
        if (previous == null) {
            return firstRequest;
        }
        Map<String, KeysAndAttributes> unprocessed = previous.unprocessedKeys();
        return firstRequest.toBuilder().requestItems(unprocessed).build();
    }
}
