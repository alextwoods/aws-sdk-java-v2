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

package software.amazon.awssdk.transfer.s3.internal.directory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.pagination.PaginationTokens;
import software.amazon.awssdk.core.pagination.async.AsyncPageFetcher;
import software.amazon.awssdk.core.pagination.async.PagePublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.config.DownloadFilter;

/**
 * The objects of a {@code downloadDirectory} request, read one {@code ListObjectsV2} page at a time with exact demand:
 * the next page is requested only when the previous page's objects have all been handed out. Without a delimiter the
 * generated {@code listObjectsV2Paginator} is used; with one, every common prefix is listed recursively (V2 behavior),
 * through a {@link PagePublisher} over the same operation. Objects whose key equals the listed prefix (a "directory"
 * placeholder) and objects the filter rejects are skipped.
 */
final class ObjectListingSource implements ItemSource<S3Object>, Subscriber<ListObjectsV2Response> {
    private final Publisher<ListObjectsV2Response> pages;
    private final DownloadFilter filter;
    private final Deque<S3Object> ready = new ArrayDeque<>();
    private Subscription subscription;
    private boolean requested;
    private boolean complete;
    private boolean closed;
    private Throwable error;
    private volatile Runnable onAvailable = () -> { };

    ObjectListingSource(S3AsyncClient s3, ListObjectsV2Request request, DownloadFilter filter) {
        this.pages = request.delimiter() == null ? s3.listObjectsV2Paginator(request)
                                                 : PagePublisher.create(new RecursiveFetcher(s3, request));
        this.filter = filter;
    }

    @Override
    public void onAvailable(Runnable callback) {
        this.onAvailable = callback;
        pages.subscribe(this);
    }

    @Override
    public S3Object poll() throws Exception {
        Subscription request = null;
        S3Object next;
        synchronized (this) {
            next = ready.poll();
            if (next == null) {
                if (error != null) {
                    Throwable t = error;
                    error = null;
                    if (t instanceof Exception e) {
                        throw e;
                    }
                    throw new RuntimeException(t);
                }
                if (!complete && !requested && subscription != null && !closed) {
                    requested = true;
                    request = subscription;
                }
            }
        }
        if (request != null) {
            request.request(1);
        }
        return next;
    }

    @Override
    public synchronized boolean exhausted() {
        return complete && ready.isEmpty() && error == null;
    }

    @Override
    public void close() {
        Subscription s;
        synchronized (this) {
            closed = true;
            s = subscription;
            ready.clear();
        }
        if (s != null) {
            s.cancel();
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        boolean cancel;
        synchronized (this) {
            cancel = subscription != null || closed;
            if (!cancel) {
                subscription = s;
            }
        }
        if (cancel) {
            s.cancel();
            return;
        }
        onAvailable.run();
    }

    @Override
    public void onNext(ListObjectsV2Response page) {
        synchronized (this) {
            requested = false;
            if (closed) {
                return;
            }
            for (S3Object object : page.contents()) {
                if (page.prefix() != null && page.prefix().equals(object.key())) {
                    continue;
                }
                if (filter.test(object)) {
                    ready.add(object);
                }
            }
        }
        onAvailable.run();
    }

    @Override
    public void onError(Throwable t) {
        synchronized (this) {
            error = t;
        }
        onAvailable.run();
    }

    @Override
    public void onComplete() {
        synchronized (this) {
            complete = true;
        }
        onAvailable.run();
    }

    /** Lists a prefix, then (depth first) every common prefix it reports, as one page sequence (V2 behavior). */
    private static final class RecursiveFetcher implements AsyncPageFetcher<ListObjectsV2Response> {
        private final S3AsyncClient s3;
        private final Deque<String> commonPrefixes = new ConcurrentLinkedDeque<>();
        private volatile ListObjectsV2Request current;

        RecursiveFetcher(S3AsyncClient s3, ListObjectsV2Request first) {
            this.s3 = s3;
            this.current = first;
        }

        @Override
        public boolean hasNextPage(ListObjectsV2Response previousPage) {
            return PaginationTokens.hasToken(previousPage.nextContinuationToken()) || !commonPrefixes.isEmpty();
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> nextPage(ListObjectsV2Response previousPage) {
            CompletableFuture<ListObjectsV2Response> page;
            if (previousPage == null) {
                page = s3.listObjectsV2(current);
            } else if (PaginationTokens.hasToken(previousPage.nextContinuationToken())) {
                page = s3.listObjectsV2(current.toBuilder()
                                               .continuationToken(previousPage.nextContinuationToken()).build());
            } else {
                current = current.toBuilder().prefix(commonPrefixes.pop()).continuationToken(null).build();
                page = s3.listObjectsV2(current);
            }
            return page.thenApply(response -> {
                List<CommonPrefix> prefixes = response.commonPrefixes();
                for (int i = prefixes.size() - 1; i >= 0; i--) {
                    commonPrefixes.push(prefixes.get(i).prefix());
                }
                return response;
            });
        }
    }
}
