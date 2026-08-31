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

package software.amazon.awssdk.protocols.json.internal.marshall;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.RandomAccess;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingKnownType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.traits.TraitType;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.awssdk.utils.DateUtils;

@SdkInternalApi
public final class SimpleTypeJsonMarshaller {

    public static final JsonMarshaller<Void> NULL = (val, context, paramName, sdkField) -> {
        if (Objects.nonNull(sdkField) && sdkField.containsTrait(RequiredTrait.class, TraitType.REQUIRED_TRAIT)) {
            throw new IllegalArgumentException(String.format("Parameter '%s' must not be null",
                                                             Optional.ofNullable(paramName)
                                                                     .orElseGet(() -> "paramName null")));
        }

        // If paramName is non null then we are emitting a field of an object, in that
        // we just don't write the field. If param name is null then we are either in a container
        // or the thing being marshalled is the payload itself in which case we want to preserve
        // the JSON null.
        if (paramName == null) {
            context.jsonGenerator().writeNull();
        }
    };

    public static final JsonMarshaller<String> STRING = new BaseJsonMarshaller<String>() {
        @Override
        public void marshall(String val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Integer> INTEGER = new BaseJsonMarshaller<Integer>() {
        @Override
        public void marshall(Integer val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Long> LONG = new BaseJsonMarshaller<Long>() {
        @Override
        public void marshall(Long val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Short> SHORT = new BaseJsonMarshaller<Short>() {
        @Override
        public void marshall(Short val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Byte> BYTE = new BaseJsonMarshaller<Byte>() {
        @Override
        public void marshall(Byte val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Float> FLOAT = new BaseJsonMarshaller<Float>() {
        @Override
        public void marshall(Float val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<BigDecimal> BIG_DECIMAL = new BaseJsonMarshaller<BigDecimal>() {
        @Override
        public void marshall(BigDecimal val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Double> DOUBLE = new BaseJsonMarshaller<Double>() {
        @Override
        public void marshall(Double val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Boolean> BOOLEAN = new BaseJsonMarshaller<Boolean>() {
        @Override
        public void marshall(Boolean val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Instant> INSTANT = (val, context, paramName, sdkField) -> {
        StructuredJsonGenerator jsonGenerator = context.jsonGenerator();
        if (paramName != null) {
            jsonGenerator.writeFieldName(paramName);
        }
        TimestampFormatTrait trait = sdkField != null ? sdkField.getTrait(TimestampFormatTrait.class,
                                                                          TraitType.TIMESTAMP_FORMAT_TRAIT) : null;
        if (trait != null) {
            switch (trait.format()) {
                case UNIX_TIMESTAMP:
                    jsonGenerator.writeNumber(DateUtils.formatUnixTimestampInstant(val));
                    break;
                case RFC_822:
                    jsonGenerator.writeValue(DateUtils.formatRfc822Date(val));
                    break;
                case ISO_8601:
                    jsonGenerator.writeValue(DateUtils.formatIso8601Date(val));
                    break;
                default:
                    throw SdkClientException.create("Unrecognized timestamp format - " + trait.format());
            }
        } else {
            // Important to fallback to the jsonGenerator implementation as that may differ per wire format,
            // irrespective of protocol. I.E. CBOR would default to unix timestamp as milliseconds while JSON
            // will default to unix timestamp as seconds with millisecond decimal precision.
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<SdkBytes> SDK_BYTES = new BaseJsonMarshaller<SdkBytes>() {
        @Override
        public void marshall(SdkBytes val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeBinaryValue(val.asByteArrayUnsafe());
        }
    };

    public static final JsonMarshaller<SdkPojo> SDK_POJO = new BaseJsonMarshaller<SdkPojo>() {
        @Override
        public void marshall(SdkPojo val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeStartObject();
            context.protocolHandler().doMarshall(val);
            jsonGenerator.writeEndObject();

        }
    };

    public static final JsonMarshaller<List<?>> LIST = new JsonMarshaller<List<?>>() {
        @Override
        public void marshall(List<?> list, JsonMarshallerContext context, String paramName,
                             SdkField<List<?>> sdkField) {
            if (list.isEmpty() && list instanceof SdkAutoConstructList) {
                return;
            }
            StructuredJsonGenerator jsonGenerator = context.jsonGenerator();
            if (paramName != null) {
                jsonGenerator.writeFieldName(paramName);
            }
            jsonGenerator.writeStartArray(list.size());

            // Resolve element dispatch once per list from the ListTrait's member metadata, instead of a
            // registry lookup (instanceof probe + two map lookups) per element.
            SdkField<?> memberField = null;
            MarshallingKnownType memberKnownType = null;
            if (sdkField != null) {
                ListTrait listTrait = sdkField.getTrait(ListTrait.class, TraitType.LIST_TRAIT);
                if (listTrait != null && listTrait.memberFieldInfo() != null) {
                    memberField = listTrait.memberFieldInfo();
                    memberKnownType = memberField.marshallingType().getKnownType();
                }
            }
            if (memberKnownType != null) {
                if (list instanceof RandomAccess) {
                    for (int i = 0; i < list.size(); i++) {
                        marshallContainerValue(memberKnownType, list.get(i), memberField, context);
                    }
                } else {
                    for (Object listValue : list) {
                        marshallContainerValue(memberKnownType, listValue, memberField, context);
                    }
                }
            } else {
                for (Object listValue : list) {
                    context.marshall(MarshallLocation.PAYLOAD, listValue);
                }
            }
            jsonGenerator.writeEndArray();
        }
    };

    /**
     * Marshalls a Map as a JSON object where each key becomes a field.
     */
    public static final JsonMarshaller<Map<String, ?>> MAP = new JsonMarshaller<Map<String, ?>>() {
        @Override
        public void marshall(Map<String, ?> map, JsonMarshallerContext context, String paramName,
                             SdkField<Map<String, ?>> sdkField) {
            if (map.isEmpty() && map instanceof SdkAutoConstructMap) {
                return;
            }
            StructuredJsonGenerator jsonGenerator = context.jsonGenerator();
            if (paramName != null) {
                jsonGenerator.writeFieldName(paramName);
            }
            jsonGenerator.writeStartObject();

            // Resolve value dispatch once per map from the MapTrait's value metadata, instead of a
            // registry lookup (instanceof probe + two map lookups) per entry.
            SdkField<?> valueField = null;
            MarshallingKnownType valueKnownType = null;
            if (sdkField != null) {
                MapTrait mapTrait = sdkField.getTrait(MapTrait.class, TraitType.MAP_TRAIT);
                if (mapTrait != null && mapTrait.valueFieldInfo() != null) {
                    valueField = mapTrait.valueFieldInfo();
                    valueKnownType = valueField.marshallingType().getKnownType();
                }
            }
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value != null) {
                    jsonGenerator.writeFieldName(entry.getKey());
                    if (valueKnownType != null) {
                        marshallContainerValue(valueKnownType, value, valueField, context);
                    } else {
                        context.marshall(MarshallLocation.PAYLOAD, value);
                    }
                }
            }
            jsonGenerator.writeEndObject();
        }
    };

    /**
     * Marshalls Document type members by visiting the document using DocumentTypeJsonMarshaller.
     */
    public static final JsonMarshaller<Document> DOCUMENT = new BaseJsonMarshaller<Document>() {
        @Override
        public void marshall(Document document, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            document.accept(new DocumentTypeJsonMarshaller(jsonGenerator));
        }
    };

    private SimpleTypeJsonMarshaller() {
    }

    /**
     * Writes a container element (list element or map value) dispatching on the member's precomputed
     * {@link MarshallingKnownType}. Produces bytes identical to the registry path
     * ({@code context.marshall(PAYLOAD, val)}) it replaces: in particular, INSTANT deliberately ignores
     * the member field's TimestampFormatTrait because the registry path never saw the member SdkField
     * and always used the wire default.
     */
    @SuppressWarnings("unchecked")
    private static void marshallContainerValue(MarshallingKnownType knownType, Object val,
                                               SdkField<?> memberField, JsonMarshallerContext context) {
        StructuredJsonGenerator jsonGenerator = context.jsonGenerator();
        if (val == null) {
            jsonGenerator.writeNull();
            return;
        }
        switch (knownType) {
            case STRING:
                jsonGenerator.writeValue((String) val);
                break;
            case INTEGER:
                jsonGenerator.writeValue((int) (Integer) val);
                break;
            case LONG:
                jsonGenerator.writeValue((long) (Long) val);
                break;
            case SHORT:
                jsonGenerator.writeValue((short) (Short) val);
                break;
            case BYTE:
                jsonGenerator.writeValue((byte) (Byte) val);
                break;
            case FLOAT:
                jsonGenerator.writeValue((float) (Float) val);
                break;
            case DOUBLE:
                jsonGenerator.writeValue((double) (Double) val);
                break;
            case BIG_DECIMAL:
                jsonGenerator.writeValue((BigDecimal) val);
                break;
            case BOOLEAN:
                jsonGenerator.writeValue((boolean) (Boolean) val);
                break;
            case INSTANT:
                jsonGenerator.writeValue((Instant) val);
                break;
            case SDK_BYTES:
                jsonGenerator.writeBinaryValue(((SdkBytes) val).asByteArrayUnsafe());
                break;
            case SDK_POJO:
                jsonGenerator.writeStartObject();
                context.protocolHandler().doMarshall((SdkPojo) val);
                jsonGenerator.writeEndObject();
                break;
            case LIST:
                LIST.marshall((List<?>) val, context, null, (SdkField<List<?>>) memberField);
                break;
            case MAP:
                MAP.marshall((Map<String, ?>) val, context, null, (SdkField<Map<String, ?>>) memberField);
                break;
            case DOCUMENT:
                DOCUMENT.marshall((Document) val, context, null, null);
                break;
            default:
                context.marshall(MarshallLocation.PAYLOAD, val);
                break;
        }
    }

    /**
     * Base marshaller that emits the field name if present. The field name may be null in cases like
     * marshalling something inside a list or if the object is the explicit payload member.
     *
     * @param <T> Type to marshall.
     */
    private abstract static class BaseJsonMarshaller<T> implements JsonMarshaller<T> {

        @Override
        public final void marshall(T val, JsonMarshallerContext context, String paramName, SdkField<T> sdkField) {
            if (!shouldEmit(val)) {
                return;
            }
            if (paramName != null) {
                context.jsonGenerator().writeFieldName(paramName);
            }
            marshall(val, context.jsonGenerator(), context);
        }

        public abstract void marshall(T val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context);

        protected boolean shouldEmit(T val) {
            return true;
        }
    }

}
