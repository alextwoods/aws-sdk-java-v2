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

package software.amazon.awssdk.transfer.s3.internal.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Runs posted tasks one at a time, in posting order, on the thread that posts while no other thread is running them
 * (a trampoline): the state a transfer coordinator owns is only touched from its tasks, without locks and without
 * hopping to another executor. Tasks must be short and non-blocking: they update state and start asynchronous calls.
 * A task that throws is reported to the failure handler and the following tasks still run. Not a public API.
 */
public final class SerialActor {
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final AtomicInteger wip = new AtomicInteger();
    private final Consumer<Throwable> onTaskFailure;

    public SerialActor(Consumer<Throwable> onTaskFailure) {
        this.onTaskFailure = onTaskFailure;
    }

    public void post(Runnable task) {
        tasks.add(task);
        if (wip.getAndIncrement() != 0) {
            return;
        }
        int missed = 1;
        do {
            Runnable next;
            while ((next = tasks.poll()) != null) {
                try {
                    next.run();
                } catch (RuntimeException e) {
                    onTaskFailure.accept(e);
                }
            }
            missed = wip.addAndGet(-missed);
        } while (missed != 0);
    }
}
