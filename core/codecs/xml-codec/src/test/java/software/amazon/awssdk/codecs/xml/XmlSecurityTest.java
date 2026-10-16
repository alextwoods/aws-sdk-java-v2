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

package software.amazon.awssdk.codecs.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLInputFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codecs.xml.internal.XmlSerdeFactory;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;

/**
 * XML External Entity (XXE) and entity expansion hardening of both XML implementations. The native reader skips a
 * {@code <!DOCTYPE>} declaration without reading it, rejects an internal subset and resolves only the predefined
 * entities and character references; the StAX reader uses an {@link XMLInputFactory} with DTDs and external entities
 * disabled (the SDK's {@code XmlDomParser} settings). SDK addition.
 */
public class XmlSecurityTest extends ProviderTestBase {
    private static final String SECRET = "TOP-SECRET-FILE-CONTENT";
    private static final Schema DOC = Schema.structureBuilder(ShapeId.from("test#Doc"))
                                            .putMember("value", PreludeSchemas.STRING)
                                            .build();

    @TempDir
    Path tempDir;

    // A file that a vulnerable parser would read: its text and, as a DTD, an entity declaration followed by garbage
    // that would make the parse fail if the DTD were loaded.
    private String secretFileUri() throws IOException {
        Path file = tempDir.resolve("secret.dtd");
        Files.write(file, ("<!ENTITY secret '" + SECRET + "'> " + SECRET + " <<<").getBytes(StandardCharsets.UTF_8));
        return file.toUri().toString();
    }

    private static SchemaStruct read(boolean useNative, String xml) {
        try (XmlCodec codec = codec(useNative)) {
            return codec.deserializeShape(xml, SchemaStruct.builder(DOC));
        }
    }

    @PerProvider
    void deserialize_externalEntityInInternalSubset_isRejectedWithoutReadingTheFile(boolean useNative) throws IOException {
        String xml = "<!DOCTYPE Doc [<!ENTITY xxe SYSTEM \"" + secretFileUri() + "\">]><Doc><value>&xxe;</value></Doc>";

        assertThatThrownBy(() -> read(useNative, xml))
            .isInstanceOf(SerializationException.class)
            .satisfies(e -> assertThat(e.toString()).doesNotContain(SECRET));
    }

    @PerProvider
    void deserialize_entityDeclaredInExternalDtd_isRejectedWithoutReadingTheDtd(boolean useNative) throws IOException {
        String xml = "<!DOCTYPE Doc SYSTEM \"" + secretFileUri() + "\"><Doc><value>&secret;</value></Doc>";

        assertThatThrownBy(() -> read(useNative, xml))
            .isInstanceOf(SerializationException.class)
            .satisfies(e -> assertThat(e.toString()).doesNotContain(SECRET));
    }

    @PerProvider
    void deserialize_externalDtdReference_isIgnoredAndNotLoaded(boolean useNative) throws IOException {
        // The DTD file ends in garbage, so the read would fail if the DTD were fetched and parsed.
        String xml = "<!DOCTYPE Doc SYSTEM \"" + secretFileUri() + "\"><Doc><value>ok</value></Doc>";

        assertThat(read(useNative, xml).get("value")).isEqualTo("ok");
    }

    @PerProvider
    void deserialize_internalEntityExpansion_isRejected(boolean useNative) {
        String xml = "<!DOCTYPE Doc [<!ENTITY a \"aaaaaaaaaa\"><!ENTITY b \"&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;\">]>"
                     + "<Doc><value>&b;</value></Doc>";

        assertThatThrownBy(() -> read(useNative, xml)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void deserialize_undeclaredEntityWithoutDoctype_isRejected(boolean useNative) {
        assertThatThrownBy(() -> read(useNative, "<Doc><value>&nbsp;</value></Doc>"))
            .isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void deserialize_predefinedEntitiesAndCharacterReferences_areResolved(boolean useNative) {
        String xml = "<Doc><value>&lt;&gt;&amp;&quot;&apos;&#65;&#x42;</value></Doc>";

        assertThat(read(useNative, xml).get("value")).isEqualTo("<>&\"'AB");
    }

    @Test
    void newHardenedInputFactory_disablesDtdsAndExternalEntities() {
        XMLInputFactory factory = XmlSerdeFactory.newHardenedInputFactory();

        assertThat(factory.getProperty(XMLInputFactory.SUPPORT_DTD)).isEqualTo(false);
        assertThat(factory.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)).isEqualTo(false);
        assertThat(factory.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES)).isEqualTo(false);
        assertThat(factory.getProperty(XMLInputFactory.IS_COALESCING)).isEqualTo(false);
    }
}
