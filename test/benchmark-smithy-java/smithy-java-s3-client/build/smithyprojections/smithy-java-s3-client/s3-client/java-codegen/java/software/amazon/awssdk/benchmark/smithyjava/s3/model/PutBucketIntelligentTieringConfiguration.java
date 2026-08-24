package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation is not supported for directory buckets.
 *
 * <p>Puts a S3 Intelligent-Tiering configuration to the specified bucket. You can have up to 1,000 S3
 * Intelligent-Tiering configurations per bucket.
 *
 * <p>The S3 Intelligent-Tiering storage class is designed to optimize storage costs by automatically moving data to the
 * most cost-effective storage access tier, without performance impact or operational overhead. S3 Intelligent-Tiering
 * delivers automatic cost savings in three low latency and high throughput access tiers. To get the lowest storage cost
 * on data that can be accessed in minutes to hours, you can choose to activate additional archiving capabilities.
 *
 * <p>The S3 Intelligent-Tiering storage class is the ideal storage class for data with unknown, changing, or
 * unpredictable access patterns, independent of object size or retention period. If the size of an object is less than
 * 128 KB, it is not monitored and not eligible for auto-tiering. Smaller objects can be stored, but they are always
 * charged at the Frequent Access tier rates in the S3 Intelligent-Tiering storage class.
 *
 * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html#sc-dynamic-data-access">Storage class for automatically optimizing frequently and infrequently accessed
 * objects</a>.
 *
 * <p>Operations related to <code>PutBucketIntelligentTieringConfiguration</code> include:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketIntelligentTieringConfiguration.html">DeleteBucketIntelligentTieringConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketIntelligentTieringConfiguration.html">GetBucketIntelligentTieringConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBucketIntelligentTieringConfigurations.html">ListBucketIntelligentTieringConfigurations</a>
 *   </li>
 * </ul>
 *
 * <p>You only need S3 Intelligent-Tiering enabled on a bucket if you want to automatically move objects stored in the
 * S3 Intelligent-Tiering storage class to the Archive Access or Deep Archive Access tier.
 *
 * <p><code>PutBucketIntelligentTieringConfiguration</code> has the following special errors:
 *
 * <dl>
 *   <dt>
 *     HTTP 400 Bad Request Error
 *   </dt>
 *   <dd>
 *
 *     <p><i>Code:</i> InvalidArgument
 *
 *     <p><i>Cause:</i> Invalid Argument
 *   </dd>
 *   <dt>
 *     HTTP 400 Bad Request Error
 *   </dt>
 *   <dd>
 *
 *     <p><i>Code:</i> TooManyConfigurations
 *
 *     <p><i>Cause:</i> You are attempting to create a new configuration but have already reached the
 *     1,000-configuration limit.
 *   </dd>
 *   <dt>
 *     HTTP 403 Forbidden Error
 *   </dt>
 *   <dd>
 *
 *     <p><i>Cause:</i> You are not the owner of the specified bucket, or you do not have the <code>
 *     s3:PutIntelligentTieringConfiguration</code> bucket permission to set the configuration on the bucket.
 *   </dd>
 * </dl>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class PutBucketIntelligentTieringConfiguration implements ApiOperation<PutBucketIntelligentTieringConfigurationInput, PutBucketIntelligentTieringConfigurationOutput> {

    private static final PutBucketIntelligentTieringConfiguration $INSTANCE = new PutBucketIntelligentTieringConfiguration();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketIntelligentTieringConfiguration"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?intelligent-tiering")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketIntelligentTieringConfiguration instance() {
        return $INSTANCE;
    }

    private PutBucketIntelligentTieringConfiguration() {}

    @Override
    public ShapeBuilder<PutBucketIntelligentTieringConfigurationInput> inputBuilder() {
        return PutBucketIntelligentTieringConfigurationInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketIntelligentTieringConfigurationOutput> outputBuilder() {
        return PutBucketIntelligentTieringConfigurationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketIntelligentTieringConfigurationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketIntelligentTieringConfigurationOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of();
    }

    @Override
    public List<ShapeId> effectiveAuthSchemes() {
        return SCHEMES;
    }

    @Override
    public Schema inputStreamMember() {
        return null;
    }

    @Override
    public Schema outputStreamMember() {
        return null;
    }

    @Override
    public Schema idempotencyTokenMember() {
        return null;
    }

    @Override
    public ApiService service() {
        return S3ApiService.instance();
    }
    }
