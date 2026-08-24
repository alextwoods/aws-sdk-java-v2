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
 * The <code>HEAD</code> operation retrieves metadata from an object without returning the object itself. This operation
 * is useful if you're interested only in an object's metadata.
 *
 * <p>A <code>HEAD</code> request has the same options as a <code>GET</code> operation on an object. The response is
 * identical to the <code>GET</code> response except that there is no response body. Because of this, if the <code>HEAD</code>
 * request generates an error, it returns a generic code, such as <code>400 Bad Request</code>, <code>403 Forbidden</code>
 * , <code>404 Not Found</code>, <code>405 Method Not Allowed</code>, <code>412 Precondition Failed</code>, or <code>304
 * Not Modified</code>. It's not possible to retrieve the exact exception of these error codes.
 *
 * <p>Request headers are limited to 8 KB in size. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTCommonRequestHeaders.html">Common Request Headers</a>.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - To use <code>HEAD</code>, you must have the <code>
 *         s3:GetObject</code> permission. You need the relevant read object (or version) permission for this
 *         operation. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/list_amazons3.html">Actions, resources, and condition keys for Amazon S3</a> in
 *         the <i>Amazon S3 User Guide</i>. For more information about the permissions to S3 API operations by
 *         S3 resource types, see <a href="/AmazonS3/latest/userguide/using-with-s3-policy-actions.html">Required permissions for Amazon S3 API operations</a> in the <i>Amazon S3
 *         User Guide</i>.If the object you request doesn't exist, the error that Amazon S3 returns depends on
 *         whether you also have the <code>s3:ListBucket</code> permission.
 *
 *         <ul>
 *           <li>
 *             If you have the <code>s3:ListBucket</code> permission on the bucket, Amazon S3 returns an
 *             HTTP status code <code>404 Not Found</code> error.
 *           </li>
 *           <li>
 *             If you don’t have the <code>s3:ListBucket</code> permission, Amazon S3 returns an HTTP status
 *             code <code>403 Forbidden</code> error.
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
 *         CreateSession</code></a>.If you enable <code>x-amz-checksum-mode</code> in the request and the object
 *         is encrypted with Amazon Web Services Key Management Service (Amazon Web Services KMS), you must also
 *         have the <code>kms:GenerateDataKey</code> and <code>kms:Decrypt</code> permissions in IAM
 *         identity-based policies and KMS key policies for the KMS key to retrieve the checksum of the object.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     Encryption
 *   </dt>
 *   <dd>
 *
 *     <p>Encryption request headers, like <code>x-amz-server-side-encryption</code>, should not be sent for <code>
 *     HEAD</code> requests if your object uses server-side encryption with Key Management Service (KMS) keys
 *     (SSE-KMS), dual-layer server-side encryption with Amazon Web Services KMS keys (DSSE-KMS), or server-side
 *     encryption with Amazon S3 managed encryption keys (SSE-S3). The <code>x-amz-server-side-encryption</code>
 *     header is used when you <code>PUT</code> an object to S3 and want to specify the encryption method. If you
 *     include this header in a <code>HEAD</code> request for an object that uses these types of keys, you’ll get an
 *     HTTP <code>400 Bad Request</code> error. It's because the encryption method can't be changed when you
 *     retrieve the object.
 *
 *     <p>If you encrypt an object by using server-side encryption with customer-provided encryption keys (SSE-C)
 *     when you store the object in Amazon S3, then when you retrieve the metadata from the object, you must use the
 *     following headers to provide the encryption key for the server to be able to retrieve the object's metadata.
 *     The headers are:
 *
 *     <ul>
 *       <li>
 *         <code>x-amz-server-side-encryption-customer-algorithm</code>
 *       </li>
 *       <li>
 *         <code>x-amz-server-side-encryption-customer-key</code>
 *       </li>
 *       <li>
 *         <code>x-amz-server-side-encryption-customer-key-MD5</code>
 *       </li>
 *     </ul>
 *
 *     <p>For more information about SSE-C, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Server-Side Encryption (Using Customer-Provided Encryption Keys)</a>
 *     in the <i>Amazon S3 User Guide</i>.
 *
 *     <p><b>Directory bucket </b> - For directory buckets, there are only two supported options for server-side
 *     encryption: SSE-S3 and SSE-KMS. SSE-C isn't supported. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-serv-side-encryption.html">Protecting data with
 *     server-side encryption</a> in the <i>Amazon S3 User Guide</i>.
 *   </dd>
 *   <dt>
 *     Versioning
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         If the current version of the object is a delete marker, Amazon S3 behaves as if the object was
 *         deleted and includes <code>x-amz-delete-marker: true</code> in the response.
 *       </li>
 *       <li>
 *         If the specified version is a delete marker, the response returns a <code>405 Method Not Allowed</code>
 *         error and the <code>Last-Modified: timestamp</code> response header.
 *       </li>
 *     </ul>
 *
 *     <ul>
 *       <li>
 *         <b>Directory buckets</b> - Delete marker is not supported for directory buckets.
 *       </li>
 *       <li>
 *         <b>Directory buckets</b> - S3 Versioning isn't enabled and supported for directory buckets. For this
 *         API operation, only the <code>null</code> value of the version ID is supported by directory buckets.
 *         You can only specify <code>null</code> to the <code>versionId</code> query parameter in the request.
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
 *     <p>For directory buckets, you must make requests for this API operation to the Zonal endpoint. These
 *     endpoints support virtual-hosted-style requests in the format <code>https://<i>amzn-s3-demo-bucket</i>
 *     .s3express-<i>zone-id</i>.<i>region-code</i>.amazonaws.com/<i>key-name</i></code>. Path-style requests are
 *     not supported. For more information about endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">Regional and Zonal
 *     endpoints for directory buckets in Availability Zones</a> in the <i>Amazon S3 User Guide</i>. For more
 *     information about endpoints in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for directory buckets in Local Zones</a> in the <i>
 *     Amazon S3 User Guide</i>.
 *   </dd>
 * </dl>
 *
 * <p>The following actions are related to <code>HeadObject</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectAttributes.html">GetObjectAttributes</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To retrieve metadata of an object without returning the object itself</h3>
 *
 * <p>The following example retrieves an object metadata.{@snippet :
 * var input = HeadObjectInput.builder()
 *                 .bucket("examplebucket").key("HappyFace.jpg")
 *                 .build();
 *
 * var result = client.headObject(input);
 * result.equals(HeadObjectOutput.builder()
 *                   .acceptRanges("bytes").contentType("image/jpeg").lastModified(Instant.parse("2016-12-15T01:19:41Z")).contentLength(3191).versionId("null").eTag("\"6805f2cfc46c0f04559748bb039d69ae\"").metadata(Collections.emptyMap())
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class HeadObject implements ApiOperation<HeadObjectInput, HeadObjectOutput> {

    private static final HeadObject $INSTANCE = new HeadObject();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#HeadObject"),
            HttpTrait.builder().method("HEAD").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}")).build());

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
    public static HeadObject instance() {
        return $INSTANCE;
    }

    private HeadObject() {}

    @Override
    public ShapeBuilder<HeadObjectInput> inputBuilder() {
        return HeadObjectInput.builder();
    }

    @Override
    public ShapeBuilder<HeadObjectOutput> outputBuilder() {
        return HeadObjectOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return HeadObjectInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return HeadObjectOutput.$SCHEMA;
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
