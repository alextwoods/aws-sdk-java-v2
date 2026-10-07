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
import software.amazon.awssdk.transfer.s3.internal.util.Validate;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.WarningSuppression;

/**
 * Composes the provided {@link TransferListener}s into a single logical chain (as in V2). Invocations are delegated to
 * every listener in order while suppressing any exception a listener throws, so a faulty listener cannot fail or
 * stall a transfer. The ignored failure is logged at WARN at most once per listener class and callback for the
 * transfer (Requirement 21.12), not once per progress event. {@code transferInitiated} is delivered at most once, and exactly one of
 * {@code transferComplete} or {@code transferFailed} at most once.
 */
public final class TransferListenerInvoker implements TransferListener {
    private static final Logger log = Logger.loggerFor(TransferListener.class);
    private static final String IGNORED_LISTENER_FAILURE = "ignored-transfer-listener-failure";

    private final List<TransferListener> listeners;
    private final AtomicBoolean initiated = new AtomicBoolean();
    private final AtomicBoolean complete = new AtomicBoolean();
    private final WarningSuppression warnings = WarningSuppression.create();

    public TransferListenerInvoker(List<TransferListener> listeners) {
        this.listeners = List.copyOf(Validate.paramNotNull(listeners, "listeners"));
    }

    @Override
    public void transferInitiated(Context.TransferInitiated context) {
        if (!initiated.getAndSet(true)) {
            forEach("transferInitiated", listener -> listener.transferInitiated(context));
        }
    }

    @Override
    public void bytesTransferred(Context.BytesTransferred context) {
        forEach("bytesTransferred", listener -> listener.bytesTransferred(context));
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
        if (!complete.getAndSet(true)) {
            forEach("transferComplete", listener -> listener.transferComplete(context));
        }
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
        if (!complete.getAndSet(true)) {
            forEach("transferFailed", listener -> listener.transferFailed(context));
        }
    }

    /** True if there is no listener to call. */
    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    private void forEach(String callback, Consumer<TransferListener> action) {
        for (TransferListener listener : listeners) {
            try {
                action.accept(listener);
            } catch (RuntimeException | Error e) {
                if (e instanceof VirtualMachineError) {
                    throw e;
                }
                warnings.warn(log, IGNORED_LISTENER_FAILURE, listener.getClass().getName() + "#" + callback,
                              () -> "Exception thrown in TransferListener " + listener + " " + callback
                                    + ", ignoring; further failures of this callback are not logged", e);
            }
        }
    }
}
