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

package software.amazon.awssdk.smithy.runtime.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

class OperationSchemasTest {

    private static final Schema OPERATION = Schema.createOperation(ShapeId.from("example#Upload"));
    private static final Schema BODY = Schema.createBlob(ShapeId.from("example#Body"), new StreamingTrait());
    private static final Schema INPUT = Schema.structureBuilder(ShapeId.from("example#UploadInput"))
                                              .putMember("body", BODY)
                                              .putMember("name", PreludeSchemas.STRING)
                                              .build();
    private static final Schema THROTTLED = Schema.structureBuilder(ShapeId.from("example#Throttled"),
                                                                    new ErrorTrait("server")).build();
    private static final Schema MISSING = Schema.structureBuilder(ShapeId.from("example#Missing"),
                                                                  new ErrorTrait("client")).build();

    @Test
    void linksInputOutputErrorsAndStreamMembers() {
        OperationSchemas links = OperationSchemas.builder(OPERATION)
                                                 .input(INPUT)
                                                 .output(Unit.SCHEMA)
                                                 .errors(MISSING, THROTTLED)
                                                 .inputStreamMember("body")
                                                 .build();

        assertThat(links.id()).isEqualTo(OPERATION.id());
        assertThat(links.operation()).isSameAs(OPERATION);
        assertThat(links.input()).isSameAs(INPUT);
        assertThat(links.output()).isSameAs(Unit.SCHEMA);
        assertThat(links.errors()).containsExactly(MISSING, THROTTLED);
        assertThat(links.inputStreamMember()).containsSame(INPUT.member("body"));
        assertThat(links.outputStreamMember()).isEmpty();
    }

    @Test
    void rejectsInvalidLinks() {
        assertThatThrownBy(() -> OperationSchemas.builder(INPUT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OperationSchemas.builder(OPERATION).output(Unit.SCHEMA).build())
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OperationSchemas.builder(OPERATION).input(BODY).output(Unit.SCHEMA).build())
            .hasMessageContaining("structure");
        assertThatThrownBy(() -> OperationSchemas.builder(OPERATION).errors(INPUT))
            .hasMessageContaining("smithy.api#error");
        assertThatThrownBy(() -> OperationSchemas.builder(OPERATION).errors(THROTTLED, THROTTLED))
            .hasMessageContaining("more than once");
        assertThatThrownBy(() -> OperationSchemas.builder(OPERATION).input(INPUT).output(Unit.SCHEMA)
                                                 .outputStreamMember("body").build())
            .hasMessageContaining("not a member");
    }
}
