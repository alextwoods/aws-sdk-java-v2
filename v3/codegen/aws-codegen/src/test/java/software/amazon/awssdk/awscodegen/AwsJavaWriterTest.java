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

package software.amazon.awssdk.awscodegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.Symbol;

class AwsJavaWriterTest {
    private static final String PACKAGE = "example.weather";

    @Test
    void importsFirstOwnerOfEachSimpleNameAndQualifiesLaterConflicts() {
        AwsJavaWriter writer = new AwsJavaWriter("example/weather/A.java", PACKAGE);
        writer.write("$T a;", type("java.util", "List"));
        writer.write("$T b;", type("java.lang", "String"));
        writer.write("$T c;", type(PACKAGE + ".model", "String"));
        writer.write("$T d;", type("java.awt", "List"));
        writer.write("$T e;", type(PACKAGE, "Local"));
        writer.write("$T f;", type("java.time", "Instant"));
        writer.write("$T g;", type("java.util", "List"));

        assertEquals(String.join("\n",
                                 "/*",
                                 " * " + AwsJavaWriter.GENERATED_HEADER,
                                 " */",
                                 "",
                                 "package example.weather;",
                                 "",
                                 "import java.time.Instant;",
                                 "import java.util.List;",
                                 "",
                                 "List a;",
                                 "String b;",
                                 "example.weather.model.String c;",
                                 "java.awt.List d;",
                                 "Local e;",
                                 "Instant f;",
                                 "List g;",
                                 ""),
                     writer.toString());
    }

    /** A modeled IllegalArgumentException error in the file's package shadows java.lang's. */
    @Test
    void javaLangTypesShadowedByAGeneratedTypeOfThePackageAreQualified() {
        AwsJavaWriter writer = new AwsJavaWriter("example/weather/A.java", PACKAGE, () -> {
        }, Set.of("IllegalArgumentException"));
        writer.write("$T a;", type("java.lang", "IllegalArgumentException"));
        writer.write("$T b;", type(PACKAGE, "IllegalArgumentException"));
        writer.write("$T c;", type("java.lang", "IllegalStateException"));
        assertEquals(String.join("\n",
                                 "/*",
                                 " * " + AwsJavaWriter.GENERATED_HEADER,
                                 " */",
                                 "",
                                 "package example.weather;",
                                 "",
                                 "java.lang.IllegalArgumentException a;",
                                 "IllegalArgumentException b;",
                                 "IllegalStateException c;",
                                 ""),
                     writer.toString());
    }

    @Test
    void omitsPackageDeclarationForTheDefaultPackageAndTrimsTrailingSpaces() {
        AwsJavaWriter writer = new AwsJavaWriter("A.java", "");
        writer.write("class A {   ");
        writer.write("}");
        assertEquals("/*\n * " + AwsJavaWriter.GENERATED_HEADER + "\n */\n\nclass A {\n}\n", writer.toString());
    }

    @Test
    void onlyWritesJavaSourcesAndSymbols() {
        assertThrows(CodegenException.class, () -> new AwsJavaWriter("META-INF/service.json", PACKAGE));
        AwsJavaWriter writer = new AwsJavaWriter("example/weather/A.java", PACKAGE);
        assertThrows(RuntimeException.class, () -> writer.write("$T", "java.lang.String"));
    }

    @Test
    void rejectsCollectionsWithoutADefinedIterationOrder() {
        AwsJavaWriter writer = new AwsJavaWriter("example/weather/A.java", PACKAGE);
        for (Object unordered : List.of(Set.of("b", "a"), new HashSet<>(List.of("a")), Map.of("k", "v"),
                                        Optional.of(Set.of("a")))) {
            assertThrows(CodegenException.class, () -> writer.write("$L", unordered), unordered.toString());
            assertThrows(CodegenException.class, () -> writer.write("$S", unordered), unordered.toString());
        }
        writer.write("$L $L $L $S", List.of("b", "a"), new TreeSet<>(Set.of("b", "a")),
                     new TreeMap<>(Map.of("k", "v")), 1);
        assertTrue(writer.toString().endsWith("\n[b, a] [a, b] {k=v} \"1\"\n"), writer.toString());
    }

    private static Symbol type(String namespace, String name) {
        return Symbol.builder().namespace(namespace, ".").name(name).build();
    }
}
