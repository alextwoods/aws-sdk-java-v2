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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;

/**
 * The factories for the SDK's typed traits. They are registered directly rather than through
 * {@link java.util.ServiceLoader}, so they are always present and cost no service lookup.
 */
@SdkInternalApi
final class BuiltInTraitFactories {

    private BuiltInTraitFactories() {
    }

    static List<TraitFactory<?>> all() {
        List<TraitFactory<?>> factories = new ArrayList<>();

        // Prelude annotation traits.
        factories.add(annotation(RequiredTrait.ID, RequiredTrait.class, RequiredTrait.INSTANCE));
        factories.add(annotation(SensitiveTrait.ID, SensitiveTrait.class, SensitiveTrait.INSTANCE));
        factories.add(annotation(SparseTrait.ID, SparseTrait.class, SparseTrait.INSTANCE));
        factories.add(annotation(IdempotencyTokenTrait.ID, IdempotencyTokenTrait.class, IdempotencyTokenTrait.INSTANCE));
        factories.add(annotation(StreamingTrait.ID, StreamingTrait.class, StreamingTrait.INSTANCE));
        factories.add(annotation(RequiresLengthTrait.ID, RequiresLengthTrait.class, RequiresLengthTrait.INSTANCE));
        factories.add(annotation(EventHeaderTrait.ID, EventHeaderTrait.class, EventHeaderTrait.INSTANCE));
        factories.add(annotation(EventPayloadTrait.ID, EventPayloadTrait.class, EventPayloadTrait.INSTANCE));
        factories.add(annotation(HostLabelTrait.ID, HostLabelTrait.class, HostLabelTrait.INSTANCE));
        factories.add(annotation(HttpChecksumRequiredTrait.ID, HttpChecksumRequiredTrait.class,
                                 HttpChecksumRequiredTrait.INSTANCE));
        factories.add(annotation(XmlAttributeTrait.ID, XmlAttributeTrait.class, XmlAttributeTrait.INSTANCE));
        factories.add(annotation(XmlFlattenedTrait.ID, XmlFlattenedTrait.class, XmlFlattenedTrait.INSTANCE));
        factories.add(annotation(HttpLabelTrait.ID, HttpLabelTrait.class, HttpLabelTrait.INSTANCE));
        factories.add(annotation(HttpQueryParamsTrait.ID, HttpQueryParamsTrait.class, HttpQueryParamsTrait.INSTANCE));
        factories.add(annotation(HttpPayloadTrait.ID, HttpPayloadTrait.class, HttpPayloadTrait.INSTANCE));
        factories.add(annotation(HttpResponseCodeTrait.ID, HttpResponseCodeTrait.class, HttpResponseCodeTrait.INSTANCE));
        factories.add(annotation(UnitTypeTrait.ID, UnitTypeTrait.class, UnitTypeTrait.INSTANCE));
        factories.add(annotation(OptionalAuthTrait.ID, OptionalAuthTrait.class, OptionalAuthTrait.INSTANCE));

        // Prelude string traits.
        factories.add(string(MediaTypeTrait.ID, MediaTypeTrait.class, MediaTypeTrait::new));
        factories.add(string(XmlNameTrait.ID, XmlNameTrait.class, XmlNameTrait::new));
        factories.add(string(JsonNameTrait.ID, JsonNameTrait.class, JsonNameTrait::new));
        factories.add(string(TimestampFormatTrait.ID, TimestampFormatTrait.class, TimestampFormatTrait::new));
        factories.add(string(HttpHeaderTrait.ID, HttpHeaderTrait.class, HttpHeaderTrait::new));
        factories.add(string(HttpQueryTrait.ID, HttpQueryTrait.class, HttpQueryTrait::new));
        factories.add(string(HttpPrefixHeadersTrait.ID, HttpPrefixHeadersTrait.class, HttpPrefixHeadersTrait::new));

        // Prelude traits with structured values.
        factories.add(factory(ErrorTrait.ID, ErrorTrait.class, ErrorTrait::fromDocument));
        factories.add(factory(DefaultTrait.ID, DefaultTrait.class, DefaultTrait::fromDocument));
        factories.add(factory(HttpErrorTrait.ID, HttpErrorTrait.class, HttpErrorTrait::fromDocument));
        factories.add(factory(RetryableTrait.ID, RetryableTrait.class, RetryableTrait::fromDocument));
        factories.add(factory(EndpointTrait.ID, EndpointTrait.class, EndpointTrait::fromDocument));
        factories.add(factory(HttpTrait.ID, HttpTrait.class, HttpTrait::fromDocument));
        factories.add(factory(RequestCompressionTrait.ID, RequestCompressionTrait.class,
                              RequestCompressionTrait::fromDocument));
        factories.add(factory(PaginatedTrait.ID, PaginatedTrait.class, PaginatedTrait::fromDocument));
        factories.add(factory(XmlNamespaceTrait.ID, XmlNamespaceTrait.class, XmlNamespaceTrait::fromDocument));
        factories.add(factory(AuthTrait.ID, AuthTrait.class, AuthTrait::fromDocument));

        // AWS traits.
        factories.add(factory(HttpChecksumTrait.ID, HttpChecksumTrait.class, HttpChecksumTrait::fromDocument));
        factories.add(factory(AwsQueryErrorTrait.ID, AwsQueryErrorTrait.class, AwsQueryErrorTrait::fromDocument));
        factories.add(annotation(AwsQueryCompatibleTrait.ID, AwsQueryCompatibleTrait.class,
                                 AwsQueryCompatibleTrait.INSTANCE));
        factories.add(string(Ec2QueryNameTrait.ID, Ec2QueryNameTrait.class, Ec2QueryNameTrait::new));

        // Protocol definition traits.
        factories.add(factory(AwsJson1_0Trait.ID, AwsJson1_0Trait.class, AwsJson1_0Trait::fromDocument));
        factories.add(factory(AwsJson1_1Trait.ID, AwsJson1_1Trait.class, AwsJson1_1Trait::fromDocument));
        factories.add(factory(RestJson1Trait.ID, RestJson1Trait.class, RestJson1Trait::fromDocument));
        factories.add(factory(RestXmlTrait.ID, RestXmlTrait.class, RestXmlTrait::fromDocument));
        factories.add(annotation(AwsQueryTrait.ID, AwsQueryTrait.class, AwsQueryTrait.INSTANCE));
        factories.add(annotation(Ec2QueryTrait.ID, Ec2QueryTrait.class, Ec2QueryTrait.INSTANCE));
        factories.add(factory(Rpcv2CborTrait.ID, Rpcv2CborTrait.class, Rpcv2CborTrait::fromDocument));

        // Auth traits.
        factories.add(factory(SigV4Trait.ID, SigV4Trait.class, SigV4Trait::fromDocument));
        factories.add(annotation(UnsignedPayloadTrait.ID, UnsignedPayloadTrait.class, UnsignedPayloadTrait.INSTANCE));

        return Collections.unmodifiableList(factories);
    }

    private static <T extends Trait> TraitFactory<T> factory(ShapeId id, Class<T> traitClass,
                                                             Function<Document, T> create) {
        return new SimpleTraitFactory<>(id, traitClass, create);
    }

    private static <T extends AnnotationTrait> TraitFactory<T> annotation(ShapeId id, Class<T> traitClass, T instance) {
        return new SimpleTraitFactory<>(id, traitClass, value -> {
            TraitValues.expectObject(id, value);
            return instance;
        });
    }

    private static <T extends StringTrait> TraitFactory<T> string(ShapeId id, Class<T> traitClass,
                                                                  Function<String, T> create) {
        return new SimpleTraitFactory<>(id, traitClass, value -> create.apply(TraitValues.expectString(id, value)));
    }

    private static final class SimpleTraitFactory<T extends Trait> implements TraitFactory<T> {
        private final ShapeId traitId;
        private final Class<T> traitClass;
        private final Function<Document, T> create;

        SimpleTraitFactory(ShapeId traitId, Class<T> traitClass, Function<Document, T> create) {
            this.traitId = traitId;
            this.traitClass = traitClass;
            this.create = create;
        }

        @Override
        public ShapeId traitId() {
            return traitId;
        }

        @Override
        public Class<T> traitClass() {
            return traitClass;
        }

        @Override
        public T create(Document value) {
            return create.apply(value);
        }

        @Override
        public String toString() {
            return ToString.builder("BuiltInTraitFactory").add("traitId", traitId).build();
        }
    }
}
