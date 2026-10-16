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


package software.amazon.awssdk.codecs.json;

import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.JsonNameTrait;

public final class JsonTestData {

    static final ShapeId BIRD_ID = ShapeId.from("smithy.example#Bird");
    static final Schema BIRD = Schema.structureBuilder(BIRD_ID)
            .putMember("name", PreludeSchemas.STRING)
            .putMember("color", PreludeSchemas.STRING, new JsonNameTrait("Color"))
            .putMember("nested", PreludeSchemas.STRING)
            .build();
    static final Schema BIRD_NAME = BIRD.member("name");
    static final Schema BIRD_COLOR = BIRD.member("color");
    static final Schema BIRD_NESTED = BIRD.member("nested");

    static final Schema ESCAPED_FIELD_NAMES = Schema.structureBuilder(ShapeId.from("smithy.example#Escaped"))
            .putMember("quoted", PreludeSchemas.STRING, new JsonNameTrait("has\"quote"))
            .putMember("slashed", PreludeSchemas.STRING, new JsonNameTrait("has\\slash"))
            .putMember("tabbed", PreludeSchemas.STRING, new JsonNameTrait("has\ttab"))
            .putMember("controlled", PreludeSchemas.STRING, new JsonNameTrait("has\u0001control"))
            .putMember("unicode", PreludeSchemas.STRING, new JsonNameTrait("héllo\u00e9"))
            .build();

    static final ShapeId NESTED_ID = ShapeId.from("smithy.example#Nested");
    static final Schema NESTED = Schema.structureBuilder(NESTED_ID)
            .putMember("number", PreludeSchemas.INTEGER)
            .build();
    static final Schema NESTED_NUMBER = NESTED.member("number");

    static final ShapeId UNION_ID = ShapeId.from("smithy.example#Union");
    static final Schema UNION = Schema.unionBuilder(UNION_ID)
            .putMember("booleanValue", PreludeSchemas.BOOLEAN)
            .putMember("intValue", PreludeSchemas.INTEGER)
            .build();
    static final Schema UNION_BOOLEAN_VALUE = UNION.member("booleanValue");
    static final Schema UNION_INTEGER_VALUE = UNION.member("intValue");
}
