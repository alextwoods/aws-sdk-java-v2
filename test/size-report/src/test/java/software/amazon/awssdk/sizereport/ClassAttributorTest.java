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
import static org.assertj.core.api.Assertions.entry;
import static software.amazon.awssdk.sizereport.SyntheticClasses.CLIENT;
import static software.amazon.awssdk.sizereport.SyntheticClasses.OPERATIONS;
import static software.amazon.awssdk.sizereport.SyntheticClasses.SERVICE;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClassAttributorTest {
    private static final ClassAttributor ATTRIBUTOR = ClassAttributor.forClasses(Arrays.asList(
        CLIENT, SERVICE + "/DefaultFooAsyncClient", SERVICE + "/DefaultFooClientBuilder",
        "test/svc/streams/DefaultFooStreamsClient", SERVICE + "/model/Bar"));

    @Test
    void forClasses_withDefaultClients_findsServicePackagesLongestFirst() {
        assertThat(ATTRIBUTOR.servicePackages()).containsExactly("test/svc/streams", SERVICE);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "test/svc/transform/GetBarRequestMarshaller, MARSHALLER",
        "test/svc/transform/GetBarResponseUnmarshaller$1, MARSHALLER",
        "test/svc/model/BarUnmarshaller, MARSHALLER",
        "test/svc/model/FooSchemas, SCHEMAS",
        "test/svc/model/FooSchemas2, SCHEMAS",
        "test/svc/internal/FooServiceSchemas, SCHEMAS",
        "test/svc/model/FooSchemaIndex, SCHEMAS",
        "test/svc/model/FooTypeRegistry, TYPE_REGISTRY",
        "test/svc/model/FooErrorRegistry, TYPE_REGISTRY",
        "test/svc/DefaultFooClient, CLIENT",
        "test/svc/FooClient, CLIENT",
        "test/svc/DefaultFooAsyncClient$1, CLIENT",
        "test/svc/paginators/ListBarsIterable$ListBarsResponseFetcher, CLIENT",
        "test/svc/waiters/internal/WaitersRuntime, CLIENT",
        "test/svc/model/FooOperations, CLIENT",
        "test/svc/model/Bar, GENERATED_MODEL_CLASS",
        "test/svc/model/Bar$BuilderImpl$InnerDeserializer, GENERATED_MODEL_CLASS",
        "test/svc/model/BarState, GENERATED_MODEL_CLASS",
        "test/svc/model/BarListCopier, OTHER",
        "test/svc/model/FooSharedSerde$BarsSerde, OTHER",
        "test/svc/model/ListSchemasRequest, GENERATED_MODEL_CLASS",
        "test/svc/endpoints/FooEndpointProvider, OTHER",
        "test/svc/internal/UserAgentUtils, OTHER",
        "test/svc/streams/DefaultFooStreamsClient, CLIENT",
        "test/svc/streams/model/Record, GENERATED_MODEL_CLASS",
        "test/other/Thing, OTHER"
    })
    void category_byName_followsTheRules(String className, SizeCategory expected) {
        assertThat(ATTRIBUTOR.category(className)).isEqualTo(expected);
    }

    @Test
    void attribute_clientWithExceptionMapping_carvesTheMappingMethodsExactly() {
        byte[] withMapping = SyntheticClasses.client(true);
        byte[] withoutMapping = SyntheticClasses.client(false);

        ClassAttribution attribution = ATTRIBUTOR.attribute(CLIENT, withMapping);

        assertThat(attribution.category()).isEqualTo(SizeCategory.CLIENT);
        assertThat(attribution.carves()).hasSize(1);
        ClassAttribution.Carve carve = attribution.carves().get(0);
        assertThat(carve.category()).isEqualTo(SizeCategory.EXCEPTION_MAPPING);
        // The mapper lambda, the ExceptionMetadata descriptor and builder users, and the lambda init creates.
        assertThat(carve.methods()).containsExactly(
            "createErrorResponseHandler(Lsoftware/amazon/awssdk/protocols/core/ExceptionMetadata;)Ljava/lang/Object;",
            "init()Ljava/lang/Object;",
            "lambda$init$1()Ljava/lang/Object;",
            "lambda$op$0(Ljava/lang/String;)Ljava/util/Optional;");
        assertThat(carve.bytes()).isEqualTo(withMapping.length - withoutMapping.length);
        assertThat(attribution.categoryBytes())
            .containsEntry(SizeCategory.EXCEPTION_MAPPING, carve.bytes())
            .containsEntry(SizeCategory.CLIENT, (long) withoutMapping.length);
    }

    @Test
    void attribute_clientWithoutExceptionMapping_hasNoCarve() {
        byte[] client = SyntheticClasses.client(false);

        ClassAttribution attribution = ATTRIBUTOR.attribute(CLIENT, client);

        assertThat(attribution.carves()).isEmpty();
        assertThat(attribution.categoryBytes()).containsOnly(entry(SizeCategory.CLIENT, (long) client.length));
    }

    @Test
    void attribute_operationsWithErrorRegistry_carvesFieldSegmentAndLambdaExactly() {
        byte[] withRegistry = SyntheticClasses.operations(true);
        byte[] withoutRegistry = SyntheticClasses.operations(false);

        ClassAttribution attribution = ATTRIBUTOR.attribute(OPERATIONS, withRegistry);

        assertThat(attribution.category()).isEqualTo(SizeCategory.CLIENT);
        assertThat(attribution.carves()).hasSize(1);
        ClassAttribution.Carve carve = attribution.carves().get(0);
        assertThat(carve.category()).isEqualTo(SizeCategory.TYPE_REGISTRY);
        assertThat(carve.fields()).containsExactly("ERROR_REGISTRY");
        assertThat(carve.methods()).containsExactly("lambda$static$0()Ljava/lang/Object;");
        assertThat(carve.staticInitializerSegments()).isEqualTo(1);
        assertThat(carve.bytes()).isEqualTo(withRegistry.length - withoutRegistry.length);
        assertThat(attribution.categoryBytes().values().stream().mapToLong(Long::longValue).sum())
            .isEqualTo(withRegistry.length);
    }

    @Test
    void attribute_classInOtherCategories_keepsAllBytes() {
        byte[] bar = SyntheticClasses.plain(SERVICE + "/model/Bar", 3);

        ClassAttribution attribution = ATTRIBUTOR.attribute(SERVICE + "/model/Bar", bar);

        assertThat(attribution.category()).isEqualTo(SizeCategory.GENERATED_MODEL_CLASS);
        assertThat(attribution.bytes()).isEqualTo(bar.length);
        assertThat(attribution.categoryBytes())
            .containsOnly(entry(SizeCategory.GENERATED_MODEL_CLASS, (long) bar.length));
    }
}
