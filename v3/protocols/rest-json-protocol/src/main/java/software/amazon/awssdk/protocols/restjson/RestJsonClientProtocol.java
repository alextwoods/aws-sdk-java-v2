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

package software.amazon.awssdk.protocols.restjson;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.binding.HttpMessageBindings;
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.protocols.core.serde.HttpBindingReader;
import software.amazon.awssdk.protocols.core.serde.LocatingDeserializer;
import software.amazon.awssdk.protocols.core.serde.TimestampFormats;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * The per-client {@code restJson1} runtime. Wire rules (Smithy {@code aws.protocols#restJson1}):
 * <ul>
 *   <li>the request method, path, query and headers follow the Smithy HTTP binding traits
 *       ({@link software.amazon.awssdk.protocols.core.serde.HttpBindingWriter}); responses and errors are read with
 *       {@link HttpBindingReader};</li>
 *   <li>the body is a JSON object of the body members, member names honoring {@code jsonName}, sent (as {@code {}} if
 *       every body member is absent) whenever the input has body members, and absent otherwise;</li>
 *   <li>an {@code httpPayload} member is the whole body: a structure as a JSON object ({@code {}} when absent), a
 *       union or document as JSON (no body when absent), a blob as its raw bytes and a string or enum as its text
 *       (no body when absent);</li>
 *   <li>{@code Content-Type} is set only when there is a body, unless an {@code httpHeader("Content-Type")} member
 *       supplies it: {@code application/json} for JSON, else the payload target's {@code mediaType},
 *       {@code application/octet-stream} for blobs or {@code text/plain} for strings;</li>
 *   <li>body timestamps honor {@code timestampFormat} and default to epoch seconds, read exactly;</li>
 *   <li>the error type comes from {@code X-Amzn-Errortype}, else the body's {@code __type}, else its {@code code},
 *       with any namespace and {@code :} suffix removed.</li>
 * </ul>
 */
final class RestJsonClientProtocol implements ClientProtocol {
    private final ProtocolContext context;
    private final JsonCodec codec;
    private final ModeledErrors errors;
    private final LocatingDeserializer.TimestampReader timestamps;
    private final Map<ShapeId, HttpBindingReader> registryErrorReaders = new ConcurrentHashMap<>();

    RestJsonClientProtocol(ProtocolContext context) {
        this.context = context;
        this.codec = JsonCodec.builder()
                              .useJsonName(true)
                              .useTimestampFormat(true)
                              .defaultTimestampFormat(TimestampFormats.EXACT_EPOCH_SECONDS)
                              .defaultNamespace(context.service().serviceId().getNamespace())
                              .build();
        this.errors = ModeledErrors.of(context);
        this.timestamps = RestJsonClientProtocol::readTimestamp;
    }

    @Override
    public ShapeId protocolId() {
        return context.selected().protocolId();
    }

    @Override
    public OperationProtocol operation(OperationSchemas operation) {
        return new RestJsonOperationProtocol(this, operation);
    }

    ProtocolContext context() {
        return context;
    }

    JsonCodec codec() {
        return codec;
    }

    ModeledErrors errors() {
        return errors;
    }

    LocatingDeserializer.TimestampReader timestamps() {
        return timestamps;
    }

    /** The reader of a modeled error that the operation does not declare (resolved through the registry). */
    HttpBindingReader registryErrorReader(Schema error) {
        return registryErrorReaders.computeIfAbsent(
            error.id(), id -> HttpBindingReader.of(HttpMessageBindings.of(error, HttpMessageBindings.Direction.RESPONSE)));
    }

    /** Epoch-seconds timestamps are read from the number's decimal text, exactly. */
    private static Instant readTimestamp(Schema schema, ShapeDeserializer delegate) {
        TimestampFormatTrait.Format format = TimestampFormats.formatOf(schema,
                                                                       TimestampFormatTrait.Format.EPOCH_SECONDS);
        if (format == TimestampFormatTrait.Format.EPOCH_SECONDS) {
            return TimestampFormats.fromEpochSeconds(delegate.readBigDecimal(schema));
        }
        return delegate.readTimestamp(schema);
    }
}
