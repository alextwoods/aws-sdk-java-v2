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

package software.amazon.awssdk.schema.serde.document;

import software.amazon.awssdk.annotations.SdkPublicApi;

// CHECKSTYLE:OFF - Keeps the smithy-java name: the class holds bit flags, not an enumeration of values.
/**
 * Flags that can be passed to {@link Document#equals(Object, Object, int)}.
 */
@SdkPublicApi
public final class DocumentEqualsFlags {
    // CHECKSTYLE:ON

    /**
     * Compares numbers using widening type promotion, like JLS 5.1.2, so that an integer document holding {@code 1}
     * equals a long document holding {@code 1}.
     */
    public static final int NUMBER_PROMOTION = 1;

    private DocumentEqualsFlags() {
    }
}
