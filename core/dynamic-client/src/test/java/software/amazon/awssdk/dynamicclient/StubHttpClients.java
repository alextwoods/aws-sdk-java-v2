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

package software.amazon.awssdk.dynamicclient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * HTTP clients that send nothing and count how often they are closed. Construction tests need no transport.
 */
final class StubHttpClients {
    private StubHttpClients() {
    }

    static final class Sync implements SdkHttpClient {
        final AtomicInteger closed = new AtomicInteger();

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            throw new UnsupportedOperationException("Construction tests send no requests");
        }

        @Override
        public String clientName() {
            return "StubSync";
        }

        @Override
        public void close() {
            closed.incrementAndGet();
        }
    }

    static final class Async implements SdkAsyncHttpClient {
        final AtomicInteger closed = new AtomicInteger();

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            throw new UnsupportedOperationException("Construction tests send no requests");
        }

        @Override
        public String clientName() {
            return "StubAsync";
        }

        @Override
        public void close() {
            closed.incrementAndGet();
        }
    }

    /**
     * A builder whose clients the SDK owns, so the client that created them closes them.
     */
    static final class SyncBuilder implements SdkHttpClient.Builder<SyncBuilder> {
        final Sync client = new Sync();

        @Override
        public SdkHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            return client;
        }
    }

    static final class AsyncBuilder implements SdkAsyncHttpClient.Builder<AsyncBuilder> {
        final Async client = new Async();

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            return client;
        }
    }
}
