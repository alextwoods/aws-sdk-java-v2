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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkResponseMetadata;
import software.amazon.awssdk.core.internal.exception.SdkExceptionCallMetadata;
import software.amazon.smithy.model.shapes.ShapeId;

class SdkExceptionTest {

    @Test
    void messageDefaultsToTheCauseMessage() {
        IllegalStateException cause = new IllegalStateException("boom");
        SdkException exception = SdkException.builder().cause(cause).numAttempts(2).build();
        assertEquals("boom", exception.getMessage());
        assertEquals("boom", exception.rawMessage());
        assertSame(cause, exception.getCause());
        assertEquals(2, exception.numAttempts());
        assertFalse(exception.retryable());
        assertEquals("boom", exception.toBuilder().build().rawMessage());
    }

    @Test
    void stackTraceCanBeMadeUnwritable() {
        assertEquals(0, SdkClientException.builder().message("m").writableStackTrace(false).build()
                                          .getStackTrace().length);
    }

    @Test
    void clientExceptionAppendsTheAttemptCount() {
        assertEquals("m", SdkClientException.create("m").getMessage());
        assertEquals("m (SDK Attempt Count: 3)", SdkClientException.builder().message("m").numAttempts(3).build()
                                                                   .getMessage());
    }

    @Test
    void serviceExceptionCarriesResponseMetadataThroughToBuilder() {
        SdkServiceException exception = SdkServiceException.builder().message("m").requestId("r").extendedRequestId("x")
                                                            .statusCode(429).build();
        assertTrue(exception.isThrottlingException());
        assertFalse(exception.isRetryableException());
        assertFalse(exception.isClockSkewException());
        SdkServiceException copy = (SdkServiceException) exception.toBuilder().build();
        assertEquals("r", copy.requestId());
        assertEquals("x", copy.extendedRequestId());
        assertEquals(429, copy.statusCode());
    }

    @Test
    void recordedCallMetadataIsWrittenOnceAndExposed() {
        SdkServiceException exception = SdkServiceException.builder().message("m").requestId("r").statusCode(503)
                                                            .build();
        Map<String, String> transport = Map.of(SdkResponseMetadata.HTTP_CLIENT, "c");
        SdkExceptionCallMetadata.record(exception, 3, transport);
        SdkExceptionCallMetadata.record(exception, 7, Map.of());

        assertEquals(3, exception.numAttempts());
        SdkResponseMetadata metadata = exception.sdkResponseMetadata();
        assertEquals(Optional.of("r"), metadata.requestId());
        assertEquals(Optional.empty(), metadata.extendedRequestId());
        assertEquals(OptionalInt.of(503), metadata.statusCode());
        assertEquals(OptionalInt.of(3), metadata.attemptCount());
        assertEquals(transport, metadata.transportMetadata());
        assertEquals(3, exception.toBuilder().build().numAttempts(), "the recorded count is copied by toBuilder");
    }

    @Test
    void unrecordedOrPreAttemptMetadataIsAbsent() {
        SdkServiceException unknownStatus = SdkServiceException.builder().message("m").build();
        assertEquals(SdkResponseMetadata.empty(), unknownStatus.sdkResponseMetadata(),
                     "status 0 (V2's unknown value) and missing IDs are absent");

        SdkClientException beforeAnyAttempt = SdkClientException.create("m");
        SdkExceptionCallMetadata.record(beforeAnyAttempt, 0, Map.of());
        assertNull(beforeAnyAttempt.numAttempts());
        assertEquals(OptionalInt.empty(), beforeAnyAttempt.sdkResponseMetadata().attemptCount());
    }

    @Test
    void missingRequiredMembersListsEveryMemberWithoutValues() {
        ShapeId shape = ShapeId.from("com.example#Widget");
        List<ShapeId> missing = new ArrayList<>(List.of(shape.withMember("name"), shape.withMember("size")));
        MissingRequiredMembersException exception = MissingRequiredMembersException.create(shape, missing);
        missing.clear();
        assertEquals(shape, exception.shape());
        assertEquals(List.of(shape.withMember("name"), shape.withMember("size")), exception.missingMembers());
        assertEquals("Missing required members of com.example#Widget: com.example#Widget$name, "
                     + "com.example#Widget$size", exception.getMessage());
        assertInstanceOf(SdkClientException.class, exception);
        assertFalse(exception.retryable());
        assertEquals(exception.missingMembers(), exception.toBuilder().build().missingMembers());
        assertThrows(IllegalArgumentException.class, () -> MissingRequiredMembersException.create(shape, List.of()));
    }
}
