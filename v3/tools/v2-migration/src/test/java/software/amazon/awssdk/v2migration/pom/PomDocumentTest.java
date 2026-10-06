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

package software.amazon.awssdk.v2migration.pom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PomDocumentTest {

    static final String POM = String.join("\n",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<!-- An application <dependency> in a comment is not a dependency. -->",
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">",
        "    <modelVersion>4.0.0</modelVersion>",
        "    <groupId>com.example</groupId>",
        "    <artifactId>app</artifactId>",
        "    <version>1.0</version>",
        "    <properties>",
        "        <awssdk.version>2.25.0</awssdk.version>",
        "    </properties>",
        "    <dependencyManagement>",
        "        <dependencies>",
        "            <dependency>",
        "                <groupId>software.amazon.awssdk</groupId>",
        "                <artifactId>bom</artifactId>",
        "                <version>${awssdk.version}</version>",
        "                <type>pom</type>",
        "                <scope>import</scope>",
        "            </dependency>",
        "        </dependencies>",
        "    </dependencyManagement>",
        "    <dependencies>",
        "        <dependency>",
        "            <groupId>software.amazon.awssdk</groupId>",
        "            <artifactId>s3</artifactId>",
        "        </dependency>",
        "        <dependency>",
        "            <groupId>software.amazon.awssdk</groupId>",
        "            <artifactId>dynamodb</artifactId>",
        "            <version>3.0.0</version>",
        "        </dependency>",
        "        <dependency>",
        "            <groupId>com.example</groupId>",
        "            <artifactId>lib</artifactId>",
        "            <version>${project.version}</version>",
        "            <scope>test</scope>",
        "        </dependency>",
        "    </dependencies>",
        "    <profiles>",
        "        <profile>",
        "            <id>netty</id>",
        "            <dependencies>",
        "                <dependency><groupId>software.amazon.awssdk</groupId><artifactId>netty-nio-client</artifactId>"
        + "<version><![CDATA[2.25.1]]></version></dependency>",
        "            </dependencies>",
        "        </profile>",
        "    </profiles>",
        "</project>",
        "");

    @Test
    void parse_pom_locatesEveryDependencySectionWithExactOffsets() {
        PomDocument pom = PomDocument.parse("pom.xml", POM);

        Map<String, List<XmlElement>> sections = pom.sections();
        assertEquals(List.of("dependencies", "dependencyManagement", "profile:netty/dependencies",
                             "profile:netty/dependencyManagement"), List.copyOf(sections.keySet()));
        assertEquals(3, sections.get("dependencies").size());
        XmlElement s3 = pom.dependency("dependencies", 0).orElseThrow();
        assertEquals(POM.substring(POM.indexOf("<dependency>\n            <groupId>software.amazon.awssdk</groupId>\n"
                                               + "            <artifactId>s3</artifactId>")),
                     POM.substring(s3.start()));
        assertTrue(s3.source().startsWith("<dependency>") && s3.source().endsWith("</dependency>"));
        XmlElement bomVersion = pom.dependency("dependencyManagement", 0).orElseThrow().child("version").orElseThrow();
        assertEquals("${awssdk.version}", POM.substring(bomVersion.contentStart(), bomVersion.contentEnd()));
        assertEquals("2.25.1", pom.dependency("profile:netty/dependencies", 0).orElseThrow()
                                  .childText("version").orElseThrow());
    }

    @Test
    void standaloneDependencies_pom_interpolatesAndAppliesManagementAndTheV2Bom() {
        ProjectDependencies dependencies = PomDocument.parse("pom.xml", POM).standaloneDependencies();

        assertEquals(List.of(
            new ProjectDependencies.Declared("dependencies", 0, "software.amazon.awssdk", "s3", null, "2.25.0", null,
                                             null, null),
            new ProjectDependencies.Declared("dependencies", 1, "software.amazon.awssdk", "dynamodb", "3.0.0", "3.0.0",
                                             null, null, null),
            new ProjectDependencies.Declared("dependencies", 2, "com.example", "lib", "${project.version}", "1.0",
                                             null, null, "test"),
            new ProjectDependencies.Declared("dependencyManagement", 0, "software.amazon.awssdk", "bom",
                                             "${awssdk.version}", "2.25.0", "pom", null, "import"),
            new ProjectDependencies.Declared("profile:netty/dependencies", 0, "software.amazon.awssdk",
                                             "netty-nio-client", "2.25.1", "2.25.1", null, null, null)),
                     dependencies.declared());
        assertTrue(dependencies.declared().get(3).isBomImport());
        assertEquals(List.of(), dependencies.resolved());
    }

    @Test
    void parse_text_decodesEntitiesAndCharacterReferences() {
        PomDocument pom = PomDocument.parse("pom.xml", "<project><name>a &amp; b &#x41;&#66; &lt;c&gt;</name></project>");

        assertEquals("a & b AB <c>", pom.project().childText("name").orElseThrow());
    }

    @Test
    void parse_malformedXml_reportsTheLine() {
        XmlFormatException mismatched = assertThrows(XmlFormatException.class,
                                                     () -> PomDocument.parse("pom.xml", "<project>\n<a>\n</b>\n</project>"));
        assertEquals("pom.xml:3: end tag </b> does not match <a>", mismatched.getMessage());

        XmlFormatException unclosed = assertThrows(XmlFormatException.class,
                                                   () -> PomDocument.parse("pom.xml", "<project>\n  <a>"));
        assertEquals("pom.xml:2: element <a> is not closed", unclosed.getMessage());

        XmlFormatException notPom = assertThrows(XmlFormatException.class,
                                                 () -> PomDocument.parse("pom.xml", "<settings/>"));
        assertEquals("pom.xml:1: the root element is <settings>, not <project>", notPom.getMessage());
    }
}
