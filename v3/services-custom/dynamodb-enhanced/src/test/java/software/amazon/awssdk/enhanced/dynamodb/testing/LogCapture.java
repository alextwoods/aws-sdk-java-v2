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
package software.amazon.awssdk.enhanced.dynamodb.testing;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Captures the records of one logger. The enhanced client logs through {@link System.Logger}, which the JDK routes
 * to {@code java.util.logging} when no other {@code System.LoggerFinder} is installed (as in these tests); its DEBUG
 * level is {@link Level#FINE}.
 */
public final class LogCapture implements AutoCloseable {
    private final Logger logger;
    private final Level previousLevel;
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

    public LogCapture(Class<?> type) {
        this.logger = Logger.getLogger(type.getName());
        this.previousLevel = logger.getLevel();
        handler.setLevel(Level.ALL);
        logger.setLevel(Level.FINE);
        logger.addHandler(handler);
    }

    public List<LogRecord> records() {
        return List.copyOf(records);
    }

    @Override
    public void close() {
        logger.removeHandler(handler);
        logger.setLevel(previousLevel);
    }
}
