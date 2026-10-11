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
 * A factory that claims a trait ID the SDK already covers. It is only registered through a temporary class loader in
 * {@code TraitFactoriesTest}, never through the test resources.
 */
public final class TestDuplicateRequiredTraitFactory implements TraitFactory<RequiredTrait> {

    @Override
    public ShapeId traitId() {
        return RequiredTrait.ID;
    }

    @Override
    public Class<RequiredTrait> traitClass() {
        return RequiredTrait.class;
    }

    @Override
    public RequiredTrait create(Document value) {
        return RequiredTrait.INSTANCE;
    }
}
