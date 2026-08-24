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
 * Gets a metrics configuration (specified by the metrics configuration ID) from the bucket. Note that this doesn't
 * include the daily storage metrics.
 *
 * <p><b>Directory buckets </b> - For directory buckets, you must make requests for this API operation to the Regional
 * endpoint. These endpoints support path-style requests in the format <code>https://s3express-control.<i>region-code</i>
 * .amazonaws.com/<i>bucket-name</i></code>. Virtual-hosted-style requests aren't supported. For more information about
 * endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">Regional and Zonal endpoints for directory buckets in Availability Zones</a>
 * in the <i>Amazon S3 User Guide</i>. For more information about endpoints in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for
 * directory buckets in Local Zones</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <p> To use this operation, you must have permissions to perform the <code>s3:GetMetricsConfiguration</code>
 *     action. The bucket owner has this permission by default. The bucket owner can grant this permission to
 *     others. For more information about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-with-s3-actions.html#using-with-s3-actions-related-to-bucket-subresources">Permissions Related to Bucket Subresource Operations</a>
 *     and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">Managing Access Permissions to Your Amazon S3 Resources</a>.
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - The <code>s3:GetMetricsConfiguration</code> permission is
 *         required in a policy. For more information about general purpose buckets permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/using-iam-policies.html">Using
 *         Bucket Policies and User Policies</a> in the <i>Amazon S3 User Guide</i>.
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - To grant access to this API operation, you must have the <code>
 *         s3express:GetMetricsConfiguration</code> permission in an IAM identity-based policy instead of a
 *         bucket policy. Cross-account access to this API operation isn't supported. This operation can only be
 *         performed by the Amazon Web Services account that owns the resource. For more information about
 *         directory bucket policies and permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam.html">Amazon Web Services Identity and Access Management
 *         (IAM) for S3 Express One Zone</a> in the <i>Amazon S3 User Guide</i>.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     HTTP Host header syntax
 *   </dt>
 *   <dd>
 *
 *     <p><b>Directory buckets </b> - The HTTP Host header syntax is <code>s3express-control.<i>region-code</i>
 *     .amazonaws.com</code>.
 *   </dd>
 * </dl>
 *
 * <p> For information about CloudWatch request metrics for Amazon S3, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/cloudwatch-monitoring.html">Monitoring Metrics with Amazon CloudWatch</a>
 * .
 *
 * <p>The following operations are related to <code>GetBucketMetricsConfiguration</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketMetricsConfiguration.html">PutBucketMetricsConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketMetricsConfiguration.html">DeleteBucketMetricsConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBucketMetricsConfigurations.html">ListBucketMetricsConfigurations</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/cloudwatch-monitoring.html">Monitoring Metrics with Amazon CloudWatch</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class GetBucketMetricsConfiguration implements ApiOperation<GetBucketMetricsConfigurationInput, GetBucketMetricsConfigurationOutput> {

    private static final GetBucketMetricsConfiguration $INSTANCE = new GetBucketMetricsConfiguration();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketMetricsConfiguration"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?metrics&x-id=GetBucketMetricsConfiguration")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketMetricsConfiguration instance() {
        return $INSTANCE;
    }

    private GetBucketMetricsConfiguration() {}

    @Override
    public ShapeBuilder<GetBucketMetricsConfigurationInput> inputBuilder() {
        return GetBucketMetricsConfigurationInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketMetricsConfigurationOutput> outputBuilder() {
        return GetBucketMetricsConfigurationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketMetricsConfigurationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketMetricsConfigurationOutput.$SCHEMA;
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
