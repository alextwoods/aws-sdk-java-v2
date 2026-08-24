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
 * This action creates an Amazon S3 bucket. To create an Amazon S3 on Outposts bucket, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_control_CreateBucket.html"><code>CreateBucket</code></a>
 * .
 *
 * <p>Creates a new S3 bucket. To create a bucket, you must set up Amazon S3 and have a valid Amazon Web Services Access
 * Key ID to authenticate requests. Anonymous requests are never allowed to create buckets. By creating the bucket, you
 * become the bucket owner.
 *
 * <p>There are two types of buckets: general purpose buckets and directory buckets. For more information about these
 * bucket types, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/creating-buckets-s3.html">Creating, configuring, and working with Amazon S3 buckets</a> in the <i>Amazon S3 User Guide</i>
 * .
 *
 * <p>General purpose buckets exist in a global namespace, which means that each bucket name must be unique across all
 * Amazon Web Services accounts in all the Amazon Web Services Regions within a partition. A partition is a grouping of
 * Regions. Amazon Web Services currently has four partitions: <code>aws</code> (Standard Regions), <code>aws-cn</code>
 * (China Regions), <code>aws-us-gov</code> (Amazon Web Services GovCloud (US)), and <code>aws-eusc</code> (European
 * Sovereign Cloud). When you create a general purpose bucket, you can choose to create a bucket in the shared global
 * namespace or you can choose to create a bucket in your account regional namespace. Your account regional namespace is
 * a subdivision of the global namespace that only your account can create buckets in. For more information on account
 * regional namespaces, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/gpbucketnamespaces.html">Namespaces for general purpose buckets</a>.
 *
 * <ul>
 *   <li>
 *     <b>General purpose buckets</b> - If you send your <code>CreateBucket</code> request to the <code>
 *     s3.amazonaws.com</code> global endpoint, the request goes to the <code>us-east-1</code> Region. So the
 *     signature calculations in Signature Version 4 must use <code>us-east-1</code> as the Region, even if the
 *     location constraint in the request specifies another Region where the bucket is to be created. If you create
 *     a bucket in a Region other than US East (N. Virginia), your application must be able to handle 307 redirect.
 *     For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/VirtualHosting.html">Virtual hosting of buckets</a> in the <i>Amazon S3 User Guide</i>.
 *   </li>
 *   <li>
 *     <b>Directory buckets </b> - For directory buckets, you must make requests for this API operation to the
 *     Regional endpoint. These endpoints support path-style requests in the format <code>https://s3express-control.<i>
 *     region-code</i>.amazonaws.com/<i>bucket-name</i></code>. Virtual-hosted-style requests aren't supported. For
 *     more information about endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">Regional and Zonal endpoints for directory
 *     buckets in Availability Zones</a> in the <i>Amazon S3 User Guide</i>. For more information about endpoints in
 *     Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for directory buckets in Local Zones</a> in the <i>Amazon S3 User Guide</i>.
 *   </li>
 * </ul>
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - In addition to the <code>s3:CreateBucket</code>
 *         permission, the following permissions are required in a policy when your <code>CreateBucket</code>
 *         request includes specific headers:
 *
 *         <ul>
 *           <li>
 *             <b>Access control lists (ACLs)</b> - In your <code>CreateBucket</code> request, if you
 *             specify an access control list (ACL) and set it to <code>public-read</code>, <code>
 *             public-read-write</code>, <code>authenticated-read</code>, or if you explicitly specify any
 *             other custom ACLs, both <code>s3:CreateBucket</code> and <code>s3:PutBucketAcl</code>
 *             permissions are required. In your <code>CreateBucket</code> request, if you set the ACL to <code>
 *             private</code>, or if you don't specify any ACLs, only the <code>s3:CreateBucket</code>
 *             permission is required.
 *           </li>
 *           <li>
 *             <b>Object Lock</b> - In your <code>CreateBucket</code> request, if you set <code>
 *             x-amz-bucket-object-lock-enabled</code> to true, the <code>
 *             s3:PutBucketObjectLockConfiguration</code> and <code>s3:PutBucketVersioning</code>
 *             permissions are required.
 *           </li>
 *           <li>
 *             <b>S3 Object Ownership</b> - If your <code>CreateBucket</code> request includes the <code>
 *             x-amz-object-ownership</code> header, then the <code>s3:PutBucketOwnershipControls</code>
 *             permission is required. To set an ACL on a bucket as part of a <code>CreateBucket</code>
 *             request, you must explicitly set S3 Object Ownership for the bucket to a different value than
 *             the default, <code>BucketOwnerEnforced</code>. Additionally, if your desired bucket ACL
 *             grants public access, you must first create the bucket (without the bucket ACL) and then
 *             explicitly disable Block Public Access on the bucket before using <code>PutBucketAcl</code>
 *             to set the ACL. If you try to create a bucket with a public ACL, the request will fail.  For
 *             the majority of modern use cases in S3, we recommend that you keep all Block Public Access
 *             settings enabled and keep ACLs disabled. If you would like to share data with users outside
 *             of your account, you can use bucket policies as needed. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html">
 *             Controlling ownership of objects and disabling ACLs for your bucket </a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html">Blocking
 *             public access to your Amazon S3 storage </a> in the <i>Amazon S3 User Guide</i>.
 *           </li>
 *           <li>
 *             <b>S3 Block Public Access</b> - If your specific use case requires granting public access to
 *             your S3 resources, you can disable Block Public Access. Specifically, you can create a new
 *             bucket with Block Public Access enabled, then separately call the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeletePublicAccessBlock.html"><code>
 *             DeletePublicAccessBlock</code></a> API. To use this operation, you must have the <code>
 *             s3:PutBucketPublicAccessBlock</code> permission. For more information about S3 Block Public
 *             Access, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html">Blocking public access to your Amazon S3 storage </a> in the <i>Amazon S3 User
 *             Guide</i>.
 *           </li>
 *         </ul>
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - You must have the <code>s3express:CreateBucket</code>
 *         permission in an IAM identity-based policy instead of a bucket policy. Cross-account access to this
 *         API operation isn't supported. This operation can only be performed by the Amazon Web Services
 *         account that owns the resource. For more information about directory bucket policies and permissions,
 *         see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam.html">Amazon Web Services Identity and Access Management (IAM) for S3 Express One Zone</a> in the <i>
 *         Amazon S3 User Guide</i>.The permissions for ACLs, Object Lock, S3 Object Ownership, and S3 Block
 *         Public Access are not supported for directory buckets. For directory buckets, all Block Public Access
 *         settings are enabled at the bucket level and S3 Object Ownership is set to Bucket owner enforced
 *         (ACLs disabled). These settings can't be modified. For more information about permissions for
 *         creating and working with directory buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-overview.html">Directory buckets</a> in the <i>Amazon S3 User
 *         Guide</i>. For more information about supported S3 features for directory buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-one-zone.html#s3-express-features">Features of
 *         S3 Express One Zone</a> in the <i>Amazon S3 User Guide</i>.
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
 * <p>The following operations are related to <code>CreateBucket</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucket.html">DeleteBucket</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To create a bucket in a specific region</h3>
 *
 * <p>The following example creates a bucket. The request specifies an AWS region where to create the bucket.{@snippet :
 * var input = CreateBucketInput.builder()
 *                 .bucket("examplebucket").createBucketConfiguration(CreateBucketConfiguration.builder()
 *                                                .locationConstraint(BucketLocationConstraint.EU_WEST_1)
 *                                                .build())
 *                 .build();
 *
 * var result = client.createBucket(input);
 * result.equals(CreateBucketOutput.builder()
 *                   .location("http://examplebucket.<Region>.s3.amazonaws.com/")
 *                   .build());
 * }
 *
 * <h3>To create a bucket </h3>
 *
 * <p>The following example creates a bucket.{@snippet :
 * var input = CreateBucketInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.createBucket(input);
 * result.equals(CreateBucketOutput.builder()
 *                   .location("/examplebucket")
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class CreateBucket implements ApiOperation<CreateBucketInput, CreateBucketOutput> {

    private static final CreateBucket $INSTANCE = new CreateBucket();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#CreateBucket"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .withMember("DisableAccessPoints", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(BucketAlreadyExists.$ID, BucketAlreadyExists.class, BucketAlreadyExists::builder)
        .putType(BucketAlreadyOwnedByYou.$ID, BucketAlreadyOwnedByYou.class, BucketAlreadyOwnedByYou::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static CreateBucket instance() {
        return $INSTANCE;
    }

    private CreateBucket() {}

    @Override
    public ShapeBuilder<CreateBucketInput> inputBuilder() {
        return CreateBucketInput.builder();
    }

    @Override
    public ShapeBuilder<CreateBucketOutput> outputBuilder() {
        return CreateBucketOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return CreateBucketInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return CreateBucketOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(BucketAlreadyExists.$SCHEMA, BucketAlreadyOwnedByYou.$SCHEMA);
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
