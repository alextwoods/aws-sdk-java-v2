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

package software.amazon.awssdk.schema.traits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;

class TraitFactoriesTest {

    @Test
    void builtInFactories_haveUniqueIdsMatchingTheirTraitClasses() {
        List<TraitFactory<?>> factories = BuiltInTraitFactories.all();
        Set<ShapeId> ids = new HashSet<>();
        for (TraitFactory<?> factory : factories) {
            assertThat(ids.add(factory.traitId())).as("duplicate %s", factory.traitId()).isTrue();
            assertThat(TraitFactories.factoryFor(factory.traitId()).traitClass()).isEqualTo(factory.traitClass());
            assertThat(factory.traitClass().getPackage().getName()).isEqualTo("software.amazon.awssdk.schema.traits");
        }
        assertThat((long) factories.size()).isEqualTo(TypedTraitsTest.cases().count());
    }

    @Test
    void create_withBuiltInId_returnsTypedTrait() {
        Trait trait = TraitFactories.create(JsonNameTrait.ID, Document.of("Name"));

        assertThat(trait).isEqualTo(new JsonNameTrait("Name"));
        assertThat(TraitFactories.create(RequiredTrait.ID, Document.EMPTY_MAP)).isSameAs(RequiredTrait.INSTANCE);
    }

    @Test
    void create_withUnknownId_returnsGenericTraitWithSameIdAndValue() {
        ShapeId id = ShapeId.from("com.example#unknown");
        Document value = Document.of(Collections.singletonMap("a", Document.of(1)));

        Trait trait = TraitFactories.create(id, value);

        assertThat(trait).isInstanceOf(GenericTrait.class);
        assertThat(trait.traitId()).isEqualTo(id);
        assertThat(((GenericTrait) trait).value()).isSameAs(value);
        assertThat(TraitFactories.factoryFor(id)).isNull();
    }

    @Test
    void create_withUnknownIdAndNullValue_returnsGenericTraitWithNullValue() {
        ShapeId id = ShapeId.from("com.example#unknown");

        assertThat(TraitFactories.create(id, null)).isEqualTo(new GenericTrait(id, null));
    }

    @Test
    void create_withThirdPartyFactoryFromServiceLoader_returnsThirdPartyTrait() {
        Trait trait = TraitFactories.create(TestCustomTrait.ID, Document.of("custom"));

        assertThat(trait).isInstanceOf(TestCustomTrait.class);
        assertThat(((TestCustomTrait) trait).value()).isEqualTo("custom");
        assertThat(TraitFactories.factoryFor(TestCustomTrait.ID)).isInstanceOf(TestCustomTraitFactory.class);
    }

    @Test
    void create_withValueNotMatchingBuiltInTraitShape_throwsNamingTraitId() {
        assertThatThrownBy(() -> TraitFactories.create(HttpErrorTrait.ID, Document.of("400")))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#httpError");
        assertThatThrownBy(() -> TraitFactories.create(RequiredTrait.ID, Document.of(true)))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#required");
        assertThatThrownBy(() -> TraitFactories.create(JsonNameTrait.ID, null))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#jsonName");
    }

    @Test
    void create_withThirdPartyErrorMissingTraitId_addsTraitIdToMessage() {
        assertThatThrownBy(() -> TraitFactories.create(TestCustomTrait.ID, Document.of(1)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Invalid value for trait com.example.test#customTrait: expected a string")
            .hasCauseInstanceOf(SerializationException.class);
    }

    @Test
    void create_withNullTraitId_throwsNullPointerException() {
        assertThatThrownBy(() -> TraitFactories.create(null, Document.EMPTY_MAP)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TraitFactories.factoryFor(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void registry_withTwoFactoriesForOneId_failsNamingIdAndBothClasses() {
        List<TraitFactory<?>> discovered = Arrays.asList(new TestCustomTraitFactory(), new TestCustomTraitFactory());

        assertThatThrownBy(() -> new TraitFactoryRegistry(Collections.<TraitFactory<?>>emptyList(), discovered))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("com.example.test#customTrait")
            .hasMessageContaining(TestCustomTraitFactory.class.getName());
    }

    @Test
    void load_withServiceLoaderFactoryDuplicatingBuiltIn_failsNamingIdAndBothClasses(@TempDir Path dir)
        throws IOException {
        Path services = dir.resolve("META-INF/services");
        Files.createDirectories(services);
        Files.write(services.resolve(TraitFactory.class.getName()),
                    TestDuplicateRequiredTraitFactory.class.getName().getBytes(StandardCharsets.UTF_8));

        try (URLClassLoader loader = new URLClassLoader(new URL[] {dir.toUri().toURL()},
                                                        TraitFactoriesTest.class.getClassLoader())) {
            assertThatThrownBy(() -> TraitFactoryRegistry.load(loader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("smithy.api#required")
                .hasMessageContaining(BuiltInTraitFactories.class.getName())
                .hasMessageContaining(TestDuplicateRequiredTraitFactory.class.getName());
        }
    }

    @Test
    void load_withDefaultClassLoader_includesBuiltInsAndServiceLoaderFactories() {
        TraitFactoryRegistry registry = TraitFactoryRegistry.load(TraitFactoriesTest.class.getClassLoader());

        assertThat(registry.factoryFor(RequiredTrait.ID)).isNotNull();
        assertThat(registry.factoryFor(TestCustomTrait.ID)).isInstanceOf(TestCustomTraitFactory.class);
    }
}
