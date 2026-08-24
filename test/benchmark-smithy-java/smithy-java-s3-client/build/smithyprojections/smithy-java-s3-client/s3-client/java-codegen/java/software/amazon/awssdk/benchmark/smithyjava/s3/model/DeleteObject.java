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
 * Removes an object from a bucket. The behavior depends on the bucket's versioning state:
 *
 * <ul>
 *   <li>
 *     If bucket versioning is not enabled, the operation permanently deletes the object.
 *   </li>
 *   <li>
 *     If bucket versioning is enabled, the operation inserts a delete marker, which becomes the current version of
 *     the object. To permanently delete an object in a versioned bucket, you must include the object’s <code>
 *     versionId</code> in the request. For more information about versioning-enabled buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeletingObjectVersions.html">Deleting
 *     object versions from a versioning-enabled bucket</a>.
 *   </li>
 *   <li>
 *     If bucket versioning is suspended, the operation removes the object that has a null <code>versionId</code>,
 *     if there is one, and inserts a delete marker that becomes the current version of the object. If there isn't
 *     an object with a null <code>versionId</code>, and all versions of the object have a <code>versionId</code>,
 *     Amazon S3 does not remove the object and only inserts a delete marker. To permanently delete an object that
 *     has a <code>versionId</code>, you must include the object’s <code>versionId</code> in the request. For more
 *     information about versioning-suspended buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeletingObjectsfromVersioningSuspendedBuckets.html">Deleting objects from versioning-suspended buckets</a>
 *     .
 *   </li>
 * </ul>
 *
 * <ul>
 *   <li>
 *     <b>Directory buckets</b> - S3 Versioning isn't enabled and supported for directory buckets. For this API
 *     operation, only the <code>null</code> value of the version ID is supported by directory buckets. You can only
 *     specify <code>null</code> to the <code>versionId</code> query parameter in the request.
 *   </li>
 *   <li>
 *     <b>Directory buckets</b> - For directory buckets, you must make requests for this API operation to the Zonal
 *     endpoint. These endpoints support virtual-hosted-style requests in the format <code>https://<i>
 *     amzn-s3-demo-bucket</i>.s3express-<i>zone-id</i>.<i>region-code</i>.amazonaws.com/<i>key-name</i></code>.
 *     Path-style requests are not supported. For more information about endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">
 *     Regional and Zonal endpoints for directory buckets in Availability Zones</a> in the <i>Amazon S3 User Guide</i>
 *     . For more information about endpoints in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for directory buckets in Local Zones</a>
 *     in the <i>Amazon S3 User Guide</i>.
 *   </li>
 * </ul>
 *
 * <p>To remove a specific version, you must use the <code>versionId</code> query parameter. Using this query parameter
 * permanently deletes the version. If the object deleted is a delete marker, Amazon S3 sets the response header <code>
 * x-amz-delete-marker</code> to true.
 *
 * <p>If the object you want to delete is in a bucket where the bucket versioning configuration is MFA Delete enabled,
 * you must include the <code>x-amz-mfa</code> request header in the DELETE <code>versionId</code> request. Requests
 * that include <code>x-amz-mfa</code> must use HTTPS. For more information about MFA Delete, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMFADelete.html">Using MFA Delete</a>
 * in the <i>Amazon S3 User Guide</i>. To see sample requests that use versioning, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTObjectDELETE.html#ExampleVersionObjectDelete">Sample Request</a>.
 *
 * <p><b>Directory buckets</b> - MFA delete is not supported by directory buckets.
 *
 * <p>You can delete objects by explicitly calling DELETE Object or calling (<a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycle.html">PutBucketLifecycle</a>) to enable Amazon
 * S3 to remove them for you. If you want to block users or accounts from removing or deleting objects from your bucket,
 * you must deny them the <code>s3:DeleteObject</code>, <code>s3:DeleteObjectVersion</code>, and <code>
 * s3:PutLifeCycleConfiguration</code> actions.
 *
 * <p><b>Directory buckets</b> - S3 Lifecycle is not supported by directory buckets.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - The following permissions are required in your policies
 *         when your <code>DeleteObjects</code> request includes specific headers.
 *
 *         <ul>
 *           <li>
 *             <b><code>s3:DeleteObject</code></b> - To delete an object from a bucket, you must always have
 *             the <code>s3:DeleteObject</code> permission.
 *           </li>
 *           <li>
 *             <b><code>s3:DeleteObjectVersion</code></b> - To delete a specific version of an object from a
 *             versioning-enabled bucket, you must have the <code>s3:DeleteObjectVersion</code> permission.
 *             If the <code>s3:DeleteObject</code> or <code>s3:DeleteObjectVersion</code> permissions are
 *             explicitly denied in your bucket policy, attempts to delete any unversioned objects result in
 *             a <code>403 Access Denied</code> error.
 *           </li>
 *         </ul>
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - To grant access to this API operation on a directory bucket, we
 *         recommend that you use the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateSession.html"><code>CreateSession</code></a> API operation for session-based
 *         authorization. Specifically, you grant the <code>s3express:CreateSession</code> permission to the
 *         directory bucket in a bucket policy or an IAM identity-based policy. Then, you make the <code>
 *         CreateSession</code> API call on the bucket to obtain a session token. With the session token in your
 *         request header, you can make API requests to this operation. After the session token expires, you
 *         make another <code>CreateSession</code> API call to generate a new session token for use. Amazon Web
 *         Services CLI or SDKs create session and refresh the session token automatically to avoid service
 *         interruptions when a session expires. For more information about authorization, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateSession.html"><code>
 *         CreateSession</code></a>.
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
 *   </dd>
 * </dl>
 *
 * <p>The following action is related to <code>DeleteObject</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <p>The <code>If-Match</code> header is supported for both general purpose and directory buckets. <code>
 * IfMatchLastModifiedTime</code> and <code>IfMatchSize</code> is only supported for directory buckets.
 *
 * <h2>Examples</h2>
 * <h3>To delete an object (from a non-versioned bucket)</h3>
 *
 * <p>The following example deletes an object from a non-versioned bucket.{@snippet :
 * var input = DeleteObjectInput.builder()
 *                 .bucket("ExampleBucket").key("HappyFace.jpg")
 *                 .build();
 *
 * var result = client.deleteObject(input);
 * result.equals();
 * }
 *
 * <h3>To delete an object</h3>
 *
 * <p>The following example deletes an object from an S3 bucket.{@snippet :
 * var input = DeleteObjectInput.builder()
 *                 .bucket("examplebucket").key("objectkey.jpg")
 *                 .build();
 *
 * var result = client.deleteObject(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class DeleteObject implements ApiOperation<DeleteObjectInput, DeleteObjectOutput> {

    private static final DeleteObject $INSTANCE = new DeleteObject();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#DeleteObject"),
            HttpTrait.builder().method("DELETE").code(204).uri(UriPattern.parse("/{Bucket}/{Key+}?x-id=DeleteObject")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteObject instance() {
        return $INSTANCE;
    }

    private DeleteObject() {}

    @Override
    public ShapeBuilder<DeleteObjectInput> inputBuilder() {
        return DeleteObjectInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteObjectOutput> outputBuilder() {
        return DeleteObjectOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteObjectInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteObjectOutput.$SCHEMA;
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
