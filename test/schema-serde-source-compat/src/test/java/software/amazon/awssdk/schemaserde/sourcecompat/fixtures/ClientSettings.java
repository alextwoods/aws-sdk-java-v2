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

package software.amazon.awssdk.schemaserde.sourcecompat.fixtures;

import java.net.URI;
import java.time.Duration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;

/**
 * Client settings shared by the fixtures, written against the Baseline_Build public API.
 *
 * <p>Everything in this package is a source-compatibility fixture (Requirement 16.8): it uses only APIs that exist in the
 * Baseline_Build, and {@code BaselineCompileTest} compiles it against the Baseline_Build JARs.
 */
public final class ClientSettings {
    public static final Region REGION = Region.US_WEST_2;
    public static final URI ENDPOINT = URI.create("https://localhost:8443");
    public static final String FIXTURE_HEADER = "x-source-compat-fixture";

    private ClientSettings() {
    }

    public static AwsCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    }

    /**
     * An override configuration with a timeout, a custom header and an interceptor that counts calls.
     */
    public static ClientOverrideConfiguration overrideConfiguration(CallCounter counter) {
        return ClientOverrideConfiguration.builder()
                                          .apiCallTimeout(Duration.ofSeconds(30))
                                          .apiCallAttemptTimeout(Duration.ofSeconds(10))
                                          .putHeader(FIXTURE_HEADER, "1")
                                          .addExecutionInterceptor(counter)
                                          .build();
    }

    /**
     * An interceptor that counts the calls that reach {@code afterExecution}.
     */
    public static final class CallCounter implements ExecutionInterceptor {
        private int calls;

        @Override
        public synchronized void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
            calls++;
        }

        public synchronized int calls() {
            return calls;
        }
    }
}
