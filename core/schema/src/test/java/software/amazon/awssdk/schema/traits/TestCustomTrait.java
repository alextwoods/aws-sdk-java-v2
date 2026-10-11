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

package software.amazon.awssdk.schema.traits;

import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * A third-party trait used to test {@link java.util.ServiceLoader} discovery of {@link TraitFactory} implementations.
 */
final class TestCustomTrait implements Trait {
    static final ShapeId ID = ShapeId.from("com.example.test#customTrait");

    private final String value;

    TestCustomTrait(String value) {
        this.value = value;
    }

    String value() {
        return value;
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    @Override
    public Document toDocument() {
        return Document.of(value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TestCustomTrait && value.equals(((TestCustomTrait) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
