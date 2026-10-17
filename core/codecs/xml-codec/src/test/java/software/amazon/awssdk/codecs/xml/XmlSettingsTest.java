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
import static software.amazon.awssdk.codecs.xml.TestCollections.listOf;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;

/**
 * {@link XmlSettings}, its presets, and the settings of {@link XmlCodec}. SDK addition.
 */
public class XmlSettingsTest {

    @Test
    void presets_haveRequirement83Values() {
        for (XmlSettings preset : listOf(XmlSettings.REST_XML, XmlSettings.AWS_QUERY_RESPONSE)) {
            assertThat(preset.useXmlName()).isTrue();
            assertThat(preset.useTimestampFormat()).isTrue();
            assertThat(preset.defaultTimestampFormat()).isEqualTo(TimestampFormatTrait.Format.DATE_TIME);
            assertThat(preset.defaultNamespace()).isNull();
            assertThat(preset.defaultNamespacePrefix()).isNull();
            assertThat(preset.defaultXmlNamespace()).isNull();
            assertThat(preset.forbidUnknownUnionMembers()).isFalse();
        }
        assertThat(XmlSettings.builder().build()).isEqualTo(XmlSettings.REST_XML);
    }

    @Test
    void toBuilder_copiesEverySetting() {
        XmlSettings settings = XmlSettings.builder()
                                          .useXmlName(false)
                                          .useTimestampFormat(false)
                                          .defaultTimestampFormat(TimestampFormatTrait.Format.EPOCH_SECONDS)
                                          .defaultNamespace("https://example.com/ns")
                                          .defaultNamespacePrefix("ex")
                                          .forbidUnknownUnionMembers(true)
                                          .build();

        assertThat(settings.toBuilder().build()).isEqualTo(settings).hasSameHashCodeAs(settings);
        assertThat(XmlSettings.builder().updateBuilder(settings).build()).isEqualTo(settings);
        assertThat(settings).isNotEqualTo(XmlSettings.REST_XML);
        assertThat(settings.defaultXmlNamespace()).isEqualTo(new XmlNamespaceTrait("https://example.com/ns", "ex"));
        assertThat(settings.toString()).isEqualTo("XmlSettings(useTimestampFormat=false, defaultTimestampFormat=epoch-seconds, "
                                                  + "defaultNamespace=https://example.com/ns, "
                                                  + "forbidUnknownUnionMembers=true, useXmlName=false, "
                                                  + "defaultNamespacePrefix=ex)");
    }

    @Test
    void equals_comparesXmlSettings() {
        assertThat(XmlSettings.builder().useXmlName(false).build()).isNotEqualTo(XmlSettings.REST_XML);
        assertThat(XmlSettings.builder().defaultNamespace("a").defaultNamespacePrefix("p").build())
            .isNotEqualTo(XmlSettings.builder().defaultNamespace("a").build());
    }

    @Test
    void defaultXmlNamespace_setsAndClearsUriAndPrefix() {
        XmlSettings.Builder builder = XmlSettings.builder().defaultXmlNamespace(new XmlNamespaceTrait("urn:a", "p"));
        assertThat(builder.build().defaultNamespace()).isEqualTo("urn:a");
        assertThat(builder.build().defaultNamespacePrefix()).isEqualTo("p");

        XmlSettings cleared = builder.defaultXmlNamespace(null).build();
        assertThat(cleared.defaultNamespace()).isNull();
        assertThat(cleared.defaultNamespacePrefix()).isNull();
    }

    @Test
    void build_prefixWithoutNamespace_throws() {
        assertThatThrownBy(() -> XmlSettings.builder().defaultNamespacePrefix("p").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("defaultNamespacePrefix requires a defaultNamespace");
    }

    @Test
    void codecBuilder_settingsThenOverride_changesOnlyTheOverriddenValue() {
        XmlSettings base = XmlSettings.builder().useXmlName(false).defaultNamespace("urn:x").build();

        XmlCodec codec = XmlCodec.builder()
                                 .settings(base)
                                 .defaultTimestampFormat(TimestampFormatTrait.Format.HTTP_DATE)
                                 .build();

        assertThat(codec.settings().useXmlName()).isFalse();
        assertThat(codec.settings().defaultNamespace()).isEqualTo("urn:x");
        assertThat(codec.settings().defaultTimestampFormat()).isEqualTo(TimestampFormatTrait.Format.HTTP_DATE);
    }

    @Test
    void codecBuilder_defaults() {
        XmlCodec codec = XmlCodec.builder().build();

        assertThat(codec.settings()).isEqualTo(XmlSettings.REST_XML);
        assertThat(codec.wrapperElements()).isEmpty();
        assertThat(codec.strictRootElement()).isTrue();
        assertThat(codec.xmlDeclaration()).isFalse();
        assertThat(codec.toString()).isEqualTo("XmlCodec(" + XmlSettings.REST_XML + ", wrapperElements=[], "
                                               + "strictRootElement=true, xmlDeclaration=false)");
    }

    @Test
    void codecBuilder_xmlDeclaration_isReported() {
        XmlCodec codec = XmlCodec.builder().xmlDeclaration(true).build();

        assertThat(codec.xmlDeclaration()).isTrue();
        assertThat(codec.toString()).endsWith("xmlDeclaration=true)");
    }

    @Test
    void codecBuilder_wrapperElements_copiesAndRejectsNull() {
        List<String> wrappers = new ArrayList<>(listOf("OpResponse", "OpResult"));
        XmlCodec codec = XmlCodec.builder().wrapperElements(wrappers).build();
        wrappers.clear();

        assertThat(codec.wrapperElements()).containsExactly("OpResponse", "OpResult");
        assertThatThrownBy(() -> codec.wrapperElements().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> XmlCodec.builder().wrapperElements(null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("wrapperElements must not be null");
        assertThatThrownBy(() -> XmlCodec.builder().wrapperElements(listOf("a", null)))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("must not contain null");
    }

    @Test
    void codecBuilder_unknownTimestampFormat_throws() {
        assertThatThrownBy(() -> XmlCodec.builder().defaultTimestampFormat(TimestampFormatTrait.Format.UNKNOWN))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
