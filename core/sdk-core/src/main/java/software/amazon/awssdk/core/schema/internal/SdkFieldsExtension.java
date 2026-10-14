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

package software.amazon.awssdk.core.schema.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;
import software.amazon.awssdk.schema.ShapeType;

/**
 * Gives every structure and union schema a slot that caches the {@code SdkField}s that
 * {@link software.amazon.awssdk.core.schema.SdkFieldsFromSchema} derives from it, so they are derived once per schema
 * (design A.6). Registered as a {@link SchemaExtensionProvider} through {@code META-INF/services}.
 */
@SdkInternalApi
public final class SdkFieldsExtension implements SchemaExtensionProvider<SdkFieldsExtension.Holder> {
    public static final SchemaExtensionKey<Holder> KEY = new SchemaExtensionKey<>();

    @Override
    public SchemaExtensionKey<Holder> key() {
        return KEY;
    }

    @Override
    public Holder provide(Schema schema) {
        if (schema.isMember() || schema.type() != ShapeType.STRUCTURE && schema.type() != ShapeType.UNION) {
            return null;
        }
        return new Holder();
    }

    /**
     * The cached value of one schema, set once. A race derives the same value twice and keeps one of them.
     */
    public static final class Holder {
        private volatile Object value;

        Holder() {
        }

        public Object get() {
            return value;
        }

        public void set(Object value) {
            this.value = value;
        }
    }
}
