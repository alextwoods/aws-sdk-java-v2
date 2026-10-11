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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.GenericTrait;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.RequiredTrait;
import software.amazon.awssdk.schema.traits.Rpcv2CborTrait;
import software.amazon.awssdk.schema.traits.SensitiveTrait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;

class TraitMapTest {
    private static final ShapeId TAGGING = ShapeId.from("com.example#tagging");
    private static final ShapeId OWNER = ShapeId.from("com.example#owner");

    @Test
    void create_noTraits_returnsEmptyMap() {
        TraitMap map = TraitMap.create();
        assertThat(map).isSameAs(TraitMap.EMPTY);
        assertThat(map.isEmpty()).isTrue();
        assertThat(map.get(TraitKey.REQUIRED_TRAIT)).isNull();
        assertThat(map.getGeneric(TAGGING)).isNull();
        assertThat(map.traits()).isEmpty();
    }

    @Test
    void get_presentTrait_returnsSameInstance() {
        JsonNameTrait jsonName = new JsonNameTrait("name");
        TraitMap map = TraitMap.create(RequiredTrait.INSTANCE, jsonName);

        assertThat(map.get(TraitKey.REQUIRED_TRAIT)).isSameAs(RequiredTrait.INSTANCE);
        assertThat(map.get(TraitKey.JSON_NAME_TRAIT)).isSameAs(jsonName);
        assertThat(map.contains(TraitKey.REQUIRED_TRAIT)).isTrue();
        assertThat(map.isEmpty()).isFalse();
    }

    @Test
    void get_absentTrait_returnsNull() {
        TraitMap map = TraitMap.create(RequiredTrait.INSTANCE, new JsonNameTrait("name"));

        assertThat(map.get(TraitKey.SENSITIVE_TRAIT)).isNull();
        assertThat(map.contains(TraitKey.SENSITIVE_TRAIT)).isFalse();
    }

    @Test
    void get_keyIdBeyondStorage_returnsNull() {
        // REQUIRED_TRAIT has index 0, so the storage has one slot.
        TraitMap map = TraitMap.create(RequiredTrait.INSTANCE);

        assertThat(map.get(TraitKey.RPCV2_CBOR_TRAIT)).isNull();
        assertThat(map.get(TraitKey.get(TestTraits.NeverStored.class))).isNull();
        assertThat(map.contains(TraitKey.RPCV2_CBOR_TRAIT)).isFalse();
    }

    @Test
    void get_thirdPartyTypedTrait_isFoundByItsKey() {
        TestTraits.ThirdParty trait = new TestTraits.ThirdParty();
        TraitMap map = TraitMap.create(trait, RequiredTrait.INSTANCE);

        assertThat(map.get(TraitKey.get(TestTraits.ThirdParty.class))).isSameAs(trait);
        assertThat(map.get(TraitKey.REQUIRED_TRAIT)).isSameAs(RequiredTrait.INSTANCE);
    }

    @Test
    void get_doesNotAllocate() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled());

        TraitMap map = TraitMap.create(RequiredTrait.INSTANCE, new JsonNameTrait("name"));
        long threadId = Thread.currentThread().getId();
        int found = lookups(map, 10_000);
        long before = threads.getThreadAllocatedBytes(threadId);
        found += lookups(map, 1_000_000);
        long allocated = threads.getThreadAllocatedBytes(threadId) - before;

        assertThat(found).isEqualTo(2 * 1_010_000);
        // Three million lookups; any per-lookup allocation would be at least megabytes. The bound leaves room for the
        // measurement calls themselves.
        assertThat(allocated).isLessThan(64 * 1024);
    }

    @Test
    void create_duplicateTypedTrait_keepsLastOne() {
        JsonNameTrait first = new JsonNameTrait("first");
        JsonNameTrait second = new JsonNameTrait("second");
        TraitMap map = TraitMap.create(first, second);

        assertThat(map.get(TraitKey.JSON_NAME_TRAIT)).isSameAs(second);
        assertThat(map.traits()).containsExactly(second);
    }

    @Test
    void getGeneric_severalGenericTraits_eachRetrievableByTraitId() {
        GenericTrait tagging = new GenericTrait(TAGGING, Document.of(Collections.singletonMap("team",
                                                                                              Document.of("sprockets"))));
        GenericTrait owner = new GenericTrait(OWNER, Document.of("platform"));
        TraitMap map = TraitMap.create(tagging, RequiredTrait.INSTANCE, owner);

        assertThat(map.getGeneric(TAGGING)).isSameAs(tagging);
        assertThat(map.getGeneric(OWNER)).isSameAs(owner);
        assertThat(map.getGeneric(ShapeId.from("com.example#other"))).isNull();
        assertThat(map.getGeneric(null)).isNull();
        assertThat(map.get(TraitKey.REQUIRED_TRAIT)).isSameAs(RequiredTrait.INSTANCE);
    }

    @Test
    void getGeneric_typedTraitId_returnsNull() {
        TraitMap map = TraitMap.create(RequiredTrait.INSTANCE);

        assertThat(map.getGeneric(RequiredTrait.ID)).isNull();
    }

    @Test
    void create_duplicateGenericTraitId_keepsLastOne() {
        GenericTrait first = new GenericTrait(OWNER, Document.of("first"));
        GenericTrait second = new GenericTrait(OWNER, Document.of("second"));
        TraitMap map = TraitMap.create(first, second);

        assertThat(map.getGeneric(OWNER)).isSameAs(second);
        assertThat(map.traits()).containsExactly(second);
    }

    @Test
    void create_nullTrait_throwsNullPointerException() {
        assertThatThrownBy(() -> TraitMap.create(RequiredTrait.INSTANCE, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("traits[1]");
        assertThatThrownBy(() -> TraitMap.create((Trait[]) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("traits");
    }

    @Test
    void traits_containsEveryTraitOnceIncludingGenericTraits() {
        GenericTrait owner = new GenericTrait(OWNER, Document.of("platform"));
        JsonNameTrait jsonName = new JsonNameTrait("name");
        TraitMap map = TraitMap.create(owner, jsonName, RequiredTrait.INSTANCE, RequiredTrait.INSTANCE);

        // Typed traits in key index order, then generic traits.
        assertThat(map.traits()).containsExactly(RequiredTrait.INSTANCE, jsonName, owner);
        assertThat(map.traits()).isSameAs(map.traits());
    }

    @Test
    void traits_viewRejectsModificationAndMapIsUnchanged() {
        GenericTrait owner = new GenericTrait(OWNER, Document.of("platform"));
        TraitMap map = TraitMap.create(RequiredTrait.INSTANCE, owner);
        Collection<Trait> traits = map.traits();

        assertThatThrownBy(() -> traits.add(SensitiveTrait.INSTANCE)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> traits.remove(RequiredTrait.INSTANCE))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(traits::clear).isInstanceOf(UnsupportedOperationException.class);
        Iterator<Trait> iterator = traits.iterator();
        iterator.next();
        assertThatThrownBy(iterator::remove).isInstanceOf(UnsupportedOperationException.class);

        assertThat(map.traits()).containsExactly(RequiredTrait.INSTANCE, owner);
        assertThat(map.get(TraitKey.REQUIRED_TRAIT)).isSameAs(RequiredTrait.INSTANCE);
        assertThat(map.get(TraitKey.SENSITIVE_TRAIT)).isNull();
        assertThat(map.getGeneric(OWNER)).isSameAs(owner);
    }

    @Test
    void withMemberTraits_memberTraitsOverrideTargetTraits() {
        XmlNameTrait targetXmlName = new XmlNameTrait("Target");
        XmlNameTrait memberXmlName = new XmlNameTrait("Member");
        GenericTrait targetOwner = new GenericTrait(OWNER, Document.of("target"));
        GenericTrait memberOwner = new GenericTrait(OWNER, Document.of("member"));
        GenericTrait memberTagging = new GenericTrait(TAGGING, Document.of("member"));
        Rpcv2CborTrait cbor = new Rpcv2CborTrait();
        TraitMap target = TraitMap.create(targetXmlName, SensitiveTrait.INSTANCE, targetOwner);
        TraitMap member = TraitMap.create(RequiredTrait.INSTANCE, memberXmlName, cbor, memberOwner, memberTagging);

        TraitMap merged = target.withMemberTraits(member);

        assertThat(merged.get(TraitKey.XML_NAME_TRAIT)).isSameAs(memberXmlName);
        assertThat(merged.get(TraitKey.SENSITIVE_TRAIT)).isSameAs(SensitiveTrait.INSTANCE);
        assertThat(merged.get(TraitKey.REQUIRED_TRAIT)).isSameAs(RequiredTrait.INSTANCE);
        assertThat(merged.get(TraitKey.RPCV2_CBOR_TRAIT)).isSameAs(cbor);
        assertThat(merged.getGeneric(OWNER)).isSameAs(memberOwner);
        assertThat(merged.getGeneric(TAGGING)).isSameAs(memberTagging);
        assertThat(merged.traits()).containsExactlyInAnyOrder(RequiredTrait.INSTANCE, SensitiveTrait.INSTANCE,
                                                              memberXmlName, cbor, memberOwner, memberTagging);

        // Neither input changes.
        assertThat(target.get(TraitKey.XML_NAME_TRAIT)).isSameAs(targetXmlName);
        assertThat(target.get(TraitKey.REQUIRED_TRAIT)).isNull();
        assertThat(target.getGeneric(OWNER)).isSameAs(targetOwner);
        assertThat(target.getGeneric(TAGGING)).isNull();
        assertThat(member.get(TraitKey.SENSITIVE_TRAIT)).isNull();
    }

    @Test
    void withMemberTraits_emptySide_returnsTheOtherMap() {
        TraitMap target = TraitMap.create(SensitiveTrait.INSTANCE);
        TraitMap member = TraitMap.create(RequiredTrait.INSTANCE);

        assertThat(target.withMemberTraits(TraitMap.EMPTY)).isSameAs(target);
        assertThat(target.withMemberTraits(target)).isSameAs(target);
        assertThat(TraitMap.EMPTY.withMemberTraits(member)).isSameAs(member);
    }

    private static int lookups(TraitMap map, int iterations) {
        int found = 0;
        for (int i = 0; i < iterations; i++) {
            if (map.get(TraitKey.REQUIRED_TRAIT) != null) {
                found++;
            }
            if (map.get(TraitKey.JSON_NAME_TRAIT) != null) {
                found++;
            }
            if (map.get(TraitKey.RPCV2_CBOR_TRAIT) != null) {
                found++;
            }
        }
        return found;
    }
}
