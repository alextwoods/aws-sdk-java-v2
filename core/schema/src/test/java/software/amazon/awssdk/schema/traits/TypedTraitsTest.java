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

package software.amazon.awssdk.schema.traits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Covers every built-in typed trait: creation from its Smithy JSON AST value, {@code toDocument} round trip, and value
 * equality.
 */
class TypedTraitsTest {

    /**
     * One case per built-in factory: the trait ID, a value in Smithy JSON AST form, the trait that value must produce,
     * and a trait of the same class with a different value (or null for singleton annotation traits).
     */
    static Stream<Arguments> cases() {
        return Stream.of(
            annotation(RequiredTrait.ID, RequiredTrait.INSTANCE),
            annotation(SensitiveTrait.ID, SensitiveTrait.INSTANCE),
            annotation(SparseTrait.ID, SparseTrait.INSTANCE),
            annotation(IdempotencyTokenTrait.ID, IdempotencyTokenTrait.INSTANCE),
            annotation(StreamingTrait.ID, StreamingTrait.INSTANCE),
            annotation(RequiresLengthTrait.ID, RequiresLengthTrait.INSTANCE),
            annotation(EventHeaderTrait.ID, EventHeaderTrait.INSTANCE),
            annotation(EventPayloadTrait.ID, EventPayloadTrait.INSTANCE),
            annotation(HostLabelTrait.ID, HostLabelTrait.INSTANCE),
            annotation(HttpChecksumRequiredTrait.ID, HttpChecksumRequiredTrait.INSTANCE),
            annotation(XmlAttributeTrait.ID, XmlAttributeTrait.INSTANCE),
            annotation(XmlFlattenedTrait.ID, XmlFlattenedTrait.INSTANCE),
            annotation(HttpLabelTrait.ID, HttpLabelTrait.INSTANCE),
            annotation(HttpQueryParamsTrait.ID, HttpQueryParamsTrait.INSTANCE),
            annotation(HttpPayloadTrait.ID, HttpPayloadTrait.INSTANCE),
            annotation(HttpResponseCodeTrait.ID, HttpResponseCodeTrait.INSTANCE),
            annotation(UnitTypeTrait.ID, UnitTypeTrait.INSTANCE),
            annotation(OptionalAuthTrait.ID, OptionalAuthTrait.INSTANCE),
            annotation(AwsQueryCompatibleTrait.ID, AwsQueryCompatibleTrait.INSTANCE),
            annotation(AwsQueryTrait.ID, AwsQueryTrait.INSTANCE),
            annotation(Ec2QueryTrait.ID, Ec2QueryTrait.INSTANCE),
            annotation(UnsignedPayloadTrait.ID, UnsignedPayloadTrait.INSTANCE),

            string(MediaTypeTrait.ID, new MediaTypeTrait("application/json"), new MediaTypeTrait("text/plain")),
            string(XmlNameTrait.ID, new XmlNameTrait("Item"), new XmlNameTrait("Other")),
            string(JsonNameTrait.ID, new JsonNameTrait("name"), new JsonNameTrait("other")),
            string(TimestampFormatTrait.ID, new TimestampFormatTrait("date-time"), new TimestampFormatTrait("http-date")),
            string(HttpHeaderTrait.ID, new HttpHeaderTrait("X-Foo"), new HttpHeaderTrait("X-Bar")),
            string(HttpQueryTrait.ID, new HttpQueryTrait("q"), new HttpQueryTrait("r")),
            string(HttpPrefixHeadersTrait.ID, new HttpPrefixHeadersTrait("X-Meta-"), new HttpPrefixHeadersTrait("X-")),
            string(Ec2QueryNameTrait.ID, new Ec2QueryNameTrait("Name"), new Ec2QueryNameTrait("Other")),
            string(ErrorTrait.ID, new ErrorTrait("client"), new ErrorTrait("server")),

            Arguments.of(DefaultTrait.ID, Document.of(5), new DefaultTrait(Document.of(5)),
                         new DefaultTrait(Document.of("5"))),
            Arguments.of(HttpErrorTrait.ID, Document.of(404), new HttpErrorTrait(404), new HttpErrorTrait(500)),
            Arguments.of(RetryableTrait.ID, map("throttling", Document.of(true)), new RetryableTrait(true),
                         new RetryableTrait(false)),
            Arguments.of(EndpointTrait.ID, map("hostPrefix", Document.of("{AccountId}.")),
                         new EndpointTrait("{AccountId}."), new EndpointTrait("data.")),
            Arguments.of(HttpTrait.ID,
                         map("method", Document.of("PUT"), "uri", Document.of("/{Bucket}/{Key+}"), "code", Document.of(201)),
                         new HttpTrait("PUT", "/{Bucket}/{Key+}", 201), new HttpTrait("PUT", "/{Bucket}/{Key+}")),
            Arguments.of(RequestCompressionTrait.ID, map("encodings", strings("gzip")),
                         new RequestCompressionTrait(Collections.singletonList("gzip")),
                         new RequestCompressionTrait(Collections.<String>emptyList())),
            Arguments.of(PaginatedTrait.ID,
                         map("inputToken", Document.of("NextToken"), "outputToken", Document.of("NextToken"),
                             "items", Document.of("Items"), "pageSize", Document.of("MaxResults")),
                         new PaginatedTrait("NextToken", "NextToken", "Items", "MaxResults"),
                         new PaginatedTrait("NextToken", "NextToken", null, null)),
            Arguments.of(XmlNamespaceTrait.ID, map("uri", Document.of("https://example.com/ns"), "prefix", Document.of("ex")),
                         new XmlNamespaceTrait("https://example.com/ns", "ex"),
                         new XmlNamespaceTrait("https://example.com/ns")),
            Arguments.of(AuthTrait.ID, strings("aws.auth#sigv4", "smithy.api#httpBearerAuth"),
                         new AuthTrait(Arrays.asList(ShapeId.from("aws.auth#sigv4"),
                                                     ShapeId.from("smithy.api#httpBearerAuth"))),
                         new AuthTrait(Collections.<ShapeId>emptyList())),
            Arguments.of(HttpChecksumTrait.ID,
                         map("requestAlgorithmMember", Document.of("ChecksumAlgorithm"),
                             "requestValidationModeMember", Document.of("ChecksumMode"),
                             "requestChecksumRequired", Document.of(true),
                             "responseAlgorithms", strings("CRC32", "SHA256")),
                         new HttpChecksumTrait(true, "ChecksumAlgorithm", "ChecksumMode", Arrays.asList("CRC32", "SHA256")),
                         new HttpChecksumTrait(false, null, null, Collections.<String>emptyList())),
            Arguments.of(AwsQueryErrorTrait.ID,
                         map("code", Document.of("AWS.SimpleQueueService.NonExistentQueue"),
                             "httpResponseCode", Document.of(400)),
                         new AwsQueryErrorTrait("AWS.SimpleQueueService.NonExistentQueue", 400),
                         new AwsQueryErrorTrait("AWS.SimpleQueueService.NonExistentQueue", 404)),
            Arguments.of(AwsJson1_0Trait.ID, map("http", strings("h2", "http/1.1")),
                         new AwsJson1_0Trait(Arrays.asList("h2", "http/1.1"), Collections.<String>emptyList()),
                         new AwsJson1_0Trait()),
            Arguments.of(AwsJson1_1Trait.ID, map("eventStreamHttp", strings("h2")),
                         new AwsJson1_1Trait(Collections.<String>emptyList(), Collections.singletonList("h2")),
                         new AwsJson1_1Trait()),
            Arguments.of(RestJson1Trait.ID, map("http", strings("http/1.1"), "eventStreamHttp", strings("h2")),
                         new RestJson1Trait(Collections.singletonList("http/1.1"), Collections.singletonList("h2")),
                         new RestJson1Trait()),
            Arguments.of(RestXmlTrait.ID, map("noErrorWrapping", Document.of(true)),
                         new RestXmlTrait(Collections.<String>emptyList(), Collections.<String>emptyList(), true),
                         new RestXmlTrait()),
            Arguments.of(Rpcv2CborTrait.ID, map("http", strings("h2")),
                         new Rpcv2CborTrait(Collections.singletonList("h2"), Collections.<String>emptyList()),
                         new Rpcv2CborTrait()),
            Arguments.of(SigV4Trait.ID, map("name", Document.of("sqs")), new SigV4Trait("sqs"), new SigV4Trait("sns")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void create_withModelValue_returnsExpectedTypedTrait(ShapeId id, Document value, Trait expected, Trait different) {
        Trait trait = TraitFactories.create(id, value);

        assertThat(trait).isEqualTo(expected);
        assertThat(trait.getClass()).isEqualTo(TraitFactories.factoryFor(id).traitClass());
        assertThat(trait.traitId()).isEqualTo(id);
        assertThat(trait.hashCode()).isEqualTo(expected.hashCode());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void toDocument_returnsModelValueAndRecreatesEqualTrait(ShapeId id, Document value, Trait expected, Trait different) {
        assertThat(expected.toDocument()).isEqualTo(value);
        assertThat(TraitFactories.create(id, expected.toDocument())).isEqualTo(expected);
        if (different != null) {
            assertThat(TraitFactories.create(id, different.toDocument())).isEqualTo(different);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void equals_withDifferentValueOrClass_isFalse(ShapeId id, Document value, Trait expected, Trait different) {
        if (different != null) {
            assertThat(expected).isNotEqualTo(different);
        }
        assertThat(expected).isNotEqualTo(new GenericTrait(id, value));
        assertThat(expected).isNotEqualTo(null);
    }

    @Test
    void annotationTraits_ignoreObjectContentsAndRejectNonObjects() {
        assertThat(TraitFactories.create(SensitiveTrait.ID, map("unknown", Document.of(1))))
            .isSameAs(SensitiveTrait.INSTANCE);
        assertThatThrownBy(() -> TraitFactories.create(SensitiveTrait.ID, Document.of("x")))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#sensitive");
    }

    @Test
    void objectTraits_ignoreUnknownMembersAndApplyDefaults() {
        assertThat(TraitFactories.create(HttpTrait.ID, map("method", Document.of("GET"), "uri", Document.of("/"),
                                                          "future", Document.of(true))))
            .isEqualTo(new HttpTrait("GET", "/", 200));
        assertThat(TraitFactories.create(RetryableTrait.ID, Document.EMPTY_MAP)).isEqualTo(new RetryableTrait(false));
        assertThat(new RetryableTrait(false).toDocument()).isEqualTo(Document.EMPTY_MAP);
        assertThat(TraitFactories.create(PaginatedTrait.ID, Document.EMPTY_MAP))
            .isEqualTo(new PaginatedTrait(null, null, null, null));
    }

    @Test
    void objectTraits_withMissingOrMistypedMembers_throwNamingTraitAndMember() {
        assertThatThrownBy(() -> TraitFactories.create(HttpTrait.ID, map("method", Document.of("GET"))))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#http")
            .hasMessageContaining("'uri'");
        assertThatThrownBy(() -> TraitFactories.create(AwsQueryErrorTrait.ID,
                                                        map("code", Document.of("C"), "httpResponseCode", Document.of(1.5))))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("aws.protocols#awsQueryError")
            .hasMessageContaining("'httpResponseCode'");
        assertThatThrownBy(() -> TraitFactories.create(RequestCompressionTrait.ID,
                                                        map("encodings", Document.of(Collections.singletonList(Document.of(1))))))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#requestCompression");
        assertThatThrownBy(() -> TraitFactories.create(AuthTrait.ID, strings("not-a-shape-id")))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#auth")
            .hasMessageContaining("not-a-shape-id");
        assertThatThrownBy(() -> TraitFactories.create(ErrorTrait.ID, Document.of("neither")))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#error");
        assertThatThrownBy(() -> TraitFactories.create(RetryableTrait.ID, map("throttling", Document.of("yes"))))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#retryable");
    }

    @Test
    void integerTraits_acceptIntegralValuesOfAnyNumericType() {
        assertThat(TraitFactories.create(HttpErrorTrait.ID, Document.of(404L))).isEqualTo(new HttpErrorTrait(404));
        assertThat(TraitFactories.create(HttpErrorTrait.ID, Document.of(404.0d))).isEqualTo(new HttpErrorTrait(404));
        assertThatThrownBy(() -> TraitFactories.create(HttpErrorTrait.ID, Document.of(1L << 40)))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.api#httpError");
    }

    @Test
    void defaultTrait_acceptsAnyValueIncludingNull() {
        assertThat(TraitFactories.create(DefaultTrait.ID, null)).isEqualTo(new DefaultTrait(null));
        assertThat(new DefaultTrait(null).getValue()).isNull();
        assertThat(new DefaultTrait(null)).isNotEqualTo(new DefaultTrait(Document.EMPTY_MAP));
        assertThat(((DefaultTrait) TraitFactories.create(DefaultTrait.ID, Document.EMPTY_MAP)).getValue())
            .isEqualTo(Document.EMPTY_MAP);
    }

    @Test
    void errorTrait_reportsFaultAndDefaultStatus() {
        assertThat(new ErrorTrait("client").isClientError()).isTrue();
        assertThat(new ErrorTrait("client").getDefaultHttpStatusCode()).isEqualTo(400);
        assertThat(new ErrorTrait("server").isServerError()).isTrue();
        assertThat(new ErrorTrait("server").getDefaultHttpStatusCode()).isEqualTo(500);
        assertThatThrownBy(() -> new ErrorTrait("other")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void timestampFormatTrait_mapsKnownAndUnknownFormats() {
        assertThat(new TimestampFormatTrait("epoch-seconds").getFormat()).isEqualTo(TimestampFormatTrait.Format.EPOCH_SECONDS);
        assertThat(new TimestampFormatTrait("date-time").getFormat()).isEqualTo(TimestampFormatTrait.Format.DATE_TIME);
        assertThat(new TimestampFormatTrait("http-date").getFormat()).isEqualTo(TimestampFormatTrait.Format.HTTP_DATE);
        assertThat(new TimestampFormatTrait("unknown").getFormat()).isEqualTo(TimestampFormatTrait.Format.UNKNOWN);
        assertThat(new TimestampFormatTrait("custom").getFormat()).isEqualTo(TimestampFormatTrait.Format.UNKNOWN);
        assertThat(TimestampFormatTrait.Format.DATE_TIME).hasToString("date-time");
    }

    @Test
    void authTrait_keepsFirstOccurrenceOrder() {
        ShapeId sigv4 = ShapeId.from("aws.auth#sigv4");
        ShapeId noAuth = ShapeId.from("smithy.api#noAuth");
        AuthTrait trait = new AuthTrait(Arrays.asList(sigv4, noAuth, sigv4));

        assertThat(trait.getValues()).containsExactly(sigv4, noAuth);
        assertThat(trait.getValueSet()).containsExactly(sigv4, noAuth);
    }

    @Test
    void listValuedTraits_copyConstructorArgumentsAndExposeUnmodifiableLists() {
        List<String> encodings = new ArrayList<>(Collections.singletonList("gzip"));
        RequestCompressionTrait trait = new RequestCompressionTrait(encodings);
        encodings.add("br");

        assertThat(trait.getEncodings()).containsExactly("gzip");
        assertThatThrownBy(() -> trait.getEncodings().add("br")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new RequestCompressionTrait(Arrays.asList("gzip", null)))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void optionalProperties_areExposedAsOptionals() {
        PaginatedTrait paginated = new PaginatedTrait("in", null, null, "size");
        assertThat(paginated.getInputToken()).hasValue("in");
        assertThat(paginated.getOutputToken()).isEmpty();
        assertThat(paginated.getItems()).isEmpty();
        assertThat(paginated.getPageSize()).hasValue("size");

        assertThat(new XmlNamespaceTrait("uri").getPrefix()).isEmpty();
        assertThat(new HttpChecksumTrait(false, null, "Mode", Collections.<String>emptyList())
                       .getRequestValidationModeMember()).hasValue("Mode");
    }

    private static Arguments annotation(ShapeId id, AnnotationTrait instance) {
        return Arguments.of(id, Document.EMPTY_MAP, instance, null);
    }

    private static Arguments string(ShapeId id, StringTrait trait, StringTrait different) {
        return Arguments.of(id, Document.of(trait.getValue()), trait, different);
    }

    private static Document strings(String... values) {
        Document[] elements = new Document[values.length];
        for (int i = 0; i < values.length; i++) {
            elements[i] = Document.of(values[i]);
        }
        return Document.of(Arrays.asList(elements));
    }

    private static Document map(Object... keysAndValues) {
        Map<String, Document> members = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            members.put((String) keysAndValues[i], (Document) keysAndValues[i + 1]);
        }
        return Document.of(members);
    }
}
