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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.model.traits.AuthDefinitionTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * The Trait_Inclusion_Policy: which traits the Schema_Generator emits into a service's generated Schemas (design A.16,
 * Requirement 9).
 *
 * <p>The effective set is computed in this order:
 * <ol>
 *     <li>The defaults in {@link DefaultTraitInclusion#DEFAULT_TRAITS} (Requirement 9.2).</li>
 *     <li>For each trait on the service shape whose definition has {@code @protocolDefinition} or
 *     {@code @authDefinition}: that trait and every trait in the definition's {@code traits} list (Requirement 9.1).</li>
 *     <li>Minus the constraint traits in {@link DefaultTraitInclusion#EXCLUDED_CONSTRAINT_TRAITS}, which the SDK runtime
 *     does not validate (Requirement 9.3, OD-13).</li>
 *     <li>Minus hard-excluded traits (Requirement 9.4). Steps 1–4 are the SDK-owned set.</li>
 *     <li>Plus the configuration's additions, minus its removals (Requirement 9.5).</li>
 * </ol>
 *
 * <p>{@link #forService} fails with a {@link SchemaCodegenException} that lists every problem in the configuration:
 * an identifier that is not an absolute shape ID (Requirement 9.11), an addition of a hard-excluded trait (Requirement
 * 9.10), a removal of an SDK-owned trait (Requirement 9.6), or a removal of a trait the configuration does not add.
 * Generation calls it before writing any code.
 */
@SdkProtectedApi
public final class TraitInclusionPolicy {
    private final ShapeId serviceId;
    private final SortedSet<ShapeId> sdkTraits;
    private final SortedSet<ShapeId> configuredTraits;
    private final SortedSet<ShapeId> effectiveTraits;

    private TraitInclusionPolicy(ShapeId serviceId, SortedSet<ShapeId> sdkTraits, SortedSet<ShapeId> configuredTraits) {
        this.serviceId = serviceId;
        this.sdkTraits = Collections.unmodifiableSortedSet(sdkTraits);
        this.configuredTraits = Collections.unmodifiableSortedSet(configuredTraits);
        SortedSet<ShapeId> effective = new TreeSet<>(sdkTraits);
        effective.addAll(configuredTraits);
        this.effectiveTraits = Collections.unmodifiableSortedSet(effective);
    }

    /**
     * Computes the policy for the service of an IntermediateModel, with the {@code schemaTraits} entry of its
     * customization configuration.
     *
     * @param intermediateModel an IntermediateModel built from a Smithy model
     * @return the policy
     * @throws SchemaCodegenException if the configuration is invalid
     */
    public static TraitInclusionPolicy forService(IntermediateModel intermediateModel) {
        Model model = intermediateModel.smithyModel();
        ShapeId serviceId = intermediateModel.serviceId();
        if (model == null || serviceId == null) {
            throw new IllegalArgumentException("The intermediate model has no Smithy model");
        }
        return forService(model, model.expectShape(serviceId, ServiceShape.class),
                          TraitInclusionConfig.fromCustomization(intermediateModel.getCustomizationConfig()));
    }

    /**
     * Computes the policy for a service.
     *
     * @param model the assembled model that contains the service and its trait definitions
     * @param service the service shape
     * @param config additions and removals
     * @return the policy
     * @throws SchemaCodegenException if the configuration is invalid
     */
    public static TraitInclusionPolicy forService(Model model, ServiceShape service, TraitInclusionConfig config) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(config, "config");

        SortedSet<ShapeId> sdkTraits = new TreeSet<>(DefaultTraitInclusion.DEFAULT_TRAITS);
        for (ShapeId traitId : new TreeSet<>(service.getAllTraits().keySet())) {
            Optional<Shape> traitShape = model.getShape(traitId);
            if (!traitShape.isPresent()) {
                continue;
            }
            Optional<ProtocolDefinitionTrait> protocol = traitShape.get().getTrait(ProtocolDefinitionTrait.class);
            if (protocol.isPresent()) {
                sdkTraits.add(traitId);
                sdkTraits.addAll(protocol.get().getTraits());
            }
            Optional<AuthDefinitionTrait> auth = traitShape.get().getTrait(AuthDefinitionTrait.class);
            if (auth.isPresent()) {
                sdkTraits.add(traitId);
                sdkTraits.addAll(auth.get().getTraits());
            }
        }
        sdkTraits.removeAll(DefaultTraitInclusion.EXCLUDED_CONSTRAINT_TRAITS);
        sdkTraits.removeIf(DefaultTraitInclusion::isHardExcluded);

        List<String> problems = new ArrayList<>();
        SortedSet<ShapeId> configured = new TreeSet<>();
        for (String addition : config.additions()) {
            ShapeId traitId = parse(addition, "add", problems);
            if (traitId == null) {
                continue;
            }
            if (DefaultTraitInclusion.isHardExcluded(traitId)) {
                problems.add("trait " + traitId + " cannot be added: it is never included in schemas");
            } else {
                configured.add(traitId);
            }
        }
        SortedSet<ShapeId> removed = new TreeSet<>();
        for (String removal : config.removals()) {
            ShapeId traitId = parse(removal, "remove", problems);
            if (traitId == null) {
                continue;
            }
            if (sdkTraits.contains(traitId)) {
                problems.add("trait " + traitId + " cannot be removed: the SDK includes it by default or through the "
                             + "service's protocol or auth traits");
            } else if (!configured.contains(traitId)) {
                problems.add("trait " + traitId + " cannot be removed: only traits added by schemaTraits can be removed");
            } else {
                removed.add(traitId);
            }
        }
        if (!problems.isEmpty()) {
            throw new SchemaCodegenException("Invalid schemaTraits configuration for service " + service.getId() + ": "
                                             + String.join("; ", problems));
        }
        configured.removeAll(removed);
        configured.removeAll(sdkTraits);
        return new TraitInclusionPolicy(service.getId(), sdkTraits, configured);
    }

    private static ShapeId parse(String value, String setting, List<String> problems) {
        try {
            ShapeId traitId = ShapeId.from(value);
            if (!traitId.hasMember()) {
                return traitId;
            }
        } catch (ShapeIdSyntaxException e) {
            // Reported below.
        }
        problems.add("invalid trait identifier `" + value + "` in " + setting + ": expected an absolute shape ID such as "
                     + "smithy.api#jsonName");
        return null;
    }

    /**
     * @return the service this policy was computed for
     */
    public ShapeId serviceId() {
        return serviceId;
    }

    /**
     * @return the trait ShapeIds that generated Schemas carry, sorted (Requirement 9.5(c))
     */
    public SortedSet<ShapeId> effectiveTraits() {
        return effectiveTraits;
    }

    /**
     * @return the SDK-owned part of the effective set: defaults plus protocol and auth traits, minus constraint and
     *     hard-excluded traits
     */
    public SortedSet<ShapeId> sdkTraits() {
        return sdkTraits;
    }

    /**
     * @return the traits the configuration adds beyond {@link #sdkTraits()}
     */
    public SortedSet<ShapeId> configuredTraits() {
        return configuredTraits;
    }

    /**
     * @param traitId a trait ShapeId
     * @return whether generated Schemas carry the trait
     */
    public boolean includes(ShapeId traitId) {
        return effectiveTraits.contains(traitId);
    }

    /**
     * Returns the included traits a shape or member declares itself, sorted by trait ShapeId (Requirement 9.9). A
     * member's result does not contain its target's traits: those are on the target's Schema and merged at runtime
     * (Requirement 10.4).
     *
     * @param shape a shape or member
     * @return the traits to emit for it
     */
    public List<Trait> includedTraits(Shape shape) {
        List<Trait> result = new ArrayList<>();
        for (Map.Entry<ShapeId, Trait> entry : new TreeMap<>(shape.getAllTraits()).entrySet()) {
            if (includes(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return Collections.unmodifiableList(result);
    }
}
