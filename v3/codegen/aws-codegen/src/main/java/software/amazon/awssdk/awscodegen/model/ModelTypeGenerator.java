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

import static software.amazon.awssdk.awscodegen.model.ModelSymbols.ARRAY_LIST;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.AWS_ERROR_DETAILS;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.AWS_SERVICE_EXCEPTION;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.COPYABLE_BUILDER;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.ENUM_SET;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.ILLEGAL_ARGUMENT;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.INTEGER;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.LINKED_HASH_MAP;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.LIST;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.MAP;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.MISSING_REQUIRED_MEMBERS;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.OBJECT;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.OBJECTS;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.OVERRIDE;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SAFE_VARARGS;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SCHEMA;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SCHEMA_UTILS;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SERIALIZABLE_STRUCT;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SET;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SHAPE_BUILDER;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SHAPE_DESERIALIZER;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SHAPE_ID;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SHAPE_SERIALIZER;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.STRING;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.SUPPRESS_WARNINGS;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.TO_COPYABLE_BUILDER;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.TO_STRING;
import static software.amazon.awssdk.awscodegen.model.ModelSymbols.UNSUPPORTED_OPERATION;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.awscodegen.AwsCodegenContext;
import software.amazon.awssdk.awscodegen.AwsJavaWriter;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Emits the generated model types of a service (phase 5 of generation): one immutable class per structure, union and
 * modeled error, one Java enum per enum and intEnum, and the service's base exception {@code <Service>Exception}.
 * See {@link ModelTypePlanner} for the rules; each file is rendered from its own plan.
 *
 * <p>Generated types implement Smithy Java's {@code SerializableStruct} (enums expose their schema), keep V2's
 * builder/accessor/equality/string contracts, and never use reflection: serialization, member lookup, builder
 * deserialization and member assignment are direct switches over the type's member schemas. Builder suppliers are not
 * registered on schemas; the type registry (task 3.3) links schemas to builders.
 */
public final class ModelTypeGenerator {

    private static final String BUILDER = "Builder";
    private static final String BUILDER_IMPL = "BuilderImpl";
    private static final String MEMBER_READER = "MemberReader";
    private static final String TYPE = "Type";
    private static final String VALUE_MAP = "ValueMap";

    private ModelTypeGenerator() {
    }

    /**
     * Emits the type of a structure, union or error shape of the context's service. Shapes whose symbol is a runtime
     * type (for example {@code smithy.api#Unit}, which is Smithy Java's {@code Unit}) have no definition file and
     * generate nothing.
     */
    public static void generate(AwsCodegenContext context, Symbol schemaHolder, Symbol serviceException,
                                Shape shape) {
        if (context.symbolProvider().toSymbol(shape).getDefinitionFile().isEmpty()) {
            return;
        }
        ModelTypePlan plan = planner(context, schemaHolder, serviceException).plan(shape);
        render(plan, context.writerDelegator());
    }

    /** Emits the Java enum of an enum or intEnum shape of the context's service. */
    public static void generateEnum(AwsCodegenContext context, Symbol schemaHolder, Shape shape) {
        EnumTypePlan plan = planner(context, schemaHolder, null).planEnum(shape);
        renderEnum(plan, context.writerDelegator());
    }

    /** Emits the service's base exception, {@code serviceException}. */
    public static void generateServiceException(AwsCodegenContext context, Symbol serviceException) {
        renderServiceException(new ServiceExceptionPlan(serviceException, context.service().getId()),
                               context.writerDelegator());
    }

    /**
     * Whether {@code member} is the message of a modeled error, held by the exception as its message rather than as a
     * field: a member named {@code message} (in any case) targeting a string. The symbol provider gives a
     * {@code message} member of any other type the Java name {@code messageValue}.
     */
    public static boolean isErrorMessage(Model model, MemberShape member) {
        return ModelTypePlanner.isErrorMessage(model, member);
    }

    private static ModelTypePlanner planner(AwsCodegenContext context, Symbol holder, Symbol serviceException) {
        return new ModelTypePlanner(context.model(), context.service(), context.symbolProvider(), holder,
                                    serviceException);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Structures, unions and errors.

    static void render(ModelTypePlan plan, WriterDelegator<AwsJavaWriter> writers) {
        Symbol type = plan.type();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type, BUILDER, BUILDER_IMPL, MEMBER_READER, TYPE);
            renderType(writer, plan);
        });
    }

    private static void renderType(AwsJavaWriter writer, ModelTypePlan plan) {
        Symbol type = plan.type();
        String name = type.getName();
        writer.write("/**");
        writer.write(" * The modeled {@code $L} $L.", plan.shape(), plan.isError() ? "error" : plan.kind());
        writer.write(" */");
        if (plan.isError()) {
            writer.openBlock("public final class $L extends $T implements $T, $T<$L.Builder, $L> {", "}",
                             new Object[] {name, plan.serviceException(), SERIALIZABLE_STRUCT, TO_COPYABLE_BUILDER,
                                           name, name},
                             () -> renderTypeBody(writer, plan));
        } else {
            writer.openBlock("public final class $L implements $T, $T<$L.Builder, $L> {", "}", name,
                             SERIALIZABLE_STRUCT, TO_COPYABLE_BUILDER, name, name,
                             () -> renderTypeBody(writer, plan));
        }
    }

    private static void renderTypeBody(AwsJavaWriter writer, ModelTypePlan plan) {
        List<ModelTypePlan.Member> members = plan.members();
        writer.write("/** Absolute ID of the modeled shape. */");
        writer.write("public static final $T ID = $T.from($S);", SHAPE_ID, SHAPE_ID, plan.shape().toString());
        writer.write("");
        writer.write("/** The one schema of this type. */");
        writer.write("public static final $T SCHEMA = $T.$L;", SCHEMA, plan.schemaHolder(), plan.schemaConstant());
        writer.write("");
        if (plan.isError()) {
            writer.write("private static final long serialVersionUID = 1L;");
            writer.write("");
        }
        for (ModelTypePlan.Member member : members) {
            writer.write("private static final $T $L = SCHEMA.member($S);", SCHEMA, member.schemaField(),
                         member.name());
        }
        if (!members.isEmpty()) {
            writer.write("");
        }
        for (ModelTypePlan.Member member : members) {
            if (member.hasField()) {
                write(writer, Fragment.of("private final ").code(member.fieldType()).text(" " + member.javaName()
                                                                                          + ";"));
            } else if (member.state().equals(ModelTypePlan.Member.MESSAGE)) {
                writer.write("private final $T $L;", STRING, member.javaName());
            } else if (member.eventStream()) {
                write(writer, Fragment.of("private final ").code(member.fieldType())
                                      .text(" " + member.javaName() + ";"));
            }
        }
        if (plan.isUnion()) {
            writer.write("private final Type type;");
        }
        writer.write("");
        renderConstructor(writer, plan);
        for (ModelTypePlan.Member member : members) {
            for (ModelTypePlan.Method accessor : member.accessors()) {
                writer.write("");
                writer.openBlock("public " + template(accessor.signature()) + " {", "}",
                                 accessor.signature().types().toArray(), () -> write(writer, accessor.body()));
            }
        }
        if (plan.isUnion()) {
            writer.write("");
            writer.write("/**");
            writer.write(" * The member that is set: {@link Type#UNKNOWN_TO_SDK_VERSION} if none is (for example a "
                         + "member unknown to this");
            writer.write(" * SDK version), or {@code null} if more than one is.");
            writer.write(" */");
            writer.openBlock("public Type type() {", "}", () -> writer.write("return this.type;"));
            for (ModelTypePlan.Member member : members) {
                for (ModelTypePlan.Method factory : member.factories()) {
                    writer.write("");
                    writer.openBlock("public static " + template(factory.signature()) + " {", "}",
                                     factory.signature().types().toArray(), () -> write(writer, factory.body()));
                }
            }
        }
        writer.write("");
        writer.write("@$T", OVERRIDE);
        writer.openBlock("public Builder toBuilder() {", "}", () -> writer.write("return new BuilderImpl(this);"));
        writer.write("");
        writer.openBlock("public static Builder builder() {", "}", () -> writer.write("return new BuilderImpl();"));
        if (plan.retryable()) {
            writer.write("");
            writer.write("@$T", OVERRIDE);
            writer.openBlock("public boolean isRetryableException() {", "}", () -> writer.write("return true;"));
        }
        if (plan.throttling()) {
            writer.write("");
            writer.write("@$T", OVERRIDE);
            writer.openBlock("public boolean isThrottlingException() {", "}", () -> writer.write("return true;"));
        }
        writer.write("");
        writer.write("@$T", OVERRIDE);
        writer.openBlock("public $T schema() {", "}", SCHEMA, () -> writer.write("return SCHEMA;"));
        writer.write("");
        writer.write("@$T", OVERRIDE);
        writer.openBlock("public void serializeMembers($T serializer) {", "}", SHAPE_SERIALIZER,
                         () -> members.forEach(member -> write(writer, member.serialize())));
        writer.write("");
        renderGetMemberValue(writer, plan);
        if (!plan.isError()) {
            writer.write("");
            renderEquality(writer, plan);
        }
        for (ModelTypePlan.Member member : members) {
            for (ModelTypePlan.Code helper : member.helpers()) {
                writer.write("");
                write(writer, helper);
            }
        }
        writer.write("");
        renderBuilderInterface(writer, plan);
        writer.write("");
        renderBuilderImpl(writer, plan);
        writer.write("");
        renderMemberReader(writer, plan);
        if (plan.isUnion()) {
            writer.write("");
            writer.write("/** The members of the union, and {@link #UNKNOWN_TO_SDK_VERSION} for none. */");
            writer.openBlock("public enum Type {", "}", () -> {
                for (ModelTypePlan.Member member : members) {
                    writer.write("$L,", member.unionConstant());
                }
                writer.write("UNKNOWN_TO_SDK_VERSION");
            });
        }
    }

    private static void renderConstructor(AwsJavaWriter writer, ModelTypePlan plan) {
        String name = plan.type().getName();
        writer.openBlock("private $L(BuilderImpl builder) {", "}", name, () -> {
            if (plan.isError()) {
                writer.write("super(builder);");
            }
            List<ModelTypePlan.Member> required = new ArrayList<>();
            for (ModelTypePlan.Member member : plan.members()) {
                if (member.hasField()) {
                    write(writer, Fragment.of("this." + member.javaName() + " = ").code(member.snapshot()).text(";"));
                } else if (member.state().equals(ModelTypePlan.Member.MESSAGE)) {
                    writer.write("this.$L = builder.message();", member.javaName());
                } else if (member.eventStream()) {
                    writer.write("this.$L = builder.$L;", member.javaName(), member.javaName());
                }
                if (member.required()) {
                    required.add(member);
                }
            }
            if (plan.isUnion()) {
                writer.write("int present$$ = 0;");
                writer.write("Type type$$ = Type.UNKNOWN_TO_SDK_VERSION;");
                for (ModelTypePlan.Member member : plan.members()) {
                    if (member.hasField()) {
                        writer.openBlock("if (this.$L != null) {", "}", member.javaName(), () -> {
                            writer.write("present$$++;");
                            writer.write("type$$ = Type.$L;", member.unionConstant());
                        });
                    }
                }
                writer.write("this.type = present$$ > 1 ? null : type$$;");
            }
            if (required.size() == 1) {
                ModelTypePlan.Member member = required.get(0);
                writer.openBlock("if (" + missing(member) + ") {", "}",
                                 () -> writer.write("throw $T.create(ID, $T.of($L.id()));", MISSING_REQUIRED_MEMBERS,
                                                    LIST, member.schemaField()));
            } else if (!required.isEmpty()) {
                String condition = String.join(" || ", required.stream()
                                                               .map(m -> m.correctsToNull() ? "(" + missing(m) + ")"
                                                                                            : missing(m))
                                                               .toList());
                writer.openBlock("if (" + condition + ") {", "}", () -> {
                    writer.write("$T<$T> missing$$ = new $T<>($L);", LIST, SHAPE_ID, ARRAY_LIST, required.size());
                    for (ModelTypePlan.Member member : required) {
                        writer.openBlock("if (" + missing(member) + ") {", "}",
                                         () -> writer.write("missing$$.add($L.id());", member.schemaField()));
                    }
                    writer.write("throw $T.create(ID, missing$$);", MISSING_REQUIRED_MEMBERS);
                });
            }
        });
    }

    /**
     * The constructor's condition that required {@code member} is missing: it is null, and, for a member error
     * correction leaves null (a document), error correction did not record it as present. The result is a writer
     * template: the flag's {@code $} is escaped.
     */
    private static String missing(ModelTypePlan.Member member) {
        String isNull = "this." + member.javaName() + " == null";
        return member.correctsToNull() ? isNull + " && !builder." + member.errorCorrectedFlag().replace("$", "$$")
                                       : isNull;
    }

    private static void renderGetMemberValue(AwsJavaWriter writer, ModelTypePlan plan) {
        writer.write("@$T", OVERRIDE);
        writer.write("@$T(\"unchecked\")", SUPPRESS_WARNINGS);
        writer.openBlock("public <T> T getMemberValue($T member) {", "}", SCHEMA, () -> {
            if (plan.members().isEmpty()) {
                writer.write("throw new $T(\"Attempted to get a non-existent member of \" + ID + \": \" + "
                             + "member.id());", ILLEGAL_ARGUMENT);
                return;
            }
            writer.openBlock("return switch (member.memberIndex()) {", "};", () -> {
                for (ModelTypePlan.Member member : sortedByIndex(plan.members())) {
                    String value = member.state().equals(ModelTypePlan.Member.EXCLUDED) ? "null"
                                                                                       : "this." + member.javaName();
                    writer.write("case $L -> (T) $T.validateSameMember($L, member, $L);", member.index(), SCHEMA_UTILS,
                                 member.schemaField(), value);
                }
                writer.write("default -> throw new $T(\"Attempted to get a non-existent member of \" + ID + \": \" + "
                             + "member.id());", ILLEGAL_ARGUMENT);
            });
        });
    }

    private static void renderEquality(AwsJavaWriter writer, ModelTypePlan plan) {
        String name = plan.type().getName();
        List<ModelTypePlan.Member> fields = plan.members().stream().filter(ModelTypePlan.Member::hasField).toList();
        writer.write("@$T", OVERRIDE);
        writer.openBlock("public int hashCode() {", "}", () -> {
            writer.write("int hashCode = 1;");
            for (ModelTypePlan.Member member : fields) {
                writer.write("hashCode = 31 * hashCode + $T.hashCode(this.$L);", OBJECTS, member.javaName());
            }
            writer.write("return hashCode;");
        });
        writer.write("");
        writer.write("@$T", OVERRIDE);
        writer.openBlock("public boolean equals($T obj) {", "}", OBJECT, () -> {
            writer.openBlock("if (this == obj) {", "}", () -> writer.write("return true;"));
            writer.openBlock("if (!(obj instanceof $L)) {", "}", name, () -> writer.write("return false;"));
            if (fields.isEmpty()) {
                writer.write("return true;");
                return;
            }
            writer.write("$L other$$ = ($L) obj;", name, name);
            Fragment condition = Fragment.of("return ");
            for (int i = 0; i < fields.size(); i++) {
                String field = fields.get(i).javaName();
                condition.text(i == 0 ? "" : "\n       && ").type(OBJECTS)
                         .text(".equals(this." + field + ", other$." + field + ")");
            }
            write(writer, condition.text(";"));
        });
        writer.write("");
        writer.write("@$T", OVERRIDE);
        writer.openBlock("public $T toString() {", "}", STRING, () -> {
            Fragment render = Fragment.of("return ").type(TO_STRING).text(".builder(").string(name).text(")");
            for (ModelTypePlan.Member member : fields) {
                render.text("\n        .add(").string(member.name()).text(", ").code(member.stringValue()).text(")");
            }
            write(writer, render.text("\n        .build();"));
        });
    }

    private static void renderBuilderInterface(AwsJavaWriter writer, ModelTypePlan plan) {
        String name = plan.type().getName();
        writer.write("/** Mutable builder of {@link $L}; not thread safe. */", name);
        String extended = plan.isError() ? "$T.Builder, $T<Builder, $L>, $T<$L>" : "$T<Builder, $L>, $T<$L>";
        Object[] args = plan.isError()
                        ? new Object[] {plan.serviceException(), COPYABLE_BUILDER, name, SHAPE_BUILDER, name}
                        : new Object[] {COPYABLE_BUILDER, name, SHAPE_BUILDER, name};
        writer.openBlock("public interface Builder extends " + extended + " {", "}", args, () -> {
            for (ModelTypePlan.Member member : plan.members()) {
                for (ModelTypePlan.Method setter : member.setters()) {
                    writer.write("Builder " + template(setter.signature()) + ";", setter.signature().types().toArray());
                    writer.write("");
                }
            }
            if (plan.isError()) {
                errorBuilderOverrides(writer, "Builder", null);
            }
            writer.write("@$T", OVERRIDE);
            writer.write("Builder deserialize($T deserializer);", SHAPE_DESERIALIZER);
            writer.write("");
            writer.write("@$T", OVERRIDE);
            writer.write("Builder deserializeMember($T deserializer, $T member);", SHAPE_DESERIALIZER, SCHEMA);
            writer.write("");
            writer.write("@$T", OVERRIDE);
            writer.write("Builder errorCorrection();");
            writer.write("");
            writer.write("/**");
            boolean collections = plan.members().stream().anyMatch(m -> !m.snapshot().types().isEmpty());
            writer.write(collections ? " * Builds the $L, taking a deep immutable snapshot of every collection."
                                     : " * Builds the $L.", name);
            if (plan.members().stream().anyMatch(ModelTypePlan.Member::required)) {
                writer.write(" *");
                writer.write(" * @throws $T listing every required member that is missing", MISSING_REQUIRED_MEMBERS);
            }
            writer.write(" */");
            writer.write("@$T", OVERRIDE);
            writer.write("$L build();", name);
        });
    }

    /** The covariant overrides of the service exception's builder methods; bodies if {@code impl} is set. */
    private static void errorBuilderOverrides(AwsJavaWriter writer, String returnType, String impl) {
        String[][] methods = {
            {"awsErrorDetails", "$T awsErrorDetails", "awsErrorDetails"},
            {"message", "$T message", "message"},
            {"requestId", "$T requestId", "requestId"},
            {"extendedRequestId", "$T extendedRequestId", "extendedRequestId"},
            {"statusCode", "int statusCode", "statusCode"},
            {"cause", "$T cause", "cause"},
            {"writableStackTrace", "$T writableStackTrace", "writableStackTrace"},
            {"numAttempts", "$T numAttempts", "numAttempts"}};
        for (String[] method : methods) {
            Symbol parameter = switch (method[0]) {
                case "awsErrorDetails" -> AWS_ERROR_DETAILS;
                case "cause" -> ModelSymbols.type("java.lang", "Throwable");
                case "writableStackTrace" -> ModelSymbols.type("java.lang", "Boolean");
                case "numAttempts" -> INTEGER;
                case "statusCode" -> null;
                default -> STRING;
            };
            Object[] args = parameter == null ? new Object[0] : new Object[] {parameter};
            writer.write("@$T", OVERRIDE);
            if (impl == null) {
                writer.write(returnType + " " + method[0] + "(" + method[1] + ");", args);
            } else {
                writer.openBlock("public " + returnType + " " + method[0] + "(" + method[1] + ") {", "}", args, () -> {
                    writer.write("this.$L = $L;", method[2], method[2]);
                    writer.write("return this;");
                });
            }
            writer.write("");
        }
    }

    private static void renderBuilderImpl(AwsJavaWriter writer, ModelTypePlan plan) {
        String name = plan.type().getName();
        String header = plan.isError() ? "static final class BuilderImpl extends $T.BuilderImpl implements Builder {"
                                       : "static final class BuilderImpl implements Builder {";
        Object[] headerArgs = plan.isError() ? new Object[] {plan.serviceException()} : new Object[0];
        writer.openBlock(header, "}", headerArgs, () -> {
            boolean fields = false;
            for (ModelTypePlan.Member member : plan.members()) {
                if (member.hasField() || member.eventStream()) {
                    Fragment field = Fragment.of("private ").code(member.builderFieldType())
                                             .text(" " + member.javaName());
                    if (!member.defaultValue().isEmpty()) {
                        field.text(" = ").code(member.defaultValue());
                    }
                    write(writer, field.text(";"));
                    fields = true;
                }
            }
            for (ModelTypePlan.Member member : plan.members()) {
                if (member.correctsToNull()) {
                    writer.write("private boolean $L;", member.errorCorrectedFlag());
                }
            }
            if (fields) {
                writer.write("");
            }
            writer.openBlock("private BuilderImpl() {", "}", () -> {
            });
            writer.write("");
            writer.openBlock("private BuilderImpl($L model) {", "}", name, () -> {
                if (plan.isError()) {
                    writer.write("super(model);");
                }
                for (ModelTypePlan.Member member : plan.members()) {
                    if (member.hasField() || member.eventStream()) {
                        writer.write("this.$L = model.$L;", member.javaName(), member.javaName());
                    }
                }
                for (ModelTypePlan.Member member : plan.members()) {
                    if (member.correctsToNull()) {
                        // A built value holds null here only if error correction made the member present.
                        writer.write("this.$L = model.$L == null;", member.errorCorrectedFlag(), member.javaName());
                    }
                }
            });
            for (ModelTypePlan.Member member : plan.members()) {
                for (ModelTypePlan.Method setter : member.setters()) {
                    writer.write("");
                    writer.write("@$T", OVERRIDE);
                    if (setter.safeVarargs()) {
                        writer.write("@$T", SAFE_VARARGS);
                    }
                    writer.openBlock("public final Builder " + template(setter.signature()) + " {", "}",
                                     setter.signature().types().toArray(), () -> {
                            write(writer, setter.body());
                            writer.write("return this;");
                        });
                }
            }
            writer.write("");
            if (plan.isError()) {
                errorBuilderOverrides(writer, "BuilderImpl", "impl");
            }
            writer.write("@$T", OVERRIDE);
            writer.openBlock("public $T schema() {", "}", SCHEMA, () -> writer.write("return SCHEMA;"));
            writer.write("");
            writer.write("@$T", OVERRIDE);
            writer.openBlock("public Builder deserialize($T deserializer) {", "}", SHAPE_DESERIALIZER, () -> {
                writer.write("deserializer.readStruct(SCHEMA, this, MemberReader.INSTANCE);");
                writer.write("return this;");
            });
            writer.write("");
            writer.write("@$T", OVERRIDE);
            writer.openBlock("public Builder deserializeMember($T deserializer, $T member) {", "}",
                             SHAPE_DESERIALIZER, SCHEMA, () -> {
                    writer.write("deserializer.readStruct(member.assertMemberTargetIs(SCHEMA), this, "
                                 + "MemberReader.INSTANCE);");
                    writer.write("return this;");
                });
            writer.write("");
            renderSetMemberValue(writer, plan);
            writer.write("");
            writer.write("@$T", OVERRIDE);
            writer.openBlock("public Builder errorCorrection() {", "}", () -> {
                for (ModelTypePlan.Member member : plan.members()) {
                    if (!member.zeroValue().isEmpty()) {
                        writer.openBlock("if (this.$L == null) {", "}", member.javaName(),
                                         () -> write(writer, Fragment.of("this." + member.javaName() + " = ")
                                                                     .code(member.zeroValue()).text(";")));
                    } else if (member.correctsToNull()) {
                        // No zero value: the member stays null and is treated as present, as in Smithy Java.
                        writer.openBlock("if (this.$L == null) {", "}", member.javaName(),
                                         () -> writer.write("this.$L = true;", member.errorCorrectedFlag()));
                    }
                }
                writer.write("return this;");
            });
            writer.write("");
            writer.write("@$T", OVERRIDE);
            writer.openBlock("public $L build() {", "}", name, () -> writer.write("return new $L(this);", name));
        });
    }

    private static void renderSetMemberValue(AwsJavaWriter writer, ModelTypePlan plan) {
        writer.write("@$T", OVERRIDE);
        writer.write("@$T(\"unchecked\")", SUPPRESS_WARNINGS);
        writer.openBlock("public void setMemberValue($T member, $T value) {", "}", SCHEMA, OBJECT, () -> {
            writer.openBlock("switch (member.memberIndex()) {", "}", () -> {
                for (ModelTypePlan.Member member : sortedByIndex(plan.members())) {
                    String validated = "$T.validateSameMember(" + member.schemaField() + ", member, value)";
                    switch (member.state()) {
                        case ModelTypePlan.Member.VALUE -> write(writer, Fragment.of("case " + member.index()
                                                                                     + " -> this." + member.javaName()
                                                                                     + " = (")
                                                                                 .code(member.builderFieldType())
                                                                                 .text(") ").type(SCHEMA_UTILS)
                                                                                 .text(".validateSameMember("
                                                                                       + member.schemaField()
                                                                                       + ", member, value);"));
                        case ModelTypePlan.Member.MESSAGE -> writer.write("case $L -> message(($T) " + validated
                                                                          + ");", member.index(), STRING,
                                                                          SCHEMA_UTILS);
                        case ModelTypePlan.Member.EVENT_STREAM -> write(writer, Fragment.of(
                                "case " + member.index() + " -> this." + member.javaName() + " = (")
                            .code(member.builderFieldType()).text(") ").type(SCHEMA_UTILS)
                            .text(".validateSameMember(" + member.schemaField() + ", member, value);"));
                        default -> writer.write("case $L -> throw new $T(\"Streaming member \" + member.id() + \" is "
                                                + "bound to the call, not held by \" + ID);", member.index(),
                                                UNSUPPORTED_OPERATION);
                    }
                }
                writer.write("default -> throw new $T(\"Attempted to set a non-existent member of \" + ID + \": \" + "
                             + "member.id());", ILLEGAL_ARGUMENT);
            });
        });
    }

    private static void renderMemberReader(AwsJavaWriter writer, ModelTypePlan plan) {
        writer.write("/** Assigns each deserialized member directly to the builder. */");
        writer.openBlock("private static final class MemberReader implements $T.StructMemberConsumer<BuilderImpl> {",
                         "}", SHAPE_DESERIALIZER, () -> {
                writer.write("private static final MemberReader INSTANCE = new MemberReader();");
                writer.write("");
                writer.write("@$T", OVERRIDE);
                writer.openBlock("public void accept(BuilderImpl builder, $T member, $T de) {", "}", SCHEMA,
                                 SHAPE_DESERIALIZER, () -> writer.openBlock("switch (member.memberIndex()) {", "}", () -> {
                                         for (ModelTypePlan.Member member : sortedByIndex(plan.members())) {
                                             if (member.read().isEmpty()) {
                                                 writer.write("case $L -> throw new $T(\"Streaming member \" + "
                                                              + "member.id() + \" is bound to the call, not held by \" "
                                                              + "+ ID);", member.index(), UNSUPPORTED_OPERATION);
                                             } else {
                                                 write(writer, Fragment.of("case " + member.index() + " -> ")
                                                                       .code(member.read()));
                                             }
                                         }
                                         writer.write("default -> throw new $T(\"Unexpected member of \" + ID + \": \" "
                                                      + "+ member.id());", ILLEGAL_ARGUMENT);
                                     }));
            });
    }

    private static List<ModelTypePlan.Member> sortedByIndex(List<ModelTypePlan.Member> members) {
        List<ModelTypePlan.Member> sorted = new ArrayList<>(members);
        sorted.sort((a, b) -> Integer.compare(a.index(), b.index()));
        return sorted;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Enums.

    static void renderEnum(EnumTypePlan plan, WriterDelegator<AwsJavaWriter> writers) {
        Symbol type = plan.type();
        String name = type.getName();
        Symbol valueType = plan.intEnum() ? INTEGER : STRING;
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type, VALUE_MAP);
            writer.write("/**");
            writer.write(" * The modeled {@code $L} $L. Values unknown to this SDK version read as",
                         plan.shape(), plan.intEnum() ? "intEnum" : "enum");
            writer.write(" * {@link #UNKNOWN_TO_SDK_VERSION}; the modeled value stays available from the containing "
                         + "type.");
            writer.write(" */");
            writer.openBlock("public enum $L {", "}", name, () -> {
                for (EnumTypePlan.Constant constant : plan.constants()) {
                    writer.write("$L($L),", constant.name(), constant.value());
                }
                writer.write("UNKNOWN_TO_SDK_VERSION(null);");
                writer.write("");
                writer.write("/** Absolute ID of the modeled shape. */");
                writer.write("public static final $T ID = $T.from($S);", SHAPE_ID, SHAPE_ID, plan.shape().toString());
                writer.write("");
                writer.write("/** The one schema of this type. */");
                writer.write("public static final $T SCHEMA = $T.$L;", SCHEMA, plan.schemaHolder(),
                             plan.schemaConstant());
                writer.write("");
                writer.write("private final $T value;", valueType);
                writer.write("");
                writer.openBlock("$L($T value) {", "}", name, valueType, () -> writer.write("this.value = value;"));
                writer.write("");
                if (plan.intEnum()) {
                    writer.write("/** The modeled value; {@code null} for {@link #UNKNOWN_TO_SDK_VERSION}. */");
                    writer.openBlock("public $T value() {", "}", INTEGER, () -> writer.write("return value;"));
                    writer.write("");
                }
                writer.write("/** The modeled value, or {@code \"null\"} for {@link #UNKNOWN_TO_SDK_VERSION}. */");
                writer.write("@$T", OVERRIDE);
                writer.openBlock("public $T toString() {", "}", STRING,
                                 () -> writer.write("return $T.valueOf(value);", STRING));
                writer.write("");
                writer.write("/**");
                writer.write(" * The constant with modeled value {@code value}, {@link #UNKNOWN_TO_SDK_VERSION} if no "
                             + "constant has it, or");
                writer.write(" * {@code null} if {@code value} is {@code null}.");
                writer.write(" */");
                writer.openBlock("public static $L fromValue($T value) {", "}", name, valueType, () -> {
                    writer.openBlock("if (value == null) {", "}", () -> writer.write("return null;"));
                    writer.write("return ValueMap.VALUES.getOrDefault(value, UNKNOWN_TO_SDK_VERSION);");
                });
                writer.write("");
                writer.write("/** Every constant except {@link #UNKNOWN_TO_SDK_VERSION}. */");
                writer.openBlock("public static $T<$L> knownValues() {", "}", SET, name, () -> {
                    writer.write("$T<$L> known = $T.allOf($L.class);", SET, name, ENUM_SET, name);
                    writer.write("known.remove(UNKNOWN_TO_SDK_VERSION);");
                    writer.write("return known;");
                });
                writer.write("");
                writer.write("/** Initialization-on-demand holder of the value index. */");
                writer.openBlock("private static final class ValueMap {", "}", () -> {
                    writer.write("static final $T<$T, $L> VALUES = create();", MAP, valueType, name);
                    writer.write("");
                    writer.openBlock("private ValueMap() {", "}", () -> {
                    });
                    writer.write("");
                    writer.openBlock("private static $T<$T, $L> create() {", "}", MAP, valueType, name, () -> {
                        writer.write("$T<$T, $L> values = new $T<>();", MAP, valueType, name, LINKED_HASH_MAP);
                        writer.openBlock("for ($L constant : values()) {", "}", name, () -> {
                            writer.openBlock("if (constant != UNKNOWN_TO_SDK_VERSION) {", "}",
                                             () -> writer.write("values.put(constant.value, constant);"));
                        });
                        writer.write("return values;");
                    });
                });
            });
        });
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Service exception.

    static void renderServiceException(ServiceExceptionPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        Symbol type = plan.type();
        String name = type.getName();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type, BUILDER, BUILDER_IMPL);
            writer.write("/**");
            writer.write(" * Base of every error of the {@code $L} service: its modeled errors, and errors the "
                         + "service returns that", plan.service());
            writer.write(" * this SDK version does not model.");
            writer.write(" */");
            writer.openBlock("public class $L extends $T {", "}", name, AWS_SERVICE_EXCEPTION, () -> {
                writer.write("private static final long serialVersionUID = 1L;");
                writer.write("");
                writer.openBlock("protected $L(Builder builder) {", "}", name, () -> writer.write("super(builder);"));
                writer.write("");
                writer.openBlock("public static Builder builder() {", "}", () -> writer.write("return new BuilderImpl();"));
                writer.write("");
                writer.write("@$T", OVERRIDE);
                writer.openBlock("public Builder toBuilder() {", "}", () -> writer.write("return new BuilderImpl(this);"));
                writer.write("");
                writer.write("/** Builder of a {@link $L}. */", name);
                writer.openBlock("public interface Builder extends $T.Builder {", "}", AWS_SERVICE_EXCEPTION, () -> {
                    errorBuilderOverrides(writer, "Builder", null);
                    writer.write("@$T", OVERRIDE);
                    writer.write("$L build();", name);
                });
                writer.write("");
                writer.openBlock("protected static class BuilderImpl extends $T.BuilderImpl implements Builder {", "}",
                                 AWS_SERVICE_EXCEPTION, () -> {
                        writer.openBlock("protected BuilderImpl() {", "}", () -> {
                        });
                        writer.write("");
                        writer.openBlock("protected BuilderImpl($L ex) {", "}", name, () -> writer.write("super(ex);"));
                        writer.write("");
                        errorBuilderOverrides(writer, "BuilderImpl", "impl");
                        writer.write("@$T", OVERRIDE);
                        writer.openBlock("public $L build() {", "}", name, () -> writer.write("return new $L(this);", name));
                    });
            });
        });
    }

    // ---------------------------------------------------------------------------------------------------------------

    private static String template(ModelTypePlan.Code code) {
        return code.template();
    }

    private static void write(AwsJavaWriter writer, ModelTypePlan.Code code) {
        if (!code.isEmpty()) {
            writer.write(code.template(), code.types().toArray());
        }
    }

    private static void write(AwsJavaWriter writer, Fragment fragment) {
        write(writer, fragment.build());
    }
}
