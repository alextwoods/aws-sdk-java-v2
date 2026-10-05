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

package software.amazon.awssdk.protocols.core.serde;

import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.protocols.core.SdkProtocolException;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Where a protocol runtime is reading or writing: the protocol and operation, used to locate every
 * {@link SdkProtocolException} it reports.
 *
 * @param protocolId absolute shape ID of the protocol trait
 * @param operationId absolute shape ID of the operation
 */
public record WireContext(ShapeId protocolId, ShapeId operationId) {
    /** The wire location of the message body. */
    public static final String BODY = "body";

    /** The wire location of the status code. */
    public static final String STATUS_CODE = "status code";

    public WireContext {
        Objects.requireNonNull(protocolId, "protocolId");
        Objects.requireNonNull(operationId, "operationId");
    }

    /** The wire location of the header {@code name}. */
    public static String header(String name) {
        return "header " + name;
    }

    /** The wire location of the query parameter {@code name}. */
    public static String query(String name) {
        return "query parameter " + name;
    }

    /** The wire location of the URI label {@code name}. */
    public static String label(String name) {
        return "URI label " + name;
    }

    /**
     * A located wire-input failure.
     *
     * @param shapeId the shape being read or written
     * @param memberPath the member path from the operation's input, output or error structure
     * @param wireLocation where on the wire, or {@code null}; never a wire value
     * @param message what was violated; never a wire value
     */
    public SdkProtocolException failure(ShapeId shapeId, List<String> memberPath, String wireLocation,
                                        String message) {
        return SdkProtocolException.builder()
                                   .protocolId(protocolId)
                                   .operationId(operationId)
                                   .shapeId(shapeId)
                                   .memberPath(memberPath)
                                   .wireLocation(wireLocation)
                                   .message(message)
                                   .build();
    }
}
