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

package software.amazon.awssdk.core.client.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;

class ProtocolSettingsTest {
    private static final ShapeId SERVICE_ID = ShapeId.from("com.example#ExampleService");
    private static final Schema SERVICE_SCHEMA = Schema.createService(SERVICE_ID);

    @Test
    void build_allValuesSet_returnsThem() {
        Supplier<SdkServiceException.Builder> exceptionBuilder = SdkServiceException::builder;

        ProtocolSettings settings = ProtocolSettings.builder()
                                                    .service(SERVICE_ID)
                                                    .serviceVersion("2012-11-05")
                                                    .serviceSchema(SERVICE_SCHEMA)
                                                    .baseExceptionBuilder(exceptionBuilder)
                                                    .build();

        assertThat(settings.service()).isEqualTo(SERVICE_ID);
        assertThat(settings.serviceVersion()).isEqualTo("2012-11-05");
        assertThat(settings.serviceSchema()).isSameAs(SERVICE_SCHEMA);
        assertThat(settings.baseExceptionBuilder()).isSameAs(exceptionBuilder);
    }

    @Test
    void build_onlyService_leavesOptionalValuesUnsetAndDefaultsExceptionBuilder() {
        ProtocolSettings settings = ProtocolSettings.builder().service(SERVICE_ID).build();

        assertThat(settings.serviceVersion()).isNull();
        assertThat(settings.serviceSchema()).isNull();
        assertThat(settings.baseExceptionBuilder().get().build()).isExactlyInstanceOf(SdkServiceException.class);
    }

    @Test
    void build_onlyServiceSchema_takesServiceFromSchema() {
        ProtocolSettings settings = ProtocolSettings.builder().serviceSchema(SERVICE_SCHEMA).build();

        assertThat(settings.service()).isEqualTo(SERVICE_ID);
    }

    @Test
    void build_serviceSetAfterSchema_acceptsMatchingIds() {
        ProtocolSettings settings = ProtocolSettings.builder().serviceSchema(SERVICE_SCHEMA).service(SERVICE_ID).build();

        assertThat(settings.service()).isEqualTo(SERVICE_ID);
    }

    @Test
    void build_neitherServiceNorSchema_throwsNullPointerException() {
        assertThatThrownBy(() -> ProtocolSettings.builder().serviceVersion("1").build())
            .isInstanceOf(NullPointerException.class)
            .hasMessage("service or serviceSchema must be set");
    }

    @Test
    void build_serviceDiffersFromSchemaId_throwsIllegalArgumentException() {
        ShapeId other = ShapeId.from("com.example#OtherService");

        assertThatThrownBy(() -> ProtocolSettings.builder().service(other).serviceSchema(SERVICE_SCHEMA).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("service com.example#OtherService does not match the ID of serviceSchema "
                        + "com.example#ExampleService");
    }

    @Test
    void build_nonServiceSchema_throwsIllegalArgumentException() {
        Schema operation = Schema.createOperation(ShapeId.from("com.example#GetThing"));

        assertThatThrownBy(() -> ProtocolSettings.builder().serviceSchema(operation).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("serviceSchema must have shape type service, but com.example#GetThing has shape type operation");
    }

    @Test
    void toBuilder_rebuilt_copiesEveryValue() {
        Supplier<SdkServiceException.Builder> exceptionBuilder = SdkServiceException::builder;
        ProtocolSettings settings = ProtocolSettings.builder()
                                                    .serviceSchema(SERVICE_SCHEMA)
                                                    .serviceVersion("1")
                                                    .baseExceptionBuilder(exceptionBuilder)
                                                    .build();

        ProtocolSettings copy = settings.toBuilder().serviceVersion("2").build();

        assertThat(copy.service()).isEqualTo(SERVICE_ID);
        assertThat(copy.serviceSchema()).isSameAs(SERVICE_SCHEMA);
        assertThat(copy.baseExceptionBuilder()).isSameAs(exceptionBuilder);
        assertThat(copy.serviceVersion()).isEqualTo("2");
        assertThat(settings.serviceVersion()).isEqualTo("1");
    }

    @Test
    void toString_namesServiceAndVersion() {
        ProtocolSettings settings = ProtocolSettings.builder().service(SERVICE_ID).serviceVersion("1").build();

        assertThat(settings).hasToString("ProtocolSettings(service=com.example#ExampleService, serviceVersion=1)");
    }
}
