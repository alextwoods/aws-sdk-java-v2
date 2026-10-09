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

package software.amazon.awssdk.benchmark.harness;

import java.util.concurrent.CompletableFuture;

/**
 * The client half of a cell's workload, living in the forked client process: one client built for the cell's path on
 * the cell's explicitly constructed transport, and one logical operation of the cell's outcome.
 *
 * <p>An operation completes normally only if it ended in the cell's expected outcome (a response for
 * {@code success}/{@code retry}, the modeled exception for {@code error}); anything else throws or completes
 * exceptionally and is counted as a failed operation, which invalidates the repetition (Req 20.27, applied in 19.2).
 * The per-call check is a type check only; {@link #verify()} does the deep pre-timing check once.
 */
public interface CellWorkload extends AutoCloseable {

    /** One blocking operation (sync paths). */
    void call();

    /** One asynchronous operation (async paths). */
    CompletableFuture<Void> callAsync();

    /** True for async paths: the driver keeps {@code concurrency} operations in flight via {@link #callAsync()}. */
    boolean isAsync();

    /** The built transport instance's class, recorded with every result. */
    String transportClass();

    /** One untimed operation whose decoded result is compared with the fixture; throws on any mismatch. */
    void verify();

    @Override
    void close() throws Exception;
}
