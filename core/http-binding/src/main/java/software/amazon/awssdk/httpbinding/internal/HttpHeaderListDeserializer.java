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

package software.amazon.awssdk.httpbinding.internal;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.SpecificShapeDeserializer;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Reads a list-typed {@code @httpHeader} member from the header's values. Each value is split on commas, honoring
 * double-quoted elements with backslash escapes; {@code http-date} timestamps, which contain a comma, are split on
 * every other comma.
 *
 * <p>Port of smithy-java {@code HttpHeaderListDeserializer}.
 */
@SdkInternalApi
final class HttpHeaderListDeserializer extends SpecificShapeDeserializer {
    private final Schema member;
    private final List<String> values;

    HttpHeaderListDeserializer(Schema member, HttpBindingSchemaExtensions.MemberBinding binding, List<String> values) {
        this.member = member;
        List<String> actualValues = new ArrayList<>();
        switch (member.listMember().type()) {
            case STRING:
            case ENUM:
            case INT_ENUM:
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_INTEGER:
            case BIG_DECIMAL:
                splitString(values, actualValues);
                break;
            case TIMESTAMP:
                // The element format: the list member's @timestampFormat, else http-date.
                if (binding.timestampFormatter().format() == TimestampFormatTrait.Format.HTTP_DATE) {
                    for (String v : values) {
                        splitHttpDates(v, actualValues);
                    }
                } else {
                    splitString(values, actualValues);
                }
                break;
            default:
                throw new SerializationException("Unsupported header list member " + member.id());
        }
        this.values = actualValues;
    }

    private static void splitString(List<String> values, List<String> accumulator) {
        for (String v : values) {
            boolean hasComma = false;
            boolean hasQuote = false;
            // One pass to find out whether quotes or commas need processing.
            for (int i = 0; i < v.length() && !hasQuote; i++) {
                char c = v.charAt(i);
                if (c == ',') {
                    hasComma = true;
                } else if (c == '"') {
                    hasQuote = true;
                }
            }
            if (hasQuote) {
                parseHeader(v, accumulator);
            } else if (hasComma) {
                for (String split : v.split(",")) {
                    accumulator.add(split.trim());
                }
            } else {
                accumulator.add(v);
            }
        }
    }

    // Parses comma-separated values that can contain quoted values.
    private static void parseHeader(String input, List<String> accumulator) {
        StringBuilder currentEntry = new StringBuilder();
        boolean inQuotes = false;
        boolean escaping = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaping) {
                // The previous character was a backslash: add this one verbatim.
                currentEntry.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                accumulator.add(currentEntry.toString().trim());
                currentEntry.setLength(0);
            } else {
                currentEntry.append(c);
            }
        }
        if (currentEntry.length() > 0) {
            accumulator.add(currentEntry.toString().trim());
        }
    }

    // HTTP dates contain commas, so split on every other comma:
    // "Mon, 16 Dec 2019 23:48:18 GMT, Mon, 16 Dec 2019 23:48:18 GMT" has two elements.
    private static void splitHttpDates(String input, List<String> accumulator) {
        StringBuilder currentPart = new StringBuilder();
        int commaCount = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ',') {
                commaCount++;
                if (commaCount % 2 == 0) {
                    accumulator.add(currentPart.toString().trim());
                    currentPart.setLength(0);
                    continue;
                }
            }
            currentPart.append(c);
        }
        if (currentPart.length() > 0) {
            accumulator.add(currentPart.toString().trim());
        }
    }

    @Override
    protected RuntimeException throwForInvalidState(Schema schema) {
        return new SerializationException("List header deserialization not supported for " + schema);
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> listMemberConsumer) {
        for (String value : values) {
            listMemberConsumer.accept(state, new HttpHeaderDeserializer(value, member));
        }
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public int containerSize() {
        return values.size();
    }
}
