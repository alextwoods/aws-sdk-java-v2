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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.smithy.runtime.registry.RegisteredType;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait;
import software.amazon.smithy.model.traits.HttpBasicAuthTrait;
import software.amazon.smithy.model.traits.HttpBearerAuthTrait;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.model.traits.HttpDigestAuthTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * The schema-fidelity oracle of one generated fleet service (Requirement 19.3): the expected schema inventory is built
 * directly from the canonical Smithy model, and every generated view of the service's compiled jar is compared with
 * it. It is the fleet counterpart of Property 1 ({@code test/codegen-tests ModelSchemaBijectionPropertyTest}), which
 * checks the same oracle over random models; the two must stay in step.
 *
 * <p><b>Expected inventory.</b> The service closure ({@link Walker}) less members, resources and {@code smithy.api}
 * prelude shapes, classified as the service, operations, public types (structures, including inputs, outputs and
 * errors, unions, enums, intEnums) and other member targets (lists, maps and simple shapes). Each shape carries its
 * exact ID and type, its members in modeled order with exact target IDs and target types, and, for the shape and each
 * member (including enum and intEnum members), the node values of its runtime-relevant traits ({@link #RUNTIME_TRAITS},
 * pinned to {@code aws-codegen}'s own table by {@code FleetSchemaFidelityTest}) and of its unknown traits
 * ({@link DynamicTrait}s). An operation's expected errors are its own and the service's (aws-codegen applies Smithy's
 * default codegen transforms, which copy service errors to operations).
 *
 * <p><b>Views compared</b> (each mismatch is one problem):
 * <ol>
 *   <li>the exported {@code public static} {@code Schema} constants of {@code <Service>Schemas}: exactly one distinct,
 *       non-member constant per service, operation and public type;</li>
 *   <li>the generated {@link ServiceSchemaIndex}: exactly the expected service, operations, types and other shapes
 *       (exported ones as the same objects); for every expected shape exactly one schema with its exact ID, shape type,
 *       exact runtime traits (nothing missing or extra, each also found through {@link Schema#getDirectTrait} with the
 *       canonical class), unknown traits, members in modeled order with exact member IDs, target IDs, target types and
 *       traits, enum values, and each member target the one indexed schema object of that ID (the exported constant of
 *       a type), so recursive links are exact;</li>
 *   <li>the model type classes of {@code <package>.model}: exactly one per public type, whose {@code SCHEMA} is the
 *       exported constant and whose {@code ID} is the shape ID, plus the service's base exception, response and
 *       response metadata;</li>
 *   <li>the primary {@link SchemaTypeRegistry}: exactly the structures and unions, each to its exported schema, model
 *       class and a builder of that schema;</li>
 *   <li>the operation descriptors: each operation's schemas are the exported operation, input, output and error
 *       constants.</li>
 * </ol>
 */
final class SchemaFidelity {

    /** The runtime-relevant prelude traits generated schemas carry (see the class documentation). */
    static final List<Class<? extends Trait>> RUNTIME_TRAITS = List.of(
        RequiredTrait.class, DefaultTrait.class, LengthTrait.class, PatternTrait.class, RangeTrait.class,
        UniqueItemsTrait.class, SparseTrait.class, RequiresLengthTrait.class, EnumValueTrait.class, EnumTrait.class,
        ErrorTrait.class, HttpErrorTrait.class, RetryableTrait.class,
        JsonNameTrait.class, TimestampFormatTrait.class, MediaTypeTrait.class, XmlNameTrait.class,
        XmlAttributeTrait.class, XmlFlattenedTrait.class, XmlNamespaceTrait.class, EventHeaderTrait.class,
        EventPayloadTrait.class, StreamingTrait.class,
        HttpTrait.class, HttpHeaderTrait.class, HttpLabelTrait.class, HttpQueryTrait.class, HttpQueryParamsTrait.class,
        HttpPayloadTrait.class, HttpPrefixHeadersTrait.class, HttpResponseCodeTrait.class,
        HttpChecksumRequiredTrait.class, EndpointTrait.class, HostLabelTrait.class,
        IdempotencyTokenTrait.class, IdempotentTrait.class, ReadonlyTrait.class, PaginatedTrait.class,
        RequestCompressionTrait.class, SensitiveTrait.class, UnitTypeTrait.class,
        AuthTrait.class, OptionalAuthTrait.class, HttpBasicAuthTrait.class, HttpBearerAuthTrait.class,
        HttpDigestAuthTrait.class, HttpApiKeyAuthTrait.class);

    static final SortedSet<ShapeId> RUNTIME_TRAIT_IDS = RUNTIME_TRAITS.stream().map(SchemaFidelity::traitId)
                                                                      .collect(Collectors.toCollection(TreeSet::new));

    private static final String PRELUDE = "smithy.api";
    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    private SchemaFidelity() {
    }

    /**
     * Compares every generated view of {@code service}, loaded by {@code loader}, with the canonical {@code model}.
     *
     * @return the result: counts and every problem found
     */
    static Result check(Fleet.Service service, Model model, Fleet.ServiceClassLoader loader) {
        Oracle oracle = Oracle.of(model, service.id());
        Views views = new Views(oracle, service, loader);
        try {
            views.check();
        } catch (RuntimeException | LinkageError | AssertionError e) {
            // A defect the runtime itself rejects on first use (for example an inconsistent index).
            List<String> causes = new ArrayList<>();
            for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
                causes.add(String.valueOf(t));
            }
            views.problems.add("loading the generated views failed: " + String.join(" caused by ", causes));
        }
        return new Result(service, oracle.shapes().size(), oracle.ids(Kind.OPERATION).size(),
                          oracle.ids(Kind.TYPE).size(), views.comparedSchemas, views.comparedMembers,
                          List.copyOf(views.problems));
    }

    /**
     * The outcome for one service.
     *
     * @param service the service
     * @param shapes the expected (non-member, non-prelude) shapes
     * @param operations the expected operations
     * @param types the expected public types
     * @param schemas the schemas compared with their canonical shape
     * @param members the member schemas compared with their canonical member
     * @param problems every mismatch, in discovery order
     */
    record Result(Fleet.Service service, int shapes, int operations, int types, int schemas, int members,
                  List<String> problems) {
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Oracle
    // ---------------------------------------------------------------------------------------------------------------

    /** How the generator exposes the schema of a shape. */
    enum Kind {
        SERVICE, OPERATION, TYPE, SHAPE
    }

    record ExpectedMember(ShapeId id, ShapeId target, ShapeType targetType, SortedMap<ShapeId, Trait> traits,
                          SortedMap<ShapeId, Node> unknown) {
    }

    record ExpectedShape(ShapeId id, ShapeType type, Kind kind, List<ExpectedMember> members,
                         SortedMap<ShapeId, Trait> traits, SortedMap<ShapeId, Node> unknown) {
    }

    record ExpectedLinks(ShapeId input, ShapeId output, List<ShapeId> errors) {
    }

    /** The expected inventory, built directly from the canonical model. */
    record Oracle(ShapeId service, SortedMap<ShapeId, ExpectedShape> shapes, SortedMap<ShapeId, ExpectedLinks> links) {

        static Oracle of(Model model, ShapeId serviceId) {
            ServiceShape service = model.expectShape(serviceId, ServiceShape.class);
            SortedMap<ShapeId, ExpectedShape> shapes = new TreeMap<>();
            for (Shape shape : new Walker(model).walkShapes(service)) {
                if (shape.isMemberShape() || shape.isResourceShape() || prelude(shape.getId())) {
                    continue;
                }
                shapes.put(shape.getId(), expected(model, shape));
            }
            SortedMap<ShapeId, ExpectedLinks> links = new TreeMap<>();
            for (ExpectedShape shape : shapes.values()) {
                if (shape.kind() != Kind.OPERATION) {
                    continue;
                }
                OperationShape operation = model.expectShape(shape.id(), OperationShape.class);
                Set<ShapeId> errors = new TreeSet<>(operation.getErrors());
                errors.addAll(service.getErrors());
                links.put(operation.getId(), new ExpectedLinks(operation.getInputShape(), operation.getOutputShape(),
                                                               List.copyOf(errors)));
            }
            return new Oracle(serviceId, shapes, links);
        }

        private static ExpectedShape expected(Model model, Shape shape) {
            Kind kind = switch (shape.getType()) {
                case SERVICE -> Kind.SERVICE;
                case OPERATION -> Kind.OPERATION;
                case STRUCTURE, UNION, ENUM, INT_ENUM -> Kind.TYPE;
                default -> Kind.SHAPE;
            };
            List<ExpectedMember> members = new ArrayList<>();
            for (MemberShape member : shape.members()) {
                members.add(new ExpectedMember(member.getId(), member.getTarget(),
                                               model.expectShape(member.getTarget()).getType(),
                                               runtimeTraits(member), unknownTraits(member)));
            }
            return new ExpectedShape(shape.getId(), shape.getType(), kind, List.copyOf(members), runtimeTraits(shape),
                                     unknownTraits(shape));
        }

        private static SortedMap<ShapeId, Trait> runtimeTraits(Shape shape) {
            SortedMap<ShapeId, Trait> result = new TreeMap<>();
            shape.getAllTraits().forEach((id, trait) -> {
                if (RUNTIME_TRAIT_IDS.contains(id)) {
                    result.put(id, trait);
                }
            });
            return result;
        }

        private static SortedMap<ShapeId, Node> unknownTraits(Shape shape) {
            SortedMap<ShapeId, Node> result = new TreeMap<>();
            shape.getAllTraits().forEach((id, trait) -> {
                if (trait instanceof DynamicTrait) {
                    result.put(id, trait.toNode());
                }
            });
            return result;
        }

        SortedSet<ShapeId> ids(Kind kind) {
            return shapes.values().stream().filter(s -> s.kind() == kind).map(ExpectedShape::id)
                         .collect(Collectors.toCollection(TreeSet::new));
        }

        SortedSet<ShapeId> unknownTargets() {
            SortedSet<ShapeId> result = new TreeSet<>();
            for (ExpectedShape shape : shapes.values()) {
                if (!shape.unknown().isEmpty()) {
                    result.add(shape.id());
                }
                shape.members().stream().filter(m -> !m.unknown().isEmpty()).forEach(m -> result.add(m.id()));
            }
            return result;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generated views
    // ---------------------------------------------------------------------------------------------------------------

    /** The generated views of one service, compared with the oracle; every mismatch is recorded in problems. */
    static final class Views {
        final List<String> problems = new ArrayList<>();
        int comparedSchemas;
        int comparedMembers;

        private final Oracle oracle;
        private final Fleet.Service service;
        private final Fleet.ServiceClassLoader loader;
        private final String serviceName;
        private final Map<ShapeId, Schema> exported = new TreeMap<>();
        private final Map<ShapeId, Class<?>> types = new TreeMap<>();

        Views(Oracle oracle, Fleet.Service service, Fleet.ServiceClassLoader loader) {
            this.oracle = oracle;
            this.service = service;
            this.loader = loader;
            this.serviceName = typeName(service, loader);
        }

        /**
         * The generated service type name (V2's naming of the SDK ID, for example {@code DynamoDb}): the one
         * {@code <Name>Schemas} holder of {@code <package>.model.internal} in the jar. Every other generated name
         * this oracle reads ({@code <Name>TypeRegistry}, {@code <Name>Operations}, {@code <Name>Exception},
         * {@code <Name>Response}) must use the same name.
         */
        private static String typeName(Fleet.Service service, Fleet.ServiceClassLoader loader) {
            String prefix = service.modelPackage() + ".internal.";
            List<String> holders = loader.classNames().stream()
                                         .filter(n -> n.startsWith(prefix) && n.endsWith("Schemas"))
                                         .filter(n -> n.indexOf('.', prefix.length()) < 0 && !n.contains("$"))
                                         .toList();
            if (holders.size() != 1) {
                throw new AssertionError("Expected exactly one " + prefix + "<Name>Schemas holder, found " + holders);
            }
            String holder = holders.get(0);
            return holder.substring(prefix.length(), holder.length() - "Schemas".length());
        }

        private String schemasHolder() {
            return service.modelPackage() + ".internal." + serviceName + "Schemas";
        }

        void check() {
            exportedConstants();
            ServiceSchemaIndex index = (ServiceSchemaIndex) invokeStatic(schemasHolder(), "index");
            indexInventory(index);
            for (ExpectedShape shape : oracle.shapes().values()) {
                index.schema(shape.id()).ifPresentOrElse(schema -> fidelity(index, shape, schema),
                                                         () -> problems.add("no schema for " + shape.id()));
            }
            same("unknown-trait targets", oracle.unknownTargets(), new TreeSet<>(index.unknownTraits().keySet()));
            modelTypes();
            registry();
            operations();
        }

        private void exportedConstants() {
            Map<Schema, String> names = new IdentityHashMap<>();
            for (Field field : loader.load(schemasHolder()).getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !Schema.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                Schema schema = (Schema) get(field, null);
                String previous = names.put(schema, field.getName());
                if (previous != null) {
                    problems.add("constants " + previous + " and " + field.getName() + " export the same object");
                } else if (schema.isMember()) {
                    problems.add("constant " + field.getName() + " exports member schema " + schema.id());
                } else if (exported.put(schema.id(), schema) != null) {
                    problems.add("two exported constants for " + schema.id());
                }
            }
            SortedSet<ShapeId> expected = new TreeSet<>(oracle.ids(Kind.SERVICE));
            expected.addAll(oracle.ids(Kind.OPERATION));
            expected.addAll(oracle.ids(Kind.TYPE));
            same("exported schema constants", expected, exported.keySet());
        }

        private void indexInventory(ServiceSchemaIndex index) {
            identical("index service", exported.get(oracle.service()), index.service());
            inventory("index operations", Kind.OPERATION, index.operations(), true);
            inventory("index types", Kind.TYPE, index.types(), true);
            inventory("index shapes", Kind.SHAPE, index.shapes(), false);
        }

        private void inventory(String what, Kind kind, List<Schema> schemas, boolean exportedKind) {
            SortedSet<ShapeId> ids = new TreeSet<>();
            for (Schema schema : schemas) {
                if (!ids.add(schema.id())) {
                    problems.add(what + ": " + schema.id() + " is listed twice");
                }
                if (exportedKind) {
                    identical(what + " " + schema.id(), exported.get(schema.id()), schema);
                }
            }
            same(what, oracle.ids(kind), ids);
        }

        private void fidelity(ServiceSchemaIndex index, ExpectedShape shape, Schema schema) {
            comparedSchemas++;
            String where = shape.id().toString();
            if (!schema.id().equals(shape.id()) || schema.isMember()) {
                problems.add(where + ": indexed schema is " + schema.id() + (schema.isMember() ? " (a member)" : ""));
                return;
            }
            if (shape.kind() != Kind.SHAPE) {
                identical(where + " exported constant", exported.get(shape.id()), schema);
            }
            if (schema.type() != shape.type()) {
                problems.add(where + ": type " + schema.type() + ", expected " + shape.type());
            }
            traits(where, schema, shape.traits());
            unknown(where, index, shape.id(), shape.unknown());

            List<Schema> ordered = index.members(schema);
            List<String> expectedNames = shape.members().stream().map(m -> m.id().getMember().orElseThrow()).toList();
            List<String> orderedNames = ordered.stream().map(Schema::memberName).toList();
            if (!expectedNames.equals(orderedNames)) {
                problems.add(where + ": members in order " + orderedNames + ", expected " + expectedNames);
                return;
            }
            switch (shape.type()) {
                case ENUM -> same(where + " enum values", enumValues(shape, false),
                                  new TreeSet<>(schema.stringEnumValues()));
                case INT_ENUM -> same(where + " intEnum values", enumValues(shape, true), new TreeSet<>(
                    schema.intEnumValues().stream().map(String::valueOf).toList()));
                default -> {
                    List<String> declared = schema.members().stream().map(Schema::memberName).toList();
                    if (!new TreeSet<>(declared).equals(new TreeSet<>(expectedNames))
                        || declared.size() != expectedNames.size()) {
                        problems.add(where + ": Schema.members() " + declared + ", expected " + expectedNames);
                        return;
                    }
                    for (Schema member : ordered) {
                        identical(where + " member " + member.memberName(), schema.member(member.memberName()),
                                  member);
                    }
                    if (shape.type() == ShapeType.LIST) {
                        identical(where + " list member", ordered.get(0), schema.listMember());
                    } else if (shape.type() == ShapeType.MAP) {
                        identical(where + " map key", ordered.get(0), schema.mapKeyMember());
                        identical(where + " map value", ordered.get(1), schema.mapValueMember());
                    }
                }
            }
            for (int i = 0; i < ordered.size(); i++) {
                member(index, shape.members().get(i), ordered.get(i));
            }
        }

        private void member(ServiceSchemaIndex index, ExpectedMember expected, Schema member) {
            comparedMembers++;
            String where = expected.id().toString();
            if (!member.isMember() || !member.id().equals(expected.id())) {
                problems.add(where + ": member schema is " + member.id());
                return;
            }
            Schema target = member.memberTarget();
            if (!target.id().equals(expected.target())) {
                problems.add(where + ": targets " + target.id() + ", expected " + expected.target());
            } else if (target.type() != expected.targetType()) {
                problems.add(where + ": target type " + target.type() + ", expected " + expected.targetType());
            } else if (!prelude(expected.target())) {
                identical(where + " target", index.schema(expected.target()).orElse(null), target);
                if (oracle.shapes().get(expected.target()).kind() == Kind.TYPE) {
                    identical(where + " target constant", exported.get(expected.target()), target);
                }
            }
            traits(where, member, expected.traits());
            unknown(where, index, expected.id(), expected.unknown());
        }

        private static SortedSet<String> enumValues(ExpectedShape shape, boolean intEnum) {
            SortedSet<String> result = new TreeSet<>();
            for (ExpectedMember member : shape.members()) {
                EnumValueTrait value = (EnumValueTrait) member.traits().get(EnumValueTrait.ID);
                result.add(intEnum ? String.valueOf(value.expectIntValue()) : value.expectStringValue());
            }
            return result;
        }

        private void traits(String where, Schema schema, SortedMap<ShapeId, Trait> expected) {
            SortedMap<ShapeId, Node> actual = new TreeMap<>();
            for (Trait trait : directTraits(schema)) {
                if (actual.put(trait.toShapeId(), trait.toNode()) != null) {
                    problems.add(where + ": two schema traits have the ID " + trait.toShapeId());
                }
            }
            SortedMap<ShapeId, Node> expectedNodes = new TreeMap<>();
            expected.forEach((id, trait) -> expectedNodes.put(id, trait.toNode()));
            if (!expectedNodes.equals(actual)) {
                problems.add(where + ": schema traits " + render(actual) + ", expected " + render(expectedNodes));
                return;
            }
            for (Trait trait : expected.values()) {
                Trait direct = schema.getDirectTrait(TraitKey.get(trait.getClass()));
                if (direct == null || direct.getClass() != trait.getClass()) {
                    problems.add(where + ": getDirectTrait(" + trait.getClass().getSimpleName() + ") is " + direct);
                }
            }
            if (schema.getDirectTrait(TraitKey.get(DynamicTrait.class)) != null) {
                problems.add(where + ": carries an unknown trait by Java class");
            }
        }

        private void unknown(String where, ServiceSchemaIndex index, ShapeId target,
                             SortedMap<ShapeId, Node> expected) {
            SortedMap<ShapeId, Node> actual = new TreeMap<>();
            index.unknownTraits(target).asMap().forEach((id, trait) -> actual.put(id, trait.toNode()));
            if (!expected.equals(actual)) {
                problems.add(where + ": unknown traits " + render(actual) + ", expected " + render(expected));
            }
        }

        /** View (3): exactly one model type class per public type, holding the exported schema. */
        private void modelTypes() {
            String prefix = service.modelPackage() + ".";
            int serviceExceptions = 0;
            int serviceResponses = 0;
            for (String className : loader.classNames()) {
                if (!className.startsWith(prefix) || className.indexOf('.', prefix.length()) >= 0
                    || className.contains("$")) {
                    continue;
                }
                Class<?> type = loader.load(className);
                Field schemaField = declaredField(type, "SCHEMA");
                if (schemaField == null) {
                    if (type.getSimpleName().equals(serviceName + "Exception")
                        && type.getSuperclass() == AwsServiceException.class) {
                        serviceExceptions++;
                    } else if (type.getSimpleName().equals(serviceName + "Response")
                               && type.getSuperclass() == AwsResponse.class
                               || type.getSimpleName().equals(serviceName + "ResponseMetadata")
                                  && type.getSuperclass() == AwsResponseMetadata.class) {
                        serviceResponses++;
                    } else if (type.isInterface() && EventStreamResponseHandler.class.isAssignableFrom(type)) {
                        // The event-stream response handler of a streaming-output operation (no schema of its own).
                        continue;
                    } else if (!oracle.ids(Kind.TYPE).stream().map(ShapeId::getName)
                                      .anyMatch(n -> n.equals(type.getSimpleName())
                                                     || (n + "Exception").equals(type.getSimpleName()))) {
                        // A hand-written service customization in the model package that models no shape (the
                        // CloudFront signer requests, RDS and DSQL auth-token requests, S3 GetUrlRequest, as in V2);
                        // a generated type without its schema is still caught by "model type classes" below.
                        continue;
                    } else {
                        problems.add("model class " + className + " has no SCHEMA and is not the service exception, "
                                     + "response or response metadata");
                    }
                    continue;
                }
                Schema schema = (Schema) get(schemaField, null);
                identical("SCHEMA of " + className, exported.get(schema.id()), schema);
                Field idField = declaredField(type, "ID");
                Object id = idField == null ? null : get(idField, null);
                if (!schema.id().equals(id)) {
                    problems.add("ID of " + className + " is " + id + ", its SCHEMA is " + schema.id());
                }
                Class<?> previous = types.put(schema.id(), type);
                if (previous != null) {
                    problems.add("model classes " + previous.getName() + " and " + className + " both hold "
                                 + schema.id());
                }
            }
            same("model type classes", oracle.ids(Kind.TYPE), types.keySet());
            if (serviceExceptions != 1) {
                problems.add(serviceExceptions + " service base exceptions, expected 1");
            }
            if (serviceResponses != 2) {
                problems.add(serviceResponses + " service base response and metadata classes, expected 2");
            }
        }

        /** View (4): the registry maps exactly the structures and unions to their schema, class and builder. */
        private void registry() {
            SchemaTypeRegistry registry = (SchemaTypeRegistry) invokeStatic(
                service.modelPackage() + ".internal." + serviceName + "TypeRegistry", "registry");
            SortedSet<ShapeId> expected = oracle.shapes().values().stream()
                                                .filter(s -> s.kind() == Kind.TYPE)
                                                .filter(s -> s.type() == ShapeType.STRUCTURE
                                                             || s.type() == ShapeType.UNION)
                                                .map(ExpectedShape::id)
                                                .collect(Collectors.toCollection(TreeSet::new));
            same("registry shape IDs", expected, registry.shapeIds());
            for (RegisteredType<?> entry : registry.entries()) {
                ShapeId id = entry.shapeId();
                identical("registry schema of " + id, exported.get(id), entry.schema());
                identical("registry schema lookup of " + id, exported.get(id), registry.schema(id).orElse(null));
                if (entry.type() != types.get(id) || registry.getShapeClass(id) != types.get(id)) {
                    problems.add("registry class of " + id + " is " + entry.type() + ", expected " + types.get(id));
                }
                identical("registry builder schema of " + id, exported.get(id), entry.createBuilder().schema());
            }
        }

        /** View (5): each operation descriptor links the exported operation, input, output and error schemas. */
        private void operations() {
            List<?> descriptors = (List<?>) invokeStatic(
                service.packageName() + ".internal.operation." + serviceName + "Operations", "operations");
            SortedSet<ShapeId> ids = new TreeSet<>();
            for (Object object : descriptors) {
                AwsOperationDescriptor descriptor = (AwsOperationDescriptor) object;
                ShapeId id = descriptor.id();
                ids.add(id);
                ExpectedLinks links = oracle.links().get(id);
                OperationSchemas schemas = descriptor.schemas().orElse(null);
                if (links == null || schemas == null) {
                    problems.add("operation " + id + ": unexpected, or no schemas");
                    continue;
                }
                if (!descriptor.serviceId().equals(oracle.service())) {
                    problems.add("operation " + id + ": service " + descriptor.serviceId());
                }
                identical("operation schema of " + id, exported.get(id), schemas.operation());
                identical("input schema of " + id, link(links.input()), schemas.input());
                identical("output schema of " + id, link(links.output()), schemas.output());
                List<Schema> errors = new ArrayList<>(schemas.errors());
                errors.sort(Comparator.comparing(Schema::id));
                same("errors of " + id, new TreeSet<>(links.errors()),
                     errors.stream().map(Schema::id).collect(Collectors.toCollection(TreeSet::new)));
                if (errors.size() == links.errors().size()) {
                    for (Schema error : errors) {
                        identical("error schema " + error.id() + " of " + id, exported.get(error.id()), error);
                    }
                }
            }
            same("operation descriptors", oracle.links().keySet(), ids);
        }

        private Schema link(ShapeId id) {
            return id.equals(UNIT) ? Unit.SCHEMA : exported.get(id);
        }

        private Object invokeStatic(String className, String method) {
            try {
                return loader.load(className).getMethod(method).invoke(null);
            } catch (InvocationTargetException e) {
                throw new AssertionError(className + "." + method + "() failed", e.getCause());
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }

        private void identical(String what, Object expected, Object actual) {
            if (expected == null || expected != actual) {
                problems.add(what + ": " + describe(actual) + " is not the same object as " + describe(expected));
            }
        }

        private <T> void same(String what, Set<T> expected, Set<T> actual) {
            if (!expected.equals(actual)) {
                Set<T> missing = new TreeSet<>(expected);
                missing.removeAll(actual);
                Set<T> extra = new TreeSet<>(actual);
                extra.removeAll(expected);
                problems.add(what + ": missing " + missing + ", extra " + extra);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * The traits a schema holds itself (a member's own traits, not its target's), read from Smithy Java's trait map:
     * {@code Schema} has no public enumeration, and exactness (nothing extra) needs one. Member schemas keep their own
     * traits in {@code directTraits}; root schemas in {@code Schema.traits}.
     */
    static List<Trait> directTraits(Schema schema) {
        Field field = null;
        for (Class<?> type = schema.getClass(); type != null && field == null; type = type.getSuperclass()) {
            field = declaredField(type, "directTraits");
        }
        if (field == null) {
            field = Objects.requireNonNull(declaredField(Schema.class, "traits"), "Schema.traits");
        }
        Object traitMap = get(field, schema);
        Field values = Objects.requireNonNull(declaredField(traitMap.getClass(), "values"), "TraitMap.values");
        List<Trait> result = new ArrayList<>();
        for (Trait trait : (Trait[]) get(values, traitMap)) {
            if (trait != null) {
                result.add(trait);
            }
        }
        return result;
    }

    private static Field declaredField(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static Object get(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    static ShapeId traitId(Class<? extends Trait> type) {
        try {
            return (ShapeId) type.getField("ID").get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(type.getName(), e);
        }
    }

    private static boolean prelude(ShapeId id) {
        return id.getNamespace().equals(PRELUDE);
    }

    private static String describe(Object value) {
        return value instanceof Schema schema ? "schema " + schema.id() + "@" + System.identityHashCode(schema)
                                              : String.valueOf(value);
    }

    private static String render(Map<ShapeId, Node> traits) {
        Map<String, String> rendered = new LinkedHashMap<>();
        traits.forEach((id, node) -> rendered.put(id.toString(), Node.printJson(node)));
        return rendered.toString();
    }
}
