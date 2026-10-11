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

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * The {@code aws.auth#sigv4} auth definition trait: the service supports AWS Signature Version 4, signing with the
 * given name.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.aws.traits.auth.SigV4Trait}.
 */
@SdkPublicApi
public final class SigV4Trait implements Trait {

    /**
     * The trait's shape ID, {@code aws.auth#sigv4}.
     */
    public static final ShapeId ID = ShapeId.fromParts("aws.auth", "sigv4");

    private static final String NAME = "name";

    private final String name;

    /**
     * Creates the trait.
     *
     * @param name the signing name, for example {@code sqs}
     * @throws NullPointerException if {@code name} is null
     */
    public SigV4Trait(String name) {
        this.name = Validate.paramNotNull(name, "name");
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns the signing name.
     *
     * @return the signing name
     */
    public String getName() {
        return name;
    }

    @Override
    public Document toDocument() {
        return Document.of(Collections.singletonMap(NAME, Document.of(name)));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SigV4Trait && name.equals(((SigV4Trait) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("SigV4Trait").add("name", name).build();
    }

    static SigV4Trait fromDocument(Document value) {
        Map<String, Document> object = TraitValues.expectObject(ID, value);
        return new SigV4Trait(TraitValues.requiredString(ID, object, NAME));
    }
}
