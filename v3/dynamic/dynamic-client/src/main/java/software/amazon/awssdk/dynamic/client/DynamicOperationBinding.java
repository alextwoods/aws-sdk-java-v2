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

package software.amazon.awssdk.dynamic.client;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.dynamic.client.DynamicInvocationException.Violation;
import software.amazon.awssdk.dynamic.model.DynamicAwsOperation;
import software.amazon.awssdk.pipeline.AwsOperation;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * The pipeline binding of one runtime operation: the {@link AwsOperation} a client's {@code ClientRuntime} plans
 * from, exactly what a generated service's {@code <Service>OperationBindings} constant holds for the operation, and
 * the validators of its input and of the events of its input event stream.
 *
 * <p>Created once per client and operation, on the operation's first call; immutable and thread safe. Holds no
 * customer data.
 *
 * @param service absolute shape ID of the service
 * @param operation the runtime operation
 * @param pipeline the operation's pipeline binding, of schema-guided input and output documents
 * @param validator the validator of the operation's input
 * @param inputEvents the validator of the events of the operation's input event stream union, or {@code null}
 *        without one
 */
record DynamicOperationBinding(ShapeId service, DynamicAwsOperation operation,
                               AwsOperation<StructDocument, StructDocument> pipeline,
                               DynamicInputValidator validator, DynamicInputValidator inputEvents) {

    DynamicOperationBinding {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(pipeline, "pipeline");
        Objects.requireNonNull(validator, "validator");
    }

    /**
     * Checks that a call with or without a request body and a response transformer matches the operation's
     * streaming blob members, as the generated client's overloads do: a streaming blob input needs the request body
     * that carries it, a streaming blob output the transformer that consumes it, and neither argument is accepted
     * without its member. Event stream members are carried by the input and output documents.
     *
     * @throws DynamicInvocationException ({@link DynamicInvocationException.Reason#REQUIRES_STREAMING_CALL}) naming
     *         the streaming blob member a missing argument would carry, or
     *         ({@link DynamicInvocationException.Reason#NO_STREAMING_MEMBER}) for an argument without a member
     */
    void requireCall(boolean requestBody, boolean responseTransformer) {
        OperationSchemas schemas = operation.schemas();
        Schema input = blob(schemas.inputStreamMember());
        Schema output = blob(schemas.outputStreamMember());
        if (input != null && !requestBody) {
            throw DynamicInvocationException.requiresStreamingCall(service, operation.id(), operation.name(), "input",
                                                                   input.memberName());
        }
        if (output != null && !responseTransformer) {
            throw DynamicInvocationException.requiresStreamingCall(service, operation.id(), operation.name(),
                                                                   "output", output.memberName());
        }
        if (input == null && requestBody) {
            throw DynamicInvocationException.noStreamingMember(service, operation.id(), operation.name(), "input",
                                                               "request body");
        }
        if (output == null && responseTransformer) {
            throw DynamicInvocationException.noStreamingMember(service, operation.id(), operation.name(), "output",
                                                               "response transformer");
        }
    }

    /**
     * Checks that the operation's output has no event stream member, for an async call without an event stream
     * response handler: an async output event stream is read only under the handler's subscriber's demand, never
     * buffered into the output document (Requirement 14.12), as the generated async client offers only the handler
     * overload for it.
     *
     * @throws DynamicInvocationException ({@link DynamicInvocationException.Reason#REQUIRES_STREAMING_CALL}) naming
     *         the output event stream member
     */
    void requireNoAsyncOutputEventStream() {
        Schema events = operation.schemas().outputStreamMember().filter(m -> m.type() != ShapeType.BLOB).orElse(null);
        if (events != null) {
            throw DynamicInvocationException.requiresStreamingCall(service, operation.id(), operation.name(),
                                                                   "output event stream", events.memberName());
        }
    }

    /**
     * Checks that the operation's output has an event stream member, for a call with an event stream response
     * handler.
     *
     * @throws DynamicInvocationException ({@link DynamicInvocationException.Reason#NO_STREAMING_MEMBER}) if it has none
     */
    void requireOutputEventStream() {
        if (operation.schemas().outputStreamMember().filter(m -> m.type() != ShapeType.BLOB).isEmpty()) {
            throw DynamicInvocationException.noStreamingMember(service, operation.id(), operation.name(),
                                                               "output event stream", "event stream handler");
        }
    }

    private static Schema blob(Optional<Schema> member) {
        return member.filter(m -> m.type() == ShapeType.BLOB).orElse(null);
    }

    /**
     * {@code event} as one event of the operation's input event stream: validated against the exact event stream
     * union, then converted to a schema-guided document of it, which the stream's writer accepts.
     *
     * @throws DynamicInvocationException ({@link DynamicInvocationException.Reason#NO_STREAMING_MEMBER}) if the
     *         operation has no input event stream, or ({@link DynamicInvocationException.Reason#INVALID_INPUT})
     *         listing every invalid element of the event
     */
    StructDocument inputEvent(Document event) {
        Objects.requireNonNull(event, "event");
        if (inputEvents == null) {
            throw DynamicInvocationException.noStreamingMember(service, operation.id(), operation.name(),
                                                               "input event stream", "event");
        }
        return convert(inputEvents, event, "event");
    }

    /**
     * {@code document} as the operation's input: validated against the exact input schema, then converted to a
     * schema-guided document of it (the same object when it already is one).
     *
     * @throws DynamicInvocationException ({@link DynamicInvocationException.Reason#INVALID_INPUT}) listing every
     *         invalid element if the document does not conform
     */
    StructDocument input(Document document) {
        Objects.requireNonNull(document, "input");
        return convert(validator, document, "input");
    }

    private StructDocument convert(DynamicInputValidator checker, Document document, String root) {
        Schema schema = checker.input();
        List<Violation> violations = checker.validate(document);
        if (!violations.isEmpty()) {
            throw DynamicInvocationException.invalidInput(service, operation.id(), operation.name(), schema.id(),
                                                          violations, null);
        }
        if (document instanceof StructDocument struct && struct.schema() == schema) {
            return struct;
        }
        try {
            if (schema.hasTrait(TraitKey.STREAMING_TRAIT)) {
                // One event of an event stream union: StructDocument.of would read the union as the stream itself.
                ShapeBuilder<StructDocument> event = SchemaConverter.createDocumentBuilder(schema, service);
                document.asStringMap().forEach((member, value) -> {
                    if (value != null) {
                        event.setMemberValue(schema.member(member), value);
                    }
                });
                return event.build();
            }
            return StructDocument.of(schema, document, service);
        } catch (RuntimeException e) {
            // A conforming document whose values the schema-guided conversion still cannot read.
            throw DynamicInvocationException.invalidInput(service, operation.id(), operation.name(), schema.id(),
                                                          List.of(new Violation(root, "cannot be read as "
                                                                                      + schema.id() + ": "
                                                                                      + e.getMessage())),
                                                          e);
        }
    }
}
