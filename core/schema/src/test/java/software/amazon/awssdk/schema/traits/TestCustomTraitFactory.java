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
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * A third-party factory registered through {@code META-INF/services} in the test resources. It is public with a
 * public no-argument constructor because {@link java.util.ServiceLoader} requires it on Java 8.
 *
 * <p>Its error message deliberately leaves out the trait ID, so tests can check that the registry adds it.
 */
public final class TestCustomTraitFactory implements TraitFactory<TestCustomTrait> {

    @Override
    public ShapeId traitId() {
        return TestCustomTrait.ID;
    }

    @Override
    public Class<TestCustomTrait> traitClass() {
        return TestCustomTrait.class;
    }

    @Override
    public TestCustomTrait create(Document value) {
        if (value == null || value.type() != ShapeType.STRING) {
            throw new SerializationException("expected a string");
        }
        return new TestCustomTrait(value.asString());
    }
}
