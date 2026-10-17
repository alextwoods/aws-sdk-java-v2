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

import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.HttpErrorTrait;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.HttpLabelTrait;
import software.amazon.awssdk.schema.traits.HttpPayloadTrait;
import software.amazon.awssdk.schema.traits.HttpPrefixHeadersTrait;
import software.amazon.awssdk.schema.traits.HttpQueryParamsTrait;
import software.amazon.awssdk.schema.traits.HttpQueryTrait;
import software.amazon.awssdk.schema.traits.HttpResponseCodeTrait;
import software.amazon.awssdk.schema.traits.HttpTrait;
import software.amazon.awssdk.schema.traits.MediaTypeTrait;
import software.amazon.awssdk.schema.traits.StreamingTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Hand-written schemas for the HTTP binding tests, standing in for generated model classes.
 */
final class TestModel {
    static final String NS = "smithy.example";
    static final ApiService SERVICE = () -> Schema.createService(ShapeId.fromParts(NS, "ExampleService"));

    static final Schema STRING_LIST = Schema.listBuilder(id("StringList"))
                                            .putMember("member", PreludeSchemas.STRING)
                                            .build();
    static final Schema INTEGER_LIST = Schema.listBuilder(id("IntegerList"))
                                             .putMember("member", PreludeSchemas.INTEGER)
                                             .build();
    static final Schema TIMESTAMP_LIST = Schema.listBuilder(id("TimestampList"))
                                               .putMember("member", PreludeSchemas.TIMESTAMP)
                                               .build();
    static final Schema DATE_TIME_LIST = Schema.listBuilder(id("DateTimeList"))
                                               .putMember("member", PreludeSchemas.TIMESTAMP,
                                                          new TimestampFormatTrait("date-time"))
                                               .build();
    static final Schema STRING_MAP = Schema.mapBuilder(id("StringMap"))
                                           .putMember("key", PreludeSchemas.STRING)
                                           .putMember("value", PreludeSchemas.STRING)
                                           .build();
    static final Schema STRING_LIST_MAP = Schema.mapBuilder(id("StringListMap"))
                                                .putMember("key", PreludeSchemas.STRING)
                                                .putMember("value", STRING_LIST)
                                                .build();

    static final Schema ALL_INPUT =
        Schema.structureBuilder(id("AllBindingsInput"))
              .putMember("bucket", PreludeSchemas.STRING, HttpLabelTrait.INSTANCE)
              .putMember("key", PreludeSchemas.STRING, HttpLabelTrait.INSTANCE)
              .putMember("count", PreludeSchemas.INTEGER, new HttpQueryTrait("count"))
              .putMember("tags", STRING_LIST, new HttpQueryTrait("tag"))
              .putMember("since", PreludeSchemas.TIMESTAMP, new HttpQueryTrait("since"))
              .putMember("params", STRING_MAP, HttpQueryParamsTrait.INSTANCE)
              .putMember("token", PreludeSchemas.STRING, new HttpHeaderTrait("X-Token"))
              .putMember("when", PreludeSchemas.TIMESTAMP, new HttpHeaderTrait("X-When"))
              .putMember("items", STRING_LIST, new HttpHeaderTrait("X-Items"))
              .putMember("meta", STRING_MAP, new HttpPrefixHeadersTrait("X-Meta-"))
              .putMember("name", PreludeSchemas.STRING)
              .putMember("size", PreludeSchemas.LONG)
              .build();
    static final ApiOperation<SerializableStruct, SerializableStruct> ALL_OPERATION =
        operation("AllBindings", new HttpTrait("PUT", "/{bucket}/{key+}?x-id=Put&flag"), ALL_INPUT, ALL_INPUT);

    static final Schema ALL_OUTPUT =
        Schema.structureBuilder(id("AllBindingsOutput"))
              .putMember("token", PreludeSchemas.STRING, new HttpHeaderTrait("X-Token"))
              .putMember("when", PreludeSchemas.TIMESTAMP, new HttpHeaderTrait("X-When"))
              .putMember("items", STRING_LIST, new HttpHeaderTrait("X-Items"))
              .putMember("numbers", INTEGER_LIST, new HttpHeaderTrait("X-Numbers"))
              .putMember("dates", TIMESTAMP_LIST, new HttpHeaderTrait("X-Dates"))
              .putMember("isoDates", DATE_TIME_LIST, new HttpHeaderTrait("X-Iso-Dates"))
              .putMember("flag", PreludeSchemas.BOOLEAN, new HttpHeaderTrait("X-Flag"))
              .putMember("num", PreludeSchemas.INTEGER, new HttpHeaderTrait("X-Num"))
              .putMember("epoch", PreludeSchemas.TIMESTAMP, new HttpHeaderTrait("X-Epoch"),
                         new TimestampFormatTrait("epoch-seconds"))
              .putMember("meta", STRING_MAP, new HttpPrefixHeadersTrait("X-Meta-"))
              .putMember("status", PreludeSchemas.INTEGER, HttpResponseCodeTrait.INSTANCE)
              .putMember("name", PreludeSchemas.STRING)
              .putMember("size", PreludeSchemas.LONG)
              .build();
    static final ApiOperation<SerializableStruct, SerializableStruct> ALL_OUTPUT_OPERATION =
        operation("AllBindingsOut", new HttpTrait("GET", "/out", 201), ALL_INPUT, ALL_OUTPUT);

    static final Schema LABEL_INPUT =
        Schema.structureBuilder(id("LabelInput"))
              .putMember("plain", PreludeSchemas.STRING, HttpLabelTrait.INSTANCE)
              .putMember("greedy", PreludeSchemas.STRING, HttpLabelTrait.INSTANCE)
              .build();
    static final ApiOperation<SerializableStruct, SerializableStruct> LABEL_OPERATION =
        operation("Labels", new HttpTrait("GET", "/a/{plain}/b/{greedy+}"), LABEL_INPUT, LABEL_INPUT);

    static final Schema QUERY_INPUT =
        Schema.structureBuilder(id("QueryInput"))
              .putMember("params", STRING_LIST_MAP, HttpQueryParamsTrait.INSTANCE)
              .putMember("value", PreludeSchemas.STRING, new HttpQueryTrait("q"))
              .build();
    static final ApiOperation<SerializableStruct, SerializableStruct> QUERY_OPERATION =
        operation("Query", new HttpTrait("GET", "/query?lit=1"), QUERY_INPUT, QUERY_INPUT);

    static final Schema EMPTY_INPUT = Schema.structureBuilder(id("EmptyInput")).build();
    static final ApiOperation<SerializableStruct, SerializableStruct> EMPTY_OPERATION =
        operation("Empty", new HttpTrait("POST", "/"), EMPTY_INPUT, EMPTY_INPUT);

    static final Schema HEADER_ONLY =
        Schema.structureBuilder(id("HeaderOnly"))
              .putMember("token", PreludeSchemas.STRING, new HttpHeaderTrait("X-Token"))
              .putMember("contentType", PreludeSchemas.STRING, new HttpHeaderTrait("Content-Type"))
              .build();
    static final ApiOperation<SerializableStruct, SerializableStruct> HEADER_ONLY_OPERATION =
        operation("HeaderOnly", new HttpTrait("POST", "/h"), HEADER_ONLY, HEADER_ONLY);

    static final Schema BLOB_PAYLOAD =
        Schema.structureBuilder(id("BlobPayload"))
              .putMember("data", PreludeSchemas.BLOB, HttpPayloadTrait.INSTANCE)
              .putMember("token", PreludeSchemas.STRING, new HttpHeaderTrait("X-Token"))
              .build();
    static final ApiOperation<SerializableStruct, SerializableStruct> BLOB_OPERATION =
        operation("BlobPayloadOperation", new HttpTrait("POST", "/blob"), BLOB_PAYLOAD, BLOB_PAYLOAD);

    static final Schema TEXT = Schema.createString(id("Text"), new MediaTypeTrait("text/markdown"));
    static final Schema STRING_PAYLOAD =
        Schema.structureBuilder(id("StringPayload"))
              .putMember("value", TEXT, HttpPayloadTrait.INSTANCE)
              .build();
    static final ApiOperation<SerializableStruct, SerializableStruct> STRING_OPERATION =
        operation("StringPayloadOperation", new HttpTrait("POST", "/string"), STRING_PAYLOAD, STRING_PAYLOAD);

    static final Schema GREETING = Schema.structureBuilder(id("Greeting"))
                                         .putMember("name", PreludeSchemas.STRING)
                                         .build();
    static final Schema STRUCT_PAYLOAD =
        Schema.structureBuilder(id("StructPayload"))
              .putMember("greeting", GREETING, HttpPayloadTrait.INSTANCE)
              .putMember("token", PreludeSchemas.STRING, new HttpHeaderTrait("X-Token"))
              .build();
    static final ApiOperation<SerializableStruct, SerializableStruct> STRUCT_OPERATION =
        operation("StructPayloadOperation", new HttpTrait("POST", "/struct"), STRUCT_PAYLOAD, STRUCT_PAYLOAD);

    static final Schema LIST_PAYLOAD =
        Schema.structureBuilder(id("ListPayload"))
              .putMember("items", STRING_LIST, HttpPayloadTrait.INSTANCE)
              .build();
    static final ApiOperation<SerializableStruct, SerializableStruct> LIST_OPERATION =
        operation("ListPayloadOperation", new HttpTrait("POST", "/list"), LIST_PAYLOAD, LIST_PAYLOAD);

    static final Schema STREAMING_BLOB = Schema.createBlob(id("Stream"), StreamingTrait.INSTANCE);
    static final Schema STREAMING_PAYLOAD =
        Schema.structureBuilder(id("StreamingPayload"))
              .putMember("body", STREAMING_BLOB, HttpPayloadTrait.INSTANCE)
              .putMember("token", PreludeSchemas.STRING, new HttpHeaderTrait("X-Token"))
              .build();
    static final ApiOperation<SerializableStruct, SerializableStruct> STREAMING_OPERATION =
        operation("StreamingOperation", new HttpTrait("PUT", "/stream"), STREAMING_PAYLOAD, STREAMING_PAYLOAD);

    static final Schema NOT_FOUND =
        Schema.structureBuilder(id("NotFound"), new ErrorTrait("client"), new HttpErrorTrait(404))
              .putMember("message", PreludeSchemas.STRING)
              .putMember("resource", PreludeSchemas.STRING, new HttpHeaderTrait("X-Resource"))
              .build();
    static final Schema SERVER_ERROR =
        Schema.structureBuilder(id("ServerError"), new ErrorTrait("server"))
              .putMember("message", PreludeSchemas.STRING)
              .build();

    private TestModel() {
    }

    static ShapeId id(String name) {
        return ShapeId.fromParts(NS, name);
    }

    static ApiOperation<SerializableStruct, SerializableStruct> operation(String name, HttpTrait http, Schema input,
                                                                         Schema output) {
        return SimpleApiOperation.<SerializableStruct, SerializableStruct>builder()
                                 .schema(Schema.createOperation(id(name), http))
                                 .inputSchema(() -> input)
                                 .outputSchema(() -> output)
                                 .service(SERVICE)
                                 .build();
    }
}
