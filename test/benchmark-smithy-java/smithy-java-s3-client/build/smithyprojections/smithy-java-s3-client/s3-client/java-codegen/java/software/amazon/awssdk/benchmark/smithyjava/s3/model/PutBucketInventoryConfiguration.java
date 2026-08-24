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
 * This implementation of the <code>PUT</code> action adds an S3 Inventory configuration (identified by the inventory
 * ID) to the bucket. You can have up to 1,000 inventory configurations per bucket.
 *
 * <p>Amazon S3 inventory generates inventories of the objects in the bucket on a daily or weekly basis, and the results
 * are published to a flat file. The bucket that is inventoried is called the <i>source</i> bucket, and the bucket where
 * the inventory flat file is stored is called the <i>destination</i> bucket. The <i>destination</i> bucket must be in
 * the same Amazon Web Services Region as the <i>source</i> bucket.
 *
 * <p>When you configure an inventory for a <i>source</i> bucket, you specify the <i>destination</i> bucket where you
 * want the inventory to be stored, and whether to generate the inventory daily or weekly. You can also configure what
 * object metadata to include and whether to inventory all object versions or only current versions. For more
 * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-inventory.html">Amazon S3 Inventory</a> in the Amazon S3 User Guide.
 *
 * <p>You must create a bucket policy on the <i>destination</i> bucket to grant permissions to Amazon S3 to write
 * objects to the bucket in the defined location. For an example policy, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/example-bucket-policies.html#example-bucket-policies-use-case-9"> Granting Permissions for Amazon S3
 * Inventory and Storage Class Analysis</a>.
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
 *     <p>To use this operation, you must have permission to perform the <code>s3:PutInventoryConfiguration</code>
 *     action. The bucket owner has this permission by default and can grant this permission to others.
 *
 *     <p>The <code>s3:PutInventoryConfiguration</code> permission allows a user to create an <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/storage-inventory.html">S3 Inventory</a>
 *     report that includes all object metadata fields available and to specify the destination bucket to store the
 *     inventory. A user with read access to objects in the destination bucket can also access all object metadata
 *     fields that are available in the inventory report.
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - The <code>s3:PutInventoryConfiguration</code> permission
 *         is required in a policy. For more information about general purpose buckets permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/using-iam-policies.html">Using
 *         Bucket Policies and User Policies</a> in the <i>Amazon S3 User Guide</i>.
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - To grant access to this API operation, you must have the <code>
 *         s3express:PutInventoryConfiguration</code> permission in an IAM identity-based policy instead of a
 *         bucket policy. For more information about directory bucket policies and permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam.html">Amazon
 *         Web Services Identity and Access Management (IAM) for S3 Express One Zone</a> in the <i>Amazon S3
 *         User Guide</i>.
 *       </li>
 *     </ul>
 *
 *     <p>To restrict access to an inventory report, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/example-bucket-policies.html#example-bucket-policies-s3-inventory">Restricting access to an Amazon S3 Inventory report</a>
 *     in the <i>Amazon S3 User Guide</i>. For more information about the metadata fields available in S3 Inventory,
 *     see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/storage-inventory.html#storage-inventory-contents">Amazon S3 Inventory lists</a> in the <i>Amazon S3 User Guide</i>. For more information about
 *     permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-with-s3-actions.html#using-with-s3-actions-related-to-bucket-subresources">Permissions related to bucket subresource operations</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">Identity and access
 *     management in Amazon S3</a> in the <i>Amazon S3 User Guide</i>.
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
 * <p><code>PutBucketInventoryConfiguration</code> has the following special errors:
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
 *     s3:PutInventoryConfiguration</code> bucket permission to set the configuration on the bucket.
 *   </dd>
 * </dl>
 *
 * <p>The following operations are related to <code>PutBucketInventoryConfiguration</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketInventoryConfiguration.html">GetBucketInventoryConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketInventoryConfiguration.html">DeleteBucketInventoryConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBucketInventoryConfigurations.html">ListBucketInventoryConfigurations</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class PutBucketInventoryConfiguration implements ApiOperation<PutBucketInventoryConfigurationInput, PutBucketInventoryConfigurationOutput> {

    private static final PutBucketInventoryConfiguration $INSTANCE = new PutBucketInventoryConfiguration();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketInventoryConfiguration"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?inventory")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketInventoryConfiguration instance() {
        return $INSTANCE;
    }

    private PutBucketInventoryConfiguration() {}

    @Override
    public ShapeBuilder<PutBucketInventoryConfigurationInput> inputBuilder() {
        return PutBucketInventoryConfigurationInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketInventoryConfigurationOutput> outputBuilder() {
        return PutBucketInventoryConfigurationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketInventoryConfigurationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketInventoryConfigurationOutput.$SCHEMA;
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
