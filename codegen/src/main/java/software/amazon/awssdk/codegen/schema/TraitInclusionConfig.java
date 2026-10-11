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

package software.amazon.awssdk.codegen.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.customization.SchemaTraitsConfig;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Additions to and removals from the default Trait_Inclusion_Policy (Requirement 9.5), from the {@code schemaTraits}
 * entry of {@code customization.config} or built through {@link #builder()}.
 *
 * <p>Entries are kept as the strings given. {@link TraitInclusionPolicy#forService} parses and checks them, so an invalid
 * identifier fails generation before any code is written (Requirement 9.11).
 */
@SdkProtectedApi
public final class TraitInclusionConfig {
    private static final TraitInclusionConfig EMPTY = builder().build();

    private final List<String> additions;
    private final List<String> removals;

    private TraitInclusionConfig(Builder builder) {
        this.additions = Collections.unmodifiableList(new ArrayList<>(builder.additions));
        this.removals = Collections.unmodifiableList(new ArrayList<>(builder.removals));
    }

    /**
     * @return a configuration with no additions or removals
     */
    public static TraitInclusionConfig empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reads the {@code schemaTraits} entry of a service's customization configuration.
     *
     * @param config the customization configuration, which may be null
     * @return the configuration, or {@link #empty()} if there is no {@code schemaTraits} entry
     */
    public static TraitInclusionConfig fromCustomization(CustomizationConfig config) {
        SchemaTraitsConfig schemaTraits = config == null ? null : config.getSchemaTraits();
        if (schemaTraits == null) {
            return EMPTY;
        }
        Builder builder = builder();
        if (schemaTraits.getAdd() != null) {
            schemaTraits.getAdd().forEach(builder::add);
        }
        if (schemaTraits.getRemove() != null) {
            schemaTraits.getRemove().forEach(builder::remove);
        }
        return builder.build();
    }

    /**
     * @return the trait identifiers to add, in the order given
     */
    public List<String> additions() {
        return additions;
    }

    /**
     * @return the trait identifiers to remove, in the order given
     */
    public List<String> removals() {
        return removals;
    }

    public static final class Builder {
        private final List<String> additions = new ArrayList<>();
        private final List<String> removals = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds a trait, given as an absolute ShapeId such as {@code smithy.api#documentation}.
         */
        public Builder add(String traitId) {
            additions.add(Objects.requireNonNull(traitId, "traitId"));
            return this;
        }

        public Builder add(ShapeId traitId) {
            return add(traitId.toString());
        }

        /**
         * Removes a trait that {@link #add} added.
         */
        public Builder remove(String traitId) {
            removals.add(Objects.requireNonNull(traitId, "traitId"));
            return this;
        }

        public Builder remove(ShapeId traitId) {
            return remove(traitId.toString());
        }

        public TraitInclusionConfig build() {
            return new TraitInclusionConfig(this);
        }
    }
}
