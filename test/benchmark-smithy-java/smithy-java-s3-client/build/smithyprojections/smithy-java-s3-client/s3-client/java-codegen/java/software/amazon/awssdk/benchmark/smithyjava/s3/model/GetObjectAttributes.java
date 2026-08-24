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
 * Retrieves all of the metadata from an object without returning the object itself. This operation is useful if you're
 * interested only in an object's metadata.
 *
 * <p><code>GetObjectAttributes</code> combines the functionality of <code>HeadObject</code> and <code>ListParts</code>.
 * All of the data returned with both of those individual calls can be returned with a single call to <code>
 * GetObjectAttributes</code>.
 *
 * <p><b>Directory buckets</b> - For directory buckets, you must make requests for this API operation to the Zonal
 * endpoint. These endpoints support virtual-hosted-style requests in the format <code>https://<i>amzn-s3-demo-bucket</i>
 * .s3express-<i>zone-id</i>.<i>region-code</i>.amazonaws.com/<i>key-name</i></code>. Path-style requests are not
 * supported. For more information about endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">Regional and Zonal endpoints for
 * directory buckets in Availability Zones</a> in the <i>Amazon S3 User Guide</i>. For more information about endpoints
 * in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for directory buckets in Local Zones</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - To use <code>GetObjectAttributes</code>, you must have
 *         READ access to the object.The other permissions that you need to use this operation depend on whether
 *         the bucket is versioned and if a version ID is passed in the <code>GetObjectAttributes</code>
 *         request.
 *
 *         <ul>
 *           <li>
 *             If you pass a version ID in your request, you need both the <code>s3:GetObjectVersion</code>
 *             and <code>s3:GetObjectVersionAttributes</code> permissions.
 *           </li>
 *           <li>
 *             If you do not pass a version ID in your request, you need the <code>s3:GetObject</code> and <code>
 *             s3:GetObjectAttributes</code> permissions.
 *           </li>
 *         </ul>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/using-with-s3-actions.html">Specifying Permissions in a Policy</a> in the <i>Amazon S3 User
 *         Guide</i>.If the object that you request does not exist, the error Amazon S3 returns depends on
 *         whether you also have the <code>s3:ListBucket</code> permission.
 *
 *         <ul>
 *           <li>
 *             If you have the <code>s3:ListBucket</code> permission on the bucket, Amazon S3 returns an
 *             HTTP status code <code>404 Not Found</code> ("no such key") error.
 *           </li>
 *           <li>
 *             If you don't have the <code>s3:ListBucket</code> permission, Amazon S3 returns an HTTP status
 *             code <code>403 Forbidden</code> ("access denied") error.
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
 *         CreateSession</code></a>.If the object is encrypted with SSE-KMS, you must also have the <code>
 *         kms:GenerateDataKey</code> and <code>kms:Decrypt</code> permissions in IAM identity-based policies
 *         and KMS key policies for the KMS key.
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
 *     include this header in a <code>GET</code> request for an object that uses these types of keys, you’ll get an
 *     HTTP <code>400 Bad Request</code> error. It's because the encryption method can't be changed when you
 *     retrieve the object.
 *
 *     <p>If you encrypted an object when you stored the object in Amazon S3 by using server-side encryption with
 *     customer-provided encryption keys (SSE-C), then when you retrieve the metadata from the object, you must use
 *     the following headers. These headers provide the server with the encryption key required to retrieve the
 *     object's metadata. The headers are:
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
 *     <p><b>Directory bucket permissions</b> - For directory buckets, there are only two supported options for
 *     server-side encryption: server-side encryption with Amazon S3 managed keys (SSE-S3) (<code>AES256</code>) and
 *     server-side encryption with KMS keys (SSE-KMS) (<code>aws:kms</code>). We recommend that the bucket's default
 *     encryption uses the desired encryption configuration and you don't override the bucket default encryption in
 *     your <code>CreateSession</code> requests or <code>PUT</code> object requests. Then, new objects are
 *     automatically encrypted with the desired encryption settings. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-serv-side-encryption.html">Protecting data
 *     with server-side encryption</a> in the <i>Amazon S3 User Guide</i>. For more information about the encryption
 *     overriding behaviors in directory buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-specifying-kms-encryption.html">Specifying server-side encryption with KMS for new object
 *     uploads</a>.
 *   </dd>
 *   <dt>
 *     Versioning
 *   </dt>
 *   <dd>
 *
 *     <p><b>Directory buckets</b> - S3 Versioning isn't enabled and supported for directory buckets. For this API
 *     operation, only the <code>null</code> value of the version ID is supported by directory buckets. You can only
 *     specify <code>null</code> to the <code>versionId</code> query parameter in the request.
 *   </dd>
 *   <dt>
 *     Conditional request headers
 *   </dt>
 *   <dd>
 *
 *     <p>Consider the following when using request headers:
 *
 *     <ul>
 *       <li>
 *         If both of the <code>If-Match</code> and <code>If-Unmodified-Since</code> headers are present in the
 *         request as follows, then Amazon S3 returns the HTTP status code <code>200 OK</code> and the data
 *         requested:
 *
 *         <ul>
 *           <li>
 *             <code>If-Match</code> condition evaluates to <code>true</code>.
 *           </li>
 *           <li>
 *             <code>If-Unmodified-Since</code> condition evaluates to <code>false</code>.
 *           </li>
 *         </ul>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
 *       </li>
 *       <li>
 *         If both of the <code>If-None-Match</code> and <code>If-Modified-Since</code> headers are present in
 *         the request as follows, then Amazon S3 returns the HTTP status code <code>304 Not Modified</code>:
 *
 *         <ul>
 *           <li>
 *             <code>If-None-Match</code> condition evaluates to <code>false</code>.
 *           </li>
 *           <li>
 *             <code>If-Modified-Since</code> condition evaluates to <code>true</code>.
 *           </li>
 *         </ul>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
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
 * <p>The following actions are related to <code>GetObjectAttributes</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectAcl.html">GetObjectAcl</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectLegalHold.html">GetObjectLegalHold</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectLockConfiguration.html">GetObjectLockConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectRetention.html">GetObjectRetention</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectTagging.html">GetObjectTagging</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadObject.html">HeadObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListParts.html">ListParts</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class GetObjectAttributes implements ApiOperation<GetObjectAttributesInput, GetObjectAttributesOutput> {

    private static final GetObjectAttributes $INSTANCE = new GetObjectAttributes();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetObjectAttributes"),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?attributes")).build());

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
    public static GetObjectAttributes instance() {
        return $INSTANCE;
    }

    private GetObjectAttributes() {}

    @Override
    public ShapeBuilder<GetObjectAttributesInput> inputBuilder() {
        return GetObjectAttributesInput.builder();
    }

    @Override
    public ShapeBuilder<GetObjectAttributesOutput> outputBuilder() {
        return GetObjectAttributesOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetObjectAttributesInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetObjectAttributesOutput.$SCHEMA;
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
