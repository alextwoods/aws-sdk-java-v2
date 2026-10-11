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

package software.amazon.awssdk.schema;

import java.util.Map;
import java.util.WeakHashMap;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.traits.AuthTrait;
import software.amazon.awssdk.schema.traits.AwsJson1_0Trait;
import software.amazon.awssdk.schema.traits.AwsJson1_1Trait;
import software.amazon.awssdk.schema.traits.AwsQueryCompatibleTrait;
import software.amazon.awssdk.schema.traits.AwsQueryErrorTrait;
import software.amazon.awssdk.schema.traits.AwsQueryTrait;
import software.amazon.awssdk.schema.traits.DefaultTrait;
import software.amazon.awssdk.schema.traits.Ec2QueryNameTrait;
import software.amazon.awssdk.schema.traits.Ec2QueryTrait;
import software.amazon.awssdk.schema.traits.EndpointTrait;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.EventHeaderTrait;
import software.amazon.awssdk.schema.traits.EventPayloadTrait;
import software.amazon.awssdk.schema.traits.GenericTrait;
import software.amazon.awssdk.schema.traits.HostLabelTrait;
import software.amazon.awssdk.schema.traits.HttpChecksumRequiredTrait;
import software.amazon.awssdk.schema.traits.HttpChecksumTrait;
import software.amazon.awssdk.schema.traits.HttpErrorTrait;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.HttpLabelTrait;
import software.amazon.awssdk.schema.traits.HttpPayloadTrait;
import software.amazon.awssdk.schema.traits.HttpPrefixHeadersTrait;
import software.amazon.awssdk.schema.traits.HttpQueryParamsTrait;
import software.amazon.awssdk.schema.traits.HttpQueryTrait;
import software.amazon.awssdk.schema.traits.HttpResponseCodeTrait;
import software.amazon.awssdk.schema.traits.HttpTrait;
import software.amazon.awssdk.schema.traits.IdempotencyTokenTrait;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.MediaTypeTrait;
import software.amazon.awssdk.schema.traits.OptionalAuthTrait;
import software.amazon.awssdk.schema.traits.PaginatedTrait;
import software.amazon.awssdk.schema.traits.RequestCompressionTrait;
import software.amazon.awssdk.schema.traits.RequiredTrait;
import software.amazon.awssdk.schema.traits.RequiresLengthTrait;
import software.amazon.awssdk.schema.traits.RestJson1Trait;
import software.amazon.awssdk.schema.traits.RestXmlTrait;
import software.amazon.awssdk.schema.traits.RetryableTrait;
import software.amazon.awssdk.schema.traits.Rpcv2CborTrait;
import software.amazon.awssdk.schema.traits.SensitiveTrait;
import software.amazon.awssdk.schema.traits.SigV4Trait;
import software.amazon.awssdk.schema.traits.SparseTrait;
import software.amazon.awssdk.schema.traits.StreamingTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.schema.traits.UnitTypeTrait;
import software.amazon.awssdk.schema.traits.UnsignedPayloadTrait;
import software.amazon.awssdk.schema.traits.XmlAttributeTrait;
import software.amazon.awssdk.schema.traits.XmlFlattenedTrait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;
import software.amazon.awssdk.utils.Validate;

/**
 * Identity-based access to one typed trait class, used to look the trait up on a schema.
 *
 * <p>There is exactly one key per trait class: {@link #get(Class)} returns the same instance for the same class on
 * every call, from any thread. Each key holds a dense integer index, assigned from 0 in key creation order, that
 * indexes the schema's trait storage, so a lookup is an array load with no hashing and no allocation.
 *
 * <p>{@link GenericTrait}s have no key, because every generic trait shares one Java class. They are looked up by their
 * trait shape ID instead.
 *
 * <p>The constants below cover every typed trait in {@link software.amazon.awssdk.schema.traits}. They are declared
 * from the most frequently present traits (member traits) to the least (service traits), so that the trait storage of a
 * typical schema, sized by the largest key index it holds, stays small. Keys for other trait classes, such as
 * third-party traits, can be obtained at any time through {@link #get(Class)}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.TraitKey}.
 *
 * @param <T> the trait class the key looks up
 */
@SdkPublicApi
public final class TraitKey<T extends Trait> {
    // Member and shape traits.

    /** The key for {@link RequiredTrait}. */
    public static final TraitKey<RequiredTrait> REQUIRED_TRAIT = get(RequiredTrait.class);
    /** The key for {@link DefaultTrait}. */
    public static final TraitKey<DefaultTrait> DEFAULT_TRAIT = get(DefaultTrait.class);
    /** The key for {@link SensitiveTrait}. */
    public static final TraitKey<SensitiveTrait> SENSITIVE_TRAIT = get(SensitiveTrait.class);
    /** The key for {@link SparseTrait}. */
    public static final TraitKey<SparseTrait> SPARSE_TRAIT = get(SparseTrait.class);
    /** The key for {@link JsonNameTrait}. */
    public static final TraitKey<JsonNameTrait> JSON_NAME_TRAIT = get(JsonNameTrait.class);
    /** The key for {@link XmlNameTrait}. */
    public static final TraitKey<XmlNameTrait> XML_NAME_TRAIT = get(XmlNameTrait.class);
    /** The key for {@link XmlAttributeTrait}. */
    public static final TraitKey<XmlAttributeTrait> XML_ATTRIBUTE_TRAIT = get(XmlAttributeTrait.class);
    /** The key for {@link XmlFlattenedTrait}. */
    public static final TraitKey<XmlFlattenedTrait> XML_FLATTENED_TRAIT = get(XmlFlattenedTrait.class);
    /** The key for {@link XmlNamespaceTrait}. */
    public static final TraitKey<XmlNamespaceTrait> XML_NAMESPACE_TRAIT = get(XmlNamespaceTrait.class);
    /** The key for {@link TimestampFormatTrait}. */
    public static final TraitKey<TimestampFormatTrait> TIMESTAMP_FORMAT_TRAIT = get(TimestampFormatTrait.class);
    /** The key for {@link MediaTypeTrait}. */
    public static final TraitKey<MediaTypeTrait> MEDIA_TYPE_TRAIT = get(MediaTypeTrait.class);
    /** The key for {@link IdempotencyTokenTrait}. */
    public static final TraitKey<IdempotencyTokenTrait> IDEMPOTENCY_TOKEN_TRAIT = get(IdempotencyTokenTrait.class);
    /** The key for {@link HttpHeaderTrait}. */
    public static final TraitKey<HttpHeaderTrait> HTTP_HEADER_TRAIT = get(HttpHeaderTrait.class);
    /** The key for {@link HttpQueryTrait}. */
    public static final TraitKey<HttpQueryTrait> HTTP_QUERY_TRAIT = get(HttpQueryTrait.class);
    /** The key for {@link HttpLabelTrait}. */
    public static final TraitKey<HttpLabelTrait> HTTP_LABEL_TRAIT = get(HttpLabelTrait.class);
    /** The key for {@link HttpPayloadTrait}. */
    public static final TraitKey<HttpPayloadTrait> HTTP_PAYLOAD_TRAIT = get(HttpPayloadTrait.class);
    /** The key for {@link HttpPrefixHeadersTrait}. */
    public static final TraitKey<HttpPrefixHeadersTrait> HTTP_PREFIX_HEADERS_TRAIT = get(HttpPrefixHeadersTrait.class);
    /** The key for {@link HttpQueryParamsTrait}. */
    public static final TraitKey<HttpQueryParamsTrait> HTTP_QUERY_PARAMS_TRAIT = get(HttpQueryParamsTrait.class);
    /** The key for {@link HttpResponseCodeTrait}. */
    public static final TraitKey<HttpResponseCodeTrait> HTTP_RESPONSE_CODE_TRAIT = get(HttpResponseCodeTrait.class);
    /** The key for {@link HostLabelTrait}. */
    public static final TraitKey<HostLabelTrait> HOST_LABEL_TRAIT = get(HostLabelTrait.class);
    /** The key for {@link Ec2QueryNameTrait}. */
    public static final TraitKey<Ec2QueryNameTrait> EC2_QUERY_NAME_TRAIT = get(Ec2QueryNameTrait.class);
    /** The key for {@link StreamingTrait}. */
    public static final TraitKey<StreamingTrait> STREAMING_TRAIT = get(StreamingTrait.class);
    /** The key for {@link RequiresLengthTrait}. */
    public static final TraitKey<RequiresLengthTrait> REQUIRES_LENGTH_TRAIT = get(RequiresLengthTrait.class);
    /** The key for {@link EventHeaderTrait}. */
    public static final TraitKey<EventHeaderTrait> EVENT_HEADER_TRAIT = get(EventHeaderTrait.class);
    /** The key for {@link EventPayloadTrait}. */
    public static final TraitKey<EventPayloadTrait> EVENT_PAYLOAD_TRAIT = get(EventPayloadTrait.class);
    /** The key for {@link UnitTypeTrait}. */
    public static final TraitKey<UnitTypeTrait> UNIT_TYPE_TRAIT = get(UnitTypeTrait.class);

    // Error shape traits.

    /** The key for {@link ErrorTrait}. */
    public static final TraitKey<ErrorTrait> ERROR_TRAIT = get(ErrorTrait.class);
    /** The key for {@link HttpErrorTrait}. */
    public static final TraitKey<HttpErrorTrait> HTTP_ERROR_TRAIT = get(HttpErrorTrait.class);
    /** The key for {@link RetryableTrait}. */
    public static final TraitKey<RetryableTrait> RETRYABLE_TRAIT = get(RetryableTrait.class);
    /** The key for {@link AwsQueryErrorTrait}. */
    public static final TraitKey<AwsQueryErrorTrait> AWS_QUERY_ERROR_TRAIT = get(AwsQueryErrorTrait.class);

    // Operation traits.

    /** The key for {@link HttpTrait}. */
    public static final TraitKey<HttpTrait> HTTP_TRAIT = get(HttpTrait.class);
    /** The key for {@link EndpointTrait}. */
    public static final TraitKey<EndpointTrait> ENDPOINT_TRAIT = get(EndpointTrait.class);
    /** The key for {@link HttpChecksumTrait}. */
    public static final TraitKey<HttpChecksumTrait> HTTP_CHECKSUM_TRAIT = get(HttpChecksumTrait.class);
    /** The key for {@link HttpChecksumRequiredTrait}. */
    public static final TraitKey<HttpChecksumRequiredTrait> HTTP_CHECKSUM_REQUIRED_TRAIT =
        get(HttpChecksumRequiredTrait.class);
    /** The key for {@link RequestCompressionTrait}. */
    public static final TraitKey<RequestCompressionTrait> REQUEST_COMPRESSION_TRAIT = get(RequestCompressionTrait.class);
    /** The key for {@link PaginatedTrait}. */
    public static final TraitKey<PaginatedTrait> PAGINATED_TRAIT = get(PaginatedTrait.class);
    /** The key for {@link AuthTrait}. */
    public static final TraitKey<AuthTrait> AUTH_TRAIT = get(AuthTrait.class);
    /** The key for {@link OptionalAuthTrait}. */
    public static final TraitKey<OptionalAuthTrait> OPTIONAL_AUTH_TRAIT = get(OptionalAuthTrait.class);
    /** The key for {@link UnsignedPayloadTrait}. */
    public static final TraitKey<UnsignedPayloadTrait> UNSIGNED_PAYLOAD_TRAIT = get(UnsignedPayloadTrait.class);

    // Service traits.

    /** The key for {@link AwsQueryCompatibleTrait}. */
    public static final TraitKey<AwsQueryCompatibleTrait> AWS_QUERY_COMPATIBLE_TRAIT = get(AwsQueryCompatibleTrait.class);
    /** The key for {@link SigV4Trait}. */
    public static final TraitKey<SigV4Trait> SIGV4_TRAIT = get(SigV4Trait.class);
    /** The key for {@link AwsJson1_0Trait}. */
    public static final TraitKey<AwsJson1_0Trait> AWS_JSON_1_0_TRAIT = get(AwsJson1_0Trait.class);
    /** The key for {@link AwsJson1_1Trait}. */
    public static final TraitKey<AwsJson1_1Trait> AWS_JSON_1_1_TRAIT = get(AwsJson1_1Trait.class);
    /** The key for {@link RestJson1Trait}. */
    public static final TraitKey<RestJson1Trait> REST_JSON_1_TRAIT = get(RestJson1Trait.class);
    /** The key for {@link RestXmlTrait}. */
    public static final TraitKey<RestXmlTrait> REST_XML_TRAIT = get(RestXmlTrait.class);
    /** The key for {@link AwsQueryTrait}. */
    public static final TraitKey<AwsQueryTrait> AWS_QUERY_TRAIT = get(AwsQueryTrait.class);
    /** The key for {@link Ec2QueryTrait}. */
    public static final TraitKey<Ec2QueryTrait> EC2_QUERY_TRAIT = get(Ec2QueryTrait.class);
    /** The key for {@link Rpcv2CborTrait}. */
    public static final TraitKey<Rpcv2CborTrait> RPCV2_CBOR_TRAIT = get(Rpcv2CborTrait.class);

    /**
     * The key's index into trait storage: unique among all keys and contiguous from 0 in key creation order.
     */
    final int id;

    private final Class<T> traitClass;

    private TraitKey(Class<T> traitClass, int id) {
        this.traitClass = traitClass;
        this.id = id;
    }

    /**
     * Returns the key for a trait class. Every call for the same class returns the same instance, including concurrent
     * calls.
     *
     * <p>Lookups match a trait by its exact runtime class, so the class should be the concrete trait class (for
     * example {@link JsonNameTrait}, not its base class).
     *
     * @param traitClass the trait class
     * @param <T> the trait class
     * @return the key for the trait class
     * @throws NullPointerException if {@code traitClass} is null
     * @throws IllegalArgumentException if {@code traitClass} is {@link GenericTrait}, which is looked up by trait shape
     *                                  ID instead
     */
    @SuppressWarnings("unchecked")
    public static <T extends Trait> TraitKey<T> get(Class<T> traitClass) {
        Validate.paramNotNull(traitClass, "traitClass");
        if (traitClass == GenericTrait.class) {
            throw new IllegalArgumentException("GenericTrait has no TraitKey: generic traits share one class, so they are "
                                               + "looked up by their trait shape ID");
        }
        return (TraitKey<T>) Pool.KEY_POOL.get(traitClass);
    }

    /**
     * Returns the trait class this key looks up.
     *
     * @return the trait class
     */
    public Class<T> traitClass() {
        return traitClass;
    }

    @Override
    public String toString() {
        return "TraitKey(" + traitClass.getName() + ", id=" + id + ")";
    }

    /**
     * The key pool and index counter. They live in a holder class so that they are initialized before the first
     * constant above is created, whatever the textual order of the static fields.
     */
    private static final class Pool {
        private static final Object ID_LOCK = new Object();

        /**
         * The index assigned to each trait class. {@link ClassValue} may compute a value more than once when threads
         * race on the first lookup of a class, and keeps only one of the results. Assigning the index here, once per
         * class, keeps the indexes dense even when that happens. The map holds only the index, so it does not keep a
         * trait class (or its class loader) reachable.
         */
        private static final Map<Class<?>, Integer> IDS = new WeakHashMap<>();

        private static final ClassValue<TraitKey<?>> KEY_POOL = new ClassValue<TraitKey<?>>() {
            @Override
            @SuppressWarnings("unchecked")
            protected TraitKey<?> computeValue(Class<?> type) {
                return new TraitKey<>((Class<Trait>) type, idFor(type));
            }
        };

        private static int nextId;

        private Pool() {
        }

        private static int idFor(Class<?> type) {
            synchronized (ID_LOCK) {
                Integer id = IDS.get(type);
                if (id == null) {
                    id = nextId++;
                    IDS.put(type, id);
                }
                return id;
            }
        }
    }
}
