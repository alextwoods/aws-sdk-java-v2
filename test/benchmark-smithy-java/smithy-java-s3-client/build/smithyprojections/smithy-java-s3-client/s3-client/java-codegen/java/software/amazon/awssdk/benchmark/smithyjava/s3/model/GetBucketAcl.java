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
 * <p>This implementation of the <code>GET</code> action uses the <code>acl</code> subresource to return the access
 * control list (ACL) of a bucket. To use <code>GET</code> to return the ACL of the bucket, you must have the <code>
 * READ_ACP</code> access to the bucket. If <code>READ_ACP</code> permission is granted to the anonymous user, you can
 * return the ACL of the bucket without using an authorization header.
 *
 * <p>When you use this API operation with an access point, provide the alias of the access point in place of the bucket
 * name.
 *
 * <p>When you use this API operation with an Object Lambda access point, provide the alias of the Object Lambda access
 * point in place of the bucket name. If the Object Lambda access point alias in a request is not valid, the error code <code>
 * InvalidAccessPointAliasError</code> is returned. For more information about <code>InvalidAccessPointAliasError</code>
 * , see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html#ErrorCodeList">List of Error Codes</a>.
 *
 * <p>If your bucket uses the bucket owner enforced setting for S3 Object Ownership, requests to read ACLs are still
 * supported and return the <code>bucket-owner-full-control</code> ACL with the owner being the account that created the
 * bucket. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html"> Controlling object ownership and disabling ACLs</a> in the <i>Amazon S3 User
 * Guide</i>.
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <p>The following operations are related to <code>GetBucketAcl</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html">ListObjects</a>
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class GetBucketAcl implements ApiOperation<GetBucketAclInput, GetBucketAclOutput> {

    private static final GetBucketAcl $INSTANCE = new GetBucketAcl();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketAcl"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?acl")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketAcl instance() {
        return $INSTANCE;
    }

    private GetBucketAcl() {}

    @Override
    public ShapeBuilder<GetBucketAclInput> inputBuilder() {
        return GetBucketAclInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketAclOutput> outputBuilder() {
        return GetBucketAclOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketAclInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketAclOutput.$SCHEMA;
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
