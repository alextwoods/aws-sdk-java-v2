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
 * <p>Gets the S3 Intelligent-Tiering configuration from the specified bucket.
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
 * <p>Operations related to <code>GetBucketIntelligentTieringConfiguration</code> include:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketIntelligentTieringConfiguration.html">DeleteBucketIntelligentTieringConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketIntelligentTieringConfiguration.html">PutBucketIntelligentTieringConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBucketIntelligentTieringConfigurations.html">ListBucketIntelligentTieringConfigurations</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class GetBucketIntelligentTieringConfiguration implements ApiOperation<GetBucketIntelligentTieringConfigurationInput, GetBucketIntelligentTieringConfigurationOutput> {

    private static final GetBucketIntelligentTieringConfiguration $INSTANCE = new GetBucketIntelligentTieringConfiguration();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketIntelligentTieringConfiguration"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?intelligent-tiering&x-id=GetBucketIntelligentTieringConfiguration")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketIntelligentTieringConfiguration instance() {
        return $INSTANCE;
    }

    private GetBucketIntelligentTieringConfiguration() {}

    @Override
    public ShapeBuilder<GetBucketIntelligentTieringConfigurationInput> inputBuilder() {
        return GetBucketIntelligentTieringConfigurationInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketIntelligentTieringConfigurationOutput> outputBuilder() {
        return GetBucketIntelligentTieringConfigurationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketIntelligentTieringConfigurationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketIntelligentTieringConfigurationOutput.$SCHEMA;
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
