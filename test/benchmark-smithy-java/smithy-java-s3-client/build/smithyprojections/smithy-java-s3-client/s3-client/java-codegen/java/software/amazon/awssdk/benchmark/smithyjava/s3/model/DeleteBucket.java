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
 * Deletes the S3 bucket. All objects (including all object versions and delete markers) in the bucket must be deleted
 * before the bucket itself can be deleted.
 *
 * <ul>
 *   <li>
 *     <b>Directory buckets</b> - If multipart uploads in a directory bucket are in progress, you can't delete the
 *     bucket until all the in-progress multipart uploads are aborted or completed.
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
 *         <b>General purpose bucket permissions</b> - You must have the <code>s3:DeleteBucket</code> permission
 *         on the specified bucket in a policy.
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - You must have the <code>s3express:DeleteBucket</code>
 *         permission in an IAM identity-based policy instead of a bucket policy. Cross-account access to this
 *         API operation isn't supported. This operation can only be performed by the Amazon Web Services
 *         account that owns the resource. For more information about directory bucket policies and permissions,
 *         see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam.html">Amazon Web Services Identity and Access Management (IAM) for S3 Express One Zone</a> in the <i>
 *         Amazon S3 User Guide</i>.
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
 * <p>The following operations are related to <code>DeleteBucket</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html">CreateBucket</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html">DeleteObject</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To delete a bucket</h3>
 *
 * <p>The following example deletes the specified bucket.{@snippet :
 * var input = DeleteBucketInput.builder()
 *                 .bucket("forrandall2")
 *                 .build();
 *
 * var result = client.deleteBucket(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class DeleteBucket implements ApiOperation<DeleteBucketInput, DeleteBucketOutput> {

    private static final DeleteBucket $INSTANCE = new DeleteBucket();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#DeleteBucket"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("DELETE").code(204).uri(UriPattern.parse("/{Bucket}")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteBucket instance() {
        return $INSTANCE;
    }

    private DeleteBucket() {}

    @Override
    public ShapeBuilder<DeleteBucketInput> inputBuilder() {
        return DeleteBucketInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteBucketOutput> outputBuilder() {
        return DeleteBucketOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteBucketInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteBucketOutput.$SCHEMA;
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
