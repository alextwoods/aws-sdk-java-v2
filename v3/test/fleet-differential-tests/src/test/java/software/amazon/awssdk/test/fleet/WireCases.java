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

package software.amazon.awssdk.test.fleet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * The deterministic case selection of the wire differential (documented in {@code wire-differential/README.md}).
 *
 * <p>Services: the fixed set {@link #V2_SERVICES}, which covers every launch protocol (awsJson1_0, awsJson1_1,
 * restJson1, restXml, awsQuery, ec2Query, rpcv2Cbor) and whose released V2 jars are the frozen baseline.
 *
 * <p>Request cases: for each service and each binding category its protocol has ({@link Category#restCategories} for
 * HTTP-binding protocols, {@link Category#BODY} for RPC protocols), the first operation in shape-ID order whose input
 * has a <em>simple</em> member of that category and whose required input members are all simple (string, enum,
 * boolean, integer, long, timestamp; a streaming blob payload for {@link Category#STREAMING}) and which has no event
 * stream. Its request sets exactly the required members and the first simple member of the category, with the
 * deterministic {@link #value values}. Response cases: for each service, the first operation in shape-ID order that
 * selected a request case and has a simple output member of the binding its protocol reads (a header for HTTP-binding
 * protocols, the body otherwise) answers with that member (SUCCESS), with its first modeled error in shape-ID order
 * (MODELED_ERROR) and with an error code no model declares (UNMODELED_ERROR).
 */
final class WireCases {
    /** The fleet services of the differential (their V2 jars are copied into target/v2-baseline by the POM). */
    static final List<String> V2_SERVICES = List.of("cloudwatch", "cognitoidentityprovider", "dynamodb", "ec2",
                                                    "kinesis", "lambda", "pinpoint", "s3", "sns", "sts");

    static final String UNMODELED_ERROR_CODE = "FleetDifferentialUnmodeledError";

    /** The binding category a request case exercises. */
    enum Category {
        LABEL, QUERY, HEADER, PAYLOAD, STREAMING, BODY, SUCCESS, MODELED_ERROR, UNMODELED_ERROR;

        static List<Category> restCategories() {
            return List.of(LABEL, QUERY, HEADER, PAYLOAD, STREAMING);
        }
    }

    /**
     * One differential case.
     *
     * @param service the fleet service
     * @param operation the operation
     * @param category the binding category
     * @param input the request members (member name to value), in modeled order
     * @param outputMember the output member the success response carries, if any
     * @param error the modeled error shape of a {@code MODELED_ERROR} case
     */
    record Case(Fleet.Service service, OperationShape operation, Category category, Map<String, Object> input,
                Optional<MemberShape> outputMember, Optional<ShapeId> error) {
        String id() {
            return service.name() + "/" + operation.getId().getName() + "/" + category;
        }

        @Override
        public String toString() {
            return id();
        }
    }

    private WireCases() {
    }

    /** Every case of {@code service}, in category order. */
    static List<Case> select(Fleet.Service service, Model model) {
        ServiceShape shape = model.expectShape(service.id(), ServiceShape.class);
        boolean rest = shape.hasTrait("aws.protocols#restJson1") || shape.hasTrait("aws.protocols#restXml");
        List<OperationShape> operations = new ArrayList<>(new TreeSet<>(TopDownIndex.of(model).getContainedOperations(shape)));
        List<Case> cases = new ArrayList<>();
        Case firstRequestCase = null;
        for (Category category : rest ? Category.restCategories() : List.of(Category.BODY)) {
            for (OperationShape operation : operations) {
                Optional<Map<String, Object>> input = input(model, operation, category, rest);
                if (input.isPresent()) {
                    Case c = new Case(service, operation, category, input.get(), Optional.empty(), Optional.empty());
                    cases.add(c);
                    break;
                }
            }
        }
        // The response cases: the first selected operation (shape-ID order) with a simple output member its protocol
        // reads from the binding this differential answers with.
        List<OperationShape> selected = cases.stream().map(Case::operation).distinct().sorted().toList();
        for (OperationShape operation : selected) {
            Optional<MemberShape> output = outputMember(model, operation, rest);
            if (output.isEmpty() || operation.getErrors().isEmpty() && shape.getErrors().isEmpty()) {
                continue;
            }
            Case base = cases.stream().filter(c -> c.operation().equals(operation)).findFirst().orElseThrow();
            TreeSet<ShapeId> errors = new TreeSet<>(operation.getErrors());
            errors.addAll(shape.getErrors());
            cases.add(new Case(service, operation, Category.SUCCESS, base.input(), output, Optional.empty()));
            cases.add(new Case(service, operation, Category.MODELED_ERROR, base.input(), Optional.empty(),
                               Optional.of(errors.first())));
            cases.add(new Case(service, operation, Category.UNMODELED_ERROR, base.input(), Optional.empty(),
                               Optional.empty()));
            break;
        }
        return cases;
    }

    private static Optional<Map<String, Object>> input(Model model, OperationShape operation, Category category,
                                                      boolean rest) {
        StructureShape input = model.expectShape(operation.getInputShape(), StructureShape.class);
        if (hasEventStream(model, operation)) {
            return Optional.empty();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        MemberShape chosen = null;
        for (MemberShape member : input.members()) {
            if (chosen == null && category(model, member, rest) == category && simple(model, member, category)) {
                chosen = member;
            }
        }
        if (chosen == null) {
            return Optional.empty();
        }
        for (MemberShape member : input.members()) {
            boolean required = member.hasTrait(RequiredTrait.class) && !member.hasTrait(IdempotencyTokenTrait.class);
            if (member.equals(chosen) || required) {
                if (!simple(model, member, category(model, member, rest))) {
                    return Optional.empty();
                }
                values.put(member.getMemberName(), value(model, member));
            }
        }
        return Optional.of(values);
    }

    private static Optional<MemberShape> outputMember(Model model, OperationShape operation, boolean rest) {
        StructureShape output = model.expectShape(operation.getOutputShape(), StructureShape.class);
        if (hasEventStream(model, operation)) {
            return Optional.empty();
        }
        for (MemberShape member : output.members()) {
            Shape target = model.expectShape(member.getTarget());
            boolean simpleType = target.getType() == ShapeType.STRING && !target.hasTrait(EnumTrait.class)
                                 || target.getType() == ShapeType.INTEGER || target.getType() == ShapeType.LONG;
            boolean binding = rest ? member.hasTrait(HttpHeaderTrait.class)
                                   : !member.hasTrait(HttpResponseCodeTrait.class);
            if (simpleType && binding) {
                return Optional.of(member);
            }
        }
        return Optional.empty();
    }

    private static boolean hasEventStream(Model model, OperationShape operation) {
        for (ShapeId io : List.of(operation.getInputShape(), operation.getOutputShape())) {
            for (MemberShape member : model.expectShape(io).members()) {
                if (StreamingTrait.isEventStream(model.expectShape(member.getTarget()))) {
                    return true;
                }
            }
        }
        return false;
    }

    static Category category(Model model, MemberShape member, boolean rest) {
        if (member.hasTrait(HttpLabelTrait.class)) {
            return Category.LABEL;
        }
        if (member.hasTrait(HttpQueryTrait.class) || member.hasTrait(HttpQueryParamsTrait.class)) {
            return Category.QUERY;
        }
        if (member.hasTrait(HttpHeaderTrait.class) || member.hasTrait(HttpPrefixHeadersTrait.class)) {
            return Category.HEADER;
        }
        if (member.hasTrait(HttpPayloadTrait.class)
            && model.expectShape(member.getTarget()).hasTrait(StreamingTrait.class)) {
            return Category.STREAMING;
        }
        // An unbound member (or a non-streaming @httpPayload) of an HTTP-binding protocol is in the payload; every
        // member of an RPC protocol is in the body.
        return rest ? Category.PAYLOAD : Category.BODY;
    }

    private static boolean simple(Model model, MemberShape member, Category category) {
        Shape target = model.expectShape(member.getTarget());
        if (category == Category.STREAMING) {
            return target.getType() == ShapeType.BLOB && target.hasTrait(StreamingTrait.class);
        }
        if (target.hasTrait(StreamingTrait.class)) {
            return false;
        }
        return switch (target.getType()) {
            case STRING, ENUM, BOOLEAN, INTEGER, LONG, TIMESTAMP -> true;
            default -> false;
        };
    }

    /**
     * The deterministic value of a simple member: {@code fd-<memberName>} for strings (lower-cased, as labels and host
     * prefixes must be valid host labels), the first enum value, {@code true}, {@code 7}, and the instant
     * {@code 2024-01-02T03:04:05Z}. A streaming payload carries the text {@code fleet-differential-body}.
     */
    static Object value(Model model, MemberShape member) {
        Shape target = model.expectShape(member.getTarget());
        return switch (target.getType()) {
            case STRING -> target.getTrait(EnumTrait.class)
                                 .map(e -> (Object) e.getValues().get(0).getValue())
                                 .orElse("fd-" + member.getMemberName().toLowerCase(java.util.Locale.ROOT));
            case ENUM -> target.asEnumShape().orElseThrow().getEnumValues().values().iterator().next();
            case BOOLEAN -> Boolean.TRUE;
            case INTEGER -> 7;
            case LONG -> 7L;
            case TIMESTAMP -> java.time.Instant.parse("2024-01-02T03:04:05Z");
            case BLOB -> "fleet-differential-body";
            default -> throw new IllegalArgumentException("Not a simple member: " + member.getId());
        };
    }
}
