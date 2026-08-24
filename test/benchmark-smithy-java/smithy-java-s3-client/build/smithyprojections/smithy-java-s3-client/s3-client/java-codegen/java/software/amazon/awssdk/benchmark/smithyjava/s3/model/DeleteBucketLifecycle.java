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
 * Deletes the lifecycle configuration from the specified bucket. Amazon S3 removes all the lifecycle configuration
 * rules in the lifecycle subresource associated with the bucket. Your objects never expire, and Amazon S3 no longer
 * automatically deletes any objects on the basis of rules contained in the deleted lifecycle configuration.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - By default, all Amazon S3 resources are private,
 *         including buckets, objects, and related subresources (for example, lifecycle configuration and
 *         website configuration). Only the resource owner (that is, the Amazon Web Services account that
 *         created it) can access the resource. The resource owner can optionally grant access permissions to
 *         others by writing an access policy. For this operation, a user must have the <code>
 *         s3:PutLifecycleConfiguration</code> permission.For more information about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">
 *         Managing Access Permissions to Your Amazon S3 Resources</a>.
 *       </li>
 *     </ul>
 *
 *     <ul>
 *       <li>
 *         <b>Directory bucket permissions</b> - You must have the <code>s3express:PutLifecycleConfiguration</code>
 *         permission in an IAM identity-based policy to use this operation. Cross-account access to this API
 *         operation isn't supported. The resource owner can optionally grant access permissions to others by
 *         creating a role or user for them as long as they are within the same account as the owner and
 *         resource.For more information about directory bucket policies and permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-security-iam.html">Authorizing
 *         Regional endpoint APIs with IAM</a> in the <i>Amazon S3 User Guide</i>.<b>Directory buckets </b> -
 *         For directory buckets, you must make requests for this API operation to the Regional endpoint. These
 *         endpoints support path-style requests in the format <code>https://s3express-control.<i>region-code</i>
 *         .amazonaws.com/<i>bucket-name</i></code>. Virtual-hosted-style requests aren't supported. For more
 *         information about endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">Regional and Zonal endpoints for directory
 *         buckets in Availability Zones</a> in the <i>Amazon S3 User Guide</i>. For more information about
 *         endpoints in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for directory buckets in Local Zones</a> in the <i>Amazon
 *         S3 User Guide</i>.
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <dl>
 *   <dt>
 *     HTTP Host header syntax
 *   </dt>
 *   <dd>
 *
 *     <p><b>Directory buckets </b> - The HTTP Host header syntax is <code>s3express-control.<i>region</i>
 *     .amazonaws.com</code>.
 *   </dd>
 * </dl>
 *
 * <p>For more information about the object expiration, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/intro-lifecycle-rules.html#intro-lifecycle-rules-actions">Elements to Describe Lifecycle Actions</a>.
 *
 * <p>Related actions include:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycleConfiguration.html">PutBucketLifecycleConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLifecycleConfiguration.html">GetBucketLifecycleConfiguration</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To delete lifecycle configuration on a bucket.</h3>
 *
 * <p>The following example deletes lifecycle configuration on a bucket.{@snippet :
 * var input = DeleteBucketLifecycleInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.deleteBucketLifecycle(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class DeleteBucketLifecycle implements ApiOperation<DeleteBucketLifecycleInput, DeleteBucketLifecycleOutput> {

    private static final DeleteBucketLifecycle $INSTANCE = new DeleteBucketLifecycle();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#DeleteBucketLifecycle"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("DELETE").code(204).uri(UriPattern.parse("/{Bucket}?lifecycle")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteBucketLifecycle instance() {
        return $INSTANCE;
    }

    private DeleteBucketLifecycle() {}

    @Override
    public ShapeBuilder<DeleteBucketLifecycleInput> inputBuilder() {
        return DeleteBucketLifecycleInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteBucketLifecycleOutput> outputBuilder() {
        return DeleteBucketLifecycleOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteBucketLifecycleInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteBucketLifecycleOutput.$SCHEMA;
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
