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

package software.amazon.awssdk.codegen.model.config.customization;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code schemaTraits} entry of {@code customization.config}: trait ShapeIds to add to, or remove from, the traits
 * that generated Schemas carry (Requirement 9.5). For example:
 *
 * <pre>
 * "schemaTraits": {
 *     "add": ["smithy.api#documentation"],
 *     "remove": []
 * }
 * </pre>
 *
 * <p>{@code remove} may name only traits that {@code add} lists. Each entry must be an absolute ShapeId. See
 * {@code software.amazon.awssdk.codegen.schema.TraitInclusionPolicy} for the rules.
 */
public class SchemaTraitsConfig {
    private List<String> add = new ArrayList<>();
    private List<String> remove = new ArrayList<>();

    public List<String> getAdd() {
        return add;
    }

    public void setAdd(List<String> add) {
        this.add = add;
    }

    public List<String> getRemove() {
        return remove;
    }

    public void setRemove(List<String> remove) {
        this.remove = remove;
    }
}
