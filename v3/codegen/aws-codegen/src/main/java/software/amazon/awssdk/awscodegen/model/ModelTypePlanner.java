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

package software.amazon.awssdk.awscodegen.model;

import static software.amazon.awssdk.awscodegen.model.ModelSymbols.ARRAYS;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.ARRAY_LIST;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.BIG_DECIMAL;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.BIG_INTEGER;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.COLLECTION;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.CONSUMER;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.DOCUMENT;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.INSTANT;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.INTEGER;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.LINKED_HASH_MAP;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.LIST;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.MAP;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.MODEL_COLLECTIONS;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SDK_BYTES;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SHAPE_DESERIALIZER;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.EVENT_STREAM;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SERIALIZABLE_STRUCT;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.STRING;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.UNIT;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.awssdk.awscodegen.CodegenOrdering;
import software.amazon.awssdk.awscodegen.schema.SchemaSymbols;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.utils.StringUtils;

/**
 * Plans one generated model type (phase 5 of generation) directly from the Smithy model: a structure, union or
 * modeled error ({@link #plan}) or an enum or intEnum ({@link #planEnum}).
 *
 * <p><b>State.</b> Every member of the shape is a final field of the type, except a member targeting a
 * {@code @streaming} blob, which is bound to the call rather than held by the type (as in V2), a member targeting a
 * {@code @streaming} union (an event stream), which has no V2 accessor or setter but holds the Smithy Java
 * {@code EventStream} set through {@code ShapeBuilder.setMemberValue} (by the generated client, from the caller's
 * publisher) or read by the protocol, and is not part of the type's equality, string form or serialization, and the
 * string
 * {@code message} member of an error, which is the exception's message (a {@code message} member of another type is a
 * field named {@code messageValue}). Enum and intEnum members hold their modeled
 * value ({@code String} or {@code Integer}), so values unknown to this SDK version survive; blobs are
 * {@code SdkBytes}; lists and maps are deep immutable snapshots taken at {@code build()}, {@code null} when absent.
 *
 * <p><b>V2 surface.</b> Accessors are fluent; list and map members have {@code hasXxx()} and read as the empty
 * auto-construct collection when absent; members that are or contain enums have an enum-typed accessor and setter
 * plus the modeled-value ones ({@code xxxAsString()}/{@code xxxAsStrings()}, {@code xxxWithStrings(...)}, with
 * {@code Integer} variants for intEnums); list members have varargs setters and structure members
 * {@code Consumer<Builder>} setters. {@code equals}/{@code hashCode} compare every modeled member value (absent and
 * empty collections differ, as {@code hasXxx()} does in V2), and {@code toString} renders modeled member names with
 * {@code @sensitive} values (or collections of them) redacted.
 *
 * <p><b>Requiredness.</b> A member is required at build time if it is {@code @required} and not client optional
 * (Smithy's client-mode nullability: not in an {@code @input} structure and not {@code @clientOptional}); a
 * {@code @default} value initializes the builder, so such a member is missing only if explicitly set to {@code null}.
 * Primitive and collection defaults are the modeled ones; no V2 defaults are invented. {@code errorCorrection()}
 * applies Smithy client error correction: every missing required member gets its zero value ({@link #zeroValue}); a
 * structure or union gets its own error-corrected empty value. A document has no zero value, so, as in Smithy Java,
 * it stays {@code null} and the builder records it as present (see {@link ModelTypePlan.Member#correctsToNull}).
 *
 * <p><b>Direct SERDE.</b> {@code serializeMembers}, {@code getMemberValue}, builder deserialization and
 * {@code setMemberValue} switch on the member's index in the schema (Smithy Java orders a structure's required members
 * without a default first); nothing uses reflection.
 */
final class ModelTypePlanner {

    /** The {@code toString} value of a non-null sensitive member, as in V2. */
    static final String REDACTED = "*** Sensitive Data Redacted ***";

    private enum Kind {
        BOOLEAN, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL, STRING, BLOB, TIMESTAMP,
        DOCUMENT, ENUM, INT_ENUM, AGGREGATE, UNIT, LIST, MAP
    }

    private final Model model;
    private final ServiceShape service;
    private final SymbolProvider symbols;
    private final Symbol holder;
    private final Symbol serviceException;
    private final Symbol serviceResponse;
    private final NullableIndex nullable;

    ModelTypePlanner(Model model, ServiceShape service, SymbolProvider symbols, Symbol holder,
                     Symbol serviceException, Symbol serviceResponse) {
        this.model = model;
        this.service = service;
        this.symbols = symbols;
        this.holder = holder;
        this.serviceException = serviceException;
        this.serviceResponse = serviceResponse;
        this.nullable = NullableIndex.of(model);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Types.

    /** The plan of a structure, union or error shape. */
    ModelTypePlan plan(Shape shape) {
        String kind = shape.isUnionShape() ? ModelTypePlan.UNION
                      : shape.hasTrait(ErrorTrait.class) ? ModelTypePlan.ERROR : ModelTypePlan.STRUCTURE;
        Symbol type = symbols.toSymbol(shape);
        List<MemberShape> members = CodegenOrdering.members(shape);
        List<MemberShape> indexOrder = new ArrayList<>(members);
        if (shape.isStructureShape()) {
            // Smithy Java's Schema stably sorts a structure's required members without a default first.
            indexOrder.sort((a, b) -> Boolean.compare(requiredWithoutDefault(b), requiredWithoutDefault(a)));
        }
        Set<String> schemaFields = new HashSet<>();
        Set<String> unionConstants = new HashSet<>(Set.of("UNKNOWN_TO_SDK_VERSION"));
        List<ModelTypePlan.Member> plans = new ArrayList<>();
        for (MemberShape member : members) {
            plans.add(member(shape, type, kind, member, indexOrder.indexOf(member), schemaFields, unionConstants));
        }
        RetryableTrait retryable = shape.getTrait(RetryableTrait.class).orElse(null);
        return new ModelTypePlan(type, shape.getId(), kind, holder, schemaConstant(shape),
                                 kind.equals(ModelTypePlan.ERROR) ? serviceException : type,
                                 kind.equals(ModelTypePlan.STRUCTURE) && isOutput(shape) ? serviceResponse : null,
                                 retryable != null, retryable != null && retryable.getThrottling(), plans);
    }

    /**
     * Whether {@code shape} is the output of an operation of the service (including resource operations): its type
     * extends the service's base response and carries the response metadata, as in V2.
     */
    private boolean isOutput(Shape shape) {
        if (serviceResponse == null) {
            return false;
        }
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            if (operation.getOutputShape().equals(shape.getId())) {
                return true;
            }
        }
        return false;
    }

    /** The plan of an enum or intEnum shape. */
    EnumTypePlan planEnum(Shape shape) {
        boolean intEnum = shape.isIntEnumShape();
        Set<String> names = new HashSet<>(Set.of("UNKNOWN_TO_SDK_VERSION", "SCHEMA", "ID"));
        List<EnumTypePlan.Constant> constants = new ArrayList<>();
        for (MemberShape member : CodegenOrdering.members(shape)) {
            EnumValueTrait value = member.expectTrait(EnumValueTrait.class);
            String name = unique(javaConstant(member.getMemberName()), names);
            constants.add(new EnumTypePlan.Constant(
                name, intEnum ? Integer.toString(value.expectIntValue()) : SchemaSymbols.javaString(
                    value.expectStringValue())));
        }
        return new EnumTypePlan(symbols.toSymbol(shape), shape.getId(), intEnum, holder, schemaConstant(shape),
                                constants);
    }

    private String schemaConstant(Shape shape) {
        return SchemaSymbols.exported(shape.getId(), service, holder).field();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Members.

    private ModelTypePlan.Member member(Shape container, Symbol type, String kind, MemberShape member, int index,
                                        Set<String> schemaFields, Set<String> unionConstants) {
        Shape target = model.expectShape(member.getTarget());
        String javaName = symbols.toMemberName(member);
        String schemaField = unique("SCHEMA_" + javaConstant(member.getMemberName()), schemaFields);
        String unionConstant = kind.equals(ModelTypePlan.UNION)
                               ? unique(javaConstant(member.getMemberName()), unionConstants) : "";
        String state;
        if (target.hasTrait(StreamingTrait.class)) {
            state = target.isUnionShape() ? ModelTypePlan.Member.EVENT_STREAM : ModelTypePlan.Member.EXCLUDED;
        } else if (isErrorMessage(model, member)) {
            state = ModelTypePlan.Member.MESSAGE;
        } else {
            state = ModelTypePlan.Member.VALUE;
        }
        boolean value = state.equals(ModelTypePlan.Member.VALUE);
        boolean required = value && member.isRequired()
                           && !nullable.isMemberNullable(member, NullableIndex.CheckMode.CLIENT);
        Kind targetKind = kind(target);
        boolean collection = targetKind == Kind.LIST || targetKind == Kind.MAP;
        String field = "this." + javaName;

        List<ModelTypePlan.Method> accessors = new ArrayList<>();
        List<ModelTypePlan.Method> setters = new ArrayList<>();
        List<ModelTypePlan.Method> factories = new ArrayList<>();
        List<ModelTypePlan.Code> helpers = new ArrayList<>();
        ModelTypePlan.Code read = ModelTypePlan.Code.NONE;
        if (value) {
            accessors(target, javaName, field, collection, accessors);
            setters(target, javaName, collection, setters);
            read = Fragment.of("builder." + javaName + " = ")
                           .code(read(target, schemaField, "de", "member", javaName, 0, helpers)).text(";").build();
            if (kind.equals(ModelTypePlan.UNION)) {
                for (ModelTypePlan.Method setter : setters) {
                    if (!setter.safeVarargs() && !setter.signature().template().contains("...")) {
                        factories.add(factory(type, setter));
                    }
                }
            }
        } else if (state.equals(ModelTypePlan.Member.MESSAGE)) {
            read = Fragment.of("builder.message(de.readString(member));").build();
        } else if (state.equals(ModelTypePlan.Member.EVENT_STREAM)) {
            read = Fragment.of("builder." + javaName + " = de.readEventStream(member);").build();
        }
        ModelTypePlan.Code snapshot = collection
                                      ? Fragment.of(MODEL_COLLECTIONS).text(targetKind == Kind.LIST ? ".copyList("
                                                                                                    : ".copyMap(")
                                                .text("builder." + javaName + ")").build()
                                      : Fragment.of("builder." + javaName).build();
        ModelTypePlan.Code serialize = value || state.equals(ModelTypePlan.Member.MESSAGE)
                                       ? Fragment.of("if (" + field + " != null) {\n")
                                                 .code(indent(serialize(target, schemaField, field, "serializer", 0)))
                                                 .text("\n}").build()
                                       : ModelTypePlan.Code.NONE;
        ModelTypePlan.Code stringValue = sensitive(target)
                                         ? Fragment.of(field + " == null ? null : ").string(REDACTED).build()
                                         : Fragment.of(field).build();
        return new ModelTypePlan.Member(
            member.getMemberName(), member.getId(), javaName, schemaField, index, state,
            value ? fieldType(target).build() : nonValueType(state),
            value ? paramType(target, false).build() : nonValueType(state), required,
            value ? defaultValue(member, target) : ModelTypePlan.Code.NONE, snapshot, serialize, read,
            required ? zeroValue(target) : ModelTypePlan.Code.NONE, stringValue, unionConstant, accessors, setters,
            factories, helpers);
    }

    /**
     * The field type of a member that is not ordinary state: the Smithy Java {@code EventStream} of an event stream
     * member, else {@code String} (an error's message, or the unused type of an excluded blob stream).
     */
    private static ModelTypePlan.Code nonValueType(String state) {
        return state.equals(ModelTypePlan.Member.EVENT_STREAM)
               ? Fragment.of(EVENT_STREAM).text("<? extends ").type(SERIALIZABLE_STRUCT).text(">").build()
               : Fragment.of(STRING).build();
    }

    /**
     * Whether {@code member} is the message of a modeled error: a member named {@code message} (in any case) that
     * targets a string, which the exception holds as its message (as in V2). A {@code message} member of another type
     * is ordinary state; its Java name is {@code messageValue}, since {@code message} is the exception builder's own
     * {@code message(String)}.
     */
    static boolean isErrorMessage(Model model, MemberShape member) {
        return model.expectShape(member.getContainer()).hasTrait(ErrorTrait.class)
               && member.getMemberName().equalsIgnoreCase("message")
               && model.expectShape(member.getTarget()).isStringShape();
    }

    private boolean requiredWithoutDefault(MemberShape member) {
        return member.isRequired() && !member.hasNonNullDefault();
    }

    private void accessors(Shape target, String javaName, String field, boolean collection,
                           List<ModelTypePlan.Method> accessors) {
        boolean enums = containsEnum(target);
        if (collection) {
            accessors.add(method(Fragment.of("boolean has" + StringUtils.capitalize(javaName) + "()"),
                                 Fragment.of("return " + field + " != null;")));
        }
        if (enums) {
            Fragment body = collection
                            ? Fragment.of("return ").type(MODEL_COLLECTIONS).text(".orAutoConstruct(")
                                      .code(toTyped(target, field, 0)).text(");")
                            : Fragment.of("return ").type(symbols.toSymbol(target)).text(".fromValue(" + field + ");");
            accessors.add(method(typedType(target).text(" " + javaName + "()"), body));
        }
        Fragment raw = collection ? Fragment.of("return ").type(MODEL_COLLECTIONS).text(".orAutoConstruct(" + field
                                                                                        + ");")
                                  : Fragment.of("return " + field + ";");
        accessors.add(method(fieldType(target).text(" " + rawGetterName(target, javaName) + "()"), raw));
    }

    private void setters(Shape target, String javaName, boolean collection, List<ModelTypePlan.Method> setters) {
        boolean enums = containsEnum(target);
        String rawName = rawSetterName(target, javaName);
        String field = "this." + javaName;
        setters.add(method(Fragment.of(rawName + "(").code(paramType(target, false)).text(" " + javaName + ")"),
                           Fragment.of(field + " = " + javaName + ";")));
        if (enums) {
            setters.add(method(Fragment.of(javaName + "(").code(paramType(target, true)).text(" " + javaName + ")"),
                               Fragment.of(field + " = ").code(toRaw(target, javaName, 0)).text(";")));
        }
        if (target instanceof CollectionShape list) {
            Shape element = model.expectShape(list.getMember().getTarget());
            boolean generic = kind(element) == Kind.LIST || kind(element) == Kind.MAP;
            setters.add(new ModelTypePlan.Method(
                Fragment.of(rawName + "(").code(varargType(element, false)).text("... " + javaName + ")").build(),
                Fragment.of(field + " = " + javaName + " == null ? null : ").type(ARRAYS)
                        .text(".asList(" + javaName + ");").build(), generic));
            if (enums) {
                setters.add(new ModelTypePlan.Method(
                    Fragment.of(javaName + "(").code(varargType(element, true)).text("... " + javaName + ")").build(),
                    Fragment.of(field + " = " + javaName + " == null ? null : ")
                            .code(toRaw(target, "(" + ARRAYS.getFullName() + ".asList(" + javaName + "))", 0))
                            .text(";").build(), generic));
            }
            if (kind(element) == Kind.AGGREGATE) {
                Symbol elementType = symbols.toSymbol(element);
                setters.add(new ModelTypePlan.Method(
                    Fragment.of(javaName + "(").type(CONSUMER).text("<").type(elementType).text(".Builder>... "
                                                                                              + javaName + ")")
                            .build(),
                    Fragment.of("if (" + javaName + " == null) {\n    " + field + " = null;\n} else {\n    ")
                            .type(LIST).text("<").type(elementType).text("> values$ = new ").type(ARRAY_LIST)
                            .text("<>(" + javaName + ".length);\n    for (").type(CONSUMER).text("<")
                            .type(elementType).text(".Builder> mutation$ : " + javaName + ") {\n        values$.add(")
                            .type(elementType).text(".builder().applyMutation(mutation$).build());\n    }\n    "
                                                    + field + " = values$;\n}").build(), true));
            }
        }
        if (!collection && kind(target) == Kind.AGGREGATE) {
            Symbol targetType = symbols.toSymbol(target);
            setters.add(method(Fragment.of(javaName + "(").type(CONSUMER).text("<").type(targetType)
                                       .text(".Builder> " + javaName + ")"),
                               Fragment.of(field + " = " + javaName + " == null ? null : ").type(targetType)
                                       .text(".builder().applyMutation(" + javaName + ").build();")));
        }
    }

    private static ModelTypePlan.Method factory(Symbol type, ModelTypePlan.Method setter) {
        String signature = setter.signature().template();
        String name = signature.substring(0, signature.indexOf('('));
        String parameter = signature.substring(signature.lastIndexOf(' ') + 1, signature.length() - 1);
        Fragment factory = Fragment.of("from" + StringUtils.capitalize(name));
        factory.code(new ModelTypePlan.Code(signature.substring(name.length()), setter.signature().types()));
        return method(Fragment.of(type).text(" ").code(factory),
                      Fragment.of("return builder()." + name + "(" + parameter + ").build();"));
    }

    private static ModelTypePlan.Method method(Fragment signature, Fragment body) {
        return new ModelTypePlan.Method(signature.build(), body.build(), false);
    }

    private String rawGetterName(Shape target, String javaName) {
        if (!containsEnum(target)) {
            return javaName;
        }
        if (target.isEnumShape()) {
            return javaName + "AsString";
        } else if (target.isIntEnumShape()) {
            return javaName + "AsInteger";
        }
        return javaName + (containsStringEnum(target) ? "AsStrings" : "AsIntegers");
    }

    private String rawSetterName(Shape target, String javaName) {
        if (!containsEnum(target) || target.isEnumShape() || target.isIntEnumShape()) {
            return javaName;
        }
        return javaName + (containsStringEnum(target) ? "WithStrings" : "WithIntegers");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Java types.

    private Kind kind(Shape shape) {
        return switch (shape.getType()) {
            case BOOLEAN -> Kind.BOOLEAN;
            case BYTE -> Kind.BYTE;
            case SHORT -> Kind.SHORT;
            case INTEGER -> Kind.INTEGER;
            case LONG -> Kind.LONG;
            case FLOAT -> Kind.FLOAT;
            case DOUBLE -> Kind.DOUBLE;
            case BIG_INTEGER -> Kind.BIG_INTEGER;
            case BIG_DECIMAL -> Kind.BIG_DECIMAL;
            case STRING -> Kind.STRING;
            case BLOB -> Kind.BLOB;
            case TIMESTAMP -> Kind.TIMESTAMP;
            case DOCUMENT -> Kind.DOCUMENT;
            case ENUM -> Kind.ENUM;
            case INT_ENUM -> Kind.INT_ENUM;
            case STRUCTURE -> shape.getId().toString().equals("smithy.api#Unit") ? Kind.UNIT : Kind.AGGREGATE;
            case UNION -> Kind.AGGREGATE;
            case LIST, SET -> Kind.LIST;
            case MAP -> Kind.MAP;
            default -> throw new AwsCodegenException("Member target " + shape.getId() + " of type " + shape.getType()
                                                     + " cannot be held by a generated type.");
        };
    }

    private Shape element(Shape list) {
        return model.expectShape(((CollectionShape) list).getMember().getTarget());
    }

    private Shape mapKey(Shape map) {
        return model.expectShape(((MapShape) map).getKey().getTarget());
    }

    private Shape mapValue(Shape map) {
        return model.expectShape(((MapShape) map).getValue().getTarget());
    }

    /** Type of the immutable field: enums hold their modeled value, collections are lists and string-keyed maps. */
    private Fragment fieldType(Shape target) {
        return switch (kind(target)) {
            case LIST -> Fragment.of(LIST).text("<").code(fieldType(element(target))).text(">");
            case MAP -> Fragment.of(MAP).text("<").type(STRING).text(", ").code(fieldType(mapValue(target)))
                                .text(">");
            case ENUM -> Fragment.of(STRING);
            case INT_ENUM -> Fragment.of(INTEGER);
            default -> Fragment.of(symbols.toSymbol(target));
        };
    }

    /** Type of an enum-typed accessor: {@link #fieldType} with every enum value and map key typed. */
    private Fragment typedType(Shape target) {
        return switch (kind(target)) {
            case LIST -> Fragment.of(LIST).text("<").code(typedType(element(target))).text(">");
            case MAP -> Fragment.of(MAP).text("<").code(typedLeaf(mapKey(target), true)).text(", ")
                                .code(typedType(mapValue(target))).text(">");
            default -> typedLeaf(target, true);
        };
    }

    /** Parameter type of a setter: collections accept any collection at every level, as in V2. */
    private Fragment paramType(Shape target, boolean typed) {
        return switch (kind(target)) {
            case LIST -> Fragment.of(COLLECTION).text("<").code(elementParam(element(target), typed)).text(">");
            case MAP -> Fragment.of(MAP).text("<").code(typedLeaf(mapKey(target), typed)).text(", ")
                                .code(elementParam(mapValue(target), typed)).text(">");
            default -> typedLeaf(target, typed);
        };
    }

    private Fragment elementParam(Shape element, boolean typed) {
        Kind kind = kind(element);
        return kind == Kind.LIST || kind == Kind.MAP ? Fragment.of("? extends ").code(paramType(element, typed))
                                                     : typedLeaf(element, typed);
    }

    private Fragment varargType(Shape element, boolean typed) {
        Kind kind = kind(element);
        return kind == Kind.LIST || kind == Kind.MAP ? paramType(element, typed) : typedLeaf(element, typed);
    }

    private Fragment typedLeaf(Shape shape, boolean typed) {
        Kind kind = kind(shape);
        if (!typed && kind == Kind.ENUM) {
            return Fragment.of(STRING);
        } else if (!typed && kind == Kind.INT_ENUM) {
            return Fragment.of(INTEGER);
        } else if (kind == Kind.LIST || kind == Kind.MAP) {
            return typed ? typedType(shape) : fieldType(shape);
        }
        return Fragment.of(symbols.toSymbol(shape));
    }

    private boolean containsEnum(Shape shape) {
        return switch (kind(shape)) {
            case ENUM, INT_ENUM -> true;
            case LIST -> containsEnum(element(shape));
            case MAP -> containsEnum(mapKey(shape)) || containsEnum(mapValue(shape));
            default -> false;
        };
    }

    private boolean containsStringEnum(Shape shape) {
        return switch (kind(shape)) {
            case ENUM -> true;
            case LIST -> containsStringEnum(element(shape));
            case MAP -> containsStringEnum(mapKey(shape)) || containsStringEnum(mapValue(shape));
            default -> false;
        };
    }

    /** Whether rendering the value would reveal a {@code @sensitive} shape: the target or a nested collection value. */
    private boolean sensitive(Shape shape) {
        if (shape.hasTrait(SensitiveTrait.class)) {
            return true;
        }
        return switch (kind(shape)) {
            case LIST -> sensitive(element(shape));
            case MAP -> sensitive(mapKey(shape)) || sensitive(mapValue(shape));
            default -> false;
        };
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Enum conversions.

    /** An expression converting {@code value} (modeled values) to its enum-typed form. */
    private Fragment toTyped(Shape shape, String value, int depth) {
        return switch (kind(shape)) {
            case ENUM -> Fragment.of(symbols.toSymbol(shape)).text(".fromValue((").type(STRING).text(") " + value
                                                                                                  + ")");
            case INT_ENUM -> Fragment.of(symbols.toSymbol(shape)).text(".fromValue((").type(INTEGER)
                                     .text(") " + value + ")");
            case LIST -> {
                String e = "e" + depth + "$";
                yield Fragment.of(MODEL_COLLECTIONS).text(".convertList((").type(COLLECTION).text("<?>) " + value
                                                                                                   + ", " + e
                                                                                                   + " -> ")
                              .code(toTyped(element(shape), e, depth + 1)).text(")");
            }
            case MAP -> {
                String k = "k" + depth + "$";
                String v = "v" + depth + "$";
                yield Fragment.of(MODEL_COLLECTIONS).text(".convertMap((").type(MAP).text("<?, ?>) " + value + ", "
                                                                                           + k + " -> ")
                              .code(toTyped(mapKey(shape), k, depth + 1)).text(", " + v + " -> ")
                              .code(toTyped(mapValue(shape), v, depth + 1)).text(")");
            }
            default -> Fragment.of(value);
        };
    }

    /** An expression converting {@code value} (enum-typed) to modeled values. */
    private Fragment toRaw(Shape shape, String value, int depth) {
        return switch (kind(shape)) {
            case ENUM -> Fragment.of("(" + value + " == null ? null : " + value + ".toString())");
            case INT_ENUM -> Fragment.of("(" + value + " == null ? null : ((").type(symbols.toSymbol(shape))
                                     .text(") " + value + ").value())");
            case LIST -> {
                String e = "e" + depth + "$";
                yield Fragment.of(MODEL_COLLECTIONS).text(".convertList((").type(COLLECTION).text("<?>) " + value
                                                                                                   + ", " + e
                                                                                                   + " -> ")
                              .code(toRaw(element(shape), e, depth + 1)).text(")");
            }
            case MAP -> {
                String k = "k" + depth + "$";
                String v = "v" + depth + "$";
                yield Fragment.of(MODEL_COLLECTIONS).text(".convertMap((").type(MAP).text("<?, ?>) " + value + ", "
                                                                                           + k + " -> ")
                              .code(toRaw(mapKey(shape), k, depth + 1)).text(", " + v + " -> ")
                              .code(toRaw(mapValue(shape), v, depth + 1)).text(")");
            }
            default -> Fragment.of(value);
        };
    }

    // ---------------------------------------------------------------------------------------------------------------
    // SERDE.

    /** Statements writing non-null {@code value} of {@code shape} with member schema {@code schema}. */
    private Fragment serialize(Shape shape, String schema, String value, String serializer, int depth) {
        String write = serializer + ".write";
        return switch (kind(shape)) {
            case BOOLEAN -> Fragment.of(write + "Boolean(" + schema + ", " + value + ");");
            case BYTE -> Fragment.of(write + "Byte(" + schema + ", " + value + ");");
            case SHORT -> Fragment.of(write + "Short(" + schema + ", " + value + ");");
            case INTEGER, INT_ENUM -> Fragment.of(write + "Integer(" + schema + ", " + value + ");");
            case LONG -> Fragment.of(write + "Long(" + schema + ", " + value + ");");
            case FLOAT -> Fragment.of(write + "Float(" + schema + ", " + value + ");");
            case DOUBLE -> Fragment.of(write + "Double(" + schema + ", " + value + ");");
            case BIG_INTEGER -> Fragment.of(write + "BigInteger(" + schema + ", " + value + ");");
            case BIG_DECIMAL -> Fragment.of(write + "BigDecimal(" + schema + ", " + value + ");");
            case STRING, ENUM -> Fragment.of(write + "String(" + schema + ", " + value + ");");
            case BLOB -> Fragment.of(write + "Blob(" + schema + ", " + value + ".asByteBuffer());");
            case TIMESTAMP -> Fragment.of(write + "Timestamp(" + schema + ", " + value + ");");
            case DOCUMENT -> Fragment.of(write + "Document(" + schema + ", " + value + ");");
            case AGGREGATE, UNIT -> Fragment.of(write + "Struct(" + schema + ", " + value + ");");
            case LIST -> {
                String list = "l" + depth + "$";
                String listSerializer = "s" + depth + "$";
                String element = "e" + depth + "$";
                String elementSchema = schema + ".listMember()";
                yield Fragment.of(write + "List(" + schema + ", " + value + ", " + value + ".size(), (" + list + ", "
                                  + listSerializer + ") -> {\n    for (var " + element + " : " + list + ") {\n")
                              .code(indent(indent(nullOr(element(shape), elementSchema, element, listSerializer,
                                                         depth))))
                              .text("\n    }\n});");
            }
            case MAP -> {
                String map = "m" + depth + "$";
                String mapSerializer = "s" + depth + "$";
                String entry = "n" + depth + "$";
                String entryValue = "v" + depth + "$";
                String valueSerializer = "w" + depth + "$";
                yield Fragment.of(write + "Map(" + schema + ", " + value + ", " + value + ".size(), (" + map + ", "
                                  + mapSerializer + ") -> {\n    for (var " + entry + " : " + map
                                  + ".entrySet()) {\n        " + mapSerializer + ".writeEntry(" + schema
                                  + ".mapKeyMember(), " + entry + ".getKey(), " + entry + ".getValue(), ("
                                  + entryValue + ", " + valueSerializer + ") -> {\n")
                              .code(indent(indent(indent(nullOr(mapValue(shape), schema + ".mapValueMember()",
                                                                entryValue, valueSerializer, depth)))))
                              .text("\n        });\n    }\n});");
            }
        };
    }

    private Fragment nullOr(Shape shape, String schema, String value, String serializer, int depth) {
        return Fragment.of("if (" + value + " == null) {\n    " + serializer + ".writeNull(" + schema + ");\n} else {\n")
                       .code(indent(serialize(shape, schema, value, serializer, depth + 1))).text("\n}");
    }

    /**
     * An expression reading {@code shape} with deserializer {@code de} and member schema {@code schema}; lists and
     * maps are read by a generated helper (added to {@code helpers}) that uses the static schema {@code staticSchema}.
     */
    private Fragment read(Shape shape, String staticSchema, String de, String schema, String javaName, int depth,
                          List<ModelTypePlan.Code> helpers) {
        return switch (kind(shape)) {
            case BOOLEAN -> Fragment.of(de + ".readBoolean(" + schema + ")");
            case BYTE -> Fragment.of(de + ".readByte(" + schema + ")");
            case SHORT -> Fragment.of(de + ".readShort(" + schema + ")");
            case INTEGER, INT_ENUM -> Fragment.of(de + ".readInteger(" + schema + ")");
            case LONG -> Fragment.of(de + ".readLong(" + schema + ")");
            case FLOAT -> Fragment.of(de + ".readFloat(" + schema + ")");
            case DOUBLE -> Fragment.of(de + ".readDouble(" + schema + ")");
            case BIG_INTEGER -> Fragment.of(de + ".readBigInteger(" + schema + ")");
            case BIG_DECIMAL -> Fragment.of(de + ".readBigDecimal(" + schema + ")");
            case STRING, ENUM -> Fragment.of(de + ".readString(" + schema + ")");
            case BLOB -> Fragment.of(SDK_BYTES).text(".fromByteBuffer(" + de + ".readBlob(" + schema + "))");
            case TIMESTAMP -> Fragment.of(de + ".readTimestamp(" + schema + ")");
            case DOCUMENT -> Fragment.of(de + ".readDocument()");
            case AGGREGATE -> Fragment.of(symbols.toSymbol(shape)).text(".builder().deserializeMember(" + de + ", "
                                                                        + schema + ").build()");
            case UNIT -> Fragment.of(UNIT).text(".builder().deserializeMember(" + de + ", " + schema + ").build()");
            case LIST, MAP -> {
                String helper = "read" + StringUtils.capitalize(javaName) + depth;
                helpers.add(readHelper(shape, staticSchema, helper, javaName, depth, helpers));
                yield Fragment.of(helper + "(" + de + ")");
            }
        };
    }

    private ModelTypePlan.Code readHelper(Shape shape, String schema, String name, String javaName, int depth,
                                          List<ModelTypePlan.Code> helpers) {
        boolean sparse = shape.hasTrait(SparseTrait.class);
        Fragment type = fieldType(shape);
        Fragment code = Fragment.of("private static ").code(type).text(" " + name + "(").type(SHAPE_DESERIALIZER)
                                .text(" de) {\n    ").code(type).text(" result = new ");
        if (kind(shape) == Kind.LIST) {
            String elementSchema = schema + ".listMember()";
            Fragment element = read(element(shape), elementSchema, "element$", elementSchema, javaName, depth + 1,
                                    helpers);
            code.type(ARRAY_LIST).text("<>();\n    de.readList(" + schema + ", result, (list$, element$) -> {\n"
                                       + "        if (element$.isNull()) {\n            element$.readNull();\n")
                .text(sparse ? "            list$.add(null);\n" : "")
                .text("        } else {\n            list$.add(").code(element).text(");\n        }\n    });\n"
                                                                              + "    return ")
                .type(MODEL_COLLECTIONS).text(".adoptList(result);\n}");
        } else {
            String valueSchema = schema + ".mapValueMember()";
            Fragment value = read(mapValue(shape), valueSchema, "value$", valueSchema, javaName, depth + 1, helpers);
            code.type(LINKED_HASH_MAP).text("<>();\n    de.readStringMap(" + schema
                                            + ", result, (map$, key$, value$) -> {\n        if (value$.isNull()) {\n"
                                            + "            value$.readNull();\n")
                .text(sparse ? "            map$.put(key$, null);\n" : "")
                .text("        } else {\n            map$.put(key$, ").code(value).text(");\n        }\n    });\n"
                                                                                  + "    return ")
                .type(MODEL_COLLECTIONS).text(".adoptMap(result);\n}");
        }
        return code.build();
    }

    private static ModelTypePlan.Code indent(Fragment fragment) {
        return indent(fragment.build());
    }

    private static ModelTypePlan.Code indent(ModelTypePlan.Code code) {
        return new ModelTypePlan.Code("    " + code.template().replace("\n", "\n    "), code.types());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Defaults and error correction.

    private ModelTypePlan.Code defaultValue(MemberShape member, Shape target) {
        Node node = member.getTrait(DefaultTrait.class).map(DefaultTrait::toNode).orElse(Node.nullNode());
        if (node.isNullNode()) {
            return ModelTypePlan.Code.NONE;
        }
        return switch (kind(target)) {
            case BOOLEAN -> Fragment.of(Boolean.toString(node.expectBooleanNode().getValue())).build();
            case BYTE -> Fragment.of("(byte) " + number(node).byteValue()).build();
            case SHORT -> Fragment.of("(short) " + number(node).shortValue()).build();
            case INTEGER, INT_ENUM -> Fragment.of(Integer.toString(number(node).intValue())).build();
            case LONG -> Fragment.of(number(node).longValue() + "L").build();
            case FLOAT -> Fragment.of(floating(node, "Float", "f")).build();
            case DOUBLE -> Fragment.of(floating(node, "Double", "d")).build();
            case BIG_INTEGER -> Fragment.of("new ").type(BIG_INTEGER)
                                        .text("(").string(decimal(node).toBigIntegerExact().toString()).text(")")
                                        .build();
            case BIG_DECIMAL -> Fragment.of("new ").type(BIG_DECIMAL).text("(").string(decimal(node).toString())
                                        .text(")").build();
            case STRING, ENUM -> Fragment.of(SchemaSymbols.javaString(node.expectStringNode().getValue())).build();
            case BLOB -> blob(Base64.getDecoder().decode(node.expectStringNode().getValue()));
            case TIMESTAMP -> timestamp(node);
            case DOCUMENT -> document(node).build();
            case LIST -> Fragment.of(LIST).text(".of()").build();
            case MAP -> Fragment.of(MAP).text(".of()").build();
            case AGGREGATE, UNIT -> throw new AwsCodegenException("Member " + member.getId()
                                                                  + " has a default value, which its target "
                                                                  + target.getId() + " cannot have.");
        };
    }

    private static Number number(Node node) {
        return node.expectNumberNode().getValue();
    }

    private static BigDecimal decimal(Node node) {
        return new BigDecimal(node.expectNumberNode().getValue().toString());
    }

    private static String floating(Node node, String type, String suffix) {
        if (node instanceof StringNode string) {
            return switch (string.getValue()) {
                case "NaN" -> type + ".NaN";
                case "Infinity" -> type + ".POSITIVE_INFINITY";
                case "-Infinity" -> type + ".NEGATIVE_INFINITY";
                default -> throw new AwsCodegenException("Invalid floating-point default " + string.getValue());
            };
        }
        double value = number(node).doubleValue();
        return (suffix.equals("f") ? Float.toString((float) value) : Double.toString(value)) + suffix;
    }

    private static ModelTypePlan.Code blob(byte[] bytes) {
        StringBuilder array = new StringBuilder("new byte[] {");
        for (int i = 0; i < bytes.length; i++) {
            array.append(i == 0 ? "" : ", ").append(bytes[i]);
        }
        return Fragment.of(SDK_BYTES).text(".fromByteArrayUnsafe(" + array + "})").build();
    }

    private static ModelTypePlan.Code timestamp(Node node) {
        if (node instanceof StringNode string) {
            return Fragment.of(INSTANT).text(".parse(").string(string.getValue()).text(")").build();
        }
        BigDecimal value = decimal(node);
        BigDecimal seconds = value.setScale(0, RoundingMode.FLOOR);
        long nanos = value.subtract(seconds).movePointRight(9).longValueExact();
        return Fragment.of(INSTANT).text(".ofEpochSecond(" + seconds.longValueExact() + "L, " + nanos + "L)").build();
    }

    private Fragment document(Node node) {
        Fragment code = Fragment.of(DOCUMENT).text(".of(");
        if (node instanceof NumberNode number) {
            Number value = number.getValue();
            code.text(value instanceof Double || value instanceof Float ? value.doubleValue() + "d"
                                                                       : value.longValue() + "L");
        } else if (node.isBooleanNode()) {
            code.text(Boolean.toString(node.expectBooleanNode().getValue()));
        } else if (node instanceof StringNode string) {
            code.string(string.getValue());
        } else if (node instanceof ArrayNode array) {
            code.type(LIST).text(".of(");
            String separator = "";
            for (Node element : array.getElements()) {
                code.text(separator).code(document(element));
                separator = ", ";
            }
            code.text(")");
        } else if (node instanceof ObjectNode object) {
            code.type(MAP).text(".ofEntries(");
            String separator = "";
            for (Map.Entry<StringNode, Node> member : object.getMembers().entrySet()) {
                code.text(separator).type(MAP).text(".entry(").string(member.getKey().getValue()).text(", ")
                    .code(document(member.getValue())).text(")");
                separator = ", ";
            }
            code.text(")");
        } else {
            throw new AwsCodegenException("A document default cannot contain a null value.");
        }
        return code.text(")");
    }

    /**
     * The Smithy client error-correction value of a missing required member; empty for a document, which has none
     * (error correction leaves it null and marks it present). Every other kind has one: a structure is its
     * error-corrected empty value (Smithy forbids cycles of required structure members, so this terminates) and a
     * union is empty, which its builder accepts.
     */
    private ModelTypePlan.Code zeroValue(Shape target) {
        return switch (kind(target)) {
            case BOOLEAN -> Fragment.of("false").build();
            case BYTE -> Fragment.of("(byte) 0").build();
            case SHORT -> Fragment.of("(short) 0").build();
            case INTEGER, INT_ENUM -> Fragment.of("0").build();
            case LONG -> Fragment.of("0L").build();
            case FLOAT -> Fragment.of("0f").build();
            case DOUBLE -> Fragment.of("0d").build();
            case BIG_INTEGER -> Fragment.of(BIG_INTEGER).text(".ZERO").build();
            case BIG_DECIMAL -> Fragment.of(BIG_DECIMAL).text(".ZERO").build();
            case STRING, ENUM -> Fragment.of("\"\"").build();
            case BLOB -> blob(new byte[0]);
            case TIMESTAMP -> Fragment.of(INSTANT).text(".EPOCH").build();
            case DOCUMENT -> ModelTypePlan.Code.NONE;
            case LIST -> Fragment.of(LIST).text(".of()").build();
            case MAP -> Fragment.of(MAP).text(".of()").build();
            case UNIT -> Fragment.of(UNIT).text(".getInstance()").build();
            case AGGREGATE -> Fragment.of(symbols.toSymbol(target)).text(".builder().errorCorrection().build()")
                                      .build();
        };
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Names.

    /** An upper-snake Java constant name for a modeled name; names that are not Java identifiers are made valid. */
    static String javaConstant(String name) {
        String constant;
        try {
            constant = SchemaSymbols.constantName(name);
        } catch (AwsCodegenException e) {
            constant = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        }
        return Character.isJavaIdentifierStart(constant.charAt(0)) ? constant : "_" + constant;
    }

    private static String unique(String base, Set<String> taken) {
        String name = base;
        for (int i = 2; !taken.add(name); i++) {
            name = base + "_" + i;
        }
        return name;
    }
}
