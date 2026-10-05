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

package software.amazon.awssdk.eventnotifications.s3;

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSchemas;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationTypeRegistry;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Schema and type-registry metadata of the S3 event notification model ({@code software.amazon.awssdk.eventnotifications.s3.model}).
 *
 * <p>Every model type is a Smithy {@code SerializableStruct} that publishes its one schema as {@code SCHEMA}, in the
 * namespace {@link #namespace()}, so notifications convert to and from Smithy {@code Document}s. The primary type
 * registry composes with generated service registries without copying entries, for example to construct typed
 * values from discriminated documents of either origin:
 *
 * <pre>{@code
 * SchemaTypeRegistry combined = SchemaTypeRegistry.compose(
 *     S3ServiceMetadata.typeRegistry(),
 *     S3EventNotificationMetadata.typeRegistry());
 * S3EventNotification event = (S3EventNotification) combined.deserialize(document);
 * }</pre>
 */
public final class S3EventNotificationMetadata {
    private S3EventNotificationMetadata() {
    }

    /** The Smithy namespace of every event notification shape. */
    public static String namespace() {
        return S3EventNotificationSchemas.NAMESPACE;
    }

    /** Every schema of the namespace (structures, lists and maps), sorted by shape ID. */
    public static List<Schema> schemas() {
        return S3EventNotificationSchemas.all();
    }

    /** The schema with exactly the absolute shape ID {@code id}, if it is an event notification shape. */
    public static Optional<Schema> schema(ShapeId id) {
        return Optional.ofNullable(S3EventNotificationSchemas.get(id));
    }

    /**
     * The primary type registry: every event notification structure mapped to its schema (the type's {@code SCHEMA}),
     * Java class and builder factory. Built on first use; looking a type up initializes the model types.
     */
    public static SchemaTypeRegistry typeRegistry() {
        return S3EventNotificationTypeRegistry.registry();
    }
}
