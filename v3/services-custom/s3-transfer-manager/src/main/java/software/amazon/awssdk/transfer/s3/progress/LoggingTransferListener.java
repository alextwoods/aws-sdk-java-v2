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

package software.amazon.awssdk.transfer.s3.progress;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.utils.Logger;

/**
 * An example implementation of {@link TransferListener} that logs a progress bar at the {@code DEBUG} level of the SDK
 * logger ({@link Logger}, JDK platform logging) named after this class. ADAPTED from V2, which logs through SLF4J at
 * {@code INFO} and logs a failed transfer at {@code WARN}: V3 runtime artifacts log nothing at {@code INFO}, and the
 * failure, which the transfer's completion future reports to the caller, is logged at {@code DEBUG}
 * (Requirements 21.9, 21.11). This implementation
 * effectively rate-limits how frequently updates are logged by only logging when a new "tick" advances in the progress bar. By
 * default, the progress bar has {@value #DEFAULT_MAX_TICKS} ticks, meaning an update is only logged, at most, once every 5%.
 */
public final class LoggingTransferListener implements TransferListener {
    private static final Logger log = Logger.loggerFor(LoggingTransferListener.class);
    private static final int DEFAULT_MAX_TICKS = 20;
    private final ProgressBar progressBar;

    private LoggingTransferListener(int maxTicks) {
        progressBar = new ProgressBar(maxTicks);
    }

    /**
     * Create an instance of {@link LoggingTransferListener} with a custom {@code maxTicks} value.
     *
     * @param maxTicks the number of ticks in the logged progress bar
     */
    public static LoggingTransferListener create(int maxTicks) {
        return new LoggingTransferListener(maxTicks);
    }

    /**
     * Create an instance of {@link LoggingTransferListener} with the default configuration.
     */
    public static LoggingTransferListener create() {
        return new LoggingTransferListener(DEFAULT_MAX_TICKS);
    }

    @Override
    public void transferInitiated(Context.TransferInitiated context) {
        log.debug(() -> "Transfer initiated...");
        context.progressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void bytesTransferred(Context.BytesTransferred context) {
        context.progressSnapshot().ratioTransferred().ifPresent(progressBar::update);
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
        context.progressSnapshot().ratioTransferred().ifPresent(progressBar::update);
        log.debug(() -> "Transfer complete!");
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
        log.debug(() -> "Transfer failed.", context.exception());
    }

    private static class ProgressBar {
        private final int maxTicks;
        private final AtomicInteger prevTicks = new AtomicInteger(-1);

        ProgressBar(int maxTicks) {
            this.maxTicks = maxTicks;
        }

        void update(double ratio) {
            int ticks = (int) Math.floor(ratio * maxTicks);
            if (prevTicks.getAndSet(ticks) != ticks) {
                log.debug(() -> String.format("|%s%s| %s",
                                             "=".repeat(ticks),
                                             " ".repeat(maxTicks - ticks),
                                             round(ratio * 100, 1) + "%"));
            }
        }

        private static double round(double value, int places) {
            BigDecimal bd = BigDecimal.valueOf(value);
            bd = bd.setScale(places, RoundingMode.FLOOR);
            return bd.doubleValue();
        }
    }
}
