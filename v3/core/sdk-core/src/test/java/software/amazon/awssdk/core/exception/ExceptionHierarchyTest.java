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

package software.amazon.awssdk.core.exception;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ExceptionHierarchyTest {
    @Test
    void configurationExceptionsAreNonRetryableClientExceptionsNamingTheOption() {
        IllegalStateException cause = new IllegalStateException("bad");
        SdkClientConfigurationException e = SdkClientConfigurationException.builder()
                                                                            .message("Unsupported option")
                                                                            .option("useArnRegion")
                                                                            .cause(cause)
                                                                            .build();
        assertInstanceOf(SdkClientException.class, e);
        assertEquals(Optional.of("useArnRegion"), e.option());
        assertSame(cause, e.getCause());
        assertFalse(e.retryable());
        SdkClientConfigurationException copy = e.toBuilder().build();
        assertEquals(Optional.of("useArnRegion"), copy.option());
        assertEquals("Unsupported option", copy.rawMessage());
        assertEquals(Optional.empty(), SdkClientConfigurationException.create("m").option());
    }

    @Test
    void timeoutExceptionsIdentifyTheirScope() {
        SdkTimeoutException call = SdkTimeoutException.builder()
                                                      .scope(SdkTimeoutException.Scope.API_CALL)
                                                      .timeout(Duration.ofSeconds(30))
                                                      .attempt(2)
                                                      .build();
        assertInstanceOf(SdkClientException.class, call);
        assertEquals("The API call timed out after PT30S.", call.rawMessage());
        assertEquals(Optional.of(Duration.ofSeconds(30)), call.timeout());
        assertEquals(OptionalInt.of(2), call.attempt());
        SdkTimeoutException copy = call.toBuilder().build();
        assertEquals(SdkTimeoutException.Scope.API_CALL, copy.scope());
        assertEquals(OptionalInt.of(2), copy.attempt());

        SdkTimeoutException attempt = SdkTimeoutException.builder()
                                                         .scope(SdkTimeoutException.Scope.API_CALL_ATTEMPT)
                                                         .build();
        assertEquals("An API call attempt timed out.", attempt.rawMessage());
        assertEquals(Optional.empty(), attempt.timeout());
        assertEquals(OptionalInt.empty(), attempt.attempt());
        assertThrows(NullPointerException.class, () -> SdkTimeoutException.builder().build());
    }

    @Test
    void closeExceptionSuppressesEveryFailureInOrder() {
        IOException first = new IOException("first");
        IllegalStateException second = new IllegalStateException("second");
        List<Throwable> failures = new ArrayList<>(List.of(first, second));
        SdkCloseException e = SdkCloseException.create(failures);
        failures.clear();
        assertInstanceOf(SdkClientException.class, e);
        assertNull(e.getCause());
        assertArrayEquals(new Throwable[] {first, second}, e.getSuppressed());
        assertEquals(List.of(first, second), e.failures());
        assertEquals("Closing 2 SDK-owned resource(s) failed.", e.rawMessage());
        assertFalse(e.retryable());
        assertEquals(List.of(first, second), e.toBuilder().build().failures());
        assertThrows(IllegalArgumentException.class, () -> SdkCloseException.create(List.of()));
    }
}
