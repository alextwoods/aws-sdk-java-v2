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

package software.amazon.awssdk.schema.serde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.RegistryTestShapes.InternalError;
import software.amazon.awssdk.schema.serde.RegistryTestShapes.Person;
import software.amazon.awssdk.schema.serde.RegistryTestShapes.QueueDoesNotExist;
import software.amazon.awssdk.schema.serde.document.DiscriminatorException;
import software.amazon.awssdk.schema.serde.document.Document;

class ErrorRegistryTest {

    private static ErrorRegistry sqsLikeRegistry() {
        // The shape the generated <Svc>Operations.ERROR_REGISTRY has (design A.18).
        return ErrorRegistry.builder()
                            .putLazyType(QueueDoesNotExist.ID, () -> QueueDoesNotExist.SCHEMA)
                            .errorCode(QueueDoesNotExist.CODE, QueueDoesNotExist.ID)
                            .putLazyType(InternalError.ID, () -> InternalError.SCHEMA)
                            .build();
    }

    @Test
    void resolveCode_awsQueryErrorCode_returnsShapeId() {
        ErrorRegistry registry = sqsLikeRegistry();

        assertThat(registry.resolveCode("AWS.SimpleQueueService.NonExistentQueue")).isEqualTo(QueueDoesNotExist.ID);
        // The code index matches the error's trait.
        assertThat(QueueDoesNotExist.SCHEMA.expectTrait(TraitKey.AWS_QUERY_ERROR_TRAIT).getCode())
            .isEqualTo(QueueDoesNotExist.CODE);
    }

    @Test
    void resolveCode_ignoresTextFromFirstColon() {
        ErrorRegistry registry = sqsLikeRegistry();

        assertThat(registry.resolveCode(QueueDoesNotExist.CODE + ":http://internal.amazon.com/coral/"))
            .isEqualTo(QueueDoesNotExist.ID);
        assertThat(registry.resolveCode(QueueDoesNotExist.CODE + ":")).isEqualTo(QueueDoesNotExist.ID);
    }

    @Test
    void resolveCode_unknownCodes_returnNull() {
        ErrorRegistry registry = sqsLikeRegistry();

        assertThat(registry.resolveCode(null)).isNull();
        assertThat(registry.resolveCode("")).isNull();
        assertThat(registry.resolveCode("NoSuchCode")).isNull();
        // Shape names and shape IDs are not custom codes; ErrorTypeUtils resolves those.
        assertThat(registry.resolveCode("QueueDoesNotExist")).isNull();
        assertThat(registry.resolveCode(QueueDoesNotExist.ID.toString())).isNull();
        assertThat(registry.resolveCode("InternalError")).isNull();
        // Codes are case-sensitive.
        assertThat(registry.resolveCode(QueueDoesNotExist.CODE.toLowerCase())).isNull();
    }

    @Test
    void duplicateCode_firstAddedWins() {
        ErrorRegistry registry = ErrorRegistry.builder()
                                              .putLazyType(QueueDoesNotExist.ID, () -> QueueDoesNotExist.SCHEMA)
                                              .putLazyType(InternalError.ID, () -> InternalError.SCHEMA)
                                              .errorCode("ResourceNotFound", InternalError.ID)
                                              .errorCode("ResourceNotFound", QueueDoesNotExist.ID)
                                              .build();

        assertThat(registry.resolveCode("ResourceNotFound")).isEqualTo(InternalError.ID);
    }

    @Test
    void build_codeForUnregisteredShape_fails() {
        ShapeId missing = ShapeId.fromParts(RegistryTestShapes.NS, "Missing");
        ErrorRegistry.Builder builder = ErrorRegistry.builder()
                                                     .putLazyType(QueueDoesNotExist.ID, () -> QueueDoesNotExist.SCHEMA)
                                                     .errorCode("Missing.Code", missing);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Missing.Code")
            .hasMessageContaining(missing.toString());
    }

    @Test
    void typeLookups_areLazyAndDelegateToEntries() {
        AtomicInteger calls = new AtomicInteger();
        ErrorRegistry registry = ErrorRegistry.builder()
                                              .putLazyType(QueueDoesNotExist.ID, () -> {
                                                  calls.incrementAndGet();
                                                  return QueueDoesNotExist.SCHEMA;
                                              })
                                              .errorCode(QueueDoesNotExist.CODE, QueueDoesNotExist.ID)
                                              .build();

        assertThat(registry.contains(QueueDoesNotExist.ID)).isTrue();
        assertThat(registry.resolveCode(QueueDoesNotExist.CODE)).isEqualTo(QueueDoesNotExist.ID);
        assertThat(calls).hasValue(0);

        assertThat(registry.getShapeClass(QueueDoesNotExist.ID)).isEqualTo(QueueDoesNotExist.class);
        assertThat(registry.createBuilder(QueueDoesNotExist.ID).build()).isInstanceOf(QueueDoesNotExist.class);
        assertThat(registry.getSchema(QueueDoesNotExist.ID)).isSameAs(QueueDoesNotExist.SCHEMA);
        assertThat(calls).hasValue(1);

        assertThat(registry.getSchema(Person.ID)).isNull();
        assertThat(registry.contains(Person.ID)).isFalse();
        assertThat(registry.getShapeClass(Person.ID)).isNull();
        assertThat(registry.createBuilder(Person.ID)).isNull();
    }

    @Test
    void putType_registersClassAndBuilderWithoutSchema() {
        AtomicInteger builders = new AtomicInteger();
        ErrorRegistry registry = ErrorRegistry.builder()
                                              .putType(QueueDoesNotExist.ID, QueueDoesNotExist.class, () -> {
                                                  builders.incrementAndGet();
                                                  return QueueDoesNotExist.builder();
                                              })
                                              .errorCode(QueueDoesNotExist.CODE, QueueDoesNotExist.ID)
                                              .build();

        assertThat(registry.contains(QueueDoesNotExist.ID)).isTrue();
        assertThat(registry.resolveCode(QueueDoesNotExist.CODE)).isEqualTo(QueueDoesNotExist.ID);
        assertThat(registry.getShapeClass(QueueDoesNotExist.ID)).isEqualTo(QueueDoesNotExist.class);
        assertThat(builders).hasValue(0);
        assertThat(registry.createBuilder(QueueDoesNotExist.ID)).isNotSameAs(registry.createBuilder(QueueDoesNotExist.ID));
        assertThat(builders).hasValue(2);
        assertThat(registry.getSchema(QueueDoesNotExist.ID)).isNull();

        ErrorRegistry.Builder builder = ErrorRegistry.builder();
        assertThatThrownBy(() -> builder.putType(null, QueueDoesNotExist.class, QueueDoesNotExist::builder))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.putType(QueueDoesNotExist.ID, null, QueueDoesNotExist::builder))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.putType(QueueDoesNotExist.ID, QueueDoesNotExist.class, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void compose_withEmptyRegistries() {
        ErrorRegistry registry = sqsLikeRegistry();
        ErrorRegistry empty = ErrorRegistry.builder().build();
        TypeRegistry other = TypeRegistry.builder().putType(Person.ID, Person.class, Person::builder).build();

        assertThat(TypeRegistry.compose(registry, TypeRegistry.empty())).isSameAs(registry);
        assertThat(TypeRegistry.compose(TypeRegistry.empty(), registry)).isSameAs(registry);
        assertThat(TypeRegistry.compose(empty, other)).isSameAs(other);

        TypeRegistry composed = TypeRegistry.compose(registry, other);
        assertThat(composed.getShapeClass(QueueDoesNotExist.ID)).isEqualTo(QueueDoesNotExist.class);
        assertThat(composed.getShapeClass(Person.ID)).isEqualTo(Person.class);
    }

    @Test
    void invalidArguments_fail() {
        ErrorRegistry.Builder builder = ErrorRegistry.builder();

        assertThatThrownBy(() -> builder.putLazyType(null, () -> QueueDoesNotExist.SCHEMA))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.putLazyType(QueueDoesNotExist.ID, null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.errorCode(null, QueueDoesNotExist.ID))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.errorCode(" ", QueueDoesNotExist.ID))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.errorCode(QueueDoesNotExist.CODE, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deserialize_errorDocument_buildsTheRegisteredErrorAndRejectsOthers() {
        ErrorRegistry registry = sqsLikeRegistry();

        Object error = registry.deserialize(Document.of(QueueDoesNotExist.builder().name("q").build()));

        assertThat(error).isInstanceOf(QueueDoesNotExist.class);
        assertThat(((QueueDoesNotExist) error).name()).isEqualTo("q");
        assertThatThrownBy(() -> registry.deserialize(Document.of(Person.builder().name("p").build())))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining(Person.ID.toString());
        assertThatThrownBy(() -> registry.deserialize(Document.of("q")))
            .isInstanceOf(DiscriminatorException.class);
    }
}
