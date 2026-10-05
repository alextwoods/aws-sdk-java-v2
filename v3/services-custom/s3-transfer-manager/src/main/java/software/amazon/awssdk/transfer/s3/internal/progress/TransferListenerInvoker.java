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

package software.amazon.awssdk.transfer.s3.internal.progress;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import software.amazon.awssdk.transfer.s3.internal.util.Log;
import software.amazon.awssdk.transfer.s3.internal.util.Validate;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

/**
 * Composes the provided {@link TransferListener}s into a single logical chain (as in V2). Invocations are delegated to
 * every listener in order while suppressing (and logging at WARNING) any exception a listener throws, so a faulty
 * listener cannot fail or stall a transfer. {@code transferInitiated} is delivered at most once, and exactly one of
 * {@code transferComplete} or {@code transferFailed} at most once.
 */
public final class TransferListenerInvoker implements TransferListener {
    private static final Log log = Log.loggerFor(TransferListener.class);

    private final List<TransferListener> listeners;
    private final AtomicBoolean initiated = new AtomicBoolean();
    private final AtomicBoolean complete = new AtomicBoolean();

    public TransferListenerInvoker(List<TransferListener> listeners) {
        this.listeners = List.copyOf(Validate.paramNotNull(listeners, "listeners"));
    }

    @Override
    public void transferInitiated(Context.TransferInitiated context) {
        if (!initiated.getAndSet(true)) {
            forEach(listener -> listener.transferInitiated(context));
        }
    }

    @Override
    public void bytesTransferred(Context.BytesTransferred context) {
        forEach(listener -> listener.bytesTransferred(context));
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
        if (!complete.getAndSet(true)) {
            forEach(listener -> listener.transferComplete(context));
        }
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
        if (!complete.getAndSet(true)) {
            forEach(listener -> listener.transferFailed(context));
        }
    }

    /** True if there is no listener to call. */
    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    private void forEach(Consumer<TransferListener> action) {
        for (TransferListener listener : listeners) {
            try {
                action.accept(listener);
            } catch (RuntimeException | Error e) {
                if (e instanceof VirtualMachineError) {
                    throw e;
                }
                log.warn(() -> "Exception thrown in TransferListener " + listener + ", ignoring", e);
            }
        }
    }
}
