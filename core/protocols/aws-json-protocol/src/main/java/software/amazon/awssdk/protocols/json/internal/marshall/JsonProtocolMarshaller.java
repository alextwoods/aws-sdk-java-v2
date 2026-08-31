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

import static software.amazon.awssdk.core.internal.util.Mimetype.MIMETYPE_EVENT_STREAM;
import static software.amazon.awssdk.http.Header.CHUNKED;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;
import static software.amazon.awssdk.http.Header.TRANSFER_ENCODING;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingKnownType;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.traits.TraitType;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.InstantToString;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.core.ProtocolUtils;
import software.amazon.awssdk.protocols.core.ValueToStringConverter.ValueToString;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.awssdk.protocols.json.StructuredJsonWritable;
import software.amazon.awssdk.protocols.json.internal.ProtocolFact;

/**
 * Implementation of {@link ProtocolMarshaller} for JSON based services. This includes JSON-RPC and REST-JSON.
 */
@SdkInternalApi
public class JsonProtocolMarshaller implements ProtocolMarshaller<SdkHttpFullRequest> {

    public static final ValueToString<Instant> INSTANT_VALUE_TO_STRING =
        InstantToString.create(getDefaultTimestampFormats());

    private static final JsonMarshallerRegistry MARSHALLER_REGISTRY = createMarshallerRegistry();

    // Caches the per-field marshalling plan (trait probes, dispatch type, resolved marshaller), keyed by
    // SdkField identity. SdkField instances are static final per generated model class, so identity-based
    // lookup is correct and the cache is bounded by the total number of SdkField instances across all
    // loaded service models — each SdkField is inserted at most once, and no eviction is needed.
    // ConcurrentHashMap is used for thread safety; the one-time put per SdkField is negligible.
    private static final ConcurrentHashMap<SdkField<?>, FieldPlan> FIELD_PLAN_CACHE = new ConcurrentHashMap<>();

    private final URI endpoint;
    private final StructuredJsonGenerator jsonGenerator;
    private final SdkHttpFullRequest.Builder request;
    private final String contentType;
    private final AwsJsonProtocolMetadata protocolMetadata;
    private final boolean hasExplicitPayloadMember;
    private final boolean hasImplicitPayloadMembers;
    private final boolean hasStreamingInput;

    private final JsonMarshallerContext marshallerContext;
    private final boolean hasEventStreamingInput;
    private final boolean hasEvent;
    private final boolean hasAwsQueryCompatible;
    private final IntConsumer marshalledSizeReporter;

    JsonProtocolMarshaller(URI endpoint,
                           StructuredJsonGenerator jsonGenerator,
                           String contentType,
                           OperationInfo operationInfo,
                           AwsJsonProtocolMetadata protocolMetadata,
                           boolean hasAwsQueryCompatible,
                           IntConsumer marshalledSizeReporter) {
        this.endpoint = endpoint;
        this.jsonGenerator = jsonGenerator;
        this.contentType = contentType;
        this.marshalledSizeReporter = marshalledSizeReporter != null ? marshalledSizeReporter : size -> { };
        this.protocolMetadata = protocolMetadata;
        this.hasExplicitPayloadMember = operationInfo.hasExplicitPayloadMember();
        this.hasImplicitPayloadMembers = operationInfo.hasImplicitPayloadMembers();
        this.hasStreamingInput = operationInfo.hasStreamingInput();
        this.hasEventStreamingInput = operationInfo.hasEventStreamingInput();
        this.hasEvent = operationInfo.hasEvent();
        this.request = fillBasicRequestParams(operationInfo);
        this.hasAwsQueryCompatible = hasAwsQueryCompatible;
        this.marshallerContext = JsonMarshallerContext.builder()
                                                      .jsonGenerator(jsonGenerator)
                                                      .marshallerRegistry(MARSHALLER_REGISTRY)
                                                      .protocolHandler(this)
                                                      .request(request)
                                                      .build();
    }

    private static JsonMarshallerRegistry createMarshallerRegistry() {
        return JsonMarshallerRegistry
            .builder()
            .payloadMarshaller(MarshallingType.STRING, SimpleTypeJsonMarshaller.STRING)
            .payloadMarshaller(MarshallingType.INTEGER, SimpleTypeJsonMarshaller.INTEGER)
            .payloadMarshaller(MarshallingType.LONG, SimpleTypeJsonMarshaller.LONG)
            .payloadMarshaller(MarshallingType.SHORT, SimpleTypeJsonMarshaller.SHORT)
            .payloadMarshaller(MarshallingType.BYTE, SimpleTypeJsonMarshaller.BYTE)
            .payloadMarshaller(MarshallingType.DOUBLE, SimpleTypeJsonMarshaller.DOUBLE)
            .payloadMarshaller(MarshallingType.FLOAT, SimpleTypeJsonMarshaller.FLOAT)
            .payloadMarshaller(MarshallingType.BIG_DECIMAL, SimpleTypeJsonMarshaller.BIG_DECIMAL)
            .payloadMarshaller(MarshallingType.BOOLEAN, SimpleTypeJsonMarshaller.BOOLEAN)
            .payloadMarshaller(MarshallingType.INSTANT, SimpleTypeJsonMarshaller.INSTANT)
            .payloadMarshaller(MarshallingType.SDK_BYTES, SimpleTypeJsonMarshaller.SDK_BYTES)
            .payloadMarshaller(MarshallingType.SDK_POJO, SimpleTypeJsonMarshaller.SDK_POJO)
            .payloadMarshaller(MarshallingType.LIST, SimpleTypeJsonMarshaller.LIST)
            .payloadMarshaller(MarshallingType.MAP, SimpleTypeJsonMarshaller.MAP)
            .payloadMarshaller(MarshallingType.NULL, SimpleTypeJsonMarshaller.NULL)
            .payloadMarshaller(MarshallingType.DOCUMENT, SimpleTypeJsonMarshaller.DOCUMENT)

            .headerMarshaller(MarshallingType.STRING, HeaderMarshaller.STRING)
            .headerMarshaller(MarshallingType.INTEGER, HeaderMarshaller.INTEGER)
            .headerMarshaller(MarshallingType.LONG, HeaderMarshaller.LONG)
            .headerMarshaller(MarshallingType.SHORT, HeaderMarshaller.SHORT)
            .headerMarshaller(MarshallingType.BYTE, HeaderMarshaller.BYTE)
            .headerMarshaller(MarshallingType.DOUBLE, HeaderMarshaller.DOUBLE)
            .headerMarshaller(MarshallingType.FLOAT, HeaderMarshaller.FLOAT)
            .headerMarshaller(MarshallingType.BOOLEAN, HeaderMarshaller.BOOLEAN)
            .headerMarshaller(MarshallingType.INSTANT, HeaderMarshaller.INSTANT)
            .headerMarshaller(MarshallingType.LIST, HeaderMarshaller.LIST)
            .headerMarshaller(MarshallingType.MAP, HeaderMarshaller.MAP)
            .headerMarshaller(MarshallingType.NULL, HeaderMarshaller.NULL)

            .queryParamMarshaller(MarshallingType.STRING, QueryParamMarshaller.STRING)
            .queryParamMarshaller(MarshallingType.INTEGER, QueryParamMarshaller.INTEGER)
            .queryParamMarshaller(MarshallingType.LONG, QueryParamMarshaller.LONG)
            .queryParamMarshaller(MarshallingType.SHORT, QueryParamMarshaller.SHORT)
            .queryParamMarshaller(MarshallingType.BYTE, QueryParamMarshaller.BYTE)
            .queryParamMarshaller(MarshallingType.DOUBLE, QueryParamMarshaller.DOUBLE)
            .queryParamMarshaller(MarshallingType.FLOAT, QueryParamMarshaller.FLOAT)
            .queryParamMarshaller(MarshallingType.BOOLEAN, QueryParamMarshaller.BOOLEAN)
            .queryParamMarshaller(MarshallingType.INSTANT, QueryParamMarshaller.INSTANT)
            .queryParamMarshaller(MarshallingType.LIST, QueryParamMarshaller.LIST)
            .queryParamMarshaller(MarshallingType.MAP, QueryParamMarshaller.MAP)
            .queryParamMarshaller(MarshallingType.NULL, QueryParamMarshaller.NULL)

            .pathParamMarshaller(MarshallingType.STRING, SimpleTypePathMarshaller.STRING)
            .pathParamMarshaller(MarshallingType.INTEGER, SimpleTypePathMarshaller.INTEGER)
            .pathParamMarshaller(MarshallingType.LONG, SimpleTypePathMarshaller.LONG)
            .pathParamMarshaller(MarshallingType.SHORT, SimpleTypePathMarshaller.SHORT)
            .pathParamMarshaller(MarshallingType.BYTE, SimpleTypePathMarshaller.BYTE)
            .pathParamMarshaller(MarshallingType.NULL, SimpleTypePathMarshaller.NULL)

            .greedyPathParamMarshaller(MarshallingType.STRING, SimpleTypePathMarshaller.GREEDY_STRING)
            .greedyPathParamMarshaller(MarshallingType.NULL, SimpleTypePathMarshaller.NULL)
            .build();
    }

    private static Map<MarshallLocation, TimestampFormatTrait.Format> getDefaultTimestampFormats() {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new EnumMap<>(MarshallLocation.class);
        // TODO the default is supposedly rfc822. See JAVA-2949
        // We are using ISO_8601 in v1. Investigate which is the right format
        formats.put(MarshallLocation.HEADER, TimestampFormatTrait.Format.RFC_822);
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.UNIX_TIMESTAMP);
        formats.put(MarshallLocation.QUERY_PARAM, TimestampFormatTrait.Format.ISO_8601);
        return Collections.unmodifiableMap(formats);
    }

    private SdkHttpFullRequest.Builder fillBasicRequestParams(OperationInfo operationInfo) {
        SdkHttpFullRequest.Builder requestBuilder = ProtocolUtils.createSdkHttpRequest(operationInfo, endpoint);
        String operationIdentifier = operationInfo.operationIdentifier();
        if (operationIdentifier != null) {
            requestBuilder.putHeader("X-Amz-Target", operationIdentifier);
        }
        Map<String, String> extraHeaders = operationInfo.addtionalMetadata(BaseAwsJsonProtocolFactory.HTTP_EXTRA_HEADERS);
        if (extraHeaders == null) {
            extraHeaders =
                ProtocolFact.from(protocolMetadata.protocol()).extraHeaders();
        }
        if (extraHeaders != null) {
            extraHeaders.forEach(requestBuilder::putHeader);
        }
        return requestBuilder;
    }

    /**
     * If there is not an explicit payload member then we need to start the implicit JSON request object. All
     * members bound to the payload will be added as fields to this object.
     */
    private void startMarshalling() {
        // Create the implicit request object if needed.
        if (needTopLevelJsonObject()) {
            jsonGenerator.writeStartObject();
        }
    }

    void doMarshall(SdkPojo pojo) {
        // Shapes whose members all bind to the payload carry generated straight-line marshalling code;
        // dispatch to it instead of the reflective field loop. Shapes with non-payload members (or from
        // older generated code) do not implement the interface and take the generic path below.
        if (pojo instanceof StructuredJsonWritable) {
            ((StructuredJsonWritable) pojo).marshallJsonFields(jsonGenerator);
            return;
        }
        List<SdkField<?>> fields = pojo.sdkFields();
        if (fields instanceof RandomAccess) {
            // Generated models return a RandomAccess list; indexed access avoids the iterator allocation.
            for (int i = 0; i < fields.size(); i++) {
                marshallPojoField(pojo, fields.get(i));
            }
        } else {
            for (SdkField<?> field : fields) {
                marshallPojoField(pojo, field);
            }
        }
    }

    private void marshallPojoField(SdkPojo pojo, SdkField<?> field) {
        FieldPlan plan = planFor(field);
        Object val = field.getValueOrDefault(pojo);
        if (plan.explicitPayload) {
            if (plan.explicitBinaryPayload) {
                if (val != null) {
                    SdkBytes sdkBytes = (SdkBytes) val;
                    request.contentStreamProvider(sdkBytes::asInputStream);
                    updateContentLengthHeader(sdkBytes.asByteArrayUnsafe().length);
                }
            } else if (plan.explicitStringPayload) {
                if (val != null) {
                    byte[] content = ((String) val).getBytes(StandardCharsets.UTF_8);
                    request.contentStreamProvider(() -> new ByteArrayInputStream(content));
                    updateContentLengthHeader(content.length);
                }
            } else {
                marshallExplicitJsonPayload(field, val);
            }
        } else if (val != null) {
            if (plan.payloadLocation) {
                // HOT PATH: switch-based dispatch, no registry, no interface dispatch
                marshallPayloadField(field, plan, val);
            } else {
                // WARM PATH: plan-cached marshaller + interface dispatch
                marshallFieldViaRegistry(field, plan, val);
            }
        } else if (!plan.payloadLocation) {
            // Null non-payload: must go through registry (null marshallers vary by location)
            marshallNullViaRegistry(field);
        } else if (plan.required) {
            throw new IllegalArgumentException(
                String.format("Parameter '%s' must not be null", field.locationName()));
        }
        // else: null payload field, not required → no-op
    }

    /**
     * Returns the cached {@link FieldPlan} for the field, computing it on first use. Get-before-put:
     * ConcurrentHashMap.get() is a single lock-free volatile read, and concurrent first-use puts are
     * idempotent because the plan is derived deterministically from the (static final) field.
     */
    private static FieldPlan planFor(SdkField<?> field) {
        FieldPlan plan = FIELD_PLAN_CACHE.get(field);
        if (plan == null) {
            plan = new FieldPlan(field);
            FIELD_PLAN_CACHE.put(field, plan);
        }
        return plan;
    }

    /**
     * Precomputed per-field marshalling decisions: every trait probe and dispatch lookup that
     * {@code doMarshall} would otherwise repeat on each request is resolved once per SdkField.
     */
    static final class FieldPlan {
        final MarshallingKnownType knownType;
        final boolean explicitPayload;
        final boolean explicitBinaryPayload;
        final boolean explicitStringPayload;
        final boolean payloadLocation;
        final boolean required;

        /**
         * The location name pre-encoded as UTF-8 {@code "name":} token bytes, for
         * {@link StructuredJsonGenerator#writeFieldName(String, byte[])}. Only computed for
         * PAYLOAD-location fields, whose names are written into the JSON document.
         */
        final byte[] fieldNameToken;

        /**
         * Registry marshaller for non-null values of non-PAYLOAD-location fields, resolved on first use.
         * Plain (non-volatile) field: the registry always returns the same instance for a given
         * (location, type) pair, so a data race can only cause a redundant lookup.
         */
        JsonMarshaller<Object> nonNullMarshaller;

        FieldPlan(SdkField<?> field) {
            this.knownType = field.marshallingType().getKnownType();
            this.explicitPayload = field.containsTrait(PayloadTrait.class, TraitType.PAYLOAD_TRAIT);
            this.explicitBinaryPayload = explicitPayload && MarshallingType.SDK_BYTES.equals(field.marshallingType());
            this.explicitStringPayload = explicitPayload && MarshallingType.STRING.equals(field.marshallingType());
            this.payloadLocation = field.location() == MarshallLocation.PAYLOAD;
            this.required = field.containsTrait(RequiredTrait.class, TraitType.REQUIRED_TRAIT);
            this.fieldNameToken = payloadLocation && !explicitPayload && field.locationName() != null
                                  ? FastJsonGenerator.encodeFieldNameToken(field.locationName())
                                  : null;
        }
    }

    private void updateContentLengthHeader(int contentLength) {
        request.putHeader(CONTENT_LENGTH, Integer.toString(contentLength));
    }

    private void marshallExplicitJsonPayload(SdkField<?> field, Object val) {
        // Explicit JSON payloads are always marshalled as an object,
        // even if they're null, in which case it's an empty object.
        jsonGenerator.writeStartObject();
        if (val != null) {
            if (MarshallingType.DOCUMENT.equals(field.marshallingType())) {
                marshallField(field, val);
            } else {
                doMarshall((SdkPojo) val);
            }
        }
        jsonGenerator.writeEndObject();
    }

    @Override
    public SdkHttpFullRequest marshall(SdkPojo pojo) {
        startMarshalling();
        doMarshall(pojo);
        return finishMarshalling();
    }

    private SdkHttpFullRequest finishMarshalling() {
        // Content may already be set if the payload is binary data.
        if (request.contentStreamProvider() == null) {
            // End the implicit request object if needed.
            if (needTopLevelJsonObject()) {
                jsonGenerator.writeEndObject();
            }

            ContentStreamProvider contentProvider = jsonGenerator.contentStreamProvider();
            if (contentProvider != null) {
                request.contentStreamProvider(contentProvider);
                int contentSize = jsonGenerator.contentSize();
                if (contentSize > 0) {
                    request.putHeader(CONTENT_LENGTH, Integer.toString(contentSize));
                    marshalledSizeReporter.accept(contentSize);
                }
            }
        }

        // We skip setting the default content type if the request is streaming as
        // content-type is determined based on the body of the stream
        // TODO: !request.headers().containsKey(CONTENT_TYPE) does not work because request is created from line 77
        // and not from the original request
        if (!request.firstMatchingHeader(CONTENT_TYPE).isPresent() && !hasEvent) {
            if (hasEventStreamingInput) {
                AwsJsonProtocol protocol = protocolMetadata.protocol();
                if (protocol == AwsJsonProtocol.AWS_JSON || protocol == AwsJsonProtocol.SMITHY_RPC_V2_CBOR) {
                    // For RPC formats, this content type will later be pushed down into the `initial-event` in the body
                    request.putHeader(CONTENT_TYPE, contentType);
                } else if (protocol == AwsJsonProtocol.REST_JSON) {
                    request.putHeader(CONTENT_TYPE, MIMETYPE_EVENT_STREAM);
                } else {
                    throw new IllegalArgumentException("Unknown AwsJsonProtocol: " + protocol);
                }
                request.removeHeader(CONTENT_LENGTH);
                request.putHeader(TRANSFER_ENCODING, CHUNKED);
            } else if (contentType != null && !hasStreamingInput && request.firstMatchingHeader(CONTENT_LENGTH).isPresent()) {
                request.putHeader(CONTENT_TYPE, contentType);
            }
        }

        if (hasAwsQueryCompatible) {
            request.putHeader("x-amzn-query-mode", "true");
        }

        return request.build();
    }

    /**
     * Marshalls a PAYLOAD-location field using a switch on {@link MarshallingKnownType} instead of
     * registry lookup and interface dispatch. Each case is a monomorphic call site that the JIT can inline.
     */
    @SuppressWarnings("unchecked")
    private void marshallPayloadField(SdkField<?> field, FieldPlan plan, Object val) {
        MarshallingKnownType knownType = plan.knownType;
        if (knownType == null) {
            marshallFieldViaRegistry(field, plan, val);
            return;
        }

        StructuredJsonGenerator gen = marshallerContext.jsonGenerator();
        String fieldName = field.locationName();
        byte[] nameToken = plan.fieldNameToken;

        switch (knownType) {
            case STRING:
                gen.writeFieldName(fieldName, nameToken);
                gen.writeValue((String) val);
                break;
            case INTEGER:
                gen.writeFieldName(fieldName, nameToken);
                gen.writeValue((int) (Integer) val);
                break;
            case LONG:
                gen.writeFieldName(fieldName, nameToken);
                gen.writeValue((long) (Long) val);
                break;
            case SHORT:
                gen.writeFieldName(fieldName, nameToken);
                gen.writeValue((short) (Short) val);
                break;
            case BYTE:
                gen.writeFieldName(fieldName, nameToken);
                gen.writeValue((byte) (Byte) val);
                break;
            case FLOAT:
                gen.writeFieldName(fieldName, nameToken);
                gen.writeValue((float) (Float) val);
                break;
            case DOUBLE:
                gen.writeFieldName(fieldName, nameToken);
                gen.writeValue((double) (Double) val);
                break;
            case BIG_DECIMAL:
                gen.writeFieldName(fieldName, nameToken);
                gen.writeValue((BigDecimal) val);
                break;
            case BOOLEAN:
                gen.writeFieldName(fieldName, nameToken);
                gen.writeValue((boolean) (Boolean) val);
                break;
            case INSTANT:
                // Write the name here (token fast path), then delegate with a null paramName to the
                // INSTANT marshaller, which preserves TimestampFormatTrait handling for the value.
                gen.writeFieldName(fieldName, nameToken);
                SimpleTypeJsonMarshaller.INSTANT.marshall((Instant) val, marshallerContext,
                    null, (SdkField<Instant>) field);
                break;
            case SDK_BYTES:
                gen.writeFieldName(fieldName, nameToken);
                gen.writeBinaryValue(((SdkBytes) val).asByteArrayUnsafe());
                break;
            case SDK_POJO:
                gen.writeFieldName(fieldName, nameToken);
                gen.writeStartObject();
                doMarshall((SdkPojo) val);
                gen.writeEndObject();
                break;
            case LIST:
                // The emit check must precede the field-name write (an empty auto-construct list emits
                // neither), mirroring the LIST marshaller's own check.
                List<?> list = (List<?>) val;
                if (!list.isEmpty() || !(list instanceof SdkAutoConstructList)) {
                    gen.writeFieldName(fieldName, nameToken);
                    SimpleTypeJsonMarshaller.LIST.marshall(list, marshallerContext,
                        null, (SdkField<List<?>>) field);
                }
                break;
            case MAP:
                Map<String, ?> map = (Map<String, ?>) val;
                if (!map.isEmpty() || !(map instanceof SdkAutoConstructMap)) {
                    gen.writeFieldName(fieldName, nameToken);
                    SimpleTypeJsonMarshaller.MAP.marshall(map, marshallerContext,
                        null, (SdkField<Map<String, ?>>) field);
                }
                break;
            case DOCUMENT:
                gen.writeFieldName(fieldName, nameToken);
                SimpleTypeJsonMarshaller.DOCUMENT.marshall((Document) val, marshallerContext,
                    null, (SdkField<Document>) field);
                break;
            default:
                // Unknown type — fall back to registry lookup
                marshallFieldViaRegistry(field, plan, val);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void marshallFieldViaRegistry(SdkField<?> field, FieldPlan plan, Object val) {
        if (val == null) {
            marshallNullViaRegistry(field);
            return;
        }
        JsonMarshaller<Object> marshaller = plan.nonNullMarshaller;
        if (marshaller == null) {
            marshaller = MARSHALLER_REGISTRY.getMarshaller(field.location(), field.marshallingType(), val);
            plan.nonNullMarshaller = marshaller;
        }
        marshaller.marshall(val, marshallerContext, field.locationName(), (SdkField<Object>) field);
    }

    @SuppressWarnings("unchecked")
    private void marshallNullViaRegistry(SdkField<?> field) {
        MARSHALLER_REGISTRY.getMarshaller(field.location(), field.marshallingType(), null)
                           .marshall(null, marshallerContext, field.locationName(), (SdkField<Object>) field);
    }

    private void marshallField(SdkField<?> field, Object val) {
        MARSHALLER_REGISTRY.getMarshaller(field.location(), field.marshallingType(), val)
                           .marshall(val, marshallerContext, field.locationName(), (SdkField<Object>) field);
    }

    private boolean needTopLevelJsonObject() {
        AwsJsonProtocol protocol = protocolMetadata.protocol();
        return protocol == AwsJsonProtocol.AWS_JSON
               || protocol == AwsJsonProtocol.SMITHY_RPC_V2_CBOR
               || (!hasExplicitPayloadMember && hasImplicitPayloadMembers);

    }
}
