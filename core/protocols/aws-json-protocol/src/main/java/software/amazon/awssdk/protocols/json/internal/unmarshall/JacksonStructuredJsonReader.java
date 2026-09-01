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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.protocols.json.JsonMemberTable;
import software.amazon.awssdk.protocols.json.StructuredJsonReader;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonToken;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.DateUtils;

/**
 * {@link StructuredJsonReader} over a Jackson token stream. Works for both JSON text and CBOR
 * (whichever {@code JsonParser} it wraps), applying exactly the token-to-value conversions of
 * {@link JsonUnmarshallingParser}'s generic loop.
 */
@SdkInternalApi
final class JacksonStructuredJsonReader implements StructuredJsonReader {

    private final JsonParser parser;
    private final TimestampFormatTrait.Format defaultTimestampFormat;

    /**
     * One-token lookahead, set when a peek (e.g. {@link #readNullIfPresent}) or the dispatch entry
     * point has already consumed the next token from the underlying parser.
     */
    private JsonToken pending;

    JacksonStructuredJsonReader(JsonParser parser,
                                TimestampFormatTrait.Format defaultTimestampFormat,
                                JsonToken alreadyConsumed) {
        this.parser = parser;
        this.defaultTimestampFormat = defaultTimestampFormat;
        this.pending = alreadyConsumed;
    }

    private JsonToken advance() {
        JsonToken token = pending;
        if (token != null) {
            pending = null;
            return token;
        }
        try {
            return parser.nextToken();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <T> void readStruct(T state, JsonMemberTable table, StructMemberConsumer<T> consumer) {
        try {
            JsonToken token = advance();
            expect(token, JsonToken.START_OBJECT);
            JsonToken current = parser.nextToken();
            while (current != JsonToken.END_OBJECT) {
                if (current != JsonToken.FIELD_NAME) {
                    throw new JsonParseException("expecting field name, got: " + current);
                }
                // Jackson canonicalizes field names, so this String is cached across documents.
                int memberIndex = table.indexOf(parser.getText());
                JsonToken valueToken = parser.nextToken();
                if (memberIndex < 0) {
                    // Unknown key: skip the whole value.
                    skipValue(valueToken);
                } else if (valueToken != JsonToken.VALUE_NULL) {
                    pending = valueToken;
                    consumer.accept(state, memberIndex, this);
                    if (pending != null) {
                        throw new IllegalStateException("Struct member consumer did not consume the value for member "
                                                        + memberIndex);
                    }
                }
                // Null-valued members are skipped: that leaves the builder in the same state the
                // generic loop's set-to-null would (see StructuredJsonReadable contract).
                current = parser.nextToken();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <T> void readList(T state, ListElementConsumer<T> consumer) {
        try {
            JsonToken token = advance();
            expect(token, JsonToken.START_ARRAY);
            JsonToken current = parser.nextToken();
            while (current != JsonToken.END_ARRAY) {
                pending = current;
                consumer.accept(state, this);
                current = parser.nextToken();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <T> void readStringMap(T state, MapEntryConsumer<T> consumer) {
        try {
            JsonToken token = advance();
            expect(token, JsonToken.START_OBJECT);
            JsonToken current = parser.nextToken();
            while (current != JsonToken.END_OBJECT) {
                if (current != JsonToken.FIELD_NAME) {
                    throw new JsonParseException("expecting field name, got: " + current);
                }
                String key = parser.getText();
                pending = parser.nextToken();
                consumer.accept(state, key, this);
                current = parser.nextToken();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean readNullIfPresent() {
        JsonToken token = advance();
        if (token == JsonToken.VALUE_NULL) {
            return true;
        }
        pending = token;
        return false;
    }

    @Override
    public String readString() {
        advance();
        return text();
    }

    @Override
    public int readInt() {
        try {
            JsonToken token = advance();
            if (token == JsonToken.VALUE_STRING) {
                return Integer.parseInt(parser.getText());
            }
            expect(token, JsonToken.VALUE_NUMBER_INT);
            return parser.getIntValue();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public long readLong() {
        try {
            JsonToken token = advance();
            if (token == JsonToken.VALUE_STRING) {
                return Long.parseLong(parser.getText());
            }
            expect(token, JsonToken.VALUE_NUMBER_INT);
            return parser.getLongValue();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public short readShort() {
        try {
            JsonToken token = advance();
            if (token == JsonToken.VALUE_STRING) {
                return Short.parseShort(parser.getText());
            }
            expect(token, JsonToken.VALUE_NUMBER_INT);
            return parser.getShortValue();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte readByte() {
        try {
            JsonToken token = advance();
            if (token == JsonToken.VALUE_STRING) {
                return Byte.parseByte(parser.getText());
            }
            expect(token, JsonToken.VALUE_NUMBER_INT);
            return parser.getByteValue();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public float readFloat() {
        try {
            JsonToken token = advance();
            if (token == JsonToken.VALUE_STRING) {
                return Float.parseFloat(parser.getText());
            }
            expectNumber(token);
            return parser.getFloatValue();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public double readDouble() {
        try {
            JsonToken token = advance();
            if (token == JsonToken.VALUE_STRING) {
                return Double.parseDouble(parser.getText());
            }
            expectNumber(token);
            return parser.getDoubleValue();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public BigDecimal readBigDecimal() {
        try {
            JsonToken token = advance();
            if (token == JsonToken.VALUE_STRING) {
                return new BigDecimal(parser.getText());
            }
            expectNumber(token);
            return parser.getDecimalValue();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean readBoolean() {
        try {
            JsonToken token = advance();
            if (token == JsonToken.VALUE_STRING) {
                return Boolean.parseBoolean(parser.getText());
            }
            if (token != JsonToken.VALUE_TRUE && token != JsonToken.VALUE_FALSE) {
                throw new JsonParseException("unexpected token, expecting a boolean, got: " + token);
            }
            return parser.getBooleanValue();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public SdkBytes readSdkBytes() {
        try {
            JsonToken token = advance();
            if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
                return SdkBytes.fromByteArray((byte[]) parser.getEmbeddedObject());
            }
            expect(token, JsonToken.VALUE_STRING);
            return SdkBytes.fromByteArray(BinaryUtils.fromBase64(parser.getText()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Instant readInstant(TimestampFormatTrait.Format format) {
        try {
            JsonToken token = advance();
            TimestampFormatTrait.Format resolved = format != null ? format : defaultTimestampFormat;
            switch (resolved) {
                case UNIX_TIMESTAMP_MILLIS:
                    return Instant.ofEpochMilli(parser.getLongValue());
                case UNIX_TIMESTAMP:
                    try {
                        return DateUtils.parseUnixTimestampInstant(parser.getText());
                    } catch (NumberFormatException e) {
                        throw SdkClientException.builder()
                                                .message("Unable to parse date : " + parser.getText())
                                                .cause(e)
                                                .build();
                    }
                case ISO_8601:
                    expect(token, JsonToken.VALUE_STRING);
                    return DateUtils.parseIso8601Date(parser.getText());
                case RFC_822:
                    expect(token, JsonToken.VALUE_STRING);
                    return DateUtils.parseRfc822Date(parser.getText());
                default:
                    throw SdkClientException.create("Unrecognized timestamp format - " + resolved);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String text() {
        try {
            return parser.getText();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Skips a whole value whose first token has already been consumed.
     */
    private void skipValue(JsonToken current) throws IOException {
        switch (current) {
            case VALUE_STRING:
            case VALUE_FALSE:
            case VALUE_TRUE:
            case VALUE_NULL:
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
            case VALUE_EMBEDDED_OBJECT:
                return;
            case START_OBJECT:
            case START_ARRAY:
                parser.skipChildren();
                return;
            default:
                throw new JsonParseException("unexpected JSON token - " + current);
        }
    }

    private static void expect(JsonToken actual, JsonToken expected) throws IOException {
        if (actual != expected) {
            throw new JsonParseException("unexpected token, expecting token: " + expected + ", got: " + actual);
        }
    }

    private static void expectNumber(JsonToken actual) throws IOException {
        if (actual != JsonToken.VALUE_NUMBER_INT && actual != JsonToken.VALUE_NUMBER_FLOAT) {
            throw new JsonParseException("unexpected token, expecting a number, got: " + actual);
        }
    }
}
