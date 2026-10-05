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

import software.amazon.smithy.java.core.serde.document.DiscriminatorException;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * A type registry was asked to construct a typed value from a document that has no shape discriminator
 * (Requirement 6.11).
 *
 * <p>It extends Smithy Java's {@link DiscriminatorException}, which {@code TypeRegistry.deserialize} documents for
 * documents without a usable discriminator. The message is deterministic and contains no document values.
 */
public final class MissingDiscriminatorException extends DiscriminatorException {

    private static final long serialVersionUID = 1L;

    private final transient ShapeType sourceKind;

    MissingDiscriminatorException(ShapeType sourceKind) {
        super("Cannot construct a registered type from a " + sourceKind + " document: the document has no shape "
              + "discriminator");
        this.sourceKind = sourceKind;
    }

    /** The Smithy kind of the document that lacks a discriminator. */
    public ShapeType sourceKind() {
        return sourceKind;
    }
}
