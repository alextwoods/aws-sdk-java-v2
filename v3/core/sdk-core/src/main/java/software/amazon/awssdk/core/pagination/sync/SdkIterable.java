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

package software.amazon.awssdk.core.pagination.sync;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An {@link Iterable} the SDK computes lazily, such as the pages of a paginated operation: each iterator issues a
 * call only when the next element is pulled.
 *
 * @param <T> the element type
 */
@FunctionalInterface
public interface SdkIterable<T> extends Iterable<T> {

    /** A sequential stream over a new iterator. */
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * The lazy concatenation of {@code mapper}'s iterable of every element: an element is pulled from this iterable
     * only when the previous element's iterable is exhausted.
     */
    default <U> SdkIterable<U> flatMapIterable(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return () -> new Iterator<U>() {
            private final Iterator<T> outer = SdkIterable.this.iterator();
            private Iterator<? extends U> inner = Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                while (!inner.hasNext()) {
                    if (!outer.hasNext()) {
                        return false;
                    }
                    Iterable<? extends U> next = mapper.apply(outer.next());
                    inner = next == null ? Collections.emptyIterator() : next.iterator();
                }
                return true;
            }

            @Override
            public U next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return inner.next();
            }
        };
    }

    /**
     * The pages {@code fetcher} produces: the first page on the first pull, then each next page while
     * {@link SyncPageFetcher#hasNextPage} holds for the previous one. Each iterator starts again from the first page.
     */
    static <T> SdkIterable<T> pages(SyncPageFetcher<T> fetcher) {
        Objects.requireNonNull(fetcher, "fetcher");
        return () -> new Iterator<T>() {
            private T previous;
            private boolean started;
            private boolean done;

            @Override
            public boolean hasNext() {
                if (done) {
                    return false;
                }
                if (started && !fetcher.hasNextPage(previous)) {
                    done = true;
                    return false;
                }
                return true;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T page = fetcher.nextPage(started ? previous : null);
                started = true;
                previous = page;
                return page;
            }
        };
    }
}
