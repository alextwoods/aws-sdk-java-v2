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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

/**
 * Transfer Manager logging through the SDK facade (Requirements 21.9, 21.11, 21.12): an ignored listener failure is
 * logged at WARN once per listener and callback for the transfer, and {@link LoggingTransferListener} logs progress and
 * the caller-visible failure at DEBUG, never at INFO or WARN.
 */
class TransferListenerLoggingTest {

    @Test
    void anIgnoredListenerFailureIsWarnedOncePerCallbackForTheTransfer() {
        AtomicInteger delivered = new AtomicInteger();
        TransferListener failing = new TransferListener() {
            @Override
            public void bytesTransferred(Context.BytesTransferred context) {
                throw new IllegalStateException("listener bug");
            }

            @Override
            public void transferComplete(Context.TransferComplete context) {
                throw new IllegalStateException("listener bug");
            }
        };
        TransferListener counting = new TransferListener() {
            @Override
            public void bytesTransferred(Context.BytesTransferred context) {
                delivered.incrementAndGet();
            }
        };
        try (Capture capture = Capture.of(TransferListener.class.getName(), Level.ALL)) {
            TransferListenerInvoker invoker = new TransferListenerInvoker(List.of(failing, counting));
            for (int i = 0; i < 5; i++) {
                invoker.bytesTransferred(context(i * 10L));
            }
            invoker.transferComplete(context(50L));

            assertThat(delivered).hasValue(5);
            assertThat(capture.records()).hasSize(2).allSatisfy(r -> {
                assertThat(r.getLevel()).isEqualTo(Level.WARNING);
                assertThat(r.getThrown()).hasMessage("listener bug");
            });
            assertThat(capture.records().get(0).getMessage()).contains("bytesTransferred");
            assertThat(capture.records().get(1).getMessage()).contains("transferComplete");

            // Another transfer has its own suppression scope.
            new TransferListenerInvoker(List.of(failing)).bytesTransferred(context(0L));
            assertThat(capture.records()).hasSize(3);
        }
    }

    @Test
    void loggingTransferListenerLogsProgressAndFailureAtDebugOnly() {
        try (Capture capture = Capture.of(LoggingTransferListener.class.getName(), Level.ALL)) {
            LoggingTransferListener listener = LoggingTransferListener.create(2);
            listener.transferInitiated(context(0L));
            listener.bytesTransferred(context(50L));
            listener.transferComplete(context(100L));
            IllegalStateException failure = new IllegalStateException("failed");
            listener.transferFailed(TransferListenerFailedContext.builder()
                                                                 .transferContext(context(100L))
                                                                 .exception(failure)
                                                                 .build());

            assertThat(capture.records()).isNotEmpty()
                                         .allSatisfy(r -> assertThat(r.getLevel()).isEqualTo(Level.FINE));
            assertThat(capture.records().stream().map(LogRecord::getMessage))
                .contains("Transfer initiated...", "Transfer complete!", "Transfer failed.", "|= | 50.0%");
            assertThat(capture.records().get(capture.records().size() - 1).getThrown()).isSameAs(failure);
        }
    }

    private static TransferListenerContext context(long transferred) {
        return TransferListenerContext.builder()
                                      .progressSnapshot(DefaultTransferProgressSnapshot.builder()
                                                                                       .transferredBytes(transferred)
                                                                                       .totalBytes(100L)
                                                                                       .build())
                                      .build();
    }

    /** Captures a {@code java.util.logging} logger, the default backend of the SDK logging facade. */
    private static final class Capture implements AutoCloseable {
        private final java.util.logging.Logger logger;
        private final Level previous;
        private final boolean previousUseParent;
        private final List<LogRecord> records = new CopyOnWriteArrayList<>();
        private final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        private Capture(String name, Level level) {
            logger = java.util.logging.Logger.getLogger(name);
            previous = logger.getLevel();
            previousUseParent = logger.getUseParentHandlers();
            handler.setLevel(Level.ALL);
            logger.setLevel(level);
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
        }

        static Capture of(String name, Level level) {
            return new Capture(name, level);
        }

        List<LogRecord> records() {
            return records;
        }

        @Override
        public void close() {
            logger.removeHandler(handler);
            logger.setLevel(previous);
            logger.setUseParentHandlers(previousUseParent);
        }
    }
}
