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

package software.amazon.awssdk.awscodegen.model;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.awscodegen.schema.SchemaSymbols;
import software.amazon.smithy.codegen.core.Symbol;

/** Builds a {@link ModelTypePlan.Code} fragment from literal text, type references and string literals. */
final class Fragment {

    private final StringBuilder template = new StringBuilder();
    private final List<Symbol> types = new ArrayList<>();

    static Fragment of(String text) {
        return new Fragment().text(text);
    }

    static Fragment of(Symbol type) {
        return new Fragment().type(type);
    }

    /** Appends literal Java text; {@code $} is escaped for the writer. */
    Fragment text(String text) {
        template.append(text.replace("$", "$$"));
        return this;
    }

    /** Appends a reference to {@code type}, imported by the writer. */
    Fragment type(Symbol type) {
        template.append("$T");
        types.add(type);
        return this;
    }

    /** Appends another fragment. */
    Fragment code(ModelTypePlan.Code code) {
        template.append(code.template());
        types.addAll(code.types());
        return this;
    }

    /** Appends another fragment. */
    Fragment code(Fragment fragment) {
        return code(fragment.build());
    }

    /** Appends a Java string literal of {@code value}. */
    Fragment string(String value) {
        return text(SchemaSymbols.javaString(value));
    }

    boolean isEmpty() {
        return template.isEmpty();
    }

    ModelTypePlan.Code build() {
        return new ModelTypePlan.Code(template.toString(), types);
    }
}
