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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;

/**
 * The {@code smithy.api#paginated} trait: the members that paginate an operation or that a service's paginated
 * operations share.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.model.traits.PaginatedTrait}. The member paths are kept as
 * strings; resolving them against the input and output shapes is left to the caller.
 */
@SdkPublicApi
public final class PaginatedTrait implements Trait {

    /**
     * The trait's shape ID, {@code smithy.api#paginated}.
     */
    public static final ShapeId ID = ShapeId.fromParts("smithy.api", "paginated");

    private static final String INPUT_TOKEN = "inputToken";
    private static final String OUTPUT_TOKEN = "outputToken";
    private static final String ITEMS = "items";
    private static final String PAGE_SIZE = "pageSize";

    private final String inputToken;
    private final String outputToken;
    private final String items;
    private final String pageSize;

    /**
     * Creates the trait. Each argument may be null when the model does not set that property.
     *
     * @param inputToken the input member that carries the continuation token
     * @param outputToken the output member path that carries the next continuation token
     * @param items the output member path of the paginated items
     * @param pageSize the input member that limits the page size
     */
    public PaginatedTrait(String inputToken, String outputToken, String items, String pageSize) {
        this.inputToken = inputToken;
        this.outputToken = outputToken;
        this.items = items;
        this.pageSize = pageSize;
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns the input member that carries the continuation token.
     *
     * @return the member name, if set
     */
    public Optional<String> getInputToken() {
        return Optional.ofNullable(inputToken);
    }

    /**
     * Returns the output member path that carries the next continuation token.
     *
     * @return the member path, if set
     */
    public Optional<String> getOutputToken() {
        return Optional.ofNullable(outputToken);
    }

    /**
     * Returns the output member path of the paginated items.
     *
     * @return the member path, if set
     */
    public Optional<String> getItems() {
        return Optional.ofNullable(items);
    }

    /**
     * Returns the input member that limits the page size.
     *
     * @return the member name, if set
     */
    public Optional<String> getPageSize() {
        return Optional.ofNullable(pageSize);
    }

    @Override
    public Document toDocument() {
        Map<String, Document> members = new LinkedHashMap<>();
        putIfSet(members, INPUT_TOKEN, inputToken);
        putIfSet(members, OUTPUT_TOKEN, outputToken);
        putIfSet(members, ITEMS, items);
        putIfSet(members, PAGE_SIZE, pageSize);
        return Document.of(members);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaginatedTrait)) {
            return false;
        }
        PaginatedTrait other = (PaginatedTrait) o;
        return Objects.equals(inputToken, other.inputToken)
               && Objects.equals(outputToken, other.outputToken)
               && Objects.equals(items, other.items)
               && Objects.equals(pageSize, other.pageSize);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(inputToken);
        result = 31 * result + Objects.hashCode(outputToken);
        result = 31 * result + Objects.hashCode(items);
        return 31 * result + Objects.hashCode(pageSize);
    }

    @Override
    public String toString() {
        return ToString.builder("PaginatedTrait")
                       .add("inputToken", inputToken)
                       .add("outputToken", outputToken)
                       .add("items", items)
                       .add("pageSize", pageSize)
                       .build();
    }

    static PaginatedTrait fromDocument(Document value) {
        Map<String, Document> object = TraitValues.expectObject(ID, value);
        return new PaginatedTrait(TraitValues.optionalString(ID, object, INPUT_TOKEN),
                                  TraitValues.optionalString(ID, object, OUTPUT_TOKEN),
                                  TraitValues.optionalString(ID, object, ITEMS),
                                  TraitValues.optionalString(ID, object, PAGE_SIZE));
    }

    private static void putIfSet(Map<String, Document> members, String name, String value) {
        if (value != null) {
            members.put(name, Document.of(value));
        }
    }
}
