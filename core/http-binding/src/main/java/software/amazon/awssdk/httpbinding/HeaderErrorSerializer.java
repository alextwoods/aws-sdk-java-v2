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

package software.amazon.awssdk.httpbinding;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.Schema;

/**
 * Writes the error type of a modeled error into the headers of a serialized error response.
 *
 * <p>Used by {@link ResponseSerializer} for error responses (the Response_Generator and mocks; clients only read
 * errors).
 */
@SdkPublicApi
@FunctionalInterface
public interface HeaderErrorSerializer {
    /**
     * Writes {@code X-Amzn-Errortype} with the absolute shape ID of the error.
     */
    HeaderErrorSerializer AMZN_ERROR_HEADER =
        (schema, headers) -> headers.put("X-Amzn-Errortype", Collections.singletonList(schema.id().toString()));

    /**
     * Writes nothing.
     */
    HeaderErrorSerializer NONE = (schema, headers) -> {
    };

    /**
     * Writes the error type.
     *
     * @param errorSchema the schema of the error being serialized
     * @param headers the response headers, keyed case-insensitively; modifiable
     */
    void writeErrorType(Schema errorSchema, Map<String, List<String>> headers);
}
