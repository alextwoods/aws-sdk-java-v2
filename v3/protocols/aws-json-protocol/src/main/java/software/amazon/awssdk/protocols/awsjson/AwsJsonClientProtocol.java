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

package software.amazon.awssdk.protocols.awsjson;

import java.time.Instant;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.protocols.core.serde.LocatingDeserializer;
import software.amazon.awssdk.protocols.core.serde.TimestampFormats;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * The per-client AWS JSON 1.0/1.1 runtime. Wire rules (Smithy {@code awsJson1_0}/{@code awsJson1_1}):
 * <ul>
 *   <li>every request is {@code POST /} with {@code X-Amz-Target: <service shape name>.<operation shape name>} and
 *       {@code Content-Type: application/x-amz-json-1.0} (or {@code 1.1}); the body is the whole input as a JSON
 *       object ({@code {}} when it has no members); HTTP binding traits are ignored;</li>
 *   <li>member names are the modeled names: {@code jsonName} is ignored;</li>
 *   <li>timestamps honor {@code timestampFormat} and default to epoch seconds, read exactly;</li>
 *   <li>the error type comes from {@code X-Amzn-Errortype}, else the body's {@code __type}, else its {@code code},
 *       with any namespace and {@code :} suffix removed; {@code awsQueryCompatible} services send
 *       {@code x-amzn-query-mode: true} and report the {@code x-amzn-query-error} code.</li>
 * </ul>
 */
final class AwsJsonClientProtocol implements ClientProtocol {
    private final ProtocolContext context;
    private final String contentType;
    private final JsonCodec codec;
    private final ModeledErrors errors;
    private final LocatingDeserializer.TimestampReader timestamps;

    AwsJsonClientProtocol(ProtocolContext context, String contentType) {
        this.context = context;
        this.contentType = contentType;
        this.codec = JsonCodec.builder()
                              .useJsonName(false)
                              .useTimestampFormat(true)
                              .defaultTimestampFormat(TimestampFormats.EXACT_EPOCH_SECONDS)
                              .defaultNamespace(context.service().serviceId().getNamespace())
                              .build();
        this.errors = ModeledErrors.of(context);
        this.timestamps = AwsJsonClientProtocol::readTimestamp;
    }

    @Override
    public ShapeId protocolId() {
        return context.selected().protocolId();
    }

    @Override
    public OperationProtocol operation(OperationSchemas operation) {
        return new AwsJsonOperationProtocol(this, operation);
    }

    ProtocolContext context() {
        return context;
    }

    String contentType() {
        return contentType;
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
