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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeId;

class ModelConflictExceptionTest {
    private static final ShapeId OPERATION = ShapeId.from("com.example#PutThing");

    @Test
    void create_identifiesOperationAndSettingAndIsNotRetryable() {
        ModelConflictException e = ModelConflictException.create(OPERATION, "ChecksumAlgorithm", "MD5 is unsupported");

        assertInstanceOf(SdkClientException.class, e);
        assertEquals(OPERATION, e.operation());
        assertEquals("ChecksumAlgorithm", e.setting());
        assertFalse(e.retryable());
        assertTrue(e.getMessage().contains("com.example#PutThing"));
        assertTrue(e.getMessage().contains("'ChecksumAlgorithm'"));
        assertTrue(e.getMessage().contains("MD5 is unsupported"));
    }

    @Test
    void toBuilder_keepsTypeAndFacts() {
        ModelConflictException e = ModelConflictException.create(OPERATION, "Content-Encoding", "detail");

        ModelConflictException copy = e.toBuilder().numAttempts(1).build();

        assertEquals(OPERATION, copy.operation());
        assertEquals("Content-Encoding", copy.setting());
        assertEquals(e.rawMessage(), copy.rawMessage());
        assertTrue(copy.getMessage().endsWith("(SDK Attempt Count: 1)"));
    }

    @Test
    void build_requiresOperationAndSetting() {
        assertThrows(NullPointerException.class, () -> ModelConflictException.builder().setting("x").build());
        assertThrows(NullPointerException.class, () -> ModelConflictException.builder().operation(OPERATION).build());
    }
}
