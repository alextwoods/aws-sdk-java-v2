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

/**
 * Implemented by generated model builders whose members all bind to the payload of a JSON-family
 * protocol, allowing the shape to consume a {@link StructuredJsonReader} with straight-line generated
 * code (direct field writes, no reflective setter dispatch, collections built exactly once) instead
 * of the generic {@code SdkField} unmarshalling loop.
 *
 * <p>The JSON protocol unmarshaller dispatches to {@link #readJsonFields} whenever a builder
 * implements this interface; builders from older generated code (or shapes with non-payload members)
 * fall back to the generic loop. Generated implementations must produce objects identical to the
 * generic loop's output.
 */
@SdkProtectedApi
public interface StructuredJsonReadable {

    /**
     * Reads this shape's fields from the reader, which is positioned at the shape's opening
     * {@code '{'}.
     */
    void readJsonFields(StructuredJsonReader reader);
}
