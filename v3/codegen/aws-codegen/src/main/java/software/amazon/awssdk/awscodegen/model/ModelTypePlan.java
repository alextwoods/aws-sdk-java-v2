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

import java.util.List;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Render plan of one generated structure, union or modeled-error type, computed by {@link ModelTypePlanner}.
 *
 * @param type symbol of the generated class
 * @param shape absolute ID of the modeled shape
 * @param kind {@link #STRUCTURE}, {@link #UNION} or {@link #ERROR}
 * @param schemaHolder the service's schema holder
 * @param schemaConstant name of the type's exported schema constant in {@code schemaHolder}
 * @param serviceException the service's base exception, which an error extends; the type itself otherwise
 * @param retryable whether an error is modeled {@code @retryable}
 * @param throttling whether an error is modeled {@code @retryable(throttling: true)}
 * @param members every member in modeled order
 */
record ModelTypePlan(Symbol type, ShapeId shape, String kind, Symbol schemaHolder, String schemaConstant,
                     Symbol serviceException, boolean retryable, boolean throttling, List<Member> members)
    implements ModelRenderPlan {

    static final String STRUCTURE = "structure";
    static final String UNION = "union";
    static final String ERROR = "error";

    ModelTypePlan {
        members = List.copyOf(members);
    }

    boolean isError() {
        return kind.equals(ERROR);
    }

    boolean isUnion() {
        return kind.equals(UNION);
    }

    /**
     * A Java source fragment written through {@code AwsJavaWriter}: one {@code $T} placeholder per element of
     * {@code types} and {@code $$} for a literal {@code $}. An empty template means "none".
     */
    record Code(String template, List<Symbol> types) {
        static final Code NONE = new Code("", List.of());

        Code {
            types = List.copyOf(types);
        }

        boolean isEmpty() {
            return template.isEmpty();
        }
    }

    /**
     * A generated method.
     *
     * @param signature name and parameters, for example {@code flags(Collection<Boolean> flags)}, preceded by the
     *        return type for accessors
     * @param body the statements of the implementation
     * @param safeVarargs whether the implementation takes generic varargs and is annotated {@code @SafeVarargs}
     */
    record Method(Code signature, Code body, boolean safeVarargs) {
    }

    /**
     * One member.
     *
     * @param name modeled member name
     * @param id absolute member ID
     * @param javaName Java field, accessor and setter base name
     * @param schemaField name of the private member-schema constant
     * @param index the member's index in the type's schema (Smithy Java orders required members without a default
     *        first in structures)
     * @param state {@link #VALUE}, {@link #MESSAGE} (an error's message, held by the base exception) or
     *        {@link #EXCLUDED} (a streaming blob member, which is not part of the type's state) or
     *        {@link #EVENT_STREAM} (an event stream member, held only for {@code getMemberValue} and
     *        {@code setMemberValue}: no V2 accessor or setter, and not part of equality, string form or
     *        serialization)
     * @param fieldType Java type of the immutable field
     * @param builderFieldType Java type of the builder field (collections accept any collection)
     * @param required whether {@code build()} requires the member
     * @param defaultValue initializer of the builder field, from the modeled {@code @default}; empty if none
     * @param snapshot expression of the field's value from the builder {@code builder}
     * @param serialize statements writing the member from {@code this} to {@code serializer}
     * @param read statement reading the member with deserializer {@code de} and member schema {@code member} into
     *        builder {@code builder}
     * @param zeroValue error-correction value of a missing required member; empty if the member is not required, or
     *        if its target has no zero value (a document), in which case error correction leaves it null and records
     *        it as present (see {@link #correctsToNull()})
     * @param stringValue expression rendered by {@code toString} (redacted if the member is sensitive)
     * @param unionConstant name of the member's {@code Type} constant, for union members
     * @param accessors accessor methods of the type
     * @param setters setter methods of the builder
     * @param factories static factories of a union
     * @param helpers private static helper methods of the type (collection readers)
     */
    record Member(String name, ShapeId id, String javaName, String schemaField, int index, String state,
                  Code fieldType, Code builderFieldType, boolean required, Code defaultValue, Code snapshot,
                  Code serialize, Code read, Code zeroValue, Code stringValue, String unionConstant,
                  List<Method> accessors, List<Method> setters, List<Method> factories, List<Code> helpers) {

        static final String VALUE = "value";
        static final String MESSAGE = "message";
        static final String EXCLUDED = "excluded";
        static final String EVENT_STREAM = "eventStream";

        Member {
            accessors = List.copyOf(accessors);
            setters = List.copyOf(setters);
            factories = List.copyOf(factories);
            helpers = List.copyOf(helpers);
        }

        /** Whether the member is an event stream, held by the type outside its V2 state. */
        boolean eventStream() {
            return state.equals(EVENT_STREAM);
        }

        /** Whether the member is a field of the generated type and its builder. */
        boolean hasField() {
            return state.equals(VALUE);
        }

        /**
         * Whether Smithy client error correction leaves the missing required member {@code null} and treats it as
         * present, as Smithy Java's generated builders do: a required member whose target has no zero value (a
         * document). The builder records that in its {@link #errorCorrectedFlag()}, which the required-member check
         * honours, so a builder without error correction still reports the member as missing.
         */
        boolean correctsToNull() {
            return required && zeroValue.isEmpty();
        }

        /**
         * Name of the builder's {@code boolean} field recording that error correction left a
         * {@link #correctsToNull()} member {@code null}; {@code $} keeps it distinct from every modeled member name.
         */
        String errorCorrectedFlag() {
            return javaName + "$errorCorrected";
        }
    }
}
