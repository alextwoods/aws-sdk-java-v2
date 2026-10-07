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

package software.amazon.awssdk.metrics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Captures the records of one logger through the default JDK platform-logging backend ({@code java.util.logging}),
 * which the SDK logging facade writes to when no application logging bridge is installed. The facade's
 * {@code TRACE}, {@code DEBUG} and {@code WARN} levels arrive as {@link Level#FINER}, {@link Level#FINE} and
 * {@link Level#WARNING}.
 */
public final class LogCapture implements AutoCloseable {
    private final java.util.logging.Logger logger;
    private final Level previousLevel;
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

    private LogCapture(String name, Level level) {
        this.logger = java.util.logging.Logger.getLogger(name);
        this.previousLevel = logger.getLevel();
        this.previousUseParent = logger.getUseParentHandlers();
        handler.setLevel(Level.ALL);
        logger.setLevel(level);
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
    }

    /** Captures every record of logger {@code name} at {@code level} or above. */
    public static LogCapture of(String name, Level level) {
        return new LogCapture(name, level);
    }

    public List<LogRecord> records() {
        return records;
    }

    public List<String> messages() {
        return records.stream().map(LogRecord::getMessage).toList();
    }

    public List<Level> levels() {
        return records.stream().map(LogRecord::getLevel).toList();
    }

    @Override
    public void close() {
        logger.removeHandler(handler);
        logger.setLevel(previousLevel);
        logger.setUseParentHandlers(previousUseParent);
    }
}
