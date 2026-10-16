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


package software.amazon.awssdk.codecs.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codecs.json.internal.SmithyJsonSerdeProvider;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.serde.document.DocumentEqualsFlags;

/**
 * Port of smithy-java {@code ParsingTest}: the JSONTestSuite parsing cases ({@code y_} must parse and round-trip,
 * {@code n_} must fail with {@link SerializationException}, {@code i_} may do either), run against the native provider
 * only (the Jackson provider is not ported).
 */
public class ParsingTest {
    @ParameterizedTest
    @MethodSource("parserTestCases")
    public void parserTestCases(JsonSerdeProvider provider, Path file) throws IOException {
        String filename = file.getFileName().toString();
        byte[] contents = Files.readAllBytes(file);
        JsonCodec codec = JsonCodec.builder()
                .overrideSerdeProvider(provider)
                .build();

        if (filename.startsWith("y_")) {
            try (ShapeDeserializer deser = codec.createDeserializer(contents)) {
                Document doc = deser.readDocument();
                if (doc != null) {
                    String str = codec.serializeToString(doc);
                    Document doc2 = codec.createDeserializer(str.getBytes(StandardCharsets.UTF_8)).readDocument();
                    if (!Document.equals(doc2, doc, DocumentEqualsFlags.NUMBER_PROMOTION)) {
                        assertThat(doc2).isEqualTo(doc);
                    }
                }
            }
        } else if (filename.startsWith("n_")) {
            Assertions.assertThrows(SerializationException.class, () -> {
                try (ShapeDeserializer deser = codec.createDeserializer(contents)) {
                    deser.readDocument();
                }
            }, "Expected SerializationError for `" + file + "`");
        } else if (filename.startsWith("i_")) {
            // Make sure that if there are errors, they're all SerializationExceptions.
            try (ShapeDeserializer deser = codec.createDeserializer(contents)) {
                deser.readDocument();
            } catch (SerializationException e) {
                System.out.println("i test failed " + filename + ": " + e.getMessage());
            }
        }
    }

    static List<Arguments> parserTestCases() throws IOException, URISyntaxException {
        SmithyJsonSerdeProvider smithyProvider = new SmithyJsonSerdeProvider();

        List<Arguments> arguments = new ArrayList<>();
        for (Path path : loadJsonFiles()) {
            arguments.add(Arguments.arguments(smithyProvider, path));
        }

        return arguments;
    }

    public static List<Path> loadJsonFiles() throws IOException, URISyntaxException {
        List<Path> files = new ArrayList<>();
        URL resource = Objects.requireNonNull(
                ParsingTest.class.getResource("tests/test_parsing"),
                "no test cases");
        Path dir = Paths.get(resource.toURI());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path path : stream) {
                files.add(path);
            }
        }
        return files;
    }
}
