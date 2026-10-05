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

package software.amazon.awssdk.awscore.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;

class AwsOperationDescriptorTest {

    private static final ShapeId OPERATION = ShapeId.from("example#GetThing");
    private static final ShapeId SERVICE = ShapeId.from("example#Service");
    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");
    private static final ShapeId NOT_FOUND = ShapeId.from("example#NotFound");
    private static final ShapeId THROTTLED = ShapeId.from("example#Throttled");

    @Test
    void recordsExactShapeIdsAndErrorOrder() {
        AwsOperationDescriptor descriptor = complete().errors(THROTTLED).errors(NOT_FOUND).build();

        assertEquals(OPERATION, descriptor.id());
        assertEquals(SERVICE, descriptor.serviceId());
        assertEquals(UNIT, descriptor.inputId());
        assertEquals(ShapeId.from("example#GetThingOutput"), descriptor.outputId());
        assertEquals(List.of(THROTTLED, NOT_FOUND), descriptor.errorIds());
        assertEquals("AwsOperationDescriptor[example#GetThing]", descriptor.toString());
    }

    @Test
    void errorsAreImmutableAndOptional() {
        AwsOperationDescriptor descriptor = complete().build();
        assertEquals(List.of(), descriptor.errorIds());
        assertThrows(UnsupportedOperationException.class, () -> descriptor.errorIds().add(NOT_FOUND));
    }

    @Test
    void requiresEveryIdentity() {
        assertThrows(NullPointerException.class, () -> complete().id(null).build());
        assertThrows(NullPointerException.class, () -> complete().service(null).build());
        assertThrows(NullPointerException.class, () -> complete().input(null).build());
        assertThrows(NullPointerException.class, () -> complete().output(null).build());
    }

    @Test
    void rejectsMemberIdsAndDuplicateErrors() {
        assertThrows(IllegalArgumentException.class,
                     () -> complete().input(ShapeId.from("example#Input$member")).build());
        assertThrows(IllegalArgumentException.class, () -> complete().errors(NOT_FOUND, NOT_FOUND));
        assertThrows(IllegalArgumentException.class, () -> complete().errors(ShapeId.from("example#E$m")));
    }

    @Test
    void linksMatchingOperationSchemas() {
        assertEquals(Optional.empty(), complete().build().schemas());
        Schema output = Schema.structureBuilder(ShapeId.from("example#GetThingOutput")).build();
        Schema notFound = Schema.structureBuilder(NOT_FOUND, new ErrorTrait("client")).build();
        Schema throttled = Schema.structureBuilder(THROTTLED, new ErrorTrait("server")).build();
        OperationSchemas schemas = OperationSchemas.builder(Schema.createOperation(OPERATION))
                                                   .input(Unit.SCHEMA)
                                                   .output(output)
                                                   .errors(notFound, throttled)
                                                   .build();

        AwsOperationDescriptor descriptor = complete().errors(NOT_FOUND, THROTTLED).schemas(schemas).build();
        assertSame(schemas, descriptor.schemas().orElseThrow());

        assertThrows(IllegalArgumentException.class, () -> complete().errors(THROTTLED, NOT_FOUND).schemas(schemas)
                                                                     .build());
        assertThrows(IllegalArgumentException.class, () -> complete().schemas(schemas).build());
        assertThrows(IllegalArgumentException.class,
                     () -> complete().errors(NOT_FOUND, THROTTLED).id(ShapeId.from("example#Other")).schemas(schemas)
                                     .build());
        assertThrows(IllegalArgumentException.class,
                     () -> complete().errors(NOT_FOUND, THROTTLED).input(ShapeId.from("example#GetThingInput"))
                                     .schemas(schemas).build());
    }

    private static AwsOperationDescriptor.Builder complete() {
        return AwsOperationDescriptor.builder()
                                     .id(OPERATION)
                                     .service(SERVICE)
                                     .input(UNIT)
                                     .output(ShapeId.from("example#GetThingOutput"));
    }
}
