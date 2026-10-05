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

package software.amazon.awssdk.smithy.runtime.registry;

import java.util.Objects;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A document's discriminator names a shape that the type registry does not register (Requirement 6.12).
 *
 * <p>It extends {@link UnsupportedOperationException}, which Smithy Java's {@code TypeRegistry.deserialize} documents
 * for documents that match no registered shape. The message is deterministic and contains no document values.
 */
public final class UnknownDiscriminatorException extends UnsupportedOperationException {

    private static final long serialVersionUID = 1L;

    private final transient ShapeId shapeId;

    UnknownDiscriminatorException(ShapeId shapeId) {
        super("No type is registered for the document discriminator " + shapeId);
        this.shapeId = Objects.requireNonNull(shapeId, "shapeId");
    }

    /** The exact shape ID the document's discriminator names. */
    public ShapeId shapeId() {
        return shapeId;
    }
}
