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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeIdSyntaxException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * The {@code smithy.api#auth} trait: the priority-ordered auth schemes of a service or operation.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.AuthTrait}. Like smithy-model, the trait keeps
 * each scheme once, in the order first given.
 */
@SdkPublicApi
public final class AuthTrait implements Trait {

    /**
     * The trait's shape ID, {@code smithy.api#auth}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "auth");

    private final Set<ShapeId> values;
    private final List<ShapeId> valueList;

    /**
     * Creates the trait.
     *
     * @param values the auth scheme shape IDs in priority order; an empty list means the operation uses no auth
     * @throws NullPointerException if {@code values} or one of its elements is null
     */
    public AuthTrait(List<ShapeId> values) {
        Validate.paramNotNull(values, "values");
        Set<ShapeId> copy = new LinkedHashSet<>(values.size() * 4 / 3 + 1);
        for (ShapeId value : values) {
            copy.add(Validate.paramNotNull(value, "values element"));
        }
        this.values = Collections.unmodifiableSet(copy);
        this.valueList = Collections.unmodifiableList(new ArrayList<>(copy));
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns the auth scheme shape IDs in priority order.
     *
     * @return an unmodifiable set of auth scheme IDs
     */
    public Set<ShapeId> getValueSet() {
        return values;
    }

    /**
     * Returns the auth scheme shape IDs in priority order, as a list.
     *
     * @return an unmodifiable list of auth scheme IDs
     */
    public List<ShapeId> getValues() {
        return valueList;
    }

    @Override
    public Document toDocument() {
        List<Document> elements = new ArrayList<>(values.size());
        for (ShapeId value : valueList) {
            elements.add(Document.of(value.toString()));
        }
        return Document.of(elements);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AuthTrait && valueList.equals(((AuthTrait) o).valueList);
    }

    @Override
    public int hashCode() {
        return valueList.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("AuthTrait").add("values", values).build();
    }

    static AuthTrait fromDocument(Document value) {
        List<String> ids = TraitValues.expectStringList(ID, value);
        List<ShapeId> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            try {
                result.add(ShapeId.from(id));
            } catch (ShapeIdSyntaxException e) {
                throw TraitValues.invalid(ID, e.getMessage());
            }
        }
        return new AuthTrait(result);
    }
}
