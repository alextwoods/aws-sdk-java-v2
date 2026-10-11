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

package software.amazon.awssdk.codegen.c2j2smithy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Converts C2J {@code paginators-1.json} entries into {@code @paginated} traits on the converted operations.
 *
 * <p>A paginator becomes {@code @paginated(inputToken, outputToken, items, pageSize)} when Smithy can express it: one
 * input token that is a non-required top-level input member targeting a string or map, one output token path that
 * resolves to a string or map. Anything else is recorded in the gap report, and the operation gets no
 * {@code @paginated} trait. A result key that is not one path to a list or map, or a limit key that is not a top-level
 * integral member, is also recorded, and only {@code items} or {@code pageSize} is left out.
 *
 * <p>Map tokens, non-integer page sizes and required output tokens are valid C2J pagination that Smithy flags as
 * DANGER; those operations also get a {@code @suppress} for exactly those validation event IDs.
 */
final class PaginatorConversion {
    private static final Set<ShapeType> TOKEN_TARGETS = EnumSet.of(ShapeType.STRING, ShapeType.MAP);
    private static final Set<ShapeType> ITEM_TARGETS = EnumSet.of(ShapeType.LIST, ShapeType.MAP);
    private static final Set<ShapeType> PAGE_SIZE_TARGETS = EnumSet.of(ShapeType.BYTE, ShapeType.SHORT,
                                                                         ShapeType.INTEGER, ShapeType.LONG);
    private static final String WRONG_SHAPE_TYPE = "PaginatedTrait.WrongShapeType";
    private static final String SHOULD_NOT_BE_REQUIRED = "PaginatedTrait.ShouldNotBeRequired";
    private static final String NOT_CONVERTED = "The operation gets no @paginated trait, so no paginator is generated "
                                                + "from standard traits.";

    private final Model model;
    private final ConversionGapReport.Builder gaps;

    private PaginatorConversion(Model model, ConversionGapReport.Builder gaps) {
        this.model = model;
        this.gaps = gaps;
    }

    /**
     * Convert every paginator of {@code paginators} into the traits to add to its operation.
     *
     * @return the traits per operation ShapeId, in paginator file order
     */
    static Map<ShapeId, List<Trait>> convert(JsonNode paginators, Model model, Function<String, ShapeId> operationIds,
                                             ConversionGapReport.Builder gaps) {
        Map<ShapeId, List<Trait>> result = new LinkedHashMap<>();
        if (paginators == null) {
            return result;
        }
        PaginatorConversion conversion = new PaginatorConversion(model, gaps);
        for (Map.Entry<String, JsonNode> e : paginators.path("pagination").properties()) {
            ShapeId operationId = operationIds.apply(e.getKey());
            List<Trait> traits = conversion.convertOne(operationId, e.getValue());
            if (!traits.isEmpty()) {
                result.put(operationId, traits);
            }
        }
        return result;
    }

    private List<Trait> convertOne(ShapeId operationId, JsonNode c2j) {
        OperationShape operation = model.getShape(operationId).flatMap(Shape::asOperationShape).orElse(null);
        if (operation == null) {
            gaps.record("paginator.operation(missing)", "A paginator names an operation the service model does not "
                                                        + "have. " + NOT_CONVERTED);
            return Collections.emptyList();
        }
        if (c2j.has("more_results")) {
            gaps.record("paginator.more_results", "@paginated has no more_results member. Pagination ends when the "
                                                  + "output token is absent, not when the more_results member is "
                                                  + "false.");
        }
        if (c2j.has("non_aggregate_keys")) {
            gaps.record("paginator.non_aggregate_keys", "@paginated has no non_aggregate_keys; the members are not "
                                                        + "identified as non-aggregated.");
        }

        String inputToken = single(c2j.path("input_token"), "input_token");
        String outputToken = single(c2j.path("output_token"), "output_token");
        if (inputToken == null || outputToken == null) {
            return Collections.emptyList();
        }

        Set<String> suppressions = new TreeSet<>();
        StructureShape input = structure(operation.getInputShape());
        StructureShape output = structure(operation.getOutputShape());

        MemberShape inputMember = input == null ? null : input.getMember(inputToken).orElse(null);
        if (!tokenMember(inputMember, false, suppressions)) {
            unresolvable("input_token");
            return Collections.emptyList();
        }
        MemberShape outputMember = resolvePath(output, outputToken);
        if (!tokenMember(outputMember, true, suppressions)) {
            unresolvable("output_token");
            return Collections.emptyList();
        }

        PaginatedTrait.Builder paginated = PaginatedTrait.builder().inputToken(inputToken).outputToken(outputToken);
        if (c2j.hasNonNull("limit_key")) {
            String pageSize = c2j.get("limit_key").asText();
            MemberShape pageSizeMember = input.getMember(pageSize).orElse(null);
            ShapeType type = targetType(pageSizeMember);
            if (type == null || !PAGE_SIZE_TARGETS.contains(type)) {
                // For example Route 53, whose MaxItems members are strings.
                gaps.record("paginator.limit_key(unresolvable)", "The limit_key is not a top-level input member that "
                                                                 + "targets an integral shape, so @paginated has no "
                                                                 + "pageSize.");
            } else {
                if (type != ShapeType.INTEGER) {
                    suppressions.add(WRONG_SHAPE_TYPE);
                }
                paginated.pageSize(pageSize);
            }
        }

        JsonNode resultKey = c2j.path("result_key");
        if (resultKey.isArray() && resultKey.size() > 1) {
            gaps.record("paginator.result_key(multiple)", "@paginated has one items member, so a paginator with "
                                                          + "several result keys gets no items and no per-result-key "
                                                          + "iterables are generated from standard traits.");
        } else if (!resultKey.isMissingNode()) {
            String items = resultKey.isArray() ? resultKey.get(0).asText() : resultKey.asText();
            ShapeType type = targetType(resolvePath(output, items));
            if (type != null && ITEM_TARGETS.contains(type)) {
                paginated.items(items);
            } else {
                gaps.record("paginator.result_key(unresolvable)", "The result_key is not a path to a list or map "
                                                                  + "output member, so @paginated has no items.");
            }
        }

        List<Trait> traits = new ArrayList<>(2);
        traits.add(paginated.build());
        if (!suppressions.isEmpty()) {
            traits.add(SuppressTrait.builder().values(new ArrayList<>(suppressions)).build());
        }
        return traits;
    }

    // A token member must exist and target a string or map. The input token must not be required (an ERROR in Smithy);
    // a required output token and a map target are DANGER events that are suppressed.
    private boolean tokenMember(MemberShape member, boolean output, Set<String> suppressions) {
        ShapeType type = targetType(member);
        if (type == null || !TOKEN_TARGETS.contains(type)) {
            return false;
        }
        if (member.isRequired()) {
            if (!output) {
                return false;
            }
            suppressions.add(SHOULD_NOT_BE_REQUIRED);
        }
        if (type == ShapeType.MAP) {
            suppressions.add(WRONG_SHAPE_TYPE);
        }
        return true;
    }

    private String single(JsonNode token, String property) {
        if (token.isTextual()) {
            return token.asText();
        }
        if (token.isArray() && token.size() == 1) {
            return token.get(0).asText();
        }
        if (token.isArray() && token.size() > 1) {
            gaps.record("paginator." + property + "(multiple)", "@paginated has one " + property + "; a paginator "
                                                                 + "with several is not converted. " + NOT_CONVERTED);
        } else {
            gaps.record("paginator." + property + "(missing)", "A paginator without an " + property + " cannot be "
                                                                + "expressed with @paginated. " + NOT_CONVERTED);
        }
        return null;
    }

    private void unresolvable(String property) {
        gaps.record("paginator." + property + "(unresolvable)",
                    "The " + property + " does not name a member that Smithy accepts for it (missing, wrong target "
                    + "type, a path for an input member, or a required input token). " + NOT_CONVERTED);
    }

    private StructureShape structure(ShapeId id) {
        return model.getShape(id).flatMap(Shape::asStructureShape).orElse(null);
    }

    private ShapeType targetType(MemberShape member) {
        if (member == null) {
            return null;
        }
        return model.getShape(member.getTarget()).map(Shape::getType).orElse(null);
    }

    // Walk a dotted member path (outputToken and items allow paths) and return the last member.
    private MemberShape resolvePath(StructureShape root, String path) {
        StructureShape current = root;
        MemberShape member = null;
        for (String part : Arrays.asList(path.split("\\.", -1))) {
            if (current == null) {
                return null;
            }
            member = current.getMember(part).orElse(null);
            if (member == null) {
                return null;
            }
            current = structure(member.getTarget());
        }
        return member;
    }
}
