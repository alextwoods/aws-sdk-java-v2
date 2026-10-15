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

package software.amazon.awssdk.codegen.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.pipeline.PerOperationSerdeScanner.Rule;
import software.amazon.awssdk.codegen.pipeline.PerOperationSerdeScanner.Violation;

class PerOperationSerdeScannerTest {
    private static final String CLEAN_CLIENT =
        "package software.amazon.awssdk.services.demo;\n"
        + "import software.amazon.awssdk.services.demo.model.DemoOperations;\n"
        + "/** Unlike the legacy route, uses no {@code FooRequestMarshaller} from {@code demo.transform.Foo}. */\n"
        + "final class DefaultDemoClient {\n"
        + "    // switch (errorCode) is gone; ExceptionMetadata too.\n"
        + "    Object op() { return DemoOperations.FOO; }\n"
        + "    void transformed(AsyncResponseTransformer<?, ?> t, String errorCode) {\n"
        + "        switch (errorCode) { default: break; }\n"
        + "    }\n"
        + "}\n";

    /**
     * The per-operation exception mapper of the legacy JSON client golden file
     * ({@code codegen/.../poet/client/test-json-client-class.java}).
     */
    private static final String LEGACY_JSON_CLIENT_METHOD =
        "package software.amazon.awssdk.services.json;\n"
        + "final class DefaultJsonClient {\n"
        + "    void aPostOperation() {\n"
        + "        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {\n"
        + "            if (errorCode == null) {\n"
        + "                return Optional.empty();\n"
        + "            }\n"
        + "            switch (errorCode) {\n"
        + "                case \"InvalidInput\":\n"
        + "                    return Optional.of(ExceptionMetadata.builder().errorCode(\"InvalidInput\")\n"
        + "                        .httpStatusCode(400).exceptionBuilderSupplier(InvalidInputException::builder)\n"
        + "                        .build());\n"
        + "                default:\n"
        + "                    return Optional.empty();\n"
        + "            }\n"
        + "        };\n"
        + "    }\n"
        + "}\n";

    @TempDir
    Path tmp;

    @Test
    void scan_withSchemaBasedSources_reportsNothing() {
        assertThat(PerOperationSerdeScanner.scan("software/amazon/awssdk/services/demo/DefaultDemoClient.java",
                                                 CLEAN_CLIENT))
            .isEmpty();
    }

    @Test
    void scan_withTransformPackageClass_reportsPackageAndMarshaller() throws IOException {
        Path src = tmp.resolve("src");
        write(src, "software/amazon/awssdk/services/demo/transform/FooRequestMarshaller.java",
              "package software.amazon.awssdk.services.demo.transform;\n"
              + "public class FooRequestMarshaller implements Marshaller<FooRequest> {}\n");
        write(src, "software/amazon/awssdk/services/demo/DefaultDemoClient.java", CLEAN_CLIENT);

        List<Violation> violations = PerOperationSerdeScanner.scan(src, tmp.resolve("missing"));

        assertThat(violations).extracting(Violation::rule).containsExactly(Rule.TRANSFORM_PACKAGE, Rule.MARSHALLER_TYPE);
        assertThat(violations).extracting(Violation::file).containsOnly(
            "software/amazon/awssdk/services/demo/transform/FooRequestMarshaller.java");
    }

    @Test
    void scan_withFileInTransformDirectory_reportsPackage() {
        assertThat(rules("software/amazon/awssdk/services/demo/transform/Helper.java", "class Helper {}"))
            .containsExactly(Rule.TRANSFORM_PACKAGE);
    }

    @Test
    void scan_withTransformImport_reportsReference() {
        assertThat(rules("software/amazon/awssdk/services/demo/DefaultDemoClient.java",
                         "package software.amazon.awssdk.services.demo;\n"
                         + "import software.amazon.awssdk.services.demo.transform.*;\n"
                         + "class DefaultDemoClient {}\n"))
            .containsExactly(Rule.TRANSFORM_REFERENCE);
    }

    @Test
    void scan_withQualifiedMarshaller_reportsReferenceAndType() {
        // Not the name of an existing class (such as sdk-core's legacy Marshaller): the dependency analyzer would count
        // the string constant as a use of its module.
        List<Violation> violations = PerOperationSerdeScanner.scan(
            "software/amazon/awssdk/services/demo/DefaultDemoClient.java",
            "class DefaultDemoClient { demo.runtime.transform.Marshaller<Object> m; }");

        assertThat(violations).extracting(Violation::rule)
                              .containsExactly(Rule.TRANSFORM_REFERENCE, Rule.MARSHALLER_TYPE);
        assertThat(violations).extracting(Violation::match)
                              .containsExactly("demo.runtime.transform.Marshaller", "Marshaller");
    }

    @Test
    void scan_withLegacyJsonExceptionMapper_reportsSwitchAndExceptionMetadata() {
        List<Violation> violations = PerOperationSerdeScanner.scan(
            "software/amazon/awssdk/services/json/DefaultJsonClient.java", LEGACY_JSON_CLIENT_METHOD);

        assertThat(violations).extracting(Violation::rule)
                              .containsExactly(Rule.EXCEPTION_MAPPER_SWITCH, Rule.EXCEPTION_METADATA);
        assertThat(violations.get(0).match()).startsWith("exceptionMetadataMapper = errorCode -> {")
                                             .endsWith("switch (errorCode)");
    }

    @Test
    void scan_withEventStreamExceptionMapper_reportsSwitch() {
        assertThat(rules("software/amazon/awssdk/services/json/DefaultJsonAsyncClient.java",
                         "class DefaultJsonAsyncClient { void op() {\n"
                         + "Function<String, Optional<Object>> eventstreamExceptionMetadataMapper = errorCode -> {\n"
                         + "switch (errorCode) { default: return Optional.empty(); }\n"
                         + "}; } }"))
            .containsExactly(Rule.EXCEPTION_MAPPER_SWITCH);
    }

    @Test
    void scan_withRegisteredModeledException_reportsExceptionMetadata() {
        assertThat(rules("software/amazon/awssdk/services/query/DefaultQueryClient.java",
                         "class DefaultQueryClient { Object init() { return AwsQueryProtocolFactory.builder()\n"
                         + ".registerModeledException(ExceptionMetadata.builder().errorCode(\"InvalidInput\")\n"
                         + ".build()); } }"))
            .containsExactly(Rule.EXCEPTION_METADATA);
    }

    private static List<Rule> rules(String file, String source) {
        return PerOperationSerdeScanner.scan(file, source).stream().map(Violation::rule).collect(Collectors.toList());
    }

    private static void write(Path root, String file, String content) throws IOException {
        Path path = root.resolve(file);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
