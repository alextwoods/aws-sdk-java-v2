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

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The fixed trait lists of the Trait_Inclusion_Policy (design A.16). This class is the single source of truth: the
 * policy reads it, and {@link DefaultTraitInclusionSpec} renders it as a dependency-free constants class for the
 * Dynamic_Client's runtime port of the policy (task 13.1).
 */
@SdkInternalApi
public final class DefaultTraitInclusion {
    /**
     * Traits included in every service's schemas regardless of protocol (Requirement 9.2). Includes {@code requiresLength}
     * and {@code unitType}, which design A.16 adds to the requirement's list.
     */
    public static final SortedSet<ShapeId> DEFAULT_TRAITS = ids(
        "smithy.api#sensitive",
        "smithy.api#required",
        "smithy.api#default",
        "smithy.api#sparse",
        "smithy.api#idempotencyToken",
        "smithy.api#error",
        "smithy.api#httpError",
        "smithy.api#retryable",
        "smithy.api#streaming",
        "smithy.api#requiresLength",
        "smithy.api#eventHeader",
        "smithy.api#eventPayload",
        "smithy.api#mediaType",
        "smithy.api#hostLabel",
        "smithy.api#endpoint",
        "smithy.api#http",
        "aws.protocols#httpChecksum",
        "smithy.api#httpChecksumRequired",
        "smithy.api#requestCompression",
        "smithy.api#paginated",
        "aws.protocols#awsQueryError",
        "aws.protocols#awsQueryCompatible",
        "aws.protocols#ec2QueryName",
        "smithy.api#xmlName",
        "smithy.api#xmlNamespace",
        "smithy.api#xmlAttribute",
        "smithy.api#xmlFlattened",
        "smithy.api#jsonName",
        "smithy.api#timestampFormat",
        "smithy.api#httpResponseCode",
        "smithy.api#unitType");

    /**
     * Constraint traits the SDK runtime does not validate, so they are left out of the defaults and the protocol and auth
     * trait lists (Requirement 9.3, OD-13). Enum values travel in {@code Schema.createEnum}, not in the {@code enum}
     * trait.
     */
    public static final SortedSet<ShapeId> EXCLUDED_CONSTRAINT_TRAITS = ids(
        "smithy.api#length",
        "smithy.api#range",
        "smithy.api#pattern",
        "smithy.api#uniqueItems",
        "smithy.api#enum");

    /**
     * Traits never included, whatever the configuration says (Requirement 9.4).
     */
    public static final SortedSet<ShapeId> HARD_EXCLUDED_TRAITS = ids(
        "smithy.api#documentation",
        "smithy.api#examples",
        "smithy.api#externalDocumentation",
        "smithy.api#since",
        "smithy.api#tags");

    /**
     * Namespaces whose traits are never included (Requirement 9.4): the C2J_Marker_Traits.
     */
    public static final SortedSet<String> HARD_EXCLUDED_NAMESPACES =
        Collections.unmodifiableSortedSet(new TreeSet<>(Collections.singleton("com.amazonaws.c2j")));

    private DefaultTraitInclusion() {
    }

    /**
     * Returns whether a trait is hard-excluded (Requirement 9.4).
     *
     * @param traitId the trait ShapeId
     * @return true if the trait is never included
     */
    public static boolean isHardExcluded(ShapeId traitId) {
        return HARD_EXCLUDED_TRAITS.contains(traitId) || HARD_EXCLUDED_NAMESPACES.contains(traitId.getNamespace());
    }

    private static SortedSet<ShapeId> ids(String... ids) {
        SortedSet<ShapeId> result = new TreeSet<>();
        for (String id : ids) {
            result.add(ShapeId.from(id));
        }
        return Collections.unmodifiableSortedSet(result);
    }
}
