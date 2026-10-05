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

package software.amazon.awssdk.eventstream;

/**
 * The typed header value kinds of the AWS event stream binary message format, with their wire type codes. A boolean
 * is encoded as its type alone ({@link #BOOLEAN_TRUE} or {@link #BOOLEAN_FALSE}).
 */
public enum HeaderType {
    BOOLEAN_TRUE(0),
    BOOLEAN_FALSE(1),
    BYTE(2),
    SHORT(3),
    INTEGER(4),
    LONG(5),
    BYTE_ARRAY(6),
    STRING(7),
    /** Milliseconds since the epoch, as a signed 64-bit integer. */
    TIMESTAMP(8),
    UUID(9);

    private static final HeaderType[] BY_CODE = values();

    private final int code;

    HeaderType(int code) {
        this.code = code;
    }

    /** The wire type code. */
    public int code() {
        return code;
    }

    /** The type of wire code {@code code}, or {@code null} if no type has that code. */
    static HeaderType fromCode(int code) {
        return code >= 0 && code < BY_CODE.length ? BY_CODE[code] : null;
    }
}
