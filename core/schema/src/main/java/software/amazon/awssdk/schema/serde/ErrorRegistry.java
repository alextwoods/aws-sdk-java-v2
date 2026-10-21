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

package software.amazon.awssdk.schema.serde;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.utils.Validate;

/**
 * The registry of a service's error shapes, with an index of the custom error codes that the
 * {@code aws.protocols#awsQueryError} trait gives some of them. Each generated service has one, shared by all of its
 * operations; client protocols use it to turn an error response into a modeled exception.
 *
 * <p>Instances are immutable and safe to use from multiple threads. Building one loads no error class: generated
 * registries register their entries with {@link Builder#putLazyType(ShapeId, Supplier)} and give the custom codes as
 * shape IDs.
 *
 * <p>SDK addition with no smithy-java counterpart; smithy-java's query protocols compute the code index by scanning
 * the operation's error schemas.
 */
@SdkProtectedApi
public final class ErrorRegistry implements TypeRegistry {
    private final DefaultTypeRegistry types;
    private final Map<String, ShapeId> codes;

    private ErrorRegistry(DefaultTypeRegistry types, Map<String, ShapeId> codes) {
        this.types = types;
        this.codes = codes;
    }

    /**
     * Creates a builder for an error registry.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Class<? extends SerializableStruct> getShapeClass(ShapeId id) {
        return types.getShapeClass(id);
    }

    @Override
    public ShapeBuilder<?> createBuilder(ShapeId id) {
        return types.createBuilder(id);
    }

    @Override
    public boolean contains(ShapeId id) {
        return types.contains(id);
    }

    /**
     * Returns the schema registered for an error shape ID. The first call for a shape ID invokes its schema supplier,
     * which loads the error class; later calls return the cached schema.
     *
     * @param id the error shape ID
     * @return the error's root schema, or null if the shape ID is not registered or was registered with
     *     {@link Builder#putType} (without a schema)
     * @throws IllegalStateException if the registered supplier returns an invalid schema
     */
    public Schema getSchema(ShapeId id) {
        return types.getSchema(id);
    }

    /**
     * Resolves a custom error code from the {@code awsQueryError} trait of an error shape, as the {@code awsQuery},
     * {@code ec2Query} and {@code awsQueryCompatible} protocols send it, to the error's shape ID. Any text from the
     * first {@code :} onward is ignored. This does not resolve shape names or shape IDs, only custom codes.
     *
     * @param rawCode the error code read from the response, for example
     *     {@code AWS.SimpleQueueService.NonExistentQueue}
     * @return the shape ID of the error with that custom code, or null if {@code rawCode} is null or no error has that
     *     code
     */
    public ShapeId resolveCode(String rawCode) {
        if (rawCode == null) {
            return null;
        }
        int colon = rawCode.indexOf(':');
        return codes.get(colon < 0 ? rawCode : rawCode.substring(0, colon));
    }

    boolean isEmpty() {
        return types.isEmpty();
    }

    @Override
    public String toString() {
        return "ErrorRegistry{types=" + types + ", codes=" + codes + "}";
    }

    /**
     * Builds an immutable {@link ErrorRegistry}. A builder is not safe for concurrent use; the registries it builds
     * are.
     */
    public static final class Builder {
        private final Map<ShapeId, DefaultTypeRegistry.Entry> entries = new LinkedHashMap<>();
        private final Map<String, ShapeId> codes = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Registers an error shape whose class and builder come from its schema, without loading the class. See
         * {@link TypeRegistry.Builder#putLazyType(ShapeId, Supplier)}. Registering a shape ID again replaces its
         * earlier entry.
         *
         * @param id the error shape ID
         * @param schema supplies the error's root schema
         * @return this builder
         * @throws NullPointerException if any argument is null
         */
        public Builder putLazyType(ShapeId id, Supplier<Schema> schema) {
            Validate.paramNotNull(id, "id");
            entries.put(id, DefaultTypeRegistry.lazyEntry(id, schema));
            return this;
        }

        /**
         * Registers an error shape with its class and builder supplier, as
         * {@link TypeRegistry.Builder#putType(ShapeId, Class, Supplier)} does. This is for errors that have no generated
         * class, such as the dynamic client's, whose builders read the error into a document; {@link #getSchema} returns
         * null for such an entry. Registering a shape ID again replaces its earlier entry.
         *
         * @param id the error shape ID
         * @param type the class of the errors the builders create
         * @param builder creates a new builder for the error on each call
         * @param <T> the error type
         * @return this builder
         * @throws NullPointerException if any argument is null
         */
        public <T extends SerializableStruct> Builder putType(ShapeId id, Class<T> type,
                                                              Supplier<ShapeBuilder<T>> builder) {
            Validate.paramNotNull(id, "id");
            entries.put(id, DefaultTypeRegistry.eagerEntry(type, builder));
            return this;
        }

        /**
         * Adds the custom error code of a registered error shape, from its {@code awsQueryError} trait. When two error
         * shapes have the same code, the one added first is used, as with the exception registration of earlier SDK
         * clients.
         *
         * @param code the custom error code
         * @param id the shape ID of the error, which must also be registered with {@link #putLazyType}
         * @return this builder
         * @throws NullPointerException if any argument is null
         * @throws IllegalArgumentException if {@code code} is blank
         */
        public Builder errorCode(String code, ShapeId id) {
            Validate.paramNotBlank(code, "code");
            Validate.paramNotNull(id, "id");
            codes.putIfAbsent(code, id);
            return this;
        }

        /**
         * Creates an error registry with the shapes and codes added so far. Later changes to this builder do not
         * affect it.
         *
         * @return the error registry
         * @throws IllegalStateException if an error code was added for a shape ID that is not registered
         */
        public ErrorRegistry build() {
            for (Map.Entry<String, ShapeId> code : codes.entrySet()) {
                if (!entries.containsKey(code.getValue())) {
                    throw new IllegalStateException("Error code " + code.getKey() + " refers to " + code.getValue()
                                                    + ", which is not registered");
                }
            }
            Map<String, ShapeId> codeIndex = codes.isEmpty()
                                             ? Collections.emptyMap()
                                             : Collections.unmodifiableMap(new HashMap<>(codes));
            return new ErrorRegistry(DefaultTypeRegistry.of(entries), codeIndex);
        }
    }
}
