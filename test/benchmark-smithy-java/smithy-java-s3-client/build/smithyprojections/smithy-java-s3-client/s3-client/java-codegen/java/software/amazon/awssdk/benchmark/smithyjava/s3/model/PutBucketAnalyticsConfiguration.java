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
 * <p>Sets an analytics configuration for the bucket (specified by the analytics configuration ID). You can have up to
 * 1,000 analytics configurations per bucket.
 *
 * <p>You can choose to have storage class analysis export analysis reports sent to a comma-separated values (CSV) flat
 * file. See the <code>DataExport</code> request element. Reports are updated daily and are based on the object filters
 * that you configure. When selecting data export, you specify a destination bucket and an optional destination prefix
 * where the file is written. You can export the data to a destination bucket in a different account. However, the
 * destination bucket must be in the same Region as the bucket that you are making the PUT analytics configuration to.
 * For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/analytics-storage-class.html">Amazon S3 Analytics – Storage Class Analysis</a>.
 *
 * <p>You must create a bucket policy on the destination bucket where the exported file is written to grant permissions
 * to Amazon S3 to write objects to the bucket. For an example policy, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/example-bucket-policies.html#example-bucket-policies-use-case-9">Granting Permissions for Amazon S3
 * Inventory and Storage Class Analysis</a>.
 *
 * <p>To use this operation, you must have permissions to perform the <code>s3:PutAnalyticsConfiguration</code> action.
 * The bucket owner has this permission by default. The bucket owner can grant this permission to others. For more
 * information about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-with-s3-actions.html#using-with-s3-actions-related-to-bucket-subresources">Permissions Related to Bucket Subresource Operations</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">Managing Access
 * Permissions to Your Amazon S3 Resources</a>.
 *
 * <p><code>PutBucketAnalyticsConfiguration</code> has the following special errors:
 *
 * <ul>
 *   <li>
 *
 *     <ul>
 *       <li>
 *         <i>HTTP Error: HTTP 400 Bad Request</i>
 *       </li>
 *       <li>
 *         <i>Code: InvalidArgument</i>
 *       </li>
 *       <li>
 *         <i>Cause: Invalid argument.</i>
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *
 *     <ul>
 *       <li>
 *         <i>HTTP Error: HTTP 400 Bad Request</i>
 *       </li>
 *       <li>
 *         <i>Code: TooManyConfigurations</i>
 *       </li>
 *       <li>
 *         <i>Cause: You are attempting to create a new configuration but have already reached the
 *         1,000-configuration limit.</i>
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *
 *     <ul>
 *       <li>
 *         <i>HTTP Error: HTTP 403 Forbidden</i>
 *       </li>
 *       <li>
 *         <i>Code: AccessDenied</i>
 *       </li>
 *       <li>
 *         <i>Cause: You are not the owner of the specified bucket, or you do not have the
 *         s3:PutAnalyticsConfiguration bucket permission to set the configuration on the bucket.</i>
 *       </li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>The following operations are related to <code>PutBucketAnalyticsConfiguration</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketAnalyticsConfiguration.html">GetBucketAnalyticsConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketAnalyticsConfiguration.html">DeleteBucketAnalyticsConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBucketAnalyticsConfigurations.html">ListBucketAnalyticsConfigurations</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class PutBucketAnalyticsConfiguration implements ApiOperation<PutBucketAnalyticsConfigurationInput, PutBucketAnalyticsConfigurationOutput> {

    private static final PutBucketAnalyticsConfiguration $INSTANCE = new PutBucketAnalyticsConfiguration();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketAnalyticsConfiguration"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?analytics")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketAnalyticsConfiguration instance() {
        return $INSTANCE;
    }

    private PutBucketAnalyticsConfiguration() {}

    @Override
    public ShapeBuilder<PutBucketAnalyticsConfigurationInput> inputBuilder() {
        return PutBucketAnalyticsConfigurationInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketAnalyticsConfigurationOutput> outputBuilder() {
        return PutBucketAnalyticsConfigurationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketAnalyticsConfigurationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketAnalyticsConfigurationOutput.$SCHEMA;
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
