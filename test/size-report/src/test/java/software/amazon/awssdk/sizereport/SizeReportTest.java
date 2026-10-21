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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static software.amazon.awssdk.sizereport.SyntheticClasses.CLIENT;
import static software.amazon.awssdk.sizereport.SyntheticClasses.OPERATIONS;
import static software.amazon.awssdk.sizereport.SyntheticClasses.SERVICE;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SizeReportTest {
    private static final String JAVA_VERSION = "21.0.2";
    private static final String JAVA_VENDOR = "Test Vendor";

    @TempDir
    Path dir;

    private Path baselineDir;
    private Path featureDir;
    private Path disabled;

    @BeforeEach
    void setUp() throws IOException {
        baselineDir = dir.resolve("baseline");
        featureDir = dir.resolve("feature");
        Files.createDirectories(baselineDir.resolve("jars"));
        Files.createDirectories(featureDir);
        writeBuildInfo("foo", JAVA_VERSION, JAVA_VENDOR);

        Map<String, byte[]> baseline = new LinkedHashMap<>();
        baseline.put(CLIENT, SyntheticClasses.client(true));
        baseline.put(SERVICE + "/transform/GetBarRequestMarshaller",
                     SyntheticClasses.plain(SERVICE + "/transform/GetBarRequestMarshaller", 3));
        writeJar(baselineDir.resolve("jars/foo.jar"), baseline);
        writeJar(baselineDir.resolve("jars/sdk-core.jar"),
                 Collections.singletonMap("core/Core", SyntheticClasses.plain("core/Core", 1)));
        writeJar(baselineDir.resolve("jars/utils.jar"),
                 Collections.singletonMap("utils/Utils", SyntheticClasses.plain("utils/Utils", 1)));
        writeJar(baselineDir.resolve("jars/protocol-core.jar"),
                 Collections.singletonMap("protocol/P", SyntheticClasses.plain("protocol/P", 1)));

        Map<String, byte[]> feature = new LinkedHashMap<>();
        feature.put(CLIENT, SyntheticClasses.client(false));
        feature.put(OPERATIONS, SyntheticClasses.operations(true));
        feature.put(SERVICE + "/model/FooSchemas", SyntheticClasses.plain(SERVICE + "/model/FooSchemas", 40));
        writeJar(featureDir.resolve("foo.jar"), feature);
        writeJar(featureDir.resolve("sdk-core.jar"),
                 Collections.singletonMap("core/Core", SyntheticClasses.plain("core/Core", 2)));
        writeJar(featureDir.resolve("utils.jar"),
                 Collections.singletonMap("utils/Utils", SyntheticClasses.plain("utils/Utils", 1)));
        writeJar(featureDir.resolve("schema.jar"),
                 Collections.singletonMap("schema/Schema", SyntheticClasses.plain("schema/Schema", 1)));

        disabled = dir.resolve("disabled-customizations.md");
        Files.write(disabled, Arrays.asList(
            "# Disabled customizations",
            "| Service | Customization | Reason |",
            "|---|---|---|",
            "| Foo | `shapeModifiers` | not supported |",
            "| Other | `x` | y |",
            "",
            "Text."), StandardCharsets.UTF_8);
    }

    @Test
    void generate_syntheticBuilds_rendersEveryRequirementSection() throws IOException {
        String report = report().generate();

        assertThat(report)
            .startsWith("# Size_Report\n")
            .contains("## Builds (Requirement 36.7)")
            .contains("| JDK | 21.0.2 (Test Vendor) | 21.0.2 (Test Vendor) |")
            .contains("| Foo | `shapeModifiers` (not supported) |")
            .contains("**Fails**: the JAR grows; categories with a net increase: Schemas, Type_Registries")
            .contains("### Change by category (Requirement 36.2)")
            .contains("| Removed marshallers and unmarshallers | ")
            .contains("| `DefaultFooClient` | Baseline_Build | Client classes | Removed exception-mapping code | 4 methods")
            .contains("| `model.FooOperations` | Feature build | Client classes | Type_Registries | 1 method, "
                      + "1 static field (`ERROR_REGISTRY`), 1 `<clinit>` segment")
            .contains("| `test.svc.transform` | 1 |")
            .contains("### Change by category, all services (Requirement 36.2)")
            .contains("| Category | Foo | All services |")
            .contains("### Model-version differences")
            .contains("### Schema-serde modules")
            .contains("| `schema` | Runtime_Schema_Module | added | 0 |")
            .contains("| `sdk-core` | changed |")
            .contains("| `protocol-core` | no longer a dependency |")
            .contains("Unchanged (same content in both builds): `utils`.")
            .contains("## Size-reduction opportunities (Requirement 36.5)")
            .contains("| | JAR change today (Summary) | |")
            .contains("| | Requirement 36.3 with all opportunities | |");
    }

    @ParameterizedTest
    @CsvSource({"schema,Runtime_Schema_Module", "codec-commons,Codec", "json-codec,Codec", "http-binding,HTTP binding",
                "aws-json-client-protocol,Client protocol"})
    void moduleKind_schemaSerdeModule_namesItsKind(String artifactId, String kind) {
        assertThat(SizeReportWriter.moduleKind(artifactId)).isEqualTo(kind);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sdk-core", "aws-json-protocol", "protocol-core", "json-utils"})
    void moduleKind_otherModule_isNull(String artifactId) {
        assertThat(SizeReportWriter.moduleKind(artifactId)).isNull();
    }

    @Test
    void generate_differentJdk_failsNamingBothJdks() throws IOException {
        writeBuildInfo("foo", "17.0.1", JAVA_VENDOR);

        assertThatThrownBy(() -> report().generate())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("compiled with JDK 17.0.1")
            .hasMessageContaining("runs JDK 21.0.2")
            .hasMessageContaining(SizeReport.BUILD_SCRIPT);
    }

    @Test
    void generate_serviceMissingFromBaselineBuild_failsWithRebuildCommand() throws IOException {
        writeBuildInfo("bar", JAVA_VERSION, JAVA_VENDOR);

        assertThatThrownBy(() -> report().generate())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("does not include [foo]")
            .hasMessageContaining("--modules foo");
    }

    @Test
    void generate_noBaselineBuild_failsWithBuildCommand() {
        SizeReport report = SizeReport.builder()
                                      .baselineDir(dir.resolve("missing"))
                                      .featureJarsDir(featureDir)
                                      .services(Collections.singletonMap("foo", "Foo"))
                                      .disabledCustomizations(disabled)
                                      .featureJdk(JAVA_VERSION, JAVA_VENDOR)
                                      .build();

        assertThatThrownBy(report::generate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No Baseline_Build in")
            .hasMessageContaining(SizeReport.BUILD_SCRIPT + " --modules foo");
    }

    @Test
    void generate_withLibraries_rendersChangedAndNewLibrariesOutsideTheModules() throws IOException {
        writeBuildInfo("foo", JAVA_VERSION, JAVA_VENDOR, "lib", "newlib");
        writeJar(baselineDir.resolve("jars/lib.jar"),
                 Collections.singletonMap("lib/Reader", SyntheticClasses.plain("lib/Reader", 3)));
        Map<String, byte[]> lib = new LinkedHashMap<>();
        lib.put("lib/Reader", SyntheticClasses.plain("lib/Reader", 1));
        lib.put("lib/Serde", SyntheticClasses.plain("lib/Serde", 5));
        writeJar(featureDir.resolve("lib.jar"), lib);
        writeJar(featureDir.resolve("newlib.jar"),
                 Collections.singletonMap("newlib/Payloads", SyntheticClasses.plain("newlib/Payloads", 2)));

        String report = libraryReport().generate();

        assertThat(report)
            .contains("## Libraries (Requirement 38.2)")
            .containsPattern("\\| Lib \\(`lib`\\) \\| changed \\| [0-9,]+ → [0-9,]+ \\| [-+][0-9,]+ \\([-+][0-9.]+%\\) "
                             + "\\| [0-9,]+ → [0-9,]+ \\| \\+[0-9,]+ \\(\\+[0-9.]+%\\) \\| 1 → 2 \\|")
            .containsPattern("\\| New lib \\(`newlib`\\) \\| new \\(not in the Baseline_Build commit\\) "
                             + "\\| 0 → [0-9,]+ \\| \\+[0-9,]+ \\| 0 → [0-9,]+ \\| \\+[0-9,]+ \\| 0 → 1 \\|")
            .contains("- **Libraries (38.2).**")
            .doesNotContain("| `lib` |")
            .doesNotContain("| `newlib` |");
    }

    @Test
    void generate_libraryMissingFromBaselineBuild_failsWithRebuildCommand() throws IOException {
        writeBuildInfo("foo", JAVA_VERSION, JAVA_VENDOR, "lib", "");

        assertThatThrownBy(() -> libraryReport().generate())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("does not include [newlib]")
            .hasMessageContaining("--modules foo --libraries lib,newlib");
    }

    @Test
    void generate_withoutLibraries_omitsTheLibrariesSection() throws IOException {
        assertThat(report().generate()).doesNotContain("## Libraries").doesNotContain("**Libraries (38.2).**");
    }

    @Test
    void parseServices_list_keepsOrder() {
        assertThat(SizeReport.parseServices("cloudwatch=CloudWatch, sqs=SQS"))
            .containsExactly(entry("cloudwatch", "CloudWatch"), entry("sqs", "SQS"));
    }

    @Test
    void parseServices_missingDisplayName_fails() {
        assertThatThrownBy(() -> SizeReport.parseServices("cloudwatch"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("artifactId=Display name");
    }

    private SizeReport report() {
        return SizeReport.builder()
                         .baselineDir(baselineDir)
                         .featureJarsDir(featureDir)
                         .services(Collections.singletonMap("foo", "Foo"))
                         .disabledCustomizations(disabled)
                         .featureJdk(JAVA_VERSION, JAVA_VENDOR)
                         .build();
    }

    private SizeReport libraryReport() {
        Map<String, String> libraries = new LinkedHashMap<>();
        libraries.put("lib", "Lib");
        libraries.put("newlib", "New lib");
        return SizeReport.builder()
                         .baselineDir(baselineDir)
                         .featureJarsDir(featureDir)
                         .services(Collections.singletonMap("foo", "Foo"))
                         .libraries(libraries)
                         .disabledCustomizations(disabled)
                         .featureJdk(JAVA_VERSION, JAVA_VENDOR)
                         .build();
    }

    private void writeBuildInfo(String modules, String javaVersion, String javaVendor) throws IOException {
        writeBuildInfo(modules, javaVersion, javaVendor, "", "");
    }

    private void writeBuildInfo(String modules, String javaVersion, String javaVendor, String libraries,
                                String absentLibraries) throws IOException {
        Files.write(baselineDir.resolve("build-info.properties"), Arrays.asList(
            "baseline.commit=0123456789abcdef0123456789abcdef01234567",
            "baseline.modules=" + modules,
            "baseline.libraries=" + libraries,
            "baseline.absentLibraries=" + absentLibraries,
            "java.version=" + javaVersion,
            "java.vendor=" + javaVendor,
            "maven.version=3.9.16",
            "maven.flags=install -P quick -pl <modules> -am",
            "compiler.options=maven.compiler.release=8"), StandardCharsets.UTF_8);
    }

    private static void writeJar(Path file, Map<String, byte[]> classes) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Build-Jdk-Spec"), "21");
        try (OutputStream out = Files.newOutputStream(file);
             JarOutputStream jar = new JarOutputStream(out, manifest)) {
            for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
                jar.putNextEntry(new ZipEntry(entry.getKey() + ".class"));
                jar.write(entry.getValue());
                jar.closeEntry();
            }
        }
    }
}
