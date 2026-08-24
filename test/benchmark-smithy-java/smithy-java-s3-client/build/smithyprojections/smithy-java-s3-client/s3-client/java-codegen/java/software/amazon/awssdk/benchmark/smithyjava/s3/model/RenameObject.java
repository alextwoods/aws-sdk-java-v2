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
 * Renames an existing object in a directory bucket that uses the S3 Express One Zone storage class. You can use <code>
 * RenameObject</code> by specifying an existing object’s name as the source and the new name of the object as the
 * destination within the same directory bucket.
 *
 * <p><code>RenameObject</code> is only supported for objects stored in the S3 Express One Zone storage class.
 *
 * <p> To prevent overwriting an object, you can use the <code>If-None-Match</code> conditional header.
 *
 * <ul>
 *   <li>
 *     <b>If-None-Match</b> - Renames the object only if an object with the specified name does not already exist in
 *     the directory bucket. If you don't want to overwrite an existing object, you can add the <code>If-None-Match</code>
 *     conditional header with the value <code>‘&#42;’</code> in the <code>RenameObject</code> request. Amazon S3 then
 *     returns a <code>412 Precondition Failed</code> error if the object with the specified name already exists.
 *     For more information, see <a href="https://datatracker.ietf.org/doc/rfc7232/">RFC 7232</a>.
 *   </li>
 * </ul>
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <p> To grant access to the <code>RenameObject</code> operation on a directory bucket, we recommend that you
 *     use the <code>CreateSession</code> operation for session-based authorization. Specifically, you grant the <code>
 *     s3express:CreateSession</code> permission to the directory bucket in a bucket policy or an IAM identity-based
 *     policy. Then, you make the <code>CreateSession</code> API call on the directory bucket to obtain a session
 *     token. With the session token in your request header, you can make API requests to this operation. After the
 *     session token expires, you make another <code>CreateSession</code> API call to generate a new session token
 *     for use. The Amazon Web Services CLI and SDKs will create and manage your session including refreshing the
 *     session token automatically to avoid service interruptions when a session expires. In your bucket policy, you
 *     can specify the <code>s3express:SessionMode</code> condition key to control who can create a <code>ReadWrite</code>
 *     or <code>ReadOnly</code> session. A <code>ReadWrite</code> session is required for executing all the Zonal
 *     endpoint API operations, including <code>RenameObject</code>. For more information about authorization, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateSession.html"><code>
 *     CreateSession</code></a>. To learn more about Zonal endpoint API operations, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-create-session.html">Authorizing Zonal
 *     endpoint API operations with CreateSession</a> in the <i>Amazon S3 User Guide</i>.
 *   </dd>
 *   <dt>
 *     HTTP Host header syntax
 *   </dt>
 *   <dd>
 *
 *     <p><b>Directory buckets </b> - The HTTP Host header syntax is <code><i>Bucket-name</i>.s3express-<i>zone-id</i>
 *     .<i>region-code</i>.amazonaws.com</code>.
 *   </dd>
 * </dl>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class RenameObject implements ApiOperation<RenameObjectInput, RenameObjectOutput> {

    private static final RenameObject $INSTANCE = new RenameObject();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#RenameObject"),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?renameObject")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(IdempotencyParameterMismatch.$ID, IdempotencyParameterMismatch.class, IdempotencyParameterMismatch::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema IDEMPOTENCY_TOKEN_MEMBER = RenameObjectInput.$SCHEMA.member("ClientToken");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static RenameObject instance() {
        return $INSTANCE;
    }

    private RenameObject() {}

    @Override
    public ShapeBuilder<RenameObjectInput> inputBuilder() {
        return RenameObjectInput.builder();
    }

    @Override
    public ShapeBuilder<RenameObjectOutput> outputBuilder() {
        return RenameObjectOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return RenameObjectInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return RenameObjectOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(IdempotencyParameterMismatch.$SCHEMA);
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
        return IDEMPOTENCY_TOKEN_MEMBER;
    }

    @Override
    public ApiService service() {
        return S3ApiService.instance();
    }
    }
