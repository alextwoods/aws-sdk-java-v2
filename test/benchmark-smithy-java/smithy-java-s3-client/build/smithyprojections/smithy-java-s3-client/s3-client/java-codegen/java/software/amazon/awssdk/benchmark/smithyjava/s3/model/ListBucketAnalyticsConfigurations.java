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
 * <p>Lists the analytics configurations for the bucket. You can have up to 1,000 analytics configurations per bucket.
 *
 * <p>This action supports list pagination and does not return more than 100 configurations at a time. You should always
 * check the <code>IsTruncated</code> element in the response. If there are no more configurations to list, <code>
 * IsTruncated</code> is set to false. If there are more configurations to list, <code>IsTruncated</code> is set to
 * true, and there will be a value in <code>NextContinuationToken</code>. You use the <code>NextContinuationToken</code>
 * value to continue the pagination of the list by passing the value in continuation-token in the request to <code>GET</code>
 * the next page.
 *
 * <p>To use this operation, you must have permissions to perform the <code>s3:GetAnalyticsConfiguration</code> action.
 * The bucket owner has this permission by default. The bucket owner can grant this permission to others. For more
 * information about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-with-s3-actions.html#using-with-s3-actions-related-to-bucket-subresources">Permissions Related to Bucket Subresource Operations</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">Managing Access
 * Permissions to Your Amazon S3 Resources</a>.
 *
 * <p>For information about Amazon S3 analytics feature, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/analytics-storage-class.html">Amazon S3 Analytics – Storage Class Analysis</a>.
 *
 * <p>The following operations are related to <code>ListBucketAnalyticsConfigurations</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketAnalyticsConfiguration.html">GetBucketAnalyticsConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketAnalyticsConfiguration.html">DeleteBucketAnalyticsConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketAnalyticsConfiguration.html">PutBucketAnalyticsConfiguration</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class ListBucketAnalyticsConfigurations implements ApiOperation<ListBucketAnalyticsConfigurationsInput, ListBucketAnalyticsConfigurationsOutput> {

    private static final ListBucketAnalyticsConfigurations $INSTANCE = new ListBucketAnalyticsConfigurations();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#ListBucketAnalyticsConfigurations"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?analytics&x-id=ListBucketAnalyticsConfigurations")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListBucketAnalyticsConfigurations instance() {
        return $INSTANCE;
    }

    private ListBucketAnalyticsConfigurations() {}

    @Override
    public ShapeBuilder<ListBucketAnalyticsConfigurationsInput> inputBuilder() {
        return ListBucketAnalyticsConfigurationsInput.builder();
    }

    @Override
    public ShapeBuilder<ListBucketAnalyticsConfigurationsOutput> outputBuilder() {
        return ListBucketAnalyticsConfigurationsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListBucketAnalyticsConfigurationsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListBucketAnalyticsConfigurationsOutput.$SCHEMA;
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
