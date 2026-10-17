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

package software.amazon.awssdk.httpbinding;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.httpbinding.TestModel.ALL_INPUT;
import static software.amazon.awssdk.httpbinding.TestModel.ALL_OUTPUT;
import static software.amazon.awssdk.httpbinding.TestModel.ALL_OUTPUT_OPERATION;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.SerializableStruct;

/**
 * Serializes with {@link ResponseSerializer}/{@link RequestSerializer} and reads the message back with
 * {@link ResponseDeserializer}/{@link RequestDeserializer}.
 */
class RoundTripTest {
    private static final Codec CODEC = RequestSerializerTest.CODEC;
    private static final Instant WHEN = Instant.parse("2014-04-29T18:30:38Z");

    @Test
    void responseRoundTrip() {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Foo", "f");
        SchemaStruct output = SchemaStruct.builder(ALL_OUTPUT)
                                          .set("token", "tok")
                                          .set("when", WHEN)
                                          .set("items", Arrays.asList("a", "b,c", "\"q\"", "back\\slash"))
                                          .set("numbers", Arrays.asList(1, 2))
                                          .set("dates", Arrays.asList(WHEN, WHEN.plusSeconds(1)))
                                          .set("isoDates", Arrays.asList(WHEN, Instant.parse("2020-01-01T00:00:00.5Z")))
                                          .set("flag", false)
                                          .set("num", -3)
                                          .set("epoch", Instant.ofEpochSecond(1, 500_000_000))
                                          .set("meta", meta)
                                          .set("status", 202)
                                          .set("name", "n")
                                          .set("size", 9L)
                                          .build();
        SdkHttpFullResponse response = HttpBinding.create().responseSerializer()
                                                  .operation(ALL_OUTPUT_OPERATION)
                                                  .payloadCodec(CODEC)
                                                  .payloadMediaType("application/json")
                                                  .shapeValue(output)
                                                  .serializeResponse();
        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.firstMatchingHeader("X-Dates"))
            .hasValue("Tue, 29 Apr 2014 18:30:38 GMT, Tue, 29 Apr 2014 18:30:39 GMT");
        assertThat(response.firstMatchingHeader("X-Iso-Dates"))
            .hasValue("2014-04-29T18:30:38Z, 2020-01-01T00:00:00.500Z");
        assertThat(response.firstMatchingHeader("X-Epoch")).hasValue("1.5");

        SchemaStruct read = ResponseDeserializerTest.deserialize(ALL_OUTPUT, response, "application/json");
        assertThat(read).isEqualTo(output);
    }

    @Test
    void responseStatusDefaultsToTheHttpTraitCode() {
        SdkHttpFullResponse response = HttpBinding.create().responseSerializer()
                                                  .operation(ALL_OUTPUT_OPERATION)
                                                  .payloadCodec(CODEC)
                                                  .payloadMediaType("application/json")
                                                  .shapeValue(SchemaStruct.builder(ALL_OUTPUT).build())
                                                  .serializeResponse();
        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    void errorResponsesUseTheErrorStatusAndTypeHeader() {
        SerializableStruct notFound = SchemaStruct.builder(TestModel.NOT_FOUND)
                                                  .set("message", "gone")
                                                  .set("resource", "r")
                                                  .build();
        SdkHttpFullResponse response = HttpBinding.create().responseSerializer()
                                                  .operation(ALL_OUTPUT_OPERATION)
                                                  .payloadCodec(CODEC)
                                                  .payloadMediaType("application/json")
                                                  .shapeValue(notFound)
                                                  .error(true)
                                                  .serializeResponse();
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.firstMatchingHeader("X-Amzn-Errortype")).hasValue("smithy.example#NotFound");
        assertThat(response.firstMatchingHeader("X-Resource")).hasValue("r");

        SdkHttpFullResponse server = HttpBinding.create().responseSerializer()
                                                .operation(ALL_OUTPUT_OPERATION)
                                                .payloadCodec(CODEC)
                                                .payloadMediaType("application/json")
                                                .shapeValue(SchemaStruct.builder(TestModel.SERVER_ERROR).build())
                                                .headerErrorSerializer(HeaderErrorSerializer.NONE)
                                                .error(true)
                                                .serializeResponse();
        assertThat(server.statusCode()).isEqualTo(500);
        assertThat(server.headers()).doesNotContainKey("X-Amzn-Errortype");
    }

    @Test
    void requestRoundTrip() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("extra", "e");
        SchemaStruct input = SchemaStruct.builder(ALL_INPUT)
                                         .set("bucket", "my bucket")
                                         .set("key", "a/b c")
                                         .set("count", 5)
                                         .set("tags", Arrays.asList("x", "y z"))
                                         .set("since", WHEN)
                                         .set("params", params)
                                         .set("token", "tok")
                                         .set("when", WHEN)
                                         .set("items", Arrays.asList("a", "b,c"))
                                         .set("meta", Collections.singletonMap("One", "1"))
                                         .set("name", "n")
                                         .set("size", 3L)
                                         .build();
        SdkHttpFullRequest request = RequestSerializerTest.serialize(TestModel.ALL_OPERATION, input);

        Map<String, String> labels = new HashMap<>();
        labels.put("bucket", "my bucket");
        labels.put("key", "a/b c");
        SchemaStruct.Builder builder = SchemaStruct.builder(ALL_INPUT);
        HttpBinding.create().requestDeserializer()
                   .payloadCodec(CODEC)
                   .payloadMediaType("application/json")
                   .request(request)
                   .pathLabelValues(labels)
                   .inputShapeBuilder(builder)
                   .deserialize();
        SchemaStruct read = builder.build();

        // @httpQueryParams reads every query parameter, including the bound ones and the literals.
        Map<String, String> allParams = new LinkedHashMap<>();
        allParams.put("x-id", "Put");
        allParams.put("flag", "");
        allParams.put("count", "5");
        allParams.put("tag", "x");
        allParams.put("since", "2014-04-29T18:30:38Z");
        allParams.put("extra", "e");
        assertThat(read.get("params")).isEqualTo(allParams);
        assertThat(SchemaStruct.builder(ALL_INPUT)
                               .set("bucket", read.get("bucket"))
                               .set("key", read.get("key"))
                               .set("count", read.get("count"))
                               .set("tags", read.get("tags"))
                               .set("since", read.get("since"))
                               .set("params", params)
                               .set("token", read.get("token"))
                               .set("when", read.get("when"))
                               .set("items", read.get("items"))
                               .set("meta", read.get("meta"))
                               .set("name", read.get("name"))
                               .set("size", read.get("size"))
                               .build())
            .isEqualTo(input);
    }
}
