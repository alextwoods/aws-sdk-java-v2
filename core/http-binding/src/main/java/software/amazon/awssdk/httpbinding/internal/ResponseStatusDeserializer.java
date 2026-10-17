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

package software.amazon.awssdk.httpbinding.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SpecificShapeDeserializer;

/**
 * Reads the HTTP status code into an {@code @httpResponseCode} member. Port of smithy-java
 * {@code ResponseStatusDeserializer}.
 */
@SdkInternalApi
final class ResponseStatusDeserializer extends SpecificShapeDeserializer {
    private final int responseStatus;

    ResponseStatusDeserializer(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    @Override
    public int readInteger(Schema schema) {
        return responseStatus;
    }
}
