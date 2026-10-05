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

package software.amazon.awssdk.smithy.runtime.document;

import java.util.Objects;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * A Document value could not be coerced to a requested schema (Requirements 6.10 and 6.14).
 *
 * <p>The exception is deterministic: the same document and schema always produce the same reason, source kind,
 * requested shape, discriminator, path and message. The message never contains document values, which may be
 * sensitive; it identifies the value only by its path from the converted root ({@code $}, then {@code .member},
 * {@code [index]} and {@code ["key"]} steps).
 *
 * <p>It extends Smithy Java's {@link SerializationException}, which Smithy Java's own document accessors throw for
 * incompatible kinds, so callers that already handle that exception continue to work.
 */
public final class DocumentCoercionException extends SerializationException {

    private static final long serialVersionUID = 1L;

    /** Why a value could not be coerced. */
    public enum Reason {
        /** The document's kind cannot represent the requested shape type (for example a string for an integer). */
        INCOMPATIBLE_KIND("incompatible document kind"),
        /** A numeric value lies outside the range of the requested numeric type. */
        OUT_OF_RANGE("numeric value out of range"),
        /** A number with a fractional part (or NaN) was requested as an integral type. */
        NOT_INTEGRAL("numeric value is not integral"),
        /** The document's protocol representation of a blob or timestamp is invalid (for example bad base64). */
        INVALID_REPRESENTATION("invalid protocol representation"),
        /** The document carries a discriminator that is not a valid shape ID. */
        INVALID_DISCRIMINATOR("invalid discriminator"),
        /** The document's discriminator names a different shape than the requested schema. */
        DISCRIMINATOR_MISMATCH("discriminator conflicts with the requested shape"),
        /** A union document sets more than one member. */
        UNION_MEMBER_COUNT("union document sets more than one member");

        private final String description;

        Reason(String description) {
            this.description = description;
        }

        /** A short, fixed description of the reason, used in messages. */
        public String description() {
            return description;
        }
    }

    private final Reason reason;
    private final ShapeType sourceKind;
    private final transient ShapeId requestedShape;
    private final transient ShapeId discriminator;
    private final String path;

    DocumentCoercionException(Reason reason, ShapeType sourceKind, ShapeId requestedShape, ShapeType requestedType,
                              ShapeId discriminator, String path, Throwable cause) {
        super(message(reason, sourceKind, requestedShape, requestedType, discriminator, path), cause);
        this.reason = Objects.requireNonNull(reason, "reason");
        this.sourceKind = sourceKind;
        this.requestedShape = Objects.requireNonNull(requestedShape, "requestedShape");
        this.discriminator = discriminator;
        this.path = Objects.requireNonNull(path, "path");
    }

    private static String message(Reason reason, ShapeType sourceKind, ShapeId requestedShape, ShapeType requestedType,
                                  ShapeId discriminator, String path) {
        StringBuilder message = new StringBuilder("Cannot coerce ")
            .append(sourceKind == null ? "null" : sourceKind.toString())
            .append(" document at ").append(path)
            .append(" to ").append(requestedShape)
            .append(" (").append(requestedType).append("): ")
            .append(reason.description());
        if (discriminator != null) {
            message.append(" (document discriminator ").append(discriminator)
                   .append(", requested shape ").append(requestedShape).append(')');
        }
        return message.toString();
    }

    /** Why the value could not be coerced. */
    public Reason reason() {
        return reason;
    }

    /** The Smithy kind of the document value, or {@code null} if the value was a null document. */
    public ShapeType sourceKind() {
        return sourceKind;
    }

    /** The absolute shape ID the value was requested as (the target shape when the value is a member). */
    public ShapeId requestedShape() {
        return requestedShape;
    }

    /**
     * The document's discriminator for {@link Reason#DISCRIMINATOR_MISMATCH}, otherwise {@code null}.
     */
    public ShapeId discriminator() {
        return discriminator;
    }

    /** The path of the value from the converted root, for example {@code $.details.sizes[2]}. */
    public String path() {
        return path;
    }
}
