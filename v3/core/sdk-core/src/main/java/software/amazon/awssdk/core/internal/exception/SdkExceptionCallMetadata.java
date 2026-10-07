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

package software.amazon.awssdk.core.internal.exception;

import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * Records the terminal call metadata (attempt count and transport facts) on the {@link SdkException} a call
 * surfaces, without a public mutator on the exception: the AWS pipeline's one exception-mapping path calls
 * {@link #record} exactly once per terminal failure (Requirement 21.1). The exception itself installs the
 * {@link Recorder} from its static initializer, which has run before any exception instance can be passed here.
 *
 * <p>Internal: the AWS pipeline is the only supported caller, and the class may change without notice.
 */
public final class SdkExceptionCallMetadata {
    private static volatile Recorder recorder;

    private SdkExceptionCallMetadata() {
    }

    /**
     * Records {@code attempts} and {@code transportMetadata} on {@code exception}, unless call metadata was already
     * recorded on it (an exception instance surfaced by more than one call keeps the first call's metadata).
     *
     * @param attempts the number of attempts the call made; values below one record no attempt count
     * @param transportMetadata immutable safe transport facts, possibly empty
     */
    public static void record(SdkException exception, int attempts, Map<String, String> transportMetadata) {
        Objects.requireNonNull(exception, "exception");
        Objects.requireNonNull(transportMetadata, "transportMetadata");
        // SdkException's static initializer installed the recorder before the instance existed.
        recorder.record(exception, attempts, transportMetadata);
    }

    /**
     * Installs the exception's recorder; called once, by {@link SdkException}'s static initializer.
     *
     * @throws IllegalStateException if a recorder is already installed
     */
    public static synchronized void install(Recorder installed) {
        Objects.requireNonNull(installed, "installed");
        if (recorder != null) {
            throw new IllegalStateException("The SdkException call-metadata recorder is already installed");
        }
        recorder = installed;
    }

    /** Writes the call metadata into an exception's private state. */
    public interface Recorder {
        void record(SdkException exception, int attempts, Map<String, String> transportMetadata);
    }
}
