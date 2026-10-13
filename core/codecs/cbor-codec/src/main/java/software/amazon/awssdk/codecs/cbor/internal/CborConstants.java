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

package software.amazon.awssdk.codecs.cbor.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * CBOR (RFC 8949) major types, additional-information values, simple values and tags used by the codec.
 *
 * <p>Port of smithy-java {@code CborConstants}, merged with the constants that smithy-java's {@code CborDeserializer}
 * declares itself.
 */
@SdkInternalApi
final class CborConstants {
    static final int MAJOR_TYPE_SHIFT = 5;
    static final int MAJOR_TYPE_MASK = 0b111_00000;
    static final int MINOR_TYPE_MASK = 0b0001_1111;

    // Major types, unshifted (the top three bits of the initial byte).
    static final byte MAJOR_TYPE_POSINT = 0;
    static final byte MAJOR_TYPE_NEGINT = 1;
    static final byte MAJOR_TYPE_BYTESTRING = 2;
    static final byte MAJOR_TYPE_TEXTSTRING = 3;
    static final byte MAJOR_TYPE_ARRAY = 4;
    static final byte MAJOR_TYPE_MAP = 5;
    static final byte MAJOR_TYPE_TAG = 6;
    static final byte MAJOR_TYPE_SIMPLE = 7;

    // Major types, shifted into place in the initial byte.
    static final int TYPE_POSINT = MAJOR_TYPE_POSINT << MAJOR_TYPE_SHIFT;
    static final int TYPE_NEGINT = MAJOR_TYPE_NEGINT << MAJOR_TYPE_SHIFT;
    static final int TYPE_BYTESTRING = MAJOR_TYPE_BYTESTRING << MAJOR_TYPE_SHIFT;
    static final int TYPE_TEXTSTRING = MAJOR_TYPE_TEXTSTRING << MAJOR_TYPE_SHIFT;
    static final int TYPE_ARRAY = MAJOR_TYPE_ARRAY << MAJOR_TYPE_SHIFT;
    static final int TYPE_MAP = MAJOR_TYPE_MAP << MAJOR_TYPE_SHIFT;
    static final int TYPE_TAG = MAJOR_TYPE_TAG << MAJOR_TYPE_SHIFT;
    static final int TYPE_SIMPLE = MAJOR_TYPE_SIMPLE << MAJOR_TYPE_SHIFT;

    // Additional information (minor type) values.
    static final int ZERO_BYTES = 23;
    static final int ONE_BYTE = 24;
    static final int TWO_BYTES = 25;
    static final int FOUR_BYTES = 26;
    static final int EIGHT_BYTES = 27;
    static final int INDEFINITE = 31;

    // Simple values (major type 7).
    static final int SIMPLE_FALSE = 20;
    static final int SIMPLE_TRUE = 21;
    static final int SIMPLE_NULL = 22;
    static final int SIMPLE_UNDEFINED = 23;
    static final int SIMPLE_VALUE_1 = 24;
    static final int SIMPLE_FLOAT = 26;
    static final int SIMPLE_DOUBLE = 27;

    static final int TYPE_SIMPLE_FALSE = TYPE_SIMPLE | SIMPLE_FALSE;
    static final int TYPE_SIMPLE_TRUE = TYPE_SIMPLE | SIMPLE_TRUE;
    static final int TYPE_SIMPLE_NULL = TYPE_SIMPLE | SIMPLE_NULL;
    static final int TYPE_SIMPLE_FLOAT = TYPE_SIMPLE | SIMPLE_FLOAT;
    static final int TYPE_SIMPLE_DOUBLE = TYPE_SIMPLE | SIMPLE_DOUBLE;
    static final int TYPE_SIMPLE_BREAK_STREAM = TYPE_SIMPLE | INDEFINITE;

    static final byte SIMPLE_STREAM_BREAK = (byte) TYPE_SIMPLE_BREAK_STREAM;

    // Tags (major type 6).
    static final byte TAG_TIME_EPOCH = 1;
    static final byte TAG_POS_BIG_INT = 2;
    static final byte TAG_NEG_BIG_INT = 3;
    static final byte TAG_DECIMAL = 4;

    private CborConstants() {
    }
}
