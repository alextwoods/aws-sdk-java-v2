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

package software.amazon.awssdk.awscodegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.lang.model.SourceVersion;
import software.amazon.awssdk.awscodegen.model.ModelTypeGenerator;
import software.amazon.awssdk.awscodegen.schema.SchemaSymbols;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.utils.StringUtils;

/**
 * Maps Smithy shapes of the generated service to Java symbols.
 *
 * <p>Every answer is computed from the shape and the {@link Model}; the provider holds no copy of model content.
 * {@code CodegenDirector} wraps it in Smithy's caching provider after integrations decorate it.
 *
 * <ul>
 *   <li>the generated service: the package-private {@value AwsCodegen#SERVICE_INFO_TYPE} identity type in the root
 *       package (until the client surface owns the service symbol);</li>
 *   <li>structures, unions, enums and intEnums: {@code <root>.model.<Name>}, with V2 names for structures: modeled
 *       errors are {@code <Name>Exception} and the input and output of one operation are
 *       {@code <Operation>Request} and {@code <Operation>Response}; {@code smithy.api#Unit} is Smithy Java's
 *       {@code Unit};</li>
 *   <li>operations: {@code <root>.internal.operation.<Name>Operation} (static descriptors, referenced from the
 *       {@code <root>.internal.operation.<Service>Operations} table, see {@link #operationTable});</li>
 *   <li>resources: a name-only symbol in the model package with no definition file (resources are not types);</li>
 *   <li>simple, list and map shapes: the corresponding JDK or runtime type ({@code SdkBytes} for blobs, Smithy
 *       Java's {@code Document} for documents);</li>
 *   <li>members: the symbol of their target; member names follow V2 (leading capitals lower-cased, and a
 *       {@code Value} suffix for Java keywords and names that clash with generated or inherited methods).</li>
 * </ul>
 *
 * <p>Names honor the service's {@code rename} map, so shapes with the same name in different namespaces get the
 * contextual names Smithy validated as unique for this service.
 */
final class AwsSymbolProvider implements SymbolProvider {

    private static final String JAVA_LANG = "java.lang";
    private static final String EXCEPTION_SUFFIX = "Exception";
    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    /**
     * Accessor names every generated type declares or inherits: V2's reserved structure names ({@code Object}
     * methods, {@code builder}, {@code toBuilder}, {@code sdkFields}, {@code overrideConfiguration},
     * {@code sdkHttpResponse}) plus the {@code SerializableStruct} methods and the response metadata accessors
     * ({@code responseMetadata}, {@code sdkResponseMetadata}). A member with one of these names gets a {@code Value}
     * suffix, as in V2.
     */
    private static final Set<String> RESERVED_NAMES = Set.of(
        "equals", "finalize", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait", "builder",
        "sdkFields", "toBuilder", "overrideConfiguration", "sdkHttpResponse", "copy", "schema", "serialize",
        "serializeMembers", "getMemberValue", "responseMetadata", "sdkResponseMetadata");

    /** Accessor names of generated errors: V2's reserved exception names plus the {@code SerializableStruct} ones. */
    private static final Set<String> RESERVED_EXCEPTION_NAMES = Set.of(
        "equals", "finalize", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait", "builder",
        "sdkFields", "toBuilder", "copy", "awsErrorDetails", "cause", "fillInStackTrace", "getCause",
        "getLocalizedMessage", "getMessage", "getStackTrace", "getSuppressed", "isClockSkewException",
        "isThrottlingException", "isRetryableException", "printStackTrace", "requestId", "extendedRequestId",
        "retryable", "serializableBuilderClass", "statusCode", "numAttempts", "rawMessage", "addSuppressed",
        "initCause", "setStackTrace", "schema", "serialize", "serializeMembers", "getMemberValue",
        "sdkResponseMetadata");

    private final Model model;
    private final ServiceShape service;
    private final String rootPackage;
    private final String modelPackage;
    private final String operationPackage;

    AwsSymbolProvider(Model model, ServiceShape service, AwsCodegenSettings settings) {
        this.model = model;
        this.service = service;
        this.rootPackage = settings.packageName();
        this.modelPackage = rootPackage + ".model";
        this.operationPackage = operationPackage(settings);
    }

    /** Package of the static operation descriptors and the service's operation table. */
    static String operationPackage(AwsCodegenSettings settings) {
        return settings.packageName() + ".internal.operation";
    }

    /**
     * The public {@code <Service>Operations} static operation table in the {@linkplain #operationPackage operation
     * package}. Descriptor classes end in {@code Operation}, so the table name never collides with one of them.
     */
    static Symbol operationTable(ServiceShape service, AwsCodegenSettings settings) {
        return generatedType(operationPackage(settings), serviceTypeName(service) + "Operations");
    }

    /**
     * The service's schema holder {@code <root>.model.internal.<Service>Schemas}, which declares the exported schema of
     * the service, of every operation and of every public model type (see {@code SchemaGenerator}).
     */
    static Symbol schemaHolder(ServiceShape service, AwsCodegenSettings settings) {
        return SchemaSymbols.holder(settings.packageName(), serviceTypeName(service));
    }

    /**
     * The public {@code <root>.<Service>ServiceMetadata} class: advanced service metadata, the operations and the
     * primary type registry.
     */
    static Symbol serviceMetadata(ServiceShape service, AwsCodegenSettings settings) {
        return generatedType(settings.packageName(), serviceTypeName(service) + "ServiceMetadata");
    }

    /**
     * The public holder of the service's primary type registry, {@code <root>.model.internal.<Service>TypeRegistry}.
     * Its package-private partitions, if any, are {@code <Service>TypeRegistry1..n} in the same package.
     */
    static Symbol typeRegistry(ServiceShape service, AwsCodegenSettings settings) {
        return generatedType(settings.packageName() + ".model.internal", serviceTypeName(service) + "TypeRegistry");
    }

    /** Partition {@code number} (from 1) of {@code registry}. */
    static Symbol typeRegistryPartition(Symbol registry, int number) {
        return generatedType(registry.getNamespace(), registry.getName() + number);
    }

    /**
     * Base Java type name of the service, as V2 names it ({@code DynamoDbClient}, {@code S3Client}): the
     * {@code @aws.api#service} SDK ID in V2 Pascal case (each word lower-cased and capitalized, so {@code DynamoDB}
     * becomes {@code DynamoDb}) without a leading {@code Amazon} or {@code AWS} or a trailing {@code Service}. A
     * service without an SDK ID (a non-AWS model) is named after its shape.
     */
    static String serviceTypeName(ServiceShape service) {
        return service.getTrait(ServiceTrait.class)
                      .map(ServiceTrait::getSdkId)
                      .filter(sdkId -> !sdkId.isBlank())
                      .map(AwsSymbolProvider::v2ServiceName)
                      .orElseGet(() -> StringUtils.capitalize(service.getId().getName(service)));
    }

    /** V2's {@code DefaultNamingStrategy.getServiceName} of an SDK ID. */
    static String v2ServiceName(String sdkId) {
        StringBuilder name = new StringBuilder();
        for (String word : v2WordBoundaries(sdkId)) {
            name.append(StringUtils.capitalize(word.toLowerCase(Locale.ROOT)));
        }
        String base = removeLeading(removeLeading(name.toString(), "amazon"), "aws");
        return base.toLowerCase(Locale.ROOT).endsWith("service") ? base.substring(0, base.length() - "service".length())
                                                                 : base;
    }

    /** V2's {@code CodegenNamingUtils.splitOnWordBoundaries}. */
    private static String[] v2WordBoundaries(String value) {
        String result = value.replaceAll("[^A-Za-z0-9]+", " ");
        result = result.replaceAll("([^a-z]{2,})v([0-9]+)", "$1 v$2 ").replaceAll("([^A-Z]{2,})V([0-9]+)", "$1 V$2 ");
        result = String.join(" ", result.split("(?<=[a-z])(?=[A-Z]([a-zA-Z]|[0-9]))"));
        result = result.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        result = result.replaceAll("([0-9])([a-zA-Z])", "$1 $2");
        return result.replaceAll(" +", " ").trim().split(" ");
    }

    private static String removeLeading(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    /**
     * The generated base exception {@code <root>.model.<Service>Exception} of the service's modeled errors, as in V2.
     */
    static Symbol serviceException(ServiceShape service, AwsCodegenSettings settings) {
        return generatedType(settings.packageName() + ".model", serviceTypeName(service) + EXCEPTION_SUFFIX);
    }

    /**
     * The generated base {@code <root>.model.<Service>Response} of the service's operation outputs, as in V2: an
     * {@code AwsResponse} whose {@code responseMetadata()} is the service's {@link #serviceResponseMetadata}.
     */
    static Symbol serviceResponse(ServiceShape service, AwsCodegenSettings settings) {
        return generatedType(settings.packageName() + ".model", serviceTypeName(service) + "Response");
    }

    /** The generated {@code <root>.model.<Service>ResponseMetadata} V2-compatible metadata view, as in V2. */
    static Symbol serviceResponseMetadata(ServiceShape service, AwsCodegenSettings settings) {
        return generatedType(settings.packageName() + ".model", serviceTypeName(service) + "ResponseMetadata");
    }

    @Override
    public Symbol toSymbol(Shape shape) {
        return switch (shape.getType()) {
            case SERVICE -> serviceSymbol(shape);
            case OPERATION -> generatedType(operationPackage, typeName(shape) + "Operation");
            case RESOURCE -> Symbol.builder().namespace(modelPackage, ".").name(typeName(shape)).build();
            case STRUCTURE -> shape.getId().equals(UNIT)
                              ? runtimeType("software.amazon.smithy.java.core.schema", "Unit")
                              : generatedType(modelPackage, structureName(shape));
            case UNION, ENUM, INT_ENUM -> generatedType(modelPackage, typeName(shape));
            case MEMBER -> toSymbol(model.expectShape(((MemberShape) shape).getTarget()));
            case LIST, SET -> runtimeType("java.util", "List");
            case MAP -> runtimeType("java.util", "Map");
            case BLOB -> runtimeType("software.amazon.awssdk.core", "SdkBytes");
            case BOOLEAN -> runtimeType(JAVA_LANG, "Boolean");
            case STRING -> runtimeType(JAVA_LANG, "String");
            case BYTE -> runtimeType(JAVA_LANG, "Byte");
            case SHORT -> runtimeType(JAVA_LANG, "Short");
            case INTEGER -> runtimeType(JAVA_LANG, "Integer");
            case LONG -> runtimeType(JAVA_LANG, "Long");
            case FLOAT -> runtimeType(JAVA_LANG, "Float");
            case DOUBLE -> runtimeType(JAVA_LANG, "Double");
            case BIG_INTEGER -> runtimeType("java.math", "BigInteger");
            case BIG_DECIMAL -> runtimeType("java.math", "BigDecimal");
            case TIMESTAMP -> runtimeType("java.time", "Instant");
            case DOCUMENT -> runtimeType("software.amazon.smithy.java.core.serde.document", "Document");
            default -> throw new CodegenException("No Java symbol is defined for " + shape.getType() + " shape "
                                                  + shape.getId());
        };
    }

    /**
     * The member name as a Java identifier: the leading upper-case run is lower-cased (keeping the final capital of a
     * run that starts the next word, so {@code SSECustomerKey} becomes {@code sseCustomerKey}), and names that are
     * Java keywords or literals get a {@code Value} suffix.
     */
    @Override
    public String toMemberName(MemberShape shape) {
        String name = lowerLeadingCapitals(shape.getMemberName());
        if (!SourceVersion.isName(name, SourceVersion.RELEASE_21) || reserved(name, shape)) {
            return lowerLeadingCapitals(shape.getMemberName() + "Value");
        }
        return name;
    }

    /**
     * Whether {@code name} would clash with a method every generated type (or every generated error, or every union)
     * declares or inherits: V2's reserved names plus the {@code SerializableStruct} methods.
     */
    private boolean reserved(String name, MemberShape member) {
        Shape container = model.expectShape(member.getContainer());
        if (container.hasTrait(ErrorTrait.class)) {
            // The exception's builder owns message(String); only the error message itself (a string) may use it.
            return RESERVED_EXCEPTION_NAMES.contains(name)
                   || name.equals("message") && !ModelTypeGenerator.isErrorMessage(model, member);
        }
        return RESERVED_NAMES.contains(name) || container.isUnionShape() && name.equals("type");
    }

    /**
     * V2 class names of structures: a modeled error is {@code <Name>Exception} (a trailing {@code Fault} becomes
     * {@code Exception}; {@code Default} is prepended if that is the service exception's name), and the input or
     * output of exactly one operation is {@code <Operation>Request} or
     * {@code <Operation>Response}.
     */
    private String structureName(Shape shape) {
        String name = typeName(shape);
        if (shape.hasTrait(ErrorTrait.class)) {
            String base = name.endsWith("Fault") ? name.substring(0, name.length() - "Fault".length()) : name;
            String exception = base.endsWith(EXCEPTION_SUFFIX) ? base : base + EXCEPTION_SUFFIX;
            String serviceException = serviceTypeName(service) + EXCEPTION_SUFFIX;
            return exception.equals(serviceException) ? "Default" + exception : exception;
        }
        List<String> uses = new ArrayList<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            if (operation.getInputShape().equals(shape.getId())) {
                uses.add(operationTypeName(operation) + "Request");
            }
            if (operation.getOutputShape().equals(shape.getId())) {
                uses.add(operationTypeName(operation) + "Response");
            }
        }
        return uses.size() == 1 ? uses.get(0) : name;
    }

    private String operationTypeName(OperationShape operation) {
        String name = typeName(operation);
        return name.equals(serviceTypeName(service)) ? "Default" + name : name;
    }

    private Symbol serviceSymbol(Shape shape) {
        if (!shape.getId().equals(service.getId())) {
            throw new CodegenException("Service " + shape.getId() + " is not the generated service " + service.getId());
        }
        return generatedType(rootPackage, AwsCodegen.SERVICE_INFO_TYPE);
    }

    /** The contextual shape name, or the V2 name of a shape V2 renames ({@link ClientSurfaceClassifier}). */
    private String typeName(Shape shape) {
        String renamed = ClientSurfaceClassifier.V2_SHAPE_RENAMES.getOrDefault(service.getId(), Map.of())
                                                                 .get(shape.getId());
        if (renamed == null) {
            renamed = ClientSurfaceClassifier.COLLIDING_ERROR_RENAMES.getOrDefault(service.getId(), Map.of())
                                                                     .get(shape.getId());
        }
        if (renamed != null) {
            return renamed;
        }
        return StringUtils.capitalize(shape.getId().getName(service));
    }

    private static Symbol generatedType(String packageName, String name) {
        return Symbol.builder()
                     .namespace(packageName, ".")
                     .name(name)
                     .definitionFile(packageName.replace('.', '/') + "/" + name + ".java")
                     .build();
    }

    private static Symbol runtimeType(String packageName, String name) {
        return Symbol.builder().namespace(packageName, ".").name(name).build();
    }

    static String lowerLeadingCapitals(String name) {
        int run = 0;
        while (run < name.length() && Character.isUpperCase(name.charAt(run))) {
            run++;
        }
        if (run == 0) {
            return name;
        }
        // A run followed by a lower-case letter ends with the first capital of the next word ("SSECustomer").
        int lowered = run > 1 && run < name.length() && Character.isLowerCase(name.charAt(run)) ? run - 1 : run;
        return name.substring(0, lowered).toLowerCase(Locale.ROOT) + name.substring(lowered);
    }
}
