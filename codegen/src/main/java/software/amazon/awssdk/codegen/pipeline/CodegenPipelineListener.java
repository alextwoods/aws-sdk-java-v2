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

package software.amazon.awssdk.codegen.pipeline;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Receives {@link CodegenPipeline} progress and warnings. The default implementation logs them.
 */
@SdkInternalApi
public interface CodegenPipelineListener {

    /**
     * The default listener: stage starts at DEBUG and warnings at WARN.
     */
    static CodegenPipelineListener logging() {
        return LoggingListener.INSTANCE;
    }

    /**
     * Called when a stage starts for a service, in stage order.
     */
    default void stageStarted(String service, CodegenPipeline.Stage stage) {
    }

    /**
     * Called for a warning about a service, for example a skipped customization.
     */
    default void warning(String service, String message) {
    }

    final class LoggingListener implements CodegenPipelineListener {
        private static final LoggingListener INSTANCE = new LoggingListener();
        private static final Logger log = Logger.loggerFor(CodegenPipeline.class);

        private LoggingListener() {
        }

        @Override
        public void stageStarted(String service, CodegenPipeline.Stage stage) {
            log.debug(() -> "Stage " + stage + " started for " + service);
        }

        @Override
        public void warning(String service, String message) {
            log.warn(() -> message);
        }
    }
}
