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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.sizereport.SyntheticClasses.CLIENT;
import static software.amazon.awssdk.sizereport.SyntheticClasses.OPERATIONS;
import static software.amazon.awssdk.sizereport.SyntheticClasses.SERVICE;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class ServiceComparisonTest {
    private static final String MARSHALLER = SERVICE + "/transform/GetBarRequestMarshaller";
    private static final String BAR = SERVICE + "/model/Bar";
    private static final String SCHEMAS = SERVICE + "/model/FooSchemas";
    private static final String ENDPOINTS = SERVICE + "/endpoints/FooEndpointProvider";
    private static final String COPIER = SERVICE + "/model/BarsCopier";

    @Test
    void categoryChanges_baselineToFeature_addUpToTheTotalChange() {
        ServiceComparison comparison = compare(1_000, 900);

        long sum = comparison.categoryChanges().values().stream().mapToLong(Long::longValue).sum();
        assertThat(sum).isEqualTo(comparison.classBytesChange());
        assertThat(comparison.categoryChanges().keySet()).containsExactly(SizeCategory.values());
    }

    @Test
    void categoryChanges_baselineToFeature_attributeEachChangeToItsCategory() {
        Map<String, byte[]> baseline = baselineClasses();
        Map<String, byte[]> feature = featureClasses();
        ServiceComparison comparison = compare(1_000, 900);
        Map<SizeCategory, Long> changes = comparison.categoryChanges();

        assertThat(changes.get(SizeCategory.MARSHALLER)).isEqualTo(-baseline.get(MARSHALLER).length);
        assertThat(changes.get(SizeCategory.SCHEMAS)).isEqualTo(feature.get(SCHEMAS).length);
        assertThat(changes.get(SizeCategory.EXCEPTION_MAPPING))
            .isEqualTo(-(SyntheticClasses.client(true).length - SyntheticClasses.client(false).length));
        assertThat(changes.get(SizeCategory.TYPE_REGISTRY))
            .isEqualTo(SyntheticClasses.operations(true).length - SyntheticClasses.operations(false).length);
        // The client keeps its non-mapping bytes; FooOperations without its registry is new client bytes.
        assertThat(changes.get(SizeCategory.CLIENT)).isEqualTo(SyntheticClasses.operations(false).length);
        assertThat(changes.get(SizeCategory.GENERATED_MODEL_CLASS))
            .isEqualTo(feature.get(BAR).length - baseline.get(BAR).length);
        assertThat(changes.get(SizeCategory.OTHER))
            .isEqualTo(feature.get(ENDPOINTS).length + feature.get(COPIER).length - baseline.get(ENDPOINTS).length);
    }

    @Test
    void measure_eachClass_hasExactlyOneCategoryAndAllItsBytes() {
        ServiceSizes feature = ServiceSizes.measure(jar(900, featureClasses()));

        assertThat(feature.classes()).hasSize(featureClasses().size());
        for (ClassAttribution attribution : feature.classes()) {
            assertThat(attribution.category()).isNotNull();
            assertThat(attribution.categoryBytes().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(attribution.bytes());
        }
        assertThat(feature.categoryBytes().values().stream().mapToLong(Long::longValue).sum())
            .isEqualTo(feature.classBytes());
        assertThat(feature.classCount()).isEqualTo(featureClasses().size());
        assertThat(feature.packages()).containsOnlyKeys("test.svc", "test.svc.model", "test.svc.endpoints");
        assertThat(feature.packages().get("test.svc.model").classes()).isEqualTo(4);
    }

    @Test
    void meetsJarSizeRequirement_featureJarSmallerOrEqual_meets() {
        assertThat(compare(1_000, 900).meetsJarSizeRequirement()).isTrue();
        assertThat(compare(1_000, 1_000).meetsJarSizeRequirement()).isTrue();
    }

    @Test
    void meetsJarSizeRequirement_featureJarLarger_failsAndListsGrownCategories() {
        ServiceComparison comparison = compare(1_000, 1_001);

        assertThat(comparison.meetsJarSizeRequirement()).isFalse();
        assertThat(comparison.compressedChange()).isEqualTo(1);
        assertThat(comparison.grownCategories())
            .contains(SizeCategory.SCHEMAS, SizeCategory.TYPE_REGISTRY, SizeCategory.GENERATED_MODEL_CLASS)
            .doesNotContain(SizeCategory.MARSHALLER, SizeCategory.EXCEPTION_MAPPING);
    }

    @Test
    void largestChanges_groupsNestedClassesUnderTheirTopLevelClass() {
        Map<String, byte[]> feature = featureClasses();
        feature.put(BAR + "$BuilderImpl", SyntheticClasses.plain(BAR + "$BuilderImpl", 1));
        ServiceComparison comparison = new ServiceComparison("foo", "Foo", ServiceSizes.measure(jar(1, baselineClasses())),
                                                             ServiceSizes.measure(jar(1, feature)));

        ServiceComparison.ClassGroupChange bar = comparison.largestChanges(100).stream()
                                                           .filter(g -> g.name().equals(BAR))
                                                           .findFirst().get();

        assertThat(bar.featureClasses()).isEqualTo(2);
        assertThat(bar.baselineClasses()).isEqualTo(1);
        assertThat(bar.category()).isEqualTo(SizeCategory.GENERATED_MODEL_CLASS);
        assertThat(comparison.largestChanges(2)).hasSize(2);
    }

    @Test
    void modelVersionDifferences_featureOnlyModelClass_listedWithoutDesignClasses() {
        Map<String, byte[]> feature = featureClasses();
        String newRequest = SERVICE + "/model/NewOperationRequest";
        String sharedSerde = SERVICE + "/model/FooSharedSerde";
        feature.put(newRequest, SyntheticClasses.plain(newRequest, 2));
        feature.put(newRequest + "$Builder", SyntheticClasses.plain(newRequest + "$Builder", 1));
        feature.put(sharedSerde, SyntheticClasses.plain(sharedSerde, 1));
        ServiceComparison comparison = new ServiceComparison("foo", "Foo", ServiceSizes.measure(jar(1, baselineClasses())),
                                                             ServiceSizes.measure(jar(1, feature)));

        // Not listed: the removed marshaller, FooSchemas, FooOperations and FooSharedSerde, which this design adds or
        // removes, and the classes in both builds.
        assertThat(comparison.modelVersionDifferences())
            .extracting(ServiceComparison.ClassGroupChange::name)
            .containsExactly(COPIER, newRequest);
        assertThat(comparison.modelVersionDifferences().get(1).featureClasses()).isEqualTo(2);
    }

    private static ServiceComparison compare(long baselineJarBytes, long featureJarBytes) {
        return new ServiceComparison("foo", "Foo", ServiceSizes.measure(jar(baselineJarBytes, baselineClasses())),
                                     ServiceSizes.measure(jar(featureJarBytes, featureClasses())));
    }

    private static Map<String, byte[]> baselineClasses() {
        Map<String, byte[]> classes = new TreeMap<>();
        classes.put(CLIENT, SyntheticClasses.client(true));
        classes.put(MARSHALLER, SyntheticClasses.plain(MARSHALLER, 4));
        classes.put(BAR, SyntheticClasses.plain(BAR, 5));
        classes.put(ENDPOINTS, SyntheticClasses.plain(ENDPOINTS, 2));
        return classes;
    }

    private static Map<String, byte[]> featureClasses() {
        Map<String, byte[]> classes = new TreeMap<>();
        classes.put(CLIENT, SyntheticClasses.client(false));
        classes.put(OPERATIONS, SyntheticClasses.operations(true));
        classes.put(SCHEMAS, SyntheticClasses.plain(SCHEMAS, 6));
        classes.put(BAR, SyntheticClasses.plain(BAR, 7));
        classes.put(COPIER, SyntheticClasses.plain(COPIER, 1));
        classes.put(ENDPOINTS, SyntheticClasses.plain(ENDPOINTS, 2));
        return classes;
    }

    private static JarContents jar(long compressedBytes, Map<String, byte[]> classes) {
        return new JarContents(compressedBytes, classes, Collections.emptyMap(), "21");
    }
}
