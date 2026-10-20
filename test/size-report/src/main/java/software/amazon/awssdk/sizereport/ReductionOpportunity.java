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

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A further size-reduction opportunity found during the prototype (Requirement 36.5), measured on a feature build
 * service JAR.
 *
 * <p>Each opportunity selects the code it would remove: whole classes, or members of classes. Whole classes are
 * measured exactly: their class-file bytes and the bytes their entries take in the JAR (compressed data plus local
 * and central headers). Members are carved out with {@link MemberCarver}; their compressed bytes are estimated as the
 * carved class-file bytes times the compression ratio of the class's own JAR entry. The opportunities select
 * disjoint code, so their estimates can be added.
 */
@SdkInternalApi
enum ReductionOpportunity {
    GENERIC_COLLECTION_SERDE(
        "Generic list and map serde in `schema` instead of a `<Prefix>SharedSerde$<Shape>Serde` class per collection "
        + "shape (a list or map serde parameterized by its member Schema and element reader/writer)",
        SizeCategory.OTHER, Estimate.FULL) {
        @Override
        boolean selectsClass(ClassAttributor.Location location, ClassNode node) {
            return location.nested() && location.outer().equals(location.prefix() + "SharedSerde");
        }
    },
    RUNTIME_SCHEMA_INDEX(
        "Build the `<Prefix>SchemaIndex` map at first use by walking the operation and error Schemas, instead of one "
        + "generated `put` per shape",
        SizeCategory.SCHEMAS, Estimate.FULL) {
        @Override
        boolean selectsClass(ClassAttributor.Location location, ClassNode node) {
            return location.outer().equals(location.prefix() + "SchemaIndex");
        }
    },
    INNER_DESERIALIZER_IN_BUILDER(
        "Let `BuilderImpl` implement `StructMemberConsumer` itself instead of a nested `InnerDeserializer` class per "
        + "structure (the class overhead goes; the `accept` methods, bridge included, move into the builder)",
        SizeCategory.GENERATED_MODEL_CLASS, Estimate.FULL) {
        @Override
        boolean selectsClass(ClassAttributor.Location location, ClassNode node) {
            return "model".equals(location.sub()) && node.name.endsWith("$BuilderImpl$InnerDeserializer");
        }

        @Override
        Set<String> keptMethods(ClassNode node) {
            Set<String> accept = new LinkedHashSet<>();
            for (MethodNode method : node.methods) {
                if ("accept".equals(method.name)) {
                    accept.add(method.name + method.desc);
                }
            }
            return accept;
        }
    },
    NO_SHAPE_BUILDER_BRIDGES(
        "Declare `BuilderImpl.deserialize` and `deserializeMember` with the `ShapeBuilder<T>` return type, so javac "
        + "emits no covariant bridge methods for them",
        SizeCategory.GENERATED_MODEL_CLASS, Estimate.FULL) {
        @Override
        boolean selectsMethod(ClassNode node, MethodNode method) {
            return isBridge(method) && ("deserialize".equals(method.name) || "deserializeMember".equals(method.name));
        }
    },
    PACKAGE_PRIVATE_MEMBER_SCHEMAS(
        "Make the `M_*` member Schema constants package-private, so the nested builder and deserializer read them "
        + "without synthetic `access$NNN()` accessors (Java 8 has no nestmates)",
        SizeCategory.GENERATED_MODEL_CLASS, Estimate.FULL) {
        @Override
        boolean selectsMethod(ClassNode node, MethodNode method) {
            return (method.access & Opcodes.ACC_SYNTHETIC) != 0 && (method.access & Opcodes.ACC_STATIC) != 0
                   && method.name.startsWith("access$") && SCHEMA_GETTER.equals(method.desc);
        }
    },
    INHERITED_SDK_FIELDS(
        "Derive `sdkFields()` and `sdkFieldNameToField()` from `sdkSchema()` in one inherited default instead of an "
        + "override in every model class and builder",
        SizeCategory.GENERATED_MODEL_CLASS, Estimate.FULL) {
        @Override
        boolean selectsMethod(ClassNode node, MethodNode method) {
            return isSchemaShape(node) && !isBridge(method)
                   && (SDK_FIELDS.equals(method.name + method.desc)
                       || SDK_FIELD_NAME_TO_FIELD.equals(method.name + method.desc));
        }
    },
    GENERIC_EQUALS_HASHCODE(
        "Generic `equals`, `hashCode` and `equalsBySdkFields` through `SerializableStruct` (opt-in customization; "
        + "slower than generated code)",
        SizeCategory.GENERATED_MODEL_CLASS, Estimate.FULL) {
        @Override
        boolean selectsMethod(ClassNode node, MethodNode method) {
            String key = method.name + method.desc;
            return node.interfaces.contains(SERIALIZABLE_STRUCT) && !isBridge(method)
                   && ("equals(Ljava/lang/Object;)Z".equals(key) || "hashCode()I".equals(key)
                       || "equalsBySdkFields(Ljava/lang/Object;)Z".equals(key));
        }
    },
    ENUM_HELPER(
        "Move the enum `fromValue`/`knownValues` boilerplate and its `VALUE_MAP` into a shared helper keyed by the "
        + "enum class",
        SizeCategory.GENERATED_MODEL_CLASS, Estimate.FULL) {
        @Override
        boolean selectsMethod(ClassNode node, MethodNode method) {
            return isEnum(node) && (method.access & Opcodes.ACC_STATIC) != 0
                   && (("fromValue".equals(method.name) && method.desc.equals("(Ljava/lang/String;)L" + node.name + ";"))
                       || ("knownValues".equals(method.name) && "()Ljava/util/Set;".equals(method.desc)));
        }

        @Override
        boolean selectsStaticField(ClassNode node, FieldNode field) {
            return isEnum(node) && "VALUE_MAP".equals(field.name);
        }
    },
    SCHEMA_COPIER(
        "Replace the generated `*Copier` classes of collection shapes with a schema-driven copier",
        SizeCategory.OTHER, Estimate.FULL) {
        @Override
        boolean selectsClass(ClassAttributor.Location location, ClassNode node) {
            return "model".equals(location.sub()) && location.outer().endsWith("Copier");
        }
    },
    GENERIC_PAGINATOR(
        "Generic schema-driven paginator for `@paginated` operations; the per-operation `*Iterable`/`*Publisher` "
        + "classes keep only their public signatures",
        SizeCategory.CLIENT, Estimate.PAGINATOR_RANGE) {
        @Override
        boolean selectsClass(ClassAttributor.Location location, ClassNode node) {
            return "paginators".equals(location.sub());
        }
    },
    /**
     * Measured from the attribution (classes in the marshaller category and exception-mapping carves); see
     * {@link #measureAll}.
     */
    LEGACY_PATH_REMAINDER(
        "Move the operations still on the legacy path to the schema path: the marshallers, unmarshallers and "
        + "exception-mapping code the feature build keeps",
        SizeCategory.MARSHALLER, Estimate.FULL);

    static final String SERIALIZABLE_STRUCT = "software/amazon/awssdk/schema/SerializableStruct";
    static final String SHAPE_BUILDER = "software/amazon/awssdk/schema/ShapeBuilder";
    private static final String SCHEMA_GETTER = "()Lsoftware/amazon/awssdk/schema/Schema;";
    private static final String SDK_FIELDS = "sdkFields()Ljava/util/List;";
    private static final String SDK_FIELD_NAME_TO_FIELD = "sdkFieldNameToField()Ljava/util/Map;";

    private final String description;
    private final SizeCategory category;
    private final Estimate estimate;

    ReductionOpportunity(String description, SizeCategory category, Estimate estimate) {
        this.description = description;
        this.category = category;
        this.estimate = estimate;
    }

    String description() {
        return description;
    }

    /**
     * The Requirement 36.2 category whose bytes the opportunity removes.
     */
    SizeCategory category() {
        return category;
    }

    Estimate estimate() {
        return estimate;
    }

    /**
     * Whether the opportunity removes this whole class (the default: no).
     */
    boolean selectsClass(ClassAttributor.Location location, ClassNode node) {
        return false;
    }

    /**
     * For a selected class, the methods that would move elsewhere instead of going away; their bytes are not counted.
     */
    Set<String> keptMethods(ClassNode node) {
        return Collections.emptySet();
    }

    /**
     * Whether the opportunity removes this method of a class it does not select whole (the default: no).
     */
    boolean selectsMethod(ClassNode node, MethodNode method) {
        return false;
    }

    /**
     * Whether the opportunity removes this static field and its {@code <clinit>} segment (the default: no).
     */
    boolean selectsStaticField(ClassNode node, FieldNode field) {
        return false;
    }

    /**
     * Measure every opportunity on one feature build service JAR, in declaration order. A class that an opportunity
     * removes whole is not measured again for member opportunities, and member opportunities apply only to the
     * {@code model} package, so the measurements do not overlap.
     */
    static Map<ReductionOpportunity, Measurement> measureAll(JarContents jar, ServiceSizes sizes) {
        Map<ReductionOpportunity, Measurement> result = new EnumMap<>(ReductionOpportunity.class);
        for (ReductionOpportunity opportunity : values()) {
            result.put(opportunity, new Measurement());
        }
        ClassAttributor attributor = ClassAttributor.forClasses(jar.classes().keySet());
        for (ClassAttribution attribution : sizes.classes()) {
            String className = attribution.className();
            byte[] classBytes = jar.classes().get(className);
            JarContents.EntrySize entry = jar.classEntrySize(className);
            if (attribution.category() == SizeCategory.MARSHALLER) {
                result.get(LEGACY_PATH_REMAINDER).addClass(classBytes.length, entry.total());
                continue;
            }
            for (ClassAttribution.Carve carve : attribution.carves()) {
                if (carve.category() == SizeCategory.EXCEPTION_MAPPING) {
                    result.get(LEGACY_PATH_REMAINDER).addMembers(carve.bytes(), classBytes.length, entry.data());
                }
            }
            ClassAttributor.Location location = attributor.locate(className);
            if (location == null) {
                continue;
            }
            ClassNode node = MemberCarver.read(classBytes);
            ReductionOpportunity whole = null;
            for (ReductionOpportunity opportunity : values()) {
                if (opportunity.selectsClass(location, node)) {
                    whole = opportunity;
                    break;
                }
            }
            if (whole != null) {
                whole.measureClass(classBytes, entry, node, result.get(whole));
            } else if ("model".equals(location.sub())) {
                for (ReductionOpportunity opportunity : values()) {
                    opportunity.measureMembers(classBytes, entry, node, result.get(opportunity));
                }
            }
        }
        return result;
    }

    private void measureClass(byte[] classBytes, JarContents.EntrySize entry, ClassNode node,
                              Measurement measurement) {
        Set<String> kept = keptMethods(node);
        long keptBytes = 0;
        if (!kept.isEmpty()) {
            ClassAttribution.Carve carve = MemberCarver.carve(classBytes, category, kept, Collections.emptySet(),
                                                              Collections.emptySet());
            keptBytes = carve == null ? 0 : carve.bytes();
        }
        measurement.addClass(classBytes.length - keptBytes,
                             entry.total() - scale(keptBytes, classBytes.length, entry.data()));
    }

    private void measureMembers(byte[] classBytes, JarContents.EntrySize entry, ClassNode node,
                                Measurement measurement) {
        Set<String> methods = new LinkedHashSet<>();
        for (MethodNode method : node.methods) {
            if (selectsMethod(node, method)) {
                methods.add(method.name + method.desc);
            }
        }
        Set<String> fields = new LinkedHashSet<>();
        for (FieldNode field : node.fields) {
            if (MemberCarver.isStatic(field) && selectsStaticField(node, field)) {
                fields.add(field.name);
            }
        }
        if (methods.isEmpty() && fields.isEmpty()) {
            return;
        }
        ClassAttribution.Carve carve = MemberCarver.carve(classBytes, category, methods, fields,
                                                          Collections.emptySet());
        if (carve != null) {
            measurement.addMembers(carve.bytes(), classBytes.length, entry.data());
        }
    }

    private static long scale(long bytes, long classBytes, long entryData) {
        return classBytes == 0 ? 0 : Math.round((double) bytes * entryData / classBytes);
    }

    private static boolean isBridge(MethodNode method) {
        return (method.access & Opcodes.ACC_BRIDGE) != 0;
    }

    private static boolean isEnum(ClassNode node) {
        return "java/lang/Enum".equals(node.superName);
    }

    private static boolean isSchemaShape(ClassNode node) {
        return node.interfaces.contains(SERIALIZABLE_STRUCT) || node.interfaces.contains(SHAPE_BUILDER);
    }

    /**
     * How the estimate in compressed JAR bytes follows from the measured bytes.
     */
    enum Estimate {
        /** The selected code goes away: the estimate is minus the measured compressed bytes. */
        FULL(100, 100),
        /** The design's estimate for a generic paginator: 20-40% of the paginator classes go away. */
        PAGINATOR_RANGE(20, 40);

        private final int lowPercent;
        private final int highPercent;

        Estimate(int lowPercent, int highPercent) {
            this.lowPercent = lowPercent;
            this.highPercent = highPercent;
        }

        /**
         * The smallest estimated reduction, in compressed JAR bytes (a positive number).
         */
        long low(Measurement measurement) {
            return measurement.jarBytes() * lowPercent / 100;
        }

        /**
         * The largest estimated reduction, in compressed JAR bytes (a positive number).
         */
        long high(Measurement measurement) {
            return measurement.jarBytes() * highPercent / 100;
        }
    }

    /**
     * The code one opportunity selects in one service JAR.
     */
    static final class Measurement {
        private int classes;
        private long classBytes;
        private long jarBytes;

        void addClass(long removedClassBytes, long removedJarBytes) {
            classes++;
            classBytes += removedClassBytes;
            jarBytes += removedJarBytes;
        }

        void addMembers(long carvedBytes, long classFileBytes, long entryData) {
            classes++;
            classBytes += carvedBytes;
            jarBytes += scale(carvedBytes, classFileBytes, entryData);
        }

        /**
         * The number of classes the opportunity removes or changes.
         */
        int classes() {
            return classes;
        }

        /**
         * The uncompressed class-file bytes of the selected code.
         */
        long classBytes() {
            return classBytes;
        }

        /**
         * The compressed JAR bytes of the selected code: exact for whole classes, estimated for members.
         */
        long jarBytes() {
            return jarBytes;
        }
    }
}
