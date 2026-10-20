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

package software.amazon.awssdk.sizereport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Assigns each class file of a service JAR to one {@link SizeCategory} (Requirement 36.2), and carves the
 * exception-mapping methods and the registry fields out of the classes that hold them.
 *
 * <p>A <em>service package</em> is a package that contains a {@code Default<Prefix>Client} or
 * {@code Default<Prefix>AsyncClient} class; {@code <Prefix>} is the service's class-name prefix (for example
 * {@code CloudWatch}). A class belongs to the longest service package that contains it. With {@code S} the class's
 * top-level simple name (the name before the first {@code $}) and {@code sub} the first package segment below the
 * service package, the first matching rule gives the category:
 * <ol>
 *     <li>Removed marshallers and unmarshallers: {@code sub} is {@code transform}, or {@code S} ends with
 *     {@code Marshaller} or {@code Unmarshaller}.</li>
 *     <li>Schemas: {@code S} is {@code <Prefix>Schemas*}, {@code <Prefix>ServiceSchemas*} or
 *     {@code <Prefix>SchemaIndex}.</li>
 *     <li>Type_Registries: {@code S} is {@code <Prefix>*TypeRegistry} or {@code <Prefix>*ErrorRegistry}.</li>
 *     <li>Client classes: the class is in the service package itself (clients, client builders, service client
 *     configuration), {@code sub} is {@code paginators}, {@code waiters} or {@code batchmanager}, or {@code S} is
 *     {@code <Prefix>Operations} (the Api_Operations shared by both clients).</li>
 *     <li>Generated_Model_Class classes: {@code sub} is {@code model} and {@code S} is neither {@code *Copier} nor
 *     {@code <Prefix>SharedSerde} (structures, unions, exceptions, their builders, enums, and their nested
 *     classes).</li>
 *     <li>Other: everything else (endpoints, auth, {@code internal}, {@code jmespath}, {@code *Copier},
 *     {@code <Prefix>SharedSerde}), and classes outside every service package.</li>
 * </ol>
 *
 * <p>Two categories are measured inside classes by bytecode attribution ({@link MemberCarver}):
 * <ul>
 *     <li>Type_Registries: every static field of type {@code ErrorRegistry} or {@code TypeRegistry} (package
 *     {@code software.amazon.awssdk.schema.serde}) in a class of another category, with the {@code <clinit>} code
 *     that builds it and the lambdas that code creates (in practice {@code <Prefix>Operations.ERROR_REGISTRY}).</li>
 *     <li>Removed exception-mapping code: in client classes, every method whose descriptor or generic signature
 *     mentions {@code software.amazon.awssdk.protocols.core.ExceptionMetadata}, whose code uses that class or its
 *     builder, or that is a {@code lambda$*(String)Optional} (the per-operation error-code mapper), plus the lambdas
 *     those methods create.</li>
 * </ul>
 * The rest of such a class stays in its own category, so every byte is counted exactly once.
 */
@SdkInternalApi
final class ClassAttributor {
    static final String EXCEPTION_METADATA = "software/amazon/awssdk/protocols/core/ExceptionMetadata";
    static final String ERROR_REGISTRY = "Lsoftware/amazon/awssdk/schema/serde/ErrorRegistry;";
    static final String TYPE_REGISTRY = "Lsoftware/amazon/awssdk/schema/serde/TypeRegistry;";

    private static final Pattern DEFAULT_CLIENT = Pattern.compile("(.+)/Default(\\w+?)(Async)?Client");
    private static final Set<String> CLIENT_SUBPACKAGES =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList("paginators", "waiters", "batchmanager")));
    private static final String ERROR_MAPPER_LAMBDA_DESC = "(Ljava/lang/String;)Ljava/util/Optional;";

    /** Service package (internal form, for example {@code a/b/cloudwatch}) to class-name prefix, longest first. */
    private final Map<String, String> servicePackages;

    private ClassAttributor(Map<String, String> servicePackages) {
        this.servicePackages = servicePackages;
    }

    /**
     * Create an attributor for the classes of one JAR, finding its service packages from the class names.
     */
    public static ClassAttributor forClasses(Collection<String> classNames) {
        Map<String, String> found = new LinkedHashMap<>();
        for (String name : classNames) {
            Matcher matcher = DEFAULT_CLIENT.matcher(name);
            if (name.indexOf('$') < 0 && matcher.matches()) {
                found.putIfAbsent(matcher.group(1), matcher.group(2));
            }
        }
        List<String> packages = new ArrayList<>(found.keySet());
        packages.sort((a, b) -> b.length() != a.length() ? b.length() - a.length() : a.compareTo(b));
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String p : packages) {
            ordered.put(p, found.get(p));
        }
        return new ClassAttributor(ordered);
    }

    /**
     * The service packages found, in internal form, longest first.
     */
    public List<String> servicePackages() {
        return new ArrayList<>(servicePackages.keySet());
    }

    /**
     * Attribute one class file.
     *
     * @param className the internal class name
     * @param classBytes the class file
     */
    public ClassAttribution attribute(String className, byte[] classBytes) {
        SizeCategory category = category(className);
        List<ClassAttribution.Carve> carves = new ArrayList<>();
        if (category != SizeCategory.TYPE_REGISTRY) {
            ClassNode node = MemberCarver.read(classBytes);
            Set<String> carved = new HashSet<>();
            Set<String> registryFields = new LinkedHashSet<>();
            for (FieldNode field : node.fields) {
                if (MemberCarver.isStatic(field) && isRegistryType(field.desc)) {
                    registryFields.add(field.name);
                }
            }
            if (!registryFields.isEmpty()) {
                add(carves, carved, MemberCarver.carve(classBytes, SizeCategory.TYPE_REGISTRY, Collections.emptySet(),
                                                       registryFields, carved));
            }
            if (category == SizeCategory.CLIENT) {
                Set<String> mappers = new LinkedHashSet<>();
                for (MethodNode method : node.methods) {
                    if (isExceptionMapping(method)) {
                        mappers.add(method.name + method.desc);
                    }
                }
                if (!mappers.isEmpty()) {
                    add(carves, carved, MemberCarver.carve(classBytes, SizeCategory.EXCEPTION_MAPPING, mappers,
                                                           Collections.emptySet(), carved));
                }
            }
        }
        return new ClassAttribution(className, classBytes.length, category, carves);
    }

    /**
     * The category of a class by name (rules 1-6 of the class documentation).
     */
    public SizeCategory category(String className) {
        Location location = locate(className);
        return location == null ? SizeCategory.OTHER : category(location.prefix(), location.sub(), location.outer());
    }

    /**
     * Where a class sits relative to its service package, or {@code null} if it is outside every service package.
     */
    Location locate(String className) {
        int slash = className.lastIndexOf('/');
        String pkg = slash < 0 ? "" : className.substring(0, slash);
        String simple = className.substring(slash + 1);
        int dollar = simple.indexOf('$');
        String outer = dollar < 0 ? simple : simple.substring(0, dollar);

        for (Map.Entry<String, String> entry : servicePackages.entrySet()) {
            String servicePackage = entry.getKey();
            if (!pkg.equals(servicePackage) && !pkg.startsWith(servicePackage + "/")) {
                continue;
            }
            String sub = pkg.length() == servicePackage.length() ? "" : pkg.substring(servicePackage.length() + 1);
            int subSlash = sub.indexOf('/');
            String top = subSlash < 0 ? sub : sub.substring(0, subSlash);
            return new Location(entry.getValue(), top, outer, dollar >= 0);
        }
        return null;
    }

    /**
     * A class in a service package: the service's class-name prefix, the first package segment below the service
     * package ({@code sub}, empty for the service package itself), and the class's top-level simple name
     * ({@code S}).
     */
    static final class Location {
        private final String prefix;
        private final String sub;
        private final String outer;
        private final boolean nested;

        Location(String prefix, String sub, String outer, boolean nested) {
            this.prefix = prefix;
            this.sub = sub;
            this.outer = outer;
            this.nested = nested;
        }

        String prefix() {
            return prefix;
        }

        String sub() {
            return sub;
        }

        String outer() {
            return outer;
        }

        /**
         * Whether the class is a nested, inner, local or anonymous class ({@code $} in its simple name).
         */
        boolean nested() {
            return nested;
        }
    }

    private static SizeCategory category(String prefix, String top, String outer) {
        if ("transform".equals(top) || outer.endsWith("Marshaller") || outer.endsWith("Unmarshaller")) {
            return SizeCategory.MARSHALLER;
        }
        if (outer.startsWith(prefix)) {
            String rest = outer.substring(prefix.length());
            if (rest.startsWith("Schemas") || rest.startsWith("ServiceSchemas") || "SchemaIndex".equals(rest)) {
                return SizeCategory.SCHEMAS;
            }
            if (rest.endsWith("TypeRegistry") || rest.endsWith("ErrorRegistry")) {
                return SizeCategory.TYPE_REGISTRY;
            }
        }
        if (top.isEmpty() || CLIENT_SUBPACKAGES.contains(top) || outer.equals(prefix + "Operations")) {
            return SizeCategory.CLIENT;
        }
        if ("model".equals(top) && !outer.endsWith("Copier") && !outer.equals(prefix + "SharedSerde")) {
            return SizeCategory.GENERATED_MODEL_CLASS;
        }
        return SizeCategory.OTHER;
    }

    private static void add(List<ClassAttribution.Carve> carves, Set<String> carved, ClassAttribution.Carve carve) {
        if (carve != null) {
            carves.add(carve);
            carved.addAll(carve.methods());
        }
    }

    private static boolean isRegistryType(String descriptor) {
        return ERROR_REGISTRY.equals(descriptor) || TYPE_REGISTRY.equals(descriptor);
    }

    private static boolean isExceptionMapping(MethodNode method) {
        if (mentionsExceptionMetadata(method.desc) || mentionsExceptionMetadata(method.signature)) {
            return true;
        }
        if ((method.access & Opcodes.ACC_SYNTHETIC) != 0 && method.name.startsWith("lambda$")
            && ERROR_MAPPER_LAMBDA_DESC.equals(method.desc)) {
            return true;
        }
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            String owner = null;
            if (insn instanceof MethodInsnNode) {
                owner = ((MethodInsnNode) insn).owner;
            } else if (insn instanceof FieldInsnNode) {
                owner = ((FieldInsnNode) insn).owner;
            } else if (insn instanceof TypeInsnNode) {
                owner = ((TypeInsnNode) insn).desc;
            }
            if (owner != null && (owner.equals(EXCEPTION_METADATA) || owner.startsWith(EXCEPTION_METADATA + "$"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean mentionsExceptionMetadata(String descriptorOrSignature) {
        return descriptorOrSignature != null
               && (descriptorOrSignature.contains("L" + EXCEPTION_METADATA + ";")
                   || descriptorOrSignature.contains("L" + EXCEPTION_METADATA + "$")
                   || descriptorOrSignature.contains("L" + EXCEPTION_METADATA + "<"));
    }
}
