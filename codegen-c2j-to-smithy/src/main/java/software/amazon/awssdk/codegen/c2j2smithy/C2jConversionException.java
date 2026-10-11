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

package software.amazon.awssdk.codegen.c2j2smithy;

import java.util.Objects;

/**
 * Thrown when a C2J shape, operation or service property cannot be converted into the Smithy model. The service then
 * fails to generate, and no sources are written for it.
 */
public final class C2jConversionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String service;
    private final String c2jName;

    /**
     * @param service the name of the service being converted
     * @param c2jName the C2J shape, member ({@code Shape$member}), operation or property name that failed
     * @param reason why the conversion failed
     */
    public C2jConversionException(String service, String c2jName, String reason) {
        this(service, c2jName, reason, null);
    }

    /**
     * @param service the name of the service being converted
     * @param c2jName the C2J shape, member ({@code Shape$member}), operation or property name that failed
     * @param reason why the conversion failed
     * @param cause the underlying failure, or null
     */
    public C2jConversionException(String service, String c2jName, String reason, Throwable cause) {
        super(message(service, c2jName, reason), cause);
        this.service = Objects.requireNonNull(service, "service");
        this.c2jName = Objects.requireNonNull(c2jName, "c2jName");
    }

    /**
     * The name of the service whose conversion failed.
     */
    public String service() {
        return service;
    }

    /**
     * The C2J shape, member ({@code Shape$member}), operation or property name that could not be converted.
     */
    public String c2jName() {
        return c2jName;
    }

    private static String message(String service, String c2jName, String reason) {
        return "Unable to convert C2J model of service " + service + ": " + c2jName + ": " + reason;
    }
}
