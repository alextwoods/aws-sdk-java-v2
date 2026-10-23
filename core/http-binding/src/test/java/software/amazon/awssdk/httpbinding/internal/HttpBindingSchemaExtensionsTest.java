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

package software.amazon.awssdk.httpbinding.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.Binding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.MemberBinding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.RequestBinding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.ResponseBinding;
import software.amazon.awssdk.httpbinding.internal.HttpBindingSchemaExtensions.StructBindings;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
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
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

class HttpBindingSchemaExtensionsTest {
    private static final Schema LIST = Schema.listBuilder(ShapeId.from("smithy.example#L"))
                                             .putMember("member", PreludeSchemas.TIMESTAMP,
                                                        new TimestampFormatTrait("epoch-seconds"))
                                             .build();
    private static final Schema MAP = Schema.mapBuilder(ShapeId.from("smithy.example#M"))
                                            .putMember("key", PreludeSchemas.STRING)
                                            .putMember("value", PreludeSchemas.STRING)
                                            .build();
    private static final Schema STRUCT =
        Schema.structureBuilder(ShapeId.from("smithy.example#S"))
              .putMember("header", PreludeSchemas.TIMESTAMP, new HttpHeaderTrait("X-When"))
              .putMember("listHeader", LIST, new HttpHeaderTrait("X-List"))
              .putMember("query", PreludeSchemas.TIMESTAMP, new HttpQueryTrait("when"))
              .putMember("httpDateQuery", PreludeSchemas.TIMESTAMP, new HttpQueryTrait("d"),
                         new TimestampFormatTrait("http-date"))
              .putMember("label", PreludeSchemas.TIMESTAMP, HttpLabelTrait.INSTANCE)
              .putMember("prefix", MAP, new HttpPrefixHeadersTrait("X-Meta-"))
              .putMember("params", MAP, HttpQueryParamsTrait.INSTANCE)
              .putMember("status", PreludeSchemas.INTEGER, HttpResponseCodeTrait.INSTANCE)
              .putMember("payload", PreludeSchemas.BLOB, HttpPayloadTrait.INSTANCE)
              .putMember("body", PreludeSchemas.STRING)
              .build();

    @Test
    void bindingIsComputedOncePerSchemaInstance() {
        StructBindings first = HttpBindingSchemaExtensions.structBindingsOf(STRUCT);
        assertThat(HttpBindingSchemaExtensions.structBindingsOf(STRUCT)).isSameAs(first);
        assertThat(first.request()).isSameAs(first.request());
        assertThat(first.response()).isSameAs(first.response());
        Schema member = STRUCT.member("header");
        assertThat(HttpBindingSchemaExtensions.memberBindingOf(member))
            .isSameAs(HttpBindingSchemaExtensions.memberBindingOf(member));
    }

    @Test
    void memberKindsAndLocationTimestampDefaults() {
        assertBinding("header", Binding.HEADER, "X-When", TimestampFormatTrait.Format.HTTP_DATE);
        assertBinding("listHeader", Binding.HEADER, "X-List", TimestampFormatTrait.Format.EPOCH_SECONDS);
        assertBinding("query", Binding.QUERY, "when", TimestampFormatTrait.Format.DATE_TIME);
        assertBinding("httpDateQuery", Binding.QUERY, "d", TimestampFormatTrait.Format.HTTP_DATE);
        assertBinding("label", Binding.LABEL, "label", TimestampFormatTrait.Format.DATE_TIME);
        assertBinding("prefix", Binding.PREFIX_HEADERS, "X-Meta-", null);
        assertBinding("params", Binding.QUERY_PARAMS, null, null);
        assertBinding("status", Binding.STATUS, null, null);
        assertBinding("payload", Binding.PAYLOAD, null, null);
        assertBinding("body", Binding.BODY, null, null);
        assertThat(HttpBindingSchemaExtensions.memberBindingOf(STRUCT.member("listHeader")).isList()).isTrue();
        assertThat(HttpBindingSchemaExtensions.memberBindingOf(STRUCT.member("header")).canonicalName())
            .isEqualTo("x-when");
    }

    @Test
    void directionPlansFoldBindingsThatDoNotApply() {
        StructBindings bindings = HttpBindingSchemaExtensions.structBindingsOf(STRUCT);
        RequestBinding request = bindings.request();
        assertThat(request.bindings[STRUCT.member("status").memberIndex()]).isEqualTo(Binding.BODY);
        assertThat(request.bindings[STRUCT.member("label").memberIndex()]).isEqualTo(Binding.LABEL);
        assertThat(request.labelMembers).containsExactly(STRUCT.member("label"));
        assertThat(request.queryWireNames).containsExactly("when", "d");
        assertThat(request.hasBody).isTrue();
        assertThat(request.headers.headerCanonicalNames).containsExactlyInAnyOrder("x-when", "x-list");

        ResponseBinding response = bindings.response();
        assertThat(response.bindings[STRUCT.member("status").memberIndex()]).isEqualTo(Binding.STATUS);
        assertThat(response.bindings[STRUCT.member("label").memberIndex()]).isEqualTo(Binding.BODY);
        assertThat(response.bindings[STRUCT.member("query").memberIndex()]).isEqualTo(Binding.BODY);
        assertThat(response.bindings[STRUCT.member("params").memberIndex()]).isEqualTo(Binding.BODY);
        assertThat(response.statusMember).isSameAs(STRUCT.member("status"));
        assertThat(response.headers.listHeaderNames).containsExactly("X-List");
        assertThat(response.headers.scalarHeaderNames).containsExactly("X-When");
        assertThat(response.defaultStatus).isEqualTo(-1);
    }

    @Test
    void errorStatusComesFromHttpErrorThenError() {
        Schema httpError = Schema.structureBuilder(ShapeId.from("smithy.example#E1"), new ErrorTrait("client"),
                                                   new HttpErrorTrait(429)).build();
        Schema client = Schema.structureBuilder(ShapeId.from("smithy.example#E2"), new ErrorTrait("client")).build();
        Schema server = Schema.structureBuilder(ShapeId.from("smithy.example#E3"), new ErrorTrait("server")).build();
        assertThat(HttpBindingSchemaExtensions.structBindingsOf(httpError).response().defaultStatus).isEqualTo(429);
        assertThat(HttpBindingSchemaExtensions.structBindingsOf(client).response().defaultStatus).isEqualTo(400);
        assertThat(HttpBindingSchemaExtensions.structBindingsOf(server).response().defaultStatus).isEqualTo(500);
    }

    @Test
    void operationBindingParsesTheUriOnce() {
        Schema op = Schema.createOperation(ShapeId.from("smithy.example#Op"),
                                           new HttpTrait("GET", "/x/{label}?a=1&b", 202));
        HttpBindingSchemaExtensions.OperationBinding binding = HttpBindingSchemaExtensions.operationBindingOf(op);
        assertThat(HttpBindingSchemaExtensions.operationBindingOf(op)).isSameAs(binding);
        assertThat(binding.queryLiteralKeys()).containsExactly("a", "b");
        assertThat(binding.queryLiteralValues()).containsExactly("1", null);
        assertThat(binding.defaultResponseStatus()).isEqualTo(202);
    }

    @Test
    void operationBindingResolvesTheSdkHttpMethodOnce() {
        HttpBindingSchemaExtensions.OperationBinding put = HttpBindingSchemaExtensions.operationBindingOf(
            Schema.createOperation(ShapeId.from("smithy.example#Put"), new HttpTrait("PUT", "/p")));
        HttpBindingSchemaExtensions.OperationBinding get = HttpBindingSchemaExtensions.operationBindingOf(
            Schema.createOperation(ShapeId.from("smithy.example#Get"), new HttpTrait("get", "/g")));
        assertThat(put.sdkHttpMethod()).isEqualTo(SdkHttpMethod.PUT);
        assertThat(put.sdkHttpMethod()).isSameAs(put.sdkHttpMethod());
        assertThat(get.sdkHttpMethod()).isEqualTo(SdkHttpMethod.GET);

        HttpBindingSchemaExtensions.OperationBinding foo = HttpBindingSchemaExtensions.operationBindingOf(
            Schema.createOperation(ShapeId.from("smithy.example#Foo"), new HttpTrait("FOO", "/f")));
        assertThatThrownBy(foo::sdkHttpMethod)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported HTTP method name FOO");
    }

    @Test
    void sdkHttpMethod_doesNotAllocateAfterTheBindingIsBuilt() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled());
        HttpBindingSchemaExtensions.OperationBinding binding = HttpBindingSchemaExtensions.operationBindingOf(
            Schema.createOperation(ShapeId.from("smithy.example#Post"), new HttpTrait("POST", "/p")));
        binding.sdkHttpMethod();

        long threadId = Thread.currentThread().getId();
        long before = threads.getThreadAllocatedBytes(threadId);
        SdkHttpMethod last = null;
        for (int i = 0; i < 1_000; i++) {
            last = binding.sdkHttpMethod();
        }
        long allocated = threads.getThreadAllocatedBytes(threadId) - before;

        assertThat(last).isEqualTo(SdkHttpMethod.POST);
        // SdkHttpMethod.fromValue allocates a stream pipeline per call (~250 B); 1,000 cached reads allocate nothing.
        assertThat(allocated).isLessThan(1_000);
    }

    @Test
    void schemasWithoutBindingsAreRejected() {
        Schema op = Schema.createOperation(ShapeId.from("smithy.example#NoHttp"));
        assertThatThrownBy(() -> HttpBindingSchemaExtensions.operationBindingOf(op))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("smithy.example#NoHttp");
        assertThatThrownBy(() -> HttpBindingSchemaExtensions.structBindingsOf(PreludeSchemas.STRING))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void quotesHeaderListElementsWithCommasOrQuotes() {
        assertThat(HttpBindingSerializer.quoteHeaderListElement("a")).isEqualTo("a");
        assertThat(HttpBindingSerializer.quoteHeaderListElement("b,c")).isEqualTo("\"b,c\"");
        assertThat(HttpBindingSerializer.quoteHeaderListElement("\"def\"")).isEqualTo("\"\\\"def\\\"\"");
        assertThat(HttpBindingSerializer.quoteHeaderListElement("a\\b")).isEqualTo("\"a\\\\b\"");
        assertThat(HttpBindingSerializer.quoteHeaderListElement("a\\,b")).isEqualTo("\"a\\\\,b\"");
    }

    private static void assertBinding(String member, Binding kind, String wireName,
                                      TimestampFormatTrait.Format format) {
        MemberBinding binding = HttpBindingSchemaExtensions.memberBindingOf(STRUCT.member(member));
        assertThat(binding.kind()).as(member).isEqualTo(kind);
        assertThat(binding.wireName()).as(member).isEqualTo(wireName);
        if (format == null) {
            assertThat(binding.timestampFormatter()).as(member).isNull();
        } else {
            assertThat(binding.timestampFormatter().format()).as(member).isEqualTo(format);
        }
    }
}
