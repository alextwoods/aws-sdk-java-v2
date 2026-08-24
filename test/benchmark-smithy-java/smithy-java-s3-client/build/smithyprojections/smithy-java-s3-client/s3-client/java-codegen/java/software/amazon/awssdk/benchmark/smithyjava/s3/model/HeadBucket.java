package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * You can use this operation to determine if a bucket exists and if you have permission to access it. The action
 * returns a <code>200 OK</code> HTTP status code if the bucket exists and you have permission to access it. You can
 * make a <code>HeadBucket</code> call on any bucket name to any Region in the partition, and regardless of the
 * permissions on the bucket, you will receive a response header with the correct bucket location so that you can then
 * make a proper, signed request to the appropriate Regional endpoint.
 *
 * <p>If the bucket doesn't exist or you don't have permission to access it, the <code>HEAD</code> request returns a
 * generic <code>400 Bad Request</code>, <code>403 Forbidden</code>, or <code>404 Not Found</code> HTTP status code. A
 * message body isn't included, so you can't determine the exception beyond these HTTP response codes.
 *
 * <dl>
 *   <dt>
 *     Authentication and authorization
 *   </dt>
 *   <dd>
 *
 *     <p><b>General purpose buckets</b> - Request to public buckets that grant the s3:ListBucket permission
 *     publicly do not need to be signed. All other <code>HeadBucket</code> requests must be authenticated and
 *     signed by using IAM credentials (access key ID and secret access key for the IAM identities). All headers
 *     with the <code>x-amz-</code> prefix, including <code>x-amz-copy-source</code>, must be signed. For more
 *     information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html">REST Authentication</a>.
 *
 *     <p><b>Directory buckets</b> - You must use IAM credentials to authenticate and authorize your access to the <code>
 *     HeadBucket</code> API operation, instead of using the temporary security credentials through the <code>
 *     CreateSession</code> API operation.
 *
 *     <p>Amazon Web Services CLI or SDKs handles authentication and authorization on your behalf.
 *   </dd>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - To use this operation, you must have permissions to
 *         perform the <code>s3:ListBucket</code> action. The bucket owner has this permission by default and
 *         can grant this permission to others. For more information about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">Managing access
 *         permissions to your Amazon S3 resources</a> in the <i>Amazon S3 User Guide</i>.
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - You must have the <b><code>s3express:CreateSession</code></b>
 *         permission in the <code>Action</code> element of a policy. If no session mode is specified, the
 *         session will be created with the maximum allowable privilege, attempting <code>ReadWrite</code>
 *         first, then <code>ReadOnly</code> if <code>ReadWrite</code> is not permitted. If you want to
 *         explicitly restrict the access to be read-only, you can set the <code>s3express:SessionMode</code>
 *         condition key to <code>ReadOnly</code> on the bucket.For more information about example bucket
 *         policies, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam-example-bucket-policies.html">Example bucket policies for S3 Express One Zone</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam-identity-policies.html">Amazon Web Services
 *         Identity and Access Management (IAM) identity-based policies for S3 Express One Zone</a> in the <i>
 *         Amazon S3 User Guide</i>.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     HTTP Host header syntax
 *   </dt>
 *   <dd>
 *
 *     <p><b>Directory buckets </b> - The HTTP Host header syntax is <code><i>Bucket-name</i>.s3express-<i>zone-id</i>
 *     .<i>region-code</i>.amazonaws.com</code>.
 *
 *     <p>You must make requests for this API operation to the Zonal endpoint. These endpoints support
 *     virtual-hosted-style requests in the format <code>https://<i>bucket-name</i>.s3express-<i>zone-id</i>.<i>
 *     region-code</i>.amazonaws.com</code>. Path-style requests are not supported. For more information about
 *     endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">Regional and Zonal endpoints for directory buckets in Availability
 *     Zones</a> in the <i>Amazon S3 User Guide</i>. For more information about endpoints in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">
 *     Concepts for directory buckets in Local Zones</a> in the <i>Amazon S3 User Guide</i>.
 *   </dd>
 * </dl>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To determine if bucket exists</h3>
 *
 * <p>This operation checks to see if a bucket exists.{@snippet :
 * var input = HeadBucketInput.builder()
 *                 .bucket("acl1")
 *                 .build();
 *
 * var result = client.headBucket(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class HeadBucket implements ApiOperation<HeadBucketInput, HeadBucketOutput> {

    private static final HeadBucket $INSTANCE = new HeadBucket();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#HeadBucket"),
            HttpTrait.builder().method("HEAD").code(200).uri(UriPattern.parse("/{Bucket}")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(NotFound.$ID, NotFound.class, NotFound::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static HeadBucket instance() {
        return $INSTANCE;
    }

    private HeadBucket() {}

    @Override
    public ShapeBuilder<HeadBucketInput> inputBuilder() {
        return HeadBucketInput.builder();
    }

    @Override
    public ShapeBuilder<HeadBucketOutput> outputBuilder() {
        return HeadBucketOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return HeadBucketInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return HeadBucketOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(NotFound.$SCHEMA);
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
