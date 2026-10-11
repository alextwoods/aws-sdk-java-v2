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

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;

class GenericTraitTest {
    private static final ShapeId ID = ShapeId.from("com.example#generic");

    @Test
    void value_returnsConstructorDocument() {
        Document value = Document.of(Collections.singletonMap("a", Document.of("b")));
        GenericTrait trait = new GenericTrait(ID, value);

        assertThat(trait.traitId()).isEqualTo(ID);
        assertThat(trait.value()).isSameAs(value);
        assertThat(trait.toDocument()).isSameAs(value);
    }

    @Test
    void equals_comparesIdAndDocumentValue() {
        GenericTrait trait = new GenericTrait(ID, Document.of(1));

        assertThat(trait).isEqualTo(new GenericTrait(ID, Document.of(1)));
        assertThat(trait.hashCode()).isEqualTo(new GenericTrait(ID, Document.of(1)).hashCode());
        assertThat(trait).isNotEqualTo(new GenericTrait(ID, Document.of(2)));
        assertThat(trait).isNotEqualTo(new GenericTrait(ShapeId.from("com.example#other"), Document.of(1)));
        assertThat(new GenericTrait(ID, null)).isEqualTo(new GenericTrait(ID, null));
        assertThat(new GenericTrait(ID, null)).isNotEqualTo(trait);
    }

    @Test
    void constructor_withNullId_throwsNullPointerException() {
        assertThatThrownBy(() -> new GenericTrait(null, Document.of(1))).isInstanceOf(NullPointerException.class);
    }
}
