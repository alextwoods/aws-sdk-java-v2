package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.aws.traits.HttpChecksumTrait;
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
 * Creates a new lifecycle configuration for the bucket or replaces an existing lifecycle configuration. Keep in mind
 * that this will overwrite an existing lifecycle configuration, so if you want to retain any configuration details,
 * they must be included in the new lifecycle configuration. For information about lifecycle configuration, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lifecycle-mgmt.html">
 * Managing your storage lifecycle</a>.
 *
 * <p>Bucket lifecycle configuration now supports specifying a lifecycle rule using an object key name prefix, one or
 * more object tags, object size, or any combination of these. Accordingly, this section describes the latest API. The
 * previous version of the API supported filtering based only on an object key name prefix, which is supported for
 * backward compatibility. For the related API description, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycle.html">PutBucketLifecycle</a>.
 *
 * <dl>
 *   <dt>
 *     Rules
 *   </dt>
 *   <dd>
 *
 *     <p>You specify the lifecycle configuration in your request body. The lifecycle configuration is specified as
 *     XML consisting of one or more rules. An Amazon S3 Lifecycle configuration can have up to 1,000 rules. This
 *     limit is not adjustable.
 *
 *     <p>Bucket lifecycle configuration supports specifying a lifecycle rule using an object key name prefix, one
 *     or more object tags, object size, or any combination of these. Accordingly, this section describes the latest
 *     API. The previous version of the API supported filtering based only on an object key name prefix, which is
 *     supported for backward compatibility for general purpose buckets. For the related API description, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycle.html">
 *     PutBucketLifecycle</a>.
 *
 *     <p>Lifecyle configurations for directory buckets only support expiring objects and cancelling multipart
 *     uploads. Expiring of versioned objects,transitions and tag filters are not supported.
 *
 *     <p>A lifecycle rule consists of the following:
 *
 *     <ul>
 *       <li>
 *         A filter identifying a subset of objects to which the rule applies. The filter can be based on a key
 *         name prefix, object tags, object size, or any combination of these.
 *       </li>
 *       <li>
 *         A status indicating whether the rule is in effect.
 *       </li>
 *       <li>
 *         One or more lifecycle transition and expiration actions that you want Amazon S3 to perform on the
 *         objects identified by the filter. If the state of your bucket is versioning-enabled or
 *         versioning-suspended, you can have many versions of the same object (one current version and zero or
 *         more noncurrent versions). Amazon S3 provides predefined actions that you can specify for current and
 *         noncurrent object versions.
 *       </li>
 *     </ul>
 *
 *     <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lifecycle-mgmt.html">Object Lifecycle Management</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/intro-lifecycle-rules.html">Lifecycle Configuration Elements</a>.
 *   </dd>
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
 *         s3:PutLifecycleConfiguration</code> permission.You can also explicitly deny permissions. An explicit
 *         deny also supersedes any other permissions. If you want to block users or accounts from removing or
 *         deleting objects from your bucket, you must deny them permissions for the following actions:
 *
 *         <ul>
 *           <li>
 *             <code>s3:DeleteObject</code>
 *           </li>
 *           <li>
 *             <code>s3:DeleteObjectVersion</code>
 *           </li>
 *           <li>
 *             <code>s3:PutLifecycleConfiguration</code>For more information about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">
 *             Managing Access Permissions to Your Amazon S3 Resources</a>.
 *           </li>
 *         </ul>
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
 *   <dt>
 *     HTTP Host header syntax
 *   </dt>
 *   <dd>
 *
 *     <p><b>Directory buckets </b> - The HTTP Host header syntax is <code>s3express-control.<i>region</i>
 *     .amazonaws.com</code>.
 *
 *     <p>The following operations are related to <code>PutBucketLifecycleConfiguration</code>:
 *
 *     <ul>
 *       <li>
 *         <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLifecycleConfiguration.html">GetBucketLifecycleConfiguration</a>
 *       </li>
 *       <li>
 *         <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketLifecycle.html">DeleteBucketLifecycle</a>
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>Put bucket lifecycle</h3>
 *
 * <p>The following example replaces existing lifecycle configuration, if any, on the specified bucket.{@snippet :
 * var input = PutBucketLifecycleConfigurationInput.builder()
 *                 .bucket("examplebucket").lifecycleConfiguration(BucketLifecycleConfiguration.builder()
 *                                             .rules(List.of(LifecycleRule.builder()
 *                                                                 .filter(LifecycleRuleFilter.builder()
 *                                                                             .prefix("documents/")
 *                                                                             .build()).status(ExpirationStatus.ENABLED).transitions(List.of(Transition.builder()
 *                                                                                           .days(365).storageClass(TransitionStorageClass.GLACIER)
 *                                                                                           .build())).expiration(LifecycleExpiration.builder()
 *                                                                                 .days(3650)
 *                                                                                 .build()).id("TestOnly")
 *                                                                 .build()))
 *                                             .build())
 *                 .build();
 *
 * var result = client.putBucketLifecycleConfiguration(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class PutBucketLifecycleConfiguration implements ApiOperation<PutBucketLifecycleConfigurationInput, PutBucketLifecycleConfigurationOutput> {

    private static final PutBucketLifecycleConfiguration $INSTANCE = new PutBucketLifecycleConfiguration();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketLifecycleConfiguration"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .withMember("requestChecksumRequired", true)
                    .build()
            ),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?lifecycle")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketLifecycleConfiguration instance() {
        return $INSTANCE;
    }

    private PutBucketLifecycleConfiguration() {}

    @Override
    public ShapeBuilder<PutBucketLifecycleConfigurationInput> inputBuilder() {
        return PutBucketLifecycleConfigurationInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketLifecycleConfigurationOutput> outputBuilder() {
        return PutBucketLifecycleConfigurationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketLifecycleConfigurationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketLifecycleConfigurationOutput.$SCHEMA;
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
