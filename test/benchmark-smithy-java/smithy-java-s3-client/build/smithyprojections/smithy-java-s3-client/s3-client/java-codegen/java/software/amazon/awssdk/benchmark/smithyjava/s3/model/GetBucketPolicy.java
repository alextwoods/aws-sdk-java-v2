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
 * Returns the policy of a specified bucket.
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
 *     <p>If you are using an identity other than the root user of the Amazon Web Services account that owns the
 *     bucket, the calling identity must both have the <code>GetBucketPolicy</code> permissions on the specified
 *     bucket and belong to the bucket owner's account in order to use this operation.
 *
 *     <p>If you don't have <code>GetBucketPolicy</code> permissions, Amazon S3 returns a <code>403 Access Denied</code>
 *     error. If you have the correct permissions, but you're not using an identity that belongs to the bucket
 *     owner's account, Amazon S3 returns a <code>405 Method Not Allowed</code> error.
 *
 *     <p>To ensure that bucket owners don't inadvertently lock themselves out of their own buckets, the root
 *     principal in a bucket owner's Amazon Web Services account can perform the <code>GetBucketPolicy</code>, <code>
 *     PutBucketPolicy</code>, and <code>DeleteBucketPolicy</code> API actions, even if their bucket policy
 *     explicitly denies the root principal's access. Bucket owner root principals can only be blocked from
 *     performing these API actions by VPC endpoint policies and Amazon Web Services Organizations policies.
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - The <code>s3:GetBucketPolicy</code> permission is
 *         required in a policy. For more information about general purpose buckets bucket policies, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/using-iam-policies.html">
 *         Using Bucket Policies and User Policies</a> in the <i>Amazon S3 User Guide</i>.
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - To grant access to this API operation, you must have the <code>
 *         s3express:GetBucketPolicy</code> permission in an IAM identity-based policy instead of a bucket
 *         policy. Cross-account access to this API operation isn't supported. This operation can only be
 *         performed by the Amazon Web Services account that owns the resource. For more information about
 *         directory bucket policies and permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam.html">Amazon Web Services Identity and Access Management
 *         (IAM) for S3 Express One Zone</a> in the <i>Amazon S3 User Guide</i>.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     Example bucket policies
 *   </dt>
 *   <dd>
 *
 *     <p><b>General purpose buckets example bucket policies</b> - See <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/example-bucket-policies.html">Bucket policy examples</a> in the <i>
 *     Amazon S3 User Guide</i>.
 *
 *     <p><b>Directory bucket example bucket policies</b> - See <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam-example-bucket-policies.html">Example bucket policies for S3 Express One Zone</a>
 *     in the <i>Amazon S3 User Guide</i>.
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
 * <p>The following action is related to <code>GetBucketPolicy</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To get bucket policy</h3>
 *
 * <p>The following example returns bucket policy associated with a bucket.{@snippet :
 * var input = GetBucketPolicyInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.getBucketPolicy(input);
 * result.equals(GetBucketPolicyOutput.builder()
 *                   .policy("{\"Version\":\"2008-10-17\",&TCX5-2025-waiver;\"Id\":\"LogPolicy\",\"Statement\":[{\"Sid\":\"Enables the log delivery group to publish logs to your bucket \",\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"111122223333\"},\"Action\":[\"s3:GetBucketAcl\",\"s3:GetObjectAcl\",\"s3:PutObject\"],\"Resource\":[\"arn:aws:s3:::policytest1/*\",\"arn:aws:s3:::policytest1\"]}]}")
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetBucketPolicy implements ApiOperation<GetBucketPolicyInput, GetBucketPolicyOutput> {

    private static final GetBucketPolicy $INSTANCE = new GetBucketPolicy();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketPolicy"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?policy")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketPolicy instance() {
        return $INSTANCE;
    }

    private GetBucketPolicy() {}

    @Override
    public ShapeBuilder<GetBucketPolicyInput> inputBuilder() {
        return GetBucketPolicyInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketPolicyOutput> outputBuilder() {
        return GetBucketPolicyOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketPolicyInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketPolicyOutput.$SCHEMA;
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
