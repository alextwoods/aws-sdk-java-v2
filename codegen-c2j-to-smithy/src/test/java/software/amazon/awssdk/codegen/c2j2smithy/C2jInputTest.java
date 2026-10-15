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

package software.amazon.awssdk.codegen.c2j2smithy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class C2jInputTest {

    @Test
    void fromDirectory_withSomeSidecars_loadsOnlyPresentFiles(@TempDir Path dir) throws IOException {
        write(dir, C2jInput.SERVICE_MODEL_FILE, "{\"metadata\": {\"serviceId\": \"S\"}}");
        write(dir, C2jInput.PAGINATORS_FILE, "{\"pagination\": {}}");
        write(dir, C2jInput.ENDPOINT_RULE_SET_FILE, "{\"version\": \"1.0\"}");

        C2jInput models = C2jInput.fromDirectory("s", dir);

        assertThat(models.serviceName()).isEqualTo("s");
        assertThat(models.serviceModel().path("metadata").path("serviceId").asText()).isEqualTo("S");
        assertThat(models.paginatorsModel().has("pagination")).isTrue();
        assertThat(models.endpointRuleSetModel().path("version").asText()).isEqualTo("1.0");
        assertThat(models.waitersModel()).isNull();
        assertThat(models.endpointTestsModel()).isNull();
    }

    @Test
    void fromDirectory_withComments_ignoresThemLikeTheCodegenLoader(@TempDir Path dir) throws IOException {
        write(dir, C2jInput.SERVICE_MODEL_FILE, "{\n  // A line comment\n  \"metadata\": {/* a block comment */ "
                                                + "\"serviceId\": \"S\"}\n}");

        C2jInput models = C2jInput.fromDirectory("s", dir);

        assertThat(models.serviceModel().path("metadata").path("serviceId").asText()).isEqualTo("S");
    }

    @Test
    void fromDirectory_missingServiceModel_throwsIllegalArgumentException(@TempDir Path dir) {
        assertThatThrownBy(() -> C2jInput.fromDirectory("s", dir))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(C2jInput.SERVICE_MODEL_FILE);
    }

    @Test
    void build_missingServiceNameOrModel_throwsNullPointerException() {
        assertThatThrownBy(() -> C2jInput.builder().serviceModel(TestModels.parse("{}")).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("serviceName");
        assertThatThrownBy(() -> C2jInput.builder().serviceName("s").build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("serviceModel");
    }

    @Test
    void build_serviceModelNotAnObject_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> C2jInput.builder().serviceName("s").serviceModel(TestModels.parse("[]")).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("JSON object");
    }

    private static void write(Path dir, String file, String contents) throws IOException {
        Files.write(dir.resolve(file), contents.getBytes(StandardCharsets.UTF_8));
    }
}
