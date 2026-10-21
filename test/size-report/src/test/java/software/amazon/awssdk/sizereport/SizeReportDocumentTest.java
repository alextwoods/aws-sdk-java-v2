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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * Builds the Size_Report from the Baseline_Build and the feature build JARs and checks that
 * {@code docs/design/schema-serde/size-report.md} matches it (Requirements 36.1, 36.2, 36.6 and 36.7). With
 * {@code -Dsize-report.update=true} it rewrites the file instead.
 *
 * <p>The inputs come from the module's {@code pom.xml}: {@code size-report.baselineDir} (written by
 * {@code scripts/schema-serde/build-size-baseline.sh}), the feature JARs copied by {@code maven-dependency-plugin},
 * {@code size-report.services} and {@code size-report.libraries}.
 */
class SizeReportDocumentTest {

    @Test
    void sizeReport_currentBuilds_matchesTheCommittedDocument() throws IOException {
        Path document = path("size-report.document");
        SizeReport report = SizeReport.builder()
                                      .baselineDir(path("size-report.baselineDir"))
                                      .featureJarsDir(path("size-report.featureJarsDir"))
                                      .services(SizeReport.parseServices(property("size-report.services")))
                                      .libraries(SizeReport.parseServices(property("size-report.libraries")))
                                      .disabledCustomizations(path("size-report.disabledCustomizations"))
                                      // Surefire forks the JDK that runs Maven, which compiled the feature build.
                                      .featureJdk(System.getProperty("java.version"),
                                                  System.getProperty("java.vendor"))
                                      .build();

        String expected = report.generate();

        if (Boolean.parseBoolean(System.getProperty("size-report.update", "false"))) {
            Files.write(document, expected.getBytes(StandardCharsets.UTF_8));
            return;
        }
        assertThat(document).as("%s does not exist; run with -Dsize-report.update=true", document).isRegularFile();
        String actual = new String(Files.readAllBytes(document), StandardCharsets.UTF_8);
        assertThat(actual)
            .as("%s is out of date. Regenerate it with mvn clean install -pl :size-report -Dsize-report.update=true",
                document)
            .isEqualTo(expected);
    }

    private static String property(String name) {
        String value = System.getProperty(name);
        assertThat(value).as("system property %s (set by the size-report pom)", name).isNotBlank();
        return value;
    }

    private static Path path(String name) {
        return Paths.get(property(name)).toAbsolutePath().normalize();
    }
}
