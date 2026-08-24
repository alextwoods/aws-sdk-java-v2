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
 * Returns the lifecycle configuration information set on the bucket. For information about lifecycle configuration, see
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lifecycle-mgmt.html">Object Lifecycle Management</a>.
 *
 * <p>Bucket lifecycle configuration now supports specifying a lifecycle rule using an object key name prefix, one or
 * more object tags, object size, or any combination of these. Accordingly, this section describes the latest API, which
 * is compatible with the new functionality. The previous version of the API supported filtering based only on an object
 * key name prefix, which is supported for general purpose buckets for backward compatibility. For the related API
 * description, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLifecycle.html">GetBucketLifecycle</a>.
 *
 * <p>Lifecyle configurations for directory buckets only support expiring objects and cancelling multipart uploads.
 * Expiring of versioned objects, transitions and tag filters are not supported.
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
 *         s3:GetLifecycleConfiguration</code> permission.For more information about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">
 *         Managing Access Permissions to Your Amazon S3 Resources</a>.
 *       </li>
 *     </ul>
 *
 *     <ul>
 *       <li>
 *         <b>Directory bucket permissions</b> - You must have the <code>s3express:GetLifecycleConfiguration</code>
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
 * <p><code>GetBucketLifecycleConfiguration</code> has the following special error:
 *
 * <ul>
 *   <li>
 *     Error code: <code>NoSuchLifecycleConfiguration</code>
 *
 *     <ul>
 *       <li>
 *         Description: The lifecycle configuration does not exist.
 *       </li>
 *       <li>
 *         HTTP Status Code: 404 Not Found
 *       </li>
 *       <li>
 *         SOAP Fault Code Prefix: Client
 *       </li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>The following operations are related to <code>GetBucketLifecycleConfiguration</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLifecycle.html">GetBucketLifecycle</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycle.html">PutBucketLifecycle</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketLifecycle.html">DeleteBucketLifecycle</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To get lifecycle configuration on a bucket</h3>
 *
 * <p>The following example retrieves lifecycle configuration on set on a bucket.{@snippet :
 * var input = GetBucketLifecycleConfigurationInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.getBucketLifecycleConfiguration(input);
 * result.equals(GetBucketLifecycleConfigurationOutput.builder()
 *                   .rules(List.of(LifecycleRule.builder()
 *                                       .prefix("TaxDocs").status(ExpirationStatus.ENABLED).transitions(List.of(Transition.builder()
 *                                                                 .days(365).storageClass(TransitionStorageClass.STANDARD_IA)
 *                                                                 .build())).id("Rule for TaxDocs/")
 *                                       .build()))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetBucketLifecycleConfiguration implements ApiOperation<GetBucketLifecycleConfigurationInput, GetBucketLifecycleConfigurationOutput> {

    private static final GetBucketLifecycleConfiguration $INSTANCE = new GetBucketLifecycleConfiguration();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketLifecycleConfiguration"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?lifecycle")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketLifecycleConfiguration instance() {
        return $INSTANCE;
    }

    private GetBucketLifecycleConfiguration() {}

    @Override
    public ShapeBuilder<GetBucketLifecycleConfigurationInput> inputBuilder() {
        return GetBucketLifecycleConfigurationInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketLifecycleConfigurationOutput> outputBuilder() {
        return GetBucketLifecycleConfigurationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketLifecycleConfigurationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketLifecycleConfigurationOutput.$SCHEMA;
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
