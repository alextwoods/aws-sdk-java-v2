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

package software.amazon.awssdk.protocols.core.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.protocols.core.binding.HttpMessageBindings.Direction;
import software.amazon.awssdk.protocols.core.binding.HttpMessageBindings.PayloadKind;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;

class HttpBindingsTest {
    private static final Schema UNIT = Schema.structureBuilder(ShapeId.from("smithy.api#Unit"), new UnitTypeTrait())
                                             .build();
    private static final Schema STRING_MAP = Schema.mapBuilder(ShapeId.from("com.example#StringMap"))
                                                   .putMember("key", PreludeSchemas.STRING)
                                                   .putMember("value", PreludeSchemas.STRING)
                                                   .build();
    private static final Schema STREAM = Schema.createBlob(ShapeId.from("com.example#Stream"), new StreamingTrait());

    private static ShapeId id(String name) {
        return ShapeId.from("com.example#" + name);
    }

    /** Every binding trait, applied to one structure. */
    private static Schema everyBinding() {
        return Schema.structureBuilder(id("PutWidgetInput"))
                     .putMember("bucket", PreludeSchemas.STRING, new HttpLabelTrait())
                     .putMember("key", PreludeSchemas.STRING, new HttpLabelTrait())
                     .putMember("version", PreludeSchemas.STRING, new HttpQueryTrait("v"))
                     .putMember("extra", STRING_MAP, new HttpQueryParamsTrait())
                     .putMember("contentType", PreludeSchemas.STRING, new HttpHeaderTrait("Content-Type"))
                     .putMember("meta", STRING_MAP, new HttpPrefixHeadersTrait("x-meta-"))
                     .putMember("status", PreludeSchemas.INTEGER, new HttpResponseCodeTrait())
                     .putMember("name", PreludeSchemas.STRING)
                     .build();
    }

    @Test
    void requestBindsLabelsQueryHeadersAndKeepsResponseCodeInTheBody() {
        HttpMessageBindings request = HttpMessageBindings.of(everyBinding(), Direction.REQUEST);
        assertEquals(List.of(HttpBindingLocation.LABEL, HttpBindingLocation.LABEL, HttpBindingLocation.QUERY,
                             HttpBindingLocation.QUERY_PARAMS, HttpBindingLocation.HEADER,
                             HttpBindingLocation.PREFIX_HEADERS, HttpBindingLocation.BODY, HttpBindingLocation.BODY),
                     request.bindings().stream().map(HttpMemberBinding::location).toList());
        assertEquals(List.of("bucket", "key", "v", "", "Content-Type", "x-meta-", "", ""),
                     request.bindings().stream().map(HttpMemberBinding::name).toList());
        assertEquals(List.of("status", "name"),
                     request.members(HttpBindingLocation.BODY).stream().map(HttpMemberBinding::memberName).toList());
        assertEquals(PayloadKind.MEMBERS, request.payloadKind());
        assertEquals(Optional.empty(), request.payload());
    }

    @Test
    void responseBindsHeadersAndStatusAndKeepsRequestOnlyTraitsInTheBody() {
        HttpMessageBindings response = HttpMessageBindings.of(everyBinding(), Direction.RESPONSE);
        assertEquals(List.of(HttpBindingLocation.BODY, HttpBindingLocation.BODY, HttpBindingLocation.BODY,
                             HttpBindingLocation.BODY, HttpBindingLocation.HEADER, HttpBindingLocation.PREFIX_HEADERS,
                             HttpBindingLocation.STATUS_CODE, HttpBindingLocation.BODY),
                     response.bindings().stream().map(HttpMemberBinding::location).toList());
        assertEquals(List.of(), response.members(HttpBindingLocation.LABEL));
        assertSame(response.structure().member("status"),
                   response.members(HttpBindingLocation.STATUS_CODE).get(0).member());
    }

    @Test
    void payloadKindsFollowThePayloadTarget() {
        assertEquals(PayloadKind.NONE, HttpMessageBindings.of(UNIT, Direction.REQUEST).payloadKind());
        assertEquals(PayloadKind.STREAMING_BLOB, payloadKind(STREAM));
        assertEquals(PayloadKind.BLOB, payloadKind(PreludeSchemas.BLOB));
        assertEquals(PayloadKind.STRING, payloadKind(PreludeSchemas.STRING));
        assertEquals(PayloadKind.STRUCTURED, payloadKind(PreludeSchemas.DOCUMENT));
        assertEquals(PayloadKind.STRUCTURED, payloadKind(Schema.structureBuilder(id("Nested"))
                                                               .putMember("a", PreludeSchemas.STRING).build()));
        Schema events = Schema.unionBuilder(id("Events"), new StreamingTrait())
                              .putMember("tick", Schema.structureBuilder(id("Tick")).build())
                              .build();
        assertEquals(PayloadKind.EVENT_STREAM, payloadKind(events));
        assertThrows(IllegalArgumentException.class, () -> payloadKind(PreludeSchemas.INTEGER));
    }

    private static PayloadKind payloadKind(Schema target) {
        Schema structure = Schema.structureBuilder(id("WithPayload"))
                                 .putMember("h", PreludeSchemas.STRING, new HttpHeaderTrait("x-h"))
                                 .putMember("body", target, new HttpPayloadTrait())
                                 .build();
        HttpMessageBindings bindings = HttpMessageBindings.of(structure, Direction.RESPONSE);
        assertEquals("body", bindings.payload().orElseThrow().memberName());
        return bindings.payloadKind();
    }

    @Test
    void unrepresentableStructuresAreRejected() {
        Schema payloadAndBody = Schema.structureBuilder(id("Bad"))
                                      .putMember("body", STREAM, new HttpPayloadTrait())
                                      .putMember("loose", PreludeSchemas.STRING)
                                      .build();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                                  () -> HttpMessageBindings.of(payloadAndBody, Direction.REQUEST));
        assertTrue(e.getMessage().contains("loose"), e.getMessage());
        Schema twoPayloads = Schema.structureBuilder(id("Bad2"))
                                   .putMember("a", STREAM, new HttpPayloadTrait())
                                   .putMember("b", STREAM, new HttpPayloadTrait())
                                   .build();
        assertThrows(IllegalArgumentException.class, () -> HttpMessageBindings.of(twoPayloads, Direction.RESPONSE));
        assertThrows(IllegalArgumentException.class, () -> HttpMessageBindings.of(STREAM, Direction.REQUEST));
    }

    @Test
    void operationPlanBindsRequestLineHostPrefixAndErrors() {
        Schema input = Schema.structureBuilder(id("PutWidgetInput"))
                             // A host label is not an HTTP binding, so with a payload it also needs a header.
                             .putMember("account", PreludeSchemas.STRING, new HostLabelTrait(),
                                        new HttpHeaderTrait("x-amz-account-id"))
                             .putMember("bucket", PreludeSchemas.STRING, new HttpLabelTrait())
                             .putMember("key", PreludeSchemas.STRING, new HttpLabelTrait())
                             .putMember("body", STREAM, new HttpPayloadTrait())
                             .build();
        Schema output = Schema.structureBuilder(id("PutWidgetOutput"))
                              .putMember("etag", PreludeSchemas.STRING, new HttpHeaderTrait("ETag"))
                              .build();
        Schema notFound = Schema.structureBuilder(id("NotFound"), new ErrorTrait("client"), new HttpErrorTrait(404))
                                .build();
        Schema throttled = Schema.structureBuilder(id("Throttled"), new ErrorTrait("client")).build();
        Schema internal = Schema.structureBuilder(id("Internal"), new ErrorTrait("server")).build();
        Schema operation = Schema.createOperation(
            id("PutWidget"),
            HttpTrait.builder().method("PUT").uri(UriPattern.parse("/{bucket}/{key+}?x-id=Put&flag")).code(201)
                     .build(),
            EndpointTrait.builder().hostPrefix("{account}.data.").build());
        HttpOperationBindings plan = HttpOperationBindings.of(OperationSchemas.builder(operation)
                                                                              .input(input)
                                                                              .output(output)
                                                                              .errors(internal, notFound, throttled)
                                                                              .inputStreamMember("body")
                                                                              .build());
        assertEquals(id("PutWidget"), plan.operationId());
        assertTrue(plan.hasHttpTrait());
        assertEquals(Optional.of("PUT"), plan.method());
        assertEquals(List.of(new UriSegment("bucket", UriSegment.Kind.LABEL),
                             new UriSegment("key", UriSegment.Kind.GREEDY_LABEL)), plan.path());
        assertEquals(List.of("x-id", "flag"), List.copyOf(plan.queryLiterals().keySet()));
        assertEquals("Put", plan.queryLiterals().get("x-id"));
        assertEquals("", plan.queryLiterals().get("flag"));
        assertEquals(201, plan.successCode());
        assertEquals(List.of("account"), plan.hostLabels().stream().map(Schema::memberName).toList());
        assertTrue(plan.hostPrefix().stream().anyMatch(s -> s.isLabel() && s.content().equals("account")));
        assertEquals(PayloadKind.STREAMING_BLOB, plan.request().payloadKind());
        assertEquals(PayloadKind.NONE, plan.response().payloadKind());
        assertEquals(Map.of(id("Internal"), 500, id("NotFound"), 404, id("Throttled"), 400), plan.errorCodes());
        assertEquals(List.of(id("Internal"), id("NotFound"), id("Throttled")), List.copyOf(plan.errors().keySet()));
    }

    @Test
    void operationWithoutHttpTraitHasBodyOnlyDefaults() {
        Schema input = Schema.structureBuilder(id("PingInput")).putMember("m", PreludeSchemas.STRING).build();
        HttpOperationBindings plan = HttpOperationBindings.of(OperationSchemas.builder(
            Schema.createOperation(id("Ping"))).input(input).output(UNIT).build());
        assertFalse(plan.hasHttpTrait());
        assertEquals(Optional.empty(), plan.method());
        assertEquals(List.of(), plan.path());
        assertEquals(HttpOperationBindings.DEFAULT_SUCCESS_CODE, plan.successCode());
        assertEquals(PayloadKind.MEMBERS, plan.request().payloadKind());
        assertEquals(Map.of(), plan.errors());
    }

    @Test
    void labelsMustCorrespondToLabelMembers() {
        Schema operation = Schema.createOperation(
            id("GetWidget"), HttpTrait.builder().method("GET").uri(UriPattern.parse("/w/{id}")).code(200).build());
        Schema missing = Schema.structureBuilder(id("NoLabel")).putMember("id", PreludeSchemas.STRING).build();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> HttpOperationBindings.of(
            OperationSchemas.builder(operation).input(missing).output(UNIT).build()));
        assertTrue(e.getMessage().contains("[id]"), e.getMessage());
        Schema extra = Schema.structureBuilder(id("ExtraLabel"))
                             .putMember("id", PreludeSchemas.STRING, new HttpLabelTrait())
                             .putMember("other", PreludeSchemas.STRING, new HttpLabelTrait())
                             .build();
        assertThrows(IllegalArgumentException.class, () -> HttpOperationBindings.of(
            OperationSchemas.builder(operation).input(extra).output(UNIT).build()));
        Schema hostOperation = Schema.createOperation(id("Hosted"),
                                                      EndpointTrait.builder().hostPrefix("{acct}.").build());
        assertThrows(IllegalArgumentException.class, () -> HttpOperationBindings.of(
            OperationSchemas.builder(hostOperation).input(missing).output(UNIT).build()));
    }
}
