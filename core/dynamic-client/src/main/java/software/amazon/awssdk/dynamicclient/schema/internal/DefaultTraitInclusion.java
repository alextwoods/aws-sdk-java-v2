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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The fixed trait lists of the SDK's Trait_Inclusion_Policy (design A.16), generated from codegen's
 * {@code DefaultTraitInclusion}.
 *
 * <p>This file is the output of codegen's {@code DefaultTraitInclusionSpec}, reformatted to the SDK code style. Do not
 * edit the lists by hand: change codegen's {@code DefaultTraitInclusion} and regenerate. Codegen's
 * {@code DefaultTraitInclusionSpecTest} fails when the two differ.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultTraitInclusion {
    /**
     * Trait shape IDs every schema includes (Requirement 9.2).
     */
    public static final List<String> DEFAULT_TRAITS = Collections.unmodifiableList(Arrays.<String>asList(
        "aws.protocols#awsQueryCompatible",
        "aws.protocols#awsQueryError",
        "aws.protocols#ec2QueryName",
        "aws.protocols#httpChecksum",
        "smithy.api#default",
        "smithy.api#endpoint",
        "smithy.api#error",
        "smithy.api#eventHeader",
        "smithy.api#eventPayload",
        "smithy.api#hostLabel",
        "smithy.api#http",
        "smithy.api#httpChecksumRequired",
        "smithy.api#httpError",
        "smithy.api#httpResponseCode",
        "smithy.api#idempotencyToken",
        "smithy.api#jsonName",
        "smithy.api#mediaType",
        "smithy.api#paginated",
        "smithy.api#requestCompression",
        "smithy.api#required",
        "smithy.api#requiresLength",
        "smithy.api#retryable",
        "smithy.api#sensitive",
        "smithy.api#sparse",
        "smithy.api#streaming",
        "smithy.api#timestampFormat",
        "smithy.api#unitType",
        "smithy.api#xmlAttribute",
        "smithy.api#xmlFlattened",
        "smithy.api#xmlName",
        "smithy.api#xmlNamespace"));

    /**
     * Constraint trait shape IDs left out of the defaults and the protocol and auth trait lists (Requirement 9.3).
     */
    public static final List<String> EXCLUDED_CONSTRAINT_TRAITS = Collections.unmodifiableList(Arrays.<String>asList(
        "smithy.api#enum",
        "smithy.api#length",
        "smithy.api#pattern",
        "smithy.api#range",
        "smithy.api#uniqueItems"));

    /**
     * Trait shape IDs never included (Requirement 9.4).
     */
    public static final List<String> HARD_EXCLUDED_TRAITS = Collections.unmodifiableList(Arrays.<String>asList(
        "smithy.api#documentation",
        "smithy.api#examples",
        "smithy.api#externalDocumentation",
        "smithy.api#since",
        "smithy.api#tags"));

    /**
     * Namespaces whose traits are never included (Requirement 9.4).
     */
    public static final List<String> HARD_EXCLUDED_NAMESPACES =
        Collections.unmodifiableList(Collections.<String>singletonList("com.amazonaws.c2j"));

    private DefaultTraitInclusion() {
    }
}
