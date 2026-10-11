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

package software.amazon.awssdk.schema.serde;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Thrown when a value cannot be serialized or deserialized, when a document cannot be coerced to the requested type,
 * or when a trait value does not match the trait's modeled shape.
 *
 * <p>This is the SDK port of smithy-java {@code software.amazon.smithy.java.core.serde.SerializationException}. It
 * extends {@link RuntimeException} because this module sits below {@code sdk-core}; client protocols wrap it in an
 * SDK client exception.
 */
@SdkPublicApi
public class SerializationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a message.
     *
     * @param message the message
     */
    public SerializationException(String message) {
        super(message);
    }

    /**
     * Creates the exception from a cause, using the cause's message.
     *
     * @param cause the cause
     */
    public SerializationException(Throwable cause) {
        this(cause.getMessage(), cause);
    }

    /**
     * Creates the exception with a message and a cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
