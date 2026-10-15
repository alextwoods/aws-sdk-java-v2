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

package software.amazon.awssdk.services.cloudwatch.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.DataTypeConversionFailureHandlingTrait;
import software.amazon.awssdk.core.traits.DefaultValueTrait;
import software.amazon.awssdk.core.traits.JsonValueTrait;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.traits.Trait;
import software.amazon.awssdk.core.traits.TraitType;
import software.amazon.awssdk.core.traits.XmlAttributeTrait;
import software.amazon.awssdk.core.traits.XmlAttributesTrait;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * The CloudWatch model classes' {@code SdkPojo} view, derived from their Schemas by {@code SdkFieldsFromSchema} since
 * the generated {@code SdkField} constants were removed (tasks 3.7 and 5.1, design K3, OD-14), equals the Baseline_Build's
 * (Requirement 16.1): for every model class of the baseline jar ({@code target/baseline/cloudwatch-baseline.jar}, the
 * japicmp baseline), {@code sdkFields()} has the same fields in the same order with the same member name, marshalling
 * type, location, location names and traits (list member and map value fields included), {@code sdkFieldNameToField()}
 * the same keys, and an instance with every member set (nested structures, lists and maps included) has the same
 * field values, {@code toString()} and {@code hashCode()} as the baseline class populated with the same values. The
 * {@code toString()} of a class built from the Schema goes through {@code ToStringSerializer} (Requirement 40.1).
 * CloudWatch has no sensitive members; the redaction is covered by the codegen {@code SchemaSerdeModelClassTest}.
 *
 * <p>The baseline classes are loaded child-first from the baseline jar, on the feature build's runtime modules
 * ({@code sdk-core}, {@code aws-core}, ...), which keep the baseline's public API.
 */
class BaselineModelParityTest {
    private static final String MODEL_PACKAGE = BaselineModelParityTest.class.getPackage().getName();
    private static final Path BASELINE_JAR = Paths.get("target", "baseline", "cloudwatch-baseline.jar");
    private static final Instant TIME = Instant.ofEpochMilli(1_700_000_000_123L);
    private static final int MAX_DEPTH = 4;

    private static final List<Class<? extends Trait>> TRAIT_CLASSES =
        Arrays.asList(DataTypeConversionFailureHandlingTrait.class, DefaultValueTrait.class, JsonValueTrait.class,
                      ListTrait.class, LocationTrait.class, MapTrait.class, PayloadTrait.class, RequiredTrait.class,
                      TimestampFormatTrait.class, XmlAttributeTrait.class, XmlAttributesTrait.class);

    private static ChildFirstClassLoader baseline;

    @BeforeAll
    static void loadBaseline() throws IOException {
        assertThat(BASELINE_JAR).as("baseline jar copied by the copy-baseline-jar execution").exists();
        baseline = new ChildFirstClassLoader(BASELINE_JAR.toUri().toURL(), BaselineModelParityTest.class.getClassLoader());
    }

    @AfterAll
    static void closeBaseline() throws IOException {
        baseline.close();
    }

    static Stream<String> modelClasses() throws IOException {
        List<String> names = new ArrayList<>();
        try (JarFile jar = new JarFile(BASELINE_JAR.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            String prefix = MODEL_PACKAGE.replace('.', '/') + "/";
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(prefix) && name.endsWith(".class") && !name.contains("$")
                    && name.indexOf('/', prefix.length()) < 0) {
                    names.add(name.substring(prefix.length(), name.length() - ".class".length()));
                }
            }
        }
        Collections.sort(names);
        return names.stream();
    }

    @Test
    void traitClasses_coverEveryTraitType() {
        assertThat(TRAIT_CLASSES.stream().map(TraitType::from).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder(TraitType.values());
    }

    @Test
    void baselineJar_hasTheModelClasses() throws IOException {
        // The 166 structure, request, response and exception classes (task 3.35) and the base exception
        // CloudWatchException.
        assertThat(modelClasses().filter(BaselineModelParityTest::isPojoClass).count()).isEqualTo(167);
    }

    @ParameterizedTest
    @MethodSource("modelClasses")
    void sdkFields_matchTheBaselineFields(String simpleName) throws Exception {
        Class<?> feature = Class.forName(MODEL_PACKAGE + "." + simpleName);
        Class<?> base = baseline.loadClass(MODEL_PACKAGE + "." + simpleName);
        assertThat(base.getClassLoader()).isSameAs(baseline);
        if (!isPojo(base)) {
            assertThat(isPojo(feature)).isFalse();
            return;
        }

        SdkPojo featureBuilder = builder(feature);
        SdkPojo baseBuilder = builder(base);
        assertThat(describe(featureBuilder.sdkFields())).isEqualTo(describe(baseBuilder.sdkFields()));
        assertThat(fieldNames(featureBuilder)).isEqualTo(fieldNames(baseBuilder));

        Object featureEmpty = build(featureBuilder);
        Object baseEmpty = build(baseBuilder);
        assertThat(describe(((SdkPojo) featureEmpty).sdkFields())).isEqualTo(describe(baseBuilder.sdkFields()));
        boolean exception = featureEmpty instanceof Throwable;
        if (!exception) {
            assertThat(featureEmpty.toString()).isEqualTo(baseEmpty.toString());
            assertThat(featureEmpty.hashCode()).isEqualTo(baseEmpty.hashCode());
        }

        Object featureFull = populated(feature, 0);
        Object baseFull = populated(base, 0);
        assertThat(values(featureFull)).isEqualTo(values(baseFull));
        assertThat(featureFull.toString()).isEqualTo(baseFull.toString());
        if (!exception) {
            assertThat(featureFull.hashCode()).isEqualTo(baseFull.hashCode());
        }
    }

    @Test
    void toString_ofRepresentativeShapesMatchesTheBaseline() throws Exception {
        // Nested structures, lists and enums stored as strings (MetricAlarm, MetricDatum), a request with nested
        // lists of structures and maps (PutMetricDataRequest through EntityMetricData and Entity), lists of
        // timestamps (GetMetricDataResponse), maps of strings and of doubles (Entity, Datapoint, AlarmHistoryItem),
        // a member named Schema (InsightRule) and a blob response.
        for (String simpleName : new String[] {"MetricAlarm", "MetricDatum", "PutMetricDataRequest",
                                               "GetMetricDataResponse", "Entity", "Datapoint", "AlarmHistoryItem",
                                               "InsightRule", "GetMetricWidgetImageResponse"}) {
            String featureString = populated(Class.forName(MODEL_PACKAGE + "." + simpleName), 0).toString();
            String baseString = populated(baseline.loadClass(MODEL_PACKAGE + "." + simpleName), 0).toString();
            assertThat(featureString).as(simpleName).isEqualTo(baseString).startsWith(simpleName + "(").contains("=");
        }
    }

    private static boolean isPojoClass(String simpleName) {
        try {
            return isPojo(baseline.loadClass(MODEL_PACKAGE + "." + simpleName));
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private static boolean isPojo(Class<?> type) {
        if (type.isInterface() || type.isEnum() || Modifier.isAbstract(type.getModifiers())) {
            return false;
        }
        try {
            Method builder = type.getMethod("builder");
            return Modifier.isStatic(builder.getModifiers()) && SdkPojo.class.isAssignableFrom(builder.getReturnType());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * The keys of {@code sdkFieldNameToField()}, sorted, or a marker when the class keeps {@code SdkPojo}'s default,
     * which throws (the base exception {@code CloudWatchException}).
     */
    private static String fieldNames(SdkPojo pojo) {
        try {
            return new TreeMap<>(pojo.sdkFieldNameToField()).keySet().toString();
        } catch (UnsupportedOperationException e) {
            return "unsupported";
        }
    }

    private static SdkPojo builder(Class<?> type) throws Exception {
        return (SdkPojo) type.getMethod("builder").invoke(null);
    }

    private static Object build(Object builder) {
        if (builder instanceof AwsServiceException.Builder) {
            // The base exception's builder is not an SdkBuilder.
            return ((AwsServiceException.Builder) builder).build();
        }
        return ((SdkBuilder<?, ?>) builder).build();
    }

    /**
     * An instance of the class with every field set through its {@code SdkField} setter.
     */
    private static Object populated(Class<?> type, int depth) throws Exception {
        return populate(builder(type), depth);
    }

    private static Object populate(SdkPojo builder, int depth) {
        for (SdkField<?> field : builder.sdkFields()) {
            Object value = value(field, depth);
            if (value != null) {
                field.set(builder, value);
            }
        }
        return build(builder);
    }

    /**
     * A value for a field, derived from its member name so that two classes get the same values.
     */
    private static Object value(SdkField<?> field, int depth) {
        MarshallingType<?> type = field.marshallingType();
        String name = field.memberName() == null ? "member" : field.memberName();
        if (type == MarshallingType.STRING) {
            return name + "-value";
        }
        if (type == MarshallingType.INTEGER) {
            return name.length();
        }
        if (type == MarshallingType.LONG) {
            return (long) name.length() << 33;
        }
        if (type == MarshallingType.SHORT) {
            return (short) name.length();
        }
        if (type == MarshallingType.FLOAT) {
            return name.length() + 0.25f;
        }
        if (type == MarshallingType.DOUBLE) {
            return name.length() + 0.5;
        }
        if (type == MarshallingType.BIG_DECIMAL) {
            return new BigDecimal(name.length() + ".125");
        }
        if (type == MarshallingType.BOOLEAN) {
            return name.length() % 2 == 0;
        }
        if (type == MarshallingType.INSTANT) {
            return TIME.plusSeconds(name.length());
        }
        if (type == MarshallingType.SDK_BYTES) {
            return SdkBytes.fromUtf8String(name);
        }
        if (type == MarshallingType.SDK_POJO) {
            return depth >= MAX_DEPTH ? null : populate(field.constructor().get(), depth + 1);
        }
        if (type == MarshallingType.LIST) {
            Object element = value(field.getRequiredTrait(ListTrait.class).memberFieldInfo(), depth);
            return element == null ? null : Collections.singletonList(element);
        }
        if (type == MarshallingType.MAP) {
            Object entry = value(field.getRequiredTrait(MapTrait.class).valueFieldInfo(), depth);
            if (entry == null) {
                return null;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(name + "-key", entry);
            return map;
        }
        throw new AssertionError("No test value for " + name + " of type " + type);
    }

    private static List<String> values(Object pojo) {
        return ((SdkPojo) pojo).sdkFields().stream()
                               .map(f -> f.memberName() + "=" + f.getValueOrDefault(pojo))
                               .collect(Collectors.toList());
    }

    private static List<String> describe(List<SdkField<?>> fields) {
        return fields.stream().map(BaselineModelParityTest::describe).collect(Collectors.toList());
    }

    /**
     * A field's member name, marshalling type, location, location names, conversion failure handling and its traits'
     * properties (every public no-argument accessor), with list member and map value fields described the same way.
     */
    private static String describe(SdkField<?> field) {
        StringBuilder out = new StringBuilder();
        out.append(field.memberName()).append(" ").append(field.marshallingType())
           .append(" ").append(field.location()).append(" ").append(field.locationName())
           .append("/").append(field.unmarshallLocationName())
           .append(" ignoreConversionFailures=").append(field.ignoreDataTypeConversionFailures());
        out.append(" constructor=").append(field.constructor() == null ? null : className(field.constructor().get()));
        for (Class<? extends Trait> traitClass : TRAIT_CLASSES) {
            Trait trait = field.getTrait(traitClass);
            if (trait != null) {
                out.append(" ").append(describe(trait));
            }
        }
        return out.toString();
    }

    private static String describe(Trait trait) {
        StringBuilder out = new StringBuilder(trait.getClass().getSimpleName()).append("{");
        List<Method> accessors = new ArrayList<>();
        for (Method method : trait.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers())
                && method.getDeclaringClass() != Object.class && !method.getName().equals("type")) {
                accessors.add(method);
            }
        }
        accessors.sort((a, b) -> a.getName().compareTo(b.getName()));
        for (Method accessor : accessors) {
            Object value;
            try {
                value = accessor.invoke(trait);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
            out.append(accessor.getName()).append("=");
            if (value instanceof SdkField) {
                out.append("[").append(describe((SdkField<?>) value)).append("]");
            } else if (value instanceof Trait || value == null || value instanceof Enum || value instanceof String
                       || value instanceof Boolean || value instanceof Number) {
                out.append(value);
            } else {
                out.append(className(value));
            }
            out.append(";");
        }
        return out.append("}").toString();
    }

    private static String className(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    /**
     * Loads the CloudWatch classes from the baseline jar and every other class from the parent.
     */
    private static final class ChildFirstClassLoader extends URLClassLoader {
        private static final String CLOUDWATCH_PREFIX = "software.amazon.awssdk.services.cloudwatch.";

        ChildFirstClassLoader(URL jar, ClassLoader parent) {
            super(new URL[] {jar}, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(CLOUDWATCH_PREFIX)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = findClass(name);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }
    }
}
