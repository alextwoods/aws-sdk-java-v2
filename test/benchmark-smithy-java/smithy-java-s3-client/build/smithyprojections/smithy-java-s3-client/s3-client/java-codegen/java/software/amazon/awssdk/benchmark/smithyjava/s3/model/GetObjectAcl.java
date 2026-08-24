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
 * This operation is not supported for directory buckets.
 *
 * <p>Returns the access control list (ACL) of an object. To use this operation, you must have <code>s3:GetObjectAcl</code>
 * permissions or <code>READ_ACP</code> access to the object. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/acl-overview.html#acl-access-policy-permission-mapping">Mapping of ACL permissions
 * and access policy permissions</a> in the <i>Amazon S3 User Guide</i>
 *
 * <p>This functionality is not supported for Amazon S3 on Outposts.
 *
 * <p>By default, GET returns ACL information about the current version of an object. To return ACL information about a
 * different version, use the versionId subresource.
 *
 * <p>If your bucket uses the bucket owner enforced setting for S3 Object Ownership, requests to read ACLs are still
 * supported and return the <code>bucket-owner-full-control</code> ACL with the owner being the account that created the
 * bucket. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html"> Controlling object ownership and disabling ACLs</a> in the <i>Amazon S3 User
 * Guide</i>.
 *
 * <p>The following operations are related to <code>GetObjectAcl</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectAttributes.html">GetObjectAttributes</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html">DeleteObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To retrieve object ACL</h3>
 *
 * <p>The following example retrieves access control list (ACL) of an object.{@snippet :
 * var input = GetObjectAclInput.builder()
 *                 .bucket("examplebucket").key("HappyFace.jpg")
 *                 .build();
 *
 * var result = client.getObjectAcl(input);
 * result.equals(GetObjectAclOutput.builder()
 *                   .owner(Owner.builder()
 *                              .displayName("owner-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                              .build()).grants(List.of(
 *                               Grant.builder()
 *                                   .grantee(Grantee.builder()
 *                                                .type(Type.CANONICAL_USER).displayName("owner-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                .build()).permission(Permission.WRITE)
 *                                   .build()
 *                               ,
 *                               Grant.builder()
 *                                   .grantee(Grantee.builder()
 *                                                .type(Type.CANONICAL_USER).displayName("owner-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                .build()).permission(Permission.WRITE_ACP)
 *                                   .build()
 *                               ,
 *                               Grant.builder()
 *                                   .grantee(Grantee.builder()
 *                                                .type(Type.CANONICAL_USER).displayName("owner-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                .build()).permission(Permission.READ)
 *                                   .build()
 *                               ,
 *                               Grant.builder()
 *                                   .grantee(Grantee.builder()
 *                                                .type(Type.CANONICAL_USER).displayName("owner-display-name").id("852b113eexamplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                .build()).permission(Permission.READ_ACP)
 *                                   .build()
 *                           ))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetObjectAcl implements ApiOperation<GetObjectAclInput, GetObjectAclOutput> {

    private static final GetObjectAcl $INSTANCE = new GetObjectAcl();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetObjectAcl"),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?acl")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(NoSuchKey.$ID, NoSuchKey.class, NoSuchKey::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetObjectAcl instance() {
        return $INSTANCE;
    }

    private GetObjectAcl() {}

    @Override
    public ShapeBuilder<GetObjectAclInput> inputBuilder() {
        return GetObjectAclInput.builder();
    }

    @Override
    public ShapeBuilder<GetObjectAclOutput> outputBuilder() {
        return GetObjectAclOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetObjectAclInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetObjectAclOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(NoSuchKey.$SCHEMA);
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
