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
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation enables you to delete multiple objects from a bucket using a single HTTP request. If you know the
 * object keys that you want to delete, then this operation provides a suitable alternative to sending individual delete
 * requests, reducing per-request overhead.
 *
 * <p>The request can contain a list of up to 1,000 keys that you want to delete. In the XML, you provide the object key
 * names, and optionally, version IDs if you want to delete a specific version of the object from a versioning-enabled
 * bucket. For each key, Amazon S3 performs a delete operation and returns the result of that delete, success or
 * failure, in the response. If the object specified in the request isn't found, Amazon S3 confirms the deletion by
 * returning the result as deleted.
 *
 * <ul>
 *   <li>
 *     <b>Directory buckets</b> - S3 Versioning isn't enabled and supported for directory buckets.
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
 * <p>The operation supports two modes for the response: verbose and quiet. By default, the operation uses verbose mode
 * in which the response includes the result of deletion of each key in your request. In quiet mode the response
 * includes only keys where the delete operation encountered an error. For a successful deletion in a quiet mode, the
 * operation does not return any information about the delete in the response body.
 *
 * <p>When performing this action on an MFA Delete enabled bucket, that attempts to delete any versioned objects, you
 * must include an MFA token. If you do not provide one, the entire request will fail, even if there are non-versioned
 * objects you are trying to delete. If you provide an invalid token, whether there are versioned keys in the request or
 * not, the entire Multi-Object Delete request will fail. For information about MFA Delete, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/Versioning.html#MultiFactorAuthenticationDelete">MFA Delete</a> in the
 * <i>Amazon S3 User Guide</i>.
 *
 * <p><b>Directory buckets</b> - MFA delete is not supported by directory buckets.
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
 *             <b><code>s3:DeleteObject</code></b> - To delete an object from a bucket, you must always
 *             specify the <code>s3:DeleteObject</code> permission.
 *           </li>
 *           <li>
 *             <b><code>s3:DeleteObjectVersion</code></b> - To delete a specific version of an object from a
 *             versioning-enabled bucket, you must specify the <code>s3:DeleteObjectVersion</code>
 *             permission.If the <code>s3:DeleteObject</code> or <code>s3:DeleteObjectVersion</code>
 *             permissions are explicitly denied in your bucket policy, attempts to delete any unversioned
 *             objects result in a <code>403 Access Denied</code> error.
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
 *     Content-MD5 request header
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket</b> - The Content-MD5 request header is required for all Multi-Object
 *         Delete requests. Amazon S3 uses the header value to ensure that your request body has not been
 *         altered in transit.
 *       </li>
 *       <li>
 *         <b>Directory bucket</b> - The Content-MD5 request header or a additional checksum request header
 *         (including <code>x-amz-checksum-crc32</code>, <code>x-amz-checksum-crc32c</code>, <code>
 *         x-amz-checksum-sha1</code>, or <code>x-amz-checksum-sha256</code>) is required for all Multi-Object
 *         Delete requests.
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
 * <p>The following operations are related to <code>DeleteObjects</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html">CompleteMultipartUpload</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListParts.html">ListParts</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html">AbortMultipartUpload</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To delete multiple object versions from a versioned bucket</h3>
 *
 * <p>The following example deletes objects from a bucket. The request specifies object versions. S3 deletes specific object versions and returns the key and versions of deleted objects in the response.{@snippet :
 * var input = DeleteObjectsInput.builder()
 *                 .bucket("examplebucket").delete(Delete.builder()
 *                             .objects(List.of(
 *                                          ObjectIdentifier.builder()
 *                                              .key("HappyFace.jpg").versionId("2LWg7lQLnY41.maGB5Z6SWW.dcq0vx7b")
 *                                              .build()
 *                                          ,
 *                                          ObjectIdentifier.builder()
 *                                              .key("HappyFace.jpg").versionId("yoz3HB.ZhCS_tKVEmIOr7qYyyAaZSKVd")
 *                                              .build()
 *                                      )).quiet(false)
 *                             .build())
 *                 .build();
 *
 * var result = client.deleteObjects(input);
 * result.equals(DeleteObjectsOutput.builder()
 *                   .deleted(List.of(
 *                                DeletedObject.builder()
 *                                    .versionId("yoz3HB.ZhCS_tKVEmIOr7qYyyAaZSKVd").key("HappyFace.jpg")
 *                                    .build()
 *                                ,
 *                                DeletedObject.builder()
 *                                    .versionId("2LWg7lQLnY41.maGB5Z6SWW.dcq0vx7b").key("HappyFace.jpg")
 *                                    .build()
 *                            ))
 *                   .build());
 * }
 *
 * <h3>To delete multiple objects from a versioned bucket</h3>
 *
 * <p>The following example deletes objects from a bucket. The bucket is versioned, and the request does not specify the object version to delete. In this case, all versions remain in the bucket and S3 adds a delete marker.{@snippet :
 * var input = DeleteObjectsInput.builder()
 *                 .bucket("examplebucket").delete(Delete.builder()
 *                             .objects(List.of(
 *                                          ObjectIdentifier.builder()
 *                                              .key("objectkey1")
 *                                              .build()
 *                                          ,
 *                                          ObjectIdentifier.builder()
 *                                              .key("objectkey2")
 *                                              .build()
 *                                      )).quiet(false)
 *                             .build())
 *                 .build();
 *
 * var result = client.deleteObjects(input);
 * result.equals(DeleteObjectsOutput.builder()
 *                   .deleted(List.of(
 *                                DeletedObject.builder()
 *                                    .deleteMarkerVersionId("A._w1z6EFiCF5uhtQMDal9JDkID9tQ7F").key("objectkey1").deleteMarker(true)
 *                                    .build()
 *                                ,
 *                                DeletedObject.builder()
 *                                    .deleteMarkerVersionId("iOd_ORxhkKe_e8G8_oSGxt2PjsCZKlkt").key("objectkey2").deleteMarker(true)
 *                                    .build()
 *                            ))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class DeleteObjects implements ApiOperation<DeleteObjectsInput, DeleteObjectsOutput> {

    private static final DeleteObjects $INSTANCE = new DeleteObjects();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#DeleteObjects"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .withMember("requestChecksumRequired", true)
                    .build()
            ),
            HttpTrait.builder().method("POST").code(200).uri(UriPattern.parse("/{Bucket}?delete")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteObjects instance() {
        return $INSTANCE;
    }

    private DeleteObjects() {}

    @Override
    public ShapeBuilder<DeleteObjectsInput> inputBuilder() {
        return DeleteObjectsInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteObjectsOutput> outputBuilder() {
        return DeleteObjectsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteObjectsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteObjectsOutput.$SCHEMA;
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
