package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.aws.traits.HttpChecksumTrait;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Retrieves an object from Amazon S3.
 *
 * <p>In the <code>GetObject</code> request, specify the full key name for the object.
 *
 * <p><b>General purpose buckets</b> - Both the virtual-hosted-style requests and the path-style requests are supported.
 * For a virtual hosted-style request example, if you have the object <code>photos/2006/February/sample.jpg</code>,
 * specify the object key name as <code>/photos/2006/February/sample.jpg</code>. For a path-style request example, if
 * you have the object <code>photos/2006/February/sample.jpg</code> in the bucket named <code>examplebucket</code>,
 * specify the object key name as <code>/examplebucket/photos/2006/February/sample.jpg</code>. For more information
 * about request types, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/VirtualHosting.html#VirtualHostingSpecifyBucket">HTTP Host Header Bucket Specification</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <p><b>Directory buckets</b> - Only virtual-hosted-style requests are supported. For a virtual hosted-style request
 * example, if you have the object <code>photos/2006/February/sample.jpg</code> in the bucket named <code>
 * amzn-s3-demo-bucket--usw2-az1--x-s3</code>, specify the object key name as <code>/photos/2006/February/sample.jpg</code>
 * . Also, when you make requests to this API operation, your requests are sent to the Zonal endpoint. These endpoints
 * support virtual-hosted-style requests in the format <code>https://<i>bucket-name</i>.s3express-<i>zone-id</i>.<i>
 * region-code</i>.amazonaws.com/<i>key-name</i></code>. Path-style requests are not supported. For more information
 * about endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">Regional and Zonal endpoints for directory buckets in Availability
 * Zones</a> in the <i>Amazon S3 User Guide</i>. For more information about endpoints in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts
 * for directory buckets in Local Zones</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - You must have the required permissions in a policy. To
 *         use <code>GetObject</code>, you must have the <code>READ</code> access to the object (or version). If
 *         you grant <code>READ</code> access to the anonymous user, the <code>GetObject</code> operation
 *         returns the object without using an authorization header. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/using-with-s3-actions.html">Specifying
 *         permissions in a policy</a> in the <i>Amazon S3 User Guide</i>.If you include a <code>versionId</code>
 *         in your request header, you must have the <code>s3:GetObjectVersion</code> permission to access a
 *         specific version of an object. The <code>s3:GetObject</code> permission is not required in this
 *         scenario.If you request the current version of an object without a specific <code>versionId</code> in
 *         the request header, only the <code>s3:GetObject</code> permission is required. The <code>
 *         s3:GetObjectVersion</code> permission is not required in this scenario. If the object that you
 *         request doesn’t exist, the error that Amazon S3 returns depends on whether you also have the <code>
 *         s3:ListBucket</code> permission.
 *
 *         <ul>
 *           <li>
 *             If you have the <code>s3:ListBucket</code> permission on the bucket, Amazon S3 returns an
 *             HTTP status code <code>404 Not Found</code> error.
 *           </li>
 *           <li>
 *             If you don’t have the <code>s3:ListBucket</code> permission, Amazon S3 returns an HTTP status
 *             code <code>403 Access Denied</code> error.
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
 *         CreateSession</code></a>.If the object is encrypted using SSE-KMS, you must also have the <code>
 *         kms:GenerateDataKey</code> and <code>kms:Decrypt</code> permissions in IAM identity-based policies
 *         and KMS key policies for the KMS key.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     Storage classes
 *   </dt>
 *   <dd>
 *
 *     <p>If the object you are retrieving is stored in the S3 Glacier Flexible Retrieval storage class, the S3
 *     Glacier Deep Archive storage class, the S3 Intelligent-Tiering Archive Access tier, or the S3
 *     Intelligent-Tiering Deep Archive Access tier, before you can retrieve the object you must first restore a
 *     copy using <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_RestoreObject.html">RestoreObject</a>. Otherwise, this operation returns an <code>InvalidObjectState</code> error.
 *     For information about restoring archived objects, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/restoring-objects.html">Restoring Archived Objects</a> in the <i>Amazon S3
 *     User Guide</i>.
 *
 *     <p><b>Directory buckets </b> - Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express
 *     One Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent Access
 *     storage class) in Dedicated Local Zones. Unsupported storage class values won't write a destination object
 *     and will respond with the HTTP status code <code>400 Bad Request</code>.
 *   </dd>
 *   <dt>
 *     Encryption
 *   </dt>
 *   <dd>
 *
 *     <p>Encryption request headers, like <code>x-amz-server-side-encryption</code>, should not be sent for the <code>
 *     GetObject</code> requests, if your object uses server-side encryption with Amazon S3 managed encryption keys
 *     (SSE-S3), server-side encryption with Key Management Service (KMS) keys (SSE-KMS), or dual-layer server-side
 *     encryption with Amazon Web Services KMS keys (DSSE-KMS). If you include the header in your <code>GetObject</code>
 *     requests for the object that uses these types of keys, you’ll get an HTTP <code>400 Bad Request</code> error.
 *
 *     <p><b>Directory buckets</b> - For directory buckets, there are only two supported options for server-side
 *     encryption: SSE-S3 and SSE-KMS. SSE-C isn't supported. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-serv-side-encryption.html">Protecting data with
 *     server-side encryption</a> in the <i>Amazon S3 User Guide</i>.
 *   </dd>
 *   <dt>
 *     Overriding response header values through the request
 *   </dt>
 *   <dd>
 *
 *     <p>There are times when you want to override certain response header values of a <code>GetObject</code>
 *     response. For example, you might override the <code>Content-Disposition</code> response header value through
 *     your <code>GetObject</code> request.
 *
 *     <p>You can override values for a set of response headers. These modified response header values are included
 *     only in a successful response, that is, when the HTTP status code <code>200 OK</code> is returned. The
 *     headers you can override using the following query parameters in the request are a subset of the headers that
 *     Amazon S3 accepts when you create an object.
 *
 *     <p>The response headers that you can override for the <code>GetObject</code> response are <code>Cache-Control</code>
 *     , <code>Content-Disposition</code>, <code>Content-Encoding</code>, <code>Content-Language</code>, <code>
 *     Content-Type</code>, and <code>Expires</code>.
 *
 *     <p>To override values for a set of response headers in the <code>GetObject</code> response, you can use the
 *     following query parameters in the request.
 *
 *     <ul>
 *       <li>
 *         <code>response-cache-control</code>
 *       </li>
 *       <li>
 *         <code>response-content-disposition</code>
 *       </li>
 *       <li>
 *         <code>response-content-encoding</code>
 *       </li>
 *       <li>
 *         <code>response-content-language</code>
 *       </li>
 *       <li>
 *         <code>response-content-type</code>
 *       </li>
 *       <li>
 *         <code>response-expires</code>
 *       </li>
 *     </ul>
 *
 *     <p>When you use these parameters, you must sign the request by using either an Authorization header or a
 *     presigned URL. These parameters cannot be used with an unsigned (anonymous) request.
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
 * <p>The following operations are related to <code>GetObject</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBuckets.html">ListBuckets</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectAcl.html">GetObjectAcl</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To retrieve a byte range of an object </h3>
 *
 * <p>The following example retrieves an object for an S3 bucket. The request specifies the range header to retrieve a specific byte range.{@snippet :
 * var input = GetObjectInput.builder()
 *                 .bucket("examplebucket").key("SampleFile.txt").range("bytes=0-9")
 *                 .build();
 *
 * var result = client.getObject(input);
 * result.equals(GetObjectOutput.builder()
 *                   .acceptRanges("bytes").contentType("text/plain").lastModified(Instant.parse("2014-10-09T22:57:28Z")).contentLength(10).versionId("null").eTag("\"0d94420ffd0bc68cd3d152506b97a9cc\"").contentRange("bytes 0-9/43").metadata(Collections.emptyMap())
 *                   .build());
 * }
 *
 * <h3>To retrieve an object</h3>
 *
 * <p>The following example retrieves an object for an S3 bucket.{@snippet :
 * var input = GetObjectInput.builder()
 *                 .bucket("examplebucket").key("HappyFace.jpg")
 *                 .build();
 *
 * var result = client.getObject(input);
 * result.equals(GetObjectOutput.builder()
 *                   .acceptRanges("bytes").contentType("image/jpeg").lastModified(Instant.parse("2016-12-15T01:19:41Z")).contentLength(3191).versionId("null").eTag("\"6805f2cfc46c0f04559748bb039d69ae\"").tagCount(2).metadata(Collections.emptyMap())
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetObject implements ApiOperation<GetObjectInput, GetObjectOutput> {

    private static final GetObject $INSTANCE = new GetObject();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetObject"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestValidationModeMember", "ChecksumMode")
                    .withMember("responseAlgorithms", ArrayNode.builder()
                        .withValue("CRC64NVME")
                        .withValue("CRC32")
                        .withValue("CRC32C")
                        .withValue("SHA256")
                        .withValue("SHA1")
                        .withValue("SHA512")
                        .withValue("MD5")
                        .withValue("XXHASH64")
                        .withValue("XXHASH3")
                        .withValue("XXHASH128")
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?x-id=GetObject")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InvalidObjectState.$ID, InvalidObjectState.class, InvalidObjectState::builder)
        .putType(NoSuchKey.$ID, NoSuchKey.class, NoSuchKey::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema OUTPUT_STREAM_MEMBER = GetObjectOutput.$SCHEMA.member("Body");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetObject instance() {
        return $INSTANCE;
    }

    private GetObject() {}

    @Override
    public ShapeBuilder<GetObjectInput> inputBuilder() {
        return GetObjectInput.builder();
    }

    @Override
    public ShapeBuilder<GetObjectOutput> outputBuilder() {
        return GetObjectOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetObjectInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetObjectOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InvalidObjectState.$SCHEMA, NoSuchKey.$SCHEMA);
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
        return OUTPUT_STREAM_MEMBER;
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
