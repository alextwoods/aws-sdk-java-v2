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

package software.amazon.awssdk.dynamicclient.schema.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Validate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AuthDefinitionTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * The runtime port of codegen's default Trait_Inclusion_Policy ({@code TraitInclusionPolicy.forService} with an empty
 * {@code schemaTraits} configuration; design A.16, Requirement 25.3).
 *
 * <p>The effective set is computed in the same order as codegen, from the lists in {@link DefaultTraitInclusion}:
 * <ol>
 *     <li>the default traits (Requirement 9.2);</li>
 *     <li>for each trait on a service shape whose definition has {@code @protocolDefinition} or
 *     {@code @authDefinition}: that trait and every trait in the definition's {@code traits} list (Requirement 9.1);
 *     </li>
 *     <li>minus the constraint traits (Requirement 9.3);</li>
 *     <li>minus the hard-excluded traits and namespaces, which include every C2J_Marker_Trait (Requirement 9.4).</li>
 * </ol>
 *
 * <p>A protocol or auth trait whose definition shape is not in the model (for example because the model was assembled
 * without {@code smithy-aws-traits}) contributes nothing, as in codegen.
 */
@SdkInternalApi
@ThreadSafe
public final class RuntimeTraitInclusion {
    private static final SortedSet<ShapeId> DEFAULT_TRAITS = ids(DefaultTraitInclusion.DEFAULT_TRAITS);
    private static final SortedSet<ShapeId> EXCLUDED_CONSTRAINT_TRAITS =
        ids(DefaultTraitInclusion.EXCLUDED_CONSTRAINT_TRAITS);
    private static final SortedSet<ShapeId> HARD_EXCLUDED_TRAITS = ids(DefaultTraitInclusion.HARD_EXCLUDED_TRAITS);
    private static final SortedSet<String> HARD_EXCLUDED_NAMESPACES =
        Collections.unmodifiableSortedSet(new TreeSet<>(DefaultTraitInclusion.HARD_EXCLUDED_NAMESPACES));

    private final SortedSet<ShapeId> effectiveTraits;

    private RuntimeTraitInclusion(SortedSet<ShapeId> effectiveTraits) {
        this.effectiveTraits = Collections.unmodifiableSortedSet(effectiveTraits);
    }

    /**
     * Computes the policy for one service, as codegen does for the service's generated schemas.
     *
     * @param model the model that holds the service and its trait definitions
     * @param service the service shape
     * @return the policy
     */
    public static RuntimeTraitInclusion forService(Model model, ServiceShape service) {
        Validate.paramNotNull(service, "service");
        return forServices(model, Collections.singletonList(service));
    }

    /**
     * Computes the policy for several services: the defaults plus the protocol and auth traits of every service.
     *
     * @param model the model that holds the services and their trait definitions
     * @param services the service shapes; may be empty, which gives the defaults only
     * @return the policy
     */
    public static RuntimeTraitInclusion forServices(Model model, Collection<ServiceShape> services) {
        Validate.paramNotNull(model, "model");
        Validate.paramNotNull(services, "services");
        SortedSet<ShapeId> traits = new TreeSet<>(DEFAULT_TRAITS);
        for (ServiceShape service : services) {
            for (ShapeId traitId : new TreeSet<>(service.getAllTraits().keySet())) {
                Optional<Shape> definition = model.getShape(traitId);
                if (!definition.isPresent()) {
                    continue;
                }
                Optional<ProtocolDefinitionTrait> protocol = definition.get().getTrait(ProtocolDefinitionTrait.class);
                if (protocol.isPresent()) {
                    traits.add(traitId);
                    traits.addAll(protocol.get().getTraits());
                }
                Optional<AuthDefinitionTrait> auth = definition.get().getTrait(AuthDefinitionTrait.class);
                if (auth.isPresent()) {
                    traits.add(traitId);
                    traits.addAll(auth.get().getTraits());
                }
            }
        }
        traits.removeAll(EXCLUDED_CONSTRAINT_TRAITS);
        traits.removeIf(RuntimeTraitInclusion::isHardExcluded);
        return new RuntimeTraitInclusion(traits);
    }

    /**
     * Returns whether a trait is never included, whatever the service (Requirement 9.4).
     *
     * @param traitId the trait shape ID
     * @return true if the trait is hard-excluded
     */
    public static boolean isHardExcluded(ShapeId traitId) {
        return HARD_EXCLUDED_TRAITS.contains(traitId) || HARD_EXCLUDED_NAMESPACES.contains(traitId.getNamespace());
    }

    /**
     * @return the trait shape IDs that converted schemas carry, sorted
     */
    public SortedSet<ShapeId> effectiveTraits() {
        return effectiveTraits;
    }

    /**
     * @param traitId a trait shape ID
     * @return whether converted schemas carry the trait
     */
    public boolean includes(ShapeId traitId) {
        return effectiveTraits.contains(traitId);
    }

    /**
     * Returns the included traits a shape or member declares itself, sorted by trait shape ID. A member's result does
     * not contain its target's traits; those are merged at runtime (Requirement 10.4).
     *
     * @param shape a shape or member
     * @return the included traits
     */
    public List<Trait> includedTraits(Shape shape) {
        List<Trait> result = new ArrayList<>();
        for (Map.Entry<ShapeId, Trait> entry : new TreeMap<>(shape.getAllTraits()).entrySet()) {
            if (includes(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    private static SortedSet<ShapeId> ids(List<String> values) {
        SortedSet<ShapeId> result = new TreeSet<>();
        for (String value : values) {
            result.add(ShapeId.from(value));
        }
        return Collections.unmodifiableSortedSet(result);
    }
}
