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

/**
 * Thrown when a document has no discriminator where one is required, or its discriminator cannot be parsed into a
 * ShapeId.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.document.DiscriminatorException}.
 */
@SdkPublicApi
public class DiscriminatorException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message the detail message
     */
    public DiscriminatorException(String message) {
        super(message);
    }

    /**
     * @param message the detail message
     * @param cause the cause
     */
    public DiscriminatorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause the cause
     */
    public DiscriminatorException(Throwable cause) {
        super(cause);
    }
}
