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

package software.amazon.awssdk.schema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.DefaultTrait;
import software.amazon.awssdk.schema.traits.RequiredTrait;
import software.amazon.awssdk.schema.traits.SensitiveTrait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;

/**
 * Port of smithy-java 1.7.0 {@code software.amazon.smithy.java.core.schema.TraitMapTest} (task 1.18). Adapted for SDK
 * traits: annotation traits are singletons ({@code X.INSTANCE}) and {@link DefaultTrait} takes a {@link Document}
 * instead of a smithy-model {@code Node}.
 */
class PortedTraitMapTest {

    @Test
    void emptyTraitMap() {
        TraitMap tm = TraitMap.create();

        assertThat(tm.get(TraitKey.REQUIRED_TRAIT)).isNull();
    }

    @Test
    void emptyTraitMapWithMemberTraits() {
        TraitMap tm = TraitMap.create();
        TraitMap tm2 = tm.withMemberTraits(TraitMap.create(SensitiveTrait.INSTANCE));

        assertThat(tm.get(TraitKey.SENSITIVE_TRAIT)).isNull();
        assertThat(tm2.get(TraitKey.SENSITIVE_TRAIT)).isInstanceOf(SensitiveTrait.class);
    }

    @Test
    void emptyGivenTraitMapWithMemberTraits() {
        TraitMap tm = TraitMap.create(SensitiveTrait.INSTANCE);
        TraitMap tm2 = tm.withMemberTraits(TraitMap.create());

        assertThat(tm.get(TraitKey.SENSITIVE_TRAIT)).isNotNull();
        assertThat(tm2).isSameAs(tm);
    }

    @Test
    void withMemberTraitsWithSmallerMin() {
        TraitMap tm = TraitMap.create(new DefaultTrait(Document.of(1)));
        TraitMap tm2 = tm.withMemberTraits(TraitMap.create(RequiredTrait.INSTANCE));

        assertThat(tm2.get(TraitKey.REQUIRED_TRAIT)).isInstanceOf(RequiredTrait.class);
        assertThat(tm2.get(TraitKey.DEFAULT_TRAIT)).isInstanceOf(DefaultTrait.class);
    }

    @Test
    void withMemberTraitsWithLargerMax() {
        TraitMap tm = TraitMap.create(RequiredTrait.INSTANCE);
        TraitMap tm2 = tm.withMemberTraits(TraitMap.create(new DefaultTrait(Document.of(1))));

        assertThat(tm2.get(TraitKey.REQUIRED_TRAIT)).isInstanceOf(RequiredTrait.class);
        assertThat(tm2.get(TraitKey.DEFAULT_TRAIT)).isInstanceOf(DefaultTrait.class);
    }

    @Test
    void withMemberTraitsWithMuchSmallerIndex() {
        TraitMap tm = TraitMap.create(new XmlNameTrait("hi"));
        TraitMap tm2 = tm.withMemberTraits(TraitMap.create(new DefaultTrait(Document.of(1))));

        assertThat(tm2.get(TraitKey.XML_NAME_TRAIT)).isInstanceOf(XmlNameTrait.class);
        assertThat(tm2.get(TraitKey.DEFAULT_TRAIT)).isInstanceOf(DefaultTrait.class);
    }
}
