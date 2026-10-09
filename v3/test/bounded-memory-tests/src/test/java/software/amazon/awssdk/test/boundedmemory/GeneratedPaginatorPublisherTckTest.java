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
package software.amazon.awssdk.test.boundedmemory;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Factory;
import software.amazon.awssdk.services.mediastoredata.MediaStoreDataAsyncClient;
import software.amazon.awssdk.services.mediastoredata.model.ListItemsRequest;
import software.amazon.awssdk.services.mediastoredata.model.ListItemsResponse;
import software.amazon.awssdk.services.mediastoredata.paginators.ListItemsPublisher;
/**
 * Reactive Streams TCK (TestNG: demand, cancellation, early termination) for a generated async paginator publisher,
 * MediaStore Data's {@link ListItemsPublisher}, over an in-JVM async client whose {@code listItems} pages are
 * {@code createPublisher(n)} pages chained by {@code NextToken}. The pages complete inline in one verification and on
 * another thread in the other, so both the synchronous and the asynchronous page paths of the generated publisher are
 * verified. (The core {@code PagePublisher} underneath is also verified through the enhanced client's operators in
 * dynamodb-enhanced.) A paginator always fetches its first page, so it has no zero-element form; those cases are
 * skipped.
 */
public final class GeneratedPaginatorPublisherTckTest extends PublisherVerification<ListItemsResponse> {
    private final boolean asyncPages;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public GeneratedPaginatorPublisherTckTest(boolean asyncPages) {
        super(new TestEnvironment());
        this.asyncPages = asyncPages;
    }
    @Factory
    public static Object[] factories() {
        return new Object[] {new GeneratedPaginatorPublisherTckTest(false), new GeneratedPaginatorPublisherTckTest(true)};
    }
    @Override
    public Publisher<ListItemsResponse> createPublisher(long elements) {
        if (elements == 0) {
            throw new SkipException("A paginator publishes at least its first page");
        }
        return new ListItemsPublisher(pagingClient(elements), ListItemsRequest.builder().path("folder").build());
    }
    /** Page failures surface only under demand, so the eager-error tests do not apply. */
    @Override
    public Publisher<ListItemsResponse> createFailedPublisher() {
        return null;
    }
    @Override
    public long maxElementsFromPublisher() {
        return 64;
    }
    @AfterClass
    public void shutdown() {
        executor.shutdownNow();
    }
    /** An async client whose {@code listItems} returns page {@code i} for token {@code i}, {@code pages} in total. */
    private MediaStoreDataAsyncClient pagingClient(long pages) {
        return (MediaStoreDataAsyncClient) Proxy.newProxyInstance(
            MediaStoreDataAsyncClient.class.getClassLoader(), new Class<?>[] {MediaStoreDataAsyncClient.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "listItems":
                        if (args != null && args.length >= 1 && args[0] instanceof ListItemsRequest request) {
                            return page(request, pages);
                        }
                        break;
                    case "toString":
                        return "PagingMediaStoreDataAsyncClient(" + pages + ")";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    case "close":
                        return null;
                    default:
                        break;
                }
                throw new UnsupportedOperationException(method.toString());
            });
    }
    private CompletableFuture<ListItemsResponse> page(ListItemsRequest request, long pages) {
        long index = request.nextToken() == null ? 0 : Long.parseLong(request.nextToken());
        ListItemsResponse.Builder page = ListItemsResponse.builder();
        if (index + 1 < pages) {
            page.nextToken(Long.toString(index + 1));
        }
        ListItemsResponse response = page.build();
        return asyncPages ? CompletableFuture.supplyAsync(() -> response, executor)
                          : CompletableFuture.completedFuture(response);
    }
    @Override
    public String toString() {
        return "GeneratedPaginatorPublisherTckTest[" + (asyncPages ? "asyncPages" : "inlinePages") + "]";
    }
}
