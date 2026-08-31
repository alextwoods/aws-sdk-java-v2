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

package software.amazon.awssdk.protocols.json;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.protocols.json.internal.marshall.FastJsonGenerator;

/**
 * Pre-encodes JSON field names as complete {@code "name":} UTF-8 token bytes for
 * {@link StructuredJsonGenerator#writeFieldName(String, byte[])}. Generated model classes call this once
 * per field in a static initializer so that field-name bytes are copied, not re-encoded, on every request.
 */
@SdkProtectedApi
public final class JsonFieldNameToken {

    private JsonFieldNameToken() {
    }

    /**
     * @param fieldName the field name.
     * @return the field name encoded as {@code "name":} token bytes, escaped exactly as
     * {@link StructuredJsonGenerator#writeFieldName(String)} would write it.
     */
    public static byte[] of(String fieldName) {
        return FastJsonGenerator.encodeFieldNameToken(fieldName);
    }
}
