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

package software.amazon.awssdk.schema;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The Smithy IDL 2.0 shape types.
 *
 * <p>This is the SDK runtime port of {@code software.amazon.smithy.model.shapes.ShapeType}, without a dependency on
 * {@code smithy-model}. Unlike smithy-model, it has no {@code SET} constant: a Smithy {@code set} is represented as
 * a {@link #LIST} with the {@code uniqueItems} trait, as in Smithy IDL 2.0.
 */
@SdkPublicApi
public enum ShapeType {
    BLOB("blob", Category.SIMPLE),
    BOOLEAN("boolean", Category.SIMPLE),
    STRING("string", Category.SIMPLE),
    ENUM("enum", Category.SIMPLE),
    TIMESTAMP("timestamp", Category.SIMPLE),
    BYTE("byte", Category.SIMPLE),
    SHORT("short", Category.SIMPLE),
    INTEGER("integer", Category.SIMPLE),
    INT_ENUM("intEnum", Category.SIMPLE),
    LONG("long", Category.SIMPLE),
    FLOAT("float", Category.SIMPLE),
    DOUBLE("double", Category.SIMPLE),
    BIG_INTEGER("bigInteger", Category.SIMPLE),
    BIG_DECIMAL("bigDecimal", Category.SIMPLE),
    DOCUMENT("document", Category.SIMPLE),

    LIST("list", Category.AGGREGATE),
    MAP("map", Category.AGGREGATE),
    STRUCTURE("structure", Category.AGGREGATE),
    UNION("union", Category.AGGREGATE),

    MEMBER("member", Category.MEMBER),

    SERVICE("service", Category.SERVICE),
    RESOURCE("resource", Category.SERVICE),
    OPERATION("operation", Category.SERVICE);

    private final String idlName;
    private final Category category;

    ShapeType(String idlName, Category category) {
        this.idlName = idlName;
        this.category = category;
    }

    /**
     * Returns the category of this shape type.
     *
     * @return the category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Returns whether a shape of this type can be used where a shape of {@code other} is expected. That is true when
     * the types are equal, when this is {@link #ENUM} and {@code other} is {@link #STRING}, and when this is
     * {@link #INT_ENUM} and {@code other} is {@link #INTEGER}.
     *
     * @param other the type to check against
     * @return true if this type is {@code other} or a specialization of it
     */
    public boolean isShapeType(ShapeType other) {
        if (this == other) {
            return true;
        }
        if (this == ENUM) {
            return other == STRING;
        }
        if (this == INT_ENUM) {
            return other == INTEGER;
        }
        return false;
    }

    /**
     * Returns the Smithy IDL name of this shape type, for example {@code bigInteger} or {@code intEnum}.
     *
     * @return the Smithy IDL name
     */
    @Override
    public String toString() {
        return idlName;
    }

    /**
     * The category of a shape type.
     */
    @SdkPublicApi
    public enum Category {
        /**
         * Simple types: blob, boolean, string, enum, timestamp, the numeric types, intEnum and document.
         */
        SIMPLE,

        /**
         * Aggregate types: list, map, structure and union.
         */
        AGGREGATE,

        /**
         * The member type.
         */
        MEMBER,

        /**
         * Service types: service, resource and operation.
         */
        SERVICE
    }
}
