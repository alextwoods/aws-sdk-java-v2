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

package software.amazon.awssdk.awscodegen.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.awssdk.awscodegen.CodegenOrdering;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait;
import software.amazon.smithy.model.traits.HttpBasicAuthTrait;
import software.amazon.smithy.model.traits.HttpBearerAuthTrait;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.model.traits.HttpDigestAuthTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * The traits that generated schemas carry as runtime trait objects, and the unknown traits they expose by ID.
 *
 * <p><b>Runtime-relevant traits.</b> A trait is runtime-relevant if Schema SERDE, validation, a protocol, the pipeline,
 * auth or the dynamic client reads it from a schema at run time. The set is the Smithy prelude traits with that role
 * (a superset of Smithy Java's own schema trait set, protocol-independent so that every protocol finds its traits):
 * <ul>
 *   <li>constraints and presence: {@code required}, {@code default}, {@code length}, {@code pattern}, {@code range},
 *       {@code uniqueItems}, {@code sparse}, {@code requiresLength}, {@code enumValue} and the legacy
 *       {@code enum};</li>
 *   <li>errors and retries: {@code error}, {@code httpError}, {@code retryable};</li>
 *   <li>serialization: {@code jsonName}, {@code timestampFormat}, {@code mediaType}, {@code xmlName},
 *       {@code xmlAttribute}, {@code xmlFlattened}, {@code xmlNamespace}, {@code eventHeader},
 *       {@code eventPayload}, {@code streaming};</li>
 *   <li>HTTP bindings and endpoints: {@code http}, {@code httpHeader}, {@code httpLabel}, {@code httpQuery},
 *       {@code httpQueryParams}, {@code httpPayload}, {@code httpPrefixHeaders}, {@code httpResponseCode},
 *       {@code httpChecksumRequired}, {@code endpoint}, {@code hostLabel};</li>
 *   <li>behavior: {@code idempotencyToken}, {@code idempotent}, {@code readonly}, {@code paginated},
 *       {@code requestCompression}, {@code sensitive}, {@code unitType};</li>
 *   <li>auth: {@code auth}, {@code optionalAuth}, {@code httpBasicAuth}, {@code httpBearerAuth},
 *       {@code httpDigestAuth}, {@code httpApiKeyAuth}.</li>
 * </ul>
 * Documentation, deprecation, examples, tags and other design-time traits are not runtime-relevant. Every runtime trait
 * is created in generated code by its class, from its exact Smithy node value, with no model parser: an annotation
 * trait with its no-argument constructor, any other with its own {@code TraitService} provider,
 * {@code new LengthTrait.Provider().createTrait(target, node)}, so the runtime object is the one Smithy's loader would
 * create. The classes all live in {@code smithy-model}'s {@code traits} package, which the runtime boundary allows.
 *
 * <p>Traits outside the prelude that have definitions on the generator's class path (the AWS protocol, auth, API,
 * rules-engine and waiter traits) are known traits but have no runtime implementation on the generated-client
 * class path ({@code smithy-aws-traits} and friends are build-time artifacts), so they are not in generated schemas;
 * the protocol, auth, endpoint and waiter runtimes read them through their own generated metadata.
 *
 * <p><b>Unknown traits</b> ({@link DynamicTrait}s, which have no definition on the generator's class path) are never
 * put in a Smithy Java schema, whose class-keyed trait map would keep at most one of them. They are exposed by exact
 * trait ID through the generated {@code ServiceSchemaIndex}.
 */
final class RuntimeTraits {

    /**
     * The runtime-relevant traits by trait ID, with the binary name of the class generated code creates them with.
     * (Names, not classes: the generator keeps no static state of Smithy semantic model types.)
     */
    static final SortedMap<ShapeId, String> TRAITS = table(
        // Constraints and presence.
        entry(RequiredTrait.ID, RequiredTrait.class),
        entry(DefaultTrait.ID, DefaultTrait.class),
        entry(LengthTrait.ID, LengthTrait.class),
        entry(PatternTrait.ID, PatternTrait.class),
        entry(RangeTrait.ID, RangeTrait.class),
        entry(UniqueItemsTrait.ID, UniqueItemsTrait.class),
        entry(SparseTrait.ID, SparseTrait.class),
        entry(RequiresLengthTrait.ID, RequiresLengthTrait.class),
        entry(EnumValueTrait.ID, EnumValueTrait.class),
        entry(EnumTrait.ID, EnumTrait.class),
        // Errors and retries.
        entry(ErrorTrait.ID, ErrorTrait.class),
        entry(HttpErrorTrait.ID, HttpErrorTrait.class),
        entry(RetryableTrait.ID, RetryableTrait.class),
        // Serialization.
        entry(JsonNameTrait.ID, JsonNameTrait.class),
        entry(TimestampFormatTrait.ID, TimestampFormatTrait.class),
        entry(MediaTypeTrait.ID, MediaTypeTrait.class),
        entry(XmlNameTrait.ID, XmlNameTrait.class),
        entry(XmlAttributeTrait.ID, XmlAttributeTrait.class),
        entry(XmlFlattenedTrait.ID, XmlFlattenedTrait.class),
        entry(XmlNamespaceTrait.ID, XmlNamespaceTrait.class),
        entry(EventHeaderTrait.ID, EventHeaderTrait.class),
        entry(EventPayloadTrait.ID, EventPayloadTrait.class),
        entry(StreamingTrait.ID, StreamingTrait.class),
        // HTTP bindings and endpoints.
        entry(HttpTrait.ID, HttpTrait.class),
        entry(HttpHeaderTrait.ID, HttpHeaderTrait.class),
        entry(HttpLabelTrait.ID, HttpLabelTrait.class),
        entry(HttpQueryTrait.ID, HttpQueryTrait.class),
        entry(HttpQueryParamsTrait.ID, HttpQueryParamsTrait.class),
        entry(HttpPayloadTrait.ID, HttpPayloadTrait.class),
        entry(HttpPrefixHeadersTrait.ID, HttpPrefixHeadersTrait.class),
        entry(HttpResponseCodeTrait.ID, HttpResponseCodeTrait.class),
        entry(HttpChecksumRequiredTrait.ID, HttpChecksumRequiredTrait.class),
        entry(EndpointTrait.ID, EndpointTrait.class),
        entry(HostLabelTrait.ID, HostLabelTrait.class),
        // Behavior.
        entry(IdempotencyTokenTrait.ID, IdempotencyTokenTrait.class),
        entry(IdempotentTrait.ID, IdempotentTrait.class),
        entry(ReadonlyTrait.ID, ReadonlyTrait.class),
        entry(PaginatedTrait.ID, PaginatedTrait.class),
        entry(RequestCompressionTrait.ID, RequestCompressionTrait.class),
        entry(SensitiveTrait.ID, SensitiveTrait.class),
        entry(UnitTypeTrait.ID, UnitTypeTrait.class),
        // Auth.
        entry(AuthTrait.ID, AuthTrait.class),
        entry(OptionalAuthTrait.ID, OptionalAuthTrait.class),
        entry(HttpBasicAuthTrait.ID, HttpBasicAuthTrait.class),
        entry(HttpBearerAuthTrait.ID, HttpBearerAuthTrait.class),
        entry(HttpDigestAuthTrait.ID, HttpDigestAuthTrait.class),
        entry(HttpApiKeyAuthTrait.ID, HttpApiKeyAuthTrait.class));

    private RuntimeTraits() {
    }

    /**
     * The runtime-relevant traits applied directly to {@code shape} (for a member, not those of its target), sorted by
     * trait ID.
     */
    static List<Trait> runtimeTraits(Shape shape) {
        List<Trait> result = new ArrayList<>();
        for (Trait trait : CodegenOrdering.traits(shape)) {
            String type = TRAITS.get(trait.toShapeId());
            if (type != null) {
                if (!trait.getClass().getName().equals(type)) {
                    throw new AwsCodegenException("Trait " + trait.toShapeId() + " on " + shape.getId() + " is a "
                                                  + trait.getClass().getName() + ", not a " + type);
                }
                result.add(trait);
            }
        }
        return result;
    }

    /** The unknown traits applied directly to {@code shape}, sorted by trait ID. */
    static List<DynamicTrait> unknownTraits(Shape shape) {
        List<DynamicTrait> result = new ArrayList<>();
        for (Trait trait : CodegenOrdering.traits(shape)) {
            if (trait instanceof DynamicTrait dynamic) {
                result.add(dynamic);
            }
        }
        return result;
    }

    @SafeVarargs
    private static SortedMap<ShapeId, String> table(Map.Entry<ShapeId, String>... entries) {
        Map<ShapeId, String> table = new TreeMap<>();
        for (Map.Entry<ShapeId, String> entry : entries) {
            if (table.put(entry.getKey(), entry.getValue()) != null) {
                throw new IllegalStateException("Duplicate runtime trait " + entry.getKey());
            }
        }
        return CodegenOrdering.sorted(table);
    }

    private static Map.Entry<ShapeId, String> entry(ShapeId id, Class<? extends Trait> type) {
        return Map.entry(id, type.getName());
    }
}
