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
import java.util.function.BiFunction;
import java.util.function.Function;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.PaginationTokens;
import software.amazon.awssdk.core.pagination.async.AsyncPageFetcher;
import software.amazon.awssdk.core.pagination.async.PagePublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.pagination.sync.SyncPageFetcher;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUserAgent;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

/**
 * Query and Scan pagination over {@code LastEvaluatedKey}/{@code ExclusiveStartKey}, with the same pages, page
 * requests and termination as the generated {@code queryPaginator}/{@code scanPaginator}, except that each page
 * request passes the enhanced client's user-agent override ({@link EnhancedClientUserAgent#OVERRIDE}), which the
 * generated paginators cannot carry. Each iteration or subscription starts again from the first request.
 */
public final class KeyPages {
    private KeyPages() {
    }

    public static SdkIterable<QueryResponse> query(DynamoDbClient client, QueryRequest firstRequest) {
        Objects.requireNonNull(client, "client");
        return iterable(firstRequest, r -> client.query(r, EnhancedClientUserAgent.OVERRIDE),
                        QueryResponse::lastEvaluatedKey, (r, key) -> r.toBuilder().exclusiveStartKey(key).build());
    }

    public static SdkPublisher<QueryResponse> query(DynamoDbAsyncClient client, QueryRequest firstRequest) {
        Objects.requireNonNull(client, "client");
        return publisher(firstRequest, r -> client.query(r, EnhancedClientUserAgent.OVERRIDE),
                         QueryResponse::lastEvaluatedKey, (r, key) -> r.toBuilder().exclusiveStartKey(key).build());
    }

    public static SdkIterable<ScanResponse> scan(DynamoDbClient client, ScanRequest firstRequest) {
        Objects.requireNonNull(client, "client");
        return iterable(firstRequest, r -> client.scan(r, EnhancedClientUserAgent.OVERRIDE),
                        ScanResponse::lastEvaluatedKey, (r, key) -> r.toBuilder().exclusiveStartKey(key).build());
    }

    public static SdkPublisher<ScanResponse> scan(DynamoDbAsyncClient client, ScanRequest firstRequest) {
        Objects.requireNonNull(client, "client");
        return publisher(firstRequest, r -> client.scan(r, EnhancedClientUserAgent.OVERRIDE),
                         ScanResponse::lastEvaluatedKey, (r, key) -> r.toBuilder().exclusiveStartKey(key).build());
    }

    private static <RequestT, ResponseT> SdkIterable<ResponseT> iterable(
        RequestT firstRequest, Function<RequestT, ResponseT> call,
        Function<ResponseT, Map<String, AttributeValue>> lastKey,
        BiFunction<RequestT, Map<String, AttributeValue>, RequestT> withStartKey) {
        Objects.requireNonNull(firstRequest, "firstRequest");
        return () -> SdkIterable.pages(new SyncPageFetcher<ResponseT>() {
            @Override
            public boolean hasNextPage(ResponseT previousPage) {
                return previousPage != null && PaginationTokens.hasToken(lastKey.apply(previousPage));
            }

            @Override
            public ResponseT nextPage(ResponseT previousPage) {
                return call.apply(previousPage == null ? firstRequest
                                                       : withStartKey.apply(firstRequest, lastKey.apply(previousPage)));
            }
        }).iterator();
    }

    private static <RequestT, ResponseT> SdkPublisher<ResponseT> publisher(
        RequestT firstRequest, Function<RequestT, CompletableFuture<ResponseT>> call,
        Function<ResponseT, Map<String, AttributeValue>> lastKey,
        BiFunction<RequestT, Map<String, AttributeValue>, RequestT> withStartKey) {
        Objects.requireNonNull(firstRequest, "firstRequest");
        return subscriber -> PagePublisher.create(new AsyncPageFetcher<ResponseT>() {
            @Override
            public boolean hasNextPage(ResponseT previousPage) {
                return previousPage != null && PaginationTokens.hasToken(lastKey.apply(previousPage));
            }

            @Override
            public CompletableFuture<ResponseT> nextPage(ResponseT previousPage) {
                return call.apply(previousPage == null ? firstRequest
                                                       : withStartKey.apply(firstRequest, lastKey.apply(previousPage)));
            }
        }).subscribe(subscriber);
    }
}
