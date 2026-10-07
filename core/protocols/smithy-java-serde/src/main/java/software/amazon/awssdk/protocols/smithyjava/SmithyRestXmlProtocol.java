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

package software.amazon.awssdk.protocols.smithyjava;

import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.MetricCollectingHttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.smithyjava.internal.SerdeSupport;
import software.amazon.awssdk.protocols.smithyjava.internal.V2HeaderBindings;
import software.amazon.awssdk.protocols.smithyjava.internal.V2XmlBodyRules;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpVersion;
import software.amazon.smithy.java.http.binding.HttpBinding;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.java.io.uri.SmithyUri;
import software.amazon.smithy.java.xml.XmlCodec;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * rest-xml marshalling and unmarshalling for the stock v2 pipeline, done by smithy-java's HTTP binding layer
 * and {@link XmlCodec}.
 *
 * <p>As with {@link SmithyAwsJsonProtocol}, this replaces only the request marshaller and the success-path
 * unmarshaller. smithy-java's {@link HttpBinding} places each member in the URI path, query string, headers or
 * body, and the codec writes and reads the XML. The result is then put in the form v2's own
 * {@code XmlProtocolMarshaller} gives a request, because the v2 pipeline downstream of the marshaller depends
 * on that form: the path is appended to the client endpoint the same way (the endpoint interceptors rewrite it
 * later), query parameters are stored decoded, header names keep their modeled case, and the body follows
 * v2's rules for when there is one, its {@code Content-Type}, and its XML escaping.
 *
 * <p>On the response side, everything that is not a plain success stays with v2: non-2xx responses, and S3's
 * 200 responses whose body is an {@code <Error>} document, are handed to the v2 handler unchanged, so error
 * unmarshalling and S3's error-in-200 detection are exactly v2's.
 */
@SdkProtectedApi
public final class SmithyRestXmlProtocol {

    private static final String XML_MEDIA_TYPE = "application/xml";
    private static final SmithyUri BINDING_ENDPOINT = SmithyUri.of("http://localhost");
    private static final byte[] EMPTY = new byte[0];

    private final URI endpoint;
    private final HttpBinding httpBinding = new HttpBinding();
    private final Map<String, XmlCodec> codecsByNamespace = new ConcurrentHashMap<>();
    // strictRootElement(false) because v2 does not check the root element's name, and S3's names do not match
    // the output shapes: ListObjectsV2 answers with <ListBucketResult>, CreateMultipartUpload with
    // <InitiateMultipartUploadResult>. Rejecting those would fail every such operation.
    private final XmlCodec responseCodec = XmlCodec.builder().strictRootElement(false).build();

    private final Map<ApiOperation<?, ?>, Marshaller<?>> marshallers = new ConcurrentHashMap<>();
    private final Map<ApiOperation<?, ?>, HttpResponseHandler<?>> combinedHandlers = new ConcurrentHashMap<>();
    private final Map<ApiOperation<?, ?>, HttpResponseHandler<?>> streamingHandlers = new ConcurrentHashMap<>();

    private SmithyRestXmlProtocol(Builder builder) {
        this.endpoint = SerdeSupport.clientEndpoint(Validate.paramNotNull(builder.clientConfiguration,
                                                                          "clientConfiguration"));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The request marshaller for {@code operation}. {@code xmlNamespace} is the namespace v2 puts on the root
     * element of the operation's XML body, or null for none, and {@code requestUri} is the operation's C2J
     * request URI; codegen takes both from the same place v2's generated marshaller does.
     *
     * <p>{@code requestUri} is needed because v2's and smithy's URI patterns are not the same string. S3's
     * customizations strip {@code {Bucket}} from the modeled URI, leaving patterns with no path at all
     * ({@code ?list-type=2}) that the endpoint rules fill in later; smithy requires a pattern to start with
     * {@code /}, so the generated {@code @http} trait has one added. Taken literally that turns v2's
     * {@code GET /bucket} into {@code GET /bucket/}. The literal query in the pattern needs it too: v2 spells a
     * parameter with no value as a null value ({@code ?tagging}) where smithy writes {@code ?tagging=}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <I extends SerializableStruct> Marshaller<I> marshaller(ApiOperation<I, ?> operation, String xmlNamespace,
                                                                   String requestUri) {
        return (Marshaller<I>) marshallers.computeIfAbsent(
            operation, op -> new RestXmlMarshaller<>((ApiOperation) op, codecFor(xmlNamespace), requestUri));
    }

    /**
     * The handler for an operation with a non-streaming response: smithy-java for 2xx, {@code v2Handler(factory)}
     * for everything else. The v2 handler is built once per operation, on first need.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <F, O extends SerializableStruct> HttpResponseHandler<Response<O>> combinedResponseHandler(
        ApiOperation<?, O> operation, F factory, Function<F, HttpResponseHandler<Response<O>>> v2Handler) {
        return (HttpResponseHandler<Response<O>>) combinedHandlers.computeIfAbsent(
            operation, op -> new CombinedHandler<>((ApiOperation) op, () -> v2Handler.apply(factory)));
    }

    /**
     * The success handler for an operation with a streaming response body ({@code GetObject}): it reads the
     * status line and headers into the response and leaves the body to the response transformer.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <O extends SerializableStruct> HttpResponseHandler<O> streamingResponseHandler(ApiOperation<?, O> operation) {
        return (HttpResponseHandler<O>) streamingHandlers.computeIfAbsent(
            operation, op -> MetricCollectingHttpResponseHandler.create(CoreMetric.UNMARSHALLING_DURATION,
                                                                        new StreamingHandler<>((ApiOperation) op)));
    }

    private XmlCodec codecFor(String xmlNamespace) {
        String key = xmlNamespace == null ? "" : xmlNamespace;
        return codecsByNamespace.computeIfAbsent(key, ns -> {
            XmlCodec.Builder builder = XmlCodec.builder();
            if (!ns.isEmpty()) {
                builder.defaultNamespace(XmlNamespaceTrait.builder().uri(ns).build());
            }
            return builder.build();
        });
    }

    private final class RestXmlMarshaller<I extends SerializableStruct> implements Marshaller<I> {
        private final ApiOperation<I, ?> operation;
        private final XmlCodec codec;
        /** v2's literal query parameters from the request URI, in order, with null for a valueless one. */
        private final List<Map.Entry<String, String>> staticQuery;
        /** True when v2's request URI contributes no path, so neither should this marshaller. */
        private final boolean emptyPath;
        /** The modeled {@code Content-Type} member, if the operation has one. */
        private SdkField<?> contentTypeField;
        /** Required members, paired with the name v2 names them by when one is missing. */
        private final List<Map.Entry<SdkField<?>, String>> required = new java.util.ArrayList<>();
        /** Lower-cased header name to the case v2 sends it in: the modeled locationName. */
        private final Map<String, String> headerNames = new HashMap<>();
        /** The header-map members (x-amz-meta-*): their prefix, and how to read the map off the request. */
        private final List<SdkField<?>> prefixHeaderMaps;
        private final boolean hasPayloadMembers;
        private final boolean hasStringPayload;
        private final boolean hasBlobPayload;
        private final boolean hasModeledContentType;

        private RestXmlMarshaller(ApiOperation<I, ?> operation, XmlCodec codec, String requestUri) {
            this.operation = operation;
            this.codec = codec;
            this.staticQuery = staticQuery(requestUri);
            this.emptyPath = pathOf(requestUri).isEmpty();
            List<SdkField<?>> fields = ((SdkPojo) operation.inputBuilder()).sdkFields();
            this.prefixHeaderMaps = new java.util.ArrayList<>();
            boolean payloadMembers = false;
            boolean stringPayload = false;
            boolean blobPayload = false;
            boolean contentType = false;
            for (SdkField<?> field : fields) {
                LocationTrait location = field.getTrait(LocationTrait.class);
                if (field.getTrait(RequiredTrait.class) != null) {
                    // v2's marshallers name a missing member by its locationName outside the body and by its
                    // member name inside it; the message is theirs verbatim, since callers match on it.
                    String name = field.location() != MarshallLocation.PAYLOAD && location != null
                                  && location.locationName() != null ? location.locationName() : field.memberName();
                    required.add(new java.util.AbstractMap.SimpleImmutableEntry<>(field, name));
                }
                if (field.location() == MarshallLocation.HEADER && location != null && location.locationName() != null) {
                    if (field.marshallingType() == MarshallingType.MAP) {
                        prefixHeaderMaps.add(field);
                    } else {
                        headerNames.put(location.locationName().toLowerCase(Locale.US), location.locationName());
                        if (CONTENT_TYPE.equalsIgnoreCase(location.locationName())) {
                            contentType = true;
                            contentTypeField = field;
                        }
                    }
                }
                if (field.location() == MarshallLocation.PAYLOAD) {
                    payloadMembers = true;
                }
                if (field.getTrait(PayloadTrait.class) != null) {
                    stringPayload |= field.marshallingType() == MarshallingType.STRING;
                    blobPayload |= field.marshallingType() == MarshallingType.SDK_BYTES;
                }
            }
            this.hasPayloadMembers = payloadMembers;
            this.hasStringPayload = stringPayload;
            this.hasBlobPayload = blobPayload;
            this.hasModeledContentType = contentType;
        }

        @Override
        public SdkHttpFullRequest marshall(I input) {
            Validate.paramNotNull(input, "input");
            try {
                // smithy-java's binding layer leaves an unset member out and carries on; v2 refuses to send the
                // request. Without this an UploadPart with no uploadId went out as a plain PUT, which S3 answers
                // by overwriting the object instead of failing.
                for (Map.Entry<SdkField<?>, String> member : required) {
                    if (member.getKey().getValueOrDefault((SdkPojo) input) == null) {
                        throw new IllegalArgumentException("Parameter '" + member.getValue() + "' must not be null");
                    }
                }
                HttpRequest bound = httpBinding.requestSerializer()
                                               .operation(operation)
                                               .payloadCodec(codec)
                                               .payloadMediaType(XML_MEDIA_TYPE)
                                               .shapeValue(input)
                                               .endpoint(BINDING_ENDPOINT)
                                               .serializeRequest();
                return toV2Request(input, bound);
            } catch (Exception e) {
                throw SdkClientException.builder()
                                        .message("Unable to marshall request to XML: " + e.getMessage())
                                        .cause(e)
                                        .build();
            }
        }

        private SdkHttpFullRequest toV2Request(I input, HttpRequest bound) {
            SdkHttpFullRequest.Builder request = SdkHttpFullRequest.builder()
                                                                   .method(SdkHttpMethod.fromValue(bound.method()))
                                                                   .uri(endpoint);
            // The binding endpoint has no path, so this is the operation's own path with its labels bound and
            // encoded. Appended to the client endpoint exactly as ProtocolUtils.createSdkHttpRequest does it.
            request.encodedPath(SdkHttpUtils.appendUri(endpoint.getRawPath(), emptyPath ? "" : bound.uri().getPath()));
            for (Map.Entry<String, String> parameter : staticQuery) {
                request.putRawQueryParameter(parameter.getKey(), parameter.getValue());
            }
            String query = bound.uri().getQuery();
            if (query != null && !query.isEmpty()) {
                putQueryParameters(request, query, staticQuery);
            }

            Map<String, String> metaNames = prefixHeaderNames(input);
            if (hasModeledContentType) {
                Object modeled = contentTypeField.getValueOrDefault((SdkPojo) input);
                if (modeled != null) {
                    request.putHeader(CONTENT_TYPE, modeled.toString());
                }
            }
            for (Map.Entry<String, List<String>> header : bound.headers().map().entrySet()) {
                String name = header.getKey();
                if (CONTENT_TYPE.equalsIgnoreCase(name)) {
                    // Dropped: smithy-java adds its own application/xml for the body, and merges it with a
                    // modeled value into one multi-valued header. The modeled value is set from the request
                    // below, and v2's default applied after it, so only one Content-Type goes out.
                    continue;
                }
                if (CONTENT_LENGTH.equalsIgnoreCase(name)) {
                    continue;
                }
                String lower = name.toLowerCase(Locale.US);
                String v2Name = headerNames.get(lower);
                if (v2Name == null) {
                    v2Name = metaNames.getOrDefault(lower, name);
                }
                request.putHeader(v2Name, header.getValue());
            }

            byte[] body = body(bound.body());
            if (body.length > 0) {
                if (hasBlobPayload) {
                    setContentTypeIfAbsent(request, "binary/octet-stream");
                } else if (!hasStringPayload) {
                    body = V2XmlBodyRules.apply(body);
                    setContentTypeIfAbsent(request, XML_MEDIA_TYPE);
                }
                request.putHeader(CONTENT_LENGTH, Integer.toString(body.length));
                request.contentStreamProvider(ContentStreamProvider.fromByteArrayUnsafe(body));
            }
            return request.build();
        }

        private byte[] body(DataStream stream) {
            if (stream == null || !hasPayloadMembers || stream.contentLength() == 0) {
                return EMPTY;
            }
            byte[] bytes = SerdeSupport.bytes(stream.asByteBuffer());
            // v2 writes no body at all when the payload member is unset; smithy-java writes an empty element.
            return isEmptyElement(bytes) ? EMPTY : bytes;
        }

        private Map<String, String> prefixHeaderNames(I input) {
            if (prefixHeaderMaps.isEmpty()) {
                return Map.of();
            }
            Map<String, String> names = new HashMap<>();
            for (SdkField<?> field : prefixHeaderMaps) {
                Object value = field.getValueOrDefault((SdkPojo) input);
                if (value instanceof Map) {
                    String prefix = field.getTrait(LocationTrait.class).locationName();
                    for (Object key : ((Map<?, ?>) value).keySet()) {
                        String name = prefix + key;
                        names.put(name.toLowerCase(Locale.US), name);
                    }
                }
            }
            return names;
        }
    }

    private static void setContentTypeIfAbsent(SdkHttpFullRequest.Builder request, String contentType) {
        if (contentType != null && !request.firstMatchingHeader(CONTENT_TYPE).isPresent()) {
            request.putHeader(CONTENT_TYPE, contentType);
        }
    }

    /** v2's literal query parameters for a C2J request URI, parsed exactly as {@code ProtocolUtils} parses them. */
    private static List<Map.Entry<String, String>> staticQuery(String requestUri) {
        if (requestUri == null) {
            return List.of();
        }
        int index = requestUri.indexOf('?');
        if (index < 0) {
            return List.of();
        }
        List<Map.Entry<String, String>> parameters = new java.util.ArrayList<>();
        for (String parameter : requestUri.substring(index + 1).split("[;&]")) {
            int eq = parameter.indexOf('=');
            if (eq < 0) {
                parameters.add(new java.util.AbstractMap.SimpleImmutableEntry<>(parameter, null));
            } else {
                parameters.add(new java.util.AbstractMap.SimpleImmutableEntry<>(parameter.substring(0, eq),
                                                                                parameter.substring(eq + 1)));
            }
        }
        return parameters;
    }

    private static String pathOf(String requestUri) {
        if (requestUri == null) {
            return "";
        }
        int index = requestUri.indexOf('?');
        return index < 0 ? requestUri : requestUri.substring(0, index);
    }

    // v2 keeps query parameters decoded and encodes them when the request is built; smithy-java hands back the
    // encoded query string. The literal parameters from the URI pattern are already set, so they are skipped.
    private static void putQueryParameters(SdkHttpFullRequest.Builder request, String query,
                                           List<Map.Entry<String, String>> staticQuery) {
        int start = 0;
        while (start <= query.length()) {
            int end = query.indexOf('&', start);
            if (end < 0) {
                end = query.length();
            }
            if (end > start) {
                String pair = query.substring(start, end);
                int eq = pair.indexOf('=');
                String name = SdkHttpUtils.urlDecode(eq < 0 ? pair : pair.substring(0, eq));
                if (!isStatic(staticQuery, name)) {
                    request.appendRawQueryParameter(name,
                                                    eq < 0 ? null : SdkHttpUtils.urlDecode(pair.substring(eq + 1)));
                }
            }
            start = end + 1;
        }
    }

    private static boolean isStatic(List<Map.Entry<String, String>> staticQuery, String name) {
        for (Map.Entry<String, String> parameter : staticQuery) {
            if (parameter.getKey().equals(name)) {
                return true;
            }
        }
        return false;
    }

    // An XML document that is nothing but one empty element, <X/> or <X></X>, optionally with attributes.
    private static boolean isEmptyElement(byte[] bytes) {
        int len = bytes.length;
        if (len < 4 || bytes[0] != '<' || bytes[len - 1] != '>') {
            return false;
        }
        if (bytes[len - 2] == '/') {
            for (int i = 1; i < len - 2; i++) {
                if (bytes[i] == '<' || bytes[i] == '>') {
                    return false;
                }
            }
            return true;
        }
        int firstClose = -1;
        for (int i = 1; i < len; i++) {
            if (bytes[i] == '>') {
                firstClose = i;
                break;
            }
        }
        return firstClose > 0 && firstClose + 1 < len && bytes[firstClose + 1] == '<' && bytes[firstClose + 2] == '/'
               && indexOf(bytes, (byte) '>', firstClose + 1) == len - 1;
    }

    private static int indexOf(byte[] bytes, byte b, int from) {
        for (int i = from; i < bytes.length; i++) {
            if (bytes[i] == b) {
                return i;
            }
        }
        return -1;
    }

    /** The structural description of an operation's output that both response handlers need. */
    private static final class OutputShape {
        private final boolean hasBodyMembers;
        private final boolean hasStringPayload;
        private final List<SdkField<?>> fields;

        private OutputShape(ApiOperation<?, ?> operation) {
            boolean body = false;
            boolean stringPayload = false;
            this.fields = ((SdkPojo) operation.outputBuilder()).sdkFields();
            for (SdkField<?> field : fields) {
                if (field.location() == MarshallLocation.PAYLOAD) {
                    body = true;
                }
                if (field.getTrait(PayloadTrait.class) != null && field.marshallingType() == MarshallingType.STRING) {
                    stringPayload = true;
                }
            }
            this.hasBodyMembers = body;
            this.hasStringPayload = stringPayload;
        }
    }

    private <O extends SerializableStruct> O deserialize(ApiOperation<?, O> operation, OutputShape output,
                                                         SdkHttpFullResponse response, byte[] body,
                                                         boolean anyRequestIdHeader) {
        ShapeBuilder<O> builder = operation.outputBuilder();
        software.amazon.smithy.java.http.api.HttpResponse smithyResponse =
            software.amazon.smithy.java.http.api.HttpResponse.of(HttpVersion.HTTP_1_1,
                                                                 response.statusCode(),
                                                                 HttpHeaders.of(response.headers()),
                                                                 body.length == 0 ? DataStream.ofEmpty()
                                                                                  : DataStream.ofBytes(body, XML_MEDIA_TYPE));
        httpBinding.responseDeserializer()
                   .payloadCodec(responseCodec)
                   .payloadMediaType(XML_MEDIA_TYPE)
                   .response(smithyResponse)
                   .outputShapeBuilder(builder)
                   .deserialize();
        V2HeaderBindings.apply((SdkPojo) builder, output.fields, response);
        if (builder instanceof AwsResponse.Builder) {
            // v2's two XML response handlers disagree about this, and both are in play: the combined handler an
            // operation with a modeled body uses (AwsXmlResponseTransformer) reads only x-amzn-RequestId, while
            // the streaming-output handler (AwsXmlResponseHandler) accepts any of v2's request-id spellings.
            ((AwsResponse.Builder) builder).responseMetadata(anyRequestIdHeader ? SerdeSupport.responseMetadata(response)
                                                                                : SerdeSupport.xmlResponseMetadata(response));
        }
        return builder.build();
    }

    private final class CombinedHandler<O extends SerializableStruct> implements HttpResponseHandler<Response<O>> {
        private final ApiOperation<?, O> operation;
        private final java.util.function.Supplier<HttpResponseHandler<Response<O>>> v2HandlerFactory;
        private final OutputShape output;
        private volatile HttpResponseHandler<Response<O>> v2Handler;

        private CombinedHandler(ApiOperation<?, O> operation,
                                java.util.function.Supplier<HttpResponseHandler<Response<O>>> v2HandlerFactory) {
            this.operation = operation;
            this.v2HandlerFactory = v2HandlerFactory;
            this.output = new OutputShape(operation);
        }

        @Override
        public Response<O> handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
            // A string payload (GetBucketPolicy) is read through an interceptor that rewrites the body for v2's
            // XML parser, so the bytes here are not the payload any more. Those operations stay on v2's handler.
            if (!response.isSuccessful() || output.hasStringPayload) {
                return v2Handler().handle(response, executionAttributes);
            }
            InputStream content = response.content().orElse(null);
            try {
                byte[] body = EMPTY;
                // Like v2, the body is only parsed when the output has members in it; v2 then also checks it for an
                // <Error> root, and on finding one treats the 200 as a failure. That decision, and the error, are
                // left to v2: it gets the response back with the bytes already read.
                if (output.hasBodyMembers && content != null) {
                    body = SerdeSupport.readAll(content, response);
                    if (V2XmlBodyRules.isErrorDocument(body)) {
                        return v2Handler().handle(withBody(response, body), executionAttributes);
                    }
                }
                O result;
                try {
                    result = deserialize(operation, output, response, body, false);
                } catch (RetryableException e) {
                    throw e;
                } catch (Exception e) {
                    if (e instanceof SdkException && ((SdkException) e).retryable()) {
                        throw (SdkException) e;
                    }
                    // Same message v2's AwsXmlPredicatedResponseHandler uses for a failed success unmarshall.
                    throw SdkClientException.builder()
                                            .message("Unable to unmarshall response (" + e.getMessage()
                                                     + "). Response Code: " + response.statusCode()
                                                     + ", Response Text: " + response.statusText().orElse(null))
                                            .cause(e)
                                            .build();
                }
                return Response.<O>builder().httpResponse(response).response(result).isSuccess(true).build();
            } finally {
                if (content != null) {
                    IoUtils.closeQuietlyV2(content, null);
                }
            }
        }

        private HttpResponseHandler<Response<O>> v2Handler() {
            HttpResponseHandler<Response<O>> handler = v2Handler;
            if (handler == null) {
                handler = v2HandlerFactory.get();
                v2Handler = handler;
            }
            return handler;
        }
    }

    private static SdkHttpFullResponse withBody(SdkHttpFullResponse response, byte[] body) {
        return response.toBuilder()
                       .content(AbortableInputStream.create(new ByteArrayInputStream(body)))
                       .build();
    }

    private final class StreamingHandler<O extends SerializableStruct> implements HttpResponseHandler<O> {
        private final ApiOperation<?, O> operation;
        private final OutputShape output;

        private StreamingHandler(ApiOperation<?, O> operation) {
            this.operation = operation;
            this.output = new OutputShape(operation);
        }

        @Override
        public O handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) {
            // Headers and status only; the body belongs to the response transformer.
            return deserialize(operation, output, response, EMPTY, true);
        }

        @Override
        public boolean needsConnectionLeftOpen() {
            return true;
        }
    }

    public static final class Builder {
        private SdkClientConfiguration clientConfiguration;

        private Builder() {
        }

        public Builder clientConfiguration(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
            return this;
        }

        public SmithyRestXmlProtocol build() {
            return new SmithyRestXmlProtocol(this);
        }
    }
}
