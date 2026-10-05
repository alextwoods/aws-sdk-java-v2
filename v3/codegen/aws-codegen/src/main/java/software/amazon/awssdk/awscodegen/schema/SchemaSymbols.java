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

package software.amazon.awssdk.awscodegen.schema;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.SourceVersion;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.CaseUtils;

/**
 * Names of generated schemas, shared by the schema emitters and by the other emitters that reference a schema (for
 * example operation descriptors), so that every file computes the same reference without a shared service-wide table.
 *
 * <p>The exported schema of a shape is the {@code public static final} constant {@link #constantName} of its
 * contextual (service-renamed) name in the service's schema holder, {@code <root>.model.internal.<Service>Schemas}.
 * Exported schemas are the service schema, one operation schema per operation, and one schema per public model type
 * (structure, union, enum and intEnum, including requests, responses and errors). Shapes of the Smithy prelude are not
 * generated: members target Smithy Java's shared {@code PreludeSchemas} constants and {@code Unit.SCHEMA}.
 */
public final class SchemaSymbols {

    private static final String PRELUDE_NAMESPACE = "smithy.api";

    /** Prelude shapes by name, and the Smithy Java constant generated code references for each. */
    private static final Map<String, Reference> PRELUDE = Map.ofEntries(
        prelude("Blob", "BLOB"), prelude("Boolean", "BOOLEAN"), prelude("String", "STRING"),
        prelude("Timestamp", "TIMESTAMP"), prelude("Byte", "BYTE"), prelude("Short", "SHORT"),
        prelude("Integer", "INTEGER"), prelude("Long", "LONG"), prelude("Float", "FLOAT"), prelude("Double", "DOUBLE"),
        prelude("BigInteger", "BIG_INTEGER"), prelude("BigDecimal", "BIG_DECIMAL"), prelude("Document", "DOCUMENT"),
        prelude("PrimitiveBoolean", "PRIMITIVE_BOOLEAN"), prelude("PrimitiveByte", "PRIMITIVE_BYTE"),
        prelude("PrimitiveShort", "PRIMITIVE_SHORT"), prelude("PrimitiveInteger", "PRIMITIVE_INTEGER"),
        prelude("PrimitiveLong", "PRIMITIVE_LONG"), prelude("PrimitiveFloat", "PRIMITIVE_FLOAT"),
        prelude("PrimitiveDouble", "PRIMITIVE_DOUBLE"),
        Map.entry("Unit", new Reference(SchemaRuntimeSymbols.UNIT, "SCHEMA")));

    private SchemaSymbols() {
    }

    /**
     * A static field that holds a schema.
     *
     * @param owner the class declaring the field
     * @param field the field name
     */
    public record Reference(Symbol owner, String field) {
    }

    /**
     * The service's schema holder, {@code <packageName>.model.internal.<serviceTypeName>Schemas}.
     *
     * @param packageName root package of the generated service
     * @param serviceTypeName base Java type name of the service
     */
    public static Symbol holder(String packageName, String serviceTypeName) {
        String namespace = packageName + ".model.internal";
        String name = serviceTypeName + "Schemas";
        return Symbol.builder()
                     .namespace(namespace, ".")
                     .name(name)
                     .definitionFile(namespace.replace('.', '/') + "/" + name + ".java")
                     .build();
    }

    /**
     * The exported schema of the service, an operation or a public model type in {@code holder}, or the Smithy Java
     * constant of a prelude shape.
     */
    public static Reference exported(ShapeId shape, ServiceShape service, Symbol holder) {
        return prelude(shape).orElseGet(() -> new Reference(holder, constantName(shape.getName(service))));
    }

    /** The Smithy Java constant generated code references for a prelude shape, if it has one. */
    static Optional<Reference> prelude(ShapeId shape) {
        if (!shape.getNamespace().equals(PRELUDE_NAMESPACE) || shape.hasMember()) {
            return Optional.empty();
        }
        return Optional.ofNullable(PRELUDE.get(shape.getName()));
    }

    /**
     * The Java constant name for a contextual shape name: {@code GetObject} becomes {@code GET_OBJECT} and
     * {@code ListObjectsV2} becomes {@code LIST_OBJECTS_V2}.
     *
     * @throws AwsCodegenException if the name does not map to a Java constant name
     */
    public static String constantName(String name) {
        String constant = CaseUtils.toSnakeCase(name).toUpperCase(Locale.ROOT);
        if (!SourceVersion.isIdentifier(constant) || SourceVersion.isKeyword(constant, SourceVersion.RELEASE_21)) {
            throw new AwsCodegenException("Shape name " + name + " does not map to a Java constant name.");
        }
        return constant;
    }

    /**
     * A Java string literal of {@code value} in ASCII, as every generated schema writes it: quotes, backslashes and
     * control characters escaped, and non-ASCII characters written as Unicode escapes.
     */
    public static String javaString(String value) {
        return JavaCode.stringLiteral(value);
    }

    private static Map.Entry<String, Reference> prelude(String name, String constant) {
        return Map.entry(name, new Reference(SchemaRuntimeSymbols.PRELUDE_SCHEMAS, constant));
    }
}
