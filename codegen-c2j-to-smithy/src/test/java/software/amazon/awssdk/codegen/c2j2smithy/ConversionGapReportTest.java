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

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConversionGapReportTest {

    @Test
    void build_noRecords_isEmptyAndSerializesEmptyEntries() {
        ConversionGapReport report = ConversionGapReport.builder("account").build();

        assertThat(report.isEmpty()).isTrue();
        JsonNode json = TestModels.parse(report.toJson());
        assertThat(json.path("service").asText()).isEqualTo("account");
        assertThat(json.path("entries").isArray()).isTrue();
        assertThat(json.path("entries").size()).isZero();
    }

    @Test
    void record_sameProperty_countsOccurrencesAndKeepsFirstImpact() {
        ConversionGapReport report = ConversionGapReport.builder("sqs")
                                                        .record("operation.authtype", "first")
                                                        .record("operation.authtype", "second")
                                                        .record(C2jMarkerTrait.AUTHTYPE)
                                                        .build();

        assertThat(report.entries()).hasSize(1);
        ConversionGapReport.Entry entry = report.entries().get(0);
        assertThat(entry.property()).isEqualTo("operation.authtype");
        assertThat(entry.impact()).isEqualTo("first");
        assertThat(entry.occurrences()).isEqualTo(3);
        assertThat(entry.services()).containsExactly("sqs");
    }

    @Test
    void toJson_entries_sortedByPropertyWithServicesImpactAndOccurrences() {
        ConversionGapReport report = ConversionGapReport.builder("sns")
                                                        .record(C2jMarkerTrait.WRAPPER)
                                                        .record(C2jMarkerTrait.METADATA)
                                                        .record(C2jMarkerTrait.WRAPPER)
                                                        .build();

        JsonNode entries = TestModels.parse(report.toJson()).path("entries");

        assertThat(entries.size()).isEqualTo(2);
        assertThat(entries.get(0).path("property").asText()).isEqualTo("metadata");
        assertThat(entries.get(1).path("property").asText()).isEqualTo("shape.wrapper");
        assertThat(entries.get(1).path("services").get(0).asText()).isEqualTo("sns");
        assertThat(entries.get(1).path("impact").asText()).isEqualTo(C2jMarkerTrait.WRAPPER.impact());
        assertThat(entries.get(1).path("occurrences").asInt()).isEqualTo(2);
    }

    @Test
    void writeTo_buildDirectory_writesServiceFileUnderGapDirectory(@TempDir Path target) throws IOException {
        ConversionGapReport.builder("route53").record(C2jMarkerTrait.HTTP).build().writeTo(target);
        ConversionGapReport report = ConversionGapReport.builder("route53").record(C2jMarkerTrait.SYNTHETIC).build();

        Path written = report.writeTo(target);

        assertThat(written).isEqualTo(target.resolve("c2j-conversion-gaps").resolve("route53.json"));
        assertThat(new String(Files.readAllBytes(written), StandardCharsets.UTF_8)).isEqualTo(report.toJson());
    }

    @Test
    void fromJson_writtenReport_equalsOriginal() {
        ConversionGapReport report = TestModels.convertFixture("restjson").gapReport();

        assertThat(ConversionGapReport.fromJson(report.toJson())).isEqualTo(report);
    }

    @Test
    void fromJson_notAReport_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> ConversionGapReport.fromJson("{\"entries\": []}"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("service");
        assertThatThrownBy(() -> ConversionGapReport.fromJson("{"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid");
    }

    @Test
    void convert_anyService_producesReportForThatService() {
        ConversionResult result = TestModels.convertFixture("json11");

        assertThat(result.gapReport().service()).isEqualTo("json11");
        assertThat(result.gapReport().entries()).extracting(ConversionGapReport.Entry::property)
                                                .containsExactly("operation.http", "shape.required");
    }
}
